package com.dtthuan3.ecommerce.productservice.mapper;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

import java.security.SecureRandom;
import java.util.UUID;

public class UuidV7Generator implements IdentifierGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public UUID generate(
            SharedSessionContractImplementor session,
            Object object
    ) {
        long timestamp = System.currentTimeMillis();

        long mostSigBits = (timestamp & 0xFFFFFFFFFFFFL) << 16;
        mostSigBits |= 0x7000L;
        mostSigBits |= RANDOM.nextInt(1 << 12);

        long leastSigBits = RANDOM.nextLong();
        leastSigBits &= 0x3FFFFFFFFFFFFFFFL;
        leastSigBits |= 0x8000000000000000L;

        return new UUID(mostSigBits, leastSigBits);
    }
}