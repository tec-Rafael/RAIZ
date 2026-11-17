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


# ------------------------------------------------------------ privacidade por modo e comparação anônima
def test_colaborador_recebe_so_comparacao_anonima():
    ped = pedido(modo="colaborador", equipe={"colegas": 6, "ams": {"nota": 70.0, "validos": 6, "dimensoes": [{"nome": "Governança", "nota": 55.0}]},
                                              "referencia": ["C01-E0002", "C01-E0003", "C01-E0004"], "colegasIds": ["C01-E0002", "C01-E0003", "C01-E0004", "C01-E0005"]})
    ref_dados = {"atual": {"pessoas": 3, "pessoas_com_uso": 3, "dias_ativos_media": 40, "taxa_falha": 0.02, "parcela_ferramentas_nao_aprovadas": 0.0,
                           "custo_por_requisicao_usd": 0.012, "cursos_concluidos": 6}}
    cursos = {"colegas": 3, "cursos": [{"curso": "Governança de IA na prática", "categoria": "Governanca", "colegas": 3, "nota_media": 88.0}], "categorias": []}
    sistema = prompt.montar(ped, None, None, {}, referencia_dados=ref_dados, referencia_cursos=cursos)[0]["content"]
    assert "COMPARAÇÃO ANÔNIMA COM OS COLEGAS DE ÁREA (6 colegas" in sistema
    assert "8,6 pontos abaixo da média" in sistema and "Governança 55,0 (você: -35,0)" in sistema
    assert "Governança de IA na prática (Governanca), concluído por 3 colegas" in sistema
    assert "Boas práticas dos 3 colegas com AMS maior" in sistema and "2,0 cursos concluídos por pessoa" in sistema
    assert "C01-E000" not in sistema                     # nenhum id de colega chega à LLM
    assert "Nunca cite nome, nota, posição" in sistema


def test_area_pequena_nao_tem_comparacao():
    sistema = prompt.montar(pedido(modo="colaborador", equipe={"colegas": 2, "ams": {"nota": 80.0}}), None, None, {})[0]["content"]
    assert "indisponível (a área tem poucas pessoas" in sistema and "80,00" not in sistema


def test_gestor_na_equipe_tem_os_proprios_dados_e_e_orientado_a_abrir_o_dashboard():
    ped = pedido(modo="gestor_equipe", usuario={"nome": "Bruno", "perfil": "gestor"},
                 escopo={"tipo": "grupo", "descricao": "sua equipe (Engenharia), 8 pessoas", "employeeIds": ["G1", "C2", "C3"], "empresas": ["C01"]},
                 pessoal={"employeeIds": ["G1"], "ams": {"nota": 82.0, "dimensoes": []}})
    sistema = prompt.montar(ped, None, None, {})[0]["content"]
    assert "SEUS DADOS (Bruno, além da equipe)" in sistema and "82,00 de 100" in sistema
    assert "filtro Colaborador para abrir o dashboard dela" in sistema
    assert "COMPARAÇÃO ANÔNIMA" not in sistema


def test_gestor_no_dashboard_do_colaborador_fala_da_pessoa():
    ped = pedido(modo="gestor_colaborador", usuario={"nome": "Bruno", "perfil": "gestor"},
                 escopo={"tipo": "pessoal", "descricao": "dados de Carla Souza", "nome": "Carla Souza", "proprio": False, "employeeIds": ["C3"]})
    sistema = prompt.montar(ped, None, None, {})[0]["content"]
    assert "responde sobre os dados de Carla Souza" in sistema and "chamando a pessoa pelo nome" in sistema


def test_cursos_dos_colegas_so_conta_e_exige_minimo():
    import pandas as pd
    from raiz_agentes.ml import treinamentos
    t = pd.DataFrame([
        {"employee_id": e, "course_name": c, "course_category": cat, "completion_status": s, "score": 80}
        for e, c, cat, s in [("EU", "A", "IA", "Concluido"),
                             ("R1", "A", "IA", "Concluido"), ("R2", "A", "IA", "Concluido"),
                             ("R1", "B", "Gov", "Concluido"), ("R2", "B", "Gov", "Concluido"), ("R3", "B", "Gov", "Concluido"),
                             ("R3", "C", "Seg", "Concluido")]])
    r = treinamentos.cursos_dos_colegas("EU", ["R1", "R2", "R3"], t)
    assert [c["curso"] for c in r["cursos"]] == ["B"]            # A já foi concluído; C só por 1 colega
    assert r["cursos"][0]["colegas"] == 3 and "R1" not in str(r)
    assert treinamentos.cursos_dos_colegas("EU", ["R1", "R2"], t)["cursos"] == []   # grupo pequeno demais
