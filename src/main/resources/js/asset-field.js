(function () {
    'use strict';
    if (window.__assetTreeFieldBoot) return;
    window.__assetTreeFieldBoot = true;

    function contextPath() {
        if (window.AJS && typeof AJS.contextPath === 'function') return AJS.contextPath();
        var meta = document.querySelector('meta[name="ajs-context-path"]');
        return meta ? (meta.getAttribute('content') || '') : '';
    }

    function ajax(path, callback) {
        var xhr = new XMLHttpRequest();
        xhr.open('GET', contextPath() + '/rest/asset-tree/1.0' + path, true);
        xhr.setRequestHeader('Accept', 'application/json');
        xhr.setRequestHeader('X-Atlassian-Token', 'no-check');
        xhr.onreadystatechange = function () {
            if (xhr.readyState !== 4) return;
            var payload = null;
            if (xhr.responseText) {
                try { payload = JSON.parse(xhr.responseText); } catch (error) { payload = null; }
            }
            callback(xhr.status, payload);
        };
        xhr.send(null);
    }

    function el(tag, className, text) {
        var node = document.createElement(tag);
        if (className) node.className = className;
        if (text) node.textContent = text;
        return node;
    }

    function norm(value) {
        return String(value || '')
            .replace(/[\u2010\u2011\u2012\u2013\u2014\u2212]/g, '-')
            .replace(/^\s+|\s+$/g, '')
            .replace(/\s+/g, ' ')
            .toLowerCase();
    }

    function ownText(node) {
        if (!node) return '';
        var copy = node.cloneNode(true);
        var nested = copy.querySelectorAll('select, input, textarea, button');
        for (var i = nested.length - 1; i >= 0; i--) {
            if (nested[i].parentNode) nested[i].parentNode.removeChild(nested[i]);
        }
        return norm(copy.textContent || copy.innerText || '');
    }

    function fieldCaption(control) {
        var linked = captionFor(control);
        if (linked) return linked;
        var wrapped = captionWrapped(control);
        if (wrapped) return wrapped;
        var boxed = captionInBox(control);
        if (boxed) return boxed;
        var beside = captionBefore(control);
        if (beside) return beside;
        return tidy(control.getAttribute('aria-label') || control.name || control.id || '');
    }

    function captionFor(control) {
        if (!control.id) return '';
        var labels = document.getElementsByTagName('label');
        for (var i = 0; i < labels.length; i++) {
            if (labels[i].htmlFor === control.id) {
                var direct = tidy(ownText(labels[i]));
                if (direct) return direct;
            }
        }
        return '';
    }

    function captionWrapped(control) {
        var node = control.parentNode;
        var guard = 0;
        while (node && guard < 6) {
            if (node.tagName && node.tagName.toLowerCase() === 'label') {
                var wrapped = tidy(ownText(node));
                if (wrapped) return wrapped;
            }
            if (node.tagName && node.tagName.toLowerCase() === 'form') break;
            node = node.parentNode;
            guard++;
        }
        return '';
    }

    function fieldBox(control) {
        var node = control.parentNode;
        var guard = 0;
        while (node && guard < 8) {
            var cls = node.className && typeof node.className === 'string' ? (' ' + node.className + ' ') : '';
            if (cls.indexOf(' field-container ') !== -1 || cls.indexOf(' field-group ') !== -1
                || cls.indexOf(' js-request-field ') !== -1
                || (node.getAttribute && (node.getAttribute('data-field-id') || node.getAttribute('data-field-name')))) {
                return node;
            }
            if (node.tagName && node.tagName.toLowerCase() === 'form') break;
            node = node.parentNode;
            guard++;
        }
        return null;
    }

    function captionInBox(control) {
        var box = fieldBox(control);
        if (!box || !box.getElementsByTagName) return '';
        var labels = box.getElementsByTagName('label');
        for (var i = 0; i < labels.length; i++) {
            var text = tidy(ownText(labels[i]));
            if (text) return text;
        }
        return tidy(box.getAttribute('data-field-name') || '');
    }

    function captionBefore(control) {
        var node = control;
        var guard = 0;
        while (node && guard < 5) {
            var prev = node.previousSibling;
            while (prev && prev.nodeType !== 1) prev = prev.previousSibling;
            if (prev && prev.tagName && prev.tagName.toLowerCase() === 'label') {
                var text = tidy(ownText(prev));
                if (text) return text;
            }
            if (node.tagName && node.tagName.toLowerCase() === 'form') break;
            node = node.parentNode;
            guard++;
        }
        return '';
    }

    function tidy(value) {
        return norm(String(value || '')
            .replace(/\([^)]{0,40}\)/g, ' ')
            .replace(/необязательно/gi, ' ')
            .replace(/\boptional\b/gi, ' ')
            .replace(/[*:]/g, ' '));
    }

    function blankChoice(text, value) {
        var shown = norm(text);
        var stored = norm(value);
        if (!shown && !stored) return true;
        var blanks = {
            '': true, '—': true, '-': true, 'none': true, 'n/a': true,
            'не выбрано': true, 'выберите': true, 'select': true, 'please select': true
        };
        return !!(blanks[shown] && (!stored || stored === '-1' || blanks[stored]));
    }

    function sameName(ruleField, answer) {
        var field = tidy(ruleField);
        var label = tidy(answer.label);
        if (!field) return false;
        if (field === label || field === norm(answer.id) || field === norm(answer.name)) return true;
        return label.indexOf(field + ' ') === 0;
    }

    function sameOption(ruleOption, answer) {
        var option = norm(ruleOption);
        if (!option) return false;
        return option === norm(answer.text) || option === norm(answer.value);
    }

    function formOf(node) {
        while (node) {
            if (node.tagName && node.tagName.toLowerCase() === 'form') return node;
            node = node.parentNode;
        }
        return document;
    }

    function insidePicker(node) {
        while (node) {
            if (node.className && (' ' + node.className + ' ').indexOf(' asset-tree-picker ') !== -1) return true;
            node = node.parentNode;
        }
        return false;
    }

    function insideSelect2(node) {
        while (node) {
            var cls = node.className && typeof node.className === 'string' ? ' ' + node.className + ' ' : '';
            if (cls.indexOf(' select2-container ') !== -1 || cls.indexOf(' select2-drop ') !== -1 || cls.indexOf(' select2-search ') !== -1) return true;
            node = node.parentNode;
        }
        return false;
    }

    function readAnswers(picker) {
        var form = formOf(picker);
        var answers = [];
        var controls = form.querySelectorAll('select, input, textarea');
        var i;
        for (i = 0; i < controls.length; i++) {
            var control = controls[i];
            if (insidePicker(control) || insideSelect2(control) || control.getAttribute('data-asset-tree') === '1') continue;
            var tag = control.tagName ? control.tagName.toLowerCase() : '';
            if (tag === 'select') {
                if (!control.options || control.selectedIndex < 0) continue;
                var chosen = control.options[control.selectedIndex];
                var text = chosen ? (chosen.text || '') : '';
                if (blankChoice(text, control.value)) continue;
                answers.push(answerOf(control, text, control.value || text));
                continue;
            }
            var type = (control.type || '').toLowerCase();
            if (type === 'hidden' || type === 'password' || type === 'file' || type === 'submit' || type === 'button' || type === 'image' || type === 'search') continue;
            if (type === 'radio' || type === 'checkbox') {
                if (!control.checked) continue;
                var choice = captionFor(control) || control.value;
                if (blankChoice(choice, control.value)) continue;
                answers.push(answerOf(control, choice, control.value || choice));
                continue;
            }
            if (blankChoice(control.value, control.value)) continue;
            answers.push(answerOf(control, control.value, control.value));
        }
        return answers;
    }

    function answerOf(control, text, value) {
        return {
            label: fieldCaption(control),
            id: norm(control.id),
            name: norm(control.name),
            text: text || '',
            value: value || ''
        };
    }

    /* Same precedence as PortalRules.choose: more conditions win, then the deeper place. */
    function chooseRule(rules, answers, byId) {
        var best = null;
        var bestScore = -1;
        var bestDepth = -1;
        (rules || []).forEach(function (rule) {
            if (!rule || !rule.conditions || !rule.conditions.length) return;
            var ok = rule.conditions.every(function (condition) {
                var field = norm(condition.field);
                var option = norm(condition.option);
                if (!field || !option) return false;
                return answers.some(function (answer) {
                    if (!norm(answer.value) && !norm(answer.text)) return false;
                    return sameName(condition.field, answer) && sameOption(condition.option, answer);
                });
            });
            if (!ok) return;
            var score = rule.conditions.length;
            var depth = depthOf(rule.assetId, byId);
            if (score > bestScore || (score === bestScore && depth > bestDepth)) {
                best = rule;
                bestScore = score;
                bestDepth = depth;
            }
        });
        return best;
    }

    function depthOf(id, byId) {
        var depth = 0;
        var cursor = byId[id];
        var guard = 0;
        while (cursor && guard < 40) {
            depth++;
            cursor = cursor.parentId ? byId[cursor.parentId] : null;
            guard++;
        }
        return depth;
    }

    function indexOf(nodes) {
        var byId = {};
        (nodes || []).forEach(function (node) { byId[node.id] = node; });
        return byId;
    }

    function childrenOf(nodes, parentId) {
        return (nodes || []).filter(function (node) {
            return (node.parentId || null) === (parentId || null);
        });
    }

    function pathTo(byId, id) {
        var chain = [];
        var cursor = byId[id];
        var guard = 0;
        while (cursor && guard < 40) {
            chain.unshift(cursor);
            cursor = cursor.parentId ? byId[cursor.parentId] : null;
            guard++;
        }
        return chain;
    }

    function contains(byId, rootId, id) {
        var cursor = byId[id];
        var guard = 0;
        while (cursor && guard < 40) {
            if (cursor.id === rootId) return true;
            cursor = cursor.parentId ? byId[cursor.parentId] : null;
            guard++;
        }
        return false;
    }

    function caption(options, parent) {
        var label = '';
        options.forEach(function (node) {
            if (!label) label = node.typeLabel || '';
            else if (label !== node.typeLabel) label = '';
        });
        if (label) return label;
        return parent ? parent.name : '';
    }

    function mount(picker) {
        if (!picker || picker.getAttribute('data-ready') === '1') return;
        picker.setAttribute('data-ready', '1');
        var projectKey = picker.getAttribute('data-project') || '';
        var hidden = picker.valueInput || picker.querySelector('.asset-tree-picker-value');
        var levels = picker.querySelector('.asset-tree-picker-levels');
        var current = picker.querySelector('.asset-tree-picker-current');
        var waitText = picker.getAttribute('data-wait') || phrase('wait');
        var chooseText = picker.getAttribute('data-choose') || phrase('choose');
        var emptyText = picker.getAttribute('data-empty') || phrase('empty');
        var flat = !!picker.valueInput;
        if (flat && (' ' + picker.className + ' ').indexOf(' asset-tree-picker-portal ') === -1) {
            picker.className += ' asset-tree-picker-portal';
        }
        if (!projectKey || !hidden || !levels) return;
        var nodes = null;
        var byId = {};
        var rules = [];
        var rulesReady = false;
        var nodesReady = false;
        var appliedRoot = undefined;
        var appliedLock = undefined;
        var searchByValue = {};

        ajax('/projects/' + encodeURIComponent(projectKey) + '/picker', function (status, payload) {
            nodesReady = true;
            nodes = status === 200 && payload ? payload : [];
            byId = indexOf(nodes);
            if (rulesReady) applyPortal(true);
        });
        ajax('/projects/' + encodeURIComponent(projectKey) + '/portal-rules', function (status, payload) {
            rulesReady = true;
            rules = status === 200 && payload ? payload : [];
            if (nodesReady) applyPortal(true);
        });

        function applyPortal(force) {
            if (!nodesReady || !rulesReady) return;
            var locked = rules.length > 0;
            var rule = locked ? chooseRule(rules, readAnswers(picker), byId) : null;
            var rootId = rule ? rule.assetId : null;
            if (!force && rootId === appliedRoot && locked === appliedLock) return;
            appliedRoot = rootId;
            appliedLock = locked;
            var value = hidden.value ? parseInt(hidden.value, 10) : null;
            if (locked || flat) {
                if (locked && !rootId) {
                    hidden.value = '';
                    drawList(null, true);
                    return;
                }
                if (rootId && value && !listedContains(rootId, value)) {
                    hidden.value = '';
                }
                drawList(locked ? rootId : null, false);
                return;
            }
            if (rootId && (!value || !contains(byId, rootId, value))) {
                hidden.value = String(rootId);
                value = rootId;
            }
            draw(value, rootId);
        }

        function listedContains(rootId, id) {
            var items = listed(rootId);
            for (var i = 0; i < items.length; i++) {
                if (items[i].id === id) return true;
            }
            return false;
        }

        function listed(rootId) {
            var items = [];
            (nodes || []).forEach(function (node) {
                if (rootId && !contains(byId, rootId, node.id)) return;
                items.push(node);
            });
            if (rootId && childrenOf(nodes, rootId).length) {
                items = items.filter(function (node) { return node.id !== rootId; });
            }
            items.sort(function (left, right) {
                return (left.name || '').localeCompare(right.name || '');
            });
            return items.filter(function (node) { return !node.group; });
        }

        function optionText(node) {
            var chain = pathTo(byId, node.id);
            var names = [];
            chain.forEach(function (item) {
                if (item.group) names.push(item.name);
            });
            names.push(node.name || '');
            return names.join(' / ');
        }

        function searchBlob(node) {
            return ((node && node.search) || '') + ' ' + ((node && node.objectKey) || '');
        }

        function nodeMatches(node, term) {
            if (!term) return true;
            var hay = (optionText(node) + ' ' + searchBlob(node)).toLowerCase();
            return hay.indexOf(term) !== -1;
        }

        function anchorId(id) {
            var cursor = byId[id];
            var guard = 0;
            while (cursor && cursor.group && guard < 40) {
                cursor = cursor.parentId ? byId[cursor.parentId] : null;
                guard++;
            }
            return cursor ? cursor.id : null;
        }

        function eachSelect(accept) {
            var form = formOf(picker);
            var nodes = form.querySelectorAll('select');
            for (var i = 0; i < nodes.length; i++) {
                if (insidePicker(nodes[i]) || nodes[i].getAttribute('data-asset-tree') === '1') continue;
                if (accept(nodes[i])) return nodes[i];
            }
            return null;
        }

        function shown(node) {
            if (!node || !window.getComputedStyle) return false;
            var style = window.getComputedStyle(node);
            if (!style || style.display === 'none' || style.visibility === 'hidden') return false;
            if (node.getAttribute('aria-hidden') === 'true') return false;
            var box = node.getBoundingClientRect();
            return box.width >= 8 && box.height >= 8;
        }

        function visibleSelect() {
            return eachSelect(shown);
        }

        function canSelect2() {
            return !!(flat && window.AJS && AJS.$ && AJS.$.fn && typeof AJS.$.fn.auiSelect2 === 'function');
        }

        function neighbourSelect2() {
            var form = formOf(picker);
            var boxes = form.querySelectorAll('.select2-container');
            for (var i = 0; i < boxes.length; i++) {
                var box = boxes[i];
                if (!shown(box) || insidePicker(box)) continue;
                var source = null;
                if (box.id && box.id.indexOf('s2id_') === 0) source = document.getElementById(box.id.substring(5));
                if (!source) {
                    var prev = box.previousSibling;
                    while (prev && prev.nodeType !== 1) prev = prev.previousSibling;
                    if (prev && prev.tagName && prev.tagName.toLowerCase() === 'select') source = prev;
                }
                if (!source || source.getAttribute('data-asset-tree') === '1') continue;
                return { box: box, select: source };
            }
            return null;
        }

        function destroyEnhancement(select) {
            if (!select || !window.AJS || !AJS.$ || !AJS.$.fn || typeof AJS.$.fn.auiSelect2 !== 'function') return;
            var $select = AJS.$(select);
            if (!$select || !$select.data || !$select.data('select2')) return;
            try { $select.auiSelect2('destroy'); } catch (error) { /* already removed */ }
        }

        function enhanceSelect(select) {
            if (!canSelect2()) return false;
            var neighbour = neighbourSelect2();
            var opts = { minimumResultsForSearch: 0 };
            if (neighbour && AJS.$(neighbour.select).data) {
                var api = AJS.$(neighbour.select).data('select2');
                if (api && api.opts) {
                    if (typeof api.opts.minimumResultsForSearch !== 'undefined') opts.minimumResultsForSearch = api.opts.minimumResultsForSearch;
                    if (api.opts.dropdownCssClass) opts.dropdownCssClass = api.opts.dropdownCssClass;
                    if (api.opts.containerCssClass) opts.containerCssClass = api.opts.containerCssClass;
                }
                var width = neighbour.box.getBoundingClientRect().width;
                if (width >= 8) opts.width = Math.round(width) + 'px';
            }
            opts.matcher = function (term, text, option) {
                var extra = '';
                var source = option;
                if (source && source.jquery) source = source[0];
                if (source && source.getAttribute) extra = source.getAttribute('data-search') || '';
                if (!extra && source && source.value && searchByValue[source.value]) extra = searchByValue[source.value];
                var hay = (text || '') + ' ' + extra;
                if (!term) return true;
                return hay.toUpperCase().indexOf(String(term).toUpperCase()) >= 0;
            };
            var $select = AJS.$(select);
            try {
                $select.auiSelect2(opts);
            } catch (error) {
                return false;
            }
            function nodeOf(value) {
                if (!value) return null;
                if (value.nodeType === 1) return value;
                if (value[0] && value[0].nodeType === 1) return value[0];
                return null;
            }
            function markDrop(dropdown, width) {
                if (!dropdown || dropdown.nodeType !== 1) return;
                if ((' ' + dropdown.className + ' ').indexOf(' asset-tree-portal-drop ') === -1) {
                    dropdown.className += ' asset-tree-portal-drop';
                }
                dropdown.setAttribute('data-asset-tree-drop', '1');
                dropdown.style.setProperty('--asset-tree-drop-width', width + 'px');
            }
            function alignDropdown() {
                var api = $select.data('select2');
                if (!api) return;
                var container = nodeOf(api.container);
                var choice = container ? container.querySelector('.select2-choice') : null;
                var measure = choice && shown(choice) ? choice : container;
                if (!measure) return;
                var width = Math.round(measure.getBoundingClientRect().width);
                if (width < 8) return;
                var seen = [];
                function add(node) {
                    node = nodeOf(node);
                    if (!node || seen.indexOf(node) !== -1) return;
                    seen.push(node);
                    markDrop(node, width);
                }
                add(api.dropdown);
                add(api.dropdownContainer);
                if (container) {
                    var nested = container.querySelectorAll('.select2-drop');
                    for (var i = 0; i < nested.length; i++) add(nested[i]);
                }
                if (container && (' ' + container.className + ' ').indexOf(' asset-tree-portal-select ') === -1) {
                    container.className += ' asset-tree-portal-select';
                }
                if (container) {
                    var chosen = container.querySelector('.select2-chosen');
                    if (chosen && chosen.style && chosen.style.setProperty) {
                        chosen.style.setProperty('max-width', 'none', 'important');
                        chosen.style.setProperty('width', 'auto', 'important');
                        chosen.style.setProperty('background', 'transparent', 'important');
                    }
                    var cover = container.querySelectorAll('input.select2-focusser, input.select2-offscreen');
                    for (var c = 0; c < cover.length; c++) {
                        var field = cover[c];
                        if (!field.style || !field.style.setProperty) continue;
                        field.style.setProperty('position', 'absolute', 'important');
                        field.style.setProperty('width', '1px', 'important');
                        field.style.setProperty('height', '1px', 'important');
                        field.style.setProperty('max-width', '1px', 'important');
                        field.style.setProperty('margin', '-1px', 'important');
                        field.style.setProperty('padding', '0', 'important');
                        field.style.setProperty('border', '0', 'important');
                        field.style.setProperty('overflow', 'hidden', 'important');
                        field.style.setProperty('clip', 'rect(0 0 0 0)', 'important');
                        field.style.setProperty('background', 'transparent', 'important');
                    }
                }
                var sampleChoice = null;
                var neighbour = neighbourSelect2();
                if (neighbour && neighbour.box) sampleChoice = neighbour.box.querySelector('.select2-choice');
                if (container && sampleChoice && window.getComputedStyle) {
                    var choiceBg = window.getComputedStyle(sampleChoice).backgroundColor;
                    if (choiceBg && choiceBg !== 'transparent' && choiceBg !== 'rgba(0, 0, 0, 0)') {
                        container.style.setProperty('--asset-tree-choice-bg', choiceBg);
                    }
                }
                var oursOpen = container && (' ' + container.className + ' ').indexOf(' select2-dropdown-open ') !== -1;
                if (oursOpen) {
                    var active = document.querySelectorAll('.select2-drop-active');
                    for (var j = 0; j < active.length; j++) add(active[j]);
                }
            }
            function hookPosition(api) {
                if (!api || typeof api.positionDropdown !== 'function' || api.positionDropdown._assetTree) return;
                var original = api.positionDropdown;
                var wrapped = function () {
                    var result = original.apply(this, arguments);
                    alignDropdown();
                    return result;
                };
                wrapped._assetTree = true;
                api.positionDropdown = wrapped;
            }
            if ($select.on) {
                $select.on('select2-open', function () {
                    hookPosition($select.data('select2'));
                    alignDropdown();
                    window.setTimeout(alignDropdown, 0);
                    window.setTimeout(alignDropdown, 50);
                });
            }
            hookPosition($select.data('select2'));
            alignDropdown();
            return true;
        }

        function dressSelect(select) {
            var sample = visibleSelect();
            var className = sample && sample.className ? sample.className : '';
            if (className && !/hidden|offscreen|assistive/i.test(className)) select.className = className;
            else select.className = (select.className ? select.className + ' ' : '') + 'select';
            if (canSelect2() || !flat || !sample) return;
            var style = window.getComputedStyle(sample);
            var props = [
                'box-sizing', 'height', 'min-height',
                'padding-top', 'padding-right', 'padding-bottom', 'padding-left',
                'border-top-width', 'border-right-width', 'border-bottom-width', 'border-left-width',
                'border-top-style', 'border-right-style', 'border-bottom-style', 'border-left-style',
                'border-top-color', 'border-right-color', 'border-bottom-color', 'border-left-color',
                'border-top-left-radius', 'border-top-right-radius', 'border-bottom-right-radius', 'border-bottom-left-radius',
                'background-color', 'background-image', 'background-repeat', 'background-position', 'background-size',
                'color', 'font-style', 'font-weight', 'font-size', 'line-height', 'font-family',
                'box-shadow', 'appearance', '-webkit-appearance', '-moz-appearance', 'cursor'
            ];
            for (var p = 0; p < props.length; p++) {
                var value = style.getPropertyValue(props[p]);
                if (!value || value === 'none' || value === 'auto') continue;
                if (props[p] === 'width' || props[p] === 'height' || props[p] === 'min-height' || props[p] === 'max-width') continue;
                select.style.setProperty(props[p], value);
                if ((props[p] === 'appearance' || props[p] === '-webkit-appearance' || props[p] === '-moz-appearance') && value !== 'none') {
                    select.style.setProperty('background-image', 'none');
                }
            }
            var box = sample.getBoundingClientRect();
            select.style.setProperty('box-sizing', 'border-box');
            if (box.width >= 8) select.style.setProperty('width', Math.round(box.width) + 'px');
            if (box.height >= 8) {
                var size = Math.round(box.height) + 'px';
                select.style.setProperty('height', size);
                select.style.setProperty('min-height', size);
                select.style.setProperty('max-height', size);
            }
        }

        function attachSelect(select) {
            select.setAttribute('data-asset-tree', '1');
            if (!flat || !picker.parentNode) {
                levels.appendChild(select);
                return;
            }
            destroyEnhancement(picker.portalSelect);
            if (picker.portalSelect && picker.portalSelect.parentNode) {
                picker.portalSelect.parentNode.removeChild(picker.portalSelect);
            }
            picker.portalSelect = select;
            picker.parentNode.insertBefore(select, picker);
            enhanceSelect(select);
        }

        function blankLabel(fallback) {
            var sample = visibleSelect() || eachSelect(function () { return true; });
            if (sample && sample.options && sample.options.length) {
                var first = sample.options[0];
                var value = first.value || '';
                if (!value || value === '-1') return first.text || fallback;
            }
            return fallback;
        }

        function drawList(rootId, waiting) {
            levels.innerHTML = '';
            if (picker.portalFind && picker.portalFind.parentNode) {
                picker.portalFind.parentNode.removeChild(picker.portalFind);
            }
            picker.portalFind = null;
            if (waiting) {
                var waitingSelect = el('select');
                dressSelect(waitingSelect);
                waitingSelect.disabled = true;
                var placeholder = el('option', null, blankLabel('—'));
                placeholder.value = '';
                waitingSelect.appendChild(placeholder);
                attachSelect(waitingSelect);
                levels.appendChild(el('p', 'asset-tree-picker-wait', waitText));
                if (current) current.textContent = '';
                return;
            }
            var options = listed(rootId);
            var select = el('select');
            dressSelect(select);
            var empty = el('option', null, blankLabel(chooseText));
            empty.value = '';
            select.appendChild(empty);
            var query = '';
            function paintOptions() {
                while (select.options.length > 1) select.remove(1);
                options.forEach(function (node) {
                    var blob = searchBlob(node).replace(/^\s+|\s+$/g, '');
                    searchByValue[String(node.id)] = blob;
                    var chosen = hidden.value === String(node.id);
                    if (query && !chosen && !nodeMatches(node, query)) return;
                    var option = el('option', null, optionText(node));
                    option.value = String(node.id);
                    if (blob) option.setAttribute('data-search', blob);
                    if (chosen) option.selected = true;
                    select.appendChild(option);
                });
            }
            paintOptions();
            select.addEventListener('change', function () {
                hidden.value = select.value;
                show(hidden.value ? parseInt(hidden.value, 10) : null);
            });
            attachSelect(select);
            if (!select2Ready(select)) mountFind(select, paintOptions, function (value) { query = value; });
            if (!options.length) levels.appendChild(el('p', 'asset-tree-picker-wait', emptyText));
            show(hidden.value ? parseInt(hidden.value, 10) : null);
        }

        function select2Ready(select) {
            return !!(window.AJS && AJS.$ && AJS.$(select).data && AJS.$(select).data('select2'));
        }

        function mountFind(select, paintOptions, remember) {
            if (!select.parentNode) return;
            var find = el('input', 'asset-tree-picker-find');
            find.type = 'search';
            find.setAttribute('autocomplete', 'off');
            find.placeholder = phrase('find');
            var width = select.style.getPropertyValue('width');
            if (width) find.style.setProperty('width', width);
            find.addEventListener('input', function () {
                var chosen = select.value;
                remember(find.value.replace(/^\s+|\s+$/g, '').toLowerCase());
                paintOptions();
                if (chosen) select.value = chosen;
            });
            select.parentNode.insertBefore(find, select);
            picker.portalFind = find;
        }

        function draw(value, rootId) {
            levels.innerHTML = '';
            if (rules.length && !rootId) {
                levels.appendChild(el('p', 'asset-tree-picker-wait', waitText));
                if (current) current.textContent = '';
                return;
            }
            var chain = value ? pathTo(byId, value) : [];
            var parent = null;
            var depth = 0;
            if (rootId && byId[rootId]) {
                var rootPath = pathTo(byId, rootId);
                levels.appendChild(el('p', 'asset-tree-picker-root', rootPath.map(function (node) { return node.name; }).join('  /  ')));
                parent = byId[rootId];
                depth = rootPath.length;
            }
            while (depth < 40) {
                var options = childrenOf(nodes, parent ? parent.id : null);
                if (!options.length) break;
                var chosen = chain[depth] || null;
                levels.appendChild(level(options, parent, chosen));
                if (!chosen) break;
                parent = chosen;
                depth++;
            }
            show(value);
        }

        function level(options, parent, chosen) {
            var wrap = el('label', 'asset-tree-picker-level');
            wrap.appendChild(el('span', null, caption(options, parent)));
            var select = el('select');
            dressSelect(select);
            var empty = el('option', null, '—');
            empty.value = '';
            select.appendChild(empty);
            options.forEach(function (node) {
                var option = el('option', null, node.name);
                option.value = String(node.id);
                if (chosen && chosen.id === node.id) option.selected = true;
                select.appendChild(option);
            });
            select.addEventListener('change', function () {
                var chosen = select.value ? byId[parseInt(select.value, 10)] : null;
                var cursor = chosen || parent;
                var stored = cursor ? anchorId(cursor.id) : null;
                hidden.value = stored ? String(stored) : '';
                var rootId = appliedLock ? appliedRoot : null;
                draw(cursor ? cursor.id : null, rootId);
            });
            wrap.appendChild(select);
            return wrap;
        }

        function show(value) {
            if (!current) return;
            if (flat) {
                current.textContent = '';
                return;
            }
            var chain = value ? pathTo(byId, value) : [];
            current.textContent = chain.map(function (node) { return node.name; }).join('  /  ');
        }

        picker.applyPortal = function () { applyPortal(false); };
    }

    function phrase(kind) {
        var lang = (document.documentElement.getAttribute('lang') || '').toLowerCase();
        var ru = lang.indexOf('ru') === 0;
        if (kind === 'choose') return ru ? 'Выберите актив' : 'Choose an asset';
        if (kind === 'empty') return ru ? 'В этом месте нет активов' : 'No assets in this place';
        if (kind === 'find') return ru ? 'Имя, ключ или поле' : 'Name, key, or field';
        return ru
            ? 'Заполните поля выше. Список активов откроется на подходящей площадке.'
            : 'Fill in the fields above. The asset list will open at the matching place.';
    }

    function bootMarker() {
        return document.getElementById('asset-tree-portal-boot');
    }

    function projectFromPage() {
        var marker = bootMarker();
        if (marker && marker.getAttribute('data-project')) return marker.getAttribute('data-project');
        var meta = document.querySelector('meta[name="ajs-project-key"]');
        if (meta && meta.getAttribute('content')) return meta.getAttribute('content');
        var marked = document.querySelector('[data-project-key]');
        if (marked && marked.getAttribute('data-project-key')) return marked.getAttribute('data-project-key');
        var match = /(?:\?|&)project=([A-Za-z][A-Za-z0-9_]*)/.exec(window.location.search || '');
        if (match) return match[1];
        var embedded = /"projectKey"\s*:\s*"([A-Z][A-Z0-9_]*)"/.exec(document.body ? document.body.innerHTML : '');
        return embedded ? embedded[1] : '';
    }

    function fieldsFromPage() {
        var marker = bootMarker();
        if (!marker) return [];
        var parts = (marker.getAttribute('data-fields') || '').split(',');
        var ids = [];
        for (var i = 0; i < parts.length; i++) {
            var id = parts[i].replace(/^\s+|\s+$/g, '');
            if (id.indexOf('customfield_') === 0) ids.push(id);
        }
        return ids;
    }

    function portalIdFromLocation() {
        var match = /\/portal\/(\d+)/.exec(window.location.pathname || '');
        if (match) return match[1];
        var marker = bootMarker();
        if (marker && marker.getAttribute('data-portal')) return marker.getAttribute('data-portal');
        var meta = document.querySelector('meta[name="ajs-portal-id"], meta[name="ajs-portalid"]');
        return meta ? (meta.getAttribute('content') || '') : '';
    }

    var fieldContext = null;
    var fieldWaiters = null;

    function contextComplete(ctx) {
        return !!(ctx && ctx.projectKey && ctx.fieldsSettled);
    }

    function loadFieldContext(done) {
        if (contextComplete(fieldContext)) {
            done(fieldContext);
            return;
        }
        if (fieldWaiters) {
            fieldWaiters.push(done);
            return;
        }
        fieldWaiters = [done];
        var knownFields = fieldsFromPage();
        var ctx = {
            fields: knownFields,
            fieldsSettled: knownFields.length > 0,
            projectKey: projectFromPage()
        };
        var pending = 1;
        function finish() {
            if (--pending > 0) return;
            if (contextComplete(ctx)) fieldContext = ctx;
            var waiters = fieldWaiters;
            fieldWaiters = null;
            for (var i = 0; i < waiters.length; i++) waiters[i](ctx);
        }
        if (!ctx.fieldsSettled) {
            pending++;
            ajax('/asset-fields', function (status, payload) {
                if (status === 200 && payload && payload.fields) {
                    ctx.fields = payload.fields;
                    ctx.fieldsSettled = true;
                }
                finish();
            });
        }
        var portalId = portalIdFromLocation();
        if (!ctx.projectKey && portalId) {
            pending++;
            ajax('/portals/' + encodeURIComponent(portalId), function (status, payload) {
                if (status === 200 && payload && payload.projectKey) {
                    ctx.projectKey = payload.projectKey;
                    finish();
                    return;
                }
                serviceDeskProject(portalId, function (key) {
                    if (key) ctx.projectKey = key;
                    finish();
                });
            });
        }
        finish();
    }

    function serviceDeskProject(portalId, done) {
        var paths = ['/rest/servicedeskapi/portals/', '/rest/servicedeskapi/servicedesk/'];
        var index = 0;
        function next() {
            if (index >= paths.length) {
                done('');
                return;
            }
            var xhr = new XMLHttpRequest();
            xhr.open('GET', contextPath() + paths[index] + encodeURIComponent(portalId), true);
            index++;
            xhr.setRequestHeader('Accept', 'application/json');
            xhr.setRequestHeader('X-Atlassian-Token', 'no-check');
            xhr.onreadystatechange = function () {
                if (xhr.readyState !== 4) return;
                var key = '';
                if (xhr.status === 200 && xhr.responseText) {
                    try {
                        var payload = JSON.parse(xhr.responseText);
                        key = payload && payload.projectKey ? payload.projectKey : '';
                    } catch (error) {
                        key = '';
                    }
                }
                if (key) done(key);
                else next();
            };
            xhr.send(null);
        }
        next();
    }

    function hideNative(input) {
        input.setAttribute('data-asset-tree', '1');
        input.className = (input.className ? input.className + ' ' : '') + 'asset-tree-picker-native';
        if (!input.style || !input.style.setProperty) {
            input.style.display = 'none';
            return;
        }
        input.style.setProperty('display', 'none', 'important');
        input.style.setProperty('position', 'absolute', 'important');
        input.style.setProperty('width', '1px', 'important');
        input.style.setProperty('max-width', '1px', 'important');
        input.style.setProperty('height', '1px', 'important');
        input.style.setProperty('margin', '-1px', 'important');
        input.style.setProperty('padding', '0', 'important');
        input.style.setProperty('border', '0', 'important');
        input.style.setProperty('overflow', 'hidden', 'important');
        input.style.setProperty('clip', 'rect(0 0 0 0)', 'important');
        input.style.setProperty('background', 'transparent', 'important');
    }

    function adoptTextFields(ctx) {
        if (!ctx || !ctx.projectKey || !ctx.fields || !ctx.fields.length) return;
        for (var f = 0; f < ctx.fields.length; f++) {
            var id = ctx.fields[f];
            var inputs = document.querySelectorAll(
                'input[name="' + id + '"], textarea[name="' + id + '"], input[id="' + id + '"], textarea[id="' + id + '"]'
            );
            for (var i = 0; i < inputs.length; i++) {
                var input = inputs[i];
                if (input.type === 'hidden' || insidePicker(input)) continue;
                if (input.getAttribute('data-asset-tree') === '1') {
                    hideNative(input);
                    continue;
                }
                hideNative(input);
                var picker = el('div', 'asset-tree-picker');
                picker.setAttribute('data-project', ctx.projectKey);
                picker.setAttribute('data-wait', phrase('wait'));
                picker.setAttribute('data-choose', phrase('choose'));
                picker.setAttribute('data-empty', phrase('empty'));
                picker.valueInput = input;
                picker.appendChild(el('div', 'asset-tree-picker-levels'));
                picker.appendChild(el('p', 'asset-tree-picker-current'));
                if (input.nextSibling) input.parentNode.insertBefore(picker, input.nextSibling);
                else if (input.parentNode) input.parentNode.appendChild(picker);
            }
        }
    }

    var PORTAL_ICONS = {
        laptop: 'M4 5h16a1 1 0 0 1 1 1v10H3V6a1 1 0 0 1 1-1zm1 2v7h14V7H5zM2 18h20v1.5a1 1 0 0 1-1 1H3a1 1 0 0 1-1-1V18z',
        medical: 'M12 21s-8-5.3-8-11a4.5 4.5 0 0 1 8-2.8A4.5 4.5 0 0 1 20 10c0 5.7-8 11-8 11zm-1.25-13v2.25H8.5v2.5h2.25V15h2.5v-2.25h2.25v-2.5h-2.25V8h-2.5z',
        device: 'M7 7h10v10H7V7zm2 2v6h6V9H9zm2-7h2v3h-2V2zm0 17h2v3h-2v-3zM2 11h3v2H2v-2zm17 0h3v2h-3v-2zM6 2h2v3H6V2zm10 0h2v3h-2V2zM6 19h2v3H6v-3zm10 0h2v3h-2v-3zM2 6h3v2H2V6zm17 0h3v2h-3V6zM2 16h3v2H2v-2zm17 0h3v2h-3v-2z',
        desktop: 'M3 4h18a1 1 0 0 1 1 1v11a1 1 0 0 1-1 1h-7v2h4v2H6v-2h4v-2H3a1 1 0 0 1-1-1V5a1 1 0 0 1 1-1zm1 2v9h16V6H4z',
        box: 'M12 3l9 4v10l-9 4-9-4V7l9-4zm0 2.2L6.5 7.6 12 10l5.5-2.4L12 5.2zM5 9.1v6.9l6 2.7v-6.9L5 9.1zm14 0l-6 2.7v6.9l6-2.7V9.1z'
    };
    var mineState = { payload: null, loading: false };

    function profilePage() {
        return /\/user\/profile(?:\/|$|\?)/.test(location.pathname || '');
    }

    function profileHost() {
        var selectors = ['.cv-profile', '.cv-user-profile', '.cp-profile', '.user-profile', '.asset-tree-profile-card', 'main', '#content'];
        for (var i = 0; i < selectors.length; i++) {
            var node = document.querySelector(selectors[i]);
            if (node) return node;
        }
        return document.body;
    }

    function safeColor(value) {
        return /^#[0-9a-fA-F]{3,8}$/.test(String(value || '')) ? String(value) : '#5D6B82';
    }

    function portalIcon(card) {
        var wrap = el('span', 'asset-tree-portal-mine-icon');
        wrap.style.background = safeColor(card.color);
        var path = PORTAL_ICONS[card.icon];
        if (path) {
            var svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
            svg.setAttribute('viewBox', '0 0 24 24');
            svg.setAttribute('aria-hidden', 'true');
            var shape = document.createElementNS('http://www.w3.org/2000/svg', 'path');
            shape.setAttribute('d', path);
            shape.setAttribute('fill', '#fff');
            svg.appendChild(shape);
            wrap.appendChild(svg);
        } else {
            wrap.textContent = String(card.name || '?').charAt(0).toUpperCase();
        }
        return wrap;
    }

    function mineSignature(data) {
        var parts = [data.title || '', String(data.total || 0)];
        (data.projects || []).forEach(function (group) {
            parts.push(group.key, String(group.count || 0));
            (group.assets || []).forEach(function (card) {
                parts.push(card.name, card.objectKey, card.role, card.statusLabel, card.showKey ? '1' : '0', card.showStatus ? '1' : '0');
                (card.lines || []).forEach(function (line) {
                    parts.push(line.label, line.value);
                });
            });
        });
        return parts.join('\u0001');
    }

    function renderMine(data) {
        var root = el('div', 'asset-tree-portal-mine-body');
        var head = el('div', 'asset-tree-portal-mine-head');
        head.appendChild(el('h2', null, data.title || 'My assets'));
        head.appendChild(el('span', null, data.totalLabel || ''));
        root.appendChild(head);
        (data.projects || []).forEach(function (group) {
            var block = el('section', 'asset-tree-portal-mine-group');
            var title = el('h3', null);
            title.appendChild(el('strong', null, group.key || ''));
            title.appendChild(document.createTextNode(' ' + (group.countLabel || '')));
            block.appendChild(title);
            (group.assets || []).forEach(function (card) {
                var item = el('article', 'asset-tree-portal-mine-card');
                item.appendChild(portalIcon(card));
                var body = el('div', 'asset-tree-portal-mine-copy');
                var row = el('div', 'asset-tree-portal-mine-title');
                row.appendChild(el('strong', null, card.name || ''));
                if (card.showKey && card.objectKey) row.appendChild(el('span', 'asset-tree-portal-mine-key', card.objectKey));
                if (card.showStatus && card.statusLabel) {
                    var tone = card.statusCategory ? ('cat-' + card.statusCategory) : (card.status || '');
                    row.appendChild(el('span', 'asset-tree-lozenge ' + tone, card.statusLabel));
                }
                body.appendChild(row);
                if (card.role) body.appendChild(el('p', 'asset-tree-portal-mine-role', card.role));
                (card.lines || []).forEach(function (line) {
                    var field = el('p', 'asset-tree-portal-mine-line');
                    field.appendChild(el('span', null, line.label || ''));
                    field.appendChild(document.createTextNode(line.value || ''));
                    body.appendChild(field);
                });
                item.appendChild(body);
                item.tabIndex = 0;
                item.setAttribute('role', 'button');
                item.addEventListener('click', function () { openPortalCard(card.id); });
                item.addEventListener('keydown', function (event) {
                    if (event.key === 'Enter' || event.keyCode === 13) openPortalCard(card.id);
                });
                block.appendChild(item);
            });
            root.appendChild(block);
        });
        return root;
    }

    function sendJson(method, path, body, callback) {
        var xhr = new XMLHttpRequest();
        xhr.open(method, contextPath() + '/rest/asset-tree/1.0' + path, true);
        xhr.setRequestHeader('Accept', 'application/json');
        xhr.setRequestHeader('Content-Type', 'application/json');
        xhr.setRequestHeader('X-Atlassian-Token', 'no-check');
        xhr.onreadystatechange = function () {
            if (xhr.readyState !== 4) return;
            var payload = null;
            if (xhr.responseText) {
                try { payload = JSON.parse(xhr.responseText); } catch (error) { payload = null; }
            }
            callback(xhr.status, payload);
        };
        xhr.send(body ? JSON.stringify(body) : null);
    }

    function portalInitials(name) {
        var parts = String(name || '').trim().split(/\s+/);
        if (!parts[0]) return '';
        return ((parts[0].charAt(0) || '') + (parts.length > 1 ? parts[1].charAt(0) : '')).toUpperCase();
    }

    function portalFileUrl(id, extra) {
        return contextPath() + '/plugins/servlet/asset-tree-file?id=' + encodeURIComponent(id) + (extra || '');
    }

    function closePortalCard() {
        var open = document.getElementById('asset-tree-portal-dialog');
        if (!open) return;
        if (open._onKey) document.removeEventListener('keydown', open._onKey);
        if (open.parentNode) open.parentNode.removeChild(open);
    }

    function openPortalCard(id) {
        if (!id) return;
        closePortalCard();
        var overlay = el('div', 'asset-tree-portal-dialog');
        overlay.id = 'asset-tree-portal-dialog';
        overlay.addEventListener('click', function (event) {
            if (event.target === overlay) closePortalCard();
        });
        var dialog = el('div', 'asset-tree-portal-sheet');
        dialog.appendChild(el('p', 'asset-tree-portal-loading', '...'));
        overlay.appendChild(dialog);
        function onKey(event) {
            if ((event.key === 'Escape' || event.keyCode === 27) && !document.querySelector('.asset-tree-preview-overlay')) closePortalCard();
        }
        overlay._onKey = onKey;
        document.addEventListener('keydown', onKey);
        document.body.appendChild(overlay);
        function paint(card) {
            dialog.innerHTML = '';
            dialog.className = 'asset-tree-portal-sheet';
            var head = el('div', 'asset-tree-portal-sheet-head');
            head.appendChild(portalIcon(card));
            var titles = el('div', 'asset-tree-portal-sheet-titles');
            titles.appendChild(el('h2', null, card.name || ''));
            var meta = el('div', 'asset-tree-portal-sheet-meta');
            if (card.objectKey) meta.appendChild(el('span', null, card.objectKey));
            if (card.role) meta.appendChild(el('span', null, card.role));
            if (card.statusLabel) {
                var tone = card.statusCategory ? ('cat-' + card.statusCategory) : (card.status || '');
                meta.appendChild(el('span', 'asset-tree-lozenge ' + tone, card.statusLabel));
            }
            titles.appendChild(meta);
            head.appendChild(titles);
            var closer = el('button', 'asset-tree-portal-close', '');
            closer.type = 'button';
            closer.setAttribute('aria-label', 'Закрыть');
            closer.appendChild(portalGlyph('M6 6l12 12M18 6L6 18'));
            closer.addEventListener('click', closePortalCard);
            head.appendChild(closer);
            dialog.appendChild(head);
            var body = el('div', 'asset-tree-portal-sheet-body');
            var details = portalSection('Детали');
            var grid = el('div', 'asset-tree-portal-fields');
            var described = false;
            (card.lines || []).forEach(function (line) {
                grid.appendChild(portalFact(line.label || '', line.value || ''));
                if (card.description && line.value === card.description) described = true;
            });
            if (!grid.childNodes.length && card.location) grid.appendChild(portalFact('Расположение', card.location));
            if (grid.childNodes.length) details.appendChild(grid);
            if (card.description && !described) details.appendChild(el('p', 'asset-tree-portal-prose', card.description));
            if (details.childNodes.length > 1) body.appendChild(details);
            var people = portalSection('Люди');
            people.appendChild(portalPerson('МОЛ', card.custodian));
            people.appendChild(portalPerson('Кому выдано', card.issued));
            people.appendChild(portalShareBlock(card));
            body.appendChild(people);
            body.appendChild(portalFiles(card));
            body.appendChild(portalComments(card));
            dialog.appendChild(body);
        }
        ajax('/portal/assets/' + id, function (status, payload) {
            if (status !== 200 || !payload) {
                dialog.innerHTML = '';
                dialog.appendChild(el('p', null, 'Карточка недоступна'));
                return;
            }
            paint(payload);
        });
    }

    function portalSection(title) {
        var section = el('section', 'asset-tree-portal-section');
        section.appendChild(el('h3', null, title));
        return section;
    }

    function portalFact(label, value) {
        var fact = el('div', 'asset-tree-portal-fact');
        fact.appendChild(el('span', null, label));
        fact.appendChild(el('strong', null, value));
        return fact;
    }

    function portalGlyph(d) {
        var svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
        svg.setAttribute('viewBox', '0 0 24 24');
        svg.setAttribute('aria-hidden', 'true');
        var path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
        path.setAttribute('d', d);
        path.setAttribute('fill', 'none');
        path.setAttribute('stroke', 'currentColor');
        path.setAttribute('stroke-width', '1.8');
        path.setAttribute('stroke-linecap', 'round');
        svg.appendChild(path);
        return svg;
    }

    function portalAvatar(profile) {
        var who = profile && profile.displayName ? profile.displayName : '';
        var url = profile && profile.avatarUrl ? profile.avatarUrl : '';
        if (url) {
            var img = el('img', 'asset-tree-portal-avatar');
            img.src = url;
            img.alt = '';
            img.addEventListener('error', function () {
                var fallback = el('span', 'asset-tree-portal-avatar' + (who ? '' : ' is-blank'), who ? portalInitials(who) : '');
                if (img.parentNode) img.parentNode.replaceChild(fallback, img);
            });
            return img;
        }
        return el('span', 'asset-tree-portal-avatar' + (who ? '' : ' is-blank'), who ? portalInitials(who) : '');
    }

    function portalPerson(label, profile, extra) {
        var field = el('div', 'asset-tree-portal-person');
        if (label) field.appendChild(el('div', 'asset-tree-portal-person-label', label));
        var who = profile && profile.displayName ? profile.displayName : '';
        var row = el('div', 'asset-tree-portal-face');
        row.appendChild(portalAvatar(who ? profile : null));
        var text = el('div', 'asset-tree-portal-person-text');
        text.appendChild(el('strong', null, who || 'Не назначен'));
        if (profile && profile.email) text.appendChild(el('span', null, profile.email));
        if (extra) text.appendChild(extra);
        row.appendChild(text);
        field.appendChild(row);
        return field;
    }

    function portalShareBlock(card) {
        var field = el('div', 'asset-tree-portal-person');
        field.appendChild(el('div', 'asset-tree-portal-person-label', 'Общий доступ'));
        var shares = card.shares || [];
        var mine = !shares.some(function (person) { return person && person.userKey === card.viewerKey; });
        if (!shares.length) {
            var self = null;
            if (card.viewerKey && mine) {
                self = el('button', 'asset-tree-portal-textbtn', 'Добавить меня');
                self.type = 'button';
                self.addEventListener('click', function () {
                    sendJson('POST', '/portal/assets/' + card.id + '/shares', { userKey: card.viewerKey }, function (status) {
                        if (status >= 200 && status < 300) openPortalCard(card.id);
                    });
                });
            }
            field.appendChild(portalPerson('', null, self).querySelector('.asset-tree-portal-face'));
        }
        shares.forEach(function (person) {
            var remove = el('button', 'asset-tree-portal-textbtn', 'Убрать');
            remove.type = 'button';
            remove.addEventListener('click', function () {
                sendJson('DELETE', '/portal/assets/' + card.id + '/shares/' + encodeURIComponent(person.userKey), null, function (status) {
                    if (status >= 200 && status < 300) openPortalCard(card.id);
                });
            });
            field.appendChild(portalPerson('', person, remove).querySelector('.asset-tree-portal-face'));
        });
        var tools = el('div', 'asset-tree-portal-share-tools');
        if (shares.length && card.viewerKey && mine) {
            var addMe = el('button', 'asset-tree-portal-textbtn', 'Добавить меня');
            addMe.type = 'button';
            addMe.addEventListener('click', function () {
                sendJson('POST', '/portal/assets/' + card.id + '/shares', { userKey: card.viewerKey }, function (status) {
                    if (status >= 200 && status < 300) openPortalCard(card.id);
                });
            });
            tools.appendChild(addMe);
        }
        var search = el('input');
        search.type = 'search';
        search.placeholder = 'Добавить человека';
        var hits = el('div', 'asset-tree-portal-hits');
        var timer = null;
        search.addEventListener('input', function () {
            clearTimeout(timer);
            timer = setTimeout(function () {
                var query = search.value.replace(/^\s+|\s+$/g, '');
                hits.innerHTML = '';
                if (query.length < 2) return;
                ajax('/users?q=' + encodeURIComponent(query), function (status, payload) {
                    hits.innerHTML = '';
                    if (status !== 200) return;
                    (payload || []).forEach(function (user) {
                        var taken = (card.shares || []).some(function (person) { return person && person.userKey === user.userKey; });
                        if (taken) return;
                        var hit = el('button', 'asset-tree-portal-hit', user.displayName || '');
                        hit.type = 'button';
                        hit.addEventListener('click', function () {
                            sendJson('POST', '/portal/assets/' + card.id + '/shares', { userKey: user.userKey }, function (code) {
                                if (code >= 200 && code < 300) openPortalCard(card.id);
                            });
                        });
                        hits.appendChild(hit);
                    });
                });
            }, 220);
        });
        tools.appendChild(search);
        tools.appendChild(hits);
        field.appendChild(tools);
        return field;
    }

    function portalPlain(node) {
        var text = '';
        function walk(current) {
            var child = current.firstChild;
            while (child) {
                if (child.nodeType === 3) text += child.nodeValue;
                else if (child.nodeType === 1) {
                    var name = child.tagName.toLowerCase();
                    if ((name === 'br' || name === 'p' || name === 'div') && text && text.charAt(text.length - 1) !== '\n') text += '\n';
                    walk(child);
                }
                child = child.nextSibling;
            }
        }
        walk(node);
        return text.replace(/^\s+|\s+$/g, '');
    }

    function portalComposer(placeholder) {
        var box = el('div', 'asset-tree-jira-editor');
        var bar = el('div', 'asset-tree-jira-bar');
        var visual = el('div', 'asset-tree-jira-visual is-empty');
        visual.contentEditable = 'true';
        visual.setAttribute('data-placeholder', placeholder);
        var source = el('textarea', 'asset-tree-jira-source');
        source.hidden = true;
        var textMode = false;
        function command(name, value) {
            if (textMode) return;
            visual.focus();
            try { document.execCommand(name, false, value || null); } catch (ignore) {}
            if (portalPlain(visual)) visual.classList.remove('is-empty');
            else visual.classList.add('is-empty');
        }
        function tool(label, action) {
            var node = el('button', 'asset-tree-jira-tool', label);
            node.type = 'button';
            node.addEventListener('mousedown', function (event) { event.preventDefault(); });
            node.addEventListener('click', function (event) {
                event.preventDefault();
                action();
            });
            bar.appendChild(node);
        }
        var style = el('select', 'asset-tree-jira-style');
        style.appendChild(el('option', null, 'Стиль'));
        tool('B', function () { command('bold'); });
        tool('I', function () { command('italic'); });
        tool('U', function () { command('underline'); });
        tool('•', function () { command('insertUnorderedList'); });
        tool('1.', function () { command('insertOrderedList'); });
        bar.insertBefore(style, bar.firstChild);
        box.appendChild(bar);
        box.appendChild(visual);
        box.appendChild(source);
        var foot = el('div', 'asset-tree-jira-foot');
        var tabs = el('div', 'asset-tree-jira-tabs');
        var visualTab = el('button', 'asset-tree-jira-tab is-on', 'Визуальный');
        var textTab = el('button', 'asset-tree-jira-tab', 'Текст');
        visualTab.type = 'button';
        textTab.type = 'button';
        visualTab.addEventListener('click', function () {
            if (!textMode) return;
            textMode = false;
            visual.innerHTML = '';
            String(source.value || '').split('\n').forEach(function (line, index) {
                if (index) visual.appendChild(document.createElement('br'));
                visual.appendChild(document.createTextNode(line));
            });
            source.hidden = true;
            visual.hidden = false;
            visualTab.classList.add('is-on');
            textTab.classList.remove('is-on');
        });
        textTab.addEventListener('click', function () {
            if (textMode) return;
            textMode = true;
            source.value = portalPlain(visual);
            visual.hidden = true;
            source.hidden = false;
            textTab.classList.add('is-on');
            visualTab.classList.remove('is-on');
        });
        tabs.appendChild(visualTab);
        tabs.appendChild(textTab);
        var actions = el('div', 'asset-tree-jira-actions');
        foot.appendChild(tabs);
        foot.appendChild(actions);
        box.appendChild(foot);
        return {
            root: box,
            foot: foot,
            actions: actions,
            read: function () {
                if (textMode) return String(source.value || '').replace(/^\s+|\s+$/g, '');
                return portalPlain(visual);
            },
            focus: function () { visual.focus(); }
        };
    }

    function portalFiles(card) {
        var section = portalSection('Вложения');
        (card.files || []).forEach(function (file) {
            var row = el('div', 'asset-tree-portal-file');
            var kind = String(file.fileName || '').split('.').pop().toLowerCase();
            var icon = el('button', 'asset-tree-portal-file-icon is-' + kind, (kind || 'file').slice(0, 4).toUpperCase());
            icon.type = 'button';
            icon.addEventListener('click', function () { openPortalPreview(file, card.files); });
            row.appendChild(icon);
            var name = el('button', 'asset-tree-portal-file-name', file.fileName || '');
            name.type = 'button';
            name.addEventListener('click', function () { openPortalPreview(file, card.files); });
            row.appendChild(name);
            if (file.size) row.appendChild(el('span', 'asset-tree-portal-file-meta', portalSize(file.size)));
            section.appendChild(row);
        });
        var drop = el('button', 'asset-tree-portal-drop', 'Добавить вложения');
        drop.type = 'button';
        drop.addEventListener('click', function () { openPortalAttach(card); });
        section.appendChild(drop);
        return section;
    }

    function openPortalAttach(card) {
        var queued = [];
        var overlay = el('div', 'asset-tree-attach-overlay');
        var dialog = el('div', 'asset-tree-attach-dialog');
        dialog.appendChild(el('h2', null, 'Добавить вложения'));
        var composer = portalComposer('Вставьте здесь комментарий...');
        var list = el('div', 'asset-tree-attach-list');
        function paint() {
            list.innerHTML = '';
            queued.forEach(function (file, index) {
                var cardNode = el('div', 'asset-tree-attach-card');
                cardNode.appendChild(el('span', 'asset-tree-attach-glyph', (file.name || 'file').slice(0, 4)));
                var remove = el('button', 'asset-tree-attach-remove', '\u00d7');
                remove.type = 'button';
                remove.addEventListener('click', function (event) {
                    event.preventDefault();
                    event.stopPropagation();
                    queued.splice(index, 1);
                    paint();
                });
                cardNode.appendChild(remove);
                cardNode.appendChild(el('div', 'asset-tree-attach-name', file.name || ''));
                list.appendChild(cardNode);
            });
            list.appendChild(el('p', 'asset-tree-attach-hint', 'Перетащите файлы или вставьте снимки экрана, чтобы приложить их'));
        }
        var zone = el('label', 'asset-tree-attach-drop');
        var input = el('input');
        input.type = 'file';
        input.multiple = true;
        input.addEventListener('change', function () {
            var index;
            for (index = 0; index < input.files.length; index++) queued.push(input.files[index]);
            input.value = '';
            paint();
        });
        zone.appendChild(input);
        zone.appendChild(list);
        zone.addEventListener('dragover', function (event) { event.preventDefault(); });
        zone.addEventListener('drop', function (event) {
            event.preventDefault();
            var files = event.dataTransfer && event.dataTransfer.files;
            var index;
            if (!files) return;
            for (index = 0; index < files.length; index++) queued.push(files[index]);
            paint();
        });
        function close() {
            if (overlay.parentNode) overlay.parentNode.removeChild(overlay);
        }
        function uploadAt(index, done) {
            if (index >= queued.length) {
                done(true);
                return;
            }
            var data = new FormData();
            data.append('file', queued[index], queued[index].name || 'file');
            var xhr = new XMLHttpRequest();
            xhr.open('POST', contextPath() + '/plugins/servlet/asset-tree-file?assetId=' + card.id + '&portal=1', true);
            xhr.setRequestHeader('Accept', 'application/json');
            xhr.setRequestHeader('X-Atlassian-Token', 'no-check');
            xhr.onreadystatechange = function () {
                if (xhr.readyState !== 4) return;
                if (xhr.status >= 200 && xhr.status < 300) uploadAt(index + 1, done);
            };
            xhr.send(data);
        }
        var send = el('button', 'asset-tree-btn primary', 'Сделать доступным клиенту');
        send.type = 'button';
        send.addEventListener('click', function () {
            var text = composer.read();
            uploadAt(0, function () {
                if (!text) {
                    close();
                    openPortalCard(card.id);
                    return;
                }
                sendJson('POST', '/portal/assets/' + card.id + '/comments', { body: text }, function (status) {
                    if (status >= 200 && status < 300) {
                        close();
                        openPortalCard(card.id);
                    }
                });
            });
        });
        var cancel = el('button', 'asset-tree-jira-quiet', 'Отмена');
        cancel.type = 'button';
        cancel.addEventListener('click', close);
        composer.actions.appendChild(send);
        composer.actions.appendChild(cancel);
        dialog.appendChild(composer.root);
        composer.root.insertBefore(zone, composer.foot);
        paint();
        overlay.appendChild(dialog);
        overlay.addEventListener('click', function (event) { if (event.target === overlay) close(); });
        document.body.appendChild(overlay);
        composer.focus();
    }

    function portalSize(bytes) {
        var size = Number(bytes) || 0;
        if (size < 1024) return size + ' B';
        if (size < 1048576) return Math.round(size / 1024) + ' KB';
        return (size / 1048576).toFixed(1) + ' MB';
    }

    function portalComments(card) {
        var section = portalSection('Комментарии');
        (card.comments || []).forEach(function (comment) {
            var note = el('div', 'asset-tree-portal-comment');
            note.appendChild(portalAvatar(comment.author));
            var text = el('div');
            var author = comment.author && comment.author.displayName ? comment.author.displayName : '';
            text.appendChild(el('strong', null, author));
            text.appendChild(el('p', null, comment.body || ''));
            note.appendChild(text);
            section.appendChild(note);
        });
        var composer = portalComposer('Вставьте здесь комментарий...');
        var submit = el('button', 'asset-tree-btn primary', 'Публичный комментарий');
        submit.type = 'button';
        submit.addEventListener('click', function () {
            var text = composer.read();
            if (!text) return;
            sendJson('POST', '/portal/assets/' + card.id + '/comments', { body: text }, function (status) {
                if (status >= 200 && status < 300) openPortalCard(card.id);
            });
        });
        var cancel = el('button', 'asset-tree-jira-quiet', 'Отмена');
        cancel.type = 'button';
        composer.actions.appendChild(submit);
        composer.actions.appendChild(cancel);
        section.appendChild(composer.root);
        return section;
    }

    function openPortalPreview(file, files) {
        var list = files && files.length ? files : [file];
        var index = 0;
        var i;
        for (i = 0; i < list.length; i++) if (list[i].id === file.id) index = i;
        var overlay = el('div', 'asset-tree-preview-overlay');
        var bar = el('div', 'asset-tree-preview-bar');
        var heading = el('div', 'asset-tree-preview-heading');
        var title = el('div', 'asset-tree-preview-title', '');
        heading.appendChild(title);
        var actions = el('div', 'asset-tree-preview-actions');
        var stage = el('div', 'asset-tree-preview-stage');
        bar.appendChild(heading);
        bar.appendChild(actions);
        overlay.appendChild(bar);
        overlay.appendChild(stage);
        function close() {
            document.removeEventListener('keydown', onKey);
            if (overlay.parentNode) overlay.parentNode.removeChild(overlay);
        }
        function paint(at) {
            index = at;
            if (index < 0) index = list.length - 1;
            if (index >= list.length) index = 0;
            var current = list[index];
            title.textContent = current.fileName || '';
            actions.innerHTML = '';
            var download = el('a', 'asset-tree-preview-icon');
            download.href = portalFileUrl(current.id);
            download.title = 'Скачать';
            download.setAttribute('aria-label', 'Скачать');
            download.appendChild(portalGlyph('M12 4v10M8 10l4 4 4-4M6 20h12'));
            actions.appendChild(download);
            var print = el('button', 'asset-tree-preview-icon');
            print.type = 'button';
            print.title = 'Печать';
            print.setAttribute('aria-label', 'Печать');
            print.appendChild(portalGlyph('M7 8V4h10v4M6 10h12a1 1 0 0 1 1 1v4h-3M6 15H5v-4a1 1 0 0 1 1-1M8 13h8v7H8z'));
            print.addEventListener('click', function () { window.open(portalFileUrl(current.id, '&inline=1'), '_blank'); });
            actions.appendChild(print);
            var closeBtn = el('button', 'asset-tree-preview-icon');
            closeBtn.type = 'button';
            closeBtn.title = 'Закрыть';
            closeBtn.setAttribute('aria-label', 'Закрыть');
            closeBtn.appendChild(portalGlyph('M6 6l12 12M18 6L6 18'));
            closeBtn.addEventListener('click', close);
            actions.appendChild(closeBtn);
            stage.innerHTML = '';
            var name = String(current.fileName || '').toLowerCase();
            if (/\.(png|jpe?g|gif|webp)$/.test(name)) {
                var picture = el('img', 'asset-tree-preview-full');
                picture.src = portalFileUrl(current.id, '&inline=1');
                picture.alt = current.fileName || '';
                stage.appendChild(picture);
            } else if (/\.pdf$/.test(name)) {
                var sheet = el('iframe', 'asset-tree-preview-pdf');
                sheet.src = portalFileUrl(current.id, '&inline=1');
                stage.appendChild(sheet);
            } else if (/\.(txt|csv|docx|md|json|xml|rtf|html?)$/.test(name)) {
                var pre = el('pre', 'asset-tree-preview-text', '...');
                stage.appendChild(pre);
                var xhr = new XMLHttpRequest();
                xhr.open('GET', portalFileUrl(current.id, '&preview=text'), true);
                xhr.onreadystatechange = function () {
                    if (xhr.readyState === 4) pre.textContent = xhr.responseText || '';
                };
                xhr.send(null);
            } else {
                stage.appendChild(el('p', 'asset-tree-preview-missing', 'Предпросмотр этого файла недоступен. Его можно скачать.'));
            }
        }
        function onKey(event) {
            if (event.key === 'Escape' || event.keyCode === 27) close();
            else if (event.key === 'ArrowLeft' || event.keyCode === 37) paint(index - 1);
            else if (event.key === 'ArrowRight' || event.keyCode === 39) paint(index + 1);
        }
        document.addEventListener('keydown', onKey);
        paint(index);
        document.body.appendChild(overlay);
    }

    function paintPortalMine() {
        var existing = document.getElementById('asset-tree-portal-mine');
        if (!profilePage()) {
            if (existing && existing.parentNode) existing.parentNode.removeChild(existing);
            return;
        }
        if (!mineState.payload) {
            if (mineState.loading) return;
            mineState.loading = true;
            ajax('/portal/mine', function (status, payload) {
                mineState.loading = false;
                mineState.payload = status === 200 && payload ? payload : { total: 0, projects: [] };
                paintPortalMine();
            });
            return;
        }
        var data = mineState.payload;
        if (!data.total) {
            if (existing && existing.parentNode) existing.parentNode.removeChild(existing);
            return;
        }
        var host = profileHost();
        if (!existing) {
            existing = el('section', 'asset-tree-portal-mine');
            existing.id = 'asset-tree-portal-mine';
        }
        if (existing.parentNode !== host) host.appendChild(existing);
        var signature = mineSignature(data);
        if (existing.getAttribute('data-sig') === signature) return;
        existing.setAttribute('data-sig', signature);
        while (existing.firstChild) existing.removeChild(existing.firstChild);
        existing.appendChild(renderMine(data));
    }

    function boot() {
        var pickers = document.querySelectorAll('.asset-tree-picker');
        for (var i = 0; i < pickers.length; i++) mount(pickers[i]);
        var inputs = document.querySelectorAll('input[name^="customfield_"], textarea[name^="customfield_"], input[id^="customfield_"], textarea[id^="customfield_"]');
        if (!inputs.length) return;
        loadFieldContext(function (ctx) {
            adoptTextFields(ctx);
            var created = document.querySelectorAll('.asset-tree-picker');
            for (var n = 0; n < created.length; n++) mount(created[n]);
        });
    }

    function watch() {
        if (!document.body) {
            setTimeout(watch, 50);
            return;
        }
        if (!watch.started && window.MutationObserver) {
            watch.started = true;
            var timer = null;
            var applying = false;
            var again = false;
            function run() {
                if (applying) {
                    again = true;
                    return;
                }
                applying = true;
                try {
                    boot();
                    refresh();
                    paintPortalMine();
                } finally {
                    applying = false;
                }
                if (again) {
                    again = false;
                    timer = setTimeout(run, 80);
                }
            }
            var observer = new MutationObserver(function () {
                if (applying) {
                    again = true;
                    return;
                }
                if (timer) return;
                timer = setTimeout(function () {
                    timer = null;
                    run();
                }, 80);
            });
            observer.observe(document.body, { childList: true, subtree: true });
        }
        boot();
        refresh();
        paintPortalMine();
    }

    function refresh() {
        var pickers = document.querySelectorAll('.asset-tree-picker');
        for (var i = 0; i < pickers.length; i++) {
            if (pickers[i].applyPortal) pickers[i].applyPortal();
        }
    }

    document.addEventListener('change', function (event) {
        var target = event.target;
        if (!target || insidePicker(target)) return;
        var tag = target.tagName ? target.tagName.toLowerCase() : '';
        if (tag !== 'select' && target.type !== 'radio') return;
        refresh();
    });

    if (window.AJS && AJS.toInit) AJS.toInit(watch);
    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', watch);
    else watch();
    var polls = 0;
    var timer = setInterval(function () {
        watch();
        if (++polls >= 40) clearInterval(timer);
    }, 500);
})();
