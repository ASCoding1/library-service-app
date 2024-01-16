package com.example.libraryservice.book.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateBookCommand {
    @NotBlank(message = "value is blank")
    @Pattern(regexp = "\\b([A-ZÀ-ÿ][-,a-z. ']+[ ]*)+", message = "Author has to match specific pattern, example = 'Harry'")
    private String title;

    @NotBlank(message = "value is blank")
    @Pattern(regexp = "\\b([A-ZÀ-ÿ][-,a-z. ']+[ ]*)+", message = "Author has to match specific pattern, example = 'Adam Kowalski'")
    private String author;

    @NotBlank
    @NotEmpty
    private String category;

    private boolean blocked;

    private LocalDateTime registerTime;
}
