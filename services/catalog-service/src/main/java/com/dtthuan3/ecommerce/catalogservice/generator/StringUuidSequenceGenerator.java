package com.dtthuan3.ecommerce.catalogservice.generator;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.Configurable;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;

import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.UUID;

public class StringUuidSequenceGenerator
        implements IdentifierGenerator, Configurable {

    private String sequenceName;

    @Override
    public void configure(
            Type type,
            Properties params,
            ServiceRegistry serviceRegistry
    ) {
        this.sequenceName = params.getProperty(
                "sequence_name",
                "categories_id_seq"
        );
    }

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