"""Carga da base de conhecimento: lê as políticas (agentes/politicas/<company_id>.md) e a metodologia
(agentes/docs/*.md), divide em trechos, gera os embeddings e grava no banco da empresa.

Uso:  python -m raiz_agentes.conhecimento.carga        (o docker-compose roda isto antes do agente subir)
É idempotente: documento sem mudança (mesmo texto e mesmo modelo de embeddings) não é regravado.
"""
import glob
import hashlib
import logging
import os
import sys
import time

from sqlalchemy import text
from sqlalchemy.exc import OperationalError

from .. import config, embeddings
from ..comum import conexao_escrita
from . import base

log = logging.getLogger("carga.conhecimento")


def documentos() -> list[dict]:
    docs = []
    for caminho in sorted(glob.glob(os.path.join(config.PASTA_DOCS, "*.md"))):
        nome = os.path.splitext(os.path.basename(caminho))[0]
        docs.append({"documento_id": f"metodologia:{nome}", "company_id": None, "tipo": "metodologia", "caminho": caminho})
    for caminho in sorted(glob.glob(os.path.join(config.PASTA_POLITICAS, "*.md"))):
        empresa = os.path.splitext(os.path.basename(caminho))[0]
        docs.append({"documento_id": f"politica:{empresa}", "company_id": empresa, "tipo": "politica", "caminho": caminho})
    return docs


def carregar() -> dict:
    modelo = embeddings.nome_modelo()
    resumo = {"gravados": 0, "sem_mudanca": 0, "trechos": 0}
    with conexao_escrita() as con:          # SQLAlchemy 2: transação automática, confirmada com commit()
        base.criar_tabelas(con)
        con.commit()
        empresas = {r[0] for r in con.execute(text("SELECT company_id FROM companies"))}
        for doc in documentos():
            if doc["company_id"] and doc["company_id"] not in empresas:
                log.warning("Política ignorada: empresa %s não existe na base.", doc["company_id"]); continue
            with open(doc["caminho"], encoding="utf-8") as f:
                conteudo = f.read()
            assinatura = hashlib.sha256((modelo + "\n" + conteudo).encode()).hexdigest()
            atual = con.execute(text("SELECT hash_conteudo FROM conhecimento_documentos WHERE documento_id = :d"),
                                {"d": doc["documento_id"]}).scalar()
            if atual == assinatura:
                resumo["sem_mudanca"] += 1; con.rollback(); continue
            trechos = base.dividir(conteudo, os.path.basename(doc["caminho"]))
            vetores = embeddings.gerar([t["texto"] for t in trechos])
            # documento inteiro numa transação: apaga a versão antiga e grava a nova
            con.execute(text("DELETE FROM conhecimento_documentos WHERE documento_id = :d"), {"d": doc["documento_id"]})
            con.execute(text("""INSERT INTO conhecimento_documentos (documento_id, company_id, tipo, titulo, hash_conteudo, modelo_embeddings)
                                VALUES (:d, :c, :t, :ti, :h, :m)"""),
                        {"d": doc["documento_id"], "c": doc["company_id"], "t": doc["tipo"],
                         "ti": base.titulo(conteudo, doc["documento_id"])[:300], "h": assinatura, "m": modelo})
            con.execute(text("""INSERT INTO conhecimento_trechos (documento_id, ordem, secao, texto, embedding)
                                VALUES (:d, :o, :s, :x, :e)"""),
                        [{"d": doc["documento_id"], "o": i, "s": t["secao"][:300], "x": t["texto"], "e": v}
                         for i, (t, v) in enumerate(zip(trechos, vetores))])
            con.commit()
            resumo["gravados"] += 1; resumo["trechos"] += len(trechos)
        # documentos removidos da pasta saem do banco
        ids = [d["documento_id"] for d in documentos()]
        removidos = con.execute(text("DELETE FROM conhecimento_documentos WHERE NOT (documento_id = ANY(:ids))"), {"ids": ids}).rowcount
        con.commit()
        resumo["removidos"] = removidos
    return resumo


def main() -> int:
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(name)s %(message)s")
    for tentativa in range(30):
        try:
            resumo = carregar()
            log.info("Base de conhecimento pronta (modelo %s): %s", embeddings.nome_modelo(), resumo)
            return 0
        except embeddings.EmbeddingsIndisponiveis as erro:
            log.error("%s", erro); return 1
        except OperationalError as erro:              # banco ainda subindo
            log.warning("Banco indisponível (%s); nova tentativa em 5 s.", erro); time.sleep(5)
    return 1


if __name__ == "__main__":
    sys.exit(main())
