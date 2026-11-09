CREATE TABLE app.usuario (
    id UUID PRIMARY KEY,
    codigo VARCHAR(50) NOT NULL,
    nome_exibicao VARCHAR(150) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_usuario_codigo UNIQUE (codigo)
);

CREATE TABLE app.unidade (
    id UUID PRIMARY KEY,
    codigo VARCHAR(50) NOT NULL,
    nome VARCHAR(150) NOT NULL,
    ativa BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_unidade_codigo UNIQUE (codigo)
);

CREATE TABLE app.produto (
    id UUID PRIMARY KEY,
    sku VARCHAR(50) NOT NULL,
    nome VARCHAR(150) NOT NULL,
    descricao VARCHAR(500),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_produto_sku UNIQUE (sku)
);

CREATE TABLE app.produto_unidade (
    id UUID PRIMARY KEY,
    produto_id UUID NOT NULL,
    unidade_id UUID NOT NULL,
    preco NUMERIC(12,2) NOT NULL,
    disponivel BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_produto_unidade_produto FOREIGN KEY (produto_id) REFERENCES app.produto (id),
    CONSTRAINT fk_produto_unidade_unidade FOREIGN KEY (unidade_id) REFERENCES app.unidade (id),
    CONSTRAINT uk_produto_unidade UNIQUE (produto_id, unidade_id),
    CONSTRAINT uk_produto_unidade_identidade_unidade UNIQUE (id, unidade_id),
    CONSTRAINT uk_produto_unidade_identidade UNIQUE (id, produto_id, unidade_id),
    CONSTRAINT ck_produto_unidade_preco CHECK (preco >= 0)
);

CREATE TABLE app.estoque (
    id UUID PRIMARY KEY,
    produto_unidade_id UUID NOT NULL,
    unidade_id UUID NOT NULL,
    quantidade_fisica INTEGER NOT NULL DEFAULT 0,
    quantidade_reservada INTEGER NOT NULL DEFAULT 0,
    versao BIGINT NOT NULL DEFAULT 0,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_estoque_produto_unidade FOREIGN KEY (produto_unidade_id, unidade_id)
        REFERENCES app.produto_unidade (id, unidade_id),
    CONSTRAINT fk_estoque_unidade FOREIGN KEY (unidade_id) REFERENCES app.unidade (id),
    CONSTRAINT uk_estoque_produto_unidade UNIQUE (produto_unidade_id),
    CONSTRAINT ck_estoque_quantidade_fisica CHECK (quantidade_fisica >= 0),
    CONSTRAINT ck_estoque_quantidade_reservada CHECK (quantidade_reservada >= 0),
    CONSTRAINT ck_estoque_reserva_saldo CHECK (quantidade_reservada <= quantidade_fisica)
);

CREATE TABLE app.movimentacao_estoque (
    id UUID PRIMARY KEY,
    estoque_id UUID NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    quantidade INTEGER NOT NULL,
    quantidade_fisica_resultante INTEGER NOT NULL,
    quantidade_reservada_resultante INTEGER NOT NULL,
    motivo VARCHAR(250) NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_movimentacao_estoque FOREIGN KEY (estoque_id) REFERENCES app.estoque (id),
    CONSTRAINT ck_movimentacao_tipo CHECK (tipo IN ('ENTRADA', 'SAIDA', 'RESERVA', 'LIBERACAO', 'AJUSTE')),
    CONSTRAINT ck_movimentacao_quantidade CHECK (quantidade > 0),
    CONSTRAINT ck_movimentacao_fisica CHECK (quantidade_fisica_resultante >= 0),
    CONSTRAINT ck_movimentacao_reservada CHECK (
        quantidade_reservada_resultante >= 0
        AND quantidade_reservada_resultante <= quantidade_fisica_resultante
    )
);

