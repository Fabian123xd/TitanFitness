package com.fitcore.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Tarea diaria que marca como VENCIDAS las inscripciones activas cuyo período terminó. */
@Component
public class VencimientoScheduler {

    private static final Logger log = LoggerFactory.getLogger(VencimientoScheduler.class);

    private final InscripcionService inscripcionService;

    public VencimientoScheduler(InscripcionService inscripcionService) {
        this.inscripcionService = inscripcionService;
    }

    @Scheduled(cron = "0 5 0 * * *")
    public void sincronizar() {
        int actualizadas = inscripcionService.sincronizarVencimientos();
        log.info("Vencimientos sincronizados: {} inscripciones marcadas como VENCIDA", actualizadas);
    }
}
