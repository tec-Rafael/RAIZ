-- ================================================================
-- Migração: classificação de funcionários (gestor/colaborador)
-- e associação 1 gestor -> 1 departamento (161 departamentos)
-- Estratégia adotada: seleção de gestores POR EMPRESA, preservando
-- o isolamento entre empresas (company_id) já existente no schema.
-- Para cada empresa, os N primeiros funcionários (ID ASC, N = nº de
-- departamentos daquela empresa) tornam-se gestores dos departamentos
-- daquela mesma empresa (department_id ASC).
-- ================================================================

USE `ai_usage`;

-- Desliga o modo "Safe Updates" só para esta sessão (evita o erro
-- "You are using safe update mode..." do MySQL Workbench, que exige
-- WHERE numa coluna-chave em todo UPDATE/DELETE). É restaurado no final.
SET SQL_SAFE_UPDATES = 0;

START TRANSACTION;

-- 1) Criação da coluna `posicao` (reutilizável se já existir)
SET @col_exists := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = 'ai_usage' AND TABLE_NAME = 'employees' AND COLUMN_NAME = 'posicao'
);

SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE `employees` ADD COLUMN `posicao` ENUM(''gestor'',''colaborador'') NOT NULL DEFAULT ''colaborador'' AFTER `senha`',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2) Garante que todos comecem como colaborador (idempotente)
UPDATE `employees` SET `posicao` = 'colaborador';

-- 3) Marca os 161 gestores (selecionados por empresa)
UPDATE `employees` SET `posicao` = 'gestor' WHERE `employee_id` IN (
  'C01-E0001',
  'C01-E0002',
  'C01-E0003',
  'C01-E0004',
  'C01-E0005',
  'C01-E0006',
  'C01-E0007',
  'C01-E0008',
  'C01-E0009',
  'C01-E0010',
  'C01-E0011',
  'C02-E0001',
  'C02-E0002',
  'C02-E0003',
  'C02-E0004',
  'C02-E0005',
  'C02-E0006',
  'C02-E0007',
  'C02-E0008',
  'C02-E0009',
  'C02-E0010',
  'C02-E0011',
  'C03-E0001',
  'C03-E0002',
  'C03-E0003',
  'C03-E0004',
  'C03-E0005',
  'C03-E0006',
  'C03-E0007',
  'C03-E0008',
  'C03-E0009',
  'C03-E0010',
  'C03-E0011',
  'C04-E0001',
  'C04-E0002',
  'C04-E0003',
  'C04-E0004',
  'C04-E0005',
  'C04-E0006',
  'C04-E0007',
  'C04-E0008',
  'C04-E0009',
  'C04-E0010',
  'C04-E0011',
  'C05-E0001',
  'C05-E0002',
  'C05-E0003',
  'C05-E0004',
  'C05-E0005',
  'C05-E0006',
  'C05-E0007',
  'C05-E0008',
  'C05-E0009',
  'C05-E0010',
  'C05-E0011',
  'C06-E0001',
  'C06-E0002',
  'C06-E0003',
  'C06-E0004',
  'C06-E0005',
  'C06-E0006',
  'C06-E0007',
  'C06-E0008',
  'C07-E0001',
  'C07-E0002',
  'C07-E0003',
  'C07-E0004',
  'C07-E0005',
  'C07-E0006',
  'C07-E0007',
  'C07-E0008',
  'C08-E0001',
  'C08-E0002',
  'C08-E0003',
  'C08-E0004',
  'C08-E0005',
  'C08-E0006',
  'C08-E0007',
  'C08-E0008',
  'C09-E0001',
  'C09-E0002',
  'C09-E0003',
  'C09-E0004',
  'C09-E0005',
  'C09-E0006',
  'C09-E0007',
  'C09-E0008',
  'C09-E0009',
  'C10-E0001',
  'C10-E0002',
  'C10-E0003',
  'C10-E0004',
  'C10-E0005',
  'C10-E0006',
  'C10-E0007',
  'C10-E0008',
  'C10-E0009',
  'C11-E0001',
  'C11-E0002',
  'C11-E0003',
  'C11-E0004',
  'C11-E0005',
  'C11-E0006',
  'C11-E0007',
  'C11-E0008',
  'C11-E0009',
  'C12-E0001',
  'C12-E0002',
  'C12-E0003',
  'C12-E0004',
  'C12-E0005',
  'C12-E0006',
  'C12-E0007',
  'C12-E0008',
  'C12-E0009',
  'C13-E0001',
  'C13-E0002',
  'C13-E0003',
  'C13-E0004',
  'C13-E0005',
  'C13-E0006',
  'C13-E0007',
  'C13-E0008',
  'C13-E0009',
  'C14-E0001',
  'C14-E0002',
  'C14-E0003',
  'C14-E0004',
  'C14-E0005',
  'C14-E0006',
  'C14-E0007',
  'C14-E0008',
  'C14-E0009',
  'C15-E0001',
  'C15-E0002',
  'C15-E0003',
  'C15-E0004',
  'C15-E0005',
  'C15-E0006',
  'C15-E0007',
  'C16-E0001',
  'C16-E0002',
  'C16-E0003',
  'C16-E0004',
  'C16-E0005',
  'C16-E0006',
  'C16-E0007',
  'C17-E0001',
  'C17-E0002',
  'C17-E0003',
  'C17-E0004',
  'C17-E0005',
  'C17-E0006',
  'C17-E0007',
  'C18-E0001',
  'C18-E0002',
  'C18-E0003',
  'C18-E0004',
  'C18-E0005',
  'C18-E0006',
  'C18-E0007'
);

