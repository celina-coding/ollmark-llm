package com.penpot.ai.core.domain.spec;

import lombok.*;

@Value
@Builder
public class TypeStyle {
    String font;
    String color;
    String size;
    Integer sizePx;
    String textCase;
    String tracking;
    String align;
}