package com.github.dimitryivaniuta.videometadata.web.dto.imports;

import java.util.List;

public record VimeoVideoPage(List<ExternalVimeoResponse> data, Paging paging) {}