/**
 * Módulo centralizado de configuración API, autenticación, control de inactividad,
 * cambio seguro de contraseña y utilidades compartidas — Titan Cobalt Pro.
 */
const API_BASE = "http://localhost:8080/api";

const ENDPOINTS = Object.freeze({
    AUTH_ME: `${API_BASE}/auth/me`,
    AUTH_PASSWORD: `${API_BASE}/auth/password`,
    REPORTES_RESUMEN: `${API_BASE}/reportes/resumen`,
    CLIENTES: `${API_BASE}/clientes`,
    MEMBRESIAS: `${API_BASE}/membresias`,
    INSCRIPCIONES: `${API_BASE}/inscripciones`,
    ENTRENADORES: `${API_BASE}/entrenadores`,
    RUTINAS: `${API_BASE}/rutinas`
});

/** Tiempo máximo de inactividad (15 minutos) y aviso previo (1 minuto antes). */
const INACTIVIDAD_LIMITE_MS = 15 * 60 * 1000;
const INACTIVIDAD_AVISO_MS = 14 * 60 * 1000;

let timerAvisoInactividad = null;
let timerCierreInactividad = null;

(function interceptarFetch() {
    const originalFetch = window.fetch.bind(window);
    window.fetch = async function (input, init = {}) {
        const url = typeof input === "string" ? input : (input && input.url) || "";
        if (url.startsWith(API_BASE) || url.includes("localhost:8080/api")) {
            const headers = new Headers(init.headers || {});
            const token = sessionStorage.getItem("authToken");
            if (token && !headers.has("Authorization")) {
                headers.set("Authorization", `Basic ${token}`);
            }
            init = { ...init, headers };
        }
        const response = await originalFetch(input, init);
        if (response.status === 401 && !window.location.pathname.endsWith("login.html")) {
            sessionStorage.clear();
            window.location.href = "login.html";
        }
        return response;
    };
})();

function cerrarSesion(motivo = "") {
    sessionStorage.clear();
    if (motivo) {
        sessionStorage.setItem("motivoCierreSesion", motivo);
    }
    window.location.href = "login.html";
}

function reiniciarTemporizadorInactividad() {
    if (window.location.pathname.endsWith("login.html")) return;
    if (sessionStorage.getItem("sesionActiva") !== "true") return;

    const bannerAviso = document.getElementById("bannerAvisoInactividad");
    if (bannerAviso) {
        bannerAviso.classList.add("d-none");
    }

    clearTimeout(timerAvisoInactividad);
    clearTimeout(timerCierreInactividad);

    timerAvisoInactividad = setTimeout(() => {
        mostrarBannerInactividad();
    }, INACTIVIDAD_AVISO_MS);

    timerCierreInactividad = setTimeout(() => {
        cerrarSesion("La sesión se cerró automáticamente por inactividad.");
    }, INACTIVIDAD_LIMITE_MS);
}

function mostrarBannerInactividad() {
    let banner = document.getElementById("bannerAvisoInactividad");
    if (!banner) {
        banner = document.createElement("div");
        banner.id = "bannerAvisoInactividad";
        banner.className = "alert alert-warning shadow position-fixed top-0 end-0 m-3 z-3 d-flex align-items-center gap-2";
        banner.innerHTML = `
            <i class="fa-solid fa-clock text-warning"></i>
            <span class="small fw-semibold">Su sesión se cerrará en 1 minuto por inactividad. Mueva el cursor o haga clic para continuar.</span>
        `;
        document.body.appendChild(banner);
    }
    banner.classList.remove("d-none");
}

function obtenerIniciales(nombreCompleto) {
    if (!nombreCompleto) return "TF";
    const partes = String(nombreCompleto).trim().split(/\s+/);
    const i1 = partes[0] ? partes[0][0] : "";
    const i2 = partes[1] ? partes[1][0] : "";
    return (i1 + i2).toUpperCase() || "TF";
}

function obtenerNombreCompleto(persona) {
    if (!persona) return "No disponible";
    const nombres = persona.nombres || persona.nombre || "";
    const apellidos = persona.apellidos || persona.apellido || "";
    return `${nombres} ${apellidos}`.trim() || `ID ${persona.id}`;
}

function formatearFechaLocal(d = new Date()) {
    const anio = d.getFullYear();
    const mes = String(d.getMonth() + 1).padStart(2, "0");
    const dia = String(d.getDate()).padStart(2, "0");
    return `${anio}-${mes}-${dia}`;
}

