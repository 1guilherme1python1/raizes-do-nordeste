package br.com.raizesdonordeste.usuarios.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import br.com.raizesdonordeste.shared.infrastructure.persistence.AuditableEntity;
import br.com.raizesdonordeste.usuarios.domain.Usuario;
import br.com.raizesdonordeste.usuarios.domain.Perfil;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(schema = "app", name = "usuario")
public class UsuarioEntity extends AuditableEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 50)
    private String codigo;

    @Column(name = "nome_exibicao", nullable = false, length = 150)
    private String nomeExibicao;

    @Column(name = "email_normalizado", length = 254)
    private String emailNormalizado;

    @Column(name = "senha_hash", length = 100)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Perfil perfil;

    @Column(nullable = false)
    private boolean ativo;

    protected UsuarioEntity() {
    }

    public UsuarioEntity(UUID id, String codigo, String nomeExibicao, String emailNormalizado,
            String senhaHash, Perfil perfil, boolean ativo,
            Instant criadoEm, Instant atualizadoEm) {
        super(criadoEm, atualizadoEm);
        this.id = id;
        this.codigo = codigo;
        this.nomeExibicao = nomeExibicao;
        this.emailNormalizado = emailNormalizado;
        this.senhaHash = senhaHash;
        this.perfil = perfil;
        this.ativo = ativo;
    }

    public static UsuarioEntity fromDomain(Usuario usuario) {
        return new UsuarioEntity(usuario.id(), usuario.codigo(), usuario.nomeExibicao(),
                usuario.emailNormalizado(), usuario.senhaHash(), usuario.perfil(), usuario.ativo(),
                usuario.criadoEm(), usuario.atualizadoEm());
    }

    public Usuario toDomain() {
        return new Usuario(id, codigo, nomeExibicao, emailNormalizado, senhaHash, perfil, ativo,
                getCriadoEm(), getAtualizadoEm());
    }

    public UUID getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNomeExibicao() {
        return nomeExibicao;
    }

    public String getEmailNormalizado() {
        return emailNormalizado;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public Perfil getPerfil() {
        return perfil;
    }

    public boolean isAtivo() {
        return ativo;
    }
}
