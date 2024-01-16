package com.example.libraryservice.book;

import com.example.libraryservice.book.command.CreateBookCommand;
import com.example.libraryservice.book.model.Book;
import com.example.libraryservice.common.enums.Role;
import com.example.libraryservice.monitoring_logs.LogMessage;
import com.example.libraryservice.rabbit.service.RabbitMqService;
import com.example.libraryservice.subscription.model.Subscription;
import com.example.libraryservice.user.UserRepository;
import com.example.libraryservice.user.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.AssertionsForClassTypes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
class BookControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookRepository bookRepository;

    @MockBean
    private RabbitMqService rabbitMqService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private Subscription subscription;

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    public void should_be_empty() {
        List<Book> books = bookRepository.findAll();
        assertThat(books).hasSize(0);
    }


    @BeforeEach
    void init() {
        subscription = new Subscription();
        subscription.setCategoryName("ADVENTURE");
        User user = new User();
        user.setRole(Role.CUSTOMER);
        user.setEmail("user@example.com");
        user.setSubscriptions(Set.of(subscription));
        userRepository.saveAndFlush(user);
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    public void testFindAllBooks() throws Exception {
        Book book1 = Book.builder()
                .title("Title")
                .id(1)
                .author("Author")
                .category("ADVENTURE")
                .blocked(false)
                .build();

        Book book2 = Book.builder()
                .title("Title 2")
                .id(2)
                .author("Author 2")
                .category("ADVENTURE")
                .blocked(false)
                .build();

        bookRepository.saveAll(Arrays.asList(book1, book2));

        mockMvc.perform(get("/api/v1/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id", is(book1.getId())))
                .andExpect(jsonPath("$.content[0].title", is(book1.getTitle())))
                .andExpect(jsonPath("$.content[0].author", is(book1.getAuthor())))
                .andExpect(jsonPath("$.content[0].category", is(book1.getCategory())))
                .andExpect(jsonPath("$.content[0].blocked", is(book1.isBlocked())))
                .andExpect(jsonPath("$.content[1].id", is(book2.getId())))
                .andExpect(jsonPath("$.content[1].title", is(book2.getTitle())))
                .andExpect(jsonPath("$.content[1].author", is(book2.getAuthor())))
                .andExpect(jsonPath("$.content[1].category", is(book2.getCategory())))
                .andExpect(jsonPath("$.content[1].blocked", is(book2.isBlocked())));

        verify(rabbitMqService, times(1)).send(any(LogMessage.class), anyString());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    public void testSaveBook() throws Exception {
        CreateBookCommand command = new CreateBookCommand();
        command.setTitle("New Book");
        command.setAuthor("Author");
        command.setCategory("ADVENTURE");
        command.setBlocked(false);


        User user1 = new User();
        user1.setEmail("user1@example.com");
        user1.setSubscriptions(Set.of(subscription));

        User user2 = new User();
        user2.setEmail("user2@example.com");

        mockMvc.perform(post("/api/v1/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is(command.getTitle())))
                .andExpect(jsonPath("$.blocked", is(command.isBlocked())))
                .andExpect(jsonPath("$.category", is(command.getCategory())))
                .andExpect(jsonPath("$.author", is(command.getAuthor())));

        List<Book> books = bookRepository.findAll();
        assertThat(books).hasSize(1);
        Book savedBook = books.get(0);
        assertThat(savedBook.getTitle()).isEqualTo(command.getTitle());
        assertThat(savedBook.getAuthor()).isEqualTo(command.getAuthor());
        assertThat(savedBook.getCategory()).isEqualTo(command.getCategory());
        AssertionsForClassTypes.assertThat(savedBook.isBlocked()).isEqualTo(command.isBlocked());
        assertThat(savedBook.isBlocked()).isFalse();


        verify(rabbitMqService, times(1)).send(any(LogMessage.class), anyString());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    public void testBlockBookById() throws Exception {
        Book book = Book.builder()
                .title("Title")
                .author("Author")
                .category("ADVENTURE")
                .blocked(false)
                .build();
        bookRepository.save(book);

        mockMvc.perform(patch("/api/v1/books/{id}/block", book.getId()))
                .andExpect(status().isOk());

        Book blockedBook = bookRepository.findById(book.getId()).orElse(null);
        assertThat(blockedBook).isNotNull();
        assertThat(blockedBook.isBlocked()).isTrue();
        verify(rabbitMqService, times(1)).send(any(LogMessage.class), anyString());
    }
}