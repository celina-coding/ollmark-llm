/**
 * Convertit des structures Transit Penpot en objets JavaScript normalisés.
 *
 * Ce parser :
 * - décode les formats Transit spécifiques
 * - remappe les clés compressées
 * - normalise les objets de page pour un usage métier
 */
export class TransitParser {
    /**
     * Table de correspondance Transit → clés normalisées.
     */
    private handlers: Map<string, string> = new Map();

    constructor() {
        this.initializeHandlers();
    }

    /**
     * Initialise les mappings Transit connus.
     */
    private initializeHandlers(): void {
        const mappings: [string, string][] = [
            // Clés de page
            ['^M', 'objects'],
            ['^A', 'id'],
            ['^=', 'name'],

            // Propriétés géométriques
            ['^T', 'width'],
            ['^18', 'height'],
            ['^X', 'rotation'],
            ['^19', 'x1'],
            ['^1:', 'y1'],
            ['^1;', 'x2'],
            ['^1<', 'y2'],

            // Propriétés de forme
            ['^14', 'proportion'],
            ['^P', 'hideFillOnExport'],
            ['^Y', 'proportionLock'],
            ['^[', 'r1'],
            ['^10', 'r2'],
            ['^15', 'r3'],
            ['^0', 'r4'],

            // Relations
            ['^11', 'parentId'],
            ['^12', 'frameId'],
            ['^1B', 'shapes'],

            // Transformations
            ['^1@', 'flipX'],
            ['^1A', 'flipY'],
            ['^Q', 'transform'],
            ['^Z', 'transformInverse'],
            ['^16', 'selrect'],
            ['^V', 'points'],
            ['^W', 'point'],
            ['^R', 'matrix'],
            ['^17', 'rect'],

            // Styles
            ['^13', 'strokes'],
            ['^1=', 'fills'],
            ['^1>', 'fillColor'],
            ['^1?', 'fillOpacity'],

            // Types
            ['^U', 'board'],
            ['^29', 'rect'],
            ['^3', 'circle'],
            ['^2:', 'path'],
            ['^2;', 'image'],
            ['^2<', 'svg-raw'],
            ['^2=', 'group'],
            ['^2>', 'bool'],

            // Texte
            ['^1D', 'growType'],
            ['^1F', 'content'],
            ['^1G', 'children'],
            ['^1H', 'lineHeight'],
            ['^1I', 'fontStyle'],
            ['^1J', 'textTransform'],
            ['^1K', 'textAlign'],
            ['^1L', 'fontId'],
            ['^1M', 'fontSize'],
            ['^1N', 'fontWeight'],
            ['^1O', 'textDirection'],
            ['^1P', 'fontVariantId'],
            ['^1Q', 'textDecoration'],
            ['^1R', 'letterSpacing'],
            ['^1S', 'fontFamily'],
            ['^1T', 'text'],
            ['^1Y', 'positionData'],
            ['^1Z', 'direction'],

            // Valeurs spéciales
            ['^20', 'auto-width'],
            ['^21', 'auto-height'],
            ['^22', 'fixed'],
            
            // Types génériques
            ['^4', 'type'],
            ['^N', 'shape'],
            ['^O', 'object']
        ];

        mappings.forEach(([key, value]) => this.handlers.set(key, value));
    }

    /**
     * Parse récursivement une structure Transit.
     *
     * @param data Donnée Transit brute.
     * @returns Structure JavaScript décodée.
     */
    parse(data: any): any {
        if (data === null || data === undefined) return data;

        if (typeof data === 'string') return this.parseString(data);
        if (Array.isArray(data)) return this.parseArray(data);
        if (typeof data === 'object') return this.parseObject(data);

        return data;
    }

    private parseString(str: string): any {
        if (str.startsWith('~u')) return str.substring(2);
        if (str.startsWith('~:')) return str.substring(2);
        if (str.startsWith('~m')) return parseInt(str.substring(2), 10);
        return str;
    }

    private parseArray(arr: any[]): any {
        if (arr.length === 0) return arr;
        const first = arr[0];

        if (first === "^ ") return this.parseTransitMap(arr);
        if (first === "~#set") return arr[1] ? this.parse(arr[1]) : [];
        if (first === "~#ordered-set") return arr[1] ? this.parse(arr[1]) : [];

        if (typeof first === 'string' && first.startsWith('^') && arr.length === 2) {
            return this.parse(arr[1]);
        }

        return arr.map(item => this.parse(item));
    }

    private parseTransitMap(arr: any[]): any {
        const result: any = {};

        for (let i = 1; i < arr.length; i += 2) {
            const key = arr[i];
            const value = arr[i + 1];

            if (key === undefined) break;

            const normalizedKey = this.parseString(key);
            const parsedValue = this.parse(value);
            const finalKey = this.handlers.get(normalizedKey) || normalizedKey;

            result[finalKey] = parsedValue;
        }

        return result;
    }

