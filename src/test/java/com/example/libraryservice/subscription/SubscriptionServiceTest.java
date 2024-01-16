package com.example.libraryservice.subscription;

import com.example.libraryservice.common.exception.model.SubscriptionException;
import com.example.libraryservice.subscription.command.CreateSubscriptionCommand;
import com.example.libraryservice.subscription.model.Subscription;
import com.example.libraryservice.subscription.model.SubscriptionDto;
import com.example.libraryservice.user.UserRepository;
import com.example.libraryservice.user.UserService;
import com.example.libraryservice.user.model.User;
import com.example.libraryservice.user.model.UserDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.awt.print.Pageable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SubscriptionServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SubscriptionService subscriptionService;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("test@example.com");
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    public void testSave() {
        CreateSubscriptionCommand command = new CreateSubscriptionCommand();
        command.setCategoryName("TestCategory");
        command.setCreationDate(LocalDate.now());

        User user = new User();
        user.setEmail("test@example.com");
        user.setSubscriptions(new HashSet<>());

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> {
            Subscription savedSubscription = invocation.getArgument(0);
            savedSubscription.setId(1);
            return savedSubscription;
        });

        SubscriptionDto result = subscriptionService.save(command);

        assertNotNull(result);
        assertEquals("TestCategory", result.getCategoryName());
    }

    @Test
    public void testUnsubscribeToCategory() {
        String bookCategory = "TestCategory";
        String userEmail = "test@example.com";

        User user = new User();
        user.setEmail(userEmail);
        user.setSubscriptions(new HashSet<>());

        Subscription subscriptionToRemove = new Subscription();
        subscriptionToRemove.setCategoryName(bookCategory);
        user.getSubscriptions().add(subscriptionToRemove);

        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(user));

        subscriptionService.unsubscribeToCategory(bookCategory);

        assertTrue(user.getSubscriptions().isEmpty());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    public void testUnsubscribeToCategoryCategoryNotSubscribed() {
        String bookCategory = "TestCategory";
        String userEmail = "test@example.com";

        User user = new User();
        user.setEmail(userEmail);

        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(user));

        assertThrows(SubscriptionException.class, () -> subscriptionService.unsubscribeToCategory(bookCategory));
        verify(userRepository, never()).save(user);
    }

    @Test
    public void testGetAuthenticatedUsername() {
        Authentication authentication = mock(Authentication.class);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(authentication.getName()).thenReturn("testuser");

        String result = subscriptionService.getAuthenticatedUsername();

        assertEquals("testuser", result);
    }

    @Test
    public void testGetUserByEmail() {
        String userEmail = "test@example.com";
        User user = new User();
        user.setEmail(userEmail);

        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(user));

        User result = subscriptionService.getUserByEmail(userEmail);

        assertNotNull(result);
        assertEquals(userEmail, result.getEmail());
    }

    @Test
    public void testGetUserByEmailNotFound() {
        String userEmail = "test@example.com";

        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> subscriptionService.getUserByEmail(userEmail));
    }

    @Test
    public void testCreateSubscriptionSave() {
        CreateSubscriptionCommand command = new CreateSubscriptionCommand();
        command.setCategoryName("TestCategory");
        command.setCreationDate(LocalDate.now());

        User user = new User();
        user.setEmail("test@example.com");
        user.setSubscriptions(new HashSet<>());

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> {
            Subscription savedSubscription = invocation.getArgument(0);
            savedSubscription.setId(1);
            return savedSubscription;
        });

        Subscription result = subscriptionService.createSubscriptionSave(command);

        assertNotNull(result);
        assertEquals("TestCategory", result.getCategoryName());
    }

    @Test
    public void testCreateSubscriptionSaveCategoryAlreadySubscribed() {
        CreateSubscriptionCommand command = new CreateSubscriptionCommand();
        command.setCategoryName("TestCategory");
        command.setCreationDate(LocalDate.now());

        User user = new User();
        user.setEmail("test@example.com");
        user.setSubscriptions(new HashSet<>());

        Subscription existingSubscription = new Subscription();
        existingSubscription.setCategoryName("TestCategory");
        user.getSubscriptions().add(existingSubscription);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));

        assertThrows(SubscriptionException.class, () -> subscriptionService.createSubscriptionSave(command));
    }
}