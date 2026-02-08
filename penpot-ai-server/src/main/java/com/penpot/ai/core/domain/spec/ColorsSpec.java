package com.penpot.ai.core.domain.spec;

import lombok.*;

@Value
@Builder
public class ColorsSpec {
    String primary;
    String secondary;
    String text;
    String background;
}