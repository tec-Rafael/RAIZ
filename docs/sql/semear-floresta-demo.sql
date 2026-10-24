-- Demonstração de crescimento: 4 colegas por departamento, em todos os períodos.
-- Não modifica contas de apresentação, registros AMS, carteira ou progresso existente.
-- Execute manualmente; reiniciar/zerar uma árvore não executa esta carga novamente.
BEGIN;
WITH candidatos AS (
 SELECT employee_id,row_number() OVER (PARTITION BY company_id,department_id ORDER BY employee_id) AS ordem
 FROM employees WHERE active=TRUE AND department_id IS NOT NULL
 AND coalesce(lower(posicao),'')<>'administrador'
 AND employee_id NOT IN ('C01-E0354','C12-E0237','C02-E0002','C03-E0004','ROOT-E0001')
), novos AS (
 INSERT INTO jardim_periodos(employee_id,periodo,dia,adubos_aplicados)
 SELECT employee_id,p,0,CASE WHEN ordem%3=0 THEN 16 WHEN ordem%3=1 THEN 8 ELSE 3 END
 FROM candidatos CROSS JOIN generate_series(1,5) AS p WHERE ordem<=4
 ON CONFLICT(employee_id,periodo) DO NOTHING
 RETURNING employee_id,periodo,adubos_aplicados
)
INSERT INTO jardim_adubacoes(employee_id,periodo,dia,quantidade)
SELECT employee_id,periodo,0,adubos_aplicados FROM novos
ON CONFLICT(employee_id,periodo,dia) DO NOTHING;
COMMIT;
