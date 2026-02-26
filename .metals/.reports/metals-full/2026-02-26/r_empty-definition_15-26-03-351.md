error id: file://<HOME>/Bureau/ollmark-poc-llm%20/penpot-ai-server/src/test/java/com/penpot/ai/application/tools/PenpotTransformToolsUnitTest.java:_empty_/PenpotTransformTools#
file://<HOME>/Bureau/ollmark-poc-llm%20/penpot-ai-server/src/test/java/com/penpot/ai/application/tools/PenpotTransformToolsUnitTest.java
empty definition using pc, found symbol in pc: _empty_/PenpotTransformTools#
empty definition using semanticdb
empty definition using fallback
non-local guesses:

offset: 704
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PenpotTransformToolsUnitTest {

    @Mock
    private PenpotToolExecutor toolExecutor;

    @InjectMocks
    private PenpotTransformTo@@ols penpotTransformTools;

   
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


    
}
```


#### Short summary: 

empty definition using pc, found symbol in pc: _empty_/PenpotTransformTools#