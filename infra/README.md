# Infraestructura

La demostración funciona en una instancia EC2 con Docker Compose. El despliegue se hizo manualmente por SSH.

La carpeta `iam/` contiene una política de consulta de infraestructura. No crea la instancia ni despliega la aplicación.

Los detalles del despliegue y lo que falta por hacer están en [AWS](../docs/aws-next-steps.md).

`ec2-deploy.yml` define el perfil de Systems Manager y los permisos de la acción EC2 de CodePipeline para actualizar la instancia existente. No administra ni recrea EC2, su disco o su red. El script `scripts/configure-cd.py` conecta estos recursos con el pipeline y la instancia. Las instrucciones de activación y verificación están en [Despliegue automático](../docs/automatic-deployment.md).
