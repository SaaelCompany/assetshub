/* Floor-plan exchange: DXF, a line-level DWG, and vector PDF. ES5. */
var AssetTreePlan = (function () {
    function bytesOf(text) {
        var list = [];
        var i;
        for (i = 0; i < text.length; i++) list.push(text.charCodeAt(i) & 255);
        return list;
    }

    function textOf(bytes) {
        if (bytes && bytes.length != null && typeof bytes !== 'string') {
            if (typeof TextDecoder !== 'undefined') {
                try { return new TextDecoder('latin1').decode(bytes); } catch (err) {}
            }
            var parts = [];
            var step = 8192;
            var i;
            for (i = 0; i < bytes.length; i += step) {
                var end = Math.min(bytes.length, i + step);
                var slice = [];
                var j;
                for (j = i; j < end; j++) slice.push(bytes[j] & 255);
                parts.push(String.fromCharCode.apply(null, slice));
            }
            return parts.join('');
        }
        return String(bytes || '');
    }

    function decodeDxf(raw, latin) {
        var text = latin || (typeof raw === 'string' ? raw : textOf(raw));
        if (typeof raw === 'string' || typeof TextDecoder === 'undefined') return text;
        var page = /\$DWGCODEPAGE[\s\S]{0,80}?ANSI_(\d+)/.exec(text.slice(0, 30000));
        var enc = 'windows-1251';
        if (page && page[1] === '65001') enc = 'utf-8';
        else if (page && page[1] === '1252') enc = 'windows-1252';
        else if (page && page[1] !== '1251') enc = 'latin1';
        if (enc === 'latin1') return text;
        try { return new TextDecoder(enc).decode(raw); } catch (err) { return text; }
    }

    function dxfBody(text) {
        var head = String(text || '').slice(0, 80);
        if (head.indexOf('\0') >= 0) return false;
        var sample = String(text || '').slice(0, 12000);
        return sample.indexOf('SECTION') >= 0 && String(text || '').indexOf('ENTITIES') >= 0;
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
            h: h,
            x1: x1,
            y1: y1,
            x2: x2,
            y2: y2
        };
    }

    function fitRooms(rooms, insunits) {
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
        var units = parseInt(insunits, 10) || 0;
        var scale = 1;
        if (units === 4) scale = 0.1;
        else if (units === 6) scale = 100;
        else if (units === 1) scale = 2.54;
        else if (units === 5) scale = 1;
        else if (span > 20000) scale = 0.1;
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
                x1: room.x1 == null ? undefined : room.x1 * scale + dx,
                y1: room.y1 == null ? undefined : room.y1 * scale + dy,
                x2: room.x2 == null ? undefined : room.x2 * scale + dx,
                y2: room.y2 == null ? undefined : room.y2 * scale + dy,
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

    function junkLayer(name) {
        var text = String(name || '').toUpperCase();
        var keys = ['DEFPOINT', 'DIMENSION', 'DIM', 'ANNO', 'ANNOT', 'TEXT', 'HATCH', 'FURN', 'MEBEL', 'МЕБЕЛ', 'РАЗМЕР', 'ТЕКСТ', 'ШТРИХ', 'AXIS', 'ОСИ', 'TITLE', 'RAMKA', 'РАМК', 'ОБОЗН', 'LABEL', 'LEADER', 'VIEWPORT', 'NOPLOT', 'NPLT', 'ОФОРМ', 'ШТАМП', 'GRID', 'СЕТК', 'ПОДПИС'];
        var i;
        for (i = 0; i < keys.length; i++) {
            if (text.indexOf(keys[i]) >= 0) return true;
        }
        return false;
    }

    function lineOffAxis(x1, y1, x2, y2) {
        var ang = Math.abs(Math.atan2(y2 - y1, x2 - x1) * 180 / Math.PI) % 180;
        var off = Math.min(ang, Math.abs(180 - ang), Math.abs(ang - 90));
        return off > 12;
    }

    function lineCell(seg) {
        var mx = Math.round((seg.x1 + seg.x2) / 160);
        var my = Math.round((seg.y1 + seg.y2) / 160);
        return mx + ':' + my;
    }

    function pointOnLine(px, py, seg) {
        var dx = seg.x2 - seg.x1;
        var dy = seg.y2 - seg.y1;
        var span = dx * dx + dy * dy;
        if (span < 1) {
            var ex = px - seg.x1;
            var ey = py - seg.y1;
            return Math.sqrt(ex * ex + ey * ey);
        }
        var t = ((px - seg.x1) * dx + (py - seg.y1) * dy) / span;
        if (t < 0) t = 0;
        else if (t > 1) t = 1;
        var qx = seg.x1 + dx * t - px;
        var qy = seg.y1 + dy * t - py;
        return Math.sqrt(qx * qx + qy * qy);
    }

    function lineHits(seg, others, reach) {
        var i;
        var head = false;
        var tail = false;
        for (i = 0; i < others.length; i++) {
            if (!head && pointOnLine(seg.x1, seg.y1, others[i]) <= reach) head = true;
            if (!tail && pointOnLine(seg.x2, seg.y2, others[i]) <= reach) tail = true;
            if (head && tail) return 2;
        }
        return (head || tail) ? 1 : 0;
    }

    function mergeCollinear(list) {
        var groups = {};
        var i;
        for (i = 0; i < list.length; i++) {
            var seg = list[i];
            var ang = Math.atan2(seg.y2 - seg.y1, seg.x2 - seg.x1);
            if (ang < 0) ang += Math.PI;
            var deg = Math.round(ang * 180 / Math.PI / 2) * 2;
            if (deg >= 180) deg = 0;
            var off = seg.x1 * Math.sin(ang) - seg.y1 * Math.cos(ang);
            var key = (seg.kind || 'wall') + '|' + deg + '|' + Math.round(off / 8);
            (groups[key] || (groups[key] = [])).push(seg);
        }
        var out = [];
        Object.keys(groups).forEach(function (key) {
            var bucket = groups[key];
            var dx = bucket[0].x2 - bucket[0].x1;
            var dy = bucket[0].y2 - bucket[0].y1;
            var span = Math.sqrt(dx * dx + dy * dy) || 1;
            var ux = dx / span;
            var uy = dy / span;
            var runs = [];
            var n;
            for (n = 0; n < bucket.length; n++) {
                var item = bucket[n];
                var a = item.x1 * ux + item.y1 * uy;
                var b = item.x2 * ux + item.y2 * uy;
                runs.push({ item: item, a: Math.min(a, b), b: Math.max(a, b) });
            }
            runs.sort(function (left, right) { return left.a - right.a; });
            var cur = runs[0];
            function emit(run) {
                var sample = run.item;
                var base = sample.x1 * ux + sample.y1 * uy;
                var ox = sample.x1 - ux * base;
                var oy = sample.y1 - uy * base;
                out.push({
                    kind: sample.kind,
                    x1: ox + ux * run.a,
                    y1: oy + uy * run.a,
                    x2: ox + ux * run.b,
                    y2: oy + uy * run.b,
                    len: run.b - run.a
                });
            }
            for (n = 1; n < runs.length; n++) {
                if (runs[n].a <= cur.b + 14) {
                    if (runs[n].b > cur.b) cur.b = runs[n].b;
                } else {
                    emit(cur);
                    cur = runs[n];
                }
            }
            emit(cur);
        });
        return out;
    }

    function chooseLines(placed) {
        var pool = [];
        var plain = 0;
        var i;
        for (i = 0; i < placed.length; i++) {
            if (placed[i].len < 8) continue;
            if (!placed[i].junk) plain += 1;
            pool.push(placed[i]);
        }
        var source = [];
        for (i = 0; i < pool.length; i++) {
            if (plain && pool[i].junk) continue;
            source.push(pool[i]);
        }
        var cells = {};
        for (i = 0; i < source.length; i++) {
            if (source[i].len >= 170) continue;
            var key = lineCell(source[i]);
            cells[key] = (cells[key] || 0) + 1;
        }
        var open = [];
        for (i = 0; i < source.length; i++) {
            var line = source[i];
            if (line.len < 170 && cells[lineCell(line)] >= 6) continue;
            if (line.diag && line.len < 100) continue;
            open.push(line);
        }
        var hasLong = false;
        for (i = 0; i < open.length; i++) {
            if (!open[i].diag && open[i].len >= 150) hasLong = true;
        }
        var chosen = open;
        if (hasLong || open.length > 500) {
            var longs = [];
            for (i = 0; i < open.length; i++) {
                var item = open[i];
                if ((!item.diag && item.len >= 100) || (item.diag && item.len >= 220)) longs.push(item);
            }
            var extra = [];
            if (longs.length && longs.length <= 900) {
                for (i = 0; i < open.length; i++) {
                    var shortLine = open[i];
                    var longEnough = (!shortLine.diag && shortLine.len >= 100) || (shortLine.diag && shortLine.len >= 220);
                    if (longEnough) continue;
                    var hits = lineHits(shortLine, longs, 18);
                    if (!shortLine.diag && shortLine.len >= 35 && hits) extra.push(shortLine);
                    else if (shortLine.diag && shortLine.len >= 120 && hits === 2) extra.push(shortLine);
                }
            }
            chosen = longs.concat(extra);
        }
        if (chosen.length > 1200) {
            chosen.sort(function (a, b) { return b.len - a.len; });
            chosen = chosen.slice(0, 1200);
        }
        if (!chosen.length && source.length) {
            var axes = [];
            for (i = 0; i < source.length; i++) {
                if (!source[i].diag && source[i].len >= 35) axes.push(source[i]);
            }
            axes.sort(function (a, b) { return b.len - a.len; });
            chosen = axes.slice(0, 400);
        }
        return mergeCollinear(chosen);
    }

    function placedRoom(kind, x1, y1, x2, y2) {
        var dx = x2 - x1;
        var dy = y2 - y1;
        var length = Math.sqrt(dx * dx + dy * dy);
        if (length < 8) return null;
        var angle = Math.round(Math.atan2(dy, dx) * 180 / Math.PI);
        if (angle < 0) angle += 360;
        var route = kind === 'power' || kind === 'scs' || kind === 'comm' || kind === 'vent' || kind === 'water';
        var thick = route ? 6 : (kind === 'partition' ? 6 : (kind === 'door' || kind === 'window' ? 10 : 18));
        var stored = kind === 'door' || kind === 'window' || kind === 'dim' || kind === 'partition' || route ? kind : 'wall';
        var w = Math.max(8, Math.round(length));
        return {
            kind: stored,
            name: '',
            color: '',
            angle: angle % 360,
            points: [],
            x: Math.round((x1 + x2) / 2 - w / 2),
            y: Math.round((y1 + y2) / 2 - thick / 2),
            w: w,
            h: thick,
            x1: x1,
            y1: y1,
            x2: x2,
            y2: y2
        };
    }

    function importDxf(raw) {
        var text = (typeof raw === 'string' ? String(raw || '') : decodeDxf(raw)).replace(/^\uFEFF/, '');
        if (!dxfBody(text) && text.indexOf('\0') >= 0) return { rooms: [], error: 'mapDwgCompressed' };
        var lines = text.split(/\r\n|\n|\r/);
        var pairs = [];
        var i = 0;
        while (i + 1 < lines.length) {
            var codeLine = String(lines[i]).replace(/^\s+|\s+$/g, '');
            if (!codeLine) {
                i += 1;
                continue;
            }
            var code = parseInt(codeLine, 10);
            if (isNaN(code)) {
                i += 1;
                continue;
            }
            pairs.push({ code: code, value: String(lines[i + 1]).replace(/^\s+|\s+$/g, '') });
            i += 2;
        }
        var entities = [];
        var current = null;
        var poly = null;
        var inEntities = false;
        var section = '';
        var insunits = 0;
        var headerKey = '';
        var drawn = { LINE: 1, LWPOLYLINE: 1, POLYLINE: 1, ARC: 1, TEXT: 1, MTEXT: 1 };
        pairs.forEach(function (pair) {
            if (pair.code === 0 && pair.value === 'SECTION') {
                section = '';
                headerKey = '';
                return;
            }
            if (pair.code === 2 && section === '') {
                section = pair.value;
                inEntities = pair.value === 'ENTITIES';
                return;
            }
            if (section === 'HEADER' && pair.code === 9) {
                headerKey = pair.value;
                return;
            }
            if (section === 'HEADER' && headerKey === '$INSUNITS' && pair.code === 70) {
                insunits = parseInt(pair.value, 10) || 0;
                headerKey = '';
                return;
            }
            if (pair.code === 0 && pair.value === 'ENDSEC') {
                if (poly) entities.push(poly);
                else if (current) entities.push(current);
                current = null;
                poly = null;
                inEntities = false;
                return;
            }
            if (!inEntities) return;
            if (pair.code === 0) {
                var type = String(pair.value || '').toUpperCase();
                if (type === 'VERTEX' && poly) {
                    current = poly;
                    poly.expectVertex = true;
                    return;
                }
                if (type === 'SEQEND') {
                    if (poly) {
                        poly.expectVertex = false;
                        entities.push(poly);
                        poly = null;
                    }
                    current = null;
                    return;
                }
                if (poly) {
                    entities.push(poly);
                    poly = null;
                } else if (current) entities.push(current);
                current = null;
                if (!drawn[type]) return;
                current = { type: type, layer: '0', x: [], y: [], r: 0, a0: 0, a1: 0, text: '', closed: false, paper: false, expectVertex: false };
                if (type === 'POLYLINE') poly = current;
                return;
            }
            if (!current || current.skip) return;
            if (pair.code === 8 && !current.expectVertex) current.layer = pair.value;
            else if (pair.code === 10 || pair.code === 11) {
                if (current.type === 'POLYLINE' && !current.expectVertex) return;
                var x = parseFloat(pair.value);
                if (!isNaN(x)) current.x.push(x);
            } else if (pair.code === 20 || pair.code === 21) {
                if (current.type === 'POLYLINE' && !current.expectVertex) return;
                var y = parseFloat(pair.value);
                if (!isNaN(y)) current.y.push(y);
            } else if (pair.code === 40) current.r = parseFloat(pair.value);
            else if (pair.code === 50) current.a0 = parseFloat(pair.value);
            else if (pair.code === 51) current.a1 = parseFloat(pair.value);
            else if (pair.code === 1 && !current.expectVertex) current.text = pair.value;
            else if (pair.code === 67 && !current.expectVertex) current.paper = parseInt(pair.value, 10) === 1;
            else if (pair.code === 70 && !current.expectVertex && (current.type === 'LWPOLYLINE' || current.type === 'POLYLINE')) {
                current.closed = (parseInt(pair.value, 10) & 1) === 1;
            }
        });
        if (poly && entities.indexOf(poly) < 0) entities.push(poly);
        else if (current && entities.indexOf(current) < 0) entities.push(current);
        var segments = [];
        var notes = [];
        entities.forEach(function (entity) {
            if (!entity || entity.paper) return;
            var kind = layerKind(entity.layer);
            var junk = junkLayer(entity.layer) || kind === 'dim';
            if (entity.type === 'LINE' && entity.x.length >= 2 && entity.y.length >= 2) {
                segments.push({ kind: kind, junk: junk, x1: entity.x[0], y1: entity.y[0], x2: entity.x[1], y2: entity.y[1] });
                return;
            }
            if ((entity.type === 'LWPOLYLINE' || entity.type === 'POLYLINE') && entity.x.length >= 2 && entity.y.length >= 2) {
                var count = Math.min(entity.x.length, entity.y.length);
                var last = entity.closed ? count : count - 1;
                var n;
                for (n = 0; n < last; n++) {
                    var a = n;
                    var b = (n + 1) % count;
                    segments.push({ kind: kind, junk: junk, x1: entity.x[a], y1: entity.y[a], x2: entity.x[b], y2: entity.y[b] });
                }
                return;
            }
            if (notes.length >= 80) return;
            if (entity.type === 'ARC' && entity.x.length && entity.r > 1 && !junk) {
                notes.push(entity);
                return;
            }
            if ((entity.type === 'TEXT' || entity.type === 'MTEXT') && entity.text && entity.x.length && !junk) notes.push(entity);
        });
        if (!segments.length && !notes.length) return { rooms: [] };
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
        segments.forEach(function (seg) {
            touch(seg.x1, seg.y1);
            touch(seg.x2, seg.y2);
        });
        if (minX === Infinity) {
            notes.forEach(function (entity) { touch(entity.x[0], entity.y[0]); });
        }
        var span = Math.max(maxX - minX, maxY - minY, 1);
        var units = parseInt(insunits, 10) || 0;
        var scale = 1;
        if (units === 4) scale = 0.1;
        else if (units === 6) scale = 100;
        else if (units === 1) scale = 2.54;
        else if (units === 5) scale = 1;
        else if (span > 20000) scale = 0.1;
        else if (span < 80) scale = 100;
        var placed = segments.map(function (seg) {
            var x1 = seg.x1 * scale;
            var x2 = seg.x2 * scale;
            var y1 = (maxY - seg.y1) * scale;
            var y2 = (maxY - seg.y2) * scale;
            var dx = x2 - x1;
            var dy = y2 - y1;
            return {
                kind: seg.kind,
                junk: seg.junk,
                x1: x1,
                y1: y1,
                x2: x2,
                y2: y2,
                len: Math.sqrt(dx * dx + dy * dy),
                diag: lineOffAxis(x1, y1, x2, y2)
            };
        });
        var kept = chooseLines(placed);
        var shiftX = 40;
        var shiftY = 40;
        if (kept.length) {
            var lowX = Infinity;
            var lowY = Infinity;
            kept.forEach(function (seg) {
                if (seg.x1 < lowX) lowX = seg.x1;
                if (seg.x2 < lowX) lowX = seg.x2;
                if (seg.y1 < lowY) lowY = seg.y1;
                if (seg.y2 < lowY) lowY = seg.y2;
            });
            shiftX = 40 - lowX;
            shiftY = 40 - lowY;
        }
        var rooms = [];
        kept.forEach(function (seg) {
            var line = placedRoom(seg.kind, seg.x1 + shiftX, seg.y1 + shiftY, seg.x2 + shiftX, seg.y2 + shiftY);
            if (line) rooms.push(line);
        });
        notes.forEach(function (entity) {
            if (entity.type === 'ARC') {
                var start = entity.a0 * Math.PI / 180;
                var end = entity.a1 * Math.PI / 180;
                var sweep = end - start;
                if (sweep <= 0) sweep += Math.PI * 2;
                var mid = start + sweep / 2;
                var cx = entity.x[0] * scale + shiftX;
                var cy = (maxY - entity.y[0]) * scale + shiftY;
                var radius = entity.r * scale;
                rooms.push({
                    kind: 'arc',
                    name: '',
                    color: '',
                    angle: 0,
                    x: Math.round(cx - radius),
                    y: Math.round(cy - radius),
                    w: Math.round(radius * 2),
                    h: Math.round(radius * 2),
                    points: [
                        { x: cx + Math.cos(start) * radius, y: cy - Math.sin(start) * radius },
                        { x: cx + Math.cos(end) * radius, y: cy - Math.sin(end) * radius },
                        { x: cx + Math.cos(mid) * radius, y: cy - Math.sin(mid) * radius }
                    ]
                });
                return;
            }
            var tx = entity.x[0] * scale + shiftX;
            var ty = (maxY - entity.y[0]) * scale + shiftY;
            rooms.push({
                kind: 'text',
                name: entity.text.slice(0, 80),
                color: '',
                angle: 0,
                x: Math.round(tx),
                y: Math.round(ty - 16),
                w: Math.max(40, entity.text.length * 8),
                h: 24,
                points: []
            });
        });
        return { rooms: rooms };
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
        if (!rooms.length) {
            var scan = scanJpeg(text);
            if (scan) return { rooms: [], underlay: scan };
        }
        if (!rooms.length && skipped) return { rooms: [], error: 'mapPdfCompressed' };
        return { rooms: meta ? rooms : fitRooms(rooms) };
    }

    function scanJpeg(text) {
        var start = text.indexOf('\xff\xd8\xff');
        if (start < 0) return '';
        var end = text.indexOf('\xff\xd9', start + 3);
        if (end < 0 || end - start > 700000 || end - start < 32) return '';
        var bytes = text.substring(start, end + 2);
        if (typeof btoa !== 'function') return '';
        try {
            return 'data:image/jpeg;base64,' + btoa(bytes);
        } catch (ex) {
            return '';
        }
    }

    function teeProject(ax, ay, bx, by, px, py) {
        var dx = bx - ax;
        var dy = by - ay;
        var span2 = dx * dx + dy * dy;
        if (span2 < 1) return null;
        var t = ((px - ax) * dx + (py - ay) * dy) / span2;
        var qx = ax + dx * t;
        var qy = ay + dy * t;
        var ex = px - qx;
        var ey = py - qy;
        return { t: t, x: qx, y: qy, dist: Math.sqrt(ex * ex + ey * ey) };
    }

    function teeTurn(left, right) {
        var r1 = Math.atan2(left.y2 - left.y1, left.x2 - left.x1);
        var r2 = Math.atan2(right.y2 - right.y1, right.x2 - right.x1);
        var deg = Math.abs(r1 - r2) * 180 / Math.PI;
        while (deg > 180) deg -= 180;
        if (deg > 90) deg = 180 - deg;
        return deg;
    }

    function teeCross(left, right) {
        var rx = left.x2 - left.x1;
        var ry = left.y2 - left.y1;
        var sx = right.x2 - right.x1;
        var sy = right.y2 - right.y1;
        var den = rx * sy - ry * sx;
        if (Math.abs(den) < 1) return null;
        var t = ((right.x1 - left.x1) * sy - (right.y1 - left.y1) * sx) / den;
        var u = ((right.x1 - left.x1) * ry - (right.y1 - left.y1) * rx) / den;
        var leftLen = Math.sqrt(rx * rx + ry * ry) || 1;
        var rightLen = Math.sqrt(sx * sx + sy * sy) || 1;
        if (t <= 2 / leftLen || t >= 1 - 2 / leftLen || u <= 2 / rightLen || u >= 1 - 2 / rightLen) return null;
        return { t: t, u: u, x: left.x1 + t * rx, y: left.y1 + t * ry };
    }

    function lineCross(a, b) {
        var rx = a.x2 - a.x1;
        var ry = a.y2 - a.y1;
        var sx = b.x2 - b.x1;
        var sy = b.y2 - b.y1;
        var den = rx * sy - ry * sx;
        if (Math.abs(den) < 1) return null;
        var t = ((b.x1 - a.x1) * sy - (b.y1 - a.y1) * sx) / den;
        var u = ((b.x1 - a.x1) * ry - (b.y1 - a.y1) * rx) / den;
        return { t: t, u: u, x: a.x1 + t * rx, y: a.y1 + t * ry };
    }

    function segLen(seg) {
        var dx = seg.x2 - seg.x1;
        var dy = seg.y2 - seg.y1;
        return Math.sqrt(dx * dx + dy * dy) || 1;
    }

    function cutTees(segments) {
        if (!segments || segments.length > 1400) return null;
        var list = segments.map(function (seg) {
            return {
                x1: seg.x1, y1: seg.y1, x2: seg.x2, y2: seg.y2,
                kind: seg.kind || 'wall', thick: seg.thick || 8,
                name: seg.name || '', color: seg.color || '',
                source: seg.source != null ? seg.source : (seg.index != null ? seg.index : -1)
            };
        });
        var changed = false;
        var real = false;
        var guard = 0;
        while (guard < 6) {
            guard += 1;
            var snaps = [];
            var cuts = {};
            function addCut(index, t, x, y) {
                if (index < 0 || !list[index]) return;
                var host = list[index];
                var span = Math.sqrt((host.x2 - host.x1) * (host.x2 - host.x1) + (host.y2 - host.y1) * (host.y2 - host.y1)) || 1;
                var edge = 2 / span;
                if (t <= edge || t >= 1 - edge) return;
                var bucket = cuts[index] || (cuts[index] = []);
                var n;
                for (n = 0; n < bucket.length; n++) {
                    if (Math.abs(bucket[n].t - t) * span < 2) return;
                }
                bucket.push({ t: t, x: x, y: y });
            }
            var i;
            var j;
            for (i = 0; i < list.length; i++) {
                for (j = i + 1; j < list.length; j++) {
                    var left = list[i];
                    var right = list[j];
                    [[right, left, i], [left, right, j]].forEach(function (pair) {
                        var stem = pair[0];
                        var bar = pair[1];
                        var barIndex = pair[2];
                        var parallel = teeTurn(stem, bar) < 18;
                        if (parallel) {
                            var linked = function (ax, ay, bx, by) {
                                return (ax - bx) * (ax - bx) + (ay - by) * (ay - by) <= 1;
                            };
                            if (linked(stem.x1, stem.y1, bar.x1, bar.y1) || linked(stem.x1, stem.y1, bar.x2, bar.y2) || linked(stem.x2, stem.y2, bar.x1, bar.y1) || linked(stem.x2, stem.y2, bar.x2, bar.y2)) return;
                            [[stem.x1, stem.y1, 0], [stem.x2, stem.y2, 1]].forEach(function (end) {
                                var hit = teeProject(bar.x1, bar.y1, bar.x2, bar.y2, end[0], end[1]);
                                if (!hit || hit.dist > 5 || (hit.t >= 0.08 && hit.t <= 0.92)) return;
                                var d1 = Math.sqrt((end[0] - bar.x1) * (end[0] - bar.x1) + (end[1] - bar.y1) * (end[1] - bar.y1));
                                var d2 = Math.sqrt((end[0] - bar.x2) * (end[0] - bar.x2) + (end[1] - bar.y2) * (end[1] - bar.y2));
                                if (Math.min(d1, d2) > 22) return;
                                var tx = d1 < d2 ? bar.x1 : bar.x2;
                                var ty = d1 < d2 ? bar.y1 : bar.y2;
                                var mx = (end[0] + tx) / 2;
                                var my = (end[1] + ty) / 2;
                                snaps.push({ seg: stem, end: end[2], x: mx, y: my });
                                snaps.push({ seg: bar, end: d1 < d2 ? 0 : 1, x: mx, y: my });
                            });
                            return;
                        }
                        var meet = lineCross(stem, bar);
                        if (!meet) return;
                        var stemLen = segLen(stem);
                        var barLen = segLen(bar);
                        var stemPast = meet.t < 0 || meet.t > 1;
                        var stemGap = stemPast ? (meet.t < 0 ? -meet.t : meet.t - 1) * stemLen : 0;
                        var barInterior = meet.u > 2 / barLen && meet.u < 1 - 2 / barLen;
                        if (barInterior && stemGap <= 22 && (stemPast || meet.t <= 0.001 || meet.t >= 0.999)) {
                            if (stemPast) snaps.push({ seg: stem, end: meet.t < 0 ? 0 : 1, x: meet.x, y: meet.y });
                            addCut(barIndex, meet.u, meet.x, meet.y);
                            return;
                        }
                        if (stemPast && stemGap <= 22) {
                            var barGap = meet.u < 0 ? -meet.u * barLen : (meet.u > 1 ? (meet.u - 1) * barLen : 0);
                            if (barGap <= 22) snaps.push({ seg: stem, end: meet.t < 0 ? 0 : 1, x: meet.x, y: meet.y });
                        }
                    });
                    var cross = teeCross(left, right);
                    if (cross) {
                        addCut(i, cross.t, cross.x, cross.y);
                        addCut(j, cross.u, cross.x, cross.y);
                    }
                }
            }
            var cutKeys = [];
            for (var key in cuts) {
                if (Object.prototype.hasOwnProperty.call(cuts, key)) cutKeys.push(key);
            }
            if (!snaps.length && !cutKeys.length) break;
            changed = true;
            snaps.forEach(function (snap) {
                if (snap.seg.source >= 0) real = true;
                if (snap.end) {
                    snap.seg.x2 = snap.x;
                    snap.seg.y2 = snap.y;
                } else {
                    snap.seg.x1 = snap.x;
                    snap.seg.y1 = snap.y;
                }
            });
            cutKeys.map(Number).sort(function (a, b) { return b - a; }).forEach(function (index) {
                var seg = list[index];
                if (!seg) return;
                var points = [{ x: seg.x1, y: seg.y1 }].concat(cuts[index].slice().sort(function (a, b) {
                    return a.t - b.t;
                })).concat([{ x: seg.x2, y: seg.y2 }]);
                var pieces = [];
                var k;
                for (k = 0; k < points.length - 1; k++) {
                    var ax = points[k].x;
                    var ay = points[k].y;
                    var bx = points[k + 1].x;
                    var by = points[k + 1].y;
                    if ((ax - bx) * (ax - bx) + (ay - by) * (ay - by) < 4) continue;
                    pieces.push({
                        x1: ax, y1: ay, x2: bx, y2: by,
                        kind: seg.kind, thick: seg.thick, name: seg.name, color: seg.color, source: seg.source
                    });
                }
                if (pieces.length < 2) return;
                if (seg.source >= 0) real = true;
                var args = [index, 1].concat(pieces);
                Array.prototype.splice.apply(list, args);
            });
        }
        return changed && real ? list : null;
    }

    function planArea(points) {
        return Math.abs(planSigned(points));
    }

    function planSigned(points) {
        var sum = 0;
        var i;
        for (i = 0; i < points.length; i++) {
            var a = points[i];
            var b = points[(i + 1) % points.length];
            sum += a.x * b.y - b.x * a.y;
        }
        return sum / 2;
    }

    function faceBreadth(points) {
        var area = planArea(points);
        var around = 0;
        var i;
        for (i = 0; i < points.length; i++) {
            var a = points[i];
            var b = points[(i + 1) % points.length];
            var dx = b.x - a.x;
            var dy = b.y - a.y;
            around += Math.sqrt(dx * dx + dy * dy);
        }
        if (around < 1) return 0;
        return (2 * area) / around;
    }

    function planFaces(segments) {
        if (!segments || segments.length > 1400) return [];
        var nodes = [];
        var adj = [];
        function nodeAt(x, y) {
            var i;
            for (i = 0; i < nodes.length; i++) {
                var dx = nodes[i].x - x;
                var dy = nodes[i].y - y;
                if (dx * dx + dy * dy <= 16 * 16) return i;
            }
            nodes.push({ x: x, y: y });
            adj.push([]);
            return nodes.length - 1;
        }
        (segments || []).forEach(function (seg) {
            var a = nodeAt(seg.x1, seg.y1);
            var b = nodeAt(seg.x2, seg.y2);
            if (a === b) return;
            if (adj[a].indexOf(b) < 0) adj[a].push(b);
            if (adj[b].indexOf(a) < 0) adj[b].push(a);
        });
        var half = [];
        var i;
        var j;
        for (i = 0; i < nodes.length; i++) {
            for (j = 0; j < adj[i].length; j++) {
                var to = adj[i][j];
                half.push({
                    from: i,
                    to: to,
                    ang: Math.atan2(nodes[to].y - nodes[i].y, nodes[to].x - nodes[i].x)
                });
            }
        }
        var outgoing = [];
        for (i = 0; i < nodes.length; i++) outgoing.push([]);
        half.forEach(function (edge, id) { outgoing[edge.from].push(id); });
        outgoing.forEach(function (list) {
            list.sort(function (left, right) { return half[left].ang - half[right].ang; });
        });
        var used = {};
        var faces = [];
        half.forEach(function (edge, startId) {
            if (used[startId] || faces.length >= 80) return;
            var loop = [];
            var id = startId;
            var guard = 0;
            var closed = false;
            while (guard < half.length + 2) {
                if (used[id] && id !== startId) break;
                used[id] = true;
                loop.push(half[id].from);
                var arrive = half[id].to;
                var list = outgoing[arrive] || [];
                var rev = -1;
                var k;
                for (k = 0; k < list.length; k++) {
                    if (half[list[k]].to === half[id].from) rev = k;
                }
                if (rev < 0 || !list.length) break;
                id = list[(rev - 1 + list.length) % list.length];
                guard++;
                if (id === startId) {
                    closed = true;
                    break;
                }
            }
            if (!closed || loop.length < 3) return;
            var pts = loop.map(function (nid) { return { x: nodes[nid].x, y: nodes[nid].y }; });
            if (planSigned(pts) > 2500 && faceBreadth(pts) >= 24) faces.push(pts);
        });
        return faces.filter(function (face, index) {
            var area = planArea(face);
            var holds = false;
            faces.forEach(function (other, otherIndex) {
                if (otherIndex === index) return;
                var cx = 0;
                var cy = 0;
                var n;
                for (n = 0; n < other.length; n++) { cx += other[n].x; cy += other[n].y; }
                cx /= other.length;
                cy /= other.length;
                if (planInside(cx, cy, face) && planArea(other) < area * 0.95) holds = true;
            });
            return !holds;
        });
    }

    function planInside(x, y, points) {
        var inside = false;
        var i;
        for (i = 0; i < points.length; i++) {
            var a = points[i];
            var b = points[(i + 1) % points.length];
            if (((a.y > y) !== (b.y > y)) && (x < (b.x - a.x) * (y - a.y) / ((b.y - a.y) || 1) + a.x)) inside = !inside;
        }
        return inside;
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
        if (dxfBody(text)) return importDxf(decodeDxf(raw, text));
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
        if (lower.indexOf('.dwg') >= 0 || (text.indexOf('AC10') === 0 && !dxfBody(text))) return importDwg(text);
        return importDxf(decodeDxf(raw, text));
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
        cutTees: cutTees,
        planFaces: planFaces,
        scanJpeg: scanJpeg,
        bytesOf: bytesOf
    };
}());
