# Metodologia do AMS (AI Maturity Score) — rAIz

> Documento escrito a partir do código Java (`br.com.raiz.score`), para o assistente do dashboard consultar.
> Revisão pendente pela equipe do rAIz.

## O que é o AMS
O AMS (AI Maturity Score) mede, de 0 a 100, a maturidade de cada funcionário no uso de Inteligência Artificial em um período.
A nota é a média das dimensões aplicáveis à pessoa:
- **Segurança**, **Governança**, **FinOps** e **AI Literacy** valem para todos.
- **Delivery** vale só para desenvolvedores, quando o cargo contém "desenvolvedor", "developer", "software engineer" ou "engenheiro de software".

Todas as dimensões aplicáveis têm o **mesmo peso**:
- quem não é desenvolvedor tem 4 dimensões, e cada uma vale 25%;
- desenvolvedores têm 5 dimensões, e cada uma vale 20%.

## Níveis
| Nível | Faixa do AMS |
|---|---|
| Excelente | 85 a 100 |
| Alto | 70 a menos de 85 |
| Médio | 60 a menos de 70 |
| Baixo | abaixo de 60 |

## Períodos
- A base é dividida em 5 períodos a partir do primeiro registro de uso de IA (01/06/2025).
- P1 a P4 têm 90 dias cada.
- P5 tem 66 dias e vai de 27/05/2026 a 31/07/2026.
- A nota de cada período considera apenas os registros daquele período.

## Dimensões

### Segurança
- Começa em 100 e perde pontos a cada evento de segurança do período:
  - baixa: −5;
  - média: −10;
  - alta: −20;
  - crítica: −40.
- A nota mínima é 0. Sem eventos no período, a nota é 100.
- Exemplos de eventos: uso de ferramenta não aprovada (Shadow AI), alerta de DLP (possível dado sensível no prompt), upload de arquivo bloqueado e tentativa de acesso fora da política.
- **Como melhorar:** usar só ferramentas aprovadas, não colar dados sensíveis em prompts e respeitar as políticas de upload.

### Governança
- É a porcentagem das requisições de IA feitas em ferramentas **aprovadas** para a empresa.
- Exemplo: 80% das requisições em ferramentas aprovadas dão nota 80.
- Sem uso de IA no período, não há nota de governança.
- **Como melhorar:** concentrar o uso nas ferramentas aprovadas pela empresa e evitar ferramentas fora da política.

### FinOps (eficiência de custo)
- Compara o custo da pessoa com a **mediana** de todos os funcionários no período, em dois indicadores de mesmo peso:
  - custo por 1.000 tokens;
  - custo por requisição.
- Em cada indicador, a nota é `mediana ÷ custo da pessoa × 100`, limitada a 100.
  - Quem gasta igual ou menos que a mediana tira 100.
  - Quem gasta o dobro tira 50.
- O custo é calculado pelos tokens de entrada e saída multiplicados pelo preço de cada modelo.
- **Como melhorar:** usar modelos mais leves para tarefas simples, como resumos, revisões curtas e reformulações, e reservar os modelos mais caros para tarefas complexas. Prompts objetivos também gastam menos tokens.

### AI Literacy (capacitação)
É formada por três partes, com estes pesos:

| Parte | Peso | Cálculo |
|---|---|---|
| Conclusão | 50% | cursos concluídos no período ÷ (concluídos + matriculados ainda não concluídos) × 100 |
| Desempenho | 30% | média das notas dos cursos concluídos no período |
| Diversidade | 20% | categorias diferentes concluídas no período ÷ 4 × 100, com máximo de 100 |

- **Cursos da árvore do rAIz:** cada curso concluído na trilha de cursos da própria plataforma (a "árvore"), dentro do período, soma **+5 pontos** à AI Literacy, até o máximo de 100. Vale mesmo para quem não tem outros cursos no período: nesse caso a nota parte de zero.
- Se a pessoa não tem nenhum curso no período, nem cursos da árvore, a AI Literacy fica **ausente** e entra como **zero** no AMS. A tela mostra "zero por ausência".
- Categorias de curso:
  - IA Generativa;
  - Governança;
  - Segurança e Compliance;
  - Ferramentas Corporativas;
  - Ferramentas de Desenvolvimento;
  - Dados e Analytics;
  - Produtividade;
  - Aplicações de Negócio.
- **Como melhorar:** concluir os cursos em que já está matriculado (é o que mais pesa), tirar boas notas, variar as categorias (até 4 diferentes no período) e concluir cursos da árvore do rAIz (+5 cada).

### Delivery (só desenvolvedores)
Combina quatro indicadores de entrega, com estes pesos:

| Indicador | Peso | Melhor quando |
|---|---|---|
| Tempo de resolução de issues (mediana, em dias) | 35% | menor |
| Tempo de integração de PRs (mediana, em dias) | 25% | menor |
| Taxa de revisão de PRs (% com ao menos uma revisão) | 20% | maior |
| Volume de PRs no período | 20% | maior |

- Cada indicador vira duas notas:
  - **individual:** compara com faixas de referência, do p10 ao p90. Por exemplo, resolver issues em 9,2 dias ou menos é o melhor nível, e em 33,8 dias ou mais é o pior;
  - **relativa:** compara com pessoas do mesmo cargo.
- Delivery = 70% da nota individual + 30% da relativa.
- **Como melhorar:** revisar e integrar PRs com mais agilidade, pedir revisão em todos os PRs e manter um fluxo constante de entregas.

## Visões do dashboard
- **Colaborador:** vê apenas o próprio AMS.
- **Gestor:** vê a si mesmo e as pessoas do próprio departamento.
- **Administrador:** vê os dados agregados por empresa e por área, e pode filtrar o recorte.
- Em recortes com várias pessoas, o AMS exibido é a **média aritmética** dos AMS disponíveis, com peso igual por pessoa.

## Perfis de uso de IA (modelo de machine learning)
O rAIz agrupa as pessoas em perfis conforme o padrão de uso no período: volume, dias de uso, custo, falhas, ferramentas e uso fora da política.
- **Desenvolvimento intensivo:** usa IA quase todo dia em várias ferramentas, com forte presença de assistentes de código.
- **Uso corporativo frequente e caro:** usa IA com frequência em apps de escritório, mas com custo por requisição alto.
- **Uso fora da política:** uso leve, com boa parte das requisições em ferramentas não aprovadas.
- **Uso esporádico:** usa IA poucas vezes e em poucas aplicações.
- **Sem uso registrado:** nenhuma requisição de IA no período.

O perfil serve para orientar recomendações; ele **não altera** a nota do AMS.
