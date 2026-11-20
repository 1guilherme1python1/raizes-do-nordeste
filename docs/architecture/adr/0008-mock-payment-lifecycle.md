# ADR 0008 — Pagamento mock e ciclo operacional

Pagamento, cancelamento e expiração adquirem `PESSIMISTIC_WRITE` sobre a linha do pedido antes de ler seu estado. Em seguida, os estoques são acessados em ordem crescente de UUID do produto. Essa ordem é compartilhada pelas instâncias através do PostgreSQL.

O simulador local recebe `APROVAR` ou `RECUSAR`; o valor e a moeda são obtidos do pedido. Uma tentativa efetiva por pedido é protegida por índice único. A chave e o hash são vinculados ao pedido e solicitante. A autorização antecede o replay.

A aprovação baixa físico e reservado; a recusa libera apenas reservado. O pagamento, os movimentos e a transição são gravados na mesma transação. A expiração durante solicitação retorna um resultado explícito, confirmado pela transação antes de ser convertido em HTTP 409 pelo controller, que não admite transação externa.

As únicas transições operacionais são CONFIRMADO → EM_PREPARO → PRONTO → ENTREGUE. Não há processamento externo, estorno ou nova tentativa nesta etapa.
