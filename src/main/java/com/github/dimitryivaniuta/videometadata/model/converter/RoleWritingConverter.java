package com.github.dimitryivaniuta.videometadata.model.converter;

import com.github.dimitryivaniuta.videometadata.model.Role;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

/** Writes the Role enum as its name() into the DB VARCHAR. */
@WritingConverter
public class RoleWritingConverter implements Converter<Role, String> {
    @Override
    public String convert(Role source) {
        return source.name();
    }
}