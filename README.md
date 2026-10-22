# RAIZ

Projeto com API Spring Boot, PostgreSQL e frontend simples.

## Dashboard

O dashboard consulta dados reais do banco pelas rotas `/api/dashboard`, com visões demonstrativas de colaborador, gestor e administrador. Inclui cinco períodos, KPIs de entregas e uso de IA, gráficos e filtros interativos. Veja [execução, contratos e critérios de cálculo](docs/dashboard.md).

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

- Email: `root@raiz.local`
- Senha: `123456`

O login consulta o banco e não usa JWT ou token.

## API

- `POST /api/funcionario/login`
- `POST /api/funcionario/criar` — só um funcionário com posição GESTOR pode cadastrar (envie `gestorId`); o novo funcionário herda `company_id`/`department_id` do gestor.
- `GET /api/funcionario/listar`
- `GET /api/funcionario/buscar/{id}`
- `DELETE /api/funcionario/deletar/{id}`

Exemplo de cadastro (usando o gestor semeado `ROOT-E0001`):

```json
POST /api/funcionario/criar
{
  "gestorId": "ROOT-E0001",
  "nome": "Ana Souza",
  "email": "ana.souza@raiz.local",
  "senha": "123456",
  "posicao": "COLABORADOR"
}
```
