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
                mode: 'browse',
                templateId: '',
                snapOn: true,
                orthoOn: false,
                osnapOn: true,
                gridOn: true,
                space: false,
                stamp: '',
                symbolId: '',
                assetId: 0,
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
        if (!session.mode) session.mode = 'browse';
        if (api.focusId) session.pendingFocus = api.focusId;

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
            ['window', '', 'schemeToolWindow'],
            ['marker', 'M', 'schemeToolMarker'],
            ['dropper', 'I', 'schemeToolDropper']
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
        if (!session.ink) {
            try {
                var savedInk = JSON.parse(global.localStorage.getItem('assetshub.schemeInk') || '{}') || {};
                if (savedInk.color) session.ink = savedInk.color;
                if (savedInk.fill != null) session.inkFill = !!savedInk.fill;
            } catch (error) { /* the default ink still works */ }
        }
        if (session.inkFill == null) session.inkFill = true;
        var palette = el('div', 'asset-tree-scheme-palette');
        var swatches = el('div', 'asset-tree-scheme-swatches');
        var inkColors = ['#172b4d', '#de350b', '#ff8b00', '#ffab00', '#36b37e', '#00b8d9', '#0052cc', '#6554c0', '#ffe380'];
        function rememberInk() {
            try {
                global.localStorage.setItem('assetshub.schemeInk', JSON.stringify({ color: session.ink || '', fill: !!session.inkFill }));
            } catch (error) { /* the choice still applies in this tab */ }
        }
        function markSwatches() {
            var chips = swatches.querySelectorAll('[data-ink]');
            var i;
            for (i = 0; i < chips.length; i++) {
                chips[i].classList.toggle('is-on', chips[i].getAttribute('data-ink') === (session.ink || ''));
            }
        }
        function chooseInk(color, custom) {
            session.ink = color;
            rememberInk();
            markSwatches();
            if (custom) showCustom(color);
            if (selectedObjects().length) paintSelection(color);
            else paint();
        }
        function showCustom(color) {
            if (!/^#[0-9a-fA-F]{6}$/.test(color || '')) return;
            customChip.classList.add('is-set');
            customChip.style.background = color;
            customInk.value = color;
        }
        inkColors.forEach(function (color) {
            var chip = el('span', 'asset-tree-scheme-swatch');
            chip.title = api.t('schemeColor');
            chip.setAttribute('data-ink', color);
            chip.style.background = color;
            chip.addEventListener('click', function () { chooseInk(color); });
            swatches.appendChild(chip);
        });
        var customChip = el('span', 'asset-tree-scheme-swatch is-custom');
        customChip.title = api.t('schemeColor');
        var customInk = el('input', 'asset-tree-scheme-swatch-input');
        customInk.type = 'color';
        customInk.value = '#ff5630';
        customInk.addEventListener('input', function () {
            chooseInk(customInk.value, true);
        });
        customChip.appendChild(customInk);
        swatches.appendChild(customChip);
        var fillInk = el('label', 'asset-tree-scheme-ink');
        var fillBox = el('input');
        fillBox.type = 'checkbox';
        fillBox.checked = !!session.inkFill;
        fillBox.addEventListener('change', function () {
            session.inkFill = fillBox.checked;
            rememberInk();
        });
        fillInk.appendChild(fillBox);
        fillInk.appendChild(document.createTextNode(api.t('schemeFill')));
        palette.appendChild(swatches);
        palette.appendChild(fillInk);
        tools.appendChild(palette);
        var scanFile = el('input');
        scanFile.type = 'file';
        scanFile.accept = '.dxf,.dwg,.pdf,.png,.jpg,.jpeg,application/pdf,image/png,image/jpeg';
        scanFile.style.display = 'none';
        var importBtn = el('button', 'asset-tree-scheme-tool', api.t('mapImport'));
        importBtn.type = 'button';
        importBtn.disabled = !api.canEdit;
        importBtn.addEventListener('click', function () {
            if (session.mode !== 'edit') return;
            scanFile.click();
        });
        tools.appendChild(importBtn);
        tools.appendChild(scanFile);
        var exportDxfBtn = el('button', 'asset-tree-scheme-tool', api.t('mapExportDxf'));
        exportDxfBtn.type = 'button';
        exportDxfBtn.addEventListener('click', function () { exportScheme('dxf'); });
        var exportPdfBtn = el('button', 'asset-tree-scheme-tool', api.t('mapExportPdf'));
        exportPdfBtn.type = 'button';
        exportPdfBtn.addEventListener('click', function () { exportScheme('pdf'); });
        tools.appendChild(exportDxfBtn);
        tools.appendChild(exportPdfBtn);
        var scaleBtn = el('button', 'asset-tree-scheme-tool', api.t('mapScanScale'));
        scaleBtn.type = 'button';
        scaleBtn.disabled = !api.canEdit;
        scaleBtn.addEventListener('click', function () {
            if (session.mode !== 'edit' || !activeFloor().underlay) return;
            session.calibrate = { points: [] };
        });
        var removeScan = el('button', 'asset-tree-scheme-tool', api.t('mapScanRemove'));
        removeScan.type = 'button';
        removeScan.disabled = !api.canEdit;
        removeScan.addEventListener('click', function () {
            if (session.mode !== 'edit') return;
            activeFloor().underlay = null;
            session.calibrate = null;
            session.dirty = true;
            paint();
        });
        tools.appendChild(scaleBtn);
        tools.appendChild(removeScan);
        var imageFile = el('input');
        imageFile.type = 'file';
        imageFile.accept = 'image/png,image/jpeg,image/webp,image/gif,.png,.jpg,.jpeg,.webp,.gif';
        imageFile.style.display = 'none';
        var imageBtn = el('button', 'asset-tree-scheme-tool', api.t('schemeImage'));
        imageBtn.type = 'button';
        imageBtn.title = api.t('schemeImageHint');
        imageBtn.disabled = !api.canEdit;
        imageBtn.addEventListener('click', function () {
            if (session.mode !== 'edit' || session.guest) return;
            imageFile.click();
        });
        tools.appendChild(imageBtn);
        tools.appendChild(imageFile);
        imageFile.addEventListener('change', function () {
            var picked = imageFile.files && imageFile.files[0];
            imageFile.value = '';
            if (!picked || session.mode !== 'edit' || session.guest) return;
            readSchemeImage(picked);
        });
        scanFile.addEventListener('change', function () {
            var picked = scanFile.files && scanFile.files[0];
            scanFile.value = '';
            if (!picked || session.mode !== 'edit' || session.guest) return;
            var reader = new FileReader();
            reader.onload = function () {
                var bytes = new Uint8Array(reader.result);
                var lower = String(picked.name || '').toLowerCase();
                if (/\.(png|jpe?g)$/.test(lower) || (picked.type && picked.type.indexOf('image/') === 0)) {
                    shrinkSchemeScan(bytes, picked.type || 'image/jpeg', function (url) { placeSchemeScan(url); });
                    return;
                }
                importSchemeDrawing(picked.name, bytes);
            };
            reader.readAsArrayBuffer(picked);
        });
        var unit = el('select', 'asset-tree-scheme-unit');
        ['cm', 'mm', 'm', 'in'].forEach(function (value) {
            var option = el('option', null, value);
            option.value = value;
            unit.appendChild(option);
        });
        unit.value = session.doc.settings.unit || 'cm';
        var measureWrap = el('label', 'asset-tree-scheme-check asset-tree-scheme-measures');
        var measureBox = el('input');
        measureBox.type = 'checkbox';
        measureBox.checked = !session.doc.settings || session.doc.settings.showMeasures !== false;
        measureWrap.appendChild(measureBox);
        measureWrap.appendChild(document.createTextNode(api.t('schemeMeasures')));
        var saveBtn = el('button', 'asset-tree-scheme-save', api.t('save'));
        saveBtn.type = 'button';
        saveBtn.disabled = !api.canEdit;
        var editBtn = el('button', 'asset-tree-scheme-save', api.t('schemeEdit'));
        editBtn.type = 'button';
        var backBtn = el('button', 'asset-tree-scheme-new', api.t('schemeBack'));
        backBtn.type = 'button';
        var templateNote = el('div', 'asset-tree-scheme-note');
        templateNote.style.display = 'none';
        var newBtn = el('button', 'asset-tree-scheme-new', api.t('schemeNew'));
        newBtn.type = 'button';
        newBtn.disabled = !api.canEdit;
        var deleteBtn = el('button', 'asset-tree-scheme-danger', api.t('schemeDelete'));
        deleteBtn.type = 'button';
        deleteBtn.disabled = !api.canEdit;
        var shareBtn = el('button', 'asset-tree-scheme-new', api.t('schemeShare'));
        shareBtn.type = 'button';
        shareBtn.disabled = !api.canEdit;
        var guestNote = el('div', 'asset-tree-scheme-note');
        guestNote.style.display = 'none';
        var status = el('div', 'asset-tree-scheme-status', '');
        head.appendChild(templateNote);
        head.appendChild(guestNote);
        head.appendChild(labeled(api.t('schemeList'), schemePick));
        head.appendChild(labeled(api.t('name'), nameInput));
        head.appendChild(labeled(api.t('schemePlace'), placePick));
        head.appendChild(unit);
        head.appendChild(measureWrap);
        head.appendChild(newBtn);
        head.appendChild(editBtn);
        head.appendChild(backBtn);
        head.appendChild(saveBtn);
        head.appendChild(shareBtn);
        var windowsBtn = el('button', 'asset-tree-scheme-new', api.t('schemeWindows'));
        windowsBtn.type = 'button';
        head.appendChild(deleteBtn);
        head.appendChild(windowsBtn);
        bar.appendChild(head);
        var body = el('div', 'asset-tree-scheme-body');
        var side = el('div', 'asset-tree-scheme-side');
        var stage = el('div', 'asset-tree-scheme-stage');
        var props = el('div', 'asset-tree-scheme-props');
        var windows = el('div', 'asset-tree-scheme-windows');
        var dockTop = el('div', 'asset-tree-scheme-dock is-top');
        var dockLeft = el('div', 'asset-tree-scheme-dock is-left');
        var dockRight = el('div', 'asset-tree-scheme-dock is-right');
        var dockBottom = el('div', 'asset-tree-scheme-dock is-bottom');
        var canvasWrap = el('div', 'asset-tree-scheme-canvas-wrap');
        var gridC = el('canvas', 'asset-tree-scheme-canvas');
        var sceneC = el('canvas', 'asset-tree-scheme-canvas');
        var overC = el('canvas', 'asset-tree-scheme-canvas');
        stage.appendChild(gridC);
        stage.appendChild(sceneC);
        stage.appendChild(overC);
        canvasWrap.appendChild(stage);
        canvasWrap.appendChild(windows);
        body.appendChild(dockTop);
        body.appendChild(dockLeft);
        body.appendChild(canvasWrap);
        body.appendChild(dockRight);
        body.appendChild(dockBottom);
        var gallery = el('div', 'asset-tree-scheme-gallery');
        host.appendChild(bar);
        host.appendChild(body);
        host.appendChild(gallery);
        host.appendChild(status);
        host.tabIndex = 0;
        if (session.mode === 'browse') {
            bar.style.display = 'none';
            body.style.display = 'none';
            status.style.display = 'none';
        }

        var panelBodies = {};
        var panelNodes = {};
        var panelState = {};
        try {
            panelState = JSON.parse(global.localStorage.getItem('assetshub.schemeWindows.v2') || '{}') || {};
        } catch (error) {
            panelState = {};
        }
        var bin = side;
        function openPanel(id) {
            bin = el('div', 'asset-tree-scheme-section');
            bin.setAttribute('data-panel', id);
            side.appendChild(bin);
        }
        function parkPanels() {
            var boxes = side.querySelectorAll('[data-panel]');
            var i;
            for (i = 0; i < boxes.length; i++) {
                var id = boxes[i].getAttribute('data-panel');
                if (!panelBodies[id]) continue;
                panelBodies[id].innerHTML = '';
                panelBodies[id].appendChild(boxes[i]);
            }
        }
        function savePanelState() {
            var next = {};
            var id;
            for (id in panelNodes) {
                var node = panelNodes[id];
                next[id] = {
                    x: parseInt(node.style.left, 10) || 8,
                    y: parseInt(node.style.top, 10) || 8,
                    open: !node.classList.contains('is-closed'),
                    collapsed: node.classList.contains('is-collapsed'),
                    pinned: node.classList.contains('is-pinned'),
                    side: node.getAttribute('data-side') || ''
                };
            }
            try { global.localStorage.setItem('assetshub.schemeWindows.v2', JSON.stringify(next)); } catch (error) { /* keep drawing */ }
        }
        function defaultPanelBox(id) {
            var width = stage.clientWidth || 1100;
            var height = stage.clientHeight || 700;
            var right = Math.max(8, width - 268);
            var map = {
                tools: { x: 12, y: 12, w: 230 },
                symbols: { x: 12, y: 280, w: 250 },
                assets: { x: 12, y: 520, w: 250 },
                props: { x: right, y: 12, w: 250 },
                floors: { x: right, y: 250, w: 250 },
                layers: { x: right, y: 400, w: 250 },
                rooms: { x: right, y: 550, w: 250 }
            };
            var box = map[id] || { x: 12, y: 12, w: 240 };
            if (box.y > height - 80) box.y = Math.max(12, height - 200);
            return box;
        }
        var dockOrder = { tools: 0, symbols: 1, assets: 2, props: 3, floors: 4, layers: 5, rooms: 6 };
        function dockBySide(side) {
            if (side === 'left') return dockLeft;
            if (side === 'top') return dockTop;
            if (side === 'bottom') return dockBottom;
            return dockRight;
        }
        function nearestSide(node) {
            var area = windows.getBoundingClientRect();
            var rect = node.getBoundingClientRect();
            var width = area.width || windows.clientWidth || 1;
            var height = area.height || windows.clientHeight || 1;
            var dist = {
                left: rect.left - area.left,
                right: area.right - rect.right,
                top: rect.top - area.top,
                bottom: area.bottom - rect.bottom
            };
            var side = 'left';
            var best = dist.left;
            ['right', 'top', 'bottom'].forEach(function (name) {
                if (dist[name] < best) {
                    best = dist[name];
                    side = name;
                }
            });
            var reach = (side === 'left' || side === 'right') ? Math.min(160, width * 0.28) : Math.min(120, height * 0.28);
            if (best > reach) return 'free';
            return side;
        }
        function syncDocks() {
            var editing = session.mode === 'edit' || !!session.templateId;
            [dockTop, dockLeft, dockRight, dockBottom].forEach(function (dock) {
                if (!editing) {
                    dock.style.display = 'none';
                    return;
                }
                var visible = false;
                var i;
                for (i = 0; i < dock.children.length; i++) {
                    if (!dock.children[i].classList.contains('is-closed')) visible = true;
                }
                dock.style.display = visible ? '' : 'none';
            });
        }
        function placePinned(node) {
            var side = node.getAttribute('data-side');
            if (side === 'free') {
                if (node.parentNode !== windows) windows.appendChild(node);
                node.classList.add('is-pinned');
                return;
            }
            if (side !== 'left' && side !== 'right' && side !== 'top' && side !== 'bottom') side = 'right';
            node.setAttribute('data-side', side);
            var dock = dockBySide(side);
            var rank = dockOrder[node.getAttribute('data-window')] || 0;
            var before = null;
            var i;
            for (i = 0; i < dock.children.length; i++) {
                var other = dock.children[i];
                if (other === node) continue;
                if ((dockOrder[other.getAttribute('data-window')] || 0) > rank) {
                    before = other;
                    break;
                }
            }
            if (before) dock.insertBefore(node, before);
            else dock.appendChild(node);
            node.classList.add('is-pinned');
        }
        function placeFloating(node) {
            if (node.parentNode !== windows) windows.appendChild(node);
            node.classList.remove('is-pinned');
        }
        function makeWindow(id, title) {
            var saved = panelState[id] || {};
            var box = defaultPanelBox(id);
            var node = el('div', 'asset-tree-scheme-window');
            node.setAttribute('data-window', id);
            node.style.left = (saved.x != null ? saved.x : box.x) + 'px';
            node.style.top = (saved.y != null ? saved.y : box.y) + 'px';
            node.style.width = box.w + 'px';
            if (saved.collapsed) node.classList.add('is-collapsed');
            if (saved.open === false) node.classList.add('is-closed');
            var titleBar = el('div', 'asset-tree-scheme-window-bar');
            titleBar.appendChild(el('span', 'asset-tree-scheme-window-title', title));
            var pin = el('button', 'asset-tree-scheme-window-btn');
            pin.type = 'button';
            pin.title = api.t(saved.pinned ? 'schemeUnpin' : 'schemePin');
            pin.innerHTML = '<svg viewBox="0 0 16 16" aria-hidden="true"><path d="M6.2 1.5h3.6v4.1l2.4 2.2v1.6H8.7V14h-1.4V9.4H3.8V7.8l2.4-2.2z" fill="currentColor"/></svg>';
            if (saved.pinned) pin.classList.add('is-on');
            var fold = el('button', 'asset-tree-scheme-window-btn', '\u2013');
            fold.type = 'button';
            var close = el('button', 'asset-tree-scheme-window-btn', '\u00d7');
            close.type = 'button';
            function isChrome(target) {
                var hit = target;
                while (hit && hit !== titleBar) {
                    if (hit === pin || hit === fold || hit === close) return true;
                    hit = hit.parentNode;
                }
                return false;
            }
            pin.addEventListener('click', function (event) {
                event.stopPropagation();
                var next = !node.classList.contains('is-pinned');
                if (next) {
                    node.setAttribute('data-side', nearestSide(node));
                    placePinned(node);
                } else placeFloating(node);
                pin.classList.toggle('is-on', next);
                pin.title = api.t(next ? 'schemeUnpin' : 'schemePin');
                syncDocks();
                savePanelState();
                paint();
            });
            fold.addEventListener('click', function (event) {
                event.stopPropagation();
                node.classList.toggle('is-collapsed');
                savePanelState();
            });
            close.addEventListener('click', function (event) {
                event.stopPropagation();
                node.classList.add('is-closed');
                syncDocks();
                savePanelState();
                paint();
            });
            titleBar.appendChild(pin);
            titleBar.appendChild(fold);
            titleBar.appendChild(close);
            var drag = null;
            titleBar.addEventListener('mousedown', function (event) {
                if (event.button !== 0 || isChrome(event.target) || node.classList.contains('is-pinned')) return;
                drag = {
                    x: event.clientX,
                    y: event.clientY,
                    left: parseInt(node.style.left, 10) || 0,
                    top: parseInt(node.style.top, 10) || 0
                };
                event.preventDefault();
                event.stopPropagation();
            });
            global.addEventListener('mousemove', function (event) {
                if (!drag) return;
                var maxX = Math.max(8, (windows.clientWidth || 400) - 80);
                var maxY = Math.max(8, (windows.clientHeight || 400) - 32);
                node.style.left = Math.min(maxX, Math.max(0, drag.left + event.clientX - drag.x)) + 'px';
                node.style.top = Math.min(maxY, Math.max(0, drag.top + event.clientY - drag.y)) + 'px';
            });
            global.addEventListener('mouseup', function () {
                if (!drag) return;
                drag = null;
                savePanelState();
            });
            var inner = el('div', 'asset-tree-scheme-window-body');
            node.appendChild(titleBar);
            node.appendChild(inner);
            windows.appendChild(node);
            var homes = { tools: 'left', symbols: 'left', assets: 'left', props: 'right', floors: 'right', layers: 'right', rooms: 'right' };
            if (saved.pinned !== false) {
                var kept = saved.side;
                if (kept !== 'left' && kept !== 'right' && kept !== 'top' && kept !== 'bottom' && kept !== 'free') kept = homes[id] || 'left';
                node.setAttribute('data-side', kept);
                placePinned(node);
                pin.classList.add('is-on');
                pin.title = api.t('schemeUnpin');
            }
            panelBodies[id] = inner;
            panelNodes[id] = node;
        }
        makeWindow('tools', api.t('schemeTools'));
        makeWindow('symbols', api.t('schemeSymbols'));
        makeWindow('assets', api.t('schemeAssets'));
        makeWindow('props', api.t('schemeProps'));
        makeWindow('floors', api.t('schemeFloors'));
        makeWindow('layers', api.t('schemeLayers'));
        makeWindow('rooms', api.t('schemeRooms'));
        panelBodies.tools.appendChild(tools);
        props = panelBodies.props;
        var windowMenu = el('div', 'asset-tree-scheme-menu asset-tree-scheme-window-menu');
        windowMenu.style.display = 'none';
        function paintWindowMenu() {
            windowMenu.innerHTML = '';
            ['tools', 'props', 'floors', 'layers', 'rooms', 'symbols', 'assets'].forEach(function (id) {
                var node = panelNodes[id];
                var row = el('label', 'asset-tree-scheme-check');
                var box = el('input');
                box.type = 'checkbox';
                box.checked = node && !node.classList.contains('is-closed');
                box.addEventListener('change', function () {
                    if (box.checked) node.classList.remove('is-closed');
                    else node.classList.add('is-closed');
                    syncDocks();
                    savePanelState();
                    paint();
                });
                row.appendChild(box);
                row.appendChild(document.createTextNode(' ' + node.querySelector('.asset-tree-scheme-window-title').textContent));
                windowMenu.appendChild(row);
            });
        }
        windowsBtn.addEventListener('click', function (event) {
            event.stopPropagation();
            if (windowMenu.style.display === 'none') {
                paintWindowMenu();
                var rect = windowsBtn.getBoundingClientRect();
                var hostRect = host.getBoundingClientRect();
                windowMenu.style.left = Math.max(8, rect.left - hostRect.left) + 'px';
                windowMenu.style.top = Math.max(8, rect.bottom - hostRect.top + 4) + 'px';
                windowMenu.style.display = '';
            } else windowMenu.style.display = 'none';
        });
        host.addEventListener('mousedown', function (event) {
            if (windowMenu.style.display === 'none') return;
            if (windowMenu.contains(event.target) || event.target === windowsBtn) return;
            windowMenu.style.display = 'none';
        });
        host.appendChild(windowMenu);

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
                window: '<rect x="5" y="6" width="14" height="12"/><path d="M12 6v12M5 12h14"/>',
                marker: '<path d="M4 18l8-8 3 3-8 8H4z"/><path d="M12 10l3-3 2 2-3 3"/><path d="M15 5l3-2 3 3-2 3"/>',
                dropper: '<path d="M14 4l4 4-1.5 1.5-4-4z"/><path d="M11.2 8.3l-6.4 6.4a1.6 1.6 0 0 0 2.3 2.3l6.4-6.4"/>',
                eye: '<path d="M2 12s3.5-6 10-6 10 6 10 6-3.5 6-10 6S2 12 2 12z"/><circle cx="12" cy="12" r="2.5"/>',
                eyeOff: '<path d="M4 5l16 14M9.5 9.7A4 4 0 0 0 14.3 14.5M6.1 6.8C3.7 8.4 2 12 2 12s3.5 6 10 6c1.7 0 3.2-.4 4.5-1.1M10.1 6.2C10.7 6.1 11.3 6 12 6c6.5 0 10 6 10 6a18 18 0 0 1-2.4 3"/>',
                lock: '<rect x="6" y="11" width="12" height="8" rx="1"/><path d="M8 11V8a4 4 0 0 1 8 0v3"/>',
                unlock: '<rect x="6" y="11" width="12" height="8" rx="1"/><path d="M16 11V8a4 4 0 0 0-7.5-2"/>',
                pencil: '<path d="M4 20h4L19 9l-4-4L4 16z"/><path d="M13 7l4 4"/>'
            };
            return '<svg viewBox="0 0 24 24" aria-hidden="true">' + (body[name] || '') + '</svg>';
        }

        function askBox(options) {
            var back = el('div', 'asset-tree-scheme-modal');
            var dialog = el('div', 'asset-tree-scheme-dialog asset-tree-scheme-ask');
            dialog.appendChild(el('div', 'asset-tree-scheme-label', options.title || ''));
            if (options.text) dialog.appendChild(el('p', 'asset-tree-scheme-ask-text', options.text));
            var input = null;
            if (options.input) {
                input = el('input', 'asset-tree-scheme-name');
                input.value = options.value || '';
            }
            var actions = el('div', 'asset-tree-scheme-head');
            var ok = el('button', options.danger ? 'asset-tree-scheme-danger' : 'asset-tree-scheme-save', options.okLabel || api.t('save'));
            var cancel = el('button', 'asset-tree-scheme-new', api.t('cancel'));
            ok.type = 'button';
            cancel.type = 'button';
            function close() {
                if (back.parentNode) back.parentNode.removeChild(back);
            }
            function commit() {
                var next = input ? String(input.value || '').replace(/^\s+|\s+$/g, '') : true;
                if (options.input && !next) return;
                close();
                if (options.onOk) options.onOk(next);
            }
            ok.addEventListener('click', commit);
            cancel.addEventListener('click', close);
            dialog.addEventListener('keydown', function (event) {
                event.stopPropagation();
                if (event.key === 'Enter') {
                    event.preventDefault();
                    commit();
                }
                if (event.key === 'Escape') {
                    event.preventDefault();
                    close();
                }
            });
            if (input) dialog.appendChild(input);
            actions.appendChild(ok);
            actions.appendChild(cancel);
            dialog.appendChild(actions);
            back.addEventListener('mousedown', function (event) {
                if (event.target === back) close();
            });
            back.appendChild(dialog);
            host.appendChild(back);
            dialog.tabIndex = 0;
            if (input) {
                input.focus();
                input.select();
            } else dialog.focus();
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
            var libs = allLibraries();
            var n;
            var k;
            for (n = 0; n < libs.length; n++) {
                var templates = libs[n].templates || [];
                for (k = 0; k < templates.length; k++) {
                    if (templates[k].id === id) return templates[k];
                }
            }
            var list = builtins(api.t);
            var i;
            for (i = 0; i < list.length; i++) {
                if (list[i].id === id) return list[i];
            }
            if (id) return { id: id, name: '', w: 40, h: 40, parts: [] };
            return list[0];
        }

        function markTools() {
            if (session.guest && session.tool && session.tool !== 'select') session.tool = 'select';
            if (stage) stage.style.cursor = session.tool === 'dropper' ? 'crosshair' : '';
            var buttons = tools.querySelectorAll('button');
            var i;
            for (i = 0; i < buttons.length; i++) {
                var tool = buttons[i].getAttribute('data-tool');
                if (tool === session.tool) buttons[i].className = 'asset-tree-scheme-tool is-on';
                else buttons[i].className = 'asset-tree-scheme-tool';
                if (!tool) buttons[i].disabled = !api.canEdit || !!session.guest;
                else buttons[i].disabled = tool !== 'select' && (!api.canEdit || !!session.guest);
            }
        }

        function ownPin(obj) {
            var binding = obj && obj.assetBinding;
            return !!(obj && obj.type === 'asset' && binding && String(binding.workspaceId || '') === String(api.projectKey));
        }

        function foreignPin(obj) {
            var binding = obj && obj.assetBinding;
            return !!(obj && obj.type === 'asset' && binding && binding.workspaceId && String(binding.workspaceId) !== String(api.projectKey));
        }

        function maySaveScheme() {
            return !!api.canEdit || (!!session.guest && !!api.canPlace);
        }

        function mayPlacePins() {
            if (session.mode !== 'edit') return false;
            if (session.guest) return !!api.canPlace;
            return !!api.canEdit;
        }

        function mutableObject(obj) {
            if (session.mode !== 'edit' || !obj) return false;
            if (foreignPin(obj)) return false;
            if (session.guest) return !!api.canPlace && ownPin(obj);
            return !!api.canEdit;
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
            found.sort(function (a, b) {
                var da = dist(a.x, a.y, raw.x, raw.y);
                var db = dist(b.x, b.y, raw.x, raw.y);
                if (Math.abs(da - db) > 0.5) return da - db;
                return (rank[a.kind] || 9) - (rank[b.kind] || 9);
            });
            var closest = found[0];
            var snapKey = closest.kind + ':' + closest.x + ':' + closest.y;
            if (session.osnapKey !== snapKey) {
                session.osnapKey = snapKey;
                session.osnapIndex = 0;
            }
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

        function objectById(id) {
            var floor = activeFloor();
            var list = (floor.objects || []).concat(floor.rooms || []);
            var i;
            for (i = 0; i < list.length; i++) {
                if (list[i].id === id || ('room:' + list[i].id) === id) return list[i];
            }
            return null;
        }

        function sampleInk(sx, sy, point) {
            var obj = objectById(hit(point.x, point.y));
            if (obj && obj.fill && /^#[0-9a-fA-F]{6}$/.test(obj.fill)) return obj.fill;
            if (obj && obj.color && /^#[0-9a-fA-F]{6}$/.test(obj.color)) return obj.color;
            try {
                var ratio = global.devicePixelRatio || 1;
                var pixel = sceneC.getContext('2d').getImageData(Math.max(0, Math.floor(sx * ratio)), Math.max(0, Math.floor(sy * ratio)), 1, 1).data;
                if (pixel[3] < 24) return '';
                function hex(n) {
                    var text = n.toString(16);
                    return text.length < 2 ? '0' + text : text;
                }
                return '#' + hex(pixel[0]) + hex(pixel[1]) + hex(pixel[2]);
            } catch (error) {
                return '';
            }
        }

        function hitsObject(obj, wx, wy, tol) {
            var local = unrotate(obj, wx, wy);
            wx = local.x;
            wy = local.y;
            if (obj.type === 'line' || obj.type === 'dimension') {
                return projectSeg(obj.x1, obj.y1, obj.x2, obj.y2, wx, wy).dist <= tol;
            }
            if (obj.type === 'rect' || obj.type === 'ellipse' || obj.type === 'symbol' || obj.type === 'asset' || obj.type === 'text' || obj.type === 'image') {
                var w = obj.w || 40;
                var h = obj.h || 20;
                return wx >= obj.x - tol && wy >= obj.y - tol && wx <= obj.x + w + tol && wy <= obj.y + h + tol;
            }
            if (obj.type === 'marker' && obj.points) {
                var reach = Math.max(tol, (obj.thickness || 18) / 2);
                var m;
                for (m = 1; m < obj.points.length; m++) {
                    if (projectSeg(obj.points[m - 1].x, obj.points[m - 1].y, obj.points[m].x, obj.points[m].y, wx, wy).dist <= reach) return true;
                }
                return false;
            }
            if ((obj.type === 'polyline' || obj.type === 'wall') && obj.points) {
                var i;
                for (i = 1; i < obj.points.length; i++) {
                    if (projectSeg(obj.points[i - 1].x, obj.points[i - 1].y, obj.points[i].x, obj.points[i].y, wx, wy).dist <= Math.max(tol, (obj.thickness || 15) / 2)) return true;
                }
                if (obj.closed && obj.points.length > 2) {
                    var last = obj.points[obj.points.length - 1];
                    if (projectSeg(last.x, last.y, obj.points[0].x, obj.points[0].y, wx, wy).dist <= Math.max(tol, (obj.thickness || 15) / 2)) return true;
                    if ((obj.fill || obj.text) && pointInPoly(wx, wy, obj.points)) return true;
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
                if (obj.type === 'symbol' || obj.type === 'asset') {
                    var knob = knobOf(obj);
                    var knobScreen = screenFrom(knob.x, knob.y);
                    if (dist(knobScreen.x, knobScreen.y, sx, sy) <= 10) return { id: obj.id, index: -1 };
                }
            }
            var k;
            for (k = 0; k < points.length; k++) {
                var worldPoint = rotatePoint(obj, points[k].x, points[k].y);
                var screen = screenFrom(worldPoint.x, worldPoint.y);
                if (dist(screen.x, screen.y, sx, sy) <= 8) return { id: obj.id, index: points[k].index };
            }
            return null;
        }

        function paintSelection(color) {
            var objects = selectedObjects().filter(function (obj) { return mutableObject(obj); });
            if (!objects.length || !/^#[0-9a-fA-F]{6}$/.test(color || '')) return;
            remember();
            objects.forEach(function (obj) {
                if (!obj.type && obj.points) {
                    obj.color = color;
                    return;
                }
                obj.color = color;
                if (obj.type === 'wall' || obj.type === 'symbol' || obj.type === 'asset') obj.fill = color;
                else if (session.inkFill && (obj.type === 'rect' || obj.type === 'ellipse' || obj.type === 'polyline')) {
                    obj.fill = color;
                    if (obj.type === 'polyline') obj.closed = true;
                }
            });
            renderProps();
            paint();
        }

        function baseObject(type) {
            return {
                id: nid(type),
                type: type,
                layerId: session.doc.activeLayerId || 'walls',
                color: session.ink || (layerById(session.doc.activeLayerId) || {}).color || '#172b4d',
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
            obj.points = session.draft.points.map(function (p) {
                var next = { x: roundCm(p.x), y: roundCm(p.y) };
                if (p.outx != null) { next.outx = roundCm(p.outx); next.outy = roundCm(p.outy); }
                if (p.inx != null) { next.inx = roundCm(p.inx); next.iny = roundCm(p.iny); }
                return next;
            });
            while (obj.points.length >= 2 && dist(obj.points[obj.points.length - 1].x, obj.points[obj.points.length - 1].y, obj.points[obj.points.length - 2].x, obj.points[obj.points.length - 2].y) < 0.5) {
                obj.points.pop();
            }
            if (closed && obj.points.length >= 4 && dist(obj.points[0].x, obj.points[0].y, obj.points[obj.points.length - 1].x, obj.points[obj.points.length - 1].y) <= Math.max(40, 12 / (session.cam.z || 1))) {
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
                if (session.inkFill && session.ink) obj.fill = session.ink;
            }
            if (session.inkFill && session.ink && obj.type === 'polyline') {
                obj.fill = session.ink;
                obj.closed = true;
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

        function commitWall(x1, y1, x2, y2) {
            session.draft = null;
            session.wallDoneAt = Date.now();
            if (dist(x1, y1, x2, y2) < 2) {
                paint();
                return;
            }
            remember();
            var obj = baseObject('wall');
            obj.points = [
                { x: roundCm(x1), y: roundCm(y1) },
                { x: roundCm(x2), y: roundCm(y2) }
            ];
            obj.closed = false;
            obj.thickness = 15;
            obj.alignment = 'center';
            obj.hatch = 'diagonal';
            obj.height = 300;
            if (session.inkFill && session.ink) obj.fill = session.ink;
            activeFloor().objects.push(obj);
            syncRooms(activeFloor());
            session.selection = [obj.id];
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
                if (session.inkFill && session.ink && (kind === 'rect' || kind === 'ellipse')) obj.fill = session.ink;
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
            if (session.assetId && placedAssetObject(session.assetId)) {
                remember();
                movePlacedAsset(session.assetId, pt);
                session.assetId = 0;
                markTools();
                renderProps();
                refreshSide();
                paint();
                return;
            }
            remember();
            var obj = baseObject('symbol');
            obj.symbolId = def.id;
            obj.x = roundCm(pt.x - def.w / 2);
            obj.y = roundCm(pt.y - def.h / 2);
            obj.w = def.w;
            obj.h = def.h;
            obj.angle = 0;
            obj.assetBinding = null;
            if (session.assetId) {
                var boundAsset = assetById(session.assetId);
                if (boundAsset) obj.assetBinding = bindingOf(boundAsset);
                session.assetId = 0;
            }
            obj.layerId = layerOr('equipment');
            activeFloor().objects.push(obj);
            session.selection = [obj.id];
            markTools();
            renderProps();
            refreshSide();
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
            var remembered = session.openingSize && session.openingSize[kind];
            opening.width = remembered ? remembered.width : (kind === 'window' ? 120 : 90);
            opening.height = remembered ? remembered.height : (kind === 'window' ? 140 : 210);
            opening.custom = remembered ? !!remembered.custom : false;
            opening.swing = 'left';
            opening.layerId = best.wall.layerId;
            floor.objects.push(opening);
            session.selection = [opening.id];
            session.tool = 'select';
            markTools();
            renderProps();
            paint();
        }

        var scanCache = { src: '', image: null };

        function placeSchemeScan(src) {
            if (!src) return;
            var image = new Image();
            image.onload = function () {
                var floor = activeFloor();
                var w = Math.min(1000, image.naturalWidth || 400);
                var h = Math.round(w * (image.naturalHeight || 1) / Math.max(1, image.naturalWidth || 1));
                floor.underlay = { src: src, x: 40, y: 40, w: Math.max(10, w), h: Math.max(10, h) };
                session.dirty = true;
                paint();
            };
            image.src = src;
        }

        function shrinkSchemeScan(bytes, mime, done) {
            var blob = new Blob([bytes], { type: mime || 'image/jpeg' });
            var url = URL.createObjectURL(blob);
            var image = new Image();
            image.onload = function () {
                var canvas = el('canvas');
                var longest = Math.max(image.naturalWidth || 1, image.naturalHeight || 1);
                var limit = 1400;
                var quality = 0.72;
                var data = '';
                var guard = 0;
                var scale = 1;
                while (guard < 4) {
                    scale = Math.min(1, limit / longest);
                    canvas.width = Math.max(1, Math.round((image.naturalWidth || 1) * scale));
                    canvas.height = Math.max(1, Math.round((image.naturalHeight || 1) * scale));
                    canvas.getContext('2d').drawImage(image, 0, 0, canvas.width, canvas.height);
                    data = canvas.toDataURL('image/jpeg', quality);
                    if (data.length <= 450000) break;
                    limit = Math.round(limit * 0.7);
                    quality = 0.55;
                    guard++;
                }
                URL.revokeObjectURL(url);
                if (data && data.length <= 450000) done(data);
                else status.textContent = api.t('schemeImageBig');
            };
            image.onerror = function () {
                URL.revokeObjectURL(url);
                status.textContent = api.t('schemeImageBig');
            };
            image.src = url;
        }

        function readSchemeImage(file) {
            if (!file) return;
            var reader = new FileReader();
            reader.onload = function () {
                shrinkSchemeScan(new Uint8Array(reader.result), file.type || 'image/jpeg', function (src) {
                    var image = new Image();
                    image.onload = function () {
                        var w = Math.min(800, image.naturalWidth || 400);
                        var h = Math.round(w * (image.naturalHeight || 1) / Math.max(1, image.naturalWidth || 1));
                        remember();
                        var obj = baseObject('image');
                        obj.src = src;
                        obj.w = Math.max(10, w);
                        obj.h = Math.max(10, h);
                        obj.x = roundCm((session.cam.x || 0) + 60);
                        obj.y = roundCm((session.cam.y || 0) + 60);
                        obj.opacity = 0.72;
                        activeFloor().objects.unshift(obj);
                        session.selection = [obj.id];
                        session.tool = 'select';
                        markTools();
                        renderProps();
                        refreshSide();
                        paint();
                    };
                    image.src = src;
                });
            };
            reader.readAsArrayBuffer(file);
        }

        function say(key) {
            if (api.say) api.say(api.t(key));
        }

        function importSchemeDrawing(name, raw) {
            if (!global.AssetTreePlan || !global.AssetTreePlan.importDrawing) return;
            var parsed = global.AssetTreePlan.importDrawing(name, raw);
            if (parsed && parsed.underlay) {
                placeSchemeScan(parsed.underlay);
                return;
            }
            if (!parsed || parsed.error || !parsed.rooms || !parsed.rooms.length) {
                say(parsed && parsed.error ? parsed.error : 'mapImportNone');
                return;
            }
            var added = 0;
            var batch = [];
            parsed.rooms.forEach(function (room) {
                if (room.kind && room.kind !== 'wall' && room.kind !== 'partition') return;
                var wall = baseObject('wall');
                if (room.x1 != null && room.y1 != null && room.x2 != null && room.y2 != null) {
                    wall.points = [
                        { x: roundCm(room.x1), y: roundCm(room.y1) },
                        { x: roundCm(room.x2), y: roundCm(room.y2) }
                    ];
                } else {
                    var len = room.w || 0;
                    if (len < 8) return;
                    var rad = (room.angle || 0) * Math.PI / 180;
                    var cx = (room.x || 0) + len / 2;
                    var cy = (room.y || 0) + (room.h || 0) / 2;
                    wall.points = [
                        { x: roundCm(cx - Math.cos(rad) * len / 2), y: roundCm(cy - Math.sin(rad) * len / 2) },
                        { x: roundCm(cx + Math.cos(rad) * len / 2), y: roundCm(cy + Math.sin(rad) * len / 2) }
                    ];
                }
                if (dist(wall.points[0].x, wall.points[0].y, wall.points[1].x, wall.points[1].y) < 8) return;
                wall.closed = false;
                wall.thickness = room.kind === 'partition' ? 6 : 8;
                wall.alignment = 'center';
                wall.hatch = 'none';
                wall.height = 300;
                wall.layerId = layerOr('walls');
                batch.push(wall);
                added += 1;
            });
            if (!added) {
                say('mapImportNone');
                return;
            }
            remember();
            var floor = activeFloor();
            batch.forEach(function (wall) { floor.objects.push(wall); });
            syncRooms(floor);
            refreshSide();
            renderProps();
            paint();
        }

        function exportScheme(kind) {
            if (!global.AssetTreePlan) return;
            var items = [];
            var maxY = 100;
            (activeFloor().objects || []).forEach(function (obj) {
                if (!obj.points || obj.points.length < 2) return;
                if (obj.type !== 'wall' && obj.type !== 'polyline' && obj.type !== 'line') return;
                var i;
                for (i = 1; i < obj.points.length; i++) {
                    items.push({
                        kind: obj.type === 'wall' ? 'wall' : 'line',
                        x1: obj.points[i - 1].x, y1: obj.points[i - 1].y,
                        x2: obj.points[i].x, y2: obj.points[i].y
                    });
                    maxY = Math.max(maxY, obj.points[i - 1].y, obj.points[i].y);
                }
            });
            var model = { planH: maxY + 40, items: items, rooms: [] };
            var data = kind === 'pdf' ? global.AssetTreePlan.exportPdf(model) : global.AssetTreePlan.exportDxf(model);
            if (!data) return;
            var bytes = new Uint8Array(data.length);
            var i;
            for (i = 0; i < data.length; i++) bytes[i] = data.charCodeAt(i) & 255;
            var blob = new Blob([bytes], { type: kind === 'pdf' ? 'application/pdf' : 'image/vnd.dxf' });
            var link = el('a');
            link.href = URL.createObjectURL(blob);
            link.download = (session.doc.name || 'scheme') + (kind === 'pdf' ? '.pdf' : '.dxf');
            link.click();
            setTimeout(function () { URL.revokeObjectURL(link.href); }, 1000);
        }

        function drawSchemeScan(ctx, floor) {
            var scan = floor && floor.underlay;
            if (!scan || !scan.src) return;
            if (scanCache.src !== scan.src) {
                scanCache.src = scan.src;
                scanCache.image = new Image();
                scanCache.image.onload = function () { paint(); };
                scanCache.image.src = scan.src;
            }
            if (!scanCache.image || !scanCache.image.complete || !scanCache.image.naturalWidth) return;
            ctx.save();
            ctx.globalAlpha = 0.55;
            ctx.drawImage(scanCache.image, scan.x || 0, scan.y || 0, scan.w || 10, scan.h || 10);
            ctx.restore();
        }

        function wallSpecs(floor) {
            var specs = [];
            (floor.objects || []).forEach(function (wall) {
                if (wall.type !== 'wall' || !wall.points || wall.points.length < 2) return;
                var i;
                for (i = 1; i < wall.points.length; i++) {
                    specs.push({
                        x1: wall.points[i - 1].x, y1: wall.points[i - 1].y,
                        x2: wall.points[i].x, y2: wall.points[i].y,
                        source: specs.length, kind: 'wall', thick: wall.thickness || 15
                    });
                }
                if (wall.closed && wall.points.length > 2) {
                    var last = wall.points.length - 1;
                    specs.push({
                        x1: wall.points[last].x, y1: wall.points[last].y,
                        x2: wall.points[0].x, y2: wall.points[0].y,
                        source: specs.length, kind: 'wall', thick: wall.thickness || 15
                    });
                }
            });
            return specs;
        }

        function joinTees(floor) {
            if (!global.AssetTreePlan || !global.AssetTreePlan.cutTees) return;
            var specs = [];
            var owners = [];
            (floor.objects || []).forEach(function (wall) {
                if (wall.type !== 'wall' || !wall.points || wall.points.length < 2) return;
                var i;
                for (i = 1; i < wall.points.length; i++) {
                    owners.push({ wall: wall, at: i - 1, close: false });
                    specs.push({
                        x1: wall.points[i - 1].x, y1: wall.points[i - 1].y,
                        x2: wall.points[i].x, y2: wall.points[i].y,
                        source: specs.length, kind: 'wall', thick: wall.thickness || 15
                    });
                }
                if (wall.closed && wall.points.length > 2) {
                    var last = wall.points.length - 1;
                    owners.push({ wall: wall, at: last, close: true });
                    specs.push({
                        x1: wall.points[last].x, y1: wall.points[last].y,
                        x2: wall.points[0].x, y2: wall.points[0].y,
                        source: specs.length, kind: 'wall', thick: wall.thickness || 15
                    });
                }
            });
            var cut = global.AssetTreePlan.cutTees(specs);
            if (!cut) return;
            var grouped = {};
            cut.forEach(function (piece) {
                if (piece.source < 0) return;
                (grouped[piece.source] || (grouped[piece.source] = [])).push(piece);
            });
            var edits = {};
            Object.keys(grouped).forEach(function (key) {
                var owner = owners[Number(key)];
                if (!owner) return;
                (edits[owner.wall.id] || (edits[owner.wall.id] = [])).push({ owner: owner, pieces: grouped[key] });
            });
            Object.keys(edits).forEach(function (id) {
                edits[id].sort(function (a, b) { return b.owner.at - a.owner.at; });
                edits[id].forEach(function (edit) {
                    var wall = edit.owner.wall;
                    var pts = wall.points;
                    var pieces = edit.pieces;
                    function put(index, x, y) {
                        pts[index].x = roundCm(x);
                        pts[index].y = roundCm(y);
                    }
                    if (!edit.owner.close) {
                        put(edit.owner.at, pieces[0].x1, pieces[0].y1);
                        put(edit.owner.at + 1, pieces[pieces.length - 1].x2, pieces[pieces.length - 1].y2);
                        if (pieces.length > 1) {
                            var extra = [];
                            var k;
                            for (k = 0; k < pieces.length - 1; k++) extra.push({ x: roundCm(pieces[k].x2), y: roundCm(pieces[k].y2) });
                            Array.prototype.splice.apply(pts, [edit.owner.at + 1, 0].concat(extra));
                        }
                    } else {
                        put(pts.length - 1, pieces[0].x1, pieces[0].y1);
                        put(0, pieces[pieces.length - 1].x2, pieces[pieces.length - 1].y2);
                        if (pieces.length > 1) {
                            var more = [];
                            var n;
                            for (n = 0; n < pieces.length - 1; n++) more.push({ x: roundCm(pieces[n].x2), y: roundCm(pieces[n].y2) });
                            Array.prototype.splice.apply(pts, [pts.length, 0].concat(more));
                        }
                    }
                });
            });
            session.dirty = true;
        }

        function syncRooms(floor) {
            joinTees(floor);
            var faces = [];
            var objects = floor.objects || [];
            var i;
            if (global.AssetTreePlan && global.AssetTreePlan.planFaces) {
                global.AssetTreePlan.planFaces(wallSpecs(floor)).forEach(function (points) {
                    var area = polygonArea(points);
                    if (area >= 3000) faces.push({ points: points, area: area, center: centroid(points) });
                });
            } else {
                for (i = 0; i < objects.length; i++) {
                    var wall = objects[i];
                    if (wall.type === 'wall' && wall.closed && wall.points && wall.points.length >= 3) {
                        var area = polygonArea(wall.points);
                        if (area >= 3000) faces.push({ points: wall.points, area: area, center: centroid(wall.points) });
                    }
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
            if (session.draft.tool === 'wall') {
                commitWall(session.draft.x1, session.draft.y1, x2, y2);
                return;
            }
            commitTwo(session.draft.tool, session.draft.x1, session.draft.y1, x2, y2);
        }

        function translateObj(obj, dx, dy) {
            if (obj.points) {
                obj.points.forEach(function (p) {
                    p.x = roundCm(p.x + dx);
                    p.y = roundCm(p.y + dy);
                    if (p.outx != null) { p.outx = roundCm(p.outx + dx); p.outy = roundCm(p.outy + dy); }
                    if (p.inx != null) { p.inx = roundCm(p.inx + dx); p.iny = roundCm(p.iny + dy); }
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
            if (!objects.length || !api.canEdit || session.guest) return;
            objects = objects.filter(function (obj) { return !foreignPin(obj); });
            if (!objects.length) return;
            remember();
            var ids = [];
            var groupMap = {};
            objects.forEach(function (obj) {
                if (String(obj.id).indexOf('room') === 0 && obj.points && !obj.type) return;
                var copy = clone(obj);
                copy.id = nid(obj.type || 'obj');
                if (copy.group) {
                    if (!groupMap[copy.group]) groupMap[copy.group] = nid('grp');
                    copy.group = groupMap[copy.group];
                }
                translateObj(copy, 20, 20);
                if (copy.type) activeFloor().objects.push(copy);
                ids.push(copy.id);
            });
            session.selection = ids;
            renderProps();
            paint();
        }

        function removeSelection() {
            if (!session.selection.length || !mayPlacePins()) return;
            var floor = activeFloor();
            var drop = {};
            var any = false;
            (floor.objects || []).forEach(function (obj) {
                if (session.selection.indexOf(obj.id) >= 0 && mutableObject(obj)) {
                    drop[obj.id] = true;
                    any = true;
                }
            });
            if (!any) return;
            remember();
            floor.objects = (floor.objects || []).filter(function (obj) {
                return !drop[obj.id];
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
                    pathCurve(ctx, part.points, false);
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

        function pathCurve(ctx, points, closed) {
            if (!points || !points.length) return;
            ctx.beginPath();
            ctx.moveTo(points[0].x, points[0].y);
            var count = points.length;
            var last = closed ? count : count - 1;
            var i;
            for (i = 0; i < last; i++) {
                var a = points[i];
                var b = points[(i + 1) % count];
                if (a.outx == null && b.inx == null) ctx.lineTo(b.x, b.y);
                else ctx.bezierCurveTo(a.outx != null ? a.outx : a.x, a.outy != null ? a.outy : a.y, b.inx != null ? b.inx : b.x, b.iny != null ? b.iny : b.y, b.x, b.y);
            }
            if (closed) ctx.closePath();
        }

        function cornerDegrees(a, b, c) {
            var left = Math.atan2(a.y - b.y, a.x - b.x);
            var right = Math.atan2(c.y - b.y, c.x - b.x);
            var deg = Math.abs(left - right) * 180 / Math.PI;
            if (deg > 180) deg = 360 - deg;
            return Math.round(deg);
        }

        function materialsOf() {
            if (!session.doc.materials) session.doc.materials = [];
            return session.doc.materials;
        }

        function materialById(id) {
            var list = (session.doc && session.doc.materials) || [];
            var i;
            for (i = 0; i < list.length; i++) {
                if (list[i].id === id) return list[i];
            }
            return null;
        }

        function materialHatch(material) {
            if (material === 'brick') return 'brick';
            if (material === 'concrete') return 'concrete';
            if (material === 'wood' || material === 'metal') return 'diagonal';
            var custom = materialById(material);
            if (custom) return custom.hatch || 'diagonal';
            return 'none';
        }

        function materialName(id) {
            if (!id) return api.t('schemeMaterialNone');
            if (id === 'brick') return api.t('schemeMatBrick');
            if (id === 'concrete') return api.t('schemeMatConcrete');
            if (id === 'wood') return api.t('schemeMatWood');
            if (id === 'glass') return api.t('schemeMatGlass');
            if (id === 'metal') return api.t('schemeMatMetal');
            var custom = materialById(id);
            return custom ? custom.name : id;
        }

        function paintFill(ctx, obj) {
            if (obj.fill) {
                ctx.fillStyle = obj.fill;
                ctx.fill();
                return;
            }
            if (obj.material === 'glass') {
                ctx.save();
                ctx.globalAlpha *= 0.35;
                ctx.fillStyle = '#deebff';
                ctx.fill();
                ctx.restore();
            }
        }

        function hatchShape(ctx, poly, material, color) {
            var kind = materialHatch(material);
            if (kind === 'none' || !poly || poly.length < 3) return;
            hatch(ctx, poly, kind, color || '#97a0af');
        }

        function ellipsePoly(obj) {
            var pts = [];
            var cx = obj.x + obj.w / 2;
            var cy = obj.y + obj.h / 2;
            var rx = Math.abs(obj.w) / 2;
            var ry = Math.abs(obj.h) / 2;
            var i;
            for (i = 0; i < 24; i++) {
                var angle = Math.PI * 2 * i / 24;
                pts.push({ x: cx + Math.cos(angle) * rx, y: cy + Math.sin(angle) * ry });
            }
            return pts;
        }

        function drawShapeText(ctx, text, x, y, color) {
            if (!text) return;
            ctx.save();
            ctx.fillStyle = color || '#172b4d';
            ctx.font = (14 / session.cam.z) + 'px sans-serif';
            ctx.textAlign = 'center';
            ctx.textBaseline = 'middle';
            ctx.fillText(text, x, y);
            ctx.restore();
        }

        function drawPenHandles(ctx, points) {
            if (!points) return;
            ctx.save();
            ctx.strokeStyle = '#6554c0';
            ctx.fillStyle = '#6554c0';
            ctx.lineWidth = 1 / session.cam.z;
            var i;
            for (i = 0; i < points.length; i++) {
                var p = points[i];
                if (p.outx == null && p.inx == null) continue;
                ctx.beginPath();
                if (p.inx != null) {
                    ctx.moveTo(p.inx, p.iny);
                    ctx.lineTo(p.x, p.y);
                }
                if (p.outx != null) {
                    ctx.moveTo(p.x, p.y);
                    ctx.lineTo(p.outx, p.outy);
                }
                ctx.stroke();
                var radius = 3 / session.cam.z;
                if (p.inx != null) {
                    ctx.beginPath();
                    ctx.arc(p.inx, p.iny, radius, 0, Math.PI * 2);
                    ctx.fill();
                }
                if (p.outx != null) {
                    ctx.beginPath();
                    ctx.arc(p.outx, p.outy, radius, 0, Math.PI * 2);
                    ctx.fill();
                }
            }
            ctx.restore();
        }

        function measuresOn() {
            return !session.doc || !session.doc.settings || session.doc.settings.showMeasures !== false;
        }

        function rotateObj(ctx, obj) {
            if (!obj.angle || obj.x == null || obj.w == null) return;
            var cx = obj.x + obj.w / 2;
            var cy = obj.y + (obj.h || 0) / 2;
            ctx.translate(cx, cy);
            ctx.rotate(obj.angle * Math.PI / 180);
            ctx.translate(-cx, -cy);
        }

        function unrotate(obj, wx, wy) {
            if (!obj || !obj.angle || obj.x == null || obj.w == null) return { x: wx, y: wy };
            var cx = obj.x + obj.w / 2;
            var cy = obj.y + (obj.h || 0) / 2;
            var rad = -obj.angle * Math.PI / 180;
            var dx = wx - cx;
            var dy = wy - cy;
            return {
                x: cx + dx * Math.cos(rad) - dy * Math.sin(rad),
                y: cy + dx * Math.sin(rad) + dy * Math.cos(rad)
            };
        }

        function knobOf(obj) {
            var cx = obj.x + obj.w / 2;
            var cy = obj.y + (obj.h || 0) / 2;
            var rad = (obj.angle || 0) * Math.PI / 180;
            var ly = -((obj.h || 0) / 2 + 18 / session.cam.z);
            return { x: cx - ly * Math.sin(rad), y: cy + ly * Math.cos(rad) };
        }

        function rotatePoint(obj, x, y) {
            if (!obj || !obj.angle || obj.x == null || obj.w == null) return { x: x, y: y };
            var cx = obj.x + obj.w / 2;
            var cy = obj.y + (obj.h || 0) / 2;
            var rad = obj.angle * Math.PI / 180;
            var dx = x - cx;
            var dy = y - cy;
            return {
                x: cx + dx * Math.cos(rad) - dy * Math.sin(rad),
                y: cy + dx * Math.sin(rad) + dy * Math.cos(rad)
            };
        }

        function layerOr(id) {
            var layers = (session.doc && session.doc.layers) || [];
            var i;
            for (i = 0; i < layers.length; i++) {
                if (layers[i].id === id) return id;
            }
            if (session.doc && session.doc.activeLayerId) return session.doc.activeLayerId;
            return layers.length ? layers[0].id : id;
        }

        function drawMeasures(ctx, obj) {
            if (!measuresOn()) return;
            if (!obj || obj.type === 'text' || obj.type === 'symbol' || obj.type === 'asset' || obj.type === 'dimension' || obj.type === 'opening') return;
            ctx.save();
            ctx.fillStyle = '#5e6c84';
            ctx.font = (11 / session.cam.z) + 'px sans-serif';
            ctx.textAlign = 'center';
            ctx.textBaseline = 'middle';
            function label(x, y, text) { ctx.fillText(text, x, y); }
            if (obj.type === 'line') {
                var bearing = Math.atan2(obj.y2 - obj.y1, obj.x2 - obj.x1) * 180 / Math.PI;
                if (bearing < 0) bearing += 360;
                label((obj.x1 + obj.x2) / 2, (obj.y1 + obj.y2) / 2 - 8 / session.cam.z, formatLen(dist(obj.x1, obj.y1, obj.x2, obj.y2)) + '  ' + Math.round(bearing) + '°');
            } else if (obj.type === 'rect') {
                label(obj.x + obj.w / 2, obj.y - 8 / session.cam.z, formatLen(Math.abs(obj.w)));
                label(obj.x + obj.w + 8 / session.cam.z, obj.y + obj.h / 2, formatLen(Math.abs(obj.h)));
                label(obj.x + 14 / session.cam.z, obj.y + 12 / session.cam.z, '90°');
            } else if (obj.type === 'ellipse') {
                label(obj.x + obj.w / 2, obj.y - 8 / session.cam.z, formatLen(Math.abs(obj.w)) + ' × ' + formatLen(Math.abs(obj.h)));
            } else if ((obj.type === 'polyline' || obj.type === 'wall') && obj.points && obj.points.length >= 2) {
                var pts = obj.points;
                var count = obj.closed ? pts.length : pts.length - 1;
                var i;
                for (i = 0; i < count; i++) {
                    var a = pts[i];
                    var b = pts[(i + 1) % pts.length];
                    if (dist(a.x, a.y, b.x, b.y) < 2) continue;
                    label((a.x + b.x) / 2, (a.y + b.y) / 2 - 7 / session.cam.z, formatLen(dist(a.x, a.y, b.x, b.y)));
                }
                var corners = obj.closed ? pts.length : Math.max(0, pts.length - 2);
                var start = obj.closed ? 0 : 1;
                for (i = 0; i < corners; i++) {
                    var idx = (start + i) % pts.length;
                    var prev = pts[(idx - 1 + pts.length) % pts.length];
                    var cur = pts[idx];
                    var next = pts[(idx + 1) % pts.length];
                    label(cur.x, cur.y - 9 / session.cam.z, cornerDegrees(prev, cur, next) + '°');
                }
            }
            ctx.restore();
        }

        var WALL_JOIN = 6;

        function floorForWalls(floor) {
            var objects = (floor && floor.objects) ? floor.objects.slice() : [];
            var draft = session.draft;
            if (!draft || draft.tool !== 'wall' || session.mode !== 'edit') return { objects: objects };
            var points = null;
            if (draft.points && draft.points.length >= 2) points = draft.points;
            else if (draft.phase === 1 && draft.x1 != null) {
                points = [{ x: draft.x1, y: draft.y1 }, { x: draft.x2, y: draft.y2 }];
            }
            if (!points) return { objects: objects };
            objects.push({ type: 'wall', points: points, thickness: 15, closed: false, draft: true });
            return { objects: objects };
        }

        function wallSegments(floor, self) {
            var list = [];
            (floor.objects || []).forEach(function (item, rank) {
                if (!item || item === self || item.type !== 'wall' || !item.points || item.points.length < 2) return;
                var pts = item.points;
                var thick = item.thickness || 15;
                var i;
                for (i = 1; i < pts.length; i++) list.push({ a: pts[i - 1], b: pts[i], thick: thick, rank: rank });
                if (item.closed && pts.length > 2) list.push({ a: pts[pts.length - 1], b: pts[0], thick: thick, rank: rank });
            });
            return list;
        }

        function wallRank(floor, wall) {
            var objects = floor.objects || [];
            var i;
            for (i = 0; i < objects.length; i++) if (objects[i] === wall) return i;
            return 1000000;
        }

        function wallVec(a, b) {
            var dx = b.x - a.x;
            var dy = b.y - a.y;
            var len = Math.sqrt(dx * dx + dy * dy) || 1;
            return { x: dx / len, y: dy / len };
        }

        function wallTurn(u, v) {
            var dot = u.x * v.x + u.y * v.y;
            if (dot > 1) dot = 1;
            if (dot < -1) dot = -1;
            return Math.acos(dot) * 180 / Math.PI;
        }

        function wallLeft(dir) {
            return { x: dir.y, y: -dir.x };
        }

        function wallNear(a, b) {
            return dist(a.x, a.y, b.x, b.y) <= WALL_JOIN;
        }

        function slideCap(p, s, half, delta, cap) {
            var toward = wallVec(s, p);
            var leftN = wallLeft(toward);
            var limit = Math.abs(half) * 1.4 + 2;
            if (delta > limit) delta = limit;
            if (delta < -limit) delta = -limit;
            var capP = { x: p.x + toward.x * delta, y: p.y + toward.y * delta };
            return {
                cap: !!cap,
                left: { x: capP.x + leftN.x * half, y: capP.y + leftN.y * half },
                right: { x: capP.x - leftN.x * half, y: capP.y - leftN.y * half }
            };
        }

        function meetAngle(toward, dir) {
            return wallTurn(toward, dir);
        }

        function endJoin(segs, p, s, half, rank) {
            var toward = wallVec(s, p);
            var len = dist(p.x, p.y, s.x, s.y);
            var ends = [];
            var bodies = [];
            segs.forEach(function (seg) {
                var atA = wallNear(seg.a, p);
                var atB = wallNear(seg.b, p);
                if (atA && wallNear(seg.b, s)) return;
                if (atB && wallNear(seg.a, s)) return;
                if (atA) ends.push({ other: seg.b, thick: seg.thick, rank: seg.rank });
                else if (atB) ends.push({ other: seg.a, thick: seg.thick, rank: seg.rank });
                else {
                    var hit = projectSeg(seg.a.x, seg.a.y, seg.b.x, seg.b.y, p.x, p.y);
                    if (hit.t > 0.04 && hit.t < 0.96 && hit.dist <= WALL_JOIN) {
                        bodies.push({ a: seg.a, b: seg.b, thick: seg.thick, dist: hit.dist, dir: wallVec(seg.a, seg.b) });
                    }
                }
            });
            var straight = [];
            var angled = [];
            ends.forEach(function (end) {
                var deg = meetAngle(toward, wallVec(p, end.other));
                if (deg < 12 || deg > 168) straight.push(end);
                else angled.push(end);
            });
            if (straight.length) return { cap: false };
            var pair = null;
            var i;
            var j;
            for (i = 0; i < angled.length && !pair; i++) {
                for (j = i + 1; j < angled.length; j++) {
                    if (meetAngle(wallVec(p, angled[i].other), wallVec(p, angled[j].other)) > 168) pair = [angled[i], angled[j]];
                }
            }
            function butt(barDir, barThick) {
                var deg = meetAngle(toward, barDir);
                var sin = Math.sin(deg * Math.PI / 180);
                if (sin < 0.34) return { cap: false };
                var cut = (barThick / 2) / sin - 1.5;
                if (cut < 0) cut = 0;
                if (cut > len * 0.45) cut = len * 0.45;
                return slideCap(p, s, half, -cut);
            }
            if (pair) {
                return butt(wallVec(pair[0].other, pair[1].other), Math.max(pair[0].thick, pair[1].thick));
            }
            if (angled.length === 1) {
                var other = angled[0];
                var away = wallVec(p, other.other);
                var deg = meetAngle(toward, away);
                var sin = Math.sin(deg * Math.PI / 180);
                if (sin < 0.34) return { cap: false };
                if (other.rank < rank) return butt(away, other.thick);
                var ext = (other.thick / 2) / sin;
                return slideCap(p, s, half, ext, true);
            }
            if (angled.length) return { cap: false };
            if (bodies.length) {
                bodies.sort(function (a, b) { return a.dist - b.dist; });
                return butt(bodies[0].dir, bodies[0].thick);
            }
            return { cap: true };
        }

        function drawWall(ctx, wall, floor, mode) {
            var points = wall.points || [];
            if (points.length < 2) return;
            var joined = floorForWalls(floor);
            var half = (wall.thickness || 15) / 2;
            if (wall.alignment === 'left') half = wall.thickness || 15;
            var openings = (floor.objects || []).filter(function (obj) {
                return obj.type === 'opening' && obj.wallId === wall.id;
            });
            ctx.save();
            ctx.globalAlpha = wall.opacity == null ? 1 : wall.opacity;
            ctx.strokeStyle = wall.color || '#172b4d';
            ctx.lineWidth = 1.2;
            ctx.fillStyle = wall.fill || '#eef2f6';
            var closed = !!wall.closed && points.length > 2;
            var leftEdge = offsetPoints(points, closed, half);
            var rightEdge = offsetPoints(points, closed, -half);
            var capStart = !closed;
            var capEnd = !closed;
            if (!closed) {
                var segs = wallSegments(joined, wall);
                var rank = wallRank(joined, wall);
                var start = endJoin(segs, points[0], points[1], half, rank);
                var end = endJoin(segs, points[points.length - 1], points[points.length - 2], half, rank);
                if (start.left) {
                    // The start cap is measured back along the wall, so its left and right are swapped.
                    leftEdge[0] = start.right;
                    rightEdge[0] = start.left;
                }
                var tail = points.length - 1;
                if (end.left) {
                    leftEdge[tail] = end.left;
                    rightEdge[tail] = end.right;
                }
                capStart = !!start.cap;
                capEnd = !!end.cap;
            }
            var i;
            for (i = 1; i < points.length; i++) {
                drawWallSegment(ctx, wall, points[i - 1], points[i], leftEdge[i - 1], leftEdge[i], rightEdge[i - 1], rightEdge[i], i - 1, openings, {
                    start: i === 1 && capStart,
                    end: i === points.length - 1 && capEnd
                }, mode);
            }
            if (closed) {
                var last = points.length - 1;
                drawWallSegment(ctx, wall, points[last], points[0], leftEdge[last], leftEdge[0], rightEdge[last], rightEdge[0], last, openings, { start: false, end: false }, mode);
            }
            ctx.restore();
        }

        function drawWallSegment(ctx, wall, a, b, la, lb, ra, rb, index, openings, caps, mode) {
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
            function along(p, q, t) { return { x: p.x + (q.x - p.x) * t, y: p.y + (q.y - p.y) * t }; }
            function band(t0, t1) {
                quad(ctx, [along(la, lb, t0), along(la, lb, t1), along(ra, rb, t1), along(ra, rb, t0)], wall, {
                    start: t0 <= 0.001 && caps.start,
                    end: t1 >= 0.999 && caps.end,
                    cutStart: t0 > 0.001,
                    cutEnd: t1 < 0.999
                }, mode);
            }
            for (i = 0; i < gaps.length; i++) {
                var gap = gaps[i];
                var from = Math.max(0, gap.from);
                var to = Math.min(1, gap.to);
                if (from > cursor) band(cursor, from);
                if (!mode || mode === 'body' || mode === 'all') drawOpening(ctx, at(from), at(to), gap.opening);
                cursor = to;
            }
            if (cursor < 1) band(cursor, 1);
        }

        function quad(ctx, poly, wall, caps, mode) {
            if (!poly || poly.length < 4) return;
            if (dist(poly[0].x, poly[0].y, poly[1].x, poly[1].y) < 0.5 && dist(poly[0].x, poly[0].y, poly[3].x, poly[3].y) < 0.5) return;
            var paint = mode || 'all';
            if (paint === 'body' || paint === 'cover' || paint === 'all') {
                pathOf(ctx, poly, true);
                ctx.fill();
            }
            if ((paint === 'hatch' || paint === 'all') && wall.hatch !== 'none') {
                hatch(ctx, poly, wall.hatch || 'diagonal', wall.color || '#97a0af');
            }
            if (paint === 'stroke' || paint === 'all') {
                ctx.beginPath();
                ctx.moveTo(poly[0].x, poly[0].y);
                ctx.lineTo(poly[1].x, poly[1].y);
                ctx.moveTo(poly[3].x, poly[3].y);
                ctx.lineTo(poly[2].x, poly[2].y);
                if (caps && (caps.end || caps.cutEnd)) {
                    ctx.moveTo(poly[1].x, poly[1].y);
                    ctx.lineTo(poly[2].x, poly[2].y);
                }
                if (caps && (caps.start || caps.cutStart)) {
                    ctx.moveTo(poly[3].x, poly[3].y);
                    ctx.lineTo(poly[0].x, poly[0].y);
                }
                ctx.stroke();
            }
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
                var y0 = Math.floor(minY / 6) * 6;
                var y;
                for (y = y0; y <= maxY; y += 6) {
                    var shift = Math.round(y / 6) % 2 ? 8 : 0;
                    var x0 = Math.floor(minX / 16) * 16 + shift;
                    var x;
                    ctx.moveTo(minX, y);
                    ctx.lineTo(maxX, y);
                    for (x = x0; x <= maxX; x += 16) {
                        ctx.moveTo(x, y);
                        ctx.lineTo(x, Math.min(maxY, y + 6));
                    }
                }
            } else if (kind === 'concrete') {
                var cx0 = Math.floor(minX / 4) * 4;
                var cy0 = Math.floor(minY / 4) * 4;
                var cx;
                var cy;
                for (cy = cy0; cy <= maxY; cy += 4) {
                    for (cx = cx0; cx <= maxX; cx += 4) {
                        ctx.moveTo(cx, cy);
                        ctx.lineTo(cx + 0.6, cy);
                    }
                }
            } else {
                var low = minY - maxX;
                var high = maxY - minX;
                var c = Math.floor(low / 4) * 4;
                for (; c <= high; c += 4) {
                    ctx.moveTo(minX, minX + c);
                    ctx.lineTo(maxX, maxX + c);
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

        function assetGlyph(obj) {
            if (obj.icon) return { d: obj.icon, color: obj.iconColor || '#5D6B82' };
            var binding = obj.assetBinding;
            if (!binding) return null;
            var asset = assetById(binding.objectId);
            if (!asset || !asset.icon) return null;
            return { d: asset.icon, color: asset.iconColor || '#5D6B82' };
        }

        function drawAssetMark(ctx, obj) {
            var s = 36;
            var x = obj.x;
            var y = obj.y;
            var glyph = assetGlyph(obj);
            ctx.save();
            ctx.translate(x, y);
            ctx.fillStyle = obj.fill || (glyph ? glyph.color : '#ffffff');
            ctx.strokeStyle = glyph ? glyph.color : (obj.color || '#0052cc');
            ctx.lineWidth = 1.2;
            ctx.beginPath();
            ctx.rect(0, 0, s, s);
            ctx.fill();
            ctx.stroke();
            if (glyph) {
                try {
                    ctx.save();
                    ctx.translate(6, 6);
                    ctx.scale((s - 12) / 24, (s - 12) / 24);
                    ctx.fillStyle = '#ffffff';
                    ctx.fill(new Path2D(glyph.d), 'evenodd');
                    ctx.restore();
                } catch (error) {
                    glyph = null;
                }
            }
            if (!glyph) {
                ctx.strokeStyle = obj.color || '#0052cc';
                ctx.beginPath();
                ctx.strokeRect(8, 8, 20, 13);
                ctx.beginPath();
                ctx.moveTo(14, 21);
                ctx.lineTo(22, 21);
                ctx.moveTo(18, 21);
                ctx.lineTo(18, 26);
                ctx.moveTo(13, 26);
                ctx.lineTo(23, 26);
                ctx.stroke();
            }
            ctx.restore();
            var name = obj.text || (obj.assetBinding && obj.assetBinding.objectLabel) || '';
            if (name) {
                ctx.save();
                ctx.fillStyle = '#172b4d';
                ctx.font = (12 / session.cam.z) + 'px sans-serif';
                ctx.textAlign = 'center';
                ctx.textBaseline = 'top';
                ctx.fillText(name, x + s / 2, y + s + 4);
                ctx.restore();
            }
        }

        function drawObject(ctx, obj, floor) {
            if (!layerVisible(obj.layerId)) return;
            ctx.save();
            ctx.globalAlpha = obj.opacity == null ? 1 : obj.opacity;
            ctx.strokeStyle = obj.color || '#172b4d';
            ctx.fillStyle = obj.color || '#172b4d';
            ctx.lineWidth = obj.thickness || 1;
            if (obj.angle && obj.x != null && obj.w != null) rotateObj(ctx, obj);
            if (obj.type === 'line') {
                ctx.beginPath();
                ctx.moveTo(obj.x1, obj.y1);
                ctx.lineTo(obj.x2, obj.y2);
                ctx.stroke();
            } else if (obj.type === 'rect') {
                ctx.beginPath();
                ctx.rect(obj.x, obj.y, obj.w, obj.h);
                paintFill(ctx, obj);
                ctx.stroke();
                if (obj.material) hatchShape(ctx, [
                    { x: obj.x, y: obj.y }, { x: obj.x + obj.w, y: obj.y },
                    { x: obj.x + obj.w, y: obj.y + obj.h }, { x: obj.x, y: obj.y + obj.h }
                ], obj.material, obj.color);
                if (obj.text) drawShapeText(ctx, obj.text, obj.x + obj.w / 2, obj.y + obj.h / 2, obj.color);
            } else if (obj.type === 'ellipse') {
                ctx.beginPath();
                ctx.ellipse(obj.x + obj.w / 2, obj.y + obj.h / 2, Math.abs(obj.w) / 2, Math.abs(obj.h) / 2, 0, 0, Math.PI * 2);
                paintFill(ctx, obj);
                ctx.stroke();
                if (obj.material) hatchShape(ctx, ellipsePoly(obj), obj.material, obj.color);
                if (obj.text) drawShapeText(ctx, obj.text, obj.x + obj.w / 2, obj.y + obj.h / 2, obj.color);
            } else if (obj.type === 'polyline') {
                pathCurve(ctx, obj.points, obj.closed);
                if (obj.closed) paintFill(ctx, obj);
                ctx.stroke();
                if (obj.closed && obj.material) hatchShape(ctx, obj.points, obj.material, obj.color);
                if (obj.text && obj.points && obj.points.length) {
                    var middle = centroid(obj.points);
                    drawShapeText(ctx, obj.text, middle.x, middle.y, obj.color);
                }
            } else if (obj.type === 'wall') {
                if (obj.material) obj.hatch = materialHatch(obj.material);
                drawWall(ctx, obj, floor);
                if (obj.text && obj.points && obj.points.length) {
                    var wallMiddle = centroid(obj.points);
                    drawShapeText(ctx, obj.text, wallMiddle.x, wallMiddle.y, obj.color);
                }
            } else if (obj.type === 'text') {
                ctx.font = (obj.h || 18) + 'px sans-serif';
                ctx.fillText(obj.text || '', obj.x, obj.y + (obj.h || 18));
            } else if (obj.type === 'dimension') {
                drawDimension(ctx, obj);
            } else if (obj.type === 'asset') {
                drawAssetMark(ctx, obj);
            } else if (obj.type === 'symbol') {
                if (obj.fill) {
                    ctx.save();
                    ctx.globalAlpha *= 0.4;
                    ctx.fillStyle = obj.fill;
                    ctx.fillRect(obj.x, obj.y, obj.w || 40, obj.h || 40);
                    ctx.restore();
                }
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
            drawMeasures(ctx, obj);
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

        var imageCache = {};

        function cachedImage(src) {
            if (!src) return null;
            if (!imageCache[src]) {
                var image = new Image();
                image.onload = function () { paint(); };
                image.src = src;
                imageCache[src] = image;
            }
            var stored = imageCache[src];
            if (!stored.complete || !stored.naturalWidth) return null;
            return stored;
        }

        function drawSchemeImages(ctx, floor) {
            var objects = (floor && floor.objects) || [];
            var i;
            for (i = 0; i < objects.length; i++) {
                var obj = objects[i];
                if (obj.type !== 'image' || !layerVisible(obj.layerId)) continue;
                var image = cachedImage(obj.src);
                if (!image) continue;
                ctx.save();
                ctx.globalAlpha = obj.opacity == null ? 0.72 : obj.opacity;
                if (obj.angle) rotateObj(ctx, obj);
                ctx.drawImage(image, obj.x || 0, obj.y || 0, obj.w || 10, obj.h || 10);
                ctx.restore();
            }
        }

        function drawFloor(ctx, floor, ghost) {
            ctx.save();
            if (!ghost) drawSchemeScan(ctx, floor);
            if (!ghost) drawSchemeImages(ctx, floor);
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
                if (objects[i].type === 'opening' || objects[i].type === 'wall' || objects[i].type === 'image' || objects[i].type === 'marker') continue;
                drawObject(ctx, objects[i], floor);
            }
            drawWallLayer(ctx, floor);
            drawMarkers(ctx, floor);
            ctx.restore();
        }

        function drawMarkerStroke(ctx, obj) {
            var points = obj.points || [];
            if (points.length < 2) return;
            ctx.save();
            ctx.globalAlpha = obj.opacity == null ? 0.45 : obj.opacity;
            ctx.strokeStyle = obj.color || '#ffe380';
            ctx.lineWidth = obj.thickness || 18;
            ctx.lineCap = 'round';
            ctx.lineJoin = 'round';
            ctx.beginPath();
            ctx.moveTo(points[0].x, points[0].y);
            var i;
            for (i = 1; i < points.length; i++) ctx.lineTo(points[i].x, points[i].y);
            ctx.stroke();
            ctx.restore();
        }

        function drawMarkers(ctx, floor) {
            var objects = (floor && floor.objects) || [];
            var i;
            for (i = 0; i < objects.length; i++) {
                if (objects[i].type === 'marker' && layerVisible(objects[i].layerId)) drawMarkerStroke(ctx, objects[i]);
            }
        }

        function drawWallLayer(ctx, floor) {
            var objects = floor.objects || [];
            var walls = [];
            var i;
            for (i = 0; i < objects.length; i++) {
                if (objects[i].type === 'wall') walls.push(objects[i]);
            }
            if (!walls.length) return;
            for (i = 0; i < walls.length; i++) drawWall(ctx, walls[i], floor, 'body');
            for (i = 0; i < walls.length; i++) drawWall(ctx, walls[i], floor, 'stroke');
            for (i = 0; i < walls.length; i++) drawWall(ctx, walls[i], floor, 'cover');
            for (i = 0; i < walls.length; i++) drawWall(ctx, walls[i], floor, 'hatch');
        }

        function settleWindows() {
            if (!windows.clientWidth || windows.clientWidth < 40) return;
            var id;
            for (id in panelNodes) {
                var node = panelNodes[id];
                if (node.classList.contains('is-pinned')) {
                    var side = node.getAttribute('data-side');
                    if (side === 'free') {
                        if (node.parentNode !== windows) windows.appendChild(node);
                    } else {
                        var home = dockBySide(side);
                        if (node.parentNode !== home) placePinned(node);
                    }
                    continue;
                }
                if (!node.getAttribute('data-settled')) {
                    var saved = panelState[id];
                    if (!saved || saved.x == null) {
                        var box = defaultPanelBox(id);
                        node.style.left = box.x + 'px';
                        node.style.top = box.y + 'px';
                    }
                    node.setAttribute('data-settled', '1');
                }
                var x = parseInt(node.style.left, 10);
                var y = parseInt(node.style.top, 10);
                if (!isFinite(x)) x = 12;
                if (!isFinite(y)) y = 12;
                var maxX = Math.max(8, windows.clientWidth - 80);
                var maxY = Math.max(8, windows.clientHeight - 36);
                if (x < 0 || x > maxX) node.style.left = Math.min(maxX, Math.max(8, x)) + 'px';
                if (y < 0 || y > maxY) node.style.top = Math.min(maxY, Math.max(8, y)) + 'px';
            }
        }

        function paint() {
            if (!stage.clientWidth || !stage.clientHeight) return;
            settleWindows();
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
                    ctx.save();
                    if (obj.angle && obj.x != null && obj.w != null) rotateObj(ctx, obj);
                    if (obj.type === 'polyline') pathCurve(ctx, obj.points, !!obj.closed);
                    else if (obj.points) pathOf(ctx, obj.points, !!obj.closed);
                    else if (obj.x1 != null) {
                        ctx.beginPath();
                        ctx.moveTo(obj.x1, obj.y1);
                        ctx.lineTo(obj.x2, obj.y2);
                    } else if (obj.w != null) ctx.strokeRect(obj.x, obj.y, obj.w, obj.h);
                    ctx.stroke();
                    ctx.restore();
                });
                ctx.setLineDash([]);
                if (session.mode === 'edit' && objects.length === 1 && (objects[0].type === 'symbol' || objects[0].type === 'asset') && objects[0].w != null) {
                    var knob = knobOf(objects[0]);
                    var hubX = objects[0].x + objects[0].w / 2;
                    var hubY = objects[0].y + (objects[0].h || 0) / 2;
                    ctx.beginPath();
                    ctx.moveTo(hubX, hubY);
                    ctx.lineTo(knob.x, knob.y);
                    ctx.stroke();
                    ctx.beginPath();
                    ctx.fillStyle = '#0052cc';
                    ctx.arc(knob.x, knob.y, 4 / session.cam.z, 0, Math.PI * 2);
                    ctx.fill();
                }
                if (session.draft && session.draft.points) {
                    if (session.draft.tool === 'marker') {
                        drawMarkerStroke(ctx, {
                            points: session.draft.points,
                            color: session.ink || '#ffe380',
                            thickness: Math.max(8, 22 / (session.cam.z || 1)),
                            opacity: 0.45
                        });
                    } else if (session.draft.tool === 'wall') {
                        drawWall(ctx, {
                            points: session.draft.points,
                            thickness: 15,
                            color: '#172b4d',
                            hatch: 'diagonal',
                            opacity: 1
                        }, activeFloor());
                    } else {
                        ctx.strokeStyle = '#0052cc';
                        ctx.lineWidth = 1;
                        pathCurve(ctx, session.draft.points, false);
                        ctx.stroke();
                        drawPenHandles(ctx, session.draft.points);
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
                    } else if (session.draft.tool === 'wall') {
                        drawWall(ctx, {
                            points: [
                                { x: session.draft.x1, y: session.draft.y1 },
                                { x: session.draft.x2, y: session.draft.y2 }
                            ],
                            thickness: 15,
                            color: '#172b4d',
                            hatch: 'diagonal',
                            opacity: 1
                        }, activeFloor());
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
            var live = measuresOn() ? liveLength() : null;
            if (live) badge(over, live.x, live.y, live.text);
            status.textContent = api.t('schemeReadout') + '  ' + (session.hover ? (roundCm(session.hover.x) + ', ' + roundCm(session.hover.y) + ' ' + (session.doc.settings.unit || 'cm')) : '') +
                '   ' + api.t('schemeGrid') + ' ' + formatLen(gridStep()) +
                (live ? '   ' + api.t('schemeLength') + ' ' + live.text : '') +
                (session.typed ? '   ' + session.typed : '');
            if (session.tool === 'wall') status.textContent += '   ' + api.t('schemeWallOne');
            if (session.tool === 'marker') status.textContent += '   ' + api.t('schemeMarkerHint');
            if (session.tool === 'dropper') status.textContent += '   ' + api.t('schemeDropperHint');
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
                if (session.draft.tool === 'rect') text += '  90°';
            } else {
                var bearing = Math.atan2(b.y - a.y, b.x - a.x) * 180 / Math.PI;
                if (bearing < 0) bearing += 360;
                text += '  ' + Math.round(bearing) + '°';
                if (session.draft.points && session.draft.points.length >= 3) {
                    var prev = session.draft.points[session.draft.points.length - 3];
                    text += '  ∠' + cornerDegrees(prev, a, b) + '°';
                }
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
            openPanel('floors');
            var floorTitle = el('div', 'asset-tree-scheme-label', api.t('schemeFloors'));
            bin.appendChild(floorTitle);
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
                    if (!api.canEdit || session.guest) return;
                    askBox({
                        title: api.t('name'),
                        input: true,
                        value: floor.name,
                        onOk: function (next) {
                            remember();
                            floor.name = next.slice(0, 80);
                            refreshSide();
                        }
                    });
                });
                bin.appendChild(row);
            });
            var addFloor = el('button', 'asset-tree-scheme-add', api.t('schemeFloorNew'));
            addFloor.type = 'button';
            addFloor.disabled = !api.canEdit || session.guest;
            addFloor.addEventListener('click', function () {
                remember();
                var floor = { id: nid('floor'), name: api.t('schemeFloor') + ' ' + (session.doc.floors.length + 1), elevation: session.doc.floors.length * 300, objects: [], rooms: [] };
                session.doc.floors.push(floor);
                session.doc.activeFloorId = floor.id;
                refreshSide();
                paint();
            });
            bin.appendChild(addFloor);
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
            bin.appendChild(ghost);

            openPanel('layers');
            bin.appendChild(el('div', 'asset-tree-scheme-label', api.t('schemeLayers')));
            (session.doc.layers || []).forEach(function (layer) {
                var hidden = layer.visible === false;
                var row = el('div', 'asset-tree-scheme-layer' + (layer.id === session.doc.activeLayerId ? ' is-on' : ''));
                var eye = el('button', 'asset-tree-scheme-icon');
                eye.type = 'button';
                eye.title = hidden ? api.t('schemeShow') : api.t('schemeHide');
                eye.innerHTML = toolIcon(hidden ? 'eyeOff' : 'eye');
                eye.addEventListener('click', function () {
                    layer.visible = hidden;
                    session.dirty = true;
                    refreshSide();
                    paint();
                });
                var name = el('button', 'asset-tree-scheme-layer-name', layer.name);
                name.type = 'button';
                name.addEventListener('click', function () {
                    session.doc.activeLayerId = layer.id;
                    refreshSide();
                });
                name.addEventListener('dblclick', function () {
                    if (!api.canEdit || session.guest) return;
                    askBox({
                        title: api.t('name'),
                        input: true,
                        value: layer.name,
                        onOk: function (next) {
                            remember();
                            layer.name = next.slice(0, 40);
                            refreshSide();
                        }
                    });
                });
                var lock = el('button', 'asset-tree-scheme-icon' + (layer.locked ? ' is-locked' : ''));
                lock.type = 'button';
                lock.title = layer.locked ? api.t('schemeUnlock') : api.t('schemeLock');
                lock.innerHTML = toolIcon(layer.locked ? 'lock' : 'unlock');
                lock.disabled = !api.canEdit || session.guest;
                lock.addEventListener('click', function () {
                    layer.locked = !layer.locked;
                    session.dirty = true;
                    refreshSide();
                    paint();
                });
                row.appendChild(eye);
                row.appendChild(name);
                row.appendChild(lock);
                if ((session.doc.layers || []).length > 1) {
                    var dropLayer = el('button', 'asset-tree-scheme-icon', '×');
                    dropLayer.type = 'button';
                    dropLayer.title = api.t('schemeLayerDelete');
                    dropLayer.disabled = !api.canEdit || session.guest;
                    dropLayer.addEventListener('click', function () { deleteLayer(layer); });
                    row.appendChild(dropLayer);
                }
                bin.appendChild(row);
            });
            var addLayer = el('button', 'asset-tree-scheme-add', api.t('schemeLayerNew'));
            addLayer.type = 'button';
            addLayer.disabled = !api.canEdit || session.guest;
            addLayer.addEventListener('click', function () {
                askBox({
                    title: api.t('schemeLayerNew'),
                    input: true,
                    value: '',
                    onOk: function (next) {
                        remember();
                        var id = nid('layer');
                        session.doc.layers.push({ id: id, name: next.slice(0, 40), visible: true, locked: false, color: '#172b4d' });
                        session.doc.activeLayerId = id;
                        refreshSide();
                    }
                });
            });
            bin.appendChild(addLayer);

            openPanel('rooms');
            bin.appendChild(el('div', 'asset-tree-scheme-label', api.t('schemeRooms')));
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
                bin.appendChild(row);
            });

            openPanel('symbols');
            bin.appendChild(el('div', 'asset-tree-scheme-label', api.t('schemeSymbols')));
            var symbolSearch = el('input', 'asset-tree-scheme-name');
            symbolSearch.placeholder = api.t('schemeSearch');
            symbolSearch.value = session.symbolQuery || '';
            var symbolList = el('div', 'asset-tree-scheme-asset-list asset-tree-scheme-template-list');
            var paintSymbols = function () {
                symbolList.innerHTML = '';
                var query = String(symbolSearch.value || '').toLowerCase();
                var count = 0;
                libraryTemplates().forEach(function (current) {
                    var name = current.label || current.name || '';
                    if (query && name.toLowerCase().indexOf(query) < 0) return;
                    count++;
                    var armed = session.symbolId === current.id;
                    var row = el('div', 'asset-tree-scheme-asset' + (armed ? ' is-on' : ''));
                    var card = el('button', 'asset-tree-scheme-asset-main');
                    card.type = 'button';
                    card.disabled = !api.canEdit || session.mode !== 'edit' || session.guest;
                    card.title = armed ? api.t('schemeTemplateOff') : name;
                    card.appendChild(el('strong', null, name));
                    card.addEventListener('click', function () {
                        if (!api.canEdit || session.mode !== 'edit' || session.guest) return;
                        if (session.symbolId === current.id) {
                            session.symbolId = '';
                            session.tool = 'select';
                        } else {
                            session.symbolId = current.id;
                            session.assetId = 0;
                            session.tool = 'symbol';
                        }
                        markTools();
                        refreshSide();
                        paint();
                    });
                    card.addEventListener('contextmenu', function (event) {
                        event.preventDefault();
                        event.stopPropagation();
                        if (session.guest) return;
                        showLibraryMenu(event.clientX, event.clientY, current);
                    });
                    row.appendChild(card);
                    symbolList.appendChild(row);
                });
                if (!count) symbolList.appendChild(el('p', 'asset-tree-scheme-muted', api.t('noResults')));
            };
            symbolSearch.addEventListener('input', function () {
                session.symbolQuery = symbolSearch.value;
                paintSymbols();
            });
            var libraryRow = el('div', 'asset-tree-scheme-library-row');
            var libraryFile = el('input');
            libraryFile.type = 'file';
            libraryFile.accept = 'application/json,.json';
            libraryFile.style.display = 'none';
            var loadLibrary = el('button', 'asset-tree-scheme-mini');
            loadLibrary.type = 'button';
            loadLibrary.title = api.t('schemeLibraryLoad');
            loadLibrary.innerHTML = '<svg viewBox="0 0 16 16" aria-hidden="true"><path d="M8 12V4M5 7l3-3 3 3" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/></svg>';
            loadLibrary.disabled = !api.canEdit || !!session.guest;
            loadLibrary.addEventListener('click', function () { libraryFile.click(); });
            libraryFile.addEventListener('change', function () {
                var picked = libraryFile.files && libraryFile.files[0];
                libraryFile.value = '';
                if (picked) importLibraryFile(picked);
            });
            var saveLibrary = el('button', 'asset-tree-scheme-mini');
            saveLibrary.type = 'button';
            saveLibrary.title = api.t('schemeLibrarySave');
            saveLibrary.innerHTML = '<svg viewBox="0 0 16 16" aria-hidden="true"><path d="M8 4v8M5 9l3 3 3-3" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/></svg>';
            saveLibrary.addEventListener('click', exportLibraryFile);
            libraryRow.appendChild(symbolSearch);
            libraryRow.appendChild(loadLibrary);
            libraryRow.appendChild(saveLibrary);
            libraryRow.appendChild(libraryFile);
            bin.appendChild(libraryRow);
            bin.appendChild(symbolList);
            paintSymbols();
            renderAssetList();
            parkPanels();
        }

        function libraryKey() {
            return 'assetshub.schemeLibs.' + (api.projectKey || '');
        }

        function readStoredLibraries() {
            try {
                var raw = global.localStorage.getItem(libraryKey());
                var list = raw ? JSON.parse(raw) : [];
                return Array.isArray(list) ? list : [];
            } catch (error) {
                return [];
            }
        }

        function writeStoredLibraries(list) {
            try { global.localStorage.setItem(libraryKey(), JSON.stringify((list || []).slice(0, 30))); } catch (error) { /* quota */ }
        }

        function stashLibrary(lib) {
            if (!lib || !lib.id || !lib.templates || !lib.templates.length) return;
            var stored = readStoredLibraries().filter(function (item) { return item.id !== lib.id; });
            stored.unshift(lib);
            writeStoredLibraries(stored);
        }

        function rememberLibrary(lib) {
            stashLibrary(lib);
            if (!session.doc.libraries) session.doc.libraries = [];
            session.doc.libraries = session.doc.libraries.filter(function (item) { return item.id !== lib.id; });
            session.doc.libraries.push(lib);
            session.dirty = true;
        }

        function allLibraries() {
            var out = [];
            var seen = {};
            function add(list) {
                (list || []).forEach(function (lib) {
                    if (!lib || !lib.id || seen[lib.id]) return;
                    seen[lib.id] = true;
                    out.push(lib);
                });
            }
            add(session.doc && session.doc.libraries);
            add(readStoredLibraries());
            return out;
        }

        function cleanTemplate(item) {
            if (!item || !item.parts || !item.parts.length) return null;
            var id = String(item.id || nid('sym')).replace(/[^\w.-]/g, '').slice(0, 60);
            return {
                id: id || nid('sym'),
                name: String(item.name || api.t('schemeTemplateNew')).slice(0, 80),
                w: Math.max(1, Number(item.w) || 40),
                h: Math.max(1, Number(item.h) || 40),
                parts: item.parts.slice(0, 400)
            };
        }

        function importLibraryFile(file) {
            if (!file || !api.canEdit || session.guest) return;
            if (file.size > 1500000) {
                status.textContent = api.t('schemeLibraryBad');
                return;
            }
            var reader = new FileReader();
            reader.onload = function () {
                var parsed;
                try { parsed = JSON.parse(String(reader.result || '')); } catch (error) {
                    status.textContent = api.t('schemeLibraryBad');
                    return;
                }
                var list = Array.isArray(parsed) ? parsed : ((parsed && (parsed.templates || parsed.symbols)) || []);
                var templates = [];
                var i;
                for (i = 0; i < list.length && templates.length < 200; i++) {
                    var one = cleanTemplate(list[i]);
                    if (one) templates.push(one);
                }
                if (!templates.length) {
                    status.textContent = api.t('schemeLibraryBad');
                    return;
                }
                remember();
                rememberLibrary({
                    id: nid('lib'),
                    name: String((parsed && parsed.name) || file.name || api.t('schemeLibraryName')).replace(/\.json$/i, '').slice(0, 80),
                    templates: templates
                });
                refreshSide();
                status.textContent = api.t('schemeLibraryDone');
            };
            reader.readAsText(file);
        }

        function exportLibraryFile() {
            var templates = [];
            var seen = {};
            function add(def) {
                if (!def || !def.id || seen[def.id] || !def.parts) return;
                seen[def.id] = true;
                templates.push({ id: def.id, name: def.name || '', w: def.w, h: def.h, parts: def.parts });
            }
            var id;
            for (id in (session.doc.symbols || {})) add(session.doc.symbols[id]);
            allLibraries().forEach(function (pack) {
                (pack.templates || []).forEach(add);
            });
            builtins(api.t).forEach(add);
            var blob = new Blob([JSON.stringify({ name: (session.doc && session.doc.name) || api.t('schemeLibraryName'), templates: templates }, null, 2)], { type: 'application/json' });
            var link = el('a');
            link.href = URL.createObjectURL(blob);
            link.download = ((session.doc && session.doc.name) || 'templates') + '.json';
            link.click();
            global.setTimeout(function () { URL.revokeObjectURL(link.href); }, 1000);
        }

        function libraryTemplates() {
            var hidden = {};
            (session.doc.hiddenSymbols || []).forEach(function (id) { hidden[id] = true; });
            var library = [];
            var seen = {};
            builtins(api.t).forEach(function (def) {
                if (hidden[def.id]) return;
                seen[def.id] = true;
                library.push((session.doc.symbols && session.doc.symbols[def.id]) || def);
            });
            var customId;
            for (customId in (session.doc.symbols || {})) {
                if (!seen[customId] && !hidden[customId]) library.push(session.doc.symbols[customId]);
            }
            allLibraries().forEach(function (pack) {
                (pack.templates || []).forEach(function (def) {
                    if (!def || !def.id || hidden[def.id] || seen[def.id]) return;
                    seen[def.id] = true;
                    library.push({
                        id: def.id,
                        name: def.name,
                        label: pack.name ? (pack.name + ' · ' + (def.name || '')) : (def.name || ''),
                        w: def.w,
                        h: def.h,
                        parts: def.parts
                    });
                });
            });
            library.sort(function (left, right) {
                return String(left.name || '').localeCompare(String(right.name || ''));
            });
            return library;
        }

        function deleteTemplate(def) {
            if (!def || !api.canEdit || session.templateId) return;
            askBox({
                title: api.t('schemeTemplateDelete'),
                text: def.name || '',
                okLabel: api.t('delete'),
                danger: true,
                onOk: function () {
                    remember();
                    if (session.doc.symbols && session.doc.symbols[def.id]) delete session.doc.symbols[def.id];
                    if (!session.doc.hiddenSymbols) session.doc.hiddenSymbols = [];
                    if (session.doc.hiddenSymbols.indexOf(def.id) < 0) session.doc.hiddenSymbols.push(def.id);
                    if (session.symbolId === def.id) {
                        session.symbolId = '';
                        session.tool = 'select';
                        markTools();
                    }
                    refreshSide();
                    paint();
                }
            });
        }

        function deleteLayer(layer) {
            if (!api.canEdit || !session.doc.layers || session.doc.layers.length <= 1) return;
            askBox({
                title: api.t('schemeLayerDelete'),
                text: layer.name || '',
                okLabel: api.t('delete'),
                danger: true,
                onOk: function () {
                    remember();
                    var rest = session.doc.layers.filter(function (item) { return item.id !== layer.id; });
                    var fallback = rest[0].id;
                    (session.doc.floors || []).forEach(function (floor) {
                        (floor.objects || []).forEach(function (obj) {
                            if (obj.layerId === layer.id) obj.layerId = fallback;
                        });
                    });
                    session.doc.layers = rest;
                    if (session.doc.activeLayerId === layer.id) session.doc.activeLayerId = fallback;
                    refreshSide();
                    paint();
                }
            });
        }

        function assetById(id) {
            var list = api.assets || [];
            var i;
            for (i = 0; i < list.length; i++) {
                if (String(list[i].id) === String(id)) return list[i];
            }
            return null;
        }

        function bindingOf(asset) {
            return {
                workspaceId: api.projectKey,
                objectSchemaId: '',
                objectTypeId: '',
                objectId: String(asset.id),
                objectKey: asset.key || String(asset.id),
                objectLabel: asset.name || '',
                attributes: asset.attributes || {},
                typeName: asset.typeName || '',
                boundAt: Date.now()
            };
        }

        function placedAssetObject(id) {
            var floors = (session.doc && session.doc.floors) || [];
            var i;
            var k;
            for (i = 0; i < floors.length; i++) {
                var objects = floors[i].objects || [];
                for (k = 0; k < objects.length; k++) {
                    var binding = objects[k].assetBinding;
                    if (binding && String(binding.objectId) === String(id)) return objects[k];
                }
            }
            return null;
        }

        function assetPlaced(id) {
            return !!placedAssetObject(id);
        }

        function movePlacedAsset(id, pt) {
            var obj = placedAssetObject(id);
            if (!obj) return;
            var w = obj.type === 'asset' ? 36 : (obj.w || 36);
            var h = obj.type === 'asset' ? 36 : (obj.h || 36);
            if (obj.type === 'asset') {
                obj.w = 36;
                obj.h = 36;
                obj.fill = '';
            }
            obj.x = roundCm(pt.x - w / 2);
            obj.y = roundCm(pt.y - h / 2);
            session.selection = [obj.id];
        }

        function removePlacedAsset(id) {
            if (!mayPlacePins()) return;
            var placed = placedAssetObject(id);
            if (placed && !mutableObject(placed)) return;
            remember();
            (session.doc.floors || []).forEach(function (floor) {
                floor.objects = (floor.objects || []).filter(function (obj) {
                    return !(obj.assetBinding && String(obj.assetBinding.objectId) === String(id));
                });
            });
            session.selection = [];
            refreshSide();
            renderProps();
            paint();
        }

        function placeAsset(pt) {
            var asset = assetById(session.assetId);
            if (!asset || !mayPlacePins()) return;
            if (session.guest && !ownPin({ type: 'asset', assetBinding: bindingOf(asset) })) return;
            remember();
            if (placedAssetObject(asset.id)) movePlacedAsset(asset.id, pt);
            else {
                var obj = baseObject('asset');
                obj.text = asset.name || asset.key || '';
                obj.w = 36;
                obj.h = 36;
                obj.x = roundCm(pt.x - 18);
                obj.y = roundCm(pt.y - 18);
                obj.fill = '';
                obj.color = asset.iconColor || '#0052cc';
                obj.icon = asset.icon || '';
                obj.iconColor = asset.iconColor || '#5D6B82';
                obj.typeName = asset.typeName || '';
                obj.angle = 0;
                obj.assetBinding = bindingOf(asset);
                obj.layerId = layerOr('equipment');
                activeFloor().objects.push(obj);
                session.selection = [obj.id];
            }
            session.assetId = 0;
            session.tool = 'select';
            markTools();
            renderProps();
            refreshSide();
            paint();
        }

        function typeMark(asset) {
            if (!asset || !asset.icon) return null;
            var tile = el('span', 'asset-tree-scheme-type-icon');
            tile.style.background = asset.iconColor || '#5D6B82';
            var svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
            svg.setAttribute('viewBox', '0 0 24 24');
            svg.setAttribute('aria-hidden', 'true');
            var path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
            path.setAttribute('d', asset.icon);
            path.setAttribute('fill', '#ffffff');
            path.setAttribute('fill-rule', 'evenodd');
            svg.appendChild(path);
            tile.appendChild(svg);
            return tile;
        }

        function renderAssetList() {
            openPanel('assets');
            var head = el('div', 'asset-tree-scheme-list-head');
            head.appendChild(el('div', 'asset-tree-scheme-label', api.t('schemeAssets')));
            var only = el('button', 'asset-tree-scheme-filter' + (session.assetPlacedOnly ? ' is-on' : ''), api.t('schemeOnScheme'));
            only.type = 'button';
            only.addEventListener('click', function () {
                session.assetPlacedOnly = !session.assetPlacedOnly;
                refreshSide();
            });
            head.appendChild(only);
            bin.appendChild(head);
            var search = el('input', 'asset-tree-scheme-name');
            search.placeholder = api.t('schemeSearch');
            search.value = session.assetQuery || '';
            var list = el('div', 'asset-tree-scheme-asset-list');
            var paintAssets = function () {
                list.innerHTML = '';
                var query = String(search.value || '').toLowerCase();
                var count = 0;
                (api.assets || []).forEach(function (asset) {
                    var name = asset.name || asset.key || '';
                    var key = asset.key || '';
                    var hay = (key + ' ' + name).toLowerCase();
                    if (query && hay.indexOf(query) < 0) return;
                    if (session.assetPlacedOnly && !assetPlaced(asset.id)) return;
                    count++;
                    var placed = assetPlaced(asset.id);
                    var armed = String(session.assetId) === String(asset.id);
                    var row = el('div', 'asset-tree-scheme-asset' + (armed ? ' is-on' : ''));
                    var main = el('button', 'asset-tree-scheme-asset-main');
                    main.type = 'button';
                    main.disabled = !mayPlacePins();
                    main.title = armed ? api.t('schemeTemplateOff') : name;
                    var mark = typeMark(asset);
                    if (mark) main.appendChild(mark);
                    main.appendChild(el('strong', null, name));
                    var meta = key;
                    if (placed) meta = meta ? (meta + ' · ' + api.t('schemeOnScheme')) : api.t('schemeOnScheme');
                    if (meta) main.appendChild(el('span', null, meta));
                    main.addEventListener('click', function () {
                        if (armed) session.assetId = 0;
                        else {
                            session.assetId = asset.id;
                            session.symbolId = '';
                            session.tool = 'select';
                        }
                        markTools();
                        refreshSide();
                    });
                    row.appendChild(main);
                    if (placed) {
                        var drop = el('button', 'asset-tree-scheme-icon', '×');
                        drop.type = 'button';
                        drop.title = api.t('schemeAssetRemove');
                        drop.disabled = !mayPlacePins();
                        drop.addEventListener('click', function (event) {
                            event.stopPropagation();
                            removePlacedAsset(asset.id);
                        });
                        row.appendChild(drop);
                    }
                    list.appendChild(row);
                });
                if (!count) list.appendChild(el('p', 'asset-tree-scheme-muted', api.t('noResults')));
            };
            search.addEventListener('input', function () {
                session.assetQuery = search.value;
                paintAssets();
            });
            bin.appendChild(search);
            bin.appendChild(list);
            paintAssets();
        }

        function renameRoom(room) {
            if (!api.canEdit) return;
            askBox({
                title: api.t('name'),
                input: true,
                value: room.name,
                onOk: function (next) {
                    remember();
                    room.name = next.slice(0, 80);
                    refreshSide();
                    paint();
                }
            });
        }

        function withGroup(ids) {
            var groups = {};
            var objects = (activeFloor() && activeFloor().objects) || [];
            var next = ids.slice();
            var i;
            for (i = 0; i < objects.length; i++) {
                if (next.indexOf(objects[i].id) >= 0 && objects[i].group) groups[objects[i].group] = true;
            }
            for (i = 0; i < objects.length; i++) {
                if (objects[i].group && groups[objects[i].group] && next.indexOf(objects[i].id) < 0) next.push(objects[i].id);
            }
            return next;
        }

        function selectionIsMutable() {
            var objects = selectedObjects();
            var i;
            if (!objects.length) return false;
            for (i = 0; i < objects.length; i++) {
                if (!mutableObject(objects[i])) return false;
            }
            return true;
        }

        function restack(where) {
            if (!api.canEdit || session.mode !== 'edit' || !session.selection.length || !selectionIsMutable()) return;
            remember();
            var floor = activeFloor();
            var objects = (floor.objects || []).slice();
            var chosen = {};
            var i;
            for (i = 0; i < session.selection.length; i++) chosen[session.selection[i]] = true;
            var picked = [];
            var rest = [];
            for (i = 0; i < objects.length; i++) {
                if (chosen[objects[i].id]) picked.push(objects[i]);
                else rest.push(objects[i]);
            }
            floor.objects = where === 'back' ? picked.concat(rest) : rest.concat(picked);
            paint();
        }

        function groupSelection() {
            var objects = selectedObjects().filter(function (obj) { return obj.type && obj.type !== 'opening'; });
            if (objects.length < 2 || !api.canEdit || session.mode !== 'edit' || !selectionIsMutable()) return;
            remember();
            var id = nid('grp');
            objects.forEach(function (obj) { obj.group = id; });
            session.selection = withGroup(session.selection);
            renderProps();
            paint();
        }

        function ungroupSelection() {
            if (!api.canEdit || session.mode !== 'edit' || !selectionIsMutable()) return;
            remember();
            selectedObjects().forEach(function (obj) { obj.group = ''; });
            renderProps();
            paint();
        }

        function arrangeBar() {
            if (!session.selection.length || session.mode !== 'edit') return;
            var row = el('div', 'asset-tree-scheme-arrange');
            function act(label, run) {
                var button = el('button', 'asset-tree-scheme-mini', label);
                button.type = 'button';
                button.disabled = !api.canEdit;
                button.addEventListener('click', run);
                row.appendChild(button);
            }
            if (session.selection.length > 1) act(api.t('schemeGroup'), groupSelection);
            var grouped = false;
            selectedObjects().forEach(function (obj) { if (obj.group) grouped = true; });
            if (grouped) act(api.t('schemeUngroup'), ungroupSelection);
            if (!row.childNodes.length) return;
            props.appendChild(row);
        }

        function renderProps() {
            props.innerHTML = '';
            props.appendChild(el('div', 'asset-tree-scheme-label', api.t('schemeProps')));
            arrangeBar();
            var objects = selectedObjects();
            if (objects.length !== 1) {
                if (!objects.length) props.appendChild(el('p', 'asset-tree-scheme-muted', api.t('schemeHint')));
                return;
            }
            var obj = objects[0];
            if (!mutableObject(obj) && (session.guest || foreignPin(obj))) {
                if (obj.assetBinding || obj.type === 'asset') props.appendChild(assetCard(obj));
                else props.appendChild(el('p', 'asset-tree-scheme-muted', api.t('schemeGuestHint')));
                return;
            }
            function field(label, value, apply) {
                var wrap = el('label', 'asset-tree-scheme-field', label);
                var input = el('input');
                input.value = value == null ? '' : String(value);
                input.disabled = !api.canEdit || session.mode !== 'edit';
                input.addEventListener('change', function () {
                    if (!api.canEdit || session.mode !== 'edit') return;
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
            if (obj.type === 'opening') {
                var openingMax = obj.custom ? 2000 : 600;
                field(api.t('schemeLength'), obj.width || 90, function (value) {
                    obj.width = Math.max(20, Math.min(openingMax, roundCm(Number(value) || 90)));
                    rememberOpening(obj);
                });
                field(api.t('schemeOpeningHeight'), obj.height || (obj.kind === 'window' ? 140 : 210), function (value) {
                    obj.height = Math.max(20, Math.min(openingMax, roundCm(Number(value) || 210)));
                    rememberOpening(obj);
                });
                var orderWrap = el('label', 'asset-tree-scheme-field', api.t('schemeMadeToOrder'));
                var order = el('input');
                order.type = 'checkbox';
                order.checked = !!obj.custom;
                order.disabled = !api.canEdit || session.mode !== 'edit';
                order.addEventListener('change', function () {
                    if (!api.canEdit || session.mode !== 'edit') return;
                    remember();
                    obj.custom = order.checked;
                    rememberOpening(obj);
                    renderProps();
                    paint();
                });
                orderWrap.appendChild(order);
                props.appendChild(orderWrap);
            }
            if (obj.type === 'symbol' || obj.type === 'asset' || obj.type === 'image') field(api.t('schemeAngle'), obj.angle || 0, function (value) { obj.angle = Math.round(Number(value) || 0); });
            if (obj.type === 'image') field(api.t('schemeOpacity'), Math.round((obj.opacity == null ? 0.72 : obj.opacity) * 100), function (value) {
                var next = Number(value);
                if (!isFinite(next)) next = 72;
                obj.opacity = Math.max(5, Math.min(100, next)) / 100;
            });
            if (!obj.type && obj.points) {
                var roomColorWrap = el('label', 'asset-tree-scheme-field', api.t('schemeColor'));
                var roomColor = el('input');
                roomColor.type = 'color';
                roomColor.disabled = !api.canEdit || session.mode !== 'edit';
                roomColor.value = /^#[0-9a-fA-F]{6}$/.test(obj.color || '') ? obj.color : '#e3fcef';
                roomColor.addEventListener('change', function () {
                    if (!api.canEdit || session.mode !== 'edit') return;
                    remember();
                    obj.color = roomColor.value;
                    paint();
                });
                roomColorWrap.appendChild(roomColor);
                props.appendChild(roomColorWrap);
            }
            if (obj.type === 'marker') {
                field(api.t('schemeThickness'), obj.thickness || 18, function (value) { obj.thickness = Math.max(4, roundCm(Number(value) || 18)); });
                field(api.t('schemeOpacity'), Math.round((obj.opacity == null ? 0.45 : obj.opacity) * 100), function (value) {
                    var next = Number(value);
                    if (!isFinite(next)) next = 45;
                    obj.opacity = Math.max(10, Math.min(90, next)) / 100;
                });
            }
            if (obj.type === 'line' || obj.type === 'rect' || obj.type === 'ellipse' || obj.type === 'polyline' || obj.type === 'wall' || obj.type === 'text' || obj.type === 'dimension' || obj.type === 'marker' || obj.type === 'symbol' || obj.type === 'asset') {
                var colorWrap = el('label', 'asset-tree-scheme-field', api.t('schemeColor'));
                var color = el('input');
                color.type = 'color';
                color.disabled = !api.canEdit || session.mode !== 'edit';
                color.value = /^#[0-9a-fA-F]{6}$/.test(obj.color || '') ? obj.color : '#172b4d';
                color.addEventListener('change', function () {
                    if (!api.canEdit || session.mode !== 'edit') return;
                    remember();
                    obj.color = color.value;
                    paint();
                });
                colorWrap.appendChild(color);
                props.appendChild(colorWrap);
            }
            if (obj.type === 'rect' || obj.type === 'ellipse' || obj.type === 'polyline' || obj.type === 'wall' || obj.type === 'symbol' || obj.type === 'asset') {
                var fillWrap = el('label', 'asset-tree-scheme-field', api.t('schemeFill'));
                var fillRow = el('div', 'asset-tree-scheme-fill');
                var fillOn = el('input');
                fillOn.type = 'checkbox';
                fillOn.checked = !!obj.fill;
                fillOn.disabled = !api.canEdit || session.mode !== 'edit';
                var fillColor = el('input');
                fillColor.type = 'color';
                fillColor.disabled = !api.canEdit || session.mode !== 'edit';
                fillColor.value = /^#[0-9a-fA-F]{6}$/.test(obj.fill || '') ? obj.fill : '#deebff';
                var applyFill = function () {
                    if (!api.canEdit || session.mode !== 'edit') return;
                    remember();
                    obj.fill = fillOn.checked ? fillColor.value : '';
                    if (obj.fill && obj.type === 'polyline') obj.closed = true;
                    paint();
                };
                fillOn.addEventListener('change', applyFill);
                fillColor.addEventListener('change', function () {
                    fillOn.checked = true;
                    applyFill();
                });
                fillRow.appendChild(fillOn);
                fillRow.appendChild(fillColor);
                fillWrap.appendChild(fillRow);
                props.appendChild(fillWrap);
            }
            if (obj.type === 'rect' || obj.type === 'ellipse' || obj.type === 'polyline' || obj.type === 'wall' || obj.type === 'text') {
                field(api.t('schemeText'), obj.text || '', function (value) { obj.text = value.slice(0, 200); });
            }
            if (obj.type === 'wall' || obj.type === 'rect' || obj.type === 'ellipse' || obj.type === 'polyline') {
                var matWrap = el('label', 'asset-tree-scheme-field', api.t('schemeMaterial'));
                var mat = el('select');
                mat.disabled = !api.canEdit || session.mode !== 'edit';
                [['', 'schemeMaterialNone'], ['brick', 'schemeMatBrick'], ['concrete', 'schemeMatConcrete'], ['wood', 'schemeMatWood'], ['glass', 'schemeMatGlass'], ['metal', 'schemeMatMetal']].forEach(function (item) {
                    var option = el('option', null, api.t(item[1]));
                    option.value = item[0];
                    mat.appendChild(option);
                });
                ((session.doc && session.doc.materials) || []).forEach(function (item) {
                    var option = el('option', null, item.name || '');
                    option.value = item.id;
                    mat.appendChild(option);
                });
                mat.value = obj.material || '';
                mat.addEventListener('change', function () {
                    if (!api.canEdit || session.mode !== 'edit') return;
                    remember();
                    obj.material = mat.value;
                    if (obj.type === 'wall') obj.hatch = obj.material ? materialHatch(obj.material) : 'diagonal';
                    renderProps();
                    paint();
                });
                matWrap.appendChild(mat);
                props.appendChild(matWrap);
                var customMat = materialById(obj.material);
                if (customMat) {
                    var hatchWrap = el('label', 'asset-tree-scheme-field', api.t('schemeHatch'));
                    var hatchPick = el('select');
                    hatchPick.disabled = !api.canEdit || session.mode !== 'edit';
                    [['diagonal', 'schemeHatchDiagonal'], ['brick', 'schemeMatBrick'], ['concrete', 'schemeMatConcrete'], ['none', 'schemeMaterialNone']].forEach(function (item) {
                        var option = el('option', null, api.t(item[1]));
                        option.value = item[0];
                        hatchPick.appendChild(option);
                    });
                    hatchPick.value = customMat.hatch || 'diagonal';
                    hatchPick.addEventListener('change', function () {
                        if (!api.canEdit || session.mode !== 'edit') return;
                        remember();
                        customMat.hatch = hatchPick.value;
                        if (obj.type === 'wall') obj.hatch = hatchPick.value;
                        paint();
                    });
                    hatchWrap.appendChild(hatchPick);
                    props.appendChild(hatchWrap);
                }
                if (api.canEdit && session.mode === 'edit') {
                    var addMat = el('button', 'asset-tree-scheme-mini', api.t('schemeMaterialCustom'));
                    addMat.type = 'button';
                    addMat.addEventListener('click', function () {
                        askBox({
                            title: api.t('schemeMaterialCustom'),
                            input: true,
                            value: '',
                            onOk: function (next) {
                                var name = String(next || '').replace(/^\s+|\s+$/g, '').slice(0, 40);
                                if (!name) return;
                                remember();
                                var item = { id: nid('mat'), name: name, hatch: 'diagonal' };
                                materialsOf().push(item);
                                obj.material = item.id;
                                if (obj.type === 'wall') obj.hatch = 'diagonal';
                                renderProps();
                                paint();
                            }
                        });
                    });
                    props.appendChild(addMat);
                }
            }
            if (obj.name && obj.points && !obj.type) {
                field(api.t('name'), obj.name, function (value) { obj.name = value.slice(0, 80); });
            }
            if (obj.type === 'symbol' || obj.type === 'asset' || (obj.points && !obj.type)) {
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
                detach.disabled = !mutableObject(obj);
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
            attach.disabled = !mutableObject(obj);
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
                typeName: asset.typeName || '',
                boundAt: Date.now()
            };
            if (obj.type === 'asset') {
                obj.icon = asset.icon || '';
                obj.iconColor = asset.iconColor || '#5D6B82';
                obj.typeName = asset.typeName || '';
            }
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
                    var at = pt;
                    askBox({
                        title: api.t('schemeText'),
                        input: true,
                        value: '',
                        onOk: function (value) {
                            edit.parts.push({ t: 't', x: at.x, y: at.y, w: Math.max(20, value.length * 6), h: 12, text: value.slice(0, 80), c: '#172b4d', s: 1 });
                            editor.sel = edit.parts.length - 1;
                            paintSymbol();
                        }
                    });
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
            (doc.floors || []).forEach(function (floor) {
                (floor.objects || []).forEach(function (obj) {
                    if (obj.type !== 'asset') return;
                    obj.w = 36;
                    obj.h = 36;
                    obj.fill = '';
                });
            });
            session.doc = doc;
            (doc.libraries || []).forEach(stashLibrary);
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
                    if (row.placeId != null) session.catalog[i].placeId = row.placeId;
                    if (row.projectKey) session.catalog[i].projectKey = row.projectKey;
                    if (row.shared != null) session.catalog[i].shared = !!row.shared;
                    if (row.shares) session.catalog[i].shares = row.shares;
                    return;
                }
            }
            session.catalog.push({
                id: row.id,
                name: row.name,
                placeId: row.placeId || 0,
                projectKey: row.projectKey || '',
                shared: !!row.shared,
                shares: row.shares || []
            });
        }

        function schemeLabel(row) {
            var name = (row && row.name) || api.t('schemeDefaultName');
            if (row && row.shared && row.projectKey) name = name + ' · ' + row.projectKey;
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

        function dropScheme(id) {
            session.catalog = (session.catalog || []).filter(function (row) { return row.id !== id; });
            session.dirty = false;
            if (session.id === id) {
                var fresh = blankDoc(api.t);
                fresh.activeFloorId = fresh.floors[0].id;
                acceptDoc(fresh, 0);
            }
            session.mode = 'browse';
            if (session.repaint) session.repaint();
        }

        function removeCurrentScheme() {
            var id = session.id;
            if (!id) {
                dropScheme(0);
                return;
            }
            api.ajax('DELETE', '/projects/' + encodeURIComponent(api.projectKey) + '/schemes/' + id, null, function (code, payload) {
                if (code === 204 || code === 200) dropScheme(id);
                else status.textContent = (payload && payload.message) || api.t('schemeSaveFailed');
            });
        }

        function openScheme(id, mode) {
            session.pendingMode = mode || 'view';
            session.templateId = '';
            session.templateReturn = null;
            api.ajax('GET', '/projects/' + encodeURIComponent(api.projectKey) + '/schemes/' + id, null, function (code, payload) {
                if (code !== 200 || !payload || !payload.body) {
                    if (session.statusNode) session.statusNode.textContent = api.t('schemeSaveFailed');
                    session.mode = 'browse';
                    if (session.repaint) session.repaint();
                    return;
                }
                try {
                    acceptDoc(JSON.parse(payload.body), payload.id);
                } catch (error) {
                    if (session.statusNode) session.statusNode.textContent = api.t('schemeSaveFailed');
                    return;
                }
                session.guest = !!payload.shared;
                session.ownerKey = payload.projectKey || api.projectKey;
                session.shares = payload.shares || [];
                if (session.guest) session.tool = 'select';
                upsertCatalog({
                    id: payload.id,
                    name: session.doc.name,
                    placeId: session.doc.placeId || 0,
                    projectKey: session.ownerKey,
                    shared: session.guest,
                    shares: session.shares
                });
                session.mode = session.pendingMode || 'view';
                session.needFit = session.mode !== 'edit';
                if (session.repaint) session.repaint();
            });
        }

        function save(done) {
            if (session.templateId) return;
            if (!maySaveScheme()) return;
            session.doc.name = nameInput.value || session.doc.name;
            var path = '/projects/' + encodeURIComponent(api.projectKey) + '/schemes';
            var method = session.id ? 'PUT' : 'POST';
            if (session.id) path += '/' + session.id;
            api.ajax(method, path, { name: session.doc.name, body: JSON.stringify(session.doc) }, function (code, payload) {
                if (code >= 200 && code < 300 && payload && payload.id) {
                    session.id = payload.id;
                    session.dirty = false;
                    if (payload.projectKey) session.ownerKey = payload.projectKey;
                    if (payload.shared != null) session.guest = !!payload.shared;
                    if (payload.shares) session.shares = payload.shares;
                    upsertCatalog({
                        id: payload.id,
                        name: session.doc.name,
                        placeId: session.doc.placeId || 0,
                        projectKey: session.ownerKey || api.projectKey,
                        shared: !!session.guest,
                        shares: session.shares || []
                    });
                    fillSchemePick();
                    if (!session.guest && !session.templateId) shareBtn.style.display = '';
                    status.textContent = session.guest ? api.t('schemeGuestSaved') : api.t('schemeSaved');
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
                if (session.pendingFocus && !session.loading) {
                    var waiting = session.pendingFocus;
                    session.pendingFocus = 0;
                    openScheme(waiting, 'view');
                    return;
                }
                if (!session.loading) {
                    showMode();
                    paint();
                }
                return;
            }
            session.loading = true;
            api.ajax('GET', '/projects/' + encodeURIComponent(api.projectKey) + '/schemes', null, function (code, list) {
                session.loading = false;
                session.loaded = true;
                session.catalog = code === 200 && list && list.length ? list : [];
                var focus = session.pendingFocus;
                session.pendingFocus = 0;
                if (focus) {
                    openScheme(focus, 'view');
                    return;
                }
                session.mode = 'browse';
                if (session.repaint) session.repaint();
                else showMode();
            });
        }

        function fitScheme() {
            if (!stage.clientWidth || !stage.clientHeight || !session.doc) return false;
            var box = floorBounds(activeFloor());
            if (!box) return false;
            var width = Math.max(80, stage.clientWidth - 22);
            var height = Math.max(80, stage.clientHeight - 22);
            var pad = 48;
            var zoom = Math.min((width - pad * 2) / box.w, (height - pad * 2) / box.h);
            if (!isFinite(zoom) || zoom <= 0) zoom = 0.5;
            if (zoom > 4) zoom = 4;
            if (zoom < 0.02) zoom = 0.02;
            var viewW = width / zoom;
            var viewH = height / zoom;
            session.cam = {
                x: box.x - (viewW - box.w) / 2,
                y: box.y - (viewH - box.h) / 2,
                z: zoom
            };
            return true;
        }

        function placeName(id) {
            var places = api.places || [];
            var i;
            for (i = 0; i < places.length; i++) {
                if (String(places[i].id) === String(id)) return places[i].name || '';
            }
            return '';
        }

        function showMode() {
            if (session.mode === 'browse' && session.id && session.doc) {
                var stamp;
                for (stamp = 0; stamp < (session.catalog || []).length; stamp++) {
                    if (session.catalog[stamp].id === session.id) session.catalog[stamp].doc = clone(session.doc);
                }
            }
            var browse = session.mode === 'browse' && !session.templateId;
            var editing = session.mode === 'edit' || !!session.templateId;
            bar.style.display = browse ? 'none' : '';
            body.style.display = browse ? 'none' : '';
            body.style.gridTemplateColumns = '';
            status.style.display = browse ? 'none' : '';
            gallery.style.display = browse ? '' : 'none';
            tools.style.display = '';
            windows.style.display = editing ? '' : 'none';
            windowsBtn.style.display = editing ? '' : 'none';
            syncDocks();
            var mayEnter = !!api.canEdit || (!!session.guest && !!api.canPlace);
            editBtn.style.display = !browse && !editing && mayEnter ? '' : 'none';
            saveBtn.disabled = !mayEnter;
            backBtn.style.display = browse ? 'none' : '';
            saveBtn.style.display = editing && mayEnter ? '' : 'none';
            saveBtn.textContent = session.templateId ? api.t('schemeTemplateSave') : api.t('save');
            newBtn.style.display = !browse && !session.templateId && api.canEdit ? '' : 'none';
            deleteBtn.style.display = editing && api.canEdit && !session.templateId && !session.guest ? '' : 'none';
            shareBtn.style.display = editing && api.canEdit && !session.templateId && !session.guest && session.id ? '' : 'none';
            guestNote.style.display = session.guest && !browse && !session.templateId ? '' : 'none';
            guestNote.textContent = session.guest ? api.t('schemeGuestHint', session.ownerKey || '') : '';
            nameInput.disabled = !api.canEdit || !!session.guest;
            placePick.disabled = !!session.guest;
            unit.disabled = !!session.guest;
            if (schemePick.parentNode) schemePick.parentNode.style.display = session.templateId ? 'none' : '';
            if (nameInput.parentNode) nameInput.parentNode.style.display = editing ? '' : 'none';
            if (placePick.parentNode) placePick.parentNode.style.display = editing && !session.templateId && !session.guest ? '' : 'none';
            unit.style.display = editing ? '' : 'none';
            measureWrap.style.display = editing && !session.templateId ? '' : 'none';
            templateNote.style.display = session.templateId ? '' : 'none';
            templateNote.textContent = session.templateId ? (api.t('schemeTemplateEditing') + ': ' + (nameInput.value || session.templateName || '')) : '';
            markTools();
            if (browse) renderGallery();
        }

        function openShare() {
            if (!api.canEdit || session.guest || !session.id) return;
            var back = el('div', 'asset-tree-scheme-modal');
            var dialog = el('div', 'asset-tree-scheme-dialog asset-tree-scheme-ask');
            dialog.appendChild(el('div', 'asset-tree-scheme-label', api.t('schemeShare')));
            dialog.appendChild(el('p', 'asset-tree-scheme-ask-text', api.t('schemeShareHint')));
            var list = el('div', 'asset-tree-scheme-share-list');
            var boxes = [];
            (api.projects || []).forEach(function (project) {
                if (!project || !project.key || project.key === api.projectKey) return;
                var label = el('label', 'asset-tree-scheme-check');
                var box = el('input');
                box.type = 'checkbox';
                var on = false;
                var i;
                for (i = 0; i < (session.shares || []).length; i++) {
                    if (String(session.shares[i]).toLowerCase() === String(project.key).toLowerCase()) on = true;
                }
                box.checked = on;
                label.appendChild(box);
                label.appendChild(document.createTextNode(' ' + (project.name || project.key)));
                list.appendChild(label);
                boxes.push({ key: project.key, box: box });
            });
            if (!boxes.length) list.appendChild(el('p', 'asset-tree-scheme-muted', api.t('schemeShareEmpty')));
            var actions = el('div', 'asset-tree-scheme-head');
            var ok = el('button', 'asset-tree-scheme-save', api.t('save'));
            var cancel = el('button', 'asset-tree-scheme-new', api.t('cancel'));
            ok.type = 'button';
            cancel.type = 'button';
            function close() {
                if (back.parentNode) back.parentNode.removeChild(back);
            }
            ok.addEventListener('click', function () {
                var projects = [];
                boxes.forEach(function (item) {
                    if (item.box.checked) projects.push(item.key);
                });
                api.ajax('PUT', '/projects/' + encodeURIComponent(api.projectKey) + '/schemes/' + session.id + '/shares', { shares: projects }, function (code, payload) {
                    if (code >= 200 && code < 300) {
                        session.shares = (payload && payload.shares) || projects;
                        upsertCatalog({
                            id: session.id,
                            name: session.doc.name,
                            placeId: session.doc.placeId || 0,
                            projectKey: api.projectKey,
                            shared: false,
                            shares: session.shares
                        });
                        status.textContent = api.t('schemeShareSaved');
                        close();
                    } else status.textContent = (payload && payload.message) || api.t('schemeSaveFailed');
                });
            });
            cancel.addEventListener('click', close);
            dialog.appendChild(list);
            actions.appendChild(ok);
            actions.appendChild(cancel);
            dialog.appendChild(actions);
            back.addEventListener('mousedown', function (event) {
                if (event.target === back) close();
            });
            back.appendChild(dialog);
            host.appendChild(back);
        }

        function renderGallery() {
            gallery.innerHTML = '';
            var title = el('div', 'asset-tree-scheme-gallery-head');
            title.appendChild(el('h2', 'asset-tree-scheme-gallery-title', api.t('schemeList')));
            var create = el('button', 'asset-tree-scheme-save', api.t('schemeNew'));
            create.type = 'button';
            create.disabled = !api.canEdit;
            create.addEventListener('click', function () { startFresh(); });
            title.appendChild(create);
            gallery.appendChild(title);
            if (!session.catalog || !session.catalog.length) {
                gallery.appendChild(el('p', 'asset-tree-scheme-muted', api.t('schemeNoSchemes')));
                return;
            }
            var cards = el('div', 'asset-tree-scheme-cards');
            session.catalog.forEach(function (row) {
                var card = el('div', 'asset-tree-scheme-card');
                card.addEventListener('click', function () { openScheme(row.id, 'view'); });
                var thumb = el('canvas', 'asset-tree-scheme-thumb');
                thumb.width = 640;
                thumb.height = 320;
                if (row.doc) {
                    try { paintThumb(thumb, row.doc); } catch (error) { /* keep the empty preview */ }
                }
                card.appendChild(thumb);
                var body = el('div', 'asset-tree-scheme-card-body');
                body.appendChild(el('div', 'asset-tree-scheme-card-name', schemeLabel(row)));
                var bound = placeName(row.placeId || (row.doc && row.doc.placeId));
                if (!row.shared) {
                    body.appendChild(el('div', 'asset-tree-scheme-muted', bound ? (api.t('schemePlace') + ': ' + bound) : api.t('schemePlaceNone')));
                }
                if (row.shared && row.projectKey) {
                    body.appendChild(el('div', 'asset-tree-scheme-muted', api.t('schemeSharedFrom', row.projectKey)));
                } else if (row.shares && row.shares.length) {
                    body.appendChild(el('div', 'asset-tree-scheme-muted', api.t('schemeShareWith', row.shares.join(', '))));
                }
                var actions = el('div', 'asset-tree-scheme-card-actions');
                var open = el('button', 'asset-tree-scheme-new', api.t('schemeOpen'));
                open.type = 'button';
                open.addEventListener('click', function (event) {
                    event.stopPropagation();
                    openScheme(row.id, 'view');
                });
                var edit = el('button', 'asset-tree-scheme-save', api.t('schemeEdit'));
                edit.type = 'button';
                edit.disabled = row.shared ? !api.canPlace : !api.canEdit;
                edit.addEventListener('click', function (event) {
                    event.stopPropagation();
                    openScheme(row.id, 'edit');
                });
                actions.appendChild(open);
                actions.appendChild(edit);
                body.appendChild(actions);
                card.appendChild(body);
                cards.appendChild(card);
            });
            gallery.appendChild(cards);
            loadThumbs();
        }

        function floorBounds(floor) {
            var box = null;
            function add(one) {
                if (!one || one.w == null) return;
                if (!box) box = { x: one.x, y: one.y, r: one.x + one.w, b: one.y + one.h };
                else {
                    box.x = Math.min(box.x, one.x);
                    box.y = Math.min(box.y, one.y);
                    box.r = Math.max(box.r, one.x + one.w);
                    box.b = Math.max(box.b, one.y + one.h);
                }
            }
            ((floor && floor.rooms) || []).forEach(function (room) { add(bounds(room)); });
            ((floor && floor.objects) || []).forEach(function (obj) {
                if (obj.type === 'opening') return;
                add(bounds(obj));
            });
            if (!box) return null;
            return { x: box.x, y: box.y, w: Math.max(1, box.r - box.x), h: Math.max(1, box.b - box.y) };
        }

        function paintThumb(canvas, doc) {
            var ctx = canvas.getContext('2d');
            var width = canvas.width;
            var height = canvas.height;
            ctx.setTransform(1, 0, 0, 1, 0, 0);
            ctx.clearRect(0, 0, width, height);
            ctx.fillStyle = '#f7f8f9';
            ctx.fillRect(0, 0, width, height);
            if (!doc || !doc.floors || !doc.floors.length) return;
            var savedDoc = session.doc;
            var savedCam = session.cam;
            session.doc = doc;
            if (!session.doc.settings) session.doc.settings = { showOtherFloors: false, unit: 'cm', showMeasures: false };
            var previousMeasures = session.doc.settings.showMeasures;
            var previousOther = session.doc.settings.showOtherFloors;
            session.doc.settings.showMeasures = false;
            session.doc.settings.showOtherFloors = false;
            try {
                var floor = activeFloor();
                var box = floorBounds(floor);
                if (box) {
                    var pad = 28;
                    var zoom = Math.min((width - pad * 2) / box.w, (height - pad * 2) / box.h);
                    if (!isFinite(zoom) || zoom <= 0) zoom = 0.2;
                    session.cam = {
                        x: box.x - ((width / zoom) - box.w) / 2,
                        y: box.y - ((height / zoom) - box.h) / 2,
                        z: zoom
                    };
                    ctx.save();
                    ctx.beginPath();
                    ctx.rect(0, 0, width, height);
                    ctx.clip();
                    ctx.scale(zoom, zoom);
                    ctx.translate(-session.cam.x, -session.cam.y);
                    drawFloor(ctx, floor, false);
                    ctx.restore();
                }
            } finally {
                if (session.doc && session.doc.settings) {
                    session.doc.settings.showMeasures = previousMeasures;
                    session.doc.settings.showOtherFloors = previousOther;
                }
                session.doc = savedDoc;
                session.cam = savedCam;
            }
        }

        function loadThumbs() {
            (session.catalog || []).forEach(function (row) {
                if (row.doc || row.thumbLoading || row.thumbFailed) return;
                row.thumbLoading = true;
                api.ajax('GET', '/projects/' + encodeURIComponent(api.projectKey) + '/schemes/' + row.id, null, function (code, payload) {
                    row.thumbLoading = false;
                    if (code === 200 && payload && payload.body) {
                        try { row.doc = JSON.parse(payload.body); } catch (error) { row.thumbFailed = true; }
                        if (row.doc && row.doc.placeId) row.placeId = row.doc.placeId;
                    } else row.thumbFailed = true;
                    if (session.mode === 'browse' && session.repaint) session.repaint();
                });
            });
        }

        function startFresh() {
            if (!api.canEdit) return;
            var fresh = blankDoc(api.t);
            fresh.name = api.t('schemeDefaultName') + ' ' + ((session.catalog || []).length + 1);
            fresh.activeFloorId = fresh.floors[0].id;
            session.templateId = '';
            session.templateReturn = null;
            acceptDoc(fresh, 0);
            session.guest = false;
            session.ownerKey = api.projectKey;
            session.shares = [];
            session.dirty = true;
            session.mode = 'edit';
            if (session.repaint) session.repaint();
        }

        function objectsFromParts(def) {
            var objects = [];
            (def.parts || []).forEach(function (part) {
                var obj = null;
                if (part.t === 'r' || part.t === 'e') {
                    obj = baseObject(part.t === 'e' ? 'ellipse' : 'rect');
                    obj.x = part.x;
                    obj.y = part.y;
                    obj.w = part.w;
                    obj.h = part.h;
                    obj.color = part.c || '#172b4d';
                    obj.fill = part.f || '';
                    obj.thickness = part.s || 1;
                    if (part.text) obj.text = part.text;
                    if (part.material) obj.material = part.material;
                } else if (part.t === 'l') {
                    obj = baseObject('line');
                    obj.x1 = part.x1;
                    obj.y1 = part.y1;
                    obj.x2 = part.x2;
                    obj.y2 = part.y2;
                    obj.color = part.c || '#172b4d';
                    obj.thickness = part.s || 1;
                } else if (part.t === 'p' && part.points) {
                    obj = baseObject('polyline');
                    obj.points = part.points.map(function (p) {
                        var next = { x: p.x, y: p.y };
                        if (p.outx != null) { next.outx = p.outx; next.outy = p.outy; }
                        if (p.inx != null) { next.inx = p.inx; next.iny = p.iny; }
                        return next;
                    });
                    var first = obj.points[0];
                    var end = obj.points[obj.points.length - 1];
                    obj.closed = obj.points.length >= 4 && dist(first.x, first.y, end.x, end.y) < 0.5;
                    if (obj.closed) obj.points.pop();
                    obj.color = part.c || '#172b4d';
                    obj.thickness = part.s || 1;
                    if (part.text) obj.text = part.text;
                } else if (part.t === 't') {
                    obj = baseObject('text');
                    obj.x = part.x;
                    obj.y = part.y;
                    obj.w = part.w || 40;
                    obj.h = part.h || 18;
                    obj.text = part.text || '';
                    obj.color = part.c || '#172b4d';
                }
                if (obj) objects.push(obj);
            });
            return objects;
        }

        function editTemplate(def) {
            if (!def || !api.canEdit || session.templateId) return;
            session.templateReturn = {
                id: session.id,
                doc: clone(session.doc),
                mode: session.mode || 'view',
                dirty: !!session.dirty
            };
            var doc = blankDoc(api.t);
            doc.name = def.name || api.t('schemeTemplateNew');
            doc.activeFloorId = doc.floors[0].id;
            doc.symbols = clone((session.doc && session.doc.symbols) || {});
            session.templateId = def.id;
            session.templateName = def.name || '';
            acceptDoc(doc, 0);
            activeFloor().objects = objectsFromParts(def);
            session.dirty = false;
            session.mode = 'edit';
            if (session.repaint) session.repaint();
        }

        function templateDefFromCanvas() {
            var objects = (activeFloor().objects || []).filter(function (obj) { return obj.type && obj.type !== 'opening'; });
            var box = null;
            var pad = 1;
            var i;
            for (i = 0; i < objects.length; i++) {
                var one = bounds(objects[i]);
                if (!one) continue;
                if (!box) box = { x: one.x, y: one.y, r: one.x + one.w, b: one.y + one.h };
                else {
                    box.x = Math.min(box.x, one.x);
                    box.y = Math.min(box.y, one.y);
                    box.r = Math.max(box.r, one.x + one.w);
                    box.b = Math.max(box.b, one.y + one.h);
                }
            }
            if (!box) box = { x: 0, y: 0, r: 40, b: 40 };
            box.x -= pad;
            box.y -= pad;
            var parts = [];
            objects.forEach(function (obj) {
                partsOf(obj).forEach(function (part) { parts.push(shiftPart(part, box.x, box.y)); });
            });
            return {
                id: session.templateId,
                name: (nameInput.value || session.templateName || api.t('schemeTemplateNew')).slice(0, 80),
                w: Math.max(5, roundCm(box.r - box.x + pad)),
                h: Math.max(5, roundCm(box.b - box.y + pad)),
                parts: parts
            };
        }

        function saveTemplate() {
            if (!session.templateId || !session.templateReturn) return;
            var def = templateDefFromCanvas();
            var back = session.templateReturn;
            session.templateId = '';
            session.templateName = '';
            session.templateReturn = null;
            acceptDoc(back.doc, back.id);
            if (!session.doc.symbols) session.doc.symbols = {};
            session.doc.symbols[def.id] = def;
            session.symbolId = def.id;
            session.dirty = true;
            session.mode = back.mode && back.mode !== 'browse' ? back.mode : 'edit';
            if (session.repaint) session.repaint();
            status.textContent = api.t('schemeTemplateSaved');
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
            if (session.mode !== 'edit') {
                var viewId = hit(pt.x, pt.y);
                session.selection = viewId ? withGroup([viewId]) : [];
                if (viewId) showSchemeInfo(event.clientX, event.clientY, viewId);
                else hideSchemeInfo();
                renderProps();
                refreshSide();
                paint();
                return;
            }
            hideSchemeInfo();
            if (session.tool === 'select') {
                var handle = handleAt(sx, sy);
                if (handle && api.canEdit && !session.guest) {
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
                    } else if (session.selection.indexOf(id) < 0) session.selection = withGroup([id]);
                    else session.selection = withGroup(session.selection);
                    if (selectedObjects().some(function (obj) { return mutableObject(obj); })) {
                        remember();
                        drag = { mode: 'move', x: pt.x, y: pt.y, lastX: pt.x, lastY: pt.y, moved: false };
                    }
                } else if (session.assetId && mayPlacePins()) {
                    placeAsset(pt);
                    return;
                } else {
                    var scan = activeFloor().underlay;
                    if (session.calibrate && scan && !session.guest) {
                        session.calibrate.points.push({ x: pt.x, y: pt.y });
                        if (session.calibrate.points.length >= 2) {
                            var marks = session.calibrate.points.slice(0, 2);
                            session.calibrate = null;
                            askBox({
                                title: api.t('mapScanLength'),
                                input: true,
                                value: '',
                                onOk: function (value) {
                                    var cm = Number(String(value).replace(',', '.'));
                                    var dx = marks[1].x - marks[0].x;
                                    var dy = marks[1].y - marks[0].y;
                                    var span = Math.sqrt(dx * dx + dy * dy);
                                    var current = activeFloor().underlay;
                                    if (!current || !(cm > 0) || span < 1) return;
                                    var scale = cm / span;
                                    current.w = Math.max(10, roundCm(current.w * scale));
                                    current.h = Math.max(10, roundCm(current.h * scale));
                                    session.dirty = true;
                                    paint();
                                }
                            });
                        }
                        paint();
                        return;
                    }
                    if (scan && pt.x >= scan.x && pt.y >= scan.y && pt.x <= scan.x + scan.w && pt.y <= scan.y + scan.h && api.canEdit && !session.guest) {
                        drag = { mode: 'underlay', x: pt.x, y: pt.y, ox: scan.x, oy: scan.y, moved: false };
                        return;
                    }
                    if (!event.shiftKey) session.selection = [];
                    drag = { mode: 'marquee', x: pt.x, y: pt.y };
                    session.marquee = { x: pt.x, y: pt.y, w: 0, h: 0 };
                }
                renderProps();
                refreshSide();
                paint();
                return;
            }
            if (!api.canEdit || session.guest) return;
            if (session.tool === 'text') {
                var at = pt;
                askBox({
                    title: api.t('schemeText'),
                    input: true,
                    value: '',
                    onOk: function (value) {
                        remember();
                        var text = baseObject('text');
                        text.x = at.x;
                        text.y = at.y;
                        text.h = 18;
                        text.w = Math.max(40, value.length * 8);
                        text.text = value.slice(0, 200);
                        text.layerId = layerOr('notes');
                        activeFloor().objects.push(text);
                        session.selection = [text.id];
                        renderProps();
                        paint();
                    }
                });
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
            if (session.tool === 'dropper') {
                var picked = sampleInk(sx, sy, pt);
                if (picked) chooseInk(picked, true);
                return;
            }
            if (session.tool === 'marker') {
                var raw = worldFrom(sx, sy);
                session.draft = { tool: 'marker', points: [{ x: raw.x, y: raw.y }] };
                drag = { mode: 'marker', moved: false };
                paint();
                return;
            }
            if (session.tool === 'polyline') {
                if (!session.draft) {
                    session.draft = { tool: 'polyline', points: [{ x: pt.x, y: pt.y }] };
                    drag = { mode: 'pen', index: 0, ox: pt.x, oy: pt.y };
                } else if (session.draft.points && session.draft.points.length) {
                    var firstPoint = session.draft.points[0];
                    var nearStart = dist(pt.x, pt.y, firstPoint.x, firstPoint.y) <= 12 / session.cam.z;
                    if (nearStart && session.draft.points.length >= 4) finishPoly(true);
                    else {
                        var lastPoint = session.draft.points[session.draft.points.length - 1];
                        lastPoint.x = pt.x;
                        lastPoint.y = pt.y;
                        delete lastPoint.outx;
                        delete lastPoint.outy;
                        delete lastPoint.inx;
                        delete lastPoint.iny;
                        drag = { mode: 'pen', index: session.draft.points.length - 1, ox: pt.x, oy: pt.y };
                    }
                }
                paint();
                return;
            }
            if (session.tool === 'wall') {
                if (session.wallDoneAt && (Date.now() - session.wallDoneAt) < 280
                    && Math.abs(event.clientX - session.wallDoneX) < 4
                    && Math.abs(event.clientY - session.wallDoneY) < 4) {
                    return;
                }
                if (!session.draft) {
                    session.draft = { tool: 'wall', phase: 1, x1: pt.x, y1: pt.y, x2: pt.x, y2: pt.y };
                } else {
                    session.wallDoneX = event.clientX;
                    session.wallDoneY = event.clientY;
                    commitWall(session.draft.x1, session.draft.y1, pt.x, pt.y);
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
            if (drag.mode === 'marker' && session.draft && session.draft.points) {
                var mark = worldFrom(event.clientX - overC.getBoundingClientRect().left, event.clientY - overC.getBoundingClientRect().top);
                var lastMark = session.draft.points[session.draft.points.length - 1];
                if (dist(lastMark.x, lastMark.y, mark.x, mark.y) >= 1.5) {
                    session.draft.points.push({ x: mark.x, y: mark.y });
                    drag.moved = true;
                }
                paint();
                return;
            }
            if (drag.mode === 'pen' && session.draft && session.draft.points && session.draft.points[drag.index]) {
                var anchor = session.draft.points[drag.index];
                var pullX = pt.x - drag.ox;
                var pullY = pt.y - drag.oy;
                if (Math.abs(pullX) + Math.abs(pullY) > 3 / session.cam.z) {
                    anchor.outx = pt.x;
                    anchor.outy = pt.y;
                    anchor.inx = drag.ox - pullX;
                    anchor.iny = drag.oy - pullY;
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
            if (drag.mode === 'underlay') {
                var scan = activeFloor().underlay;
                if (scan) {
                    scan.x = roundCm(drag.ox + pt.x - drag.x);
                    scan.y = roundCm(drag.oy + pt.y - drag.y);
                    drag.moved = true;
                    session.dirty = true;
                }
                paint();
                return;
            }
            if (drag.mode === 'move') {
                var dx = pt.x - drag.lastX;
                var dy = pt.y - drag.lastY;
                drag.lastX = pt.x;
                drag.lastY = pt.y;
                drag.moved = true;
                selectedObjects().forEach(function (obj) {
                    if (mutableObject(obj)) translateObj(obj, dx, dy);
                });
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
                if (drag.handle.index === -1 && target && target.w != null) {
                    var centerX = target.x + target.w / 2;
                    var centerY = target.y + (target.h || 0) / 2;
                    target.angle = Math.round(Math.atan2(pt.y - centerY, pt.x - centerX) * 180 / Math.PI + 90);
                    paint();
                    return;
                }
                if (target && target.points && target.points[drag.handle.index]) {
                    var point = target.points[drag.handle.index];
                    var nextX = roundCm(pt.x);
                    var nextY = roundCm(pt.y);
                    var shiftX = nextX - point.x;
                    var shiftY = nextY - point.y;
                    point.x = nextX;
                    point.y = nextY;
                    if (point.outx != null) { point.outx = roundCm(point.outx + shiftX); point.outy = roundCm(point.outy + shiftY); }
                    if (point.inx != null) { point.inx = roundCm(point.inx + shiftX); point.iny = roundCm(point.iny + shiftY); }
                    if (target.type === 'wall') syncRooms(activeFloor());
                } else if (target && target.x1 != null) {
                    if (drag.handle.index === 0) { target.x1 = roundCm(pt.x); target.y1 = roundCm(pt.y); }
                    else { target.x2 = roundCm(pt.x); target.y2 = roundCm(pt.y); }
                } else if (target && target.w != null) {
                    var local = unrotate(target, pt.x, pt.y);
                    var x = target.x;
                    var y = target.y;
                    var r = target.x + target.w;
                    var b = target.y + target.h;
                    if (drag.handle.index === 0) { x = local.x; y = local.y; }
                    if (drag.handle.index === 1) { r = local.x; y = local.y; }
                    if (drag.handle.index === 2) { r = local.x; b = local.y; }
                    if (drag.handle.index === 3) { x = local.x; b = local.y; }
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
            if (drag.mode === 'marker') {
                var stroke = session.draft && session.draft.points;
                var painted = drag.moved && stroke && stroke.length >= 2;
                var spot = stroke && stroke.length ? stroke[0] : null;
                session.draft = null;
                drag = null;
                if (painted) {
                    remember();
                    var markObj = baseObject('marker');
                    markObj.points = stroke.map(function (p) { return { x: roundCm(p.x), y: roundCm(p.y) }; });
                    markObj.color = session.ink || '#ffe380';
                    markObj.thickness = Math.max(8, roundCm(22 / (session.cam.z || 1)));
                    markObj.opacity = 0.45;
                    markObj.layerId = layerOr('notes');
                    activeFloor().objects.push(markObj);
                    session.selection = [markObj.id];
                    renderProps();
                } else if (spot) {
                    var markId = hit(spot.x, spot.y);
                    if (markId) {
                        session.selection = withGroup([markId]);
                        paintSelection(session.ink || '#ffe380');
                    }
                }
                paint();
                return;
            }
            if (drag.mode === 'pen') {
                var pts = session.draft && session.draft.points;
                if (pts && pts[drag.index]) {
                    var anchor = pts[drag.index];
                    if (anchor.outx == null || dist(anchor.x, anchor.y, anchor.outx, anchor.outy) < 2) {
                        delete anchor.outx;
                        delete anchor.outy;
                        delete anchor.inx;
                        delete anchor.iny;
                    }
                    pts.push({ x: anchor.x, y: anchor.y });
                }
                drag = null;
                paint();
                return;
            }
            if (drag.mode === 'rmb') {
                var pt = pointer(event);
                var id = hit(pt.x, pt.y);
                if (id && session.selection.indexOf(id) < 0) session.selection = withGroup([id]);
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
                session.selection = withGroup(ids);
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
                return [{ t: obj.type === 'ellipse' ? 'e' : 'r', x: obj.x, y: obj.y, w: obj.w, h: obj.h, c: obj.color || '#172b4d', f: obj.fill || '', s: obj.thickness || 1, text: obj.text || '', material: obj.material || '' }];
            }
            if (obj.type === 'text') {
                return [{ t: 't', x: obj.x, y: obj.y, w: obj.w || 40, h: obj.h || 18, text: obj.text || '', c: obj.color || '#172b4d', s: 1 }];
            }
            if ((obj.type === 'polyline' || obj.type === 'wall') && obj.points && obj.points.length) {
                var points = obj.points.map(function (p) {
                    var next = { x: p.x, y: p.y };
                    if (p.outx != null) { next.outx = p.outx; next.outy = p.outy; }
                    if (p.inx != null) { next.inx = p.inx; next.iny = p.iny; }
                    return next;
                });
                if (obj.closed && points.length > 2) {
                    var endPoint = { x: points[0].x, y: points[0].y };
                    if (points[0].inx != null) { endPoint.inx = points[0].inx; endPoint.iny = points[0].iny; }
                    points.push(endPoint);
                }
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
            if (next.points) next.points = next.points.map(function (p) {
                var moved = { x: roundCm(p.x - ox), y: roundCm(p.y - oy) };
                if (p.outx != null) { moved.outx = roundCm(p.outx - ox); moved.outy = roundCm(p.outy - oy); }
                if (p.inx != null) { moved.inx = roundCm(p.inx - ox); moved.iny = roundCm(p.iny - oy); }
                return moved;
            });
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

        function placeMenu(menu, clientX, clientY) {
            host.appendChild(menu);
            var hostRect = host.getBoundingClientRect();
            var left = clientX - hostRect.left;
            var top = clientY - hostRect.top;
            if (left + 220 > host.clientWidth) left = Math.max(8, host.clientWidth - 220);
            if (top + menu.offsetHeight + 8 > host.clientHeight) top = Math.max(8, host.clientHeight - menu.offsetHeight - 8);
            menu.style.left = left + 'px';
            menu.style.top = top + 'px';
        }

        function menuAction(menu, label, run, disabled) {
            var button = el('button', 'asset-tree-scheme-menu-item', label);
            button.type = 'button';
            button.disabled = !!disabled;
            button.addEventListener('click', function (event) {
                event.preventDefault();
                event.stopPropagation();
                hideTemplateMenu();
                if (!button.disabled) run();
            });
            menu.appendChild(button);
        }

        function showLibraryMenu(clientX, clientY, symbol) {
            hideTemplateMenu();
            if (!api.canEdit || session.templateId) return;
            var menu = el('div', 'asset-tree-scheme-menu');
            menu.addEventListener('mousedown', function (event) { event.stopPropagation(); });
            menuAction(menu, api.t('schemeTemplateEdit'), function () { editTemplate(symbol); });
            menuAction(menu, api.t('schemeTemplateDelete'), function () { deleteTemplate(symbol); });
            placeMenu(menu, clientX, clientY);
        }

        function rememberOpening(obj) {
            if (!obj || obj.type !== 'opening') return;
            if (!session.openingSize) session.openingSize = {};
            session.openingSize[obj.kind || 'door'] = {
                width: obj.width,
                height: obj.height,
                custom: !!obj.custom
            };
        }

        function showTemplateMenu(clientX, clientY) {
            hideTemplateMenu();
            if (!api.canEdit || session.mode !== 'edit' || session.templateId) return;
            var menu = el('div', 'asset-tree-scheme-menu');
            menu.addEventListener('mousedown', function (event) { event.stopPropagation(); });
            menuAction(menu, api.t('schemeUndo'), undo, !session.past.length);
            menuAction(menu, api.t('schemeRedo'), redo, !session.future.length);
            if (session.selection.length) {
                menuAction(menu, api.t('schemeToFront'), function () { restack('front'); });
                menuAction(menu, api.t('schemeToBack'), function () { restack('back'); });
            }
            var objects = selectedObjects().filter(function (obj) { return obj.type && obj.type !== 'opening'; });
            var field = null;
            if (objects.length) {
                var suggested = objects.length === 1 && objects[0].type === 'symbol' ? (symbolDef(objects[0].symbolId).name || '') : api.t('schemeTemplateNew');
                field = el('input', 'asset-tree-scheme-name');
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
                menu.appendChild(field);
                menu.appendChild(item);
            }
            placeMenu(menu, clientX, clientY);
            if (field) {
                field.focus();
                field.select();
            }
        }

        function findDrawn(id) {
            var floor = activeFloor();
            if (!floor || !id) return null;
            var i;
            if (String(id).indexOf('room:') === 0) {
                var wanted = String(id).slice(5);
                var rooms = floor.rooms || [];
                for (i = 0; i < rooms.length; i++) {
                    if (String(rooms[i].id) === wanted) return rooms[i];
                }
                return null;
            }
            var objects = floor.objects || [];
            for (i = 0; i < objects.length; i++) {
                if (objects[i].id === id) return objects[i];
            }
            return null;
        }

        function cmText(value) {
            return String(Math.round(Number(value) || 0)) + ' cm';
        }

        function chainLength(points, closed) {
            if (!points || points.length < 2) return 0;
            var total = 0;
            var i;
            for (i = 1; i < points.length; i++) total += dist(points[i - 1].x, points[i - 1].y, points[i].x, points[i].y);
            if (closed && points.length > 2) total += dist(points[points.length - 1].x, points[points.length - 1].y, points[0].x, points[0].y);
            return Math.round(total);
        }

        function infoTitle(obj) {
            if (!obj.type && obj.name) return obj.name;
            if (obj.type === 'opening') return api.t(obj.kind === 'window' ? 'schemeToolWindow' : 'schemeToolDoor');
            if (obj.type === 'symbol') {
                var def = symbolDef(obj.symbolId);
                return (def && def.name) || api.t('schemeSymbols');
            }
            if (obj.type === 'asset') return obj.text || (obj.assetBinding && obj.assetBinding.objectLabel) || api.t('schemeAssets');
            var names = {
                wall: 'schemeToolWall',
                line: 'schemeToolLine',
                rect: 'schemeToolRect',
                ellipse: 'schemeToolEllipse',
                polyline: 'schemeToolPen',
                text: 'schemeToolText',
                dimension: 'schemeToolDim',
                image: 'schemeImage'
            };
            return api.t(names[obj.type] || 'schemeProps');
        }

        function infoRows(obj) {
            var rows = [];
            var binding = obj.assetBinding;
            var live = binding ? assetById(binding.objectId) : null;
            if (obj.type === 'wall' || obj.type === 'polyline' || obj.type === 'line' || obj.type === 'dimension') {
                rows.push([api.t('schemeLength'), cmText(chainLength(obj.points, obj.closed))]);
            }
            if (obj.type === 'wall') rows.push([api.t('schemeThickness'), cmText(obj.thickness || 15)]);
            if (obj.w != null && obj.type !== 'text' && obj.type !== 'asset') rows.push(['W', cmText(obj.w)]);
            if (obj.h != null && obj.type !== 'text' && obj.type !== 'asset' && obj.type !== 'opening') rows.push(['H', cmText(obj.h)]);
            if (obj.type === 'opening') {
                rows.push([api.t('schemeLength'), cmText(obj.width || 90)]);
                rows.push([api.t('schemeOpeningHeight'), cmText(obj.height || (obj.kind === 'window' ? 140 : 210))]);
                if (obj.custom) rows.push([api.t('schemeMadeToOrder'), '✓']);
            }
            if (obj.material) rows.push([api.t('schemeMaterial'), materialName(obj.material)]);
            if (obj.type === 'symbol' || obj.type === 'asset') rows.push([api.t('schemeAngle'), String(obj.angle || 0) + '°']);
            if (obj.text && obj.type !== 'asset') rows.push([api.t('schemeText'), obj.text]);
            if (!obj.type && obj.area) rows.push([api.t('schemeRoom'), ((obj.area || 0) / 10000).toFixed(2) + ' m2']);
            if (binding || live) {
                var label = (live && live.name) || (binding && binding.objectLabel) || '';
                var key = (live && live.key) || (binding && binding.objectKey) || '';
                var typeName = (live && live.typeName) || (binding && binding.typeName) || obj.typeName || '';
                if (label) rows.push([api.t('name'), label]);
                if (key) rows.push([api.t('schemeAssetTitle'), key]);
                if (typeName) rows.push([api.t('type'), typeName]);
                var attrs = (binding && binding.attributes) || (live && live.attributes) || {};
                var attr;
                for (attr in attrs) {
                    if (attrs[attr] !== '') rows.push([attr, String(attrs[attr])]);
                }
            }
            return rows;
        }

        function hideSchemeInfo() {
            var old = host.querySelector('.asset-tree-scheme-info');
            if (old && old.parentNode) old.parentNode.removeChild(old);
        }

        function showSchemeInfo(clientX, clientY, id) {
            hideSchemeInfo();
            var obj = findDrawn(id);
            if (!obj) return;
            var card = el('div', 'asset-tree-scheme-info');
            card.appendChild(el('strong', 'asset-tree-scheme-info-title', infoTitle(obj)));
            infoRows(obj).forEach(function (row) {
                var line = el('div', 'asset-tree-scheme-info-row');
                line.appendChild(el('span', null, row[0]));
                line.appendChild(el('b', null, row[1]));
                card.appendChild(line);
            });
            card.addEventListener('mousedown', function (event) { event.stopPropagation(); });
            placeMenu(card, clientX, clientY);
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

        stage.addEventListener('dblclick', function (event) {
            if (session.draft && session.draft.points && session.draft.points.length >= 3) {
                finishPoly(false);
                return;
            }
            if (session.mode !== 'edit' || !api.canEdit || session.guest) return;
            var pt = pointer(event);
            var id = hit(pt.x, pt.y);
            if (!id) return;
            var objects = (activeFloor().objects || []);
            var i;
            var found = null;
            for (i = 0; i < objects.length; i++) if (objects[i].id === id) found = objects[i];
            if (!found) return;
            if (found.type !== 'text' && found.type !== 'rect' && found.type !== 'ellipse' && found.type !== 'polyline' && found.type !== 'wall') return;
            askBox({
                title: api.t('schemeText'),
                input: true,
                value: found.text || '',
                onOk: function (value) {
                    remember();
                    found.text = value.slice(0, 200);
                    session.selection = [found.id];
                    renderProps();
                    paint();
                }
            });
        });

        stage.addEventListener('contextmenu', function (event) { event.preventDefault(); });
        stage.addEventListener('dragover', function (event) {
            if (session.mode !== 'edit' || session.guest) return;
            var file = event.dataTransfer && event.dataTransfer.files && event.dataTransfer.files[0];
            if (!file || String(file.type || '').indexOf('image/') !== 0) return;
            event.preventDefault();
        });
        stage.addEventListener('drop', function (event) {
            if (session.mode !== 'edit' || session.guest) return;
            var file = event.dataTransfer && event.dataTransfer.files && event.dataTransfer.files[0];
            if (!file || String(file.type || '').indexOf('image/') !== 0) return;
            event.preventDefault();
            readSchemeImage(file);
        });

        function onKey(event) {
            if (!host.parentNode) {
                document.removeEventListener('keydown', onKey);
                return;
            }
            if (host.querySelector('.asset-tree-scheme-modal')) return;
            if (session.mode !== 'edit') {
                if (event.key === 'Escape') {
                    session.selection = [];
                    renderProps();
                    paint();
                }
                return;
            }
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
                session.symbolId = '';
                session.assetId = 0;
                if (session.tool === 'symbol') session.tool = 'select';
                markTools();
                refreshSide();
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
            var hot = { v: 'select', l: 'line', r: 'rect', e: 'ellipse', p: 'polyline', w: 'wall', t: 'text', d: 'dimension', m: 'marker', i: 'dropper' };
            if (!event.ctrlKey && !event.metaKey && !event.altKey && hot[event.key]) {
                if (session.guest && hot[event.key] !== 'select') return;
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
        measureBox.addEventListener('change', function () {
            if (!session.doc.settings) session.doc.settings = { showOtherFloors: true, unit: unit.value || 'cm' };
            session.doc.settings.showMeasures = measureBox.checked;
            session.dirty = true;
            paint();
        });
        placePick.addEventListener('change', function () {
            session.doc.placeId = Number(placePick.value) || 0;
            session.dirty = true;
            if (session.id) upsertCatalog({ id: session.id, name: session.doc.name, placeId: session.doc.placeId });
        });
        schemePick.addEventListener('change', function () {
            if (session.templateId) return;
            var next = Number(schemePick.value);
            if (!next || next === session.id) return;
            var target = next;
            var nextMode = session.mode === 'edit' ? 'edit' : 'view';
            if (session.dirty && maySaveScheme()) save(function () { openScheme(target, nextMode); });
            else openScheme(target, nextMode);
        });
        saveBtn.addEventListener('click', function () {
            if (session.templateId) saveTemplate();
            else save();
        });
        shareBtn.addEventListener('click', openShare);
        editBtn.addEventListener('click', function () {
            if (session.templateId) return;
            if (!maySaveScheme()) return;
            session.mode = 'edit';
            showMode();
            renderProps();
            paint();
        });
        backBtn.addEventListener('click', function () {
            if (session.templateId) {
                saveTemplate();
                return;
            }
            var leave = function () {
                session.mode = 'browse';
                showMode();
            };
            if (session.dirty && maySaveScheme() && session.id) save(leave);
            else if (session.dirty && maySaveScheme() && !session.id) save(leave);
            else leave();
        });
        deleteBtn.addEventListener('click', function () {
            if (!api.canEdit) return;
            askBox({
                title: api.t('schemeDelete'),
                text: session.doc.name || api.t('schemeDefaultName'),
                okLabel: api.t('delete'),
                danger: true,
                onOk: removeCurrentScheme
            });
        });
        newBtn.addEventListener('click', function () {
            if (session.templateId) return;
            if (session.dirty && api.canEdit) save(startFresh);
            else startFresh();
        });

        var timer = global.setInterval(function () {
            if (!host.parentNode) {
                global.clearInterval(timer);
                return;
            }
            if (session.templateId || session.mode === 'browse') return;
            if (session.dirty && session.loaded && maySaveScheme()) save();
        }, 30000);
        global.addEventListener('resize', paint);
        session.repaint = function () {
            if (!host.parentNode) return;
            nameInput.value = session.doc.name || '';
            if (unit.value !== (session.doc.settings.unit || 'cm')) unit.value = session.doc.settings.unit || 'cm';
            measureBox.checked = measuresOn();
            fillSchemePick();
            fillPlacePick();
            markTools();
            refreshSide();
            renderProps();
            showMode();
            paint();
            if (session.needFit) {
                session.needFit = false;
                global.setTimeout(function () {
                    if (fitScheme()) paint();
                }, 40);
            }
        };
        session.statusNode = status;
        markTools();
        fillSchemePick();
        fillPlacePick();
        refreshSide();
        renderProps();
        showMode();
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
