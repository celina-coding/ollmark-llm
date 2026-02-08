package com.penpot.ai.core.domain.spec;

import lombok.*;
import java.util.List;

@Value
@Builder
public class BackgroundSpec {
    String color;
    String texture;
    String pattern;
    List<String> gradient;
    List<String> shapes;
}