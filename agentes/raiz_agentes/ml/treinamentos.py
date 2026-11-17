"""Recomendação de treinamentos.

A base não mostra efeito causal dos cursos sobre o uso (diferenças pequenas e de sinal trocado, ver
relatório), então a recomendação não promete "este curso melhora seu uso". Ela combina três sinais:
1. a regra do AI Literacy do AMS (AiLiteracyService): concluir cursos já iniciados (50%), boa nota (30%) e
   diversidade de categorias, até 4 (20%);
2. os pontos de atenção do perfil (ex.: uso fora da política → Governança);
3. filtro colaborativo simples: cursos com melhor conclusão e nota entre pessoas do mesmo perfil e cargo.

Para a comparação com os colegas de área (pergunta do colaborador), cursos_dos_colegas() conta os cursos que os
colegas com AMS maior concluíram e a pessoa ainda não fez. Só devolve contagens (nunca quem fez) e só cursos
concluídos por pelo menos MINIMO_COLEGAS pessoas do grupo, para ninguém ser identificado.
"""
import pandas as pd

CATEGORIAS_POR_ATENCAO = {
    "parcela_nao_aprovada": ["Governanca", "Seguranca e Compliance"],
    "eventos_seguranca": ["Seguranca e Compliance", "Governanca"],
    "taxa_falha": ["IA Generativa"],
    "custo_por_requisicao": ["IA Generativa", "Ferramentas Corporativas"],
    "dias_ativos": ["Produtividade", "IA Generativa"],
}
CATEGORIAS_POR_PERFIL = {
    "dev_intensivo": ["Ferramentas de Desenvolvimento"],
    "corporativo_custo_alto": ["Ferramentas Corporativas"],
    "fora_da_politica": ["Governanca", "Seguranca e Compliance"],
    "esporadico": ["IA Generativa", "Produtividade"],
    "sem_uso": ["IA Generativa"],
}


def carregar(con) -> pd.DataFrame:
    return pd.read_sql("""SELECT employee_id, course_name, course_category, enrollment_date, completion_date,
                                 completion_status, score FROM training_records""", con)


def recomendar(employee_id: str, perfil: str, codigos_atencao: list[str], cargo: str,
               trein: pd.DataFrame, perfis_pessoas: pd.Series, cargos: pd.Series, limite: int = 3) -> list[dict]:
    meus = trein[trein.employee_id == employee_id]
    catalogo = trein.drop_duplicates("course_name")[["course_name", "course_category"]]
    recs = []
    # 1) cursos já iniciados e não concluídos: concluí-los é o que mais pesa no AI Literacy
    for _, c in meus[meus.completion_status.isin(["Em Andamento", "Nao Iniciado"])].iterrows():
        recs.append({"curso": c.course_name, "categoria": c.course_category,
                     "motivo": "Você já está matriculado: concluir este curso aumenta direto a nota de AI Literacy."})
    feitos = set(meus.course_name)
    cats_feitas = set(meus.loc[meus.completion_status == "Concluido", "course_category"])
    # 2) categorias ligadas aos pontos de atenção e ao perfil
    prioridade = [c for a in codigos_atencao for c in CATEGORIAS_POR_ATENCAO.get(a, [])] + CATEGORIAS_POR_PERFIL.get(perfil, [])
    # 3) colaborativo: conclusão e nota entre pessoas parecidas (mesmo perfil ou mesmo cargo)
    parecidos = set(perfis_pessoas[perfis_pessoas == perfil].index) | set(cargos[cargos == cargo].index)
    tp = trein[trein.employee_id.isin(parecidos - {employee_id})]
    est = (tp.groupby("course_name")
             .agg(conclusao=("completion_status", lambda s: (s == "Concluido").mean()), nota=("score", "mean"), n=("employee_id", "size"))
             .reindex(catalogo.course_name).fillna({"conclusao": 0, "n": 0}))
    cand = catalogo[~catalogo.course_name.isin(feitos)].copy()
    cand["prio"] = cand.course_category.map(lambda c: len(prioridade) - prioridade.index(c) if c in prioridade else 0)
    cand["nova_categoria"] = ~cand.course_category.isin(cats_feitas)
    cand = cand.join(est, on="course_name")
    cand["pontos"] = cand.prio * 2 + cand.nova_categoria * 1 + cand.conclusao + cand.nota.fillna(70) / 100
    for _, c in cand.sort_values("pontos", ascending=False).iterrows():
        if len(recs) >= limite:
            break
        motivos = []
        if c.prio > 0:
            motivos.append("trabalha um ponto de atenção do seu uso" if any(c.course_category in CATEGORIAS_POR_ATENCAO.get(a, []) for a in codigos_atencao) else "combina com o seu perfil de uso")
        if c.nova_categoria:
            motivos.append("é uma categoria nova para você (diversidade conta no AI Literacy)")
        if c.n >= 5:
            motivos.append(f"{c.conclusao:.0%} das pessoas parecidas com você concluíram")
        recs.append({"curso": c.course_name, "categoria": c.course_category, "motivo": "Este curso " + ", ".join(motivos) + "." if motivos else "Curso disponível no catálogo."})
    return recs[:limite]


MINIMO_COLEGAS = 2


def cursos_dos_colegas(employee_id: str, referencia: list[str], trein: pd.DataFrame, limite: int = 4) -> dict:
    """Cursos concluídos pelos colegas de referência (AMS maior) que a pessoa ainda não concluiu, com quantos colegas
    concluíram e a nota média; e as categorias em que esse grupo concluiu mais cursos por pessoa que ela."""
    ref = set(referencia) - {employee_id}
    if len(ref) < 3:
        return {"colegas": len(ref), "cursos": [], "categorias": []}
    meus = trein[trein.employee_id == employee_id]
    concluidos_meus = set(meus.loc[meus.completion_status == "Concluido", "course_name"])
    feitos = trein[trein.employee_id.isin(ref) & (trein.completion_status == "Concluido")]
    est = (feitos.groupby(["course_name", "course_category"], as_index=False)
                 .agg(pessoas=("employee_id", "nunique"), nota=("score", "mean")))
    est = est[(est.pessoas >= MINIMO_COLEGAS) & ~est.course_name.isin(concluidos_meus)]
    est = est.sort_values(["pessoas", "nota"], ascending=[False, False]).head(limite)
    cursos = [{"curso": r.course_name, "categoria": r.course_category, "colegas": int(r.pessoas),
               "nota_media": None if pd.isna(r.nota) else round(float(r.nota), 1)} for r in est.itertuples()]
    # categorias: cursos concluídos por pessoa no grupo x da própria pessoa
    por_pessoa = feitos.groupby("course_category").size() / len(ref)
    minhas = meus[meus.completion_status == "Concluido"].groupby("course_category").size()
    cats = [{"categoria": c, "media_colegas": round(float(v), 1), "voce": int(minhas.get(c, 0))}
            for c, v in por_pessoa.sort_values(ascending=False).items() if v - minhas.get(c, 0) >= 0.5][:3]
    return {"colegas": len(ref), "cursos": cursos, "categorias": cats}
