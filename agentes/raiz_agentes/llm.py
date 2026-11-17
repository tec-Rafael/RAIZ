"""Fachada da LLM: o orquestrador e o agente de conhecimento chamam só daqui, e o provedor vem de
LLM_PROVEDOR ("claude", "gemini" ou "ollama"). Uso somente de inferência: nenhum modelo é treinado.

Claude (API da Anthropic, POST /v1/messages):
- claude-haiku-5-5 com esforço "low" e raciocínio desligado, o jeito mais rápido e barato indicado na
  documentação; a versão 2026-10-21 da API não aceita temperature/top_p/top_k;
- a Anthropic não tem modelo de embeddings: a busca vetorial usa embeddings.py (modelo local, por padrão).

Gemini (API REST do Google AI Studio):
- generateContent com systemInstruction e generationConfig (o 2.5 Flash-Lite vem com o raciocínio
  desligado por padrão, o que mantém a resposta rápida e barata);
- embeddings com batchEmbedContents (gemini-embedding-001).
"""
import httpx

from . import config, ollama


class LLMIndisponivel(RuntimeError):
    pass


class LimiteExcedido(LLMIndisponivel):
    """Limite de requisições ou cota do provedor (HTTP 429), comum na camada gratuita."""


def modelo() -> str:
    return {"claude": config.CLAUDE_MODELO, "gemini": config.GEMINI_MODELO}.get(config.LLM_PROVEDOR, config.OLLAMA_MODELO)


def tem_embeddings() -> bool:
    return config.LLM_PROVEDOR in ("gemini", "ollama")


async def _claude(mensagens: list[dict]) -> str:
    if not config.ANTHROPIC_API_KEY:
        raise LLMIndisponivel("ANTHROPIC_API_KEY não configurada.")
    corpo = {"model": config.CLAUDE_MODELO, "max_tokens": 1024,
             "system": "\n\n".join(m["content"] for m in mensagens if m["role"] == "system"),
             "messages": [{"role": m["role"], "content": m["content"]} for m in mensagens if m["role"] != "system"],
             "thinking": {"type": "disabled"}, "output_config": {"effort": "low"}}
    cabecalhos = {"x-api-key": config.ANTHROPIC_API_KEY, "anthropic-version": config.ANTHROPIC_VERSION,
                  "content-type": "application/json"}
    if config.ANTHROPIC_WORKSPACE_ID:
        cabecalhos["anthropic-workspace-id"] = config.ANTHROPIC_WORKSPACE_ID
    try:
        async with httpx.AsyncClient(timeout=config.LLM_TIMEOUT) as cliente:
            r = await cliente.post(config.ANTHROPIC_URL + "/v1/messages", headers=cabecalhos, json=corpo)
    except httpx.HTTPError as erro:
        raise LLMIndisponivel(f"API da Anthropic inacessível: {erro}") from erro
    if r.status_code == 429:
        raise LimiteExcedido("Limite de uso da API da Anthropic atingido (HTTP 429).")
    if r.status_code >= 400:
        try:
            detalhe = r.json().get("error", {})
        except ValueError:
            detalhe = {"message": r.text[:200]}
        raise LLMIndisponivel(f"Anthropic respondeu {r.status_code} ({detalhe.get('type')}): {detalhe.get('message')}")
    try:
        texto = "".join(b.get("text", "") for b in r.json()["content"] if b.get("type") == "text").strip()
    except (KeyError, ValueError) as erro:
        raise LLMIndisponivel(f"Resposta inesperada da Anthropic: {r.text[:200]}") from erro
    if not texto:
        raise LLMIndisponivel("A API da Anthropic não devolveu texto.")
    return texto


def _cabecalhos() -> dict:
    if not config.GEMINI_API_KEY:
        raise LLMIndisponivel("GEMINI_API_KEY não configurada.")
    return {"x-goog-api-key": config.GEMINI_API_KEY, "Content-Type": "application/json"}


def _erro_gemini(r: httpx.Response) -> None:
    if r.status_code == 429:
        raise LimiteExcedido("Limite de uso da API do Gemini atingido (HTTP 429).")
    if r.status_code >= 400:
        try:
            detalhe = r.json().get("error", {}).get("message", "")
        except ValueError:
            detalhe = r.text[:200]
        raise LLMIndisponivel(f"Gemini respondeu {r.status_code}: {detalhe}")


async def conversar(mensagens: list[dict], temperatura: float = 0.2) -> str:
    if config.LLM_PROVEDOR == "claude":
        return await _claude(mensagens)
    if config.LLM_PROVEDOR != "gemini":
        try:
            return await ollama.conversar(mensagens, temperatura)
        except ollama.OllamaIndisponivel as erro:
            raise LLMIndisponivel(str(erro)) from erro
    sistema = "\n\n".join(m["content"] for m in mensagens if m["role"] == "system")
    corpo = {"systemInstruction": {"parts": [{"text": sistema}]},
             "contents": [{"role": "model" if m["role"] == "assistant" else "user", "parts": [{"text": m["content"]}]}
                          for m in mensagens if m["role"] != "system"],
             "generationConfig": {"temperature": temperatura, "maxOutputTokens": 600}}
    try:
        async with httpx.AsyncClient(timeout=config.LLM_TIMEOUT) as cliente:
            r = await cliente.post(f"{config.GEMINI_URL}/models/{config.GEMINI_MODELO}:generateContent",
                                   headers=_cabecalhos(), json=corpo)
    except httpx.HTTPError as erro:
        raise LLMIndisponivel(f"Gemini inacessível: {erro}") from erro
    _erro_gemini(r)
    try:
        partes = r.json()["candidates"][0]["content"]["parts"]
        texto = "".join(p.get("text", "") for p in partes if not p.get("thought")).strip()
    except (KeyError, IndexError, ValueError) as erro:
        raise LLMIndisponivel(f"Resposta inesperada do Gemini: {r.text[:200]}") from erro
    if not texto:
        raise LLMIndisponivel("O Gemini não devolveu texto (resposta vazia ou bloqueada).")
    return texto


def embeddings(textos: list[str], provedor: str | None = None) -> list[list[float]]:
    p = provedor or config.LLM_PROVEDOR
    if p not in ("gemini", "ollama"):
        raise LLMIndisponivel(f"O provedor {p} não tem embeddings.")
    if p != "gemini":
        try:
            return ollama.embeddings(textos)
        except ollama.OllamaIndisponivel as erro:
            raise LLMIndisponivel(str(erro)) from erro
    vetores = []
    try:
        for inicio in range(0, len(textos), 100):            # o lote aceita até 100 textos
            lote = textos[inicio:inicio + 100]
            corpo = {"requests": [{"model": f"models/{config.GEMINI_EMBEDDINGS}", "content": {"parts": [{"text": t}]}} for t in lote]}
            r = httpx.post(f"{config.GEMINI_URL}/models/{config.GEMINI_EMBEDDINGS}:batchEmbedContents",
                           headers=_cabecalhos(), json=corpo, timeout=config.LLM_TIMEOUT)
            _erro_gemini(r)
            vetores += [e["values"] for e in r.json()["embeddings"]]
    except (httpx.HTTPError, KeyError, ValueError) as erro:
        raise LLMIndisponivel(f"Embeddings do Gemini indisponíveis: {erro}") from erro
    return vetores
