import assert from "node:assert/strict";

class MockClassList {
  constructor() { this.values = new Set(); }
  add(...names) { names.forEach(name => this.values.add(name)); }
  remove(...names) { names.forEach(name => this.values.delete(name)); }
  toggle(name, force) {
    if (force === true) { this.values.add(name); return true; }
    if (force === false) { this.values.delete(name); return false; }
    if (this.values.has(name)) { this.values.delete(name); return false; }
    this.values.add(name); return true;
  }
  contains(name) { return this.values.has(name); }
}

class MockElement {
  constructor(id = "") {
    this.id = id;
    this.value = "";
    this.textContent = "";
    this.innerHTML = "";
    this.checked = false;
    this.disabled = false;
    this.required = false;
    this.dataset = {};
    this.className = "";
    this.classList = new MockClassList();
    this.listeners = {};
  }
  addEventListener(type, handler) { this.listeners[type] = handler; }
  querySelectorAll() { return []; }
  querySelector() { return null; }
  closest() { return null; }
  reset() { this.value = ""; this.checked = false; }
  scrollIntoView() {}
  click() {}
  setAttribute(name, value) { this[name] = value; }
  removeAttribute(name) { delete this[name]; }
}

const elements = new Map();
function el(id) {
  if (!elements.has(id)) elements.set(id, new MockElement(id));
  return elements.get(id);
}

const localStore = new Map();
global.localStorage = {
  getItem(key) { return localStore.has(key) ? localStore.get(key) : null; },
  setItem(key, value) { localStore.set(key, String(value)); },
  removeItem(key) { localStore.delete(key); },
  clear() { localStore.clear(); }
};

global.window = {
  ACHABITATION_API_BASE_URL: "http://localhost:8080/api/v1",
  addEventListener() {},
  clearTimeout() {},
  setTimeout() { return 0; },
  URL: { createObjectURL() { return "blob:csv"; }, revokeObjectURL() {} }
};

global.document = {
  getElementById: el,
  querySelector() { return null; },
  querySelectorAll() { return []; },
  createElement() { return new MockElement(); },
  body: { appendChild() {}, removeChild() {} }
};

global.confirm = () => true;

function jsonResponse(status, body) {
  const ok = status >= 200 && status < 300;
  return {
    ok,
    status,
    statusText: ok ? "OK" : "ERROR",
    json: async () => body,
    text: async () => body === null || body === undefined ? "" : JSON.stringify(body),
    blob: async () => new Blob(["csv"])
  };
}

const calls = [];
const db = {
  users: [],
  trips: [],
  persons: [],
  expenses: [],
  invitations: []
};
let nextId = 1;
const id = prefix => `${prefix}-${nextId++}`;

