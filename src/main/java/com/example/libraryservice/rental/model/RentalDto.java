package com.example.libraryservice.rental.model;

import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RentalDto {
    private Integer id;
    private LocalDate fromDate;
    private LocalDate toDate;
    private boolean returned;
}
