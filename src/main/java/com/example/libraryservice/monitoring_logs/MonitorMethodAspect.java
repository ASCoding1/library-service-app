package com.example.libraryservice.monitoring_logs;

import com.example.libraryservice.book.BookService;
import com.example.libraryservice.rabbit.service.RabbitMqService;
import lombok.AllArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Aspect
@Component
@AllArgsConstructor
public class MonitorMethodAspect {
    private final SecurityUtils securityUtils;
    private final RabbitMqService rabbitMqService;
    private final static String queueName = "logging-queue";
    private final Logger logger = LoggerFactory.getLogger(BookService.class);

    @Around("@annotation(MonitorMethod)")
    public Object monitorMethodExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        Optional<String> userEmail = securityUtils.getLoggedInUserId();

        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        long startTime = System.currentTimeMillis();

        Object result = joinPoint.proceed();

        long executionTime = System.currentTimeMillis() - startTime;

        LocalDateTime localDateTime = LocalDateTime.now();

        saveLogToSystem(userEmail.get(), className, methodName, executionTime, localDateTime);

        return result;
    }


    //wysylanie logow do innego mikroserwisu ktory zapisuje je w bazie
    private void saveLogToSystem(String userEmail, String className, String methodName, long executionTime, LocalDateTime localDateTime) {
        try {
            LogMessage logMessage = new LogMessage(userEmail, className, methodName, executionTime, localDateTime);
            rabbitMqService.send(logMessage, queueName);

            logger.info("Log saved to RabbitMQ: User: {}, Class: {}, Method: {}, Execution Time: {} ms",
                    userEmail, className, methodName, executionTime);
        } catch (Exception e) {
            logger.error("An error occurred while saving log: {}", e.getMessage(), e);
        }
    }
}
