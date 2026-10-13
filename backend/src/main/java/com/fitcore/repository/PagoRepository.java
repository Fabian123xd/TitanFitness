package com.fitcore.repository;

import com.fitcore.model.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {

    // Buscar pagos de una inscripción
    List<Pago> findByInscripcionId(Long inscripcionId);

    // Buscar pagos de un cliente
    List<Pago> findByInscripcionClienteId(Long clienteId);

    // Calcular ingresos totales del gimnasio
    @Query("SELECT COALESCE(SUM(p.monto), 0) FROM Pago p")
    BigDecimal calcularIngresosTotales();

    // Calcular ingresos entre dos fechas
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

