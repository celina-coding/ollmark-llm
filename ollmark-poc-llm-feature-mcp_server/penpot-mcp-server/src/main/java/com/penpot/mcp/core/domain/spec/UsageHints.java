package com.penpot.mcp.core.domain.spec;

import lombok.*;
import java.util.List;

@Value
@Builder
public class UsageHints {
    String typeHint;
    String layoutHint;
    List<String> creationOrder;
}