const API_RUTINAS = ENDPOINTS.RUTINAS;
const API_CLIENTES = ENDPOINTS.CLIENTES;
const API_ENTRENADORES = ENDPOINTS.ENTRENADORES;

const DIAS_SEMANA = ["LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES", "SABADO", "DOMINGO"];

const formRutina = document.getElementById("formRutina");
const tablaRutinas = document.getElementById("tablaRutinas");
const selectCliente = document.getElementById("clienteId");
const selectEntrenador = document.getElementById("entrenadorId");
const filtroCliente = document.getElementById("filtroClienteRutina");
const filtroEntrenador = document.getElementById("filtroEntrenadorRutina");
const mensajeRutinas = document.getElementById("mensajeRutinas");
const btnGuardar = document.getElementById("btnGuardarRutina");
const rutinaIdInput = document.getElementById("rutinaId");
const btnCancelarEdicion = document.getElementById("btnCancelarEdicion");
const contenedorDias = document.getElementById("contenedorDias");

let rutinasCargadas = [];

function mostrarMensaje(texto, tipo = "success") {
    mostrarAlertaUI("mensajeRutinas", texto, tipo, 5000);
}

const obtenerNombre = obtenerNombreCompleto;

function actualizarContadoresRutinas() {
    const total = rutinasCargadas.length;
    const activas = rutinasCargadas.filter(r => r.activa === true).length;
    const inactivas = total - activas;

    const elTotal = document.getElementById("cntRutinasTotal");
    const elAct = document.getElementById("cntRutinasActivas");
    const elIna = document.getElementById("cntRutinasInactivas");

    if (elTotal) elTotal.textContent = total;
    if (elAct) elAct.textContent = activas;
    if (elIna) elIna.textContent = inactivas;
}

function agregarFilaEjercicio(contenedorEjercicios, datos = {}) {
    const div = document.createElement("div");
    div.className = "ejercicio-fila";
    div.dataset.completado = datos.completado === true ? "true" : "false";
    div.innerHTML = `
        <div class="row g-2 align-items-center">
            <div class="col-12 col-md-4">
                <input type="text" class="form-control form-control-sm ej-nombre"
                       placeholder="Ejercicio (ej. Press banca)" maxlength="100"
                       value="${datos.nombre ?? ""}" required>
            </div>
            <div class="col-3 col-md-2">
                <input type="number" class="form-control form-control-sm ej-series"
                       placeholder="Series" min="1" max="20" title="Series"
                       value="${datos.series ?? 4}" required>
            </div>
            <div class="col-3 col-md-2">
                <input type="number" class="form-control form-control-sm ej-reps"
                       placeholder="Reps" min="1" max="200" title="Repeticiones"
                       value="${datos.repeticiones ?? 12}" required>
            </div>
            <div class="col-3 col-md-2">
                <input type="number" class="form-control form-control-sm ej-descanso"
                       placeholder="Seg" min="0" max="3600" title="Descanso (seg)"
                       value="${datos.descansoSeg ?? 60}">
            </div>
            <div class="col-3 col-md-2 d-flex gap-1 align-items-center">
                <input type="number" class="form-control form-control-sm ej-peso"
                       placeholder="Kg" min="0" max="1000" step="0.5" title="Peso utilizado (0 a 1000 kg)"
                       value="${datos.pesoKg ?? ""}">
                <button type="button" class="btn btn-sm btn-outline-danger btn-quitar-ej" title="Quitar ejercicio">
                    <i class="fa-solid fa-trash"></i>
                </button>
            </div>
            <div class="col-12">
                <input type="text" class="form-control form-control-sm ej-notas"
                       placeholder="Notas técnicas opcionales..." maxlength="300"
                       value="${datos.notas ?? ""}">
            </div>
        </div>
    `;
    div.querySelector(".btn-quitar-ej").addEventListener("click", () => div.remove());
    contenedorEjercicios.appendChild(div);
}

