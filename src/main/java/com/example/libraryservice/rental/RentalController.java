package com.example.libraryservice.rental;

import com.example.libraryservice.rental.command.CreateRentalCommand;
import com.example.libraryservice.rental.model.RentalDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/rentals")
public class RentalController {
    private final RentalService rentalService;

    @PostMapping()
    public ResponseEntity<RentalDto> rentBook(@RequestBody @Valid CreateRentalCommand command) {
        RentalDto saved = rentalService.rentBook(command);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @PutMapping("/{rentalId}/return")
    public ResponseEntity<String> returnBook(@PathVariable int rentalId) {
        rentalService.returnBook(rentalId);
        return new ResponseEntity<>("Book returned successfully", HttpStatus.OK);
    }
}
