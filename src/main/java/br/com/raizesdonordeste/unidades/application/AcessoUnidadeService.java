package br.com.raizesdonordeste.unidades.application;

import java.util.UUID;
import java.util.List;
import br.com.raizesdonordeste.shared.application.AcessoNegadoException;
import br.com.raizesdonordeste.shared.application.ConflitoException;
import br.com.raizesdonordeste.unidades.infrastructure.persistence.UsuarioUnidadeEntity;
import br.com.raizesdonordeste.unidades.infrastructure.persistence.UsuarioUnidadeJpaRepository;
import br.com.raizesdonordeste.usuarios.application.UsuarioService;
import br.com.raizesdonordeste.usuarios.domain.Perfil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;

@Service
public class AcessoUnidadeService {
    private final UsuarioUnidadeJpaRepository repository;
    private final UnidadeService unidadeService;
    private final UsuarioService usuarioService;
    private final JdbcTemplate jdbc;
    public AcessoUnidadeService(UsuarioUnidadeJpaRepository repository, UnidadeService unidadeService,
            UsuarioService usuarioService, JdbcTemplate jdbc) {
        this.repository = repository; this.unidadeService = unidadeService; this.usuarioService = usuarioService; this.jdbc=jdbc;
    }

    @Transactional
    public void associar(UUID unidadeId, UUID usuarioId) {
        associarInterno(unidadeId, usuarioId, null);
    }

    @Transactional
    public void associar(UUID unidadeId, UUID usuarioId, UUID atorId) {
        associarInterno(unidadeId, usuarioId, atorId);
    }

    private void associarInterno(UUID unidadeId, UUID usuarioId, UUID atorId) {
        unidadeService.buscar(unidadeId);
        var usuario = usuarioService.buscarPorId(usuarioId);
        if (usuario.perfil() == Perfil.CLIENTE || usuario.perfil() == Perfil.ADMIN) {
            throw new ConflitoException("Somente funcionários operacionais podem ser vinculados a unidades");
        }
        if (!repository.existsByUsuarioIdAndUnidadeId(usuarioId, unidadeId)) {
            repository.saveAndFlush(new UsuarioUnidadeEntity(usuarioId, unidadeId));
            auditar(atorId, usuarioId, unidadeId, "ASSOCIAR_USUARIO_UNIDADE");
        }
    }

    @Transactional
    public void desassociar(UUID unidadeId, UUID usuarioId) {
        desassociarInterno(unidadeId, usuarioId, null);
    }

    @Transactional
    public void desassociar(UUID unidadeId, UUID usuarioId, UUID atorId) {
        desassociarInterno(unidadeId, usuarioId, atorId);
    }

    private void desassociarInterno(UUID unidadeId, UUID usuarioId, UUID atorId) {
        if (!repository.existsByUsuarioIdAndUnidadeId(usuarioId, unidadeId)) {
            throw new br.com.raizesdonordeste.shared.application.RecursoNaoEncontradoException("Vínculo operacional");
        }
        repository.deleteByUsuarioIdAndUnidadeId(usuarioId, unidadeId);
        auditar(atorId, usuarioId, unidadeId, "DESASSOCIAR_USUARIO_UNIDADE");
    }

    private void auditar(UUID atorId, UUID usuarioId, UUID unidadeId, String acao) {
        jdbc.update("insert into app.auditoria_administrativa(id,ator_id,origem,acao,usuario_alvo_id,unidade_id) values (?,?,?,?,?,?)",
                UUID.randomUUID(), atorId, atorId == null ? "SISTEMA" : "USUARIO", acao, usuarioId, unidadeId);
    }

    @Transactional(readOnly = true)
    public void validarOperacao(UUID unidadeId, UUID usuarioId, boolean admin, boolean gerente) {
        if (admin) return;
        if (!gerente || !repository.existsByUsuarioIdAndUnidadeId(usuarioId, unidadeId)) throw new AcessoNegadoException();
    }

    @Transactional(readOnly = true)
    public boolean possuiVinculo(UUID unidadeId, UUID usuarioId) {
        return repository.existsByUsuarioIdAndUnidadeId(usuarioId, unidadeId);
    }

    @Transactional(readOnly = true)
    public List<UUID> unidadesDoUsuario(UUID usuarioId) {
        return repository.findByUsuarioId(usuarioId).stream().map(UsuarioUnidadeEntity::getUnidadeId).toList();
    }
}
