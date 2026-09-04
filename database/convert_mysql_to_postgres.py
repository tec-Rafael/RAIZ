from pathlib import Path
import re
src = Path(__file__).with_name('ai_usage_mysql_updated.sql').read_text(encoding='utf-8')
# Remove comandos e opções exclusivos do MySQL.
src = re.sub(r'(?m)^\s*(CREATE DATABASE.*|USE `[^`]+`;|SET FOREIGN_KEY_CHECKS.*;|SET SQL_MODE.*;|SET time_zone.*;)$', '', src)
src = src.replace('`', '')
src = re.sub(r'\) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;', ');', src)
src = re.sub(r'\bDATETIME\b', 'TIMESTAMP', src, flags=re.I)
src = re.sub(r'\bDOUBLE\b', 'DOUBLE PRECISION', src, flags=re.I)
src = re.sub(r'\bTINYINT\b', 'SMALLINT', src, flags=re.I)
src = re.sub(r'\bactive\s+SMALLINT\b', 'active BOOLEAN', src, flags=re.I)
# O dataset original usa 0/1 em employees.active; PostgreSQL exige TRUE/FALSE.
def employees_booleans(match):
    statement = match.group(0)
    statement = re.sub(r",\s*1,\s*'", ", TRUE, '", statement)
    statement = re.sub(r",\s*0,\s*'", ", FALSE, '", statement)
    return statement
src = re.sub(r'INSERT INTO employees.*?;', employees_booleans, src, flags=re.I | re.S)
# PostgreSQL não aceita os índices nomeados com a mesma sintaxe quando já existem; a sintaxe abaixo é compatível.
# Garante a coluna exigida pelo módulo de funcionários.
extra = """

ALTER TABLE employees ADD COLUMN IF NOT EXISTS posicao VARCHAR(20) NOT NULL DEFAULT 'colaborador';
UPDATE employees SET posicao = CASE WHEN employee_id IN (
  SELECT employee_id FROM employees ORDER BY company_id, employee_id
) THEN 'colaborador' ELSE 'colaborador' END;
"""
# Migração de classificação: os IDs presentes no script CriacaoPosicao são gestores.
classification = Path(__file__).with_name('CriacaoPosicao.sql').read_text(encoding='utf-8')
ids = re.findall(r"'([A-Z0-9]+-E[0-9]+)'", classification)
ids = list(dict.fromkeys(ids))
if ids:
    values = ', '.join("'%s'" % x.replace("'", "''") for x in ids)
    extra += f"\nUPDATE employees SET posicao = 'gestor' WHERE employee_id IN ({values});\n"
marker = "\n-- Carregamento do dataset fornecido. Os dados possuem referências cruzadas;\n-- as constraints são reativadas ao final do carregamento.\nSET session_replication_role = replica;\n"
Path(__file__).with_name('001_ai_usage.sql').write_text(src + marker + extra + "\nSET session_replication_role = origin;\n", encoding='utf-8')
print(f'PostgreSQL gerado: {len(ids)} gestores mapeados')
