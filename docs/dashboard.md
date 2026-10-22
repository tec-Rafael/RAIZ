# Dashboard com dados do banco

Implementação em Spring Boot (controller, service, repository e DTOs), com consultas agregadas no PostgreSQL e proxy Express. A página EJS usa Chart.js e preserva a paleta verde/creme do projeto. Não há fallback para mocks nem migração de banco.

## Executar

Inicie o PostgreSQL existente com `docker compose up -d postgres` e a API com `mvn spring-boot:run`. Em `frontend/.env`, configure `url_springboot=http://localhost:8080`. Execute `npm start` na pasta `frontend` e abra http://localhost:3000/dashboard.

O seletor de empresa/pessoa define a identidade de referência; o seletor de visão permite demonstrar os três perfis sobre dados do banco. Não autentica nem representa uma barreira de segurança. A lista de pessoas pertence ao modo demonstrativo. O banco fornecido pelo projeto tem origem sintética, mesmo sendo consultado diretamente pela aplicação.

## Endpoints

| Método e caminho | Resposta |
| --- | --- |
| `GET /api/dashboard/usuarios` | Pessoas de referência, empresa, departamento e posição; sem email ou senha |
| `GET /api/dashboard/filtros?perfil=gestor&usuarioId=C01-E0001` | Pessoa de referência, áreas, integrantes, ferramentas e períodos |
| `GET /api/dashboard/resumo?perfil=gestor&usuarioId=C01-E0001&escopo=equipe&periodo=5` | KPIs do período, anterior, evolução dos cinco períodos, distribuição por ferramenta e scores individuais quando aplicável |
| `GET /api/dashboard/integrantes?perfil=administrador&usuarioId=C01-E0001&escopo=empresa&periodo=5&agrupamento=area` | Tabela agregada, paginada e pesquisável |

As quatro rotas estão disponíveis no Java e no proxy Express. O Swagger descreve os parâmetros. A evolução e a distribuição são retornadas junto com o resumo para reduzir chamadas da página.

Parâmetros comuns: `perfil=colaborador|gestor|administrador`, `usuarioId`, `escopo=pessoal|equipe|area|empresa`, `employeeId` (pessoal; padrão: própria pessoa), `departmentId` (área), `aiToolId` (opcional), `periodo=1..5` (padrão: 5).

Tabela: `agrupamento=pessoa|area`, `busca` (até 150 caracteres), `ordem=nome|requisicoes|issues|prs`, `page` (zero-based), `size=1..100` (padrão: 10). Retorno: `content`, `totalElements`, `page`, `size`. Ordenação e trechos SQL são restritos a opções conhecidas; valores de filtros são parâmetros vinculados.

Colaborador consulta a si próprio. Gestor consulta a si, integrantes do seu departamento e o departamento agregado. Administrador consulta a própria pessoa, pessoas e departamentos da sua empresa ou o agregado empresarial. Como acordado, equipe equivale a departamento. O vínculo considerado é o cadastro atual, pois não há histórico de associação departamental no modelo.

## Períodos e indicadores

A âncora é `MIN(ai_usage_logs.timestamp)` do banco inteiro. Os períodos são contíguos: quatro de 90 dias e um quinto de 66 dias. Não dependem do relógio do computador. Datas finais são inclusivas na resposta; SQL usa início inclusivo e dia seguinte ao fim exclusivo.

Na base validada: P1 01/06/2025–29/08/2025; P2 30/08/2025–27/11/2025; P3 28/11/2025–25/02/2026; P4 26/02/2026–26/05/2026; P5 27/05/2026–31/07/2026. Novos logs além dessa janela não ampliam automaticamente os cinco períodos definidos para esta demonstração.

- Requisições: soma de `request_count`. Tokens: entrada + saída, com agregação em bigint.
- Issues concluídas: registros com `closed_at` no período. PRs integrados: `merged_at` no período. Vínculos com empresa e funcionário são validados na consulta. Estas são métricas de engenharia, não uma nota universal de produtividade.
- Ciclos: mediana em horas entre criação e fechamento/merge; datas ausentes ou invertidas ficam fora do cálculo, com contagem de cobertura na resposta.
- Sucesso: percentual de linhas com `request_status=success` entre linhas com status informado. Não é uma taxa ponderada por requisições. Latência: média dos registros com valor disponível, acompanhada da cobertura.
- AI Literacy, governança e segurança reutilizam os serviços existentes na visão pessoal. Valores ausentes permanecem `null`. Segurança mantém a regra existente de 100 pontos na ausência de eventos. Não foi criado AMS ou AI Credits, nem uma média organizacional sem metodologia definida.
- Ferramenta filtra apenas consumo, requisições, sucesso e latência. Entregas e scores individuais continuam relativos ao período e à pessoa selecionada, considerando todas as ferramentas.
- Variações dos cartões comparam taxas por dia corrido (66 contra 90 dias), sem extrapolar totais. Comparação com base anterior zero não produz infinito. O gráfico permite alternar total e taxa diária; todos os gráficos têm alternativa tabular.

## Validação

- `mvn test`: 33 testes (7 novos de dashboard) aprovados com JDK 21. O Java 25 padrão desta máquina é incompatível com a versão atual de Byte Buddy/Mockito; utilize JDK 17/21 para a suíte.
- `npm run test:dashboard` em `frontend`: integração somente de leitura com a API e a base de exemplo. `DASHBOARD_URL` altera a origem do frontend e `DASHBOARD_EMPLOYEE` altera a pessoa padrão (`C01-E0001`). Verifica cinco períodos, agregação de áreas, filtros, visão de gestor, visão individual, busca vazia e respostas 400/403.
- Os KPIs da empresa C01 no P5 foram conferidos por SQL independente: 213 issues, 396 PRs, 864.131 requisições e 510.660.923 tokens.
- A página cancela requisições antigas, esconde resultados enquanto recalcula e oferece nova tentativa em caso de falha. Sem logs, apresenta estado vazio em vez de inventar períodos.

O banco existente foi utilizado sem recriação, importação ou atualização de registros. Os serviços existentes de score não foram alterados.
