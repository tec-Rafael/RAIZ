"""Features de uso de IA por funcionário e período, lidas direto do Postgres (somente leitura)."""
import pandas as pd

from .periodos import TOTAL, periodo

FALHAS = ("error", "timeout")


def _periodos_sql() -> str:
    linhas = [f"({n}, DATE '{i}', DATE '{f}')" for n in range(1, TOTAL + 1) for i, f in [periodo(n)]]
    return "(VALUES " + ", ".join(linhas) + ") AS p(periodo, inicio, fim)"


def carregar(con) -> pd.DataFrame:
    """Uma linha por funcionário × período (P1..P5), inclusive quem não usou IA no período."""
    per = _periodos_sql()
    uso = pd.read_sql(f"""
        SELECT u.employee_id, p.periodo,
               SUM(u.request_count)                                         AS requisicoes,
               COUNT(*)                                                     AS chamadas,
               COUNT(DISTINCT u.timestamp::date)                            AS dias_ativos,
               SUM(u.input_tokens)                                          AS tokens_entrada,
               SUM(u.output_tokens)                                         AS tokens_saida,
               AVG(u.latency_ms)                                            AS latencia_media,
               AVG((u.request_status IN {FALHAS})::int)                     AS taxa_falha,
               AVG((u.request_status = 'rate_limited')::int)                AS taxa_limite,
               COUNT(DISTINCT u.ai_tool_id)                                 AS ferramentas,
               COUNT(DISTINCT u.application)                                AS aplicacoes,
               AVG((u.ai_tool_id IN ('T05','T06','T07'))::int)              AS parcela_dev,
               SUM(u.input_tokens / 1000.0 * m.input_price_usd_per_1k_tokens
                 + u.output_tokens / 1000.0 * m.output_price_usd_per_1k_tokens) AS custo_usd,
               AVG((a.ai_tool_id IS NULL)::int)                             AS parcela_nao_aprovada
        FROM ai_usage_logs u
        JOIN {per} ON u.timestamp::date BETWEEN p.inicio AND p.fim
        LEFT JOIN ai_models m ON m.model_id = u.model_id
        LEFT JOIN approved_ai_tools a ON a.company_id = u.company_id AND a.ai_tool_id = u.ai_tool_id
                                     AND a.approval_status = 'Aprovado'
        GROUP BY u.employee_id, p.periodo
    """, con)
    seg = pd.read_sql(f"""
        SELECT s.employee_id, p.periodo, COUNT(*) AS eventos_seguranca,
               SUM((s.severity IN ('Alta','Critica'))::int) AS eventos_graves
        FROM security_events s JOIN {per} ON s.timestamp::date BETWEEN p.inicio AND p.fim
        GROUP BY 1, 2
    """, con)
    trein = pd.read_sql(f"""
        SELECT t.employee_id, p.periodo, COUNT(*) AS cursos_concluidos, AVG(t.score) AS nota_cursos
        FROM training_records t JOIN {per} ON t.completion_date BETWEEN p.inicio AND p.fim
        WHERE t.completion_status = 'Concluido'
        GROUP BY 1, 2
    """, con)
    func = pd.read_sql("""
        SELECT e.employee_id, e.company_id, e.department_id, d.department_name AS departamento,
               e.job_role AS cargo, e.seniority AS senioridade, e.posicao
        FROM employees e LEFT JOIN departments d ON d.department_id = e.department_id
    """, con)

    base = func.merge(pd.DataFrame({"periodo": range(1, TOTAL + 1)}), how="cross")
    df = (base.merge(uso, on=["employee_id", "periodo"], how="left")
              .merge(seg, on=["employee_id", "periodo"], how="left")
              .merge(trein, on=["employee_id", "periodo"], how="left"))
    zeros = ["requisicoes", "chamadas", "dias_ativos", "tokens_entrada", "tokens_saida", "ferramentas",
             "aplicacoes", "custo_usd", "eventos_seguranca", "eventos_graves", "cursos_concluidos"]
    df[zeros] = df[zeros].fillna(0)
    df["usou_ia"] = df["chamadas"] > 0
    df["tokens_por_requisicao"] = (df["tokens_entrada"] + df["tokens_saida"]) / df["requisicoes"].where(df["requisicoes"] > 0)
    df["custo_por_requisicao"] = df["custo_usd"] / df["requisicoes"].where(df["requisicoes"] > 0)
    return df


def ferramentas(con) -> pd.DataFrame:
    """Requisições e custo por funcionário × período × ferramenta (para dizer onde a pessoa usa e gasta mais)."""
    per = _periodos_sql()
    return pd.read_sql(f"""
        SELECT u.employee_id, p.periodo, t.tool_name AS ferramenta, SUM(u.request_count) AS requisicoes,
               SUM(u.input_tokens / 1000.0 * m.input_price_usd_per_1k_tokens
                 + u.output_tokens / 1000.0 * m.output_price_usd_per_1k_tokens) AS custo_usd,
               BOOL_OR(a.ai_tool_id IS NOT NULL) AS aprovada
        FROM ai_usage_logs u
        JOIN {per} ON u.timestamp::date BETWEEN p.inicio AND p.fim
        JOIN ai_tools t ON t.ai_tool_id = u.ai_tool_id
        LEFT JOIN ai_models m ON m.model_id = u.model_id
        LEFT JOIN approved_ai_tools a ON a.company_id = u.company_id AND a.ai_tool_id = u.ai_tool_id
                                     AND a.approval_status = 'Aprovado'
        GROUP BY 1, 2, 3
    """, con)
