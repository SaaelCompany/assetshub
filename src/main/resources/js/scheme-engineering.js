/* Heating, structured cabling and electrical rooms. Plan view, centimetres, wall at y = 0. */
(function (root) {
    var INK = '#172b4d';
    var MET = '#5e6c84';
    var STEEL = '#ebecf0';
    var WHITE = '#ffffff';
    var DARK = '#091e42';
    var RED = '#de350b';
    var AMBER = '#ff8b00';
    var FIRE = '#ffebe6';
    var BLUE = '#0747a6';
    var GLASS = '#deebff';
    var GREEN = '#006644';
    var GREENF = '#e3fcef';

    function n(value) {
        return Math.round(value * 100) / 100;
    }

    function rect(x, y, w, h, stroke, fill, width) {
        return { t: 'r', x: n(x), y: n(y), w: n(w), h: n(h), c: stroke || INK, f: fill || '', s: width == null ? 0.4 : width };
    }

    function oval(x, y, w, h, stroke, fill, width) {
        return { t: 'e', x: n(x), y: n(y), w: n(w), h: n(h), c: stroke || INK, f: fill || '', s: width == null ? 0.35 : width };
    }

    function line(x1, y1, x2, y2, stroke, width) {
        return { t: 'l', x1: n(x1), y1: n(y1), x2: n(x2), y2: n(y2), c: stroke || INK, s: width == null ? 0.28 : width };
    }

    function arc(cx, cy, radius, start, end, steps, stroke, width) {
        var points = [];
        var i;
        for (i = 0; i <= steps; i++) {
            var angle = start + (end - start) * (i / steps);
            points.push({ x: n(cx + Math.cos(angle) * radius), y: n(cy + Math.sin(angle) * radius) });
        }
        return { t: 'p', points: points, c: stroke || INK, s: width == null ? 0.24 : width };
    }

    function box(w, h, parts) {
        return { w: n(w), h: n(h), parts: parts };
    }

    function wall(parts, w) {
        parts.push(line(0, 0.6, w, 0.6, INK, 1));
    }

    function modules(parts, x, y, count, h) {
        var i;
        var pitch = 1.8;
        if (h < 2.4) return;
        for (i = 0; i < count; i++) {
            parts.push(rect(x + i * pitch, y, pitch - 0.22, h, INK, WHITE, 0.06));
            parts.push(rect(x + i * pitch + 0.45, y + h * 0.4, 0.7, Math.min(1.5, h * 0.24), INK, DARK, 0.04));
        }
    }

    function singleDoor(parts, w, d) {
        var span = Math.min(w - 4, 36);
        parts.push(line(2, d, 2, d + span, INK, 0.35));
        parts.push(arc(2, d, span, 0, Math.PI / 2, 8, MET, 0.22));
        return span;
    }

    function pairDoors(parts, w, d) {
        var span = Math.min((w - 6) / 2, 42);
        parts.push(line(2, d, 2, d + span, INK, 0.35));
        parts.push(arc(2, d, span, 0, Math.PI / 2, 8, MET, 0.22));
        parts.push(line(w - 2, d, w - 2, d + span, INK, 0.35));
        parts.push(arc(w - 2, d, span, Math.PI, Math.PI / 2, 8, MET, 0.22));
        return span;
    }

    function cabinet(w, d, count, pair) {
        var parts = [rect(0, 0, w, d, INK, STEEL, 0.5)];
        var span;
        wall(parts, w);
        parts.push(rect(2, 2, w - 4, d - 4, MET, WHITE, 0.12));
        if (count) {
            var rows = d > 28 ? 2 : 1;
            var r;
            for (r = 0; r < rows; r++) modules(parts, 4, 3 + r * 7, count, 5);
        }
        span = pair ? pairDoors(parts, w, d) : singleDoor(parts, w, d);
        return box(w, d + span, parts);
    }

    function radiator(w, sections) {
        var d = 14;
        var parts = [];
        var i;
        var sw = w / sections;
        wall(parts, w);
        parts.push(rect(0, 3, w, 8, INK, WHITE, 0.4));
        for (i = 0; i < sections; i++) parts.push(rect(i * sw + 0.7, 4, sw - 1.4, 6, MET, STEEL, 0.08));
        parts.push(oval(1.2, 8, 4.2, 4.2, INK, MET, 0.12));
        parts.push(oval(w - 5.4, 3.4, 4.2, 4.2, INK, MET, 0.12));
        return box(w, d, parts);
    }

    function panelRad() {
        var w = 120;
        var d = 10;
        var parts = [];
        var i;
        wall(parts, w);
        parts.push(rect(0, 2, w, 6, INK, WHITE, 0.4));
        for (i = 4; i < w - 2; i += 4) parts.push(line(i, 3, i, 7, MET, 0.1));
        parts.push(oval(2, 3, 4, 4, INK, MET, 0.12));
        parts.push(oval(w - 6, 3, 4, 4, INK, MET, 0.12));
        return box(w, d, parts);
    }

    function convector(floor) {
        var w = floor ? 160 : 140;
        var d = floor ? 26 : 20;
        var parts = [rect(0, 0, w, d, INK, floor ? STEEL : WHITE, 0.45)];
        var i;
        wall(parts, w);
        for (i = 6; i < w - 4; i += 6) parts.push(line(i, 3, i, d - 3, MET, 0.12));
        parts.push(rect(4, 4, w - 8, 3, INK, DARK, 0.1));
        parts.push(oval(6, d - 8, 5, 5, INK, MET, 0.12));
        return box(w, d, parts);
    }

    function towelRail() {
        var w = 50;
        var d = 14;
        var parts = [];
        var i;
        wall(parts, w);
        parts.push(rect(4, 3, 4, 8, INK, MET, 0.22));
        parts.push(rect(w - 8, 3, 4, 8, INK, MET, 0.22));
        for (i = 0; i < 5; i++) parts.push(rect(8, 3.2 + i * 1.4, w - 16, 0.9, INK, STEEL, 0.08));
        return box(w, d, parts);
    }

    function boiler(floor) {
        var w = floor ? 62 : 46;
        var d = floor ? 54 : 34;
        var parts = [rect(4, floor ? 8 : 6, w - 8, d - (floor ? 14 : 10), INK, WHITE, 0.5)];
        wall(parts, w);
        parts.push(rect(8, floor ? 14 : 10, w - 16, 10, INK, STEEL, 0.16));
        parts.push(oval(10, floor ? 28 : 22, 10, 10, INK, MET, 0.16));
        parts.push(oval(w - 20, floor ? 28 : 22, 10, 10, INK, MET, 0.16));
        parts.push(rect(w / 2 - 6, 1, 12, 6, INK, DARK, 0.16));
        parts.push(oval(w / 2 - 3, 2, 6, 4, AMBER, FIRE, 0.1));
        if (floor) {
            parts.push(rect(2, d - 6, 8, 6, INK, DARK, 0.14));
            parts.push(rect(w - 10, d - 6, 8, 6, INK, DARK, 0.14));
        }
        return box(w, d, parts);
    }

    function cylinder(d, pipes) {
        var parts = [oval(0, 0, d, d, INK, WHITE, 0.5)];
        parts.push(oval(d * 0.16, d * 0.16, d * 0.68, d * 0.68, MET, '', 0.18));
        parts.push(oval(d * 0.38, d * 0.38, d * 0.24, d * 0.24, INK, STEEL, 0.14));
        if (pipes) {
            parts.push(rect(d * 0.12, d * 0.7, d * 0.18, d * 0.14, INK, MET, 0.12));
            parts.push(rect(d * 0.7, d * 0.7, d * 0.18, d * 0.14, INK, RED, 0.12));
        }
        return box(d, d, parts);
    }

    function pump() {
        var parts = [oval(0, 1, 12, 12, INK, STEEL, 0.32)];
        parts.push(oval(3, 4, 6, 6, MET, '', 0.14));
        parts.push(rect(10, 4, 12, 6, INK, MET, 0.26));
        parts.push(oval(18, 5, 4, 4, INK, DARK, 0.1));
        return box(24, 14, parts);
    }

    function manifold() {
        var w = 52;
        var d = 14;
        var parts = [rect(0, 4, w, 6, INK, MET, 0.35)];
        var i;
        for (i = 0; i < 6; i++) {
            parts.push(rect(4 + i * 8, 0, 3, 4, INK, STEEL, 0.12));
            parts.push(oval(4.2 + i * 8, 0.4, 2.4, 2.4, INK, RED, 0.08));
            parts.push(rect(4 + i * 8, 10, 3, 4, INK, STEEL, 0.12));
        }
        return box(w, d, parts);
    }

    function manifoldCabinet() {
        var drawn = cabinet(64, 16, 0, false);
        var i;
        for (i = 0; i < 5; i++) drawn.parts.push(rect(8 + i * 10, 5, 3, 6, INK, MET, 0.12));
        return drawn;
    }

    function meter(w) {
        var d = 10;
        var parts = [rect(0, 0, w, d, INK, WHITE, 0.3)];
        parts.push(rect(1.2, 1.2, w - 2.4, 4, INK, GLASS, 0.12));
        parts.push(line(2, 3, w - 2, 3, BLUE, 0.12));
        parts.push(oval(w / 2 - 1.2, 6.4, 2.4, 2.4, INK, DARK, 0.08));
        return box(w, d, parts);
    }

    function strainer() {
        var parts = [oval(1, 1, 16, 16, INK, STEEL, 0.3)];
        parts.push(line(4, 14, 14, 4, MET, 0.2));
        parts.push(rect(0, 7, 4, 4, INK, MET, 0.14));
        parts.push(rect(14, 7, 4, 4, INK, MET, 0.14));
        return box(18, 18, parts);
    }

    function airVent() {
        var parts = [oval(0, 0, 8, 8, INK, STEEL, 0.22)];
        parts.push(oval(2, 2, 4, 4, MET, '', 0.12));
        parts.push(line(4, 0, 4, 2,2, MET, 0.16));
        return box(8, 8, parts);
    }

    function mixUnit() {
        var parts = [rect(0, 0, 42, 28, INK, STEEL, 0.4)];
        parts.push(oval(4, 6, 12, 12, INK, MET, 0.2));
        parts.push(oval(7, 9, 6, 6, MET, '', 0.12));
        parts.push(rect(20, 8, 16, 6, INK, WHITE, 0.16));
        parts.push(oval(24, 16, 6, 6, INK, RED, 0.12));
        parts.push(line(10, 18, 24, 19, MET, 0.18));
        return box(42, 28, parts);
    }

    function exchanger() {
        var parts = [rect(0, 2, 46, 20, INK, STEEL, 0.4)];
        var i;
        for (i = 0; i < 8; i++) parts.push(line(4 + i * 5, 4, 4 + i * 5, 20, MET, 0.12));
        parts.push(oval(2, 0, 6, 6, INK, RED, 0.12));
        parts.push(oval(38, 16, 6, 6, INK, BLUE, 0.12));
        return box(46, 24, parts);
    }

    function underfloor() {
        var w = 180;
        var d = 140;
        var parts = [rect(0, 0, w, d, INK, '#fff7e6', 0.35)];
        var y;
        parts.push(rect(8, 8, w - 16, d - 16, AMBER, '', 0.2));
        for (y = 16; y < d - 12; y += 14) parts.push(line(16, y, w - 16, y, RED, 0.4));
        for (y = 16; y < d - 26; y += 28) {
            parts.push(line(w - 16, y, w - 16, y + 14, RED, 0.4));
            parts.push(line(16, y + 14, 16, y + 28, RED, 0.4));
        }
        parts.push(oval(8, d - 18, 8, 8, INK, MET, 0.14));
        return box(w, d, parts);
    }

    function flue() {
        var parts = [rect(2, 2, 20, 20, INK, STEEL, 0.35)];
        parts.push(oval(5, 5, 14, 14, INK, DARK, 0.2));
        parts.push(oval(8, 8, 8, 8, MET, '', 0.12));
        return box(24, 24, parts);
    }

    function radValve() {
        var parts = [rect(2, 2, 8, 4, INK, MET, 0.2)];
        parts.push(oval(3, 0, 6, 4, INK, WHITE, 0.14));
        parts.push(line(6, 4, 6, 6, MET, 0.16));
        return box(12, 8, parts);
    }

    function thermoHead() {
        var parts = [oval(0, 0, 10, 10, INK, WHITE, 0.28)];
        var i;
        for (i = 0; i < 6; i++) {
            var angle = i * Math.PI / 3;
            parts.push(line(5, 5, 5 + Math.cos(angle) * 4.2, 5 + Math.sin(angle) * 4.2, MET, 0.14));
        }
        parts.push(oval(3, 3, 4, 4, INK, STEEL, 0.12));
        return box(10, 10, parts);
    }

    function patch(ports) {
        var w = 48.3;
        var rows = ports > 24 ? 2 : 1;
        var d = rows === 2 ? 16 : 12;
        var cols = ports > 24 ? 24 : ports;
        var parts = [rect(0, 0, w, d, INK, DARK, 0.45)];
        var gap = (w - 4) / cols;
        var r;
        var i;
        for (r = 0; r < rows; r++) {
            for (i = 0; i < cols; i++) {
                parts.push(rect(2 + i * gap, 2 + r * 5, Math.max(0.7, gap - 0.45), 3.2, INK, STEEL, 0.04));
            }
        }
        return box(w, d, parts);
    }

    function organizer() {
        var w = 48.3;
        var d = 8;
        var parts = [rect(0, 0, w, d, INK, STEEL, 0.35)];
        var i;
        for (i = 2; i < w - 3; i += 3.2) parts.push(rect(i, 1.4, 1.6, 5.2, INK, DARK, 0.06));
        return box(w, d, parts);
    }

    function jack(pair) {
        var w = pair ? 16 : 9;
        var d = 8;
        var parts = [rect(0, 0, w, d, INK, WHITE, 0.3)];
        wall(parts, w);
        parts.push(rect(pair ? 2 : 2.2, 2.2, 4.2, 3.4, INK, STEEL, 0.1));
        if (pair) parts.push(rect(9, 2.2, 4.2, 3.4, INK, STEEL, 0.1));
        return box(w, d, parts);
    }

    function floorBox() {
        var parts = [rect(0, 0, 24, 24, INK, STEEL, 0.4)];
        parts.push(rect(2, 2, 20, 20, MET, WHITE, 0.16));
        parts.push(rect(4, 5, 6, 4, INK, DARK, 0.1));
        parts.push(rect(14, 5, 6, 4, INK, DARK, 0.1));
        parts.push(oval(9, 13, 6, 6, INK, MET, 0.12));
        return box(24, 24, parts);
    }

    function tray(ladder) {
        var w = 140;
        var d = ladder ? 30 : 20;
        var parts = [rect(0, 0, w, d, INK, STEEL, 0.35)];
        var i;
        parts.push(rect(0, 0, w, 2.2, INK, MET, 0.16));
        parts.push(rect(0, d - 2.2, w, 2.2, INK, MET, 0.16));
        for (i = 8; i < w; i += 10) parts.push(line(i, 2, i, d - 2, MET, ladder ? 0.28 : 0.14));
        return box(w, d, parts);
    }

    function duct() {
        var w = 120;
        var d = 8;
        var parts = [rect(0, 0, w, d, INK, WHITE, 0.35)];
        wall(parts, w);
        parts.push(line(0, d / 2, w, d / 2, MET, 0.16));
        return box(w, d, parts);
    }

    function optic() {
        var drawn = patch(24);
        drawn.h = 14;
        drawn.parts[0].h = 14;
        drawn.parts.push(rect(2, 8, 44, 4, INK, GREENF, 0.12));
        return drawn;
    }

    function splice() {
        var parts = [rect(0, 0, 22, 16, INK, WHITE, 0.3)];
        var i;
        for (i = 0; i < 6; i++) parts.push(oval(2 + (i % 3) * 6, 2 + Math.floor(i / 3) * 7, 4, 4, INK, GREENF, 0.1));
        return box(22, 16, parts);
    }

    function rack(w, d) {
        var parts = [rect(0, 0, w, d, INK, STEEL, 0.5)];
        var span = pairDoors(parts, w, d);
        parts.push(rect(6, 4, 3, d - 8, INK, DARK, 0.16));
        parts.push(rect(w - 9, 4, 3, d - 8, INK, DARK, 0.16));
        parts.push(line(9, d / 2, w - 9, d / 2, MET, 0.16));
        return box(w, d + span, parts);
    }

    function wallCabinet() {
        return cabinet(60, 18, 0, false);
    }

    function consolidation() {
        var parts = [rect(0, 0, 30, 20, INK, WHITE, 0.35)];
        var i;
        for (i = 0; i < 4; i++) parts.push(rect(3 + i * 6, 4, 4, 6, INK, STEEL, 0.1));
        parts.push(rect(3, 13, 24, 3, INK, DARK, 0.1));
        return box(30, 20, parts);
    }

    function closure() {
        var parts = [oval(0, 2, 28, 8, INK, DARK, 0.3)];
        parts.push(rect(2, 4, 6, 4, INK, STEEL, 0.1));
        parts.push(rect(20, 4, 6, 4, INK, STEEL, 0.1));
        return box(28, 12, parts);
    }

    function entry() {
        var parts = [rect(0, 2, 22, 10, INK, STEEL, 0.3)];
        parts.push(oval(6, 0, 10, 8, INK, MET, 0.2));
        parts.push(oval(8, 2, 6, 4, MET, '', 0.12));
        return box(22, 14, parts);
    }

    function device(w, kind) {
        var d = 8;
        var parts = [rect(0, 0, w, d, INK, WHITE, 0.22)];
        parts.push(rect(0.25, 0.4, w - 0.5, 1.4, INK, STEEL, 0.05));
        parts.push(rect(0.25, d - 1.8, w - 0.5, 1.4, INK, STEEL, 0.05));
        parts.push(rect(w * 0.28, 3.1, Math.max(0.7, w * 0.28), 1.7, INK, kind === 'rcd' ? RED : DARK, 0.08));
        if (kind === 'rcd' || kind === 'rcbo') parts.push(oval(w * 0.7, 3.2, 1.2, 1.2, INK, RED, 0.06));
        if (kind === 'contactor') parts.push(rect(w * 0.2, 5.2, w * 0.6, 0.8, INK, MET, 0.06));
        return box(w, d, parts);
    }

    function bar(color) {
        var parts = [rect(0, 0, 36, 5, INK, color, 0.25)];
        var i;
        for (i = 0; i < 8; i++) parts.push(oval(2 + i * 4, 1.2, 2.4, 2.4, INK, DARK, 0.06));
        return box(36, 5, parts);
    }

    function transformer() {
        var parts = [rect(0, 4, 60, 38, INK, STEEL, 0.45)];
        parts.push(oval(6, 10, 20, 20, INK, WHITE, 0.2));
        parts.push(oval(34, 10, 20, 20, INK, WHITE, 0.2));
        parts.push(oval(10, 14, 12, 12, MET, '', 0.14));
        parts.push(oval(38, 14, 12, 12, MET, '', 0.14));
        parts.push(rect(8, 0, 8, 6, INK, MET, 0.14));
        parts.push(rect(44, 0, 8, 6, INK, MET, 0.14));
        return box(60, 46, parts);
    }

    function row(id, name, drawn) {
        return { id: id, name: name, w: drawn.w, h: drawn.h, parts: drawn.parts };
    }

    function pack(id, name, templates) {
        return { id: id, name: name, templates: templates };
    }

    function engineering() {
        return [
            pack('lib-heat', 'Отопление', [
                row('heat-rad-s', 'Радиатор секционный', radiator(80, 10)),
                row('heat-rad-p', 'Радиатор панельный', panelRad()),
                row('heat-conv', 'Конвектор напольный', convector(false)),
                row('heat-fconv', 'Конвектор внутрипольный', convector(true)),
                row('heat-towel', 'Полотенцесушитель', towelRail()),
                row('heat-boil-w', 'Котёл настенный', boiler(false)),
                row('heat-boil-f', 'Котёл напольный', boiler(true)),
                row('heat-tank', 'Бойлер', cylinder(48, true)),
                row('heat-exp', 'Бак расширительный', cylinder(32, false)),
                row('heat-pump', 'Насос циркуляционный', pump()),
                row('heat-man', 'Коллектор', manifold()),
                row('heat-mancab', 'Шкаф коллекторный', manifoldCabinet()),
                row('heat-meter', 'Теплосчётчик', meter(16)),
                row('heat-filter', 'Грязевик', strainer()),
                row('heat-air', 'Воздухоотводчик', airVent()),
                row('heat-mix', 'Узел смешения', mixUnit()),
                row('heat-hex', 'Теплообменник', exchanger()),
                row('heat-ufh', 'Контур тёплого пола', underfloor()),
                row('heat-flue', 'Дымоход', flue()),
                row('heat-valve', 'Кран радиаторный', radValve()),
                row('heat-head', 'Термоголовка', thermoHead())
            ]),
            pack('lib-scs', 'СКС', [
                row('scs-pp24', 'Патч-панель 24 порта', patch(24)),
                row('scs-pp48', 'Патч-панель 48 портов', patch(48)),
                row('scs-org', 'Органайзер кабельный', organizer()),
                row('scs-jack', 'Розетка RJ45', jack(false)),
                row('scs-jack2', 'Розетка RJ45 двойная', jack(true)),
                row('scs-box', 'Лючок напольный', floorBox()),
                row('scs-tray', 'Лоток проволочный', tray(false)),
                row('scs-ladder', 'Лоток лестничный', tray(true)),
                row('scs-duct', 'Короб настенный', duct()),
                row('scs-optic', 'Оптический кросс', optic()),
                row('scs-splice', 'Кассета КДЗС', splice()),
                row('scs-rack', 'Шкаф телекоммуникационный', rack(80, 60)),
                row('scs-wall', 'Шкаф настенный', wallCabinet()),
                row('scs-cp', 'Точка консолидации', consolidation()),
                row('scs-closure', 'Муфта оптическая', closure()),
                row('scs-entry', 'Кабельный ввод', entry())
            ]),
            pack('lib-el', 'Электрощитовая', [
                row('el-board', 'Щит навесной', cabinet(40, 14, 12, false)),
                row('el-flush', 'Щит встраиваемый', cabinet(48, 10, 18, false)),
                row('el-floor', 'Щит напольный', cabinet(80, 40, 24, true)),
                row('el-mcc', 'ВРУ', cabinet(110, 42, 36, true)),
                row('el-main', 'ГРЩ', cabinet(140, 50, 36, true)),
                row('el-light', 'Щит освещения', cabinet(36, 12, 10, false)),
                row('el-power', 'Щит силовой', cabinet(60, 20, 18, false)),
                row('el-ats', 'АВР', cabinet(80, 32, 12, true)),
                row('el-mcb', 'Автомат', device(1.8, 'mcb')),
                row('el-rcd', 'УЗО', device(3.6, 'rcd')),
                row('el-rcbo', 'Дифавтомат', device(3.6, 'rcbo')),
                row('el-cont', 'Контактор', device(5.4, 'contactor')),
                row('el-meter', 'Счётчик', meter(12.6)),
                row('el-pe', 'Шина PE', bar(GREENF)),
                row('el-n', 'Шина N', bar(GLASS)),
                row('el-box', 'Бокс на 24 модуля', cabinet(52, 12, 24, false)),
                row('el-trafo', 'Трансформатор', transformer()),
                row('el-entry', 'Кабельный ввод', entry())
            ])
        ];
    }

    root.AssetTreeSchemeEngineering = engineering;
    var previous = root.AssetTreeSchemeLibrary;
    root.AssetTreeSchemeLibrary = function () {
        var base = typeof previous === 'function' ? previous() : [];
        return base.concat(engineering());
    };
})(typeof window !== 'undefined' ? window : global);
