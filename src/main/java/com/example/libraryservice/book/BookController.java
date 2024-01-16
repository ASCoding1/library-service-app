package com.example.libraryservice.book;

import com.example.libraryservice.book.command.CreateBookCommand;
import com.example.libraryservice.book.model.Book;
import com.example.libraryservice.book.model.BookDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.example.libraryservice.mapper.BookMapper.MAPPER;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/books")
public class BookController {

    private final BookService bookService;

    @GetMapping()
    public Page<BookDto> findAll(@PageableDefault(size = 10) Pageable pageable) {
        Page<Book> bookPage = bookService.findAll(pageable);
        return bookPage.map(MAPPER::mapToDto);
    }

    @PostMapping()
    public ResponseEntity<BookDto> save(@RequestBody @Valid CreateBookCommand command) throws InterruptedException {
        BookDto saved = bookService.save(command);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @PatchMapping("/{id}/block")
    public ResponseEntity<Void> blockBookById(@PathVariable("id") int id) {
        bookService.blockBookById(id);
        return ResponseEntity.ok().build();
    }
}
