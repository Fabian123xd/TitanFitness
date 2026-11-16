const API_URL = API_BASE;

function renderizarBarrasDashboard(r) {
    const activas = Number(r.inscripcionesActivas ?? 0);
    const pendientes = Number(r.inscripcionesPendientes ?? 0);
    const vencidas = Number(r.inscripcionesVencidas ?? 0);
    const canceladas = Number(r.inscripcionesCanceladas ?? 0);
    const totalIns = activas + pendientes + vencidas + canceladas;

    asignarTexto("totalInscripcionesGlobal", `${totalIns} inscripción(es)`);
    asignarTexto("legActivas", activas);
    asignarTexto("legPendientes", pendientes);
    asignarTexto("legVencidas", vencidas);
    asignarTexto("legCanceladas", canceladas);

    const barraIns = document.getElementById("barraEstadosInscripciones");
    if (barraIns) {
        if (totalIns === 0) {
            barraIns.innerHTML = `<div class="metric-bar-segment" style="width:100%; background:#e2e8f0;"></div>`;
        } else {
            const pAct = ((activas / totalIns) * 100).toFixed(1);
            const pPen = ((pendientes / totalIns) * 100).toFixed(1);
            const pVen = ((vencidas / totalIns) * 100).toFixed(1);
            const pCan = ((canceladas / totalIns) * 100).toFixed(1);
            barraIns.innerHTML = `
                <div class="metric-bar-segment" style="width:${pAct}%; background:#10b981;" title="Vigentes: ${activas}"></div>
                <div class="metric-bar-segment" style="width:${pPen}%; background:#2563eb;" title="Pendientes: ${pendientes}"></div>
                <div class="metric-bar-segment" style="width:${pVen}%; background:#ef4444;" title="Vencidas: ${vencidas}"></div>
                <div class="metric-bar-segment" style="width:${pCan}%; background:#64748b;" title="Canceladas: ${canceladas}"></div>
            `;
        }
    }

    const rutActivas = Number(r.rutinasActivas ?? 0);
    const rutInactivas = Number(r.rutinasInactivas ?? 0);
    const totalRut = Number(r.totalRutinas ?? (rutActivas + rutInactivas));

    asignarTexto("legRutActivas", rutActivas);
    asignarTexto("legRutInactivas", rutInactivas);
    asignarTexto("legStaffActivo", r.entrenadoresActivos ?? 0);
    asignarTexto("legStaffTotal", r.totalEntrenadores ?? 0);

    const pctRut = totalRut > 0 ? Math.round((rutActivas / totalRut) * 100) : 0;
    asignarTexto("ratioRutinasActivas", `${pctRut}% activas`);

    const barraRut = document.getElementById("barraEstadosRutinas");
    if (barraRut) {
        if (totalRut === 0) {
            barraRut.innerHTML = `<div class="metric-bar-segment" style="width:100%; background:#e2e8f0;"></div>`;
        } else {
            const pActR = ((rutActivas / totalRut) * 100).toFixed(1);
            const pInaR = ((rutInactivas / totalRut) * 100).toFixed(1);
            barraRut.innerHTML = `
                <div class="metric-bar-segment" style="width:${pActR}%; background:#2563eb;" title="Activas: ${rutActivas}"></div>
                <div class="metric-bar-segment" style="width:${pInaR}%; background:#94a3b8;" title="Inactivas: ${rutInactivas}"></div>
            `;
        }
    }
}

