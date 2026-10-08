# Gestión de BetFriends en GitHub Projects

Este documento define la forma en que el equipo administra el backlog,
los sprints, las historias de usuario y las pull requests de BetFriends.

## Tipos de elementos

| Tipo | Uso |
|---|---|
| Epic | Agrupa historias relacionadas con un objetivo general |
| HU | Representa una necesidad de una persona usuaria |
| Task | Trabajo técnico, documental o de configuración |
| Bug | Corrección de un comportamiento incorrecto |

## Estados

| Estado | Significado |
|---|---|
| Backlog | Elemento registrado, pero todavía no iniciado |
| In Progress | El responsable está trabajando en el elemento |
| In Review | Existe una PR o resultado pendiente de revisión |
| Done | El trabajo fue revisado e integrado |

## Priorización MoSCoW

| Prioridad | Significado |
|---|---|
| Must | Indispensable para la versión actual |
| Should | Importante, pero no bloquea la entrega principal |
| Could | Mejora opcional si existe capacidad |
| Won't | Fuera del alcance de la versión actual |

## Story Points

Las historias, tareas y bugs se estiman con valores del 1 al 5.

Las épicas no reciben Story Points porque abarcan varios elementos y
posiblemente más de un sprint.

## Sprints

Los sprints tienen una duración máxima de siete días.

Cada elemento que entre a un sprint debe tener:

- Responsable.
- Criterios de aceptación.
- Tipo.
- MoSCoW.
- Story Points.
- Parent issue.
- Sprint.

## Vistas del Project

### Backlog de recuperación

Muestra el trabajo del MVP de puntos BF que todavía no está terminado.

### Sprint actual

Tablero agrupado por Status y filtrado mediante `Sprint:@current`.

### Roadmap

Muestra las épicas y su avance general.

### QA

Muestra los elementos con Status igual a In Review.

### Bugs

Muestra los elementos con tipo Bug, priorizados mediante MoSCoW.

### Carga del equipo

Agrupa el trabajo del sprint actual por Assignee.

### Legacy

Contiene trabajo histórico, reemplazado o registrado después de su
implementación.

## Flujo de trabajo

1. Un issue nuevo entra con Status igual a Backlog.
2. Al comenzar el desarrollo cambia a In Progress.
3. Al abrir una pull request cambia a In Review.
4. La PR debe incluir `Closes #número`.
5. Después de la revisión y el merge, el issue se cierra y pasa a Done.

## Convención de ramas

- `feat/<issue>-descripcion`
- `fix/<issue>-descripcion`
- `chore/<issue>-descripcion`
- `test/<issue>-descripcion`

Ejemplo:

`fix/81-identidad-checkin`

## Definición de Ready

Un elemento está listo para entrar a un sprint cuando tiene:

- Descripción completa.
- Criterios de aceptación.
- Responsable.
- Tipo.
- MoSCoW.
- Story Points.
- Sprint.
- Dependencias identificadas.

## Definición de Done

Un elemento se considera terminado cuando:

- Cumple todos sus criterios de aceptación.
- Incluye las pruebas correspondientes.
- Tiene una pull request revisada.
- La pull request fue integrada a `master`.
- La documentación o migración necesaria fue actualizada.
