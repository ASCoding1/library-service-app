package com.example.libraryservice.subscription;

import com.example.libraryservice.subscription.command.CreateSubscriptionCommand;
import com.example.libraryservice.subscription.model.Subscription;
import com.example.libraryservice.subscription.model.SubscriptionDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.example.libraryservice.mapper.SubscriptionMapper.MAPPER;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/sub")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping()
    public ResponseEntity<SubscriptionDto> save(@RequestBody @Valid CreateSubscriptionCommand command) {
        SubscriptionDto saved = subscriptionService.save(command);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @GetMapping()
    public Page<SubscriptionDto> findAll(@PageableDefault(size = 10) Pageable pageable) {
        Page<Subscription> subscriptionPage = subscriptionService.findAll(pageable);
        return subscriptionPage.map(MAPPER::mapToDto);
    }

    @DeleteMapping()
    public ResponseEntity<String> unsubscribeToCategories(@RequestParam String bookCategory) {
        subscriptionService.unsubscribeToCategory(bookCategory);
        return new ResponseEntity<>("Category unsubscribed success!", HttpStatus.NO_CONTENT);
    }
}
