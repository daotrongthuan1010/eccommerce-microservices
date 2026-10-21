package com.dtthuan3.ecommerce.reviewservice.config;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

public class StringSequenceGenerator implements IdentifierGenerator {
    @Override
    public Object generate(
            SharedSessionContractImplementor session,
            Object object
    ) {
        String sequenceName = (String) session
                .getFactory()
                .getProperties()
                .get("app.id.sequence");

        Object result = session
                .createNativeQuery(
                        "SELECT nextval('" + sequenceName + "')",
                        Long.class
                )
                .getSingleResult();

        return result.toString();
    }
}
