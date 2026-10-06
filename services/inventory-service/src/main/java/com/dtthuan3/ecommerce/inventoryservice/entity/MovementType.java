package com.dtthuan3.ecommerce.inventoryservice.entity;


public enum MovementType {

    /** Hàng được nhập vào tồn vật lý. */
    RECEIVE,

    /** Hàng khả dụng được giữ trước cho một đơn/nghiệp vụ. */
    RESERVE,

    /** Hàng đã giữ được trả lại tồn khả dụng. */
    RELEASE,

    /** Hàng được xuất khỏi tồn vật lý. */
    ISSUE,

    /** Tồn được sửa theo kết quả kiểm kê. */
    ADJUSTMENT,

    /** Ghi nhận phần hàng rời khỏi inventory nguồn trong giao dịch chuyển kho. */
    TRANSFER_OUT,

    /** Ghi nhận phần hàng đi vào inventory đích trong giao dịch chuyển kho. */
    TRANSFER_IN
}
