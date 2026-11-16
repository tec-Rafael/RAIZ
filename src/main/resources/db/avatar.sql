-- Avatar em pixel art de cada colaborador (configuração JSON versao 2 de public/js/avatarSprite.js).
-- origem: 'sorteado' (atribuído automaticamente), 'escolhido' (editado no perfil) ou 'fixo' (administrador).
-- Não altera nenhuma tabela de negócio; os avatares iniciais são gravados por AvataresIniciais.
CREATE TABLE IF NOT EXISTS avatares (
    employee_id VARCHAR(64) PRIMARY KEY REFERENCES employees(employee_id) ON DELETE CASCADE,
    genero VARCHAR(10) NOT NULL CHECK (genero IN ('masculino','feminino')),
    pele VARCHAR(3) NOT NULL CHECK (pele IN ('p1','p2','p3','p4','p5','p6','p7','p8')),
    olhos VARCHAR(3) NOT NULL CHECK (olhos IN ('o1','o2','o3','o4','o5','o6','o7','o8')),
    cabelo VARCHAR(12) NOT NULL CHECK (cabelo IN ('careca','curto','topete','espetado','cacheado','chanel','longo','rabo','coque','moicano')),
    cor_cabelo VARCHAR(3) NOT NULL CHECK (cor_cabelo IN ('c1','c2','c3','c4','c5','c6','c7','c8','c9','c10','c11','c12')),
    facial VARCHAR(12) NOT NULL CHECK (facial IN ('nenhum','bigode','cavanhaque','barbaCurta','barbaCheia')),
    sardas BOOLEAN NOT NULL DEFAULT FALSE,
    camisa VARCHAR(3) NOT NULL CHECK (camisa IN ('r1','r2','r3','r4','r5','r6','r7','r8','r9','r10','r11','r12')),
    calca VARCHAR(3) NOT NULL CHECK (calca IN ('k1','k2','k3','k4','k5','k6','k7','k8')),
    origem VARCHAR(10) NOT NULL DEFAULT 'sorteado' CHECK (origem IN ('sorteado','escolhido','fixo')),
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
)@@
