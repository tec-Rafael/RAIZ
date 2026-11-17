"""Base de conhecimento no banco da empresa: documentos (políticas por empresa e metodologia do AMS) divididos
em trechos, cada um com seu embedding. Sem pgvector: o vetor fica numa coluna REAL[] e a similaridade
(cosseno) é calculada no agente."""
import re

from sqlalchemy import text

DDL = [
    """CREATE TABLE IF NOT EXISTS conhecimento_documentos (
        documento_id   VARCHAR(80) PRIMARY KEY,
        company_id     VARCHAR(20) REFERENCES companies(company_id) ON DELETE CASCADE,  -- NULL: vale para todas
        tipo           VARCHAR(20) NOT NULL CHECK (tipo IN ('politica', 'metodologia')),
        titulo         VARCHAR(300) NOT NULL,
        hash_conteudo  CHAR(64) NOT NULL,
        modelo_embeddings VARCHAR(200) NOT NULL,
        atualizado_em  TIMESTAMPTZ NOT NULL DEFAULT now())""",
    """CREATE TABLE IF NOT EXISTS conhecimento_trechos (
        documento_id VARCHAR(80) NOT NULL REFERENCES conhecimento_documentos(documento_id) ON DELETE CASCADE,
        ordem        INTEGER NOT NULL,
        secao        VARCHAR(300) NOT NULL,
        texto        TEXT NOT NULL,
        embedding    REAL[] NOT NULL,
        PRIMARY KEY (documento_id, ordem))""",
    "CREATE INDEX IF NOT EXISTS ix_conhecimento_documentos_empresa ON conhecimento_documentos(company_id)",
]


def criar_tabelas(con) -> None:
    for comando in DDL:
        con.execute(text(comando))


def dividir(texto: str, arquivo: str) -> list[dict]:
    """Um trecho por seção (## ou ###); o título do documento e o da seção vão junto, para dar contexto."""
    titulo_doc = next((l[2:].strip() for l in texto.splitlines() if l.startswith("# ")), arquivo)
    trechos, titulo, linhas = [], titulo_doc, []
    def fechar():
        corpo = "\n".join(linhas).strip()
        if len(corpo) > 40:
            trechos.append({"fonte": arquivo, "secao": titulo, "texto": f"{titulo_doc} — {titulo}\n{corpo}"})
    for linha in texto.splitlines():
        if re.match(r"^#{2,3} ", linha):
            fechar(); titulo, linhas = linha.lstrip("#").strip(), []
        elif not linha.startswith("# "):
            linhas.append(linha)
    fechar()
    return trechos


def titulo(texto: str, padrao: str) -> str:
    return next((l[2:].strip() for l in texto.splitlines() if l.startswith("# ")), padrao)


def carregar_trechos(con) -> list[dict]:
    linhas = con.execute(text("""
        SELECT d.documento_id, d.company_id, d.tipo, d.titulo, c.company_name, t.ordem, t.secao, t.texto, t.embedding
        FROM conhecimento_trechos t JOIN conhecimento_documentos d USING (documento_id)
        LEFT JOIN companies c ON c.company_id = d.company_id
        ORDER BY d.tipo, d.documento_id, t.ordem""")).mappings().all()
    return [dict(l) for l in linhas]
