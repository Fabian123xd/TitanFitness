# 🏋️ Titan Fitness Club

## Sistema Web de Gestión y Seguimiento para Gimnasio

Titan Fitness Club es un sistema web desarrollado para automatizar y optimizar los procesos administrativos de un gimnasio.

Permite gestionar clientes, membresías, inscripciones, pagos, asistencias, entrenadores y rutinas desde una plataforma centralizada, facilitando el control de las operaciones diarias.

El sistema utiliza una arquitectura por capas, una API REST desarrollada con Spring Boot y una base de datos PostgreSQL alojada en Neon Cloud.

---

## 🎯 Objetivo general

Desarrollar un sistema web que permita optimizar la gestión administrativa de un gimnasio mediante la automatización de procesos, centralización de información y seguimiento de las operaciones.

### Objetivos específicos

- Automatizar el registro y administración de clientes.
- Gestionar membresías e inscripciones.
- Controlar los pagos realizados.
- Registrar las asistencias de los clientes.
- Administrar entrenadores y rutinas de entrenamiento.
- Generar reportes administrativos.
- Almacenar información en una base de datos en la nube.

---

## 🛠️ Tecnologías utilizadas

### Backend

- Java
- Spring Boot
- Spring Data JPA
- Hibernate
- Maven
- API REST

### Frontend

- HTML5
- CSS3
- JavaScript
- Bootstrap 5
- Font Awesome

### Base de datos

- PostgreSQL
- Neon Cloud

### Herramientas

- Visual Studio Code
- Git
- GitHub
- Live Server
- pgAdmin 4

---

## ✅ Funcionalidades implementadas

### 1. Dashboard

- Visualización de indicadores administrativos.
- Conteo de clientes, membresías, pagos y asistencias.
- Accesos rápidos a los módulos principales.
- Seguimiento de vencimientos.
- Visualización de información actualizada.

### 2. Gestión de clientes

- Registro de nuevos clientes.
- Listado y búsqueda de clientes.
- Actualización de información.
- Eliminación de clientes.
- Validación de DNI y correo duplicados.
- Consulta del historial individual de clientes.

### 3. Gestión de membresías

- Registro de planes de membresía.
- Configuración de precios.
- Configuración de duración.
- Actualización de membresías.
- Gestión de disponibilidad.

### 4. Gestión de inscripciones

- Asignación de membresías a clientes.
- Registro de fechas de inicio y vencimiento.
- Control de estados de inscripción.
- Validación de inscripciones duplicadas.
- Activación de inscripciones mediante pagos.

### 5. Gestión de pagos

- Registro de pagos de membresías.
- Selección de métodos de pago.
- Validación de montos.
- Cálculo automático del vuelto.
- Historial de pagos.
- Activación automática de inscripciones.
- Consulta de ingresos.

### 6. Control de asistencias

- Registro de asistencias.
- Validación de membresías activas.
- Consulta del historial de asistencias.
- Visualización de indicadores.

### 7. Reportes administrativos

- Consulta de ingresos totales.
- Resumen de clientes, pagos y asistencias.
- Reportes por rango de fechas.
- Validación de fechas.
- Actualización de indicadores.

### 8. Gestión de entrenadores

- Registro de entrenadores.
- Listado de entrenadores.
- Actualización de información.
- Registro de especialidad y contacto.
- Activación de entrenadores.
- Desactivación de entrenadores.
- Visualización de estados.

### 9. Gestión de rutinas

- Registro de rutinas de entrenamiento.
- Asignación de rutinas a clientes.
- Asignación de entrenadores.
- Registro de nombre y descripción.
- Fecha automática de asignación.
- Listado de rutinas.
- Actualización de rutinas.
- Desactivación de rutinas.
- Reactivación de rutinas.
- Validación de entrenadores activos.

---

## 🏗️ Arquitectura del sistema

El proyecto utiliza una arquitectura por capas.

### Controller

Recibe las solicitudes HTTP y expone los endpoints de la API REST.

### Service

Contiene la lógica de negocio y las validaciones del sistema.

### Repository

Gestiona las operaciones de persistencia mediante Spring Data JPA.

### Model

Representa las entidades del sistema y sus relaciones.

### Frontend

Proporciona las interfaces HTML y consume los servicios REST mediante JavaScript.

### Base de datos

PostgreSQL almacena la información en Neon Cloud.

---

## 📁 Estructura del proyecto

```text
FitCore/
│
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/fitcore/
│   │   │   │   ├── controller/
│   │   │   │   ├── model/
│   │   │   │   ├── repository/
│   │   │   │   └── service/
│   │   │   └── resources/
│   │   └── test/
│   ├── pom.xml
│   └── mvnw.cmd
│
├── frontend/
│   ├── css/
│   ├── js/
│   │   ├── app.js
│   │   ├── clientes.js
│   │   ├── membresias.js
│   │   ├── inscripciones.js
│   │   ├── pagos.js
│   │   ├── asistencias.js
│   │   ├── reportes.js
│   │   ├── entrenadores.js
│   │   └── rutinas.js
│   │
│   ├── index.html
│   ├── clientes.html
│   ├── membresias.html
│   ├── inscripciones.html
│   ├── pagos.html
│   ├── asistencias.html
│   ├── reportes.html
│   ├── entrenadores.html
│   └── rutinas.html
│
├── .gitignore
└── README.md
```

