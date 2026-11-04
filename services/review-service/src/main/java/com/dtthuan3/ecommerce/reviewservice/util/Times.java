package com.dtthuan3.ecommerce.reviewservice.util;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public final class Times {

    private Times() {
    }

    public static LocalDateTime parseUtc(String value) {
        try {
            return LocalDateTime.ofInstant(OffsetDateTime.parse(value).toInstant(), ZoneOffset.UTC);
        } catch (DateTimeException e) {
            return LocalDateTime.parse(value);
        }
    }
}
