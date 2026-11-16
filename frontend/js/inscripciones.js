const API_INSCRIPCIONES = ENDPOINTS.INSCRIPCIONES;
const API_CLIENTES = ENDPOINTS.CLIENTES;
const API_MEMBRESIAS = ENDPOINTS.MEMBRESIAS;

let inscripcionesCache = [];
let planesActivosCache = [];

function mostrarMensajeInscripcion(texto, tipo = "success") {
    mostrarAlertaUI("mensajeInscripciones", texto, tipo, 5000);
}

function actualizarPreviewVencimiento() {
    const box = document.getElementById("previewVencimiento");
    const planId = Number(document.getElementById("membresiaId").value);
    const fechaInput = document.getElementById("fechaInicio").value;
    if (!box) return;

    const plan = planesActivosCache.find(p => Number(p.id) === planId);
    if (!plan) {
        box.classList.add("d-none");
        return;
    }

    const baseDate = fechaInput ? new Date(fechaInput + "T00:00:00") : new Date();
    const finDate = new Date(baseDate.getTime());
    finDate.setDate(finDate.getDate() + Number(plan.duracionDias));

    const fmtInicio = formatearFechaLocal(baseDate);
    const fmtFin = formatearFechaLocal(finDate);

    box.innerHTML = `
        <div class="d-flex justify-content-between align-items-center">
            <span><i class="fa-solid fa-calendar-check text-primary me-1"></i><strong>Cálculo automático:</strong></span>
            <span class="badge bg-primary">${plan.duracionDias} días</span>
        </div>
        <div class="mt-1 small">
            Inicio: <strong>${fmtInicio}</strong> → Vencimiento: <strong>${fmtFin}</strong>
        </div>
    `;
    box.classList.remove("d-none");
}

async function cargarSelectores() {
    const selectCliente = document.getElementById("clienteId");
    const selectPlan = document.getElementById("membresiaId");

    try {
        const [resClientes, resPlanes] = await Promise.all([
            fetch(API_CLIENTES),
            fetch(`${API_MEMBRESIAS}/activas`)
        ]);

        if (resClientes.ok) {
            const clientes = await resClientes.json();
            selectCliente.innerHTML = '<option value="">Seleccione un cliente</option>';
            clientes.forEach(c => {
                const opt = document.createElement("option");
                opt.value = c.id;
                opt.textContent = `${c.nombres} ${c.apellidos} (${c.dni})`;
                selectCliente.appendChild(opt);
            });
        }

        if (resPlanes.ok) {
            planesActivosCache = await resPlanes.json();
            selectPlan.innerHTML = '<option value="">Seleccione un plan activo</option>';
            planesActivosCache.forEach(p => {
                const opt = document.createElement("option");
                opt.value = p.id;
                opt.textContent = `${p.nombre} — ${p.duracionDias} días (S/ ${Number(p.precio).toFixed(2)})`;
                selectPlan.appendChild(opt);
            });
        }

        const params = new URLSearchParams(window.location.search);
        const clientePreseleccionado = params.get("clienteId");
        if (clientePreseleccionado) {
            selectCliente.value = clientePreseleccionado;
        }
    } catch (error) {
        console.error(error);
        mostrarMensajeInscripcion("No se pudieron cargar clientes o planes.", "danger");
    }
}

async function cargarResumen() {
    try {
        const res = await fetch(`${API_INSCRIPCIONES}/resumen`);
        if (!res.ok) return;
        const r = await res.json();
        document.getElementById("cntActivas").textContent = r.activas ?? 0;
        document.getElementById("cntProximas").textContent = r.proximasAVencer ?? 0;
        document.getElementById("cntPendientes").textContent = r.pendientes ?? 0;
        document.getElementById("cntVencidas").textContent = r.vencidas ?? 0;
    } catch (e) {
        console.error(e);
    }
}

function renderizarEstado(ins) {
    return badgeEstadoInscripcion(ins.estado, ins.proximaAVencer);
}

