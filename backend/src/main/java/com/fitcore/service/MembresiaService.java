package com.fitcore.service;

import com.fitcore.exception.RecursoNoEncontradoException;
import com.fitcore.exception.ReglaDeNegocioException;
import com.fitcore.model.Membresia;
import com.fitcore.repository.InscripcionRepository;
import com.fitcore.repository.MembresiaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Gestión de planes de membresía (el PLAN, no la contratación de un cliente).
 *
 *  MB-01 El nombre del plan es único (al crear y al editar).
 *  MB-02/03 Duración y precio positivos (validación en la entidad).
 *  MB-04 Un plan inactivo no se puede usar en nuevas inscripciones.
 *  MB-05 Un plan con inscripciones en el historial no se elimina: se desactiva.
 *  MB-06 Desactivar un plan no afecta las inscripciones ya vigentes.
 *
 * Nota: "activo" describe al PLAN; la vigencia de un cliente se describe con el
 * estado de su Inscripcion (PENDIENTE/ACTIVA/VENCIDA/CANCELADA).
 */
@Service
@Transactional(readOnly = true)
public class MembresiaService {

    private final MembresiaRepository membresiaRepository;
    private final InscripcionRepository inscripcionRepository;

    public MembresiaService(MembresiaRepository membresiaRepository,
                            InscripcionRepository inscripcionRepository) {
        this.membresiaRepository = membresiaRepository;
        this.inscripcionRepository = inscripcionRepository;
    }

    public List<Membresia> listarTodas() {
        return membresiaRepository.findAll();
    }

    public List<Membresia> listarActivas() {
        return membresiaRepository.findByActivoTrue();
    }

    public Optional<Membresia> buscarPorId(Long id) {
        return membresiaRepository.findById(id);
    }

    @Transactional
    public Membresia registrar(Membresia membresia) {
        if (membresiaRepository.existsByNombre(membresia.getNombre())) {
            throw new ReglaDeNegocioException(
                    "Ya existe un plan de membresía con el nombre '" + membresia.getNombre() + "'.");
        }
        if (membresia.getActivo() == null) {
            membresia.setActivo(true);
        }
        return membresiaRepository.save(membresia);
    }

    @Transactional
    public Membresia actualizar(Long id, Membresia datos) {
        Membresia membresia = membresiaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Membresía", id));

        if (!membresia.getNombre().equalsIgnoreCase(datos.getNombre())
                && membresiaRepository.existsByNombre(datos.getNombre())) {
            throw new ReglaDeNegocioException(
                    "Ya existe otro plan con el nombre '" + datos.getNombre() + "'.");
        }

        membresia.setNombre(datos.getNombre());
        membresia.setDescripcion(datos.getDescripcion());
        membresia.setDuracionDias(datos.getDuracionDias());
        membresia.setPrecio(datos.getPrecio());
        if (datos.getActivo() != null) {
            membresia.setActivo(datos.getActivo());
        }
        return membresiaRepository.save(membresia);
    }

    /** Activa o desactiva el plan. */
    @Transactional
    public Membresia toggleActivo(Long id) {
        Membresia membresia = membresiaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Membresía", id));
        membresia.setActivo(!Boolean.TRUE.equals(membresia.getActivo()));
        return membresiaRepository.save(membresia);
    }

    @Transactional
    public void eliminar(Long id) {
        if (!membresiaRepository.existsById(id)) {
            throw new RecursoNoEncontradoException("Membresía", id);
        }
        if (inscripcionRepository.existsByMembresiaId(id)) {
            throw new ReglaDeNegocioException(
                    "No se puede eliminar el plan: tiene inscripciones en el historial. Desactívelo en su lugar.");
        }
        membresiaRepository.deleteById(id);
    }
}
