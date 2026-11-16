# Actualización automática de EC2

## Flujo

GitHub (main) → CodePipeline → CodeBuild (pruebas y artefactos) → CodeDeploy → EC2.

CodeBuild ejecuta las pruebas y compila el JAR y el frontend. El ZIP incluye `appspec.yml`, los scripts de despliegue y Dockerfiles que utilizan esos artefactos ya compilados. EC2 no vuelve a compilar Java ni TypeScript: construye las imágenes de ejecución con el contenido probado.

CodeDeploy comprueba el servidor, guarda una copia local de PostgreSQL, construye las imágenes y actualiza únicamente frontend y backend. Conserva el volumen externo `campus-orders_postgres_data` y `/home/ec2-user/campus-orders/.env`. La comprobación final consulta la web, la API y `/version.json` para verificar el commit publicado.

## Infraestructura versionada

`infra/codedeploy.yml` crea mediante CloudFormation los roles, el perfil de la instancia, la aplicación de CodeDeploy, el grupo de despliegue y los permisos del pipeline. El script `scripts/configure-cd.py` asocia el perfil a la instancia existente, la etiqueta y añade la etapa Deploy al pipeline existente. No crea ni importa en CloudFormation la instancia, el disco, la red o el pipeline previos. Sus configuraciones permanecen fuera de ese stack.

## Activación

Primero revisar y publicar estos archivos en GitHub. La publicación inicial ejecutará el CI existente, todavía sin despliegue. Esperar a que termine correctamente.

En AWS CloudShell, con la cuenta del proyecto y región Norte de Virginia:

```bash
git clone https://github.com/05sebas17/campus-orders.git campus-orders-cd
cd campus-orders-cd
python3 scripts/configure-cd.py plan
python3 scripts/configure-cd.py prepare
```

El modo `plan` consulta los destinos y valida la plantilla. `prepare` crea el stack, asocia su perfil IAM a la instancia y añade la etiqueta de despliegue. Si encuentra un perfil ajeno, se detiene en lugar de reemplazarlo. Se necesita una sesión con permisos de CloudFormation, IAM, EC2, CodeDeploy y CodePipeline; el usuario local de despliegue de documentos no dispone de esos permisos.

Desde PowerShell local, en la raíz del proyecto, copiar el instalador a EC2:

```powershell
scp -i .\pedidos-aws.pem .\deploy\install-agent.sh ec2-user@ec2-54-234-225-183.compute-1.amazonaws.com:~/campus-orders-install-agent.sh
ssh -i .\pedidos-aws.pem ec2-user@ec2-54-234-225-183.compute-1.amazonaws.com
```

Dentro de EC2, ejecutar una vez:

```bash
sudo bash ~/campus-orders-install-agent.sh
```

Cuando el agente esté activo y CodeBuild haya terminado, regresar a CloudShell:

```bash
python3 scripts/configure-cd.py enable
```

El modo `enable` añade Deploy sin ejecutar por sí solo la canalización. Conserva una copia JSON de la configuración anterior en el directorio personal de CloudShell. Detiene la operación si encuentra una ejecución activa o una etapa Deploy diferente.

## Prueba de extremo a extremo

Desde el repositorio local, crear un commit nuevo y subirlo a main. Puede ser una actualización de esta documentación. No pulsar manualmente “Liberar cambio” para esta evidencia: comprobar que el push dispara la ejecución.

Conservar capturas del commit, Source/Build/Deploy correctos, los hooks de CodeDeploy y el resultado de abrir `http://IP_PUBLICA/version.json`. El campo `commit` debe coincidir con `git rev-parse HEAD`. Comprobar también que siguen existiendo los pedidos previos y que se puede crear uno nuevo.

La preparación de archivos, la validación local y un stack creado no sustituyen esa prueba. Hasta completar este recorrido, el despliegue automático debe figurar como pendiente de validar.

## Operación y recuperación

- Las revisiones se guardan en `/opt/campus-orders/releases/`. El enlace `/opt/campus-orders/current` identifica la revisión activa.
- Las copias SQL comprimidas se guardan en `/var/backups/campus-orders/`, accesibles solo por root. Son locales: no protegen contra la pérdida del disco.
- Los contenedores reciben la política `unless-stopped`; no es necesario reiniciar la instancia para aplicarla. El comportamiento tras reiniciar EC2 requiere una comprobación independiente.
- Puede haber una interrupción breve mientras se reemplaza el backend. No es un despliegue de alta disponibilidad.
- Si falla la validación inmediata, el script intenta restaurar las imágenes anteriores. CodeDeploy también habilita rollback a una revisión previa correcta. La recuperación no revierte migraciones de esquema: los cambios deben ser compatibles con la versión anterior; no se restaura la base automáticamente.
- Para el primer despliegue, el respaldo de la aplicación es la configuración manual original en `/home/ec2-user/campus-orders/`. No borrar ni editar ese archivo durante la activación.
- Tras adoptar CodeDeploy, no ejecutar `docker compose up` con el compose manual original: podría restaurar las imágenes antiguas. Para consultar el estado se puede usar `docker ps`.
- Revisar periódicamente el espacio de imágenes, revisiones y copias. No se borran automáticamente datos ni backups.
- Para retirar el stack, primero quitar Deploy del pipeline y desvincular el perfil de la instancia. Eliminar el stack no elimina EC2 ni el volumen de PostgreSQL.

## Documentación oficial

- https://docs.aws.amazon.com/codedeploy/latest/userguide/reference-appspec-file-structure-hooks.html
- https://docs.aws.amazon.com/codedeploy/latest/userguide/codedeploy-agent-operations-install-linux.html
- https://docs.aws.amazon.com/AWSCloudFormation/latest/TemplateReference/aws-resource-codedeploy-deploymentgroup.html
