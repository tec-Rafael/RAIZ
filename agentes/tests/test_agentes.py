"""Testes sem banco e sem LLM: prompt, divisão em trechos, busca vetorial (embeddings "hash") e token."""
import os

os.environ.setdefault("AGENTES_TOKEN", "teste")
os.environ.setdefault("EMBEDDINGS_PROVEDOR", "hash")

from fastapi.testclient import TestClient

from raiz_agentes import embeddings
from raiz_agentes.conhecimento import app as conhecimento
from raiz_agentes.conhecimento.base import dividir
from raiz_agentes.orquestrador import prompt
from raiz_agentes.orquestrador.app import Pedido, app as orquestrador

TOKEN = {"X-Agentes-Token": "teste"}


def pedido(**extra):
    base = {"pergunta": "Como melhorar?", "usuario": {"nome": "Ana", "perfil": "colaborador"}, "periodo": 5,
            "escopo": {"tipo": "pessoal", "descricao": "seus dados", "proprio": True, "employeeIds": ["C01-E0001"], "empresas": ["C01"]},
            "ams": {"nota": 61.4, "notaAnterior": 64.9, "dimensoes": [{"nome": "Governança", "nota": 20.0},
                                                                      {"nome": "AI Literacy", "nota": 0, "ausente": True}]}}
    base.update(extra)
    return Pedido(**base)


def _base_teste():
    """Monta o índice do agente de conhecimento em memória, como se viesse do banco."""
    linhas = []
    for doc, empresa, nome, tipo, texto in [
        ("metodologia:ams", None, None, "metodologia", "# AMS\n\n## Governança\nPorcentagem de requisições em ferramentas aprovadas.\n\n## FinOps\nCompara o custo com a mediana do período."),
        ("politica:C01", "C01", "NexoCode", "politica", "# Política NexoCode\n\n## Ferramentas aprovadas\nGitHub Copilot aprovado para Engenharia.\n\n## Dados confidenciais\nProibido colar CPF de clientes em prompts."),
        ("politica:C06", "C06", "Banco Alterosa", "politica", "# Política Banco\n\n## Dados confidenciais\nSigilo bancário: proibido enviar saldos e CPF.")]:
        for i, t in enumerate(dividir(texto, doc)):
            linhas.append({"documento_id": doc, "company_id": empresa, "company_name": nome, "tipo": tipo, "titulo": doc,
                           "ordem": i, "secao": t["secao"], "texto": t["texto"], "embedding": embeddings.gerar([t["texto"]])[0]})
    conhecimento._montar(linhas)
    conhecimento._indice["quando"] = 1e18          # não tenta recarregar do banco durante o teste


def test_numeros_e_niveis_em_portugues():
    assert prompt.br(1234.5) == "1.234,50"
    assert prompt.br(-3.5, 2, sinal=True) == "-3,50"
    assert [prompt.nivel(n) for n in (90, 70, 60, 59.9)] == ["Excelente", "Alto", "Médio", "Baixo"]


def test_divide_documento_por_secoes():
    trechos = dividir("# Doc\n\n## Segurança\nComeça em 100 e perde pontos a cada evento.\n\n### FinOps\nCompara o custo com a mediana do período.", "doc.md")
    assert [t["secao"] for t in trechos] == ["Segurança", "FinOps"]
    assert trechos[0]["texto"].startswith("Doc — Segurança")


def test_busca_vetorial_filtra_politicas_pela_empresa():
    _base_teste()
    c = TestClient(conhecimento.app)
    r = c.post("/buscar", json={"pergunta": "posso colar CPF de cliente?", "empresas": ["C01"], "tipo": "politica"}, headers=TOKEN).json()
    assert r["trechos"] and all(t["empresa"] == "NexoCode" for t in r["trechos"])        # nunca a política de outra empresa
    assert r["trechos"][0]["secao"] == "Dados confidenciais"
    assert c.post("/buscar", json={"pergunta": "CPF", "empresas": [], "tipo": "politica"}, headers=TOKEN).json()["trechos"] == []


def test_metodologia_completa_e_politicas_no_prompt():
    _base_teste()
    c = TestClient(conhecimento.app)
    met = c.post("/todos", json={"tipo": "metodologia"}, headers=TOKEN).json()
    pol = c.post("/buscar", json={"pergunta": "CPF", "empresas": ["C01"], "tipo": "politica"}, headers=TOKEN).json()
    sistema = prompt.montar(pedido(), None, None, {"metodologia": met, "politicas": pol})[0]["content"]
    assert "Metodologia do rAIz (documento completo)" in sistema and "mediana do período" in sistema
    assert "[NexoCode] Dados confidenciais" in sistema and "Sigilo bancário" not in sistema
    assert "61,40 de 100 (nível Médio)" in sistema and "Dimensão mais fraca: Governança" in sistema
    assert "Uso de IA: dados indisponíveis" in sistema


def test_agentes_exigem_token():
    assert TestClient(orquestrador).post("/perguntar", json=pedido().model_dump(by_alias=True)).status_code == 401
    assert TestClient(conhecimento.app).post("/todos", json={}).status_code == 401
