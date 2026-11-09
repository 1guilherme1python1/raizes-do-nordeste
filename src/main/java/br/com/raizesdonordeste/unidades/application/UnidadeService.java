package br.com.raizesdonordeste.unidades.application;

import java.util.UUID;
import br.com.raizesdonordeste.shared.application.ConflitoException;
import br.com.raizesdonordeste.shared.application.RecursoNaoEncontradoException;
import br.com.raizesdonordeste.unidades.domain.Unidade;
import br.com.raizesdonordeste.unidades.infrastructure.persistence.UnidadeEntity;
import br.com.raizesdonordeste.unidades.infrastructure.persistence.UnidadeJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UnidadeService {
    private final UnidadeJpaRepository repository;
    public UnidadeService(UnidadeJpaRepository repository) { this.repository = repository; }

    @Transactional
    public Unidade criar(String codigo, String nome) {
        if (codigo == null || codigo.isBlank() || nome == null || nome.isBlank()) throw new IllegalArgumentException("Código e nome são obrigatórios");
        var codigoNormalizado = codigo.strip().toUpperCase();
        if (repository.existsByCodigo(codigoNormalizado)) throw new ConflitoException("Código de unidade já cadastrado");
        var entity = new UnidadeEntity(UUID.randomUUID(), codigoNormalizado, nome.strip(), true, null, null);
        return repository.saveAndFlush(entity).toDomain();
    }

    @Transactional(readOnly = true)
    public Page<Unidade> listar(Boolean ativa, String nome, Pageable pageable) {
        Specification<UnidadeEntity> spec = (root, query, cb) -> cb.conjunction();
        if (ativa != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("ativa"), ativa));
        if (nome != null && !nome.isBlank()) {
            var termo = "%" + nome.strip().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("nome")), termo));
        }
        return repository.findAll(spec, pageable).map(UnidadeEntity::toDomain);
    }

    @Transactional(readOnly = true)
    public Unidade buscar(UUID id) { return buscarEntity(id).toDomain(); }

    @Transactional
    public Unidade atualizar(UUID id, String nome, Boolean ativa) {
        var entity = buscarEntity(id);
        if (nome != null && nome.isBlank()) throw new IllegalArgumentException("Nome da unidade não pode ser vazio");
        entity.atualizar(nome == null ? entity.getNome() : nome.strip(), ativa == null ? entity.isAtiva() : ativa);
        return repository.saveAndFlush(entity).toDomain();
    }

    private UnidadeEntity buscarEntity(UUID id) {
        return repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Unidade"));
    }
}
