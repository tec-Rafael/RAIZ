"""Agente orquestrador: recebe a pergunta (do Java, já com o recorte autorizado), consulta os agentes de
dados, perfil e conhecimento em paralelo por HTTP, monta o prompt e pede a resposta ao Ollama."""
import asyncio
import logging
import time
from typing import Literal

import httpx
from fastapi import Depends, FastAPI, HTTPException
from pydantic import BaseModel, Field

from .. import config
from ..comum import exigir_token
from .. import llm
from . import prompt

log = logging.getLogger("agente.orquestrador")
app = FastAPI(title="rAIz · Agente orquestrador (assistente)", version="1.0")


class Usuario(BaseModel):
    nome: str = Field(max_length=200)
    perfil: Literal["colaborador", "gestor", "administrador"]


class Escopo(BaseModel):
    tipo: Literal["pessoal", "grupo"]
    descricao: str = Field(max_length=300)
    proprio: bool = False          # pessoal e sobre o próprio usuário
    empresas: list[str] = Field(default_factory=list, max_length=100)   # empresas do recorte (filtra as políticas)
    employee_ids: list[str] = Field(alias="employeeIds", min_length=1, max_length=5000)
    model_config = {"populate_by_name": True}


class Pedido(BaseModel):
    pergunta: str = Field(min_length=2, max_length=500)
    usuario: Usuario
    periodo: int = Field(ge=1, le=5)
    escopo: Escopo
    ams: dict | None = None        # resumo do AMS calculado pelo Java (nota, notaAnterior, dimensões...)


async def _chamar(cliente: httpx.AsyncClient, url: str, corpo: dict, nome: str):
    try:
        r = await cliente.post(url, json=corpo, headers={"X-Agentes-Token": config.AGENTES_TOKEN})
        r.raise_for_status()
        return r.json()
    except httpx.HTTPError as erro:          # um agente fora do ar não derruba a resposta: o contexto fica menor
        log.warning("Agente %s indisponível: %s", nome, erro)
        return None


@app.get("/saude")
async def saude():
    async with httpx.AsyncClient(timeout=3) as c:
        async def ver(url):
            try:
                return (await c.get(url + "/saude")).json()
            except httpx.HTTPError:
                return {"pronto": False}
        dados, perfil, conh = await asyncio.gather(ver(config.AGENTE_DADOS_URL), ver(config.AGENTE_PERFIL_URL), ver(config.AGENTE_CONHECIMENTO_URL))
    return {"agente": "orquestrador", "provedor": config.LLM_PROVEDOR, "modelo": llm.modelo(), "dados": dados, "perfil": perfil, "conhecimento": conh}


@app.post("/perguntar", dependencies=[Depends(exigir_token)])
async def perguntar(pedido: Pedido):
    inicio = time.perf_counter()
    recorte = {"employeeIds": pedido.escopo.employee_ids, "periodo": pedido.periodo}
    conh = config.AGENTE_CONHECIMENTO_URL
    async with httpx.AsyncClient(timeout=30) as cliente:
        dados, perfil, metodologia, politicas = await asyncio.gather(
            _chamar(cliente, config.AGENTE_DADOS_URL + "/contexto", recorte, "dados"),
            _chamar(cliente, config.AGENTE_PERFIL_URL + "/perfil", recorte, "perfil"),
            _chamar(cliente, conh + "/todos", {"tipo": "metodologia"}, "conhecimento") if config.RAG_MODO == "completo"
            else _chamar(cliente, conh + "/buscar", {"pergunta": pedido.pergunta, "quantidade": 3, "tipo": "metodologia"}, "conhecimento"),
            _chamar(cliente, conh + "/buscar", {"pergunta": pedido.pergunta, "empresas": pedido.escopo.empresas,
                                                 "quantidade": config.RAG_TRECHOS_POLITICA, "tipo": "politica"}, "conhecimento")
            if pedido.escopo.empresas else asyncio.sleep(0, result=None))
    conhecimento = {"metodologia": metodologia, "politicas": politicas}
    mensagens = prompt.montar(pedido, dados, perfil, conhecimento)
    try:
        resposta = await llm.conversar(mensagens)
    except llm.LimiteExcedido as erro:
        log.warning("%s", erro)
        raise HTTPException(429, "Muitas perguntas em pouco tempo. Aguarde um minuto e tente de novo.")
    except llm.LLMIndisponivel as erro:
        log.error("%s", erro)
        raise HTTPException(503, "O assistente está indisponível no momento. Tente novamente em instantes.")
    fontes = [n for n, v in (("dados", dados), ("perfil", perfil)) if v]
    if (metodologia or {}).get("trechos"):
        fontes.append("metodologia")
    fontes += sorted({f"politica:{t['empresa']}" for t in (politicas or {}).get("trechos", [])})
    return {"resposta": resposta, "fontes": fontes, "modelo": llm.modelo(),
            "tempo_ms": round((time.perf_counter() - inicio) * 1000)}