---

## 🗄️ Base de datos en la nube

El sistema utiliza PostgreSQL alojado en Neon Cloud.

### Tablas principales

1. clientes
2. membresias
3. inscripciones
4. pagos
5. asistencias
6. entrenadores
7. rutinas

Las entidades están relacionadas mediante claves primarias y foráneas.

La conexión se realiza mediante Spring Data JPA e Hibernate.

---

## ⚙️ Ejecución del proyecto

### 1. Requisitos

- Java compatible con el proyecto.
- Maven Wrapper incluido.
- Visual Studio Code.
- Conexión a internet.
- Base de datos PostgreSQL en Neon.

### 2. Configurar variables de entorno

El backend requiere:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
```

Estas variables deben contener las credenciales correspondientes a la base de datos Neon.

Por seguridad, no deben publicarse en GitHub.

### 3. Ejecutar el backend

Abrir una terminal dentro de la carpeta `backend`.

En Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

El servidor se ejecuta en:

```text
http://localhost:8080
```

### 4. Ejecutar el frontend

Abrir la carpeta `frontend` en Visual Studio Code.

Ejecutar `index.html` utilizando Live Server.

El frontend consume los endpoints REST del backend.

---

## 🔗 API REST

El sistema dispone de los siguientes endpoints principales:

| Módulo | Endpoint |
|---|---|
| Clientes | `/api/clientes` |
| Membresías | `/api/membresias` |
| Inscripciones | `/api/inscripciones` |
| Pagos | `/api/pagos` |
| Asistencias | `/api/asistencias` |
| Reportes | `/api/reportes` |
| Entrenadores | `/api/entrenadores` |
| Rutinas | `/api/rutinas` |

### Ejemplo: gestión de rutinas

```http
GET /api/rutinas
POST /api/rutinas
PUT /api/rutinas/{id}
PUT /api/rutinas/{id}/desactivar
PUT /api/rutinas/{id}/reactivar
```

El registro y la actualización de rutinas utilizan parámetros de solicitud.

---

## 🧪 Pruebas funcionales

Se han realizado pruebas manuales de las principales funcionalidades del sistema.

### Clientes

- Registro y actualización.
- Validación de DNI duplicado.
- Validación de correo duplicado.
- Consulta del historial individual.

### Inscripciones y pagos

- Registro de inscripciones.
- Validación de duplicados.
- Registro de pagos.
- Validación de montos.
- Cálculo del vuelto.
- Activación automática de inscripciones.

### Asistencias y reportes

- Registro de asistencias.
- Consulta de historial.
- Consulta de ingresos por fechas.
- Visualización de indicadores.

### Entrenadores

- Registro de entrenadores.
- Actualización de información.
- Desactivación.
- Reactivación.

### Rutinas

- Registro de rutinas.
- Asignación a clientes y entrenadores.
- Actualización de información.
- Desactivación.
- Reactivación.
- Comprobación de cambios en PostgreSQL Neon.

### Pruebas automatizadas

El proyecto incluye una prueba básica de carga del contexto de Spring Boot.

Queda pendiente ampliar la cobertura de pruebas unitarias y de integración.

---

## 🔒 Seguridad y buenas prácticas

- Separación del código en capas.
- Uso de repositorios JPA.
- Validaciones de reglas de negocio.
- Gestión de versiones mediante Git.
- Exclusión de credenciales mediante `.gitignore`.
- Uso de variables de entorno para conexión a la base de datos.

El sistema se encuentra configurado principalmente para un entorno de desarrollo académico.

Antes de publicarlo en producción se recomienda implementar autenticación, autorización y una configuración CORS restringida.

---

## 🚀 Mejoras futuras

- Implementación de autenticación y roles.
- Ampliación de pruebas automatizadas.
- Mejoras en las alertas de vencimiento.
- Generación de reportes descargables.
- Optimización de la interfaz para dispositivos móviles.
- Despliegue público del backend y frontend.

---

## 📌 Estado del proyecto

Los módulos administrativos principales están implementados y disponibles en el entorno de desarrollo.

El sistema integra frontend, backend y base de datos PostgreSQL en Neon Cloud.

Las funcionalidades principales han sido sometidas a pruebas manuales.

El proyecto continúa en etapa de validación y mejora antes de su despliegue público.

---

## 🎓 Contexto académico

Proyecto desarrollado para la carrera de Ingeniería de Sistemas de la Universidad Tecnológica del Perú (UTP).

El sistema aplica conceptos de:

- Programación Orientada a Objetos.
- Arquitectura por capas.
- Desarrollo de API REST.
- Persistencia de datos.
- Bases de datos relacionales.
- Servicios de base de datos en la nube.
- Control de versiones con Git y GitHub.

---

## 🔗 Repositorio GitHub

https://github.com/Fabian123xd/TitanFitness