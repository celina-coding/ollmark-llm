package com.penpot.mcp.core.domain;

import com.penpot.mcp.core.domain.spec.*;
import lombok.*;
import java.util.*;

/**
 * Value Object représentant les spécifications complètes d'un template.
 */
@Value
@Builder
public class TemplateSpecs {
    String templateId;
    String type;
    String description;
    List<String> tags;
    DimensionsSpec dimensions;
    LayoutSpec layout;
    BackgroundSpec background;
    TypographySpec typography;
    ColorsSpec colors;
    List<Map<String, Object>> elements;
    List<Map<String, Object>> sections;
    UsageHints usageHints;
}