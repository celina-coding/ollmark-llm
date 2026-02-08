package com.penpot.ai.core.domain.spec;

import lombok.*;
import java.util.Map;
import com.penpot.ai.core.domain.spec.TypeStyle;

@Value
@Builder
public class TypographySpec {
    Map<String, TypeStyle> styles;
}