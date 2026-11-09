package br.com.raizesdonordeste.auth.application;

import br.com.raizesdonordeste.usuarios.application.UsuarioNaoEncontradoException;
import br.com.raizesdonordeste.usuarios.application.UsuarioService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioService usuarioService;
    private final JwtService jwtService;

    public AuthService(AuthenticationManager authenticationManager, UsuarioService usuarioService,
            JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.usuarioService = usuarioService;
        this.jwtService = jwtService;
    }

    public LoginResult login(String email, String senha) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, senha));
            var usuario = usuarioService.buscarPorEmail(email);
            return new LoginResult(jwtService.issue(usuario), usuario);
        } catch (AuthenticationException | UsuarioNaoEncontradoException exception) {
            throw new CredenciaisInvalidasException();
        }
    }
}
