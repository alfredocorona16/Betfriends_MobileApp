const assert = require("node:assert/strict");
const { before, after, test } = require("node:test");
const { randomUUID } = require("node:crypto");
const { readFileSync } = require("node:fs");
const { initializeTestEnvironment, assertFails, assertSucceeds } = require("@firebase/rules-unit-testing");
const { serverTimestamp, GeoPoint, increment, arrayUnion } = require("firebase/firestore");

let env;
before(async () => {
  env = await initializeTestEnvironment({ projectId: "demo-betfriends",
    firestore: { host: "127.0.0.1", port: 8080, rules: readFileSync("firestore.rules", "utf8") } });
});
after(async () => { if (env) await env.cleanup(); });

function fixture(options = {}) {
  const id = randomUUID();
  const ids = Array.from({ length: options.count || 3 }, (_, i) => id + "-" + i);
  const [creator, other, mod] = ids;
  const db = (uid) => env.authenticatedContext(uid, { email: uid + "@example.test" }).firestore();
  const bet = (uid) => db(uid).doc("apuestas/" + id);
  const privateData = (uid) => ({ nombre: "Alex", correo: uid + "@example.test", puntos: 0, fechaCreacion: serverTimestamp() });
  const members = ids.map((uid) => ({ id: uid, name: "Alex", email: uid + "@example.test",
    invitationStatus: uid === creator ? "ACCEPTED" : "PENDING" }));
  const now = Date.now();
  const data = { id, creatorId: creator, creatorName: "Alex", title: "Circuito", description: "",
    type: "TIME", startsAtMillis: now - 180000, endsAtMillis: now + 180000,
    participants: members, participantIds: ids, acceptedParticipantIds: [creator], declinedParticipantIds: [],
    moderatorId: mod, status: "WAITING", winnerName: null, pointsProcessed: false, ...options.fields };

  async function create() {
    const client = db(creator);
    await Promise.all(ids.map((uid) => db(uid).doc("usuarios/" + uid).set(privateData(uid))));
    const batch = client.batch();
    batch.set(client.doc("apuestas/" + id), data);
    for (const uid of ids.slice(1)) {
      const key = id + "_" + uid;
      batch.set(client.doc("invitaciones/" + key), { id: key, betId: id, betTitle: data.title, inviterId: creator,
        inviterName: "Alex", inviteeId: uid, inviteeName: "Alex", status: "PENDING", createdAtMillis: now, updatedAtMillis: now });
    }
    return batch.commit();
  }

  function reply(uid, status = "ACCEPTED", actor = uid) {
    const c = db(actor), batch = c.batch();
    batch.update(c.doc("invitaciones/" + id + "_" + uid), { status, updatedAtMillis: Date.now() });
    batch.update(c.doc("apuestas/" + id), { [status === "ACCEPTED" ? "acceptedParticipantIds" : "declinedParticipantIds"]: arrayUnion(uid) });
    return batch.commit();
  }

  function checkIn(actor, candidate = actor, extra = {}) {
    return bet(actor).update({ ["checkInsByUid." + candidate]: {
      name: "Alex", email: actor + "@example.test", at: serverTimestamp(), ...extra } });
  }

  function approve(actor, candidate) {
    return bet(actor).update({ status: "FINISHED", winnerId: candidate, winnerName: "Alex", approvedBy: actor, approvedAt: serverTimestamp() });
  }

  async function pay(actor, uid, { delta, skipProfile, skipReceipt } = {}) {
    const c = db(actor), match = c.doc("apuestas/" + id);
    let observedCount = -1;
    while (true) {
      try {
        await c.runTransaction(async (tx) => {
          const b = (await tx.get(match)).data();
          const previous = b.creditedIds || [];
          observedCount = previous.length;
          if (previous.includes(uid)) return;
          const creditedIds = previous.concat(uid);
          const points = delta ?? (b.winnerId === uid ? 60 : 10);
          const key = id + "_" + uid;
          if (!skipProfile) tx.update(c.doc("usuarios/" + uid), { puntos: increment(points), ultimoRetoBF: id });
          if (!skipReceipt) tx.set(c.doc("movimientosPuntos/" + key), { id: key, userId: uid, betId: id, betTitle: b.title,
            wasWinner: b.winnerId === uid, points, createdAtMillis: Math.floor(b.approvedAt.toMillis()) });
          tx.update(match, { creditedIds, pointsProcessed: b.acceptedParticipantIds.filter((v) => v !== b.moderatorId).every((v) => creditedIds.includes(v)) });
        });
        return;
      } catch (error) {
        const paid = (await match.get({ source: "server" })).data().creditedIds || [];
        if (paid.includes(uid)) return;
        if (observedCount < 0 || paid.length <= observedCount) throw error;
      }
    }
  }
  return { id, ids, creator, other, mod, data, db, bet, create, reply, checkIn, approve, pay };
}

