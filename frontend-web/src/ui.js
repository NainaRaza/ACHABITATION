export const $ = (id) => document.getElementById(id);

export function showMessage(message, type = "success") {
    const box = $("messageBox");
    if (!box) return;
    box.textContent = message;
    box.className = `message ${type}`;
    box.classList.remove("hidden");
    window.clearTimeout(showMessage._timer);
    showMessage._timer = window.setTimeout(() => box.classList.add("hidden"), type === "error" ? 9000 : 3500);
}

export function showApiError(prefix, error) {
    const message = error?.message || String(error || "Erreur inconnue");
    showMessage(`${prefix} : ${message}`, "error");
}
