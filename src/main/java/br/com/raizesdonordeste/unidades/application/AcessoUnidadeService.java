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

@Service
public class AcessoUnidadeService {
    private final UsuarioUnidadeJpaRepository repository;
    private final UnidadeService unidadeService;
    private final UsuarioService usuarioService;
    public AcessoUnidadeService(UsuarioUnidadeJpaRepository repository, UnidadeService unidadeService,
            UsuarioService usuarioService) {
        this.repository = repository; this.unidadeService = unidadeService; this.usuarioService = usuarioService;
    }

    @Transactional
    public void associar(UUID unidadeId, UUID usuarioId) {
        unidadeService.buscar(unidadeId);
        var usuario = usuarioService.buscarPorId(usuarioId);
        if (usuario.perfil() == Perfil.CLIENTE || usuario.perfil() == Perfil.ADMIN) {
            throw new ConflitoException("Somente funcionários operacionais podem ser vinculados a unidades");
        }
        if (!repository.existsByUsuarioIdAndUnidadeId(usuarioId, unidadeId)) {
            repository.saveAndFlush(new UsuarioUnidadeEntity(usuarioId, unidadeId));
        }
    }

    @Transactional
    public void desassociar(UUID unidadeId, UUID usuarioId) {
        if (!repository.existsByUsuarioIdAndUnidadeId(usuarioId, unidadeId)) {
            throw new br.com.raizesdonordeste.shared.application.RecursoNaoEncontradoException("Vínculo operacional");
        }
        repository.deleteByUsuarioIdAndUnidadeId(usuarioId, unidadeId);
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
