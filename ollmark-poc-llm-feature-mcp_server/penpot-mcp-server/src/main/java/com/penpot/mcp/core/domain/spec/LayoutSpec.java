package com.penpot.mcp.core.domain.spec;

import lombok.*;

@Value
@Builder
public class LayoutSpec {
    String mode;
    String direction;
    String type;
    String hint;
}