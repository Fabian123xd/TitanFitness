const API_CLIENTES = ENDPOINTS.CLIENTES;
const API_INSCRIPCIONES = ENDPOINTS.INSCRIPCIONES;
const API_RUTINAS = ENDPOINTS.RUTINAS;

function mostrarAlertaCliente(mensaje, tipo = "success") {
    mostrarAlertaUI("alertaClientes", mensaje, tipo, 4500);
}

async function cargarClientes() {
    const tabla = document.getElementById("tablaClientes");

    try {
        const respuesta = await fetch(API_CLIENTES);
        if (!respuesta.ok) {
            throw new Error("No se pudieron obtener los clientes");
        }

        const clientes = await respuesta.json();

        const contador = document.getElementById("contadorClientes");
        const contadorCorreo = document.getElementById("contadorClientesCorreo");
        const contadorTelefono = document.getElementById("contadorClientesTelefono");

        if (contador) contador.textContent = clientes.length;
        if (contadorCorreo) {
            contadorCorreo.textContent = clientes.filter(c => c.correo && String(c.correo).trim() !== "").length;
        }
        if (contadorTelefono) {
            contadorTelefono.textContent = clientes.filter(c => c.telefono && String(c.telefono).trim() !== "").length;
        }

        tabla.innerHTML = "";

        if (clientes.length === 0) {
            tabla.innerHTML = `
                <tr>
                    <td colspan="8" class="text-center text-muted py-4">
                        No hay clientes registrados.
                    </td>
                </tr>
            `;
            return;
        }

        clientes.forEach(cliente => {
            const fila = document.createElement("tr");
            const iniciales = obtenerIniciales(`${cliente.nombres || ""} ${cliente.apellidos || ""}`);
            fila.innerHTML = `
                <td class="text-muted fw-semibold">#${cliente.id}</td>
                <td>
                    <div class="d-flex align-items-center gap-2">
                        <span class="avatar-chip">${iniciales}</span>
                        <span class="fw-semibold" style="color: var(--navy-900);">${cliente.nombres}</span>
                    </div>
                </td>
                <td class="fw-medium">${cliente.apellidos}</td>
                <td><span class="badge bg-primary">${cliente.dni}</span></td>
                <td>${cliente.telefono || "-"}</td>
                <td>${cliente.correo || "-"}</td>
                <td>${cliente.fechaRegistro || "-"}</td>
                <td class="text-center text-nowrap">
                    <button class="btn btn-sm btn-info text-white me-1"
                            onclick="verHistorial(${cliente.id})"
                            title="Ver historial de membresías y rutinas">
                        <i class="fa-solid fa-clock-rotate-left"></i>
                    </button>
                    <button class="btn btn-sm btn-warning me-1"
                            onclick="editarCliente(${cliente.id})"
                            title="Editar cliente">
                        <i class="fa-solid fa-pen"></i>
                    </button>
                    <button class="btn btn-sm btn-danger"
                            onclick="eliminarCliente(${cliente.id})"
                            title="Eliminar cliente">
                        <i class="fa-solid fa-trash"></i>
                    </button>
                </td>
            `;
            tabla.appendChild(fila);
        });
    } catch (error) {
        console.error(error);
        tabla.innerHTML = `
            <tr>
                <td colspan="8" class="text-center text-danger py-4">
                    Error al cargar los clientes.
                </td>
            </tr>
        `;
    }
}

document.addEventListener("DOMContentLoaded", cargarClientes);

const formCliente = document.getElementById("formCliente");

formCliente.addEventListener("submit", async function (event) {
    event.preventDefault();

    const correoVal = document.getElementById("correo").value.trim();
    const cliente = {
        nombres: document.getElementById("nombres").value.trim(),
        apellidos: document.getElementById("apellidos").value.trim(),
        dni: document.getElementById("dni").value.trim(),
        telefono: document.getElementById("telefono").value.trim() || null,
        correo: correoVal || null
    };

    const clienteId = formCliente.dataset.clienteId;
    const url = clienteId ? `${API_CLIENTES}/${clienteId}` : API_CLIENTES;
    const metodo = clienteId ? "PUT" : "POST";

    try {
        const respuesta = await fetch(url, {
            method: metodo,
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(cliente)
        });

        if (!respuesta.ok) {
            const msg = await extraerMensajeError(respuesta, "No se pudo guardar el cliente");
            throw new Error(msg);
        }

        formCliente.reset();
        delete formCliente.dataset.clienteId;

        document.querySelector("#modalCliente .modal-title").textContent = "Registrar nuevo cliente";
        const modalElemento = document.getElementById("modalCliente");
        const modal = bootstrap.Modal.getInstance(modalElemento);
        if (modal) modal.hide();

        await cargarClientes();
        mostrarAlertaCliente(
            clienteId ? "Cliente actualizado correctamente." : "Cliente registrado correctamente.",
            "success"
        );
    } catch (error) {
        console.error(error);
        mostrarAlertaCliente(error.message, "danger");
    }
});

