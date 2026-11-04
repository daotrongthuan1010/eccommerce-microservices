package com.dtthuan3.ecommerce.shippingservice.provider;

import com.dtthuan3.ecommerce.shippingservice.entity.Shipment;

/**
 * Abstraction nha van chuyen. Hom nay la fake GHN; co tai khoan GHN/GHTK that
 * chi them adapter, core shipping va order khong doi.
 */
public interface CarrierProvider {

    String carrierCode();

    /** Tao van don ben carrier, tra ve ma van don (tracking code). */
    String createShipment(Shipment shipment);

    void cancelShipment(String carrierCode);

    /** Kiem chu ky webhook: canonical = carrierCode.status.timestamp */
    boolean verifyWebhookSignature(String carrierCode, String status, long timestamp, String signature);
}
