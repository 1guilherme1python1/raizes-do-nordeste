ALTER TABLE app.usuario
    ADD COLUMN email_normalizado VARCHAR(254),
    ADD COLUMN senha_hash VARCHAR(100),
    ADD COLUMN perfil VARCHAR(20);

ALTER TABLE app.usuario
    ADD CONSTRAINT ck_usuario_email_normalizado CHECK (
        email_normalizado IS NULL OR email_normalizado = LOWER(BTRIM(email_normalizado))
    ),
    ADD CONSTRAINT ck_usuario_perfil CHECK (
        perfil IS NULL OR perfil IN ('ADMIN', 'GERENTE', 'ATENDENTE', 'COZINHA', 'CLIENTE')
    ),
    ADD CONSTRAINT ck_usuario_credenciais_completas CHECK (
        (email_normalizado IS NULL AND senha_hash IS NULL AND perfil IS NULL)
        OR
        (email_normalizado IS NOT NULL AND senha_hash IS NOT NULL AND perfil IS NOT NULL)
    );

CREATE UNIQUE INDEX uk_usuario_email_normalizado
    ON app.usuario (email_normalizado)
    WHERE email_normalizado IS NOT NULL;

CREATE INDEX idx_usuario_perfil_ativo
    ON app.usuario (perfil, ativo)
    WHERE perfil IS NOT NULL;
