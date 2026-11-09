package br.com.raizesdonordeste.usuarios.application;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import br.com.raizesdonordeste.usuarios.domain.Perfil;
import br.com.raizesdonordeste.usuarios.domain.Usuario;
import br.com.raizesdonordeste.usuarios.infrastructure.persistence.UsuarioEntity;
import br.com.raizesdonordeste.usuarios.infrastructure.persistence.UsuarioJpaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    private final UsuarioJpaRepository repository;
    private final EmailNormalizer emailNormalizer;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioJpaRepository repository, EmailNormalizer emailNormalizer,
            PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.emailNormalizer = emailNormalizer;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Usuario registrarCliente(String nomeExibicao, String email, String senha) {
        return criar(new CriarUsuarioCommand(nomeExibicao, email, senha, Perfil.CLIENTE));
    }

    @Transactional
    public Usuario criarInterno(CriarUsuarioCommand command) {
        return criar(command);
    }

    @Transactional(readOnly = true)
    public Usuario buscarPorId(UUID id) {
        return repository.findById(id).map(UsuarioEntity::toDomain)
                .filter(Usuario::autenticavel)
                .orElseThrow(UsuarioNaoEncontradoException::new);
    }

    @Transactional(readOnly = true)
    public Usuario buscarPorEmail(String email) {
        return repository.findByEmailNormalizado(emailNormalizer.normalize(email))
                .map(UsuarioEntity::toDomain)
                .filter(Usuario::autenticavel)
                .orElseThrow(UsuarioNaoEncontradoException::new);
    }

    private Usuario criar(CriarUsuarioCommand command) {
        var email = emailNormalizer.normalize(command.email());
        validarSenha(command.senha());
        if (repository.existsByEmailNormalizado(email)) {
            throw new EmailJaCadastradoException();
        }
        var id = UUID.randomUUID();
        var usuario = new Usuario(id, "USR-" + id, command.nomeExibicao().strip(), email,
                passwordEncoder.encode(command.senha()), command.perfil(), true, null, null);
        try {
            return repository.saveAndFlush(UsuarioEntity.fromDomain(usuario)).toDomain();
        } catch (DataIntegrityViolationException exception) {
            if (exception.getMostSpecificCause().getMessage().contains("uk_usuario_email_normalizado")) {
                throw new EmailJaCadastradoException();
            }
            throw exception;
        }
    }

    private void validarSenha(String senha) {
        if (senha == null || senha.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("A senha deve possuir no máximo 72 bytes");
        }
    }
}