function agregarBloqueDia(datos = {}) {
    const usados = Array.from(contenedorDias.querySelectorAll(".dia-select")).map(s => s.value);
    const diaPorDefecto = datos.diaSemana || DIAS_SEMANA.find(d => !usados.includes(d)) || "LUNES";

    const bloque = document.createElement("div");
    bloque.className = "dia-bloque";

    const opcionesDias = DIAS_SEMANA.map(
        d => `<option value="${d}" ${d === diaPorDefecto ? "selected" : ""}>${d}</option>`
    ).join("");

    bloque.innerHTML = `
        <div class="d-flex justify-content-between align-items-center gap-2 mb-2">
            <div class="d-flex gap-2 flex-grow-1">
                <select class="form-select form-select-sm dia-select fw-bold text-primary" style="max-width: 145px;" required>
                    ${opcionesDias}
                </select>
                <input type="text" class="form-control form-control-sm dia-nombre"
                       placeholder="Enfoque del día (ej. Pecho y Tríceps)" maxlength="100"
                       value="${datos.nombreDia ?? ""}">
            </div>
            <button type="button" class="btn btn-sm btn-outline-danger btn-quitar-dia" title="Eliminar día">
                <i class="fa-solid fa-xmark"></i>
            </button>
        </div>
        <div class="small text-muted mb-1">Ejercicios (Nombre · Series · Reps · Descanso seg · Peso kg):</div>
        <div class="contenedor-ejercicios"></div>
        <button type="button" class="btn btn-sm btn-link text-decoration-none px-0 fw-semibold btn-agregar-ej">
            <i class="fa-solid fa-plus me-1"></i>Agregar ejercicio a este día
        </button>
    `;

    const contenedorEj = bloque.querySelector(".contenedor-ejercicios");
    bloque.querySelector(".btn-quitar-dia").addEventListener("click", () => bloque.remove());
    bloque.querySelector(".btn-agregar-ej").addEventListener("click", () => agregarFilaEjercicio(contenedorEj));

    if (Array.isArray(datos.ejercicios) && datos.ejercicios.length > 0) {
        datos.ejercicios.forEach(ej => agregarFilaEjercicio(contenedorEj, ej));
    } else {
        agregarFilaEjercicio(contenedorEj);
    }

    contenedorDias.appendChild(bloque);
}

document.getElementById("btnAgregarDia").addEventListener("click", () => agregarBloqueDia());

function construirDiasDesdeFormulario() {
    const bloques = Array.from(contenedorDias.querySelectorAll(".dia-bloque"));
    return bloques.map((bloque, idxDia) => {
        const diaSemana = bloque.querySelector(".dia-select").value;
        const nombreDia = bloque.querySelector(".dia-nombre").value.trim() || null;
        const filasEj = Array.from(bloque.querySelectorAll(".ejercicio-fila"));

        const ejercicios = filasEj.map((f, idxEj) => {
            const pesoRaw = f.querySelector(".ej-peso").value;
            return {
                nombre: f.querySelector(".ej-nombre").value.trim(),
                series: parseInt(f.querySelector(".ej-series").value, 10) || 1,
                repeticiones: parseInt(f.querySelector(".ej-reps").value, 10) || 1,
                descansoSeg: f.querySelector(".ej-descanso").value !== ""
                    ? parseInt(f.querySelector(".ej-descanso").value, 10)
                    : 60,
                notas: f.querySelector(".ej-notas").value.trim() || null,
                orden: idxEj + 1,
                completado: f.dataset.completado === "true",
                pesoKg: pesoRaw !== "" ? parseFloat(pesoRaw) : null
            };
        });

        return {
            diaSemana,
            nombreDia,
            orden: idxDia + 1,
            ejercicios
        };
    });
}

async function cargarClientes() {
    try {
        const respuesta = await fetch(API_CLIENTES);
        if (!respuesta.ok) throw new Error("No se pudieron cargar los clientes");

        const clientes = await respuesta.json();
        selectCliente.innerHTML = '<option value="">Seleccione un cliente</option>';
        filtroCliente.innerHTML = '<option value="">Todos los clientes</option>';

        clientes.forEach(cliente => {
            const nombre = obtenerNombre(cliente);
            const op1 = document.createElement("option");
            op1.value = cliente.id;
            op1.textContent = nombre;
            selectCliente.appendChild(op1);

            const op2 = document.createElement("option");
            op2.value = cliente.id;
            op2.textContent = nombre;
            filtroCliente.appendChild(op2);
        });

        const params = new URLSearchParams(window.location.search);
        const cid = params.get("clienteId");
        if (cid) selectCliente.value = cid;
    } catch (error) {
        console.error(error);
        mostrarMensaje(error.message, "danger");
    }
}

