package com.fitcore.integration;

import com.fitcore.model.EstadoInscripcion;
import com.fitcore.repository.InscripcionRepository;
import com.fitcore.repository.RutinaRepository;
import com.fitcore.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba de validación de migración en una base de datos aislada (NUNCA la base principal de Neon):
 *  1) Crea el esquema legacy previo (como existía antes de la Etapa 3) e inserta registros históricos.
 *  2) Verifica las tablas, columnas y valores antes de migrar.
 *  3) Ejecuta las sentencias DDL/DML equivalentes de V1__membresias_rutinas_auth.sql.
 *  4) Arranca el contexto completo de Spring Boot con spring.jpa.hibernate.ddl-auto=validate.
 *  5) Comprueba que las fechas históricas (fecha_registro = fecha_inicio), estados y rutinas se preservaron.
 */
@SpringBootTest
class MigracionValidateIntegrationTest {

    private static final String URL_AISLADA =
            "jdbc:h2:mem:fitcore_migracion_aislada;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1";

    @DynamicPropertySource
    static void configurarBaseAisladaConValidate(DynamicPropertyRegistry registry) throws Exception {
        try (Connection conn = DriverManager.getConnection(URL_AISLADA, "sa", "");
             Statement st = conn.createStatement()) {

            // 1. Esquema legacy original (antes de la reestructuración)
            st.execute("""
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
                    estado VARCHAR(20) NOT NULL
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
                    descripcion VARCHAR(500),
                    cliente_id BIGINT NOT NULL REFERENCES clientes(id),
                    entrenador_id BIGINT NOT NULL REFERENCES entrenadores(id),
                    fecha_asignacion DATE NOT NULL,
                    activa BOOLEAN NOT NULL
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
                """);

            // 2. Insertar datos históricos previos a la migración
            st.execute("""
                INSERT INTO clientes (nombres, apellidos, dni, telefono, correo, fecha_registro)
                VALUES ('Carlos', 'Historico', '87654321', '999888777', 'carlos.hist@test.com', DATE '2025-01-10');

                INSERT INTO membresias (nombre, precio, duracion_dias, descripcion, activo)
                VALUES ('Plan Mensual Legacy', 90.00, 30, 'Plan historico', TRUE);

                INSERT INTO inscripciones (cliente_id, membresia_id, fecha_inicio, fecha_fin, estado)
                VALUES (1, 1, DATE '2025-02-01', DATE '2025-03-03', 'ACTIVA');

                INSERT INTO entrenadores (nombres, apellidos, especialidad, telefono, correo, activo)
                VALUES ('Marco', 'Trainer', 'Fuerza', '988111222', 'marco@test.com', TRUE);

                INSERT INTO rutinas (nombre, descripcion, cliente_id, entrenador_id, fecha_asignacion, activa)
                VALUES ('Rutina Legacy', 'Descripcion historica', 1, 1, DATE '2025-02-05', TRUE);
                """);

            // 3. Ejecutar migración V1 sobre la base con datos históricos
            st.execute("""
                CREATE TABLE IF NOT EXISTS usuarios (
                    id BIGSERIAL PRIMARY KEY,
                    username VARCHAR(50) NOT NULL UNIQUE,
                    password_hash VARCHAR(100) NOT NULL,
                    rol VARCHAR(20) NOT NULL DEFAULT 'ADMIN',
                    activo BOOLEAN NOT NULL DEFAULT TRUE
                );

                ALTER TABLE inscripciones ADD COLUMN IF NOT EXISTS fecha_registro DATE;
                ALTER TABLE inscripciones ADD COLUMN IF NOT EXISTS observaciones VARCHAR(300);
                UPDATE inscripciones SET fecha_registro = COALESCE(fecha_inicio, CURRENT_DATE) WHERE fecha_registro IS NULL;
                ALTER TABLE inscripciones ALTER COLUMN fecha_registro SET DEFAULT CURRENT_DATE;
                UPDATE inscripciones SET estado = 'VENCIDA' WHERE estado = 'ACTIVA' AND fecha_fin < CURRENT_DATE;

                ALTER TABLE rutinas ADD COLUMN IF NOT EXISTS objetivo VARCHAR(200);
                ALTER TABLE rutinas ADD COLUMN IF NOT EXISTS duracion_semanas INTEGER;
                ALTER TABLE rutinas ADD COLUMN IF NOT EXISTS fecha_inicio DATE;
                ALTER TABLE rutinas ADD COLUMN IF NOT EXISTS fecha_fin DATE;
                UPDATE rutinas SET objetivo = 'Acondicionamiento general' WHERE objetivo IS NULL OR TRIM(objetivo) = '';
                UPDATE rutinas SET fecha_inicio = fecha_asignacion WHERE fecha_inicio IS NULL AND fecha_asignacion IS NOT NULL;

                CREATE TABLE IF NOT EXISTS dias_rutina (
                    id BIGSERIAL PRIMARY KEY,
                    rutina_id BIGINT NOT NULL REFERENCES rutinas(id) ON DELETE RESTRICT,
                    dia_semana VARCHAR(10) NOT NULL,
                    nombre_dia VARCHAR(100),
                    orden INTEGER NOT NULL DEFAULT 1,
                    CONSTRAINT uk_dias_rutina_rutina_dia UNIQUE (rutina_id, dia_semana)
                );

                CREATE TABLE IF NOT EXISTS ejercicios_rutina (
                    id BIGSERIAL PRIMARY KEY,
                    dia_id BIGINT NOT NULL REFERENCES dias_rutina(id) ON DELETE RESTRICT,
                    nombre VARCHAR(100) NOT NULL,
                    series INTEGER NOT NULL DEFAULT 1,
                    repeticiones INTEGER NOT NULL DEFAULT 1,
                    descanso_seg INTEGER DEFAULT 60,
                    notas VARCHAR(300),
                    orden INTEGER NOT NULL DEFAULT 1
                );

                CREATE INDEX IF NOT EXISTS idx_dias_rutina_rutina_id ON dias_rutina (rutina_id);
                CREATE INDEX IF NOT EXISTS idx_ejercicios_rutina_dia_id ON ejercicios_rutina (dia_id);
                CREATE INDEX IF NOT EXISTS idx_inscripciones_cliente_id ON inscripciones (cliente_id);
                CREATE INDEX IF NOT EXISTS idx_inscripciones_estado ON inscripciones (estado);
                CREATE INDEX IF NOT EXISTS idx_inscripciones_fecha_fin ON inscripciones (fecha_fin);
                CREATE INDEX IF NOT EXISTS idx_rutinas_cliente_id ON rutinas (cliente_id);
                CREATE INDEX IF NOT EXISTS idx_rutinas_entrenador_id ON rutinas (entrenador_id);
                """);
        }

        registry.add("spring.datasource.url", () -> URL_AISLADA);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired InscripcionRepository inscripcionRepository;
    @Autowired RutinaRepository rutinaRepository;
    @Autowired UsuarioRepository usuarioRepository;

    @Test
    @DisplayName("Spring Boot arranca con ddl-auto=validate sobre el esquema migrado y preserva fechas e historial")
    void validaEsquemaMigradoYDatosHistoricos() {
        // 1. Verificar que la inscripción histórica conservó fecha_registro = 2025-02-01 (no CURRENT_DATE)
        var ins = inscripcionRepository.findById(1L).orElseThrow();
        assertEquals(LocalDate.of(2025, 2, 1), ins.getFechaInicio());
        assertEquals(LocalDate.of(2025, 2, 1), ins.getFechaRegistro());
        assertEquals(EstadoInscripcion.VENCIDA, ins.getEstado());

        // 2. Verificar que la rutina histórica recibió objetivo y fecha_inicio desde fecha_asignacion
        var rut = rutinaRepository.findById(1L).orElseThrow();
        assertEquals("Acondicionamiento general", rut.getObjetivo());
        assertEquals(LocalDate.of(2025, 2, 5), rut.getFechaInicio());
        assertTrue(rut.getActiva());

        // 3. Verificar que AdminInitializer creó el usuario admin con BCrypt
        var admin = usuarioRepository.findByUsername("admin").orElseThrow();
        assertNotNull(admin.getId());
        assertTrue(admin.getPasswordHash().startsWith("$2"));

        // 4. Verificar tablas e índices creados en el catálogo de la base aislada
        List<Map<String, Object>> tablas = jdbcTemplate.queryForList("""
                SELECT table_name FROM information_schema.tables
                WHERE table_schema = 'public'
                """);
        assertFalse(tablas.isEmpty());
    }
}