function nuevoCliente() {
    formCliente.reset();
    delete formCliente.dataset.clienteId;
    document.querySelector("#modalCliente .modal-title").textContent = "Registrar nuevo cliente";
}

async function editarCliente(id) {
    try {
        const respuesta = await fetch(`${API_CLIENTES}/${id}`);
        if (!respuesta.ok) {
            throw new Error("No se pudo obtener el cliente");
        }

        const cliente = await respuesta.json();
        document.getElementById("nombres").value = cliente.nombres;
        document.getElementById("apellidos").value = cliente.apellidos;
        document.getElementById("dni").value = cliente.dni;
        document.getElementById("telefono").value = cliente.telefono ?? "";
        document.getElementById("correo").value = cliente.correo ?? "";

        formCliente.dataset.clienteId = cliente.id;
        document.querySelector("#modalCliente .modal-title").textContent = "Editar cliente";

        const modal = bootstrap.Modal.getOrCreateInstance(document.getElementById("modalCliente"));
        modal.show();
    } catch (error) {
        console.error(error);
        mostrarAlertaCliente("Error al cargar el cliente", "danger");
    }
}

async function eliminarCliente(id) {
    if (!confirm("¿Estás seguro de que deseas eliminar este cliente?")) {
        return;
    }

    try {
        const respuesta = await fetch(`${API_CLIENTES}/${id}`, { method: "DELETE" });
        if (!respuesta.ok) {
            const msg = await extraerMensajeError(
                respuesta,
                "No se puede eliminar un cliente con inscripciones o rutinas en su historial."
            );
            throw new Error(msg);
        }

        await cargarClientes();
        mostrarAlertaCliente("Cliente eliminado correctamente.", "success");
    } catch (error) {
        console.error(error);
        mostrarAlertaCliente(error.message, "danger");
    }
}

const buscarCliente = document.getElementById("buscarCliente");
if (buscarCliente) {
    buscarCliente.addEventListener("input", function () {
        const texto = this.value.toLowerCase().trim();
        const filas = document.querySelectorAll("#tablaClientes tr");
        filas.forEach(fila => {
            const contenido = fila.textContent.toLowerCase();
            fila.style.display = contenido.includes(texto) ? "" : "none";
        });
    });
}

