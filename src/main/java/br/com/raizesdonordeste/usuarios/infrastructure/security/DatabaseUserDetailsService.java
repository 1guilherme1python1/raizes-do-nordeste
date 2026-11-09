package br.com.raizesdonordeste.usuarios.infrastructure.security;

import br.com.raizesdonordeste.usuarios.application.EmailNormalizer;
import br.com.raizesdonordeste.usuarios.infrastructure.persistence.UsuarioJpaRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UsuarioJpaRepository repository;
    private final EmailNormalizer emailNormalizer;

    public DatabaseUserDetailsService(UsuarioJpaRepository repository, EmailNormalizer emailNormalizer) {
        this.repository = repository;
        this.emailNormalizer = emailNormalizer;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var entity = repository.findByEmailNormalizado(emailNormalizer.normalize(username))
                .filter(user -> user.getPerfil() != null && user.getSenhaHash() != null)
                .orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas"));
        return User.withUsername(entity.getEmailNormalizado())
                .password(entity.getSenhaHash())
                .authorities("ROLE_" + entity.getPerfil().name())
                .disabled(!entity.isAtivo())
                .build();
    }
}
