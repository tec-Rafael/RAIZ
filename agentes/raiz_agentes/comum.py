"""Peças comuns aos agentes: token interno, banco (somente leitura) e contratos de entrada."""
import hmac
from functools import lru_cache

from fastapi import Header, HTTPException
from pydantic import BaseModel, Field
from sqlalchemy import create_engine, text

from . import config


def exigir_token(x_agentes_token: str = Header(default="")) -> None:
    """Toda chamada entre agentes (e do Java) precisa do token compartilhado."""
    if not hmac.compare_digest(x_agentes_token.encode(), config.AGENTES_TOKEN.encode()):
        raise HTTPException(status_code=401, detail="Token de agente ausente ou inválido.")


@lru_cache(maxsize=1)
def motor():
    engine = create_engine(config.BANCO_URL, pool_pre_ping=True, pool_size=2, max_overflow=2)
    return engine


def conexao():
    """Conexão em transação somente leitura: os agentes nunca escrevem no banco."""
    con = motor().connect()
    con.execute(text("SET TRANSACTION READ ONLY"))
    return con


def conexao_escrita():
    """Só para a carga da base de conhecimento (conhecimento/carga.py); os agentes usam conexao()."""
    return motor().connect()


class Recorte(BaseModel):
    """Recorte já autorizado pelo Java (o agente não decide permissões)."""
    employee_ids: list[str] = Field(alias="employeeIds", min_length=1, max_length=5000)
    periodo: int = Field(ge=1, le=5)

    model_config = {"populate_by_name": True}
