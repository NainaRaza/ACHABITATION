import { state, writeJson, asArray } from "./src/state.js";
import { api, fetchBlob, sanitizeAuthUser } from "./src/api.js";
import { $, showMessage, showApiError } from "./src/ui.js";
import { money, dateFr, selectedCurrency, canonicalConstraintName, constraintKey, parseCsvSet } from "./src/utils.js";

import { install as installFormHelpers } from "./src/form-helpers.js";
import { install as installConstraints } from "./src/constraints.js";
import { install as installProfile } from "./src/profile.js";
import { install as installAuth } from "./src/auth.js";
import { install as installTrips } from "./src/trips.js";
import { install as installInvitations } from "./src/invitations.js";
import { install as installPersons } from "./src/persons.js";
import { install as installExpenses } from "./src/expenses.js";
import { install as installSummary } from "./src/summary.js";
import { install as installAudit } from "./src/audit.js";
import { install as installRender } from "./src/render.js";

const app = {
    state,
    writeJson,
    asArray,
    api,
    fetchBlob,
    sanitizeAuthUser,
    $,
    showMessage,
    showApiError,
    money,
    dateFr,
    selectedCurrency,
    canonicalConstraintName,
    constraintKey,
    parseCsvSet
};

[
    installFormHelpers,
    installConstraints,
    installProfile,
    installAuth,
    installTrips,
    installInvitations,
    installPersons,
    installExpenses,
    installSummary,
    installAudit,
    installRender
].forEach(install => install(app));

window.AchabitationApp = app;

window.addEventListener("DOMContentLoaded", () => {
    app.init().catch(error => app.showMessage(error.message, "error"));
});
