ALTER TABLE app.pagamento
    ADD COLUMN solicitante_id UUID REFERENCES app.usuario(id),
    ADD COLUMN idempotency_key VARCHAR(100),
    ADD COLUMN requisicao_hash VARCHAR(64),
    ADD COLUMN valor NUMERIC(12,2),
    ADD COLUMN moeda VARCHAR(3),
    ADD CONSTRAINT uk_pagamento_idempotencia UNIQUE (pedido_id, solicitante_id, idempotency_key),
    ADD CONSTRAINT ck_pagamento_dados_operacao CHECK (
        (solicitante_id IS NULL AND idempotency_key IS NULL AND requisicao_hash IS NULL AND valor IS NULL AND moeda IS NULL)
        OR (solicitante_id IS NOT NULL AND idempotency_key IS NOT NULL AND requisicao_hash IS NOT NULL AND valor >= 0 AND moeda = 'BRL')
    );

CREATE UNIQUE INDEX uk_pagamento_efetivo ON app.pagamento(pedido_id) WHERE solicitante_id IS NOT NULL;

CREATE TABLE app.pedido_transicao (
    id UUID PRIMARY KEY,
    pedido_id UUID NOT NULL REFERENCES app.pedido(id),
    ator_id UUID REFERENCES app.usuario(id),
    origem VARCHAR(20) NOT NULL,
    estado_anterior VARCHAR(30) NOT NULL,
    estado_novo VARCHAR(30) NOT NULL,
    motivo VARCHAR(100),
    ocorrido_em TIMESTAMPTZ NOT NULL
);
