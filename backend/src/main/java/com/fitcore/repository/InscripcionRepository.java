package com.fitcore.repository;

import com.fitcore.model.EstadoInscripcion;
import com.fitcore.model.Inscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface InscripcionRepository extends JpaRepository<Inscripcion, Long> {

    // ── Consultas básicas (con JOIN FETCH para evitar N+1) ────────────────────

    @Query("SELECT i FROM Inscripcion i JOIN FETCH i.cliente JOIN FETCH i.membresia ORDER BY i.fechaInicio DESC, i.id DESC")
    List<Inscripcion> findAllConRelaciones();

    @Query("SELECT i FROM Inscripcion i JOIN FETCH i.cliente JOIN FETCH i.membresia WHERE i.cliente.id = :clienteId ORDER BY i.fechaInicio DESC, i.id DESC")
    List<Inscripcion> findByClienteId(@Param("clienteId") Long clienteId);

    // ── Reglas de negocio ─────────────────────────────────────────────────────

    /**
     * ¿Existe para el cliente una inscripción PENDIENTE o ACTIVA cuyo período se
     * superpone con [inicio, fin]? Evita duplicados y períodos superpuestos.
     */
    @Query("""
            SELECT COUNT(i) > 0 FROM Inscripcion i
            WHERE i.cliente.id = :clienteId
              AND i.id <> :excluirId
              AND i.estado IN (com.fitcore.model.EstadoInscripcion.ACTIVA,
                               com.fitcore.model.EstadoInscripcion.PENDIENTE)
              AND i.fechaInicio <= :fin
              AND i.fechaFin >= :inicio
            """)
    boolean existsSolapamiento(@Param("clienteId") Long clienteId,
                               @Param("inicio") LocalDate inicio,
                               @Param("fin") LocalDate fin,
                               @Param("excluirId") Long excluirId);

    /**
     * Verifica si una inscripción base ya fue renovada mediante otra inscripción no cancelada.
     */
    @Query("""
            SELECT COUNT(i) > 0 FROM Inscripcion i
            WHERE i.inscripcionOrigenId = :origenId
              AND i.estado <> com.fitcore.model.EstadoInscripcion.CANCELADA
            """)
    boolean existsRenovacionDeOrigen(@Param("origenId") Long origenId);

    /** Marca como VENCIDA toda inscripción ACTIVA cuya fecha de fin ya pasó. Conserva el historial. */
    @Modifying
    @Query("""
            UPDATE Inscripcion i SET i.estado = com.fitcore.model.EstadoInscripcion.VENCIDA
            WHERE i.estado = com.fitcore.model.EstadoInscripcion.ACTIVA AND i.fechaFin < :hoy
            """)
    int marcarVencidas(@Param("hoy") LocalDate hoy);

    // ── Listados para pantallas y dashboard ───────────────────────────────────

    @Query("SELECT i FROM Inscripcion i JOIN FETCH i.cliente JOIN FETCH i.membresia WHERE i.estado = com.fitcore.model.EstadoInscripcion.ACTIVA AND i.fechaFin >= :hoy ORDER BY i.fechaFin")
    List<Inscripcion> findActivasVigentes(@Param("hoy") LocalDate hoy);

    @Query("""
            SELECT i FROM Inscripcion i JOIN FETCH i.cliente JOIN FETCH i.membresia
            WHERE i.estado = com.fitcore.model.EstadoInscripcion.VENCIDA
               OR (i.estado = com.fitcore.model.EstadoInscripcion.ACTIVA AND i.fechaFin < :hoy)
            ORDER BY i.fechaFin DESC
            """)
    List<Inscripcion> findVencidas(@Param("hoy") LocalDate hoy);

    // ── Contadores ────────────────────────────────────────────────────────────

    @Query("SELECT COUNT(i) FROM Inscripcion i WHERE i.estado = com.fitcore.model.EstadoInscripcion.ACTIVA AND i.fechaFin >= :hoy")
    long countActivas(@Param("hoy") LocalDate hoy);

    long countByEstado(EstadoInscripcion estado);

    boolean existsByClienteId(Long clienteId);

    boolean existsByMembresiaId(Long membresiaId);

    @Query("""
            SELECT COUNT(i) FROM Inscripcion i
            WHERE i.estado = com.fitcore.model.EstadoInscripcion.VENCIDA
               OR (i.estado = com.fitcore.model.EstadoInscripcion.ACTIVA AND i.fechaFin < :hoy)
            """)
    long countVencidas(@Param("hoy") LocalDate hoy);

    default long countPendientes() {
        return countByEstado(EstadoInscripcion.PENDIENTE);
    }

    default long countCanceladas() {
        return countByEstado(EstadoInscripcion.CANCELADA);
    }
}
