ALTER TABLE app.pedido
    ADD COLUMN criado_por_usuario_id UUID,
    ADD COLUMN forma_pagamento VARCHAR(20),
    ADD COLUMN reserva_expira_em TIMESTAMPTZ,
    ADD COLUMN idempotency_key VARCHAR(100),
    ADD COLUMN requisicao_hash VARCHAR(64),
    ADD COLUMN versao BIGINT NOT NULL DEFAULT 0;

ALTER TABLE app.pedido DROP CONSTRAINT ck_pedido_status;

ALTER TABLE app.pedido
    ADD CONSTRAINT fk_pedido_criado_por FOREIGN KEY (criado_por_usuario_id) REFERENCES app.usuario (id),
    ADD CONSTRAINT ck_pedido_status CHECK (status IN (
        'AGUARDANDO_PAGAMENTO', 'CONFIRMADO', 'EM_PREPARO', 'PRONTO', 'ENTREGUE', 'CANCELADO', 'EXPIRADO'
    )),
    ADD CONSTRAINT ck_pedido_forma_pagamento CHECK (forma_pagamento IS NULL OR forma_pagamento = 'MOCK'),
    ADD CONSTRAINT ck_pedido_reserva_integral CHECK (
        (criado_por_usuario_id IS NULL AND forma_pagamento IS NULL AND reserva_expira_em IS NULL
            AND idempotency_key IS NULL AND requisicao_hash IS NULL)
        OR
        (criado_por_usuario_id IS NOT NULL AND forma_pagamento IS NOT NULL AND reserva_expira_em IS NOT NULL
            AND idempotency_key IS NOT NULL AND requisicao_hash IS NOT NULL)
    ),
    ADD CONSTRAINT uk_pedido_solicitante_unidade_idempotencia
        UNIQUE (criado_por_usuario_id, unidade_id, idempotency_key);

ALTER TABLE app.movimentacao_estoque
    ADD COLUMN pedido_id UUID,
    ADD COLUMN item_pedido_id UUID,
    ADD CONSTRAINT fk_movimentacao_pedido FOREIGN KEY (pedido_id) REFERENCES app.pedido (id),
    ADD CONSTRAINT fk_movimentacao_item_pedido FOREIGN KEY (item_pedido_id) REFERENCES app.item_pedido (id),
    ADD CONSTRAINT ck_movimentacao_referencia_pedido CHECK (
        (pedido_id IS NULL AND item_pedido_id IS NULL)
        OR (pedido_id IS NOT NULL AND item_pedido_id IS NOT NULL)
    );

-- Movimentos automáticos de expiração não possuem usuário executor, mas continuam
-- integralmente idempotentes e rastreados pelo pedido/item.
ALTER TABLE app.movimentacao_estoque DROP CONSTRAINT ck_movimentacao_idempotencia_integral;
ALTER TABLE app.movimentacao_estoque
    ADD CONSTRAINT ck_movimentacao_idempotencia_integral CHECK (
        (responsavel_usuario_id IS NULL AND idempotency_key IS NULL AND requisicao_hash IS NULL)
        OR
        (idempotency_key IS NOT NULL AND requisicao_hash IS NOT NULL
            AND (responsavel_usuario_id IS NOT NULL OR pedido_id IS NOT NULL))
    );

CREATE INDEX idx_pedido_expiracao
    ON app.pedido (reserva_expira_em, id)
    WHERE status = 'AGUARDANDO_PAGAMENTO';
CREATE INDEX idx_pedido_cliente_data ON app.pedido (cliente_id, criado_em DESC);
CREATE INDEX idx_pedido_criado_por_data ON app.pedido (criado_por_usuario_id, criado_em DESC);
CREATE INDEX idx_movimentacao_pedido ON app.movimentacao_estoque (pedido_id, item_pedido_id);