async function cargarEntrenadores() {
    try {
        const [resActivos, resTodos] = await Promise.all([
            fetch(`${API_ENTRENADORES}/activos`),
            fetch(API_ENTRENADORES)
        ]);

        if (resActivos.ok) {
            const entrenadores = await resActivos.json();
            selectEntrenador.innerHTML = '<option value="">Seleccione un entrenador</option>';
            entrenadores.forEach(entrenador => {
                const opcion = document.createElement("option");
                opcion.value = entrenador.id;
                opcion.textContent = obtenerNombre(entrenador);
                selectEntrenador.appendChild(opcion);
            });
        }

        if (resTodos.ok) {
            const todos = await resTodos.json();
            filtroEntrenador.innerHTML = '<option value="">Todos los entrenadores</option>';
            todos.forEach(e => {
                const op = document.createElement("option");
                op.value = e.id;
                op.textContent = obtenerNombre(e);
                filtroEntrenador.appendChild(op);
            });
        }
    } catch (error) {
        console.error(error);
        mostrarMensaje(error.message, "danger");
    }
}

function renderizarTablaRutinas() {
    const cid = filtroCliente.value;
    const eid = filtroEntrenador.value;

    const lista = rutinasCargadas.filter(r => {
        if (cid && String(r.clienteId) !== String(cid)) return false;
        if (eid && String(r.entrenadorId) !== String(eid)) return false;
        return true;
    });

    tablaRutinas.innerHTML = "";

    if (lista.length === 0) {
        tablaRutinas.innerHTML = `
            <tr>
                <td colspan="8" class="text-center text-muted py-4">
                    No hay rutinas registradas para el filtro seleccionado.
                </td>
            </tr>
        `;
        return;
    }

    lista.forEach(rutina => {
        const fila = document.createElement("tr");

        const estadoBadge = rutina.activa
            ? '<span class="badge bg-success">Activa</span>'
            : '<span class="badge bg-secondary">Inactiva</span>';

        const vigencia = rutina.fechaInicio
            ? `<div class="small"><strong>${rutina.fechaInicio}</strong>${rutina.fechaFin ? ` → <strong>${rutina.fechaFin}</strong>` : ""}</div>
               <div class="small text-muted">${rutina.duracionSemanas ? `${rutina.duracionSemanas} sem.` : "Continua"}</div>`
            : `<span class="small text-muted">${rutina.fechaAsignacion || "-"}</span>`;

        const totalEj = rutina.totalEjercicios ?? 0;
        const compEj = rutina.ejerciciosCompletados ?? 0;
        const pct = rutina.porcentajeProgreso ?? 0;
        const colorBarra = pct === 100 ? "#10b981" : "#2563eb";

        fila.innerHTML = `
            <td class="text-muted fw-semibold">#${rutina.id}</td>
            <td>
                <div class="fw-bold" style="color: var(--navy-900);">${rutina.nombre}</div>
                <div class="small text-primary fw-semibold">${rutina.objetivo || "-"}</div>
            </td>
            <td class="fw-semibold">${rutina.nombreCliente || obtenerNombre(rutina.cliente)}</td>
            <td>${rutina.nombreEntrenador || obtenerNombre(rutina.entrenador)}</td>
            <td style="min-width: 155px;">
                <div class="d-flex justify-content-between align-items-center small mb-1">
                    <span class="fw-semibold">${rutina.totalDias ?? 0}d · ${compEj}/${totalEj} ej.</span>
                    <span class="badge ${pct === 100 && totalEj > 0 ? "bg-success" : "bg-primary"}">${pct}%</span>
                </div>
                <div class="metric-bar-track" style="height: 6px;">
                    <div class="metric-bar-segment" style="width: ${pct}%; background: ${colorBarra};"></div>
                </div>
            </td>
            <td>${vigencia}</td>
            <td>${estadoBadge}</td>
            <td class="text-nowrap">
                <button type="button" class="btn btn-info text-white btn-sm me-1" title="Ver y registrar progreso de ejercicios" onclick="verDetalleRutina(${rutina.id})">
                    <i class="fa-solid fa-list-check"></i>
                </button>
                <button type="button" class="btn btn-warning btn-sm me-1" title="Editar rutina" onclick="editarRutina(${rutina.id})">
                    <i class="fa-solid fa-pen"></i>
                </button>
                ${
                    rutina.activa
                        ? `<button type="button" class="btn btn-outline-danger btn-sm" title="Desactivar" onclick="desactivarRutina(${rutina.id})">
                               <i class="fa-solid fa-ban"></i>
                           </button>`
                        : `<button type="button" class="btn btn-outline-success btn-sm" title="Reactivar" onclick="reactivarRutina(${rutina.id})">
                               <i class="fa-solid fa-check"></i>
                           </button>`
                }
            </td>
        `;
        tablaRutinas.appendChild(fila);
    });
}

