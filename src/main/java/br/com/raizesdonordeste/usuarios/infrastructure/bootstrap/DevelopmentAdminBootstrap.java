package br.com.raizesdonordeste.usuarios.infrastructure.bootstrap;

import br.com.raizesdonordeste.usuarios.application.CriarUsuarioCommand;
import br.com.raizesdonordeste.usuarios.application.EmailNormalizer;
import br.com.raizesdonordeste.usuarios.application.UsuarioService;
import br.com.raizesdonordeste.usuarios.domain.Perfil;
import br.com.raizesdonordeste.usuarios.infrastructure.persistence.UsuarioJpaRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
@ConditionalOnProperty(prefix = "app.dev.admin", name = "enabled", havingValue = "true")
public class DevelopmentAdminBootstrap implements ApplicationRunner {

    private final UsuarioJpaRepository repository;
    private final UsuarioService usuarioService;
    private final EmailNormalizer emailNormalizer;
    private final String email;
    private final String password;
    private final String displayName;

    public DevelopmentAdminBootstrap(UsuarioJpaRepository repository, UsuarioService usuarioService,
            EmailNormalizer emailNormalizer,
            @Value("${app.dev.admin.email:}") String email,
            @Value("${app.dev.admin.password:}") String password,
            @Value("${app.dev.admin.display-name:Administrador de Desenvolvimento}") String displayName) {
        this.repository = repository;
        this.usuarioService = usuarioService;
        this.emailNormalizer = emailNormalizer;
        this.email = email;
        this.password = password;
        this.displayName = displayName;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank()) {
            throw new IllegalStateException("DEV_ADMIN_EMAIL e DEV_ADMIN_PASSWORD são obrigatórios quando o bootstrap está ativo");
        }
        var normalized = emailNormalizer.normalize(email);
        if (!repository.existsByEmailNormalizado(normalized)) {
            usuarioService.criarInterno(new CriarUsuarioCommand(displayName, normalized, password, Perfil.ADMIN));
        }
    }
}
