"""Embeddings para a busca vetorial do RAG (políticas e metodologia guardadas no banco).

"local" (padrão): modelo multilíngue rodando dentro do próprio agente (fastembed, ONNX, CPU). O texto das
políticas e das perguntas não sai do servidor. O modelo é baixado no build da imagem Docker.
"""
import hashlib
import math
import re
import unicodedata
from functools import lru_cache

from . import config, llm


class EmbeddingsIndisponiveis(RuntimeError):
    pass


def nome_modelo() -> str:
    p = config.EMBEDDINGS_PROVEDOR
    return {"local": config.EMBEDDINGS_MODELO, "ollama": config.OLLAMA_EMBEDDINGS, "gemini": config.GEMINI_EMBEDDINGS}.get(p, p)


@lru_cache(maxsize=1)
def _local():
    from fastembed import TextEmbedding           # importado só quando usado (testes não precisam dele)
    return TextEmbedding(config.EMBEDDINGS_MODELO, cache_dir=config.EMBEDDINGS_CACHE)


def _hash(texto: str, dimensoes: int = 384) -> list[float]:
    """Vetor determinístico por palavras (apenas para testes, sem modelo)."""
    t = "".join(c for c in unicodedata.normalize("NFD", texto.lower()) if unicodedata.category(c) != "Mn")
    v = [0.0] * dimensoes
    for p in re.findall(r"[a-z0-9]{3,}", t):
        v[int(hashlib.md5(p.encode()).hexdigest(), 16) % dimensoes] += 1.0
    n = math.sqrt(sum(x * x for x in v)) or 1.0
    return [x / n for x in v]


def gerar(textos: list[str]) -> list[list[float]]:
    p = config.EMBEDDINGS_PROVEDOR
    try:
        if p == "local":
            return [vetor.tolist() for vetor in _local().embed(textos)]
        if p == "hash":
            return [_hash(t) for t in textos]
        if p in ("ollama", "gemini"):
            return llm.embeddings(textos, provedor=p)      # reaproveita os clientes de llm.py
    except EmbeddingsIndisponiveis:
        raise
    except Exception as erro:                     # modelo não baixado, provedor fora do ar etc.
        raise EmbeddingsIndisponiveis(f"Embeddings ({p}/{nome_modelo()}) indisponíveis: {erro}") from erro
    raise EmbeddingsIndisponiveis(f"EMBEDDINGS_PROVEDOR desconhecido: {p}")


def cosseno(a, b) -> float:
    na, nb = math.sqrt(sum(x * x for x in a)), math.sqrt(sum(x * x for x in b))
    return sum(x * y for x, y in zip(a, b)) / (na * nb) if na and nb else 0.0
