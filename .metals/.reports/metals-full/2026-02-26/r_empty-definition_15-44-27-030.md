error id: file://<HOME>/Bureau/ollmark-poc-llm%20/penpot-ai-server/src/test/java/com/penpot/ai/application/tools/PenpotTransformToolsUnitTest.java:org/mockito/ArgumentMatchers#
file://<HOME>/Bureau/ollmark-poc-llm%20/penpot-ai-server/src/test/java/com/penpot/ai/application/tools/PenpotTransformToolsUnitTest.java
empty definition using pc, found symbol in pc: org/mockito/ArgumentMatchers#
empty definition using semanticdb
empty definition using fallback
non-local guesses:

offset: 600
uri: file://<HOME>/Bureau/ollmark-poc-llm%20/penpot-ai-server/src/test/java/com/penpot/ai/application/tools/PenpotTransformToolsUnitTest.java
text:
```scala
package com.penpot.ai.application.tools;

import com.penpot.ai.application.tools.support.PenpotToolExecutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// Imports statiques manquants
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy; 
import static org.mockito.ArgumentMatchers.anyString;           
import static org.mockito.ArgumentMatche@@rs.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;                           

@ExtendWith(MockitoExtension.class)
class PenpotTransformToolsUnitTest {

    @Mock
    private PenpotToolExecutor toolExecutor;

    @InjectMocks
    private PenpotTransformTools penpotTransformTools;

   
    @Test
    public void shouldBeSuccessfulWhenResizingShapeWithValidDimensions() {
        // GIVEN
        String shapeId = "rect-123";
        float newWidth = 800.0f;
        float newHeight = 600.0f;
        ArgumentCaptor<String> jsCodeCaptor = ArgumentCaptor.forClass(String.class);

        // WHEN
        penpotTransformTools.resizeShape(shapeId, newWidth, newHeight);

        // THEN
     
        verify(toolExecutor, times(1)).transformShape(
            jsCodeCaptor.capture(),
            eq("resized"),
            eq(shapeId)
        );

        String capturedJsCode = jsCodeCaptor.getValue();
        assertThat(capturedJsCode)
            .contains("penpot.selection[0]")
            .contains("penpot.currentPage.getShapeById('rect-123')") 
            .contains("shape.resize(800.00, 600.00);")
            .contains("return { id: shape.id, width: shape.width, height: shape.height };");
    }


    @Test
    public void shouldFallbackToSelectionWhenShapeIdIsEmptyForResize() {
        // GIVEN
        String emptyShapeId = "";
        float newWidth = 400.0f;
        float newHeight = 300.0f;
        ArgumentCaptor<String> jsCodeCaptor = ArgumentCaptor.forClass(String.class);

        // WHEN
        penpotTransformTools.resizeShape(emptyShapeId, newWidth, newHeight);

        // THEN
        verify(toolExecutor, times(1)).transformShape(
            jsCodeCaptor.capture(),
            eq("resized"),
            eq(emptyShapeId)
        );

       String capturedJsCode = jsCodeCaptor.getValue();
        assertThat(capturedJsCode)
            .contains("penpot.currentPage.getShapeById('')") 
            .contains("if (penpot.selection.length > 0) shape = penpot.selection[0];") 
            .contains("shape.resize(400.00, 300.00);")
            .contains("return { id: shape.id, width: shape.width, height: shape.height };");
    }

    @Test
     public void shouldFormatDimensionsWithTwoDecimals() {
        // GIVEN
        String shapeId = "ellipse-456";
        float newWidth = 123.4567f;
        float newHeight = 89.1234f;
        ArgumentCaptor<String> jsCodeCaptor = ArgumentCaptor.forClass(String.class);

        // WHEN
        penpotTransformTools.resizeShape(shapeId, newWidth, newHeight);

        // THEN
        verify(toolExecutor, times(1)).transformShape(
            jsCodeCaptor.capture(),
            eq("resized"),
            eq(shapeId)
        );

        String capturedJsCode = jsCodeCaptor.getValue();
        assertThat(capturedJsCode)
            .contains("shape.resize(123.46, 89.12);");
     }

    @Test
    public void shouldGenerateJsEvenWithNegativeDimensions() {
        // GIVEN
        String shapeId = "rect-negative";
        float negativeWidth = -50.0f;
        float height = 200.0f;
        ArgumentCaptor<String> jsCodeCaptor = ArgumentCaptor.forClass(String.class);

        // WHEN
        penpotTransformTools.resizeShape(shapeId, negativeWidth, height);

        // THEN
        verify(toolExecutor).transformShape(jsCodeCaptor.capture(), eq("resized"), eq(shapeId));
        assertThat(jsCodeCaptor.getValue())
            .contains("shape.resize(-50.00, 200.00);"); 
    }
    
    @Test
    public void shouldThrowExceptionWhenExecutorFails() {
        // GIVEN
        when(toolExecutor.transformShape(anyString(), eq("resized"), anyString()))
            .thenThrow(new RuntimeException("Connection failed"));

        // THEN
        assertThatThrownBy(() -> {
            // WHEN
            penpotTransformTools.resizeShape("id", 100, 100);
        }).isInstanceOf(RuntimeException.class)
        .hasMessage("Connection failed");
    }


}
```


#### Short summary: 

empty definition using pc, found symbol in pc: org/mockito/ArgumentMatchers#