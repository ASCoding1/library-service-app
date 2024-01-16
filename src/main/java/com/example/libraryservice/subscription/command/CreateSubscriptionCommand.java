package com.example.libraryservice.subscription.command;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateSubscriptionCommand {
    @NotBlank
    private String categoryName;
    private LocalDate creationDate;
    private String userEmail;
    private boolean active;
}