-- 4) Associação gestor -> departamento (reutiliza department_id existente)
--    Apenas os pares abaixo têm UPDATE de fato: os demais já apontavam
--    para o departamento correto e foram mantidos como estavam.
UPDATE `employees` SET `department_id` = 'C01-D01' WHERE `employee_id` = 'C01-E0001';
UPDATE `employees` SET `department_id` = 'C01-D02' WHERE `employee_id` = 'C01-E0002';
UPDATE `employees` SET `department_id` = 'C01-D03' WHERE `employee_id` = 'C01-E0003';
UPDATE `employees` SET `department_id` = 'C01-D04' WHERE `employee_id` = 'C01-E0004';
UPDATE `employees` SET `department_id` = 'C01-D05' WHERE `employee_id` = 'C01-E0005';
UPDATE `employees` SET `department_id` = 'C01-D06' WHERE `employee_id` = 'C01-E0006';
UPDATE `employees` SET `department_id` = 'C01-D07' WHERE `employee_id` = 'C01-E0007';
UPDATE `employees` SET `department_id` = 'C01-D08' WHERE `employee_id` = 'C01-E0008';
UPDATE `employees` SET `department_id` = 'C01-D09' WHERE `employee_id` = 'C01-E0009';
UPDATE `employees` SET `department_id` = 'C01-D10' WHERE `employee_id` = 'C01-E0010';
UPDATE `employees` SET `department_id` = 'C01-D11' WHERE `employee_id` = 'C01-E0011';
UPDATE `employees` SET `department_id` = 'C02-D01' WHERE `employee_id` = 'C02-E0001';
UPDATE `employees` SET `department_id` = 'C02-D02' WHERE `employee_id` = 'C02-E0002';
UPDATE `employees` SET `department_id` = 'C02-D03' WHERE `employee_id` = 'C02-E0003';
UPDATE `employees` SET `department_id` = 'C02-D04' WHERE `employee_id` = 'C02-E0004';
UPDATE `employees` SET `department_id` = 'C02-D05' WHERE `employee_id` = 'C02-E0005';
UPDATE `employees` SET `department_id` = 'C02-D06' WHERE `employee_id` = 'C02-E0006';
UPDATE `employees` SET `department_id` = 'C02-D07' WHERE `employee_id` = 'C02-E0007';
UPDATE `employees` SET `department_id` = 'C02-D08' WHERE `employee_id` = 'C02-E0008';
UPDATE `employees` SET `department_id` = 'C02-D09' WHERE `employee_id` = 'C02-E0009';
UPDATE `employees` SET `department_id` = 'C02-D10' WHERE `employee_id` = 'C02-E0010';
UPDATE `employees` SET `department_id` = 'C02-D11' WHERE `employee_id` = 'C02-E0011';
UPDATE `employees` SET `department_id` = 'C03-D01' WHERE `employee_id` = 'C03-E0001';
UPDATE `employees` SET `department_id` = 'C03-D02' WHERE `employee_id` = 'C03-E0002';
UPDATE `employees` SET `department_id` = 'C03-D03' WHERE `employee_id` = 'C03-E0003';
UPDATE `employees` SET `department_id` = 'C03-D04' WHERE `employee_id` = 'C03-E0004';
UPDATE `employees` SET `department_id` = 'C03-D05' WHERE `employee_id` = 'C03-E0005';
UPDATE `employees` SET `department_id` = 'C03-D06' WHERE `employee_id` = 'C03-E0006';
UPDATE `employees` SET `department_id` = 'C03-D07' WHERE `employee_id` = 'C03-E0007';
UPDATE `employees` SET `department_id` = 'C03-D08' WHERE `employee_id` = 'C03-E0008';
UPDATE `employees` SET `department_id` = 'C03-D09' WHERE `employee_id` = 'C03-E0009';
UPDATE `employees` SET `department_id` = 'C03-D10' WHERE `employee_id` = 'C03-E0010';
UPDATE `employees` SET `department_id` = 'C03-D11' WHERE `employee_id` = 'C03-E0011';
UPDATE `employees` SET `department_id` = 'C04-D01' WHERE `employee_id` = 'C04-E0001';
UPDATE `employees` SET `department_id` = 'C04-D02' WHERE `employee_id` = 'C04-E0002';
UPDATE `employees` SET `department_id` = 'C04-D03' WHERE `employee_id` = 'C04-E0003';
UPDATE `employees` SET `department_id` = 'C04-D04' WHERE `employee_id` = 'C04-E0004';
UPDATE `employees` SET `department_id` = 'C04-D05' WHERE `employee_id` = 'C04-E0005';
UPDATE `employees` SET `department_id` = 'C04-D06' WHERE `employee_id` = 'C04-E0006';
UPDATE `employees` SET `department_id` = 'C04-D07' WHERE `employee_id` = 'C04-E0007';
UPDATE `employees` SET `department_id` = 'C04-D08' WHERE `employee_id` = 'C04-E0008';
UPDATE `employees` SET `department_id` = 'C04-D09' WHERE `employee_id` = 'C04-E0009';
UPDATE `employees` SET `department_id` = 'C04-D10' WHERE `employee_id` = 'C04-E0010';
UPDATE `employees` SET `department_id` = 'C04-D11' WHERE `employee_id` = 'C04-E0011';
UPDATE `employees` SET `department_id` = 'C05-D01' WHERE `employee_id` = 'C05-E0001';
UPDATE `employees` SET `department_id` = 'C05-D02' WHERE `employee_id` = 'C05-E0002';
UPDATE `employees` SET `department_id` = 'C05-D03' WHERE `employee_id` = 'C05-E0003';
UPDATE `employees` SET `department_id` = 'C05-D04' WHERE `employee_id` = 'C05-E0004';
UPDATE `employees` SET `department_id` = 'C05-D05' WHERE `employee_id` = 'C05-E0005';
UPDATE `employees` SET `department_id` = 'C05-D06' WHERE `employee_id` = 'C05-E0006';
UPDATE `employees` SET `department_id` = 'C05-D07' WHERE `employee_id` = 'C05-E0007';
UPDATE `employees` SET `department_id` = 'C05-D08' WHERE `employee_id` = 'C05-E0008';
UPDATE `employees` SET `department_id` = 'C05-D09' WHERE `employee_id` = 'C05-E0009';
UPDATE `employees` SET `department_id` = 'C05-D10' WHERE `employee_id` = 'C05-E0010';
UPDATE `employees` SET `department_id` = 'C05-D11' WHERE `employee_id` = 'C05-E0011';
UPDATE `employees` SET `department_id` = 'C06-D01' WHERE `employee_id` = 'C06-E0001';
UPDATE `employees` SET `department_id` = 'C06-D02' WHERE `employee_id` = 'C06-E0002';
UPDATE `employees` SET `department_id` = 'C06-D03' WHERE `employee_id` = 'C06-E0003';
UPDATE `employees` SET `department_id` = 'C06-D04' WHERE `employee_id` = 'C06-E0004';
UPDATE `employees` SET `department_id` = 'C06-D05' WHERE `employee_id` = 'C06-E0005';
UPDATE `employees` SET `department_id` = 'C06-D06' WHERE `employee_id` = 'C06-E0006';
UPDATE `employees` SET `department_id` = 'C06-D07' WHERE `employee_id` = 'C06-E0007';
UPDATE `employees` SET `department_id` = 'C06-D08' WHERE `employee_id` = 'C06-E0008';
UPDATE `employees` SET `department_id` = 'C07-D01' WHERE `employee_id` = 'C07-E0001';
UPDATE `employees` SET `department_id` = 'C07-D02' WHERE `employee_id` = 'C07-E0002';
UPDATE `employees` SET `department_id` = 'C07-D03' WHERE `employee_id` = 'C07-E0003';
UPDATE `employees` SET `department_id` = 'C07-D04' WHERE `employee_id` = 'C07-E0004';
UPDATE `employees` SET `department_id` = 'C07-D05' WHERE `employee_id` = 'C07-E0005';
UPDATE `employees` SET `department_id` = 'C07-D06' WHERE `employee_id` = 'C07-E0006';
UPDATE `employees` SET `department_id` = 'C07-D07' WHERE `employee_id` = 'C07-E0007';
UPDATE `employees` SET `department_id` = 'C07-D08' WHERE `employee_id` = 'C07-E0008';
UPDATE `employees` SET `department_id` = 'C08-D01' WHERE `employee_id` = 'C08-E0001';
UPDATE `employees` SET `department_id` = 'C08-D02' WHERE `employee_id` = 'C08-E0002';
UPDATE `employees` SET `department_id` = 'C08-D03' WHERE `employee_id` = 'C08-E0003';
UPDATE `employees` SET `department_id` = 'C08-D04' WHERE `employee_id` = 'C08-E0004';
UPDATE `employees` SET `department_id` = 'C08-D05' WHERE `employee_id` = 'C08-E0005';
UPDATE `employees` SET `department_id` = 'C08-D06' WHERE `employee_id` = 'C08-E0006';
UPDATE `employees` SET `department_id` = 'C08-D07' WHERE `employee_id` = 'C08-E0007';
UPDATE `employees` SET `department_id` = 'C08-D08' WHERE `employee_id` = 'C08-E0008';
UPDATE `employees` SET `department_id` = 'C09-D01' WHERE `employee_id` = 'C09-E0001';
UPDATE `employees` SET `department_id` = 'C09-D02' WHERE `employee_id` = 'C09-E0002';
UPDATE `employees` SET `department_id` = 'C09-D03' WHERE `employee_id` = 'C09-E0003';
UPDATE `employees` SET `department_id` = 'C09-D04' WHERE `employee_id` = 'C09-E0004';
UPDATE `employees` SET `department_id` = 'C09-D05' WHERE `employee_id` = 'C09-E0005';
UPDATE `employees` SET `department_id` = 'C09-D06' WHERE `employee_id` = 'C09-E0006';
UPDATE `employees` SET `department_id` = 'C09-D07' WHERE `employee_id` = 'C09-E0007';
UPDATE `employees` SET `department_id` = 'C09-D08' WHERE `employee_id` = 'C09-E0008';
UPDATE `employees` SET `department_id` = 'C09-D09' WHERE `employee_id` = 'C09-E0009';
UPDATE `employees` SET `department_id` = 'C10-D01' WHERE `employee_id` = 'C10-E0001';
UPDATE `employees` SET `department_id` = 'C10-D02' WHERE `employee_id` = 'C10-E0002';
UPDATE `employees` SET `department_id` = 'C10-D03' WHERE `employee_id` = 'C10-E0003';
UPDATE `employees` SET `department_id` = 'C10-D04' WHERE `employee_id` = 'C10-E0004';
UPDATE `employees` SET `department_id` = 'C10-D05' WHERE `employee_id` = 'C10-E0005';
UPDATE `employees` SET `department_id` = 'C10-D06' WHERE `employee_id` = 'C10-E0006';
UPDATE `employees` SET `department_id` = 'C10-D07' WHERE `employee_id` = 'C10-E0007';
UPDATE `employees` SET `department_id` = 'C10-D08' WHERE `employee_id` = 'C10-E0008';
UPDATE `employees` SET `department_id` = 'C10-D09' WHERE `employee_id` = 'C10-E0009';
UPDATE `employees` SET `department_id` = 'C11-D01' WHERE `employee_id` = 'C11-E0001';
UPDATE `employees` SET `department_id` = 'C11-D02' WHERE `employee_id` = 'C11-E0002';
UPDATE `employees` SET `department_id` = 'C11-D03' WHERE `employee_id` = 'C11-E0003';
UPDATE `employees` SET `department_id` = 'C11-D04' WHERE `employee_id` = 'C11-E0004';
UPDATE `employees` SET `department_id` = 'C11-D05' WHERE `employee_id` = 'C11-E0005';
UPDATE `employees` SET `department_id` = 'C11-D06' WHERE `employee_id` = 'C11-E0006';
UPDATE `employees` SET `department_id` = 'C11-D07' WHERE `employee_id` = 'C11-E0007';
UPDATE `employees` SET `department_id` = 'C11-D08' WHERE `employee_id` = 'C11-E0008';
UPDATE `employees` SET `department_id` = 'C11-D09' WHERE `employee_id` = 'C11-E0009';
UPDATE `employees` SET `department_id` = 'C12-D01' WHERE `employee_id` = 'C12-E0001';
UPDATE `employees` SET `department_id` = 'C12-D02' WHERE `employee_id` = 'C12-E0002';
UPDATE `employees` SET `department_id` = 'C12-D03' WHERE `employee_id` = 'C12-E0003';
UPDATE `employees` SET `department_id` = 'C12-D04' WHERE `employee_id` = 'C12-E0004';
UPDATE `employees` SET `department_id` = 'C12-D05' WHERE `employee_id` = 'C12-E0005';
UPDATE `employees` SET `department_id` = 'C12-D06' WHERE `employee_id` = 'C12-E0006';
UPDATE `employees` SET `department_id` = 'C12-D07' WHERE `employee_id` = 'C12-E0007';
UPDATE `employees` SET `department_id` = 'C12-D08' WHERE `employee_id` = 'C12-E0008';
UPDATE `employees` SET `department_id` = 'C12-D09' WHERE `employee_id` = 'C12-E0009';
UPDATE `employees` SET `department_id` = 'C13-D01' WHERE `employee_id` = 'C13-E0001';
UPDATE `employees` SET `department_id` = 'C13-D02' WHERE `employee_id` = 'C13-E0002';
UPDATE `employees` SET `department_id` = 'C13-D03' WHERE `employee_id` = 'C13-E0003';
UPDATE `employees` SET `department_id` = 'C13-D04' WHERE `employee_id` = 'C13-E0004';
UPDATE `employees` SET `department_id` = 'C13-D05' WHERE `employee_id` = 'C13-E0005';
UPDATE `employees` SET `department_id` = 'C13-D06' WHERE `employee_id` = 'C13-E0006';
UPDATE `employees` SET `department_id` = 'C13-D07' WHERE `employee_id` = 'C13-E0007';
UPDATE `employees` SET `department_id` = 'C13-D08' WHERE `employee_id` = 'C13-E0008';
UPDATE `employees` SET `department_id` = 'C13-D09' WHERE `employee_id` = 'C13-E0009';
UPDATE `employees` SET `department_id` = 'C14-D01' WHERE `employee_id` = 'C14-E0001';
UPDATE `employees` SET `department_id` = 'C14-D02' WHERE `employee_id` = 'C14-E0002';
UPDATE `employees` SET `department_id` = 'C14-D03' WHERE `employee_id` = 'C14-E0003';
UPDATE `employees` SET `department_id` = 'C14-D04' WHERE `employee_id` = 'C14-E0004';
UPDATE `employees` SET `department_id` = 'C14-D05' WHERE `employee_id` = 'C14-E0005';
UPDATE `employees` SET `department_id` = 'C14-D06' WHERE `employee_id` = 'C14-E0006';
UPDATE `employees` SET `department_id` = 'C14-D07' WHERE `employee_id` = 'C14-E0007';
UPDATE `employees` SET `department_id` = 'C14-D08' WHERE `employee_id` = 'C14-E0008';
UPDATE `employees` SET `department_id` = 'C14-D09' WHERE `employee_id` = 'C14-E0009';
UPDATE `employees` SET `department_id` = 'C15-D01' WHERE `employee_id` = 'C15-E0001';
UPDATE `employees` SET `department_id` = 'C15-D02' WHERE `employee_id` = 'C15-E0002';
UPDATE `employees` SET `department_id` = 'C15-D03' WHERE `employee_id` = 'C15-E0003';
UPDATE `employees` SET `department_id` = 'C15-D04' WHERE `employee_id` = 'C15-E0004';
UPDATE `employees` SET `department_id` = 'C15-D05' WHERE `employee_id` = 'C15-E0005';
UPDATE `employees` SET `department_id` = 'C15-D06' WHERE `employee_id` = 'C15-E0006';
UPDATE `employees` SET `department_id` = 'C15-D07' WHERE `employee_id` = 'C15-E0007';
UPDATE `employees` SET `department_id` = 'C16-D01' WHERE `employee_id` = 'C16-E0001';
UPDATE `employees` SET `department_id` = 'C16-D02' WHERE `employee_id` = 'C16-E0002';
UPDATE `employees` SET `department_id` = 'C16-D03' WHERE `employee_id` = 'C16-E0003';
UPDATE `employees` SET `department_id` = 'C16-D04' WHERE `employee_id` = 'C16-E0004';
UPDATE `employees` SET `department_id` = 'C16-D05' WHERE `employee_id` = 'C16-E0005';
UPDATE `employees` SET `department_id` = 'C16-D06' WHERE `employee_id` = 'C16-E0006';
UPDATE `employees` SET `department_id` = 'C16-D07' WHERE `employee_id` = 'C16-E0007';
UPDATE `employees` SET `department_id` = 'C17-D01' WHERE `employee_id` = 'C17-E0001';
UPDATE `employees` SET `department_id` = 'C17-D02' WHERE `employee_id` = 'C17-E0002';
UPDATE `employees` SET `department_id` = 'C17-D03' WHERE `employee_id` = 'C17-E0003';
UPDATE `employees` SET `department_id` = 'C17-D04' WHERE `employee_id` = 'C17-E0004';
UPDATE `employees` SET `department_id` = 'C17-D05' WHERE `employee_id` = 'C17-E0005';
UPDATE `employees` SET `department_id` = 'C17-D06' WHERE `employee_id` = 'C17-E0006';
UPDATE `employees` SET `department_id` = 'C17-D07' WHERE `employee_id` = 'C17-E0007';
UPDATE `employees` SET `department_id` = 'C18-D01' WHERE `employee_id` = 'C18-E0001';
UPDATE `employees` SET `department_id` = 'C18-D02' WHERE `employee_id` = 'C18-E0002';
UPDATE `employees` SET `department_id` = 'C18-D03' WHERE `employee_id` = 'C18-E0003';
UPDATE `employees` SET `department_id` = 'C18-D04' WHERE `employee_id` = 'C18-E0004';
UPDATE `employees` SET `department_id` = 'C18-D05' WHERE `employee_id` = 'C18-E0005';
UPDATE `employees` SET `department_id` = 'C18-D06' WHERE `employee_id` = 'C18-E0006';
UPDATE `employees` SET `department_id` = 'C18-D07' WHERE `employee_id` = 'C18-E0007';

COMMIT;

-- Restaura o modo seguro da sessão
SET SQL_SAFE_UPDATES = 1;
SHOW COLUMNS FROM employees;
