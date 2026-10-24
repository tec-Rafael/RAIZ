-- Migração aditiva e idempotente: preserva cadastros e histórico.
ALTER TABLE employees ADD COLUMN IF NOT EXISTS token VARCHAR(128)@@
ALTER TABLE employees ADD COLUMN IF NOT EXISTS token_expira_em TIMESTAMPTZ@@
CREATE TABLE IF NOT EXISTS raiz_migracoes (
    versao VARCHAR(100) PRIMARY KEY,
    aplicada_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
)@@
CREATE UNIQUE INDEX IF NOT EXISTS employees_admin_unico
    ON employees ((lower(posicao))) WHERE lower(posicao) = 'administrador'@@
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'employees_admin_reservado') THEN
        ALTER TABLE employees ADD CONSTRAINT employees_admin_reservado CHECK (
            (lower(posicao) = 'administrador' AND employee_id = 'ROOT-E0001' AND lower(email) = 'raiz@gmail.com')
            OR (coalesce(lower(posicao), '') <> 'administrador' AND lower(email) <> 'raiz@gmail.com')
        );
    END IF;
END $$@@
