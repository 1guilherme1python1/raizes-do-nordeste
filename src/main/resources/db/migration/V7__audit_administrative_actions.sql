CREATE TABLE app.auditoria_administrativa (
    id UUID PRIMARY KEY,
    ator_id UUID REFERENCES app.usuario(id),
    origem VARCHAR(20) NOT NULL CHECK (origem IN ('USUARIO', 'SISTEMA')),
    acao VARCHAR(40) NOT NULL CHECK (acao IN ('CRIAR_USUARIO_INTERNO', 'ASSOCIAR_USUARIO_UNIDADE', 'DESASSOCIAR_USUARIO_UNIDADE')),
    usuario_alvo_id UUID NOT NULL REFERENCES app.usuario(id),
    unidade_id UUID REFERENCES app.unidade(id),
    ocorrido_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_auditoria_origem CHECK ((origem = 'USUARIO' AND ator_id IS NOT NULL) OR (origem = 'SISTEMA' AND ator_id IS NULL)),
    CONSTRAINT ck_auditoria_unidade CHECK (
        (acao = 'CRIAR_USUARIO_INTERNO' AND unidade_id IS NULL)
        OR (acao IN ('ASSOCIAR_USUARIO_UNIDADE', 'DESASSOCIAR_USUARIO_UNIDADE') AND unidade_id IS NOT NULL)
    )
);
CREATE INDEX idx_auditoria_administrativa_alvo ON app.auditoria_administrativa(usuario_alvo_id, ocorrido_em DESC);
