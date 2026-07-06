import assert from "node:assert/strict";

global.window = { ACHABITATION_API_BASE_URL: "http://localhost:8080/api/v1" };
global.localStorage = {
  store: new Map(),
  getItem(key) { return this.store.has(key) ? this.store.get(key) : null; },
  setItem(key, value) { this.store.set(key, String(value)); },
  removeItem(key) { this.store.delete(key); }
};

const utils = await import("../src/utils.js");
const stateModule = await import("../src/state.js");

assert.equal(utils.canonicalConstraintName("  Sans   lactose  "), "Sans lactose");
assert.equal(utils.constraintKey("Été  Sans   Gluten"), "ete sans gluten");
assert.equal(utils.dateFr("2026-07-06"), "06/07/2026");

stateModule.state.selectedTrip = { referenceCurrency: "EUR" };
assert.match(utils.money(12.5), /12,50/);

console.log("Frontend smoke tests OK");
