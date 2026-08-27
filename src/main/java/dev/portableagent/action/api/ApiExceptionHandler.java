package dev.portableagent.action.api;

import dev.portableagent.action.application.ActionNotFoundException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ActionNotFoundException.class)
    ProblemDetail notFound(ActionNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "Action not found", exception.getMessage(), "action-not-found");
    }

    @ExceptionHandler({IllegalStateException.class, IllegalArgumentException.class})
    ProblemDetail conflict(RuntimeException exception) {
        return problem(HttpStatus.CONFLICT, "Action cannot be changed", exception.getMessage(), "action-conflict");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validation(MethodArgumentNotValidException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Validation failed", "Request does not match the contract", "validation-failed");
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail, String type) {
        var problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("https://portable-agent.dev/problems/" + type));
        return problem;
    }
}
