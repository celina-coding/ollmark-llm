import { Board, Group, Shape } from "@penpot/plugin-types";
import { PageExportResult } from "../types";

/**
 * Exporte la structure d'une page Penpot sous forme sérialisée.
 */
export class PageExporter {
    /**
     * Exporte la page fournie.
     *
     * @param page Page Penpot active.
     * @returns Résultat de l'export.
     */
    export(page: unknown): PageExportResult {
        if (!page) {
            return { success: false, error: 'Aucune page active' };
        }

        try {
            const pageData = {
                id: (page as any).id,
                name: (page as any).name,
                objects: {} as Record<string, unknown>
            };

            const root = (page as any).root;

            if (this.hasChildren(root)) {
                root.children.forEach(child =>
                    this.extractShapes(child, pageData.objects)
                );
            }

            return { success: true, pageData };
        } catch (error) {
            console.error('Erreur lors de l’export:', error);
            return {
                success: false,
                error: error instanceof Error
                    ? error.message
                    : 'Erreur inconnue'
            };
        }
    }

    private hasChildren(shape: Shape): shape is Board | Group {
        return !!shape && typeof shape === 'object' && 'children' in shape;
    }

    private extractShapes(
        shape: Shape,
        objects: Record<string, unknown>
    ): void {
        objects[shape.id] = {
            id: shape.id,
            type: shape.type,
            name: shape.name,
            x: shape.x,
            y: shape.y,
            width: shape.width,
            height: shape.height,
            opacity: shape.opacity,
            rotation: shape.rotation,
            parentId: shape.parent?.id ?? null
        };

        if (this.hasChildren(shape)) {
            shape.children.forEach(child =>
                this.extractShapes(child, objects)
            );
        }
    }
}