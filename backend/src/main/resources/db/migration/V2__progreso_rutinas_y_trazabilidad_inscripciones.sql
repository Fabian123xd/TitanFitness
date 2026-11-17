-- ============================================================================
-- MIGRACIÓN INCREMENTAL V2 — TITAN FITNESS CLUB
-- Seguimiento de progreso en ejercicios de rutina + Trazabilidad completa de
-- renovaciones, activaciones y cancelaciones en inscripciones.
--
-- Motor objetivo: PostgreSQL 15+ (Neon Cloud)
-- Modo: NO DESTRUCTIVO, TRANSACCIONAL E IDEMPOTENTE
--   * No ejecuta DROP TABLE, TRUNCATE ni DELETE.
--   * Preserva íntegramente las tablas históricas (incluyendo pagos y asistencias).
--   * Incorpora backfill seguro para inscripciones históricas en estado CANCELADA.
--   * Compatible 1:1 con V1 y con Hibernate (spring.jpa.hibernate.ddl-auto=validate).
-- ============================================================================

-- ============================================================================
-- FASE A — CONSULTAS DE PRE-VERIFICACIÓN (SOLO LECTURA)
-- Ejecute este bloque ANTES de aplicar V2 para inspeccionar el estado actual
-- de las tablas ejercicios_rutina e inscripciones sin modificar datos.
-- ============================================================================
/*
-- A.1 Verificar columnas actuales en ejercicios_rutina e inscripciones
SELECT table_name, column_name, data_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name IN ('ejercicios_rutina', 'inscripciones')
ORDER BY table_name, ordinal_position;

-- A.2 Contar ejercicios registrados actualmente
SELECT COUNT(*) AS total_ejercicios
FROM ejercicios_rutina;

-- A.3 Contar inscripciones por estado antes de aplicar trazabilidad V2
SELECT estado, COUNT(*) AS total
FROM inscripciones
GROUP BY estado
ORDER BY total DESC;
*/

-- ============================================================================
-- FASE B — MIGRACIÓN TRANSACCIONAL V2
-- Si cualquier sentencia falla, PostgreSQL revierte toda la transacción (ROLLBACK).
-- ============================================================================

BEGIN;

-- ----------------------------------------------------------------------------
-- 1. SEGUIMIENTO DE PROGRESO EN ejercicios_rutina
--    Atributos en EjercicioRutina.java:
--      - completado BOOLEAN NOT NULL DEFAULT FALSE
--      - peso_kg    DOUBLE PRECISION (opcional, entre 0 y 1000 kg)
-- ----------------------------------------------------------------------------
ALTER TABLE ejercicios_rutina
    ADD COLUMN IF NOT EXISTS completado BOOLEAN,
    ADD COLUMN IF NOT EXISTS peso_kg    DOUBLE PRECISION;

-- Backfill seguro: ejercicios existentes inician como pendientes (FALSE)
UPDATE ejercicios_rutina
SET completado = FALSE
WHERE completado IS NULL;

-- Normalizar cualquier peso fuera de rango antes de añadir la restricción CHECK
UPDATE ejercicios_rutina
SET peso_kg = NULL
WHERE peso_kg IS NOT NULL AND (peso_kg < 0 OR peso_kg > 1000);

ALTER TABLE ejercicios_rutina
    ALTER COLUMN completado SET DEFAULT FALSE,
    ALTER COLUMN completado SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'chk_ejercicios_rutina_peso_positivo'
          AND conrelid = 'ejercicios_rutina'::regclass
    ) THEN
        ALTER TABLE ejercicios_rutina
            ADD CONSTRAINT chk_ejercicios_rutina_peso_positivo
            CHECK (peso_kg IS NULL OR (peso_kg >= 0 AND peso_kg <= 1000));
    END IF;
END $$;

-- ----------------------------------------------------------------------------
-- 2. TRAZABILIDAD DE RENOVACIONES, ACTIVACIONES Y CANCELACIONES EN inscripciones
--    Atributos en Inscripcion.java:
--      - inscripcion_origen_id BIGINT (FK autorreferencial opcional)
--      - fecha_activacion      TIMESTAMP
--      - fecha_cancelacion     TIMESTAMP
--      - motivo_cancelacion    VARCHAR(300)
-- ----------------------------------------------------------------------------
ALTER TABLE inscripciones
    ADD COLUMN IF NOT EXISTS inscripcion_origen_id BIGINT,
    ADD COLUMN IF NOT EXISTS fecha_activacion      TIMESTAMP,
    ADD COLUMN IF NOT EXISTS fecha_cancelacion     TIMESTAMP,
    ADD COLUMN IF NOT EXISTS motivo_cancelacion    VARCHAR(300);

-- Backfill seguro: documentar inscripciones históricas que ya estaban CANCELADAS antes de V2
UPDATE inscripciones
SET motivo_cancelacion = 'Cancelación histórica previa a V2'
WHERE estado = 'CANCELADA'
  AND (motivo_cancelacion IS NULL OR TRIM(motivo_cancelacion) = '');

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_inscripciones_origen'
          AND conrelid = 'inscripciones'::regclass
    ) THEN
        ALTER TABLE inscripciones
            ADD CONSTRAINT fk_inscripciones_origen
            FOREIGN KEY (inscripcion_origen_id)
            REFERENCES inscripciones(id)
            ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_inscripciones_origen_id
    ON inscripciones (inscripcion_origen_id);

COMMIT;

-- ============================================================================
-- FASE C — CONSULTAS DE POST-VERIFICACIÓN (SOLO LECTURA)
-- Ejecute este bloque DESPUÉS de aplicar V2 para confirmar la estructura final.
-- ============================================================================
/*
-- C.1 Confirmar que todos los ejercicios tienen completado poblado (sin NULL)
SELECT COUNT(*) AS total_ejercicios,
       COUNT(*) FILTER (WHERE completado IS TRUE) AS completados,
       COUNT(*) FILTER (WHERE completado IS FALSE) AS pendientes,
       COUNT(*) FILTER (WHERE completado IS NULL) AS nulos_invalidos
FROM ejercicios_rutina;

-- C.2 Confirmar trazabilidad en inscripciones y backfill de cancelaciones históricas
SELECT id, cliente_id, estado, fecha_inicio, fecha_fin,
       inscripcion_origen_id, fecha_activacion, fecha_cancelacion, motivo_cancelacion
FROM inscripciones
ORDER BY id;

-- C.3 Confirmar restricciones y claves foráneas de V1 + V2
SELECT conrelid::regclass AS tabla, conname AS restriccion, pg_get_constraintdef(oid) AS definicion
FROM pg_constraint
WHERE conrelid IN ('inscripciones'::regclass, 'ejercicios_rutina'::regclass)
ORDER BY tabla, restriccion;
*/
