# Autenticação e acesso no RAIZ

O projeto continua usando Express/EJS no frontend, Java/Spring Boot nas rotas que acessam o banco e PostgreSQL. Os dados são simulados. O dashboard mantém o layout, os gráficos e as regras AMS; a seleção de identidade de demonstração foi substituída pela sessão autenticada.

## Sessão de 8 horas

1. `POST /api/funcionario/login` recebe `email` e `senha`. O Java valida o hash BCrypt existente, verifica a conta ativa e gera 64 bytes aleatórios (128 caracteres hexadecimais).
2. O token e o vencimento ficam em `employees.token` e `employees.token_expira_em`. Um novo login substitui o token anterior: existe uma sessão ativa por usuário.
3. O Java devolve o cookie `usuario` com token + `employee_id` alfanumérico. O proxy Express repassa `Set-Cookie`; a resposta JSON não expõe token nem senha. Cookie: HttpOnly, SameSite=Lax, Path=/, Max-Age=28800.
4. A validade é fixa; navegar não renova as 8 horas. A API valida token, vencimento, conta ativa e cargo no banco. Alterar `perfil` ou `usuarioId` na URL não muda a identidade.
5. `POST /api/funcionario/logout` revoga somente o token recebido e expira o cookie. Um cookie antigo não encerra uma sessão mais recente. Não há logout por GET.

A home (`/`), `/dashboard`, `/perfil` e as páginas de funcionários exigem sessão. Sem sessão, as páginas redirecionam para `/login`; APIs retornam 401. Recurso fora do cargo retorna 403. A API Java aplica as mesmas restrições quando acessada diretamente em :8080.

## Permissões

| Cargo | Dashboard e funcionários | Cadastro/remoção |
| --- | --- | --- |
| Colaborador | Somente os próprios dados | Sem acesso |
| Gestor | Próprios dados e membros do seu departamento na mesma empresa | Cadastra no próprio departamento; remove membros permitidos sem histórico vinculado |
| Administrador | Todas as empresas, áreas e funcionários, incluindo a própria análise | Indica um gestor responsável no cadastro; pode remover funcionários sem vínculos |

Não é permitido excluir a própria conta nem o administrador. O cadastro não cria outro administrador e não aceita `raiz@gmail.com` para contas comuns. O banco também restringe o administrador ao registro `ROOT-E0001` e ao e-mail reservado, com índice único para esse cargo.

A listagem bruta de uso de IA respeita os funcionários permitidos; a consulta individual valida o dono do registro. Escrita/remoção de logs e o ranking global legado de uso ficam reservados ao administrador. Os gestores usam os recortes autorizados do dashboard para acompanhar a equipe.

Os endpoints `/api/dashboard/ams/usuarios` e `/api/dashboard/ams/filtros` retornam apenas pessoas permitidas. As consultas AMS recebem somente filtros de análise (`companyId`, `gestorId`, `departmentId`, `employeeId`, `periodo`, busca/paginação). O detalhamento técnico legado aceita `companyId` para o administrador selecionar outra empresa; o servidor escolhe a referência dessa empresa.

## Perfil e consumo de IA

`GET /api/funcionario/sessao` retorna o usuário autenticado e o vencimento, sem credenciais. `/perfil` mostra nome, e-mail, cargo, perfil de acesso e botão **Sair do sistema**.

`GET /api/funcionario/me/consumo-ia?periodo=5` agrega os logs do próprio usuário, agrupados por ferramenta, com tokens de entrada, saída e total. Aceita períodos 1 a 5 ou `historico`. Não aceita escolher outra identidade. Ferramentas sem consumo aparecem com zero. Esses números são quantidades de tokens, não créditos fictícios nem valores monetários.

Os períodos preservam a regra existente: começam no primeiro log da base, quatro de 90 dias e um quinto de 66 dias.

## Contas de demonstração

| Uso | Nome | E-mail | Senha | Identificador |
| --- | --- | --- | --- | --- |
| Único administrador | Administrador RAIZ | raiz@gmail.com | 12345 | ROOT-E0001 |
| Colaborador desenvolvedor, 5 dimensões preenchidas no P5 | Bruno Neves | bruno.neves@empresa.com | 123456 | C01-E0354 |
| Colaboradora, 4 dimensões preenchidas no P5 | Valentina Souza | valentina.souza@empresa.com | 123456 | C12-E0237 |
| Gestora, equipe com maior AMS entre os exemplos | Ana Andrade | ana.andrade@empresa.com | 123456 | C02-E0002 |
| Gestora, equipe com menor AMS entre os exemplos | Mariana Machado | mariana.machado@empresa.com | 123456 | C03-E0004 |

