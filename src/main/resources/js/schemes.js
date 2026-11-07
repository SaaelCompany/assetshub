(function (global) {
    function nid(prefix) {
        nid.n = (nid.n || 0) + 1;
        return prefix + '-' + Date.now().toString(36) + '-' + nid.n;
    }

    function leftNorm(a, b) {
        var dx = b.x - a.x;
        var dy = b.y - a.y;
        var len = Math.sqrt(dx * dx + dy * dy) || 1;
        return { x: dy / len, y: -dx / len };
    }

    function offsetPoints(points, closed, dist) {
        var n = points.length;
        var out = [];
        var i;
        for (i = 0; i < n; i++) {
            var curr = points[i];
            var hasPrev = closed || i > 0;
            var hasNext = closed || i < n - 1;
            var prev = hasPrev ? points[closed ? (i - 1 + n) % n : i - 1] : curr;
            var next = hasNext ? points[closed ? (i + 1) % n : i + 1] : curr;
            var n1 = hasPrev ? leftNorm(prev, curr) : null;
            var n2 = hasNext ? leftNorm(curr, next) : null;
            if (!n1) n1 = n2;
            if (!n2) n2 = n1;
            if (!n1) {
                out.push({ x: curr.x, y: curr.y });
                continue;
            }
            var nx = n1.x + n2.x;
            var ny = n1.y + n2.y;
            var len = Math.sqrt(nx * nx + ny * ny) || 1;
            nx /= len;
            ny /= len;
            var denom = nx * n2.x + ny * n2.y;
            var miter = Math.abs(denom) < 0.05 ? dist : dist / denom;
            var limit = Math.abs(dist) * 6;
            if (miter > limit) miter = limit;
            if (miter < -limit) miter = -limit;
            out.push({ x: curr.x + nx * miter, y: curr.y + ny * miter });
        }
        return out;
    }

    function polygonArea(points) {
        var sum = 0;
        var i;
        for (i = 0; i < points.length; i++) {
            var a = points[i];
            var b = points[(i + 1) % points.length];
            sum += a.x * b.y - b.x * a.y;
        }
        return Math.abs(sum) / 2;
    }

    function centroid(points) {
        var area = 0;
        var x = 0;
        var y = 0;
        var i;
        for (i = 0; i < points.length; i++) {
            var a = points[i];
            var b = points[(i + 1) % points.length];
            var cross = a.x * b.y - b.x * a.y;
            area += cross;
            x += (a.x + b.x) * cross;
            y += (a.y + b.y) * cross;
        }
        if (Math.abs(area) < 1e-6) {
            var sx = 0;
            var sy = 0;
            for (i = 0; i < points.length; i++) {
                sx += points[i].x;
                sy += points[i].y;
            }
            return { x: sx / points.length, y: sy / points.length };
        }
        return { x: x / (3 * area), y: y / (3 * area) };
    }

    function roundCm(value) {
        return Math.round(value * 100) / 100;
    }

    function dist(ax, ay, bx, by) {
        var dx = ax - bx;
        var dy = ay - by;
        return Math.sqrt(dx * dx + dy * dy);
    }

    function projectSeg(ax, ay, bx, by, px, py) {
        var dx = bx - ax;
        var dy = by - ay;
        var len2 = dx * dx + dy * dy || 1;
        var t = ((px - ax) * dx + (py - ay) * dy) / len2;
        if (t < 0) t = 0;
        if (t > 1) t = 1;
        var x = ax + dx * t;
        var y = ay + dy * t;
        return { t: t, x: x, y: y, dist: dist(px, py, x, y), len: Math.sqrt(len2) };
    }

    function pointInPoly(x, y, points) {
        var inside = false;
        var i;
        var j = points.length - 1;
        for (i = 0; i < points.length; j = i++) {
            var a = points[i];
            var b = points[j];
            if (((a.y > y) !== (b.y > y)) && (x < (b.x - a.x) * (y - a.y) / ((b.y - a.y) || 1e-9) + a.x)) {
                inside = !inside;
            }
        }
        return inside;
    }

    function clone(value) {
        return JSON.parse(JSON.stringify(value));
    }

    function rackParts() {
        var parts = [{ t: 'r', x: 1, y: 1, w: 58, h: 88, c: '#172b4d', f: '#f4f5f7', s: 1.2 }];
        var i;
        for (i = 0; i < 19; i++) {
            var y = 4 + i * (82 / 19);
            parts.push({ t: 'l', x1: 6, y1: y, x2: 54, y2: y, c: '#6b778c', s: 0.35 });
        }
        return parts;
    }

    function builtins(t) {
        return [
            { id: 'rack', name: t('schemeSymRack'), w: 60, h: 90, parts: rackParts() },
            { id: 'switch', name: t('schemeSymSwitch'), w: 48, h: 10, parts: [
                { t: 'r', x: 0, y: 0, w: 48, h: 10, c: '#172b4d', f: '#deebff', s: 0.8 },
                { t: 'r', x: 3, y: 3, w: 4, h: 4, c: '#0052cc', f: '#0052cc', s: 0.2 },
                { t: 'r', x: 9, y: 3, w: 4, h: 4, c: '#0052cc', f: '#0052cc', s: 0.2 },
                { t: 'r', x: 15, y: 3, w: 4, h: 4, c: '#0052cc', f: '#0052cc', s: 0.2 },
                { t: 'r', x: 21, y: 3, w: 4, h: 4, c: '#0052cc', f: '#0052cc', s: 0.2 },
                { t: 'r', x: 27, y: 3, w: 4, h: 4, c: '#0052cc', f: '#0052cc', s: 0.2 },
                { t: 'r', x: 33, y: 3, w: 4, h: 4, c: '#0052cc', f: '#0052cc', s: 0.2 },
                { t: 'r', x: 39, y: 3, w: 4, h: 4, c: '#0052cc', f: '#0052cc', s: 0.2 }
            ] },
            { id: 'desk', name: t('schemeSymDesk'), w: 140, h: 70, parts: [
                { t: 'r', x: 0, y: 0, w: 140, h: 70, c: '#172b4d', f: '#fff7e6', s: 1.2 }
            ] },
            { id: 'chair', name: t('schemeSymChair'), w: 45, h: 45, parts: [
                { t: 'e', x: 8, y: 4, w: 29, h: 29, c: '#172b4d', f: '#ffffff', s: 1 },
                { t: 'r', x: 10, y: 34, w: 25, h: 6, c: '#172b4d', f: '#172b4d', s: 0.4 }
            ] },
            { id: 'sofa', name: t('schemeSymSofa'), w: 180, h: 80, parts: [
                { t: 'r', x: 0, y: 14, w: 180, h: 66, c: '#172b4d', f: '#eae6ff', s: 1.2 },
                { t: 'r', x: 8, y: 0, w: 164, h: 18, c: '#172b4d', f: '#c0b6f2', s: 1 }
            ] },
            { id: 'socket', name: t('schemeSymSocket'), w: 8, h: 8, parts: [
                { t: 'r', x: 0, y: 0, w: 8, h: 8, c: '#172b4d', f: '#ffffff', s: 0.4 },
                { t: 'e', x: 2, y: 2, w: 1.4, h: 2.4, c: '#172b4d', f: '#172b4d', s: 0.2 },
                { t: 'e', x: 4.6, y: 2, w: 1.4, h: 2.4, c: '#172b4d', f: '#172b4d', s: 0.2 }
            ] },
            { id: 'lamp', name: t('schemeSymLamp'), w: 30, h: 30, parts: [
                { t: 'e', x: 2, y: 2, w: 26, h: 26, c: '#ff8b00', f: '#fff7e6', s: 1 },
                { t: 'l', x1: 15, y1: 6, x2: 15, y2: 24, c: '#ff8b00', s: 0.6 },
                { t: 'l', x1: 6, y1: 15, x2: 24, y2: 15, c: '#ff8b00', s: 0.6 }
            ] }
        ];
    }

    function blankDoc(t) {
        return {
            name: t('schemeDefaultName'),
            floors: [{ id: nid('floor'), name: t('schemeFloor') + ' 1', elevation: 0, objects: [], rooms: [] }],
            activeFloorId: '',
            layers: [
                { id: 'walls', name: t('schemeLayerWalls'), visible: true, locked: false, color: '#172b4d' },
                { id: 'furniture', name: t('schemeLayerFurniture'), visible: true, locked: false, color: '#6b778c' },
                { id: 'equipment', name: t('schemeLayerEquipment'), visible: true, locked: false, color: '#0052cc' },
                { id: 'notes', name: t('schemeLayerNotes'), visible: true, locked: false, color: '#ff8b00' }
            ],
            activeLayerId: 'walls',
            symbols: {},
            placeId: 0,
            settings: { showOtherFloors: true, unit: 'cm' }
        };
    }

    function mount(host, api) {
        var session = global.__assetScheme;
        if (!session || session.project !== api.projectKey) {
            var doc = blankDoc(api.t);
            doc.activeFloorId = doc.floors[0].id;
            session = {
                project: api.projectKey,
                doc: doc,
                id: 0,
                loaded: false,
                loading: false,
                tool: 'select',
                cam: { x: -80, y: -80, z: 0.55 },
                selection: [],
                draft: null,
                typed: '',
                snapOn: true,
                orthoOn: false,
                osnapOn: true,
                gridOn: true,
                space: false,
                stamp: '',
                symbolId: 'rack',
                catalog: [],
                dirty: false,
                past: [],
                future: [],
                osnap: null,
                osnapList: [],
                osnapIndex: 0,
                clipboard: null
            };
            global.__assetScheme = session;
        }

        host.className = 'asset-tree-schemes';
        host.innerHTML = '';
        var bar = el('div', 'asset-tree-scheme-bar');
        var head = el('div', 'asset-tree-scheme-head');
        var schemePick = el('select', 'asset-tree-scheme-pick');
        var placePick = el('select', 'asset-tree-scheme-pick');
        var nameInput = el('input', 'asset-tree-scheme-name');
        nameInput.value = session.doc.name || '';
        nameInput.disabled = !api.canEdit;
        var tools = el('div', 'asset-tree-scheme-tools');
        var toolDefs = [
            ['select', 'V', 'schemeToolSelect'],
            ['line', 'L', 'schemeToolLine'],
            ['rect', 'R', 'schemeToolRect'],
            ['ellipse', 'E', 'schemeToolEllipse'],
            ['polyline', 'P', 'schemeToolPen'],
            ['wall', 'W', 'schemeToolWall'],
            ['text', 'T', 'schemeToolText'],
            ['dimension', 'D', 'schemeToolDim'],
            ['door', '', 'schemeToolDoor'],
            ['window', '', 'schemeToolWindow']
        ];
        toolDefs.forEach(function (item) {
            var button = el('button', 'asset-tree-scheme-tool');
            button.type = 'button';
            button.title = api.t(item[2]) + (item[1] ? ' (' + item[1] + ')' : '');
            button.setAttribute('data-tool', item[0]);
            button.disabled = !api.canEdit && item[0] !== 'select';
            button.innerHTML = toolIcon(item[0]);
            var caption = el('span', null, api.t(item[2]));
            button.appendChild(caption);
            button.addEventListener('click', function () {
                session.tool = item[0];
                session.draft = null;
                session.typed = '';
                markTools();
                paint();
            });
            tools.appendChild(button);
        });
        var unit = el('select', 'asset-tree-scheme-unit');
        ['cm', 'mm', 'm', 'in'].forEach(function (value) {
            var option = el('option', null, value);
            option.value = value;
            unit.appendChild(option);
        });
        unit.value = session.doc.settings.unit || 'cm';
        var saveBtn = el('button', 'asset-tree-scheme-save', api.t('save'));
        saveBtn.type = 'button';
        saveBtn.disabled = !api.canEdit;
        var newBtn = el('button', 'asset-tree-scheme-new', api.t('schemeNew'));
        newBtn.type = 'button';
        newBtn.disabled = !api.canEdit;
        var status = el('div', 'asset-tree-scheme-status', '');
        head.appendChild(labeled(api.t('schemeList'), schemePick));
        head.appendChild(labeled(api.t('name'), nameInput));
        head.appendChild(labeled(api.t('schemePlace'), placePick));
        head.appendChild(unit);
        head.appendChild(newBtn);
        head.appendChild(saveBtn);
        bar.appendChild(head);
        bar.appendChild(tools);
        var body = el('div', 'asset-tree-scheme-body');
        var side = el('div', 'asset-tree-scheme-side');
        var stage = el('div', 'asset-tree-scheme-stage');
        var props = el('div', 'asset-tree-scheme-props');
        var gridC = el('canvas', 'asset-tree-scheme-canvas');
        var sceneC = el('canvas', 'asset-tree-scheme-canvas');
        var overC = el('canvas', 'asset-tree-scheme-canvas');
        stage.appendChild(gridC);
        stage.appendChild(sceneC);
        stage.appendChild(overC);
        body.appendChild(side);
        body.appendChild(stage);
        body.appendChild(props);
        host.appendChild(bar);
        host.appendChild(body);
        host.appendChild(status);
        host.tabIndex = 0;

        function el(tag, cls, text) {
            var node = document.createElement(tag);
            if (cls) node.className = cls;
            if (text) node.textContent = text;
            return node;
        }

        function labeled(text, control) {
            var wrap = el('label', 'asset-tree-scheme-field-inline');
            wrap.appendChild(document.createTextNode(text));
            wrap.appendChild(control);
            return wrap;
        }

        function toolIcon(name) {
            var body = {
                select: '<path d="M6 4l9 12-3-.5-2 4-2-1 2-4-4-2z"/>',
                line: '<path d="M5 19L19 5"/>',
                rect: '<rect x="5" y="7" width="14" height="10"/>',
                ellipse: '<ellipse cx="12" cy="12" rx="7" ry="5"/>',
                polyline: '<path d="M4 17l5-8 4 5 7-9"/>',
                wall: '<path d="M4 16V8h16v8"/>',
                text: '<path d="M6 7h12M12 7v11"/>',
                dimension: '<path d="M4 12h16M6 9L4 12l2 3M18 9l2 3-2 3"/>',
                door: '<path d="M8 19V6h5a7 7 0 0 1 0 13"/>',
                window: '<rect x="5" y="6" width="14" height="12"/><path d="M12 6v12M5 12h14"/>'
            };
            return '<svg viewBox="0 0 24 24" aria-hidden="true">' + (body[name] || '') + '</svg>';
        }

        function activeFloor() {
            var floors = session.doc.floors || [];
            var i;
            for (i = 0; i < floors.length; i++) {
                if (floors[i].id === session.doc.activeFloorId) return floors[i];
            }
            return floors[0];
        }

        function layerById(id) {
            var layers = session.doc.layers || [];
            var i;
            for (i = 0; i < layers.length; i++) {
                if (layers[i].id === id) return layers[i];
            }
            return layers[0];
        }

        function layerVisible(id) {
            var layer = layerById(id);
            return !layer || layer.visible !== false;
        }

        function symbolDef(id) {
            if (session.doc.symbols && session.doc.symbols[id]) return session.doc.symbols[id];
            var list = builtins(api.t);
            var i;
            for (i = 0; i < list.length; i++) {
                if (list[i].id === id) return list[i];
            }
            return list[0];
        }

        function markTools() {
            var buttons = tools.querySelectorAll('button');
            var i;
            for (i = 0; i < buttons.length; i++) {
                if (buttons[i].getAttribute('data-tool') === session.tool) buttons[i].className = 'asset-tree-scheme-tool is-on';
                else buttons[i].className = 'asset-tree-scheme-tool';
            }
        }

        function worldFrom(sx, sy) {
            return {
                x: (sx - 22) / session.cam.z + session.cam.x,
                y: (sy - 22) / session.cam.z + session.cam.y
            };
        }

        function screenFrom(x, y) {
            return {
                x: (x - session.cam.x) * session.cam.z + 22,
                y: (y - session.cam.y) * session.cam.z + 22
            };
        }

        function gridStep() {
            var steps = [1, 5, 10, 25, 50, 100];
            var i;
            for (i = 0; i < steps.length; i++) {
                var px = steps[i] * session.cam.z;
                if (px >= 14 && px <= 90) return steps[i];
            }
            var step = 100;
            while (step * session.cam.z < 14 && step < 200000) step *= 5;
            if (session.cam.z >= 90) return 1;
            return step;
        }

        function formatLen(cm) {
            var unitName = session.doc.settings.unit || 'cm';
            if (unitName === 'm') return (Math.round(cm) / 100).toFixed(2) + ' m';
            if (unitName === 'mm') return String(Math.round(cm * 10)) + ' mm';
            if (unitName === 'in') return (cm / 2.54).toFixed(2) + ' in';
            return (Math.round(cm * 100) / 100) + ' cm';
        }

        function remember() {
            session.past.push(JSON.stringify(session.doc));
            if (session.past.length > 100) session.past.shift();
            session.future = [];
            session.dirty = true;
            try {
                global.localStorage.setItem('assethub-scheme-' + api.projectKey, JSON.stringify({
                    id: session.id,
                    doc: session.doc
                }));
            } catch (error) {
                /* the drawing still lives in this tab */
            }
        }

        function undo() {
            if (!session.past.length) return;
            session.future.push(JSON.stringify(session.doc));
            session.doc = JSON.parse(session.past.pop());
            session.selection = [];
            session.draft = null;
            nameInput.value = session.doc.name || '';
            refreshSide();
            renderProps();
            paint();
        }

        function redo() {
            if (!session.future.length) return;
            session.past.push(JSON.stringify(session.doc));
            session.doc = JSON.parse(session.future.pop());
            session.selection = [];
            nameInput.value = session.doc.name || '';
            refreshSide();
            renderProps();
            paint();
        }

        function segmentsOf(floor) {
            var segs = [];
            var objects = floor.objects || [];
            var i;
            var k;
            for (i = 0; i < objects.length; i++) {
                var obj = objects[i];
                if (!layerVisible(obj.layerId)) continue;
                if (obj.type === 'line' || obj.type === 'dimension') {
                    segs.push({ x1: obj.x1, y1: obj.y1, x2: obj.x2, y2: obj.y2 });
                } else if (obj.type === 'rect' || obj.type === 'ellipse' || obj.type === 'symbol') {
                    var x = obj.x;
                    var y = obj.y;
                    var w = obj.w;
                    var h = obj.h;
                    segs.push({ x1: x, y1: y, x2: x + w, y2: y });
                    segs.push({ x1: x + w, y1: y, x2: x + w, y2: y + h });
                    segs.push({ x1: x + w, y1: y + h, x2: x, y2: y + h });
                    segs.push({ x1: x, y1: y + h, x2: x, y2: y });
                } else if ((obj.type === 'polyline' || obj.type === 'wall') && obj.points) {
                    for (k = 1; k < obj.points.length; k++) {
                        segs.push({ x1: obj.points[k - 1].x, y1: obj.points[k - 1].y, x2: obj.points[k].x, y2: obj.points[k].y });
                    }
                    if (obj.closed && obj.points.length > 2) {
                        var last = obj.points[obj.points.length - 1];
                        segs.push({ x1: last.x, y1: last.y, x2: obj.points[0].x, y2: obj.points[0].y });
                    }
                }
            }
            return segs;
        }

        function snapPoint(raw, from) {
            var point = { x: roundCm(raw.x), y: roundCm(raw.y) };
            if (session.orthoOn && from) {
                if (Math.abs(point.x - from.x) >= Math.abs(point.y - from.y)) point.y = from.y;
                else point.x = from.x;
            }
            if (session.snapOn) {
                var step = gridStep();
                point.x = Math.round(point.x / step) * step;
                point.y = Math.round(point.y / step) * step;
            }
            session.osnap = null;
            if (!session.osnapOn) return point;
            var tol = 12 / session.cam.z;
            var found = [];
            function add(kind, x, y) {
                if (dist(x, y, raw.x, raw.y) <= tol) found.push({ kind: kind, x: roundCm(x), y: roundCm(y) });
            }
            var floor = activeFloor();
            var segs = segmentsOf(floor);
            var i;
            var j;
            for (i = 0; i < segs.length; i++) {
                var seg = segs[i];
                add('endpoint', seg.x1, seg.y1);
                add('endpoint', seg.x2, seg.y2);
                add('midpoint', (seg.x1 + seg.x2) / 2, (seg.y1 + seg.y2) / 2);
                var near = projectSeg(seg.x1, seg.y1, seg.x2, seg.y2, raw.x, raw.y);
                if (near.t > 0.02 && near.t < 0.98) add('nearest', near.x, near.y);
                if (from) {
                    var foot = projectSeg(seg.x1, seg.y1, seg.x2, seg.y2, from.x, from.y);
                    if (foot.t > 0 && foot.t < 1) add('perpendicular', foot.x, foot.y);
                }
            }
            var objects = floor.objects || [];
            for (i = 0; i < objects.length; i++) {
                var obj = objects[i];
                if (obj.type === 'rect' || obj.type === 'ellipse' || obj.type === 'symbol') {
                    add('center', obj.x + obj.w / 2, obj.y + obj.h / 2);
                }
            }
            var nearSegs = [];
            for (i = 0; i < segs.length; i++) {
                var box = segs[i];
                if (dist((box.x1 + box.x2) / 2, (box.y1 + box.y2) / 2, raw.x, raw.y) < tol * 8) nearSegs.push(box);
            }
            if (nearSegs.length <= 80) {
                for (i = 0; i < nearSegs.length; i++) {
                    for (j = i + 1; j < nearSegs.length; j++) {
                        var hit = intersect(nearSegs[i], nearSegs[j]);
                        if (hit) add('intersection', hit.x, hit.y);
                    }
                }
            }
            if (!found.length) return point;
            var rank = { endpoint: 0, midpoint: 1, intersection: 2, center: 3, perpendicular: 4, nearest: 5 };
            found.sort(function (a, b) { return rank[a.kind] - rank[b.kind]; });
            session.osnapList = found;
            if (session.osnapIndex >= found.length) session.osnapIndex = 0;
            session.osnap = found[session.osnapIndex];
            return { x: session.osnap.x, y: session.osnap.y };
        }

        function intersect(a, b) {
            var d1x = a.x2 - a.x1;
            var d1y = a.y2 - a.y1;
            var d2x = b.x2 - b.x1;
            var d2y = b.y2 - b.y1;
            var den = d1x * d2y - d1y * d2x;
            if (Math.abs(den) < 1e-6) return null;
            var t = ((b.x1 - a.x1) * d2y - (b.y1 - a.y1) * d2x) / den;
            var u = ((b.x1 - a.x1) * d1y - (b.y1 - a.y1) * d1x) / den;
            if (t < 0 || t > 1 || u < 0 || u > 1) return null;
            return { x: a.x1 + d1x * t, y: a.y1 + d1y * t };
        }

        function pointer(event) {
            var rect = overC.getBoundingClientRect();
            var sx = event.clientX - rect.left;
            var sy = event.clientY - rect.top;
            var from = null;
            if (session.draft && session.draft.points && session.draft.points.length) {
                from = session.draft.points[session.draft.points.length - 1];
            } else if (session.draft && session.draft.x1 != null && session.draft.phase === 1) {
                from = { x: session.draft.x1, y: session.draft.y1 };
            }
            var raw = worldFrom(sx, sy);
            if (event.shiftKey) {
                var saved = session.orthoOn;
                session.orthoOn = true;
                var snapped = snapPoint(raw, from);
                session.orthoOn = saved;
                return snapped;
            }
            return snapPoint(raw, from);
        }

        function selectedObjects() {
            var floor = activeFloor();
            var out = [];
            var i;
            var k;
            for (i = 0; i < session.selection.length; i++) {
                var id = session.selection[i];
                var objects = floor.objects || [];
                for (k = 0; k < objects.length; k++) {
                    if (objects[k].id === id) out.push(objects[k]);
                }
                var rooms = floor.rooms || [];
                for (k = 0; k < rooms.length; k++) {
                    if ('room:' + rooms[k].id === id) out.push(rooms[k]);
                }
            }
            return out;
        }

        function hit(wx, wy) {
            var floor = activeFloor();
            var objects = floor.objects || [];
            var tol = 8 / session.cam.z;
            var i;
            for (i = objects.length - 1; i >= 0; i--) {
                var obj = objects[i];
                if (!layerVisible(obj.layerId)) continue;
                var layer = layerById(obj.layerId);
                if (layer && layer.locked) continue;
                if (hitsObject(obj, wx, wy, tol)) return obj.id;
            }
            var rooms = floor.rooms || [];
            for (i = rooms.length - 1; i >= 0; i--) {
                if (rooms[i].points && pointInPoly(wx, wy, rooms[i].points)) return 'room:' + rooms[i].id;
            }
            return '';
        }

        function hitsObject(obj, wx, wy, tol) {
            if (obj.type === 'line' || obj.type === 'dimension') {
                return projectSeg(obj.x1, obj.y1, obj.x2, obj.y2, wx, wy).dist <= tol;
            }
            if (obj.type === 'rect' || obj.type === 'ellipse' || obj.type === 'symbol' || obj.type === 'text') {
                var w = obj.w || 40;
                var h = obj.h || 20;
                return wx >= obj.x - tol && wy >= obj.y - tol && wx <= obj.x + w + tol && wy <= obj.y + h + tol;
            }
            if ((obj.type === 'polyline' || obj.type === 'wall') && obj.points) {
                var i;
                for (i = 1; i < obj.points.length; i++) {
                    if (projectSeg(obj.points[i - 1].x, obj.points[i - 1].y, obj.points[i].x, obj.points[i].y, wx, wy).dist <= Math.max(tol, (obj.thickness || 15) / 2)) return true;
                }
                if (obj.closed && obj.points.length > 2) {
                    var last = obj.points[obj.points.length - 1];
                    if (projectSeg(last.x, last.y, obj.points[0].x, obj.points[0].y, wx, wy).dist <= Math.max(tol, (obj.thickness || 15) / 2)) return true;
                }
            }
            if (obj.type === 'opening') return false;
            return false;
        }

        function handleAt(sx, sy) {
            var objects = selectedObjects();
            if (objects.length !== 1) return null;
            var obj = objects[0];
            var points = [];
            if (obj.type === 'line' || obj.type === 'dimension') {
                points = [{ x: obj.x1, y: obj.y1, index: 0 }, { x: obj.x2, y: obj.y2, index: 1 }];
            } else if (obj.points) {
                var i;
                for (i = 0; i < obj.points.length; i++) points.push({ x: obj.points[i].x, y: obj.points[i].y, index: i });
            } else if (obj.w != null) {
                points = [
                    { x: obj.x, y: obj.y, index: 0 },
                    { x: obj.x + obj.w, y: obj.y, index: 1 },
                    { x: obj.x + obj.w, y: obj.y + obj.h, index: 2 },
                    { x: obj.x, y: obj.y + obj.h, index: 3 }
                ];
            }
            var k;
            for (k = 0; k < points.length; k++) {
                var screen = screenFrom(points[k].x, points[k].y);
                if (dist(screen.x, screen.y, sx, sy) <= 8) return { id: obj.id, index: points[k].index };
            }
            return null;
        }

        function baseObject(type) {
            return {
                id: nid(type),
                type: type,
                layerId: session.doc.activeLayerId || 'walls',
                color: (layerById(session.doc.activeLayerId) || {}).color || '#172b4d',
                thickness: type === 'wall' ? 15 : 1,
                opacity: 1
            };
        }

        function finishPoly(closed) {
            if (!session.draft || !session.draft.points || session.draft.points.length < 2) {
                session.draft = null;
                return;
            }
            var obj = baseObject(session.draft.tool);
            obj.points = session.draft.points.map(function (p) { return { x: roundCm(p.x), y: roundCm(p.y) }; });
            while (obj.points.length >= 2 && dist(obj.points[obj.points.length - 1].x, obj.points[obj.points.length - 1].y, obj.points[obj.points.length - 2].x, obj.points[obj.points.length - 2].y) < 0.5) {
                obj.points.pop();
            }
            if (closed && obj.points.length >= 4 && dist(obj.points[0].x, obj.points[0].y, obj.points[obj.points.length - 1].x, obj.points[obj.points.length - 1].y) <= 40) {
                obj.points.pop();
            }
            if (obj.points.length < 2) {
                session.draft = null;
                paint();
                return;
            }
            remember();
            obj.closed = !!closed;
            if (obj.type === 'wall') {
                obj.thickness = 15;
                obj.alignment = 'center';
                obj.hatch = 'diagonal';
                obj.height = 300;
            }
            activeFloor().objects.push(obj);
            if (obj.type === 'wall') syncRooms(activeFloor());
            session.selection = [obj.id];
            session.draft = null;
            session.typed = '';
            refreshSide();
            renderProps();
            paint();
        }

        function commitTwo(kind, x1, y1, x2, y2) {
            remember();
            var obj = baseObject(kind);
            if (kind === 'line' || kind === 'dimension') {
                obj.x1 = roundCm(x1);
                obj.y1 = roundCm(y1);
                obj.x2 = roundCm(x2);
                obj.y2 = roundCm(y2);
                if (kind === 'dimension') obj.offset = 18;
            } else {
                obj.x = roundCm(Math.min(x1, x2));
                obj.y = roundCm(Math.min(y1, y2));
                obj.w = roundCm(Math.abs(x2 - x1));
                obj.h = roundCm(Math.abs(y2 - y1));
                if (obj.w < 1) obj.w = 1;
                if (obj.h < 1) obj.h = 1;
            }
            activeFloor().objects.push(obj);
            session.selection = [obj.id];
            session.draft = null;
            session.typed = '';
            renderProps();
            paint();
        }

        function placeSymbol(pt) {
            var def = symbolDef(session.symbolId);
            remember();
            var obj = baseObject('symbol');
            obj.symbolId = def.id;
            obj.x = roundCm(pt.x - def.w / 2);
            obj.y = roundCm(pt.y - def.h / 2);
            obj.w = def.w;
            obj.h = def.h;
            obj.angle = 0;
            obj.assetBinding = null;
            obj.layerId = 'equipment';
            activeFloor().objects.push(obj);
            session.selection = [obj.id];
            session.tool = 'select';
            markTools();
            renderProps();
            paint();
        }

        function placeOpening(kind, pt) {
            var floor = activeFloor();
            var best = null;
            var objects = floor.objects || [];
            var i;
            var k;
            var tol = Math.max(12, 16 / session.cam.z);
            for (i = 0; i < objects.length; i++) {
                var wall = objects[i];
                if (wall.type !== 'wall' || !wall.points) continue;
                for (k = 1; k < wall.points.length; k++) {
                    var hitSeg = projectSeg(wall.points[k - 1].x, wall.points[k - 1].y, wall.points[k].x, wall.points[k].y, pt.x, pt.y);
                    if (hitSeg.dist <= tol && (!best || hitSeg.dist < best.dist)) {
                        best = { wall: wall, index: k - 1, t: hitSeg.t, len: hitSeg.len, dist: hitSeg.dist };
                    }
                }
                if (wall.closed && wall.points.length > 2) {
                    var start = wall.points[0];
                    var end = wall.points[wall.points.length - 1];
                    var closeSeg = projectSeg(end.x, end.y, start.x, start.y, pt.x, pt.y);
                    if (closeSeg.dist <= tol && (!best || closeSeg.dist < best.dist)) {
                        best = { wall: wall, index: wall.points.length - 1, t: closeSeg.t, len: closeSeg.len, dist: closeSeg.dist };
                    }
                }
            }
            if (!best) return;
            remember();
            var opening = baseObject('opening');
            opening.kind = kind;
            opening.wallId = best.wall.id;
            opening.index = best.index;
            opening.t = best.t;
            opening.width = kind === 'window' ? 120 : 90;
            opening.swing = 'left';
            opening.layerId = best.wall.layerId;
            floor.objects.push(opening);
            session.selection = [opening.id];
            session.tool = 'select';
            markTools();
            renderProps();
            paint();
        }

        function syncRooms(floor) {
            var faces = [];
            var objects = floor.objects || [];
            var i;
            for (i = 0; i < objects.length; i++) {
                var wall = objects[i];
                if (wall.type === 'wall' && wall.closed && wall.points && wall.points.length >= 3) {
                    var area = polygonArea(wall.points);
                    if (area >= 3000) faces.push({ points: wall.points, area: area, center: centroid(wall.points) });
                }
            }
            var used = {};
            var next = [];
            var rooms = floor.rooms || [];
            for (i = 0; i < faces.length; i++) {
                var face = faces[i];
                var keep = null;
                var r;
                for (r = 0; r < rooms.length; r++) {
                    if (used[rooms[r].id] || !rooms[r].points) continue;
                    var center = centroid(rooms[r].points);
                    var previous = rooms[r].area || polygonArea(rooms[r].points);
                    var ratio = previous ? face.area / previous : 1;
                    if (dist(center.x, center.y, face.center.x, face.center.y) <= 40 && ratio >= 0.8 && ratio <= 1.2) {
                        keep = rooms[r];
                        break;
                    }
                }
                if (keep) {
                    used[keep.id] = true;
                    keep.points = face.points.map(function (p) { return { x: p.x, y: p.y }; });
                    keep.area = face.area;
                    next.push(keep);
                } else {
                    next.push({
                        id: nid('room'),
                        name: api.t('schemeRoom') + ' ' + (next.length + 1),
                        color: '#e3fcef',
                        points: face.points.map(function (p) { return { x: p.x, y: p.y }; }),
                        area: face.area,
                        assetBinding: null
                    });
                }
            }
            floor.rooms = next;
        }

        function applyTyped() {
            var text = session.typed.replace(',', '.').trim();
            var pair = /^(-?\d+(?:\.\d+)?)\s*[,;]\s*(-?\d+(?:\.\d+)?)$/.exec(session.typed.trim());
            var length = /^(-?\d+(?:\.\d+)?)$/.exec(text);
            if (!session.draft) return;
            if (session.draft.points) {
                var last = session.draft.points[session.draft.points.length - 1];
                var prev = session.draft.points.length > 1 ? session.draft.points[session.draft.points.length - 2] : last;
                if (pair) {
                    session.draft.points[session.draft.points.length - 1] = {
                        x: roundCm(last.x + Number(pair[1])),
                        y: roundCm(last.y + Number(pair[2]))
                    };
                } else if (length && session.draft.points.length > 1) {
                    var dx = last.x - prev.x;
                    var dy = last.y - prev.y;
                    var len = Math.sqrt(dx * dx + dy * dy) || 1;
                    var want = Number(length[1]);
                    session.draft.points[session.draft.points.length - 1] = {
                        x: roundCm(prev.x + dx / len * want),
                        y: roundCm(prev.y + dy / len * want)
                    };
                }
                session.typed = '';
                paint();
                return;
            }
            if (session.draft.phase !== 1) return;
            var x2 = session.draft.x2;
            var y2 = session.draft.y2;
            if (pair) {
                x2 = session.draft.x1 + Number(pair[1]);
                y2 = session.draft.y1 + Number(pair[2]);
            } else if (length) {
                var ddx = x2 - session.draft.x1;
                var ddy = y2 - session.draft.y1;
                var current = Math.sqrt(ddx * ddx + ddy * ddy) || 1;
                var next = Number(length[1]);
                x2 = session.draft.x1 + ddx / current * next;
                y2 = session.draft.y1 + ddy / current * next;
            } else return;
            if (session.draft.tool === 'rect' && session.orthoOn) {
                var side = Math.max(Math.abs(x2 - session.draft.x1), Math.abs(y2 - session.draft.y1));
                x2 = session.draft.x1 + side * (x2 >= session.draft.x1 ? 1 : -1);
                y2 = session.draft.y1 + side * (y2 >= session.draft.y1 ? 1 : -1);
            }
            commitTwo(session.draft.tool, session.draft.x1, session.draft.y1, x2, y2);
        }

        function translateObj(obj, dx, dy) {
            if (obj.points) {
                obj.points.forEach(function (p) {
                    p.x = roundCm(p.x + dx);
                    p.y = roundCm(p.y + dy);
                });
            } else if (obj.x1 != null) {
                obj.x1 = roundCm(obj.x1 + dx);
                obj.y1 = roundCm(obj.y1 + dy);
                obj.x2 = roundCm(obj.x2 + dx);
                obj.y2 = roundCm(obj.y2 + dy);
            } else if (obj.x != null) {
                obj.x = roundCm(obj.x + dx);
                obj.y = roundCm(obj.y + dy);
            }
        }

        function duplicateSelection() {
            var objects = selectedObjects();
            if (!objects.length || !api.canEdit) return;
            remember();
            var ids = [];
            objects.forEach(function (obj) {
                if (String(obj.id).indexOf('room') === 0 && obj.points && !obj.type) return;
                var copy = clone(obj);
                copy.id = nid(obj.type || 'obj');
                translateObj(copy, 20, 20);
                if (copy.type) activeFloor().objects.push(copy);
                ids.push(copy.id);
            });
            session.selection = ids;
            renderProps();
            paint();
        }

        function removeSelection() {
            if (!session.selection.length || !api.canEdit) return;
            remember();
            var floor = activeFloor();
            floor.objects = (floor.objects || []).filter(function (obj) {
                return session.selection.indexOf(obj.id) < 0;
            });
            syncRooms(floor);
            session.selection = [];
            refreshSide();
            renderProps();
            paint();
        }

        function drawParts(ctx, def, x, y, w, h) {
            var sx = w / (def.w || 1);
            var sy = h / (def.h || 1);
            var parts = def.parts || [];
            var i;
            for (i = 0; i < parts.length; i++) {
                var part = parts[i];
                ctx.save();
                ctx.translate(x, y);
                ctx.scale(sx, sy);
                ctx.strokeStyle = part.c || '#172b4d';
                ctx.fillStyle = part.f || 'transparent';
                ctx.lineWidth = part.s || 1;
                if (part.t === 'r' || part.t === 'e') {
                    ctx.beginPath();
                    if (part.t === 'e') ctx.ellipse(part.x + part.w / 2, part.y + part.h / 2, Math.abs(part.w) / 2, Math.abs(part.h) / 2, 0, 0, Math.PI * 2);
                    else ctx.rect(part.x, part.y, part.w, part.h);
                    if (part.f) ctx.fill();
                    ctx.stroke();
                } else if (part.t === 'l') {
                    ctx.beginPath();
                    ctx.moveTo(part.x1, part.y1);
                    ctx.lineTo(part.x2, part.y2);
                    ctx.stroke();
                } else if (part.t === 'p' && part.points && part.points.length) {
                    ctx.beginPath();
                    ctx.moveTo(part.points[0].x, part.points[0].y);
                    var p;
                    for (p = 1; p < part.points.length; p++) ctx.lineTo(part.points[p].x, part.points[p].y);
                    ctx.stroke();
                } else if (part.t === 't') {
                    ctx.fillStyle = part.c || '#172b4d';
                    ctx.font = '8px sans-serif';
                    ctx.fillText(part.text || '', part.x, part.y);
                }
                ctx.restore();
            }
        }

        function pathOf(ctx, points, closed) {
            if (!points || !points.length) return;
            ctx.beginPath();
            ctx.moveTo(points[0].x, points[0].y);
            var i;
            for (i = 1; i < points.length; i++) ctx.lineTo(points[i].x, points[i].y);
            if (closed) ctx.closePath();
        }

        function drawWall(ctx, wall, floor) {
            var points = wall.points || [];
            if (points.length < 2) return;
            var half = (wall.thickness || 15) / 2;
            if (wall.alignment === 'left') half = wall.thickness || 15;
            var openings = (floor.objects || []).filter(function (obj) {
                return obj.type === 'opening' && obj.wallId === wall.id;
            });
            ctx.save();
            ctx.globalAlpha = wall.opacity == null ? 1 : wall.opacity;
            ctx.fillStyle = '#f4f5f7';
            ctx.strokeStyle = wall.color || '#172b4d';
            ctx.lineWidth = 1.2;
            ctx.fillStyle = '#eef2f6';
            var i;
            for (i = 1; i < points.length; i++) drawWallSegment(ctx, wall, points[i - 1], points[i], i - 1, openings);
            if (wall.closed) drawWallSegment(ctx, wall, points[points.length - 1], points[0], points.length - 1, openings);
            for (i = 0; i < points.length; i++) {
                ctx.beginPath();
                ctx.arc(points[i].x, points[i].y, half, 0, Math.PI * 2);
                ctx.fill();
                ctx.stroke();
            }
            ctx.restore();
        }

        function drawWallSegment(ctx, wall, a, b, index, openings) {
            var gaps = [];
            var i;
            for (i = 0; i < openings.length; i++) {
                if (openings[i].index !== index) continue;
                var len = dist(a.x, a.y, b.x, b.y) || 1;
                var halfT = (openings[i].width || 90) / 2 / len;
                gaps.push({ from: openings[i].t - halfT, to: openings[i].t + halfT, opening: openings[i] });
            }
            gaps.sort(function (left, right) { return left.from - right.from; });
            var cursor = 0;
            var dx = b.x - a.x;
            var dy = b.y - a.y;
            function at(t) { return { x: a.x + dx * t, y: a.y + dy * t }; }
            for (i = 0; i < gaps.length; i++) {
                var gap = gaps[i];
                var from = Math.max(0, gap.from);
                var to = Math.min(1, gap.to);
                if (from > cursor) quad(ctx, at(cursor), at(from), wall);
                drawOpening(ctx, at(from), at(to), gap.opening);
                cursor = to;
            }
            if (cursor < 1) quad(ctx, at(cursor), at(1), wall);
        }

        function quad(ctx, a, b, wall) {
            if (dist(a.x, a.y, b.x, b.y) < 0.5) return;
            var half = (wall.thickness || 15) / 2;
            var n = leftNorm(a, b);
            var poly = [
                { x: a.x + n.x * half, y: a.y + n.y * half },
                { x: b.x + n.x * half, y: b.y + n.y * half },
                { x: b.x - n.x * half, y: b.y - n.y * half },
                { x: a.x - n.x * half, y: a.y - n.y * half }
            ];
            pathOf(ctx, poly, true);
            ctx.fill();
            if (wall.hatch !== 'none') hatch(ctx, poly, wall.hatch || 'diagonal', wall.color || '#97a0af');
            pathOf(ctx, poly, true);
            ctx.stroke();
        }

        function hatch(ctx, poly, kind, color) {
            var minX = poly[0].x;
            var minY = poly[0].y;
            var maxX = poly[0].x;
            var maxY = poly[0].y;
            var i;
            for (i = 1; i < poly.length; i++) {
                minX = Math.min(minX, poly[i].x);
                minY = Math.min(minY, poly[i].y);
                maxX = Math.max(maxX, poly[i].x);
                maxY = Math.max(maxY, poly[i].y);
            }
            ctx.save();
            pathOf(ctx, poly, true);
            ctx.clip();
            ctx.strokeStyle = color;
            ctx.lineWidth = 0.35;
            ctx.beginPath();
            if (kind === 'brick') {
                var y;
                var row = 0;
                for (y = minY; y <= maxY; y += 6) {
                    var shift = row % 2 ? 8 : 0;
                    var x;
                    ctx.moveTo(minX, y);
                    ctx.lineTo(maxX, y);
                    for (x = minX + shift; x <= maxX; x += 16) {
                        ctx.moveTo(x, y);
                        ctx.lineTo(x, Math.min(maxY, y + 6));
                    }
                    row++;
                }
            } else if (kind === 'concrete') {
                var cx;
                var cy;
                for (cy = minY; cy <= maxY; cy += 4) {
                    for (cx = minX; cx <= maxX; cx += 4) {
                        ctx.moveTo(cx, cy);
                        ctx.lineTo(cx + 0.6, cy);
                    }
                }
            } else {
                var d;
                for (d = minX - (maxY - minY); d < maxX; d += 4) {
                    ctx.moveTo(d, minY);
                    ctx.lineTo(d + (maxY - minY), maxY);
                }
            }
            ctx.stroke();
            ctx.restore();
        }

        function drawOpening(ctx, a, b, opening) {
            ctx.save();
            ctx.strokeStyle = '#172b4d';
            ctx.lineWidth = 0.8;
            if (opening.kind === 'window') {
                var n = leftNorm(a, b);
                ctx.beginPath();
                ctx.moveTo(a.x + n.x * 2, a.y + n.y * 2);
                ctx.lineTo(b.x + n.x * 2, b.y + n.y * 2);
                ctx.moveTo(a.x - n.x * 2, a.y - n.y * 2);
                ctx.lineTo(b.x - n.x * 2, b.y - n.y * 2);
                ctx.stroke();
            } else {
                var len = dist(a.x, a.y, b.x, b.y);
                var normal = leftNorm(a, b);
                if (opening.swing === 'right') {
                    normal = { x: -normal.x, y: -normal.y };
                }
                ctx.beginPath();
                ctx.moveTo(a.x, a.y);
                ctx.lineTo(a.x + normal.x * len, a.y + normal.y * len);
                var start = Math.atan2(b.y - a.y, b.x - a.x);
                var end = Math.atan2(normal.y, normal.x);
                ctx.moveTo(a.x, a.y);
                ctx.arc(a.x, a.y, len, start, end, opening.swing === 'right');
                ctx.stroke();
            }
            ctx.restore();
        }

        function drawObject(ctx, obj, floor) {
            if (!layerVisible(obj.layerId)) return;
            ctx.save();
            ctx.globalAlpha = obj.opacity == null ? 1 : obj.opacity;
            ctx.strokeStyle = obj.color || '#172b4d';
            ctx.fillStyle = obj.color || '#172b4d';
            ctx.lineWidth = obj.thickness || 1;
            if (obj.type === 'line') {
                ctx.beginPath();
                ctx.moveTo(obj.x1, obj.y1);
                ctx.lineTo(obj.x2, obj.y2);
                ctx.stroke();
            } else if (obj.type === 'rect') {
                ctx.strokeRect(obj.x, obj.y, obj.w, obj.h);
            } else if (obj.type === 'ellipse') {
                ctx.beginPath();
                ctx.ellipse(obj.x + obj.w / 2, obj.y + obj.h / 2, Math.abs(obj.w) / 2, Math.abs(obj.h) / 2, 0, 0, Math.PI * 2);
                ctx.stroke();
            } else if (obj.type === 'polyline') {
                pathOf(ctx, obj.points, obj.closed);
                ctx.stroke();
            } else if (obj.type === 'wall') {
                drawWall(ctx, obj, floor);
            } else if (obj.type === 'text') {
                ctx.font = (obj.h || 18) + 'px sans-serif';
                ctx.fillText(obj.text || '', obj.x, obj.y + (obj.h || 18));
            } else if (obj.type === 'dimension') {
                drawDimension(ctx, obj);
            } else if (obj.type === 'symbol') {
                drawParts(ctx, symbolDef(obj.symbolId), obj.x, obj.y, obj.w, obj.h);
                if (obj.assetBinding) {
                    ctx.fillStyle = '#0052cc';
                    ctx.beginPath();
                    ctx.arc(obj.x + obj.w - 3, obj.y + 3, 2.2, 0, Math.PI * 2);
                    ctx.fill();
                    var attrs = obj.assetBinding.attributes || {};
                    var lines = [];
                    var key;
                    for (key in attrs) {
                        if (lines.length < 2 && attrs[key] !== '') lines.push(key + ': ' + attrs[key]);
                    }
                    ctx.fillStyle = '#172b4d';
                    ctx.font = '8px sans-serif';
                    var n;
                    for (n = 0; n < lines.length; n++) ctx.fillText(lines[n], obj.x, obj.y + obj.h + 10 + n * 10);
                }
            }
            ctx.restore();
        }

        function drawDimension(ctx, obj) {
            var dx = obj.x2 - obj.x1;
            var dy = obj.y2 - obj.y1;
            var len = Math.sqrt(dx * dx + dy * dy) || 1;
            var ox = -dy / len * (obj.offset || 18);
            var oy = dx / len * (obj.offset || 18);
            ctx.beginPath();
            ctx.moveTo(obj.x1, obj.y1);
            ctx.lineTo(obj.x1 + ox, obj.y1 + oy);
            ctx.moveTo(obj.x2, obj.y2);
            ctx.lineTo(obj.x2 + ox, obj.y2 + oy);
            ctx.moveTo(obj.x1 + ox, obj.y1 + oy);
            ctx.lineTo(obj.x2 + ox, obj.y2 + oy);
            ctx.stroke();
            ctx.font = '12px sans-serif';
            ctx.fillText(formatLen(len), (obj.x1 + obj.x2) / 2 + ox, (obj.y1 + obj.y2) / 2 + oy - 2);
        }

        function drawFloor(ctx, floor, ghost) {
            ctx.save();
            if (ghost) ctx.globalAlpha = 0.16;
            var rooms = floor.rooms || [];
            var i;
            for (i = 0; i < rooms.length; i++) {
                var room = rooms[i];
                if (!room.points) continue;
                pathOf(ctx, room.points, true);
                ctx.fillStyle = room.color || '#e3fcef';
                ctx.globalAlpha = ghost ? 0.08 : 0.55;
                ctx.fill();
                if (!ghost) {
                    var center = centroid(room.points);
                    ctx.globalAlpha = 1;
                    ctx.fillStyle = '#172b4d';
                    ctx.font = '14px sans-serif';
                    ctx.textAlign = 'center';
                    ctx.fillText(room.name || '', center.x, center.y);
                    ctx.font = '11px sans-serif';
                    ctx.fillText((room.area / 10000).toFixed(2) + ' m2', center.x, center.y + 16);
                    ctx.textAlign = 'left';
                }
            }
            ctx.globalAlpha = ghost ? 0.16 : 1;
            var objects = floor.objects || [];
            for (i = 0; i < objects.length; i++) {
                if (objects[i].type === 'opening') continue;
                drawObject(ctx, objects[i], floor);
            }
            ctx.restore();
        }

        function paint() {
            if (!stage.clientWidth || !stage.clientHeight) return;
            var width = stage.clientWidth;
            var height = stage.clientHeight;
            var dpr = global.devicePixelRatio || 1;
            [gridC, sceneC, overC].forEach(function (canvas) {
                if (canvas.width !== Math.floor(width * dpr) || canvas.height !== Math.floor(height * dpr)) {
                    canvas.width = Math.floor(width * dpr);
                    canvas.height = Math.floor(height * dpr);
                    canvas.style.width = width + 'px';
                    canvas.style.height = height + 'px';
                }
            });
            var grid = gridC.getContext('2d');
            var scene = sceneC.getContext('2d');
            var over = overC.getContext('2d');
            grid.setTransform(dpr, 0, 0, dpr, 0, 0);
            scene.setTransform(dpr, 0, 0, dpr, 0, 0);
            over.setTransform(dpr, 0, 0, dpr, 0, 0);
            grid.clearRect(0, 0, width, height);
            scene.clearRect(0, 0, width, height);
            over.clearRect(0, 0, width, height);
            function world(ctx, draw) {
                ctx.save();
                ctx.beginPath();
                ctx.rect(22, 22, width - 22, height - 22);
                ctx.clip();
                ctx.translate(22, 22);
                ctx.scale(session.cam.z, session.cam.z);
                ctx.translate(-session.cam.x, -session.cam.y);
                draw(ctx);
                ctx.restore();
            }
            if (session.gridOn) {
                world(grid, function (ctx) {
                    var step = gridStep();
                    var x0 = Math.floor(session.cam.x / step) * step;
                    var y0 = Math.floor(session.cam.y / step) * step;
                    var x1 = session.cam.x + (width - 22) / session.cam.z;
                    var y1 = session.cam.y + (height - 22) / session.cam.z;
                    ctx.strokeStyle = '#ebecf0';
                    ctx.lineWidth = 1 / session.cam.z;
                    ctx.beginPath();
                    var x;
                    var y;
                    var count = 0;
                    for (x = x0; x <= x1 && count < 400; x += step) {
                        ctx.moveTo(x, y0);
                        ctx.lineTo(x, y1);
                        count++;
                    }
                    for (y = y0; y <= y1 && count < 800; y += step) {
                        ctx.moveTo(x0, y);
                        ctx.lineTo(x1, y);
                        count++;
                    }
                    ctx.stroke();
                });
            }
            world(scene, function (ctx) {
                var floors = session.doc.floors || [];
                var i;
                if (session.doc.settings.showOtherFloors) {
                    for (i = 0; i < floors.length; i++) {
                        if (floors[i].id !== session.doc.activeFloorId) drawFloor(ctx, floors[i], true);
                    }
                }
                drawFloor(ctx, activeFloor(), false);
            });
            world(over, function (ctx) {
                var objects = selectedObjects();
                ctx.strokeStyle = '#0052cc';
                ctx.lineWidth = 1 / session.cam.z;
                ctx.setLineDash([6 / session.cam.z, 4 / session.cam.z]);
                objects.forEach(function (obj) {
                    if (obj.points) pathOf(ctx, obj.points, !!obj.closed);
                    else if (obj.x1 != null) {
                        ctx.beginPath();
                        ctx.moveTo(obj.x1, obj.y1);
                        ctx.lineTo(obj.x2, obj.y2);
                    } else if (obj.w != null) ctx.strokeRect(obj.x, obj.y, obj.w, obj.h);
                    ctx.stroke();
                });
                ctx.setLineDash([]);
                if (session.draft && session.draft.points) {
                    if (session.draft.tool === 'wall') {
                        drawWall(ctx, {
                            points: session.draft.points,
                            thickness: 15,
                            color: '#172b4d',
                            hatch: 'diagonal',
                            opacity: 1
                        }, { objects: [] });
                    } else {
                        ctx.strokeStyle = '#0052cc';
                        ctx.lineWidth = 1;
                        pathOf(ctx, session.draft.points, false);
                        ctx.stroke();
                    }
                    ctx.strokeStyle = '#0052cc';
                    ctx.lineWidth = 1;
                    ctx.strokeRect(session.draft.points[0].x - 4 / session.cam.z, session.draft.points[0].y - 4 / session.cam.z, 8 / session.cam.z, 8 / session.cam.z);
                } else if (session.draft && session.draft.phase === 1) {
                    ctx.strokeStyle = '#0052cc';
                    ctx.lineWidth = 1;
                    if (session.draft.tool === 'line' || session.draft.tool === 'dimension') {
                        ctx.beginPath();
                        ctx.moveTo(session.draft.x1, session.draft.y1);
                        ctx.lineTo(session.draft.x2, session.draft.y2);
                        ctx.stroke();
                    } else {
                        ctx.strokeRect(
                            Math.min(session.draft.x1, session.draft.x2),
                            Math.min(session.draft.y1, session.draft.y2),
                            Math.abs(session.draft.x2 - session.draft.x1),
                            Math.abs(session.draft.y2 - session.draft.y1)
                        );
                    }
                }
                if (session.marquee) {
                    ctx.strokeStyle = '#0052cc';
                    ctx.strokeRect(session.marquee.x, session.marquee.y, session.marquee.w, session.marquee.h);
                }
                if (session.osnap) {
                    var marker = screenFrom(session.osnap.x, session.osnap.y);
                    drawMarker(over, marker.x, marker.y, session.osnap.kind);
                }
            });
            drawRulers(over, width, height);
            var live = liveLength();
            if (live) badge(over, live.x, live.y, live.text);
            status.textContent = api.t('schemeReadout') + '  ' + (session.hover ? (roundCm(session.hover.x) + ', ' + roundCm(session.hover.y) + ' ' + (session.doc.settings.unit || 'cm')) : '') +
                '   ' + api.t('schemeGrid') + ' ' + formatLen(gridStep()) +
                (live ? '   ' + api.t('schemeLength') + ' ' + live.text : '') +
                (session.typed ? '   ' + session.typed : '');
        }

        function liveLength() {
            var a = null;
            var b = null;
            if (session.draft && session.draft.points && session.draft.points.length >= 2) {
                a = session.draft.points[session.draft.points.length - 2];
                b = session.draft.points[session.draft.points.length - 1];
            } else if (session.draft && session.draft.phase === 1) {
                a = { x: session.draft.x1, y: session.draft.y1 };
                b = { x: session.draft.x2, y: session.draft.y2 };
            }
            if (!a || !b) return null;
            var mid = screenFrom((a.x + b.x) / 2, (a.y + b.y) / 2);
            var text = formatLen(dist(a.x, a.y, b.x, b.y));
            if (session.draft.tool === 'rect' || session.draft.tool === 'ellipse') {
                text = formatLen(Math.abs(b.x - a.x)) + ' × ' + formatLen(Math.abs(b.y - a.y));
            }
            return { x: mid.x, y: mid.y, text: text };
        }

        function badge(ctx, x, y, text) {
            ctx.save();
            ctx.setTransform(global.devicePixelRatio || 1, 0, 0, global.devicePixelRatio || 1, 0, 0);
            ctx.font = '12px sans-serif';
            var width = Math.max(48, text.length * 7 + 14);
            ctx.fillStyle = '#172b4d';
            ctx.fillRect(x + 10, y - 24, width, 20);
            ctx.fillStyle = '#ffffff';
            ctx.fillText(text, x + 16, y - 10);
            ctx.restore();
        }

        function drawMarker(ctx, x, y, kind) {
            ctx.save();
            ctx.setTransform(global.devicePixelRatio || 1, 0, 0, global.devicePixelRatio || 1, 0, 0);
            ctx.lineWidth = 1.5;
            if (kind === 'endpoint') {
                ctx.strokeStyle = '#ff8b00';
                ctx.strokeRect(x - 4, y - 4, 8, 8);
            } else if (kind === 'midpoint') {
                ctx.fillStyle = '#36b37e';
                ctx.beginPath();
                ctx.moveTo(x, y - 5);
                ctx.lineTo(x + 5, y + 4);
                ctx.lineTo(x - 5, y + 4);
                ctx.closePath();
                ctx.fill();
            } else if (kind === 'intersection') {
                ctx.strokeStyle = '#de350b';
                ctx.beginPath();
                ctx.moveTo(x - 4, y - 4);
                ctx.lineTo(x + 4, y + 4);
                ctx.moveTo(x + 4, y - 4);
                ctx.lineTo(x - 4, y + 4);
                ctx.stroke();
            } else if (kind === 'perpendicular') {
                ctx.strokeStyle = '#6554c0';
                ctx.beginPath();
                ctx.moveTo(x, y - 5);
                ctx.lineTo(x + 5, y);
                ctx.lineTo(x, y + 5);
                ctx.lineTo(x - 5, y);
                ctx.closePath();
                ctx.stroke();
            } else if (kind === 'center') {
                ctx.strokeStyle = '#0052cc';
                ctx.beginPath();
                ctx.arc(x, y, 4, 0, Math.PI * 2);
                ctx.moveTo(x - 6, y);
                ctx.lineTo(x + 6, y);
                ctx.moveTo(x, y - 6);
                ctx.lineTo(x, y + 6);
                ctx.stroke();
            } else {
                ctx.strokeStyle = '#36b37e';
                ctx.beginPath();
                ctx.moveTo(x, y - 4);
                ctx.lineTo(x + 4, y);
                ctx.lineTo(x, y + 4);
                ctx.lineTo(x - 4, y);
                ctx.closePath();
                ctx.stroke();
            }
            ctx.restore();
        }

        function drawRulers(ctx, width, height) {
            ctx.save();
            ctx.setTransform(global.devicePixelRatio || 1, 0, 0, global.devicePixelRatio || 1, 0, 0);
            ctx.fillStyle = '#f4f5f7';
            ctx.fillRect(0, 0, width, 22);
            ctx.fillRect(0, 0, 22, height);
            ctx.strokeStyle = '#dfe1e6';
            ctx.beginPath();
            ctx.moveTo(0, 22);
            ctx.lineTo(width, 22);
            ctx.moveTo(22, 0);
            ctx.lineTo(22, height);
            ctx.stroke();
            ctx.fillStyle = '#6b778c';
            ctx.font = '10px sans-serif';
            var step = gridStep();
            var x = Math.floor(session.cam.x / step) * step;
            var endX = session.cam.x + width / session.cam.z;
            var guard = 0;
            for (; x < endX && guard < 200; x += step) {
                var sx = (x - session.cam.x) * session.cam.z + 22;
                ctx.fillRect(sx, 18, 1, 4);
                if (guard % 2 === 0) ctx.fillText(String(Math.round(x)), sx + 2, 12);
                guard++;
            }
            var y = Math.floor(session.cam.y / step) * step;
            var endY = session.cam.y + height / session.cam.z;
            guard = 0;
            for (; y < endY && guard < 200; y += step) {
                var sy = (y - session.cam.y) * session.cam.z + 22;
                ctx.fillRect(18, sy, 4, 1);
                guard++;
            }
            ctx.restore();
        }

        function refreshSide() {
            side.innerHTML = '';
            var floorTitle = el('div', 'asset-tree-scheme-label', api.t('schemeFloors'));
            side.appendChild(floorTitle);
            (session.doc.floors || []).forEach(function (floor) {
                var row = el('button', 'asset-tree-scheme-row' + (floor.id === session.doc.activeFloorId ? ' is-on' : ''), floor.name + ' (' + ((floor.rooms || []).length) + ')');
                row.type = 'button';
                row.addEventListener('click', function () {
                    session.doc.activeFloorId = floor.id;
                    session.selection = [];
                    refreshSide();
                    renderProps();
                    paint();
                });
                row.addEventListener('dblclick', function () {
                    if (!api.canEdit) return;
                    var next = global.prompt(api.t('name'), floor.name);
                    if (!next) return;
                    remember();
                    floor.name = next.slice(0, 80);
                    refreshSide();
                });
                side.appendChild(row);
            });
            var addFloor = el('button', 'asset-tree-scheme-mini', api.t('schemeFloorNew'));
            addFloor.type = 'button';
            addFloor.disabled = !api.canEdit;
            addFloor.addEventListener('click', function () {
                remember();
                var floor = { id: nid('floor'), name: api.t('schemeFloor') + ' ' + (session.doc.floors.length + 1), elevation: session.doc.floors.length * 300, objects: [], rooms: [] };
                session.doc.floors.push(floor);
                session.doc.activeFloorId = floor.id;
                refreshSide();
                paint();
            });
            side.appendChild(addFloor);
            var ghost = el('label', 'asset-tree-scheme-check');
            var ghostBox = el('input');
            ghostBox.type = 'checkbox';
            ghostBox.checked = !!session.doc.settings.showOtherFloors;
            ghostBox.addEventListener('change', function () {
                session.doc.settings.showOtherFloors = ghostBox.checked;
                session.dirty = true;
                paint();
            });
            ghost.appendChild(ghostBox);
            ghost.appendChild(document.createTextNode(' ' + api.t('schemeOtherFloors')));
            side.appendChild(ghost);

            side.appendChild(el('div', 'asset-tree-scheme-label', api.t('schemeLayers')));
            (session.doc.layers || []).forEach(function (layer) {
                var row = el('div', 'asset-tree-scheme-layer' + (layer.id === session.doc.activeLayerId ? ' is-on' : ''));
                var eye = el('button', 'asset-tree-scheme-mini', layer.visible === false ? '·' : '●');
                eye.type = 'button';
                eye.addEventListener('click', function () {
                    layer.visible = layer.visible === false;
                    paint();
                });
                var name = el('button', 'asset-tree-scheme-layer-name', layer.name);
                name.type = 'button';
                name.addEventListener('click', function () {
                    session.doc.activeLayerId = layer.id;
                    refreshSide();
                });
                name.addEventListener('dblclick', function () {
                    if (!api.canEdit) return;
                    var next = global.prompt(api.t('name'), layer.name);
                    if (!next) return;
                    remember();
                    layer.name = next.slice(0, 40);
                    refreshSide();
                });
                var lock = el('button', 'asset-tree-scheme-mini', layer.locked ? 'L' : '');
                lock.type = 'button';
                lock.addEventListener('click', function () {
                    layer.locked = !layer.locked;
                    refreshSide();
                    paint();
                });
                row.appendChild(eye);
                row.appendChild(name);
                row.appendChild(lock);
                side.appendChild(row);
            });
            var addLayer = el('button', 'asset-tree-scheme-mini', api.t('schemeLayerNew'));
            addLayer.type = 'button';
            addLayer.disabled = !api.canEdit;
            addLayer.addEventListener('click', function () {
                var next = global.prompt(api.t('schemeLayerNew'), '');
                if (!next) return;
                remember();
                var id = nid('layer');
                session.doc.layers.push({ id: id, name: next.slice(0, 40), visible: true, locked: false, color: '#172b4d' });
                session.doc.activeLayerId = id;
                refreshSide();
            });
            side.appendChild(addLayer);

            side.appendChild(el('div', 'asset-tree-scheme-label', api.t('schemeRooms')));
            ((activeFloor() || {}).rooms || []).forEach(function (room) {
                var row = el('button', 'asset-tree-scheme-row' + (session.selection.indexOf('room:' + room.id) >= 0 ? ' is-on' : ''), room.name + '  ' + ((room.area || 0) / 10000).toFixed(2) + ' m2');
                row.type = 'button';
                row.addEventListener('click', function () {
                    session.selection = ['room:' + room.id];
                    renderProps();
                    refreshSide();
                    paint();
                });
                row.addEventListener('dblclick', function () {
                    renameRoom(room);
                });
                side.appendChild(row);
            });

            side.appendChild(el('div', 'asset-tree-scheme-label', api.t('schemeSymbols')));
            var library = builtins(api.t).slice();
            var seenSymbol = {};
            library.forEach(function (def) { seenSymbol[def.id] = true; });
            var customId;
            for (customId in session.doc.symbols) {
                if (!seenSymbol[customId]) library.push(session.doc.symbols[customId]);
            }
            library.forEach(function (def) {
                var row = el('div', 'asset-tree-scheme-symbol-row');
                var card = el('button', 'asset-tree-scheme-symbol' + (session.symbolId === def.id ? ' is-on' : ''), def.name);
                card.type = 'button';
                card.addEventListener('click', function () {
                    session.symbolId = def.id;
                    session.tool = 'symbol';
                    markTools();
                    refreshSide();
                });
                var editCard = el('button', 'asset-tree-scheme-mini', api.t('schemeTemplateEdit'));
                editCard.type = 'button';
                editCard.disabled = !api.canEdit;
                editCard.addEventListener('click', function () {
                    var current = (session.doc.symbols && session.doc.symbols[def.id]) || def;
                    openSymbolEditor(current);
                });
                row.appendChild(card);
                row.appendChild(editCard);
                side.appendChild(row);
            });
        }

        function renameRoom(room) {
            if (!api.canEdit) return;
            var next = global.prompt(api.t('name'), room.name);
            if (!next) return;
            remember();
            room.name = next.slice(0, 80);
            refreshSide();
            paint();
        }

        function renderProps() {
            props.innerHTML = '';
            props.appendChild(el('div', 'asset-tree-scheme-label', api.t('schemeProps')));
            var objects = selectedObjects();
            if (objects.length !== 1) {
                props.appendChild(el('p', 'asset-tree-scheme-muted', api.t('schemeHint')));
                return;
            }
            var obj = objects[0];
            function field(label, value, apply) {
                var wrap = el('label', 'asset-tree-scheme-field', label);
                var input = el('input');
                input.value = value == null ? '' : String(value);
                input.addEventListener('change', function () {
                    if (!api.canEdit) return;
                    remember();
                    apply(input.value);
                    paint();
                    refreshSide();
                });
                wrap.appendChild(input);
                props.appendChild(wrap);
            }
            if (obj.x != null) field('X', obj.x, function (value) { obj.x = roundCm(Number(value) || 0); });
            if (obj.y != null) field('Y', obj.y, function (value) { obj.y = roundCm(Number(value) || 0); });
            if (obj.w != null) field('W', obj.w, function (value) { obj.w = Math.max(1, roundCm(Number(value) || 1)); });
            if (obj.h != null && obj.type !== 'text') field('H', obj.h, function (value) { obj.h = Math.max(1, roundCm(Number(value) || 1)); });
            if (obj.type === 'wall') field(api.t('schemeThickness'), obj.thickness || 15, function (value) { obj.thickness = Math.max(1, roundCm(Number(value) || 15)); });
            if (obj.color) {
                var colorWrap = el('label', 'asset-tree-scheme-field', api.t('schemeColor'));
                var color = el('input');
                color.type = 'color';
                color.value = /^#[0-9a-fA-F]{6}$/.test(obj.color) ? obj.color : '#172b4d';
                color.addEventListener('change', function () {
                    remember();
                    obj.color = color.value;
                    paint();
                });
                colorWrap.appendChild(color);
                props.appendChild(colorWrap);
            }
            if (obj.text != null || obj.type === 'text') {
                field(api.t('schemeText'), obj.text || '', function (value) { obj.text = value.slice(0, 200); });
            }
            if (obj.name && obj.points && !obj.type) {
                field(api.t('name'), obj.name, function (value) { obj.name = value.slice(0, 80); });
            }
            if (obj.type === 'symbol' || (obj.points && !obj.type)) {
                props.appendChild(assetCard(obj));
            }
        }

        function assetCard(obj) {
            var box = el('div', 'asset-tree-scheme-bind');
            box.appendChild(el('div', 'asset-tree-scheme-label', api.t('schemeAssetTitle')));
            var binding = obj.assetBinding;
            if (binding) {
                box.appendChild(el('div', 'asset-tree-scheme-bind-name', binding.objectLabel || binding.objectKey || ''));
                if (binding.objectKey) box.appendChild(el('div', 'asset-tree-scheme-muted', binding.objectKey));
                var attrs = binding.attributes || {};
                var key;
                for (key in attrs) {
                    if (attrs[key] !== '') box.appendChild(el('div', 'asset-tree-scheme-muted', key + ': ' + attrs[key]));
                }
                var detach = el('button', 'asset-tree-scheme-mini', api.t('schemeUnbind'));
                detach.type = 'button';
                detach.disabled = !api.canEdit;
                detach.addEventListener('click', function () {
                    remember();
                    obj.assetBinding = null;
                    renderProps();
                    paint();
                });
                box.appendChild(detach);
            } else {
                box.appendChild(el('p', 'asset-tree-scheme-muted', api.t('schemeNone')));
            }
            var attach = el('button', 'asset-tree-scheme-save', api.t('schemeBind'));
            attach.type = 'button';
            attach.disabled = !api.canEdit;
            attach.addEventListener('click', function () { openAssetPicker(obj); });
            box.appendChild(attach);
            return box;
        }

        function bindAsset(obj, asset) {
            remember();
            obj.assetBinding = {
                workspaceId: api.projectKey,
                objectSchemaId: '',
                objectTypeId: '',
                objectId: String(asset.id),
                objectKey: asset.key || String(asset.id),
                objectLabel: asset.name || '',
                attributes: asset.attributes || {},
                boundAt: Date.now()
            };
            if (obj.name && !obj.type) obj.name = asset.name || obj.name;
            renderProps();
            refreshSide();
            paint();
        }

        function openAssetPicker(obj) {
            var back = el('div', 'asset-tree-scheme-modal');
            var dialog = el('div', 'asset-tree-scheme-dialog asset-tree-scheme-picker');
            dialog.appendChild(el('div', 'asset-tree-scheme-label', api.t('schemeAssetTitle')));
            var search = el('input', 'asset-tree-scheme-name');
            search.placeholder = api.t('schemeSearch');
            var list = el('div', 'asset-tree-scheme-asset-list');
            function renderList() {
                list.innerHTML = '';
                var query = String(search.value || '').toLowerCase();
                var count = 0;
                (api.assets || []).forEach(function (asset) {
                    var label = (asset.key ? asset.key + '  ' : '') + (asset.name || '');
                    if (query && label.toLowerCase().indexOf(query) < 0) return;
                    count++;
                    var row = el('button', 'asset-tree-scheme-row', label);
                    row.type = 'button';
                    row.addEventListener('click', function () {
                        bindAsset(obj, asset);
                        if (back.parentNode) back.parentNode.removeChild(back);
                    });
                    list.appendChild(row);
                });
                if (!count) list.appendChild(el('p', 'asset-tree-scheme-muted', api.t('schemeNone')));
            }
            search.addEventListener('input', renderList);
            var cancel = el('button', 'asset-tree-scheme-mini', api.t('cancel'));
            cancel.type = 'button';
            cancel.addEventListener('click', function () {
                if (back.parentNode) back.parentNode.removeChild(back);
            });
            dialog.appendChild(search);
            dialog.appendChild(list);
            dialog.appendChild(cancel);
            back.appendChild(dialog);
            host.appendChild(back);
            renderList();
            search.focus();
        }

        function openSymbolEditor(def) {
            var edit = clone(def);
            edit.parts = edit.parts || [];
            if (!edit.w) edit.w = 100;
            if (!edit.h) edit.h = 60;
            var editor = { tool: 'select', sel: -1, draft: null, pen: null, drag: null };
            var back = el('div', 'asset-tree-scheme-modal');
            var dialog = el('div', 'asset-tree-scheme-dialog asset-tree-scheme-editor-dialog');
            var title = el('input', 'asset-tree-scheme-name');
            title.value = edit.name || '';
            var toolRow = el('div', 'asset-tree-scheme-tools');
            var editorTools = [
                ['select', 'schemeToolSelect'],
                ['line', 'schemeToolLine'],
                ['rect', 'schemeToolRect'],
                ['ellipse', 'schemeToolEllipse'],
                ['pen', 'schemeToolPen'],
                ['text', 'schemeToolText']
            ];
            editorTools.forEach(function (item) {
                var button = el('button', 'asset-tree-scheme-tool');
                button.type = 'button';
                button.title = api.t(item[1]);
                button.setAttribute('data-editor-tool', item[0]);
                button.innerHTML = toolIcon(item[0] === 'pen' ? 'polyline' : item[0]);
                button.appendChild(el('span', null, api.t(item[1])));
                button.addEventListener('click', function () {
                    editor.tool = item[0];
                    editor.pen = null;
                    markEditorTools();
                });
                toolRow.appendChild(button);
            });
            var canvas = el('canvas', 'asset-tree-scheme-symbol-canvas');
            canvas.width = 640;
            canvas.height = 420;
            var propsBox = el('div', 'asset-tree-scheme-editor-props');
            function scaleOf() {
                return Math.min(600 / edit.w, 380 / edit.h);
            }
            function localPoint(event) {
                var rect = canvas.getBoundingClientRect();
                var scale = scaleOf();
                var x = (event.clientX - rect.left) * (canvas.width / rect.width);
                var y = (event.clientY - rect.top) * (canvas.height / rect.height);
                return {
                    x: Math.max(0, Math.min(edit.w, Math.round(((x - 16) / scale) / 5) * 5)),
                    y: Math.max(0, Math.min(edit.h, Math.round(((y - 16) / scale) / 5) * 5))
                };
            }
            function partHit(pt) {
                var i;
                for (i = edit.parts.length - 1; i >= 0; i--) {
                    var part = edit.parts[i];
                    if ((part.t === 'r' || part.t === 'e' || part.t === 't') && pt.x >= part.x && pt.y >= part.y && pt.x <= part.x + (part.w || 20) && pt.y <= part.y + (part.h || 12)) return i;
                    if (part.t === 'l' && projectSeg(part.x1, part.y1, part.x2, part.y2, pt.x, pt.y).dist <= 4) return i;
                    if (part.t === 'p' && part.points) {
                        var k;
                        for (k = 1; k < part.points.length; k++) {
                            if (projectSeg(part.points[k - 1].x, part.points[k - 1].y, part.points[k].x, part.points[k].y, pt.x, pt.y).dist <= 4) return i;
                        }
                    }
                }
                return -1;
            }
            function paintSymbol() {
                var ctx = canvas.getContext('2d');
                var scale = scaleOf();
                ctx.setTransform(1, 0, 0, 1, 0, 0);
                ctx.clearRect(0, 0, canvas.width, canvas.height);
                ctx.fillStyle = '#ffffff';
                ctx.fillRect(0, 0, canvas.width, canvas.height);
                ctx.save();
                ctx.translate(16, 16);
                ctx.scale(scale, scale);
                ctx.strokeStyle = '#ebecf0';
                ctx.lineWidth = 1 / scale;
                var g;
                for (g = 0; g <= edit.w; g += 5) {
                    ctx.beginPath();
                    ctx.moveTo(g, 0);
                    ctx.lineTo(g, edit.h);
                    ctx.stroke();
                }
                for (g = 0; g <= edit.h; g += 5) {
                    ctx.beginPath();
                    ctx.moveTo(0, g);
                    ctx.lineTo(edit.w, g);
                    ctx.stroke();
                }
                ctx.strokeStyle = '#dfe1e6';
                ctx.strokeRect(0, 0, edit.w, edit.h);
                drawParts(ctx, edit, 0, 0, edit.w, edit.h);
                if (editor.sel >= 0 && edit.parts[editor.sel]) {
                    var part = edit.parts[editor.sel];
                    ctx.strokeStyle = '#0052cc';
                    ctx.lineWidth = 1 / scale;
                    ctx.setLineDash([4 / scale, 3 / scale]);
                    if (part.t === 'l') ctx.strokeRect(Math.min(part.x1, part.x2), Math.min(part.y1, part.y2), Math.abs(part.x2 - part.x1) || 1, Math.abs(part.y2 - part.y1) || 1);
                    else if (part.t === 'p' && part.points && part.points.length) {
                        var minX = part.points[0].x;
                        var minY = part.points[0].y;
                        var maxX = minX;
                        var maxY = minY;
                        part.points.forEach(function (p) {
                            minX = Math.min(minX, p.x);
                            minY = Math.min(minY, p.y);
                            maxX = Math.max(maxX, p.x);
                            maxY = Math.max(maxY, p.y);
                        });
                        ctx.strokeRect(minX, minY, Math.max(1, maxX - minX), Math.max(1, maxY - minY));
                    } else ctx.strokeRect(part.x, part.y, part.w || 20, part.h || 12);
                    ctx.setLineDash([]);
                }
                if (editor.pen && editor.pen.length) {
                    ctx.strokeStyle = '#0052cc';
                    ctx.lineWidth = 1;
                    ctx.beginPath();
                    ctx.moveTo(editor.pen[0].x, editor.pen[0].y);
                    editor.pen.forEach(function (p) { ctx.lineTo(p.x, p.y); });
                    ctx.stroke();
                }
                ctx.restore();
                renderEditorProps();
                markEditorTools();
            }
            function renderEditorProps() {
                propsBox.innerHTML = '';
                function num(label, value, apply) {
                    var wrap = el('label', 'asset-tree-scheme-field', label);
                    var input = el('input');
                    input.value = value == null ? '' : String(value);
                    input.addEventListener('change', function () { apply(Number(input.value)); paintSymbol(); });
                    wrap.appendChild(input);
                    propsBox.appendChild(wrap);
                }
                num('W', edit.w, function (value) { edit.w = Math.max(10, value || 10); });
                num('H', edit.h, function (value) { edit.h = Math.max(10, value || 10); });
                var part = editor.sel >= 0 ? edit.parts[editor.sel] : null;
                if (!part) {
                    propsBox.appendChild(el('p', 'asset-tree-scheme-muted', api.t('schemeHint')));
                    return;
                }
                if (part.x != null) num('X', part.x, function (value) { part.x = value; });
                if (part.y != null) num('Y', part.y, function (value) { part.y = value; });
                if (part.w != null) num('W', part.w, function (value) { part.w = Math.max(1, value || 1); });
                if (part.h != null) num('H', part.h, function (value) { part.h = Math.max(1, value || 1); });
                if (part.x1 != null) {
                    num('X1', part.x1, function (value) { part.x1 = value; });
                    num('Y1', part.y1, function (value) { part.y1 = value; });
                    num('X2', part.x2, function (value) { part.x2 = value; });
                    num('Y2', part.y2, function (value) { part.y2 = value; });
                }
                num(api.t('schemeThickness'), part.s || 1, function (value) { part.s = Math.max(0.2, value || 1); });
                var colorWrap = el('label', 'asset-tree-scheme-field', api.t('schemeColor'));
                var color = el('input');
                color.type = 'color';
                color.value = /^#[0-9a-fA-F]{6}$/.test(part.c || '') ? part.c : '#172b4d';
                color.addEventListener('change', function () { part.c = color.value; paintSymbol(); });
                colorWrap.appendChild(color);
                propsBox.appendChild(colorWrap);
                if (part.t === 't') {
                    var textWrap = el('label', 'asset-tree-scheme-field', api.t('schemeText'));
                    var text = el('input');
                    text.value = part.text || '';
                    text.addEventListener('change', function () { part.text = text.value.slice(0, 80); paintSymbol(); });
                    textWrap.appendChild(text);
                    propsBox.appendChild(textWrap);
                }
                var remove = el('button', 'asset-tree-scheme-mini', api.t('schemeDeletePart'));
                remove.type = 'button';
                remove.addEventListener('click', function () {
                    edit.parts.splice(editor.sel, 1);
                    editor.sel = -1;
                    paintSymbol();
                });
                propsBox.appendChild(remove);
            }
            function markEditorTools() {
                var buttons = toolRow.querySelectorAll('button');
                var i;
                for (i = 0; i < buttons.length; i++) {
                    buttons[i].className = buttons[i].getAttribute('data-editor-tool') === editor.tool ? 'asset-tree-scheme-tool is-on' : 'asset-tree-scheme-tool';
                }
            }
            canvas.addEventListener('mousedown', function (event) {
                dialog.focus();
                var pt = localPoint(event);
                if (editor.tool === 'select') {
                    editor.sel = partHit(pt);
                    editor.drag = editor.sel >= 0 ? { x: pt.x, y: pt.y } : null;
                    paintSymbol();
                    return;
                }
                if (editor.tool === 'text') {
                    var value = global.prompt(api.t('schemeText'), '');
                    if (!value) return;
                    edit.parts.push({ t: 't', x: pt.x, y: pt.y, w: Math.max(20, value.length * 6), h: 12, text: value.slice(0, 80), c: '#172b4d', s: 1 });
                    editor.sel = edit.parts.length - 1;
                    paintSymbol();
                    return;
                }
                if (editor.tool === 'pen') {
                    if (!editor.pen) editor.pen = [pt];
                    else editor.pen.push(pt);
                    paintSymbol();
                    return;
                }
                editor.draft = pt;
            });
            canvas.addEventListener('mousemove', function (event) {
                if (!editor.drag || editor.sel < 0) return;
                var pt = localPoint(event);
                var dx = pt.x - editor.drag.x;
                var dy = pt.y - editor.drag.y;
                if (!dx && !dy) return;
                editor.drag = pt;
                var part = edit.parts[editor.sel];
                ['x', 'y', 'x1', 'y1', 'x2', 'y2'].forEach(function (key) {
                    if (part[key] != null) part[key] += dx;
                });
                if (part.points) part.points.forEach(function (p) { p.x += dx; p.y += dy; });
                paintSymbol();
            });
            canvas.addEventListener('mouseup', function (event) {
                editor.drag = null;
                if (!editor.draft || editor.tool === 'select' || editor.tool === 'pen' || editor.tool === 'text') return;
                var end = localPoint(event);
                var part = { c: '#172b4d', s: 1 };
                if (editor.tool === 'line') {
                    part.t = 'l';
                    part.x1 = editor.draft.x;
                    part.y1 = editor.draft.y;
                    part.x2 = end.x;
                    part.y2 = end.y;
                } else {
                    part.t = editor.tool === 'ellipse' ? 'e' : 'r';
                    part.x = Math.min(editor.draft.x, end.x);
                    part.y = Math.min(editor.draft.y, end.y);
                    part.w = Math.max(5, Math.abs(end.x - editor.draft.x));
                    part.h = Math.max(5, Math.abs(end.y - editor.draft.y));
                    part.f = '#ffffff';
                }
                edit.parts.push(part);
                editor.sel = edit.parts.length - 1;
                editor.draft = null;
                paintSymbol();
            });
            function finishPen() {
                if (!editor.pen || editor.pen.length < 2) {
                    editor.pen = null;
                    paintSymbol();
                    return;
                }
                edit.parts.push({ t: 'p', points: editor.pen.slice(), c: '#172b4d', s: 1 });
                editor.sel = edit.parts.length - 1;
                editor.pen = null;
                paintSymbol();
            }
            dialog.addEventListener('keydown', function (event) {
                var tag = event.target && event.target.tagName;
                if (tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT') return;
                event.stopPropagation();
                if (event.key === 'Enter' && editor.tool === 'pen') {
                    event.preventDefault();
                    finishPen();
                }
                if (event.key === 'Backspace' && editor.pen && editor.pen.length) {
                    editor.pen.pop();
                    paintSymbol();
                }
                if (event.key === 'Delete' && editor.sel >= 0) {
                    edit.parts.splice(editor.sel, 1);
                    editor.sel = -1;
                    paintSymbol();
                }
                if (event.key === 'Escape') {
                    event.preventDefault();
                    if (back.parentNode) back.parentNode.removeChild(back);
                }
            });
            var actions = el('div', 'asset-tree-scheme-head');
            var save = el('button', 'asset-tree-scheme-save', api.t('save'));
            save.type = 'button';
            save.addEventListener('click', function () {
                finishPen();
                edit.name = title.value.slice(0, 80) || edit.name || api.t('schemeTemplateNew');
                remember();
                if (!session.doc.symbols) session.doc.symbols = {};
                session.doc.symbols[edit.id] = edit;
                session.symbolId = edit.id;
                if (back.parentNode) back.parentNode.removeChild(back);
                refreshSide();
                paint();
            });
            var cancel = el('button', 'asset-tree-scheme-mini', api.t('cancel'));
            cancel.type = 'button';
            cancel.addEventListener('click', function () {
                if (back.parentNode) back.parentNode.removeChild(back);
            });
            actions.appendChild(save);
            actions.appendChild(cancel);
            var layout = el('div', 'asset-tree-scheme-editor');
            layout.appendChild(toolRow);
            layout.appendChild(canvas);
            layout.appendChild(propsBox);
            dialog.appendChild(title);
            dialog.appendChild(layout);
            dialog.appendChild(actions);
            back.appendChild(dialog);
            host.appendChild(back);
            dialog.tabIndex = 0;
            paintSymbol();
            dialog.focus();
        }

        function acceptDoc(raw, id) {
            var doc = raw || blankDoc(api.t);
            if (!doc.floors || !doc.floors.length) throw new Error('empty');
            if (!doc.settings) doc.settings = { showOtherFloors: true, unit: 'cm' };
            if (!doc.layers) doc.layers = blankDoc(api.t).layers;
            if (!doc.symbols) doc.symbols = {};
            if (!doc.activeFloorId) doc.activeFloorId = doc.floors[0].id;
            if (!doc.placeId) doc.placeId = 0;
            session.doc = doc;
            session.id = id || 0;
            session.past = [];
            session.future = [];
            session.selection = [];
            session.draft = null;
        }

        function upsertCatalog(row) {
            var i;
            for (i = 0; i < session.catalog.length; i++) {
                if (session.catalog[i].id === row.id) {
                    session.catalog[i].name = row.name;
                    return;
                }
            }
            session.catalog.push({ id: row.id, name: row.name });
        }

        function schemeLabel(row) {
            var name = (row && row.name) || api.t('schemeDefaultName');
            var same = 0;
            var i;
            for (i = 0; i < (session.catalog || []).length; i++) {
                if ((session.catalog[i].name || '') === ((row && row.name) || '')) same++;
            }
            if (same > 1 && row && row.id) return name + ' #' + row.id;
            return name;
        }

        function fillSchemePick() {
            var current = String(session.id || 0);
            var seen = false;
            schemePick.innerHTML = '';
            (session.catalog || []).forEach(function (row) {
                var option = el('option', null, schemeLabel(row));
                option.value = String(row.id);
                schemePick.appendChild(option);
                if (String(row.id) === current) seen = true;
            });
            if (!seen) {
                var fresh = el('option', null, (session.doc && session.doc.name) || api.t('schemeDefaultName'));
                fresh.value = '0';
                schemePick.appendChild(fresh);
            }
            schemePick.value = seen ? current : '0';
        }

        function fillPlacePick() {
            var value = String((session.doc && session.doc.placeId) || 0);
            placePick.innerHTML = '';
            var none = el('option', null, api.t('schemePlaceNone'));
            none.value = '0';
            placePick.appendChild(none);
            (api.places || []).forEach(function (place) {
                var option = el('option', null, place.name || String(place.id));
                option.value = String(place.id);
                placePick.appendChild(option);
            });
            placePick.value = value;
            if (placePick.value !== value) placePick.value = '0';
        }

        function openScheme(id) {
            api.ajax('GET', '/projects/' + encodeURIComponent(api.projectKey) + '/schemes/' + id, null, function (code, payload) {
                if (code !== 200 || !payload || !payload.body) {
                    if (session.statusNode) session.statusNode.textContent = api.t('schemeSaveFailed');
                    return;
                }
                try {
                    acceptDoc(JSON.parse(payload.body), payload.id);
                } catch (error) {
                    if (session.statusNode) session.statusNode.textContent = api.t('schemeSaveFailed');
                    return;
                }
                if (session.repaint) session.repaint();
            });
        }

        function save(done) {
            if (!api.canEdit) return;
            session.doc.name = nameInput.value || session.doc.name;
            var path = '/projects/' + encodeURIComponent(api.projectKey) + '/schemes';
            var method = session.id ? 'PUT' : 'POST';
            if (session.id) path += '/' + session.id;
            api.ajax(method, path, { name: session.doc.name, body: JSON.stringify(session.doc) }, function (code, payload) {
                if (code >= 200 && code < 300 && payload && payload.id) {
                    session.id = payload.id;
                    session.dirty = false;
                    upsertCatalog({ id: payload.id, name: session.doc.name });
                    fillSchemePick();
                    status.textContent = api.t('schemeSaved');
                    if (done) done();
                } else {
                    status.textContent = (payload && payload.message) || api.t('schemeSaveFailed');
                    fillSchemePick();
                }
            });
        }

        function show() {
            if (session.repaint) session.repaint();
        }

        function load() {
            if (session.loaded || session.loading) {
                paint();
                return;
            }
            session.loading = true;
            api.ajax('GET', '/projects/' + encodeURIComponent(api.projectKey) + '/schemes', null, function (code, list) {
                session.loading = false;
                session.loaded = true;
                session.catalog = code === 200 && list && list.length ? list : (session.catalog || []);
                if (code === 200 && list && list.length) {
                    openScheme(list[0].id);
                    return;
                }
                show();
            });
        }

        var drag = null;
        stage.addEventListener('mousedown', function (event) {
            if (event.button !== 2) hideTemplateMenu();
            host.focus();
            var rect = overC.getBoundingClientRect();
            var sx = event.clientX - rect.left;
            var sy = event.clientY - rect.top;
            if (event.button === 2) {
                drag = { mode: 'rmb', x: event.clientX, y: event.clientY, camX: session.cam.x, camY: session.cam.y };
                event.preventDefault();
                return;
            }
            if (event.button === 1 || (event.button === 0 && session.space)) {
                drag = { mode: 'pan', x: event.clientX, y: event.clientY, camX: session.cam.x, camY: session.cam.y };
                event.preventDefault();
                return;
            }
            if (event.button !== 0) return;
            var pt = pointer(event);
            if (session.tool === 'select') {
                var handle = handleAt(sx, sy);
                if (handle && api.canEdit) {
                    remember();
                    drag = { mode: 'handle', handle: handle, moved: false };
                    return;
                }
                var id = hit(pt.x, pt.y);
                if (id) {
                    if (event.shiftKey) {
                        var at = session.selection.indexOf(id);
                        if (at >= 0) session.selection.splice(at, 1);
                        else session.selection.push(id);
                    } else if (session.selection.indexOf(id) < 0) session.selection = [id];
                    if (api.canEdit) {
                        remember();
                        drag = { mode: 'move', x: pt.x, y: pt.y, lastX: pt.x, lastY: pt.y, moved: false };
                    }
                } else {
                    if (!event.shiftKey) session.selection = [];
                    drag = { mode: 'marquee', x: pt.x, y: pt.y };
                    session.marquee = { x: pt.x, y: pt.y, w: 0, h: 0 };
                }
                renderProps();
                refreshSide();
                paint();
                return;
            }
            if (!api.canEdit) return;
            if (session.tool === 'text') {
                var value = global.prompt(api.t('schemeText'), '');
                if (!value) return;
                remember();
                var text = baseObject('text');
                text.x = pt.x;
                text.y = pt.y;
                text.h = 18;
                text.w = Math.max(40, value.length * 8);
                text.text = value.slice(0, 200);
                text.layerId = 'notes';
                activeFloor().objects.push(text);
                session.selection = [text.id];
                renderProps();
                paint();
                return;
            }
            if (session.tool === 'symbol') {
                placeSymbol(pt);
                return;
            }
            if (session.tool === 'door' || session.tool === 'window') {
                placeOpening(session.tool, pt);
                return;
            }
            if (session.tool === 'polyline' || session.tool === 'wall') {
                if (!session.draft) session.draft = { tool: session.tool, points: [pt, { x: pt.x, y: pt.y }] };
                else {
                    session.draft.points[session.draft.points.length - 1] = { x: pt.x, y: pt.y };
                    var first = session.draft.points[0];
                    var back = dist(pt.x, pt.y, first.x, first.y) <= 12 / session.cam.z;
                    if (back && session.draft.points.length >= 4) finishPoly(true);
                    else if (!back) session.draft.points.push({ x: pt.x, y: pt.y });
                }
                paint();
                return;
            }
            if (!session.draft) session.draft = { tool: session.tool, phase: 1, x1: pt.x, y1: pt.y, x2: pt.x, y2: pt.y };
            else commitTwo(session.draft.tool, session.draft.x1, session.draft.y1, pt.x, pt.y);
            paint();
        });

        stage.addEventListener('mousemove', function (event) {
            var pt = pointer(event);
            session.hover = pt;
            if (!drag) {
                if (session.draft && session.draft.points) {
                    session.draft.points[session.draft.points.length - 1] = pt;
                } else if (session.draft && session.draft.phase === 1) {
                    var x2 = pt.x;
                    var y2 = pt.y;
                    if ((event.shiftKey || session.orthoOn) && (session.draft.tool === 'rect')) {
                        var side = Math.max(Math.abs(x2 - session.draft.x1), Math.abs(y2 - session.draft.y1));
                        x2 = session.draft.x1 + side * (x2 >= session.draft.x1 ? 1 : -1);
                        y2 = session.draft.y1 + side * (y2 >= session.draft.y1 ? 1 : -1);
                    }
                    session.draft.x2 = x2;
                    session.draft.y2 = y2;
                }
                paint();
                return;
            }
            if (drag.mode === 'rmb') {
                if (Math.abs(event.clientX - drag.x) + Math.abs(event.clientY - drag.y) <= 16) return;
                drag.mode = 'pan';
            }
            if (drag.mode === 'pan') {
                session.cam.x = drag.camX - (event.clientX - drag.x) / session.cam.z;
                session.cam.y = drag.camY - (event.clientY - drag.y) / session.cam.z;
                paint();
                return;
            }
            if (drag.mode === 'move') {
                var dx = pt.x - drag.lastX;
                var dy = pt.y - drag.lastY;
                drag.lastX = pt.x;
                drag.lastY = pt.y;
                drag.moved = true;
                selectedObjects().forEach(function (obj) { translateObj(obj, dx, dy); });
                var floor = activeFloor();
                if ((floor.objects || []).some(function (obj) { return obj.type === 'wall' && session.selection.indexOf(obj.id) >= 0; })) syncRooms(floor);
                paint();
                return;
            }
            if (drag.mode === 'handle') {
                var target = null;
                var all = (activeFloor().objects || []).concat(activeFloor().rooms || []);
                var i;
                for (i = 0; i < all.length; i++) {
                    if (all[i].id === drag.handle.id || ('room:' + all[i].id) === drag.handle.id) target = all[i];
                }
                if (target && target.points && target.points[drag.handle.index]) {
                    target.points[drag.handle.index] = { x: roundCm(pt.x), y: roundCm(pt.y) };
                    if (target.type === 'wall') syncRooms(activeFloor());
                } else if (target && target.x1 != null) {
                    if (drag.handle.index === 0) { target.x1 = roundCm(pt.x); target.y1 = roundCm(pt.y); }
                    else { target.x2 = roundCm(pt.x); target.y2 = roundCm(pt.y); }
                } else if (target && target.w != null) {
                    var x = target.x;
                    var y = target.y;
                    var r = target.x + target.w;
                    var b = target.y + target.h;
                    if (drag.handle.index === 0) { x = pt.x; y = pt.y; }
                    if (drag.handle.index === 1) { r = pt.x; y = pt.y; }
                    if (drag.handle.index === 2) { r = pt.x; b = pt.y; }
                    if (drag.handle.index === 3) { x = pt.x; b = pt.y; }
                    target.x = roundCm(Math.min(x, r));
                    target.y = roundCm(Math.min(y, b));
                    target.w = roundCm(Math.max(1, Math.abs(r - x)));
                    target.h = roundCm(Math.max(1, Math.abs(b - y)));
                }
                paint();
                return;
            }
            if (drag.mode === 'marquee') {
                session.marquee = {
                    x: Math.min(drag.x, pt.x),
                    y: Math.min(drag.y, pt.y),
                    w: Math.abs(pt.x - drag.x),
                    h: Math.abs(pt.y - drag.y)
                };
                paint();
            }
        });

        global.addEventListener('mouseup', onUp);
        function onUp(event) {
            if (!drag) return;
            if (drag.mode === 'rmb') {
                var pt = pointer(event);
                var id = hit(pt.x, pt.y);
                if (id && session.selection.indexOf(id) < 0) session.selection = [id];
                drag = null;
                renderProps();
                paint();
                showTemplateMenu(event.clientX, event.clientY);
                return;
            }
            if (drag.mode === 'marquee' && session.marquee) {
                var box = session.marquee;
                var ids = [];
                (activeFloor().objects || []).forEach(function (obj) {
                    var boxObj = bounds(obj);
                    if (!boxObj) return;
                    if (boxObj.x < box.x + box.w && boxObj.x + boxObj.w > box.x && boxObj.y < box.y + box.h && boxObj.y + boxObj.h > box.y) ids.push(obj.id);
                });
                session.selection = ids;
                session.marquee = null;
                renderProps();
            }
            if (drag.mode === 'move' && !drag.moved && session.past.length) session.past.pop();
            drag = null;
            paint();
        }

        function bounds(obj) {
            if (obj.x != null && obj.w != null) return { x: obj.x, y: obj.y, w: obj.w, h: obj.h || 10 };
            if (obj.x1 != null) return { x: Math.min(obj.x1, obj.x2), y: Math.min(obj.y1, obj.y2), w: Math.abs(obj.x2 - obj.x1), h: Math.abs(obj.y2 - obj.y1) };
            if (obj.points && obj.points.length) {
                var minX = obj.points[0].x;
                var minY = obj.points[0].y;
                var maxX = minX;
                var maxY = minY;
                obj.points.forEach(function (p) {
                    minX = Math.min(minX, p.x);
                    minY = Math.min(minY, p.y);
                    maxX = Math.max(maxX, p.x);
                    maxY = Math.max(maxY, p.y);
                });
                return { x: minX, y: minY, w: maxX - minX, h: maxY - minY };
            }
            return null;
        }

        function hideTemplateMenu() {
            var old = host.querySelector('.asset-tree-scheme-menu');
            if (old && old.parentNode) old.parentNode.removeChild(old);
        }

        function partsOf(obj) {
            if (obj.type === 'line' || obj.type === 'dimension') {
                return [{ t: 'l', x1: obj.x1, y1: obj.y1, x2: obj.x2, y2: obj.y2, c: obj.color || '#172b4d', s: obj.thickness || 1 }];
            }
            if (obj.type === 'rect' || obj.type === 'ellipse') {
                return [{ t: obj.type === 'ellipse' ? 'e' : 'r', x: obj.x, y: obj.y, w: obj.w, h: obj.h, c: obj.color || '#172b4d', f: obj.fill || '', s: obj.thickness || 1 }];
            }
            if (obj.type === 'text') {
                return [{ t: 't', x: obj.x, y: obj.y, w: obj.w || 40, h: obj.h || 18, text: obj.text || '', c: obj.color || '#172b4d', s: 1 }];
            }
            if ((obj.type === 'polyline' || obj.type === 'wall') && obj.points && obj.points.length) {
                var points = obj.points.map(function (p) { return { x: p.x, y: p.y }; });
                if (obj.closed && points.length > 2) points.push({ x: points[0].x, y: points[0].y });
                return [{ t: 'p', points: points, c: obj.color || '#172b4d', s: obj.type === 'wall' ? (obj.thickness || 15) : (obj.thickness || 1) }];
            }
            if (obj.type === 'symbol') {
                var def = symbolDef(obj.symbolId);
                var sx = (obj.w || def.w || 1) / (def.w || 1);
                var sy = (obj.h || def.h || 1) / (def.h || 1);
                return (def.parts || []).map(function (part) {
                    var next = clone(part);
                    if (next.x != null) { next.x = obj.x + next.x * sx; next.w = (next.w || 0) * sx; }
                    if (next.y != null) { next.y = obj.y + next.y * sy; next.h = (next.h || 0) * sy; }
                    if (next.x1 != null) {
                        next.x1 = obj.x + next.x1 * sx;
                        next.y1 = obj.y + next.y1 * sy;
                        next.x2 = obj.x + next.x2 * sx;
                        next.y2 = obj.y + next.y2 * sy;
                    }
                    if (next.points) {
                        next.points = next.points.map(function (p) { return { x: obj.x + p.x * sx, y: obj.y + p.y * sy }; });
                    }
                    if (next.s) next.s = next.s * ((Math.abs(sx) + Math.abs(sy)) / 2);
                    return next;
                });
            }
            return [];
        }

        function shiftPart(part, ox, oy) {
            var next = clone(part);
            if (next.x != null) next.x = roundCm(next.x - ox);
            if (next.y != null) next.y = roundCm(next.y - oy);
            if (next.w != null) next.w = roundCm(next.w);
            if (next.h != null) next.h = roundCm(next.h);
            if (next.x1 != null) {
                next.x1 = roundCm(next.x1 - ox);
                next.y1 = roundCm(next.y1 - oy);
                next.x2 = roundCm(next.x2 - ox);
                next.y2 = roundCm(next.y2 - oy);
            }
            if (next.points) next.points = next.points.map(function (p) { return { x: roundCm(p.x - ox), y: roundCm(p.y - oy) }; });
            return next;
        }

        function makeTemplateFromSelection(name) {
            var objects = selectedObjects().filter(function (obj) { return obj.type && obj.type !== 'opening'; });
            name = String(name || '').replace(/^\s+|\s+$/g, '');
            if (!api.canEdit || !objects.length || !name) return;
            var box = null;
            var pad = 1;
            var i;
            for (i = 0; i < objects.length; i++) {
                var one = bounds(objects[i]);
                if (!one) continue;
                var extra = objects[i].type === 'wall' ? (objects[i].thickness || 15) / 2 : (objects[i].thickness || 1) / 2;
                if (extra > pad) pad = extra;
                if (!box) box = { x: one.x, y: one.y, r: one.x + one.w, b: one.y + one.h };
                else {
                    box.x = Math.min(box.x, one.x);
                    box.y = Math.min(box.y, one.y);
                    box.r = Math.max(box.r, one.x + one.w);
                    box.b = Math.max(box.b, one.y + one.h);
                }
            }
            if (!box) return;
            box.x -= pad;
            box.y -= pad;
            var parts = [];
            objects.forEach(function (obj) {
                partsOf(obj).forEach(function (part) { parts.push(shiftPart(part, box.x, box.y)); });
            });
            if (!parts.length) return;
            remember();
            var def = {
                id: nid('sym'),
                name: name.slice(0, 80),
                w: Math.max(5, roundCm(box.r - box.x + pad)),
                h: Math.max(5, roundCm(box.b - box.y + pad)),
                parts: parts
            };
            if (!session.doc.symbols) session.doc.symbols = {};
            session.doc.symbols[def.id] = def;
            session.symbolId = def.id;
            refreshSide();
            paint();
            status.textContent = api.t('schemeTemplateSaved');
        }

        function showTemplateMenu(clientX, clientY) {
            hideTemplateMenu();
            if (!api.canEdit) return;
            var objects = selectedObjects().filter(function (obj) { return obj.type && obj.type !== 'opening'; });
            if (!objects.length) return;
            var menu = el('div', 'asset-tree-scheme-menu');
            var suggested = objects.length === 1 && objects[0].type === 'symbol' ? (symbolDef(objects[0].symbolId).name || '') : api.t('schemeTemplateNew');
            var field = el('input', 'asset-tree-scheme-name');
            field.value = suggested;
            field.placeholder = api.t('name');
            var item = el('button', 'asset-tree-scheme-menu-item', api.t('schemeMakeTemplate'));
            item.type = 'button';
            function commit() {
                var value = field.value;
                hideTemplateMenu();
                makeTemplateFromSelection(value);
            }
            item.addEventListener('click', function (event) {
                event.preventDefault();
                event.stopPropagation();
                commit();
            });
            field.addEventListener('keydown', function (event) {
                event.stopPropagation();
                if (event.key === 'Enter') {
                    event.preventDefault();
                    commit();
                }
            });
            menu.addEventListener('mousedown', function (event) { event.stopPropagation(); });
            menu.appendChild(field);
            menu.appendChild(item);
            host.appendChild(menu);
            var hostRect = host.getBoundingClientRect();
            var left = clientX - hostRect.left;
            var top = clientY - hostRect.top;
            if (left + 200 > host.clientWidth) left = Math.max(8, host.clientWidth - 200);
            if (top + 88 > host.clientHeight) top = Math.max(8, host.clientHeight - 88);
            menu.style.left = left + 'px';
            menu.style.top = top + 'px';
            field.focus();
            field.select();
        }

        stage.addEventListener('wheel', function (event) {
            event.preventDefault();
            var rect = overC.getBoundingClientRect();
            var sx = event.clientX - rect.left;
            var sy = event.clientY - rect.top;
            var before = worldFrom(sx, sy);
            var factor = event.deltaY < 0 ? 1.1 : 1 / 1.1;
            session.cam.z = Math.max(0.02, Math.min(20, session.cam.z * factor));
            var after = worldFrom(sx, sy);
            session.cam.x += before.x - after.x;
            session.cam.y += before.y - after.y;
            paint();
        }, { passive: false });

        stage.addEventListener('dblclick', function () {
            if (session.draft && session.draft.points && session.draft.points.length >= 3) finishPoly(false);
        });

        stage.addEventListener('contextmenu', function (event) { event.preventDefault(); });

        function onKey(event) {
            if (!host.parentNode) {
                document.removeEventListener('keydown', onKey);
                return;
            }
            if (host.querySelector('.asset-tree-scheme-modal')) return;
            var tag = event.target && event.target.tagName;
            if (tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT') return;
            if (event.key === ' ' ) {
                session.space = true;
                event.preventDefault();
                return;
            }
            if ((event.ctrlKey || event.metaKey) && (event.key === 'z' || event.key === 'Z')) {
                event.preventDefault();
                if (event.shiftKey) redo();
                else undo();
                return;
            }
            if ((event.ctrlKey || event.metaKey) && (event.key === 'y' || event.key === 'Y')) {
                event.preventDefault();
                redo();
                return;
            }
            if ((event.ctrlKey || event.metaKey) && (event.key === 'd' || event.key === 'D')) {
                event.preventDefault();
                duplicateSelection();
                return;
            }
            if ((event.ctrlKey || event.metaKey) && (event.key === 'a' || event.key === 'A')) {
                event.preventDefault();
                session.selection = (activeFloor().objects || []).map(function (obj) { return obj.id; });
                renderProps();
                paint();
                return;
            }
            if (event.key === 'Escape') {
                hideTemplateMenu();
                session.draft = null;
                session.selection = [];
                session.typed = '';
                renderProps();
                paint();
                return;
            }
            if (event.key === 'Enter') {
                if (session.typed) applyTyped();
                else if (session.draft && session.draft.points) finishPoly(false);
                return;
            }
            if (event.key === 'Backspace' && session.draft && session.draft.points && session.draft.points.length > 2) {
                session.draft.points.splice(session.draft.points.length - 2, 1);
                paint();
                return;
            }
            if (event.key === 'Delete') {
                removeSelection();
                return;
            }
            if (event.key === 'F2') {
                var rooms = selectedObjects();
                if (rooms.length === 1 && rooms[0].points && !rooms[0].type) renameRoom(rooms[0]);
                return;
            }
            if (event.key === 'F7') { session.gridOn = !session.gridOn; paint(); return; }
            if (event.key === 'F8') { session.orthoOn = !session.orthoOn; paint(); return; }
            if (event.key === 'F9') { session.snapOn = !session.snapOn; paint(); return; }
            if (event.key === 'F3') { session.osnapOn = !session.osnapOn; paint(); return; }
            if (event.key === 'Tab' && session.osnapList && session.osnapList.length) {
                event.preventDefault();
                session.osnapIndex = (session.osnapIndex + 1) % session.osnapList.length;
                return;
            }
            var hot = { v: 'select', l: 'line', r: 'rect', e: 'ellipse', p: 'polyline', w: 'wall', t: 'text', d: 'dimension' };
            if (!event.ctrlKey && !event.metaKey && !event.altKey && hot[event.key]) {
                session.tool = hot[event.key];
                session.draft = null;
                markTools();
                paint();
                return;
            }
            if (/^[0-9.,-]$/.test(event.key) && session.draft) {
                session.typed += event.key;
                paint();
            }
        }
        if (global.__schemeKey) document.removeEventListener('keydown', global.__schemeKey);
        global.__schemeKey = onKey;
        document.addEventListener('keydown', onKey);
        document.addEventListener('keyup', function (event) {
            if (event.key === ' ') session.space = false;
        });

        nameInput.addEventListener('change', function () {
            session.doc.name = nameInput.value.slice(0, 80) || session.doc.name;
            session.dirty = true;
            if (session.id) upsertCatalog({ id: session.id, name: session.doc.name });
            fillSchemePick();
        });
        unit.addEventListener('change', function () {
            session.doc.settings.unit = unit.value;
            session.dirty = true;
            paint();
        });
        placePick.addEventListener('change', function () {
            session.doc.placeId = Number(placePick.value) || 0;
            session.dirty = true;
        });
        schemePick.addEventListener('change', function () {
            var next = Number(schemePick.value);
            if (!next || next === session.id) return;
            var target = next;
            if (session.dirty && api.canEdit) save(function () { openScheme(target); });
            else openScheme(target);
        });
        saveBtn.addEventListener('click', function () { save(); });
        newBtn.addEventListener('click', function () {
            var startFresh = function () {
                var fresh = blankDoc(api.t);
                fresh.name = api.t('schemeDefaultName') + ' ' + (session.catalog.length + 1);
                fresh.activeFloorId = fresh.floors[0].id;
                acceptDoc(fresh, 0);
                session.dirty = true;
                if (session.repaint) session.repaint();
            };
            if (session.dirty && api.canEdit) save(startFresh);
            else startFresh();
        });

        var timer = global.setInterval(function () {
            if (!host.parentNode) {
                global.clearInterval(timer);
                return;
            }
            if (session.dirty && api.canEdit && session.loaded) save();
        }, 30000);
        global.addEventListener('resize', paint);
        session.repaint = function () {
            if (!host.parentNode) return;
            nameInput.value = session.doc.name || '';
            if (unit.value !== (session.doc.settings.unit || 'cm')) unit.value = session.doc.settings.unit || 'cm';
            fillSchemePick();
            fillPlacePick();
            markTools();
            refreshSide();
            renderProps();
            paint();
        };
        session.statusNode = status;
        markTools();
        fillSchemePick();
        fillPlacePick();
        refreshSide();
        renderProps();
        load();
        global.setTimeout(paint, 30);
    }

    var exported = {
        mount: mount,
        polygonArea: polygonArea,
        centroid: centroid,
        offsetPoints: offsetPoints
    };
    if (typeof module !== 'undefined' && module.exports) module.exports = exported;
    global.AssetTreeSchemes = exported;
})(typeof window !== 'undefined' ? window : this);
