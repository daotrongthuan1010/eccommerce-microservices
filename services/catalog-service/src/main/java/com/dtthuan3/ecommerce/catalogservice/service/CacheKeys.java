package com.dtthuan3.ecommerce.catalogservice.service;


public final class CacheKeys {

    private CacheKeys() {
    }

    public static final String PREFIX = "catalog:";

    // Category
    public static final String CATEGORY = "catalog:category:";

    public static final String CATEGORY_CHILDREN =
            "catalog:category:children:";

    public static final String CATEGORY_ROOTS =
            "catalog:category:roots";

    public static final String CATEGORY_TREE =
            "catalog:category:tree";

    public static final String CATEGORY_PATTERN =
            PREFIX + "category:*";

    public static final String BRANDS =
            "catalog:brands:";

    public static final String BRANDS_LIST =
            "catalog:brands:list";

    public static final String BRANDS_ACTIVE =
            "catalog:brands:active";

    public static final String BRANDS_SEARCH =
            "catalog:brands:search:";

    public static final String MANUFACTURER =
            "catalog:manufacturer:";

    public static final String MANUFACTURER_LIST =
            "catalog:manufacturer:list";

    public static final String MANUFACTURER_ACTIVE =
            "catalog:manufacturer:active";

    public static final String MANUFACTURER_SEARCH =
            "catalog:manufacturer:search:";
}