async function cargarRutinas() {
    tablaRutinas.innerHTML = `
        <tr>
            <td colspan="8" class="text-center py-4">Cargando rutinas...</td>
        </tr>
    `;

    try {
        const respuesta = await fetch(API_RUTINAS);
        if (!respuesta.ok) throw new Error("No se pudieron cargar las rutinas");

        rutinasCargadas = await respuesta.json();
        actualizarContadoresRutinas();
        renderizarTablaRutinas();
    } catch (error) {
        console.error(error);
        tablaRutinas.innerHTML = `
            <tr>
                <td colspan="8" class="text-center text-danger py-4">
                    Error al cargar las rutinas.
                </td>
            </tr>
        `;
        mostrarMensaje(error.message, "danger");
    }
}

formRutina.addEventListener("submit", async evento => {
    evento.preventDefault();

    const id = rutinaIdInput.value;
    const clienteId = Number(selectCliente.value);
    const entrenadorId = Number(selectEntrenador.value);
    const nombre = document.getElementById("nombre").value.trim();
    const objetivo = document.getElementById("objetivo").value.trim();
    const descripcion = document.getElementById("descripcion").value.trim() || null;
    const duracionVal = document.getElementById("duracionSemanas").value;
    const fechaInicioVal = document.getElementById("fechaInicio").value;

    if (!clienteId || !entrenadorId || !nombre || !objetivo) {
        mostrarMensaje("Complete todos los campos obligatorios (*).", "warning");
        return;
    }

    const payload = {
        nombre,
        objetivo,
        descripcion,
        clienteId,
        entrenadorId,
        duracionSemanas: duracionVal ? parseInt(duracionVal, 10) : null,
        fechaInicio: fechaInicioVal || null,
        dias: construirDiasDesdeFormulario()
    };

    const url = id ? `${API_RUTINAS}/${id}` : API_RUTINAS;
    btnGuardar.disabled = true;

    try {
        const respuesta = await fetch(url, {
            method: id ? "PUT" : "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });

        if (!respuesta.ok) {
            const msg = await extraerMensajeError(
                respuesta,
                "No se pudo guardar la rutina. Verifique los días, ejercicios y entrenador activo."
            );
            throw new Error(msg);
        }

        cancelarEdicion();
        await cargarRutinas();
        mostrarMensaje(id ? "Rutina actualizada correctamente." : "Rutina registrada correctamente.", "success");
    } catch (error) {
        console.error(error);
        mostrarMensaje(error.message, "danger");
    } finally {
        btnGuardar.disabled = false;
    }
});

function renderizarContenidoModalRutina(r) {
    const cuerpo = document.getElementById("cuerpoDetalleRutina");
    document.getElementById("tituloModalDetalleRutina").textContent = `${r.nombre} (#${r.id})`;
    document.getElementById("subtituloModalDetalleRutina").textContent =
        `Cliente: ${r.nombreCliente} · Entrenador: ${r.nombreEntrenador} · Objetivo: ${r.objetivo}`;

    const totalEj = r.totalEjercicios ?? 0;
    const compEj = r.ejerciciosCompletados ?? 0;
    const pendEj = Math.max(0, totalEj - compEj);
    const pct = r.porcentajeProgreso ?? 0;
    const colorBarra = pct === 100 ? "#10b981" : "#2563eb";

    let html = `
        <div id="alertaModalRutina" class="alert d-none py-2 px-3 small mb-3" role="alert"></div>
        <div class="p-3 rounded-3 border bg-light mb-3 small">
            <div class="d-flex justify-content-between align-items-center flex-wrap gap-2 mb-2">
                <div>
                    <strong>Avance de la rutina:</strong>
                    <span class="ms-1 badge ${pct === 100 && totalEj > 0 ? "bg-success" : "bg-primary"}">${pct}% completado</span>
                </div>
                <div class="text-muted">
                    <strong class="text-success">${compEj} completado(s)</strong> ·
                    <strong>${pendEj} pendiente(s)</strong> (Total: ${totalEj})
                </div>
            </div>
            <div class="metric-bar-track mb-2" style="height: 10px;">
                <div class="metric-bar-segment" style="width: ${pct}%; background: ${colorBarra};"></div>
            </div>
            <div class="mt-2">
                <strong>Objetivo:</strong> <span class="text-primary fw-bold">${r.objetivo}</span> ·
                <strong>Vigencia:</strong> ${r.fechaInicio || r.fechaAsignacion || "-"}
                ${r.fechaFin ? ` al <strong>${r.fechaFin}</strong>` : ""}
                ${r.duracionSemanas ? ` (${r.duracionSemanas} semanas)` : ""}
            </div>
            ${r.descripcion ? `<div class="mt-1"><strong>Indicaciones:</strong> ${r.descripcion}</div>` : ""}
        </div>
    `;

    if (!r.dias || r.dias.length === 0) {
        html += '<div class="alert alert-light border mb-0">Esta rutina no tiene días de ejercicios detallados.</div>';
    } else {
        r.dias.forEach(d => {
            const filas = (d.ejercicios || []).map(ej => {
                const esComp = ej.completado === true;
                const pesoVal = ej.pesoKg != null ? ej.pesoKg : "";
                return `
                    <tr>
                        <td>
                            <div class="fw-semibold ${esComp ? "text-decoration-line-through text-muted" : ""}">${ej.nombre}</div>
                            ${ej.notas ? `<div class="text-muted small">${ej.notas}</div>` : ""}
                        </td>
                        <td><span class="badge bg-primary">${ej.series}</span></td>
                        <td><strong>${ej.repeticiones}</strong></td>
                        <td>${ej.descansoSeg ?? 60} seg</td>
                        <td style="width: 145px;">
                            <div class="input-group input-group-sm">
                                <input type="number" class="form-control form-control-sm"
                                       id="peso-ej-${ej.id}" min="0" max="1000" step="0.5" placeholder="Kg"
                                       value="${pesoVal}">
                                <button type="button" class="btn btn-outline-primary"
                                        id="btn-peso-ej-${ej.id}"
                                        title="Guardar peso (kg)"
                                        onclick="guardarPesoEjercicio(${r.id}, ${ej.id})">
                                    <i class="fa-solid fa-floppy-disk"></i>
                                </button>
                            </div>
                        </td>
                        <td class="text-end text-nowrap">
                            <button type="button"
                                    class="btn btn-sm ${esComp ? "btn-success" : "btn-outline-secondary"}"
                                    onclick="cambiarEstadoEjercicio(${r.id}, ${ej.id})">
                                <i class="fa-solid ${esComp ? "fa-circle-check" : "fa-circle"} me-1"></i>
                                ${esComp ? "Completado" : "Pendiente"}
                            </button>
                        </td>
                    </tr>
                `;
            }).join("");

            html += `
                <div class="border rounded-3 p-3 mb-3 bg-white shadow-xs">
                    <h6 class="fw-bold text-primary mb-2">
                        <i class="fa-solid fa-calendar-day me-1"></i>
                        ${d.diaSemana}${d.nombreDia ? ` — ${d.nombreDia}` : ""}
                    </h6>
                    <div class="table-responsive">
                        <table class="table table-sm align-middle mb-0">
                            <thead>
                                <tr>
                                    <th>Ejercicio</th>
                                    <th>Series</th>
                                    <th>Repeticiones</th>
                                    <th>Descanso</th>
                                    <th>Peso (kg)</th>
                                    <th class="text-end">Estado</th>
                                </tr>
                            </thead>
                            <tbody>${filas}</tbody>
                        </table>
                    </div>
                </div>
            `;
        });
    }

    cuerpo.innerHTML = html;
}

async function verDetalleRutina(id) {
    const modal = bootstrap.Modal.getOrCreateInstance(document.getElementById("modalDetalleRutina"));
    const cuerpo = document.getElementById("cuerpoDetalleRutina");
    cuerpo.innerHTML = '<div class="text-muted">Cargando estructura de la rutina...</div>';
    modal.show();

    try {
        const res = await fetch(`${API_RUTINAS}/${id}`);
        if (!res.ok) throw new Error("No se pudo obtener el detalle de la rutina");
        const r = await res.json();
        renderizarContenidoModalRutina(r);
    } catch (error) {
        cuerpo.innerHTML = `<div class="text-danger">${error.message}</div>`;
    }
}

async function cambiarEstadoEjercicio(rutinaId, ejercicioId) {
    try {
        const res = await fetch(`${API_RUTINAS}/${rutinaId}/ejercicios/${ejercicioId}/toggle-completado`, {
            method: "PATCH"
        });
        if (!res.ok) {
            const msg = await extraerMensajeError(res, "No se pudo actualizar el estado del ejercicio");
            throw new Error(msg);
        }
        const actualizada = await res.json();
        renderizarContenidoModalRutina(actualizada);

        const idx = rutinasCargadas.findIndex(r => Number(r.id) === Number(rutinaId));
        if (idx !== -1) {
            rutinasCargadas[idx] = actualizada;
            renderizarTablaRutinas();
        }
    } catch (error) {
        alert(error.message);
    }
}

async function guardarPesoEjercicio(rutinaId, ejercicioId) {
    const input = document.getElementById(`peso-ej-${ejercicioId}`);
    if (!input) return;
    const raw = input.value.trim();
    const pesoKg = raw === "" ? null : parseFloat(raw);

    if (pesoKg !== null && (isNaN(pesoKg) || pesoKg < 0 || pesoKg > 1000)) {
        mostrarAlertaUI("alertaModalRutina", "El peso utilizado debe estar entre 0 y 1000 kg.", "warning", 4000);
        return;
    }

    try {
        const res = await fetch(`${API_RUTINAS}/${rutinaId}/ejercicios/${ejercicioId}/peso`, {
            method: "PATCH",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ pesoKg })
        });
        if (!res.ok) {
            const msg = await extraerMensajeError(res, "No se pudo actualizar el peso del ejercicio");
            throw new Error(msg);
        }
        const actualizada = await res.json();
        renderizarContenidoModalRutina(actualizada);

        const idx = rutinasCargadas.findIndex(r => Number(r.id) === Number(rutinaId));
        if (idx !== -1) {
            rutinasCargadas[idx] = actualizada;
            renderizarTablaRutinas();
        }

        const btn = document.getElementById(`btn-peso-ej-${ejercicioId}`);
        if (btn) {
            btn.className = "btn btn-success";
            btn.innerHTML = '<i class="fa-solid fa-check"></i>';
            setTimeout(() => {
                if (btn.isConnected) {
                    btn.className = "btn btn-outline-primary";
                    btn.innerHTML = '<i class="fa-solid fa-floppy-disk"></i>';
                }
            }, 1800);
        }
        mostrarAlertaUI(
            "alertaModalRutina",
            pesoKg !== null ? `Peso actualizado a ${pesoKg} kg correctamente.` : "Peso restablecido (sin carga externa).",
            "success",
            2800
        );
    } catch (error) {
        mostrarAlertaUI("alertaModalRutina", error.message, "danger", 4500);
    }
}

