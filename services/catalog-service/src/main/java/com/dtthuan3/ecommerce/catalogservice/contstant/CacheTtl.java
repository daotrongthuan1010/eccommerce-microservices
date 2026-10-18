package com.dtthuan3.ecommerce.catalogservice.contstant;



import java.time.Duration;

public final class CacheTtl {

    private CacheTtl() {
    }

    public static final Duration SHORT =
            Duration.ofSeconds(60);

    public static final Duration MEDIUM =
            Duration.ofMinutes(5);

    public static final Duration LONG =
            Duration.ofMinutes(30);
}