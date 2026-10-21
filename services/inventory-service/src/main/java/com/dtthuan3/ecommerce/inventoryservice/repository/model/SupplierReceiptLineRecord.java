package com.dtthuan3.ecommerce.inventoryservice.repository.model;

import java.math.BigDecimal;

/** Bản ghi đọc/ghi của một dòng phiếu nhập, ánh xạ trực tiếp với các cột cần thiết trong SQL. */
public record SupplierReceiptLineRecord(
        /** UUID của dòng phiếu, do Inventory tạo. */
        String lineId,
        /** Vị trí dòng bắt đầu từ 1, giữ thứ tự request ban đầu. */
        int lineNumber,
        /** Mã SKU đã được xác thực còn hoạt động trước transaction ghi. */
        String skuId,
        /** Đơn vị tính dùng để diễn giải expectedQty. */
        String uom,
        /** Số lượng dự kiến, chưa phải tồn kho thực tế. */
        BigDecimal expectedQty,
        /** Mã lô nhà cung cấp nếu có tại thời điểm tạo phiếu. */
        String lotCode,
        /** Tổng số lượng đã nhận thực tế; 0 trước khi kiểm đếm. */
        BigDecimal actualQty,
        /** Số lượng đã xác định đạt chất lượng. */
        BigDecimal goodQty,
        /** Số lượng đã xác định hỏng. */
        BigDecimal damagedQty,
        /** Số lượng đang chờ kiểm tra chất lượng. */
        BigDecimal inspectionQty,
        /** Location được gán khi ghi sổ; null trước khi phiếu được ghi nhận vào tồn. */
        String locationId
) {}
