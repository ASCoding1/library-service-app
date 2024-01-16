package com.example.libraryservice.book.model;

import com.example.libraryservice.rental.model.Rental;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@EntityListeners(AuditingEntityListener.class)
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String title;
    private String author;
    private String category;
    private boolean blocked;
    @OneToMany(mappedBy = "book")
    private Set<Rental> rentals = new HashSet<>();
    @CreatedDate
    private LocalDateTime registerTime;

}

