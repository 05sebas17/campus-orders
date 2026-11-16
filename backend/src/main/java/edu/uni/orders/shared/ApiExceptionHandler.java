package edu.uni.orders.shared;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.*;
import org.slf4j.MDC;
import org.springframework.dao.*;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(basePackages = {"edu.uni.orders.orders.api", "edu.uni.orders.products.api"})
public class ApiExceptionHandler {
  @ExceptionHandler(Exception.class)
  public ProblemDetail handle(Exception ex, HttpServletRequest request) {
    int status = 500;
    String detail = "No se pudo completar la operación.";
    if (ex instanceof NotFoundException) {
      status = 404;
      detail = ex.getMessage();
    } else if (ex instanceof ConflictException) {
      status = 409;
      detail = ex.getMessage();
    } else if (ex instanceof OptimisticLockingFailureException
        || ex instanceof DataIntegrityViolationException) {
      status = 409;
      detail = "El pedido cambió. Actualiza los datos e inténtalo de nuevo.";
    } else if (ex instanceof IllegalArgumentException) {
      status = 400;
      detail = ex.getMessage();
    } else if (ex instanceof MethodArgumentNotValidException
        || ex instanceof HttpMessageNotReadableException
        || ex instanceof MethodArgumentTypeMismatchException) {
      status = 400;
      detail = "Revisa los datos de la solicitud.";
    }
    var problem = ProblemDetail.forStatusAndDetail(HttpStatus.valueOf(status), detail);
    problem.setTitle(
        status == 400
            ? "Datos inválidos"
            : status == 404 ? "No encontrado" : status == 409 ? "Conflicto" : "Error interno");
    problem.setInstance(URI.create(request.getRequestURI()));
    problem.setProperty("traceId", MDC.get("traceId"));
    var fields = new LinkedHashMap<String, String>();
    if (ex instanceof MethodArgumentNotValidException invalid)
      invalid
          .getBindingResult()
          .getFieldErrors()
          .forEach(e -> fields.putIfAbsent(e.getField(), e.getDefaultMessage()));
    problem.setProperty("errors", fields);
    return problem;
  }
}
