package com.example.libraryservice.book;

import com.example.libraryservice.book.command.CreateBookCommand;
import com.example.libraryservice.book.model.Book;
import com.example.libraryservice.book.model.BookDto;
import com.example.libraryservice.common.exception.model.RabbitMessageSendingException;
import com.example.libraryservice.common.exception.model.SubAddingException;
import com.example.libraryservice.monitoring_logs.MonitorMethod;
import com.example.libraryservice.rabbit.model.BookInfo;
import com.example.libraryservice.subscription.SubscriptionRepository;
import com.example.libraryservice.common.exception.model.BookNotFoundException;
import com.example.libraryservice.rabbit.model.MailInfoRabbit;
import com.example.libraryservice.rabbit.service.RabbitMqService;
import com.example.libraryservice.subscription.model.Subscription;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.example.libraryservice.mapper.BookMapper.MAPPER;

@Service
@RequiredArgsConstructor
public class BookService {
    private final BookRepository bookRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final RabbitMqService rabbitMqService;

    @Value("${library-queue-name}")
    private String queueName;

    private final int corePoolSize = 2;

    private final int maxPoolSize = 4;

    private final int keepAliveTime = 60;

    private final Logger logger = LoggerFactory.getLogger(BookService.class);
    private final BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>();
    private final ThreadPoolExecutor executorService = new ThreadPoolExecutor(
            corePoolSize, maxPoolSize, keepAliveTime, TimeUnit.SECONDS, workQueue
    );

    @MonitorMethod
    public BookDto save(CreateBookCommand command) {
        Book savedBook = createBookSave(command);
        return MAPPER.mapToDto(savedBook);
    }


    //metoda wysylająca info do subskrybentów danych kategorii o nowych książkach danego dnia
    @Scheduled(cron = "0 0 19 * * ?")
    public void sendInfo() {
        List<Book> newBooks = findBooksAddedLast24Hours();

        Map<String, List<String>> categorySubscribersMap = buildCategorySubscribersMap(newBooks);

        sendMailInfoForCategories(categorySubscribersMap, newBooks);
    }

    public Map<String, List<String>> buildCategorySubscribersMap(List<Book> newBooks) {
        Map<String, List<String>> categorySubscribersMap = new HashMap<>();

        for (Book book : newBooks) {
            String bookCategory = book.getCategory();
            categorySubscribersMap.computeIfAbsent(bookCategory, k -> new ArrayList<>());
            try {
                addSubscribersForCategory(bookCategory, categorySubscribersMap);
            } catch (SubAddingException e) {
                logger.error("An error occurred while adding subscribers for category: " + bookCategory, e);
            }
        }

        return categorySubscribersMap;
    }

    public List<Book> findBooksAddedLast24Hours() {
        int pageSize = 5000;
        LocalDateTime cutOffTime = LocalDateTime.now().minusDays(1);
        List<Book> result = new ArrayList<>();
        int pageNumber = 0;

        while (true) {
            Slice<Book> booksPage = bookRepository.findBooksAddedWithinLast24Hours(
                    cutOffTime,
                    PageRequest.of(pageNumber, pageSize)
            );

            List<Book> books = booksPage.getContent();
            result.addAll(books);

            if (!booksPage.hasNext()) {
                break;
            }

            pageNumber++;
        }

        return result;
    }

    public void addSubscribersForCategory(String bookCategory, Map<String, List<String>> categorySubscribersMap) {
        int pageSize = 5000;
        int pageNumber = 0;

        while (true) {
            Slice<Subscription> subscribersSlice = subscriptionRepository.findSubscriptionsByCategoryNameAndActiveFetchUser(
                    bookCategory, true, PageRequest.of(pageNumber, pageSize));

            List<Subscription> subscribers = subscribersSlice.getContent();

            if (subscribers.isEmpty()) {
                break;
            }

            List<String> subscriberEmails = categorySubscribersMap.computeIfAbsent(bookCategory, k -> new ArrayList<>());
            for (Subscription subscriber : subscribers) {
                String userEmail = subscriber.getUser().getEmail();
                if (!subscriberEmails.contains(userEmail)) {
                    subscriberEmails.add(userEmail);
                }
            }
            pageNumber++;
        }
    }

    public void sendMailInfoForCategories(Map<String, List<String>> categorySubscribersMap, List<Book> newBooks) {
        for (Map.Entry<String, List<String>> entry : categorySubscribersMap.entrySet()) {
            String bookCategory = entry.getKey();
            List<String> subscriberEmails = entry.getValue();

            List<Book> booksInCategory = newBooks.stream()
                    .filter(book -> book.getCategory().equals(bookCategory))
                    .collect(Collectors.toList());

            executorService.execute(() -> {
                try {
                    MailInfoRabbit mailInfo = createMailInfo(subscriberEmails, bookCategory, booksInCategory);
                    rabbitMqService.send(mailInfo, queueName);
                } catch (RabbitMessageSendingException e) {
                    logger.error("Error while sending mail info for category: " + bookCategory, e);
                }
            });
        }
    }

    public MailInfoRabbit createMailInfo(List<String> subscriberEmails, String bookCategory, List<Book> newBooks) {
        MailInfoRabbit mailInfo = new MailInfoRabbit();
        mailInfo.setSubscriberEmails(subscriberEmails);

        List<BookInfo> bookInfoList = new ArrayList<>();
        for (Book book : newBooks) {
            if (book.getCategory().equals(bookCategory)) {
                BookInfo bookInfo = BookInfo.builder()
                        .title(book.getTitle())
                        .category(book.getCategory())
                        .author(book.getAuthor())
                        .build();
                bookInfoList.add(bookInfo);
            }
        }
        mailInfo.setBookInfoList(bookInfoList);

        return mailInfo;
    }

    @MonitorMethod
    public Page<Book> findAll(Pageable pageable) {
        return bookRepository.findAll(pageable);
    }

    @MonitorMethod
    @Transactional
    public void blockBookById(int id) {
        int updatedCount = bookRepository.blockBookById(id);

        if (updatedCount == 0) {
            throw new BookNotFoundException("BOOK_NOT_FOUND_OR_ALREADY_BLOCKED");
        }
    }

    public Book createBookSave(CreateBookCommand command) {
        Book book = new Book();
        book.setTitle(command.getTitle());
        book.setAuthor(command.getAuthor());
        book.setCategory(command.getCategory());
        book.setBlocked(command.isBlocked());
        return bookRepository.save(book);
    }
}
