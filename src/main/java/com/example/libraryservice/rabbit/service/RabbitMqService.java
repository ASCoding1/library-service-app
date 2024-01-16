package com.example.libraryservice.rabbit.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
@Service
public class RabbitMqService {
    private final RabbitTemplate rabbitTemplate;
    private final Logger logger = LoggerFactory.getLogger(RabbitMqService.class);

    @Autowired
    public RabbitMqService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void send(Object message, String queueName) {
            try {
                rabbitTemplate.convertAndSend(queueName, message);
                logger.info("Message sent to RabbitMQ successfully");
            } catch (Exception e) {
                logger.error("Error sending message to RabbitMQ: {}", e.getMessage(), e);
            }
    }
}
