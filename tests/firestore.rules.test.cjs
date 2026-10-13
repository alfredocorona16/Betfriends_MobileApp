
const { readFileSync } = require("node:fs");
const { test, before, after } = require("node:test");
const assert = require("node:assert/strict");

const {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails
} = require("@firebase/rules-unit-testing");

let testEnv;

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: "demo-betfriends",
    firestore: {
      host: "127.0.0.1",
      port: 8080,
      rules: readFileSync("firestore.rules", "utf8")
    }
  });

  // Datos exclusivos del emulador.
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();

    await db.doc("usuarios/usuarioA").set({
      nombre: "Usuario A",
      saldo: 1000
    });

    await db.doc("usuarios/usuarioB").set({
      nombre: "Usuario B",
      saldo: 1000
    });

    await db.doc("perfilesPublicos/usuarioA").set({
      nombre: "Usuario A"
    });

    await db.doc("apuestas/apuesta1").set({
      participantIds: ["usuarioA", "usuarioB"],
      confirmedPot: 100
    });

    await db.doc("invitaciones/invitacion1").set({
      inviterId: "usuarioA",
      inviteeId: "usuarioB"
    });
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

test("El propietario puede leer su perfil", async () => {
  const db = testEnv.authenticatedContext("usuarioA").firestore();

  await assertSucceeds(
    db.doc("usuarios/usuarioA").get()
  );
});

test("No se puede leer el perfil privado ajeno", async () => {
  const db = testEnv.authenticatedContext("usuarioB").firestore();

  await assertFails(
    db.doc("usuarios/usuarioA").get()
  );
});

test("Un usuario sin sesion no puede leer perfiles publicos", async () => {
  const db = testEnv.unauthenticatedContext().firestore();

  await assertFails(
    db.doc("perfilesPublicos/usuarioA").get()
  );
});

test("Un usuario autenticado puede leer perfiles publicos", async () => {
  const db = testEnv.authenticatedContext("usuarioB").firestore();

  await assertSucceeds(
    db.doc("perfilesPublicos/usuarioA").get()
  );
});

test("Un usuario no puede aumentar arbitrariamente su saldo", async () => {
  const db = testEnv.authenticatedContext("usuarioA").firestore();

  await assertFails(
    db.doc("usuarios/usuarioA").update({
      saldo: 999999
    })
  );
});

test("Un usuario no puede modificar directamente una apuesta", async () => {
  const db = testEnv.authenticatedContext("usuarioA").firestore();

  await assertFails(
    db.doc("apuestas/apuesta1").update({
      confirmedPot: 999999
    })
  );
});

test("Un usuario ajeno no puede consultar invitaciones", async () => {
  const db = testEnv.authenticatedContext("usuarioC").firestore();

  await assertFails(
    db.doc("invitaciones/invitacion1").get()
  );
});

test("Los participantes pueden consultar su apuesta", async () => {
  const db = testEnv.authenticatedContext("usuarioA").firestore();

  await assertSucceeds(
    db.doc("apuestas/apuesta1").get()
  );
});
