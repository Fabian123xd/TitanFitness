const API_ASISTENCIAS = "http://localhost:8080/api/asistencias";
const API_INSCRIPCIONES = "http://localhost:8080/api/inscripciones";

let asistencias = [];

// Cargar asistencias desde Spring Boot
async function cargarAsistencias() {
    const tabla = document.getElementById("tablaAsistencias");

    try {
        const respuesta = await fetch(API_ASISTENCIAS);

        if (!respuesta.ok) {
            throw new Error("No se pudieron cargar las asistencias");
        }

        asistencias = await respuesta.json();

        mostrarAsistencias(asistencias);
        actualizarIndicadores();

    } catch (error) {
        console.error(error);
        tabla.innerHTML = `
            <tr>
                <td colspan="6" class="text-center text-danger py-4">
                    Error al conectar con el servidor
                </td>
            </tr>
        `;
    }
}

// Mostrar historial de ingresos
function mostrarAsistencias(datos) {
    const tabla = document.getElementById("tablaAsistencias");
    tabla.innerHTML = "";

    if (datos.length === 0) {
        tabla.innerHTML = `
            <tr>
                <td colspan="6" class="text-center py-4">
                    No hay asistencias registradas
                </td>
            </tr>
        `;
        return;
    }

    const ordenadas = [...datos].sort(
        (a, b) => new Date(b.fechaHora) - new Date(a.fechaHora)
    );

    ordenadas.forEach(asistencia => {
        const fila = document.createElement("tr");

        const cliente = asistencia.cliente;
        const membresia = asistencia.inscripcion?.membresia;

        const nombre = cliente
            ? `${cliente.nombres} ${cliente.apellidos}`
            : "Sin cliente";

        const fechaHora = asistencia.fechaHora
            ? new Date(asistencia.fechaHora)
            : null;

        const fecha = fechaHora
            ? fechaHora.toLocaleDateString("es-PE")
            : "-";

        const hora = fechaHora
            ? fechaHora.toLocaleTimeString("es-PE", {
                hour: "2-digit",
                minute: "2-digit"
            })
            : "-";

        const valores = [
            asistencia.id,
            nombre,
            membresia?.nombre || "-",
            fecha,
            hora
        ];

        valores.forEach(valor => {
            const celda = document.createElement("td");
            celda.textContent = valor;
            fila.appendChild(celda);
        });

        const celdaEstado = document.createElement("td");
        const etiqueta = document.createElement("span");

        etiqueta.className = "badge bg-success";
        etiqueta.textContent = asistencia.estado || "INGRESO";

        celdaEstado.appendChild(etiqueta);
        fila.appendChild(celdaEstado);

        tabla.appendChild(fila);
    });
}

// Actualizar tarjetas de resumen
function actualizarIndicadores() {
    const hoy = obtenerFechaLocal();
    const mesActual = hoy.slice(0, 7);

    const ingresosHoy = asistencias.filter(a =>
        a.fechaHora?.startsWith(hoy)
    );

    const ingresosMes = asistencias.filter(a =>
        a.fechaHora?.startsWith(mesActual)
    );

    const clientesUnicos = new Set(
        ingresosHoy.map(a => a.cliente?.id).filter(Boolean)
    );

    document.getElementById("totalAsistencias").textContent =
        asistencias.length;

    document.getElementById("asistenciasHoy").textContent =
        ingresosHoy.length;

    document.getElementById("clientesHoy").textContent =
        clientesUnicos.size;

    document.getElementById("asistenciasMes").textContent =
        ingresosMes.length;
}

function obtenerFechaLocal() {
    const fecha = new Date();
    const anio = fecha.getFullYear();
    const mes = String(fecha.getMonth() + 1).padStart(2, "0");
    const dia = String(fecha.getDate()).padStart(2, "0");

    return `${anio}-${mes}-${dia}`;
}

// Cargar clientes con inscripciones vigentes
async function cargarInscripciones() {
    const selector = document.getElementById("inscripcionId");

    selector.innerHTML =
        '<option value="">Seleccionar cliente</option>';

    try {
        const respuesta = await fetch(API_INSCRIPCIONES);

        if (!respuesta.ok) {
            throw new Error("Error al cargar inscripciones");
        }

        const inscripciones = await respuesta.json();
        const hoy = obtenerFechaLocal();

        const vigentes = inscripciones.filter(inscripcion =>
            inscripcion.estado?.toUpperCase() === "ACTIVA" &&
            inscripcion.fechaInicio <= hoy &&
            inscripcion.fechaFin >= hoy
        );

        vigentes.forEach(inscripcion => {
            const opcion = document.createElement("option");

            opcion.value = inscripcion.id;

            const cliente = inscripcion.cliente;
            const membresia = inscripcion.membresia;

            opcion.textContent =
                `${cliente.nombres} ${cliente.apellidos} - ${membresia.nombre}`;

            selector.appendChild(opcion);
        });

        if (vigentes.length === 0) {
            document.getElementById("mensajeAsistencia").textContent =
                "No hay clientes con membresías vigentes.";
        }

    } catch (error) {
        console.error(error);
        document.getElementById("mensajeAsistencia").textContent =
            "No se pudieron cargar las inscripciones.";
    }
}

// Registrar ingreso
document.getElementById("formAsistencia").addEventListener(
    "submit",
    async function (evento) {
        evento.preventDefault();

        const inscripcionId =
            document.getElementById("inscripcionId").value;

        const boton =
            document.getElementById("btnGuardarAsistencia");

        const mensaje =
            document.getElementById("mensajeAsistencia");

        if (!inscripcionId) {
            mensaje.textContent = "Selecciona un cliente.";
            return;
        }

        if (boton.disabled) {
    return;
}

boton.disabled = true;
mensaje.textContent = "";
        try {
            const respuesta = await fetch(
                `${API_ASISTENCIAS}?inscripcionId=${encodeURIComponent(inscripcionId)}`,
                { method: "POST" }
            );

          if (!respuesta.ok) {
    const detalle = await respuesta.text();

    throw new Error(
        detalle || "No se pudo registrar el ingreso."
    );
}

            const modal = bootstrap.Modal.getInstance(
                document.getElementById("modalAsistencia")
            );

            modal.hide();

            document.getElementById("formAsistencia").reset();

            await cargarAsistencias();

            alert("Ingreso registrado correctamente.");

        } catch (error) {
            mensaje.textContent = error.message;
            mensaje.className = "small text-danger";

        } finally {
            boton.disabled = false;
        }
    }
);

// Buscar por cliente
document.getElementById("buscarAsistencia").addEventListener(
    "input",
    function () {
        const busqueda = this.value.toLowerCase().trim();

        const filtradas = asistencias.filter(a => {
            const nombre =
                `${a.cliente?.nombres || ""} ${a.cliente?.apellidos || ""}`;

            return nombre.toLowerCase().includes(busqueda);
        });

        mostrarAsistencias(filtradas);
    }
);

// Preparar modal
document.getElementById("modalAsistencia").addEventListener(
    "show.bs.modal",
    function () {
        const mensaje = document.getElementById("mensajeAsistencia");
        mensaje.textContent = "";
        mensaje.className = "small";

        document.getElementById("formAsistencia").reset();
        cargarInscripciones();
    }
);

// Cargar datos al abrir la página
cargarAsistencias();