package com.example.libraryservice.subscription;

import com.example.libraryservice.subscription.model.Subscription;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SubscriptionRepository extends JpaRepository<Subscription, Integer> {

    @Query("SELECT s FROM Subscription s JOIN FETCH s.user WHERE s.categoryName = :categoryName AND s.active = :active")
    Slice<Subscription> findSubscriptionsByCategoryNameAndActiveFetchUser(String categoryName, boolean active, Pageable pageable);

}
