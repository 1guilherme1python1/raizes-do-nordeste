package br.com.raizesdonordeste.usuarios.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioJpaRepository extends JpaRepository<UsuarioEntity, UUID> {

    Optional<UsuarioEntity> findByCodigo(String codigo);

    Optional<UsuarioEntity> findByEmailNormalizado(String emailNormalizado);

    boolean existsByEmailNormalizado(String emailNormalizado);
}
