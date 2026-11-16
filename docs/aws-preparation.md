# Preparación de CodeBuild

El despliegue actual se hizo manualmente en EC2. La automatización con CodeBuild y CodePipeline sigue pendiente.

`buildspec.verify.yml` prepara la verificación del proyecto con Java 21, Node 22, Maven, PostgreSQL Testcontainers, comprobación de tipos, lint, pruebas del frontend y compilación. Define reportes y artefactos JAR y dist.

Para utilizarlo, hace falta crear el proyecto de CodeBuild, seleccionar este archivo y configurar una imagen Linux con los runtimes necesarios. Testcontainers necesita Docker disponible y el modo privilegiado del entorno de compilación. También deben configurarse los permisos de logs, reportes y artefactos.

El archivo no realiza el despliegue en EC2. La fase de despliegue y la conexión con el repositorio deben configurarse y probarse por separado.

Referencias:
- https://docs.aws.amazon.com/codebuild/latest/userguide/available-runtimes.html
- https://docs.aws.amazon.com/codebuild/latest/userguide/build-spec-ref.html
- https://docs.aws.amazon.com/codebuild/latest/userguide/sample-docker-custom-image.html
