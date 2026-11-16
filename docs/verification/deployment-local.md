# Verificación local de la ampliación de despliegue

Fecha: 16 de noviembre de 2026.

- Cinco pruebas Python aprobadas: conservación de las etapas y disparadores del pipeline, selección del artefacto de Build, repetición sin duplicar Deploy, rechazo de configuraciones ambiguas y generación del identificador de revisión.
- Plantilla `infra/codedeploy.yml` validada con cfn-lint sin errores.
- Scripts de shell revisados con `bash -n` sin errores de sintaxis.
- Imágenes de ejecución construidas usando el JAR y el frontend compilados disponibles localmente.
- Prueba con proyecto Docker Compose separado, puerto local 19081 y volumen exclusivo de prueba: los tres servicios alcanzaron el estado healthy.
- La API permitió crear un pedido. Después de reemplazar los contenedores del backend y frontend, se recuperó ese mismo pedido.
- `/version.json` devolvió el identificador de revisión esperado.
- Los contenedores y el volumen exclusivos de esta prueba se retiraron al finalizar.

Esta comprobación valida el paquete y la conservación de datos al reemplazar los servicios en el entorno local. No ejecuta los hooks mediante el agente de CodeDeploy, no comprueba el rollback y no acredita un despliegue automático en AWS. Esa comprobación requiere activar la etapa Deploy y verificar una ejecución completa iniciada por un push.

## Adaptación a la acción EC2 mediante SSM

Tras el rechazo de CodeDeploy por la suscripción de la cuenta, se adaptó la configuración a la acción EC2 nativa de CodePipeline. Se aprobaron seis pruebas Python, incluida la conservación de los disparadores y el rechazo de pipelines V1 sin modificarlos. La nueva plantilla `infra/ec2-deploy.yml` pasó cfn-lint. Los scripts de entrada y registro de SSM pasaron la comprobación de sintaxis Bash. No se repitió la prueba de los contenedores porque sus Dockerfiles, compose y lógica de reemplazo no cambiaron. Falta comprobar el agente SSM Online y una ejecución completa de Deploy en AWS; estos resultados no acreditan todavía ese despliegue.
