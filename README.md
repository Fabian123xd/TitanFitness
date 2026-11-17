# 🏋️ Titan Fitness Club — Edición Titan Cobalt Pro

## Sistema Web de Gestión de Membresías, Inscripciones y Rutinas de Entrenamiento

**Titan Fitness Club** es un sistema web de nivel comercial desarrollado con **Java 21**, **Spring Boot 4.1.1**, **Spring Data JPA (Hibernate)**, **Spring Security (BCrypt)**, **PostgreSQL (Neon Cloud)** y **Bootstrap 5 (Diseño Titan Cobalt Pro)**, especializado exclusivamente en dos procesos centrales: **Gestión de Membresías e Inscripciones** y **Gestión de Rutinas de Entrenamiento**.

---

## 🌟 Funcionalidades Principales

1. **Gestión de Membresías, Inscripciones y Trazabilidad Completa**:
   - Catálogo visual y técnico de planes de membresía con activación/desactivación (`/toggle`).
   - Asignación de membresías con cálculo automático de vencimiento y vista previa en vivo.
   - **Activación directa e inmediata** (`PUT /api/inscripciones/{id}/activar`), registrando fecha y hora exacta de activación (`fechaActivacion`).
   - **Cancelación auditada** (`PUT /api/inscripciones/{id}/cancelar`) con registro de `fechaCancelacion` y `motivoCancelacion`.
   - **Renovación con trazabilidad real** (`POST /api/inscripciones/{id}/renovar`): vincula `inscripcionOrigenId`, evita renovaciones duplicadas, adquiere bloqueo pesimista (`PESSIMISTIC_WRITE`) por cliente y muestra la línea de tiempo cronológica en el expediente del socio.
   - **Alertas de vencimiento con 7 días hábiles de anticipación** (`CalendarioLaboral`), excluyendo sábados, domingos y feriados configurables.

2. **Gestión de Rutinas y Seguimiento del Progreso de Ejercicios**:
   - Modelo normalizado `Rutina` → `DiaRutina` (`DiaSemana`) → `EjercicioRutina` (series, repeticiones, descanso en segundos, `pesoKg` y `completado`).
   - **Seguimiento de avance en tiempo real**: marcado de ejercicios completados (`PATCH /api/rutinas/{rutinaId}/ejercicios/{ejercicioId}/toggle-completado`), actualización de peso utilizado (`PATCH /api/rutinas/{rutinaId}/ejercicios/{ejercicioId}/peso`) y cálculo automático de `porcentajeProgreso` (`0%` a `100%`).
   - Validación de entrenadores activos y sincronización inteligente de días para evitar errores `SQLState: 23505`.

3. **Seguridad y Rendimiento**:
   - Autenticación HTTP Basic con **BCrypt**, cambio seguro de contraseña (`PUT /api/auth/password`), enfriamiento temporal ante intentos fallidos en login y **cierre automático de sesión por inactividad**.
   - Optimización JPA con `spring.jpa.open-in-view=false`, consultas `JOIN FETCH`, conteos directos `countByActivoTrue()` y validación de esquema `spring.jpa.hibernate.ddl-auto=validate`.

---

## 📚 Documentación Técnica (`docs/`)

1. [**01 — Arquitectura y Estructura del Proyecto**](docs/01-arquitectura-y-estructura.md)
2. [**02 — Modelo de Datos y Migración SQL**](docs/02-modelo-datos-y-migracion.md)
3. [**03 — Reglas de Negocio y Endpoints REST**](docs/03-reglas-negocio-y-endpoints.md)
4. [**04 — Instalación, Ejecución y Pruebas Automatizadas**](docs/04-instalacion-y-pruebas.md)
5. [**05 — Seguimiento del Progreso de Rutinas y Ejercicios**](docs/05-seguimiento-progreso-rutinas.md)
6. [**06 — Trazabilidad de Membresías y Control de Concurrencia**](docs/06-trazabilidad-membresias.md)
7. [**07 — Configuración de Neon Cloud PostgreSQL y Migraciones V1/V2**](docs/07-configuracion-neon-postgresql.md)

---

## 🚀 Ejecución Rápida y Pruebas (71 Pruebas Automatizadas)

```powershell
# 1. Ejecutar las 71 pruebas automatizadas
cd backend
.\mvnw.cmd clean test

# 2. Iniciar el backend en modo aislado con ddl-auto=validate (sin modificar Neon Cloud)
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=isolated"
```

Luego abre `frontend/login.html` con **Live Server** e ingresa con `admin` / `AdminTitan2026`.
