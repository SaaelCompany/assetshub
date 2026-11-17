(function () {
    var plugin = 'com.assetstree.jira.asset-tree';
    var registered = false;
    var statusCache = {};
    var typeCache = {};
    var portalCache = {};
    var fieldCache = {};

    function clear(el) {
        while (el.firstChild) {
            el.removeChild(el.firstChild);
        }
    }

    function readOnly() {
        return window.CodeBarrel && CodeBarrel.Automation && CodeBarrel.Automation.isReadOnly && CodeBarrel.Automation.isReadOnly();
    }

    function field(config, name) {
        var value = config && config.value ? config.value : {};
        return value[name] == null ? '' : String(value[name]);
    }

    function save(config, next) {
        CodeBarrel.Automation.updateComponentValue(config, next);
    }

    // A stored status name that is still an internal key is turned into words.
    // Jira only ships a phrase when AJS.I18n.getText('key') is written out below.
    function text(key) {
        if (!key || key.indexOf('asset-tree.') !== 0) {
            return key || '';
        }
        var translated = AJS.I18n.getText(key);
        return translated && translated !== key ? translated : (key || '');
    }

    function label(parent, caption) {
        var node = document.createElement('label');
        node.style.display = 'block';
        node.style.margin = '8px 0 4px';
        node.appendChild(document.createTextNode(caption));
        parent.appendChild(node);
    }

    function paragraph(parent, caption) {
        var node = document.createElement('p');
        node.appendChild(document.createTextNode(caption));
        parent.appendChild(node);
    }

    function projectKey() {
        var href = window.location ? window.location.href : '';
        var patterns = [
            /[?&]projectKey=([^&#]+)/,
            /[?&]project=([^&#]+)/,
            /\/project-config\/([^/]+)/,
            /\/projects\/([^/]+)/
        ];
        for (var i = 0; i < patterns.length; i++) {
            var match = href.match(patterns[i]);
            if (match && match[1]) {
                return decodeURIComponent(match[1]);
            }
        }
        return '';
    }

    function builtinStatuses() {
        return [
            { value: '', label: AJS.I18n.getText('asset-tree.automation.anyStatus') },
            { value: 'in_stock', label: AJS.I18n.getText('asset-tree.ui.statusStock') },
            { value: 'in_use', label: AJS.I18n.getText('asset-tree.ui.statusInUse') },
            { value: 'repair', label: AJS.I18n.getText('asset-tree.ui.statusRepair') },
            { value: 'reserve', label: AJS.I18n.getText('asset-tree.ui.statusReserve') },
            { value: 'maintenance', label: AJS.I18n.getText('asset-tree.ui.statusMaintenance') },
            { value: 'written_off', label: AJS.I18n.getText('asset-tree.ui.statusWrittenOff') }
        ];
    }

    function loadStatuses(done) {
        var key = projectKey();
        if (!key) {
            done(builtinStatuses());
            return;
        }
        if (statusCache[key]) {
            done(statusCache[key]);
            return;
        }
        var root = window.AJS && AJS.contextPath ? AJS.contextPath() : '';
        var request = new XMLHttpRequest();
        request.open('GET', root + '/rest/asset-tree/1.0/projects/' + encodeURIComponent(key) + '/statuses', true);
        request.setRequestHeader('Accept', 'application/json');
        request.onreadystatechange = function () {
            if (request.readyState !== 4) {
                return;
            }
            var options = builtinStatuses();
            if (request.status === 200) {
                try {
                    var rows = JSON.parse(request.responseText);
                    if (rows && rows.length) {
                        options = [{ value: '', label: AJS.I18n.getText('asset-tree.automation.anyStatus') }];
                        for (var i = 0; i < rows.length; i++) {
                            options.push({
                                value: rows[i].statusKey || '',
                                label: text(rows[i].label || rows[i].statusKey || '')
                            });
                        }
                    }
                } catch (ignore) {
                    options = builtinStatuses();
                }
            }
            statusCache[key] = options;
            done(options);
        };
        request.send();
    }

    function statusWord(key) {
        if (!key) {
            return '';
        }
        var options = statusCache[projectKey()] || builtinStatuses();
        for (var i = 0; i < options.length; i++) {
            if (options[i].value === key) {
                return options[i].label;
            }
        }
        return key;
    }

    function fillSelect(box, options, current) {
        while (box.firstChild) {
            box.removeChild(box.firstChild);
        }
        var known = false;
        for (var i = 0; i < options.length; i++) {
            var item = document.createElement('option');
            item.value = options[i].value;
            item.appendChild(document.createTextNode(options[i].label));
            if (item.value === current) {
                item.selected = true;
                known = true;
            }
            box.appendChild(item);
        }
        if (current && !known) {
            var extra = document.createElement('option');
            extra.value = current;
            extra.selected = true;
            extra.appendChild(document.createTextNode(current));
            box.appendChild(extra);
        }
    }

    function pick(parent, config, key, options, onChange) {
        var box = document.createElement('select');
        box.disabled = !!readOnly();
        box.style.maxWidth = '100%';
        fillSelect(box, options, field(config, key));
        box.addEventListener('change', function () {
            var value = config.value || {};
            value[key] = box.value;
            save(config, value);
            if (onChange) {
                onChange();
            }
        });
        parent.appendChild(box);
        return box;
    }

    function statusPick(parent, config, key, allowEmpty, placeholder) {
        var box = document.createElement('select');
        box.disabled = !!readOnly();
        box.style.maxWidth = '100%';
        function show(options) {
            var list = [];
            if (!allowEmpty) {
                list.push({ value: '', label: placeholder });
            }
            for (var i = 0; i < options.length; i++) {
                if (!allowEmpty && !options[i].value) {
                    continue;
                }
                list.push(options[i]);
            }
            fillSelect(box, list, field(config, key));
        }
        show(builtinStatuses());
        loadStatuses(function (options) {
            if (box.parentNode) {
                show(options);
            }
        });
        box.addEventListener('change', function () {
            var value = config.value || {};
            value[key] = box.value;
            save(config, value);
        });
        parent.appendChild(box);
    }

    function input(parent, config, key, placeholder) {
        var box = document.createElement('input');
        box.type = 'text';
        box.value = field(config, key);
        box.placeholder = placeholder;
        box.disabled = !!readOnly();
        box.style.width = '100%';
        box.style.boxSizing = 'border-box';
        box.addEventListener('input', function () {
            var value = config.value || {};
            value[key] = box.value;
            save(config, value);
        });
        parent.appendChild(box);
    }

    function eventNames() {
        return {
            '': AJS.I18n.getText('asset-tree.automation.changedAny'),
            'asset.created': AJS.I18n.getText('asset-tree.automation.changedCreated'),
            'asset.updated': AJS.I18n.getText('asset-tree.automation.changedUpdated'),
            'asset.status': AJS.I18n.getText('asset-tree.automation.changedStatus'),
            'asset.writeoff': AJS.I18n.getText('asset-tree.automation.changedWriteoff'),
            'asset.schedule': AJS.I18n.getText('asset-tree.automation.changedSchedule'),
            'asset.calendar': AJS.I18n.getText('asset-tree.automation.changedCalendar'),
            'asset.deleted': AJS.I18n.getText('asset-tree.automation.changedDeleted'),
            'asset.restored': AJS.I18n.getText('asset-tree.automation.changedRestored')
        };
    }

    function eventOptions() {
        var names = eventNames();
        var keys = ['', 'asset.created', 'asset.updated', 'asset.status', 'asset.writeoff', 'asset.schedule', 'asset.calendar', 'asset.deleted', 'asset.restored'];
        var options = [];
        for (var i = 0; i < keys.length; i++) {
            options.push({ value: keys[i], label: names[keys[i]] });
        }
        return options;
    }

    function detailOptions(event) {
        if (event === 'asset.writeoff') {
            return [
                { value: '', label: AJS.I18n.getText('asset-tree.automation.anyDetail') },
                { value: 'pending', label: AJS.I18n.getText('asset-tree.automation.detailPending') },
                { value: 'approved', label: AJS.I18n.getText('asset-tree.automation.detailApproved') },
                { value: 'rejected', label: AJS.I18n.getText('asset-tree.automation.detailRejected') }
            ];
        }
        if (event === 'asset.schedule') {
            return [
                { value: '', label: AJS.I18n.getText('asset-tree.automation.anyDetail') },
                { value: 'created', label: AJS.I18n.getText('asset-tree.automation.detailCreated') },
                { value: 'changed', label: AJS.I18n.getText('asset-tree.automation.detailChanged') },
                { value: 'moved', label: AJS.I18n.getText('asset-tree.automation.detailMoved') },
                { value: 'done', label: AJS.I18n.getText('asset-tree.automation.detailDone') },
                { value: 'deleted', label: AJS.I18n.getText('asset-tree.automation.detailDeleted') }
            ];
        }
        return null;
    }

    function detailWord(key) {
        if (!key) {
            return '';
        }
        var options = detailOptions('asset.writeoff').concat(detailOptions('asset.schedule'));
        for (var i = 0; i < options.length; i++) {
            if (options[i].value === key) {
                return options[i].label;
            }
        }
        return key;
    }

    function changed() {
        return {
            type: plugin + ':asset-changed-trigger',
            componentType: 'TRIGGER',
            name: function () { return AJS.I18n.getText('asset-tree.automation.changed'); },
            icon: function () { return '../' + plugin + ':asset-tree-automation/automation.svg'; },
            initialValue: function () { return { event: '' }; },
            categories: [],
            labels: [],
            getRenderer: function () {
                return {
                    summaryLabel: function () { return AJS.I18n.getText('asset-tree.automation.changed'); },
                    renderSummary: function (config, context, el) {
                        clear(el);
                        var names = eventNames();
                        var chosen = field(config, 'event');
                        var summary = names[chosen] || chosen;
                        var status = statusWord(field(config, 'status'));
                        if (status) summary += ': ' + status;
                        var detail = detailWord(field(config, 'detail'));
                        if (detail) summary += ' / ' + detail;
                        el.appendChild(document.createTextNode(summary));
                    },
                    renderDetailed: function (config, context, el) {
                        function paint() {
                            clear(el);
                            paragraph(el, AJS.I18n.getText('asset-tree.automation.changedCard'));
                            label(el, AJS.I18n.getText('asset-tree.automation.event'));
                            pick(el, config, 'event', eventOptions(), function () {
                                var options = detailOptions(field(config, 'event'));
                                var current = field(config, 'detail');
                                var kept = false;
                                if (options) {
                                    for (var i = 0; i < options.length; i++) {
                                        if (options[i].value === current) kept = true;
                                    }
                                }
                                if (!kept) {
                                    var value = config.value || {};
                                    value.detail = '';
                                    save(config, value);
                                }
                                paint();
                            });
                            label(el, AJS.I18n.getText('asset-tree.automation.statusList'));
                            statusPick(el, config, 'status', true, AJS.I18n.getText('asset-tree.automation.anyStatus'));
                            var details = detailOptions(field(config, 'event'));
                            if (details) {
                                label(el, AJS.I18n.getText('asset-tree.automation.detailList'));
                                pick(el, config, 'detail', details);
                            }
                        }
                        paint();
                    },
                    renderCard: function (el) {
                        clear(el);
                        el.appendChild(document.createTextNode(AJS.I18n.getText('asset-tree.automation.changedCard')));
                    }
                };
            },
            validate: function () { return {}; }
        };
    }

    function linked() {
        return {
            type: plugin + ':asset-linked-condition',
            componentType: 'CONDITION',
            name: function () { return AJS.I18n.getText('asset-tree.automation.linked'); },
            icon: function () { return '../' + plugin + ':asset-tree-automation/automation.svg'; },
            initialValue: function () { return { status: '' }; },
            categories: [],
            labels: [],
            getRenderer: function () {
                return {
                    summaryLabel: function () { return AJS.I18n.getText('asset-tree.automation.linked'); },
                    renderSummary: function (config, context, el) {
                        clear(el);
                        el.appendChild(document.createTextNode(statusWord(field(config, 'status'))));
                    },
                    renderDetailed: function (config, context, el) {
                        clear(el);
                        paragraph(el, AJS.I18n.getText('asset-tree.automation.linkedCard'));
                        label(el, AJS.I18n.getText('asset-tree.automation.linkedStatus'));
                        statusPick(el, config, 'status', true, AJS.I18n.getText('asset-tree.automation.anyStatus'));
                    },
                    renderCard: function (el) {
                        clear(el);
                        el.appendChild(document.createTextNode(AJS.I18n.getText('asset-tree.automation.linkedCard')));
                    }
                };
            },
            validate: function () { return {}; }
        };
    }

    function statusAction() {
        return {
            type: plugin + ':asset-status-action',
            componentType: 'ACTION',
            name: function () { return AJS.I18n.getText('asset-tree.automation.status'); },
            icon: function () { return '../' + plugin + ':asset-tree-automation/automation.svg'; },
            initialValue: function () { return { status: '' }; },
            categories: [],
            labels: [],
            isEditable: true,
            getRenderer: function () {
                return {
                    summaryLabel: function () { return AJS.I18n.getText('asset-tree.automation.status'); },
                    renderSummary: function (config, context, el) {
                        clear(el);
                        el.appendChild(document.createTextNode(statusWord(field(config, 'status'))));
                    },
                    renderDetailed: function (config, context, el) {
                        clear(el);
                        paragraph(el, AJS.I18n.getText('asset-tree.automation.statusCard'));
                        label(el, AJS.I18n.getText('asset-tree.automation.statusField'));
                        statusPick(el, config, 'status', false, AJS.I18n.getText('asset-tree.automation.statusChoose'));
                    },
                    renderCard: function (el) {
                        clear(el);
                        el.appendChild(document.createTextNode(AJS.I18n.getText('asset-tree.automation.statusCard')));
                    }
                };
            },
            validate: function (rule, config) {
                if (!field(config, 'status')) {
                    var errors = {};
                    errors.status = AJS.I18n.getText('asset-tree.automation.statusRequired');
                    return errors;
                }
                return {};
            }
        };
    }

    function requestAction() {
        return {
            type: plugin + ':calendar-request-action',
            componentType: 'ACTION',
            name: function () { return AJS.I18n.getText('asset-tree.automation.request'); },
            icon: function () { return '../' + plugin + ':asset-tree-automation/automation.svg'; },
            initialValue: function () {
                return {
                    issueTypeId: '',
                    issueTypeName: '',
                    requestTypeId: '',
                    requestTypeName: '',
                    customer: 'assignee',
                    customerUser: '',
                    customerName: '',
                    fieldValues: ''
                };
            },
            categories: [],
            labels: [],
            isEditable: true,
            getRenderer: function () {
                return {
                    summaryLabel: function () { return AJS.I18n.getText('asset-tree.automation.request'); },
                    renderSummary: function (config, context, el) {
                        clear(el);
                        var name = field(config, 'requestTypeName') || field(config, 'issueTypeName') || field(config, 'issueTypeId');
                        el.appendChild(document.createTextNode(name || AJS.I18n.getText('asset-tree.automation.requestChoose')));
                    },
                    renderDetailed: function (config, context, el) {
                        clear(el);
                        paragraph(el, AJS.I18n.getText('asset-tree.automation.requestCard'));
                        label(el, AJS.I18n.getText('asset-tree.automation.requestPortal'));
                        var portalExtra = document.createElement('div');
                        portalPick(el, config, function () { paintPortalExtra(portalExtra, config); });
                        el.appendChild(portalExtra);
                        paintPortalExtra(portalExtra, config);
                        label(el, AJS.I18n.getText('asset-tree.automation.requestType'));
                        typePick(el, config);
                    },
                    renderCard: function (el) {
                        clear(el);
                        el.appendChild(document.createTextNode(AJS.I18n.getText('asset-tree.automation.requestCard')));
                    }
                };
            },
            validate: function (rule, config) {
                if (!field(config, 'issueTypeId') && !field(config, 'requestTypeId')) {
                    var errors = {};
                    errors.issueTypeId = AJS.I18n.getText('asset-tree.automation.requestTypeRequired');
                    return errors;
                }
                return {};
            }
        };
    }

    function portalPick(parent, config, onPick) {
        var box = document.createElement('select');
        box.disabled = !!readOnly();
        box.style.maxWidth = '100%';
        function show(options) {
            var list = [{ value: '', label: AJS.I18n.getText('asset-tree.automation.requestPortalEmpty') }];
            for (var i = 0; i < options.length; i++) {
                if (options[i].value) {
                    list.push(options[i]);
                }
            }
            fillSelect(box, list, field(config, 'requestTypeId'));
        }
        var stored = field(config, 'requestTypeId');
        show(stored ? [{ value: stored, label: field(config, 'requestTypeName') || stored }] : []);
        loadRequestTypes(function (options) {
            if (box.parentNode) {
                show(options);
            }
        });
        box.addEventListener('change', function () {
            var value = config.value || {};
            value.requestTypeId = box.value;
            var caption = '';
            if (box.selectedIndex >= 0 && box.options[box.selectedIndex]) {
                caption = box.options[box.selectedIndex].textContent || '';
            }
            value.requestTypeName = box.value ? caption : '';
            value.fieldValues = '';
            save(config, value);
            if (typeof onPick === 'function') {
                onPick();
            }
        });
        parent.appendChild(box);
    }

    function paintPortalExtra(parent, config) {
        clear(parent);
        if (!field(config, 'requestTypeId')) {
            return;
        }
        label(parent, AJS.I18n.getText('asset-tree.automation.requestCustomer'));
        var role = document.createElement('select');
        role.disabled = !!readOnly();
        role.style.maxWidth = '100%';
        fillSelect(role, [
            { value: 'assignee', label: AJS.I18n.getText('asset-tree.automation.requestCustomerAssignee') },
            { value: 'custodian', label: AJS.I18n.getText('asset-tree.automation.requestCustomerCustodian') },
            { value: 'user', label: AJS.I18n.getText('asset-tree.automation.requestCustomerUser') }
        ], field(config, 'customer') || 'assignee');
        role.addEventListener('change', function () {
            var value = config.value || {};
            value.customer = role.value;
            save(config, value);
            paintPortalExtra(parent, config);
        });
        parent.appendChild(role);
        if ((field(config, 'customer') || 'assignee') === 'user') {
            customerFinder(parent, config);
        }
        var fields = document.createElement('div');
        parent.appendChild(fields);
        var typeId = field(config, 'requestTypeId');
        loadRequestFields(typeId, function (rows) {
            if (!fields.parentNode) {
                return;
            }
            clear(fields);
            if (!rows.length) {
                return;
            }
            label(fields, AJS.I18n.getText('asset-tree.automation.requestFields'));
            var stored = fieldMap(config);
            for (var i = 0; i < rows.length; i++) {
                addPortalField(fields, config, rows[i], stored);
            }
        });
    }

    function fieldMap(config) {
        var raw = field(config, 'fieldValues');
        if (!raw) {
            return {};
        }
        try {
            var parsed = JSON.parse(raw);
            return parsed && typeof parsed === 'object' ? parsed : {};
        } catch (ignore) {
            return {};
        }
    }

    function writeField(config, id, text) {
        var map = fieldMap(config);
        var next = text ? String(text).trim() : '';
        if (next) {
            map[id] = next;
        } else {
            delete map[id];
        }
        var value = config.value || {};
        value.fieldValues = JSON.stringify(map);
        save(config, value);
    }

    function addPortalField(parent, config, row, stored) {
        if (!row || !row.id) {
            return;
        }
        label(parent, row.name || row.id);
        var current = stored[row.id] || '';
        if (row.kind === 'select' && row.options && row.options.length) {
            var box = document.createElement('select');
            box.disabled = !!readOnly();
            box.style.maxWidth = '100%';
            var options = [{ value: '', label: AJS.I18n.getText('asset-tree.automation.requestChoose') }];
            for (var i = 0; i < row.options.length; i++) {
                options.push({ value: row.options[i].id || '', label: row.options[i].name || row.options[i].id || '' });
            }
            fillSelect(box, options, current);
            box.addEventListener('change', function () {
                writeField(config, row.id, box.value);
            });
            parent.appendChild(box);
            return;
        }
        var input = document.createElement(row.kind === 'textarea' ? 'textarea' : 'input');
        if (input.tagName === 'INPUT') {
            input.type = 'text';
        }
        input.value = current;
        input.disabled = !!readOnly();
        input.style.width = '100%';
        input.style.boxSizing = 'border-box';
        input.addEventListener('change', function () {
            writeField(config, row.id, input.value);
        });
        parent.appendChild(input);
    }

    function customerFinder(parent, config) {
        var box = document.createElement('input');
        box.type = 'text';
        box.value = field(config, 'customerName');
        box.placeholder = AJS.I18n.getText('asset-tree.automation.mailFind');
        box.disabled = !!readOnly();
        box.style.width = '100%';
        box.style.boxSizing = 'border-box';
        box.style.marginTop = '4px';
        var list = document.createElement('select');
        list.size = 4;
        list.style.width = '100%';
        list.style.marginTop = '4px';
        list.disabled = !!readOnly();
        var timer = null;
        function show(rows) {
            while (list.firstChild) {
                list.removeChild(list.firstChild);
            }
            for (var i = 0; i < rows.length; i++) {
                var item = document.createElement('option');
                item.value = rows[i].userKey || '';
                var caption = rows[i].displayName || rows[i].username || item.value;
                item.appendChild(document.createTextNode(caption));
                item.setAttribute('data-name', caption);
                list.appendChild(item);
            }
        }
        box.addEventListener('input', function () {
            if (timer) {
                clearTimeout(timer);
            }
            var query = box.value;
            timer = setTimeout(function () {
                if (query.length < 2) {
                    show([]);
                    return;
                }
                var root = window.AJS && AJS.contextPath ? AJS.contextPath() : '';
                var request = new XMLHttpRequest();
                request.open('GET', root + '/rest/asset-tree/1.0/users?q=' + encodeURIComponent(query), true);
                request.setRequestHeader('Accept', 'application/json');
                request.onreadystatechange = function () {
                    if (request.readyState !== 4 || request.status !== 200) {
                        return;
                    }
                    try {
                        show(JSON.parse(request.responseText) || []);
                    } catch (ignore) {
                        show([]);
                    }
                };
                request.send();
            }, 250);
        });
        list.addEventListener('change', function () {
            var chosen = list.options[list.selectedIndex];
            if (!chosen) {
                return;
            }
            var value = config.value || {};
            value.customerUser = chosen.value;
            value.customerName = chosen.getAttribute('data-name') || chosen.text;
            box.value = value.customerName;
            save(config, value);
        });
        parent.appendChild(box);
        parent.appendChild(list);
    }

    function loadRequestFields(typeId, done) {
        var key = projectKey();
        if (!key || !typeId) {
            done([]);
            return;
        }
        var cacheKey = key + ':' + typeId;
        if (fieldCache[cacheKey]) {
            done(fieldCache[cacheKey]);
            return;
        }
        var root = window.AJS && AJS.contextPath ? AJS.contextPath() : '';
        var request = new XMLHttpRequest();
        request.open('GET', root + '/rest/asset-tree/1.0/projects/' + encodeURIComponent(key)
            + '/request-types/' + encodeURIComponent(typeId) + '/fields', true);
        request.setRequestHeader('Accept', 'application/json');
        request.onreadystatechange = function () {
            if (request.readyState !== 4) {
                return;
            }
            var rows = [];
            if (request.status === 200) {
                try {
                    rows = JSON.parse(request.responseText) || [];
                } catch (ignore) {
                    rows = [];
                }
            }
            fieldCache[cacheKey] = rows;
            done(rows);
        };
        request.send();
    }

    function loadRequestTypes(done) {
        var key = projectKey();
        if (!key) {
            done([]);
            return;
        }
        if (portalCache[key]) {
            done(portalCache[key]);
            return;
        }
        var root = window.AJS && AJS.contextPath ? AJS.contextPath() : '';
        var request = new XMLHttpRequest();
        request.open('GET', root + '/rest/asset-tree/1.0/projects/' + encodeURIComponent(key) + '/request-types', true);
        request.setRequestHeader('Accept', 'application/json');
        request.onreadystatechange = function () {
            if (request.readyState !== 4) {
                return;
            }
            var options = [];
            if (request.status === 200) {
                try {
                    var rows = JSON.parse(request.responseText) || [];
                    for (var i = 0; i < rows.length; i++) {
                        options.push({ value: rows[i].id || '', label: rows[i].name || rows[i].id || '' });
                    }
                } catch (ignore) {
                    options = [];
                }
            }
            portalCache[key] = options;
            done(options);
        };
        request.send();
    }

    function typePick(parent, config) {
        var box = document.createElement('select');
        box.disabled = !!readOnly();
        box.style.maxWidth = '100%';
        var placeholder = AJS.I18n.getText('asset-tree.automation.requestChoose');
        function show(options) {
            var list = [{ value: '', label: placeholder }];
            for (var i = 0; i < options.length; i++) {
                if (options[i].value) {
                    list.push(options[i]);
                }
            }
            fillSelect(box, list, field(config, 'issueTypeId'));
        }
        var stored = field(config, 'issueTypeId');
        var storedName = field(config, 'issueTypeName') || stored;
        show(stored ? [{ value: stored, label: storedName }] : []);
        loadIssueTypes(function (options) {
            if (box.parentNode) {
                show(options);
            }
        });
        box.addEventListener('change', function () {
            var value = config.value || {};
            value.issueTypeId = box.value;
            var caption = '';
            if (box.selectedIndex >= 0 && box.options[box.selectedIndex]) {
                caption = box.options[box.selectedIndex].textContent || '';
            }
            value.issueTypeName = box.value ? caption : '';
            save(config, value);
        });
        parent.appendChild(box);
    }

    function loadIssueTypes(done) {
        var key = projectKey();
        if (!key) {
            done([]);
            return;
        }
        if (typeCache[key]) {
            done(typeCache[key]);
            return;
        }
        var root = window.AJS && AJS.contextPath ? AJS.contextPath() : '';
        var request = new XMLHttpRequest();
        request.open('GET', root + '/rest/asset-tree/1.0/projects/' + encodeURIComponent(key) + '/issue-types', true);
        request.setRequestHeader('Accept', 'application/json');
        request.onreadystatechange = function () {
            if (request.readyState !== 4) {
                return;
            }
            var options = [];
            if (request.status === 200) {
                try {
                    var rows = JSON.parse(request.responseText) || [];
                    for (var i = 0; i < rows.length; i++) {
                        options.push({ value: rows[i].id || '', label: rows[i].name || rows[i].id || '' });
                    }
                } catch (ignore) {
                    options = [];
                }
            }
            typeCache[key] = options;
            done(options);
        };
        request.send();
    }

    function linkAction() {
        return {
            type: plugin + ':asset-link-action',
            componentType: 'ACTION',
            name: function () { return AJS.I18n.getText('asset-tree.automation.link'); },
            icon: function () { return '../' + plugin + ':asset-tree-automation/automation.svg'; },
            initialValue: function () { return {}; },
            categories: [],
            labels: [],
            getRenderer: function () {
                return {
                    summaryLabel: function () { return AJS.I18n.getText('asset-tree.automation.link'); },
                    renderSummary: function (config, context, el) {
                        clear(el);
                        el.appendChild(document.createTextNode(AJS.I18n.getText('asset-tree.automation.link')));
                    },
                    renderDetailed: function (config, context, el) {
                        clear(el);
                        paragraph(el, AJS.I18n.getText('asset-tree.automation.linkCard'));
                    },
                    renderCard: function (el) {
                        clear(el);
                        el.appendChild(document.createTextNode(AJS.I18n.getText('asset-tree.automation.linkCard')));
                    }
                };
            },
            validate: function () { return {}; }
        };
    }

    function recipientOptions() {
        return [
            { value: 'custodian', label: AJS.I18n.getText('asset-tree.automation.mailCustodian') },
            { value: 'issued', label: AJS.I18n.getText('asset-tree.automation.mailIssued') },
            { value: 'approver', label: AJS.I18n.getText('asset-tree.automation.mailApprover') },
            { value: 'requester', label: AJS.I18n.getText('asset-tree.automation.mailRequester') },
            { value: 'assignee', label: AJS.I18n.getText('asset-tree.automation.mailAssignee') },
            { value: 'user', label: AJS.I18n.getText('asset-tree.automation.mailUser') }
        ];
    }

    function recipientWord(key) {
        var options = recipientOptions();
        for (var i = 0; i < options.length; i++) {
            if (options[i].value === key) {
                return options[i].label;
            }
        }
        return recipientOptions()[0].label;
    }

    function lineOptions() {
        return [
            ['key', AJS.I18n.getText('asset-tree.automation.mailKey')],
            ['name', AJS.I18n.getText('asset-tree.automation.mailName')],
            ['status', AJS.I18n.getText('asset-tree.automation.mailStatus')],
            ['previous', AJS.I18n.getText('asset-tree.automation.mailPrevious')],
            ['plan', AJS.I18n.getText('asset-tree.automation.mailPlan')],
            ['due', AJS.I18n.getText('asset-tree.automation.mailDue')],
            ['detail', AJS.I18n.getText('asset-tree.automation.mailDetail')],
            ['note', AJS.I18n.getText('asset-tree.automation.mailNote')]
        ];
    }

    function hasPart(config, token) {
        var parts = field(config, 'parts');
        if (parts === 'none') {
            return false;
        }
        if (!parts) {
            return true;
        }
        var bits = parts.split(',');
        for (var i = 0; i < bits.length; i++) {
            if (bits[i] === token) {
                return true;
            }
        }
        return false;
    }

    function writeParts(config, token, on) {
        var lines = lineOptions();
        var next = [];
        for (var i = 0; i < lines.length; i++) {
            var key = lines[i][0];
            var kept = key === token ? on : hasPart(config, key);
            if (kept) {
                next.push(key);
            }
        }
        var value = config.value || {};
        value.parts = next.length ? next.join(',') : 'none';
        save(config, value);
    }

    function personFinder(parent, config) {
        var box = document.createElement('input');
        box.type = 'text';
        box.value = field(config, 'userName');
        box.placeholder = AJS.I18n.getText('asset-tree.automation.mailFind');
        box.disabled = !!readOnly();
        box.style.width = '100%';
        box.style.boxSizing = 'border-box';
        var list = document.createElement('select');
        list.size = 4;
        list.style.width = '100%';
        list.style.marginTop = '4px';
        list.disabled = !!readOnly();
        var timer = null;
        function show(rows) {
            while (list.firstChild) {
                list.removeChild(list.firstChild);
            }
            for (var i = 0; i < rows.length; i++) {
                var item = document.createElement('option');
                item.value = rows[i].userKey || '';
                var caption = rows[i].displayName || rows[i].username || item.value;
                item.appendChild(document.createTextNode(caption));
                item.setAttribute('data-name', caption);
                list.appendChild(item);
            }
        }
        box.addEventListener('input', function () {
            if (timer) {
                clearTimeout(timer);
            }
            var query = box.value;
            timer = setTimeout(function () {
                if (query.length < 2) {
                    show([]);
                    return;
                }
                var root = window.AJS && AJS.contextPath ? AJS.contextPath() : '';
                var request = new XMLHttpRequest();
                request.open('GET', root + '/rest/asset-tree/1.0/users?q=' + encodeURIComponent(query), true);
                request.setRequestHeader('Accept', 'application/json');
                request.onreadystatechange = function () {
                    if (request.readyState !== 4 || request.status !== 200) {
                        return;
                    }
                    try {
                        show(JSON.parse(request.responseText) || []);
                    } catch (ignore) {
                        show([]);
                    }
                };
                request.send();
            }, 250);
        });
        list.addEventListener('change', function () {
            var chosen = list.options[list.selectedIndex];
            if (!chosen) {
                return;
            }
            var value = config.value || {};
            value.userKey = chosen.value;
            value.userName = chosen.getAttribute('data-name') || chosen.text;
            box.value = value.userName;
            save(config, value);
        });
        parent.appendChild(box);
        parent.appendChild(list);
    }

    function mailAction() {
        return {
            type: plugin + ':asset-mail-action',
            componentType: 'ACTION',
            name: function () { return AJS.I18n.getText('asset-tree.automation.mail'); },
            icon: function () { return '../' + plugin + ':asset-tree-automation/automation.svg'; },
            initialValue: function () {
                return { to: 'custodian', parts: 'key,name,status,previous,plan,due,detail,note', extra: '' };
            },
            categories: [],
            labels: [],
            getRenderer: function () {
                return {
                    summaryLabel: function () { return AJS.I18n.getText('asset-tree.automation.mail'); },
                    renderSummary: function (config, context, el) {
                        clear(el);
                        var summary = recipientWord(field(config, 'to') || 'custodian');
                        var person = field(config, 'userName');
                        if (field(config, 'to') === 'user' && person) {
                            summary += ': ' + person;
                        }
                        el.appendChild(document.createTextNode(summary));
                    },
                    renderDetailed: function (config, context, el) {
                        function paint() {
                            clear(el);
                            paragraph(el, AJS.I18n.getText('asset-tree.automation.mailCard'));
                            label(el, AJS.I18n.getText('asset-tree.automation.mailTo'));
                            pick(el, config, 'to', recipientOptions(), paint);
                            if ((field(config, 'to') || 'custodian') === 'user') {
                                personFinder(el, config);
                            }
                            label(el, AJS.I18n.getText('asset-tree.automation.mailLines'));
                            var lines = lineOptions();
                            for (var i = 0; i < lines.length; i++) {
                                (function (token, caption) {
                                    var row = document.createElement('label');
                                    row.style.display = 'block';
                                    var box = document.createElement('input');
                                    box.type = 'checkbox';
                                    box.checked = hasPart(config, token);
                                    box.disabled = !!readOnly();
                                    box.addEventListener('change', function () {
                                        writeParts(config, token, box.checked);
                                    });
                                    row.appendChild(box);
                                    row.appendChild(document.createTextNode(' ' + caption));
                                    el.appendChild(row);
                                }(lines[i][0], lines[i][1]));
                            }
                            label(el, AJS.I18n.getText('asset-tree.automation.mailExtra'));
                            input(el, config, 'extra', AJS.I18n.getText('asset-tree.automation.mailExtraHint'));
                        }
                        paint();
                    },
                    renderCard: function (el) {
                        clear(el);
                        el.appendChild(document.createTextNode(AJS.I18n.getText('asset-tree.automation.mailCard')));
                    }
                };
            },
            validate: function (rule, config) {
                if (field(config, 'to') === 'user' && !field(config, 'userKey')) {
                    var errors = {};
                    errors.userKey = AJS.I18n.getText('asset-tree.automation.mailRequired');
                    return errors;
                }
                return {};
            }
        };
    }

    var workflowCache = {};

    function loadWorkflow(done) {
        var key = projectKey();
        if (!key) {
            done([]);
            return;
        }
        if (workflowCache[key]) {
            done(workflowCache[key]);
            return;
        }
        var root = window.AJS && AJS.contextPath ? AJS.contextPath() : '';
        var request = new XMLHttpRequest();
        request.open('GET', root + '/rest/asset-tree/1.0/projects/' + encodeURIComponent(key) + '/workflow-statuses', true);
        request.setRequestHeader('Accept', 'application/json');
        request.onreadystatechange = function () {
            if (request.readyState !== 4) {
                return;
            }
            var options = [{ value: '', label: AJS.I18n.getText('asset-tree.automation.anyIssueStatus') }];
            if (request.status === 200) {
                try {
                    var rows = JSON.parse(request.responseText) || [];
                    for (var i = 0; i < rows.length; i++) {
                        options.push({ value: rows[i].id || '', label: rows[i].name || rows[i].id || '' });
                    }
                } catch (ignore) {
                    options = [{ value: '', label: AJS.I18n.getText('asset-tree.automation.anyIssueStatus') }];
                }
            }
            workflowCache[key] = options;
            done(options);
        };
        request.send();
    }

    function issueStatusWord(key) {
        if (!key) {
            return '';
        }
        var options = workflowCache[projectKey()] || [];
        for (var i = 0; i < options.length; i++) {
            if (options[i].value === key) {
                return options[i].label;
            }
        }
        return key;
    }

    function issueStatus() {
        return {
            type: plugin + ':issue-status-trigger',
            componentType: 'TRIGGER',
            name: function () { return AJS.I18n.getText('asset-tree.automation.issueStatus'); },
            icon: function () { return '../' + plugin + ':asset-tree-automation/automation.svg'; },
            initialValue: function () { return { issueStatus: '' }; },
            categories: [],
            labels: [],
            getRenderer: function () {
                return {
                    summaryLabel: function () { return AJS.I18n.getText('asset-tree.automation.issueStatus'); },
                    renderSummary: function (config, context, el) {
                        clear(el);
                        var name = issueStatusWord(field(config, 'issueStatus'));
                        el.appendChild(document.createTextNode(name || AJS.I18n.getText('asset-tree.automation.anyIssueStatus')));
                    },
                    renderDetailed: function (config, context, el) {
                        clear(el);
                        paragraph(el, AJS.I18n.getText('asset-tree.automation.issueCard'));
                        label(el, AJS.I18n.getText('asset-tree.automation.issueList'));
                        var box = document.createElement('select');
                        box.disabled = !!readOnly();
                        box.style.maxWidth = '100%';
                        function show(options) {
                            fillSelect(box, options, field(config, 'issueStatus'));
                        }
                        show([{ value: '', label: AJS.I18n.getText('asset-tree.automation.anyIssueStatus') }]);
                        loadWorkflow(function (options) {
                            if (box.parentNode) {
                                show(options);
                            }
                        });
                        box.addEventListener('change', function () {
                            var value = config.value || {};
                            value.issueStatus = box.value;
                            save(config, value);
                        });
                        el.appendChild(box);
                    },
                    renderCard: function (el) {
                        clear(el);
                        el.appendChild(document.createTextNode(AJS.I18n.getText('asset-tree.automation.issueCard')));
                    }
                };
            },
            validate: function () { return {}; }
        };
    }

    function dayKinds() {
        return [
            { value: '', label: AJS.I18n.getText('asset-tree.automation.anyKind') },
            { value: 'to', label: AJS.I18n.getText('asset-tree.ui.kindTo') },
            { value: 'kts', label: AJS.I18n.getText('asset-tree.ui.kindKts') },
            { value: 'verify', label: AJS.I18n.getText('asset-tree.ui.kindVerify') },
            { value: 'staff', label: AJS.I18n.getText('asset-tree.ui.kindStaff') },
            { value: 'event', label: AJS.I18n.getText('asset-tree.ui.kindEvent') },
            { value: 'update', label: AJS.I18n.getText('asset-tree.ui.kindUpdate') }
        ];
    }

    function knownKind(key) {
        var options = dayKinds();
        for (var i = 0; i < options.length; i++) {
            if (options[i].value === key) {
                return true;
            }
        }
        return false;
    }

    function kindWord(key) {
        if (!key) {
            return AJS.I18n.getText('asset-tree.automation.anyKind');
        }
        var options = dayKinds();
        for (var i = 0; i < options.length; i++) {
            if (options[i].value === key) {
                return options[i].label;
            }
        }
        return key;
    }

    function calendarDay() {
        return {
            type: plugin + ':schedule-due-trigger',
            componentType: 'TRIGGER',
            name: function () { return AJS.I18n.getText('asset-tree.automation.day'); },
            icon: function () { return '../' + plugin + ':asset-tree-automation/automation.svg'; },
            initialValue: function () { return { kind: '' }; },
            categories: [],
            labels: [],
            getRenderer: function () {
                return {
                    summaryLabel: function () { return AJS.I18n.getText('asset-tree.automation.day'); },
                    renderSummary: function (config, context, el) {
                        clear(el);
                        el.appendChild(document.createTextNode(kindWord(field(config, 'kind'))));
                    },
                    renderDetailed: function (config, context, el) {
                        clear(el);
                        paragraph(el, AJS.I18n.getText('asset-tree.automation.dayCard'));
                        label(el, AJS.I18n.getText('asset-tree.automation.dayKind'));
                        var current = field(config, 'kind');
                        var box = pick(el, config, 'kind', dayKinds());
                        if (!knownKind(current)) {
                            fillSelect(box, dayKinds(), '');
                        }
                        label(el, AJS.I18n.getText('asset-tree.automation.dayCustom'));
                        var own = document.createElement('input');
                        own.type = 'text';
                        own.value = knownKind(current) ? '' : current;
                        own.placeholder = 'to';
                        own.disabled = !!readOnly();
                        own.style.width = '100%';
                        own.style.boxSizing = 'border-box';
                        own.addEventListener('input', function () {
                            var slug = own.value.trim().toLowerCase();
                            var value = config.value || {};
                            value.kind = slug || box.value;
                            save(config, value);
                        });
                        box.addEventListener('change', function () {
                            own.value = '';
                        });
                        el.appendChild(own);
                    },
                    renderCard: function (el) {
                        clear(el);
                        el.appendChild(document.createTextNode(AJS.I18n.getText('asset-tree.automation.dayCard')));
                    }
                };
            },
            validate: function () { return {}; }
        };
    }

    function register(attempt) {
        if (registered) {
            return;
        }
        if (!window.CodeBarrel || !CodeBarrel.Automation || !CodeBarrel.Automation.registerComponent || !window.AJS || !AJS.I18n) {
            if (attempt < 20) {
                setTimeout(function () { register(attempt + 1); }, 250);
            }
            return;
        }
        registered = true;
        CodeBarrel.Automation.registerComponent(issueStatus());
        CodeBarrel.Automation.registerComponent(calendarDay());
        CodeBarrel.Automation.registerComponent(changed());
        CodeBarrel.Automation.registerComponent(linked());
        CodeBarrel.Automation.registerComponent(statusAction());
        CodeBarrel.Automation.registerComponent(linkAction());
        CodeBarrel.Automation.registerComponent(requestAction());
        CodeBarrel.Automation.registerComponent(mailAction());
    }

    register(0);
}());
