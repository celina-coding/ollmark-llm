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

    /**
     * Teste le succès du scale avec des facteurs positifs.
     * <p>
     * Vérifie que le code JavaScript généré calcule correctement les nouvelles dimensions
     * en multipliant la largeur et la hauteur actuelles par les facteurs fournis, 
     * tout en gérant les valeurs par défaut (fallback à 1 si non défini).
     * </p>
     */
    @Test
    public void shouldBeSuccessfulWhenScalingShape() {
        // GIVEN
        String shapeId = "rect-scale";
        float scaleX = 2.0f; 
        float scaleY = 0.5f; 
        ArgumentCaptor<String> jsCodeCaptor = ArgumentCaptor.forClass(String.class);

        // WHEN
        penpotTransformTools.scaleShape(shapeId, scaleX, scaleY);

        // THEN
        verify(toolExecutor, times(1)).transformShape(
            jsCodeCaptor.capture(),
            eq("scaled"),
            eq(shapeId)
        );

        String capturedJsCode = jsCodeCaptor.getValue();
        assertThat(capturedJsCode)
            .contains("const currentWidth = shape.width || 1;")
            .contains("const currentHeight = shape.height || 1;")
            .contains("shape.resize(currentWidth * 2.00, currentHeight * 0.50);")
            .contains("return { id: shape.id, width: shape.width, height: shape.height };");
    }

    /**
     * Teste le repli sur la sélection courante si l'identifiant de la forme est vide.
     * <p>
     * Garantit que l'outil de mise à l'échelle peut s'appliquer sur l'objet sélectionné 
     * dans l'interface Penpot si aucun ID n'est fourni par l'IA.
     * </p>
     */
    @Test
    public void shouldFallbackToSelectionWhenShapeIdIsEmptyForScale() {
        // GIVEN
        String emptyId = "";
        float sX = 1.5f;
        float sY = 1.5f;
        ArgumentCaptor<String> jsCodeCaptor = ArgumentCaptor.forClass(String.class);

        // WHEN
        penpotTransformTools.scaleShape(emptyId, sX, sY);

        // THEN
        verify(toolExecutor).transformShape(jsCodeCaptor.capture(), eq("scaled"), eq(""));
        assertThat(jsCodeCaptor.getValue())
            .contains("if (penpot.selection.length > 0) shape = penpot.selection[0];")
            .contains("shape.resize(currentWidth * 1.50, currentHeight * 1.50);");
    }

    /**
     * Teste la robustesse de la génération de code face à des facteurs d'échelle négatifs.
     * <p>
     * Vérifie que le formateur traite correctement les signes négatifs, permettant ainsi
     * des transformations de type "miroir" via l'API JavaScript de Penpot.
     * </p>
     */
    @Test
    public void shouldHandleNegativeScaleFactors() {
        // GIVEN
        String shapeId = "id-neg";
        float scaleX = -1.0f;
        float scaleY = 1.0f;
        ArgumentCaptor<String> jsCodeCaptor = ArgumentCaptor.forClass(String.class);

        // WHEN
        penpotTransformTools.scaleShape(shapeId, scaleX, scaleY);

        // THEN
        verify(toolExecutor).transformShape(jsCodeCaptor.capture(), eq("scaled"), eq(shapeId));
        assertThat(jsCodeCaptor.getValue())
            .contains("shape.resize(currentWidth * -1.00, currentHeight * 1.00);");
    }

    /**
     * Teste la propagation des exceptions lors d'un échec technique de l'exécuteur.
     * <p>
     * Assure que si une erreur survient lors de la communication avec le plugin,
     * l'exception est correctement levée pour informer l'appelant.
     * </p>
     */
    @Test
    public void shouldThrowExceptionWhenExecutorFailsForScale() {
        // GIVEN
        when(toolExecutor.transformShape(anyString(), eq("scaled"), anyString()))
            .thenThrow(new RuntimeException("Scaling error"));

        // THEN
        assertThatThrownBy(() -> {
            // WHEN
            penpotTransformTools.scaleShape("id", 2.0f, 2.0f);
        }).isInstanceOf(RuntimeException.class)
          .hasMessage("Scaling error");
    }

    /**
     * Teste le déplacement absolu d'une forme.
     * <p>
     * Vérifie que lorsque 'relative' est false (ou null), le code JS 
     * assigne directement les coordonnées x et y.
     * </p>
     */
    @Test
    public void shouldBeSuccessfulWhenMovingShapeAbsolutely() {
        // GIVEN
        String shapeId = "rect-move";
        float newX = 150.0f;
        float newY = 200.0f;
        Boolean relative = false;
        ArgumentCaptor<String> jsCodeCaptor = ArgumentCaptor.forClass(String.class);

        // WHEN
        penpotTransformTools.moveShape(shapeId, newX, newY, relative);

        // THEN
        verify(toolExecutor).transformShape(jsCodeCaptor.capture(), eq("moved"), eq(shapeId));
        
        String capturedJsCode = jsCodeCaptor.getValue();
        assertThat(capturedJsCode)
            .contains("shape.x = 150.00;")
            .contains("shape.y = 200.00;")
            .contains("return { id: shape.id, x: shape.x, y: shape.y };");
    }

    /**
     * Teste le déplacement relatif d'une forme.
     * <p>
     * Vérifie que lorsque 'relative' est true, le code JS incrémente 
     * les coordonnées actuelles de la forme.
     * </p>
     */
    @Test
    public void shouldBeSuccessfulWhenMovingShapeRelatively() {
        // GIVEN
        String shapeId = "rect-rel";
        float offsetX = 10.0f;
        float offsetY = -5.0f;
        Boolean relative = true;
        ArgumentCaptor<String> jsCodeCaptor = ArgumentCaptor.forClass(String.class);

        // WHEN
        penpotTransformTools.moveShape(shapeId, offsetX, offsetY, relative);

        // THEN
        verify(toolExecutor).transformShape(jsCodeCaptor.capture(), eq("moved"), eq(shapeId));
        
        String capturedJsCode = jsCodeCaptor.getValue();
        assertThat(capturedJsCode)
            .contains("shape.x = (shape.x || 0) + 10.00;")
            .contains("shape.y = (shape.y || 0) + -5.00;");
    }

    /**
     * Teste le comportement par défaut (absolu) si le paramètre 'relative' est null.
     * <p>
     * Couvre la branche 'boolean isRelative = relative != null && relative;' de la méthode.
     * </p>
     */
    @Test
    public void shouldDefaultToAbsoluteMoveWhenRelativeIsNull() {
        // GIVEN
        String shapeId = "rect-null";
        // WHEN
        penpotTransformTools.moveShape(shapeId, 10f, 10f, null);
        // THEN
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(toolExecutor).transformShape(captor.capture(), anyString(), anyString());
        assertThat(captor.getValue()).contains("shape.x = 10.00;"); // Branche absolue
    }

    /**
     * Teste le repli sur la sélection si l'ID est vide pour un déplacement.
     */
    @Test
    public void shouldFallbackToSelectionWhenShapeIdIsEmptyForMove() {
        // GIVEN
        String emptyId = "";
        ArgumentCaptor<String> jsCodeCaptor = ArgumentCaptor.forClass(String.class);

        // WHEN
        penpotTransformTools.moveShape(emptyId, 50f, 50f, false);

        // THEN
        verify(toolExecutor).transformShape(jsCodeCaptor.capture(), eq("moved"), eq(""));
        assertThat(jsCodeCaptor.getValue())
            .contains("if (penpot.selection.length > 0) shape = penpot.selection[0];");
    }

    /**
     * Teste la propagation des exceptions pour le déplacement.
     */
    @Test
    public void shouldThrowExceptionWhenExecutorFailsForMove() {
        // GIVEN
        when(toolExecutor.transformShape(anyString(), eq("moved"), anyString()))
            .thenThrow(new RuntimeException("Move failed"));

        // THEN
        assertThatThrownBy(() -> {
            // WHEN
            penpotTransformTools.moveShape("id", 0, 0, false);
        }).isInstanceOf(RuntimeException.class)
          .hasMessage("Move failed");
    }


}