# Evidencias del taller y guion

## Matriz de requisitos académicos

| Requisito del taller | Implementación / estado |
|---|---|
| API REST para gestionar pedidos | Implementada: OrderController, /api/v1/orders |
| Cliente, productos, cantidades y envío | DTO validado, Order + OrderItem, formulario React |
| Consultas con filtros y ordenamiento | OrderService, paginación SQL, fechas, búsqueda, estados, sort permitido |
| Actualización de estados | Máquina de estados secuencial, historial y @Version |
| Versionado con GitHub/CodeCommit | Repositorio local preparado; conexión/publicación remota pendiente |
| CodePipeline y CodeBuild | Pendiente fase AWS; comandos y artefactos documentados |
| buildspec.yml | buildspec.verify.yml preparado; ejecución en CodeBuild pendiente |
| Construcción y pruebas unitarias | Maven Wrapper, JUnit/Mockito, Vitest, build Vite; ver testing.md |
| Generación de artefactos | JAR y dist, Dockerfiles multietapa |
| Despliegue AWS | Despliegue manual en EC2 comprobado el 15 de noviembre de 2026 |
| Informe PDF con evidencias | Evidencias recopiladas; organización del informe pendiente de revisión |
| Video máximo 10 minutos | Guion propuesto debajo; grabación pendiente |
| Entrega continua entre entornos (opcional) | Pendiente si se decide incluirla |
| Diagrama de arquitectura (opcional) | Incluido en architecture.md |
| SOAP y SOLID | Ampliación solicitada: endpoint con XSD/WSDL y diseño modular; SOAP no es requisito del taller |

## Capturas que debe tomar el estudiante

- [ ] VS Code con estructura, Maven Wrapper y módulos visibles.
- [x] `docker compose ps` con tres servicios saludables en EC2.
- [ ] Resumen con conteos de base de datos y pedidos recientes.
- [x] Crear un pedido y comprobar la validación de campos obligatorios (confirmado durante la demostración).
- [ ] Listado con búsqueda, filtro de estado, fechas, orden y paginación.
- [ ] Detalle: precio histórico, cliente, dirección y avance confirmado.
- [ ] Respuesta 409 ante salto de estado o versión obsoleta.
- [ ] Swagger con petición POST 201 y cabecera Location.
- [ ] WSDL y respuesta SOAP GetOrderStatus, más un Fault de pedido inexistente.
- [ ] Pruebas unitarias, integración PostgreSQL y E2E terminadas correctamente.
- [x] Pedido conservado después de recargar la página en AWS (confirmado durante la demostración).
- [ ] Persistencia después de reiniciar los contenedores en AWS; los resultados del reinicio local están en testing.md.
- [ ] Repositorio remoto con commits, cuando se publique.
- [x] Despliegue manual en EC2 y acceso desde el navegador.
- [ ] Ejecución de CodeBuild y CodePipeline.

Usar exclusivamente datos de demostración en capturas. Las capturas locales no acreditan AWS. No sustituir evidencias por resultados simulados.

## Guion de hasta 10 minutos

| Tiempo | Contenido |
|---|---|
| 0:00–0:50 | Objetivo, arquitectura y separación REST/SOAP/SOLID |
| 0:50–2:50 | Crear pedido, validar formulario y explicar total del servidor |
| 2:50–4:00 | Buscar, filtrar, ordenar y consultar detalle |
| 4:00–5:10 | Avanzar estado, mostrar historial y conflicto |
| 5:10–6:10 | Swagger y consulta SOAP |
| 6:10–7:20 | Pruebas, migraciones y persistencia tras reinicio |
| 7:20–9:20 | Despliegue en EC2 y estado de los contenedores; indicar que el pipeline sigue pendiente |
| 9:20–9:50 | Resultados comprobados y límites |

Esta lista reúne las evidencias disponibles y las comprobaciones que faltan para organizar la entrega. El estado final del PDF y del video debe revisarse antes de entregar.

