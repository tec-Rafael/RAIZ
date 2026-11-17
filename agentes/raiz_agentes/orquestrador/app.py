"""Agente orquestrador: recebe a pergunta (do Java, já com o recorte autorizado), consulta os agentes de
dados, perfil e conhecimento em paralelo por HTTP, monta o prompt e pede a resposta ao Ollama."""
import asyncio
import logging
import time
from typing import Literal

import httpx
from fastapi import Depends, FastAPI, HTTPException, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from pydantic import BaseModel, Field

from .. import config
from ..comum import exigir_token
from .. import llm
from . import prompt

logging.basicConfig(level=logging.INFO)
log = logging.getLogger("agente.orquestrador")
app = FastAPI(title="rAIz · Agente orquestrador (assistente)", version="1.0")


@app.exception_handler(RequestValidationError)
async def _pedido_invalido(request: Request, erro: RequestValidationError):
    """Registra no log por que o pedido do Java foi recusado (422), para facilitar o diagnóstico."""
    log.warning("Pedido recusado (422): %s", erro.errors())
    return JSONResponse(status_code=422, content={"detail": erro.errors()})


class Usuario(BaseModel):
    nome: str = Field(max_length=200)
    perfil: Literal["colaborador", "gestor", "administrador"]


class Escopo(BaseModel):
    tipo: Literal["pessoal", "grupo"]
    descricao: str = Field(max_length=300)
    proprio: bool = False          # pessoal e sobre o próprio usuário
    nome: str | None = Field(default=None, max_length=200)   # pessoal: nome da pessoa do dashboard
    empresas: list[str] = Field(default_factory=list, max_length=100)   # empresas do recorte (filtra as políticas)
    employee_ids: list[str] = Field(alias="employeeIds", min_length=1, max_length=5000)
    model_config = {"populate_by_name": True}


class Pessoal(BaseModel):
    """Dados do próprio gestor quando ele pergunta no dashboard da equipe."""
    employee_ids: list[str] = Field(alias="employeeIds", min_length=1, max_length=1)
    ams: dict | None = None
    model_config = {"populate_by_name": True}


class Equipe(BaseModel):
    """Comparação anônima com os colegas de área (só para quem pergunta sobre os próprios dados).
    Os ids servem apenas para os agentes calcularem médias e contagens; nunca entram no texto do prompt."""
    colegas: int = Field(ge=0)
    ams: dict | None = None
    referencia: list[str] = Field(default_factory=list, max_length=5000)
    colegas_ids: list[str] = Field(alias="colegasIds", default_factory=list, max_length=5000)
    model_config = {"populate_by_name": True}


MODOS = Literal["colaborador", "gestor_proprio", "gestor_colaborador", "gestor_equipe", "administrador", "administrador_pessoa"]


class Pedido(BaseModel):
    pergunta: str = Field(min_length=2, max_length=500)
    usuario: Usuario
    periodo: int = Field(ge=1, le=5)
    escopo: Escopo
    ams: dict | None = None        # resumo do AMS calculado pelo Java (nota, notaAnterior, dimensões...)
    modo: MODOS | None = None      # o que a IA pode responder (definido pelo Java a partir do perfil e do filtro)
    pessoal: Pessoal | None = None
    equipe: Equipe | None = None

    def modo_efetivo(self) -> str:
        if self.modo:
            return self.modo
        if self.escopo.tipo == "pessoal":
            return "colaborador" if self.escopo.proprio else "administrador_pessoa"
        return "administrador"


async def _chamar(cliente: httpx.AsyncClient, url: str, corpo: dict, nome: str):
    try:
        r = await cliente.post(url, json=corpo, headers={"X-Agentes-Token": config.AGENTES_TOKEN})
        r.raise_for_status()
        return r.json()
    except httpx.HTTPError as erro:          # um agente fora do ar não derruba a resposta: o contexto fica menor
        log.warning("Agente %s indisponível: %s", nome, erro)
        return None


async def _extras(pedido: Pedido) -> dict:
    """Dados que dependem do modo: os do próprio gestor (dashboard da equipe) e a comparação anônima com os colegas."""
    tarefas, nomes = [], []
    async with httpx.AsyncClient(timeout=30) as cliente:
        if pedido.pessoal:
            r = {"employeeIds": pedido.pessoal.employee_ids, "periodo": pedido.periodo}
            tarefas += [_chamar(cliente, config.AGENTE_DADOS_URL + "/contexto", r, "dados"),
                        _chamar(cliente, config.AGENTE_PERFIL_URL + "/perfil", r, "perfil")]
            nomes += ["pessoal_dados", "pessoal_perfil"]
        eq = pedido.equipe
        if eq and len(eq.colegas_ids) >= 3:
            tarefas.append(_chamar(cliente, config.AGENTE_DADOS_URL + "/contexto", {"employeeIds": eq.colegas_ids, "periodo": pedido.periodo}, "dados"))
            nomes.append("colegas_dados")
        if eq and len(eq.referencia) >= 3:
            tarefas += [_chamar(cliente, config.AGENTE_DADOS_URL + "/contexto", {"employeeIds": eq.referencia, "periodo": pedido.periodo}, "dados"),
                        _chamar(cliente, config.AGENTE_PERFIL_URL + "/cursos_colegas",
                                {"employeeId": pedido.escopo.employee_ids[0], "referencia": eq.referencia, "periodo": pedido.periodo}, "perfil")]
            nomes += ["referencia_dados", "referencia_cursos"]
        resultados = await asyncio.gather(*tarefas) if tarefas else []
    return dict(zip(nomes, resultados))


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
    extras = await _extras(pedido)
    mensagens = prompt.montar(pedido, dados, perfil, conhecimento, **extras)
    try:
        resposta = await llm.conversar(mensagens)
    except llm.LimiteExcedido as erro:
        log.warning("%s", erro)
        raise HTTPException(429, "Muitas perguntas em pouco tempo. Aguarde um minuto e tente de novo.")
    except llm.LLMIndisponivel as erro:
        log.error("%s", erro)
        raise HTTPException(503, "O assistente está indisponível no momento. Tente novamente em instantes.")
    fontes = [n for n, v in (("dados", dados), ("perfil", perfil)) if v]
    if extras.get("colegas_dados") or extras.get("referencia_cursos"):
        fontes.append("colegas")
    if (metodologia or {}).get("trechos"):
        fontes.append("metodologia")
    fontes += sorted({f"politica:{t['empresa']}" for t in (politicas or {}).get("trechos", [])})
    return {"resposta": resposta, "fontes": fontes, "modelo": llm.modelo(),
            "tempo_ms": round((time.perf_counter() - inicio) * 1000)}
