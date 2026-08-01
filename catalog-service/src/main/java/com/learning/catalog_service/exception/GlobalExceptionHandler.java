package com.learning.catalog_service.exception;

import com.learning.catalog_service.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.nio.file.AccessDeniedException;
import java.util.List;

/**
 * @RestControllerAdvice = @ControllerAdvice + @ResponseBody bundled
 * together. Spring intercepts any exception thrown from WITHIN a
 * controller method's execution (including exceptions thrown deeper —
 * service layer, repository layer — since they propagate UP through the
 * same call stack the controller sits on top of) and routes it here
 * instead of letting it hit Spring Boot's default error page/handler.
 *
 * IMPORTANT SCOPE LIMIT: this only intercepts exceptions from the
 * DispatcherServlet's request-processing pipeline — i.e., controller
 * method execution. It does NOT intercept anything thrown inside the
 * Spring Security filter chain, which runs BEFORE the DispatcherServlet
 * even routes the request to a controller. That's exactly why
 * CustomAuthenticationEntryPoint (Phase C File 3c) had to manually build
 * its own JSON response instead of just throwing and letting this class
 * catch it — a security filter's exceptions never reach here. Good to
 * have this boundary crisply in mind; it's a genuinely common point of
 * confusion.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationError(MethodArgumentNotValidException ex, WebRequest request){
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage()).toList();

        log.warn("Validation failed on {} : {}",request.getDescription(false),errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiResponse.error(HttpStatus.BAD_REQUEST.value(),"Validation failed",errors));

    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateResource(DuplicateResourceException ex){
        log.warn("Duplicate Resource: {}",ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ApiResponse.error(HttpStatus.CONFLICT.value(), ex.getMessage(),null));

    }


    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(ResourceNotFoundException ex){
        log.warn("Resource Not Found: {}",ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiResponse.error(HttpStatus.NOT_FOUND.value(), ex.getMessage(),null));

    }


    /**
     * Thrown by AuthenticationManager.authenticate() in AuthService.login()
     * when the email/password combination doesn't match. Deliberately
     * generic message on the response — "email or password incorrect"
     * rather than confirming/denying which one was wrong, so an attacker
     * can't use error messages to enumerate valid registered emails.
     */
//    @ExceptionHandler(BadCredentialsException.class)
//    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex){
//        log.warn("Authentification failed: Bad Credentials {}",ex.getMessage());
//        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
//                ApiResponse.error(HttpStatus.UNAUTHORIZED.value(), "Email or Password is incorrect",null));
//
//    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex){
        log.warn("Access Denied: {}",ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                ApiResponse.error(HttpStatus.FORBIDDEN.value(), "You do not have permission to perform this task",null));

    }


    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex){
        log.warn("Bad Request: {}",ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiResponse.error(HttpStatus.BAD_REQUEST.value(), ex.getMessage(),null));
    }

    /**
     * The catch-all. Deliberately positioned LAST and matched only when
     * nothing more specific above handles it. Notice the response never
     * includes ex.getMessage() or a stack trace — internal exception
     * detail (a raw NullPointerException message, a SQL error string)
     * is exactly the kind of thing that shouldn't reach a client; it
     * goes to the server LOG (where you, the developer, can see it) and
     * the client gets a safe, generic message instead.
     */
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex, WebRequest request){

        log.error("Unhandled Exception: {}",request.getDescription(false),ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "A Unexpected error occured , please try again later",null));
    }


}
