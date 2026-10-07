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