export class TransitParser {
    private handlers: Map<string, string> = new Map();

    constructor() {
        this.initializeHandlers();
    }

    private initializeHandlers(): void {
        // Mapping des clés Transit vers les clés normalisées
        const mappings: [string, string][] = [
            // Clés de page
            ['^M', 'objects'],
            ['^A', 'id'],
            ['^=', 'name'],
            
            // Propriétés géométriques
            ['^T', 'width'],
            ['^X', 'rotation'],
            ['^19', 'x1'],
            ['^1:', 'y1'],
            ['^1;', 'x2'],
            ['^1<', 'y2'],
            
            // Propriétés de forme
            ['^14', 'proportion'],
            ['^P', 'hideFillOnExport'],
            ['^Y', 'proportionLock'],
            ['^[', 'r2'],
            ['^10', 'r3'],
            ['^15', 'r4'],
            
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
            ['^U', 'frame'],
            ['^29', 'rect'],
            ['^N', 'shape'],
            ['^O', 'object'],
            
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
            ['^20', 'auto-width']
        ];

        mappings.forEach(([key, value]) => this.handlers.set(key, value));
    }

    /**
     * Parse un tableau Transit en objet JavaScript
     * Format Transit: ["^ ", "~:key1", value1, "~:key2", value2, ...]
     */
    parse(data: any): any {
        if (data === null || data === undefined) {
            return data;
        }

        if (typeof data === 'string') {
            return this.parseString(data);
        }

        if (Array.isArray(data)) {
            return this.parseArray(data);
        }

        if (typeof data === 'object') {
            return this.parseObject(data);
        }

        return data;
    }

    private parseString(str: string): any {
        // UUID avec préfixe ~u
        if (str.startsWith('~u')) {
            return str.substring(2);
        }
        // Clé avec préfixe ~:
        if (str.startsWith('~:')) {
            return str.substring(2);
        }
        // Timestamp avec préfixe ~m
        if (str.startsWith('~m')) {
            return parseInt(str.substring(2), 10);
        }
        return str;
    }

    private parseArray(arr: any[]): any {
        if (arr.length === 0) return arr;

        const first = arr[0];

        // Map Transit: ["^ ", key1, val1, key2, val2, ...]
        if (first === "^ ") {
            return this.parseTransitMap(arr);
        }

        // Set Transit: ["~#set", [...]]
        if (first === "~#set") {
            return arr[1] ? this.parse(arr[1]) : [];
        }

        // Ordered set: ["~#ordered-set", [...]]
        if (first === "~#ordered-set") {
            return arr[1] ? this.parse(arr[1]) : [];
        }

        // Tagged value: ["^type", value]
        if (typeof first === 'string' && first.startsWith('^') && arr.length === 2) {
            return this.parse(arr[1]);
        }

        // Array normal
        return arr.map(item => this.parse(item));
    }

    private parseTransitMap(arr: any[]): any {
        const result: any = {};
        
        // Sauter le premier élément "^ " et itérer par paires
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
     * Normalise un objet de page pour correspondre au format standardisé
     */
    normalizePage(pageData: any): any {
        if (!pageData || !pageData.objects) {
            return pageData;
        }

        const normalized: any = {
            id: this.normalizeId(pageData.id),
            name: pageData.name,
            objects: {}
        };

        // Parcourir et normaliser tous les objets
        for (const [objId, objData] of Object.entries(pageData.objects)) {
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
        if (!obj) return obj;

        // Propriétés de base
        const normalized: any = {
            id: this.normalizeId(obj.id),
            name: obj.name || 'Unnamed',
            type: obj.type || 'unknown',
            x: obj.x ?? 0,
            y: obj.y ?? 0,
            width: obj.width ?? 0,
            height: obj.height ?? 0,
            rotation: obj.rotation ?? 0,
            
            // Sélection rectangle
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
            
            // Points et transformations
            points: obj.points,
            transform: obj.transform,
            transformInverse: obj.transformInverse,
            
            // Relations
            parentId: this.normalizeId(obj.parentId),
            frameId: this.normalizeId(obj.frameId),
            
            // Flags
            flipX: obj.flipX ?? null,
            flipY: obj.flipY ?? null,
            hideFillOnExport: obj.hideFillOnExport ?? false,
            proportionLock: obj.proportionLock ?? false,
            
            // Coins arrondis
            r1: obj.r1 ?? 0,
            r2: obj.r2 ?? 0,
            r3: obj.r3 ?? 0,
            r4: obj.r4 ?? 0,
            
            // Styles
            strokes: obj.strokes || [],
            fills: obj.fills || [],
            proportion: obj.proportion ?? 1
        };

        // Propriétés conditionnelles
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
}