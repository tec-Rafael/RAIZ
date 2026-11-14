"""Cliente mínimo do Ollama (somente inferência: chat e embeddings)."""
import httpx

from . import config


class OllamaIndisponivel(RuntimeError):
    pass


async def conversar(mensagens: list[dict], temperatura: float = 0.2) -> str:
    corpo = {"model": config.OLLAMA_MODELO, "messages": mensagens, "stream": False,
             "options": {"temperature": temperatura, "num_ctx": 8192}}
    try:
        async with httpx.AsyncClient(timeout=config.OLLAMA_TIMEOUT) as cliente:
            r = await cliente.post(config.OLLAMA_URL + "/api/chat", json=corpo)
            r.raise_for_status()
            return r.json()["message"]["content"].strip()
    except (httpx.HTTPError, KeyError, ValueError) as erro:
        raise OllamaIndisponivel(f"Ollama indisponível ou modelo {config.OLLAMA_MODELO} não baixado: {erro}") from erro


def embeddings(textos: list[str]) -> list[list[float]]:
    try:
        r = httpx.post(config.OLLAMA_URL + "/api/embed", json={"model": config.OLLAMA_EMBEDDINGS, "input": textos},
                       timeout=config.OLLAMA_TIMEOUT)
        r.raise_for_status()
        return r.json()["embeddings"]
    except (httpx.HTTPError, KeyError, ValueError) as erro:
        raise OllamaIndisponivel(f"Embeddings indisponíveis ({config.OLLAMA_EMBEDDINGS}): {erro}") from erro