async function verHistorial(id) {
    const modal = bootstrap.Modal.getOrCreateInstance(document.getElementById("modalHistorial"));

    document.getElementById("nombreClienteHistorial").textContent = "Cargando cliente...";
    document.getElementById("dniClienteHistorial").textContent = "";
    document.getElementById("historialInscripciones").innerHTML = '<div class="text-muted">Cargando inscripciones...</div>';
    document.getElementById("historialRutinas").innerHTML = '<div class="text-muted">Cargando rutinas...</div>';
    document.getElementById("linkInscribirCliente").href = `inscripciones.html?clienteId=${id}`;
    document.getElementById("linkRutinaCliente").href = `rutinas.html?clienteId=${id}`;

    modal.show();

    try {
        const [resCliente, resInscripciones, resRutinas] = await Promise.all([
            fetch(`${API_CLIENTES}/${id}`),
            fetch(`${API_INSCRIPCIONES}/cliente/${id}`),
            fetch(`${API_RUTINAS}/cliente/${id}`)
        ]);

        if (!resCliente.ok) throw new Error("No se pudo obtener el cliente");
        const cliente = await resCliente.json();

        document.getElementById("nombreClienteHistorial").textContent =
            `${cliente.nombres} ${cliente.apellidos}`;
        document.getElementById("dniClienteHistorial").textContent =
            `DNI: ${cliente.dni} · Teléfono: ${cliente.telefono || "-"} · Correo: ${cliente.correo || "-"}`;

        const inscripciones = resInscripciones.ok ? await resInscripciones.json() : [];
        const contenedorIns = document.getElementById("historialInscripciones");
        const badgeIns = document.getElementById("resumenHistorialIns");
        if (badgeIns) badgeIns.textContent = `${inscripciones.length} registro(s)`;

        if (inscripciones.length === 0) {
            contenedorIns.innerHTML = '<div class="alert alert-light border mb-0">Este cliente aún no registra inscripciones de membresía.</div>';
        } else {
            // Línea de tiempo cronológica (de la primera inscripción a las renovaciones posteriores)
            const cronologicas = [...inscripciones].sort((a, b) => Number(a.id) - Number(b.id));
            const resumenCadena = cronologicas.map(i =>
                i.inscripcionOrigenId ? `Renovada en #${i.id} (desde #${i.inscripcionOrigenId})` : `Inscripción inicial #${i.id}`
            ).join(" → ");

            contenedorIns.innerHTML = `
                <div class="p-2 px-3 mb-2 rounded-3 bg-primary bg-opacity-10 border border-primary border-opacity-25 small text-primary fw-semibold">
                    <i class="fa-solid fa-timeline me-1"></i>Cadena de trazabilidad: ${resumenCadena}
                </div>
            `;

            inscripciones.forEach(ins => {
                const div = document.createElement("div");
                div.className = "border rounded-3 p-3 mb-2 bg-white shadow-xs";
                const plan = ins.nombreMembresia || (ins.membresia ? ins.membresia.nombre : "Plan");
                const alerta = ins.proximaAVencer
                    ? `<span class="badge bg-warning text-dark ms-1">Vence en ${ins.diasHabilesRestantes} día(s) hábiles</span>`
                    : "";
                const origenBadge = ins.inscripcionOrigenId
                    ? `<span class="badge bg-primary ms-1">Renovada desde #${ins.inscripcionOrigenId}</span>`
                    : `<span class="badge bg-secondary ms-1">Inicial</span>`;
                const trazabilidad = [
                    ins.fechaActivacion ? `Activada: ${formatearFechaHoraLegible(ins.fechaActivacion)}` : null,
                    ins.fechaCancelacion ? `Cancelada: ${formatearFechaHoraLegible(ins.fechaCancelacion)}` : null,
                    ins.motivoCancelacion ? `Motivo: ${ins.motivoCancelacion}` : null
                ].filter(Boolean).join(" · ");

                div.innerHTML = `
                    <div class="d-flex justify-content-between align-items-center flex-wrap gap-2">
                        <div><strong>#${ins.id} · ${plan}</strong> ${origenBadge}</div>
                        <div>${badgeEstadoInscripcion(ins.estado)} ${alerta}</div>
                    </div>
                    <div class="small text-muted mt-1">
                        Inicio: <strong>${ins.fechaInicio}</strong> · Vencimiento: <strong>${ins.fechaFin}</strong>
                        ${ins.observaciones ? `<div class="mt-1"><em>Nota: ${ins.observaciones}</em></div>` : ""}
                        ${trazabilidad ? `<div class="mt-1 text-primary fw-medium">${trazabilidad}</div>` : ""}
                    </div>
                `;
                contenedorIns.appendChild(div);
            });
        }

        const rutinas = resRutinas.ok ? await resRutinas.json() : [];
        const contenedorRut = document.getElementById("historialRutinas");
        const badgeRut = document.getElementById("resumenHistorialRut");
        if (badgeRut) badgeRut.textContent = `${rutinas.length} rutina(s)`;

        if (rutinas.length === 0) {
            contenedorRut.innerHTML = '<div class="alert alert-light border mb-0">Este cliente aún no tiene rutinas asignadas.</div>';
        } else {
            contenedorRut.innerHTML = "";
            rutinas.forEach(rut => {
                const div = document.createElement("div");
                div.className = "border rounded-3 p-3 mb-2 bg-white shadow-xs";
                const estadoBadge = rut.activa
                    ? '<span class="badge bg-success">Activa</span>'
                    : '<span class="badge bg-secondary">Inactiva</span>';
                div.innerHTML = `
                    <div class="d-flex justify-content-between align-items-center flex-wrap gap-2">
                        <strong>#${rut.id} · ${rut.nombre}</strong>
                        ${estadoBadge}
                    </div>
                    <div class="small text-primary fw-semibold mt-1">Objetivo: ${rut.objetivo || "General"}</div>
                    <div class="small text-muted mt-1">
                        Entrenador: <strong>${rut.nombreEntrenador || "-"}</strong> ·
                        Asignada: ${rut.fechaAsignacion || "-"} ·
                        ${rut.totalDias ?? 0} día(s) ·
                        <strong>${rut.ejerciciosCompletados ?? 0}/${rut.totalEjercicios ?? 0} ej. (${rut.porcentajeProgreso ?? 0}%)</strong>
                    </div>
                `;
                contenedorRut.appendChild(div);
            });
        }
    } catch (error) {
        console.error(error);
        document.getElementById("nombreClienteHistorial").textContent = "Error al cargar el historial";
    }
}