async function editarRutina(id) {
    try {
        const res = await fetch(`${API_RUTINAS}/${id}`);
        if (!res.ok) throw new Error("No se encontró la rutina");
        const rutina = await res.json();

        await Promise.all([cargarClientes(), cargarEntrenadores()]);

        rutinaIdInput.value = rutina.id;
        document.getElementById("nombre").value = rutina.nombre || "";
        document.getElementById("objetivo").value = rutina.objetivo || "";
        document.getElementById("descripcion").value = rutina.descripcion || "";
        document.getElementById("duracionSemanas").value = rutina.duracionSemanas ?? "";
        document.getElementById("fechaInicio").value = rutina.fechaInicio ?? "";

        selectCliente.value = rutina.clienteId || "";

        const entrenadorId = String(rutina.entrenadorId || "");
        if (entrenadorId && !Array.from(selectEntrenador.options).some(o => o.value === entrenadorId)) {
            const opcion = document.createElement("option");
            opcion.value = entrenadorId;
            opcion.textContent = `${rutina.nombreEntrenador} (Inactivo)`;
            selectEntrenador.appendChild(opcion);
        }
        selectEntrenador.value = entrenadorId;

        contenedorDias.innerHTML = "";
        if (Array.isArray(rutina.dias) && rutina.dias.length > 0) {
            rutina.dias.forEach(d => agregarBloqueDia(d));
        }

        btnGuardar.innerHTML = '<i class="fa-solid fa-floppy-disk me-2"></i>Guardar cambios';
        btnCancelarEdicion.classList.remove("d-none");
        mostrarMensaje(`Editando rutina #${id}`, "info");
        window.scrollTo({ top: 0, behavior: "smooth" });
    } catch (error) {
        mostrarMensaje(error.message, "danger");
    }
}

