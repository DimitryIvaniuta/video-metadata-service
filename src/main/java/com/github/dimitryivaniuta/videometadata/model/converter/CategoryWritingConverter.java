package com.github.dimitryivaniuta.videometadata.model.converter;

import com.github.dimitryivaniuta.videometadata.model.Role;
import com.github.dimitryivaniuta.videometadata.model.VideoCategory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

/** Writes the Category enum as its name() into the DB VARCHAR. */
@WritingConverter
public class CategoryWritingConverter implements Converter<VideoCategory, Short> {

    @Override
    public Short convert(VideoCategory src) {
        return (short) src.ordinal();
    }

}
