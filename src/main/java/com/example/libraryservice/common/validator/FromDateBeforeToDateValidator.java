package com.example.libraryservice.common.validator;

import com.example.libraryservice.rental.command.CreateRentalCommand;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

class FromDateBeforeToDateValidator implements ConstraintValidator<FromDateBeforeToDate, CreateRentalCommand> {
    @Override
    public void initialize(FromDateBeforeToDate constraintAnnotation) {
    }

    @Override
    public boolean isValid(CreateRentalCommand dateRange, ConstraintValidatorContext context) {
        if (dateRange == null) {
            return true;
        }

        if (dateRange.getFromDate() == null || dateRange.getToDate() == null) {
            if (dateRange.getFromDate() == null) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("FROM_DATE_CANT_BE_EMPTY")
                        .addPropertyNode("fromDate")
                        .addConstraintViolation();
            }

            if (dateRange.getToDate() == null) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("TO_DATE_CANT_BE_EMPTY")
                        .addPropertyNode("toDate")
                        .addConstraintViolation();
            }

            return false;
        }

        boolean isValid = dateRange.getFromDate().isBefore(dateRange.getToDate());

        if (!isValid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("TO_DATE_CAN_NOT_BE_BEFORE_FROM_DATE")
                    .addPropertyNode("fromDate")
                    .addConstraintViolation();
        }

        return isValid;
    }
}