function cancelarEdicion() {
    formRutina.reset();
    rutinaIdInput.value = "";
    contenedorDias.innerHTML = "";
    btnGuardar.innerHTML = '<i class="fa-solid fa-floppy-disk me-2"></i>Registrar rutina';
    btnCancelarEdicion.classList.add("d-none");
}

btnCancelarEdicion.addEventListener("click", cancelarEdicion);

async function desactivarRutina(id) {
    if (!confirm("¿Desea desactivar esta rutina? Se conservará en el historial del cliente.")) return;

    try {
        const respuesta = await fetch(`${API_RUTINAS}/${id}/desactivar`, { method: "PUT" });
        if (!respuesta.ok) {
            const msg = await extraerMensajeError(respuesta, "No se pudo desactivar la rutina.");
            throw new Error(msg);
        }
        await cargarRutinas();
        mostrarMensaje("Rutina desactivada correctamente.");
    } catch (error) {
        console.error(error);
        mostrarMensaje(error.message, "danger");
    }
}

async function reactivarRutina(id) {
    if (!confirm("¿Desea reactivar esta rutina?")) return;

    try {
        const respuesta = await fetch(`${API_RUTINAS}/${id}/reactivar`, { method: "PUT" });
        if (!respuesta.ok) {
            const msg = await extraerMensajeError(
                respuesta,
                "No se pudo reactivar. Verifique que el entrenador asignado esté activo."
            );
            throw new Error(msg);
        }
        await cargarRutinas();
        mostrarMensaje("Rutina reactivada correctamente.");
    } catch (error) {
        console.error(error);
        mostrarMensaje(error.message, "danger");
    }
}

filtroCliente.addEventListener("change", renderizarTablaRutinas);
filtroEntrenador.addEventListener("change", renderizarTablaRutinas);

document.getElementById("btnActualizarRutinas").addEventListener("click", async () => {
    await Promise.all([cargarRutinas(), cargarClientes(), cargarEntrenadores()]);
});

document.addEventListener("DOMContentLoaded", async () => {
    await Promise.all([cargarClientes(), cargarEntrenadores(), cargarRutinas()]);
});
