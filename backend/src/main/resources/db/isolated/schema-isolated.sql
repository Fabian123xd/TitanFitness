-- Esquema inicial + Migraciones V1 y V2 para base de datos aislada de validación (NO toca Neon)
CREATE TABLE IF NOT EXISTS clientes (
    id BIGSERIAL PRIMARY KEY,
    nombres VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    dni VARCHAR(8) NOT NULL UNIQUE,
    telefono VARCHAR(15),
    correo VARCHAR(120) UNIQUE,
    fecha_registro DATE NOT NULL
);

CREATE TABLE IF NOT EXISTS membresias (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    precio NUMERIC(10,2) NOT NULL,
    duracion_dias INTEGER NOT NULL,
    descripcion VARCHAR(200),
    activo BOOLEAN NOT NULL
);

CREATE TABLE IF NOT EXISTS inscripciones (
    id BIGSERIAL PRIMARY KEY,
    cliente_id BIGINT NOT NULL REFERENCES clientes(id),
    membresia_id BIGINT NOT NULL REFERENCES membresias(id),
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE NOT NULL,
    estado VARCHAR(20) NOT NULL,
    fecha_registro DATE DEFAULT CURRENT_DATE,
    observaciones VARCHAR(300),
    inscripcion_origen_id BIGINT REFERENCES inscripciones(id) ON DELETE SET NULL,
    fecha_activacion TIMESTAMP,
    fecha_cancelacion TIMESTAMP,
    motivo_cancelacion VARCHAR(300),
    CONSTRAINT chk_inscripciones_estado CHECK (estado IN ('PENDIENTE', 'ACTIVA', 'VENCIDA', 'CANCELADA'))
);

CREATE TABLE IF NOT EXISTS entrenadores (
    id BIGSERIAL PRIMARY KEY,
    nombres VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    especialidad VARCHAR(100),
    telefono VARCHAR(15),
    correo VARCHAR(120) UNIQUE,
    activo BOOLEAN NOT NULL
);

CREATE TABLE IF NOT EXISTS rutinas (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    objetivo VARCHAR(200),
    descripcion VARCHAR(500),
    cliente_id BIGINT NOT NULL REFERENCES clientes(id),
    entrenador_id BIGINT NOT NULL REFERENCES entrenadores(id),
    duracion_semanas INTEGER,
    fecha_inicio DATE,
    fecha_fin DATE,
    fecha_asignacion DATE NOT NULL,
    activa BOOLEAN NOT NULL
);

CREATE TABLE IF NOT EXISTS dias_rutina (
    id BIGSERIAL PRIMARY KEY,
    rutina_id BIGINT NOT NULL REFERENCES rutinas(id) ON DELETE RESTRICT,
    dia_semana VARCHAR(10) NOT NULL CHECK (dia_semana IN ('LUNES','MARTES','MIERCOLES','JUEVES','VIERNES','SABADO','DOMINGO')),
    nombre_dia VARCHAR(100),
    orden INTEGER NOT NULL DEFAULT 1,
    CONSTRAINT uk_dias_rutina_rutina_dia UNIQUE (rutina_id, dia_semana)
);

CREATE TABLE IF NOT EXISTS ejercicios_rutina (
    id BIGSERIAL PRIMARY KEY,
    dia_id BIGINT NOT NULL REFERENCES dias_rutina(id) ON DELETE RESTRICT,
    nombre VARCHAR(100) NOT NULL,
    series INTEGER NOT NULL DEFAULT 1 CHECK (series >= 1),
    repeticiones INTEGER NOT NULL DEFAULT 1 CHECK (repeticiones >= 1),
    descanso_seg INTEGER DEFAULT 60 CHECK (descanso_seg >= 0),
    notas VARCHAR(300),
    orden INTEGER NOT NULL DEFAULT 1,
    completado BOOLEAN NOT NULL DEFAULT FALSE,
    peso_kg DOUBLE PRECISION CHECK (peso_kg IS NULL OR (peso_kg >= 0 AND peso_kg <= 1000))
);

CREATE TABLE IF NOT EXISTS usuarios (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    rol VARCHAR(20) NOT NULL DEFAULT 'ADMIN',
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS pagos (
    id BIGSERIAL PRIMARY KEY,
    inscripcion_id BIGINT NOT NULL REFERENCES inscripciones(id),
    monto NUMERIC(10,2) NOT NULL,
    monto_recibido NUMERIC(10,2),
    vuelto NUMERIC(10,2),
    fecha_pago TIMESTAMP NOT NULL,
    metodo_pago VARCHAR(30) NOT NULL,
    estado VARCHAR(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS asistencias (
    id BIGSERIAL PRIMARY KEY,
    cliente_id BIGINT NOT NULL REFERENCES clientes(id),
    inscripcion_id BIGINT NOT NULL REFERENCES inscripciones(id),
    fecha_hora TIMESTAMP NOT NULL,
    estado VARCHAR(20) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_dias_rutina_rutina_id ON dias_rutina (rutina_id);
CREATE INDEX IF NOT EXISTS idx_ejercicios_rutina_dia_id ON ejercicios_rutina (dia_id);
CREATE INDEX IF NOT EXISTS idx_inscripciones_cliente_id ON inscripciones (cliente_id);
CREATE INDEX IF NOT EXISTS idx_inscripciones_estado ON inscripciones (estado);
CREATE INDEX IF NOT EXISTS idx_inscripciones_fecha_fin ON inscripciones (fecha_fin);
CREATE INDEX IF NOT EXISTS idx_inscripciones_origen_id ON inscripciones (inscripcion_origen_id);
CREATE INDEX IF NOT EXISTS idx_rutinas_cliente_id ON rutinas (cliente_id);
CREATE INDEX IF NOT EXISTS idx_rutinas_entrenador_id ON rutinas (entrenador_id);
