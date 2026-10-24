/* QR Code, byte mode, error correction M. Enough for an equipment card URL. */
(function (root) {
    var ALIGN = [
        [],
        [6, 18],
        [6, 22],
        [6, 26],
        [6, 30],
        [6, 34],
        [6, 22, 38],
        [6, 24, 42],
        [6, 26, 46],
        [6, 28, 50],
        [6, 30, 54],
        [6, 32, 58],
        [6, 34, 62],
        [6, 26, 46, 66],
        [6, 26, 48, 70],
        [6, 26, 50, 74],
        [6, 30, 54, 78],
        [6, 30, 56, 82],
        [6, 30, 58, 86],
        [6, 34, 62, 90]
    ];
    /* Error correction M: groups of [block count, total codewords, data codewords]. */
    var RS = [
        null,
        [[1, 26, 16]],
        [[1, 44, 28]],
        [[1, 70, 44]],
        [[2, 50, 32]],
        [[2, 67, 43]],
        [[4, 43, 27]],
        [[4, 49, 31]],
        [[2, 60, 38], [2, 61, 39]],
        [[3, 58, 36], [2, 59, 37]],
        [[4, 69, 43], [1, 70, 44]],
        [[1, 80, 50], [4, 81, 51]],
        [[6, 58, 36], [2, 59, 37]],
        [[8, 59, 37], [1, 60, 38]],
        [[4, 64, 40], [5, 65, 41]],
        [[5, 65, 41], [5, 66, 42]],
        [[7, 73, 45], [3, 74, 46]],
        [[10, 74, 46], [1, 75, 47]],
        [[9, 69, 43], [4, 70, 44]],
        [[3, 70, 44], [11, 71, 45]],
        [[3, 67, 41], [13, 68, 42]]
    ];
    var EXP = new Array(512);
    var LOG = new Array(256);
    (function () {
        var value = 1;
        for (var i = 0; i < 255; i++) {
            EXP[i] = value;
            LOG[value] = i;
            value <<= 1;
            if (value & 256) {
                value ^= 0x11d;
            }
        }
        for (var extra = 255; extra < 512; extra++) {
            EXP[extra] = EXP[extra - 255];
        }
    })();

    function mul(left, right) {
        if (left === 0 || right === 0) {
            return 0;
        }
        return EXP[LOG[left] + LOG[right]];
    }

    function utf8(text) {
        var bytes = [];
        var value = String(text == null ? '' : text);
        for (var i = 0; i < value.length; i++) {
            var code = value.charCodeAt(i);
            if (code < 0x80) {
                bytes.push(code);
            } else if (code < 0x800) {
                bytes.push(0xc0 | (code >> 6), 0x80 | (code & 0x3f));
            } else if (code >= 0xd800 && code <= 0xdbff && i + 1 < value.length) {
                var next = value.charCodeAt(++i);
                var point = 0x10000 + ((code & 0x3ff) << 10) + (next & 0x3ff);
                bytes.push(0xf0 | (point >> 18), 0x80 | ((point >> 12) & 0x3f), 0x80 | ((point >> 6) & 0x3f), 0x80 | (point & 0x3f));
            } else {
                bytes.push(0xe0 | (code >> 12), 0x80 | ((code >> 6) & 0x3f), 0x80 | (code & 0x3f));
            }
        }
        return bytes;
    }

    function dataBits(length, version) {
        return 4 + (version < 10 ? 8 : 16) + length * 8;
    }

    function capacityBits(version) {
        var words = 0;
        RS[version].forEach(function (group) {
            words += group[0] * group[2];
        });
        return words * 8;
    }

    function versionFor(length) {
        for (var version = 1; version < RS.length; version++) {
            if (dataBits(length, version) <= capacityBits(version)) {
                return version;
            }
        }
        return 0;
    }

    function rsRemainder(data, ecLength) {
        var generator = [1];
        for (var i = 0; i < ecLength; i++) {
            var next = [];
            for (var pad = 0; pad < generator.length + 1; pad++) {
                next.push(0);
            }
            for (var j = 0; j < generator.length; j++) {
                next[j] ^= generator[j];
                next[j + 1] ^= mul(generator[j], EXP[i]);
            }
            generator = next;
        }
        var result = data.slice();
        for (var zero = 0; zero < ecLength; zero++) {
            result.push(0);
        }
        for (var index = 0; index < data.length; index++) {
            var factor = result[index];
            if (!factor) {
                continue;
            }
            for (var term = 0; term < generator.length; term++) {
                result[index + term] ^= mul(generator[term], factor);
            }
        }
        return result.slice(data.length);
    }

    function codewords(version, bytes) {
        var bits = [];
        function put(value, length) {
            for (var shift = length - 1; shift >= 0; shift--) {
                bits.push((value >>> shift) & 1);
            }
        }
        put(0x4, 4);
        put(bytes.length, version < 10 ? 8 : 16);
        bytes.forEach(function (byte) {
            put(byte, 8);
        });
        var limit = capacityBits(version);
        var terminator = Math.min(4, limit - bits.length);
        for (var end = 0; end < terminator; end++) {
            bits.push(0);
        }
        while (bits.length % 8) {
            bits.push(0);
        }
        var pad = 0xec;
        while (bits.length < limit) {
            put(pad, 8);
            pad = pad === 0xec ? 0x11 : 0xec;
        }
        var words = [];
        for (var i = 0; i < bits.length; i += 8) {
            var word = 0;
            for (var bit = 0; bit < 8; bit++) {
                word = (word << 1) | bits[i + bit];
            }
            words.push(word);
        }
        var blocks = [];
        var offset = 0;
        var maxData = 0;
        var maxEc = 0;
        RS[version].forEach(function (group) {
            for (var count = 0; count < group[0]; count++) {
                var data = words.slice(offset, offset + group[2]);
                offset += group[2];
                var ec = rsRemainder(data, group[1] - group[2]);
                blocks.push({ data: data, ec: ec });
                if (data.length > maxData) {
                    maxData = data.length;
                }
                if (ec.length > maxEc) {
                    maxEc = ec.length;
                }
            }
        });
        var out = [];
        for (var dataIndex = 0; dataIndex < maxData; dataIndex++) {
            blocks.forEach(function (block) {
                if (dataIndex < block.data.length) {
                    out.push(block.data[dataIndex]);
                }
            });
        }
        for (var ecIndex = 0; ecIndex < maxEc; ecIndex++) {
            blocks.forEach(function (block) {
                if (ecIndex < block.ec.length) {
                    out.push(block.ec[ecIndex]);
                }
            });
        }
        return out;
    }

    function bitLength(value) {
        var digits = 0;
        var current = value;
        while (current) {
            digits++;
            current >>>= 1;
        }
        return digits;
    }

    function bch(data, shift, poly) {
        var rest = data << shift;
        var polyDigits = bitLength(poly);
        while (bitLength(rest) - polyDigits >= 0) {
            rest ^= poly << (bitLength(rest) - polyDigits);
        }
        return rest;
    }

    function blank(size) {
        var rows = [];
        for (var row = 0; row < size; row++) {
            var line = [];
            for (var col = 0; col < size; col++) {
                line.push(null);
            }
            rows.push(line);
        }
        return rows;
    }

    function finder(modules, row, col) {
        var size = modules.length;
        for (var r = -1; r <= 7; r++) {
            if (row + r < 0 || row + r >= size) {
                continue;
            }
            for (var c = -1; c <= 7; c++) {
                if (col + c < 0 || col + c >= size) {
                    continue;
                }
                var edge = (r >= 0 && r <= 6 && (c === 0 || c === 6)) || (c >= 0 && c <= 6 && (r === 0 || r === 6));
                var core = r >= 2 && r <= 4 && c >= 2 && c <= 4;
                modules[row + r][col + c] = edge || core;
            }
        }
    }

    function draw(version, data, mask, reserve) {
        var size = version * 4 + 17;
        var modules = blank(size);
        finder(modules, 0, 0);
        finder(modules, size - 7, 0);
        finder(modules, 0, size - 7);
        var positions = ALIGN[version - 1];
        positions.forEach(function (row) {
            positions.forEach(function (col) {
                if (modules[row][col] !== null) {
                    return;
                }
                for (var r = -2; r <= 2; r++) {
                    for (var c = -2; c <= 2; c++) {
                        modules[row + r][col + c] = r === -2 || r === 2 || c === -2 || c === 2 || (r === 0 && c === 0);
                    }
                }
            });
        });
        for (var timing = 8; timing < size - 8; timing++) {
            if (modules[timing][6] === null) {
                modules[timing][6] = timing % 2 === 0;
            }
            if (modules[6][timing] === null) {
                modules[6][timing] = timing % 2 === 0;
            }
        }
        var format = ((0 << 3) | mask);
        var formatBits = ((format << 10) | bch(format, 10, 0x537)) ^ 0x5412;
        for (var i = 0; i < 15; i++) {
            var on = !reserve && ((formatBits >> i) & 1) === 1;
            if (i < 6) {
                modules[i][8] = on;
            } else if (i < 8) {
                modules[i + 1][8] = on;
            } else {
                modules[size - 15 + i][8] = on;
            }
            if (i < 8) {
                modules[8][size - i - 1] = on;
            } else if (i < 9) {
                modules[8][7] = on;
            } else {
                modules[8][14 - i] = on;
            }
        }
        modules[size - 8][8] = !reserve;
        if (version >= 7) {
            var versionBits = (version << 12) | bch(version, 12, 0x1f25);
            for (var bit = 0; bit < 18; bit++) {
                var dark = !reserve && ((versionBits >> bit) & 1) === 1;
                modules[Math.floor(bit / 3)][bit % 3 + size - 11] = dark;
                modules[bit % 3 + size - 11][Math.floor(bit / 3)] = dark;
            }
        }
        var upward = -1;
        var row = size - 1;
        var bitIndex = 7;
        var byteIndex = 0;
        for (var col = size - 1; col > 0; col -= 2) {
            if (col === 6) {
                col--;
            }
            for (;;) {
                for (var shift = 0; shift < 2; shift++) {
                    var x = col - shift;
                    if (modules[row][x] !== null) {
                        continue;
                    }
                    var darkBit = false;
                    if (byteIndex < data.length) {
                        darkBit = ((data[byteIndex] >> bitIndex) & 1) === 1;
                    }
                    if (masked(mask, row, x)) {
                        darkBit = !darkBit;
                    }
                    modules[row][x] = darkBit;
                    bitIndex--;
                    if (bitIndex < 0) {
                        byteIndex++;
                        bitIndex = 7;
                    }
                }
                row += upward;
                if (row < 0 || row >= size) {
                    row -= upward;
                    upward = -upward;
                    break;
                }
            }
        }
        return modules;
    }

    function masked(mask, row, col) {
        if (mask === 0) return (row + col) % 2 === 0;
        if (mask === 1) return row % 2 === 0;
        if (mask === 2) return col % 3 === 0;
        if (mask === 3) return (row + col) % 3 === 0;
        if (mask === 4) return (Math.floor(row / 2) + Math.floor(col / 3)) % 2 === 0;
        if (mask === 5) return ((row * col) % 2) + ((row * col) % 3) === 0;
        if (mask === 6) return (((row * col) % 2) + ((row * col) % 3)) % 2 === 0;
        return (((row * col) % 3) + ((row + col) % 2)) % 2 === 0;
    }

    function penalty(modules) {
        var size = modules.length;
        var score = 0;
        function run(get) {
            for (var line = 0; line < size; line++) {
                var length = 1;
                for (var i = 1; i < size; i++) {
                    if (get(line, i) === get(line, i - 1)) {
                        length++;
                    } else {
                        if (length >= 5) {
                            score += length - 2;
                        }
                        length = 1;
                    }
                }
                if (length >= 5) {
                    score += length - 2;
                }
            }
        }
        run(function (row, col) { return modules[row][col]; });
        run(function (col, row) { return modules[row][col]; });
        for (var row = 0; row < size - 1; row++) {
            for (var col = 0; col < size - 1; col++) {
                var color = modules[row][col];
                if (color === modules[row][col + 1] && color === modules[row + 1][col] && color === modules[row + 1][col + 1]) {
                    score += 3;
                }
            }
        }
        var dark = 0;
        for (var r = 0; r < size; r++) {
            for (var c = 0; c < size; c++) {
                if (modules[r][c]) {
                    dark++;
                }
            }
        }
        score += Math.floor(Math.abs(dark * 100 / (size * size) - 50) / 5) * 10;
        return score;
    }

    function matrix(text) {
        var bytes = utf8(text);
        var version = versionFor(bytes.length);
        if (!version) {
            return [];
        }
        var data = codewords(version, bytes);
        var bestMask = 0;
        var bestScore = -1;
        for (var mask = 0; mask < 8; mask++) {
            var score = penalty(draw(version, data, mask, true));
            if (bestScore < 0 || score < bestScore) {
                bestScore = score;
                bestMask = mask;
            }
        }
        return draw(version, data, bestMask, false);
    }

    function svg(text) {
        var rows = matrix(text);
        if (!rows.length) {
            return '';
        }
        var quiet = 4;
        var size = rows.length + quiet * 2;
        var marks = [];
        for (var row = 0; row < rows.length; row++) {
            for (var col = 0; col < rows.length; col++) {
                if (rows[row][col]) {
                    marks.push('<rect x="' + (col + quiet) + '" y="' + (row + quiet) + '" width="1" height="1"/>');
                }
            }
        }
        return '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ' + size + ' ' + size + '" shape-rendering="crispEdges" role="img"><rect width="' + size + '" height="' + size + '" fill="#ffffff"/><g fill="#172b4d">' + marks.join('') + '</g></svg>';
    }

    var api = { matrix: matrix, svg: svg };
    if (typeof module === 'object' && module.exports) {
        module.exports = api;
    }
    root.AssetTreeQr = api;
})(typeof window === 'undefined' ? global : window);