function renderizarTabla() {
    const tabla = document.getElementById("tablaInscripciones");
    const filtro = document.getElementById("filtroEstado").value;
    const texto = (document.getElementById("buscarInscripcion").value || "").toLowerCase().trim();

    const filtradas = inscripcionesCache.filter(ins => {
        if (filtro === "PROXIMAS" && !ins.proximaAVencer) return false;
        if (filtro !== "TODAS" && filtro !== "PROXIMAS" && ins.estado !== filtro) return false;

        if (texto) {
            const cadena = `${ins.id} ${ins.nombreCliente || ""} ${ins.nombreMembresia || ""} ${ins.estado || ""}`.toLowerCase();
            return cadena.includes(texto);
        }
        return true;
    });

    tabla.innerHTML = "";

    if (filtradas.length === 0) {
        tabla.innerHTML = `
            <tr>
                <td colspan="7" class="text-center text-muted py-4">
                    No se encontraron inscripciones con los criterios seleccionados.
                </td>
            </tr>
        `;
        return;
    }

    filtradas.forEach(ins => {
        const tr = document.createElement("tr");

        const diasTexto = ins.estado === "ACTIVA" && !ins.vencida
            ? `<span class="fw-semibold">${ins.diasHabilesRestantes} hábiles</span> <span class="text-muted">(${ins.diasRestantes} cal.)</span>`
            : "-";

        let botones = "";
        if (ins.estado === "PENDIENTE") {
            botones += `
                <button class="btn btn-sm btn-success me-1" onclick="activarInscripcion(${ins.id})" title="Activar inscripción">
                    <i class="fa-solid fa-check"></i>
                </button>
                <button class="btn btn-sm btn-outline-danger me-1" onclick="cancelarInscripcion(${ins.id})" title="Cancelar">
                    <i class="fa-solid fa-xmark"></i>
                </button>
            `;
        } else if (ins.estado === "ACTIVA") {
            botones += `
                <button class="btn btn-sm btn-primary me-1" onclick="renovarInscripcion(${ins.id})" title="Renovar membresía (conserva historial)">
                    <i class="fa-solid fa-rotate"></i>
                </button>
                <button class="btn btn-sm btn-outline-danger me-1" onclick="cancelarInscripcion(${ins.id})" title="Cancelar">
                    <i class="fa-solid fa-ban"></i>
                </button>
            `;
        } else if (ins.estado === "VENCIDA") {
            botones += `
                <button class="btn btn-sm btn-primary me-1" onclick="renovarInscripcion(${ins.id})" title="Renovar membresía">
                    <i class="fa-solid fa-rotate"></i>
                </button>
            `;
        }

        botones += `
            <button class="btn btn-sm btn-info text-white" onclick="verHistorialCliente(${ins.clienteId}, '${(ins.nombreCliente || "").replace(/'/g, "\\'")}')" title="Ver historial del cliente">
                <i class="fa-solid fa-clock-rotate-left"></i>
            </button>
        `;

        const iniciales = obtenerIniciales(ins.nombreCliente || "");

        tr.innerHTML = `
            <td class="text-muted fw-semibold">#${ins.id}</td>
            <td>
                <div class="d-flex align-items-center gap-2">
                    <span class="avatar-chip">${iniciales}</span>
                    <span class="fw-semibold" style="color: var(--navy-900);">${ins.nombreCliente || "-"}</span>
                </div>
            </td>
            <td><span class="fw-semibold">${ins.nombreMembresia || "-"}</span></td>
            <td>
                <div class="small">Inicio: <strong>${ins.fechaInicio}</strong></div>
                <div class="small text-muted">Fin: <strong>${ins.fechaFin}</strong></div>
            </td>
            <td class="small">${diasTexto}</td>
            <td>${renderizarEstado(ins)}</td>
            <td class="text-center text-nowrap">${botones}</td>
        `;
        tabla.appendChild(tr);
    });
}

async function cargarInscripciones() {
    try {
        const res = await fetch(API_INSCRIPCIONES);
        if (!res.ok) throw new Error("No se pudieron cargar las inscripciones");
        inscripcionesCache = await res.json();
        renderizarTabla();
        await cargarResumen();
    } catch (error) {
        console.error(error);
        mostrarMensajeInscripcion(error.message, "danger");
    }
}

document.getElementById("membresiaId").addEventListener("change", actualizarPreviewVencimiento);
document.getElementById("fechaInicio").addEventListener("change", actualizarPreviewVencimiento);

document.getElementById("formInscripcion").addEventListener("submit", async function (e) {
    e.preventDefault();

    const clienteId = Number(document.getElementById("clienteId").value);
    const membresiaId = Number(document.getElementById("membresiaId").value);
    const fechaInicioVal = document.getElementById("fechaInicio").value;
    const observacionesVal = document.getElementById("observaciones").value.trim();

    if (!clienteId || !membresiaId) {
        mostrarMensajeInscripcion("Seleccione un cliente y un plan de membresía.", "warning");
        return;
    }

    const payload = {
        clienteId,
        membresiaId,
        fechaInicio: fechaInicioVal || null,
        observaciones: observacionesVal || null
    };

    const btn = document.getElementById("btnRegistrarInscripcion");
    btn.disabled = true;

    try {
        const res = await fetch(API_INSCRIPCIONES, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });

        if (!res.ok) {
            const msg = await extraerMensajeError(res, "No se pudo registrar la inscripción");
            throw new Error(msg);
        }

        this.reset();
        document.getElementById("previewVencimiento").classList.add("d-none");
        await cargarInscripciones();
        mostrarMensajeInscripcion(
            "Inscripción registrada en estado PENDIENTE. Puede activarla directamente desde la tabla.",
            "success"
        );
    } catch (error) {
        console.error(error);
        mostrarMensajeInscripcion(error.message, "danger");
    } finally {
        btn.disabled = false;
    }
});

