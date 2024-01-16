package com.example.libraryservice.common.exception.model;

public class RabbitMessageSendingException extends RuntimeException{
    public RabbitMessageSendingException(String message) {
        super(message);
    }
}
