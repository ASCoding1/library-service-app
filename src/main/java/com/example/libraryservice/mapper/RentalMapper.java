package com.example.libraryservice.mapper;

import com.example.libraryservice.rental.model.Rental;
import com.example.libraryservice.rental.model.RentalDto;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
@Mapper
public interface RentalMapper {
    RentalMapper MAPPER = Mappers.getMapper(RentalMapper.class);

    RentalDto mapToDto(Rental rental);

}
