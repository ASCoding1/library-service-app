package com.example.libraryservice.mapper;

import com.example.libraryservice.subscription.model.Subscription;
import com.example.libraryservice.subscription.model.SubscriptionDto;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
@Mapper
public interface SubscriptionMapper {
    SubscriptionMapper MAPPER = Mappers.getMapper(SubscriptionMapper.class);

    SubscriptionDto mapToDto(Subscription subscription);

}
