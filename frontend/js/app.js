const API_URL = "http://localhost:8080/api";

async function cargarDashboard() {
    try {
        const [clientes, membresias, pagos, asistencias] = await Promise.all([
            fetch(`${API_URL}/clientes`).then(res => res.json()),
            fetch(`${API_URL}/membresias`).then(res => res.json()),
            fetch(`${API_URL}/pagos`).then(res => res.json()),
            fetch(`${API_URL}/asistencias`).then(res => res.json())
        ]);

        document.getElementById("totalClientes").textContent = clientes.length;
        document.getElementById("totalMembresias").textContent = membresias.length;
        document.getElementById("totalPagos").textContent = pagos.length;
        document.getElementById("totalAsistencias").textContent = asistencias.length;

    } catch (error) {
        console.error("Error al cargar el Dashboard:", error);
    }
}

document.addEventListener("DOMContentLoaded", cargarDashboard);
// ==========================================
// ALERTAS DE MEMBRESÍAS PRÓXIMAS A VENCER
// ==========================================

async function cargarAlertasVencimiento() {
    const tabla = document.getElementById("tablaAlertas");
    const contador = document.getElementById("totalAlertas");

    if (!tabla || !contador) return;

    try {
        const respuesta = await fetch(
            "http://localhost:8080/api/inscripciones/proximas-vencer"
        );

        if (!respuesta.ok) {
            throw new Error("No se pudieron cargar las alertas");
        }

        const inscripciones = await respuesta.json();

        tabla.innerHTML = "";
        contador.textContent = `${inscripciones.length} alertas`;

        if (inscripciones.length === 0) {
            tabla.innerHTML = `
                <tr>
                    <td colspan="5" class="text-center text-success">
                        No hay membresías próximas a vencer.
                    </td>
                </tr>
            `;
            return;
        }

        const hoy = new Date();
        hoy.setHours(0, 0, 0, 0);

        inscripciones
            .sort((a, b) => a.fechaFin.localeCompare(b.fechaFin))
            .forEach(inscripcion => {
                const fecha = new Date(inscripcion.fechaFin + "T00:00:00");

                const diasRestantes = Math.round(
                    (fecha - hoy) / 86400000
                );

                const fila = document.createElement("tr");

                const datos = [
                    `${inscripcion.cliente.nombres} ${inscripcion.cliente.apellidos}`,
                    inscripcion.membresia.nombre,
                    fecha.toLocaleDateString("es-PE"),
                    diasRestantes === 0
                        ? "Vence hoy"
                        : `${diasRestantes} días`
                ];

                datos.forEach(valor => {
                    const celda = document.createElement("td");
                    celda.textContent = valor;
                    fila.appendChild(celda);
                });

                const celdaEstado = document.createElement("td");
                const estado = document.createElement("span");

                estado.className = diasRestantes <= 2
                    ? "badge bg-danger"
                    : "badge bg-warning text-dark";

                estado.textContent = diasRestantes <= 2
                    ? "Urgente"
                    : "Próxima a vencer";

                celdaEstado.appendChild(estado);
                fila.appendChild(celdaEstado);

                tabla.appendChild(fila);
            });

    } catch (error) {
        console.error("Error al cargar alertas:", error);

        contador.textContent = "Sin datos";

        tabla.innerHTML = `
            <tr>
                <td colspan="5" class="text-center text-danger">
                    No se pudieron cargar las alertas.
                </td>
            </tr>
        `;
    }
}

cargarAlertasVencimiento();