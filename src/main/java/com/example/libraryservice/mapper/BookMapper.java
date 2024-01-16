package com.example.libraryservice.mapper;

import com.example.libraryservice.book.model.Book;
import com.example.libraryservice.book.model.BookDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface BookMapper {
    BookMapper MAPPER = Mappers.getMapper(BookMapper.class);

    @Mapping(source = "registerTime", target = "registerTime")
    BookDto mapToDto(Book book);
}
