package com.example.playground.kafka.rest.exception.handler;

import com.example.playground.kafka.rest.exception.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;

import static org.springframework.http.HttpStatus.*;

@Order(Integer.MIN_VALUE)
@ControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

    private final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

    // Used by ProblemDetail.type (https://datatracker.ietf.org/doc/html/rfc9457).
    // Recommended (but not required) that it be a URL that provides human-readable documentation about the problem.
    private static final URI PROBLEM_DETAIL_NOT_FOUND = URI.create("https://example.com/docs/errors/not_found");
    private static final URI PROBLEM_DETAIL_FORBIDDEN = URI.create("https://example.com/docs/errors/forbidden");
    private static final URI PROBLEM_DETAIL_INTERNAL = URI.create("https://example.com/docs/errors/server_error");
    private static final URI PROBLEM_DETAIL_NOT_IMPLEMENTED = URI.create("https://example.com/docs/errors/coming_soon");


    @ExceptionHandler({Exception.class})
    protected ProblemDetail handle500(Exception exception, WebRequest webRequest) {
        log.error(exception.getMessage(), exception);
        ProblemDetail problemDetail = generateProblemDetail(INTERNAL_SERVER_ERROR, exception.getMessage(), webRequest);
        log.error("{}", problemDetail);
        return problemDetail;
    }

    @ExceptionHandler({NotImplementedException.class})
    protected ProblemDetail handle501(Exception exception, WebRequest webRequest) {
        log.error(exception.getMessage(), exception);
        ProblemDetail problemDetail = generateProblemDetail(NOT_IMPLEMENTED, exception.getMessage(), webRequest);
        log.error("{}", problemDetail);
        return problemDetail;
    }

    private ProblemDetail generateProblemDetail(HttpStatus status, String detail, WebRequest webRequest) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        switch(status) {
            case NOT_FOUND -> problem.setType(PROBLEM_DETAIL_NOT_FOUND);
            case FORBIDDEN -> problem.setType(PROBLEM_DETAIL_FORBIDDEN);
            case INTERNAL_SERVER_ERROR -> problem.setType(PROBLEM_DETAIL_INTERNAL);
            case NOT_IMPLEMENTED -> problem.setType(PROBLEM_DETAIL_NOT_IMPLEMENTED);
            default -> log.debug("No URI found for {}", status);
        }
        problem.setInstance(URI.create(((ServletWebRequest)webRequest).getRequest().getRequestURI()));
        problem.setProperty("ts_ms", Instant.now());
        problem.setProperty("user", Objects.requireNonNull(webRequest.getUserPrincipal()).getName());
        return problem;
    }
}
