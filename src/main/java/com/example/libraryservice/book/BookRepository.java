package com.example.libraryservice.book;

import com.example.libraryservice.book.model.Book;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Book> findWithLockingById(int id);

    @Modifying
    @Query("UPDATE Book b SET b.blocked = true WHERE b.id = :id AND b.blocked = false")
    int blockBookById(int id);

    @Query("SELECT b FROM Book b WHERE b.registerTime >= :cutoffTime")
    Slice<Book> findBooksAddedWithinLast24Hours(LocalDateTime cutoffTime, Pageable pageable);
}



