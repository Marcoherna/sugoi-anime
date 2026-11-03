package cl.sugoianime.msauth.repository;

import cl.sugoianime.msauth.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    /** Cierre de sesion en todos los dispositivos (RF-08). */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revocado = true WHERE rt.usuario.id = :usuarioId")
    int revocarTodosDelUsuario(@Param("usuarioId") Long usuarioId);

    /** Limpieza de tokens caducados. */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiraEn < :ahora OR rt.revocado = true")
    int eliminarCaducadosYRevocados(@Param("ahora") OffsetDateTime ahora);
}
