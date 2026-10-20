package com.dtthuan3.ecommerce.catalogservice.service;

public final class CacheKeys {

    private CacheKeys() {
    }

    public static final String PREFIX = "catalog:";

    // =========================================================
    // CATEGORY
    // =========================================================

    public static final String CATEGORY =
            PREFIX + "category:";

    public static final String CATEGORY_CHILDREN =
            PREFIX + "category:children:";

    public static final String CATEGORY_ROOTS =
            PREFIX + "category:roots";

    public static final String CATEGORY_TREE =
            PREFIX + "category:tree";

    public static final String CATEGORY_PATTERN =
            PREFIX + "category:*";


    // =========================================================
    // BRANDS
    // =========================================================

    public static final String BRANDS =
            PREFIX + "brands:";

    public static final String BRANDS_LIST =
            PREFIX + "brands:list";

    public static final String BRANDS_ACTIVE =
            PREFIX + "brands:active";

    public static final String BRANDS_SEARCH =
            PREFIX + "brands:search:";

    public static final String BRANDS_PATTERN =
            PREFIX + "brands:*";


    // =========================================================
    // MANUFACTURER
    // =========================================================

    public static final String MANUFACTURER =
            PREFIX + "manufacturer:";

    public static final String MANUFACTURER_LIST =
            PREFIX + "manufacturer:list";

    public static final String MANUFACTURER_ACTIVE =
            PREFIX + "manufacturer:active";

    public static final String MANUFACTURER_SEARCH =
            PREFIX + "manufacturer:search:";

    public static final String MANUFACTURER_PATTERN =
            PREFIX + "manufacturer:*";


    // =========================================================
    // COLLECTION
    // =========================================================

    public static final String COLLECTION =
            PREFIX + "collection:";

    public static final String COLLECTION_LIST =
            PREFIX + "collection:list:";

    public static final String COLLECTION_ACTIVE =
            PREFIX + "collection:active";

    public static final String COLLECTION_HOMEPAGE =
            PREFIX + "collection:homepage";

    public static final String COLLECTION_PATTERN =
            PREFIX + "collection:*";


    // =========================================================
    // DISPLAY RULE
    // =========================================================

    public static final String DISPLAY_RULE =
            PREFIX + "display-rule:";

    public static final String DISPLAY_RULE_COLLECTION =
            PREFIX + "display-rule:collection:";

    public static final String DISPLAY_RULE_ACTIVE =
            PREFIX + "display-rule:active:";

    public static final String DISPLAY_RULE_PATTERN =
            PREFIX + "display-rule:*";


    // =========================================================
    // SYNONYM SET
    // =========================================================

    public static final String SYNONYM_SET =
            PREFIX + "synonym-set:";

    public static final String SYNONYM_SET_LIST =
            PREFIX + "synonym-set:list";

    public static final String SYNONYM_SET_ACTIVE =
            PREFIX + "synonym-set:active";

    public static final String SYNONYM_SET_PATTERN =
            PREFIX + "synonym-set:*";


    // =========================================================
    // SYNONYM
    // =========================================================

    public static final String SYNONYM =
            PREFIX + "synonym:";

    public static final String SYNONYM_LIST =
            PREFIX + "synonym:list:";

    public static final String SYNONYM_PATTERN =
            PREFIX + "synonym:*";
}