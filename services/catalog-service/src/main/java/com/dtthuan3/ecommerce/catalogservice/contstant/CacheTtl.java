package com.dtthuan3.ecommerce.catalogservice.contstant;

import java.time.Duration;

public final class CacheTtl {

    private CacheTtl() {
    }

    /**
     * Cache ngắn hạn:
     * dữ liệu có thể thay đổi tương đối thường xuyên.
     */

    public static final Duration SHORT =
            Duration.ofMinutes(5);

    /**
     * Cache trung bình:
     * category, danh mục dùng chung.
     */
    public static final Duration MEDIUM =
            Duration.ofMinutes(30);

    /**
     * Cache dài hạn:
     * dữ liệu tham chiếu ít thay đổi.
     */
    public static final Duration LONG =
            Duration.ofHours(2);
}