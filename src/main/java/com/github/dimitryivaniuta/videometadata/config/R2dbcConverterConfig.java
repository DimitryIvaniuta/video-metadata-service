package com.github.dimitryivaniuta.videometadata.config;


import com.github.dimitryivaniuta.videometadata.model.UserStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;

import java.util.List;

@Configuration
public class R2dbcConverterConfig {

    @Bean
    public R2dbcCustomConversions r2dbcCustomConversions() {
        return R2dbcCustomConversions.of(
                R2dbcCustomConversions.StoreConversions.NONE,  // Boot will detect the right dialect
                List.of(new UserStatusReadConverter(), new UserStatusWriteConverter())
        );
    }

    /** Convert DB integer → UserStatus */
    @ReadingConverter
    static class UserStatusReadConverter implements Converter<Integer, UserStatus> {
        @Override
        public UserStatus convert(Integer source) {
            return UserStatus.values()[source];
        }
    }

    /** Convert UserStatus → DB integer */
    @WritingConverter
    static class UserStatusWriteConverter implements Converter<UserStatus, Integer> {
        @Override
        public Integer convert(UserStatus source) {
            return source.ordinal();
        }
    }
}
