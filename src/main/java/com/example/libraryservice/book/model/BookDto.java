package com.example.libraryservice.book.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookDto {
    private Integer id;
    private String title;
    private String author;
    private String category;
    private boolean blocked;
    private LocalDateTime registerTime;
}
