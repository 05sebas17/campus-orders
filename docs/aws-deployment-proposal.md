# Decisión de despliegue

Para la demostración se utilizó una sola instancia EC2 con Docker Compose. Así se ejecutan el frontend, el backend y PostgreSQL en el mismo servidor.

La instancia seleccionada fue `t3.small`, con Amazon Linux 2023 y 30 GiB de almacenamiento gp3. El acceso administrativo se hizo por SSH y las imágenes se construyeron en el servidor.

Se eligió esta configuración para mantener sencillo el despliegue del proyecto. No se utilizaron RDS, un balanceador ni repositorios ECR nuevos para esta aplicación.

La base de datos utiliza un volumen Docker. Esto conserva los datos cuando se reemplazan los contenedores, pero no sustituye una copia de seguridad. Todos los servicios dependen de la misma instancia, por lo que no hay alta disponibilidad.

La demostración está publicada por HTTP y no incluye autenticación. HTTPS, respaldos y despliegue automatizado quedan pendientes.

El consumo depende del tiempo de uso de EC2, el disco, la dirección pública y los demás recursos de la cuenta. Los créditos disponibles no convierten el despliegue en un servicio sin costo. Al terminar la evaluación se revisarán los recursos antes de eliminarlos.

El estado detallado está en [Despliegue en AWS](aws-next-steps.md).
