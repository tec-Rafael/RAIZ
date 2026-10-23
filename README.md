# RAIZ

Projeto com API Spring Boot, PostgreSQL e frontend simples.

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

## Simulação e otimização de custos

A API `POST /api/custos/otimizar` compara alternativas de contratação para cada cenário. O sistema escolhe a opção viável de menor custo dentro do prazo, calcula a reserva de risco, manutenção e custo estimado do primeiro ano. Os valores e capacidades são parâmetros da requisição, não ficam fixos no código.

Exemplo de requisição:

```json
{
  "contingencyPercent": 15,
  "annualMaintenancePercentMin": 15,
  "annualMaintenancePercentMax": 20,
  "monthlyHostingCostMin": 300,
  "monthlyHostingCostMax": 3000,
  "monthlyAiCostMin": 0,
  "monthlyAiCostBase": 0,
  "monthlyAiCostMax": 0,
  "providers": [
    { "name": "Freelancer pleno", "hourlyRate": 125, "capacityHoursPerMonth": 110 },
    { "name": "Software house", "hourlyRate": 225, "capacityHoursPerMonth": 300 }
  ],
  "scenarios": [
    { "name": "Pequeno", "estimatedHours": 420, "deadlineMonths": 4 },
    { "name": "Médio", "estimatedHours": 600, "deadlineMonths": 6 },
    { "name": "Grande", "estimatedHours": 850, "deadlineMonths": 7 }
  ]
}
```

A resposta informa a capacidade e viabilidade de cada alternativa, os custos detalhados, a opção escolhida e as projeções mensal e anual. Para testar no projeto, acesse o Swagger em `http://localhost:8080/swagger-ui.html` após iniciar a aplicação.
