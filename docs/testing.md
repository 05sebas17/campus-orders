# Pruebas y resultados comprobados

Verificaciones ejecutadas el **14 de noviembre de 2026**, en Windows con Docker Desktop (Linux). Los resultados siguientes corresponden a ejecuciones reales; corresponden al entorno local. Las comprobaciones de EC2 se indican al final de este documento.

## Resultado final

| Verificación | Resultado |
|---|---|
| Maven Wrapper 3.9.11 | Generado y ejecutado correctamente |
| Java 21.0.9 / Maven 3.9.11 en contenedor | `spotless:apply verify`: BUILD SUCCESS |
| JUnit/Mockito | 6 pruebas unitarias, 0 fallos, 0 errores, 0 omitidas |
| Integración PostgreSQL 17.6/Testcontainers | 11 pruebas, 0 fallos, 0 errores, 0 omitidas |
| Total backend | **17 aprobadas** |
| `npm run typecheck` | Aprobado |
| `npm run lint` | Aprobado, sin errores |
| Vitest/Testing Library | **5 aprobadas** en 2 archivos |
| Build de producción Vite | Aprobado; dist generado |
| Playwright/Chromium contra Compose real | **2 aprobadas** |
| `npm audit` | 0 vulnerabilidades reportadas, incluidas dependencias de desarrollo |
| Dockerfiles / Compose | Build y arranque correctos con Java 21 y Node 22 |
| Salud | db, backend y frontend saludables |
| Swagger y WSDL | HTTP 200; OpenAPI documenta POST 201 y ProblemDetail |
| SOAP HTTP real | GetOrderStatus devolvió PED-DEMO-001 |
| Reinicio de db/backend/frontend | 3 pedidos conservaron identificador, importe y estado |
| Proxy Nginx y ruta SPA profunda | Verificados mediante HTTP después del reinicio |
| Revisión visual | Escritorio 1440 px y móvil 390 px; sin errores JavaScript observados |

Los reportes unitarios e integración originales están en `backend/target/surefire-reports/` y `backend/target/failsafe-reports/`. Se conservan resúmenes de texto y log Java 21 en `docs/verification/`. El reporte de navegador se genera en `frontend/playwright-report/` y está excluido de Git.

## Qué se verifica

Unitarias: redondeo HALF_UP y total, historial inicial, transiciones secuenciales, saltos/retrocesos, repetición sin cambios, versión obsoleta, ausencia de productos, cantidades y catálogo inexistente/inactivo con Mockito.

Integración sobre una base PostgreSQL desechable: Flyway + Hibernate validate; creación 201 y Location; snapshot histórico después de cambiar precio/nombre; JSON inválido, correo, duplicados, fracciones/cantidades fuera de rango y precios enviados por cliente; 404; filtros UTC, búsqueda literal, orden/páginas; estadísticas globales; PATCH y versionado; dos transacciones simultáneas; detección directa de versión JPA obsoleta y rollback del historial; SOAP válido y Faults; rechazo de DTD; contrato OpenAPI.

Frontend: validación, cantidades fraccionarias y duplicados, añadir/quitar líneas, botón bloqueado durante envío y presentación del error del servidor con referencia.

E2E sin mocks: crea un pedido con dos cuadernos, verifica confirmación/detalle, avanza por los tres estados con diálogo, recarga, busca por cliente y filtra por entregado. El segundo test espera la tabla cargada y comprueba que la página móvil no desborda; la tabla tiene su propio scroll horizontal. Los pedidos E2E permanecen en la base local como datos de demostración.

## Reproducir en PowerShell

Desde `order-management`, con JDK 21 y Docker Desktop activo:

```powershell
Push-Location backend
.\mvnw.cmd -B -ntp verify
Pop-Location
Push-Location frontend
npm.cmd ci
npm.cmd run typecheck
npm.cmd run lint
npm.cmd test
npm.cmd run build
npm.cmd audit
Pop-Location
docker compose up -d --build
Push-Location frontend
npm.cmd exec playwright install chromium
npm.cmd run test:e2e
Pop-Location
```

Para usar Java 21 desde Docker sin cambiar el Java global de Windows:

```powershell
$backendPath = (Resolve-Path .\backend).Path
docker run --rm `
  -v "${backendPath}:/workspace" `
  -v /var/run/docker.sock:/var/run/docker.sock `
  -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal `
  -w /workspace maven:3.9.11-eclipse-temurin-21 `
  mvn -B -ntp verify
```

La verificación realizada también montó la caché Maven del equipo para acelerar descargas. Docker Desktop permite acceder a los puertos de los contenedores de prueba a través de host.docker.internal. Este comando está pensado para Docker Desktop; en Linux la configuración de host puede ser diferente.

Para solo unitarias: `.\mvnw.cmd test`. Sin Docker, `verify` no puede completar integraciones y debe considerarse pendiente/fallido, nunca aprobado. Si se cambia el puerto de la web: `$env:E2E_BASE_URL='http://localhost:PUERTO'` antes de ejecutar Playwright.

## Incidencias detectadas y resueltas

- Motor Docker detenido al comenzar: se inició Docker Desktop.
- Puertos 5432/8080 ocupados o reservados: se eligieron 55432/18080/18081.
- Se actualizaron dependencias vulnerables y se fijó minimatch 11.0.1 transitivo del linter. La auditoría final quedó en cero.
- Se corrigieron un selector de prueba del catálogo, el desbordamiento móvil de una etiqueta accesible, la coerción de cantidades decimales y una diferencia de codificación de texto en pruebas. Las ejecuciones finales incluyen estas correcciones.
- Swagger mostraba 200 para creación; se documentó 201 y se añadió una prueba de contrato.

## Límites reales

El Java global del equipo era 25.0.2; Java 21 fue verificado en contenedor. Para desarrollar directamente con Maven en Windows se debe seleccionar JDK 21. El build Vite emite dos avisos sobre comentarios de optimización de Zod que elimina al compilar; no impiden el build. Mockito avisa sobre carga dinámica del agente; las pruebas en Java 21 pasan.

No se ejecutaron pruebas de carga masiva, una auditoría de seguridad integral ni pruebas automatizadas en otros navegadores. No existe autenticación. Las pruebas automatizadas locales no se volvieron a ejecutar en EC2 y no demuestran un pipeline.

## Comprobaciones manuales en AWS — 15 de noviembre de 2026

Las capturas de la terminal muestran los tres servicios en estado healthy y la respuesta ok de la ruta /health del frontend. También se comprobó desde el navegador la creación de un pedido, su permanencia después de recargar y la validación de campos obligatorios. Estas últimas comprobaciones fueron confirmadas durante la demostración; las capturas se conservan por separado.

El despliegue se hizo manualmente en EC2. No se ha comprobado una ejecución de CodeBuild ni CodePipeline.

