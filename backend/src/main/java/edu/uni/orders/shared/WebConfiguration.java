package edu.uni.orders.shared;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfiguration implements WebMvcConfigurer {
  private final String[] origins;

  public WebConfiguration(@Value("${app.cors-origins}") String[] origins) {
    this.origins = origins;
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry
        .addMapping("/api/**")
        .allowedOrigins(origins)
        .allowedMethods("GET", "POST", "PATCH", "OPTIONS")
        .allowedHeaders("Content-Type", "X-Correlation-ID")
        .exposedHeaders("Location", "X-Correlation-ID");
  }

  @Bean
  OpenAPI openAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Gestión de pedidos · API REST")
                .version("v1")
                .description(
                    "Demostración local sin autenticación. Errores application/problem+json con errors por campo y traceId. SOAP es una ampliación educativa."));
  }

  @Bean
  OpenApiCustomizer errorResponses() {
    return api -> {
      var problem =
          new io.swagger.v3.oas.models.media.ObjectSchema()
              .addProperty("type", new io.swagger.v3.oas.models.media.StringSchema())
              .addProperty("title", new io.swagger.v3.oas.models.media.StringSchema())
              .addProperty("status", new io.swagger.v3.oas.models.media.IntegerSchema())
              .addProperty("detail", new io.swagger.v3.oas.models.media.StringSchema())
              .addProperty("instance", new io.swagger.v3.oas.models.media.StringSchema())
              .addProperty("traceId", new io.swagger.v3.oas.models.media.StringSchema())
              .addProperty(
                  "errors",
                  new io.swagger.v3.oas.models.media.MapSchema()
                      .additionalProperties(new io.swagger.v3.oas.models.media.StringSchema()));
      api.getComponents().addSchemas("ApiProblem", problem);
      api.getPaths()
          .values()
          .forEach(
              path ->
                  path.readOperations()
                      .forEach(
                          op -> {
                            for (String code : new String[] {"400", "404", "409"}) {
                              var content =
                                  new io.swagger.v3.oas.models.media.Content()
                                      .addMediaType(
                                          "application/problem+json",
                                          new io.swagger.v3.oas.models.media.MediaType()
                                              .schema(
                                                  new io.swagger.v3.oas.models.media.Schema<>()
                                                      .$ref("#/components/schemas/ApiProblem"))
                                              .example(
                                                  java.util.Map.of(
                                                      "title",
                                                      "Error de solicitud",
                                                      "status",
                                                      Integer.parseInt(code),
                                                      "detail",
                                                      "Revisa los datos o el estado del pedido.",
                                                      "traceId",
                                                      "demo-trace-id",
                                                      "errors",
                                                      java.util.Map.of())));
                              op.getResponses()
                                  .addApiResponse(
                                      code,
                                      new ApiResponse()
                                          .description(
                                              "ProblemDetail con identificador de seguimiento y errores por campo.")
                                          .content(content));
                            }
                          }));
      var create = api.getPaths().get("/api/v1/orders").getPost();
      create
          .getRequestBody()
          .getContent()
          .get("application/json")
          .setExample(
              java.util.Map.of(
                  "customerName",
                  "Cliente de ejemplo",
                  "email",
                  "cliente@example.com",
                  "address",
                  java.util.Map.of(
                      "street",
                      "Calle 10",
                      "city",
                      "Bogotá",
                      "region",
                      "Bogotá D.C.",
                      "country",
                      "CO"),
                  "items",
                  java.util.List.of(
                      java.util.Map.of(
                          "productId", "11111111-1111-4111-8111-111111111111", "quantity", 2))));
      create
          .getResponses()
          .get("201")
          .addHeaderObject(
              "Location",
              new io.swagger.v3.oas.models.headers.Header()
                  .description("Ruta del pedido creado")
                  .schema(new io.swagger.v3.oas.models.media.StringSchema()));
    };
  }
}
