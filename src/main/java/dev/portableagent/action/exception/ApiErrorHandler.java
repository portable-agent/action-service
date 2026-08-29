package dev.portableagent.action.exception;

import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiErrorHandler {
  @ExceptionHandler(ActionNotFound.class)
  ProblemDetail notFound(ActionNotFound error) {
    return problem(
        HttpStatus.NOT_FOUND, "Action not found", error.getMessage(), "action-not-found");
  }

  @ExceptionHandler({IllegalStateException.class, IllegalArgumentException.class})
  ProblemDetail conflict(RuntimeException error) {
    return problem(
        HttpStatus.CONFLICT, "Action cannot be changed", error.getMessage(), "action-conflict");
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ProblemDetail validation() {
    return problem(
        HttpStatus.BAD_REQUEST,
        "Validation failed",
        "Request does not match the contract",
        "validation-failed");
  }

  private ProblemDetail problem(HttpStatus status, String title, String detail, String type) {
    var problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setTitle(title);
    problem.setType(URI.create("https://portable-agent.dev/problems/" + type));
    return problem;
  }
}
