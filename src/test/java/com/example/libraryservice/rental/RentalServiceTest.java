package com.example.libraryservice.rental;

import com.example.libraryservice.book.BookRepository;
import com.example.libraryservice.book.model.Book;
import com.example.libraryservice.common.exception.model.RentalException;
import com.example.libraryservice.common.exception.model.RentalNotFoundException;
import com.example.libraryservice.common.exception.model.UserNotFoundException;
import com.example.libraryservice.rental.command.CreateRentalCommand;
import com.example.libraryservice.rental.model.Rental;
import com.example.libraryservice.rental.model.RentalDto;
import com.example.libraryservice.user.UserRepository;
import com.example.libraryservice.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


class RentalServiceTest {

    @Mock
    private RentalRepository rentalRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private RentalService rentalService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("test@example.com");
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    @WithMockUser("test@example.com")
    public void testRentBook_SuccessfulRental() {
        CreateRentalCommand command = new CreateRentalCommand();
        command.setBookId(1);
        command.setFromDate(LocalDate.now().plusDays(25));
        command.setToDate(LocalDate.now().plusDays(26));

        User user = new User();
        user.setEmail("testuser@example.com");

        Book book = new Book();
        book.setId(1);
        book.setTitle("Test Book");

        Rental rental = new Rental();
        rental.setUser(user);
        rental.setBook(book);

        when(rentalRepository.save(any(Rental.class))).thenReturn(rental);
        when(userRepository.findWithLockingByEmail(anyString())).thenReturn(Optional.of(user));
        when(bookRepository.findWithLockingById(anyInt())).thenReturn(Optional.of(book));
        when(rentalRepository.findOverlappingRentals(any(Book.class), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(false);

        RentalDto rentalDto = rentalService.rentBook(command);

        assertNotNull(rentalDto);

        assertNotNull(rentalDto);
        assertFalse(rentalDto.isReturned());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void rentBook_userNotFound_throwClientNotFoundException() {
        CreateRentalCommand command = new CreateRentalCommand();
        command.setBookId(1);
        command.setFromDate(LocalDate.now());
        command.setToDate(LocalDate.now().plusDays(7));

        String email = "test143@example.com";

        User user = new User();
        user.setEmail(email);

        Book book = new Book();
        book.setId(1);
        book.setBlocked(false);

        when(userRepository.findWithLockingByEmail(email)).thenReturn(Optional.of(user));
        when(bookRepository.findWithLockingById(1)).thenReturn(Optional.of(book));

        assertThrows(UserNotFoundException.class,
                () -> rentalService.rentBook(command));
    }

    @Test
    void returnBook_ValidData_ReturnedRental() {
        int rentalId = 1;
        String email = "test@example.com";

        Rental rental = new Rental();
        rental.setId(rentalId);
        rental.setReturned(false);
        rental.setToDate(LocalDate.now().plusDays(7));
        User user = new User();
        user.setEmail(email);
        rental.setUser(user);

        when(rentalRepository.findWithLockingById(rentalId)).thenReturn(Optional.of(rental));
        rentalService.returnBook(rentalId);
        assertTrue(rental.isReturned());
        verify(rentalRepository, times(1)).save(rental);
    }

    @Test
    void returnBook_RentalNotFound_ThrowsRentalNotFoundException() {
        int rentalId = 1;

        when(rentalRepository.findWithLockingById(rentalId)).thenReturn(Optional.empty());
        assertThrows(RentalNotFoundException.class, () -> rentalService.returnBook(rentalId));
        verify(rentalRepository, never()).save(any());
    }

    @Test
    void returnBook_UserDoesNotOwnRental_ThrowsRentalException() {
        int rentalId = 1;

        Rental rental = new Rental();
        rental.setId(rentalId);
        rental.setReturned(false);
        rental.setToDate(LocalDate.now().plusDays(7));
        User user = new User();
        user.setEmail("other@example.com");
        rental.setUser(user);

        when(rentalRepository.findWithLockingById(rentalId)).thenReturn(Optional.of(rental));
        assertThrows(RentalException.class, () -> rentalService.returnBook(rentalId));
        verify(rentalRepository, never()).save(any());
    }

    @Test
    void returnBook_RentalAlreadyReturned_ThrowsRentalException() {
        int rentalId = 1;
        String email = "test@example.com";

        Rental rental = new Rental();
        rental.setId(rentalId);
        rental.setReturned(true);
        rental.setToDate(LocalDate.now().minusDays(1));
        User user = new User();
        user.setEmail(email);
        rental.setUser(user);

        when(rentalRepository.findWithLockingById(rentalId)).thenReturn(Optional.of(rental));
        assertThrows(RentalException.class, () -> rentalService.returnBook(rentalId));
        verify(rentalRepository, never()).save(any());
    }

    @Test
    void returnBook_RentalAlreadyPastDueDate_ThrowsRentalException() {
        int rentalId = 1;
        String email = "test@example.com";

        Rental rental = new Rental();
        rental.setId(rentalId);
        rental.setReturned(false);
        rental.setToDate(LocalDate.now().minusDays(1));
        User user = new User();
        user.setEmail(email);
        rental.setUser(user);

        when(rentalRepository.findWithLockingById(rentalId)).thenReturn(Optional.of(rental));
        assertThrows(RentalException.class, () -> rentalService.returnBook(rentalId));
        verify(rentalRepository, never()).save(any());
    }

    @Test
    void markRentalAsReturned_ValidRental_RentalMarkedAsReturned() {
        Rental rental = new Rental();
        rental.setReturned(false);

        when(rentalRepository.save(any())).thenReturn(rental);

        rentalService.markRentalAsReturned(rental);

        assertTrue(rental.isReturned());

        verify(rentalRepository, times(1)).save(rental);
    }

    @Test
    void markRentalAsReturned_AlreadyReturnedRental_ThrowsRentalException() {
        Rental rental = new Rental();
        rental.setReturned(true);

        assertThrows(RentalException.class, () -> rentalService.markRentalAsReturned(rental));

        verify(rentalRepository, never()).save(any());
    }

    @Test
    void validateUserOwnership_OwnerUser_ValidatesSuccessfully() {
        Rental rental = new Rental();
        User user = new User();
        user.setEmail("test@example.com");
        rental.setUser(user);

        assertDoesNotThrow(() -> rentalService.validateUserOwnership("test@example.com", rental));
    }

    @Test
    void validateUserOwnership_NonOwnerUser_ThrowsRentalException() {
        Rental rental = new Rental();
        User user = new User();
        user.setEmail("other@example.com");
        rental.setUser(user);

        assertThrows(RentalException.class, () -> rentalService.validateUserOwnership("test@example.com", rental));
    }

    @Test
    void validateReturnDate_ReturnDateInFuture_NoExceptionThrown() {
        LocalDate returnDate = LocalDate.now().plusDays(1);

        assertDoesNotThrow(() -> rentalService.validateReturnDate(returnDate));
    }

    @Test
    void validateReturnDate_ReturnDateInPast_ThrowsRentalException() {
        LocalDate returnDate = LocalDate.now().minusDays(1);

        assertThrows(RentalException.class, () -> rentalService.validateReturnDate(returnDate));
    }

    @Test
    public void testIsAvailableForRental_WhenNoOverlappingRentals_ReturnsTrue() {
        Book book = new Book();
        book.setId(1);
        book.setTitle("Test Book");

        LocalDate fromDate = LocalDate.now();
        LocalDate toDate = LocalDate.now().plusDays(7);

        when(rentalRepository.findOverlappingRentals(any(Book.class), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(true);

        boolean isAvailable = rentalService.isAvailableForRental(book, fromDate, toDate);

        assertTrue(isAvailable);
    }

    @Test
    public void testIsAvailableForRental_WhenOverlappingRentalsExist_ReturnsFalse() {
        Book book = new Book();
        book.setId(1);
        book.setTitle("Test Book");

        LocalDate fromDate = LocalDate.now();
        LocalDate toDate = LocalDate.now().plusDays(7);

        Rental overlappingRental = new Rental();
        overlappingRental.setBook(book);

        when(rentalRepository.findOverlappingRentals(any(Book.class), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(false);

        boolean isAvailable = rentalService.isAvailableForRental(book, fromDate, toDate);

        assertFalse(isAvailable);
    }


}