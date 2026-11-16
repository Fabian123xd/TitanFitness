-- ============================================================================
-- MIGRACIÓN V1 — TITAN FITNESS CLUB (MEMBRESÍAS, INSCRIPCIONES, RUTINAS Y AUTH)
-- Motor objetivo: PostgreSQL 15+ (Neon Cloud)
-- Modo: NO DESTRUCTIVO, TRANSACCIONAL E IDEMPOTENTE
--   * No ejecuta DROP TABLE, TRUNCATE ni DELETE.
--   * Preserva las fechas históricas reales al poblar inscripciones.fecha_registro.
--   * Normaliza valores históricos antes de agregar restricciones CHECK.
--   * Compatible 1:1 con Hibernate SchemaValidator (spring.jpa.hibernate.ddl-auto=validate).
-- ============================================================================

-- ============================================================================
-- FASE A — CONSULTAS DE PRE-VERIFICACIÓN (SOLO LECTURA)
-- Ejecute este bloque ANTES de la migración para diagnosticar el estado actual
-- de Neon Cloud sin modificar ningún dato.
-- ============================================================================
/*
-- A.1 Verificar tipos de columnas PK y FK en tablas existentes (deben ser bigint / int8)
SELECT table_name, column_name, data_type, character_maximum_length, is_nullable, column_default
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name IN ('clientes','membresias','inscripciones','entrenadores','rutinas',
                     'dias_rutina','ejercicios_rutina','usuarios')
ORDER BY table_name, ordinal_position;

-- A.2 Verificar valores actuales de inscripciones.estado antes de mapear a EstadoInscripcion
SELECT estado, COUNT(*) AS total
FROM inscripciones
GROUP BY estado
ORDER BY total DESC;

-- A.3 Detectar inscripciones con fecha_fin anterior a fecha_inicio o con referencias huérfanas
SELECT id, cliente_id, membresia_id, fecha_inicio, fecha_fin, estado
FROM inscripciones
WHERE fecha_inicio IS NULL
   OR fecha_fin IS NULL
   OR fecha_fin < fecha_inicio;

-- A.4 Verificar registros existentes en rutinas
SELECT COUNT(*) AS total_rutinas,
       COUNT(*) FILTER (WHERE activa IS TRUE) AS rutinas_activas,
       COUNT(*) FILTER (WHERE fecha_asignacion IS NULL) AS sin_fecha_asignacion
FROM rutinas;
*/

-- ============================================================================
-- FASE B — MIGRACIÓN TRANSACCIONAL
-- Si cualquier sentencia falla, PostgreSQL revierte toda la transacción (ROLLBACK)
-- dejando la base de datos exactamente como estaba.
-- ============================================================================

BEGIN;

-- ----------------------------------------------------------------------------
-- 1. TABLA usuarios (entidad com.fitcore.model.Usuario)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS usuarios (
    id            BIGSERIAL    PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    rol           VARCHAR(20)  NOT NULL DEFAULT 'ADMIN',
    activo        BOOLEAN      NOT NULL DEFAULT TRUE
);

-- Garantizar columnas en caso de que la tabla usuarios hubiera existido parcialmente
ALTER TABLE usuarios
    ADD COLUMN IF NOT EXISTS username      VARCHAR(50),
    ADD COLUMN IF NOT EXISTS password_hash VARCHAR(100),
    ADD COLUMN IF NOT EXISTS rol           VARCHAR(20) DEFAULT 'ADMIN',
    ADD COLUMN IF NOT EXISTS activo        BOOLEAN     DEFAULT TRUE;

UPDATE usuarios SET rol = 'ADMIN' WHERE rol IS NULL OR TRIM(rol) = '';
UPDATE usuarios SET activo = TRUE WHERE activo IS NULL;

ALTER TABLE usuarios
    ALTER COLUMN username      SET NOT NULL,
    ALTER COLUMN password_hash SET NOT NULL,
    ALTER COLUMN rol           SET NOT NULL,
    ALTER COLUMN activo        SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_usuarios_username ON usuarios (username);