async function cargarDashboard() {
    const usuario = sessionStorage.getItem("usuario");
    if (usuario) {
        asignarTexto("nombreUsuarioTop", usuario);
    }

    try {
        const res = await fetch(`${API_URL}/reportes/resumen`);
        if (!res.ok) throw new Error("No se pudo obtener el resumen del dashboard");
        const r = await res.json();

        asignarTexto("totalClientes", r.totalClientes ?? 0);
        asignarTexto("totalMembresias", r.totalPlanes ?? 0);
        asignarTexto("planesActivos", r.planesActivos ?? 0);
        asignarTexto("inscripcionesActivas", r.inscripcionesActivas ?? 0);
        asignarTexto("inscripcionesPendientes", r.inscripcionesPendientes ?? 0);
        asignarTexto("inscripcionesProximas", r.inscripcionesProximasAVencer ?? 0);
        asignarTexto("inscripcionesVencidas", r.inscripcionesVencidas ?? 0);
        asignarTexto("rutinasActivas", r.rutinasActivas ?? 0);
        asignarTexto("totalRutinas", r.totalRutinas ?? 0);
        asignarTexto("entrenadoresActivos", r.entrenadoresActivos ?? 0);

        renderizarBarrasDashboard(r);
    } catch (error) {
        console.error("Error al cargar el Dashboard:", error);
    }
}

async function cargarAlertasVencimiento() {
    const tabla = document.getElementById("tablaAlertas");
    const contador = document.getElementById("totalAlertas");

    if (!tabla || !contador) return;

    try {
        const respuesta = await fetch(`${API_URL}/inscripciones/proximas-vencer`);
        if (!respuesta.ok) {
            throw new Error("No se pudieron cargar las alertas");
        }

        const inscripciones = await respuesta.json();
        tabla.innerHTML = "";
        contador.textContent = `${inscripciones.length} alerta(s)`;

        if (inscripciones.length === 0) {
            tabla.innerHTML = `
                <tr>
                    <td colspan="7" class="text-center text-success py-4">
                        <i class="fa-solid fa-circle-check me-2"></i>
                        No hay membresías próximas a vencer en los próximos 7 días hábiles.
                    </td>
                </tr>
            `;
            return;
        }

        inscripciones
            .sort((a, b) => (a.fechaFin || "").localeCompare(b.fechaFin || ""))
            .forEach(ins => {
                const fila = document.createElement("tr");

                const nombreCliente = ins.nombreCliente
                    || (ins.cliente ? `${ins.cliente.nombres} ${ins.cliente.apellidos}` : "-");
                const nombrePlan = ins.nombreMembresia
                    || (ins.membresia ? ins.membresia.nombre : "-");

                const habiles = ins.diasHabilesRestantes ?? 0;
                const calendario = ins.diasRestantes ?? 0;

                const tdCliente = document.createElement("td");
                tdCliente.className = "fw-semibold";
                tdCliente.textContent = nombreCliente;

                const tdPlan = document.createElement("td");
                tdPlan.textContent = nombrePlan;

                const tdFin = document.createElement("td");
                tdFin.textContent = ins.fechaFin || "-";

                const tdHabiles = document.createElement("td");
                tdHabiles.className = "fw-bold";
                tdHabiles.textContent = habiles === 0 ? "Vence hoy" : `${habiles} día(s) hábiles`;

                const tdCalendario = document.createElement("td");
                tdCalendario.textContent = `${calendario} día(s)`;

                const tdEstado = document.createElement("td");
                const badge = document.createElement("span");
                badge.className = habiles <= 2 ? "badge bg-danger" : "badge bg-warning text-dark";
                badge.textContent = habiles <= 2 ? "Urgente" : "Próxima a vencer";
                tdEstado.appendChild(badge);

                const tdAccion = document.createElement("td");
                tdAccion.className = "text-end";
                const link = document.createElement("a");
                link.href = `inscripciones.html?renovar=${ins.id}`;
                link.className = "btn btn-sm btn-outline-primary";
                link.innerHTML = '<i class="fa-solid fa-rotate me-1"></i>Gestionar';
                tdAccion.appendChild(link);

                fila.append(tdCliente, tdPlan, tdFin, tdHabiles, tdCalendario, tdEstado, tdAccion);
                tabla.appendChild(fila);
            });
    } catch (error) {
        console.error("Error al cargar alertas:", error);
        contador.textContent = "Sin datos";
        tabla.innerHTML = `
            <tr>
                <td colspan="7" class="text-center text-danger py-4">
                    No se pudieron cargar las alertas de vencimiento.
                </td>
            </tr>
        `;
    }
}

document.addEventListener("DOMContentLoaded", () => {
    cargarDashboard();
    cargarAlertasVencimiento();
});
