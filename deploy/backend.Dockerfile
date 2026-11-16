FROM eclipse-temurin:21.0.9_10-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/* && groupadd --system app && useradd --system --gid app app
WORKDIR /app
COPY --chown=app:app backend/target/order-management-1.0.0.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
