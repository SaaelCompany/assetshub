/* Plan furniture in centimetres. The back, head or wall side sits at y = 0. */
(function (root) {
    var INK = '#172b4d';
    var MET = '#5e6c84';
    var STEEL = '#ebecf0';
    var WOOD = '#8d6e43';
    var WOODF = '#fff6e4';
    var WOOD2 = '#f3e2c4';
    var FABF = '#efeaff';
    var FAB2 = '#d9d0f5';
    var WHITE = '#ffffff';
    var GLASS = '#deebff';
    var DARK = '#091e42';
    var GREEN = '#006644';
    var GREENF = '#e3fcef';
    var RED = '#de350b';
    var AMBER = '#ff8b00';
    var FIRE = '#ffebe6';
    var TILE = '#f4f5f7';

    function n(value) {
        return Math.round(value * 100) / 100;
    }

    function rect(x, y, w, h, stroke, fill, width) {
        return { t: 'r', x: n(x), y: n(y), w: n(w), h: n(h), c: stroke || INK, f: fill || '', s: width == null ? 0.45 : width };
    }

    function oval(x, y, w, h, stroke, fill, width) {
        return { t: 'e', x: n(x), y: n(y), w: n(w), h: n(h), c: stroke || INK, f: fill || '', s: width == null ? 0.4 : width };
    }

    function line(x1, y1, x2, y2, stroke, width) {
        return { t: 'l', x1: n(x1), y1: n(y1), x2: n(x2), y2: n(y2), c: stroke || INK, s: width == null ? 0.3 : width };
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

    function box(w, h, parts) {
        return { w: n(w), h: n(h), parts: parts };
    }

    function moved(parts, dx, dy) {
        return parts.map(function (part) {
            var copy = JSON.parse(JSON.stringify(part));
            if (copy.t === 'r' || copy.t === 'e' || copy.t === 't') {
                copy.x = n(copy.x + dx);
                copy.y = n(copy.y + dy);
            } else if (copy.t === 'l') {
                copy.x1 = n(copy.x1 + dx);
                copy.y1 = n(copy.y1 + dy);
                copy.x2 = n(copy.x2 + dx);
                copy.y2 = n(copy.y2 + dy);
            } else if (copy.points) {
                copy.points.forEach(function (point) {
                    point.x = n(point.x + dx);
                    point.y = n(point.y + dy);
                });
            }
            return copy;
        });
    }

    function legs(parts, x, y, w, h, inset, size) {
        var s = size || 4;
        var m = inset == null ? 5 : inset;
        [[x + m, y + m], [x + w - m - s, y + m], [x + m, y + h - m - s], [x + w - m - s, y + h - m - s]].forEach(function (at) {
            parts.push(rect(at[0], at[1], s, s, INK, DARK, 0.2));
        });
    }

    function wheels(parts, x, y, w, h, size) {
        var s = size || 7;
        [[x, y], [x + w - s, y], [x, y + h - s], [x + w - s, y + h - s]].forEach(function (at) {
            parts.push(oval(at[0], at[1], s, s, INK, DARK, 0.2));
        });
    }

    function drawers(parts, x, y, w, h, count) {
        var row = h / count;
        var i;
        parts.push(rect(x, y, w, h, INK, WOODF, 0.35));
        for (i = 0; i < count; i++) {
            if (i) parts.push(line(x, y + i * row, x + w, y + i * row, INK, 0.22));
            if (row > 8) parts.push(rect(x + 2.4, y + i * row + 1.8, w - 4.8, row - 3.6, WOOD, '', 0.12));
            parts.push(rect(x + w / 2 - 5, y + i * row + row / 2 - 1.2, 10, 2.4, INK, STEEL, 0.18));
        }
    }

    function books(parts, x, y, w, h) {
        var cursor = x;
        var tones = [FAB2, WOOD2, GLASS, GREENF, FIRE];
        var i = 0;
        while (cursor < x + w - 3) {
            var span = 2.2 + (i % 3);
            if (cursor + span > x + w) break;
            parts.push(rect(cursor, y, span, h, INK, tones[i % tones.length], 0.08));
            cursor += span + 0.4;
            i++;
        }
    }

    function hangers(parts, x, y, w) {
        var i;
        for (i = x; i < x + w - 6; i += 9) {
            parts.push(arc(i + 3, y, 3.2, Math.PI * 1.05, Math.PI * 1.95, 5, MET, 0.16));
        }
    }

    function burners(parts, x, y, w, h, count) {
        var cols = count > 2 ? 2 : count;
        var rows = Math.ceil(count / cols);
        var i;
        for (i = 0; i < count; i++) {
            var cw = w / cols;
            var rh = h / rows;
            parts.push(oval(x + (i % cols) * cw + cw * 0.18, y + Math.floor(i / cols) * rh + rh * 0.18, cw * 0.64, rh * 0.64, INK, DARK, 0.16));
        }
    }

    function swing(parts, hx, hy, radius, closed, open) {
        parts.push(line(hx, hy, hx + Math.cos(open) * radius, hy + Math.sin(open) * radius, INK, 0.35));
        parts.push(arc(hx, hy, radius, closed, open, 12, MET, 0.26));
    }

    function wallLine(parts, w) {
        parts.push(line(0, 0.6, w, 0.6, INK, 1.1));
    }

    function slats(parts, x, y, w, h, step) {
        var i;
        for (i = y + step; i < y + h - 1.5; i += step) {
            parts.push(line(x + 1.5, i, x + w - 1.5, i, WOOD, 0.16));
        }
    }

    function panel(parts, x, y, w, h) {
        if (w < 8 || h < 8) return;
        parts.push(rect(x, y, w, h, WOOD, '', 0.14));
    }

    function quilt(parts, x, y, w, h) {
        var i;
        parts.push(rect(x, y, w, h, INK, WHITE, 0.16));
        parts.push(rect(x + 1.4, y + 1.4, w - 2.8, h - 2.8, MET, '', 0.12));
        for (i = 1; i < 4; i++) parts.push(line(x + 1, y + h * i / 4, x + w - 1, y + h * i / 4, MET, 0.45));
        for (i = 1; i < 3; i++) parts.push(line(x + w * i / 3, y + 1, x + w * i / 3, y + h - 1, MET, 0.45));
        var j;
        for (i = 1; i < 4; i++) {
            for (j = 1; j < 3; j++) parts.push(oval(x + w * j / 3 - 1.2, y + h * i / 4 - 1.2, 2.4, 2.4, MET, FAB2, 0.08));
        }
    }

    function buttons(parts, x, y, w, h, cols, rows) {
        var i;
        var j;
        if (w < 8 || h < 8) return;
        for (i = 0; i < cols; i++) {
            for (j = 0; j < rows; j++) {
                parts.push(oval(x + w * (i + 0.5) / cols - 2.2, y + h * (j + 0.5) / rows - 2.2, 4.4, 4.4, MET, FAB2, 0.2));
            }
        }
    }

    function unitWalls(parts, w, d, gapX, gapW) {
        var t = 8;
        parts.push(rect(0, 0, w, t, INK, TILE, 0.4));
        parts.push(rect(0, 0, t, d, INK, TILE, 0.4));
        parts.push(rect(w - t, 0, t, d, INK, TILE, 0.4));
        if (gapX > 0) parts.push(rect(0, d - t, gapX, t, INK, TILE, 0.4));
        if (gapX + gapW < w) parts.push(rect(gapX + gapW, d - t, w - gapX - gapW, t, INK, TILE, 0.4));
        parts.push(line(gapX, d - t, gapX, d - t - gapW, INK, 0.4));
        parts.push(arc(gapX, d - t, gapW, 0, -Math.PI / 2, 10, MET, 0.26));
    }

    function sofa(w, d, seats) {
        var arm = Math.max(12, Math.min(16, w * 0.08));
        var back = Math.max(16, Math.min(22, d * 0.24));
        var parts = [rect(0, 0, w, d, INK, FABF, 0.8)];
        var gap = 2.4;
        var inner = w - arm * 2 - 4;
        var cw = (inner - gap * (seats - 1)) / seats;
        var i;
        parts.push(rect(0, 0, w, back, INK, FAB2, 0.4));
        parts.push(rect(0, back * 0.2, arm, d - back * 0.2, INK, FAB2, 0.35));
        parts.push(rect(w - arm, back * 0.2, arm, d - back * 0.2, INK, FAB2, 0.35));
        for (i = 0; i < seats; i++) {
            var x = arm + 2 + i * (cw + gap);
            parts.push(oval(x + 2, 3, Math.max(8, cw - 6), back - 6, INK, WHITE, 0.16));
            parts.push(line(x + cw * 0.35, 4, x + cw * 0.35, back - 4, FAB2, 0.12));
            parts.push(line(x + cw * 0.65, 4, x + cw * 0.65, back - 4, FAB2, 0.12));
            parts.push(rect(x, back + 4, cw, d - back - 8, INK, WHITE, 0.22));
            parts.push(rect(x + 2.2, back + 6.4, Math.max(4, cw - 4.4), Math.max(4, d - back - 16), FAB2, '', 0.18));
            parts.push(line(x + 2, back + (d - back) * 0.62, x + cw - 2, back + (d - back) * 0.62, MET, 0.16));
            parts.push(rect(x + 3, d - 9, Math.max(4, cw - 6), 3, INK, WOOD2, 0.14));
            buttons(parts, x + 4, back + 8, cw - 8, Math.max(10, (d - back) * 0.42), 2, 2);
        }
        parts.push(oval(2, back + 2, arm - 4, 12, INK, WHITE, 0.14));
        parts.push(oval(w - arm + 2, back + 2, arm - 4, 12, INK, WHITE, 0.14));
        parts.push(rect(2, d - 5, 6, 4, INK, WOOD, 0.16));
        parts.push(rect(w - 8, d - 5, 6, 4, INK, WOOD, 0.16));
        return box(w, d, parts);
    }

    function cornerSofa(w, d, seat) {
        var back = 18;
        var parts = [];
        parts.push(rect(0, 0, w, seat, INK, FABF, 0.8));
        parts.push(rect(w - seat, seat - 0.6, seat, d - seat + 0.6, INK, FABF, 0.8));
        parts.push(rect(0, 0, w - seat + back, back, INK, FAB2, 0.35));
        parts.push(rect(w - back, back, back, d - back, INK, FAB2, 0.35));
        parts.push(rect(0, back, 14, seat - back, INK, FAB2, 0.3));
        parts.push(rect(w - seat, d - 14, seat, 14, INK, FAB2, 0.3));
        parts.push(rect(18, back + 5, (w - seat - 28) / 2 - 2, seat - back - 12, INK, WHITE, 0.2));
        parts.push(rect(22 + (w - seat - 28) / 2, back + 5, (w - seat - 28) / 2 - 2, seat - back - 12, INK, WHITE, 0.2));
        parts.push(rect(w - seat + 6, back + 5, seat - back - 12, seat - back - 12, INK, WHITE, 0.2));
        parts.push(rect(w - seat + 6, seat + 6, seat - back - 12, d - seat - 24, INK, WHITE, 0.2));
        return box(w, d, parts);
    }

    function modularSofa(modW, d, count) {
        var gap = 5;
        var parts = [];
        var i;
        for (i = 0; i < count; i++) {
            parts = parts.concat(moved(sofa(modW, d, 1).parts, i * (modW + gap), 0));
        }
        return box(modW * count + gap * (count - 1), d, parts);
    }

    function pouf(d) {
        var parts = [oval(0, 0, d, d, INK, FABF, 0.7)];
        parts.push(oval(d * 0.16, d * 0.16, d * 0.68, d * 0.68, INK, WHITE, 0.2));
        parts.push(line(d * 0.28, d / 2, d * 0.72, d / 2, MET, 0.16));
        parts.push(line(d / 2, d * 0.28, d / 2, d * 0.72, MET, 0.16));
        return box(d, d, parts);
    }

    function bench(w, d, back) {
        var parts = [];
        if (back) {
            parts.push(rect(0, 0, w, d, INK, FABF, 0.7));
            parts.push(rect(0, 0, w, Math.min(14, d * 0.28), INK, FAB2, 0.35));
            parts.push(rect(8, 18, w - 16, d - 26, INK, WHITE, 0.2));
        } else {
            parts.push(rect(0, 0, w, d, INK, WOODF, 0.7));
            slats(parts, 0, 0, w, d, Math.max(4, d / 4));
            legs(parts, 0, 0, w, d, 4, 4);
        }
        return box(w, d, parts);
    }

    function stool(d) {
        var parts = [oval(2, 2, d - 4, d - 4, INK, WOODF, 0.6)];
        legs(parts, 0, 0, d, d, 3, 3.5);
        return box(d, d, parts);
    }

    function barSeat(back) {
        var w = 42;
        var d = back ? 50 : 42;
        var parts = [];
        if (back) parts.push(rect(7, 0, 28, 8, INK, FAB2, 0.4));
        parts.push(oval(4, back ? 6 : 4, 34, 34, INK, WOODF, 0.6));
        parts.push(oval(7, back ? 20 : 16, 28, 16, MET, '', 0.22));
        return box(w, d, parts);
    }

    function table(w, d, shape) {
        var parts = [];
        if (shape === 'round' || shape === 'oval') {
            parts.push(oval(0, 0, w, d, INK, WOODF, 0.85));
            parts.push(oval(w * 0.08, d * 0.08, w * 0.84, d * 0.84, WOOD, '', 0.22));
        } else {
            parts.push(rect(0, 0, w, d, INK, WOODF, 0.85));
            parts.push(rect(3, 3, w - 6, d - 6, WOOD, '', 0.22));
            if (w > 40) {
                parts.push(line(w * 0.33, 6, w * 0.33, d - 6, WOOD, 0.16));
                parts.push(line(w * 0.66, 6, w * 0.66, d - 6, WOOD, 0.16));
            }
            if (w > 50 && d > 28) parts.push(rect(w / 2 - 2.4, 8, 4.8, d - 16, INK, WOOD2, 0.16));
        }
        if (shape === 'leaf') {
            parts.push(line(w * 0.33, 4, w * 0.33, d - 4, AMBER, 0.28));
            parts.push(line(w * 0.67, 4, w * 0.67, d - 4, AMBER, 0.28));
        }
        if (shape !== 'round') legs(parts, 0, 0, w, d, Math.min(8, w * 0.08), Math.max(4, Math.min(w, d) * 0.06));
        else {
            var s = Math.max(4, d * 0.06);
            [[d * 0.28, d * 0.28], [d * 0.72 - s, d * 0.28], [d * 0.28, d * 0.72 - s], [d * 0.72 - s, d * 0.72 - s]].forEach(function (at) {
                parts.push(rect(at[0], at[1], s, s, INK, DARK, 0.16));
            });
        }
        return box(w, d, parts);
    }

    function tv(w, d, screen) {
        var parts = [rect(0, screen ? 8 : 0, w, d, INK, WOODF, 0.7)];
        drawers(parts, 6, (screen ? 8 : 0) + 6, w / 2 - 10, d - 12, 2);
        parts.push(rect(w / 2 + 2, (screen ? 8 : 0) + 6, w / 2 - 8, d - 12, INK, DARK, 0.25));
        if (screen) {
            parts.push(rect(w * 0.14, 0, w * 0.72, 8, INK, DARK, 0.4));
            parts.push(rect(w * 0.2, 1.2, w * 0.6, 5.2, '#4c9aff', '#071426', 0.12));
        }
        return box(w, (screen ? 8 : 0) + d, parts);
    }

    function shelf(w, d, cols, rows, withBooks) {
        var parts = [rect(0, 0, w, d, INK, WOODF, 0.7)];
        var i;
        for (i = 1; i < cols; i++) parts.push(line(w * i / cols, 2, w * i / cols, d - 2, INK, 0.28));
        for (i = 1; i < rows; i++) parts.push(line(2, d * i / rows, w - 2, d * i / rows, WOOD, 0.2));
        if (withBooks) {
            books(parts, 4, 4, w / cols - 8, d / rows - 6);
            books(parts, w / cols + 4, d / rows + 3, w / cols - 8, d / rows - 6);
        }
        return box(w, d, parts);
    }

    function wallShelf(w, d) {
        var parts = [rect(0, 0, w, d, INK, WOODF, 0.6)];
        wallLine(parts, w);
        parts.push(line(w * 0.2, d, w * 0.08, d * 0.2, WOOD, 0.3));
        parts.push(line(w * 0.8, d, w * 0.92, d * 0.2, WOOD, 0.3));
        return box(w, d, parts);
    }

    function dresser(w, d, count) {
        var parts = [];
        drawers(parts, 0, 0, w, d, count || 4);
        legs(parts, 0, 0, w, d, 2, 3);
        return box(w, d, parts);
    }

    function wardrobe(w, d, doors) {
        var leaf = w / doors;
        var h = d + leaf;
        var parts = [rect(0, 0, w, d, INK, WOODF, 0.8)];
        var i;
        parts.push(line(8, 10, w - 8, 10, MET, 0.28));
        hangers(parts, 10, 16, w - 20);
        for (i = 0; i < doors; i++) {
            var x = i * leaf;
            if (i) parts.push(line(x, 3, x, d - 2, INK, 0.28));
            panel(parts, x + 4, 4, leaf - 8, d - 8);
            parts.push(rect(i % 2 === 0 ? x + 3 : x + leaf - 5, 8, 2.2, 6, MET, STEEL, 0.1));
            parts.push(rect(i % 2 === 0 ? x + 3 : x + leaf - 5, d - 16, 2.2, 6, MET, STEEL, 0.1));
            parts.push(rect(i % 2 === 0 ? x + leaf - 8 : x + 4, d * 0.52, 4, 8, INK, STEEL, 0.16));
            if (i % 2 === 0) swing(parts, x, d, leaf - 0.8, 0, Math.PI / 2);
            else swing(parts, x + leaf, d, leaf - 0.8, Math.PI, Math.PI / 2);
        }
        return box(w, h, parts);
    }

    function sliding(w, d) {
        var parts = [rect(0, 0, w, d, INK, WOODF, 0.8)];
        parts.push(rect(2, 2, w / 2 + 4, d - 4, INK, WOOD2, 0.25));
        parts.push(rect(w / 2 - 6, 4, w / 2 + 2, d - 8, INK, WHITE, 0.25));
        parts.push(line(8, 8, w - 8, 8, MET, 0.24));
        hangers(parts, 12, 14, w - 24);
        parts.push(line(w * 0.3, d * 0.7, w * 0.45, d * 0.7, MET, 0.3));
        parts.push(line(w * 0.55, d * 0.7, w * 0.7, d * 0.7, MET, 0.3));
        return box(w, d, parts);
    }

    function closet(w, d) {
        var arm = Math.min(60, w * 0.32);
        var parts = [];
        parts.push(line(0, 0, w, 0, INK, 1));
        parts.push(line(0, 0, 0, d, INK, 1));
        parts.push(line(w, 0, w, d, INK, 1));
        parts.push(rect(0, 0, arm, d - 30, INK, WOODF, 0.4));
        parts.push(rect(w - arm, 0, arm, d - 30, INK, WOODF, 0.4));
        parts.push(rect(arm - 0.4, 0, w - arm * 2 + 0.8, 36, INK, WOODF, 0.4));
        parts.push(line(8, 12, arm - 8, 12, MET, 0.25));
        parts.push(line(w - arm + 8, 12, w - 8, 12, MET, 0.25));
        hangers(parts, 8, 18, arm - 16);
        hangers(parts, w - arm + 8, 18, arm - 16);
        parts.push(rect(w / 2 - 28, d - 46, 56, 28, INK, FABF, 0.3));
        return box(w, d, parts);
    }

    function coatStand(d) {
        var parts = [oval(d * 0.2, d * 0.2, d * 0.6, d * 0.6, INK, WOODF, 0.4)];
        var i;
        parts.push(oval(d * 0.42, d * 0.42, d * 0.16, d * 0.16, INK, DARK, 0.16));
        for (i = 0; i < 6; i++) {
            var angle = i * Math.PI / 3;
            parts.push(line(d / 2, d / 2, d / 2 + Math.cos(angle) * d * 0.42, d / 2 + Math.sin(angle) * d * 0.42, WOOD, 0.28));
        }
        return box(d, d, parts);
    }

    function coatBar(w) {
        var d = 14;
        var parts = [rect(0, 2, w, 8, INK, WOODF, 0.45)];
        var i;
        wallLine(parts, w);
        for (i = 8; i < w - 4; i += 12) parts.push(arc(i, 10, 3.5, 0.2, Math.PI - 0.2, 5, MET, 0.18));
        return box(w, d, parts);
    }

    function hook() {
        var parts = [rect(0, 0, 10, 8, INK, WOODF, 0.35)];
        wallLine(parts, 10);
        parts.push(arc(5, 3, 3, 0.4, Math.PI - 0.2, 6, MET, 0.28));
        return box(10, 8, parts);
    }

    function mirror(w, d, tilt) {
        var parts = [];
        if (tilt) {
            wallLine(parts, w);
            parts.push(rect(2, 4, w - 4, 5, INK, GLASS, 0.4));
            parts.push(line(8, 0.6, 8, 4, MET, 0.3));
            parts.push(line(w - 8, 0.6, w - 8, 4, MET, 0.3));
            parts.push(rect(w / 2 - 8, d - 7, 16, 7, INK, MET, 0.22));
        } else {
            parts.push(rect(0, 0, w, d, INK, GLASS, 0.55));
            parts.push(line(3, 3, w - 6, d - 3, '#4c9aff', 0.16));
            wallLine(parts, w);
        }
        return box(w, d, parts);
    }

    function floorMirror(w, d) {
        var parts = [rect(w * 0.2, d - 12, w * 0.6, 12, INK, WOODF, 0.3)];
        parts.push(rect(w / 2 - 3, 6, 6, Math.max(4, d - 18), INK, MET, 0.22));
        parts.push(rect(2, 0, w - 4, 5, INK, GLASS, 0.4));
        parts.push(line(6, 2.4, w - 6, 2.4, '#4c9aff', 0.16));
        return box(w, d, parts);
    }

    function floorLamp() {
        var d = 36;
        var parts = [oval(4, 4, 28, 28, INK, FIRE, 0.45)];
        parts.push(oval(12, 12, 12, 12, AMBER, WHITE, 0.2));
        parts.push(oval(15, 15, 6, 6, INK, DARK, 0.15));
        return box(d, d, parts);
    }

    function arcLamp() {
        var w = 54;
        var d = 48;
        var parts = [oval(0, d - 16, 18, 14, INK, WOODF, 0.4)];
        parts.push(arc(14, d - 8, 20, Math.PI * 1.28, Math.PI * 1.82, 10, MET, 0.35));
        parts.push(oval(w - 20, 4, 18, 14, INK, FIRE, 0.35));
        return box(w, d, parts);
    }

    function rug(w, d) {
        var parts = [rect(8, 8, w - 16, d - 16, WOOD, '#f8efe2', 0.55)];
        var i;
        parts.push(rect(16, 16, w - 32, d - 32, WOOD, '', 0.2));
        for (i = 14; i < w - 12; i += 8) {
            parts.push(line(i, 0, i, 8, WOOD, 0.14));
            parts.push(line(i, d - 8, i, d, WOOD, 0.14));
        }
        return box(w, d, parts);
    }

    function basket(d, linen) {
        var h = Math.round(d * 0.86);
        var parts = [oval(0, 0, d, h, INK, linen ? FABF : WOOD2, 0.55)];
        parts.push(line(d * 0.2, h * 0.32, d * 0.8, h * 0.32, WOOD, 0.16));
        parts.push(line(d * 0.2, h * 0.54, d * 0.8, h * 0.54, WOOD, 0.16));
        parts.push(arc(d / 2, h * 0.14, d * 0.2, Math.PI * 0.15, Math.PI * 0.85, 6, WOOD, 0.2));
        return box(d, h, parts);
    }

    function fireplace() {
        var w = 146;
        var d = 78;
        var parts = [];
        var i;
        wallLine(parts, w);
        parts.push(rect(0, 8, w, d - 8, INK, TILE, 0.5));
        parts.push(rect(18, 8, w - 36, 28, INK, '#4a342c', 0.45));
        parts.push(rect(28, 14, w - 56, 16, INK, '#2c211c', 0.3));
        parts.push(oval(w / 2 - 16, 16, 14, 8, AMBER, FIRE, 0.14));
        parts.push(oval(w / 2 + 2, 18, 12, 7, AMBER, FIRE, 0.12));
        parts.push(line(24, 36, w - 24, 36, MET, 0.25));
        for (i = 0; i < 5; i++) parts.push(line(30 + i * 18, 32, 30 + i * 18, 40, MET, 0.16));
        return box(w, d, parts);
    }

    function mantel(w, d) {
        var parts = [rect(0, 0, w, d, INK, WOODF, 0.55)];
        wallLine(parts, w);
        parts.push(rect(8, d - 6, 10, 6, WOOD, WOOD2, 0.2));
        parts.push(rect(w - 18, d - 6, 10, 6, WOOD, WOOD2, 0.2));
        return box(w, d, parts);
    }

    function bed(w, d, pillows) {
        var parts = [rect(0, 0, w, d, INK, WOODF, 0.8)];
        parts.push(rect(0, 0, w, 12, INK, WOOD2, 0.4));
        parts.push(rect(2, 0, 6, 18, INK, WOOD, 0.25));
        parts.push(rect(w - 8, 0, 6, 18, INK, WOOD, 0.25));
        quilt(parts, 5, 16, w - 10, d - 26);
        var i;
        var pw = (w - 16 - (pillows - 1) * 3) / pillows;
        for (i = 0; i < pillows; i++) {
            parts.push(oval(8 + i * (pw + 3), 18, pw, 22, INK, FABF, 0.18));
            parts.push(line(12 + i * (pw + 3), 24, 4 + i * (pw + 3) + pw, 34, FAB2, 0.12));
        }
        parts.push(line(8, d * 0.62, w - 8, d * 0.62, MET, 0.16));
        parts.push(rect(0, d - 8, w, 8, INK, WOOD2, 0.3));
        return box(w, d, parts);
    }

    function loftBed() {
        var w = 150;
        var d = 206;
        var parts = [];
        [[0, 0], [104, 0], [0, d - 8], [104, d - 8]].forEach(function (at) {
            parts.push(rect(at[0], at[1], 8, 8, INK, DARK, 0.25));
        });
        parts.push(rect(4, 4, 104, d - 8, INK, WOODF, 0.55));
        parts.push(rect(12, 12, 88, 26, INK, WHITE, 0.2));
        parts.push(rect(12, 44, 88, 120, INK, FABF, 0.2));
        parts.push(rect(116, 24, 8, 160, INK, WOOD, 0.3));
        parts.push(rect(138, 24, 8, 160, INK, WOOD, 0.3));
        var i;
        for (i = 0; i < 8; i++) parts.push(line(116, 36 + i * 18, 146, 36 + i * 18, WOOD, 0.24));
        parts.push(rect(18, 78, 78, 46, INK, WOOD2, 0.3));
        return box(w, d, parts);
    }

    function sofaBed(w, d, seats) {
        var drawn = sofa(w, d, seats || 3);
        drawn.parts.push(line(18, d * 0.58, w - 18, d * 0.58, AMBER, 0.28));
        drawn.parts.push(rect(16, d - 6, w - 32, 10, INK, WHITE, 0.25));
        drawn.h = n(d + 4);
        return drawn;
    }

    function nightstand(w, d) {
        var parts = [];
        drawers(parts, 0, 0, w, d, 2);
        return box(w, d, parts);
    }

    function vanity(w, d) {
        var parts = [rect(0, 6, w, d, INK, WOODF, 0.6)];
        parts.push(rect(w * 0.2, 0, w * 0.6, 7, INK, GLASS, 0.35));
        drawers(parts, 4, 14, w * 0.42, d - 16, 2);
        parts.push(oval(w * 0.62, d * 0.35, 16, 12, INK, WHITE, 0.16));
        parts.push(oval(w * 0.78, d * 0.42, 8, 8, MET, STEEL, 0.12));
        return box(w, d + 6, parts);
    }

    function changing(w, d) {
        var parts = [rect(0, 0, w, d, INK, WOODF, 0.65)];
        parts.push(rect(4, 4, 8, d - 8, INK, WOOD2, 0.25));
        parts.push(rect(w - 12, 4, 8, d - 8, INK, WOOD2, 0.25));
        parts.push(oval(14, 10, w - 28, d - 22, INK, WHITE, 0.25));
        parts.push(rect(16, d - 16, w - 32, 10, INK, WOOD2, 0.2));
        return box(w, d, parts);
    }

    function crib(w, d) {
        var parts = [rect(0, 0, w, d, INK, WOODF, 0.6)];
        var i;
        for (i = 6; i < w; i += 6) parts.push(line(i, 4, i, d - 4, WOOD, 0.14));
        parts.push(rect(6, 8, w - 12, 18, INK, WHITE, 0.18));
        parts.push(rect(6, 30, w - 12, d - 40, INK, FABF, 0.18));
        return box(w, d, parts);
    }

    function playpen(w, d) {
        var parts = [rect(0, 0, w, d, INK, WOOD2, 0.6)];
        var i;
        for (i = 8; i < w; i += 8) parts.push(line(i, 3, i, d - 3, WOOD, 0.12));
        for (i = 8; i < d; i += 8) parts.push(line(3, i, w - 3, i, WOOD, 0.12));
        parts.push(rect(8, 8, w - 16, d - 16, INK, FABF, 0.2));
        swing(parts, w * 0.35, d, w * 0.3, Math.PI, Math.PI / 2);
        return box(w, d + w * 0.3, parts);
    }

    function kitchen() {
        var w = 300;
        var wall = 34;
        var baseY = 40;
        var base = 60;
        var parts = [rect(0, 0, 240, wall, INK, WOOD2, 0.55)];
        var i;
        for (i = 0; i < 4; i++) {
            if (i) parts.push(line(i * 60, 2, i * 60, wall - 2, INK, 0.22));
            parts.push(oval(i * 60 + 46, 14, 4, 4, MET, STEEL, 0.1));
        }
        parts.push(rect(182, 4, 54, 26, INK, STEEL, 0.25));
        parts.push(rect(0, baseY, w, base, INK, WOODF, 0.8));
        parts.push(line(0, baseY + base - 8, w, baseY + base - 8, WOOD, 0.22));
        for (i = 1; i < 5; i++) parts.push(line(i * 60, baseY + 2, i * 60, baseY + base - 10, INK, 0.28));
        parts.push(oval(70, baseY + 12, 40, 26, INK, GLASS, 0.22));
        parts.push(oval(84, baseY + 6, 10, 6, MET, STEEL, 0.12));
        parts.push(line(128, baseY + 18, 172, baseY + 18, MET, 0.16));
        parts.push(line(128, baseY + 30, 172, baseY + 30, MET, 0.16));
        parts.push(line(128, baseY + 42, 172, baseY + 42, MET, 0.16));
        burners(parts, 188, baseY + 12, 46, 32, 4);
        parts.push(rect(244, baseY + 3, 52, base - 14, INK, STEEL, 0.28));
        parts.push(line(290, baseY + 18, 290, baseY + 38, MET, 0.35));
        return box(w, baseY + base, parts);
    }

    function cabinet(w, d, doors, kind) {
        var leaf = w / doors;
        var swings = kind !== 'drawers' && kind !== 'file';
        var swingH = swings ? ((kind === 'wall' || kind === 'overhead') ? leaf * 0.85 : leaf) : 0;
        var h = d + swingH;
        var fill = kind === 'glass' ? GLASS : (kind === 'file' ? STEEL : WOODF);
        var parts = [rect(0, 0, w, d, INK, fill, 0.7)];
        var i;
        if (kind === 'drawers') {
            drawers(parts, 3, 3, w - 6, d - 6, 3);
        } else if (kind === 'file') {
            var row;
            for (row = 0; row < 4; row++) {
                parts.push(rect(4, 4 + row * ((d - 8) / 4), w - 8, (d - 12) / 4, INK, WHITE, 0.14));
            }
        } else if (kind === 'mixed') {
            parts.push(rect(2, 2, w / 2 - 3, d - 4, INK, GLASS, 0.22));
            parts.push(line(w / 2, 2, w / 2, d - 2, INK, 0.25));
            books(parts, w / 2 + 4, 6, w / 2 - 8, d - 12);
        } else if (kind === 'glass') {
            parts.push(rect(4, 4, w - 8, d - 8, '#4c9aff', '', 0.16));
            books(parts, 8, 8, w - 16, d * 0.4);
        }
        if (kind !== 'drawers' && kind !== 'file') {
            for (i = 0; i < doors; i++) {
                var x = i * leaf;
                if (i) parts.push(line(x, 2, x, d - 2, INK, 0.24));
                panel(parts, x + 3, 4, leaf - 6, d - 8);
                parts.push(oval(i % 2 === 0 ? x + leaf - 6 : x + 3.5, d * 0.5, 2.4, 4.2, MET, STEEL, 0.12));
                if (i % 2 === 0) swing(parts, x, d, swingH - 0.6, 0, Math.PI / 2);
                else swing(parts, x + leaf, d, swingH - 0.6, Math.PI, Math.PI / 2);
            }
        }
        if (kind === 'wall' || kind === 'overhead') wallLine(parts, w);
        return box(w, h, parts);
    }

    function island() {
        var w = 180;
        var d = 112;
        var parts = [rect(0, 0, w, d, INK, WOODF, 0.85)];
        parts.push(rect(18, 14, w - 36, d - 48, INK, WOOD2, 0.35));
        burners(parts, 36, 22, 72, 40, 4);
        parts.push(line(16, d - 10, w - 16, d - 10, WOOD, 0.24));
        return box(w, d, parts);
    }

    function peninsula() {
        var w = 210;
        var d = 72;
        var parts = [rect(0, 0, w, d, INK, WOODF, 0.8)];
        parts.push(rect(0, 0, 40, d, INK, WOOD2, 0.4));
        parts.push(line(40, 4, 40, d - 4, INK, 0.28));
        parts.push(line(52, d - 14, w - 12, d - 14, WOOD, 0.22));
        return box(w, d, parts);
    }

    function barCounter(w, d) {
        var parts = [rect(0, 0, w, d, INK, WOODF, 0.8)];
        parts.push(rect(0, 0, w, d * 0.55, INK, WOOD2, 0.3));
        parts.push(line(8, d - 6, w - 8, d - 6, MET, 0.4));
        return box(w, d, parts);
    }

    function sinkUnit(w, d, bowls) {
        var parts = [rect(0, 0, w, d, INK, STEEL, 0.7)];
        var i;
        var bw = (w - 16 - (bowls - 1) * 6) / bowls;
        for (i = 0; i < bowls; i++) {
            parts.push(oval(8 + i * (bw + 6), 12, bw, d - 24, INK, GLASS, 0.25));
            parts.push(oval(8 + i * (bw + 6) + bw / 2 - 3, 16, 6, 5, MET, STEEL, 0.1));
        }
        parts.push(line(10, d - 6, w - 10, d - 6, MET, 0.16));
        return box(w, d, parts);
    }

    function dishRack(w, d) {
        var parts = [rect(0, 0, w, d, INK, STEEL, 0.45)];
        var i;
        for (i = 4; i < w; i += 4) parts.push(line(i, 3, i, d - 3, MET, 0.12));
        return box(w, d, parts);
    }

    function fridge(w, d, split) {
        var h = d + w * 0.9;
        var parts = [rect(0, 0, w, d, INK, STEEL, 0.7)];
        parts.push(rect(3, 3, w - 6, d - 6, INK, WHITE, 0.2));
        if (split) parts.push(line(3, d * 0.38, w - 3, d * 0.38, INK, 0.25));
        parts.push(line(w - 6, 12, w - 6, d * (split ? 0.32 : 0.7), MET, 0.4));
        swing(parts, 0, d, w * 0.9 - 1, 0, Math.PI / 2);
        return box(w, h, parts);
    }

    function stove(w, d) {
        var parts = [rect(0, 0, w, d, INK, STEEL, 0.7)];
        burners(parts, 6, 6, w - 12, d * 0.62, 4);
        parts.push(rect(6, d * 0.74, w - 12, d * 0.16, INK, DARK, 0.16));
        return box(w, d, parts);
    }

    function oven(w, d) {
        var parts = [rect(0, 0, w, d, INK, STEEL, 0.65)];
        parts.push(rect(6, 8, w - 12, d - 20, INK, DARK, 0.25));
        parts.push(rect(10, 12, w - 20, d - 28, '#4c9aff', '', 0.12));
        parts.push(line(8, d - 6, w - 8, d - 6, MET, 0.35));
        return box(w, d, parts);
    }

    function hood(w, d) {
        var parts = [rect(6, 8, w - 12, d - 8, INK, STEEL, 0.55)];
        parts.push(rect(0, 0, w, 10, INK, MET, 0.35));
        var i;
        for (i = 12; i < w - 12; i += 8) parts.push(line(i, 16, i, d - 6, MET, 0.14));
        wallLine(parts, w);
        return box(w, d, parts);
    }

    function coffeeMaker() {
        var w = 28;
        var d = 42;
        var parts = [rect(0, 0, w, d, INK, DARK, 0.45)];
        parts.push(rect(4, 6, w - 8, 14, INK, STEEL, 0.16));
        parts.push(oval(7, 24, 14, 12, INK, WHITE, 0.16));
        parts.push(oval(18, 4, 6, 6, GREEN, GREENF, 0.1));
        return box(w, d, parts);
    }

    function cooler(stand) {
        var w = stand ? 44 : 36;
        var d = stand ? 44 : 40;
        var parts = [rect(4, 10, w - 8, d - 10, INK, stand ? WOODF : WHITE, 0.55)];
        if (!stand) {
            parts.push(oval(8, 0, 20, 16, INK, GLASS, 0.3));
            parts.push(rect(8, 22, 8, 8, INK, GLASS, 0.14));
            parts.push(oval(22, 28, 6, 6, GREEN, GREENF, 0.1));
        } else {
            parts.push(oval(w / 2 - 10, 2, 20, 12, INK, '', 0.25));
        }
        return box(w, d, parts);
    }

    function bin(d, outdoor) {
        var parts = [rect(2, 2, d - 4, d - 4, INK, outdoor ? GREENF : STEEL, 0.55)];
        parts.push(oval(d * 0.2, 0, d * 0.6, d * 0.28, INK, outdoor ? '#b3d4c8' : WHITE, 0.25));
        if (!outdoor) parts.push(rect(d * 0.35, d * 0.72, d * 0.3, 4, INK, DARK, 0.12));
        return box(d, d, parts);
    }

    function desk(w, d) {
        var parts = [rect(0, 0, w, d, INK, WOODF, 0.85)];
        parts.push(rect(3, 3, w - 6, d - 6, WOOD, '', 0.2));
        parts.push(rect(10, d - 12, w - 20, 6, WOOD, WOOD2, 0.14));
        parts.push(rect(8, d * 0.38, Math.min(36, w * 0.28), 7, INK, STEEL, 0.12));
        parts.push(oval(w * 0.62, 5, 7, 7, INK, DARK, 0.14));
        if (w > 70) parts.push(line(w * 0.5, 8, w * 0.5, d - 16, WOOD, 0.14));
        legs(parts, 0, 0, w, d, 5, 5);
        return box(w, d, parts);
    }

    function computerDesk(w, d, laptop) {
        var drawn = desk(w, d);
        if (laptop) drawn.parts.push(rect(w * 0.28, d * 0.3, w * 0.4, d * 0.38, INK, STEEL, 0.25));
        else {
            drawn.parts.push(rect(w * 0.3, 5, w * 0.34, 9, INK, DARK, 0.28));
            drawn.parts.push(rect(w * 0.22, d * 0.46, w * 0.4, 7, INK, WHITE, 0.16));
        }
        return drawn;
    }

    function execDesk() {
        var w = 200;
        var d = 96;
        var parts = desk(w, d).parts;
        parts.push(oval(24, 14, 110, 58, INK, '#f7f1e6', 0.22));
        drawers(parts, w - 56, 8, 44, d - 16, 3);
        return box(w, d, parts);
    }

    function pedestalDesk() {
        var w = 168;
        var d = 74;
        var parts = desk(w, d).parts;
        drawers(parts, w - 52, 6, 42, d - 12, 3);
        return box(w, d, parts);
    }

    function cornerDesk(w, d, depth) {
        var parts = [rect(0, 0, w, depth, INK, WOODF, 0.8)];
        parts.push(rect(0, depth - 0.5, depth, d - depth + 0.5, INK, WOODF, 0.8));
        parts.push(rect(8, 8, 42, depth - 16, INK, WOOD2, 0.3));
        drawers(parts, 12, 12, 34, depth - 24, 3);
        legs(parts, w - depth, 4, depth - 4, depth - 8, 4, 4);
        return box(w, d, parts);
    }

    function pedestal(w, d, mobile) {
        var parts = [];
        drawers(parts, 0, 0, w, d, 3);
        if (mobile) wheels(parts, 2, d - 8, w - 4, 8, 6);
        return box(w, mobile ? d + 2 : d, parts);
    }

    function partition(w, thick) {
        var d = 36;
        var parts = [rect(0, 12, w, thick, INK, GLASS, 0.45)];
        parts.push(rect(6, 0, 10, d, INK, STEEL, 0.25));
        parts.push(rect(w - 16, 0, 10, d, INK, STEEL, 0.25));
        return box(w, d, parts);
    }

    function deskScreen(w) {
        var parts = [rect(8, 0, w - 16, 4, INK, FAB2, 0.4)];
        parts.push(rect(0, 0, 8, 16, INK, MET, 0.25));
        parts.push(rect(w - 8, 0, 8, 16, INK, MET, 0.25));
        return box(w, 16, parts);
    }

    function safe(w, d, cupboard) {
        var parts = [rect(0, 0, w, d, INK, cupboard ? STEEL : '#253858', 0.7)];
        parts.push(rect(4, 4, w - 8, d - 8, INK, cupboard ? WHITE : '#1b2a4a', 0.25));
        parts.push(oval(w / 2 - 6, d / 2 - 6, 12, 12, AMBER, '#fff7e6', 0.2));
        parts.push(line(w / 2, d / 2, w / 2 + 4, d / 2, DARK, 0.2));
        if (cupboard) {
            parts.push(line(4, d * 0.33, w - 4, d * 0.33, MET, 0.16));
            parts.push(line(4, d * 0.66, w - 4, d * 0.66, MET, 0.16));
        }
        return box(w, d, parts);
    }

    function cpuStand() {
        var parts = [rect(0, 0, 24, 50, INK, DARK, 0.45)];
        var i;
        for (i = 6; i < 46; i += 4) parts.push(line(3, i, 21, i, MET, 0.12));
        parts.push(oval(16, 6, 4, 4, GREEN, GREENF, 0.1));
        return box(24, 50, parts);
    }

    function reception(w, d, kind) {
        var parts = [];
        if (kind === 'corner') {
            var depth = 78;
            parts.push(rect(0, 0, w, depth, INK, WOODF, 0.8));
            parts.push(rect(w - depth, depth - 0.5, depth, d - depth + 0.5, INK, WOODF, 0.8));
            parts.push(rect(0, 0, w - 20, 28, INK, WOOD2, 0.3));
            parts.push(rect(w - 28, 28, 28, d - 28, INK, WOOD2, 0.3));
            return box(w, d, parts);
        }
        if (kind === 'low') {
            parts.push(rect(100, 0, w - 100, d, INK, WOODF, 0.75));
            parts.push(rect(0, 28, 100, d - 28, INK, GLASS, 0.45));
            parts.push(rect(8, 36, 84, 28, INK, '', 0.2));
            parts.push(oval(108, 16, 16, 16, INK, DARK, 0.16));
            return box(w, d, parts);
        }
        parts.push(rect(0, 0, w, d, INK, WOODF, 0.8));
        parts.push(rect(0, 0, w, 26, INK, WOOD2, 0.3));
        parts.push(oval(w / 2 - 10, 12, 20, 14, INK, DARK, 0.16));
        if (kind === 'module') parts.push(rect(6, 32, w - 12, d - 40, INK, WHITE, 0.18));
        return box(w, d, parts);
    }

    function starChair(w, d, tall) {
        var parts = [];
        var backH = tall ? 16 : 9;
        var cx = w / 2;
        var cy = d * 0.64;
        var a;
        parts.push(rect(w * 0.18, 0, w * 0.64, backH, INK, FAB2, 0.45));
        parts.push(oval(w * 0.16, backH, w * 0.68, d * 0.4, INK, FABF, 0.5));
        parts.push(oval(w * 0.34, backH + d * 0.1, w * 0.32, d * 0.16, MET, '', 0.12));
        if (tall) {
            parts.push(rect(w * 0.08, backH, w * 0.1, d * 0.34, INK, FAB2, 0.28));
            parts.push(rect(w * 0.82, backH, w * 0.1, d * 0.34, INK, FAB2, 0.28));
        }
        parts.push(oval(cx - 5, cy - 5, 10, 10, MET, STEEL, 0.18));
        for (a = 0; a < 5; a++) {
            var angle = -Math.PI / 2 + a * Math.PI * 2 / 5;
            var ex = cx + Math.cos(angle) * w * 0.38;
            var ey = cy + Math.sin(angle) * d * 0.28;
            parts.push(line(cx, cy, ex, ey, MET, 0.3));
            parts.push(oval(ex - 3, ey - 3, 6, 6, INK, DARK, 0.14));
        }
        return box(w, d, parts);
    }

    function sideChair(w, d, arms, tablet) {
        var parts = [rect(w * 0.14, 10, w * 0.72, d - 18, INK, arms ? FABF : WHITE, 0.55)];
        parts.push(rect(w * 0.22, 14, w * 0.56, Math.max(6, d - 30), FAB2, '', 0.16));
        parts.push(rect(w * 0.12, 0, w * 0.76, 9, INK, FAB2, 0.4));
        parts.push(line(w * 0.32, 1, w * 0.32, 8, WOOD, 0.16));
        parts.push(line(w * 0.5, 1, w * 0.5, 8, WOOD, 0.16));
        parts.push(line(w * 0.68, 1, w * 0.68, 8, WOOD, 0.16));
        parts.push(oval(w * 0.38, d * 0.38, w * 0.24, d * 0.16, MET, '', 0.12));
        if (arms) {
            parts.push(rect(0, 10, w * 0.14, d * 0.62, INK, FAB2, 0.28));
            parts.push(rect(w * 0.86, 10, w * 0.14, d * 0.62, INK, FAB2, 0.28));
        }
        if (tablet) parts.push(rect(w * 0.72, d * 0.28, w * 0.26, d * 0.32, INK, WOODF, 0.3));
        legs(parts, 0, 0, w, d, 3, 3.4);
        return box(tablet ? w + 8 : w, d, parts);
    }

    function meetingTable(w, d, shape, video) {
        var drawn = table(w, d, shape);
        if (shape === 'rect' || shape === 'leaf') {
            drawn.parts.push(rect(w / 2 - 14, d / 2 - 8, 28, 16, INK, DARK, 0.2));
        }
        if (video) {
            drawn.parts.push(rect(w / 2 - 36, 0, 72, 8, INK, DARK, 0.35));
            drawn.parts.push(oval(w / 2 - 4, 10, 8, 8, INK, DARK, 0.16));
        }
        return drawn;
    }

    function podium(w, d) {
        var parts = [rect(4, 8, w - 8, d - 8, INK, WOODF, 0.6)];
        parts.push(line(0, 0, w, 0, INK, 0.45));
        parts.push(line(0, 0, 4, 8, INK, 0.35));
        parts.push(line(w, 0, w - 4, 8, INK, 0.35));
        parts.push(oval(w / 2 - 4, 2, 8, 6, INK, DARK, 0.14));
        return box(w, d, parts);
    }

    function board(w, d, kind) {
        var fill = kind === 'cork' ? '#f6e4c2' : (kind === 'class' ? GREENF : WHITE);
        var parts = [rect(0, 0, w, d, INK, fill, 0.6)];
        wallLine(parts, w);
        parts.push(rect(0, d - 3, w, 4, INK, WOOD2, 0.2));
        if (kind === 'cork') {
            parts.push(oval(w * 0.2, d * 0.35, 3, 3, RED, RED, 0.08));
            parts.push(oval(w * 0.55, d * 0.5, 3, 3, '#0747a6', '#0747a6', 0.08));
        }
        if (kind === 'interactive') {
            parts.push(rect(4, 2, w - 8, d - 6, '#4c9aff', '', 0.18));
            parts.push(oval(w - 8, d / 2 - 1.5, 3, 3, INK, GREEN, 0.1));
        }
        return box(w, d + 1, parts);
    }

    function flipchart() {
        var w = 78;
        var d = 70;
        var parts = [rect(8, 0, 62, 7, INK, WHITE, 0.5)];
        parts.push(line(14, 2, 64, 2, MET, 0.2));
        parts.push(line(14, 5, 48, 5, MET, 0.14));
        parts.push(oval(6, 58, 10, 10, INK, DARK, 0.2));
        parts.push(oval(62, 58, 10, 10, INK, DARK, 0.2));
        parts.push(oval(34, 16, 10, 10, INK, DARK, 0.2));
        parts.push(line(39, 7, 11, 62, MET, 0.32));
        parts.push(line(39, 7, 67, 62, MET, 0.32));
        parts.push(line(39, 7, 39, 20, MET, 0.32));
        return box(w, d, parts);
    }

    function easel() {
        var w = 64;
        var d = 70;
        var parts = [rect(12, 0, 40, 6, INK, WHITE, 0.45)];
        parts.push(oval(4, 58, 10, 10, INK, WOOD, 0.2));
        parts.push(oval(50, 58, 10, 10, INK, WOOD, 0.2));
        parts.push(oval(27, 14, 10, 10, INK, WOOD, 0.2));
        parts.push(line(32, 6, 9, 62, WOOD, 0.32));
        parts.push(line(32, 6, 55, 62, WOOD, 0.32));
        parts.push(line(32, 6, 32, 18, WOOD, 0.3));
        return box(w, d, parts);
    }

    function projScreen(w) {
        var d = 14;
        var parts = [rect(0, 0, w, 6, INK, STEEL, 0.45)];
        parts.push(rect(4, 6, w - 8, 4, INK, WHITE, 0.25));
        parts.push(line(8, 8, w - 8, 8, MET, 0.14));
        return box(w, d, parts);
    }

    function studentDesk() {
        var w = 120;
        var d = 52;
        var parts = desk(w, d).parts;
        parts.push(line(w / 2, 4, w / 2, d - 4, INK, 0.25));
        parts.push(rect(6, d - 14, w / 2 - 14, 8, INK, WOOD2, 0.16));
        parts.push(rect(w / 2 + 8, d - 14, w / 2 - 14, 8, INK, WOOD2, 0.16));
        return box(w, d, parts);
    }

    function schoolDesk() {
        var w = 120;
        var d = 56;
        var parts = [rect(0, 6, w, d - 6, INK, WOODF, 0.7)];
        parts.push(line(0, 6, w, 0, INK, 0.35));
        parts.push(line(w, 0, w, 6, INK, 0.35));
        parts.push(rect(4, d - 16, w - 8, 8, INK, WOOD2, 0.18));
        return box(w, d, parts);
    }

    function labDesk() {
        var w = 180;
        var d = 74;
        var parts = desk(w, d).parts;
        parts.push(oval(16, 14, 36, 28, INK, GLASS, 0.22));
        parts.push(oval(118, 16, 8, 8, MET, STEEL, 0.12));
        parts.push(oval(136, 16, 8, 8, MET, STEEL, 0.12));
        return box(w, d, parts);
    }

    function playModule() {
        var w = 140;
        var d = 90;
        var parts = [rect(0, 0, w, d, INK, FABF, 0.6)];
        parts.push(oval(10, 16, 52, 52, INK, WHITE, 0.3));
        parts.push(oval(24, 30, 24, 24, INK, '', 0.2));
        parts.push(rect(74, 14, 52, 58, INK, FAB2, 0.3));
        return box(w, d, parts);
    }

    function softModule() {
        return box(110, 52, [oval(0, 0, 110, 52, INK, FABF, 0.6), oval(10, 8, 90, 36, INK, WHITE, 0.2)]);
    }

    function toyBox() {
        var parts = [rect(0, 0, 72, 46, INK, WOODF, 0.6)];
        parts.push(line(0, 8, 72, 8, WOOD, 0.3));
        parts.push(oval(32, 16, 8, 6, MET, STEEL, 0.12));
        return box(72, 46, parts);
    }

    function couch(w, d, paper) {
        var parts = [rect(0, 0, w, d, INK, WHITE, 0.7)];
        parts.push(rect(4, 8, w - 8, 18, INK, FABF, 0.2));
        if (paper) parts.push(oval(w / 2 - 8, 0, 16, 10, INK, STEEL, 0.18));
        parts.push(line(4, d * 0.42, w - 4, d * 0.42, MET, 0.16));
        wheels(parts, 2, 6, w - 4, d - 12, 6);
        return box(w, d, parts);
    }

    function hospitalBed(functional) {
        var w = 108;
        var d = 222;
        var parts = [rect(10, 12, w - 20, d - 24, INK, WHITE, 0.65)];
        parts.push(rect(0, 36, 8, d - 80, INK, MET, 0.3));
        parts.push(rect(w - 8, 36, 8, d - 80, INK, MET, 0.3));
        parts.push(rect(8, 0, w - 16, 12, INK, STEEL, 0.35));
        parts.push(rect(8, d - 12, w - 16, 12, INK, STEEL, 0.35));
        parts.push(rect(20, 18, w - 40, 26, INK, FABF, 0.18));
        if (functional) {
            parts.push(line(16, 78, w - 16, 78, AMBER, 0.24));
            parts.push(line(16, 140, w - 16, 140, AMBER, 0.24));
        }
        wheels(parts, 6, 8, w - 12, d - 16, 8);
        return box(w, d, parts);
    }

    function gurney() {
        var w = 78;
        var d = 214;
        var parts = [rect(8, 16, w - 16, d - 28, INK, WHITE, 0.6)];
        parts.push(rect(6, 4, w - 12, 8, INK, MET, 0.3));
        wheels(parts, 2, 18, w - 4, d - 36, 10);
        return box(w, d, parts);
    }

    function stretcher() {
        var w = 64;
        var d = 228;
        var parts = [rect(10, 18, w - 20, d - 36, INK, GREENF, 0.55)];
        parts.push(line(6, 8, w - 6, 8, INK, 0.45));
        parts.push(line(6, 8, 6, 18, INK, 0.4));
        parts.push(line(w - 6, 8, w - 6, 18, INK, 0.4));
        parts.push(line(6, d - 8, w - 6, d - 8, INK, 0.45));
        parts.push(line(6, d - 18, 6, d - 8, INK, 0.4));
        parts.push(line(w - 6, d - 18, w - 6, d - 8, INK, 0.4));
        return box(w, d, parts);
    }

    function medTable(w, d, tray) {
        var parts = [rect(0, 0, w, d, INK, tray ? STEEL : WHITE, 0.6)];
        if (tray) parts.push(rect(4, 4, w - 8, d - 8, INK, '', 0.2));
        else drawers(parts, 4, 4, w - 8, d - 8, 2);
        legs(parts, 0, 0, w, d, 3, 3);
        return box(w, d, parts);
    }

    function crossMark(parts, x, y, s) {
        parts.push(rect(x + s * 0.35, y, s * 0.3, s, RED, RED, 0.05));
        parts.push(rect(x, y + s * 0.35, s, s * 0.3, RED, RED, 0.05));
    }

    function medCabinet(w, d, kind) {
        var drawn = cabinet(w, d, 2, kind === 'glass' ? 'glass' : 'base');
        if (kind === 'meds') crossMark(drawn.parts, w / 2 - 8, 8, 16);
        if (kind === 'tools') {
            drawn.parts.push(line(8, 10, w - 8, 10, MET, 0.16));
            drawn.parts.push(line(8, 18, w - 8, 18, MET, 0.16));
        }
        return drawn;
    }

    function procedureChair() {
        var w = 70;
        var d = 150;
        var parts = [rect(12, 0, w - 24, 28, INK, FAB2, 0.4)];
        parts.push(rect(8, 24, w - 16, 70, INK, FABF, 0.5));
        parts.push(rect(14, 96, w - 28, 36, INK, WHITE, 0.3));
        parts.push(rect(0, 40, 8, 36, INK, FAB2, 0.25));
        parts.push(rect(w - 8, 40, 8, 36, INK, FAB2, 0.25));
        return box(w, d, parts);
    }

    function bloodChair() {
        var w = 86;
        var d = 110;
        var parts = [rect(8, 0, 46, 16, INK, FAB2, 0.4)];
        parts.push(rect(8, 14, 50, 52, INK, FABF, 0.45));
        parts.push(rect(58, 28, 26, 22, INK, WHITE, 0.3));
        parts.push(rect(16, 78, 36, 22, INK, WOODF, 0.25));
        return box(w, d, parts);
    }

    function wheelchair() {
        var w = 78;
        var d = 120;
        var parts = [];
        parts.push(oval(0, 30, 58, 58, INK, WHITE, 0.4));
        parts.push(oval(w - 58, 30, 58, 58, INK, WHITE, 0.4));
        parts.push(oval(16, 46, 26, 26, MET, STEEL, 0.16));
        parts.push(oval(w - 42, 46, 26, 26, MET, STEEL, 0.16));
        parts.push(rect(18, 18, 42, 8, INK, FAB2, 0.3));
        parts.push(rect(16, 28, 46, 40, INK, FABF, 0.4));
        parts.push(rect(22, 96, 34, 14, INK, MET, 0.25));
        parts.push(oval(20, 102, 10, 10, INK, DARK, 0.16));
        parts.push(oval(48, 102, 10, 10, INK, DARK, 0.16));
        return box(w, d, parts);
    }

    function foldScreen() {
        var parts = [];
        parts.push(rect(0, 14, 52, 5, INK, WHITE, 0.35));
        parts.push(rect(48, 2, 52, 5, INK, WHITE, 0.35));
        parts.push(rect(96, 14, 52, 5, INK, WHITE, 0.35));
        parts.push(oval(48, 12, 6, 6, MET, STEEL, 0.12));
        parts.push(oval(96, 12, 6, 6, MET, STEEL, 0.12));
        return box(148, 24, parts);
    }

    function medBath() {
        var w = 190;
        var d = 86;
        var parts = [rect(0, 8, w, d - 8, INK, WHITE, 0.7)];
        parts.push(oval(10, 16, w - 20, d - 28, INK, GLASS, 0.25));
        parts.push(rect(0, 0, 36, 16, INK, STEEL, 0.3));
        return box(w, d, parts);
    }

    function luggageRack() {
        var w = 74;
        var d = 48;
        var parts = [rect(0, 0, w, d, INK, WOOD2, 0.5)];
        slats(parts, 0, 0, w, d, 6);
        legs(parts, 0, 0, w, d, 3, 3);
        return box(w, d, parts);
    }

    function minibar() {
        var parts = [rect(0, 0, 46, 48, INK, DARK, 0.5)];
        parts.push(rect(4, 6, 38, 28, INK, '#1b2a4a', 0.2));
        parts.push(line(8, 16, 38, 16, MET, 0.14));
        parts.push(line(8, 24, 38, 24, MET, 0.14));
        parts.push(oval(20, 38, 6, 4, MET, STEEL, 0.1));
        return box(46, 48, parts);
    }

    function showcase(w, d, cold) {
        var parts = [rect(0, 0, w, d, INK, cold ? GLASS : WHITE, 0.6)];
        parts.push(rect(4, 4, w - 8, d - 8, '#4c9aff', '', 0.16));
        if (cold) parts.push(rect(w - 14, d - 12, 8, 6, INK, STEEL, 0.14));
        else parts.push(line(8, d * 0.45, w - 8, d * 0.45, MET, 0.14));
        return box(w, d, parts);
    }

    function cashDesk() {
        var w = 140;
        var d = 70;
        var parts = [rect(0, 0, w, d, INK, WOODF, 0.7)];
        parts.push(rect(0, 0, w, 22, INK, WOOD2, 0.3));
        parts.push(rect(w - 42, 6, 28, 16, INK, DARK, 0.25));
        parts.push(rect(16, 30, 36, 24, INK, STEEL, 0.2));
        return box(w, d, parts);
    }

    function gondola(w, d, islandBay) {
        var parts = [rect(0, 0, w, d, INK, STEEL, 0.6)];
        var i;
        if (islandBay) {
            parts.push(line(w / 2, 3, w / 2, d - 3, INK, 0.35));
            parts.push(line(3, d / 2, w - 3, d / 2, INK, 0.3));
        } else {
            for (i = 1; i < 4; i++) parts.push(line(4, d * i / 4, w - 4, d * i / 4, MET, 0.16));
            parts.push(rect(6, 4, 18, d - 8, INK, WOOD2, 0.16));
        }
        return box(w, d, parts);
    }

    function fittingRoom() {
        var w = 120;
        var d = 120;
        var parts = [];
        parts.push(line(0, 0, w, 0, INK, 1));
        parts.push(line(0, 0, 0, d, INK, 1));
        parts.push(line(w, 0, w, d, INK, 1));
        parts.push(line(0, d, 34, d, INK, 1));
        parts.push(line(w - 34, d, w, d, INK, 1));
        parts.push(rect(22, 4, 76, 6, INK, GLASS, 0.25));
        parts.push(rect(20, 78, 80, 24, INK, FABF, 0.3));
        parts.push(arc(60, d - 40, 24, Math.PI * 0.2, Math.PI * 0.8, 8, FAB2, 0.3));
        return box(w, d, parts);
    }

    function shopCart() {
        var w = 62;
        var d = 100;
        var parts = [rect(8, 18, 46, 60, INK, STEEL, 0.45)];
        var i;
        for (i = 1; i <= 3; i++) parts.push(line(8, 18 + i * 15, 54, 18 + i * 15, MET, 0.12));
        parts.push(line(10, 8, 10, 18, INK, 0.4));
        parts.push(line(52, 8, 52, 18, INK, 0.4));
        parts.push(line(10, 8, 52, 8, INK, 0.45));
        parts.push(oval(4, 78, 14, 14, INK, DARK, 0.2));
        parts.push(oval(44, 78, 14, 14, INK, DARK, 0.2));
        return box(w, d, parts);
    }

    function mannequin() {
        var parts = [oval(15, 0, 16, 16, INK, FAB2, 0.3)];
        parts.push(oval(8, 14, 30, 14, INK, FABF, 0.35));
        parts.push(oval(16, 26, 14, 8, INK, FAB2, 0.2));
        parts.push(oval(12, 34, 6, 8, INK, DARK, 0.14));
        parts.push(oval(28, 34, 6, 8, INK, DARK, 0.14));
        return box(46, 42, parts);
    }

    function gymBench(w, d) {
        var parts = [rect(0, 0, w, d, INK, WOODF, 0.55)];
        slats(parts, 0, 0, w, d, 5);
        legs(parts, 0, 0, w, d, 6, 4);
        return box(w, d, parts);
    }

    function metalLocker(w, d, doors) {
        var parts = [rect(0, 0, w, d, INK, STEEL, 0.6)];
        var i;
        for (i = 1; i < doors; i++) parts.push(line(w * i / doors, 2, w * i / doors, d - 2, INK, 0.25));
        for (i = 0; i < doors; i++) {
            parts.push(oval(w * (i + 0.72) / doors, d * 0.3, 3, 6, MET, DARK, 0.12));
            var j;
            for (j = 8; j < d - 6; j += 6) parts.push(line(w * i / doors + 4, j, w * (i + 1) / doors - 4, j, MET, 0.08));
        }
        return box(w, d, parts);
    }

    function stallBars() {
        var w = 92;
        var d = 16;
        var parts = [rect(0, 4, w, 8, INK, WOODF, 0.45)];
        var i;
        wallLine(parts, w);
        for (i = 6; i < w; i += 8) parts.push(oval(i, 6, 4, 4, INK, WOOD2, 0.12));
        return box(w, d, parts);
    }

    function parallelBars() {
        var w = 78;
        var d = 240;
        var parts = [rect(4, 8, 10, d - 16, INK, WOODF, 0.4)];
        parts.push(rect(w - 14, 8, 10, d - 16, INK, WOODF, 0.4));
        parts.push(rect(0, 6, 22, 10, INK, MET, 0.25));
        parts.push(rect(w - 22, 6, 22, 10, INK, MET, 0.25));
        parts.push(rect(0, d - 16, 22, 10, INK, MET, 0.25));
        parts.push(rect(w - 22, d - 16, 22, 10, INK, MET, 0.25));
        return box(w, d, parts);
    }

    function pullup() {
        var w = 130;
        var d = 70;
        var parts = [rect(8, 0, w - 16, 8, INK, MET, 0.45)];
        parts.push(rect(10, 8, 8, d - 16, INK, STEEL, 0.3));
        parts.push(rect(w - 18, 8, 8, d - 16, INK, STEEL, 0.3));
        parts.push(rect(0, d - 10, 28, 10, INK, DARK, 0.25));
        parts.push(rect(w - 28, d - 10, 28, 10, INK, DARK, 0.25));
        return box(w, d, parts);
    }

    function trainer() {
        var w = 150;
        var d = 96;
        var parts = [rect(0, 20, w, 16, INK, DARK, 0.4)];
        parts.push(rect(w - 36, 4, 28, 28, INK, STEEL, 0.4));
        parts.push(rect(w - 30, 10, 16, 16, INK, DARK, 0.16));
        parts.push(rect(24, 40, 36, 28, INK, FABF, 0.3));
        parts.push(oval(70, 28, 22, 22, INK, STEEL, 0.25));
        return box(w, d, parts);
    }

    function exBike() {
        var w = 54;
        var d = 124;
        var parts = [oval(8, 36, 38, 38, INK, DARK, 0.35)];
        parts.push(oval(16, 44, 22, 22, MET, STEEL, 0.16));
        parts.push(rect(16, 8, 22, 10, INK, FAB2, 0.3));
        parts.push(line(27, 18, 27, 40, MET, 0.3));
        parts.push(rect(10, 0, 34, 6, INK, DARK, 0.25));
        parts.push(line(20, 74, 34, 100, MET, 0.3));
        return box(w, d, parts);
    }

    function treadmill() {
        var w = 88;
        var d = 190;
        var parts = [rect(6, 20, w - 12, d - 28, INK, DARK, 0.5)];
        parts.push(rect(12, 28, w - 24, d - 46, INK, '#253858', 0.2));
        parts.push(rect(8, 0, w - 16, 22, INK, STEEL, 0.4));
        parts.push(rect(16, 4, w - 32, 12, '#4c9aff', '#071426', 0.12));
        return box(w, d, parts);
    }

    function benchPress() {
        var w = 140;
        var d = 180;
        var parts = [rect(w / 2 - 16, 20, 32, 140, INK, FABF, 0.45)];
        parts.push(rect(8, 16, w - 16, 10, INK, MET, 0.35));
        parts.push(rect(12, 6, 10, 30, INK, STEEL, 0.25));
        parts.push(rect(w - 22, 6, 10, 30, INK, STEEL, 0.25));
        parts.push(line(6, 10, w - 6, 10, DARK, 0.45));
        parts.push(oval(2, 4, 14, 14, INK, DARK, 0.2));
        parts.push(oval(w - 16, 4, 14, 14, INK, DARK, 0.2));
        return box(w, d, parts);
    }

    function dumbbellRack() {
        var w = 130;
        var d = 52;
        var parts = [rect(0, 12, w, d - 12, INK, STEEL, 0.5)];
        var i;
        for (i = 0; i < 8; i++) {
            parts.push(oval(8 + i * 15, 0, 12, 12, INK, DARK, 0.18));
            parts.push(line(10 + i * 15, 12, 18 + i * 15, 12, MET, 0.3));
        }
        return box(w, d, parts);
    }

    function mat(w, d) {
        var parts = [rect(0, 0, w, d, INK, GREENF, 0.5)];
        parts.push(rect(6, 6, w - 12, d - 12, GREEN, '', 0.18));
        return box(w, d, parts);
    }

    function ropeCoil() {
        var parts = [oval(0, 0, 42, 42, INK, WOOD2, 0.4)];
        parts.push(arc(21, 21, 12, 0.4, Math.PI * 1.7, 12, WOOD, 0.3));
        parts.push(arc(21, 21, 6, 0.2, Math.PI * 1.4, 8, WOOD, 0.25));
        return box(42, 42, parts);
    }

    function toiletPlan(kind) {
        var wall = kind === 'wall';
        var compact = kind === 'compact';
        var w = wall ? 38 : 40;
        var d = wall ? 58 : (compact ? 76 : 70);
        var bowlY = wall ? 6 : (compact ? 20 : 16);
        var bowlH = wall ? 42 : 44;
        var tank = compact ? 20 : 18;
        var parts = [];
        wallLine(parts, w);
        if (wall) {
            parts.push(rect(6, 1, 26, 6, INK, STEEL, 0.4));
            parts.push(oval(10, 2, 4, 3, INK, MET, 0.12));
            parts.push(oval(24, 2, 4, 3, INK, MET, 0.12));
        } else {
            parts.push(rect(3, 1, 34, tank, INK, WHITE, 0.7));
            parts.push(rect(6, 3, 28, tank - 6, MET, STEEL, 0.14));
            parts.push(line(8, 8, 32, 8, MET, 0.22));
            parts.push(oval(17, 4, 6, 5, INK, MET, 0.16));
            parts.push(oval(8, 14, 3.4, 3.4, INK, MET, 0.1));
            parts.push(oval(28.6, 14, 3.4, 3.4, INK, MET, 0.1));
        }
        parts.push(oval(2, bowlY, w - 4, bowlH, INK, WHITE, 0.75));
        parts.push(oval(7, bowlY + 6, w - 14, bowlH - 14, INK, '', 0.5));
        parts.push(oval(11, bowlY + 12, w - 22, bowlH - 24, INK, GLASS, 0.28));
        parts.push(oval(w / 2 - 3, bowlY + bowlH * 0.42, 6, 8, MET, '', 0.16));
        parts.push(rect(w / 2 - 5, bowlY + bowlH - 4, 10, 7, INK, WHITE, 0.16));
        parts.push(oval(w / 2 - 3, d - 7, 6, 5, INK, STEEL, 0.14));
        return box(w, d, parts);
    }

    function toilet() {
        return toiletPlan('floor');
    }

    function faucet(kind) {
        var parts = [];
        var w;
        var d;
        if (kind === 'kitchen') {
            w = 18;
            d = 30;
            parts.push(oval(3, 14, 12, 12, INK, STEEL, 0.35));
            parts.push(oval(6, 17, 6, 6, MET, '', 0.16));
            parts.push(rect(7, 2, 4, 16, INK, MET, 0.28));
            parts.push(oval(5, 0, 8, 7, INK, STEEL, 0.22));
            parts.push(oval(1, 16, 5, 5, INK, MET, 0.14));
            parts.push(oval(12, 16, 5, 5, INK, MET, 0.14));
        } else if (kind === 'bath') {
            w = 36;
            d = 16;
            parts.push(rect(0, 4, w, 8, INK, STEEL, 0.45));
            parts.push(oval(4, 5, 6, 6, INK, MET, 0.16));
            parts.push(oval(w - 10, 5, 6, 6, INK, MET, 0.16));
            parts.push(rect(w / 2 - 3, 0, 6, 8, INK, MET, 0.28));
            parts.push(oval(w / 2 - 4, 0, 8, 5, INK, STEEL, 0.16));
        } else if (kind === 'shower') {
            w = 16;
            d = 24;
            parts.push(oval(2, 4, 12, 12, INK, STEEL, 0.35));
            parts.push(rect(6, 0, 4, 8, INK, MET, 0.22));
            parts.push(oval(5, 7, 6, 6, MET, '', 0.16));
            parts.push(arc(8, 14, 6, 0.3, Math.PI - 0.3, 8, MET, 0.24));
            parts.push(oval(2, 16, 5, 5, INK, STEEL, 0.14));
        } else if (kind === 'hose') {
            w = 18;
            d = 28;
            parts.push(rect(2, 6, 14, 8, INK, STEEL, 0.32));
            parts.push(oval(4, 7, 5, 6, INK, MET, 0.14));
            parts.push(oval(10, 7, 5, 6, INK, MET, 0.14));
            parts.push(rect(7, 0, 4, 8, INK, MET, 0.24));
            parts.push(oval(4, 14, 10, 8, INK, STEEL, 0.2));
            parts.push(line(9, 18, 9, 24, MET, 0.22));
            parts.push(oval(5, 21, 8, 6, INK, MET, 0.14));
        } else if (kind === 'wall') {
            w = 14;
            d = 22;
            wallLine(parts, w);
            parts.push(rect(3, 2, 8, 8, INK, STEEL, 0.32));
            parts.push(oval(4, 3, 6, 5, INK, MET, 0.14));
            parts.push(rect(5, 8, 4, 9, INK, MET, 0.26));
            parts.push(oval(3, 15, 8, 5, INK, STEEL, 0.16));
        } else {
            w = 20;
            d = 24;
            parts.push(oval(4, 8, 12, 12, INK, STEEL, 0.35));
            parts.push(oval(7, 11, 6, 6, MET, '', 0.14));
            parts.push(oval(1, 10, 5, 5, INK, MET, 0.16));
            parts.push(oval(14, 10, 5, 5, INK, MET, 0.16));
            parts.push(rect(8, 1, 4, 12, INK, MET, 0.28));
            parts.push(oval(6, 0, 8, 5, INK, STEEL, 0.18));
        }
        return box(w, d, parts);
    }

    function bidet() {
        var w = 38;
        var d = 60;
        var parts = [oval(2, 8, 34, 48, INK, WHITE, 0.6)];
        parts.push(oval(10, 20, 18, 24, INK, GLASS, 0.18));
        parts.push(oval(15, 2, 8, 8, MET, STEEL, 0.14));
        return box(w, d, parts);
    }

    function basin(w, d) {
        var parts = [rect(0, 0, w, d, INK, WHITE, 0.65)];
        wallLine(parts, w);
        parts.push(oval(8, 12, w - 16, d - 22, INK, GLASS, 0.25));
        parts.push(rect(w / 2 - 2, 1, 4, 12, INK, MET, 0.24));
        parts.push(oval(w / 2 - 3.5, 10, 7, 5, INK, STEEL, 0.16));
        parts.push(oval(w / 2 - 9, 2, 5, 5, INK, MET, 0.16));
        parts.push(oval(w / 2 + 4, 2, 5, 5, INK, MET, 0.16));
        parts.push(oval(w / 2 - 3, d / 2, 6, 6, MET, '', 0.12));
        parts.push(oval(w / 2 + w * 0.16, d * 0.42, 3, 5, MET, '', 0.1));
        return box(w, d, parts);
    }

    function urinal() {
        var parts = [rect(4, 0, 32, 8, INK, WHITE, 0.4)];
        wallLine(parts, 40);
        parts.push(oval(2, 6, 36, 36, INK, WHITE, 0.55));
        parts.push(oval(14, 18, 12, 14, INK, GLASS, 0.16));
        return box(40, 46, parts);
    }

    function bathtub(w, d) {
        var parts = [rect(0, 0, w, d, INK, WHITE, 0.8)];
        parts.push(rect(5, 4, w - 10, d - 8, MET, '', 0.16));
        parts.push(oval(8, 6, w - 16, d - 12, INK, GLASS, 0.28));
        parts.push(oval(w - 24, 8, 12, 10, MET, STEEL, 0.16));
        parts.push(line(w - 28, 14, w - 16, 14, MET, 0.14));
        parts.push(oval(16, d / 2 - 4, 8, 8, MET, '', 0.12));
        parts.push(oval(w * 0.42, d - 14, 5, 7, MET, '', 0.1));
        return box(w, d, parts);
    }

    function showerCabin() {
        var s = 90;
        var door = 55;
        var parts = [rect(2, 2, s - 4, s - 4, INK, TILE, 0.3)];
        parts.push(line(0, 0, s, 0, INK, 1.1));
        parts.push(line(0, 0, 0, s, INK, 1.1));
        parts.push(line(s, 0, s, s, INK, 1.1));
        parts.push(line(0, s, s - door, s, INK, 1.1));
        parts.push(line(18, 18, s - 18, s - 18, MET, 0.1));
        parts.push(line(s - 18, 18, 18, s - 18, MET, 0.1));
        parts.push(oval(s / 2 - 5, s / 2 - 5, 10, 10, MET, STEEL, 0.16));
        parts.push(oval(12, 8, 8, 8, INK, MET, 0.14));
        parts.push(line(16, 12, 16, s * 0.4, MET, 0.16));
        swing(parts, s - door, s, door - 1, 0, Math.PI / 2);
        return box(s, s + door, parts);
    }

    function showerTray() {
        var s = 90;
        var parts = [rect(0, 0, s, s, INK, WHITE, 0.55)];
        parts.push(oval(s / 2 - 6, s / 2 - 6, 12, 12, MET, STEEL, 0.16));
        parts.push(rect(4, 4, s - 8, s - 8, MET, '', 0.16));
        return box(s, s, parts);
    }

    function towelRail() {
        var w = 64;
        var d = 14;
        var parts = [rect(4, 3, w - 8, 6, INK, MET, 0.35)];
        wallLine(parts, w);
        parts.push(oval(2, 2, 8, 8, INK, STEEL, 0.16));
        parts.push(oval(w - 10, 2, 8, 8, INK, STEEL, 0.16));
        return box(w, d, parts);
    }

    function vanityBasin() {
        var w = 96;
        var d = 50;
        var parts = [rect(0, 0, w, d, INK, WOODF, 0.65)];
        drawers(parts, 4, 8, 34, d - 14, 2);
        parts.push(oval(46, 8, 42, 28, INK, WHITE, 0.25));
        parts.push(oval(62, 4, 10, 8, MET, STEEL, 0.12));
        return box(w, d, parts);
    }

    function grabBar(w, fold) {
        var d = fold ? 28 : 10;
        var parts = [rect(0, fold ? 8 : 2, w, 6, INK, MET, 0.45)];
        parts.push(oval(0, fold ? 6 : 0, 10, 10, INK, STEEL, 0.18));
        parts.push(oval(w - 10, fold ? 6 : 0, 10, 10, INK, STEEL, 0.18));
        if (fold) {
            parts.push(oval(0, 0, 14, 14, AMBER, '', 0.22));
            parts.push(line(8, 8, 26, 2, MET, 0.22));
            wallLine(parts, 20);
        } else wallLine(parts, w);
        return box(w, d, parts);
    }

    function holder() {
        var parts = [rect(0, 0, 14, 10, INK, WHITE, 0.35)];
        wallLine(parts, 14);
        parts.push(oval(2, 3, 10, 6, INK, STEEL, 0.14));
        return box(14, 10, parts);
    }

    function dispenser() {
        var parts = [rect(0, 0, 16, 14, INK, WHITE, 0.4)];
        wallLine(parts, 16);
        parts.push(rect(3, 4, 10, 6, INK, GLASS, 0.12));
        return box(16, 14, parts);
    }

    function handDryer() {
        var parts = [rect(0, 0, 22, 18, INK, WHITE, 0.45)];
        wallLine(parts, 22);
        parts.push(oval(4, 5, 14, 8, INK, STEEL, 0.14));
        return box(22, 18, parts);
    }

    function showerBench() {
        var parts = [rect(0, 6, 76, 28, INK, STEEL, 0.5)];
        slats(parts, 0, 6, 76, 28, 6);
        wallLine(parts, 76);
        parts.push(rect(6, 30, 6, 8, INK, MET, 0.2));
        parts.push(rect(64, 30, 6, 8, INK, MET, 0.2));
        return box(76, 40, parts);
    }

    function showerChair() {
        var parts = [oval(6, 4, 36, 36, INK, WHITE, 0.5)];
        parts.push(oval(14, 12, 8, 8, MET, '', 0.14));
        parts.push(oval(26, 12, 8, 8, MET, '', 0.14));
        parts.push(oval(14, 24, 8, 8, MET, '', 0.14));
        parts.push(oval(26, 24, 8, 8, MET, '', 0.14));
        parts.push(rect(16, 0, 16, 6, INK, MET, 0.25));
        return box(48, 46, parts);
    }

    function palletRack() {
        var w = 280;
        var d = 110;
        var parts = [rect(0, 0, 8, d, INK, AMBER, 0.3)];
        parts.push(rect(w / 2 - 4, 0, 8, d, INK, AMBER, 0.3));
        parts.push(rect(w - 8, 0, 8, d, INK, AMBER, 0.3));
        parts.push(line(0, 8, w, 8, AMBER, 0.45));
        parts.push(line(0, d - 8, w, d - 8, AMBER, 0.45));
        parts.push(rect(18, 16, 110, 78, INK, WOOD2, 0.3));
        slats(parts, 18, 16, 110, 78, 10);
        parts.push(rect(154, 16, 110, 78, INK, WOOD2, 0.3));
        slats(parts, 154, 16, 110, 78, 10);
        return box(w, d, parts);
    }

    function metalShelf(w, d) {
        var parts = [rect(0, 0, w, d, INK, STEEL, 0.6)];
        [[0, 0], [w - 6, 0], [0, d - 6], [w - 6, d - 6]].forEach(function (at) {
            parts.push(rect(at[0], at[1], 6, 6, INK, DARK, 0.2));
        });
        parts.push(line(6, d * 0.33, w - 6, d * 0.33, MET, 0.2));
        parts.push(line(6, d * 0.66, w - 6, d * 0.66, MET, 0.2));
        parts.push(rect(12, 8, 28, 22, INK, WOOD2, 0.16));
        parts.push(rect(48, 8, 36, 18, INK, GLASS, 0.14));
        return box(w, d, parts);
    }

    function cantilever() {
        var w = 240;
        var d = 90;
        var parts = [rect(0, 0, 10, d, INK, AMBER, 0.35)];
        var i;
        for (i = 0; i < 4; i++) {
            parts.push(rect(10, 8 + i * 20, w - 20, 6, INK, STEEL, 0.22));
        }
        return box(w, d, parts);
    }

    function mobileShelf() {
        var w = 210;
        var d = 78;
        var parts = metalShelf(w, d - 10).parts;
        parts.push(rect(0, d - 8, w, 6, INK, DARK, 0.3));
        parts.push(line(0, d - 5, w, d - 5, MET, 0.2));
        return box(w, d, parts);
    }

    function archiveShelf() {
        var w = 220;
        var d = 42;
        var parts = [rect(0, 0, w, d, INK, STEEL, 0.55)];
        var i;
        for (i = 1; i < 8; i++) parts.push(line(w * i / 8, 2, w * i / 8, d - 2, INK, 0.18));
        books(parts, 4, 6, 20, d - 12);
        return box(w, d, parts);
    }

    function cardFile(w, d, fine) {
        var parts = [rect(0, 0, w, d, INK, STEEL, 0.55)];
        var cols = fine ? 6 : 4;
        var rows = fine ? 6 : 4;
        var i;
        var j;
        for (i = 0; i < cols; i++) {
            for (j = 0; j < rows; j++) {
                parts.push(rect(3 + i * ((w - 6) / cols), 3 + j * ((d - 6) / rows), (w - 6) / cols - 1.5, (d - 6) / rows - 1.5, INK, WHITE, 0.08));
            }
        }
        return box(w, d, parts);
    }

    function workbench(shelves) {
        var w = 170;
        var d = 74;
        var parts = [rect(0, 0, w, d, INK, WOODF, 0.75)];
        parts.push(rect(w - 28, 6, 18, 22, INK, MET, 0.3));
        parts.push(oval(w - 22, 10, 8, 8, INK, DARK, 0.14));
        if (shelves) {
            parts.push(line(8, d * 0.55, w - 36, d * 0.55, WOOD, 0.22));
            parts.push(rect(10, d * 0.62, 40, 16, INK, WOOD2, 0.16));
        }
        legs(parts, 0, 0, w, d, 4, 5);
        return box(w, d, parts);
    }

    function pallet() {
        var w = 120;
        var d = 80;
        var parts = [rect(0, 0, w, d, INK, WOOD2, 0.5)];
        slats(parts, 0, 0, w, d, 9);
        parts.push(rect(6, 6, 16, d - 12, INK, WOOD, 0.16));
        parts.push(rect(w / 2 - 8, 6, 16, d - 12, INK, WOOD, 0.16));
        parts.push(rect(w - 22, 6, 16, d - 12, INK, WOOD, 0.16));
        return box(w, d, parts);
    }

    function container() {
        var parts = [rect(0, 0, 90, 70, INK, STEEL, 0.6)];
        var i;
        for (i = 8; i < 86; i += 8) parts.push(line(i, 4, i, 66, MET, 0.12));
        return box(90, 70, parts);
    }

    function ladder(big) {
        var w = big ? 56 : 44;
        var d = big ? 110 : 78;
        var parts = [];
        parts.push(line(6, 0, 2, d, WOOD, 0.45));
        parts.push(line(w - 6, 0, w - 2, d, WOOD, 0.45));
        parts.push(line(18, 0, 14, d, WOOD, 0.4));
        parts.push(line(w - 18, 0, w - 14, d, WOOD, 0.4));
        parts.push(oval(w / 2 - 5, 0, 10, 8, INK, MET, 0.18));
        var i;
        for (i = 16; i < d - 8; i += 14) parts.push(line(8, i, w - 8, i, WOOD, 0.22));
        return box(w, d, parts);
    }

    function platformCart() {
        var w = 80;
        var d = 130;
        var parts = [rect(8, 16, w - 16, d - 30, INK, WOOD2, 0.5)];
        parts.push(line(12, 6, 12, 16, INK, 0.4));
        parts.push(line(w - 12, 6, w - 12, 16, INK, 0.4));
        parts.push(line(12, 6, w - 12, 6, INK, 0.45));
        wheels(parts, 6, 20, w - 12, d - 36, 10);
        return box(w, d, parts);
    }

    function toolCart() {
        var parts = [];
        drawers(parts, 0, 0, 70, 48, 4);
        wheels(parts, 4, 44, 62, 12, 8);
        return box(70, 58, parts);
    }

    function niche() {
        var w = 150;
        var d = 36;
        var parts = [rect(0, 0, w, d, INK, TILE, 0.4)];
        parts.push(line(0, 0, w, 0, INK, 1.2));
        parts.push(line(8, 12, w - 8, 12, WOOD, 0.22));
        parts.push(line(8, 24, w - 8, 24, WOOD, 0.22));
        return box(w, d, parts);
    }

    function podiumDeck() {
        var w = 220;
        var d = 160;
        var parts = [rect(0, 0, w, d - 28, INK, WOODF, 0.7)];
        parts.push(rect(30, d - 28, w - 60, 28, INK, WOOD2, 0.4));
        parts.push(line(8, 8, w - 8, 8, WOOD, 0.18));
        return box(w, d, parts);
    }

    function murphy() {
        var w = 170;
        var d = 46;
        var parts = [rect(0, 0, w, d, INK, WOODF, 0.7)];
        parts.push(rect(8, 6, w - 16, d - 12, INK, WHITE, 0.25));
        parts.push(oval(20, 10, 36, 16, INK, FABF, 0.16));
        parts.push(line(16, 28, w - 16, 28, MET, 0.16));
        parts.push(oval(w / 2 - 3, d - 10, 6, 4, MET, STEEL, 0.12));
        return box(w, d, parts);
    }

    function foldTable() {
        var w = 90;
        var d = 70;
        var parts = [rect(0, 0, w, 8, INK, WOOD2, 0.45)];
        wallLine(parts, w);
        parts.push(rect(4, 8, w - 8, d - 8, INK, WOODF, 0.4));
        parts.push(line(w / 2, 8, w / 2, d, AMBER, 0.22));
        return box(w, d, parts);
    }

    function transformBed() {
        var drawn = bed(170, 210, 2);
        drawn.parts.push(rect(0, 0, 170, 18, INK, WOOD2, 0.45));
        drawn.parts.push(line(8, 18, 20, 36, AMBER, 0.3));
        drawn.parts.push(line(162, 18, 150, 36, AMBER, 0.3));
        return drawn;
    }

    function musicStand() {
        var parts = [rect(10, 0, 36, 8, INK, WOODF, 0.4)];
        parts.push(line(28, 8, 28, 48, MET, 0.35));
        parts.push(line(12, 48, 44, 48, MET, 0.3));
        parts.push(line(28, 36, 8, 52, MET, 0.25));
        parts.push(line(28, 36, 48, 52, MET, 0.25));
        return box(56, 54, parts);
    }

    function parkBench(w, d, back) {
        var parts = [rect(0, back ? 10 : 0, w, d - (back ? 10 : 0), INK, WOODF, 0.55)];
        slats(parts, 0, back ? 10 : 0, w, d - (back ? 10 : 0), 6);
        if (back) {
            parts.push(rect(0, 0, w, 10, INK, WOOD2, 0.4));
            parts.push(rect(8, 0, 6, d, INK, DARK, 0.2));
            parts.push(rect(w - 14, 0, 6, d, INK, DARK, 0.2));
        } else {
            legs(parts, 0, 0, w, d, 8, 5);
        }
        return box(w, d, parts);
    }

    function gardenChair(arms) {
        var w = arms ? 70 : 50;
        var d = arms ? 74 : 58;
        var parts = [rect(arms ? 10 : 4, 12, w - (arms ? 20 : 8), d - 20, INK, WOODF, 0.45)];
        slats(parts, arms ? 10 : 4, 12, w - (arms ? 20 : 8), d - 20, 6);
        parts.push(rect(arms ? 8 : 4, 0, w - (arms ? 16 : 8), 10, INK, WOOD2, 0.35));
        if (arms) {
            parts.push(rect(0, 12, 8, 40, INK, WOOD2, 0.25));
            parts.push(rect(w - 8, 12, 8, 40, INK, WOOD2, 0.25));
        }
        return box(w, d, parts);
    }

    function picnic() {
        var w = 180;
        var d = 170;
        var parts = [rect(0, 50, w, 70, INK, WOODF, 0.6)];
        slats(parts, 0, 50, w, 70, 8);
        parts.push(rect(8, 8, w - 16, 28, INK, WOOD2, 0.4));
        slats(parts, 8, 8, w - 16, 28, 6);
        parts.push(rect(8, d - 36, w - 16, 28, INK, WOOD2, 0.4));
        slats(parts, 8, d - 36, w - 16, 28, 6);
        return box(w, d, parts);
    }

    function gazebo() {
        var s = 320;
        var c = s / 2;
        var r = 148;
        var parts = [rect(36, 36, s - 72, s - 72, INK, WOOD2, 0.3)];
        var i;
        var points = [];
        for (i = 0; i < 8; i++) {
            var angle = -Math.PI / 2 + i * Math.PI / 4;
            points.push([c + Math.cos(angle) * r, c + Math.sin(angle) * r]);
        }
        for (i = 0; i < 8; i++) {
            var a = points[i];
            var b = points[(i + 1) % 8];
            parts.push(line(a[0], a[1], b[0], b[1], INK, 0.7));
            parts.push(rect(a[0] - 5, a[1] - 5, 10, 10, INK, DARK, 0.25));
        }
        parts.push(oval(c - 28, c - 28, 56, 56, INK, WOODF, 0.3));
        return box(s, s, parts);
    }

    function canopy() {
        var w = 360;
        var d = 240;
        var parts = [rect(0, 0, w, d, INK, '#f7f8f9', 0.4)];
        [[12, 12], [w - 26, 12], [12, d - 26], [w - 26, d - 26], [w / 2 - 8, 12], [w / 2 - 8, d - 26]].forEach(function (at) {
            parts.push(rect(at[0], at[1], 16, 16, INK, DARK, 0.3));
        });
        parts.push(line(20, 20, w - 20, 20, MET, 0.3));
        parts.push(line(20, d - 20, w - 20, d - 20, MET, 0.3));
        return box(w, d, parts);
    }

    function umbrella() {
        var s = 220;
        var parts = [oval(0, 0, s, s, INK, FIRE, 0.4)];
        var i;
        for (i = 0; i < 8; i++) {
            var angle = i * Math.PI / 4;
            parts.push(line(s / 2, s / 2, s / 2 + Math.cos(angle) * (s / 2 - 6), s / 2 + Math.sin(angle) * (s / 2 - 6), WOOD, 0.2));
        }
        parts.push(oval(s / 2 - 6, s / 2 - 6, 12, 12, INK, DARK, 0.2));
        return box(s, s, parts);
    }

    function lounger(chaise) {
        var w = chaise ? 76 : 64;
        var d = 196;
        var parts = [rect(chaise ? 8 : 0, 20, w - (chaise ? 16 : 0), d - 28, INK, FABF, 0.5)];
        parts.push(rect(chaise ? 6 : 0, 0, w - (chaise ? 12 : 0), 36, INK, FAB2, 0.4));
        if (chaise) {
            parts.push(rect(0, 30, 8, 80, INK, WOOD2, 0.25));
            parts.push(rect(w - 8, 30, 8, 80, INK, WOOD2, 0.25));
            parts.push(oval(4, d - 16, 10, 10, INK, DARK, 0.16));
            parts.push(oval(w - 14, d - 16, 10, 10, INK, DARK, 0.16));
        }
        return box(w, d, parts);
    }

    function swingSeat() {
        var w = 190;
        var d = 140;
        var parts = [rect(8, 8, 12, 12, INK, DARK, 0.25)];
        parts.push(rect(w - 20, 8, 12, 12, INK, DARK, 0.25));
        parts.push(rect(8, d - 20, 12, 12, INK, DARK, 0.25));
        parts.push(rect(w - 20, d - 20, 12, 12, INK, DARK, 0.25));
        parts.push(line(14, 14, 14, d - 14, MET, 0.35));
        parts.push(line(w - 14, 14, w - 14, d - 14, MET, 0.35));
        parts.push(rect(28, d / 2 - 16, w - 56, 32, INK, WOODF, 0.45));
        slats(parts, 28, d / 2 - 16, w - 56, 32, 6);
        return box(w, d, parts);
    }

    function planter(kind) {
        if (kind === 'pot') {
            return box(42, 42, [oval(0, 0, 42, 42, INK, WOOD2, 0.5), oval(8, 8, 26, 26, GREEN, GREENF, 0.25)]);
        }
        if (kind === 'box') {
            var parts = [rect(0, 0, 120, 36, INK, WOODF, 0.5)];
            parts.push(oval(8, 6, 22, 22, GREEN, GREENF, 0.18));
            parts.push(oval(48, 6, 22, 22, GREEN, GREENF, 0.18));
            parts.push(oval(88, 6, 22, 22, GREEN, GREENF, 0.18));
            return box(120, 36, parts);
        }
        return box(80, 80, [rect(0, 0, 80, 80, INK, '#d7c4a3', 0.55), oval(14, 14, 52, 52, GREEN, GREENF, 0.25)]);
    }

    function fence() {
        var w = 200;
        var d = 16;
        var parts = [rect(0, 4, w, 6, INK, WOOD2, 0.3)];
        var i;
        for (i = 0; i <= 4; i++) parts.push(rect(i * 48, 0, 8, d, INK, WOOD, 0.25));
        return box(w, d, parts);
    }

    function bikePark() {
        var w = 180;
        var d = 56;
        var parts = [rect(0, d - 8, w, 8, INK, DARK, 0.35)];
        var i;
        for (i = 0; i < 4; i++) {
            var x = 16 + i * 42;
            parts.push(line(x, d - 8, x, 18, MET, 0.4));
            parts.push(line(x + 22, d - 8, x + 22, 18, MET, 0.4));
            parts.push(arc(x + 11, 18, 11, Math.PI, Math.PI * 2, 8, MET, 0.4));
        }
        return box(w, d, parts);
    }

    function bikeStand() {
        var parts = [rect(8, 10, 24, 70, INK, STEEL, 0.4)];
        parts.push(oval(4, 0, 32, 18, INK, DARK, 0.25));
        parts.push(line(20, 18, 20, 70, MET, 0.3));
        return box(40, 84, parts);
    }

    function infoStand(w, d) {
        var parts = [rect(w / 2 - 10, d - 14, 20, 14, INK, DARK, 0.25)];
        parts.push(rect(w / 2 - 3, 7, 6, Math.max(4, d - 22), INK, MET, 0.25));
        parts.push(rect(2, 0, w - 4, 6, INK, WHITE, 0.4));
        parts.push(line(6, 3, w - 6, 3, MET, 0.16));
        return box(w, d, parts);
    }

    function waySign() {
        var parts = [oval(11, 30, 14, 14, INK, MET, 0.25)];
        parts.push(rect(2, 6, 32, 6, INK, GREENF, 0.35));
        parts.push(line(6, 9, 28, 9, GREEN, 0.28));
        parts.push(line(18, 12, 18, 32, MET, 0.25));
        return box(36, 48, parts);
    }

    function dumpster() {
        var w = 160;
        var d = 96;
        var parts = [rect(0, 8, w, d - 8, INK, GREENF, 0.7)];
        parts.push(rect(6, 0, w - 12, 12, INK, '#b7e0c8', 0.3));
        wheels(parts, 6, d - 18, w - 12, 16, 12);
        return box(w, d, parts);
    }

    function ramp() {
        var w = 110;
        var d = 180;
        var parts = [rect(0, 0, w, d, INK, STEEL, 0.6)];
        parts.push(rect(0, 0, 8, d, INK, MET, 0.25));
        parts.push(rect(w - 8, 0, 8, d, INK, MET, 0.25));
        var i;
        for (i = 1; i <= 5; i++) parts.push(line(14, d * i / 6, w - 14, d * i / 6 - 12, MET, 0.22));
        parts.push(line(w / 2, d - 20, w / 2, 24, INK, 0.3));
        parts.push(line(w / 2, 24, w / 2 - 8, 36, INK, 0.3));
        parts.push(line(w / 2, 24, w / 2 + 8, 36, INK, 0.3));
        return box(w, d, parts);
    }

    function tactile() {
        var w = 80;
        var d = 40;
        var parts = [rect(0, 0, w, d, INK, '#fff7e6', 0.4)];
        var x;
        var y;
        for (x = 6; x < w - 4; x += 8) {
            for (y = 6; y < d - 4; y += 8) parts.push(oval(x, y, 3.2, 3.2, INK, AMBER, 0.06));
        }
        return box(w, d, parts);
    }

    function accessTable() {
        var w = 110;
        var d = 70;
        var parts = [rect(0, 0, w, d, INK, WOODF, 0.7)];
        parts.push(rect(0, d - 16, 14, 16, INK, WOOD2, 0.25));
        parts.push(rect(w - 14, d - 16, 14, 16, INK, WOOD2, 0.25));
        parts.push(rect(4, 4, w - 8, 8, INK, '', 0.16));
        return box(w, d, parts);
    }

    function adjustableDesk() {
        var drawn = desk(150, 76);
        drawn.parts.push(rect(18, 58, 10, 12, INK, MET, 0.18));
        drawn.parts.push(rect(122, 58, 10, 12, INK, MET, 0.18));
        drawn.parts.push(rect(20, 62, 6, 8, INK, DARK, 0.12));
        drawn.parts.push(rect(124, 62, 6, 8, INK, DARK, 0.12));
        return drawn;
    }

    function ballCart() {
        var parts = [rect(6, 10, 70, 46, INK, STEEL, 0.45)];
        parts.push(oval(12, 4, 16, 16, INK, WHITE, 0.2));
        parts.push(oval(32, 0, 16, 16, INK, WHITE, 0.2));
        parts.push(oval(50, 6, 16, 16, INK, WHITE, 0.2));
        wheels(parts, 4, 40, 74, 18, 10);
        return box(82, 60, parts);
    }

    function decorScreen() {
        var w = 150;
        var d = 28;
        var parts = [rect(0, 6, w, 8, INK, FABF, 0.4)];
        var i;
        for (i = 10; i < w; i += 16) parts.push(oval(i, 0, 10, 10, INK, FAB2, 0.16));
        parts.push(rect(8, 14, 8, 14, INK, WOOD2, 0.16));
        parts.push(rect(w - 16, 14, 8, 14, INK, WOOD2, 0.16));
        return box(w, d, parts);
    }

    function keyCabinet() {
        var parts = [rect(0, 0, 36, 18, INK, WOODF, 0.45)];
        wallLine(parts, 36);
        var i;
        for (i = 0; i < 4; i++) parts.push(arc(6 + i * 8, 10, 2.4, 0.3, Math.PI - 0.2, 4, MET, 0.14));
        return box(36, 18, parts);
    }

    function organizer() {
        var parts = [rect(0, 0, 48, 16, INK, WOODF, 0.4)];
        wallLine(parts, 48);
        parts.push(rect(3, 4, 12, 10, INK, WHITE, 0.12));
        parts.push(rect(18, 4, 12, 10, INK, GLASS, 0.12));
        parts.push(rect(33, 4, 12, 10, INK, FABF, 0.12));
        return box(48, 16, parts);
    }

    function ironBoard() {
        var w = 38;
        var d = 128;
        var parts = [rect(4, 0, w - 8, 16, INK, STEEL, 0.35)];
        parts.push(rect(0, 12, w, d - 24, INK, FABF, 0.45));
        parts.push(line(6, 20, w - 8, d - 16, MET, 0.2));
        parts.push(line(w - 6, 20, 8, d - 16, MET, 0.2));
        return box(w, d, parts);
    }

    function shoeCab() {
        var parts = [];
        drawers(parts, 0, 0, 90, 28, 2);
        return box(90, 28, parts);
    }

    function hostStand() {
        var parts = [rect(0, 0, 70, 50, INK, WOODF, 0.6)];
        parts.push(rect(8, 6, 54, 8, INK, WOOD2, 0.2));
        parts.push(rect(18, 20, 34, 22, INK, WHITE, 0.18));
        return box(70, 50, parts);
    }

    function drinkRack() {
        var parts = shelf(80, 40, 4, 3, false);
        var i;
        for (i = 0; i < 4; i++) parts.parts.push(oval(8 + i * 18, 6, 8, 14, INK, GLASS, 0.12));
        return parts;
    }

    function builtWardrobe(w, d) {
        var drawn = sliding(w, d);
        drawn.parts.unshift(line(0, 0, w, 0, INK, 1.3));
        return drawn;
    }

    function tallPantry() {
        var drawn = cabinet(60, 60, 1, 'base');
        drawn.parts.push(line(6, 16, 54, 16, WOOD, 0.18));
        drawn.parts.push(line(6, 28, 54, 28, WOOD, 0.18));
        drawn.parts.push(line(6, 40, 54, 40, WOOD, 0.18));
        return drawn;
    }

    function chestFreezer() {
        var parts = [rect(0, 0, 100, 68, INK, WHITE, 0.7)];
        parts.push(rect(5, 8, 90, 54, INK, GLASS, 0.22));
        parts.push(line(6, 6, 94, 6, MET, 0.35));
        return box(100, 68, parts);
    }

    function hob(w, d) {
        var parts = [rect(0, 0, w, d, INK, DARK, 0.6)];
        burners(parts, 6, 6, w - 12, d - 12, 4);
        return box(w, d, parts);
    }

    function microwave() {
        var w = 52;
        var d = 36;
        var leaf = 40;
        var parts = [rect(0, 0, w, d, INK, STEEL, 0.55)];
        parts.push(rect(6, 5, w - 16, d - 12, INK, DARK, 0.2));
        parts.push(oval(w / 2 - 8, d / 2 - 6, 14, 12, '#4c9aff', '', 0.12));
        parts.push(line(w - 5, 8, w - 5, d - 8, MET, 0.3));
        swing(parts, 0, d, leaf, 0, Math.PI / 2);
        return box(w, d + leaf, parts);
    }

    function dishwasher() {
        var drawn = cabinet(60, 60, 1, 'base');
        drawn.parts.push(line(8, 18, 52, 18, MET, 0.16));
        drawn.parts.push(line(8, 30, 52, 30, MET, 0.16));
        drawn.parts.push(line(8, 42, 52, 42, MET, 0.16));
        return drawn;
    }

    function lecternDesk() {
        var w = 70;
        var d = 54;
        var parts = [rect(0, 10, w, d - 10, INK, WOODF, 0.65)];
        parts.push(line(0, 10, w, 0, INK, 0.4));
        parts.push(line(w, 0, w, 10, INK, 0.35));
        parts.push(rect(8, d - 16, w - 16, 8, INK, WOOD2, 0.16));
        return box(w, d, parts);
    }

    function wallGondola() {
        var drawn = gondola(150, 40, false);
        drawn.parts.unshift(line(0, 0.6, drawn.w, 0.6, INK, 1.1));
        return drawn;
    }

    function glassCase(w, d) {
        var parts = [rect(0, 0, w, d, INK, GLASS, 0.35)];
        parts.push(rect(5, 5, w - 10, d - 10, '#4c9aff', '', 0.2));
        parts.push(line(5, 5, w - 5, d - 5, '#4c9aff', 0.12));
        parts.push(line(w - 5, 5, 5, d - 5, '#4c9aff', 0.12));
        return box(w, d, parts);
    }

    function cageCart() {
        var w = 90;
        var d = 120;
        var parts = [rect(6, 14, w - 12, d - 28, INK, STEEL, 0.45)];
        var i;
        for (i = 1; i < 4; i++) parts.push(line(6, 14 + i * ((d - 28) / 4), w - 6, 14 + i * ((d - 28) / 4), MET, 0.14));
        parts.push(line(10, 4, 10, 14, INK, 0.4));
        parts.push(line(w - 10, 4, w - 10, 14, INK, 0.4));
        parts.push(line(10, 4, w - 10, 4, INK, 0.4));
        wheels(parts, 4, 18, w - 8, d - 32, 10);
        return box(w, d, parts);
    }

    function transformWardrobe() {
        var drawn = wardrobe(140, 60, 2);
        drawn.parts.push(rect(18, 78, 64, 8, INK, WOOD2, 0.25));
        return drawn;
    }

    function wcCombined() {
        var w = 210;
        var d = 260;
        var parts = [];
        unitWalls(parts, w, d, 70, 80);
        parts = parts.concat(moved(bathtub(170, 75).parts, 20, 16));
        parts = parts.concat(moved(toilet().parts, 22, 96));
        parts = parts.concat(moved(basin(70, 48).parts, 112, 104));
        parts.push(rect(118, 96, 70, 6, INK, GLASS, 0.16));
        return box(w, d, parts);
    }

    function wcBath() {
        var w = 180;
        var d = 240;
        var parts = [];
        unitWalls(parts, w, d, 54, 70);
        parts = parts.concat(moved(bathtub(160, 75).parts, 10, 16));
        parts = parts.concat(moved(basin(64, 46).parts, 96, 108));
        parts = parts.concat(moved(towelRail().parts, 16, 100));
        return box(w, d, parts);
    }

    function wcShower() {
        var w = 200;
        var d = 230;
        var parts = [];
        unitWalls(parts, w, d, 50, 70);
        parts = parts.concat(moved(showerCabin().parts, 16, 16));
        parts = parts.concat(moved(basin(62, 46).parts, 124, 168));
        return box(w, d, parts);
    }

    function wcSplit() {
        var w = 150;
        var d = 190;
        var parts = [];
        unitWalls(parts, w, d, 28, 70);
        parts = parts.concat(moved(toilet().parts, 22, 18));
        parts = parts.concat(moved(basin(60, 42).parts, 74, 22));
        return box(w, d, parts);
    }

    function wcStall() {
        var w = 90;
        var d = 150;
        var parts = [];
        unitWalls(parts, w, d, 12, 66);
        parts = parts.concat(moved(toilet().parts, 25, 18));
        parts.push(line(10, 16, 10, 100, MET, 0.2));
        parts.push(line(w - 10, 16, w - 10, 100, MET, 0.2));
        return box(w, d, parts);
    }

    function wcPublic() {
        var w = 340;
        var d = 220;
        var parts = [];
        var i;
        unitWalls(parts, w, d, 250, 70);
        for (i = 0; i < 3; i++) {
            var x = 14 + i * 78;
            parts.push(rect(x, 14, 4, 108, INK, TILE, 0.25));
            parts.push(rect(x + 70, 14, 4, 108, INK, TILE, 0.25));
            parts.push(rect(x, 14, 74, 4, INK, TILE, 0.25));
            parts = parts.concat(moved(toilet().parts, x + 18, 24));
            parts.push(line(x + 8, 122, x + 8, 174, INK, 0.35));
            parts.push(arc(x + 8, 122, 52, 0, Math.PI / 2, 8, MET, 0.22));
        }
        parts = parts.concat(moved(urinal().parts, 250, 16));
        parts = parts.concat(moved(urinal().parts, 292, 16));
        parts = parts.concat(moved(basin(70, 42).parts, 248, 110));
        return box(w, d, parts);
    }

    function washroom() {
        var w = 250;
        var d = 150;
        var parts = [];
        var i;
        unitWalls(parts, w, d, 90, 70);
        for (i = 0; i < 3; i++) parts = parts.concat(moved(basin(64, 46).parts, 16 + i * 74, 18));
        parts.push(rect(16, 70, 210, 6, INK, GLASS, 0.16));
        return box(w, d, parts);
    }

    function showerRoom() {
        var w = 210;
        var d = 240;
        var parts = [];
        unitWalls(parts, w, d, 70, 70);
        parts = parts.concat(moved(showerTray().parts, 16, 16));
        parts = parts.concat(moved(showerTray().parts, 104, 16));
        parts = parts.concat(moved(showerBench().parts, 16, 120));
        return box(w, d, parts);
    }

    function wcAccess() {
        var w = 220;
        var d = 220;
        var parts = [];
        unitWalls(parts, w, d, 16, 90);
        parts = parts.concat(moved(toilet().parts, 28, 36));
        parts = parts.concat(moved(basin(64, 46).parts, 130, 20));
        parts = parts.concat(moved(grabBar(70, false).parts, 24, 24));
        parts = parts.concat(moved(grabBar(76, true).parts, 78, 70));
        parts.push(oval(40, 90, 150, 100, MET, '', 0.16));
        return box(w, d, parts);
    }

    function row(id, name, drawn) {
        return { id: id, name: name, w: drawn.w, h: drawn.h, parts: drawn.parts };
    }

    function pack(id, name, templates) {
        return { id: id, name: name, templates: templates };
    }

    function furniture() {
        return [
            pack('lib-fur-plumb', 'Сантехника', [
                row('fur-plumb-wc', 'Унитаз напольный', toiletPlan('floor')),
                row('fur-plumb-wall', 'Унитаз подвесной', toiletPlan('wall')),
                row('fur-plumb-compact', 'Унитаз компакт', toiletPlan('compact')),
                row('fur-plumb-bidet', 'Биде', bidet()),
                row('fur-plumb-urin', 'Писсуар', urinal()),
                row('fur-plumb-tap', 'Кран для раковины', faucet('basin')),
                row('fur-plumb-tap-kit', 'Кран кухонный', faucet('kitchen')),
                row('fur-plumb-tap-bath', 'Кран для ванны', faucet('bath')),
                row('fur-plumb-tap-shower', 'Смеситель душевой', faucet('shower')),
                row('fur-plumb-tap-hose', 'Кран с душевой лейкой', faucet('hose')),
                row('fur-plumb-tap-wall', 'Кран водоразборный', faucet('wall')),
                row('fur-plumb-sink', 'Раковина со смесителем', basin(62, 48)),
                row('fur-plumb-tub', 'Ванна со смесителем', bathtub(170, 75))
            ]),
            pack('lib-fur-live', 'Жилые комнаты', [
                row('fur-live-sofa', 'Диван прямой', sofa(210, 94, 3)),
                row('fur-live-corner', 'Диван угловой', cornerSofa(250, 176, 94)),
                row('fur-live-modular', 'Диван модульный', modularSofa(86, 94, 3)),
                row('fur-live-armchair', 'Кресло мягкое', sofa(92, 96, 1)),
                row('fur-live-chairbed', 'Кресло-кровать', sofaBed(96, 98, 1)),
                row('fur-live-pouf', 'Пуф', pouf(46)),
                row('fur-live-bench', 'Банкетка', bench(120, 42, true)),
                row('fur-live-stool', 'Табурет', stool(36)),
                row('fur-live-coffee', 'Журнальный столик', table(110, 60, 'rect')),
                row('fur-live-side', 'Кофейный столик', table(52, 52, 'round')),
                row('fur-live-tv', 'ТВ-тумба', tv(160, 44, false)),
                row('fur-live-tvset', 'ТВ-панель на тумбе', tv(160, 44, true)),
                row('fur-live-shelf', 'Стеллаж', shelf(100, 36, 3, 4, false)),
                row('fur-live-wshelf', 'Полка настенная', wallShelf(120, 24)),
                row('fur-live-books', 'Шкаф для книг', shelf(90, 36, 2, 5, true)),
                row('fur-live-dresser', 'Комод', dresser(120, 50, 4)),
                row('fur-live-ward', 'Шкаф платяной', wardrobe(140, 62, 2)),
                row('fur-live-slide', 'Шкаф-купе', sliding(200, 66)),
                row('fur-live-closet', 'Гардероб', closet(220, 180)),
                row('fur-live-stand', 'Вешалка напольная', coatStand(42)),
                row('fur-live-bar', 'Вешалка настенная', coatBar(90)),
                row('fur-live-mirror', 'Зеркало настенное', mirror(70, 8, false)),
                row('fur-live-lamp', 'Торшер', floorLamp()),
                row('fur-live-arc', 'Напольная лампа', arcLamp()),
                row('fur-live-rug', 'Ковёр', rug(200, 140)),
                row('fur-live-basket', 'Корзина', basket(38, false)),
                row('fur-live-fire', 'Камин', fireplace()),
                row('fur-live-mantel', 'Каминная полка', mantel(150, 22))
            ]),
            pack('lib-fur-bed', 'Спальни', [
                row('fur-bed-single', 'Кровать односпальная', bed(90, 200, 1)),
                row('fur-bed-double', 'Кровать двуспальная', bed(160, 200, 2)),
                row('fur-bed-child', 'Кровать детская', bed(70, 160, 1)),
                row('fur-bed-loft', 'Кровать-чердак', loftBed()),
                row('fur-bed-sofabed', 'Диван-кровать', sofaBed(200, 96)),
                row('fur-bed-night', 'Тумба прикроватная', nightstand(45, 40)),
                row('fur-bed-dresser', 'Комод', dresser(100, 48, 4)),
                row('fur-bed-ward', 'Шкаф платяной', wardrobe(120, 60, 2)),
                row('fur-bed-slide', 'Шкаф-купе', sliding(180, 64)),
                row('fur-bed-closet', 'Гардероб', closet(200, 160)),
                row('fur-bed-vanity', 'Трюмо', vanity(100, 46)),
                row('fur-bed-dressing', 'Туалетный столик', vanity(120, 50)),
                row('fur-bed-pouf', 'Пуф', pouf(40)),
                row('fur-bed-bench', 'Банкетка', bench(110, 40, false)),
                row('fur-bed-chair', 'Стул', sideChair(46, 52, false, false)),
                row('fur-bed-arm', 'Кресло', sofa(84, 90, 1)),
                row('fur-bed-change', 'Пеленальный стол', changing(90, 60)),
                row('fur-bed-crib', 'Детская кроватка', crib(70, 120)),
                row('fur-bed-pen', 'Манеж', playpen(100, 100)),
                row('fur-bed-linen', 'Шкаф для белья', cabinet(90, 50, 2, 'drawers'))
            ]),
            pack('lib-fur-kit', 'Кухни и столовые', [
                row('fur-kit-run', 'Кухонный гарнитур', kitchen()),
                row('fur-kit-cab', 'Кухонный шкаф', cabinet(60, 60, 1, 'base')),
                row('fur-kit-wall', 'Навесной шкаф', cabinet(80, 35, 2, 'wall')),
                row('fur-kit-base', 'Тумба кухонная', cabinet(60, 60, 1, 'drawers')),
                row('fur-kit-tall', 'Пенал кухонный', tallPantry()),
                row('fur-kit-island', 'Остров кухонный', island()),
                row('fur-kit-penin', 'Полуостров', peninsula()),
                row('fur-kit-bar', 'Барная стойка', barCounter(180, 48)),
                row('fur-kit-table', 'Стол обеденный', table(160, 90, 'rect')),
                row('fur-kit-round', 'Стол круглый', table(110, 110, 'round')),
                row('fur-kit-oval', 'Стол овальный', table(180, 90, 'oval')),
                row('fur-kit-square', 'Стол квадратный', table(90, 90, 'rect')),
                row('fur-kit-leaf', 'Стол-трансформер', table(140, 90, 'leaf')),
                row('fur-kit-chair', 'Стул обеденный', sideChair(46, 54, false, false)),
                row('fur-kit-stool', 'Табурет', stool(34)),
                row('fur-kit-barstool', 'Табурет барный', barSeat(false)),
                row('fur-kit-banq', 'Диван-банкетка', bench(170, 58, true)),
                row('fur-kit-sink', 'Мойка', sinkUnit(80, 50, 2)),
                row('fur-kit-dry', 'Сушилка для посуды', dishRack(50, 38)),
                row('fur-kit-fridge', 'Холодильник', fridge(60, 66, true)),
                row('fur-kit-freeze', 'Морозильник', chestFreezer()),
                row('fur-kit-stove', 'Плита', stove(60, 60)),
                row('fur-kit-hob', 'Варочная панель', hob(60, 52)),
                row('fur-kit-oven', 'Духовой шкаф', oven(60, 60)),
                row('fur-kit-micro', 'Микроволновая печь', microwave()),
                row('fur-kit-dish', 'Посудомоечная машина', dishwasher()),
                row('fur-kit-hood', 'Вытяжка', hood(90, 48)),
                row('fur-kit-coffee', 'Кофемашина', coffeeMaker()),
                row('fur-kit-cool', 'Кулер', cooler(false)),
                row('fur-kit-cstand', 'Подставка под кулер', cooler(true)),
                row('fur-kit-bin', 'Урна', bin(34, false)),
                row('fur-kit-china', 'Шкаф для посуды', cabinet(90, 46, 2, 'glass')),
                row('fur-kit-food', 'Стеллаж для продуктов', shelf(90, 46, 3, 4, false))
            ]),
            pack('lib-fur-hall', 'Прихожие и гардеробные', [
                row('fur-hall-ward', 'Шкаф для одежды', wardrobe(110, 60, 2)),
                row('fur-hall-slide', 'Шкаф-купе', sliding(180, 62)),
                row('fur-hall-closet', 'Гардероб', closet(200, 170)),
                row('fur-hall-stand', 'Вешалка напольная', coatStand(40)),
                row('fur-hall-bar', 'Вешалка настенная', coatBar(100)),
                row('fur-hall-hook', 'Крючок настенный', hook()),
                row('fur-hall-shoes', 'Тумба для обуви', shoeCab()),
                row('fur-hall-sshelf', 'Полка для обуви', shelf(90, 32, 1, 4, false)),
                row('fur-hall-bench', 'Банкетка', bench(90, 38, false)),
                row('fur-hall-pouf', 'Пуф', pouf(42)),
                row('fur-hall-mirror', 'Зеркало', floorMirror(60, 36)),
                row('fur-hall-console', 'Консоль', table(110, 36, 'rect')),
                row('fur-hall-table', 'Столик прихожий', table(70, 40, 'rect')),
                row('fur-hall-keys', 'Шкаф для ключей', keyCabinet()),
                row('fur-hall-org', 'Органайзер', organizer()),
                row('fur-hall-basket', 'Корзина для белья', basket(44, true)),
                row('fur-hall-iron', 'Гладильная доска', ironBoard())
            ]),
            pack('lib-fur-off', 'Офисы', [
                row('fur-off-desk', 'Рабочий стол', desk(140, 70)),
                row('fur-off-exec', 'Стол руководителя', execDesk()),
                row('fur-off-straight', 'Прямой стол', desk(160, 70)),
                row('fur-off-corner', 'Угловой стол', cornerDesk(160, 160, 70)),
                row('fur-off-peddesk', 'Стол с тумбой', pedestalDesk()),
                row('fur-off-pc', 'Компьютерный стол', computerDesk(140, 70, false)),
                row('fur-off-lap', 'Стол для ноутбука', computerDesk(100, 56, true)),
                row('fur-off-mped', 'Тумба подкатная', pedestal(42, 52, true)),
                row('fur-off-sped', 'Тумба стационарная', pedestal(42, 56, false)),
                row('fur-off-dresser', 'Комод', dresser(90, 48, 3)),
                row('fur-off-file', 'Шкаф для документов', cabinet(80, 42, 2, 'file')),
                row('fur-off-glass', 'Шкаф со стеклом', cabinet(90, 42, 2, 'glass')),
                row('fur-off-closed', 'Шкаф закрытый', cabinet(90, 46, 2, 'base')),
                row('fur-off-mix', 'Шкаф комбинированный', cabinet(90, 42, 2, 'mixed')),
                row('fur-off-over', 'Антресоль', cabinet(100, 36, 2, 'overhead')),
                row('fur-off-shelf', 'Стеллаж офисный', shelf(100, 36, 3, 4, false)),
                row('fur-off-wshelf', 'Полка настенная', wallShelf(100, 24)),
                row('fur-off-part', 'Перегородка офисная', partition(160, 8)),
                row('fur-off-screen', 'Экран-перегородка', deskScreen(140)),
                row('fur-off-task', 'Офисное кресло', starChair(66, 68, false)),
                row('fur-off-execc', 'Кресло руководителя', starChair(78, 84, true)),
                row('fur-off-side', 'Стул офисный', sideChair(50, 54, false, false)),
                row('fur-off-visit', 'Стул посетителя', sideChair(52, 56, true, false)),
                row('fur-off-stool', 'Табурет', stool(36)),
                row('fur-off-stand', 'Вешалка', coatStand(40)),
                row('fur-off-closet', 'Гардероб', closet(180, 150)),
                row('fur-off-safe', 'Сейф', safe(48, 46, false)),
                row('fur-off-scab', 'Сейф-шкаф', safe(70, 52, true)),
                row('fur-off-bin', 'Корзина для бумаг', bin(30, false)),
                row('fur-off-cpu', 'Подставка под системный блок', cpuStand()),
                row('fur-off-rec', 'Стойка ресепшн', reception(200, 80, 'straight')),
                row('fur-off-rcorn', 'Ресепшн угловой', reception(240, 200, 'corner')),
                row('fur-off-rstr', 'Ресепшн прямой', reception(240, 74, 'straight')),
                row('fur-off-rmod', 'Модуль ресепшн', reception(90, 74, 'module')),
                row('fur-off-wait', 'Диван для ожидания', sofa(180, 82, 3)),
                row('fur-off-wchair', 'Кресло для ожидания', sofa(78, 82, 1)),
                row('fur-off-bench', 'Банкетка', bench(140, 46, true)),
                row('fur-off-coffee', 'Журнальный столик', table(90, 50, 'rect')),
                row('fur-off-side-tbl', 'Столик для кофе', table(50, 50, 'round')),
                row('fur-off-cool', 'Кулер', cooler(false)),
                row('fur-off-cstand', 'Подставка под кулер', cooler(true))
            ]),
            pack('lib-fur-meet', 'Переговорные', [
                row('fur-meet-table', 'Стол переговоров', meetingTable(240, 120, 'rect', false)),
                row('fur-meet-conf', 'Конференц-стол', meetingTable(420, 140, 'rect', false)),
                row('fur-meet-leaf', 'Стол-трансформер', meetingTable(200, 100, 'leaf', false)),
                row('fur-meet-round', 'Круглый стол', meetingTable(150, 150, 'round', false)),
                row('fur-meet-oval', 'Овальный стол', meetingTable(280, 120, 'oval', false)),
                row('fur-meet-video', 'Стол для видеоконференций', meetingTable(300, 120, 'rect', true)),
                row('fur-meet-chair', 'Стул для конференций', starChair(60, 64, false)),
                row('fur-meet-arm', 'Кресло для конференций', starChair(72, 76, true)),
                row('fur-meet-tab', 'Стул с планшетом', sideChair(70, 58, true, true)),
                row('fur-meet-pod', 'Трибуна', podium(64, 54)),
                row('fur-meet-lect', 'Кафедра', podium(78, 60)),
                row('fur-meet-mark', 'Доска маркерная', board(200, 12, 'marker')),
                row('fur-meet-cork', 'Доска пробковая', board(140, 10, 'cork')),
                row('fur-meet-flip', 'Флипчарт', flipchart()),
                row('fur-meet-screen', 'Экран проекционный', projScreen(220)),
                row('fur-meet-proj', 'Подставка под проектор', table(50, 50, 'rect')),
                row('fur-meet-shelf', 'Стеллаж для документации', shelf(100, 36, 3, 4, true)),
                row('fur-meet-file', 'Шкаф для документов', cabinet(80, 42, 2, 'file')),
                row('fur-meet-stand', 'Вешалка', coatStand(40)),
                row('fur-meet-bin', 'Урна', bin(32, false))
            ]),
            pack('lib-fur-edu', 'Учебные и детские', [
                row('fur-edu-desk', 'Ученический стол', studentDesk()),
                row('fur-edu-chair', 'Ученический стул', sideChair(42, 44, false, false)),
                row('fur-edu-tdesk', 'Стол учителя', desk(140, 70)),
                row('fur-edu-tchair', 'Стул учителя', starChair(60, 64, false)),
                row('fur-edu-board', 'Доска классная', board(360, 14, 'class')),
                row('fur-edu-inter', 'Доска интерактивная', board(170, 14, 'interactive')),
                row('fur-edu-flip', 'Флипчарт', flipchart()),
                row('fur-edu-books', 'Шкаф для учебников', cabinet(90, 42, 2, 'base')),
                row('fur-edu-shelf', 'Стеллаж для книг', shelf(100, 36, 2, 5, true)),
                row('fur-edu-toys', 'Стеллаж для игрушек', shelf(100, 40, 3, 3, false)),
                row('fur-edu-school', 'Парта', schoolDesk()),
                row('fur-edu-stand', 'Конторка', lecternDesk()),
                row('fur-edu-draw', 'Стол для рисования', desk(100, 64)),
                row('fur-edu-easel', 'Мольберт', easel()),
                row('fur-edu-lab', 'Стол лабораторный', labDesk()),
                row('fur-edu-pc', 'Стол компьютерный', computerDesk(120, 64, false)),
                row('fur-edu-pcchair', 'Кресло компьютерное', starChair(64, 66, false)),
                row('fur-edu-sofa', 'Диван мягкий', sofa(160, 84, 2)),
                row('fur-edu-pouf', 'Пуф', pouf(42)),
                row('fur-edu-bench', 'Банкетка', bench(100, 40, false)),
                row('fur-edu-crib', 'Детская кроватка', crib(70, 120)),
                row('fur-edu-pen', 'Манеж', playpen(110, 110)),
                row('fur-edu-change', 'Пеленальный стол', changing(90, 60)),
                row('fur-edu-coat', 'Шкаф для одежды', wardrobe(100, 55, 2)),
                row('fur-edu-sup', 'Шкаф для пособий', cabinet(80, 42, 2, 'mixed')),
                row('fur-edu-box', 'Ящик для игрушек', toyBox()),
                row('fur-edu-play', 'Игровой модуль', playModule()),
                row('fur-edu-soft', 'Мягкий модуль', softModule()),
                row('fur-edu-mat', 'Мат', mat(200, 100)),
                row('fur-edu-rope', 'Канат', ropeCoil()),
                row('fur-edu-wall', 'Шведская стенка', stallBars()),
                row('fur-edu-gbench', 'Скамья гимнастическая', gymBench(200, 26))
            ]),
            pack('lib-fur-med', 'Медицина', [
                row('fur-med-couch', 'Кушетка медицинская', couch(190, 68, true)),
                row('fur-med-bed', 'Кровать медицинская', hospitalBed(false)),
                row('fur-med-func', 'Кровать функциональная', hospitalBed(true)),
                row('fur-med-gurney', 'Каталка', gurney()),
                row('fur-med-stretch', 'Носилки', stretcher()),
                row('fur-med-table', 'Столик медицинский', medTable(80, 50, false)),
                row('fur-med-tray', 'Столик инструментальный', medTable(70, 42, true)),
                row('fur-med-ped', 'Тумба медицинская', pedestal(46, 48, true)),
                row('fur-med-cab', 'Шкаф медицинский', medCabinet(90, 42, 'base')),
                row('fur-med-meds', 'Шкаф для медикаментов', medCabinet(60, 40, 'meds')),
                row('fur-med-tools', 'Шкаф для инструментов', medCabinet(70, 40, 'tools')),
                row('fur-med-shelf', 'Стеллаж медицинский', shelf(100, 40, 3, 4, false)),
                row('fur-med-chair', 'Кресло медицинское', procedureChair()),
                row('fur-med-seat', 'Стул медицинский', sideChair(48, 52, false, false)),
                row('fur-med-stool', 'Табурет медицинский', stool(36)),
                row('fur-med-wheel', 'Кресло-коляска', wheelchair()),
                row('fur-med-blood', 'Кресло для забора крови', bloodChair()),
                row('fur-med-exam', 'Кушетка смотровая', couch(190, 72, false)),
                row('fur-med-screen', 'Ширма медицинская', foldScreen()),
                row('fur-med-stand', 'Вешалка', coatStand(38)),
                row('fur-med-bin', 'Урна', bin(32, false)),
                row('fur-med-cool', 'Кулер', cooler(false)),
                row('fur-med-change', 'Пеленальный стол', changing(90, 60)),
                row('fur-med-bath', 'Ванна медицинская', medBath()),
                row('fur-med-btable', 'Столик для ванны', medTable(70, 32, true))
            ]),
            pack('lib-fur-hot', 'Гостиницы', [
                row('fur-hot-single', 'Кровать односпальная', bed(90, 200, 1)),
                row('fur-hot-double', 'Кровать двуспальная', bed(160, 200, 2)),
                row('fur-hot-night', 'Тумба прикроватная', nightstand(45, 42)),
                row('fur-hot-ward', 'Шкаф платяной', wardrobe(110, 58, 2)),
                row('fur-hot-slide', 'Шкаф-купе', sliding(160, 62)),
                row('fur-hot-closet', 'Гардероб', closet(180, 150)),
                row('fur-hot-dresser', 'Комод', dresser(90, 48, 4)),
                row('fur-hot-desk', 'Стол письменный', desk(110, 60)),
                row('fur-hot-chair', 'Стул', sideChair(46, 52, false, false)),
                row('fur-hot-arm', 'Кресло', sofa(80, 86, 1)),
                row('fur-hot-sofa', 'Диван', sofa(160, 86, 2)),
                row('fur-hot-pouf', 'Пуф', pouf(40)),
                row('fur-hot-bench', 'Банкетка', bench(100, 40, false)),
                row('fur-hot-coffee', 'Журнальный столик', table(80, 50, 'rect')),
                row('fur-hot-tv', 'ТВ-тумба', tv(120, 42, false)),
                row('fur-hot-lug', 'Багажная подставка', luggageRack()),
                row('fur-hot-stand', 'Вешалка', coatStand(36)),
                row('fur-hot-mirror', 'Зеркало', floorMirror(54, 32)),
                row('fur-hot-bar', 'Мини-бар', minibar()),
                row('fur-hot-safe', 'Сейф', safe(40, 38, false)),
                row('fur-hot-case', 'Столик для чемоданов', table(60, 42, 'rect')),
                row('fur-hot-basket', 'Корзина', basket(36, false)),
                row('fur-hot-linen', 'Шкаф для белья', cabinet(80, 48, 2, 'drawers')),
                row('fur-hot-iron', 'Гладильная доска', ironBoard())
            ]),
            pack('lib-fur-cafe', 'Рестораны и кафе', [
                row('fur-cafe-table', 'Стол обеденный', table(120, 80, 'rect')),
                row('fur-cafe-round', 'Стол круглый', table(90, 90, 'round')),
                row('fur-cafe-square', 'Стол квадратный', table(80, 80, 'rect')),
                row('fur-cafe-bar', 'Стол барный', table(60, 60, 'round')),
                row('fur-cafe-chair', 'Стул обеденный', sideChair(46, 52, false, false)),
                row('fur-cafe-barch', 'Стул барный', barSeat(true)),
                row('fur-cafe-stool', 'Табурет барный', barSeat(false)),
                row('fur-cafe-banq', 'Диван-банкетка', bench(180, 60, true)),
                row('fur-cafe-bench', 'Банкетка', bench(140, 42, false)),
                row('fur-cafe-arm', 'Кресло', sofa(78, 82, 1)),
                row('fur-cafe-counter', 'Барная стойка', barCounter(240, 60)),
                row('fur-cafe-host', 'Стойка администратора', hostStand()),
                row('fur-cafe-cash', 'Кассовая стойка', cashDesk()),
                row('fur-cafe-case', 'Витрина', showcase(120, 50, false)),
                row('fur-cafe-cold', 'Холодильная витрина', showcase(150, 70, true)),
                row('fur-cafe-china', 'Шкаф для посуды', cabinet(80, 46, 2, 'glass')),
                row('fur-cafe-drinks', 'Стеллаж для напитков', drinkRack()),
                row('fur-cafe-sink', 'Мойка', sinkUnit(70, 50, 1)),
                row('fur-cafe-cool', 'Кулер', cooler(false)),
                row('fur-cafe-bin', 'Урна', bin(34, false)),
                row('fur-cafe-stand', 'Вешалка', coatBar(80)),
                row('fur-cafe-screen', 'Ширма', foldScreen()),
                row('fur-cafe-decor', 'Перегородка декоративная', decorScreen())
            ]),
            pack('lib-fur-shop', 'Торговля', [
                row('fur-shop-gond', 'Стеллаж торговый', gondola(140, 50, false)),
                row('fur-shop-wall', 'Стеллаж пристенный', wallGondola()),
                row('fur-shop-island', 'Стеллаж островной', gondola(110, 110, true)),
                row('fur-shop-rec', 'Стойка ресепшн', reception(180, 74, 'straight')),
                row('fur-shop-cash', 'Кассовая стойка', cashDesk()),
                row('fur-shop-case', 'Витрина', showcase(120, 54, false)),
                row('fur-shop-glass', 'Витрина стеклянная', glassCase(100, 50)),
                row('fur-shop-cold', 'Холодильная витрина', showcase(160, 74, true)),
                row('fur-shop-counter', 'Прилавок', barCounter(160, 60)),
                row('fur-shop-fit', 'Примерочная кабина', fittingRoom()),
                row('fur-shop-mirror', 'Зеркало', mirror(70, 8, false)),
                row('fur-shop-bench', 'Банкетка', bench(90, 40, false)),
                row('fur-shop-pouf', 'Пуф', pouf(40)),
                row('fur-shop-cart', 'Тележка торговая', shopCart()),
                row('fur-shop-basket', 'Корзина торговая', basket(42, false)),
                row('fur-shop-mann', 'Манекен', mannequin()),
                row('fur-shop-stand', 'Подставка для товара', table(40, 40, 'rect')),
                row('fur-shop-file', 'Шкаф для документов', cabinet(80, 42, 2, 'file')),
                row('fur-shop-safe', 'Сейф', safe(50, 46, false)),
                row('fur-shop-bin', 'Урна', bin(36, false))
            ]),
            pack('lib-fur-sport', 'Спорт', [
                row('fur-sport-bench', 'Скамья гимнастическая', gymBench(200, 26)),
                row('fur-sport-lockb', 'Скамья для раздевалки', gymBench(160, 36)),
                row('fur-sport-coat', 'Шкаф для одежды', metalLocker(60, 52, 1)),
                row('fur-sport-metal', 'Шкаф металлический', metalLocker(150, 52, 3)),
                row('fur-sport-stand', 'Вешалка', coatBar(120)),
                row('fur-sport-hook', 'Крючок', hook()),
                row('fur-sport-arm', 'Кресло', sideChair(56, 60, true, false)),
                row('fur-sport-chair', 'Стул', sideChair(46, 50, false, false)),
                row('fur-sport-stool', 'Табурет', stool(36)),
                row('fur-sport-table', 'Стол', desk(120, 70)),
                row('fur-sport-shelf', 'Стеллаж для инвентаря', metalShelf(140, 50)),
                row('fur-sport-balls', 'Тележка для мячей', ballCart()),
                row('fur-sport-equip', 'Подставка для инвентаря', shelf(70, 40, 2, 3, false)),
                row('fur-sport-mat', 'Мат', mat(200, 120)),
                row('fur-sport-yoga', 'Коврик', mat(180, 60)),
                row('fur-sport-wall', 'Шведская стенка', stallBars()),
                row('fur-sport-bars', 'Брусья', parallelBars()),
                row('fur-sport-pull', 'Турник', pullup()),
                row('fur-sport-gym', 'Тренажёр', trainer()),
                row('fur-sport-bike', 'Велотренажёр', exBike()),
                row('fur-sport-run', 'Беговая дорожка', treadmill()),
                row('fur-sport-press', 'Скамья для жима', benchPress()),
                row('fur-sport-bells', 'Стойка для гантелей', dumbbellRack()),
                row('fur-sport-mirror', 'Зеркало', mirror(180, 8, false)),
                row('fur-sport-bin', 'Урна', bin(36, false))
            ]),
            pack('lib-fur-wc', 'Санузлы', [
                row('fur-wc-comb', 'Санузел совмещённый', wcCombined()),
                row('fur-wc-bath', 'Санузел с ванной', wcBath()),
                row('fur-wc-shower', 'Санузел с душем', wcShower()),
                row('fur-wc-split', 'Санузел раздельный', wcSplit()),
                row('fur-wc-stall', 'Туалетная кабина', wcStall()),
                row('fur-wc-public', 'Туалет общественный', wcPublic()),
                row('fur-wc-wash', 'Умывальная', washroom()),
                row('fur-wc-showers', 'Душевая', showerRoom()),
                row('fur-wc-access', 'Санузел для МГН', wcAccess())
            ]),
            pack('lib-fur-bath', 'Санузлы и МГН', [
                row('fur-bath-wc', 'Унитаз', toilet()),
                row('fur-bath-bidet', 'Биде', bidet()),
                row('fur-bath-sink', 'Раковина', basin(62, 48)),
                row('fur-bath-urin', 'Писсуар', urinal()),
                row('fur-bath-tub', 'Ванна', bathtub(170, 75)),
                row('fur-bath-cabin', 'Душевая кабина', showerCabin()),
                row('fur-bath-tray', 'Душевой поддон', showerTray()),
                row('fur-bath-towel', 'Полотенцесушитель', towelRail()),
                row('fur-bath-cab', 'Шкаф для ванной', cabinet(40, 32, 1, 'wall')),
                row('fur-bath-van', 'Тумба под раковину', vanityBasin()),
                row('fur-bath-mirror', 'Зеркало', mirror(70, 8, false)),
                row('fur-bath-shelf', 'Полка', wallShelf(60, 16)),
                row('fur-bath-hook', 'Крючок', hook()),
                row('fur-bath-hold', 'Держатель', holder()),
                row('fur-bath-grab', 'Поручень', grabBar(70, false)),
                row('fur-bath-fold', 'Откидной поручень', grabBar(76, true)),
                row('fur-bath-sbench', 'Скамья для душа', showerBench()),
                row('fur-bath-schair', 'Стул для душа', showerChair()),
                row('fur-bath-wheel', 'Кресло-коляска', wheelchair()),
                row('fur-bath-change', 'Пеленальный стол', changing(86, 58)),
                row('fur-bath-bin', 'Урна', bin(28, false)),
                row('fur-bath-basket', 'Корзина', basket(32, false)),
                row('fur-bath-disp', 'Диспенсер', dispenser()),
                row('fur-bath-dry', 'Сушилка для рук', handDryer())
            ]),
            pack('lib-fur-store', 'Склад и производство', [
                row('fur-store-metal', 'Стеллаж металлический', metalShelf(200, 60)),
                row('fur-store-pallet', 'Стеллаж паллетный', palletRack()),
                row('fur-store-cant', 'Стеллаж консольный', cantilever()),
                row('fur-store-mob', 'Стеллаж мобильный', mobileShelf()),
                row('fur-store-arch', 'Стеллаж архивный', archiveShelf()),
                row('fur-store-acab', 'Шкаф архивный', cabinet(100, 46, 2, 'base')),
                row('fur-store-card', 'Шкаф картотечный', cardFile(90, 60, false)),
                row('fur-store-cat', 'Картотека', cardFile(60, 42, true)),
                row('fur-store-bench', 'Верстак', workbench(false)),
                row('fur-store-bshelf', 'Верстак с полками', workbench(true)),
                row('fur-store-mount', 'Монтажный стол', desk(180, 80)),
                row('fur-store-cargo', 'Тележка грузовая', platformCart()),
                row('fur-store-cart', 'Тележка складская', cageCart()),
                row('fur-store-tool', 'Тележка инструментальная', toolCart()),
                row('fur-store-pal', 'Поддон', pallet()),
                row('fur-store-box', 'Контейнер складской', container()),
                row('fur-store-crate', 'Ящик для хранения', toyBox()),
                row('fur-store-step', 'Стремянка', ladder(false)),
                row('fur-store-lad', 'Лестница-стремянка', ladder(true)),
                row('fur-store-tcab', 'Шкаф для инструмента', cabinet(90, 50, 2, 'base')),
                row('fur-store-ppe', 'Шкаф для СИЗ', metalLocker(80, 48, 2)),
                row('fur-store-safe', 'Сейф', safe(54, 48, false)),
                row('fur-store-bin', 'Урна', bin(40, false))
            ]),
            pack('lib-fur-built', 'Встроенная мебель', [
                row('fur-built-ward', 'Встроенный шкаф', builtWardrobe(200, 62)),
                row('fur-built-shelf', 'Встроенная полка', wallShelf(140, 28)),
                row('fur-built-niche', 'Ниша', niche()),
                row('fur-built-over', 'Антресоль', cabinet(160, 40, 2, 'overhead')),
                row('fur-built-pod', 'Подиум', podiumDeck()),
                row('fur-built-murphy', 'Откидная кровать', murphy()),
                row('fur-built-fold', 'Откидной стол', foldTable()),
                row('fur-built-trans', 'Трансформируемый стол', table(150, 84, 'leaf')),
                row('fur-built-bed', 'Кровать-трансформер', transformBed()),
                row('fur-built-tcab', 'Шкаф-трансформер', transformWardrobe()),
                row('fur-built-div', 'Стеллаж-перегородка', shelf(160, 36, 4, 4, false)),
                row('fur-built-mob', 'Мобильная перегородка', partition(160, 10)),
                row('fur-built-screen', 'Ширма', foldScreen()),
                row('fur-built-panel', 'Экран', projScreen(180)),
                row('fur-built-rec', 'Стойка-ресепшн модульная', reception(240, 80, 'straight')),
                row('fur-built-lect', 'Кафедра', podium(80, 62)),
                row('fur-built-trib', 'Трибуна', podium(70, 58)),
                row('fur-built-music', 'Пюпитр', musicStand()),
                row('fur-built-safe', 'Сейф', safe(46, 44, false)),
                row('fur-built-keys', 'Шкаф для ключей', keyCabinet()),
                row('fur-built-file', 'Шкаф для документов', cabinet(80, 42, 2, 'file')),
                row('fur-built-closet', 'Гардероб', closet(200, 160)),
                row('fur-built-stand', 'Вешалка', coatStand(40))
            ]),
            pack('lib-fur-out', 'Уличная мебель', [
                row('fur-out-park', 'Скамья парковая', parkBench(180, 62, true)),
                row('fur-out-back', 'Скамья со спинкой', parkBench(160, 58, true)),
                row('fur-out-plain', 'Скамья без спинки', parkBench(160, 42, false)),
                row('fur-out-sofa', 'Скамья-диван', parkBench(200, 74, true)),
                row('fur-out-arm', 'Кресло садовое', gardenChair(true)),
                row('fur-out-chair', 'Стул садовый', gardenChair(false)),
                row('fur-out-table', 'Стол садовый', table(90, 90, 'rect')),
                row('fur-out-picnic', 'Стол пикниковый', picnic()),
                row('fur-out-gaz', 'Беседка', gazebo()),
                row('fur-out-canopy', 'Навес', canopy()),
                row('fur-out-umb', 'Зонт', umbrella()),
                row('fur-out-loung', 'Лежак', lounger(false)),
                row('fur-out-chaise', 'Шезлонг', lounger(true)),
                row('fur-out-swing', 'Качели', swingSeat()),
                row('fur-out-bin', 'Урна уличная', bin(48, true)),
                row('fur-out-urn', 'Вазон', planter('square')),
                row('fur-out-pot', 'Кашпо', planter('pot')),
                row('fur-out-flower', 'Цветочница', planter('box')),
                row('fur-out-fence', 'Ограждение', fence()),
                row('fur-out-bikes', 'Велопарковка', bikePark()),
                row('fur-out-bike', 'Стойка для велосипедов', bikeStand()),
                row('fur-out-info', 'Информационная стойка', infoStand(46, 36)),
                row('fur-out-sign', 'Указатель', waySign()),
                row('fur-out-dump', 'Мусорный контейнер', dumpster())
            ]),
            pack('lib-fur-acc', 'Доступная среда', [
                row('fur-acc-lockb', 'Скамья для раздевалки', gymBench(160, 40)),
                row('fur-acc-sbench', 'Скамья для душа', showerBench()),
                row('fur-acc-schair', 'Стул для душа', showerChair()),
                row('fur-acc-wheel', 'Кресло-коляска', wheelchair()),
                row('fur-acc-fold', 'Откидной поручень', grabBar(80, true)),
                row('fur-acc-grab', 'Поручень', grabBar(70, false)),
                row('fur-acc-hook', 'Крючок', hook()),
                row('fur-acc-mirror', 'Зеркало с наклоном', mirror(70, 22, true)),
                row('fur-acc-change', 'Пеленальный стол', changing(96, 62)),
                row('fur-acc-table', 'Столик для МГН', accessTable()),
                row('fur-acc-rec', 'Стойка ресепшн с пониженной секцией', reception(260, 86, 'low')),
                row('fur-acc-desk', 'Стол с регулировкой высоты', adjustableDesk()),
                row('fur-acc-chair', 'Стул с регулировкой высоты', starChair(64, 68, false)),
                row('fur-acc-ramp', 'Пандус', ramp()),
                row('fur-acc-tile', 'Тактильная плитка', tactile()),
                row('fur-acc-info', 'Информационный стенд', infoStand(70, 40))
            ])
        ];
    }

    root.AssetTreeSchemeFurniture = furniture;
    var previous = root.AssetTreeSchemeLibrary;
    root.AssetTreeSchemeLibrary = function () {
        var base = typeof previous === 'function' ? previous() : [];
        return base.concat(furniture());
    };
})(typeof window !== 'undefined' ? window : global);
