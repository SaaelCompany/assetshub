/* Built-in plan and rack symbols. Sizes are centimetres. A rack unit is 4.45 cm, a 19-inch face is 48.3 cm wide. */
(function (root) {
    var INK = '#172b4d';
    var METAL = '#5e6c84';
    var FACE = '#f4f5f7';
    var EAR = '#dfe1e6';
    var BLUE = '#0747a6';
    var BLUEF = '#deebff';
    var GREEN = '#006644';
    var GREENF = '#e3fcef';
    var AMBER = '#ff8b00';
    var RED = '#de350b';
    var PURPLE = '#403294';
    var PURPLEF = '#eae6ff';
    var DARK = '#091e42';
    var RU = 48.3;

    function n(value) {
        return Math.round(value * 100) / 100;
    }

    function rect(x, y, w, h, stroke, fill, width) {
        return { t: 'r', x: n(x), y: n(y), w: n(w), h: n(h), c: stroke || INK, f: fill || '', s: width == null ? 0.3 : width };
    }

    function oval(x, y, w, h, stroke, fill, width) {
        return { t: 'e', x: n(x), y: n(y), w: n(w), h: n(h), c: stroke || INK, f: fill || '', s: width == null ? 0.25 : width };
    }

    function line(x1, y1, x2, y2, stroke, width) {
        return { t: 'l', x1: n(x1), y1: n(y1), x2: n(x2), y2: n(y2), c: stroke || INK, s: width == null ? 0.25 : width };
    }

    function arc(cx, cy, radius, start, end, steps, stroke, width) {
        var points = [];
        var i;
        for (i = 0; i <= steps; i++) {
            var angle = start + (end - start) * (i / steps);
            points.push({ x: n(cx + Math.cos(angle) * radius), y: n(cy + Math.sin(angle) * radius) });
        }
        return { t: 'p', points: points, c: stroke || INK, s: width == null ? 0.3 : width };
    }

    function uHeight(units) {
        return n(4.45 * units);
    }

    function screw(parts, x, y) {
        parts.push(oval(x, y, 0.7, 0.7, METAL, '#ffffff', 0.12));
        parts.push(line(x + 0.14, y + 0.35, x + 0.56, y + 0.35, METAL, 0.08));
        parts.push(line(x + 0.35, y + 0.14, x + 0.35, y + 0.56, METAL, 0.08));
    }

    function chassis(units, fill) {
        var height = uHeight(units);
        var parts = [
            rect(0, 0, RU, height, INK, fill || FACE, 0.4),
            rect(1.95, 0.28, RU - 3.9, Math.max(0.5, height - 0.56), METAL, '', 0.1),
            rect(0, 0, 1.7, height, METAL, EAR, 0.18),
            rect(RU - 1.7, 0, 1.7, height, METAL, EAR, 0.18)
        ];
        screw(parts, 0.45, 0.4);
        screw(parts, 0.45, height - 1.1);
        screw(parts, RU - 1.2, 0.4);
        screw(parts, RU - 1.2, height - 1.1);
        if (units >= 2) {
            screw(parts, 0.45, height / 2 - 0.35);
            screw(parts, RU - 1.2, height / 2 - 0.35);
        }
        return { w: RU, h: height, parts: parts };
    }

    function leds(parts, x, y, count, colors) {
        var i;
        for (i = 0; i < count; i++) {
            var color = colors ? colors[i % colors.length] : GREENF.replace('#e3fcef', '#36b37e');
            if (!colors) color = i % 3 === 2 ? AMBER : '#36b37e';
            parts.push(oval(x + i * 0.85, y, 0.55, 0.55, color, color, 0.05));
        }
    }

    function ports(parts, x, y, width, height, count, rows, color) {
        var used = rows || (count > 12 ? 2 : 1);
        if (used < 1) used = 1;
        var cols = Math.ceil(count / used);
        var gapX = count > 24 ? 0.14 : 0.26;
        var gapY = 0.16;
        var portH = Math.min(count > 24 ? 1.2 : 1.7, (height - gapY * (used - 1)) / used);
        var portW = Math.min(count > 24 ? 1.35 : 2.05, (width - gapX * Math.max(0, cols - 1)) / cols);
        if (!(portW > 0.35)) portW = 0.35;
        if (!(portH > 0.3)) portH = 0.3;
        var pins = count <= 16 && portW >= 1.2 && portH >= 0.95;
        var led = !pins && count <= 32 && portW >= 0.7;
        var row;
        var col;
        var drawn = 0;
        for (row = 0; row < used; row++) {
            for (col = 0; col < cols && drawn < count; col++) {
                var px = x + col * (portW + gapX);
                var py = y + row * (portH + gapY);
                parts.push(rect(px, py, portW, portH, color, '#fafbfc', 0.1));
                parts.push(rect(px + portW * 0.12, py + portH * 0.16, portW * 0.76, portH * 0.5, color, '#172b4d', 0.04));
                parts.push(rect(px + portW * 0.3, py + portH * 0.74, portW * 0.4, Math.max(0.08, portH * 0.14), color, color, 0.03));
                if (pins) {
                    var pin;
                    for (pin = 0; pin < 4; pin++) {
                        parts.push(line(px + portW * (0.24 + pin * 0.15), py + portH * 0.26, px + portW * (0.24 + pin * 0.15), py + portH * 0.56, '#ffab00', 0.04));
                    }
                } else if (led) {
                    parts.push(oval(px + portW * 0.72, py + portH * 0.08, Math.min(0.28, portW * 0.2), Math.min(0.28, portH * 0.2), '#36b37e', '#36b37e', 0.02));
                }
                drawn++;
            }
        }
    }

    function cages(parts, x, y, count, tall) {
        var i;
        var cageH = tall ? 2.6 : 1.35;
        for (i = 0; i < count; i++) {
            var px = x + i * 2.35;
            parts.push(rect(px, y, 2.1, cageH, DARK, '#253858', 0.12));
            parts.push(rect(px + 0.28, y + 0.22, 1.54, cageH * 0.42, '#ffab00', '#fff7e6', 0.05));
            parts.push(line(px + 0.35, y + cageH - 0.28, px + 1.75, y + cageH - 0.28, METAL, 0.08));
            parts.push(line(px + 1.55, y + cageH * 0.55, px + 1.55, y + cageH - 0.18, '#c1c7d0', 0.06));
        }
    }

    function vents(parts, x, y, width, height, step) {
        var i;
        parts.push(rect(x, y, width, height, METAL, '#ebecf0', 0.12));
        for (i = y + step; i < y + height - 0.08; i += step) {
            parts.push(line(x + 0.25, i, x + width - 0.25, i, METAL, 0.08));
        }
    }

    function bay(parts, x, y, width, height) {
        parts.push(rect(x, y, width, height, INK, '#253858', 0.16));
        parts.push(rect(x + 0.22, y + 0.28, width - 0.44, height * 0.46, METAL, '#7a869a', 0.08));
        parts.push(oval(x + width * 0.28, y + height * 0.22, width * 0.38, height * 0.34, INK, '#505f79', 0.06));
        parts.push(rect(x + width * 0.18, y + height * 0.72, width * 0.46, Math.max(0.2, height * 0.12), METAL, EAR, 0.05));
        parts.push(oval(x + width - Math.min(1.05, width * 0.28), y + height - Math.min(0.95, height * 0.28), Math.min(0.5, width * 0.16), Math.min(0.5, height * 0.16), '#36b37e', '#36b37e', 0.04));
    }

    function bays(parts, x, y, width, height, count, cols) {
        var used = cols || count;
        var rows = Math.ceil(count / used);
        var gap = 0.28;
        var bayW = (width - gap * (used - 1)) / used;
        var bayH = (height - gap * (rows - 1)) / rows;
        var i;
        for (i = 0; i < count; i++) {
            bay(parts, x + (i % used) * (bayW + gap), y + Math.floor(i / used) * (bayH + gap), bayW, bayH);
        }
    }

    function display(parts, x, y, width, height) {
        parts.push(rect(x, y, width, height, DARK, '#071426', 0.12));
        parts.push(rect(x + 0.25, y + 0.25, width - 0.5, height - 0.5, '#4c9aff', '', 0.06));
        parts.push(line(x + 0.5, y + height * 0.68, x + width * 0.55, y + height * 0.32, '#4c9aff', 0.1));
        parts.push(line(x + width * 0.4, y + height * 0.72, x + width * 0.82, y + height * 0.46, '#36b37e', 0.08));
    }

    function power(parts, x, y) {
        parts.push(oval(x, y, 1.1, 1.1, GREEN, '#ffffff', 0.12));
        parts.push(line(x + 0.55, y + 0.25, x + 0.55, y + 0.6, GREEN, 0.1));
    }

    function handle(parts, x, y, width, height) {
        parts.push(rect(x, y, width, height, METAL, '#ffffff', 0.14));
        parts.push(line(x + 0.3, y + height / 2, x + width - 0.3, y + height / 2, METAL, 0.12));
    }

    function fan(parts, x, y, size) {
        var cx = x + size / 2;
        var cy = y + size / 2;
        var blade;
        parts.push(oval(x, y, size, size, METAL, '#ebecf0', 0.16));
        parts.push(oval(x + size * 0.07, y + size * 0.07, size * 0.86, size * 0.86, METAL, '#f7f8f9', 0.08));
        for (blade = 0; blade < 5; blade++) {
            var angle = blade * Math.PI * 2 / 5 - 0.4;
            parts.push(line(cx, cy, cx + Math.cos(angle) * size * 0.36, cy + Math.sin(angle) * size * 0.36, BLUE, Math.max(0.06, size * 0.015)));
        }
        parts.push(oval(cx - size * 0.12, cy - size * 0.12, size * 0.24, size * 0.24, BLUE, BLUEF, 0.08));
        screw(parts, x + size * 0.08, y + size * 0.08);
    }

    function unit(id, name, units, fill, draw) {
        var box = chassis(units, fill);
        if (draw) draw(box.parts, box.w, box.h);
        return { id: 'net-' + id, name: name, w: box.w, h: box.h, parts: box.parts };
    }

    function plan(id, name, width, height, parts) {
        return { id: 'net-' + id, name: name, w: n(width), h: n(height), parts: parts };
    }

    function posts(parts, width, depth) {
        var size = Math.max(3, Math.min(width, depth) * 0.06);
        [[1.2, 1.2], [width - size - 1.2, 1.2], [1.2, depth - size - 1.2], [width - size - 1.2, depth - size - 1.2]].forEach(function (at) {
            parts.push(rect(at[0], at[1], size, size, INK, DARK, 0.2));
        });
    }

    function rackFrame(width, depth, open) {
        var parts = [rect(0, 0, width, depth, INK, open ? '#fafbfc' : '#f4f5f7', 0.7)];
        posts(parts, width, depth);
        var railX = width * 0.18;
        var railW = width * 0.64;
        parts.push(rect(railX, depth * 0.08, 1.1, depth * 0.84, METAL, EAR, 0.15));
        parts.push(rect(railX + railW, depth * 0.08, 1.1, depth * 0.84, METAL, EAR, 0.15));
        var i;
        for (i = 0; i < 18; i++) {
            var y = depth * 0.1 + i * (depth * 0.8 / 18);
            parts.push(line(railX, y, railX + railW, y, '#c1c7d0', 0.12));
        }
        return parts;
    }

    function portWord(count) {
        var ten = count % 10;
        var hundred = count % 100;
        if (ten === 1 && hundred !== 11) return 'порт';
        if (ten >= 2 && ten <= 4 && (hundred < 12 || hundred > 14)) return 'порта';
        return 'портов';
    }

    function clampPorts(count) {
        var next = Math.round(Number(count) || 0);
        if (next < 2) next = 2;
        if (next > 52) next = 52;
        return next;
    }

    function drawSwitchFace(parts, w, h, count, face) {
        var color = (face === 'poe' || face === 'cam') ? AMBER : BLUE;
        var rows = count > 12 ? 2 : 1;
        var sfp = 0;
        if (face === 'agg' || face === 'core') sfp = count >= 24 ? 4 : 2;
        else if (face !== 'cam' && face !== 'ind') sfp = count >= 12 ? 4 : (count >= 8 ? 2 : 0);
        if (face === 'core') {
            var cards = Math.max(1, Math.min(4, Math.ceil(count / 12)));
            var per = Math.ceil(count / cards);
            var cardH = (h - 1.4) / cards;
            var card;
            for (card = 0; card < cards; card++) {
                var left = count - card * per;
                var n = left > per ? per : left;
                if (n < 1) break;
                parts.push(rect(2.2, 0.5 + card * cardH, w - 4.4, cardH - 0.3, INK, FACE, 0.1));
                parts.push(rect(2.5, 0.7 + card * cardH, 0.35, cardH - 0.7, color, color, 0.04));
                ports(parts, 3.2, 0.75 + card * cardH, w - 16, cardH - 1, n, n > 8 ? 2 : 1, color);
                cages(parts, w - 11.2, 0.85 + card * cardH, 2, cardH > 4);
            }
            return;
        }
        if (h >= 2.4) leds(parts, 2.4, 0.32, h >= 6 ? 5 : 3, face === 'poe' || face === 'cam' ? [AMBER, '#36b37e', '#36b37e'] : null);
        var portY = h > 3 ? 1.15 : 0.55;
        var sfpW = sfp ? sfp * 2.4 + 0.6 : 0;
        ports(parts, 2.4, portY, Math.max(4, w - 4.6 - sfpW), Math.max(0.8, h - portY - 0.4), count, rows, color);
        if (sfp) cages(parts, w - 2.1 - sfpW, portY + 0.1, sfp, h >= 6);
        if (face === 'poe' || face === 'cam') parts.push(rect(w - 7.2, 0.28, 3.6, 0.5, AMBER, AMBER, 0.04));
        if (face === 'access') parts.push(rect(w - 7.2, 0.28, 3.2, 0.45, BLUE, BLUE, 0.04));
    }

    function buildSwitch(id, name, count, face) {
        count = clampPorts(count);
        face = face || 'rack';
        if (face === 'ind') {
            var width = count > 16 ? 32 : 18;
            var depth = count > 24 ? 16 : 12;
            var body = [rect(0, 0, width, depth, INK, '#253858', 0.35), rect(0, 0, width, 1.5, AMBER, AMBER, 0.08)];
            screw(body, 0.6, 0.4);
            screw(body, width - 1.4, 0.4);
            ports(body, 1, 2.4, width - 2, depth - 3.4, count, count > 8 ? 2 : 1, AMBER);
            var symbol = plan(id, name, width, depth, body);
            symbol.ports = count;
            symbol.face = face;
            return symbol;
        }
        var units = 1;
        if (face === 'core') units = Math.max(2, Math.min(6, Math.ceil(count / 8)));
        else if (face === 'agg' && count > 16) units = 2;
        var fill = (face === 'poe' || face === 'cam') ? '#fff7e6' : BLUEF;
        var symbolUnit = unit(id, name, units, fill, function (parts, w, h) {
            drawSwitchFace(parts, w, h, count, face);
        });
        symbolUnit.ports = count;
        symbolUnit.face = face;
        return symbolUnit;
    }

    function racks() {
        var open = rackFrame(60, 110, true);
        var closed = rackFrame(80, 120, false);
        closed.push(rect(6, 112, 68, 6, INK, BLUEF, 0.25));
        closed.push(rect(36, 113.2, 2.2, 3.4, INK, DARK, 0.12));
        closed.push(rect(6, 2, 68, 3.2, INK, EAR, 0.18));
        closed.push(oval(14, 116, 3, 3, METAL, DARK, 0.1));
        closed.push(oval(63, 116, 3, 3, METAL, DARK, 0.1));
        var rails = [rect(0, 0, 48.3, 186.9, INK, FACE, 0.4)];
        var hole;
        rails.push(rect(2, 0, 6, 186.9, METAL, EAR, 0.2));
        rails.push(rect(40.3, 0, 6, 186.9, METAL, EAR, 0.2));
        for (hole = 0; hole < 42; hole++) {
            var hy = 2.2 + hole * 4.4;
            rails.push(rect(3.6, hy, 1.3, 1.3, INK, '#ffffff', 0.08));
            rails.push(rect(43.4, hy, 1.3, 1.3, INK, '#ffffff', 0.08));
        }
        var guides = [
            rect(0, 0, 48.3, 8, INK, FACE, 0.35),
            rect(2, 1.2, 44.3, 1.6, METAL, EAR, 0.12),
            rect(2, 5.2, 44.3, 1.6, METAL, EAR, 0.12)
        ];
        var step;
        for (step = 0; step < 8; step++) {
            guides.push(line(4 + step * 5.2, 1.2, 4 + step * 5.2, 2.8, METAL, 0.08));
            guides.push(line(4 + step * 5.2, 5.2, 4 + step * 5.2, 6.8, METAL, 0.08));
        }
        var plinth = [rect(0, 0, 86, 114, INK, '#ebecf0', 0.5), rect(4, 4, 78, 106, METAL, FACE, 0.25)];
        posts(plinth, 86, 114);
        var roof = [rect(0, 0, 80, 110, INK, FACE, 0.5)];
        fan(roof, 10, 12, 22);
        fan(roof, 48, 12, 22);
        fan(roof, 10, 62, 22);
        fan(roof, 48, 62, 22);
        roof.push(rect(30, 96, 20, 8, INK, DARK, 0.16));
        var sides = [rect(0, 0, 80, 110, INK, FACE, 0.45), rect(0, 0, 3, 110, INK, DARK, 0.16), rect(77, 0, 3, 110, INK, DARK, 0.16)];
        var i;
        for (i = 0; i < 16; i++) sides.push(rect(8, 4 + i * 6.4, 64, 1.3, METAL, EAR, 0.08));
        screw(sides, 3.4, 3);
        screw(sides, 74.5, 3);
        screw(sides, 3.4, 105);
        screw(sides, 74.5, 105);
        var doors = [
            rect(0, 0, 80, 200, INK, FACE, 0.6),
            line(40, 2, 40, 198, METAL, 0.25),
            rect(6, 16, 28, 150, BLUE, BLUEF, 0.25),
            rect(46, 16, 28, 150, BLUE, BLUEF, 0.25),
            rect(33, 96, 3, 16, INK, DARK, 0.15),
            rect(44, 96, 3, 16, INK, DARK, 0.15)
        ];
        for (i = 0; i < 6; i++) {
            doors.push(oval(2.2, 20 + i * 28, 2.2, 2.2, METAL, EAR, 0.1));
            doors.push(oval(75.6, 20 + i * 28, 2.2, 2.2, METAL, EAR, 0.1));
        }
        for (i = 0; i < 7; i++) {
            doors.push(line(8, 28 + i * 18, 32, 28 + i * 18, BLUE, 0.08));
            doors.push(line(48, 28 + i * 18, 72, 28 + i * 18, BLUE, 0.08));
            doors.push(line(12 + i * 3, 20, 12 + i * 3, 160, BLUE, 0.06));
            doors.push(line(52 + i * 3, 20, 52 + i * 3, 160, BLUE, 0.06));
        }
        var floor = [];
        var tile;
        for (tile = 0; tile < 4; tile++) {
            var tx = (tile % 2) * 60;
            var ty = Math.floor(tile / 2) * 60;
            floor.push(rect(tx + 1, ty + 1, 58, 58, INK, tile === 3 ? '#deebff' : FACE, 0.4));
            floor.push(line(tx + 4, ty + 30, tx + 56, ty + 30, '#c1c7d0', 0.12));
            floor.push(line(tx + 30, ty + 4, tx + 30, ty + 56, '#c1c7d0', 0.12));
        }
        floor.push(rect(68, 68, 16, 16, BLUE, '#ffffff', 0.2));
        var gland = [rect(0, 0, 36, 10, INK, FACE, 0.35), rect(2, 2, 32, 6, METAL, DARK, 0.15)];
        for (i = 0; i < 16; i++) gland.push(line(3 + i * 2, 2.4, 3 + i * 2, 7.6, '#c1c7d0', 0.08));
        var vertical = [rect(0, 0, 12, 186.9, INK, FACE, 0.4), rect(5.4, 0, 1.2, 186.9, METAL, EAR, 0.12)];
        for (i = 0; i < 16; i++) {
            var fy = 3 + i * 11.4;
            vertical.push(rect(0.5, fy, 4.4, 7.2, INK, '#ffffff', 0.1));
            vertical.push(rect(7.1, fy, 4.4, 7.2, INK, '#ffffff', 0.1));
            vertical.push(line(0.8, fy + 1.2, 4.4, fy + 5.6, METAL, 0.1));
            vertical.push(line(11.2, fy + 1.2, 7.6, fy + 5.6, METAL, 0.1));
        }
        return [
            plan('rack-dyn', 'Динамический блок стойки', 60, 110, open),
            plan('cab-dyn', 'Динамический блок шкафа', 80, 120, closed),
            plan('rack-open', 'Открытая стойка', 60, 100, rackFrame(60, 100, true)),
            plan('cab-closed', 'Закрытый шкаф', 80, 110, (function () {
                var body = rackFrame(80, 110, false);
                body.push(rect(8, 100, 64, 7, INK, BLUEF, 0.25));
                body.push(rect(38, 101.4, 3, 4, INK, DARK, 0.12));
                body.push(rect(8, 3, 64, 4, INK, EAR, 0.16));
                return body;
            })()),
            plan('rails-19', '19-дюймовые монтажные профили', 48.3, 186.9, rails),
            plan('guides', 'Направляющие', 48.3, 8, guides),
            plan('plinth', 'Цоколь', 86, 114, plinth),
            plan('roof', 'Крыша шкафа', 80, 110, roof),
            plan('sides', 'Боковые панели', 80, 110, sides),
            plan('doors', 'Дверцы', 80, 200, doors),
            plan('raised-floor', 'Фальшпол', 120, 120, floor),
            plan('gland', 'Кабельный ввод', 36, 10, gland),
            plan('vert-org', 'Вертикальный органайзер', 12, 186.9, vertical),
            unit('horz-org', 'Горизонтальный органайзер', 1, FACE, function (parts, w, h) {
                var ring;
                for (ring = 0; ring < 5; ring++) {
                    parts.push(oval(4 + ring * 8, 0.7, 5.2, h - 1.4, BLUE, '#ffffff', 0.16));
                }
            }),
            unit('cord-org', 'Кабельный органайзер для патч-кордов', 2, FACE, function (parts, w, h) {
                var finger;
                for (finger = 0; finger < 10; finger++) {
                    parts.push(rect(3 + finger * 4.3, 0.8, 1.1, h - 1.6, METAL, EAR, 0.12));
                    parts.push(line(3 + finger * 4.3, 1.4, 4.1 + finger * 4.3, 1.4, INK, 0.1));
                }
            }),
            unit('blank-1', 'Заглушка 1U', 1, FACE, function (parts, w, h) {
                parts.push(rect(16, h / 2 - 0.25, 16, 0.5, METAL, EAR, 0.1));
            }),
            unit('blank-2', 'Заглушка 2U', 2, FACE, function (parts, w, h) {
                vents(parts, 8, 1.2, 32, h - 2.4, 0.7);
            }),
            unit('fans', 'Вентиляторный модуль', 1, FACE, function (parts, w, h) {
                fan(parts, 4, 0.4, h - 0.8);
                fan(parts, 14, 0.4, h - 0.8);
                fan(parts, 24, 0.4, h - 0.8);
                fan(parts, 34, 0.4, h - 0.8);
            }),
            plan('thermostat', 'Термостат', 8, 12, (function () {
                var parts = [
                    rect(0, 0, 8, 12, INK, FACE, 0.3),
                    oval(1.4, 1.6, 5.2, 5.2, AMBER, '#ffffff', 0.16),
                    line(4, 4.2, 5.6, 2.8, INK, 0.12)
                ];
                display(parts, 1.2, 8, 5.6, 2.6);
                return parts;
            })()),
            plan('crac', 'Кондиционер', 100, 70, (function () {
                var parts = [rect(0, 0, 100, 70, INK, FACE, 0.6), rect(4, 46, 92, 20, METAL, EAR, 0.25)];
                var g;
                for (g = 0; g < 12; g++) parts.push(line(8, 50 + g * 1.2, 92, 50 + g * 1.2, METAL, 0.1));
                fan(parts, 8, 6, 26);
                fan(parts, 38, 6, 26);
                fan(parts, 68, 6, 26);
                return parts;
            })()),
            plan('climate', 'Датчик температуры и влажности', 8, 8, [
                oval(0.4, 0.4, 7.2, 7.2, INK, FACE, 0.3),
                oval(2.3, 1.6, 3.4, 3.4, BLUE, BLUEF, 0.14),
                line(4, 5.4, 4, 6.8, AMBER, 0.16)
            ]),
            plan('pdu', 'PDU', 8, 160, (function () {
                var parts = [rect(0, 0, 8, 160, INK, DARK, 0.4), rect(1, 1.2, 6, 6, METAL, FACE, 0.12)];
                var outlet;
                for (outlet = 0; outlet < 20; outlet++) {
                    parts.push(rect(1.3, 10 + outlet * 7.2, 5.4, 4.2, '#ffab00', '#253858', 0.1));
                    parts.push(line(2.2, 11.2 + outlet * 7.2, 5.8, 11.2 + outlet * 7.2, '#ffab00', 0.08));
                }
                return parts;
            })()),
            unit('pdu-h', 'Блок распределения питания', 2, DARK, function (parts, w, h) {
                var breaker;
                for (breaker = 0; breaker < 8; breaker++) {
                    parts.push(rect(3 + breaker * 4, 1.1, 3, 2.4, METAL, FACE, 0.1));
                    parts.push(line(4.5 + breaker * 4, 1.4, 4.5 + breaker * 4, 2.6, INK, 0.1));
                }
                ports(parts, 3, 4.4, 40, 3.6, 10, 1, AMBER);
            }),
            unit('ups', 'ИБП', 3, FACE, function (parts, w, h) {
                display(parts, 3, 1.4, 14, 6);
                parts.push(rect(19, 2, 16, 2.2, GREEN, GREENF, 0.12));
                parts.push(rect(19, 2, 11, 2.2, GREEN, '#36b37e', 0.06));
                vents(parts, 19, 5.4, 24, 5, 0.8);
                power(parts, 44, 2);
            }),
            unit('battery', 'Батарейный модуль', 3, FACE, function (parts, w, h) {
                var cell;
                for (cell = 0; cell < 8; cell++) {
                    parts.push(rect(3 + cell * 5.2, 2, 4.4, h - 4, INK, '#253858', 0.12));
                    parts.push(line(5.2 + cell * 5.2, 3, 5.2 + cell * 5.2, h - 3, '#ffab00', 0.1));
                }
            }),
            plan('breaker', 'Автомат защиты', 8, 10, [
                rect(0, 0, 8, 10, INK, FACE, 0.3),
                rect(2.4, 1.2, 3.2, 4.6, INK, EAR, 0.14),
                rect(3.3, 2.2, 1.4, 2.2, RED, RED, 0.08),
                rect(1.2, 7.2, 5.6, 1.6, METAL, DARK, 0.1)
            ]),
            plan('terminals', 'Клеммная колодка', 16, 5, (function () {
                var parts = [rect(0, 0, 16, 5, INK, FACE, 0.25)];
                var pin;
                for (pin = 0; pin < 8; pin++) {
                    parts.push(rect(0.6 + pin * 1.9, 0.6, 1.5, 3.8, METAL, EAR, 0.08));
                    parts.push(oval(0.95 + pin * 1.9, 1.8, 0.8, 0.8, INK, DARK, 0.06));
                }
                return parts;
            })())
        ];
    }

    function serverFace(parts, w, h, drives, cols) {
        bays(parts, 2.4, 0.8, w * 0.46, h - 1.6, drives, cols || Math.min(drives, 4));
        vents(parts, w * 0.56, 0.9, w * 0.28, h - 1.8, 0.55);
        power(parts, w - 4.2, h / 2 - 0.6);
        leds(parts, w - 8.5, 0.8, 3);
    }

    function servers() {
        return [
            unit('srv-1', 'Сервер 1U', 1, FACE, function (parts, w, h) { serverFace(parts, w, h, 4, 4); }),
            unit('srv-2', 'Сервер 2U', 2, FACE, function (parts, w, h) { serverFace(parts, w, h, 8, 4); }),
            unit('srv-4', 'Сервер 4U', 4, FACE, function (parts, w, h) { serverFace(parts, w, h, 12, 4); }),
            unit('blade-chassis', 'Blade-шасси', 10, FACE, function (parts, w, h) {
                var blade;
                var slot = (w - 4.4) / 14;
                for (blade = 0; blade < 14; blade++) {
                    var x = 2.2 + blade * slot;
                    parts.push(rect(x, 1, slot - 0.3, h - 2, INK, blade % 2 ? '#253858' : DARK, 0.12));
                    parts.push(rect(x + 0.35, 2, slot - 1, 6, METAL, EAR, 0.08));
                    parts.push(oval(x + slot * 0.28, h - 3.2, 0.7, 0.7, '#36b37e', '#36b37e', 0.04));
                }
            }),
            plan('blade', 'Лезвийный сервер', 3.2, 42, (function () {
                var parts = [rect(0, 0, 3.2, 42, INK, '#253858', 0.25), rect(0.45, 1.4, 2.3, 7, METAL, EAR, 0.1)];
                vents(parts, 0.45, 10, 2.3, 24, 1.2);
                handle(parts, 0.5, 36, 2.2, 3.2);
                return parts;
            })()),
            unit('video-srv', 'Сервер видеонаблюдения', 2, FACE, function (parts, w, h) {
                serverFace(parts, w, h, 4, 4);
                parts.push(rect(w - 12, h - 2.2, 6, 1.2, RED, RED, 0.06));
            }),
            unit('video-engine', 'Видеосервер', 2, '#fff7e6', function (parts, w, h) {
                display(parts, 3, 1.2, 16, h - 2.4);
                bays(parts, 21, 1.2, 18, h - 2.4, 4, 4);
                leds(parts, 41, 1.4, 3, [RED, '#36b37e', AMBER]);
            }),
            unit('video-store', 'Сервер хранения видео', 4, FACE, function (parts, w, h) {
                bays(parts, 2.4, 1, 28, h - 2, 16, 4);
                display(parts, 32, 1.4, 12, 5);
            }),
            unit('san-array', 'СХД', 3, PURPLEF, function (parts, w, h) {
                bays(parts, 2.4, 1, 30, h - 2, 12, 6);
                display(parts, 34, 1.6, 10, 4);
                parts.push(rect(34, 7, 10, 3, PURPLE, PURPLEF, 0.1));
            }),
            plan('nas', 'NAS', 22, 32, (function () {
                var parts = [rect(0, 0, 22, 32, INK, FACE, 0.4)];
                bays(parts, 2, 2, 18, 22, 4, 2);
                leds(parts, 3, 26, 3);
                power(parts, 16, 26);
                return parts;
            })()),
            unit('san', 'SAN', 2, PURPLEF, function (parts, w, h) {
                ports(parts, 3, 1.2, 28, h - 2.2, 16, 2, PURPLE);
                cages(parts, 33, 2.2, 4, true);
            }),
            unit('shelf', 'Дисковая полка', 3, FACE, function (parts, w, h) {
                bays(parts, 2.2, 1, w - 4.4, h - 2, 16, 8);
            }),
            unit('tape', 'Ленточная библиотека', 6, FACE, function (parts, w, h) {
                parts.push(rect(3, 1.2, 14, h - 2.4, INK, '#253858', 0.16));
                parts.push(rect(18, 1.2, 14, h - 2.4, INK, '#253858', 0.16));
                var slot;
                for (slot = 0; slot < 6; slot++) {
                    parts.push(rect(4, 2 + slot * 3.6, 12, 2.6, METAL, EAR, 0.08));
                    parts.push(rect(19, 2 + slot * 3.6, 12, 2.6, METAL, EAR, 0.08));
                }
                display(parts, 34, 2, 10, 6);
            }),
            unit('kvm-lcd', 'KVM-консоль', 1, FACE, function (parts, w, h) {
                display(parts, 3, 0.6, 28, h - 1.2);
                parts.push(rect(33, 1.2, 10, h - 2.4, METAL, EAR, 0.12));
            }),
            unit('kvm', 'KVM-переключатель', 1, FACE, function (parts, w, h) {
                ports(parts, 3, 1.1, 30, h - 2, 16, 2, BLUE);
                leds(parts, 36, 1.2, 4);
            }),
            unit('console', 'Консольный сервер', 1, FACE, function (parts, w, h) {
                ports(parts, 3, 0.8, 36, h - 1.5, 32, 2, GREEN);
            }),
            unit('term-srv', 'Терминальный сервер', 1, FACE, function (parts, w, h) {
                ports(parts, 3, 0.8, 36, h - 1.5, 16, 2, PURPLE);
                display(parts, 40, 0.8, 5, h - 1.6);
            })
        ];
    }

    function numberedSwitches() {
        return [2, 4, 5, 8, 12, 16, 24, 48, 52].map(function (count) {
            return buildSwitch('sw-' + count, 'Коммутатор ' + count + ' ' + portWord(count), count, 'rack');
        });
    }

    function network() {
        return [
            buildSwitch('sw-access', 'Коммутатор доступа', 24, 'access'),
            buildSwitch('sw-agg', 'Коммутатор агрегации', 24, 'agg'),
            buildSwitch('sw-core', 'Ядро сети', 48, 'core'),
            buildSwitch('sw-poe', 'PoE-коммутатор', 24, 'poe'),
            buildSwitch('sw-poe-cam', 'PoE-коммутатор для камер', 16, 'cam'),
            buildSwitch('sw-ind', 'Промышленный коммутатор', 8, 'ind')
        ].concat(numberedSwitches()).concat([
            unit('router', 'Маршрутизатор', 1, GREENF, function (parts, w, h) {
                ports(parts, 3, 1.3, 18, h - 1.8, 8, 2, GREEN);
                cages(parts, 24, 1.6, 4, false);
                display(parts, 36, 0.8, 8, h - 1.6);
            }),
            plan('router-soho', 'Роутер', 22, 14, (function () {
                var parts = [rect(0, 0, 22, 14, INK, GREENF, 0.35), oval(7, 1.2, 8, 8, GREEN, '#ffffff', 0.16)];
                ports(parts, 1.2, 10.2, 19, 2.6, 5, 1, GREEN);
                parts.push(line(11, 2.4, 11, 7.6, GREEN, 0.16));
                parts.push(line(8.4, 5.2, 13.6, 5.2, GREEN, 0.16));
                return parts;
            })()),
            unit('firewall', 'Межсетевой экран', 1, '#ffebe6', function (parts, w, h) {
                parts.push(rect(3, 0.45, 1.2, h - 0.9, RED, RED, 0.06));
                ports(parts, 6, 1.2, 22, h - 1.8, 8, 2, RED);
                cages(parts, 30, 1.5, 4, false);
                parts.push(rect(41, 1.2, 4, h - 2.4, INK, FACE, 0.12));
            }),
            unit('vpn', 'VPN-шлюз', 1, GREENF, function (parts, w, h) {
                ports(parts, 3, 1.3, 20, h - 1.8, 6, 2, GREEN);
                parts.push(rect(28, 0.9, 6, h - 1.8, INK, FACE, 0.14));
                parts.push(oval(30, 1.5, 2.2, 2.2, GREEN, GREENF, 0.1));
            }),
            unit('lb', 'Балансировщик нагрузки', 1, PURPLEF, function (parts, w, h) {
                ports(parts, 3, 1.3, 16, h - 1.8, 8, 2, PURPLE);
                ports(parts, 22, 1.3, 16, h - 1.8, 8, 2, BLUE);
                parts.push(line(20.2, h / 2, 21.6, h / 2, PURPLE, 0.16));
            }),
            unit('wifi-ctl', 'Контроллер Wi-Fi', 1, BLUEF, function (parts, w, h) {
                ports(parts, 3, 1.3, 18, h - 1.8, 8, 2, BLUE);
                fan(parts, 26, 0.5, h - 1);
                display(parts, 34, 0.7, 10, h - 1.4);
            }),
            plan('ap', 'Точка доступа', 15, 15, [
                oval(0.5, 0.5, 14, 14, INK, FACE, 0.35),
                oval(3.4, 3.4, 8.2, 8.2, BLUE, BLUEF, 0.16),
                line(7.5, 1.8, 7.5, 13.2, BLUE, 0.14),
                line(1.8, 7.5, 13.2, 7.5, BLUE, 0.14)
            ]),
            plan('ap-out', 'Уличная точка доступа', 9, 18, [
                rect(1.2, 2.6, 6.6, 14, INK, FACE, 0.28),
                rect(2.4, 0, 0.8, 3.4, METAL, DARK, 0.1),
                rect(5.8, 0, 0.8, 3.4, METAL, DARK, 0.1),
                oval(2.2, 5, 4.6, 4.6, BLUE, BLUEF, 0.14),
                rect(2, 11.2, 5, 3.6, METAL, EAR, 0.1)
            ]),
            plan('ap-ceiling', 'Потолочная точка доступа', 16, 16, [
                oval(0.35, 0.35, 15.3, 15.3, INK, FACE, 0.3),
                oval(2.6, 2.6, 10.8, 10.8, BLUE, '#ffffff', 0.16),
                oval(5.4, 5.4, 5.2, 5.2, BLUE, BLUEF, 0.12)
            ]),
            plan('ap-wall', 'Настенная точка доступа', 8, 12, (function () {
                var parts = [
                    rect(0, 1.4, 8, 10.6, INK, FACE, 0.28),
                    rect(3.3, 0, 1.4, 2.6, METAL, DARK, 0.1),
                    oval(1.6, 3.2, 4.8, 4.8, BLUE, BLUEF, 0.14)
                ];
                ports(parts, 0.6, 9.2, 6.8, 2, 2, 1, BLUE);
                return parts;
            })()),
            plan('wifi-ant', 'Антенна Wi-Fi', 8, 16, [
                line(4, 3, 4, 15.2, INK, 0.28),
                rect(3.1, 13, 1.8, 3, INK, DARK, 0.1),
                arc(4, 5.5, 2.1, Math.PI * 1.15, Math.PI * 1.85, 8, BLUE, 0.14),
                arc(4, 5.5, 3.4, Math.PI * 1.15, Math.PI * 1.85, 10, BLUE, 0.12)
            ]),
            plan('sfp', 'SFP-модуль', 1.4, 5.6, [
                rect(0, 0, 1.4, 5.6, INK, '#253858', 0.12),
                rect(0.25, 0.4, 0.9, 1.2, '#ffab00', '#fff7e6', 0.06),
                line(0.7, 2.2, 0.7, 5, METAL, 0.08)
            ]),
            plan('sfp-plus', 'SFP+', 1.4, 5.6, [
                rect(0, 0, 1.4, 5.6, INK, DARK, 0.12),
                rect(0.25, 0.4, 0.9, 1.2, BLUE, BLUEF, 0.06),
                line(0.7, 2.2, 0.7, 5, BLUE, 0.08)
            ]),
            plan('qsfp', 'QSFP', 1.8, 7.2, [
                rect(0, 0, 1.8, 7.2, INK, DARK, 0.14),
                rect(0.3, 0.4, 1.2, 1.6, PURPLE, PURPLEF, 0.06),
                line(0.45, 2.6, 0.45, 6.4, METAL, 0.08),
                line(1.35, 2.6, 1.35, 6.4, METAL, 0.08)
            ]),
            plan('media', 'Медиаконвертер', 8, 12, [
                rect(0, 0, 8, 12, INK, FACE, 0.3),
                rect(1, 1.2, 6, 3.2, BLUE, BLUEF, 0.12),
                rect(2.4, 6.2, 3.2, 1.6, '#ffab00', '#fff7e6', 0.1),
                oval(1.4, 9.2, 1.2, 1.2, '#36b37e', '#36b37e', 0.06),
                oval(5.2, 9.2, 1.2, 1.2, AMBER, AMBER, 0.06)
            ]),
            unit('patch', 'Патч-панель', 1, FACE, function (parts, w, h) {
                ports(parts, 3, 1.2, 42, h - 1.8, 24, 2, INK);
            }),
            unit('fiber-shelf', 'Оптическая полка', 1, '#e6fcff', function (parts, w, h) {
                var pair;
                for (pair = 0; pair < 12; pair++) {
                    parts.push(oval(3 + pair * 3.4, 1.1, 1.1, 1.1, BLUE, BLUEF, 0.08));
                    parts.push(oval(4.3 + pair * 3.4, 1.1, 1.1, 1.1, GREEN, GREENF, 0.08));
                }
            }),
            unit('odf-cross', 'Оптический кросс', 2, '#e6fcff', function (parts, w, h) {
                var pair;
                for (pair = 0; pair < 12; pair++) {
                    parts.push(rect(3 + pair * 3.6, 1.2, 2.8, h - 2.4, INK, FACE, 0.1));
                    parts.push(oval(3.5 + pair * 3.6, 2.2, 0.8, 0.8, BLUE, BLUE, 0.05));
                    parts.push(oval(4.5 + pair * 3.6, 2.2, 0.8, 0.8, GREEN, GREEN, 0.05));
                }
            }),
            unit('odf', 'ODF', 1, '#e6fcff', function (parts, w, h) {
                var pair;
                for (pair = 0; pair < 24; pair++) {
                    parts.push(oval(2.6 + pair * 1.85, 1.5, 1.2, 1.2, pair % 2 ? GREEN : BLUE, '#ffffff', 0.08));
                }
            })
        ]);
    }

    function irRing(parts, cx, cy, radius) {
        var i;
        for (i = 0; i < 8; i++) {
            var angle = i * Math.PI / 4;
            parts.push(oval(cx + Math.cos(angle) * radius - 0.35, cy + Math.sin(angle) * radius - 0.35, 0.7, 0.7, RED, '#ffbdad', 0.04));
        }
    }

    function camera(kind) {
        if (kind === 'dome') {
            var dome = [oval(0.3, 0.3, 13.4, 13.4, INK, FACE, 0.35), oval(1.2, 1.2, 11.6, 11.6, METAL, EAR, 0.12), oval(3.2, 3.2, 7.6, 7.6, DARK, '#071426', 0.16), oval(5.2, 5.2, 3.6, 3.6, BLUE, '#4c9aff', 0.1), oval(6.2, 6.2, 1.4, 1.4, '#ffffff', '#ffffff', 0.04)];
            irRing(dome, 7, 7, 4.6);
            return dome;
        }
        if (kind === 'ptz') {
            var ptz = [oval(0.2, 0.2, 17.6, 17.6, INK, EAR, 0.4), rect(7.4, 0, 3.2, 2.2, METAL, DARK, 0.12), oval(2.4, 2.4, 13.2, 13.2, DARK, '#253858', 0.18), oval(5.6, 5.2, 6.8, 6.8, BLUE, '#4c9aff', 0.12), oval(7.6, 7.2, 2.6, 2.6, '#ffffff', BLUEF, 0.06), rect(8.2, 15.2, 1.6, 2.4, METAL, DARK, 0.1)];
            irRing(ptz, 9, 8.6, 5.2);
            return ptz;
        }
        if (kind === 'bullet') {
            var bullet = [rect(0, 3.2, 14, 7.6, INK, FACE, 0.3), rect(0, 4.6, 3.2, 4.8, METAL, DARK, 0.12), oval(11, 2.6, 9, 8.8, DARK, '#071426', 0.16), oval(13.4, 4.6, 4.4, 4.4, BLUE, '#4c9aff', 0.1), oval(14.6, 5.8, 1.8, 1.8, '#ffffff', BLUEF, 0.05), rect(2, 11.2, 6, 1.6, METAL, EAR, 0.1)];
            irRing(bullet, 15.6, 6.8, 3.2);
            return bullet;
        }
        if (kind === 'outdoor') {
            var outdoor = [rect(0, 4.2, 16, 8, INK, EAR, 0.3), rect(0, 1.6, 18, 3.2, METAL, DARK, 0.14), rect(0, 1.6, 18, 0.6, INK, '#ffffff', 0.06), oval(12.5, 3.6, 9, 8.8, DARK, '#071426', 0.16), oval(14.8, 5.6, 4.2, 4.2, BLUE, '#4c9aff', 0.1), rect(1.2, 12.4, 5, 2.2, METAL, DARK, 0.1), line(1.2, 13.5, 6.2, 13.5, '#ffab00', 0.08)];
            irRing(outdoor, 17, 8, 3.3);
            return outdoor;
        }
        if (kind === 'thermal') {
            return [rect(0, 3, 15, 8, INK, '#253858', 0.3), rect(0, 4.4, 3, 5, AMBER, DARK, 0.1), oval(12, 2.6, 9.2, 8.8, RED, '#ffbdad', 0.16), oval(14.6, 5, 4, 4, AMBER, '#fff7e6', 0.1), line(15.4, 5.6, 17.6, 8.4, RED, 0.12), line(17.6, 5.6, 15.4, 8.4, RED, 0.12), rect(1.4, 11.4, 4.5, 1.8, METAL, DARK, 0.1)];
        }
        if (kind === 'multi') {
            var parts = [rect(0, 0, 22, 22, INK, FACE, 0.4), rect(9.4, 0, 3.2, 22, METAL, EAR, 0.1), rect(0, 9.4, 22, 3.2, METAL, EAR, 0.1)];
            [[1.2, 1.2], [11.6, 1.2], [1.2, 11.6], [11.6, 11.6]].forEach(function (at) {
                parts.push(oval(at[0], at[1], 9.2, 9.2, DARK, '#071426', 0.14));
                parts.push(oval(at[0] + 2.6, at[1] + 2.6, 4, 4, BLUE, '#4c9aff', 0.08));
                parts.push(oval(at[0] + 3.8, at[1] + 3.8, 1.6, 1.6, '#ffffff', BLUEF, 0.04));
            });
            return parts;
        }
        var plain = [oval(0.4, 0.4, 11.2, 11.2, INK, FACE, 0.3), oval(3.2, 3.2, 5.6, 5.6, DARK, '#071426', 0.14), oval(4.6, 4.6, 2.6, 2.6, BLUE, BLUEF, 0.08)];
        irRing(plain, 6, 6, 3.6);
        return plain;
    }

    function video() {
        return [
            unit('nvr', 'Видеорегистратор NVR', 2, FACE, function (parts, w, h) {
                bays(parts, 2.4, 1, 16, h - 2, 4, 4);
                display(parts, 20, 1.2, 14, h - 2.4);
                ports(parts, 36, 1.4, 9, h - 2.6, 4, 2, BLUE);
            }),
            unit('dvr', 'Видеорегистратор DVR', 2, FACE, function (parts, w, h) {
                var bnc;
                for (bnc = 0; bnc < 16; bnc++) parts.push(oval(3 + (bnc % 8) * 3.2, 1.3 + Math.floor(bnc / 8) * 3.2, 2.2, 2.2, INK, '#ffffff', 0.1));
                display(parts, 30, 1.4, 14, h - 2.8);
            }),
            unit('hybrid', 'Гибридный видеорегистратор', 2, FACE, function (parts, w, h) {
                var bnc;
                for (bnc = 0; bnc < 8; bnc++) parts.push(oval(3 + bnc * 2.6, 1.2, 2, 2, INK, EAR, 0.08));
                ports(parts, 3, 4.2, 22, 3.4, 8, 1, BLUE);
                display(parts, 30, 1.4, 14, h - 2.8);
            }),
            plan('cam-ip', 'IP-камера', 14, 14, camera('dome').concat([oval(10.4, 10.4, 1.8, 1.8, BLUE, BLUE, 0.05)])),
            plan('cam-analog', 'Аналоговая камера', 14, 14, camera('dome').concat([rect(5.4, 11.6, 3.2, 1.4, INK, DARK, 0.08)])),
            plan('cam-dome', 'Купольная камера', 14, 14, camera('dome')),
            plan('cam-ptz', 'PTZ-камера', 18, 18, camera('ptz')),
            plan('cam-fixed', 'Фиксированная камера', 22, 14, camera('bullet')),
            plan('cam-out', 'Уличная камера', 24, 16, camera('outdoor')),
            plan('cam-in', 'Внутренняя камера', 14, 14, camera('dome')),
            plan('cam-thermal', 'Тепловизионная камера', 24, 14, camera('thermal')),
            plan('cam-multi', 'Мультисенсорная камера', 22, 22, camera('multi')),
            unit('decoder', 'Видеодекодер', 1, FACE, function (parts, w, h) {
                ports(parts, 3, 1.2, 20, h - 1.8, 8, 2, BLUE);
                display(parts, 26, 0.7, 16, h - 1.4);
            }),
            plan('videowall', 'Видеостена', 160, 96, (function () {
                var parts = [rect(0, 0, 160, 96, INK, DARK, 0.6)];
                var col;
                var row;
                for (row = 0; row < 2; row++) {
                    for (col = 0; col < 3; col++) {
                        parts.push(rect(4 + col * 52, 4 + row * 46, 48, 42, METAL, '#071426', 0.25));
                        parts.push(line(8 + col * 52, 40 + row * 46, 28 + col * 52, 14 + row * 46, '#4c9aff', 0.16));
                    }
                }
                return parts;
            })()),
            plan('wall-monitor', 'Монитор видеостены', 48, 30, [
                rect(0, 0, 48, 30, INK, DARK, 0.4),
                rect(2, 2, 44, 23, METAL, '#071426', 0.16),
                rect(20, 26, 8, 2.4, METAL, EAR, 0.1)
            ]),
            plan('ops-console', 'Консоль оператора видеонаблюдения', 160, 80, [
                rect(0, 20, 160, 60, INK, '#fff7e6', 0.5),
                rect(8, 0, 40, 24, INK, DARK, 0.25),
                rect(60, 0, 40, 24, INK, DARK, 0.25),
                rect(112, 0, 40, 24, INK, DARK, 0.25),
                oval(74, 48, 12, 12, METAL, FACE, 0.16)
            ]),
            plan('cam-psu', 'Блок питания камер', 18, 10, [
                rect(0, 0, 18, 10, INK, FACE, 0.3),
                rect(1.2, 2, 4, 6, METAL, EAR, 0.12),
                line(8, 3, 16, 3, RED, 0.14),
                line(8, 7, 16, 7, DARK, 0.14)
            ]),
            plan('poe-inj', 'PoE-инжектор', 10, 6, [
                rect(0, 0, 10, 6, INK, '#fff7e6', 0.25),
                rect(0.8, 1.6, 2.4, 2.8, BLUE, BLUEF, 0.1),
                rect(6.6, 1.6, 2.4, 2.8, AMBER, '#ffffff', 0.1)
            ]),
            plan('surge', 'Грозозащита камер', 8, 6, [
                rect(0, 0, 8, 6, INK, FACE, 0.25),
                line(1.2, 5, 4, 1.2, AMBER, 0.2),
                line(4, 1.2, 6.8, 5, AMBER, 0.2),
                oval(3.4, 2.4, 1.2, 1.2, AMBER, AMBER, 0.06)
            ])
        ];
    }

    function access() {
        return [
            plan('acs', 'Контроллер СКУД', 22, 16, (function () {
                var parts = [rect(0, 0, 22, 16, INK, FACE, 0.35)];
                display(parts, 1.4, 1.4, 8, 5);
                var pin;
                for (pin = 0; pin < 8; pin++) parts.push(rect(11, 1.2 + pin * 1.7, 9, 1.2, METAL, EAR, 0.08));
                return parts;
            })()),
            plan('reader', 'Считыватель карт', 8, 12, (function () {
                var parts = [
                    rect(0, 0, 8, 12, INK, FACE, 0.3),
                    rect(1.4, 1.4, 5.2, 3.4, BLUE, BLUEF, 0.12),
                    oval(2.6, 6.4, 2.8, 2.8, INK, EAR, 0.12)
                ];
                leds(parts, 1.6, 10, 3, ['#36b37e', RED, AMBER]);
                return parts;
            })()),
            plan('bio', 'Биометрический терминал', 10, 18, [
                rect(0, 0, 10, 18, INK, FACE, 0.3),
                rect(1.2, 1.2, 7.6, 5, DARK, '#071426', 0.12),
                oval(2.4, 8, 5.2, 6.4, METAL, '#ffffff', 0.14),
                oval(3.6, 9.6, 2.8, 3.4, INK, EAR, 0.1)
            ]),
            plan('turnstile', 'Турникет', 140, 110, [
                rect(0, 30, 28, 50, INK, FACE, 0.45),
                rect(112, 30, 28, 50, INK, FACE, 0.45),
                line(28, 55, 70, 55, INK, 0.35),
                line(70, 55, 112, 40, METAL, 0.25),
                line(70, 55, 112, 70, METAL, 0.25),
                oval(10, 48, 8, 8, BLUE, BLUEF, 0.16),
                oval(122, 48, 8, 8, RED, '#ffebe6', 0.16)
            ]),
            plan('mantrap', 'Шлюз СКУД', 160, 120, [
                rect(0, 0, 160, 120, INK, FACE, 0.5),
                rect(8, 8, 144, 104, METAL, '#ffffff', 0.25),
                rect(70, 0, 20, 8, INK, EAR, 0.16),
                rect(70, 112, 20, 8, INK, EAR, 0.16),
                arc(70, 8, 18, 0, Math.PI / 2, 10, METAL, 0.16),
                arc(90, 112, 18, Math.PI, Math.PI * 1.5, 10, METAL, 0.16)
            ]),
            plan('maglock', 'Электромагнитный замок', 20, 4, [
                rect(0, 0, 14, 4, INK, DARK, 0.25),
                rect(14.4, 0.4, 5.6, 3.2, METAL, EAR, 0.14)
            ]),
            plan('strike', 'Электромеханический замок', 4, 16, [
                rect(0, 0, 4, 16, INK, EAR, 0.25),
                rect(0.6, 2, 2.8, 5, INK, FACE, 0.1),
                rect(1.2, 9, 1.6, 4, METAL, DARK, 0.1)
            ]),
            plan('closer', 'Доводчик двери', 22, 6, [
                rect(0, 1.4, 16, 3.2, INK, EAR, 0.22),
                rect(14, 0, 8, 6, METAL, FACE, 0.16),
                line(4, 3, 14, 3, METAL, 0.14)
            ]),
            plan('door-contact', 'Датчик положения двери', 6, 2.4, [
                rect(0, 0, 2.6, 2.4, INK, FACE, 0.14),
                rect(3.2, 0.3, 2.8, 1.8, METAL, EAR, 0.1)
            ]),
            plan('rex', 'Кнопка выхода', 8, 8, [
                rect(0, 0, 8, 8, INK, FACE, 0.28),
                oval(1.6, 1.6, 4.8, 4.8, GREEN, GREENF, 0.16),
                line(4, 2.8, 4, 5.2, GREEN, 0.16)
            ]),
            plan('intercom', 'Домофон', 10, 18, [
                rect(0, 0, 10, 18, INK, FACE, 0.3),
                oval(2.2, 1.6, 5.6, 5.6, METAL, EAR, 0.14),
                rect(2, 8.4, 6, 1.1, INK, DARK, 0.08),
                rect(2, 10.2, 6, 1.1, INK, DARK, 0.08),
                rect(2, 12, 6, 1.1, INK, DARK, 0.08),
                rect(2, 13.8, 6, 1.1, INK, DARK, 0.08)
            ]),
            plan('ip-intercom', 'IP-домофон', 10, 20, [
                rect(0, 0, 10, 20, INK, FACE, 0.3),
                oval(3, 1.4, 4, 4, DARK, '#071426', 0.12),
                oval(4.2, 2.6, 1.6, 1.6, BLUE, BLUEF, 0.08),
                rect(1.4, 6.4, 7.2, 4, DARK, '#071426', 0.1),
                rect(2, 12, 6, 1.1, INK, DARK, 0.08),
                rect(2, 14, 6, 1.1, INK, DARK, 0.08),
                rect(2, 16, 6, 1.1, INK, DARK, 0.08)
            ]),
            plan('call-panel', 'Вызывная панель', 12, 24, (function () {
                var parts = [rect(0, 0, 12, 24, INK, FACE, 0.3), oval(3.4, 1.4, 5.2, 5.2, DARK, '#071426', 0.12)];
                var key;
                for (key = 0; key < 12; key++) parts.push(rect(1.6 + (key % 3) * 3.1, 8 + Math.floor(key / 3) * 3.4, 2.5, 2.6, METAL, EAR, 0.08));
                return parts;
            })()),
            plan('acs-psu', 'Блок питания СКУД', 16, 10, [
                rect(0, 0, 16, 10, INK, FACE, 0.28),
                rect(1, 2, 5, 6, METAL, EAR, 0.12),
                line(8, 3, 14, 3, RED, 0.14),
                line(8, 7, 14, 7, DARK, 0.14)
            ]),
            plan('door-ctl', 'Контроллер домофона', 18, 12, [
                rect(0, 0, 18, 12, INK, FACE, 0.3),
                rect(1.2, 1.4, 6, 4, DARK, '#071426', 0.1),
                rect(8.4, 1.6, 8, 1.4, METAL, EAR, 0.08),
                rect(8.4, 3.6, 8, 1.4, METAL, EAR, 0.08),
                rect(8.4, 5.6, 8, 1.4, METAL, EAR, 0.08),
                oval(2, 8, 1.4, 1.4, '#36b37e', '#36b37e', 0.06)
            ])
        ];
    }

    function voice() {
        return [
            unit('pbx', 'АТС', 4, FACE, function (parts, w, h) {
                var card;
                for (card = 0; card < 6; card++) {
                    parts.push(rect(2.4 + card * 5.4, 1.2, 4.8, h - 2.4, INK, card % 2 ? EAR : FACE, 0.12));
                    ports(parts, 2.8 + card * 5.4, 2, 4, 8, 4, 2, GREEN);
                }
                display(parts, 36, 1.6, 9, 6);
            }),
            unit('ip-pbx', 'IP-АТС', 1, GREENF, function (parts, w, h) {
                ports(parts, 3, 1.2, 24, h - 1.8, 12, 2, GREEN);
                display(parts, 30, 0.7, 14, h - 1.4);
            }),
            unit('voip-gw', 'VoIP-шлюз', 1, FACE, function (parts, w, h) {
                ports(parts, 3, 1.3, 16, h - 2, 8, 2, GREEN);
                ports(parts, 22, 1.3, 16, h - 2, 4, 1, BLUE);
            }),
            plan('voip-phone', 'VoIP-телефон', 22, 18, (function () {
                var parts = [
                    rect(0, 4, 22, 14, INK, FACE, 0.35),
                    rect(2, 0, 7, 9, METAL, EAR, 0.16),
                    oval(4.2, 1.2, 2.6, 2.6, DARK, '#071426', 0.1),
                    rect(10, 5.6, 10, 4.6, DARK, '#071426', 0.12)
                ];
                var key;
                for (key = 0; key < 12; key++) parts.push(rect(10.4 + (key % 4) * 2.6, 11 + Math.floor(key / 4) * 2.1, 2.1, 1.6, METAL, EAR, 0.06));
                return parts;
            })()),
            plan('ip-phone', 'IP-телефон', 22, 18, (function () {
                var parts = [
                    rect(0, 4, 22, 14, INK, BLUEF, 0.35),
                    rect(2, 0, 7, 9, METAL, EAR, 0.16),
                    oval(4.2, 1.2, 2.6, 2.6, BLUE, BLUEF, 0.1),
                    rect(9.2, 5.4, 11, 5, DARK, '#071426', 0.12),
                    line(10.4, 8.4, 18.6, 6.6, '#4c9aff', 0.1)
                ];
                var key;
                for (key = 0; key < 6; key++) parts.push(oval(10 + (key % 3) * 2.4, 11.4 + Math.floor(key / 3) * 2.4, 1.8, 1.8, BLUE, '#ffffff', 0.08));
                return parts;
            })()),
            plan('dect-base', 'DECT-базовая станция', 14, 14, [
                oval(0.4, 0.4, 13.2, 13.2, INK, FACE, 0.3),
                oval(4, 4, 6, 6, GREEN, GREENF, 0.14),
                line(7, 1.4, 7, 4, GREEN, 0.14)
            ]),
            plan('dect-rep', 'DECT-репитер', 8, 10, [
                rect(1, 2, 6, 8, INK, FACE, 0.25),
                line(4, 0, 4, 3, GREEN, 0.16),
                oval(2.6, 4.2, 2.8, 2.8, GREEN, GREENF, 0.1)
            ]),
            plan('dect-hand', 'DECT-трубка', 5, 16, [
                rect(0.4, 0, 4.2, 16, INK, FACE, 0.25),
                oval(1.2, 1, 2.6, 3, METAL, EAR, 0.1),
                rect(1.2, 6, 2.6, 6, DARK, '#071426', 0.08)
            ]),
            plan('dect-ant', 'Антенна DECT', 12, 20, [
                line(6, 4, 6, 19, INK, 0.3),
                rect(4.6, 16, 2.8, 4, INK, DARK, 0.12),
                arc(6, 7, 3, Math.PI * 1.2, Math.PI * 1.8, 8, GREEN, 0.14),
                arc(6, 7, 5, Math.PI * 1.2, Math.PI * 1.8, 8, GREEN, 0.12)
            ]),
            unit('cross', 'Кросс-панель', 2, FACE, function (parts, w, h) {
                var pin;
                for (pin = 0; pin < 20; pin++) {
                    parts.push(line(4 + pin * 2, 1.4, 4 + pin * 2, h - 1.4, METAL, 0.1));
                    parts.push(oval(3.6 + pin * 2, 2, 0.8, 0.8, INK, DARK, 0.05));
                    parts.push(oval(3.6 + pin * 2, h - 2.8, 0.8, 0.8, INK, DARK, 0.05));
                }
            }),
            unit('voice-patch', 'Патч-панель телефонии', 1, FACE, function (parts, w, h) {
                ports(parts, 3, 1.2, 42, h - 1.8, 24, 2, GREEN);
            }),
            plan('conf', 'Конференц-терминал', 36, 22, [
                oval(2, 4, 32, 16, INK, FACE, 0.35),
                rect(12, 0, 12, 8, INK, DARK, 0.2),
                oval(6, 8, 4, 4, METAL, EAR, 0.12),
                oval(26, 8, 4, 4, METAL, EAR, 0.12),
                oval(16, 9, 4, 4, BLUE, BLUEF, 0.12)
            ]),
            plan('operator', 'Терминал оператора', 40, 30, [
                rect(0, 8, 40, 22, INK, FACE, 0.4),
                rect(4, 0, 32, 16, INK, DARK, 0.25),
                rect(14, 18, 12, 6, METAL, EAR, 0.12)
            ]),
            plan('thin', 'Тонкий клиент', 18, 14, [
                rect(0, 2, 18, 10, INK, FACE, 0.3),
                rect(1, 3.2, 10, 6, DARK, '#071426', 0.1),
                oval(13, 5, 3, 3, BLUE, BLUEF, 0.12)
            ]),
            plan('workstation', 'Рабочая станция', 20, 46, (function () {
                var parts = [rect(0, 0, 20, 46, INK, FACE, 0.4), rect(2, 2, 16, 8, DARK, '#071426', 0.14)];
                vents(parts, 2, 12, 16, 22, 1.3);
                bays(parts, 3, 36, 14, 7, 2, 2);
                power(parts, 15, 2.4);
                return parts;
            })()),
            plan('pay', 'Платежный терминал', 14, 22, [
                rect(0, 0, 14, 22, INK, FACE, 0.35),
                rect(1.4, 1.4, 11.2, 8, DARK, '#071426', 0.14),
                rect(2, 11, 10, 1.4, METAL, EAR, 0.08),
                rect(2, 13.2, 10, 1.4, METAL, EAR, 0.08),
                rect(2, 15.4, 10, 1.4, METAL, EAR, 0.08),
                rect(3, 18, 8, 2.4, GREEN, GREENF, 0.1)
            ]),
            plan('vms-term', 'Терминал видеонаблюдения', 28, 20, [
                rect(0, 4, 28, 16, INK, FACE, 0.35),
                rect(2, 0, 16, 10, INK, DARK, 0.2),
                oval(20, 8, 5, 5, RED, '#ffebe6', 0.14)
            ])
        ];
    }

    function tidy(symbol) {
        symbol.parts = (symbol.parts || []).filter(function (part) {
            return part && part.t;
        });
        return symbol;
    }

    var switchCache = {};

    root.AssetTreeSchemeLibrary = function () {
        return [
            { id: 'lib-rack', name: 'Стойки и конструктив', templates: racks().map(tidy) },
            { id: 'lib-server', name: 'Серверы и хранение', templates: servers().map(tidy) },
            { id: 'lib-net', name: 'Сеть', templates: network().map(tidy) },
            { id: 'lib-video', name: 'Видеонаблюдение', templates: video().map(tidy) },
            { id: 'lib-acs', name: 'СКУД', templates: access().map(tidy) },
            { id: 'lib-voice', name: 'АТС и терминалы', templates: voice().map(tidy) }
        ];
    };

    root.AssetTreeSchemeSwitch = function (symbol, count) {
        if (!symbol || !symbol.face) return symbol;
        count = clampPorts(count);
        if (count === symbol.ports) return symbol;
        var key = symbol.face + ':' + count;
        if (!switchCache[key]) {
            var built = tidy(buildSwitch('tmp', symbol.name, count, symbol.face));
            built.id = symbol.id;
            switchCache[key] = built;
        }
        var cached = switchCache[key];
        return { id: symbol.id, name: symbol.name, w: cached.w, h: cached.h, parts: cached.parts, ports: count, face: symbol.face };
    };
})(typeof window !== 'undefined' ? window : global);
