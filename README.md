# RAIZ

Projeto com API Spring Boot, PostgreSQL e frontend simples.

## Dashboard

O dashboard calcula o AMS por pessoa e período, com visões de colaborador, gestor e administrador autorizadas pela sessão Java. Exibe dimensões, pesos, notas ausentes, evolução, melhores desempenhos e comparações entre empresas e áreas. As métricas de uso de IA e entregas permanecem no detalhamento técnico. Veja [regras, exemplos e contratos AMS](docs/ams.md).

## Rodar

```bash
docker compose up --build
```

- PostgreSQL: `localhost:5433`
- API: `http://localhost:8080`
- Swagger: `http://localhost:8080/swagger-ui.html`

Para recriar o banco do zero:

```bash
docker compose down -v
docker compose up --build
```

## Login inicial

- Email: `raiz@gmail.com`
- Senha: `12345`

O Java valida a senha BCrypt e cria uma sessão opaca de 8 horas no banco. O cookie HttpOnly identifica a sessão; o logout revoga o token. Veja [autenticação, permissões e contas de demonstração](docs/autenticacao.md).

A home também oferece o [jardim gamificado](docs/jardim.md): saúde pelo AMS, crescimento com adubo, moedas por dia histórico, conquistas e ranking da empresa ou departamento. A jornada é uma simulação persistida no banco, sem modificar as métricas de produtividade. Reinicie o Node depois de atualizar para carregar o proxy `/api/jardim`.

## API

Os testes unitários e cenários HTTP em BDD podem ser executados com `mvn test`, sem
subir o banco. Veja [testes, cobertura e fluxo TDD](docs/testes.md).

- `POST /api/funcionario/login`
- `GET /api/funcionario/sessao`
- `POST /api/funcionario/logout`
- `GET /api/funcionario/me/consumo-ia?periodo=5`
- `POST /api/funcionario/criar` — gestor autenticado cadastra no próprio departamento; administrador pode indicar um gestor responsável (envie `gestorId`); o novo funcionário herda `company_id`/`department_id` do gestor.
- `GET /api/funcionario/listar`
- `GET /api/funcionario/buscar/{id}`
- `DELETE /api/funcionario/deletar/{id}`

Exemplo de cadastro com sessão do gestor `C02-E0002` (ou do administrador):

```json
POST /api/funcionario/criar
{
  "gestorId": "C02-E0002",
  "nome": "Ana Souza",
  "email": "ana.souza@raiz.local",
  "senha": "123456",
  "posicao": "COLABORADOR"
}
```
