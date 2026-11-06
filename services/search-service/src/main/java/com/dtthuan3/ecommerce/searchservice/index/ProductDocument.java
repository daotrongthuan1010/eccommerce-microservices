package com.dtthuan3.ecommerce.searchservice.index;

/**
 * Document ES cho 1 san pham PUBLISHED — du lieu den tu product-service
 * (enrich sau khi nhan event Kafka). Index name co version (products_v2) de
 * thay doi mapping = tao index moi + reindex, khong doi index truc tuyen.
 *
 * Cac truong tim kiem:
 * - name_noaccent: ten da bo dau (asciifolding) — khach go "dien thoai" van ra.
 * - searchText: name + productCode + shortDescription, analyzer vi (CocCoc).
 */
public record ProductDocument(
        String productId,
        String productCode,
        String name,
        String shortDescription,
        long priceFrom,
        String categoryId,
        String brandId,
        String primaryImageKey,
        int sellableSkuCount,
        String status,
        String nameNoaccent,
        String searchText,
        String updatedAt
) {

    public static final String INDEX = "products_v2";

    public static ProductDocument of(
            String productId, String productCode, String name, String shortDescription,
            long priceFrom, String categoryId, String brandId, String primaryImageKey,
            int sellableSkuCount, String status, String updatedAt
    ) {
        String searchText = String.join(" ",
                nullSafe(name), nullSafe(productCode), nullSafe(shortDescription));
        return new ProductDocument(productId, productCode, name, shortDescription,
                priceFrom, categoryId, brandId, primaryImageKey, sellableSkuCount,
                status, stripAccents(name), searchText, updatedAt);
    }

    /** Bo dau tieng Viet: NFD tach combining marks roi xoa; đ/Đ phai map thu cong. */
    public static String stripAccents(String input) {
        if (input == null) return null;
        String normalized = java.text.Normalizer
                .normalize(input, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return normalized.replace('đ', 'd').replace('Đ', 'D');
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }
}
