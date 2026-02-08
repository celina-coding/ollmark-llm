package com.penpot.ai.application.tools;

import com.penpot.ai.core.ports.in.ExecuteCodeUseCase;
import com.penpot.ai.core.domain.*;
import com.penpot.ai.shared.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.*;
import org.springframework.stereotype.Component;

/**
 * Service de création de contenu pour Penpot (Version Minimaliste).
 * 
 * <p>Cette classe génère des scripts JavaScript ultra-légers pour la création 
 * de textes et d'images, en se basant uniquement sur les fonctions natives 
 * de création de l'objet global 'penpot'.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PenpotContentTools {

    private final ExecuteCodeUseCase executeCodeUseCase;

    // Constantes de style
    private static final int H1_SIZE = 48;
    private static final int H2_SIZE = 32;
    private static final int P_SIZE = 16;
    private static final String BOLD = "bold";
    private static final String NORMAL = "normal";

    /**
     * Crée un titre de niveau 1 (H1).
     */
    @Tool(description = "Create a large H1 title.")
    public String createTitle(
        @ToolParam(description = "Text content") String content,
        @ToolParam(description = "X coordinate") Integer x,
        @ToolParam(description = "Y coordinate") Integer y,
        @ToolParam(description = "Hex color", required = false) String color
    ) {
        String code = buildTextCode(content, x, y, H1_SIZE, BOLD, color, "Title");
        return process(code, "title");
    }

    /**
     * Crée un sous-titre de niveau 2 (H2).
     */
    @Tool(description = "Create a medium H2 subtitle.")
    public String createSubtitle(
        @ToolParam(description = "Text content") String content,
        @ToolParam(description = "X coordinate") Integer x,
        @ToolParam(description = "Y coordinate") Integer y,
        @ToolParam(description = "Hex color", required = false) String color
    ) {
        String code = buildTextCode(content, x, y, H2_SIZE, BOLD, color, "Subtitle");
        return process(code, "subtitle");
    }

    /**
     * Crée un paragraphe standard.
     */
    @Tool(description = "Create a standard text paragraph.")
    public String createParagraph(
        @ToolParam(description = "Text content") String content,
        @ToolParam(description = "X coordinate") Integer x,
        @ToolParam(description = "Y coordinate") Integer y,
        @ToolParam(description = "Hex color", required = false) String color
    ) {
        String code = buildTextCode(content, x, y, P_SIZE, NORMAL, color, "Paragraph");
        return process(code, "paragraph");
    }

    /**
     * Importe une image depuis une URL et l'affiche dans un rectangle.
     */
    @Tool(description = "Create an image from a URL.")
    public String createImage(
        @ToolParam(description = "Image URL") String url,
        @ToolParam(description = "X position") Integer x,
        @ToolParam(description = "Y position") Integer y,
        @ToolParam(description = "Width", required = false) Integer width,
        @ToolParam(description = "Height", required = false) Integer height
    ) {
        int w = (width != null) ? width : 300;
        int h = (height != null) ? height : 200;

        StringBuilder code = new StringBuilder();

        code.append(String.format("const imageData = await penpot.uploadMediaUrl('IA-Upload', '%s');\n", url));
        code.append("const rect = penpot.createRectangle();\n");
        code.append(String.format("rect.resize(%d, %d);\n", w, h));
        code.append(String.format("rect.x = %d;\n", x));
        code.append(String.format("rect.y = %d;\n", y));
        code.append("rect.fills = [{ fillOpacity: 1, fillImage: imageData }];\n");
        code.append("return rect.id;");

        return process(code.toString(), "image");
    }

    /**
     * Génère le code JavaScript minimaliste pour un élément texte.
     */
    private String buildTextCode(
        String content, Integer x, Integer y,
        Integer fontSize, String fontWeight, String fillColor, String name
    ) {
        StringBuilder code = new StringBuilder();
        String escapedContent = content.replace("'", "\\'").replace("\n", "\\n");

        code.append(String.format("const text = penpot.createText('%s');\n", escapedContent));
        code.append(String.format("text.x = %d;\n", x));
        code.append(String.format("text.y = %d;\n", y));

        if (fontSize != null) code.append(String.format("text.fontSize = %d;\n", fontSize));
        if (fontWeight != null) code.append(String.format("text.fontWeight = '%s';\n", fontWeight));
        if (fillColor != null) code.append(String.format("text.fills = [{ fillColor: '%s' }];\n", fillColor));
        if (name != null) code.append(String.format("text.name = '%s';\n", name.replace("'", "\\'")));

        code.append("return text.id;\n");
        return code.toString();
    }

    /**
     * Exécute le code généré.
     */
    private String process(String code, String type) {
        try {
            TaskResult result = executeCodeUseCase.execute(ExecuteCodeCommand.of(code));
            if (!result.isSuccess()) {
                return String.format("{\"success\": false, \"error\": \"%s\"}", 
                    result.getError().orElse("Execution failed"));
            }
            String shapeId = result.getData().map(Object::toString).orElse("unknown");
            return String.format("{\"success\": true, \"type\": \"%s\", \"id\": \"%s\"}", 
                type, shapeId);
        } catch (Exception e) {
            return String.format("{\"success\": false, \"error\": \"%s\"}", e.getMessage());
        }
    }
}