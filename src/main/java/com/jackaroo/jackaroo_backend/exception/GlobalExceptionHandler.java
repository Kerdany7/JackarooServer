package com.jackaroo.jackaroo_backend.exception;

import com.jackaroo.jackaroo_backend.dto.ErrorResponse;
import exception.GameException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(GameException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleGameException(GameException ex) {
        return new ErrorResponse(ex.getMessage(), ex.getClass().getSimpleName());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleIllegalState(IllegalStateException ex) {
        return new ErrorResponse(ex.getMessage(), "IllegalState");
    }

    @ExceptionHandler(IndexOutOfBoundsException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleOutOfBounds(IndexOutOfBoundsException ex) {
        return new ErrorResponse(ex.getMessage(), "IndexOutOfBounds");
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(NoSuchElementException ex) {
        return new ErrorResponse(ex.getMessage(), "NotFound");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGeneral(Exception ex) {
        return new ErrorResponse(ex.getMessage(), "ServerError");
    }
}
