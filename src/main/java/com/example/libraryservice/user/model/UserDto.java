package com.example.libraryservice.user.model;

import com.example.libraryservice.common.enums.Role;
import com.example.libraryservice.subscription.model.Subscription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
    private String name;
    private String surname;
    private String email;
    private Role role;
    private Set<Subscription> subscriptions;

}
