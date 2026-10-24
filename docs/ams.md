# Dashboard AMS

O dashboard calcula o AMS por colaborador e período a partir do PostgreSQL e oferece três visões conforme a sessão autenticada. A interface mostra as notas originais, as notas usadas, as ausências, os pesos efetivos e a contribuição de cada dimensão.

## Execução

Execute `docker compose up -d --build` para atualizar a API na porta 8080 e iniciar o PostgreSQL. O volume existente é preservado. Em `frontend/.env`, use `url_springboot=http://localhost:8080`. Na pasta `frontend`, execute `npm start` e abra http://localhost:3000/dashboard.

Para desenvolvimento local Java, utilize JDK 17/21 (`mvn spring-boot:run`). Os testes foram executados com JDK 21; o Mockito atual do projeto não suporta o JDK 25 padrão desta máquina.

## Cálculo do AMS

- Desenvolvedores: Segurança, Governança, FinOps, AI Literacy e Delivery, inicialmente com 20% cada.
- Outros profissionais: Segurança, Governança, FinOps e AI Literacy, inicialmente com 25% cada. Delivery é omitida, inclusive do denominador e do detalhamento individual.
- Dimensões sem nota são excluídas. Os pesos das dimensões restantes são normalizados para somar 100%.
- AI Literacy sem nota registrada usa zero no cálculo e continua sendo identificada como ausente. Seu peso não é descartado; ela participa da normalização junto com as dimensões disponíveis.
- Se nenhuma dimensão tiver nota originalmente disponível, o AMS fica `null`, mesmo com o zero aplicado a AI Literacy.
- Notas registradas iguais a zero são válidas. Valores inválidos (fora de 0–100 ou não finitos) são rejeitados pelo calculador.
- O cálculo mantém a precisão completa; arredondamento ocorre apenas na apresentação, exceto nas parcelas de Delivery que já possuíam arredondamento na fórmula original.

Exemplos conferíveis:

| Perfil | Segurança | Governança | FinOps | AI Literacy registrada/usada | Delivery | AMS |
| --- | --- | --- | --- | --- | --- | --- |
| Desenvolvedor | 100 | 80 | 60 | 40 / 40 | 20 | 60 |
| Não desenvolvedor | 100 | 80 | 60 | 40 / 40 | Não se aplica | 70 |
| Não desenvolvedor | 80 | 80 | 80 | Ausente / 0 | Não se aplica | 60 |
| Desenvolvedor | 90 | Ausente | Ausente | 30 / 30 | Ausente | 60 (50% Segurança, 50% AI Literacy) |
| Qualquer perfil | Ausente | Ausente | Ausente | Ausente / 0 | Ausente ou não aplicável | Indisponível |

O cargo identifica desenvolvedores: descrições com Desenvolvedor, Developer, Software Engineer ou Engenheiro(a) de Software. QA, DevOps, Dados e outros cargos não recebem Delivery apenas por terem registros de engenharia. O cargo utilizado é exibido na análise individual.

## Delivery: esclarecimento adotado

Na conversa, os cinco tópicos foram esclarecidos como as cinco dimensões do AMS. Por isso não foi criado um quinto indicador interno de Delivery. Mantêm-se os quatro existentes no sistema:

1. Tempo mediano de resolução de issues: peso interno inicial de 35%.
2. Tempo mediano de integração de PRs: 25%.
3. Taxa de revisão de PRs: 20%.
4. Volume de PRs: 20%.

As notas individuais usam os referenciais P10/P90 e a normalização já existentes em `DeliveryMetricsService`. As notas relativas usam o mesmo cargo e exigem no mínimo quatro pessoas com dados válidos. Os pesos internos são redistribuídos entre os indicadores com nota. A composição de Delivery preserva 70% individual + 30% relativa quando ambas existem; caso só uma exista, usa a parcela disponível. Quando todos os indicadores estiverem ausentes, Delivery fica sem nota e seu peso é redistribuído no AMS.

As issues precisam ser criadas e fechadas dentro do período, conforme o critério do serviço existente. PRs entram pelo momento da criação. A consulta AMS considera somente integrações ocorridas até o fim do período, evitando usar uma integração futura para calcular a nota histórica. O número de revisões não possui data própria no esquema e reflete o registro atual.

O período final tem 66 dias; os referenciais históricos de volume foram preservados e não há extrapolação de entregas para 90 dias. Essa diferença de duração pode afetar a nota do componente de volume.

## Fontes e critérios

- Segurança: regra existente de penalidades por severidade, com piso zero e nota 100 na ausência de incidentes registrados.
- Governança: requisições em ferramentas aprovadas / total de requisições, vezes 100. Sem requisições, a nota fica ausente.
- FinOps: média disponível de eficiência de custo por mil tokens e por requisição, cada uma com 50% inicial. Compara custos estimados com a mediana dos colaboradores da base no mesmo período e limita a eficiência a 100. Se algum modelo utilizado não tiver preço, FinOps fica ausente. É uma estimativa por tokens, sem incorporar assinaturas/licenças fixas.
- AI Literacy: mesma composição existente (50% conclusão, 30% nota de treinamento, 20% diversidade), redistribuindo pesos internos quando faltam parcelas. O zero obrigatório é aplicado ao resultado dessa dimensão no AMS, sem apagar a ausência original.

A consulta de origem calcula as métricas em lote no PostgreSQL. O serviço trabalha com linhas agregadas por funcionário, sem enviar os logs ao navegador. Um cache interno de 30 segundos evita recalcular todos os períodos em cada paginação. Não foram criadas tabelas nem gravadas notas ou registros no banco.

