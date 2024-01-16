package com.example.libraryservice.rental.command;

import com.example.libraryservice.common.validator.FromDateBeforeToDate;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import lombok.Data;

import java.time.LocalDate;

@Data
@FromDateBeforeToDate
public class CreateRentalCommand {

    @Future
    private LocalDate fromDate;
    @Future
    private LocalDate toDate;
    private int bookId;
    @Email
    private String userEmail;
    private boolean returned;
}
