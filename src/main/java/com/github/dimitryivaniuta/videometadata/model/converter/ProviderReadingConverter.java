package com.github.dimitryivaniuta.videometadata.model.converter;

import com.github.dimitryivaniuta.videometadata.model.VideoProvider;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class ProviderReadingConverter implements Converter<Short, VideoProvider> {

    @Override
    public VideoProvider convert(@NotNull Short src) {
        return VideoProvider.values()[src];
    }

}
