import { expect, test } from "@playwright/test";

function json(status, body) {
  return {
    status,
    contentType: "application/json",
    body: body == null ? "" : JSON.stringify(body)
  };
}


async function openAccountPanel(page) {
  await page.click("#topProfileBtn");
  await expect(page.locator("#profilePanel")).toHaveClass(/active/);
}

async function closeAccountPanel(page) {
  await page.click("#closeProfilePanelBtn");
  await expect(page.locator("#dashboardTab")).toHaveClass(/active/);
}

async function openTripTab(page, tab, panelSelector) {
  await page.click(`.tab[data-tab="${tab}"]`);
  await expect(page.locator(panelSelector)).toHaveClass(/active/);
}

async function mockApi(page) {
  const db = {
    users: [],
    trips: [],
    persons: [],
    expenses: [],
    invitations: [],
    logoutCalls: 0
  };
  let nextId = 1;
  const id = prefix => `${prefix}-${nextId++}`;

  await page.route("**/api/v1/**", async route => {
    const request = route.request();
    const url = new URL(request.url());
    const path = url.pathname.replace("/api/v1", "");
    const method = request.method();
    const token = request.headers().authorization?.replace("Bearer ", "");
    const body = request.postDataJSON?.() ?? null;

    if (path === "/health") return route.fulfill(json(200, { status: "UP" }));
    if (path === "/auth/register" && method === "POST") {
      const user = { userId: id("user"), email: body.email, displayName: body.displayName, accessToken: id("token") };
      db.users.push(user);
      return route.fulfill(json(200, user));
    }
    if (path === "/auth/login" && method === "POST") {
      const user = db.users.find(candidate => candidate.email === body.email || candidate.displayName === body.email);
      return route.fulfill(user ? json(200, user) : json(401, { message: "Identifiants invalides" }));
    }
    if (path === "/auth/logout" && method === "POST") {
      db.logoutCalls += 1;
      return route.fulfill(json(204, null));
    }
    if (!token) return route.fulfill(json(401, { message: "Session expirée" }));
    if (path === "/auth/profile" && method === "GET") {
      const user = db.users.find(candidate => candidate.accessToken === token);
      return route.fulfill(json(200, { userId: user?.userId, email: user?.email, displayName: user?.displayName, livingRest: 0, weightMode: "AVERAGE", customConstraints: [], linkedPersons: [] }));
    }
    if (path === "/trips" && method === "GET") return route.fulfill(json(200, db.trips));
    if (path === "/trips" && method === "POST") {
      const trip = { id: id("trip"), name: body.name, startDate: body.startDate, endDate: body.endDate, referenceCurrency: body.referenceCurrency || "EUR", customConstraints: [] };
      db.trips.push(trip);
      return route.fulfill(json(200, trip));
    }

    const personCurrentUser = path.match(/^\/trips\/([^/]+)\/persons\/current-user$/);
    if (personCurrentUser && method === "POST") {
      const tripId = personCurrentUser[1];
      const trip = db.trips.find(candidate => candidate.id === tripId);
      const user = db.users.find(candidate => candidate.accessToken === token);
      const person = { id: "person-1", tripId, name: user.displayName, linkedUserId: user.userId, guest: false, active: true, livingRestHidden: false, weightMode: "AVERAGE", presencePeriods: [{ startDate: trip.startDate, endDate: trip.endDate }], customConstraints: [] };
      db.persons.push(person);
      return route.fulfill(json(200, person));
    }

    const persons = path.match(/^\/trips\/([^/]+)\/persons$/);
    if (persons && method === "GET") return route.fulfill(json(200, db.persons.filter(person => person.tripId === persons[1])));

    const expenses = path.match(/^\/trips\/([^/]+)\/expenses$/);
    if (expenses && method === "GET") return route.fulfill(json(200, db.expenses.filter(expense => expense.tripId === expenses[1])));
    if (expenses && method === "POST") {
      const expense = { id: id("expense"), tripId: expenses[1], payerName: "Alpha", ...body };
      db.expenses.push(expense);
      return route.fulfill(json(200, expense));
    }

    const summary = path.match(/^\/trips\/([^/]+)\/summary$/);
    if (summary && method === "GET") return route.fulfill(json(200, { referenceCurrency: "EUR", balances: [], settlements: [] }));

    const audit = path.match(/^\/trips\/([^/]+)\/audit-logs$/);
    if (audit && method === "GET") return route.fulfill(json(200, []));

    const invitations = path.match(/^\/trips\/([^/]+)\/invitations$/);
    if (invitations && method === "GET") return route.fulfill(json(200, db.invitations.filter(invitation => invitation.tripId === invitations[1])));
    if (invitations && method === "POST") {
      const invitation = { id: id("invitation"), tripId: invitations[1], code: "INVITE-123", roleToGrant: body.roleToGrant || "PARTICIPANT", usable: true };
      db.invitations.push(invitation);
      return route.fulfill(json(200, invitation));
    }

    return route.fulfill(json(404, { message: `Route non mockée : ${method} ${path}` }));
  });
  return db;
}