-- ----------------------------------------------------------------------------
-- 2. TABLA inscripciones (entidad com.fitcore.model.Inscripcion)
--    IMPORTANTE: fecha_registro se agrega SIN DEFAULT primero para NO sobrescribir
--    las filas históricas con CURRENT_DATE. Luego se copia fecha_inicio y recién
--    al final se define DEFAULT CURRENT_DATE para futuros registros.
-- ----------------------------------------------------------------------------
ALTER TABLE inscripciones
    ADD COLUMN IF NOT EXISTS fecha_registro DATE,
    ADD COLUMN IF NOT EXISTS observaciones  VARCHAR(300);

-- Si en algún entorno de prueba se agregó un DEFAULT prematuro, se retira antes del backfill
ALTER TABLE inscripciones
    ALTER COLUMN fecha_registro DROP DEFAULT;

-- Backfill seguro: preserva la fecha histórica real (fecha_inicio) en registros antiguos
UPDATE inscripciones
SET fecha_registro = COALESCE(fecha_inicio, CURRENT_DATE)
WHERE fecha_registro IS NULL;

-- Ahora sí se configura el DEFAULT para inserciones futuras
ALTER TABLE inscripciones
    ALTER COLUMN fecha_registro SET DEFAULT CURRENT_DATE;

-- Normalización de valores históricos de 'estado' para compatibilidad con enum EstadoInscripcion
UPDATE inscripciones
SET estado = UPPER(TRIM(estado))
WHERE estado IS NOT NULL AND estado <> UPPER(TRIM(estado));

UPDATE inscripciones
SET estado = CASE
    WHEN estado IN ('VIGENTE', 'ACTIVO', 'PAGADO', 'PAGADA') THEN 'ACTIVA'
    WHEN estado IN ('EXPIRADA', 'EXPIRADO', 'VENCIDO')       THEN 'VENCIDA'
    WHEN estado IN ('INACTIVA', 'INACTIVO', 'ANULADA', 'ANULADO', 'CANCELADO') THEN 'CANCELADA'
    WHEN estado IS NULL OR TRIM(estado) = ''                 THEN 'PENDIENTE'
    ELSE estado
END
WHERE estado IS NULL
   OR estado NOT IN ('PENDIENTE', 'ACTIVA', 'VENCIDA', 'CANCELADA');

-- Marcar como VENCIDA las inscripciones ACTIVAS cuya fecha_fin ya expiró
UPDATE inscripciones
SET estado = 'VENCIDA'
WHERE estado = 'ACTIVA'
  AND fecha_fin < CURRENT_DATE;

-- Restricción CHECK de valores válidos para EstadoInscripcion (idempotente)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'chk_inscripciones_estado'
          AND conrelid = 'inscripciones'::regclass
    ) THEN
        ALTER TABLE inscripciones
            ADD CONSTRAINT chk_inscripciones_estado
            CHECK (estado IN ('PENDIENTE', 'ACTIVA', 'VENCIDA', 'CANCELADA'));
    END IF;
END $$;

-- ----------------------------------------------------------------------------
-- 3. TABLA rutinas (entidad com.fitcore.model.Rutina)
-- ----------------------------------------------------------------------------
ALTER TABLE rutinas
    ADD COLUMN IF NOT EXISTS objetivo         VARCHAR(200),
    ADD COLUMN IF NOT EXISTS duracion_semanas INTEGER,
    ADD COLUMN IF NOT EXISTS fecha_inicio     DATE,
    ADD COLUMN IF NOT EXISTS fecha_fin        DATE;

-- Poblar valores históricos sin sobrescribir datos ya registrados
UPDATE rutinas
SET objetivo = 'Acondicionamiento general'
WHERE objetivo IS NULL OR TRIM(objetivo) = '';

UPDATE rutinas
SET fecha_inicio = fecha_asignacion
WHERE fecha_inicio IS NULL AND fecha_asignacion IS NOT NULL;

