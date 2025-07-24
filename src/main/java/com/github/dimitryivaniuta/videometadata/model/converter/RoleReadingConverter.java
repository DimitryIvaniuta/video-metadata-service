package com.github.dimitryivaniuta.videometadata.model.converter;

import com.github.dimitryivaniuta.videometadata.model.Role;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;

/** Reads the DB VARCHAR and turns it into a Role enum. */
@ReadingConverter
public class RoleReadingConverter implements Converter<String, Role> {
    @Override
    public Role convert(@NotNull String source) {
        return Role.valueOf(source);
    }
}