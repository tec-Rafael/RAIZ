# Dashboard RAIZ: proposta de integração

Status: proposta aprovada na conversa e implementada. Este arquivo preserva o diagnóstico inicial; o contrato entregue e a validação estão em [dashboard.md](dashboard.md). Equipe foi confirmada como departamento; períodos começam no primeiro log e os perfis permanecem demonstrativos.

## Escopo solicitado

Manter EJS, Express, Chart.js e a identidade visual da página existente. Oferecer três visões: colaborador (própria evolução), gestor (própria evolução, equipe e integrantes) e administrador (própria evolução, funcionários e áreas da empresa). Esta solicitação prevalece sobre a divisão em quatro perfis do documento de visão.

## Constatações no código

- `frontend/routes/api/dashboardApi.js` usa mocks por padrão. Desativar o mock encaminha chamadas a endpoints Java `/api/dashboard/*` que não estão implementados. A validação ainda depende de identidades simuladas.
- `/api/funcionario/listar` e `/buscar/{id}` entregam empresa, departamento e posição. Não entregam associação gestor-equipe. O enum `Posicao` contém somente COLABORADOR e GESTOR.
- `/api/utilizacao-ia/listar` pagina os registros em lotes de até 100, sem filtros por funcionário, departamento ou data. Ler todas as páginas para cada interação seria inadequado para o dashboard.
- `/api/utilizacao-ia/destaques` calcula rankings no banco, mas não recebe período, departamento ou funcionário. Seu resultado não deve ser apresentado como se obedecesse aos filtros da página.
- `/score/ai-literacy/{employeeId}`, `/score/governance/{employeeId}` e `/score/security-incidents/{employeeId}` já recebem `inicio` e `fim`.
- `AiMaturityScoreService.calcularAMS` está vazio. O documento de visão, página 22, também deixa a fórmula definitiva de AMS e as regras de AI Credits em aberto.
- O SQL contém `issues`, `pull_requests`, `builds` e `deployments`; as duas primeiras possuem vínculo com funcionário. Não há controllers para essas fontes de produtividade.
- O login atual não estabelece uma sessão autenticada. O seletor de perfil existente é uma simulação, não um controle de autorização.

As constatações referem-se ao código e ao SQL versionados; o conteúdo do banco em execução ainda não foi validado.

## Alterações propostas, ainda não executadas

1. Implementar consultas agregadas no Spring Boot, reutilizando as tabelas existentes e os cálculos de score. O Express continuará como intermediário. Não carregar logs completos no navegador.
2. Disponibilizar os contratos abaixo, com datas, escopo e cobertura na resposta. Validar empresa, funcionário, área e intervalo no servidor.
3. Para uma primeira versão sem migração, usar departamento como unidade da equipe SOMENTE se essa equivalência for confirmada. Se houver várias equipes por departamento, modelar equipes e associações reais, sem inferir vínculos a partir dos cargos.
4. Manter o seletor solicitado explicitamente como modo de demonstração com dados do banco, se esse for o objetivo atual. Para acesso real por perfil, implementar sessão e autorização no servidor, além do papel ADMINISTRADOR, em uma mudança aprovada separadamente.

### Contrato proposto

| Rota Java | Resultado |
| --- | --- |
| `GET /api/dashboard/filtros` | Funcionário de referência, empresa, áreas/pessoas disponíveis, períodos e data de cobertura |
| `GET /api/dashboard/resumo` | KPIs do escopo e período, comparação anterior, cobertura e métricas indisponíveis |
| `GET /api/dashboard/evolucao` | Os cinco períodos, durações, valores totais e taxas diárias |
| `GET /api/dashboard/distribuicao` | Uso por ferramenta e modelo, respeitando o mesmo período e escopo |
| `GET /api/dashboard/integrantes` | Tabela paginada por funcionário ou área, com busca e ordenação |

Parâmetros de consulta: `periodoId`, `escopo=pessoal|equipe|area|empresa`, `employeeId`, `departmentId`, `aiToolId`; paginação e ordenação apenas onde aplicáveis. O servidor resolve limites de datas a partir do período configurado. A empresa deve vir do contexto validado, não ser aceita irrestritamente do navegador. No modo demonstrativo, documentar explicitamente a ausência de autorização real.

