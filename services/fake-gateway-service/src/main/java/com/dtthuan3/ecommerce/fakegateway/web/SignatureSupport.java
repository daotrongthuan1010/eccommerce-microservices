package com.dtthuan3.ecommerce.fakegateway.web;

import com.dtthuan3.ecommerce.fakegateway.util.FakeHmac;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Verify chu ky request den fake-gateway (payment/shipping ky bang secret tuong ung). */
@Component
public class SignatureSupport {

    public void requireValid(String secret, String canonical, String timestampHeader, String signature) {
        long ts;
        try {
            ts = Long.parseLong(timestampHeader);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing/invalid X-Timestamp");
        }
        if (Math.abs(Instant.now().getEpochSecond() - ts) > 300) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Timestamp outside tolerance (replay?)");
        }
        if (!FakeHmac.verify(secret, canonical, signature)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid signature");
        }
    }
}
