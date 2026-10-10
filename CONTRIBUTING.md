# Guía de contribución de BetFriends

Este documento establece la forma en que el equipo debe crear issues, ramas, commits y Pull Requests dentro del repositorio de BetFriends.

## 1. Principio general

Todo cambio debe estar relacionado con un issue de GitHub.

La relación esperada es:

```text
1 issue → 1 rama → 1 Pull Request
```

No se deben realizar commits directamente en `master`.

## 2. Tipos de elementos

El GitHub Project utiliza los siguientes tipos:

| Tipo | Uso |
|---|---|
| Epic | Agrupa HU, Task y Bug relacionados con un objetivo |
| HU | Representa una necesidad de una persona usuaria |
| Task | Trabajo técnico, documentación, pruebas o configuración |
| Bug | Corrección de un comportamiento incorrecto |

Las épicas no reciben Story Points y pueden abarcar varios sprints.

## 3. Campos obligatorios del Project

Antes de iniciar un elemento se deben completar:

- `Status`
- `Type`
- `MoSCoW`
- `Story Points`
- `Sprint`
- `Assignee`
- `Parent issue`

Los valores de `Status` son:

| Momento | Status |
|---|---|
| Issue creado y pendiente | Backlog |
| Se comienza a trabajar | In Progress |
| Se abre el Pull Request | In Review |
| La PR se fusiona y valida | Done |

Las categorías MoSCoW son:

- `Must`: indispensable para la versión actual.
- `Should`: importante, pero no bloquea completamente la entrega.
- `Could`: deseable si existe tiempo.
- `Won't`: fuera del alcance de la versión actual.

## 4. Antes de comenzar un cambio

El issue debe cumplir la definición de Ready:

- Tiene título claro.
- Tiene tipo definido.
- Tiene criterios de aceptación verificables.
- Tiene responsable.
- Tiene Story Points cuando corresponde.
- Tiene Sprint.
- Tiene MoSCoW.
- Está vinculado a una épica.
- Sus dependencias están resueltas o identificadas.

Después, actualizar `master`:

```bash
git switch master
git pull --ff-only origin master
git status
```

El resultado debe indicar:

```text
nothing to commit, working tree clean
```

## 5. Convención de ramas

Formato:

```text
tipo/numero-descripcion-corta
```

Tipos permitidos:

| Prefijo | Uso | Ejemplo |
|---|---|---|
| `feat` | Historia o funcionalidad | `feat/90-puntos-bf` |
| `fix` | Corrección de bug | `fix/75-gradle-wrapper` |
| `docs` | Documentación | `docs/76-readme-architecture` |
| `test` | Pruebas | `test/91-domain-rules` |
| `chore` | Mantenimiento o configuración | `chore/77-templates` |
| `refactor` | Reestructuración sin modificar comportamiento | `refactor/92-bet-model` |
| `ci` | Integración continua | `ci/93-android-checks` |
| `release` | Preparación de una entrega | `release/100-version-1` |

Reglas para las ramas:

- Usar el número real del issue.
- Usar letras minúsculas.
- Separar palabras con guiones.
- No usar espacios.
- No usar acentos.
- No incluir nombres de integrantes.
- Crear la rama desde `master` actualizado.
- No reutilizar una rama de otro issue.

Ejemplo:

```bash
git switch -c chore/77-templates
```

## 6. Convención de commits

Formato:

```text
tipo(alcance): descripción breve
```

Tipos permitidos:

| Tipo | Uso |
|---|---|
| `feat` | Nueva funcionalidad |
| `fix` | Corrección de un defecto |
| `docs` | Documentación |
| `test` | Creación o actualización de pruebas |
| `refactor` | Reestructuración sin cambio funcional |
| `build` | Gradle, dependencias o compilación |
| `ci` | Integración continua |
| `chore` | Mantenimiento o configuración |
| `style` | Formato sin cambio funcional |
| `perf` | Mejora de rendimiento |
| `revert` | Reversión de un cambio |

