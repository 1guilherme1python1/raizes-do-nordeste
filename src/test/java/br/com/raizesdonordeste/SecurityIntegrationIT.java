package br.com.raizesdonordeste;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import br.com.raizesdonordeste.auth.application.AuthService;
import br.com.raizesdonordeste.usuarios.application.CriarUsuarioCommand;
import br.com.raizesdonordeste.usuarios.application.UsuarioService;
import br.com.raizesdonordeste.usuarios.domain.Perfil;
import br.com.raizesdonordeste.usuarios.infrastructure.persistence.UsuarioJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@ActiveProfiles("test")
@SpringBootTest(properties = {
        "app.security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "app.security.jwt.issuer=integration-test-issuer",
        "app.security.jwt.audience=integration-test-audience"
})
@AutoConfigureMockMvc
@Testcontainers
class SecurityIntegrationIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRESQL =
            new PostgreSQLContainer<>("postgres:16.10-alpine3.22");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioJpaRepository usuarioRepository;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private AuthService authService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Test
    void t01CadastraClienteValido() throws Exception {
        var email = email("cliente");

        mockMvc.perform(post("/api/v1/usuarios/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registroJson("Cliente Válido", email, "senha-segura-123")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.perfil").value("CLIENTE"))
                .andExpect(jsonPath("$.ativo").value(true));

        assertThat(usuarioRepository.findByEmailNormalizado(email)).isPresent();
    }

    @Test
    void t02RejeitaEmailDuplicadoNormalizado() throws Exception {
        var local = "duplicado-" + UUID.randomUUID();
        var email = local + "@example.com";
        registrarCliente(email, "senha-segura-123");

        mockMvc.perform(post("/api/v1/usuarios/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registroJson("Outro Cliente", "  " + local.toUpperCase() + "@EXAMPLE.COM  ",
                                "outra-senha-123")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("email_already_registered"));
    }

    @Test
    void t03LoginValidoEmiteJwtDeTrintaMinutos() throws Exception {
        var email = email("login");
        registrarCliente(email, "senha-segura-123");

        var result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email, "senha-segura-123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").isString())
                .andReturn();

        var token = com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(),
                "$.accessToken").toString();
        var jwt = jwtDecoder.decode(token);
        assertThat(jwt.getAudience()).containsExactly("integration-test-audience");
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("CLIENTE");
        assertThat(jwt.getExpiresAt()).isEqualTo(jwt.getIssuedAt().plusSeconds(1800));
    }

    @Test
    void t04LoginComSenhaIncorretaNaoRevelaQualCredencialFalhou() throws Exception {
        var email = email("senha-incorreta");
        registrarCliente(email, "senha-segura-123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email, "senha-errada-123")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenciais inválidas"));
    }

    @Test
    void t05RecursoProtegidoSemTokenRetorna401() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("unauthorized"));
    }

    @Test
    void t06TokenInvalidoOuExpiradoRetorna401() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios/me").header("Authorization", "Bearer invalido"))
                .andExpect(status().isUnauthorized());

        var expired = signedToken(UUID.randomUUID(), "integration-test-issuer",
                "integration-test-audience", Instant.now().minusSeconds(7200), Instant.now().minusSeconds(3600),
                Perfil.CLIENTE);
        mockMvc.perform(get("/api/v1/usuarios/me").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void t07ClienteNaoPodeCadastrarUsuarioInterno() throws Exception {
        var token = criarUsuarioELogin(Perfil.CLIENTE);

        mockMvc.perform(post("/api/v1/usuarios")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(usuarioInternoJson(email("interno-negado"), "ATENDENTE")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("forbidden"));
    }

    @Test
    void t08AdminPodeCadastrarUsuarioInterno() throws Exception {
        var token = criarUsuarioELogin(Perfil.ADMIN);

        mockMvc.perform(post("/api/v1/usuarios")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(usuarioInternoJson(email("interno"), "ATENDENTE")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.perfil").value("ATENDENTE"));
    }

    @Test
    void t09ConsultaSomenteUsuarioAutenticado() throws Exception {
        var email = email("me");
        usuarioService.registrarCliente("Cliente Me", email, "senha-segura-123");
        var token = authService.login(email, "senha-segura-123").accessToken().value();

        mockMvc.perform(get("/api/v1/usuarios/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.perfil").value("CLIENTE"));
    }

    @Test
    void t10SenhaFicaComoHashENuncaApareceNaResposta() throws Exception {
        var email = email("hash");
        var senha = "senha-segura-123";

        var result = mockMvc.perform(post("/api/v1/usuarios/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registroJson("Cliente Hash", email, senha)))
                .andExpect(status().isCreated())
                .andExpect(content().string(not(containsString("senha"))))
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).doesNotContain(senha);
        var entity = usuarioRepository.findByEmailNormalizado(email).orElseThrow();
        assertThat(entity.getSenhaHash()).isNotEqualTo(senha).startsWith("$2");
        assertThat(passwordEncoder.matches(senha, entity.getSenhaHash())).isTrue();
    }

    @Test
    void t11CadastroPublicoIgnoraTentativaDeAtribuirAdmin() throws Exception {
        var email = email("sem-escalacao");
        var json = """
                {"nomeExibicao":"Cliente", "email":"%s", "senha":"senha-segura-123", "perfil":"ADMIN"}
                """.formatted(email);

        mockMvc.perform(post("/api/v1/usuarios/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.perfil").value("CLIENTE"));

        assertThat(usuarioRepository.findByEmailNormalizado(email).orElseThrow().getPerfil())
                .isEqualTo(Perfil.CLIENTE);
    }

    @Test
    void t12RejeitaEmissorOuAudienciaIncorretos() throws Exception {
        var id = UUID.randomUUID();
        var now = Instant.now();
        var wrongIssuer = signedToken(id, "outro-emissor", "integration-test-audience", now,
                now.plusSeconds(300), Perfil.CLIENTE);
        var wrongAudience = signedToken(id, "integration-test-issuer", "outra-audiencia", now,
                now.plusSeconds(300), Perfil.CLIENTE);

        mockMvc.perform(get("/api/v1/usuarios/me").header("Authorization", "Bearer " + wrongIssuer))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/usuarios/me").header("Authorization", "Bearer " + wrongAudience))
                .andExpect(status().isUnauthorized());
    }

    private void registrarCliente(String email, String senha) {
        usuarioService.registrarCliente("Cliente de Teste", email, senha);
    }

    private String criarUsuarioELogin(Perfil perfil) {
        var email = email(perfil.name().toLowerCase());
        usuarioService.criarInterno(new CriarUsuarioCommand("Usuário " + perfil, email,
                "senha-segura-123", perfil));
        return authService.login(email, "senha-segura-123").accessToken().value();
    }

    private String signedToken(UUID subject, String issuer, String audience, Instant issuedAt,
            Instant expiresAt, Perfil perfil) {
        var claims = JwtClaimsSet.builder()
                .subject(subject.toString())
                .issuer(issuer)
                .audience(List.of(audience))
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("roles", List.of(perfil.name()))
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    private String email(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }

    private String registroJson(String nome, String email, String senha) {
        return """
                {"nomeExibicao":"%s", "email":"%s", "senha":"%s"}
                """.formatted(nome, email, senha);
    }

    private String loginJson(String email, String senha) {
        return """
                {"email":"%s", "senha":"%s"}
                """.formatted(email, senha);
    }

    private String usuarioInternoJson(String email, String perfil) {
        return """
                {"nomeExibicao":"Usuário Interno", "email":"%s", "senha":"senha-segura-123", "perfil":"%s"}
                """.formatted(email, perfil);
    }
}
