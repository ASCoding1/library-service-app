package com.example.libraryservice.common.exception;

import com.example.libraryservice.common.exception.model.*;
import jakarta.persistence.EntityNotFoundException;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ExceptionDto handleEntityNotFoundException(EntityNotFoundException exception) {
        return new ExceptionDto(exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ExceptionDto handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        ValidationErrorDto errorDto = new ValidationErrorDto();
        exception.getFieldErrors().forEach(error ->
                errorDto.addViolationInfo(error.getField(), error.getDefaultMessage()));
        return errorDto;
    }

    @ExceptionHandler(RentalException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ExceptionDto handleRentalServiceException(RentalException exception) {
        return new ExceptionDto(exception.getMessage());
    }

    @ExceptionHandler(BookException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ExceptionDto handleBookServiceException(BookException exception) {
        return new ExceptionDto(exception.getMessage());
    }

    @ExceptionHandler(UserException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ExceptionDto handleClientServiceException(UserException exception) {
        return new ExceptionDto(exception.getMessage());
    }

    @ExceptionHandler(UserNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ExceptionDto handleClientNotFoundException(UserNotFoundException exception) {
        return new ExceptionDto(exception.getMessage());
    }

    @ExceptionHandler(BookNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ExceptionDto handleBookNotFoundException(BookNotFoundException exception) {
        return new ExceptionDto(exception.getMessage());
    }

    @ExceptionHandler(RentalNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ExceptionDto handleRentalNotFoundException(RentalNotFoundException exception) {
        return new ExceptionDto(exception.getMessage());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String handleConstraintViolationException() {
        return "Column need to be unique";
    }

    @ExceptionHandler(BookCategoryException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ExceptionDto handleBookCategoryException(BookCategoryException exception) {
        return new ExceptionDto(exception.getMessage());
    }

    @ExceptionHandler(SubscriptionException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ExceptionDto handleSubscriptionException(SubscriptionException exception) {
        return new ExceptionDto(exception.getMessage());
    }

    @ExceptionHandler(RabbitMessageSendingException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ExceptionDto handleRabbitMessageSendingException(RabbitMessageSendingException exception) {
        return new ExceptionDto(exception.getMessage());
    }

    @ExceptionHandler(SubAddingException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ExceptionDto handleSubAddingException(SubAddingException exception) {
        return new ExceptionDto(exception.getMessage());
    }


}
