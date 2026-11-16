# Ejemplos de API

La referencia ejecutable es Swagger: http://localhost:18080/swagger-ui/index.html. Base REST: `http://localhost:18080/api/v1`.

## Crear, consultar y actualizar (PowerShell)

```powershell
$base = 'http://localhost:18080/api/v1'
$catalog = Invoke-RestMethod "$base/products?size=50"
$body = @{
  customerName = 'Cliente de ejemplo'
  email = 'cliente@example.com'
  phone = '3001234567'
  address = @{
    street = 'Calle 10 # 20-30'; city = 'Bogotá'; region = 'Bogotá D.C.'
    postalCode = '110111'; country = 'CO'
  }
  items = @(@{ productId = $catalog.content[0].id; quantity = 2 })
} | ConvertTo-Json -Depth 5
$response = Invoke-WebRequest "$base/orders" -Method Post -ContentType 'application/json; charset=utf-8' -Body ([Text.Encoding]::UTF8.GetBytes($body))
$response.StatusCode # 201
$response.Headers.Location
$order = $response.Content | ConvertFrom-Json
Invoke-RestMethod "$base/orders/$($order.id)"
Invoke-RestMethod "$base/orders/$($order.id)/history"
$change = @{ status = 'IN_PROGRESS'; version = $order.version } | ConvertTo-Json
$order = Invoke-RestMethod "$base/orders/$($order.id)/status" -Method Patch -ContentType 'application/json' -Body $change
Invoke-RestMethod "$base/orders?status=IN_PROGRESS&search=cliente&sort=total&direction=desc&page=0&size=10"
Invoke-RestMethod "$base/orders?from=2026-11-01T00:00:00Z&to=2026-12-01T00:00:00Z"
Invoke-RestMethod "$base/orders/stats"
```

`from <= createdAt < to`. Para un día local, convertir el inicio de ese día y el inicio del siguiente a UTC. El frontend lo hace antes de consultar. No se filtra por updatedAt. `search` es subcadena literal, sin distinguir mayúsculas, hasta 120 caracteres. Los filtros se combinan con AND; número/nombre/correo se buscan con OR. No se permite ordenar por un campo arbitrario.

## Contratos

| Método y ruta | Entrada / resultado |
|---|---|
| POST /orders | Cliente, dirección, 1–50 ítems únicos; 201 + Location + OrderView |
| GET /orders | page=0, size=10 (máx. 100), status, from, to, search, sort, direction; página de resúmenes |
| GET /orders/stats | Conteos globales por estado y total |
| GET /orders/{id} | Detalle con cliente, dirección, ítems históricos, historial y versión |
| PATCH /orders/{id}/status | `{ "status": "IN_PROGRESS", "version": 0 }`; detalle actualizado |
| GET /orders/{id}/history | Lista de cambios, inicial con previousStatus=null |
| GET /products | Catálogo activo paginado: page=0, size=50, máx. 100 |
| GET /products/{id} | Producto, incluido su indicador active |

Página: `{content: [...], number: 0, size: 10, totalElements: 42, totalPages: 5}`. El detalle tiene `id`, `number`, `customerName`, `email`, `phone`, `address`, `status`, `currency`, `total`, `createdAt`, `updatedAt`, `version`, `items` e `history`. En ítems: `productId`, `sku`, `name`, `unitPrice`, `quantity`, `subtotal`.

Longitudes: nombre 120, correo 254, teléfono 30, dirección 200, ciudad/departamento 100, código postal 20, país exactamente 2. Campos desconocidos en JSON se rechazan. Precios/total no son parte del DTO de entrada.

## Errores

- 400: validación, JSON/UUID/fecha/estado inválido, producto repetido/inactivo, cantidad fuera de límites o filtro no permitido.
- 404: pedido o producto inexistente.
- 409: transición inválida, versión obsoleta o carrera de actualizaciones.
- 500: fallo inesperado con mensaje genérico.

Ejemplo ilustrativo (el traceId real se genera por solicitud):

```json
{
  "type": "about:blank",
  "title": "Datos inválidos",
  "status": 400,
  "detail": "Revisa los datos de la solicitud.",
  "instance": "/api/v1/orders",
  "traceId": "af23d216-38ac-4b21-b03e-a4cbec7a4eda",
  "errors": { "email": "Correo inválido." }
}
```

El mapa errors está vacío para errores de negocio generales; no se expone stack trace. Un conflicto requiere recargar el pedido y decidir de nuevo con su versión vigente. Si se repite el estado vigente con versión vigente, no cambian versión, fecha ni historial. Este comportamiento no convierte POST en idempotente: la interfaz evita clics duplicados, pero un cliente externo debe evitar reintentar POST ciegamente tras una interrupción de red.

## SOAP GetOrderStatus

WSDL: http://localhost:18080/ws/orders.wsdl. Namespace: `urn:uni:orders:v1`. SOAP 1.1, POST `/ws`, Content-Type `text/xml`.

Solicitud con el identificador de demostración:

```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/" xmlns:o="urn:uni:orders:v1">
  <soap:Header/>
  <soap:Body>
    <o:GetOrderStatusRequest>
      <o:orderId>aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa</o:orderId>
    </o:GetOrderStatusRequest>
  </soap:Body>
</soap:Envelope>
```

Respuesta ilustrativa (la fecha y estado dependen del pedido real):

```xml
<SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">
  <SOAP-ENV:Header/>
  <SOAP-ENV:Body>
    <GetOrderStatusResponse xmlns="urn:uni:orders:v1">
      <number>PED-DEMO-001</number>
      <status>PENDING</status>
      <updatedAt>2026-11-14T19:00:00Z</updatedAt>
    </GetOrderStatusResponse>
  </SOAP-ENV:Body>
</SOAP-ENV:Envelope>
```

```powershell
$xml = Get-Content -Raw .\docs\soap-request.xml
Invoke-WebRequest 'http://localhost:18080/ws' -Method Post -ContentType 'text/xml; charset=utf-8' -Body $xml
```

UUID inválido o pedido inexistente devuelve `soap:Client` Fault; XML peligroso/malformado se rechaza con Fault y HTTP 400 por el filtro previo. La validación XSD y los errores del endpoint SOAP siguen el transporte estándar Spring-WS (normalmente HTTP 500 con Fault). REST conserva sus propios códigos 400/404/409. SOAP no reemplaza ni es utilizado por el frontend REST.
