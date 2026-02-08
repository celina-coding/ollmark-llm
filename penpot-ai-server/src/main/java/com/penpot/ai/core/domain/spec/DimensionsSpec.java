package com.penpot.ai.core.domain.spec;

import lombok.*;

@Value
@Builder
public class DimensionsSpec {
    int width;
    int height;
    String canvasSize;
    String format;
    String source;
}