async function activarInscripcion(id) {
    if (!confirm(`¿Desea activar la inscripción #${id}?`)) return;

    try {
        const res = await fetch(`${API_INSCRIPCIONES}/${id}/activar`, { method: "PUT" });
        if (!res.ok) {
            const msg = await extraerMensajeError(res, "No se pudo activar la inscripción");
            throw new Error(msg);
        }
        await cargarInscripciones();
        mostrarMensajeInscripcion(`Inscripción #${id} activada correctamente.`, "success");
    } catch (error) {
        console.error(error);
        mostrarMensajeInscripcion(error.message, "danger");
    }
}

async function cancelarInscripcion(id) {
    if (!confirm(`¿Desea cancelar la inscripción #${id}? El registro permanecerá en el historial.`)) return;

    try {
        const res = await fetch(`${API_INSCRIPCIONES}/${id}/cancelar`, { method: "PUT" });
        if (!res.ok) {
            const msg = await extraerMensajeError(res, "No se pudo cancelar la inscripción");
            throw new Error(msg);
        }
        await cargarInscripciones();
        mostrarMensajeInscripcion(`Inscripción #${id} cancelada.`, "info");
    } catch (error) {
        console.error(error);
        mostrarMensajeInscripcion(error.message, "danger");
    }
}

async function renovarInscripcion(id) {
    const nota = prompt(
        `Renovar inscripción #${id}.\nSe creará una nueva inscripción consecutiva conservando el historial anterior.\n\nObservación opcional:`,
        `Renovación de inscripción #${id}`
    );
    if (nota === null) return;

    try {
        const params = nota.trim() ? `?observaciones=${encodeURIComponent(nota.trim())}` : "";
        const res = await fetch(`${API_INSCRIPCIONES}/${id}/renovar${params}`, { method: "POST" });
        if (!res.ok) {
            const msg = await extraerMensajeError(res, "No se pudo renovar la inscripción");
            throw new Error(msg);
        }
        const nueva = await res.json();
        await cargarInscripciones();
        mostrarMensajeInscripcion(
            `Renovación creada (#${nueva.id}: ${nueva.fechaInicio} → ${nueva.fechaFin}). Actívela cuando corresponda.`,
            "success"
        );
    } catch (error) {
        console.error(error);
        mostrarMensajeInscripcion(error.message, "danger");
    }
}

async function verHistorialCliente(clienteId, nombreCliente) {
    document.getElementById("tituloClienteHistorial").textContent = nombreCliente || `Cliente #${clienteId}`;
    const contenedor = document.getElementById("cuerpoHistorialCliente");
    contenedor.innerHTML = '<div class="text-muted">Cargando historial...</div>';

    const modal = bootstrap.Modal.getOrCreateInstance(document.getElementById("modalHistorialCliente"));
    modal.show();

    try {
        const res = await fetch(`${API_INSCRIPCIONES}/cliente/${clienteId}`);
        if (!res.ok) throw new Error("No se pudo obtener el historial del cliente");
        const lista = await res.json();

        if (lista.length === 0) {
            contenedor.innerHTML = '<div class="alert alert-light border mb-0">Sin inscripciones registradas.</div>';
            return;
        }

        contenedor.innerHTML = "";
        lista.forEach(ins => {
            const item = document.createElement("div");
            item.className = "border rounded-3 p-3 mb-2 d-flex justify-content-between align-items-center flex-wrap gap-2 bg-white shadow-xs";
            item.innerHTML = `
                <div>
                    <strong>Inscripción #${ins.id} · ${ins.nombreMembresia}</strong>
                    <div class="small text-muted">
                        Período: <strong>${ins.fechaInicio}</strong> al <strong>${ins.fechaFin}</strong>
                        ${ins.observaciones ? ` · <em>${ins.observaciones}</em>` : ""}
                    </div>
                </div>
                <div>${renderizarEstado(ins)}</div>
            `;
            contenedor.appendChild(item);
        });
    } catch (error) {
        contenedor.innerHTML = `<div class="text-danger">${error.message}</div>`;
    }
}

document.getElementById("filtroEstado").addEventListener("change", renderizarTabla);
document.getElementById("buscarInscripcion").addEventListener("input", renderizarTabla);

document.addEventListener("DOMContentLoaded", async () => {
    await Promise.all([cargarSelectores(), cargarInscripciones()]);

    const params = new URLSearchParams(window.location.search);
    const renovarId = params.get("renovar");
    if (renovarId) {
        const encontrada = inscripcionesCache.find(i => String(i.id) === String(renovarId));
        if (encontrada) {
            document.getElementById("buscarInscripcion").value = encontrada.nombreCliente || `#${renovarId}`;
            renderizarTabla();
            mostrarMensajeInscripcion(
                `Gestionando inscripción #${encontrada.id} de ${encontrada.nombreCliente} (vence el ${encontrada.fechaFin}). Puede renovarla con el botón azul de rotación.`,
                "info"
            );
        }
    }
});
