# Campus · Gestión de pedidos

Este es mi proyecto de gestión de pedidos para la universidad. Permite registrar pedidos, consultar los productos, buscar pedidos y actualizar su estado. La información se guarda en PostgreSQL.

La aplicación tiene una interfaz en React y una API en Java con Spring Boot. Para ejecutarla uso Docker Compose, con un contenedor para cada servicio: frontend, backend y base de datos.

## Qué se puede hacer

- Crear pedidos con los datos del cliente, la dirección y los productos.
- Consultar el detalle y el total de cada pedido.
- Buscar y filtrar los pedidos.
- Actualizar el estado y consultar su historial.
- Ver un resumen de los pedidos registrados.

El total se calcula en el backend usando los precios del catálogo. Los estados avanzan en este orden: pendiente, en proceso, enviado y entregado.

## Tecnologías

- Java 21 y Spring Boot.
- React, TypeScript y Vite.
- PostgreSQL y Flyway para la base de datos y sus migraciones.
- Docker Compose para ejecutar los servicios.
- AWS EC2 para el despliegue de la demostración.

La interfaz usa la API REST. También se incluye una consulta SOAP del estado de un pedido, como parte adicional del proyecto.

## Ejecutar en mi computador

Se necesita Docker Desktop abierto y configurado para contenedores Linux. Los comandos se ejecutan desde la carpeta `order-management`.

En una copia nueva, preparo el archivo de configuración:

```powershell
Copy-Item .env.example .env
notepad .env
```

Cambio `DB_PASSWORD` por una contraseña propia y guardo el archivo. Si ya existe `.env`, conservo el archivo actual.

Después inicio la aplicación:

```powershell
docker compose up -d --build
docker compose ps
```

| Servicio | Dirección local |
|---|---|
| Aplicación | http://localhost:18081 |
| Swagger | http://localhost:18080/swagger-ui/index.html |
| Estado del backend | http://localhost:18080/actuator/health |
| WSDL | http://localhost:18080/ws/orders.wsdl |
| PostgreSQL | localhost:55432 |

Los puertos se pueden cambiar en `.env`. La configuración del repositorio publica los servicios solo en el equipo local.

## Despliegue en AWS

La demostración se desplegó el 15 de noviembre de 2026 en una instancia EC2 llamada `campus-orders`, con Amazon Linux 2023, tipo `t3.small` y un disco gp3 de 30 GiB.

Se instaló Docker, se copiaron los archivos del proyecto y se construyeron las imágenes dentro del servidor. Los tres servicios quedaron en estado `healthy`.

Dirección utilizada en la demostración: http://54.234.225.183

En el servidor, el frontend publica el puerto 80. El backend y PostgreSQL mantienen sus puertos vinculados a `127.0.0.1`. La configuración de puertos del servidor es distinta de la configuración local de este repositorio.

Probé la creación de un pedido, su permanencia después de recargar la página y la validación de campos obligatorios. El despliegue fue manual; CodePipeline y CodeBuild todavía no están conectados al proyecto.

La dirección puede cambiar si se detiene y se vuelve a iniciar la instancia. Cuando se eliminen los recursos, la aplicación dejará de estar disponible en esa dirección.

## Comandos de uso

```bash
# Revisar los servicios
docker compose ps

# Ver los últimos mensajes del backend
docker compose logs --tail 80 backend

# Detener y volver a iniciar los contenedores
docker compose stop
docker compose start
```

Los pedidos se guardan en el volumen `campus-orders_postgres_data`. No uso `docker compose down -v` para detener la aplicación, porque elimina el volumen de la base de datos. Cambiar la contraseña en `.env` no cambia la contraseña de una base que ya está creada.

## Desarrollo sin construir imágenes

Se necesita JDK 21 y Node.js 22.12 o superior. Desde PowerShell, en la raíz del proyecto:

```powershell
docker compose stop backend frontend
docker compose up -d db
. .\scripts\load-env.ps1
Set-Location backend
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=local'
```

En otra terminal, desde la raíz:

```powershell
Set-Location frontend
npm.cmd ci
npm.cmd run dev
```

La interfaz de desarrollo se abre en http://localhost:5173.

## Pruebas

Para el backend, con Java 21 y Docker disponibles:

```powershell
Set-Location backend
.\mvnw.cmd verify
```

Para el frontend, desde su carpeta:

```powershell
npm.cmd ci
npm.cmd run typecheck
npm.cmd run lint
npm.cmd test
npm.cmd run build
```

Las pruebas de navegador necesitan la aplicación local ejecutándose en el puerto 18081:

```powershell
npm.cmd exec playwright install chromium
npm.cmd run test:e2e
```

Los resultados registrados y las instrucciones completas están en [Pruebas](docs/testing.md).

## Carpetas

| Carpeta o archivo | Contenido |
|---|---|
| `backend/` | API, reglas de los pedidos, persistencia y pruebas |
| `frontend/` | Pantallas, formularios y conexión con la API |
| `docs/` | Arquitectura, ejemplos y evidencias |
| `infra/` | Documentación y política de consulta de infraestructura |
| `scripts/` | Script para cargar las variables locales |
| `compose.yaml` | Configuración local de los tres servicios |
| `.env.example` | Ejemplo de las variables necesarias |

## Alcance de la demostración

La aplicación usa datos de prueba. Todavía no tiene inicio de sesión ni control de permisos, y el despliegue usa HTTP. HTTPS, autenticación, respaldos y automatización del despliegue quedan como mejoras pendientes.

El perfil `local` carga un catálogo y un pedido de ejemplo. Los pedidos nuevos se guardan en PostgreSQL; no se almacenan solo en memoria.

La contraseña de la base de datos y la clave SSH no se incluyen en Git. El archivo `.env.example` contiene únicamente valores de ejemplo.

## Documentación adicional

- [Arquitectura](docs/architecture.md)
- [Ejemplos REST y SOAP](docs/api-examples.md)
- [Despliegue y pendientes de AWS](docs/aws-next-steps.md)
- [Lista de evidencias](docs/evidence-checklist.md)
