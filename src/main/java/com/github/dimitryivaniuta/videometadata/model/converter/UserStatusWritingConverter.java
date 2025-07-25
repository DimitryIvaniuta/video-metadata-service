package com.github.dimitryivaniuta.videometadata.model.converter;

import com.github.dimitryivaniuta.videometadata.model.UserStatus;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.*;

@WritingConverter
public class UserStatusWritingConverter implements Converter<UserStatus, Short> {
    @Override
    public Short convert(UserStatus source) {
        return source.getCode();
    }
}
