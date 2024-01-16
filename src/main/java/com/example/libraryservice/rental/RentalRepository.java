package com.example.libraryservice.rental;

import com.example.libraryservice.book.model.Book;
import com.example.libraryservice.rental.model.Rental;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;


public interface RentalRepository extends JpaRepository<Rental, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Rental> findWithLockingById(int id);

    @Query("SELECT COUNT(r) > 0 FROM Rental r WHERE r.book = :book AND " +
            "(:fromDate BETWEEN r.fromDate AND r.toDate OR :toDate BETWEEN r.fromDate AND r.toDate)")
    boolean findOverlappingRentals(@Param("book") Book book,
                                   @Param("fromDate") LocalDate fromDate,
                                   @Param("toDate") LocalDate toDate);
}