function formatearFechaHoraLegible(isoDateTime) {
    if (!isoDateTime) return null;
    const limpio = String(isoDateTime).replace("T", " ").slice(0, 16);
    return limpio;
}

function asignarTexto(id, valor) {
    const el = document.getElementById(id);
    if (el) el.textContent = valor;
}

function mostrarAlertaUI(idElemento, mensaje, tipo = "success", duracionMs = 4500) {
    const alerta = document.getElementById(idElemento);
    if (!alerta) return;
    alerta.className = `alert alert-${tipo}`;
    alerta.textContent = mensaje;
    if (duracionMs > 0) {
        setTimeout(() => alerta.classList.add("d-none"), duracionMs);
    }
}

function badgeEstadoInscripcion(estado, proximaAVencer = false) {
    if (estado === "ACTIVA") {
        if (proximaAVencer) {
            return '<span class="badge bg-warning text-dark">ACTIVA · Próxima a vencer</span>';
        }
        return '<span class="badge bg-success">ACTIVA</span>';
    }
    if (estado === "PENDIENTE") {
        return '<span class="badge bg-primary">PENDIENTE</span>';
    }
    if (estado === "VENCIDA") {
        return '<span class="badge bg-danger">VENCIDA</span>';
    }
    if (estado === "CANCELADA") {
        return '<span class="badge bg-secondary">CANCELADA</span>';
    }
    return `<span class="badge bg-secondary">${estado || "-"}</span>`;
}

async function extraerMensajeError(respuesta, mensajePorDefecto = "Ocurrió un error en la operación") {
    try {
        const data = await respuesta.clone().json();
        if (data && data.mensaje) return data.mensaje;
        if (data && data.message) return data.message;
    } catch (_) {
        try {
            const texto = await respuesta.text();
            if (texto) return texto;
        } catch (_) {}
    }
    return mensajePorDefecto;
}

async function abrirPromptCambioPassword() {
    const passwordActual = prompt("Seguridad Titan Cobalt Pro\n\nIngrese su contraseña actual:");
    if (passwordActual === null || !passwordActual) return;

    const passwordNueva = prompt("Ingrese la nueva contraseña (mínimo 8 caracteres):");
    if (passwordNueva === null || !passwordNueva) return;

    if (passwordNueva.length < 8) {
        alert("La nueva contraseña debe tener al menos 8 caracteres.");
        return;
    }

    try {
        const res = await fetch(ENDPOINTS.AUTH_PASSWORD, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ passwordActual, passwordNueva })
        });
        if (!res.ok) {
            const msg = await extraerMensajeError(res, "No se pudo actualizar la contraseña.");
            throw new Error(msg);
        }
        const usuario = sessionStorage.getItem("usuario") || "admin";
        sessionStorage.setItem("authToken", btoa(`${usuario}:${passwordNueva}`));
        alert("Contraseña actualizada correctamente con hash BCrypt.");
    } catch (error) {
        alert(error.message);
    }
}

document.addEventListener("DOMContentLoaded", () => {
    const usuario = sessionStorage.getItem("usuario") || "admin";

    document.querySelectorAll("#nombreUsuarioTop, .nombre-usuario-activo").forEach(el => {
        el.textContent = usuario;
    });

    const sidebar = document.querySelector(".sidebar");
    if (sidebar && !sidebar.querySelector(".btn-logout-sidebar")) {
        const contenedor = document.createElement("div");
        contenedor.className = "sidebar-user-box";
        contenedor.innerHTML = `
            <div class="d-flex align-items-center justify-content-between text-light small mb-2 px-1 user-meta">
                <span class="fw-semibold text-white">
                    <i class="fa-solid fa-user-shield me-2 text-info"></i>${usuario}
                </span>
                <span class="badge bg-primary">ADMIN</span>
            </div>
            <div class="d-grid gap-1">
                <button type="button" class="btn btn-outline-info btn-sm w-100" onclick="abrirPromptCambioPassword()" title="Cambiar contraseña">
                    <i class="fa-solid fa-key me-2"></i><span>Cambiar clave</span>
                </button>
                <button type="button" class="btn btn-outline-light btn-sm w-100 btn-logout-sidebar" onclick="cerrarSesion()">
                    <i class="fa-solid fa-right-from-bracket me-2"></i><span>Cerrar sesión</span>
                </button>
            </div>
        `;
        sidebar.appendChild(contenedor);
    }

    ["mousemove", "keydown", "click", "scroll"].forEach(evento => {
        window.addEventListener(evento, reiniciarTemporizadorInactividad, { passive: true });
    });
    reiniciarTemporizadorInactividad();
});
