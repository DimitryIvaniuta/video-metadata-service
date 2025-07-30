package com.github.dimitryivaniuta.videometadata.model.converter;

import com.github.dimitryivaniuta.videometadata.model.VideoCategory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class CategoryReadingConverter implements Converter<Short, VideoCategory> {

    @Override
    public VideoCategory convert(Short src) {
        return (src < 0 || src >= VideoCategory.values().length)
                ? VideoCategory.UNSPECIFIED
                : VideoCategory.values()[src];
    }

}
