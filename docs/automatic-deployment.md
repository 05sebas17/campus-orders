# Actualización automática de EC2

## Flujo

GitHub (main) → CodePipeline V2 → CodeBuild → acción EC2 mediante Systems Manager → instancia existente.

CodeBuild prueba y compila el JAR y el frontend. La acción EC2 descarga el artefacto en `/opt/campus-orders/incoming` y ejecuta `deploy/run-ec2.sh` mediante SSM como root. Cada intento recibe un identificador único. El script respalda PostgreSQL, construye las imágenes de ejecución, reemplaza backend y frontend y comprueba la API y el commit de `/version.json`.

Se conservan el volumen `campus-orders_postgres_data` y `/home/ec2-user/campus-orders/.env`. EC2 no recompila Java ni TypeScript.

## Infraestructura

`infra/ec2-deploy.yml` crea el perfil IAM para Systems Manager y lectura del artefacto, y los permisos del pipeline para ejecutar comandos en la instancia seleccionada. `scripts/configure-cd.py` asocia el perfil, etiqueta la instancia y añade Deploy. La instancia, el disco, la red y el pipeline existentes no se recrean ni se importan en CloudFormation.

CodeDeploy rechazó la creación de su aplicación por una restricción de suscripción de la cuenta. Este flujo utiliza la acción nativa EC2. No cambia el plan de la cuenta. La consulta de SSM funcionó; todavía falta comprobar el despliegue completo. EC2, CodePipeline, CodeBuild, S3 y registros siguen sujetos a las condiciones de consumo de la cuenta.

## Activación

Publicar los cambios en GitHub y esperar a que Source y Build terminen correctamente. En CloudShell, dentro de la copia del repositorio:

```bash
git pull --ff-only origin main
python3 scripts/configure-cd.py plan
python3 scripts/configure-cd.py prepare
```

El nuevo stack se llama `campus-orders-ec2-cd`. El stack fallido `campus-orders-cd` no se reutiliza ni elimina automáticamente. `plan` consulta y valida; `prepare` crea los permisos y asocia el perfil y la etiqueta. El script se detiene ante un perfil ajeno o un pipeline que no sea V2; no cambia automáticamente su tipo.

En PowerShell local, desde la raíz del proyecto:

```powershell
scp -i .\pedidos-aws.pem .\deploy\install-ssm.sh ec2-user@ec2-54-234-225-183.compute-1.amazonaws.com:~/campus-orders-install-ssm.sh
ssh -i .\pedidos-aws.pem ec2-user@ec2-54-234-225-183.compute-1.amazonaws.com
```

En EC2:

```bash
sudo bash ~/campus-orders-install-ssm.sh
```

En CloudShell, comprobar que la instancia aparece Online y habilitar Deploy:

```bash
aws ssm describe-instance-information --region us-east-1 --query 'InstanceInformationList[].{Instancia:InstanceId,Estado:PingStatus}' --output table --no-cli-pager
python3 scripts/configure-cd.py enable
```

`enable` exige la instancia Online y ninguna ejecución activa del pipeline. Guarda una copia de su configuración anterior en el directorio personal de CloudShell. Añade Deploy, pero no inicia una ejecución.

## Prueba completa

Crear y subir un nuevo commit a main. Comprobar que el push dispara Source, Build y Deploy. Conservar sus resultados y registros. Abrir `http://IP_PUBLICA/version.json` y comparar `commit` con `git rev-parse HEAD`. Verificar los pedidos previos y crear uno nuevo.

Las pruebas locales no sustituyen este recorrido. El despliegue automático solo queda comprobado después de ejecutarlo en AWS.

## Operación y recuperación

- Las revisiones se guardan en `/opt/campus-orders/releases`; el enlace `current` señala la activa.
- Los backups SQL se guardan en `/var/backups/campus-orders` con acceso restringido. Son locales y no protegen contra pérdida del disco.
- Si falla la actualización o su validación, el script intenta restaurar los contenedores anteriores. El primer intento utiliza como respaldo el compose manual original. No restaura automáticamente PostgreSQL ni revierte migraciones, que deben ser compatibles con la versión previa.
- No se ha configurado rollback automático de la etapa de CodePipeline; la recuperación inmediata pertenece al script.
- Los contenedores usan `unless-stopped`. El reinicio de EC2 requiere una prueba independiente. Puede haber una interrupción breve al reemplazar el backend.
- No ejecutar el compose manual original después de adoptar este flujo: podría restaurar las imágenes antiguas. Consultar el estado con `docker ps`.
- Revisar el espacio ocupado por imágenes, revisiones y backups; no se eliminan automáticamente.
- Para retirar el stack, quitar primero Deploy y desvincular su perfil de EC2. El stack no administra la instancia ni PostgreSQL.

## Referencia

[Acción EC2 de CodePipeline](https://docs.aws.amazon.com/codepipeline/latest/userguide/action-reference-EC2Deploy.html).
