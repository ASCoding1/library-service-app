package com.example.libraryservice.user.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class RegisterRequest {
    @NotBlank(message = "value is blank")
    @Pattern(regexp = "[A-Z][a-z]*", message = "name has to match specific pattern, example = 'Adam'")
    private String name;
    @NotBlank
    @Pattern(regexp = "[A-Z][a-z]*", message = "surname has to match specific pattern, example = 'Kowalski'")
    private String surname;
    @Email
    private String email;
    private String password;
}
