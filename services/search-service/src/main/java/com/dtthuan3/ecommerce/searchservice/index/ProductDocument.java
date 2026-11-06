package com.dtthuan3.ecommerce.searchservice.index;

/**
 * Document ES cho 1 san pham PUBLISHED — du lieu den tu product-service
 * (enrich sau khi nhan event Kafka). Index name co version (products_v2) de
 * thay doi mapping = tao index moi + reindex, khong doi index truc tuyen.
 *
 * Bo dau KHONG lam o day: analyzer vi_search da co filter asciifolding ap
 * dung cho CA index lan query → khach go "dien thoai" match "điện thoại".
 * nameNoaccent chi la ban ten de phong (backup field) khi can query truc tiep.
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

    /**
     * Bo dau cho truong du phong nameNoaccent (analyzer da lam viec nay cho
     * cac truong text; truong nay de FE/debug doc nhanh). NFD tach combining
     * marks roi xoa; đ/Đ NFD khong tach nen phai map thu cong.
     */
    static String stripAccents(String input) {
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
