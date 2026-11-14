"""Gera as políticas de uso de IA (fictícias, para o case) de cada empresa da base, a partir dos dados reais
de ferramentas aprovadas/em avaliação/bloqueadas, orçamento e setor."""
import psycopg2, collections, datetime
con = psycopg2.connect(host="127.0.0.1", port=5433, user="root", password="root", dbname="ai_usage"); cur = con.cursor()
cur.execute("select company_id, company_name, industry, company_size, employee_count from companies order by 1"); empresas = cur.fetchall()
cur.execute("select a.company_id, t.tool_name, t.provider, a.approval_status, a.approval_date, a.allowed_departments from approved_ai_tools a join ai_tools t using(ai_tool_id) order by 1, 2"); ferr = collections.defaultdict(list)
for r in cur.fetchall(): ferr[r[0]].append(r[1:])
cur.execute("select company_id, round(avg(budget_amount)) from ai_budgets group by 1"); orc = dict(cur.fetchall())
cur.execute("select tool_name from ai_tools order by 1"); todas = [r[0] for r in cur.fetchall()]
SETOR = {
 "Tecnologia": "Código-fonte, segredos de infraestrutura (chaves, senhas, tokens) e dados de clientes nunca devem ser colados em prompts. Assistentes de código podem ser usados, mas todo código gerado passa por revisão em pull request antes de ir para produção.",
 "Financeiro": "Dados de clientes, números de conta, CPF, saldos e informações sob sigilo bancário são proibidos em prompts. Análises que apoiem decisões de crédito ou investimento feitas com IA precisam de revisão e assinatura de um analista responsável.",
 "Industria": "Fórmulas, desenhos técnicos, parâmetros de processo e dados de fornecedores são confidenciais e não podem ser enviados a ferramentas de IA. A IA pode apoiar relatórios e manutenção, mas não substitui procedimentos de segurança do trabalho.",
 "Varejo": "Dados de clientes (nome, CPF, endereço, histórico de compras) e condições comerciais com fornecedores não podem ser enviados em prompts. Textos de campanhas e descrições de produtos gerados por IA devem ser revisados antes de publicados.",
 "Servicos": "Informações de clientes atendidos, contratos e propostas comerciais são confidenciais. Entregáveis produzidos com apoio de IA devem ser revisados por um responsável antes do envio ao cliente.",
 "Marketing": "Dados pessoais de clientes e de leads, briefings sob confidencialidade e materiais não lançados não podem ser enviados em prompts. Peças criadas com IA devem ser revisadas quanto a direitos autorais, marcas e veracidade antes da publicação.",
}
def setor(ind):
    for k, v in SETOR.items():
        if ind.lower().startswith(k.lower()[:6]): return v
    return SETOR["Servicos"]
def data(d): return d.strftime("%d/%m/%Y") if d else "data não informada"
def deps(s): return ", ".join(x.strip() for x in s.replace(";", ",").split(",") if x.strip()) if s else "todos os departamentos"
for cid, nome, ind, porte, func in empresas:
    lst = ferr[cid]; apr = [f for f in lst if f[2] == "Aprovado"]; ava = [f for f in lst if f[2] == "Em Avaliacao"]; blq = [f for f in lst if f[2] == "Bloqueado"]
    citadas = {f[0] for f in lst}; fora = [t for t in todas if t not in citadas]
    L = [f"# Política de Uso de Inteligência Artificial — {nome}", "",
         f"> Documento fictício criado para o case do rAIz a partir dos dados da base. Empresa {cid}, setor {ind}, porte {porte.lower()}, {func} colaboradores.", "",
         "## Objetivo e abrangência",
         f"Esta política define como os colaboradores da empresa {nome} podem usar ferramentas de Inteligência Artificial no trabalho. Ela vale para todos os colaboradores, gestores e terceiros com acesso aos sistemas da empresa, em qualquer dispositivo usado para atividades de trabalho.", "",
         "## Ferramentas aprovadas",
         "Só as ferramentas abaixo estão aprovadas, e apenas para os departamentos indicados:"]
    L += [f"- **{f[0]}** ({f[1]}): aprovada desde {data(f[3])}, para {deps(f[4])}." for f in apr] or ["- Nenhuma ferramenta aprovada no momento."]
    L += ["", "Usar uma ferramenta aprovada fora dos departamentos indicados também é considerado uso fora da política.", ""]
    L += ["## Ferramentas em avaliação e bloqueadas"]
    if ava: L += ["Em avaliação (ainda **não** liberadas para uso com dados da empresa): " + ", ".join(f[0] for f in ava) + "."]
    if blq: L += ["Bloqueadas (uso **proibido**): " + ", ".join(f[0] for f in blq) + "."]
    L += [f"Qualquer outra ferramenta de IA não listada como aprovada{(' (por exemplo, ' + ', '.join(fora[:4]) + ')') if fora else ''} é considerada Shadow AI e não pode ser usada. Pedidos de novas ferramentas devem ser enviados ao Comitê de IA.", ""]
    L += ["## Dados confidenciais e LGPD",
          "É proibido inserir em ferramentas de IA dados pessoais de colaboradores ou clientes (como CPF, endereço, telefone e dados de saúde), dados financeiros não públicos e documentos marcados como confidenciais, conforme a Lei Geral de Proteção de Dados (LGPD).",
          setor(ind),
          "Uploads de arquivos para ferramentas de IA são monitorados; arquivos com dados sensíveis são bloqueados automaticamente (DLP).", ""]
    L += ["## Uso responsável e revisão humana",
          "Respostas de IA podem conter erros. Todo conteúdo gerado por IA deve ser revisado por uma pessoa antes de ser usado em decisões, enviado a clientes ou publicado. O colaborador continua responsável pelo resultado do seu trabalho.", ""]
    L += ["## Custos e uso eficiente (FinOps)",
          f"O orçamento médio mensal de IA da empresa é de US$ {int(orc.get(cid, 0)):,}".replace(",", ".") + ", acompanhado por centro de custo.",
          "Para tarefas simples (resumos, revisões curtas, reformulações), use os modelos mais leves disponíveis na ferramenta aprovada. Reserve os modelos mais caros para tarefas complexas e evite repetir a mesma requisição sem necessidade.", ""]
    L += ["## Capacitação obrigatória",
          "Todo colaborador que usa IA deve concluir, no primeiro período de uso, os cursos **Ética e Governança de Dados em IA** e **Segurança da Informação e Uso de IA**. Colaboradores das áreas técnicas devem concluir também **Copilot para Desenvolvedores** ou **Prompt Engineering na Prática**. Os cursos concluídos contam para a nota de AI Literacy do AMS.", ""]
    L += ["## Incidentes e consequências",
          "Eventos de segurança são classificados como baixa, média, alta ou crítica gravidade e reduzem a nota de Segurança do AMS. Incidentes de gravidade alta ou crítica (como vazamento de dados sensíveis ou uso repetido de ferramenta bloqueada) são analisados pelo Comitê de IA e podem levar ao bloqueio do acesso às ferramentas e a medidas disciplinares.", ""]
    L += ["## Contato",
          f"Dúvidas sobre esta política e pedidos de novas ferramentas: Comitê de IA ({nome}), pelo canal interno de atendimento de TI.", ""]
    open(f"/home/claude/proj/RAIZ/agentes/politicas/{cid}.md", "w", encoding="utf-8").write("\n".join(L))
print(len(empresas), "políticas")
