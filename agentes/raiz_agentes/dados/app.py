"""Agente de dados: métricas de uso de IA do recorte que o Java já autorizou.

A base é histórica, então as métricas de todos os funcionários e períodos são calculadas na subida
(e de novo a cada RECARREGAR_SEGUNDOS) e ficam em memória; cada pergunta só filtra.
"""
import logging
from contextlib import asynccontextmanager
import threading
import time

import pandas as pd
from fastapi import Depends, FastAPI, HTTPException

from ..comum import Recorte, conexao, exigir_token
from ..ml import features
from ..ml.periodos import periodo as datas_periodo

RECARREGAR_SEGUNDOS = 1800
log = logging.getLogger("agente.dados")
@asynccontextmanager
async def _ciclo(_app):
    threading.Thread(target=lambda: _tentar_carregar(), daemon=True).start()   # prepara em segundo plano; /saude já responde
    yield


app = FastAPI(title="rAIz · Agente de dados", version="1.0", lifespan=_ciclo)
_cache: dict = {"features": None, "ferramentas": None, "quando": 0.0}
_trava = threading.Lock()


def _carregar(forcar: bool = False):
    with _trava:
        if forcar or _cache["features"] is None or time.time() - _cache["quando"] > RECARREGAR_SEGUNDOS:
            with conexao() as con:
                _cache["features"] = features.carregar(con)
                _cache["ferramentas"] = features.ferramentas(con)
            _cache["quando"] = time.time()
            log.info("Métricas carregadas: %d linhas.", len(_cache["features"]))
    return _cache["features"], _cache["ferramentas"]


def _num(v, casas=2):
    return None if v is None or pd.isna(v) else round(float(v), casas)


def _metricas(linhas: pd.DataFrame) -> dict:
    """Métricas de uma pessoa (uma linha) ou de um grupo (várias linhas, agregadas)."""
    uso = linhas[linhas.usou_ia]
    req = float(uso.requisicoes.sum())
    pond = (lambda col: _num((uso[col] * uso.chamadas).sum() / uso.chamadas.sum(), 4)) if len(uso) else (lambda col: None)
    return {
        "pessoas": int(len(linhas)), "pessoas_com_uso": int(len(uso)),
        "requisicoes": int(req), "dias_ativos_media": _num(uso.dias_ativos.mean(), 1) if len(uso) else 0,
        "custo_usd": _num(uso.custo_usd.sum()), "custo_por_requisicao_usd": _num(uso.custo_usd.sum() / req, 4) if req else None,
        "tokens_por_requisicao": _num((uso.tokens_entrada.sum() + uso.tokens_saida.sum()) / req, 0) if req else None,
        "taxa_falha": pond("taxa_falha"), "taxa_limite": pond("taxa_limite"),
        "parcela_ferramentas_nao_aprovadas": pond("parcela_nao_aprovada"),
        "eventos_seguranca": int(linhas.eventos_seguranca.sum()), "cursos_concluidos": int(linhas.cursos_concluidos.sum()),
    }




def _tentar_carregar():
    for tentativa in range(30):
        try:
            _carregar(True); return
        except Exception as erro:   # o banco pode ainda estar subindo
            log.warning("Banco indisponível (%s); nova tentativa em 10 s.", erro); time.sleep(10)


@app.get("/saude")
def saude():
    return {"agente": "dados", "pronto": _cache["features"] is not None}


@app.post("/contexto", dependencies=[Depends(exigir_token)])
def contexto(recorte: Recorte):
    try:
        df, ferr = _carregar()
    except Exception as erro:
        raise HTTPException(503, f"Não foi possível ler o banco: {erro}")
    ids = set(recorte.employee_ids)
    atual = df[(df.periodo == recorte.periodo) & df.employee_id.isin(ids)]
    if atual.empty:
        raise HTTPException(404, "Nenhuma pessoa do recorte foi encontrada na base.")
    resposta = {"periodo": recorte.periodo, "inicio": str(datas_periodo(recorte.periodo)[0]),
                "fim": str(datas_periodo(recorte.periodo)[1]), "atual": _metricas(atual)}
    if recorte.periodo > 1:
        resposta["anterior"] = _metricas(df[(df.periodo == recorte.periodo - 1) & df.employee_id.isin(ids)])
    f = ferr[(ferr.periodo == recorte.periodo) & ferr.employee_id.isin(ids)]
    top = (f.groupby(["ferramenta", "aprovada"], as_index=False)[["requisicoes", "custo_usd"]].sum()
             .sort_values("requisicoes", ascending=False).head(5))
    total = f.requisicoes.sum()
    resposta["ferramentas"] = [{"nome": r.ferramenta, "aprovada": bool(r.aprovada), "parcela_requisicoes": _num(r.requisicoes / total, 3),
                                "custo_usd": _num(r.custo_usd)} for r in top.itertuples()] if total else []
    return resposta
