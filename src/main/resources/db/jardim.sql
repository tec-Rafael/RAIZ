-- Estado do jogo separado das métricas AMS. Não modifica os registros de produtividade.
CREATE TABLE IF NOT EXISTS jardim_carteiras (
    employee_id VARCHAR(64) PRIMARY KEY REFERENCES employees(employee_id) ON DELETE CASCADE,
    moedas INTEGER NOT NULL DEFAULT 0 CHECK (moedas >= 0),
    adubos INTEGER NOT NULL DEFAULT 0 CHECK (adubos >= 0),
    versao BIGINT NOT NULL DEFAULT 0 CHECK (versao >= 0),
    periodo_atual SMALLINT NOT NULL DEFAULT 1 CHECK (periodo_atual BETWEEN 1 AND 5),
    decoracao VARCHAR(30) NOT NULL DEFAULT 'nenhuma'
)@@
CREATE TABLE IF NOT EXISTS jardim_periodos (
    employee_id VARCHAR(64) NOT NULL REFERENCES employees(employee_id) ON DELETE CASCADE,
    periodo SMALLINT NOT NULL CHECK (periodo BETWEEN 1 AND 5),
    dia INTEGER NOT NULL DEFAULT 0 CHECK (dia BETWEEN 0 AND 90),
    adubos_aplicados INTEGER NOT NULL DEFAULT 0 CHECK (adubos_aplicados BETWEEN 0 AND 16),
    encerrado BOOLEAN NOT NULL DEFAULT FALSE,
    ams_final DOUBLE PRECISION,
    nivel_final VARCHAR(20),
    PRIMARY KEY (employee_id, periodo)
)@@
CREATE TABLE IF NOT EXISTS jardim_recompensas (
    employee_id VARCHAR(64) NOT NULL REFERENCES employees(employee_id) ON DELETE CASCADE,
    data DATE NOT NULL,
    periodo SMALLINT NOT NULL CHECK (periodo BETWEEN 1 AND 5),
    dia INTEGER NOT NULL CHECK (dia BETWEEN 1 AND 90),
    moedas SMALLINT NOT NULL CHECK (moedas BETWEEN 0 AND 8),
    ams DOUBLE PRECISION,
    nivel VARCHAR(20),
    PRIMARY KEY (employee_id, data),
    UNIQUE (employee_id, periodo, dia)
)@@
CREATE TABLE IF NOT EXISTS jardim_itens (
    employee_id VARCHAR(64) NOT NULL REFERENCES employees(employee_id) ON DELETE CASCADE,
    item VARCHAR(30) NOT NULL,
    PRIMARY KEY (employee_id, item)
)@@
CREATE TABLE IF NOT EXISTS jardim_adubacoes (
    employee_id VARCHAR(64) NOT NULL REFERENCES employees(employee_id) ON DELETE CASCADE,
    periodo SMALLINT NOT NULL CHECK (periodo BETWEEN 1 AND 5),
    dia INTEGER NOT NULL CHECK (dia BETWEEN 1 AND 90),
    quantidade INTEGER NOT NULL CHECK (quantidade BETWEEN 1 AND 16),
    PRIMARY KEY (employee_id,periodo,dia)
)@@
CREATE TABLE IF NOT EXISTS jardim_conquistas (
    employee_id VARCHAR(64) NOT NULL REFERENCES employees(employee_id) ON DELETE CASCADE,
    codigo VARCHAR(40) NOT NULL,
    conquistada_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (employee_id, codigo)
)@@

-- O cuidado pode começar no dia zero, sem avançar o histórico nem conceder moedas.
ALTER TABLE jardim_adubacoes DROP CONSTRAINT IF EXISTS jardim_adubacoes_dia_check@@
ALTER TABLE jardim_adubacoes ADD CONSTRAINT jardim_adubacoes_dia_check CHECK (dia BETWEEN 0 AND 90)@@
ALTER TABLE jardim_carteiras ALTER COLUMN decoracao TYPE VARCHAR(200)@@
