package com.penpot.ai.core.domain.spec;

import lombok.*;

@Value
@Builder
public class LayoutSpec {
    String mode;
    String direction;
    String type;
    String hint;
}