    private parseObject(obj: any): any {
        const result: any = {};

        for (const [key, value] of Object.entries(obj)) {
            const normalizedKey = this.handlers.get(key) || key;
            result[normalizedKey] = this.parse(value);
        }

        return result;
    }

    /**
     * Normalise une page exportée vers un format standardisé.
     *
     * @param pageData Données de page brutes.
     * @returns Page normalisée.
     */
    normalizePage(pageData: any): any {
        if (!pageData || !pageData.objects) {
            return pageData;
        }

        const page = pageData as {
            id: string;
            name: string;
            objects: Record<string, unknown>;
        };

        const normalized = {
            id: this.normalizeId(page.id),
            name: page.name,
            objects: {} as Record<string, unknown>
        };

        for (const [objId, objData] of Object.entries(page.objects)) {
            const cleanId = this.normalizeId(objId);
            const parsedObj = this.parse(objData);
            normalized.objects[cleanId] = this.normalizeObject(parsedObj);
        }

        return normalized;
    }

    private normalizeId(id: any): string {
        if (typeof id !== 'string') return String(id);
        return id.startsWith('~u') ? id.substring(2) : id;
    }

    private normalizeObject(obj: any): any {
        if (!obj || typeof obj !== 'object') return obj;

        const shapeType = this.determineShapeType(obj);

        const normalized: any = {
            id: this.normalizeId(obj.id),
            name: obj.name || 'Unnamed',
            type: shapeType,
            x: obj.x ?? 0,
            y: obj.y ?? 0,
            width: obj.width ?? 0,
            height: obj.height ?? 0,
            rotation: obj.rotation ?? 0,

            selrect: obj.selrect || {
                x: obj.x ?? 0,
                y: obj.y ?? 0,
                width: obj.width ?? 0,
                height: obj.height ?? 0,
                x1: obj.x ?? 0,
                y1: obj.y ?? 0,
                x2: (obj.x ?? 0) + (obj.width ?? 0),
                y2: (obj.y ?? 0) + (obj.height ?? 0)
            },

            points: obj.points,
            parentId: this.normalizeId(obj.parentId),
            frameId: this.normalizeId(obj.frameId),
            strokes: obj.strokes || [],
            fills: obj.fills || [],
        };

        // Coins arrondis
        if (obj.r1 !== undefined) normalized.r1 = obj.r1;
        if (obj.r2 !== undefined) normalized.r2 = obj.r2;
        if (obj.r3 !== undefined) normalized.r3 = obj.r3;
        if (obj.r4 !== undefined) normalized.r4 = obj.r4;

        if (obj.shapes) {
            normalized.shapes = Array.isArray(obj.shapes) 
                ? obj.shapes.map((id: any) => this.normalizeId(id))
                : [];
        }

        if (obj.growType) normalized.growType = obj.growType;
        if (obj.content) normalized.content = obj.content;
        if (obj.positionData) normalized.positionData = obj.positionData;

        return normalized;
    }

    /**
     * Détermine le type de forme à partir des propriétés de l'objet.
     * 
     * @param obj Objet parsé contenant les données de la forme.
     * @returns Type de forme Penpot ('board', 'rect', 'text', etc.).
     */
    private determineShapeType(obj: any): string {
        if (obj.type && this.isValidShapeType(obj.type)) {
            return obj.type;
        }

        // Board/Frame : a des shapes enfants
        if (obj.shapes && Array.isArray(obj.shapes) && obj.shapes.length > 0) {
            return 'board';
        }

        // Text : a du contenu textuel, growType ou positionData
        if (obj.content || obj.growType || obj.positionData) {
            return 'text';
        }

        // Rectangle : a des points formant un rectangle (4 points)
        if (obj.points && Array.isArray(obj.points) && obj.points.length === 4) {
            if (this.isRectangularShape(obj.points)) {
                return 'rect';
            }
        }

        // Path : a des points mais pas rectangulaire
        if (obj.points && Array.isArray(obj.points)) return 'path';

        // Group : nom contient "group"
        if (obj.name && obj.name.toLowerCase().includes('group')) {
            return 'group';
        }

        // Par défaut, rectangle
        return 'rect';
    }

    /**
     * Vérifie si le type est un type de forme Penpot valide.
     */
    private isValidShapeType(type: string): boolean {
        const validTypes = [
            'board', 'group', 'boolean', 'rect', 'rectangle',
            'path', 'text', 'ellipse', 'svg-raw', 'image'
        ];
        return validTypes.includes(type);
    }

    /**
     * Vérifie si les points forment un rectangle.
     * 
     * @param points Tableau de points {x, y}.
     * @returns true si les points forment un rectangle.
     */
    private isRectangularShape(points: Array<{x: number, y: number}>): boolean {
        if (points.length !== 4) return false;

        const xValues = [...new Set(points.map(p => p.x))];
        const yValues = [...new Set(points.map(p => p.y))];

        return xValues.length === 2 && yValues.length === 2;
    }
}