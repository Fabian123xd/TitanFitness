# 🏋️ Titan Fitness Club — Edición Titan Cobalt Pro

## Sistema Web de Gestión de Membresías, Inscripciones y Rutinas de Entrenamiento

**Titan Fitness Club** es un sistema web universitario de nivel comercial desarrollado con **Java 21**, **Spring Boot 4.1.1**, **Spring Data JPA**, **Spring Security (BCrypt)**, **PostgreSQL (Neon Cloud)** y **Bootstrap 5 (Diseño Titan Cobalt Pro)**, especializado en dos procesos centrales: **Gestión de Membresías e Inscripciones** y **Gestión de Rutinas y Ejercicios**.

---

## 📚 Documentación Técnica (`docs/`)

Toda la documentación técnica detallada se encuentra organizada en la carpeta [`docs/`](docs/):

1. [**01 — Arquitectura y Estructura del Proyecto**](docs/01-arquitectura-y-estructura.md): Organización de paquetes del backend (`config`, `security`, `controller`, `service`, `repository`, `model`, `dto`, `exception`) y arquitectura del frontend **Titan Cobalt Pro**.
2. [**02 — Modelo de Datos y Migración SQL**](docs/02-modelo-datos-y-migracion.md): Diagrama Entidad-Relación (MER), relaciones JPA, prevención de `SQLState: 23505` en `dias_rutina` y migración segura `V1__membresias_rutinas_auth.sql`.
3. [**03 — Reglas de Negocio y Endpoints REST**](docs/03-reglas-negocio-y-endpoints.md): Catálogo completo de reglas de negocio (`MB`, `IN`, `RU`) y tabla de endpoints REST protegidos.
4. [**04 — Instalación, Ejecución y Pruebas Automatizadas**](docs/04-instalacion-y-pruebas.md): Guía paso a paso para ejecutar el proyecto en Windows (modo aislado o conectado a Neon Cloud) y detalle de las **65 pruebas automatizadas**.

---

## 🗂️ Estructura del Repositorio

```text
FitCore/
├── backend/
│   ├── src/main/java/com/fitcore/
│   │   ├── config/          # AppConfig (Clock, @EnableScheduling)
│   │   ├── security/        # SecurityConfig, UsuarioDetailsService, AdminInitializer
│   │   ├── controller/      # Auth, Reporte, Cliente, Membresia, Inscripcion, Entrenador, Rutina
│   │   ├── service/         # Reporte, Cliente, Membresia, Inscripcion, CalendarioLaboral,
│   │   │                    # VencimientoScheduler, Entrenador, Rutina
│   │   ├── repository/      # Interfaces Spring Data JPA
│   │   ├── model/           # Cliente, Membresia, Inscripcion, EstadoInscripcion, Entrenador,
│   │   │                    # Rutina, DiaRutina, DiaSemana, EjercicioRutina, Usuario
│   │   ├── dto/             # Records de entrada/salida (Inscripcion*, Rutina*, Dia*, Ejercicio*)
│   │   ├── exception/       # GlobalExceptionHandler, ErrorResponse, excepciones de negocio
│   │   └── BackendApplication.java
│   ├── src/main/resources/
│   │   ├── db/migration/V1__membresias_rutinas_auth.sql
│   │   ├── db/isolated/schema-isolated.sql
│   │   ├── application.properties
│   │   └── application-isolated.properties
│   ├── src/test/
│   │   ├── java/com/fitcore/
│   │   │   ├── integration/ # ApiIntegrationTest, MigracionValidateIntegrationTest
│   │   │   ├── service/     # CalendarioLaboralTest, InscripcionServiceTest,
│   │   │   │                # MembresiaServiceTest, RutinaServiceTest
│   │   │   └── BackendApplicationTests.java
│   │   └── resources/application.properties
│   ├── pom.xml
│   └── mvnw.cmd
├── frontend/
│   ├── css/style.css        # Sistema visual Titan Cobalt Pro
│   ├── js/
│   │   ├── auth.js          # Configuración API centralizada, Auth Basic, sesión y utilidades
│   │   ├── app.js           # Dashboard ejecutivo y radar de alertas (7 días hábiles)
│   │   ├── clientes.js      # Gestión de clientes e historial
│   │   ├── membresias.js    # Catálogo visual y técnico de planes
│   │   ├── inscripciones.js # Asignación, activación sin pagos y renovaciones
│   │   ├── entrenadores.js  # Gestión de entrenadores activos/inactivos
│   │   └── rutinas.js       # Constructor de rutinas, días y ejercicios
│   ├── login.html
│   ├── index.html
│   ├── clientes.html
│   ├── membresias.html
│   ├── inscripciones.html
│   ├── entrenadores.html
│   └── rutinas.html
├── docs/                    # Documentación técnica detallada
├── .gitignore
└── README.md
```

---

## 🚀 Inicio Rápido (Modo Aislado con `ddl-auto=validate`)

```powershell
# 1. Ejecutar las 65 pruebas automatizadas
cd backend
.\mvnw.cmd clean test

# 2. Iniciar el backend en http://localhost:8080 (sin tocar Neon Cloud)
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=isolated"
```

Luego abre `frontend/login.html` con **Live Server** e inicia sesión con `admin` / `AdminTitan2026`.
