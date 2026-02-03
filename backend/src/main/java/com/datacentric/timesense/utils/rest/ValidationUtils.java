package com.datacentric.timesense.utils.rest;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.datacentric.utils.rest.ValidationFailure;
import com.datacentric.timesense.utils.i18n.MessagesCodes;
import com.datacentric.utils.hibernate.JSON;

public class ValidationUtils {
    public static Map<String, String> createValidationMap(String fieldName, String messageCode,
            Object... messageParams) {
        Map<String, String> fieldMap = new HashMap<>();
        fieldMap.put("field", fieldName);
        fieldMap.put("message", messageCode);

        /*
         * Convert each parameter with its string representation
         * Join every string into a one string separated by "," making it easier to
         * split them into individual parameters on i18n method
         */
        if (messageParams != null && messageParams.length > 0) {
            fieldMap.put("messageParams", Arrays.stream(messageParams)
                    .map(Object::toString)
                    .collect(Collectors.joining(",")));
        }

        return fieldMap;
    }

    public static <T> void checkFieldFilled(T value, String fieldName,
            List<ValidationFailure> failedValidations) {
        if (value == null) {
            ValidationFailure fieldMap = new ValidationFailure(fieldName,
                    MessagesCodes.FIELD_MUST_BE_FILLED);
            failedValidations.add(fieldMap);
        }
    }

    public static void checkFieldFilled(String value, String fieldName,
            List<ValidationFailure> failedValidations) {
        if (value == null || value.isEmpty()) {
            ValidationFailure fieldMap = new ValidationFailure(fieldName,
                    MessagesCodes.FIELD_MUST_BE_FILLED);
            failedValidations.add(fieldMap);
        }
    }

    public static void checkFieldFilled(JSON value, String fieldName,
            List<ValidationFailure> failedValidations) {
        if (value == null || value.isEmpty()) {
            ValidationFailure fieldMap = new ValidationFailure(fieldName,
                    MessagesCodes.FIELD_MUST_BE_FILLED);
            failedValidations.add(fieldMap);
        }
    }

    public static void checkFieldLength(String value, String fieldName, int maxLength,
            List<ValidationFailure> failedValidations) {
        if (value != null && value.length() > maxLength) {
            ValidationFailure fieldMap = new ValidationFailure(fieldName,
                    MessagesCodes.FIELD_TOO_LONG, maxLength);
            failedValidations.add(fieldMap);
        }
    }

    private ValidationUtils() {
    }
}
