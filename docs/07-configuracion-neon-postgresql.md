# ☁️ Configuración de PostgreSQL en Neon Cloud y Migraciones SQL

## 1. Variables de Entorno

El perfil principal (`application.properties`) se conecta a **PostgreSQL en Neon Cloud** exclusivamente mediante variables de entorno, sin exponer credenciales en el código fuente:

| Variable | Descripción | Ejemplo |
|---|---|---|
| `DB_URL` | URL JDBC de Neon Cloud con SSL | `jdbc:postgresql://<endpoint>.neon.tech/<db>?sslmode=require` |
| `DB_USERNAME` | Usuario de la base de datos en Neon | `neondb_owner` |
| `DB_PASSWORD` | Contraseña de Neon Cloud | *(secreto)* |
| `ADMIN_USERNAME` | Usuario administrador inicial | `admin` |
| `ADMIN_PASSWORD` | Contraseña inicial (mínimo 8 caracteres, guardada con BCrypt) | *(secreto)* |
| `JPA_DDL_AUTO` | Modo de validación de Hibernate (`validate` por defecto) | `validate` |
| `FITCORE_FERIADOS` | Lista opcional de feriados ISO para el cálculo de 7 días hábiles | `2026-12-25,2027-01-01` |

Propiedades de rendimiento y seguridad activas en `application.properties`:
- `spring.jpa.hibernate.ddl-auto=${JPA_DDL_AUTO:validate}`
- `spring.jpa.open-in-view=false`

---

## 2. Migraciones SQL Versionadas (`db/migration/`)

1. **[`V1__membresias_rutinas_auth.sql`](../backend/src/main/resources/db/migration/V1__membresias_rutinas_auth.sql)**:
   - Crea `usuarios`, `dias_rutina` (`uk_dias_rutina_rutina_dia`) y `ejercicios_rutina`.
   - Añade en `inscripciones` las columnas `fecha_registro` (preservando `fecha_inicio` histórica antes de fijar `DEFAULT CURRENT_DATE`) y `observaciones`.
   - Añade en `rutinas` las columnas `objetivo`, `duracion_semanas`, `fecha_inicio` y `fecha_fin`.
2. **[`V2__progreso_rutinas_y_trazabilidad_inscripciones.sql`](../backend/src/main/resources/db/migration/V2__progreso_rutinas_y_trazabilidad_inscripciones.sql)**:
   - Añade en `ejercicios_rutina` las columnas `completado BOOLEAN NOT NULL DEFAULT FALSE` y `peso_kg DOUBLE PRECISION CHECK (peso_kg IS NULL OR peso_kg >= 0)`.
   - Añade en `inscripciones` las columnas `inscripcion_origen_id BIGINT REFERENCES inscripciones(id) ON DELETE SET NULL`, `fecha_activacion TIMESTAMP`, `fecha_cancelacion TIMESTAMP` y `motivo_cancelacion VARCHAR(300)`, más el índice `idx_inscripciones_origen_id`.

> **Conservación de tablas históricas**: La tabla heredada `pagos` y la tabla `asistencias` en Neon Cloud no se eliminan (`NO DROP TABLE`), preservando cualquier registro histórico previo mientras el sistema opera 100% desacoplado del módulo de pagos.
