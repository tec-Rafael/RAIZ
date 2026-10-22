# Dashboard RAIZ

O dashboard agora acompanha o AMS por pessoa e período, com visões demonstrativas de colaborador, gestor e administrador. Consulte [execução, regras, exemplos e contratos AMS](ams.md).

A página inclui notas de 0 a 100, tratamento explícito de ausências, pesos efetivos, evolução, melhores desempenhos, comparação entre empresas/áreas e distribuição de notas. Um painel expansível preserva as métricas técnicas de uso de IA e entregas.

As rotas anteriores `/api/dashboard/usuarios`, `/filtros`, `/resumo` e `/integrantes` permanecem disponíveis. `resumo` retorna requisições, tokens, issues fechadas, PRs integrados, ciclos medianos, sucesso/latência, evolução e distribuição por ferramenta. `integrantes` mantém busca, ordenação e paginação por pessoa ou área. O teste `npm run test:dashboard` continua validando esses contratos.

Os períodos começam no primeiro log do banco: quatro de 90 dias e um quinto de 66. A equipe do gestor equivale ao seu departamento. A troca de perfil continua em modo de demonstração.
