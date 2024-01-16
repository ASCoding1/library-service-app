package com.example.libraryservice.subscription;

import com.example.libraryservice.common.exception.model.SubscriptionException;
import com.example.libraryservice.monitoring_logs.MonitorMethod;
import com.example.libraryservice.subscription.command.CreateSubscriptionCommand;
import com.example.libraryservice.subscription.model.Subscription;
import com.example.libraryservice.subscription.model.SubscriptionDto;
import com.example.libraryservice.user.UserRepository;
import com.example.libraryservice.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import static com.example.libraryservice.mapper.SubscriptionMapper.MAPPER;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;

    @MonitorMethod
    public Page<Subscription> findAll(Pageable pageable) {
        return subscriptionRepository.findAll(pageable);
    }

    @MonitorMethod
    public SubscriptionDto save(CreateSubscriptionCommand command) {
        Subscription saved = createSubscriptionSave(command);
        return MAPPER.mapToDto(saved);
    }

    @MonitorMethod
    public void unsubscribeToCategory(String bookCategory) {
        String username = getAuthenticatedUsername();
        User user = getUserByEmail(username);
        if (user.getSubscriptions() == null) {
            throw new SubscriptionException("USER_HAS_N0T_SUBSCRIPTIONS");
        }
        Subscription subscriptionToRemove = user.getSubscriptions().stream()
                .filter(subscription -> subscription.getCategoryName().equals(bookCategory))
                .findFirst()
                .orElse(null);

        if (subscriptionToRemove == null) {
            throw new SubscriptionException("CATEGORY_IS_NOT_SUBSCRIBED");
        }
        subscriptionToRemove.setActive(false);

        user.getSubscriptions().remove(subscriptionToRemove);
        userRepository.save(user);
    }

    public String getAuthenticatedUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("USER_NOT_FOUND"));
    }

    public Subscription createSubscriptionSave(CreateSubscriptionCommand command) {
        String username = getAuthenticatedUsername();
        User user = getUserByEmail(username);
        if (user.getSubscriptions().stream().anyMatch(subscription -> subscription.getCategoryName().equals(command.getCategoryName()))) {
            throw new SubscriptionException("CATEGORY_IS_ALREADY_SUBSCRIBED");
        }
        command.setActive(true);
        Subscription subscription = new Subscription();
        subscription.setUser(user);
        subscription.setCategoryName(command.getCategoryName());
        subscription.setActive(command.isActive());
        user.getSubscriptions().add(subscription);
        return subscriptionRepository.save(subscription);
    }
}
