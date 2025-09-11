package tiameds.com.tiameds.utils;


import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

import java.io.Serializable;

public class SnowflakeIdentifierGenerator implements IdentifierGenerator {

    private static final SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1, 1);

    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object object) {
        return generator.nextId();
    }
}