UPDATE rutinas
SET fecha_fin = (fecha_inicio + (duracion_semanas * 7))
WHERE fecha_fin IS NULL
  AND fecha_inicio IS NOT NULL
  AND duracion_semanas IS NOT NULL
  AND duracion_semanas > 0;

-- ----------------------------------------------------------------------------
-- 4. TABLA dias_rutina (entidad com.fitcore.model.DiaRutina)
--    Nota sobre ON DELETE: las rutinas nunca se eliminan en el sistema (solo se
--    desactivan para conservar el historial). Al editar una rutina, Hibernate
--    administra la colección con orphanRemoval = true. Se usa ON DELETE RESTRICT
--    para impedir que un DELETE directo en 'rutinas' borre silenciosamente el
--    detalle histórico en la base de datos.
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS dias_rutina (
    id         BIGSERIAL    PRIMARY KEY,
    rutina_id  BIGINT       NOT NULL,
    dia_semana VARCHAR(10)  NOT NULL,
    nombre_dia VARCHAR(100),
    orden      INTEGER      NOT NULL DEFAULT 1
);

ALTER TABLE dias_rutina
    ADD COLUMN IF NOT EXISTS rutina_id  BIGINT,
    ADD COLUMN IF NOT EXISTS dia_semana VARCHAR(10),
    ADD COLUMN IF NOT EXISTS nombre_dia VARCHAR(100),
    ADD COLUMN IF NOT EXISTS orden      INTEGER DEFAULT 1;

UPDATE dias_rutina SET orden = 1 WHERE orden IS NULL;

ALTER TABLE dias_rutina
    ALTER COLUMN rutina_id  SET NOT NULL,
    ALTER COLUMN dia_semana SET NOT NULL,
    ALTER COLUMN orden      SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_dias_rutina_rutina'
          AND conrelid = 'dias_rutina'::regclass
    ) THEN
        ALTER TABLE dias_rutina
            ADD CONSTRAINT fk_dias_rutina_rutina
            FOREIGN KEY (rutina_id) REFERENCES rutinas(id) ON DELETE RESTRICT;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'chk_dias_rutina_dia_semana'
          AND conrelid = 'dias_rutina'::regclass
    ) THEN
        ALTER TABLE dias_rutina
            ADD CONSTRAINT chk_dias_rutina_dia_semana
            CHECK (dia_semana IN ('LUNES','MARTES','MIERCOLES','JUEVES','VIERNES','SABADO','DOMINGO'));
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'uk_dias_rutina_rutina_dia'
          AND conrelid = 'dias_rutina'::regclass
    ) THEN
        ALTER TABLE dias_rutina
            ADD CONSTRAINT uk_dias_rutina_rutina_dia
            UNIQUE (rutina_id, dia_semana);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_dias_rutina_rutina_id ON dias_rutina (rutina_id);

-- ----------------------------------------------------------------------------
-- 5. TABLA ejercicios_rutina (entidad com.fitcore.model.EjercicioRutina)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ejercicios_rutina (
    id           BIGSERIAL    PRIMARY KEY,
    dia_id       BIGINT       NOT NULL,
    nombre       VARCHAR(100) NOT NULL,
    series       INTEGER      NOT NULL DEFAULT 1,
    repeticiones INTEGER      NOT NULL DEFAULT 1,
    descanso_seg INTEGER      DEFAULT 60,
    notas        VARCHAR(300),
    orden        INTEGER      NOT NULL DEFAULT 1
);

ALTER TABLE ejercicios_rutina
    ADD COLUMN IF NOT EXISTS dia_id       BIGINT,
    ADD COLUMN IF NOT EXISTS nombre       VARCHAR(100),
    ADD COLUMN IF NOT EXISTS series       INTEGER DEFAULT 1,
    ADD COLUMN IF NOT EXISTS repeticiones INTEGER DEFAULT 1,
    ADD COLUMN IF NOT EXISTS descanso_seg INTEGER DEFAULT 60,
    ADD COLUMN IF NOT EXISTS notas        VARCHAR(300),
    ADD COLUMN IF NOT EXISTS orden        INTEGER DEFAULT 1;