test("inscription, voyage, participant, dépense et résumé", async ({ page }) => {
  await mockApi(page);
  await page.goto("/");
  await openAccountPanel(page);

  await page.fill("#registerEmail", "alpha@example.test");
  await page.fill("#registerDisplayName", "Alpha");
  await page.fill("#registerPassword", "password123");
  await page.click("#createUserBtn");
  await expect(page.locator("#messageBox")).toContainText("Compte créé");
  await closeAccountPanel(page);

  await page.click("#showTripFormBtn");
  await page.fill("#tripName", "Vacances test");
  await page.fill("#tripStartDate", "2026-08-01");
  await page.fill("#tripEndDate", "2026-08-15");
  await page.fill("#tripCurrency", "EUR");
  await page.click("#tripForm button[type=submit]");
  await expect(page.locator("#messageBox")).toContainText("Voyage créé");

  await openTripTab(page, "persons", "#personsTab");
  await expect(page.locator("#addCurrentUserPersonBtn")).toBeVisible();
  page.once("dialog", dialog => dialog.accept());
  await page.click("#addCurrentUserPersonBtn");
  await expect(page.locator("#messageBox")).toContainText("ajouté");

  await openTripTab(page, "expenses", "#expensesTab");
  await expect(page.locator("#showExpenseFormBtn")).toBeVisible();
  await page.click("#showExpenseFormBtn");
  await page.fill("#expenseTitle", "Courses");
  await page.fill("#expenseDate", "2026-08-02");
  await page.selectOption("#expensePayer", "person-1");
  await page.fill("#expenseTotal", "42");
  await page.click("#expenseForm button[type=submit]");
  await expect(page.locator("#messageBox")).toContainText("Dépense ajoutée");

  await page.click('.tab[data-tab="summary"]');
  await expect(page.locator("#summaryTab")).toHaveClass(/active/);
});

test("erreur de connexion affichée proprement", async ({ page }) => {
  await mockApi(page);
  await page.goto("/");
  await openAccountPanel(page);

  await page.fill("#loginIdentifier", "inconnu@example.test");
  await page.fill("#loginPassword", "password123");
  await page.click("#loginUserBtn");

  await expect(page.locator("#messageBox")).toContainText("Connexion refusée");
});

test("logout appelle le backend et nettoie la session locale", async ({ page }) => {
  const db = await mockApi(page);
  await page.goto("/");
  await openAccountPanel(page);

  await page.fill("#registerEmail", "logout@example.test");
  await page.fill("#registerDisplayName", "Logout");
  await page.fill("#registerPassword", "password123");
  await page.click("#createUserBtn");
  await expect(page.locator("#logoutUserBtn")).toBeVisible();

  await page.click("#logoutUserBtn");
  await expect(page.locator("#messageBox")).toContainText("Compte déconnecté");
  expect(db.logoutCalls).toBe(1);
  await expect.poll(() => page.evaluate(() => localStorage.getItem("achabitation.user"))).toBe(null);
});
