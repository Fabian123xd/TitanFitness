Sistema Web de Gestión y Seguimiento para Gimnasio

Titan Fitness Club es un sistema web desarrollado para automatizar los procesos administrativos de un gimnasio, facilitando la gestión de clientes, membresías, inscripciones, pagos y asistencias mediante una plataforma centralizada.

🎯 Objetivo

Desarrollar un sistema web que permita optimizar la gestión administrativa de un gimnasio, centralizando la información, automatizando procesos y facilitando el seguimiento de las operaciones diarias.

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

Live Server

✅ Funcionalidades implementadas

1. Dashboard

Visualización de indicadores administrativos.

Conteo de clientes, membresías, pagos y asistencias.

Accesos rápidos a los módulos principales.

Sección de seguimiento de vencimientos.

2. Gestión de clientes

Registro de nuevos clientes.

Búsqueda y listado de clientes.

Actualización de información.

Eliminación de clientes.

Validación de DNI y correo electrónico duplicados.

Consulta del historial individual de inscripciones, pagos y asistencias.

3. Gestión de membresías

Registro y administración de planes.

Configuración de precios y duración.

Actualización de membresías.

Gestión de disponibilidad.

4. Gestión de inscripciones

Asignación de membresías a clientes.

Registro de fechas de inicio y vencimiento.

Estados PENDIENTE y ACTIVA.

Validación para evitar inscripciones activas o pendientes duplicadas.

Activación de inscripciones mediante el registro de pagos.

5. Gestión de pagos

Registro de pagos de membresías.

Selección de métodos de pago.

Validación de montos.

Cálculo automático del vuelto para pagos en efectivo.

Historial de pagos registrados.

Activación automática de inscripciones después del pago.

Consulta del total recaudado.

6. Control de asistencias

Registro de asistencias de clientes.

Validación de membresías activas.

Consulta del historial de asistencias.

Visualización de indicadores de asistencia.

7. Reportes administrativos

Consulta de ingresos totales.

Resumen de clientes, pagos y asistencias.

Reporte de ingresos por rango de fechas.

Validación de fechas incorrectas.

Actualización de indicadores a partir de los registros del sistema.

🚧 Funcionalidades futuras

Las siguientes funcionalidades se consideran posibles ampliaciones del sistema:

Gestión de entrenadores.

Asignación y seguimiento de rutinas.

Mejoras en el sistema de alertas de vencimiento.

Despliegue completo de la aplicación web para acceso público.

📁 Estructura del proyecto

FitCore/
├── backend/
│   ├── src/main/
│   │   ├── java/com/fitcore/
│   │   │   ├── controller/
│   │   │   ├── model/
│   │   │   ├── repository/
│   │   │   └── service/
│   │   └── resources/
│   └── mvnw.cmd
├── frontend/
│   ├── css/
│   ├── js/
│   ├── index.html
│   ├── clientes.html
│   ├── membresias.html
│   ├── inscripciones.html
│   ├── pagos.html
│   └── reportes.html
├── .gitignore
└── README.md

⚙️ Ejecución del proyecto

1. Configurar las variables de entorno

El backend requiere las siguientes variables para conectarse a PostgreSQL en Neon:

DB_URL
DB_USERNAME
DB_PASSWORD

Las credenciales deben mantenerse fuera del repositorio.

2. Ejecutar el backend

Desde la carpeta backend, ejecutar en Windows:

.\mvnw.cmd spring-boot:run

El backend utiliza el puerto 8080 en la configuración de desarrollo.

3. Ejecutar el frontend

Abrir frontend/index.html utilizando Live Server en Visual Studio Code.

El frontend consume los servicios REST proporcionados por Spring Boot.

🏗️ Arquitectura del sistema

Titan Fitness Club utiliza una arquitectura por capas:

Controller: recibe y procesa solicitudes HTTP.

Service: implementa la lógica de negocio y las validaciones.

Repository: gestiona el acceso a la base de datos mediante JPA.

Model: representa las entidades del sistema.

Frontend: proporciona la interfaz de usuario y consume los servicios REST.

La información se almacena en PostgreSQL mediante el servicio de base de datos en la nube Neon.

🧪 Pruebas funcionales realizadas

Se han realizado pruebas manuales de las principales funcionalidades:

Registro y actualización de clientes.

Validación de DNI y correos duplicados.

Validación de inscripciones duplicadas.

Registro de pagos.

Rechazo de pagos en efectivo insuficientes.

Cálculo automático del vuelto.

Activación de inscripciones después del pago.

Consulta de historial de clientes.

Registro y consulta de asistencias.

Consulta de ingresos por fechas.

Validación de rangos de fechas incorrectos.

Visualización de indicadores del Dashboard.

Navegación mediante accesos rápidos.

📌 Estado del proyecto

Sistema en desarrollo con módulos administrativos principales implementados y sometidos a pruebas funcionales manuales.

Las funcionalidades de clientes, membresías, inscripciones, pagos, asistencias, reportes y Dashboard están disponibles en el entorno de desarrollo.

🎓 Contexto académico

Proyecto desarrollado para la carrera de Ingeniería de Sistemas de la Universidad Tecnológica del Perú (UTP).

El sistema aplica principios de Programación Orientada a Objetos, arquitectura por capas, desarrollo de API REST y persistencia de datos en la nube.

🔗 Repositorio

https://github.com/Fabian123xd/TitanFitness