Na base consultada, P5: Bruno AMS 86,00; Valentina 96,18; departamento de Ana 70,24 (13 pessoas); departamento de Mariana 54,43 (5 pessoas). Os gestores foram selecionados entre equipes com ao menos 5 pessoas e cobertura de Governança/FinOps em pelo menos 80% dos integrantes. A comparação representa AMS segundo as métricas existentes, não uma comprovação de produtividade absoluta. Nenhum score ou histórico foi alterado para produzir esses exemplos.

## Inicialização e configuração

Recompile a API: `docker compose up -d --build --no-deps api`. O frontend inicia normalmente com `npm start` dentro de `frontend` (não mantenha duas instâncias na porta 3000).

A migração `src/main/resources/db/autenticacao.sql` roda pelo inicializador SQL do Spring **antes da validação JPA**, inclusive no volume existente. Usa o separador `@@` para preservar o bloco PostgreSQL. Adiciona colunas de sessão, restrições do administrador e a tabela de controle `raiz_migracoes`. Não recria tabelas de negócio nem apaga logs.

`AdministradorInicial` promove o antigo registro técnico ROOT-E0001 uma única vez. Reiniciar a API não redefine sua senha. `ContasDemonstracaoInicial` define a senha das quatro contas existentes uma única vez, com BCrypt e controle transacional em `raiz_migracoes`. Preserva IDs, vínculos, posições e históricos. O marcador impede redefinições em reinicializações.

| Variável na API Java | Padrão nesta demonstração | Finalidade |
| --- | --- | --- |
| `COOKIE_SECURE` | `false` | Use `true` em HTTPS para enviar cookies somente por conexão segura |
| `AUTH_ORIGINS` | localhost/127.0.0.1 nas portas 3000 e 8080 | Lista exata, separada por vírgulas, das origens permitidas para mutações |
| `RAIZ_DEMO_CONTAS` | `true` | Habilita a configuração inicial das quatro contas de demonstração; use `false` fora dessa base simulada |

Defina essas variáveis no processo Java; ao usar Docker, configure-as no bloco `environment` do serviço `api` no Compose.

O Express verifica a origem das mutações e repassa Origin/Sec-Fetch-Site para o Java, que valida novamente. Requisições cross-site são recusadas. Foi removido o CORS curinga dos controllers de funcionários e utilização de IA. A navegação permanece pela mesma origem do frontend.

## Validação

- Java: 72 testes aprovados com `mvn test` (neste ambiente, usar JDK 21). Testes cobrem geração/expiração, senha inválida, conta inativa, ID alfanumérico, revogação, escopo de cargo, origem e acesso aos scores com parâmetros de matriz.
- Integrações aprovadas com frontend/API e banco atuais: `npm run test:auth`, `npm run test:ams` e `npm run test:dashboard`, dentro de `frontend`. Login/logout também foram verificados com a origem do navegador.
- Os testes de integração usam as contas de demonstração, autenticam e encerram as sessões ao terminar. Não criam nem excluem funcionários ou logs. Por haver uma sessão por conta, executá-los desconecta logins anteriores dessas contas.
- `test/selecionar-contas-demo.cjs` é uma consulta auxiliar que recalcula candidatos sem alterar cadastros; não é necessária para rodar a aplicação.

A navegação do dashboard, perfil e páginas de funcionários compartilha a mesma sidebar. No desktop ela ocupa apenas a altura da janela e permanece fixa durante a rolagem; no celular vira uma barra compacta no topo. A home preserva sua sidebar original. Os atalhos para cadastrar e listar funcionários aparecem somente no perfil de gestores e administradores, mantendo a autorização Java existente.

O perfil organiza consumo em resumo, card de destaque da IA mais utilizada e grade com participação percentual, entrada e saída. Ferramentas sem consumo ficam em uma seção expansível. O filtro de períodos, o estado vazio e o logout foram preservados.

As views foram conferidas para os três cargos; `npm run test:navigation` verifica a home protegida e os links por perfil. A renderização foi conferida no navegador em desktop e em uma área de 390 px, com dados da API. Nenhuma regra de cálculo ou contrato Java foi alterado nesta atualização.

No dashboard do gestor, empresa e departamento ficam fixos nos vínculos da sessão e o seletor de gestores fica oculto. O filtro de colaboradores alterna entre a própria análise e os integrantes da mesma equipe; limpar os filtros mantém esse contexto. A escolha entre gestores permanece exclusiva da visão administrativa.
