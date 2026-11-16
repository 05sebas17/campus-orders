# Despliegue en AWS y próximos pasos

## Despliegue realizado

El 15 de noviembre de 2026 se desplegó la aplicación en EC2, en la región `us-east-1`.

- Instancia: `campus-orders`, tipo `t3.small`.
- Sistema operativo: Amazon Linux 2023.
- Almacenamiento: 30 GiB gp3.
- Acceso al servidor: SSH con la clave privada del proyecto y el origen restringido a la IP del equipo.
- Servicios: React servido por Nginx, Spring Boot y PostgreSQL, ejecutados con Docker Compose.
- Acceso web utilizado: http://54.234.225.183, por el puerto 80.
- Backend y PostgreSQL: puertos publicados solo en `127.0.0.1`.

Se copiaron los archivos con SCP y se construyeron las dos imágenes en EC2. La contraseña de PostgreSQL se generó en el servidor y se guardó en `.env`.

## Comprobaciones

Los tres contenedores aparecen en estado `healthy`. La ruta `/health` del frontend devuelve `ok`. Se comprobó desde el navegador la creación de pedidos, su consulta después de recargar y la validación de campos obligatorios.

Estas pruebas corresponden al despliegue manual. No representan una ejecución de CodePipeline o CodeBuild.

## Integración continua

El repositorio está publicado en GitHub como `05sebas17/campus-orders`. La canalización `campus-orders-ci` ejecutó correctamente el proyecto `campus-orders-build`, con informes de pruebas y artefactos en S3. Esa ejecución tenía las etapas Source y Build; no actualizó EC2 automáticamente.

La ampliación con CodeDeploy está preparada en el repositorio. Para crear sus recursos, activar Deploy y comprobar un despliegue disparado por un push, seguir [Despliegue automático](automatic-deployment.md).

## Pendientes

- Activar y comprobar la nueva etapa Deploy en AWS, conservando los pedidos existentes.
- Incorporar HTTPS y autenticación antes de utilizar datos reales.
- Definir respaldos y comprobar la recuperación de los datos.
- Configurar el arranque automático de los contenedores después de reiniciar el servidor; habilitar Docker no basta para garantizarlo.
- Revisar los recursos al terminar la evaluación y eliminar únicamente los que ya no se necesiten.

El presupuesto no se modificó durante este despliegue. Los recursos activos siguen consumiendo saldo. La URL deja de funcionar al eliminar la instancia y puede cambiar después de detenerla y volverla a iniciar.
