package br.com.raizesdonordeste.auth.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import br.com.raizesdonordeste.shared.infrastructure.security.JwtProperties;
import br.com.raizesdonordeste.usuarios.domain.Usuario;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;
    private final Clock clock;

    public JwtService(JwtEncoder jwtEncoder, JwtProperties properties) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
        this.clock = Clock.systemUTC();
    }

    public AccessToken issue(Usuario usuario) {
        var issuedAt = Instant.now(clock);
        var expiresAt = issuedAt.plus(properties.accessTokenValidity());
        var claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .audience(List.of(properties.audience()))
                .subject(usuario.id().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("roles", List.of(usuario.perfil().name()))
                .build();
        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        return new AccessToken(jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue(),
                expiresAt);
    }
}
