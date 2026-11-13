CREATE TABLE IF NOT EXISTS arvore_curso (
 id bigserial PRIMARY KEY, empresa text NOT NULL, departamento text NOT NULL,
 autor text NOT NULL REFERENCES employees(employee_id), nivel integer NOT NULL CHECK (nivel > 0),
 titulo text NOT NULL, aula text NOT NULL, pergunta text NOT NULL, correta text NOT NULL, alternativa text NOT NULL,
 UNIQUE (empresa, departamento, nivel, titulo)
)@@
CREATE TABLE IF NOT EXISTS arvore_conclusao (
 curso bigint NOT NULL REFERENCES arvore_curso(id), funcionario text NOT NULL REFERENCES employees(employee_id),
 dia date NOT NULL, concluido_em timestamptz NOT NULL DEFAULT now(), PRIMARY KEY(funcionario, curso)
)@@

ALTER TABLE arvore_curso ALTER COLUMN autor DROP NOT NULL@@
ALTER TABLE arvore_curso ALTER COLUMN empresa DROP NOT NULL@@
ALTER TABLE arvore_curso ALTER COLUMN departamento DROP NOT NULL@@
ALTER TABLE arvore_curso ADD COLUMN IF NOT EXISTS logo text NOT NULL DEFAULT 'semente'@@
CREATE UNIQUE INDEX IF NOT EXISTS arvore_catalogo_titulo ON arvore_curso(titulo) WHERE empresa IS NULL@@
INSERT INTO arvore_curso(nivel,titulo,logo,aula,pergunta,correta,alternativa)
SELECT v.* FROM (VALUES
(1,'Fundamentos de IA Generativa','semente','Um modelo generativo produz conteúdo a partir de padrões aprendidos. Uma resposta plausível pode conter fatos inventados. Confira as afirmações importantes em fontes independentes.','Como validar um fato gerado pelo modelo?','Conferir em uma fonte independente','Aceitar se o texto parecer convincente'),
(2,'Introducao ao ChatGPT Enterprise','broto','Comece uma conversa com o objetivo, o contexto permitido pela empresa e o formato esperado. Antes de compartilhar o resultado, revise as informações.','O que ajuda a orientar uma conversa?','Explicar objetivo e contexto','Omitir o resultado esperado'),
(2,'Seguranca da Informacao e Uso de IA','bolota','Antes de enviar um documento à IA, verifique a classificação dos dados e a política da empresa. Remova segredos e use apenas ferramentas autorizadas para aquela informação.','Como lidar com uma chave de acesso em um documento?','Remover a chave antes de enviar','Manter a chave para contextualizar'),
(3,'Prompt Engineering na Pratica','folha','Descreva a tarefa, forneça um exemplo representativo e especifique critérios de aceitação. Compare o resultado com esses critérios antes de ajustar o pedido.','Qual prática permite avaliar um prompt?','Testar com critérios definidos','Aumentar sempre o tamanho do pedido'),
(3,'LGPD e Ferramentas de IA','bolota','Identifique a finalidade do uso e limite os dados pessoais ao mínimo necessário. Consulte as orientações da organização antes de enviar dados a uma ferramenta.','Quais dados incluir?','Os necessários para a finalidade autorizada','Todo o cadastro disponível'),
(3,'IA Responsavel: Vieses e Limitacoes','folha','Uma boa média de acertos pode esconder erros concentrados em um grupo. Compare os resultados entre grupos relevantes e investigue diferenças antes de automatizar decisões.','Como investigar um possível viés?','Comparar erros entre grupos','Usar apenas a média geral'),
(4,'Analise de Dados com IA','broto','Inspecione a origem dos dados, os valores ausentes e as unidades. Recalcule indicadores em uma amostra antes de divulgar conclusões produzidas pela IA.','Como verificar uma média sugerida?','Recalcular com os dados de origem','Pedir uma redação mais convincente'),
(4,'Automatizacao de Tarefas com IA','semente','Defina entradas e saídas esperadas e teste casos de falha. Para ações de impacto, inclua revisão humana antes da execução e registre o resultado.','Quando revisar uma ação de impacto?','Antes da execução','Apenas após uma reclamação'),
(4,'Etica e Governanca de Dados em IA','bolota','Registre a origem dos dados, a finalidade e quem responde pela decisão. Uma trilha de auditoria permite investigar erros e justificar o uso de um sistema.','Qual registro facilita uma auditoria?','Origem dos dados e responsável pela decisão','Somente o nome da ferramenta')
) AS v(nivel,titulo,logo,aula,pergunta,correta,alternativa)
WHERE EXISTS (SELECT 1 FROM training_records t WHERE t.course_name=v.titulo)
ON CONFLICT DO NOTHING@@
