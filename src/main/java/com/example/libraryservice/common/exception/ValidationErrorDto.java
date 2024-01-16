package com.example.libraryservice.common.exception;

import lombok.Getter;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ValidationErrorDto extends ExceptionDto {

    private final List<ViolationInfo> violations = new ArrayList<>();

    public ValidationErrorDto() {
        super("validation errors");
    }

    public void addViolationInfo(String field, String info) {
        violations.add(new ViolationInfo(field, info));
    }

    @Value
    public static class ViolationInfo {
        String field;
        String info;
    }
}