## Períodos

A âncora é `MIN(ai_usage_logs.timestamp)` do banco. As rotas de score e o dashboard usam a mesma resolução dinâmica. São quatro períodos de 90 dias e um quinto de 66, sem sobreposição. Na base atual: P1 01/06/2025–29/08/2025, P2 30/08/2025–27/11/2025, P3 28/11/2025–25/02/2026, P4 26/02/2026–26/05/2026 e P5 27/05/2026–31/07/2026.

## Visões e filtros

Empresa → gestor → departamento → colaborador → período. Os filtros seguintes são refeitos quando uma seleção anterior muda. Em administração, selecionar todas as empresas habilita a comparação organizacional; selecione uma empresa para filtrar gestor, área ou pessoa.

- Colaborador: somente a própria pessoa de referência.
- Gestor: própria pessoa e funcionários do próprio departamento na própria empresa. Equipe continua equivalendo a departamento.
- Administrador: pode comparar todas as empresas, filtrar gestores, áreas e colaboradores e consultar sua própria nota.

O backend rejeita filtros fora do recorte da visão selecionada com HTTP 403. Gestores de filtro são funcionários cadastrados com posição GESTOR. A identidade e o cargo usados na autorização vêm exclusivamente da sessão validada pelo Java. A estrutura não tem histórico de vínculos, então departamentos e cargos são os do cadastro atual.

O seletor de identidade/perfil foi substituído pela sessão autenticada. Os dados continuam simulados, mas a autorização por cargo é aplicada no Java. Parâmetros `perfil` e `usuarioId` enviados pelo cliente não alteram a identidade nem ampliam permissões. Veja [autenticação](autenticacao.md).

## Agregação e apresentação

- AMS da equipe/área/empresa: média aritmética dos AMS disponíveis, com peso igual por pessoa. AMS ausente não entra no denominador. Uma comparação geral é ponderada pelo número de pessoas com AMS, não pela média simples das médias das empresas.
- Dimensões: média das notas disponíveis na população aplicável. Delivery considera apenas desenvolvedores. AI Literacy considera os zeros usados por ausência, mas também conta a ausência original.
- `validos` da dimensão representa notas usadas na média, inclusive zeros obrigatórios de AI Literacy. `ausentes` representa registros originalmente nulos; esses números se sobrepõem em AI Literacy, o que é explicitado na interface.
- Melhor resultado: maior AMS; empates são informados e o ID desempata a pessoa de referência de forma estável. Um gráfico mostra o melhor resultado de cada período e outro acompanha a mesma pessoa que lidera o período selecionado.
- Distribuição: faixas [0,20), [20,40), [40,60), [60,80), [80,100] e AMS indisponível.
- Comparações: barras das 12 maiores notas e evolução das 6 maiores do período, com tabela contendo todas as organizações e suas ausências/cobertura. Gráficos não conectam pontos através de notas nulas.
- Tabela paginada de pessoas preserva notas ausentes. A busca afeta a tabela, não a população dos agregados. Selecionar uma pessoa abre sua nota e composição.
- O painel expansível de uso de IA e entregas preserva o acesso a requisições, tokens, issues, PRs e distribuição por ferramenta, com as APIs técnicas anteriores.

## Endpoints

| Rota | Resposta |
| --- | --- |
| `GET /api/dashboard/ams/usuarios` | Pessoas permitidas pela sessão, sem credenciais |
| `GET /api/dashboard/ams/filtros` | Pessoa autenticada, pessoas disponíveis e períodos |
| `GET /api/dashboard/ams?periodo=5` | Resumo AMS, composição pessoal quando aplicável, evolução, empresas/áreas, destaques, distribuição e tabela paginada |
| `GET /score/ams/{employeeId}?periodo=5` | Pessoa, período e cálculo AMS completo |

As três rotas de dashboard possuem proxy Express. Parâmetros de filtro adicionais: `companyId`, `gestorId`, `departmentId`, `employeeId`. Parâmetros da tabela: `page` (zero-based), `size` (1–100), `busca` (até 150 caracteres), `ordem=ams|nome`. As rotas técnicas anteriores e `/score/ai-literacy`, `/score/governance`, `/score/security-incidents`, `/score/finops` e `/score/delivery` continuam disponíveis.

## Validação

- `mvn test`: 57 testes aprovados com JDK 21, incluindo 17 novos testes de AMS. Cobrem exemplos de desenvolvedor/não desenvolvedor, zeros, notas ausentes, pesos e contribuições, limites de score, mediana FinOps, Delivery parcial, filtros fora do escopo, agregação e empates. O caso final de Delivery não aplicável também foi conferido separadamente.
- `npm run test:ams`: integração somente de leitura. Verifica pesos, composição dos AMS, cinco períodos, exclusão de Delivery para não desenvolvedores, agregação das empresas, distribuição, filtros e respostas 400/403. Compara Segurança, Governança e AI Literacy com os serviços existentes.
- `npm run test:dashboard`: mantém a verificação das APIs técnicas anteriores.
- Para outro endereço de teste, defina `DASHBOARD_URL` (frontend) e `SPRINGBOOT_URL` (API). Os padrões são http://127.0.0.1:3000 e http://127.0.0.1:8080.

A base validada tem 2.357 funcionários e 18 empresas. No P5, a média geral AMS é aproximadamente 58,3690; há 4.277 notas de dimensão originalmente ausentes entre 9.631 dimensões aplicáveis. São resultados calculados sobre a base existente, de origem sintética.
