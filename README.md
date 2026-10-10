🏋️ Titan Fitness Club

Sistema Web de Gestión y Seguimiento para Gimnasio

Titan Fitness Club es un sistema web desarrollado para automatizar los procesos administrativos de un gimnasio, mejorando la gestión de clientes, membresías, inscripciones, pagos y asistencias.

🎯 Objetivo

Desarrollar un sistema web que permita optimizar la gestión administrativa de un gimnasio mediante la centralización de información y la automatización de procesos.

🛠️ Tecnologías utilizadas

Backend

Java

Spring Boot

Spring Data JPA

API REST

Maven

Frontend

HTML5

CSS3

JavaScript

Bootstrap

Base de datos

PostgreSQL

Neon (base de datos en la nube)

Herramientas

Visual Studio Code

Git y GitHub

✅ Funcionalidades implementadas

Dashboard

Visualización de indicadores administrativos.

Consulta de información registrada.

Gestión de clientes

Registro, búsqueda, edición y eliminación.

Validación de DNI y correo electrónico.

Gestión de membresías

Registro y administración de planes.

Configuración de precios y duración.

Actualización y gestión de disponibilidad.

Gestión de inscripciones

Asignación de membresías a clientes.

Cálculo de fechas de inicio y vencimiento.

Estados de inscripción: PENDIENTE y ACTIVA.

Validación de inscripciones duplicadas.

Gestión de pagos

Registro de pagos.

Diferentes métodos de pago.

Cálculo automático del vuelto en efectivo.

Historial de pagos.

Activación automática de inscripciones después del pago.

🚧 Funcionalidades en desarrollo

Control de asistencias.

Gestión de entrenadores y rutinas.

Alertas de vencimiento.

Reportes administrativos.

Despliegue del sistema.

📁 Estructura del proyecto

FitCore/
├── backend/
│   └── src/main/
│       ├── java/com/fitcore/
│       │   ├── controller/
│       │   ├── model/
│       │   ├── repository/
│       │   └── service/
│       └── resources/
├── frontend/
│   ├── css/
│   ├── js/
│   ├── index.html
│   ├── clientes.html
│   ├── membresias.html
│   ├── inscripciones.html
│   └── pagos.html
├── .gitignore
└── README.md

⚙️ Ejecución del proyecto

1. Configurar las variables de entorno

El backend requiere las siguientes variables:

DB_URL
DB_USERNAME
DB_PASSWORD

Las credenciales deben mantenerse fuera del repositorio.

2. Ejecutar el backend

Desde la carpeta backend:

.\mvnw.cmd spring-boot:run

3. Ejecutar el frontend

Abrir frontend/index.html utilizando Live Server en Visual Studio Code.

🏗️ Arquitectura

El sistema utiliza una arquitectura por capas:

Controller: gestión de solicitudes HTTP.

Service: lógica de negocio.

Repository: acceso a la base de datos.

Model: entidades del sistema.

Frontend: interfaz de usuario.

📌 Estado del proyecto

En desarrollo. Los módulos de clientes, membresías, inscripciones y pagos se encuentran implementados y probados.

🎓 Contexto académico

Proyecto desarrollado para la carrera de Ingeniería de Sistemas de la Universidad Tecnológica del Perú (UTP).

🔗 Repositorio

https://github.com/Fabian123xd/TitanFitness