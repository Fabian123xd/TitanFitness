const API_REPORTES = "http://localhost:8080/api/reportes";

// Formatear montos en soles
function formatoSoles(monto) {
    return "S/ " + Number(monto || 0).toFixed(2);
}

// Cargar estadísticas generales
async function cargarResumenReportes() {
    try {
        const [respuestaResumen, respuestaIngresos] = await Promise.all([
            fetch(`${API_REPORTES}/resumen`),
            fetch(`${API_REPORTES}/ingresos`)
        ]);

        if (!respuestaResumen.ok || !respuestaIngresos.ok) {
            throw new Error("No se pudieron cargar los reportes");
        }

        const resumen = await respuestaResumen.json();
        const ingresos = await respuestaIngresos.json();

        document.getElementById("totalClientes").textContent =
            resumen.totalClientes;

        document.getElementById("totalPagos").textContent =
            resumen.totalPagos;

        document.getElementById("totalAsistencias").textContent =
            resumen.totalAsistencias;

        document.getElementById("ingresosTotales").textContent =
            formatoSoles(ingresos.ingresosTotales);

    } catch (error) {
        console.error("Error al cargar reportes:", error);
        document.getElementById("mensajeReporte").textContent =
            "No se pudieron cargar los reportes. Verifica que Spring Boot esté ejecutándose.";
        document.getElementById("mensajeReporte").className =
            "mt-3 mb-0 text-danger";
    }
}

// Consultar ingresos por fechas
async function consultarIngresosPorFechas(evento) {
    evento.preventDefault();

    const fechaInicio = document.getElementById("fechaInicio").value;
    const fechaFin = document.getElementById("fechaFin").value;
    const mensaje = document.getElementById("mensajeReporte");

    if (fechaInicio > fechaFin) {
        mensaje.textContent =
            "La fecha inicial no puede ser posterior a la fecha final.";
        mensaje.className = "mt-3 mb-0 text-danger";
        return;
    }

    try {
        mensaje.textContent = "Consultando ingresos...";
        mensaje.className = "mt-3 mb-0 text-muted";

        const parametros = new URLSearchParams({
            fechaInicio,
            fechaFin
        });

        const respuesta = await fetch(
            `${API_REPORTES}/ingresos/fechas?${parametros}`
        );

        if (!respuesta.ok) {
            throw new Error("Error al consultar ingresos");
        }

        const datos = await respuesta.json();

        document.getElementById("ingresosPeriodo").textContent =
            formatoSoles(datos.ingresosTotales);

        document.getElementById("periodoConsultado").textContent =
            `${fechaInicio} al ${fechaFin}`;

        mensaje.textContent = "Reporte generado correctamente.";
        mensaje.className = "mt-3 mb-0 text-success";

    } catch (error) {
        console.error("Error:", error);
        mensaje.textContent =
            "No se pudieron consultar los ingresos.";
        mensaje.className = "mt-3 mb-0 text-danger";
    }
}

// Inicializar página
document.addEventListener("DOMContentLoaded", () => {
    cargarResumenReportes();

    const hoy = new Date();
    const anio = hoy.getFullYear();
    const mes = String(hoy.getMonth() + 1).padStart(2, "0");
    const dia = String(hoy.getDate()).padStart(2, "0");

    document.getElementById("fechaInicio").value =
        `${anio}-${mes}-01`;

    document.getElementById("fechaFin").value =
        `${anio}-${mes}-${dia}`;

    document.getElementById("formReporteFechas")
        .addEventListener("submit", consultarIngresosPorFechas);
});