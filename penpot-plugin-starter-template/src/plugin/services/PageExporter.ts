import { Board, Group, Shape } from "@penpot/plugin-types";
import { PageExportResult } from "../types";

export class PageExporter {
    export(page: any): PageExportResult {
        try {
            if (!page) {
                return {
                    success: false,
                    error: 'Aucune page active'
                };
            }

            const pageData = {
                id: page.id,
                name: page.name,
                objects: {}
            };

            const rootShape = page.root;

            if (rootShape && this.hasChildren(rootShape)) {
                rootShape.children.forEach((shape: any) => {
                    this.extractShapes(shape, pageData.objects);
                });
            }

            console.log('Page exportée:', {
                id: pageData.id,
                name: pageData.name,
                objectsCount: Object.keys(pageData.objects).length
            });

            return {
                success: true,
                pageData
            };
        } catch (error) {
            console.error('Erreur lors de l\'export de la page:', error);
            return {
                success: false,
                error: error instanceof Error ? error.message : 'Erreur inconnue'
            };
        }
    }

    private hasChildren(shape: any): shape is Board | Group {
        return 'children' in shape && Array.isArray(shape.children);
    }

    private extractShapes(shape: Shape, objects: any): void {
        objects[shape.id] = {
            id: shape.id,
            type: shape.type,
            name: shape.name,
            x: shape.x,
            y: shape.y,
            width: shape.width,
            height: shape.height,
            fills: shape.fills,
            strokes: shape.strokes,
            opacity: shape.opacity,
            rotation: shape.rotation,
            parentId: shape.parent?.id || null
        };

        if (shape.type === 'text' && 'content' in shape) {
            objects[shape.id].content = (shape as any).content;
        }

        if (this.hasChildren(shape)) {
            objects[shape.id].children = shape.children.map((child: any) => child.id);
            shape.children.forEach((child: any) => this.extractShapes(child, objects));
        }
    }
}