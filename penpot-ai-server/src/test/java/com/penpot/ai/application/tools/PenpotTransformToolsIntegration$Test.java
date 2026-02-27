package com.penpot.ai.application.tools;

import com.penpot.ai.application.tools.support.PenpotToolExecutor;
import com.penpot.ai.core.domain.TaskResult;
import com.penpot.ai.core.ports.in.ExecuteCodeUseCase;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests d'intégration pour PenpotTransformTools.
 * Note: On utilise @SpringBootTest avec les classes spécifiques pour éviter de charger 
 * tout le contexte (IA, base de données) qui fait échouer le build.
 */
@SpringBootTest(classes = {PenpotTransformTools.class, PenpotToolExecutor.class})
@ActiveProfiles("test")
@DisplayName("PenpotTransformTools — Integration")
class PenpotTransformToolsIntegrationTest {

    @MockitoBean
    private ExecuteCodeUseCase executeCodeUseCase;

    @Autowired
    private PenpotTransformTools penpotTransformTools;

    private static final String SHAPE_ID = "shape-integration-001";

    @BeforeEach
    void resetMocks() {
        reset(executeCodeUseCase);
    }

    // =========================================================================
    // rotateShape
    // =========================================================================

    @Nested
    @DisplayName("rotateShape")
    class RotateShapeIntegrationTests {

        @Test
        @DisplayName("rotateShape — success with positive angle")
        void rotateShape_success() {
            when(executeCodeUseCase.execute(any()))
                    .thenReturn(TaskResult.success("{\"id\": \"" + SHAPE_ID + "\", \"rotation\": 45}"));

            String result = penpotTransformTools.rotateShape(SHAPE_ID, 45);

            assertThat(result).contains("\"success\": true").contains("rotated");
        }

        @Test
        @DisplayName("rotateShape — fallback to selection when ID is empty")
        void rotateShape_fallbackToSelection() {
            when(executeCodeUseCase.execute(any()))
                    .thenReturn(TaskResult.success("{\"id\": \"selected-id\", \"rotation\": 90}"));

            String result = penpotTransformTools.rotateShape("", 90);

            assertThat(result).contains("\"success\": true");
            verify(executeCodeUseCase).execute(argThat(cmd -> cmd.getCode().contains("penpot.selection[0]")));
        }

        @Test
        @DisplayName("rotateShape — handles negative angles")
        void rotateShape_negativeAngle() {
            when(executeCodeUseCase.execute(any()))
                    .thenReturn(TaskResult.success("{}"));

            penpotTransformTools.rotateShape(SHAPE_ID, -45);

            verify(executeCodeUseCase).execute(argThat(cmd -> cmd.getCode().contains("-45")));
        }
    }

    // =========================================================================
    // scaleShape
    // =========================================================================

    @Nested
    @DisplayName("scaleShape")
    class ScaleShapeIntegrationTests {

        @Test
        @DisplayName("scaleShape — success with factors")
        void scaleShape_success() {
            when(executeCodeUseCase.execute(any()))
                    .thenReturn(TaskResult.success("{\"id\": \"" + SHAPE_ID + "\"}"));

            String result = penpotTransformTools.scaleShape(SHAPE_ID, 2.0f, 0.5f);

            assertThat(result).contains("\"success\": true").contains("scaled");
        }

        @Test
        @DisplayName("scaleShape — handles negative scale (mirroring)")
        void scaleShape_negativeScale() {
            when(executeCodeUseCase.execute(any()))
                    .thenReturn(TaskResult.success("{}"));

            penpotTransformTools.scaleShape(SHAPE_ID, -1.0f, 1.0f);

            verify(executeCodeUseCase).execute(argThat(cmd -> cmd.getCode().contains("-1.00")));
        }
    }

    // =========================================================================
    // moveShape
    // =========================================================================

    @Nested
    @DisplayName("moveShape")
    class MoveShapeIntegrationTests {

        @Test
        @DisplayName("moveShape — absolute move (relative=false)")
        void moveShape_absolute() {
            when(executeCodeUseCase.execute(any()))
                    .thenReturn(TaskResult.success("{\"x\": 100, \"y\": 100}"));

            String result = penpotTransformTools.moveShape(SHAPE_ID, 100f, 100f, false);

            assertThat(result).contains("\"success\": true");
            // Vérifie que le code généré est bien une assignation directe (pas de addition)
            verify(executeCodeUseCase).execute(argThat(cmd -> 
                cmd.getCode().contains("shape.x = 100.00") && !cmd.getCode().contains("+")));
        }

        @Test
        @DisplayName("moveShape — relative move (relative=true)")
        void moveShape_relative() {
            when(executeCodeUseCase.execute(any()))
                    .thenReturn(TaskResult.success("{}"));

            penpotTransformTools.moveShape(SHAPE_ID, 10f, 10f, true);

            verify(executeCodeUseCase).execute(argThat(cmd -> cmd.getCode().contains("(shape.x || 0) + 10.00")));
        }

        @Test
        @DisplayName("moveShape — defaults to absolute when relative is null")
        void moveShape_relativeNull() {
            when(executeCodeUseCase.execute(any()))
                    .thenReturn(TaskResult.success("{}"));

            penpotTransformTools.moveShape(SHAPE_ID, 50f, 50f, null);

            verify(executeCodeUseCase).execute(argThat(cmd -> cmd.getCode().contains("shape.x = 50.00")));
        }
    }

    // =========================================================================
    // resizeShape
    // =========================================================================

    @Nested
    @DisplayName("resizeShape")
    class ResizeShapeIntegrationTests {

        @Test
        @DisplayName("resizeShape — formats with two decimals (US Locale check)")
        void resizeShape_formatting() {
            when(executeCodeUseCase.execute(any()))
                    .thenReturn(TaskResult.success("{}"));

            penpotTransformTools.resizeShape(SHAPE_ID, 123.456f, 789.012f);

            // On vérifie que le point est utilisé (pas la virgule) et l'arrondi à .46
            verify(executeCodeUseCase).execute(argThat(cmd -> cmd.getCode().contains("123.46")));
        }

        @Test
        @DisplayName("resizeShape — handles failure from executeCodeUseCase")
        void resizeShape_failure() {
            when(executeCodeUseCase.execute(any()))
                    .thenReturn(TaskResult.failure("Penpot connection timeout"));

            String result = penpotTransformTools.resizeShape(SHAPE_ID, 100f, 100f);

            assertThat(result).contains("\"success\": false")
                             .contains("Penpot connection timeout");
        }
    }
}