"""Agente de conhecimento (RAG): responde só com o que está na base de conhecimento do banco da empresa
(políticas de uso de IA de cada empresa e metodologia do AMS), nunca com conteúdo da internet.

- POST /buscar: gera o embedding da pergunta e devolve os trechos mais parecidos (cosseno), filtrando as
  políticas pelas empresas do recorte. Documentos sem empresa (metodologia) valem para todos.
- POST /todos: todos os trechos de um tipo (a metodologia vai inteira em cada pergunta no modo "completo").
A base é gravada por conhecimento/carga.py; este agente só lê (transação somente leitura).
"""
import logging
import threading
import time
from contextlib import asynccontextmanager
from typing import Literal

import numpy as np
from fastapi import Depends, FastAPI, HTTPException
from pydantic import BaseModel, Field

from .. import embeddings
from ..comum import conexao, exigir_token
from .base import carregar_trechos, dividir  # noqa: F401  (dividir é reexportado para os testes)

RECARREGAR_SEGUNDOS = 300
log = logging.getLogger("agente.conhecimento")
_indice: dict = {"trechos": [], "matriz": None, "quando": 0.0, "erro": None}
_trava = threading.Lock()


def _montar(trechos: list[dict]) -> None:
    matriz = None
    if trechos:
        m = np.array([t["embedding"] for t in trechos], dtype=np.float32)
        matriz = m / np.maximum(np.linalg.norm(m, axis=1, keepdims=True), 1e-9)
    _indice.update(trechos=[{k: v for k, v in t.items() if k != "embedding"} for t in trechos], matriz=matriz,
                   quando=time.time(), erro=None)


def recarregar() -> None:
    with _trava:
        with conexao() as con:
            _montar(carregar_trechos(con))
        log.info("Base de conhecimento carregada do banco: %d trechos.", len(_indice["trechos"]))


def _tentar():
    for _ in range(60):
        try:
            recarregar(); return
        except Exception as erro:                    # tabelas ainda não criadas pela carga, banco subindo...
            _indice["erro"] = str(erro); log.warning("Base de conhecimento indisponível (%s); nova tentativa em 10 s.", erro)
            time.sleep(10)


@asynccontextmanager
async def _ciclo(_app):
    threading.Thread(target=_tentar, daemon=True).start()
    yield


app = FastAPI(title="rAIz · Agente de conhecimento (RAG no banco)", version="2.0", lifespan=_ciclo)


class Busca(BaseModel):
    pergunta: str = Field(min_length=1, max_length=1000)
    empresas: list[str] = Field(default_factory=list, max_length=100)   # vazio: só documentos gerais
    quantidade: int = Field(default=4, ge=1, le=10)
    tipo: Literal["politica", "metodologia", "todos"] = "todos"


class Todos(BaseModel):
    tipo: Literal["politica", "metodologia"] = "metodologia"


def _atualizar_se_preciso():
    if time.time() - _indice["quando"] > RECARREGAR_SEGUNDOS:
        try:
            recarregar()
        except Exception as erro:
            log.warning("Falha ao recarregar a base (%s); usando a versão em memória.", erro)


def _publico(t: dict, relevancia: float | None = None) -> dict:
    r = {"fonte": t["titulo"], "documento": t["documento_id"], "tipo": t["tipo"], "empresa": t["company_name"],
         "secao": t["secao"], "texto": t["texto"]}
    if relevancia is not None:
        r["relevancia"] = round(relevancia, 3)
    return r


@app.get("/saude")
def saude():
    tipos = {}
    for t in _indice["trechos"]:
        tipos[t["tipo"]] = tipos.get(t["tipo"], 0) + 1
    return {"agente": "conhecimento", "pronto": _indice["matriz"] is not None, "trechos": len(_indice["trechos"]),
            "por_tipo": tipos, "embeddings": embeddings.nome_modelo(), "erro": _indice["erro"]}


@app.post("/recarregar", dependencies=[Depends(exigir_token)])
def recarregar_agora():
    recarregar()
    return saude()


@app.post("/todos", dependencies=[Depends(exigir_token)])
def todos(pedido: Todos | None = None):
    _atualizar_se_preciso()
    tipo = (pedido or Todos()).tipo
    return {"modo": "completo", "trechos": [_publico(t) for t in _indice["trechos"] if t["tipo"] == tipo]}


@app.post("/buscar", dependencies=[Depends(exigir_token)])
def buscar(busca: Busca):
    _atualizar_se_preciso()
    if _indice["matriz"] is None:
        raise HTTPException(503, "A base de conhecimento ainda não foi carregada.")
    empresas = set(busca.empresas)
    candidatos = [i for i, t in enumerate(_indice["trechos"])
                  if (busca.tipo == "todos" or t["tipo"] == busca.tipo) and (t["company_id"] is None or t["company_id"] in empresas)]
    if not candidatos:
        return {"modo": "vetorial", "trechos": []}
    try:
        q = np.array(embeddings.gerar([busca.pergunta])[0], dtype=np.float32)
    except embeddings.EmbeddingsIndisponiveis as erro:
        raise HTTPException(503, str(erro))
    q /= max(float(np.linalg.norm(q)), 1e-9)
    notas = _indice["matriz"][candidatos] @ q
    ordem = np.argsort(-notas)[:busca.quantidade]
    return {"modo": "vetorial", "trechos": [_publico(_indice["trechos"][candidatos[k]], float(notas[k])) for k in ordem]}
