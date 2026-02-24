package com.penpot.ai.core.domain.spec;

import lombok.*;
import java.util.Map;

@Value
@Builder
public class TypographySpec {
    Map<String, TypeStyle> styles;
}