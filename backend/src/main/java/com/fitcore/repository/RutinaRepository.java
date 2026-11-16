package com.fitcore.repository;

import com.fitcore.model.Rutina;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RutinaRepository extends JpaRepository<Rutina, Long> {

    // Los listados cargan cliente y entrenador en la misma consulta (evita N+1).
    @Query("SELECT r FROM Rutina r JOIN FETCH r.cliente JOIN FETCH r.entrenador ORDER BY r.fechaAsignacion DESC, r.id DESC")
    List<Rutina> findAllConRelaciones();

    @Query("SELECT r FROM Rutina r JOIN FETCH r.cliente JOIN FETCH r.entrenador WHERE r.cliente.id = :clienteId ORDER BY r.fechaAsignacion DESC, r.id DESC")
    List<Rutina> findByClienteId(@Param("clienteId") Long clienteId);

    @Query("SELECT r FROM Rutina r JOIN FETCH r.cliente JOIN FETCH r.entrenador WHERE r.entrenador.id = :entrenadorId ORDER BY r.fechaAsignacion DESC, r.id DESC")
    List<Rutina> findByEntrenadorId(@Param("entrenadorId") Long entrenadorId);

    @Query("SELECT r FROM Rutina r JOIN FETCH r.cliente JOIN FETCH r.entrenador WHERE r.cliente.id = :clienteId AND r.activa = true ORDER BY r.fechaAsignacion DESC")
    List<Rutina> findActivasByClienteId(@Param("clienteId") Long clienteId);

    @Query("SELECT r FROM Rutina r JOIN FETCH r.cliente JOIN FETCH r.entrenador WHERE r.id = :id")
    Optional<Rutina> findByIdConRelaciones(@Param("id") Long id);

    boolean existsByClienteId(Long clienteId);

    boolean existsByEntrenadorId(Long entrenadorId);

    long countByActivaTrue();

    long countByActivaFalse();
}
