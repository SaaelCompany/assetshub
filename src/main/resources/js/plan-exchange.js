/* Floor-plan exchange: DXF, a line-level DWG, and vector PDF. ES5. */
var AssetTreePlan = (function () {
    function bytesOf(text) {
        var list = [];
        var i;
        for (i = 0; i < text.length; i++) list.push(text.charCodeAt(i) & 255);
        return list;
    }

    function textOf(bytes) {
        var out = '';
        var i;
        var list = bytes;
        if (bytes && bytes.length != null && typeof bytes !== 'string') {
            for (i = 0; i < list.length; i++) out += String.fromCharCode(list[i] & 255);
            return out;
        }
        return String(bytes || '');
    }

    function u16(n) {
        return String.fromCharCode(n & 255, (n >> 8) & 255);
    }

    function u32(n) {
        return String.fromCharCode(n & 255, (n >> 8) & 255, (n >> 16) & 255, (n >> 24) & 255);
    }

    function f64(value) {
        var buffer = new ArrayBuffer(8);
        var view = new DataView(buffer);
        view.setFloat64(0, Number(value) || 0, true);
        var out = '';
        var i;
        for (i = 0; i < 8; i++) out += String.fromCharCode(view.getUint8(i));
        return out;
    }

    function readU32(text, offset) {
        return (text.charCodeAt(offset) & 255)
            + ((text.charCodeAt(offset + 1) & 255) << 8)
            + ((text.charCodeAt(offset + 2) & 255) << 16)
            + ((text.charCodeAt(offset + 3) & 255) << 24);
    }

    function readF64(text, offset) {
        var buffer = new ArrayBuffer(8);
        var view = new DataView(buffer);
        var i;
        for (i = 0; i < 8; i++) view.setUint8(i, text.charCodeAt(offset + i) & 255);
        return view.getFloat64(0, true);
    }

    function circleThrough(a, b, c) {
        var d = 2 * (a.x * (b.y - c.y) + b.x * (c.y - a.y) + c.x * (a.y - b.y));
        if (Math.abs(d) < 1) return null;
        var a2 = a.x * a.x + a.y * a.y;
        var b2 = b.x * b.x + b.y * b.y;
        var c2 = c.x * c.x + c.y * c.y;
        var ux = (a2 * (b.y - c.y) + b2 * (c.y - a.y) + c2 * (a.y - b.y)) / d;
        var uy = (a2 * (c.x - b.x) + b2 * (a.x - c.x) + c2 * (b.x - a.x)) / d;
        var r = Math.sqrt((a.x - ux) * (a.x - ux) + (a.y - uy) * (a.y - uy));
        if (r < 1) return null;
        return { x: ux, y: uy, r: r };
    }

    function turn(from, to) {
        var d = to - from;
        while (d <= -Math.PI) d += Math.PI * 2;
        while (d > Math.PI) d -= Math.PI * 2;
        return d;
    }

    function arcPoints(points, steps) {
        var list = points || [];
        if (list.length < 2) return [];
        if (list.length < 3) return [{ x: list[0].x, y: list[0].y }, { x: list[1].x, y: list[1].y }];
        var a = list[0];
        var b = list[1];
        var c = list[2];
        var circle = circleThrough(a, c, b);
        var count = steps || 8;
        if (!circle) return [{ x: a.x, y: a.y }, { x: b.x, y: b.y }];
        var a0 = Math.atan2(a.y - circle.y, a.x - circle.x);
        var a1 = Math.atan2(b.y - circle.y, b.x - circle.x);
        var am = Math.atan2(c.y - circle.y, c.x - circle.x);
        var sweep = turn(a0, a1);
        if (sweep * turn(a0, am) < 0) sweep = sweep > 0 ? sweep - Math.PI * 2 : sweep + Math.PI * 2;
        var out = [];
        var i;
        for (i = 0; i <= count; i++) {
            var ang = a0 + sweep * (i / count);
            out.push({ x: circle.x + Math.cos(ang) * circle.r, y: circle.y + Math.sin(ang) * circle.r });
        }
        return out;
    }

    function wallRoom(kind, x1, y1, x2, y2) {
        var dx = x2 - x1;
        var dy = y2 - y1;
        var length = Math.sqrt(dx * dx + dy * dy);
        if (length < 8) return null;
        var route = kind === 'power' || kind === 'scs' || kind === 'comm' || kind === 'vent' || kind === 'water';
        var thick = route ? 6 : (kind === 'partition' ? 6 : (kind === 'door' || kind === 'window' ? 10 : (kind === 'dim' ? 8 : 18)));
        var angle = Math.round(Math.atan2(dy, dx) * 180 / Math.PI);
        if (angle < 0) angle += 360;
        var w = Math.max(8, Math.round(length));
        var h = thick;
        var storedKind = kind === 'door' || kind === 'window' || kind === 'dim' || kind === 'partition' || route ? kind : 'wall';
        return {
            kind: storedKind,
            name: '',
            color: '',
            angle: angle % 360,
            points: [],
            x: Math.round((x1 + x2) / 2 - w / 2),
            y: Math.round((y1 + y2) / 2 - h / 2),
            w: w,
            h: h
        };
    }

    function fitRooms(rooms) {
        if (!rooms.length) return rooms;
        var minX = Infinity;
        var minY = Infinity;
        var maxX = -Infinity;
        var maxY = -Infinity;
        function touch(x, y) {
            if (x < minX) minX = x;
            if (y < minY) minY = y;
            if (x > maxX) maxX = x;
            if (y > maxY) maxY = y;
        }
        rooms.forEach(function (room) {
            touch(room.x, room.y);
            touch(room.x + room.w, room.y + room.h);
            (room.points || []).forEach(function (point) { touch(point.x, point.y); });
        });
        var span = Math.max(maxX - minX, maxY - minY, 1);
        var scale = 1;
        if (span > 20000) scale = 0.1;
        else if (span < 80) scale = 100;
        var dx = 40 - minX * scale;
        var dy = 40 - minY * scale;
        return rooms.map(function (room) {
            var next = {
                kind: room.kind,
                name: room.name || '',
                color: room.color || '',
                angle: room.angle || 0,
                x: Math.round(room.x * scale + dx),
                y: Math.round(room.y * scale + dy),
                w: Math.max(4, Math.round(room.w * scale)),
                h: Math.max(4, Math.round(room.h * scale)),
                points: (room.points || []).map(function (point) {
                    return { x: point.x * scale + dx, y: point.y * scale + dy };
                })
            };
            return next;
        });
    }

    function layerKind(name) {
        var text = String(name || '').toUpperCase();
        if (text.indexOf('POWER') >= 0 || text.indexOf('ЭЛЕКТ') >= 0) return 'power';
        if (text.indexOf('SCS') >= 0 || text.indexOf('СКС') >= 0) return 'scs';
        if (text.indexOf('COMM') >= 0 || text.indexOf('СВЯЗ') >= 0) return 'comm';
        if (text.indexOf('VENT') >= 0 || text.indexOf('ВЕНТ') >= 0) return 'vent';
        if (text.indexOf('WATER') >= 0 || text.indexOf('ВОД') >= 0) return 'water';
        if (text.indexOf('HEAT') >= 0 || text.indexOf('ОТОП') >= 0) return 'heat';
        if (text.indexOf('FIRE') >= 0 || text.indexOf('ПОЖ') >= 0) return 'fire';
        if (text.indexOf('GUARD') >= 0 || text.indexOf('ОХР') >= 0) return 'guard';
        if (text.indexOf('PART') >= 0 || text.indexOf('ПЕРЕГ') >= 0) return 'partition';
        if (text.indexOf('DOOR') >= 0 || text.indexOf('ДВЕР') >= 0) return 'door';
        if (text.indexOf('WIND') >= 0 || text.indexOf('ОКН') >= 0) return 'window';
        if (text.indexOf('DIM') >= 0 || text.indexOf('РАЗМ') >= 0) return 'dim';
        return 'wall';
    }

    function dxfPair(code, value) {
        var line = String(code);
        while (line.length < 3) line = ' ' + line;
        return line + '\n' + value + '\n';
    }

    function exportDxf(model) {
        var out = '';
        out += dxfPair(0, 'SECTION');
        out += dxfPair(2, 'HEADER');
        out += dxfPair(9, '$ACADVER');
        out += dxfPair(1, 'AC1015');
        out += dxfPair(9, '$INSUNITS');
        out += dxfPair(70, '5');
        out += dxfPair(0, 'ENDSEC');
        out += dxfPair(0, 'SECTION');
        out += dxfPair(2, 'ENTITIES');
        var height = model.planH || 760;
        (model.items || []).forEach(function (item) {
            if (item.kind === 'text') {
                out += dxfPair(0, 'TEXT');
                out += dxfPair(8, 'TEXT');
                out += dxfPair(10, item.x);
                out += dxfPair(20, height - item.y);
                out += dxfPair(40, 12);
                out += dxfPair(1, String(item.name || '').replace(/[\r\n]/g, ' '));
                return;
            }
            if (item.kind === 'arc' && item.points && item.points.length >= 3) {
                var samples = arcPoints(item.points, 8);
                var i;
                for (i = 0; i < samples.length - 1; i++) {
                    out += dxfPair(0, 'LINE');
                    out += dxfPair(8, 'ARC');
                    out += dxfPair(10, samples[i].x);
                    out += dxfPair(20, height - samples[i].y);
                    out += dxfPair(11, samples[i + 1].x);
                    out += dxfPair(21, height - samples[i + 1].y);
                }
                return;
            }
            if (item.x1 == null) return;
            out += dxfPair(0, 'LINE');
            var dxfLayer = 'WALL';
            if (item.kind === 'partition') dxfLayer = 'PARTITION';
            else if (item.kind === 'dim') dxfLayer = 'DIM';
            else if (item.kind === 'door') dxfLayer = 'DOOR';
            else if (item.kind === 'window') dxfLayer = 'WINDOW';
            else if (item.kind === 'power') dxfLayer = 'POWER';
            else if (item.kind === 'scs') dxfLayer = 'SCS';
            else if (item.kind === 'comm') dxfLayer = 'COMM';
            else if (item.kind === 'vent') dxfLayer = 'VENT';
            else if (item.kind === 'water') dxfLayer = 'WATER';
            else if (item.kind === 'heat') dxfLayer = 'HEAT';
            else if (item.kind === 'fire') dxfLayer = 'FIRE';
            else if (item.kind === 'guard') dxfLayer = 'GUARD';
            out += dxfPair(8, dxfLayer);
            out += dxfPair(10, item.x1);
            out += dxfPair(20, height - item.y1);
            out += dxfPair(11, item.x2);
            out += dxfPair(21, height - item.y2);
        });
        out += dxfPair(0, 'ENDSEC');
        out += dxfPair(0, 'EOF');
        return out;
    }

    function importDxf(raw) {
        var text = String(raw || '').replace(/^\uFEFF/, '');
        var lines = text.split(/\r\n|\n|\r/);
        var pairs = [];
        var i;
        for (i = 0; i + 1 < lines.length; i += 2) {
            var code = parseInt(String(lines[i]).trim(), 10);
            if (isNaN(code)) continue;
            pairs.push({ code: code, value: String(lines[i + 1]).trim() });
        }
        var entities = [];
        var current = null;
        var inEntities = false;
        var section = '';
        pairs.forEach(function (pair) {
            if (pair.code === 0 && pair.value === 'SECTION') {
                section = '';
                return;
            }
            if (pair.code === 2 && section === '') {
                section = pair.value;
                inEntities = pair.value === 'ENTITIES';
                return;
            }
            if (pair.code === 0 && pair.value === 'ENDSEC') {
                if (current) entities.push(current);
                current = null;
                inEntities = false;
                return;
            }
            if (!inEntities) return;
            if (pair.code === 0) {
                if (current) entities.push(current);
                current = { type: pair.value, layer: '0', x: [], y: [], r: 0, a0: 0, a1: 0, text: '', closed: false };
                return;
            }
            if (!current) return;
            if (pair.code === 8) current.layer = pair.value;
            else if (pair.code === 10 || pair.code === 11) current.x.push(parseFloat(pair.value));
            else if (pair.code === 20 || pair.code === 21) current.y.push(parseFloat(pair.value));
            else if (pair.code === 40) current.r = parseFloat(pair.value);
            else if (pair.code === 50) current.a0 = parseFloat(pair.value);
            else if (pair.code === 51) current.a1 = parseFloat(pair.value);
            else if (pair.code === 1) current.text = pair.value;
            else if (pair.code === 70) current.closed = (parseInt(pair.value, 10) & 1) === 1;
        });
        if (current) entities.push(current);
        var height = 0;
        entities.forEach(function (entity) {
            entity.y.forEach(function (value) { if (value > height) height = value; });
        });
        if (height < 100) height = 760;
        function down(y) { return height - y; }
        var rooms = [];
        entities.forEach(function (entity) {
            var kind = layerKind(entity.layer);
            if (entity.type === 'LINE' && entity.x.length >= 2 && entity.y.length >= 2) {
                var line = wallRoom(kind, entity.x[0], down(entity.y[0]), entity.x[1], down(entity.y[1]));
                if (line) rooms.push(line);
                return;
            }
            if ((entity.type === 'LWPOLYLINE' || entity.type === 'POLYLINE') && entity.x.length >= 2) {
                var count = Math.min(entity.x.length, entity.y.length);
                var n;
                var last = entity.closed ? count : count - 1;
                for (n = 0; n < last; n++) {
                    var a = n;
                    var b = (n + 1) % count;
                    var edge = wallRoom(kind, entity.x[a], down(entity.y[a]), entity.x[b], down(entity.y[b]));
                    if (edge) rooms.push(edge);
                }
                return;
            }
            if (entity.type === 'ARC' && entity.x.length && entity.r > 1) {
                var start = entity.a0 * Math.PI / 180;
                var end = entity.a1 * Math.PI / 180;
                var sweep = end - start;
                if (sweep <= 0) sweep += Math.PI * 2;
                var mid = start + sweep / 2;
                var cx = entity.x[0];
                var cy = entity.y[0];
                rooms.push({
                    kind: 'arc',
                    name: '',
                    color: '',
                    angle: 0,
                    x: Math.round(cx - entity.r),
                    y: Math.round(down(cy) - entity.r),
                    w: Math.round(entity.r * 2),
                    h: Math.round(entity.r * 2),
                    points: [
                        { x: cx + Math.cos(start) * entity.r, y: down(cy + Math.sin(start) * entity.r) },
                        { x: cx + Math.cos(end) * entity.r, y: down(cy + Math.sin(end) * entity.r) },
                        { x: cx + Math.cos(mid) * entity.r, y: down(cy + Math.sin(mid) * entity.r) }
                    ]
                });
                return;
            }
            if ((entity.type === 'TEXT' || entity.type === 'MTEXT') && entity.text && entity.x.length) {
                rooms.push({
                    kind: 'text',
                    name: entity.text.slice(0, 80),
                    color: '',
                    angle: 0,
                    x: Math.round(entity.x[0]),
                    y: Math.round(down(entity.y[0]) - 16),
                    w: Math.max(40, entity.text.length * 8),
                    h: 24,
                    points: []
                });
            }
        });
        return { rooms: fitRooms(rooms) };
    }

    function pdfEscape(text) {
        return String(text || '').replace(/\\/g, '\\\\').replace(/[()]/g, function (ch) { return '\\' + ch; }).replace(/[^\x20-\x7E]/g, '?');
    }

    function exportPdf(model) {
        var pageW = 842;
        var pageH = 595;
        var margin = 28;
        var planW = model.planW || 1200;
        var planH = model.planH || 760;
        var scale = Math.min((pageW - margin * 2) / planW, (pageH - margin * 2) / planH);
        function px(x) { return margin + x * scale; }
        function py(y) { return margin + (planH - y) * scale; }
        var commands = [];
        commands.push('0.11 0.11 0.11 RG');
        commands.push('1.4 w');
        (model.items || []).forEach(function (item) {
            if (item.kind === 'room' && item.points && item.points.length >= 3) {
                commands.push('0.90 0.91 0.93 rg');
                item.points.forEach(function (point, index) {
                    commands.push(px(point.x).toFixed(2) + ' ' + py(point.y).toFixed(2) + (index ? ' l' : ' m'));
                });
                commands.push('h f');
            }
        });
        commands.push('0.11 0.11 0.11 RG');
        (model.items || []).forEach(function (item) {
            var samples = null;
            if (item.kind === 'arc' && item.points) samples = arcPoints(item.points, 8);
            else if (item.x1 != null) samples = [{ x: item.x1, y: item.y1 }, { x: item.x2, y: item.y2 }];
            if (!samples || samples.length < 2) return;
            if (item.kind === 'partition') commands.push('0.7 w');
            else if (item.kind === 'dim') commands.push('0.4 w 0.25 0.33 0.48 RG');
            else commands.push('1.6 w 0.11 0.11 0.11 RG');
            samples.forEach(function (point, index) {
                commands.push(px(point.x).toFixed(2) + ' ' + py(point.y).toFixed(2) + (index ? ' l' : ' m'));
            });
            commands.push('S');
        });
        commands.push('BT /F1 9 Tf 0.09 0.17 0.30 rg');
        (model.items || []).forEach(function (item) {
            if (item.kind !== 'text' && !(item.kind === 'room' && item.name)) return;
            var x = item.kind === 'text' ? item.x : item.points[0].x;
            var y = item.kind === 'text' ? item.y : item.points[0].y;
            commands.push('1 0 0 1 ' + px(x).toFixed(2) + ' ' + py(y).toFixed(2) + ' Tm (' + pdfEscape(item.name) + ') Tj');
        });
        commands.push('ET');
        var stream = commands.join('\n');
        var objects = [];
        objects.push('1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n');
        objects.push('2 0 obj << /Type /Pages /Count 1 /Kids [3 0 R] >> endobj\n');
        objects.push('3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 ' + pageW + ' ' + pageH + '] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >> endobj\n');
        objects.push('4 0 obj << /Length ' + stream.length + ' >> stream\n' + stream + '\nendstream endobj\n');
        objects.push('5 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> endobj\n');
        var body = '%PDF-1.4\n% AssetTree ' + scale.toFixed(6) + ' ' + margin + ' ' + planH + '\n';
        var offsets = [0];
        objects.forEach(function (object) {
            offsets.push(body.length);
            body += object;
        });
        var xref = body.length;
        body += 'xref\n0 ' + (objects.length + 1) + '\n';
        body += '0000000000 65535 f \n';
        var n;
        for (n = 1; n < offsets.length; n++) {
            var slot = String(offsets[n]);
            while (slot.length < 10) slot = '0' + slot;
            body += slot + ' 00000 n \n';
        }
        body += 'trailer << /Size ' + (objects.length + 1) + ' /Root 1 0 R >>\nstartxref\n' + xref + '\n%%EOF';
        return body;
    }

    function pdfStreams(raw) {
        var streams = [];
        var cursor = 0;
        while (cursor < raw.length) {
            var start = raw.indexOf('stream', cursor);
            if (start < 0) break;
            var dictStart = raw.lastIndexOf('<<', start);
            var dict = dictStart >= 0 ? raw.slice(dictStart, start) : '';
            var dataStart = start + 6;
            if (raw.charAt(dataStart) === '\r') dataStart++;
            if (raw.charAt(dataStart) === '\n') dataStart++;
            var end = raw.indexOf('endstream', dataStart);
            if (end < 0) break;
            var data = raw.slice(dataStart, end).replace(/\r?\n$/, '');
            streams.push({ dict: dict, data: data });
            cursor = end + 9;
        }
        return streams;
    }

    function pdfPaths(content) {
        var tokens = content.replace(/[()]/g, ' ').split(/\s+/);
        var lines = [];
        var cx = 0;
        var cy = 0;
        var path = [];
        var i = 0;
        function point(x, y) { return { x: x, y: y }; }
        function flush() {
            var n;
            for (n = 1; n < path.length; n++) lines.push({ x1: path[n - 1].x, y1: path[n - 1].y, x2: path[n].x, y2: path[n].y });
            path = [];
        }
        while (i < tokens.length) {
            var token = tokens[i];
            if (token === 'm' || token === 'l') {
                var x = parseFloat(tokens[i - 2]);
                var y = parseFloat(tokens[i - 1]);
                if (!isNaN(x) && !isNaN(y)) {
                    if (token === 'm') {
                        flush();
                        path = [point(x, y)];
                    } else if (!path.length) path = [point(cx, cy), point(x, y)];
                    else path.push(point(x, y));
                    cx = x;
                    cy = y;
                }
            } else if (token === 'c' && i >= 6) {
                var ex = parseFloat(tokens[i - 2]);
                var ey = parseFloat(tokens[i - 1]);
                if (!isNaN(ex)) {
                    if (!path.length) path = [point(cx, cy)];
                    path.push(point(ex, ey));
                    cx = ex;
                    cy = ey;
                }
            } else if (token === 're' && i >= 4) {
                var rx = parseFloat(tokens[i - 4]);
                var ry = parseFloat(tokens[i - 3]);
                var rw = parseFloat(tokens[i - 2]);
                var rh = parseFloat(tokens[i - 1]);
                if (!isNaN(rw) && Math.abs(rw) > 2 && Math.abs(rh) > 2) {
                    lines.push({ x1: rx, y1: ry, x2: rx + rw, y2: ry });
                    lines.push({ x1: rx + rw, y1: ry, x2: rx + rw, y2: ry + rh });
                    lines.push({ x1: rx + rw, y1: ry + rh, x2: rx, y2: ry + rh });
                    lines.push({ x1: rx, y1: ry + rh, x2: rx, y2: ry });
                }
            } else if (token === 'f' || token === 'F') {
                path = [];
            } else if (token === 'S' || token === 's' || token === 'B' || token === 'b') {
                if (token === 's' || token === 'b') {
                    if (path.length > 2) path.push(path[0]);
                }
                flush();
            } else if (token === 'h' && path.length > 2) {
                path.push(path[0]);
            }
            i++;
        }
        flush();
        return lines;
    }

    function importPdf(raw) {
        var text = textOf(raw);
        var meta = /% AssetTree ([\d.]+) ([\d.]+) ([\d.]+)/.exec(text);
        var scale = meta ? parseFloat(meta[1]) : 1;
        var margin = meta ? parseFloat(meta[2]) : 0;
        var planH = meta ? parseFloat(meta[3]) : 0;
        var page = /MediaBox\s*\[\s*([\d.\-]+)\s+([\d.\-]+)\s+([\d.\-]+)\s+([\d.\-]+)\s*\]/.exec(text);
        var pageH = page ? parseFloat(page[4]) : 595;
        if (!planH) planH = pageH;
        var rooms = [];
        var skipped = 0;
        pdfStreams(text).forEach(function (stream) {
            if (/FlateDecode/.test(stream.dict)) {
                skipped += 1;
                return;
            }
            pdfPaths(stream.data).forEach(function (line) {
                var x1;
                var y1;
                var x2;
                var y2;
                if (meta && scale) {
                    x1 = (line.x1 - margin) / scale;
                    y1 = planH - (line.y1 - margin) / scale;
                    x2 = (line.x2 - margin) / scale;
                    y2 = planH - (line.y2 - margin) / scale;
                } else {
                    x1 = line.x1;
                    y1 = pageH - line.y1;
                    x2 = line.x2;
                    y2 = pageH - line.y2;
                }
                var edge = wallRoom('wall', x1, y1, x2, y2);
                if (edge) rooms.push(edge);
            });
        });
        if (!rooms.length && skipped) return { rooms: [], error: 'mapPdfCompressed' };
        return { rooms: meta ? rooms : fitRooms(rooms) };
    }

    function importPdfFlate(raw, plain) {
        var text = textOf(raw);
        var merged = text;
        if (plain) {
            var streams = pdfStreams(text);
            streams.forEach(function (stream) {
                if (/FlateDecode/.test(stream.dict)) merged += '\n' + plain + '\n';
            });
        }
        return importPdf(merged);
    }

    function exportDwg(model) {
        var height = model.planH || 760;
        var records = [];
        (model.items || []).forEach(function (item) {
            if (item.kind === 'text') {
                var name = String(item.name || '').slice(0, 80);
                records.push(String.fromCharCode(2) + f64(item.x) + f64(height - item.y) + u16(name.length) + name);
                return;
            }
            if (item.kind === 'arc' && item.points && item.points.length >= 3) {
                records.push(String.fromCharCode(3)
                    + f64(item.points[0].x) + f64(height - item.points[0].y)
                    + f64(item.points[1].x) + f64(height - item.points[1].y)
                    + f64(item.points[2].x) + f64(height - item.points[2].y));
                return;
            }
            if (item.x1 == null) return;
            var kindCode = item.kind === 'partition' ? 4 : (item.kind === 'dim' ? 5 : 1);
            records.push(String.fromCharCode(kindCode) + f64(item.x1) + f64(height - item.y1) + f64(item.x2) + f64(height - item.y2));
        });
        return 'AC1015' + String.fromCharCode(0) + 'ASSETPLAN' + u32(records.length) + records.join('');
    }

    function importDwg(raw) {
        var text = textOf(raw);
        if (text.indexOf('SECTION') >= 0 && text.indexOf('ENTITIES') >= 0) return importDxf(text);
        var marker = text.indexOf('ASSETPLAN');
        if (text.indexOf('AC10') !== 0 || marker < 0 || marker > 16) {
            return { rooms: [], error: 'mapDwgCompressed' };
        }
        var offset = marker + 'ASSETPLAN'.length;
        if (offset + 4 > text.length) return { rooms: [], error: 'mapDwgCompressed' };
        var count = readU32(text, offset);
        offset += 4;
        if (count < 0 || count > 5000) return { rooms: [], error: 'mapDwgCompressed' };
        var height = 760;
        var pending = [];
        var n;
        for (n = 0; n < count; n++) {
            if (offset >= text.length) break;
            var type = text.charCodeAt(offset) & 255;
            offset += 1;
            if (type === 2) {
                var x = readF64(text, offset);
                var y = readF64(text, offset + 8);
                var len = (text.charCodeAt(offset + 16) & 255) + ((text.charCodeAt(offset + 17) & 255) << 8);
                offset += 18;
                var name = text.substr(offset, len);
                offset += len;
                pending.push({ kind: 'text', x: x, y: y, name: name });
            } else if (type === 3) {
                var pts = [];
                var k;
                for (k = 0; k < 3; k++) {
                    pts.push({ x: readF64(text, offset), y: readF64(text, offset + 8) });
                    offset += 16;
                }
                pending.push({ kind: 'arc', points: pts });
            } else if (type === 1 || type === 4 || type === 5) {
                var x1 = readF64(text, offset);
                var y1 = readF64(text, offset + 8);
                var x2 = readF64(text, offset + 16);
                var y2 = readF64(text, offset + 24);
                offset += 32;
                var kind = type === 4 ? 'partition' : (type === 5 ? 'dim' : 'wall');
                pending.push({ kind: kind, x1: x1, y1: y1, x2: x2, y2: y2 });
            } else {
                return { rooms: [], error: 'mapDwgCompressed' };
            }
            if (type !== 2) {
                var sample = type === 3 ? pending[pending.length - 1].points[0].y : pending[pending.length - 1].y1;
                if (sample > height) height = sample;
            }
        }
        var rooms = [];
        pending.forEach(function (item) {
            if (item.kind === 'text') {
                rooms.push({
                    kind: 'text', name: item.name, color: '', angle: 0,
                    x: Math.round(item.x), y: Math.round(height - item.y - 16),
                    w: Math.max(40, item.name.length * 8), h: 24, points: []
                });
                return;
            }
            if (item.kind === 'arc') {
                var points = item.points.map(function (point) { return { x: point.x, y: height - point.y }; });
                rooms.push({
                    kind: 'arc', name: '', color: '', angle: 0,
                    x: Math.round(Math.min(points[0].x, points[1].x, points[2].x)),
                    y: Math.round(Math.min(points[0].y, points[1].y, points[2].y)),
                    w: 32, h: 32, points: points
                });
                return;
            }
            var line = wallRoom(item.kind, item.x1, height - item.y1, item.x2, height - item.y2);
            if (line) rooms.push(line);
        });
        return { rooms: fitRooms(rooms) };
    }

    function importDrawing(name, raw) {
        var text = textOf(raw);
        var lower = String(name || '').toLowerCase();
        if (lower.indexOf('.pdf') >= 0 || text.indexOf('%PDF') === 0) return importPdf(text);
        if (lower.indexOf('.dwg') >= 0 || (text.indexOf('AC10') === 0 && text.indexOf('SECTION') < 0)) return importDwg(text);
        return importDxf(text);
    }

    return {
        arcPoints: arcPoints,
        exportDxf: exportDxf,
        importDxf: importDxf,
        exportPdf: exportPdf,
        importPdf: importPdf,
        importPdfFlate: importPdfFlate,
        exportDwg: exportDwg,
        importDwg: importDwg,
        importDrawing: importDrawing,
        bytesOf: bytesOf
    };
}());