test("El moderador invitado confirma un UID con llegada y el reparto se puede retomar una sola vez", async () => {
  const f = fixture(), { creator, other, mod } = f;
  await assertSucceeds(f.create());
  await assertFails(f.checkIn(creator));  // Moderador pendiente.
  await assertFails(f.reply(mod, "ACCEPTED", other));
  await assertSucceeds(f.reply(mod));
  await assertFails(f.checkIn(other));    // Competidor pendiente.
  await assertSucceeds(f.reply(other));
  await assertFails(f.checkIn(mod));
  await assertFails(f.checkIn(creator, other));
  await assertFails(f.approve(mod, creator)); // Falta llegada.
  await assertSucceeds(f.checkIn(creator));
  await assertSucceeds(f.checkIn(other));
  await assertFails(f.checkIn(creator)); // Reemplazar el registro.
  await assertFails(f.approve(creator, creator));
  await assertFails(f.approve("ajeno", other));
  await assertFails(f.approve(mod, mod));
  await assertFails(f.bet(creator).update({ moderatorId: creator }));
  await assertSucceeds(f.approve(mod, other));
  await assertFails(f.approve(mod, creator)); // Resultado definitivo.
  assert.equal((await f.bet(creator).get()).data().winnerId, other);
  await assertFails(f.pay(mod, other, { delta: 600 }));
  await assertFails(f.pay(mod, other, { skipProfile: true }));
  await assertFails(f.pay(mod, other, { skipReceipt: true }));
  await assertFails(f.pay(creator, other));
  await assertFails(f.pay(mod, mod));
  await Promise.all([f.pay(mod, other), f.pay(other, other)]);
  await assertSucceeds(f.pay(creator, creator));
  await assertSucceeds(f.pay(mod, other));
  for (const [uid, n] of [[creator, 10], [other, 60], [mod, 0]]) {
    assert.equal((await f.db(uid).doc("usuarios/" + uid).get()).data().puntos, n);
  }
  assert.equal((await f.bet(mod).get()).data().pointsProcessed, true);
});

test("El creador puede moderar y la llegada GPS exige radio, precisión y horario válidos", async () => {
  const f = fixture({ fields: { type: "LOCATION", radiusMeters: 100, targetLatitude: 19.3, targetLongitude: -98.2 } });
  f.data.moderatorId = f.creator;
  await f.create(); await f.reply(f.other);
  await assertFails(f.checkIn(f.creator));
  await assertFails(f.checkIn(f.other));
  await assertFails(f.checkIn(f.other, f.other, { position: new GeoPoint(20, -98.2), accuracy: 5 }));
  await assertFails(f.checkIn(f.other, f.other, { position: new GeoPoint(19.3, -98.2), accuracy: 500 }));
  await assertSucceeds(f.checkIn(f.other, f.other, { position: new GeoPoint(19.3, -98.2), accuracy: 5 }));
  await assertSucceeds(f.approve(f.creator, f.other));
  const expired = fixture({ fields: { startsAtMillis: 100, endsAtMillis: 200 } });
  await expired.create(); await expired.reply(expired.mod);
  await assertFails(expired.checkIn(expired.creator));
});

test("Una negativa permite reasignar el rol y un reto anterior abierto admite designación", async () => {
  const f = fixture(); await f.create(); await f.reply(f.mod, "DECLINED");
  await assertFails(f.bet(f.other).update({ moderatorId: f.other }));
  await assertFails(f.bet(f.creator).update({ moderatorId: f.mod }));
  await assertSucceeds(f.bet(f.creator).update({ moderatorId: f.creator }));
  await f.reply(f.other); await f.checkIn(f.other); await f.approve(f.creator, f.other);

  const old = fixture();
  const legacy = { ...old.data };
  delete legacy.moderatorId; delete legacy.declinedParticipantIds;
  await env.withSecurityRulesDisabled((ctx) => ctx.firestore().doc("apuestas/" + old.id).set(legacy));
  await assertFails(old.bet(old.other).update({ moderatorId: old.other }));
  await assertSucceeds(old.bet(old.creator).update({ moderatorId: old.creator }));
  await env.withSecurityRulesDisabled((ctx) => ctx.firestore().doc("apuestas/" + old.id).update({
    status: "FINISHED", winnerName: "Histórico"
  }));
  await assertFails(old.approve(old.creator, old.other));
});

test("La creación y las invitaciones admiten el máximo de diez personas", async () => {
  const f = fixture({ count: 10 });
  await assertSucceeds(f.create());
  const invalid = fixture();
  await assertFails(invalid.bet(invalid.creator).set({ ...invalid.data, winnerId: invalid.other }));
  await assertFails(invalid.bet(invalid.other).set(invalid.data));
});

test("El directorio publica únicamente la identidad de la cuenta", async () => {
  const f = fixture(); await f.create();
  const uid = f.other, now = Date.now(), c = f.db(uid);
  const profile = { uid, nombre: "Alex", correo: uid + "@example.test",
    correoBusqueda: uid + "@example.test", actualizadoEn: now };
  await assertSucceeds(c.doc("perfilesPublicos/" + uid).set(profile));
  await assertFails(c.doc("perfilesPublicos/" + f.mod).set(profile));
  await assertFails(c.doc("perfilesPublicos/" + uid).update({ correoBusqueda: "ajeno@example.test" }));
  assert.equal((await f.db(f.creator).collection("perfilesPublicos").where("correoBusqueda", "==", profile.correo).get()).size, 1);
});

test("Si dos dispositivos confirman distintos candidatos, solo queda un ganador y los créditos distintos pueden concurrir", async () => {
  const f = fixture({ count: 5 });
  await f.create();
  for (const uid of f.ids.slice(1)) await f.reply(uid);
  await Promise.all([f.checkIn(f.creator), f.checkIn(f.other)]);
  const outcomes = await Promise.allSettled([f.approve(f.mod, f.creator), f.approve(f.mod, f.other)]);
  assert.equal(outcomes.filter((r) => r.status === "fulfilled").length, 1);
  const competitors = f.ids.filter((uid) => uid !== f.mod);
  await Promise.all(competitors.map((uid) => f.pay(uid, uid)));
  const final = (await f.bet(f.mod).get()).data();
  assert.equal(final.pointsProcessed, true);
  assert.deepEqual(new Set(final.creditedIds), new Set(competitors));
  assert.ok([f.creator, f.other].includes(final.winnerId));
  for (const uid of competitors) {
    const balance = (await f.db(uid).doc("usuarios/" + uid).get()).data().puntos;
    assert.equal(balance, uid === final.winnerId ? 60 : 10);
  }
});
