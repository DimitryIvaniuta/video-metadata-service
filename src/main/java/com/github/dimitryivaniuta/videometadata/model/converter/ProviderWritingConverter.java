package com.github.dimitryivaniuta.videometadata.model.converter;

import com.github.dimitryivaniuta.videometadata.model.VideoProvider;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

/** Writes the Provider enum as its name() into the DB VARCHAR. */
@WritingConverter
public class ProviderWritingConverter implements Converter<VideoProvider, Short> {

    @Override
    public Short convert(VideoProvider src) {
        return (short) src.getCode();
    }

}