CREATE TABLE app.pedido (
    id UUID PRIMARY KEY,
    unidade_id UUID NOT NULL,
    cliente_id UUID,
    canal_pedido VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,
    valor_total NUMERIC(12,2) NOT NULL,
    moeda VARCHAR(3) NOT NULL DEFAULT 'BRL',
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pedido_unidade FOREIGN KEY (unidade_id) REFERENCES app.unidade (id),
    CONSTRAINT fk_pedido_cliente FOREIGN KEY (cliente_id) REFERENCES app.usuario (id),
    CONSTRAINT uk_pedido_identidade_unidade UNIQUE (id, unidade_id),
    CONSTRAINT ck_pedido_canal CHECK (canal_pedido IN ('APP', 'TOTEM', 'BALCAO', 'PICKUP', 'WEB')),
    CONSTRAINT ck_pedido_status CHECK (status IN (
        'AGUARDANDO_PAGAMENTO', 'CONFIRMADO', 'EM_PREPARO', 'PRONTO', 'ENTREGUE', 'CANCELADO'
    )),
    CONSTRAINT ck_pedido_valor_total CHECK (valor_total >= 0),
    CONSTRAINT ck_pedido_moeda CHECK (moeda = 'BRL')
);

CREATE TABLE app.item_pedido (
    id UUID PRIMARY KEY,
    pedido_id UUID NOT NULL,
    unidade_id UUID NOT NULL,
    produto_unidade_id UUID NOT NULL,
    produto_id UUID NOT NULL,
    quantidade INTEGER NOT NULL,
    preco_unitario NUMERIC(12,2) NOT NULL,
    subtotal NUMERIC(12,2) NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_item_pedido_pedido FOREIGN KEY (pedido_id, unidade_id)
        REFERENCES app.pedido (id, unidade_id),
    CONSTRAINT fk_item_pedido_produto_unidade FOREIGN KEY (produto_unidade_id, produto_id, unidade_id)
        REFERENCES app.produto_unidade (id, produto_id, unidade_id),
    CONSTRAINT uk_item_pedido_produto UNIQUE (pedido_id, produto_unidade_id),
    CONSTRAINT ck_item_pedido_quantidade CHECK (quantidade > 0),
    CONSTRAINT ck_item_pedido_preco CHECK (preco_unitario >= 0),
    CONSTRAINT ck_item_pedido_subtotal CHECK (subtotal >= 0),
    CONSTRAINT ck_item_pedido_calculo CHECK (subtotal = ROUND(preco_unitario * quantidade, 2))
);

CREATE TABLE app.pagamento (
    id UUID PRIMARY KEY,
    pedido_id UUID NOT NULL,
    numero_tentativa INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    transacao_id VARCHAR(100) NOT NULL,
    solicitado_em TIMESTAMPTZ NOT NULL,
    resultado_em TIMESTAMPTZ,
    motivo_resultado VARCHAR(250),
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pagamento_pedido FOREIGN KEY (pedido_id) REFERENCES app.pedido (id),
    CONSTRAINT uk_pagamento_tentativa UNIQUE (pedido_id, numero_tentativa),
    CONSTRAINT uk_pagamento_transacao UNIQUE (transacao_id),
    CONSTRAINT ck_pagamento_tentativa CHECK (numero_tentativa > 0),
    CONSTRAINT ck_pagamento_status CHECK (status IN ('PENDENTE', 'APROVADO', 'RECUSADO')),
    CONSTRAINT ck_pagamento_resultado CHECK (
        (status = 'PENDENTE' AND resultado_em IS NULL AND motivo_resultado IS NULL)
        OR (status = 'APROVADO' AND resultado_em IS NOT NULL)
        OR (status = 'RECUSADO' AND resultado_em IS NOT NULL AND motivo_resultado IS NOT NULL)
    ),
    CONSTRAINT ck_pagamento_ordem_temporal CHECK (resultado_em IS NULL OR resultado_em >= solicitado_em)
);

CREATE INDEX idx_produto_unidade_unidade_disponivel
    ON app.produto_unidade (unidade_id, disponivel);
CREATE INDEX idx_estoque_unidade ON app.estoque (unidade_id);
CREATE INDEX idx_movimentacao_estoque_data ON app.movimentacao_estoque (estoque_id, criado_em DESC);
CREATE INDEX idx_pedido_unidade_data ON app.pedido (unidade_id, criado_em DESC);
CREATE INDEX idx_pedido_canal ON app.pedido (canal_pedido);
CREATE INDEX idx_pedido_status ON app.pedido (status);
CREATE INDEX idx_item_pedido_pedido ON app.item_pedido (pedido_id);
CREATE INDEX idx_pagamento_pedido_status ON app.pagamento (pedido_id, status);
