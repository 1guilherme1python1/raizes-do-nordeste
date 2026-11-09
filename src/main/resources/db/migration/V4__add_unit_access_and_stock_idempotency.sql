CREATE TABLE app.usuario_unidade (
    usuario_id UUID NOT NULL,
    unidade_id UUID NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (usuario_id, unidade_id),
    CONSTRAINT fk_usuario_unidade_usuario FOREIGN KEY (usuario_id) REFERENCES app.usuario (id),
    CONSTRAINT fk_usuario_unidade_unidade FOREIGN KEY (unidade_id) REFERENCES app.unidade (id)
);

ALTER TABLE app.movimentacao_estoque
    ADD COLUMN unidade_id UUID,
    ADD COLUMN responsavel_usuario_id UUID,
    ADD COLUMN idempotency_key VARCHAR(100),
    ADD COLUMN requisicao_hash VARCHAR(64);

UPDATE app.movimentacao_estoque movimentacao
SET unidade_id = estoque.unidade_id
FROM app.estoque estoque
WHERE estoque.id = movimentacao.estoque_id;

ALTER TABLE app.movimentacao_estoque
    ALTER COLUMN unidade_id SET NOT NULL,
    ADD CONSTRAINT fk_movimentacao_unidade FOREIGN KEY (unidade_id) REFERENCES app.unidade (id),
    ADD CONSTRAINT fk_movimentacao_responsavel FOREIGN KEY (responsavel_usuario_id) REFERENCES app.usuario (id),
    ADD CONSTRAINT ck_movimentacao_idempotencia_integral CHECK (
        (responsavel_usuario_id IS NULL AND idempotency_key IS NULL AND requisicao_hash IS NULL)
        OR
        (responsavel_usuario_id IS NOT NULL AND idempotency_key IS NOT NULL AND requisicao_hash IS NOT NULL)
    ),
    ADD CONSTRAINT uk_movimentacao_unidade_idempotencia UNIQUE (unidade_id, idempotency_key);

CREATE INDEX idx_usuario_unidade_unidade ON app.usuario_unidade (unidade_id, usuario_id);
CREATE INDEX idx_movimentacao_unidade_data ON app.movimentacao_estoque (unidade_id, criado_em DESC);
