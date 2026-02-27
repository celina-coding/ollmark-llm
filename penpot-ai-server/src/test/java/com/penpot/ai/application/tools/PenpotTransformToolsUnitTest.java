package com.penpot.ai.application.tools;

import com.penpot.ai.application.tools.support.PenpotToolExecutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy; 
import static org.mockito.ArgumentMatchers.anyString;           
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;                           


/**
 * Tests unitaires pour la classe {@link PenpotTransformTools}.
 * <p>
 * Cette classe valide la génération du code JavaScript envoyé au plugin Penpot
 * ainsi que la robustesse des appels à l'exécuteur de tools.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class PenpotTransformToolsUnitTest {

    /**
     * Simulation de l'exécuteur de code Penpot.
     */
    @Mock
    private PenpotToolExecutor toolExecutor;
    /**
     * Injection du mock.
     */
    @InjectMocks
    private PenpotTransformTools penpotTransformTools;


    /**
     * Teste le succès de la modification de taille (resize) avec des dimensions valides.
     * <p>
     * Vérifie que le code JS contient l'appel à getShapeById et la méthode resize correcte.
     * </p>
     */
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

    /**
     * Teste le mécanisme de fallback sur la sélection courante si l'ID est vide.
     */
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

    /**
     * Vérifie que les dimensions sont formatées avec deux décimales et un point (US Locale).
     * <p>
     * Crucial pour la validité syntaxique du JavaScript généré.
     * </p>
     */
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

    /**
     * Teste la robustesse de la génération de code avec des dimensions négatives.
     */
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

    /**
     * Teste la propagation des exceptions en cas d'échec de l'exécuteur technique.
     */
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

    /**
     * Teste la rotation réussie d'une forme avec un angle positif.
     * <p>
     * Vérifie que le code JS généré récupère la forme par son ID et 
     * incrémente correctement la propriété rotation.
     * </p>
     */
    @Test 
    public void shouldBeSuccessfulWhenRotatingShape(){
        //GIVEN 
        String shapeId = "rect-456";
        int angle = 45;
        ArgumentCaptor<String> jsCodeCaptor = ArgumentCaptor.forClass(String.class);
        //WHEN 
        penpotTransformTools.rotateShape(shapeId, angle);

        //THEN
        verify(toolExecutor, times(1)).transformShape(
            jsCodeCaptor.capture(),
            eq("rotated"),
            eq(shapeId)
        );

        String capturedJsCode = jsCodeCaptor.getValue();
        assertThat(capturedJsCode)
            .contains("penpot.currentPage.getShapeById('rect-456')")
            .contains("shape.rotation = (shape.rotation || 0) + 45;")
            .contains("return { id: shape.id, rotation: shape.rotation };");

    }
    /**
     * Teste le mécanisme de repli sur la sélection si l'ID de la forme est vide.
     * <p>
     * Garantit que l'outil peut fonctionner sur l'objet actuellement sélectionné 
     * dans l'interface Penpot si aucun identifiant n'est fourni.
     * </p>
     */
    @Test
    public void shouldFallbackToSelectionWhenShapeIdIsEmptyForRotate() {
        // GIVEN
        String emptyId = "";
        int angle = 90;
        ArgumentCaptor<String> jsCodeCaptor = ArgumentCaptor.forClass(String.class);

        // WHEN
        penpotTransformTools.rotateShape(emptyId, angle);

        // THEN
        verify(toolExecutor).transformShape(jsCodeCaptor.capture(), eq("rotated"), eq(""));
        
        assertThat(jsCodeCaptor.getValue())
            .contains("if (penpot.selection.length > 0) shape = penpot.selection[0];")
            .contains("shape.rotation = (shape.rotation || 0) + 90;");
    }  

    /**
     * Teste la rotation avec un angle négatif (sens anti-horaire).
     * <p>
     * Vérifie que le signe négatif est correctement traité dans la chaîne 
     * de caractères du code JavaScript généré.
     * </p>
     */
    @Test
    public void shouldHandleNegativeAngleWhenRotating() {
        // GIVEN
        String shapeId = "rect-789";
        int negativeAngle = -45;
        ArgumentCaptor<String> jsCodeCaptor = ArgumentCaptor.forClass(String.class);

        // WHEN
        penpotTransformTools.rotateShape(shapeId, negativeAngle);

        // THEN
        verify(toolExecutor).transformShape(
            jsCodeCaptor.capture(),
            eq("rotated"),
            eq(shapeId)
        );

        String capturedJsCode = jsCodeCaptor.getValue();
        assertThat(capturedJsCode).contains("shape.rotation = (shape.rotation || 0) + -45;");
    }

    /**
     * Teste la rotation avec un angle de zéro degré.
     * <p>
     * Cas limite vérifiant que le générateur de code produit un script valide 
     * même si l'action n'entraîne aucune modification visuelle.
     * </p>
     */
     @Test
    public void shouldGenerateCorrectJsWhenAngleIsZero() {
        // GIVEN
        String shapeId = "rect-zero";
        int angle = 0;
        ArgumentCaptor<String> jsCodeCaptor = ArgumentCaptor.forClass(String.class);

        // WHEN
        penpotTransformTools.rotateShape(shapeId, angle);

        // THEN
        verify(toolExecutor).transformShape(jsCodeCaptor.capture(), eq("rotated"), eq(shapeId));
        assertThat(jsCodeCaptor.getValue())
            .contains("shape.rotation = (shape.rotation || 0) + 0;");
    }

    /**
     * Teste la propagation des exceptions lors d'un échec de l'exécuteur pour la rotation.
     * <p>
     * Assure la robustesse du système en vérifiant que les erreurs techniques 
     * ne sont pas étouffées par le tool.
     * </p>
     */
     @Test
    public void shouldThrowExceptionWhenExecutorFailsForRotate() {
        // GIVEN
        when(toolExecutor.transformShape(anyString(), eq("rotated"), anyString()))
            .thenThrow(new RuntimeException("Rotation failed"));

        // THEN
        assertThatThrownBy(() -> {
         // WHEN
        penpotTransformTools.rotateShape("any-id", 30);
        }).isInstanceOf(RuntimeException.class)
          .hasMessage("Rotation failed");
    } 

}