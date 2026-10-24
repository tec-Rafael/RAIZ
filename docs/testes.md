# Testes de rotas, BDD e TDD

Os testes automatizados anteriores validam individualmente regras de AMS, períodos,
autenticação, permissões e economia do jardim. A existência desses testes não significa
que toda a aplicação tenha sido desenvolvida com TDD: TDD é escrever o teste antes da
alteração, observar uma falha relevante, implementar o mínimo e refatorar mantendo os testes verdes.

## Executar

Na raiz do projeto, com JDK 17 ou superior e Maven:

```powershell
mvn test
```

Somente os cenários HTTP BDD:

```powershell
mvn "-Dtest=RotasBddTest,FuncionarioRotasBddTest" test
```

Não é necessário iniciar Node, Java, Docker nem PostgreSQL. Os testes usam dados
fictícios próprios e não modificam as contas, moedas ou métricas da demonstração.
Resultados individuais: `target/surefire-reports/`. Cobertura JaCoCo:
`target/site/jacoco/index.html`, gerada ao terminar `mvn test` com sucesso.

## Como o BDD é executado

Os cenários são implementados em JUnit 5, MockMvc e BDDMockito, com estrutura
**Dado / Quando / Então**: `given` prepara o contexto, `mvc.perform` executa a requisição
e `andExpect` / `then(...).should()` verificam o resultado e os efeitos.
`@DisplayName` e nomes de testes identificam os comportamentos; testes parametrizados
exercitam cargos, rotas, limites e entradas diferentes. Não dependem de Cucumber nem
de arquivos Gherkin que apenas descrevem cenários sem executá-los.

Exemplo de comportamento executado em `FuncionarioRotasBddTest.cadastroHerdaDepartamento`:

```gherkin
Cenário: gestor cadastra funcionário no próprio departamento
  Dado um gestor autenticado da empresa C1 e departamento D1
  Quando cadastra uma pessoa enviando outra empresa e outro departamento no JSON
  Então a API retorna 201
  E o funcionário recebe empresa C1 e departamento D1
  E a senha persistida passa pelo codificador
  E a resposta não contém a senha
```

## Camadas e limites

| Suíte | Componentes reais | Componentes isolados |
| --- | --- | --- |
| Testes existentes de serviços | Regras de cada serviço | Repositórios e dependências externas |
| `RotasBddTest` | Seis controllers, interceptor, vinculação HTTP, validação de DTOs e JSON | Serviços; validação do token substituída por identidades de teste |
| `FuncionarioRotasBddTest` | Controller, FuncionarioService e regras de permissão do SessaoService | Repositório, codificador de senha, JDBC e validação do token |

MockMvc executa o fluxo MVC em memória, sem abrir porta. A suíte usa
`standaloneSetup`: não valida a inicialização completa do Spring Boot, SQL,
transações PostgreSQL, proxy Express ou navegação no navegador. Os testes de sessão
existentes verificam separadamente geração, validade, expiração e revogação de tokens.
Os scripts de integração em `frontend/test` continuam disponíveis para o ambiente em
execução; eles são separados desta suíte porque alguns alteram dados de demonstração.

## Rotas cobertas

As 34 combinações de método e caminho dos seis controllers Java têm cenário HTTP.
As 32 protegidas são exercitadas tanto com sessão autenticada quanto sem cookie.
Login e logout têm cenários próprios. O teste `todasAsRotasEstaoNoManifesto` compara
o manifesto de cenários com os mapeamentos reais dos controllers carregados: adicionar
um endpoint nesses controllers exige atualizar seus cenários. Um novo controller também
precisa ser incluído no `standaloneSetup`.

| Grupo | Comportamentos adicionais |
| --- | --- |
| Funcionário | Login válido/inválido, cookie HttpOnly/Lax de oito horas, logout expirado, sessão ausente/expirada, consumo do próprio usuário, cadastro válido/inválido/duplicado, exclusão e listagem por cargo |
| Permissões | Colaborador restrito a si, gestor restrito à empresa/departamento, admin entre empresas, admin reservado, proibição de autoexclusão e exclusão do admin, bloqueio de mutações de origem externa |
| Utilização de IA | Cadastro exclusivo de admin, ID obrigatório, contagens não negativas, paginação limitada e ordenada, destaques limitados, busca inexistente |
| Dashboard | Identidade da sessão prevalece sobre perfil forjado, filtros de resumo/AMS, integrantes com busca/paginação e empresa inexistente |
| Scores | As seis rotas exigem período e autorização da pessoa; serialização das notas e período |
| Jardim | Estado e prévia, ranking, avanço e moedas retornadas, adubo no dia zero, reset, compra, equipamento, encerramento, limites de entrada e erros 400/403/409 em ProblemDetail |

As garantias internas de que adubar não avança dias, de que o avanço recompensa e de
que zerar preserva moedas são verificadas também em `JardimServiceTest`. Um teste de
controller com serviço mockado verifica o contrato HTTP e a delegação; sozinho não
prova a regra de cálculo nem a gravação no banco.

## Ciclo TDD aplicado nesta alteração

1. **Red:** criado `cadastroIaRejeitaDadosInvalidos` antes de alterar o DTO. Os seis
   casos (sem ID, ID vazio, requests/tokens/latência negativos) falharam: esperavam
   `400`, mas recebiam `201`. Isso revelou que `@Valid` não tinha restrições no DTO.
2. **Green:** adicionados `@NotBlank` e limite de 64 caracteres para `usageId`, além
   de `@PositiveOrZero` nas contagens e latência. Os demais campos opcionais continuam
   opcionais; zero continua válido. Os testes garantem que entradas inválidas não
   chegam ao serviço.
3. **Refactor:** casos compartilhados agrupados em testes parametrizados e manifesto
   de rotas; verificações específicas de JSON, argumentos e permissões permanecem
   separadas. Executar toda a suíte para detectar regressões.

Para futuras mudanças, primeiro acrescente o cenário de comportamento esperado,
execute-o contra o código atual e confirme que falha pelo motivo correto. Só então
altere a implementação e rode novamente os testes. Testes de regressão adicionados
depois de uma implementação não devem ser apresentados como TDD retroativo.
