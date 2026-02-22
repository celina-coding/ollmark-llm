package com.penpot.ai.application.tools;

import com.penpot.ai.application.tools.support.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.*;
import org.springframework.stereotype.Component;

/**
 * Tools pour la création de contenu marketing dans Penpot.
 *
 * <p>La génération de texte réutilise {@link PenpotJsSnippets#createText} — élimine la duplication
 * avec {@code PenpotShapeTools}.</p>
 * <p>L'exécution est déléguée à {@link PenpotToolExecutor#createContent}.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PenpotContentTools {

    private final PenpotToolExecutor toolExecutor;

    private static final int H1_SIZE    = 48;
    private static final int H2_SIZE    = 32;
    private static final int P_SIZE     = 16;
    private static final String BOLD    = "bold";
    private static final String NORMAL  = "normal";

    @Tool(description = "Create a large H1 title.")
    public String createTitle(
        @ToolParam(description = "Text content") String content,
        @ToolParam(description = "X coordinate") Integer x,
        @ToolParam(description = "Y coordinate") Integer y,
        @ToolParam(description = "Hex color", required = false) String color
    ) {
        log.info("Tool called: createTitle");
        return toolExecutor.createContent(
            PenpotJsSnippets.createText(content, x, y, H1_SIZE, BOLD, color, "Title"),
            "title"
        );
    }

    @Tool(description = "Create a medium H2 subtitle.")
    public String createSubtitle(
        @ToolParam(description = "Text content") String content,
        @ToolParam(description = "X coordinate") Integer x,
        @ToolParam(description = "Y coordinate") Integer y,
        @ToolParam(description = "Hex color", required = false) String color
    ) {
        log.info("Tool called: createSubtitle");
        return toolExecutor.createContent(
            PenpotJsSnippets.createText(content, x, y, H2_SIZE, BOLD, color, "Subtitle"),
            "subtitle"
        );
    }

    @Tool(description = "Create a standard text paragraph.")
    public String createParagraph(
        @ToolParam(description = "Text content") String content,
        @ToolParam(description = "X coordinate") Integer x,
        @ToolParam(description = "Y coordinate") Integer y,
        @ToolParam(description = "Hex color", required = false) String color
    ) {
        log.info("Tool called: createParagraph");
        return toolExecutor.createContent(
            PenpotJsSnippets.createText(content, x, y, P_SIZE, NORMAL, color, "Paragraph"),
            "paragraph"
        );
    }

    @Tool(description = "Create an image from a URL.")
    public String createImage(
        @ToolParam(description = "Image URL") String url,
        @ToolParam(description = "X position") Integer x,
        @ToolParam(description = "Y position") Integer y,
        @ToolParam(description = "Width", required = false) Integer width,
        @ToolParam(description = "Height", required = false) Integer height
    ) {
        log.info("Tool called: createImage (url={})", url);
        int w = (width  != null) ? width  : 300;
        int h = (height != null) ? height : 200;

        String code = String.format("""
            const imageData = await penpot.uploadMediaUrl('IA-Upload', '%s');
            const rect = penpot.createRectangle();
            rect.resize(%d, %d);
            rect.x = %d;
            rect.y = %d;
            rect.fills = [{ fillOpacity: 1, fillImage: imageData }];
            return rect.id;
            """, url, w, h, x, y);

        return toolExecutor.createContent(code, "image");
    }
}