package com.dtthuan3.ecommerce.catalogservice.generator;


import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class StringUuidSequenceGenerator implements IdentifierGenerator {

    private final String sequenceName = "categories_id_seq";

    @Override
    public Object generate(
            SharedSessionContractImplementor session,
            Object object
    ) {

        Number sequenceValue = (Number) session
                .createNativeQuery(
                        "SELECT nextval('" + sequenceName + "')"
                )
                .getSingleResult();

        return UUID.nameUUIDFromBytes(
                (sequenceName + ":" + sequenceValue.longValue())
                        .getBytes(StandardCharsets.UTF_8)
        ).toString();
    }
}