Alcances recomendados:

- `auth`
- `points`
- `bets`
- `invitations`
- `checkin`
- `firebase`
- `ui`
- `gradle`
- `github`
- `qa-security`

Ejemplos:

```text
feat(points): mostrar saldo en puntos BF
fix(checkin): usar uid del usuario autenticado
docs(readme): documentar modelo Firebase
test(bets): cubrir cálculo del premio
build(gradle): actualizar wrapper
ci(android): ejecutar pruebas en pull requests
chore(github): agregar plantillas
```

La descripción debe:

- Comenzar en minúscula.
- Utilizar un verbo en infinitivo.
- Explicar un solo cambio.
- Ser breve y específica.
- No terminar con punto.

Para relacionar un commit con el issue sin cerrarlo:

```text
Refs #NUMERO
```

Ejemplo:

```bash
git commit \
  -m "chore(github): agregar plantillas y convenciones" \
  -m "Refs #77"
```

## 7. Preparar archivos

Antes de agregar archivos:

```bash
git status --short
```

Agregar únicamente los archivos relacionados con el issue:

```bash
git add archivo1
git add archivo2
```

Evitar utilizar:

```bash
git add .
```

especialmente cuando existan archivos locales o modificaciones ajenas.

Revisar lo preparado:

```bash
git diff --cached --name-status
git diff --cached --check
git diff --cached --stat
```

Si `git diff --cached --check` no muestra información, no se detectaron errores de formato.

## 8. Validaciones antes del push

Cuando corresponda, ejecutar:

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

Los comandos deben terminar con:

```text
BUILD SUCCESSFUL
```

Para tareas exclusivamente documentales se debe revisar:

- Ortografía.
- Enlaces.
- Rutas de archivos.
- Renderizado Markdown.
- Diagramas Mermaid.
- Ausencia de datos sensibles.

## 9. Subir una rama

La primera vez:

```bash
git push -u origin nombre-de-la-rama
```

Las siguientes veces:

```bash
git push
```

No se debe utilizar `--force` en ramas compartidas.

## 10. Pull Requests

Toda PR debe:

- Originarse desde una rama específica.
- Apuntar a `master`.
- Utilizar la plantilla del repositorio.
- Incluir `Closes #NUMERO`.
- Describir claramente los cambios.
- Explicar cómo fueron probados.
- Incluir evidencia cuando corresponde.
- Describir riesgos o migraciones.
- Recibir al menos una revisión.
- Tener sus comprobaciones exitosas.

Al abrir la PR, el issue debe pasar de:

```text
In Progress → In Review
```

El autor no debe marcar el issue como `Done` antes del merge.

## 11. Revisión

La persona revisora debe comprobar:

- Que el issue vinculado sea el correcto.
- Que los criterios estén cumplidos.
- Que no existan cambios fuera del alcance.
- Que no se incluyan secretos.
- Que las pruebas sean suficientes.
- Que la documentación corresponda al comportamiento real.
- Que la rama pueda integrarse sin conflictos.

Las observaciones deben resolverse antes del merge.

## 12. Después del merge

Actualizar el repositorio local:

```bash
git switch master
git pull --ff-only origin master
```

Eliminar la rama local:

```bash
git branch -d nombre-de-la-rama
```

La rama remota puede eliminarse desde GitHub después del merge.

Confirmar que:

- La PR está fusionada.
- El issue quedó cerrado.
- La PR aparece en `Linked pull requests`.
- El elemento está en `Done`.
- Los criterios de aceptación están marcados.

## 13. Definición de Done

Un elemento puede pasar a `Done` únicamente cuando:

- Cumple todos sus criterios de aceptación.
- Los cambios están integrados en `master`.
- La aplicación compila cuando corresponde.
- Las pruebas necesarias pasan.
- La revisión fue aprobada.
- La PR fue fusionada.
- El issue quedó cerrado.
- La documentación fue actualizada.
- No existen secretos ni datos sensibles.
- La evidencia está registrada.