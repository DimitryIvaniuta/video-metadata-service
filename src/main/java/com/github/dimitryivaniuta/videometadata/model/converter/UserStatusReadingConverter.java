package com.github.dimitryivaniuta.videometadata.model.converter;

import com.github.dimitryivaniuta.videometadata.model.UserStatus;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.*;

@ReadingConverter
public class UserStatusReadingConverter implements Converter<Short, UserStatus> {
    @Override
    public UserStatus convert(Short source) {
        return UserStatus.fromCode(source);
    }
}