global.fetch = async (url, options = {}) => {
  const parsedUrl = new URL(url);
  const path = parsedUrl.pathname.replace("/api/v1", "");
  const method = options.method || "GET";
  const token = options.headers?.Authorization?.replace("Bearer ", "");
  const body = options.body ? JSON.parse(options.body) : null;
  calls.push({ method, path, token, body });

  if (path === "/health") return jsonResponse(200, { status: "UP" });

  if (path === "/auth/register" && method === "POST") {
    const user = { userId: id("user"), email: body.email, displayName: body.displayName, accessToken: id("token") };
    db.users.push(user);
    return jsonResponse(200, user);
  }

  if (path === "/auth/login" && method === "POST") {
    const user = db.users.find(candidate => candidate.email === body.identifier || candidate.displayName === body.identifier);
    if (!user) return jsonResponse(401, { message: "Identifiants invalides" });
    return jsonResponse(200, user);
  }

  if (path === "/auth/logout" && method === "POST") return jsonResponse(204, null);

  if (!token) return jsonResponse(401, { message: "Session expirée" });

  if (path === "/auth/profile" && method === "GET") {
    const user = db.users.find(candidate => candidate.accessToken === token);
    return jsonResponse(200, { userId: user?.userId, email: user?.email, displayName: user?.displayName, livingRest: 0, weightMode: "AVERAGE", customConstraints: [], linkedPersons: [] });
  }

  if (path === "/trips" && method === "GET") return jsonResponse(200, db.trips);

  if (path === "/trips" && method === "POST") {
    const trip = { id: id("trip"), name: body.name, startDate: body.startDate, endDate: body.endDate, referenceCurrency: body.referenceCurrency, customConstraints: [] };
    db.trips.push(trip);
    return jsonResponse(200, trip);
  }

  const currentUser = db.users.find(candidate => candidate.accessToken === token);
  const personsCurrentUserMatch = path.match(/^\/trips\/([^/]+)\/persons\/current-user$/);
  if (personsCurrentUserMatch && method === "POST") {
    const tripId = personsCurrentUserMatch[1];
    const trip = db.trips.find(candidate => candidate.id === tripId);
    const person = { id: id("person"), tripId, name: currentUser.displayName, linkedUserId: currentUser.userId, guest: false, active: true, weightMode: "AVERAGE", livingRestHidden: false, presencePeriods: [{ startDate: trip.startDate, endDate: trip.endDate }], customConstraints: [] };
    db.persons.push(person);
    return jsonResponse(200, person);
  }

  const personsMatch = path.match(/^\/trips\/([^/]+)\/persons$/);
  if (personsMatch && method === "GET") return jsonResponse(200, db.persons.filter(person => person.tripId === personsMatch[1]));

  const expensesMatch = path.match(/^\/trips\/([^/]+)\/expenses$/);
  if (expensesMatch && method === "GET") return jsonResponse(200, db.expenses.filter(expense => expense.tripId === expensesMatch[1]));
  if (expensesMatch && method === "POST") {
    const tripId = expensesMatch[1];
    const payer = db.persons.find(person => person.id === body.payerPersonId);
    const expense = { id: id("expense"), tripId, payerName: payer?.name, ...body };
    db.expenses.push(expense);
    return jsonResponse(200, expense);
  }

  const summaryMatch = path.match(/^\/trips\/([^/]+)\/summary$/);
  if (summaryMatch && method === "GET") return jsonResponse(200, { balances: [], settlements: [] });

  const auditMatch = path.match(/^\/trips\/([^/]+)\/audit-logs$/);
  if (auditMatch && method === "GET") return jsonResponse(200, []);

  const invitationsMatch = path.match(/^\/trips\/([^/]+)\/invitations$/);
  if (invitationsMatch && method === "GET") return jsonResponse(200, db.invitations.filter(invitation => invitation.tripId === invitationsMatch[1]));
  if (invitationsMatch && method === "POST") {
    const invitation = { id: id("invitation"), tripId: invitationsMatch[1], code: "INVITE-123", role: body.role || "PARTICIPANT", active: true, expiresAt: body.expiresAt };
    db.invitations.push(invitation);
    return jsonResponse(200, invitation);
  }

  return jsonResponse(404, { message: `Route non mockée : ${method} ${path}` });
};

await import("../app.js");
const app = global.window.AchabitationApp;

el("registerEmail").value = "alpha@example.com";
el("registerDisplayName").value = "Alpha";
el("registerPassword").value = "password123";
await app.createUser();
assert.equal(app.state.user.email, "alpha@example.com");
assert.equal(calls.at(-1).path, "/trips");

el("tripName").value = "Vacances test";
el("tripStartDate").value = "2026-08-01";
el("tripEndDate").value = "2026-08-15";
el("tripCurrency").value = "EUR";
await app.createTrip({ preventDefault() {} });
assert.equal(app.state.trips.length, 1);
assert.equal(app.state.selectedTrip.name, "Vacances test");

await app.createCurrentUserPerson();
assert.equal(app.state.persons.length, 1);
assert.equal(app.state.persons[0].linkedUserId, app.state.user.userId);

el("expenseTitle").value = "Courses";
el("expenseDate").value = "2026-08-03";
el("expensePayer").value = app.state.persons[0].id;
el("expenseTotal").value = "42";
el("expenseMeat").value = "0";
el("expenseAlcohol").value = "0";
el("expenseType").value = "NORMAL";
el("expenseCurrency").value = "EUR";
el("expenseExchangeRate").value = "1";
el("expenseAdvancedMode").checked = false;
await app.saveExpense({ preventDefault() {} });
assert.equal(app.state.expenses.length, 1);
assert.equal(app.state.expenses[0].title, "Courses");

el("inviteRole").value = "PARTICIPANT";
el("inviteExpiresAt").value = "2026-08-10";
await app.createInvitation();
assert.equal(app.state.invitations.length, 1);
assert.equal(app.state.invitations[0].code, "INVITE-123");

await app.loadSummary();
assert.deepEqual(app.state.summary, { balances: [], settlements: [] });

await app.logoutUser();
assert.equal(app.state.user, null);
assert.ok(calls.some(call => call.method === "POST" && call.path === "/auth/logout"));

await assert.rejects(() => app.api("/trips"), /Session expirée|401|token/i);

console.log("Frontend flow tests OK");
