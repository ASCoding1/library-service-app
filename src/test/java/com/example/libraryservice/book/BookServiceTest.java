package com.example.libraryservice.book;

import com.example.libraryservice.book.command.CreateBookCommand;
import com.example.libraryservice.book.model.Book;
import com.example.libraryservice.book.model.BookDto;
import com.example.libraryservice.common.exception.model.BookNotFoundException;
import com.example.libraryservice.rabbit.model.BookInfo;
import com.example.libraryservice.rabbit.model.MailInfoRabbit;
import com.example.libraryservice.rabbit.service.RabbitMqService;
import com.example.libraryservice.subscription.SubscriptionRepository;
import com.example.libraryservice.subscription.model.Subscription;
import com.example.libraryservice.user.UserRepository;
import com.example.libraryservice.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.*;


import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BookServiceTest {
    @InjectMocks
    private BookService bookService;
    @Mock
    private BookRepository bookRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private RabbitMqService rabbitMqService;

    @BeforeEach
    void setUp() {
        Subscription subscription = new Subscription();
        subscription.setActive(true);
        subscription.setCategoryName("ADVENTURE");
        subscription.setUser(new User());

        MockitoAnnotations.openMocks(this);
        List<Subscription> sampleSubscribers = List.of(subscription);
        Slice<Subscription> sampleSlice = new SliceImpl<>(sampleSubscribers);

        when(subscriptionRepository.findSubscriptionsByCategoryNameAndActiveFetchUser(eq("ADVENTURE"), eq(true), any(Pageable.class))).thenReturn(sampleSlice);
    }

    @Test
    void testFindAll() {
        List<Book> books = new ArrayList<>();
        books.add(new Book(1, "Book 1", "Author 1", "ADVENTURE", false, null, LocalDateTime.now()));
        books.add(new Book(2, "Book 2", "Author 2", "SCIENCE_FICTION", false, null, LocalDateTime.now()));

        Pageable pageable = Pageable.ofSize(10);
        Page<Book> bookPage = new PageImpl<>(books, pageable, books.size());

        when(bookRepository.findAll(pageable)).thenReturn(bookPage);

        Page<Book> result = bookService.findAll(pageable);

        assertEquals(bookPage, result);
        verify(bookRepository, times(1)).findAll(pageable);
    }

    @Test
    void testSave() {
        CreateBookCommand command = new CreateBookCommand();
        command.setTitle("Book 1");
        command.setAuthor("Author 1");
        command.setBlocked(false);
        command.setCategory("SCIENCE_FICTION");

        Book bookToSave = Book.builder()
                .id(1)
                .title("Book 1")
                .author("Author 1")
                .category("SCIENCE_FICTION")
                .blocked(false)
                .build();

        when(bookRepository.save(any(Book.class))).thenReturn(bookToSave);

        BookDto result = bookService.save(command);

        verify(userRepository, never()).findAll();
        verify(rabbitMqService, never()).send(any(), any());
        verify(bookRepository, times(1)).save(any());
        assertThat(result.getId()).isEqualTo(bookToSave.getId());
        assertThat(result.getTitle()).isEqualTo(bookToSave.getTitle());
        assertThat(result.getCategory()).isEqualTo(bookToSave.getCategory());
        assertThat(result.getAuthor()).isEqualTo(bookToSave.getAuthor());
    }

    @Test
    void testBlockBookById_BookFoundAndBlocked() {
        int bookId = 1;
        Book book = Book.builder()
                .id(bookId)
                .blocked(false)
                .build();

        when(bookRepository.blockBookById(bookId)).thenAnswer(invocation -> {
            book.setBlocked(true);
            return 1;
        });

        assertDoesNotThrow(() -> bookService.blockBookById(bookId));

        assertTrue(book.isBlocked());
        verify(bookRepository, times(1)).blockBookById(bookId);
    }

    @Test
    void testBlockBookById_BookNotFound_orBlocked() {
        int bookId = 1;

        when(bookRepository.blockBookById(bookId)).thenReturn(0);

        assertThrows(BookNotFoundException.class, () -> bookService.blockBookById(bookId));
    }

    @Test
    public void testCreateBookSave() {
        CreateBookCommand command = new CreateBookCommand();
        command.setTitle("Test Book");
        command.setAuthor("Test Author");
        command.setCategory("Test Category");
        command.setBlocked(false);

        Book savedBook = new Book();
        savedBook.setId(1);
        savedBook.setTitle(command.getTitle());
        savedBook.setAuthor(command.getAuthor());
        savedBook.setCategory(command.getCategory());
        savedBook.setBlocked(command.isBlocked());

        when(bookRepository.save(any(Book.class))).thenReturn(savedBook);

        Book result = bookService.createBookSave(command);

        assertThat(result.getId()).isEqualTo(savedBook.getId());
        assertThat(result.getTitle()).isEqualTo(savedBook.getTitle());
        assertThat(result.getAuthor()).isEqualTo(savedBook.getAuthor());
        assertThat(result.getCategory()).isEqualTo(savedBook.getCategory());
        assertThat(result.isBlocked()).isEqualTo(savedBook.isBlocked());
    }


    @Test
    public void testSaveExecutionTime() {
        CreateBookCommand command = new CreateBookCommand();
        Book savedBook = new Book();
        savedBook.setCategory("exampleCategory");
        savedBook.setTitle("exampleTitle");

        List<Subscription> subscriptions = new ArrayList<>();

        for (int i = 0; i < 1000000; i++) {
            Subscription subscription = new Subscription();
            User user = new User();
            user.setEmail("user" + i + "@example.com");
            subscription.setUser(user);
            subscriptions.add(subscription);
        }

        Page<Subscription> subscriptionPage = new PageImpl<>(subscriptions);

        Pageable pageable = Pageable.ofSize(20).withPage(0);

        when(subscriptionRepository.findSubscriptionsByCategoryNameAndActiveFetchUser(
                eq("exampleCategory"), eq(true), eq(pageable)))
                .thenReturn(subscriptionPage);
        when(bookRepository.save(any(Book.class))).thenReturn(savedBook);

        long startTime = System.currentTimeMillis();

        BookDto bookDto = bookService.save(command);

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        System.out.println(executionTime);

        assertNotNull(bookDto);
    }

    @Test
    void testFindBooksAddedLast24Hours() {
        LocalDateTime currentTime = LocalDateTime.now();
        Book book1 = new Book();
        book1.setRegisterTime(currentTime.minusHours(23));
        Book book2 = new Book();
        book2.setRegisterTime(currentTime.minusHours(23));
        Book book3 = new Book();
        book3.setRegisterTime(currentTime.minusDays(2));

        List<Book> books = new ArrayList<>();
        books.add(book1);
        books.add(book2);
        books.add(book3);

        when(bookRepository.findBooksAddedWithinLast24Hours(any(), any())).thenReturn(new SliceImpl<>(books));

        List<Book> result = bookService.findBooksAddedLast24Hours();

        assertNotNull(result);
        assertEquals(3, result.size());

        verify(bookRepository, times(1)).findBooksAddedWithinLast24Hours(any(), any());
    }

    @Test
    void testSendMailInfoForCategories() throws InterruptedException {
        Map<String, List<String>> categorySubscribersMap = Collections.singletonMap("Fiction", Collections.singletonList("user@example.com"));
        List<Book> newBooks = new ArrayList<>();
        Book newBook = new Book();
        newBook.setTitle("New Book");
        newBook.setCategory("Fiction");
        newBook.setAuthor("Jane Doe");
        newBook.setBlocked(false);
        newBook.setRegisterTime(LocalDateTime.now().minusHours(12));
        newBooks.add(newBook);

        CountDownLatch latch = new CountDownLatch(categorySubscribersMap.size());

        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(rabbitMqService).send(any(), any());

        bookService.sendMailInfoForCategories(categorySubscribersMap, newBooks);

        assertTrue(latch.await(10, TimeUnit.SECONDS));

        verify(rabbitMqService, times(1)).send(any(MailInfoRabbit.class), any());
    }

    @Test
    void testCreateMailInfo() {
        List<String> subscriberEmails = Collections.singletonList("user@example.com");
        String bookCategory = "Fiction";

        List<Book> newBooks = new ArrayList<>();
        Book newBook = new Book();
        newBook.setTitle("New Book");
        newBook.setCategory("Fiction");
        newBook.setAuthor("Jane Doe");
        newBook.setBlocked(false);
        newBook.setRegisterTime(LocalDateTime.now().minusHours(12));
        newBooks.add(newBook);

        MailInfoRabbit mailInfo = bookService.createMailInfo(subscriberEmails, bookCategory, newBooks);

        assertNotNull(mailInfo);
        assertEquals(subscriberEmails, mailInfo.getSubscriberEmails());
        assertEquals(1, mailInfo.getBookInfoList().size());
        BookInfo bookInfo = mailInfo.getBookInfoList().get(0);
        assertEquals(newBook.getTitle(), bookInfo.getTitle());
        assertEquals(newBook.getCategory(), bookInfo.getCategory());
        assertEquals(newBook.getAuthor(), bookInfo.getAuthor());
    }

    @Test
    void testBuildCategorySubscribersMap_WithNoBooks() {
        List<Book> emptyBookList = Collections.emptyList();

        Map<String, List<String>> categorySubscribersMap = bookService.buildCategorySubscribersMap(emptyBookList);

        assertNotNull(categorySubscribersMap);
        assertTrue(categorySubscribersMap.isEmpty());
    }

}


