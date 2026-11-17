# 🗄️ Modelo de Datos y Migración SQL — Titan Fitness Club

## 1. Diagrama Entidad-Relación (MER)

```mermaid
erDiagram
    USUARIOS {
        bigserial id PK
        varchar(50) username UK
        varchar(100) password_hash
        varchar(20) rol
        boolean activo
    }

    CLIENTES {
        bigserial id PK
        varchar(100) nombres
        varchar(100) apellidos
        varchar(8) dni UK
        varchar(15) telefono
        varchar(120) correo UK
        date fecha_registro
    }

    MEMBRESIAS {
        bigserial id PK
        varchar(50) nombre
        varchar(200) descripcion
        integer duracion_dias
        numeric(10_2) precio
        boolean activo
    }

    INSCRIPCIONES {
        bigserial id PK
        bigint cliente_id FK
        bigint membresia_id FK
        date fecha_inicio
        date fecha_fin
        varchar(20) estado
        date fecha_registro
        varchar(300) observaciones
        bigint inscripcion_origen_id FK
        timestamp fecha_activacion
        timestamp fecha_cancelacion
        varchar(300) motivo_cancelacion
    }

    ENTRENADORES {
        bigserial id PK
        varchar(100) nombres
        varchar(100) apellidos
        varchar(100) especialidad
        varchar(15) telefono
        varchar(120) correo UK
        boolean activo
    }

    RUTINAS {
        bigserial id PK
        varchar(100) nombre
        varchar(200) objetivo
        varchar(500) descripcion
        bigint cliente_id FK
        bigint entrenador_id FK
        integer duracion_semanas
        date fecha_inicio
        date fecha_fin
        date fecha_asignacion
        boolean activa
    }

    DIAS_RUTINA {
        bigserial id PK
        bigint rutina_id FK
        varchar(10) dia_semana
        varchar(100) nombre_dia
        integer orden
    }

    EJERCICIOS_RUTINA {
        bigserial id PK
        bigint dia_id FK
        varchar(100) nombre
        integer series
        integer repeticiones
        integer descanso_seg
        varchar(300) notas
        integer orden
        boolean completado
        double_precision peso_kg
    }

    CLIENTES ||--o{ INSCRIPCIONES : "contrata (historial)"
    MEMBRESIAS ||--o{ INSCRIPCIONES : "define condiciones"
    INSCRIPCIONES |o--o{ INSCRIPCIONES : "renueva desde (inscripcion_origen_id)"
    CLIENTES ||--o{ RUTINAS : "recibe (historial)"
    ENTRENADORES ||--o{ RUTINAS : "supervisa"
    RUTINAS ||--o{ DIAS_RUTINA : "organiza (UK rutina_id + dia_semana)"
    DIAS_RUTINA ||--o{ EJERCICIOS_RUTINA : "contiene"
```

---

## 2. Decisiones de Integridad y Sincronización JPA

1. **Separación entre Plan (`membresias`) e Inscripción (`inscripciones`)**:
   - `membresias.activo` indica únicamente si el plan puede seleccionarse en **nuevas** inscripciones.
   - `inscripciones.estado` (`PENDIENTE`, `ACTIVA`, `VENCIDA`, `CANCELADA`) representa la vigencia real del socio.
2. **Restricción de unicidad `uk_dias_rutina_rutina_dia (rutina_id, dia_semana)` y prevención de `SQLState: 23505`**:
   - La tabla `dias_rutina` y la entidad `DiaRutina` incluyen `@UniqueConstraint(name = "uk_dias_rutina_rutina_dia", columnNames = {"rutina_id", "dia_semana"})` para impedir que una misma rutina tenga dos veces el mismo día (por ejemplo, dos `LUNES`).
   - Al actualizar una rutina en `RutinaService.aplicarDatos(...)`, el servicio **reutiliza las instancias existentes de `DiaRutina` cuyo `DiaSemana` coincide** y solo elimina mediante `removeIf(...)` los días que fueron descartados, evitando que Hibernate ejecute un `INSERT` duplicado antes del `DELETE` de huérfanos (`SQLState: 23505`).
   - Las colecciones `Rutina.dias` y `DiaRutina.ejercicios` cuentan con `@BatchSize(size = 50)` para evitar consultas `N+1` al calcular los indicadores de progreso con `spring.jpa.open-in-view=false`.
3. **Preservación de fechas históricas en `V1__membresias_rutinas_auth.sql` y `V2__progreso_rutinas_y_trazabilidad_inscripciones.sql`**:
   - En PostgreSQL (`V1`), la columna `inscripciones.fecha_registro` se agrega inicialmente sin `DEFAULT`, se puebla con `COALESCE(fecha_inicio, CURRENT_DATE)` para preservar la fecha histórica real de cada inscripción antigua y, finalmente, se configura `SET DEFAULT CURRENT_DATE` para registros futuros.
   - En `V2`, se añaden `completado` (`BOOLEAN NOT NULL DEFAULT FALSE`) y `peso_kg` (`0 <= peso_kg <= 1000`) en `ejercicios_rutina`, así como `inscripcion_origen_id`, `fecha_activacion`, `fecha_cancelacion` y `motivo_cancelacion` en `inscripciones`, poblando con backfill seguro el motivo de las cancelaciones históricas previas a `V2`.
   - Se utiliza `ON DELETE RESTRICT` en las claves foráneas principales y `ON DELETE SET NULL` en `inscripcion_origen_id` para impedir borrados accidentales en cascada del historial.
