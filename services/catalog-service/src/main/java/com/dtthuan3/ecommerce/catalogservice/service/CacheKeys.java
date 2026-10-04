package com.dtthuan3.ecommerce.catalogservice.service;


public final class CacheKeys {

    private CacheKeys() {
    }

    public static final String PREFIX = "catalog:";

    // Category
    public static final String CATEGORY =
            PREFIX + "category:";

    public static final String CATEGORY_ROOTS =
            PREFIX + "category:roots";

    public static final String CATEGORY_CHILDREN =
            PREFIX + "category:children:";

    public static final String CATEGORY_TREE =
            PREFIX + "category:tree";

    public static final String CATEGORY_PATTERN =
            PREFIX + "category:*";

    public static final String BRAND =
            PREFIX + "brand:";

    public static final String BRAND_PATTERN =
            PREFIX + "brand:*";
}