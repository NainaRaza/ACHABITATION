import { state } from "./state.js";

export function selectedCurrency() {
    return state.selectedTrip?.referenceCurrency || "EUR";
}

export function money(value, currency = selectedCurrency()) {
    const number = Number(value ?? 0);
    return new Intl.NumberFormat("fr-FR", {
        style: "currency",
        currency: currency || "EUR",
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    }).format(number);
}

export function dateFr(value) {
    if (!value) return "—";
    return new Date(`${value}T00:00:00`).toLocaleDateString("fr-FR");
}

export function canonicalConstraintName(value) {
    return String(value ?? "").trim().replace(/\s+/g, " ");
}

export function constraintKey(value) {
    return canonicalConstraintName(value)
        .normalize("NFD")
        .replace(/[\u0300-\u036f]/g, "")
        .toLowerCase();
}
