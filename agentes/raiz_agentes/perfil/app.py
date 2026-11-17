"""Agente de perfil: perfil de uso de IA (k-means), pontos de atenção e cursos recomendados.

Na primeira subida o modelo é treinado com a base e salvo em PASTA_DADOS; nas próximas, só é carregado.
POST /treinar refaz o treino (por exemplo, depois de importar dados novos).
"""
import logging
from contextlib import asynccontextmanager
import os
import threading
import time

import joblib
import pandas as pd
from fastapi import Depends, FastAPI, HTTPException
from pydantic import BaseModel, Field

from .. import config
from ..comum import Recorte, conexao, exigir_token
from ..ml import features, treinamentos
from ..ml.perfis import PERFIS, ModeloPerfis

log = logging.getLogger("agente.perfil")
@asynccontextmanager
async def _ciclo(_app):
    threading.Thread(target=lambda: _tentar(), daemon=True).start()   # prepara em segundo plano; /saude já responde
    yield


app = FastAPI(title="rAIz · Agente de perfil (machine learning)", version="1.0", lifespan=_ciclo)
ARQUIVO = os.path.join(config.PASTA_DADOS, "modelo_perfis.joblib")
_estado: dict = {"modelo": None, "df": None, "treinos": None}
_trava = threading.Lock()


def _preparar(treinar: bool = False):
    with _trava:
        with conexao() as con:
            df = features.carregar(con)
            trein = treinamentos.carregar(con)
        if treinar or not os.path.exists(ARQUIVO):
            modelo = ModeloPerfis.treinar(df)
            os.makedirs(config.PASTA_DADOS, exist_ok=True)
            joblib.dump(modelo, ARQUIVO)
            log.info("Modelo de perfis treinado: %s", modelo.metricas)
        else:
            modelo = joblib.load(ARQUIVO)
        df["perfil"] = modelo.atribuir(df)
        _estado.update(modelo=modelo, df=df, treinos=trein)


def _tentar():
    for _ in range(30):
        try:
            _preparar(); return
        except Exception as erro:
            log.warning("Não foi possível preparar o modelo (%s); nova tentativa em 10 s.", erro); time.sleep(10)




@app.get("/saude")
def saude():
    m = _estado["modelo"]
    return {"agente": "perfil", "pronto": m is not None, "metricas": m.metricas if m else None}


@app.post("/treinar", dependencies=[Depends(exigir_token)])
def treinar():
    _preparar(treinar=True)
    return {"metricas": _estado["modelo"].metricas}


class Colegas(BaseModel):
    """Pessoa e grupo de referência (colegas de área com AMS maior), já autorizados pelo Java."""
    employee_id: str = Field(alias="employeeId", max_length=60)
    referencia: list[str] = Field(default_factory=list, max_length=5000)
    periodo: int = Field(ge=1, le=5)
    model_config = {"populate_by_name": True}


@app.post("/cursos_colegas", dependencies=[Depends(exigir_token)])
def cursos_colegas(pedido: Colegas):
    """Cursos e categorias mais concluídos pelos colegas de referência (só contagens, sem identificar ninguém),
    e a distribuição de perfis de uso desse grupo."""
    modelo, df, trein = _estado["modelo"], _estado["df"], _estado["treinos"]
    if modelo is None:
        raise HTTPException(503, "O modelo de perfis ainda está sendo preparado.")
    r = treinamentos.cursos_dos_colegas(pedido.employee_id, pedido.referencia, trein)
    grupo = df[(df.periodo == pedido.periodo) & df.employee_id.isin(set(pedido.referencia) - {pedido.employee_id})]
    if len(grupo) >= 3:
        contagem = grupo.perfil.value_counts()
        r["perfis"] = [{"nome": PERFIS[p]["nome"], "pessoas": int(n)} for p, n in contagem.items() if n >= 2]
    return r


@app.post("/perfil", dependencies=[Depends(exigir_token)])
def perfil(recorte: Recorte):
    modelo, df, trein = _estado["modelo"], _estado["df"], _estado["treinos"]
    if modelo is None:
        raise HTTPException(503, "O modelo de perfis ainda está sendo preparado.")
    atual = df[(df.periodo == recorte.periodo) & df.employee_id.isin(set(recorte.employee_ids))]
    if atual.empty:
        raise HTTPException(404, "Nenhuma pessoa do recorte foi encontrada na base.")
    if len(atual) == 1:
        linha = atual.iloc[0]
        explicacao = modelo.explicar(linha.drop("perfil"))
        doperiodo = df[df.periodo == recorte.periodo].set_index("employee_id")
        cursos = treinamentos.recomendar(linha.employee_id, explicacao["perfil"], explicacao["codigos_atencao"], linha.cargo,
                                         trein, doperiodo.perfil, doperiodo.cargo)
        anterior = df[(df.periodo == recorte.periodo - 1) & (df.employee_id == linha.employee_id)]
        explicacao.pop("codigos_atencao")
        return {"tipo": "pessoal", **explicacao, "cursos_recomendados": cursos,
                "perfil_anterior": PERFIS[anterior.perfil.iloc[0]]["nome"] if len(anterior) else None}
    # Grupo: distribuição dos perfis e pontos de atenção mais frequentes (sem expor pessoas)
    contagem = atual.perfil.value_counts()
    codigos = pd.Series([c for _, l in atual[atual.usou_ia].iterrows() for c in modelo.explicar(l.drop("perfil"))["codigos_atencao"]],
                        dtype=object).value_counts()
    textos = {"taxa_falha": "falhas acima do comum", "taxa_limite": "requisições barradas por limite de uso",
              "parcela_nao_aprovada": "uso de ferramentas não aprovadas", "custo_por_requisicao": "custo por requisição alto",
              "dias_ativos": "poucos dias de uso", "eventos_seguranca": "eventos de segurança"}
    return {"tipo": "grupo", "pessoas": int(len(atual)),
            "perfis": [{"perfil": p, "nome": PERFIS[p]["nome"], "pessoas": int(n), "parcela": round(n / len(atual), 3),
                        "recomendacoes": PERFIS[p]["recomendacoes"]} for p, n in contagem.items()],
            "pontos_de_atencao": [{"ponto": textos.get(c, c), "pessoas": int(n)} for c, n in codigos.head(5).items()]}
