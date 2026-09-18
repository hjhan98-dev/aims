package com.aims.demo.logging;

import com.aims.demo.scenario.SimulatedFailureException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SimulatedFailureException.class)
    public ResponseEntity<Map<String, String>> handleSimulatedFailure(SimulatedFailureException ex,
                                                                        HttpServletRequest request) {
        request.setAttribute(RequestLoggingFilter.ERROR_TYPE_ATTRIBUTE, ex.getClass().getSimpleName());
        request.setAttribute(RequestLoggingFilter.ERROR_MESSAGE_ATTRIBUTE, ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal Server Error"));
    }
}
