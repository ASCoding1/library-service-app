package com.example.libraryservice.common.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = FromDateBeforeToDateValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface FromDateBeforeToDate {
    String message() default "TO_DATE_CAN_NOT_BE_BEFORE_FROM_DATE";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
