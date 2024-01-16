package com.example.libraryservice.rental;

import com.example.libraryservice.common.exception.model.*;
import com.example.libraryservice.monitoring_logs.MonitorMethod;
import com.example.libraryservice.book.BookRepository;
import com.example.libraryservice.book.model.Book;
import com.example.libraryservice.rental.command.CreateRentalCommand;
import com.example.libraryservice.rental.model.RentalDto;
import com.example.libraryservice.user.UserRepository;
import com.example.libraryservice.user.model.User;
import com.example.libraryservice.rental.model.Rental;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import static com.example.libraryservice.mapper.RentalMapper.MAPPER;

@Service
@RequiredArgsConstructor
public class RentalService {

    private final RentalRepository rentalRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;

    @MonitorMethod
    @Transactional
    public RentalDto rentBook(CreateRentalCommand command) {
        Rental savedRental = createRental(command);
        return MAPPER.mapToDto(savedRental);
    }

    @Transactional
    @MonitorMethod
    public void returnBook(int rentalId) {
        String username = getAuthenticatedUsername();
        Rental rental = getRentalById(rentalId);

        validateUserOwnership(username, rental);
        validateReturnDate(rental.getToDate());

        markRentalAsReturned(rental);
    }

    private String getAuthenticatedUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

    private User getUserByEmail(String email) {
        return userRepository.findWithLockingByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("USER_NOT_FOUND"));
    }

    private Book getBookById(int bookId) {
        return bookRepository.findWithLockingById(bookId)
                .orElseThrow(() -> new BookNotFoundException("BOOK_WITH_ID " + bookId + " NOT_FOUND"));
    }

    public Rental createRental(CreateRentalCommand command) {
        String username = getAuthenticatedUsername();
        User user = getUserByEmail(username);
        Book book = getBookById(command.getBookId());

        if (isAvailableForRental(book, command.getFromDate(), command.getToDate())) {
            throw new RentalException("BOOK_IS_ALREADY_RENTED_ON_THESE_DATES");
        }

        Rental rental = new Rental();
        rental.setUser(user);
        rental.setBook(book);
        rental.setFromDate(command.getFromDate());
        rental.setToDate(command.getToDate());
        rental.setReturned(false);

        user.getRentals().add(rental);
        book.getRentals().add(rental);

        return rentalRepository.save(rental);
    }

    public boolean isAvailableForRental(Book book, LocalDate fromDate, LocalDate toDate) {
        return rentalRepository.findOverlappingRentals(book, fromDate, toDate);
    }

    public void markRentalAsReturned(Rental rental) {
        if (rental.isReturned()) {
            throw new RentalException("BOOK_ALREADY_RETURNED");
        }
        rental.setReturned(true);
        rentalRepository.save(rental);
    }

    public void validateUserOwnership(String username, Rental rental) {
        if (!rental.getUser().getEmail().equals(username)) {
            throw new RentalException("USER_DOES_NOT_OWN_RENTAL");
        }
    }

    public void validateReturnDate(LocalDate toDate) {
        if (toDate.isBefore(LocalDate.now())) {
            throw new RentalException("RENTAL_ALREADY_RETURNED");
        }
    }

    private Rental getRentalById(int rentalId) {
        return rentalRepository.findWithLockingById(rentalId)
                .orElseThrow(() -> new RentalNotFoundException("RENTAL_WITH_ID:" + rentalId + " NOT_FOUND"));
    }
}
