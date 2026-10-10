Closes #NUMERO_DEL_ISSUE

## Elemento del backlog

- **ID interno:** `BF-`
- **Tipo:** Epic / HU / Task / Bug
- **Épica relacionada:** `BF-EP`
- **Sprint:** Sprint N
- **Responsable:** @usuario

## Resumen

Explicar brevemente el objetivo del Pull Request y el problema que resuelve.

## Cambios realizados

- Describir el cambio principal.
- Indicar los archivos o módulos modificados.
- Mencionar las decisiones técnicas importantes.
- Indicar cualquier comportamiento que permanezca pendiente.

## Criterios de aceptación

- [ ] Criterio 1 del issue.
- [ ] Criterio 2 del issue.
- [ ] Criterio 3 del issue.

## Cómo se probó

### Validación manual

1. Indicar el primer paso.
2. Indicar la acción realizada.
3. Explicar el resultado obtenido.

### Comandos ejecutados

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

### Resultados

- [ ] Las pruebas unitarias terminaron correctamente.
- [ ] La compilación debug terminó correctamente.
- [ ] El flujo principal fue probado manualmente.
- [ ] No aplica ejecutar pruebas para este cambio.

> Marcar “No aplica” únicamente en cambios de documentación o configuración que no afecten la compilación.

## Evidencia

Agregar aquí:

- Capturas de pantalla.
- Video del flujo.
- Resultado `BUILD SUCCESSFUL`.
- Captura de las plantillas o documentación.
- Registros relevantes sin información sensible.

Si no se requiere evidencia visual, escribir:

```text
No aplica: el cambio no modifica la interfaz.
```

## Riesgos y migración

- **Riesgos conocidos:** describir o escribir `No aplica`.
- **Cambio de configuración:** describir o escribir `No aplica`.
- **Migración de datos:** describir o escribir `No aplica`.
- **Forma de revertir:** explicar cómo regresar el cambio.

## Archivos o datos sensibles

Confirmar que este PR no contiene:

- Contraseñas.
- Tokens.
- Claves privadas.
- Cuentas de servicio.
- Keystores.
- Datos personales usados como evidencia.
- Archivos generados por el IDE.

## Notas para la revisión

Indicar qué partes necesitan mayor atención por parte del revisor.

## Checklist final

- [ ] Reemplacé `#NUMERO_DEL_ISSUE` por el número correcto.
- [ ] La PR contiene `Closes #N`.
- [ ] La PR apunta hacia `master`.
- [ ] Los cambios pertenecen únicamente al issue vinculado.
- [ ] Cumplí todos los criterios de aceptación.
- [ ] Revisé personalmente los cambios.
- [ ] Las pruebas necesarias terminaron correctamente.
- [ ] La aplicación compila cuando corresponde.
- [ ] No incluí secretos ni datos sensibles.
- [ ] Actualicé la documentación cuando corresponde.
- [ ] Agregué evidencia cuando corresponde.
- [ ] El issue se encuentra en `In Review`.