package com.fitcore.repository;

import com.fitcore.model.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @deprecated Módulo de pagos fuera del alcance funcional principal.
 * Se conserva la interfaz para mantener compatibilidad con la tabla histórica 'pagos'.
 */
@Deprecated(since = "Etapa3", forRemoval = false)
@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {

    List<Pago> findByInscripcionId(Long inscripcionId);

    List<Pago> findByInscripcionClienteId(Long clienteId);

    @Query("SELECT COALESCE(SUM(p.monto), 0) FROM Pago p")
    BigDecimal calcularIngresosTotales();

    @Query("""
        SELECT COALESCE(SUM(p.monto), 0)
        FROM Pago p
        WHERE p.fechaPago >= :inicio
          AND p.fechaPago < :fin
          AND p.estado = 'PAGADO'
        """)
    BigDecimal calcularIngresosPorFechas(
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin
    );
}