UPDATE ejercicios_rutina SET series = 1       WHERE series IS NULL OR series < 1;
UPDATE ejercicios_rutina SET repeticiones = 1 WHERE repeticiones IS NULL OR repeticiones < 1;
UPDATE ejercicios_rutina SET descanso_seg = 0 WHERE descanso_seg < 0;
UPDATE ejercicios_rutina SET orden = 1        WHERE orden IS NULL;

ALTER TABLE ejercicios_rutina
    ALTER COLUMN dia_id       SET NOT NULL,
    ALTER COLUMN nombre       SET NOT NULL,
    ALTER COLUMN series       SET NOT NULL,
    ALTER COLUMN repeticiones SET NOT NULL,
    ALTER COLUMN orden        SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_ejercicios_rutina_dia'
          AND conrelid = 'ejercicios_rutina'::regclass
    ) THEN
        ALTER TABLE ejercicios_rutina
            ADD CONSTRAINT fk_ejercicios_rutina_dia
            FOREIGN KEY (dia_id) REFERENCES dias_rutina(id) ON DELETE RESTRICT;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'chk_ejercicios_rutina_valores'
          AND conrelid = 'ejercicios_rutina'::regclass
    ) THEN
        ALTER TABLE ejercicios_rutina
            ADD CONSTRAINT chk_ejercicios_rutina_valores
            CHECK (series >= 1 AND repeticiones >= 1 AND (descanso_seg IS NULL OR descanso_seg >= 0));
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_ejercicios_rutina_dia_id ON ejercicios_rutina (dia_id);

-- ----------------------------------------------------------------------------
-- 6. ÍNDICES DE RENDIMIENTO EN TABLAS EXISTENTES
-- ----------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_inscripciones_cliente_id ON inscripciones (cliente_id);
CREATE INDEX IF NOT EXISTS idx_inscripciones_estado     ON inscripciones (estado);
CREATE INDEX IF NOT EXISTS idx_inscripciones_fecha_fin  ON inscripciones (fecha_fin);
CREATE INDEX IF NOT EXISTS idx_rutinas_cliente_id       ON rutinas (cliente_id);
CREATE INDEX IF NOT EXISTS idx_rutinas_entrenador_id    ON rutinas (entrenador_id);

COMMIT;

-- ============================================================================
-- FASE C — CONSULTAS DE POST-VERIFICACIÓN (SOLO LECTURA)
-- Ejecute este bloque DESPUÉS de la migración para confirmar que el esquema y
-- los datos quedaron íntegros.
-- ============================================================================
/*
-- C.1 Confirmar que todas las inscripciones históricas conservaron fecha_registro = fecha_inicio
SELECT id, cliente_id, fecha_inicio, fecha_registro, estado, observaciones
FROM inscripciones
ORDER BY id;

-- C.2 Confirmar que no existen estados fuera del enum EstadoInscripcion
SELECT estado, COUNT(*) AS total
FROM inscripciones
GROUP BY estado;

-- C.3 Confirmar que todas las rutinas tienen objetivo y fecha_inicio poblados
SELECT id, nombre, objetivo, duracion_semanas, fecha_inicio, fecha_fin, fecha_asignacion, activa
FROM rutinas
ORDER BY id;

-- C.4 Confirmar estructura de tablas y tipos de datos esperados por Hibernate (validate)
SELECT table_name, column_name, data_type, character_maximum_length, is_nullable, column_default
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name IN ('usuarios','inscripciones','rutinas','dias_rutina','ejercicios_rutina')
ORDER BY table_name, ordinal_position;

-- C.5 Confirmar claves foráneas y restricciones CHECK registradas
SELECT conrelid::regclass AS tabla, conname AS restriccion, pg_get_constraintdef(oid) AS definicion
FROM pg_constraint
WHERE conrelid IN ('usuarios'::regclass, 'inscripciones'::regclass, 'rutinas'::regclass,
                   'dias_rutina'::regclass, 'ejercicios_rutina'::regclass)
ORDER BY tabla, restriccion;
*/
