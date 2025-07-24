package com.github.dimitryivaniuta.videometadata.config;

import com.github.dimitryivaniuta.videometadata.model.converter.RoleReadingConverter;
import com.github.dimitryivaniuta.videometadata.model.converter.RoleWritingConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.r2dbc.convert.*;
import org.springframework.data.r2dbc.dialect.PostgresDialect;
import java.util.List;

@Configuration
public class R2dbcConfig {

    @Bean
    public R2dbcCustomConversions r2dbcCustomConversions() {
        // pick the Postgres dialect (adjust if you use another)
        var dialect = PostgresDialect.INSTANCE;
        var storeConversions = CustomConversions.StoreConversions.of(dialect.getSimpleTypeHolder());

        List<Converter<?, ?>> converters = List.of(
                new RoleReadingConverter(),
                new RoleWritingConverter()
        );

        return new R2dbcCustomConversions(storeConversions, converters);
    }
}