O filtro de ferramenta afeta métricas de uso de IA. Métricas de entregas e capacitação permanecem relativas à pessoa/área/período, com legenda explícita, pois não há atribuição direta de entregas ou cursos à ferramenta.

## KPIs e critérios de cálculo

| Indicador | Fonte e regra |
| --- | --- |
| Issues concluídas | Contagem por `closed_at` dentro do período e vínculo `employee_id`, validando os status reais |
| PRs integrados | Contagem por `merged_at` dentro do período e vínculo `employee_id` |
| Tempo de ciclo | Mediana entre criação e fechamento/merge das entregas concluídas no período; excluir datas ausentes ou invertidas e informar cobertura |
| Entregas por dia | Issues e PRs em séries separadas, cada contagem dividida pelos dias observados do período |
| Requisições | Soma de `request_count`, não contagem das linhas de log |
| Tokens | Soma de entrada e saída, apresentada como consumo |
| Sucesso e latência | Indicadores técnicos de uso; explicitar denominador e cobertura, validando a semântica de cada registro antes de ponderar por requisições |
| AI Literacy, governança e segurança | Reutilizar os serviços existentes; ausência de dados continua ausente, sem converter `null` em zero |

Não criar nota global de produtividade, AMS ou AI Credits sem uma metodologia definida. Não atribuir ganhos de produtividade causalmente à IA com base em correlação. Entregas de engenharia só se aplicam aos cargos e fontes que tenham esse tipo de atividade.

Nos agregados, calcular taxas a partir dos numeradores e denominadores combinados. Se houver média de scores individuais, identificá-la como média e informar quantas pessoas têm dados válidos; não chamá-la de score organizacional definitivo.

## Períodos

Interpretar a solicitação como P1, P2, P3 e P4 completos (90 dias cada), mais P5 atual com 66 dias observados: 426 dias no total. Confirmar a data inicial antes de fixar os limites. Usar intervalos contíguos, sem sobreposição; em consultas de timestamp, início inclusivo e fim exclusivo no dia seguinte.

Mostrar totais e taxas por dia. Comparar P5 com períodos completos usando taxas diárias ou uma janela equivalente de 66 dias explicitamente selecionada. Não apresentar extrapolação para 90 dias como resultado observado. Informar que taxas por dia corrido não corrigem diferenças de calendário ou jornada.

## Página proposta

- Cabeçalho com perfil, pessoa de referência e indicação da fonte/data de cobertura.
- Filtros encadeados: visão, área/equipe, funcionário, período e ferramenta. Alterar o perfil redefine filtros incompatíveis.
- Cartões de entregas, tempo de ciclo e uso de IA, com variação e unidade explícitas.
- Gráfico de linhas para evolução entre P1 e P5, alternando indicador e total/taxa diária.
- Barras horizontais para distribuição por ferramenta; barras comparativas por área na visão administrativa.
- Indicadores de capacitação, governança e segurança separados do consumo e das entregas.
- Tabela pesquisável e paginada; selecionar uma pessoa abre sua análise e mantém o período.
- Responsividade, rótulos acessíveis, alternativa tabular aos gráficos, carregamento, vazio, falha e botão de tentar novamente.
- Cancelamento de requisições antigas para evitar resultados de filtros anteriores sobrescrevendo a seleção atual.

## Validação após aprovação

Testar limites dos cinco períodos, período parcial, divisão por zero, campos ausentes, escopos de empresa e área, filtros combinados, contagens de entregas e agregação de requisições. Comparar resultados da API com consultas somente de leitura no banco configurado. Verificar página em desktop e celular, mudanças rápidas de filtros e indisponibilidade do backend. Não recriar ou reimportar o banco existente.

## Decisões necessárias

1. Aprovar endpoints agregados de dashboard, incluindo consultas de entregas nas tabelas existentes.
2. Confirmar se equipe equivale a departamento ou fornecer a regra de associação.
3. Informar a data inicial de P1 e confirmar quatro períodos completos mais um quinto parcial.
4. Confirmar se a troca de perfil continuará como demonstração ou se deve incluir autenticação e autorização reais nesta implementação.
