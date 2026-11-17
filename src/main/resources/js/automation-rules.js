(function () {
    var plugin = 'com.assetstree.jira.asset-tree';
    var registered = false;
    var statusCache = {};
    var typeCache = {};
    var portalCache = {};

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

    function text(key) {
        return AJS.I18n.getText(key);
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
            { value: '', label: text('asset-tree.automation.anyStatus') },
            { value: 'in_stock', label: text('asset-tree.ui.statusStock') },
            { value: 'in_use', label: text('asset-tree.ui.statusInUse') },
            { value: 'repair', label: text('asset-tree.ui.statusRepair') },
            { value: 'reserve', label: text('asset-tree.ui.statusReserve') },
            { value: 'maintenance', label: text('asset-tree.ui.statusMaintenance') },
            { value: 'written_off', label: text('asset-tree.ui.statusWrittenOff') }
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
                        options = [{ value: '', label: text('asset-tree.automation.anyStatus') }];
                        for (var i = 0; i < rows.length; i++) {
                            options.push({
                                value: rows[i].statusKey || '',
                                label: rows[i].label || rows[i].statusKey || ''
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
            '': text('asset-tree.automation.changedAny'),
            'asset.created': text('asset-tree.automation.changedCreated'),
            'asset.updated': text('asset-tree.automation.changedUpdated'),
            'asset.status': text('asset-tree.automation.changedStatus'),
            'asset.writeoff': text('asset-tree.automation.changedWriteoff'),
            'asset.schedule': text('asset-tree.automation.changedSchedule'),
            'asset.calendar': text('asset-tree.automation.changedCalendar'),
            'asset.deleted': text('asset-tree.automation.changedDeleted'),
            'asset.restored': text('asset-tree.automation.changedRestored')
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
                { value: '', label: text('asset-tree.automation.anyDetail') },
                { value: 'pending', label: text('asset-tree.automation.detailPending') },
                { value: 'approved', label: text('asset-tree.automation.detailApproved') },
                { value: 'rejected', label: text('asset-tree.automation.detailRejected') }
            ];
        }
        if (event === 'asset.schedule') {
            return [
                { value: '', label: text('asset-tree.automation.anyDetail') },
                { value: 'created', label: text('asset-tree.automation.detailCreated') },
                { value: 'changed', label: text('asset-tree.automation.detailChanged') },
                { value: 'moved', label: text('asset-tree.automation.detailMoved') },
                { value: 'done', label: text('asset-tree.automation.detailDone') },
                { value: 'deleted', label: text('asset-tree.automation.detailDeleted') }
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
            name: function () { return text('asset-tree.automation.changed'); },
            icon: function () { return '../' + plugin + ':asset-tree-automation/automation.svg'; },
            initialValue: function () { return { event: '' }; },
            categories: [],
            labels: [],
            getRenderer: function () {
                return {
                    summaryLabel: function () { return text('asset-tree.automation.changed'); },
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
                            paragraph(el, text('asset-tree.automation.changedCard'));
                            label(el, text('asset-tree.automation.event'));
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
                            label(el, text('asset-tree.automation.statusList'));
                            statusPick(el, config, 'status', true, text('asset-tree.automation.anyStatus'));
                            var details = detailOptions(field(config, 'event'));
                            if (details) {
                                label(el, text('asset-tree.automation.detailList'));
                                pick(el, config, 'detail', details);
                            }
                        }
                        paint();
                    },
                    renderCard: function (el) {
                        clear(el);
                        el.appendChild(document.createTextNode(text('asset-tree.automation.changedCard')));
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
            name: function () { return text('asset-tree.automation.linked'); },
            icon: function () { return '../' + plugin + ':asset-tree-automation/automation.svg'; },
            initialValue: function () { return { status: '' }; },
            categories: [],
            labels: [],
            getRenderer: function () {
                return {
                    summaryLabel: function () { return text('asset-tree.automation.linked'); },
                    renderSummary: function (config, context, el) {
                        clear(el);
                        el.appendChild(document.createTextNode(statusWord(field(config, 'status'))));
                    },
                    renderDetailed: function (config, context, el) {
                        clear(el);
                        paragraph(el, text('asset-tree.automation.linkedCard'));
                        label(el, text('asset-tree.automation.linkedStatus'));
                        statusPick(el, config, 'status', true, text('asset-tree.automation.anyStatus'));
                    },
                    renderCard: function (el) {
                        clear(el);
                        el.appendChild(document.createTextNode(text('asset-tree.automation.linkedCard')));
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
            name: function () { return text('asset-tree.automation.status'); },
            icon: function () { return '../' + plugin + ':asset-tree-automation/automation.svg'; },
            initialValue: function () { return { status: '' }; },
            categories: [],
            labels: [],
            isEditable: true,
            getRenderer: function () {
                return {
                    summaryLabel: function () { return text('asset-tree.automation.status'); },
                    renderSummary: function (config, context, el) {
                        clear(el);
                        el.appendChild(document.createTextNode(statusWord(field(config, 'status'))));
                    },
                    renderDetailed: function (config, context, el) {
                        clear(el);
                        paragraph(el, text('asset-tree.automation.statusCard'));
                        label(el, text('asset-tree.automation.statusField'));
                        statusPick(el, config, 'status', false, text('asset-tree.automation.statusChoose'));
                    },
                    renderCard: function (el) {
                        clear(el);
                        el.appendChild(document.createTextNode(text('asset-tree.automation.statusCard')));
                    }
                };
            },
            validate: function (rule, config) {
                if (!field(config, 'status')) {
                    var errors = {};
                    errors.status = text('asset-tree.automation.statusRequired');
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
            name: function () { return text('asset-tree.automation.request'); },
            icon: function () { return '../' + plugin + ':asset-tree-automation/automation.svg'; },
            initialValue: function () { return { issueTypeId: '', issueTypeName: '', requestTypeId: '', requestTypeName: '' }; },
            categories: [],
            labels: [],
            isEditable: true,
            getRenderer: function () {
                return {
                    summaryLabel: function () { return text('asset-tree.automation.request'); },
                    renderSummary: function (config, context, el) {
                        clear(el);
                        var name = field(config, 'requestTypeName') || field(config, 'issueTypeName') || field(config, 'issueTypeId');
                        el.appendChild(document.createTextNode(name || text('asset-tree.automation.requestChoose')));
                    },
                    renderDetailed: function (config, context, el) {
                        clear(el);
                        paragraph(el, text('asset-tree.automation.requestCard'));
                        label(el, text('asset-tree.automation.requestPortal'));
                        portalPick(el, config);
                        label(el, text('asset-tree.automation.requestType'));
                        typePick(el, config);
                    },
                    renderCard: function (el) {
                        clear(el);
                        el.appendChild(document.createTextNode(text('asset-tree.automation.requestCard')));
                    }
                };
            },
            validate: function (rule, config) {
                if (!field(config, 'issueTypeId') && !field(config, 'requestTypeId')) {
                    var errors = {};
                    errors.issueTypeId = text('asset-tree.automation.requestTypeRequired');
                    return errors;
                }
                return {};
            }
        };
    }

    function portalPick(parent, config) {
        var box = document.createElement('select');
        box.disabled = !!readOnly();
        box.style.maxWidth = '100%';
        function show(options) {
            var list = [{ value: '', label: text('asset-tree.automation.requestPortalEmpty') }];
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
            save(config, value);
        });
        parent.appendChild(box);
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
        var placeholder = text('asset-tree.automation.requestChoose');
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
            name: function () { return text('asset-tree.automation.link'); },
            icon: function () { return '../' + plugin + ':asset-tree-automation/automation.svg'; },
            initialValue: function () { return {}; },
            categories: [],
            labels: [],
            getRenderer: function () {
                return {
                    summaryLabel: function () { return text('asset-tree.automation.link'); },
                    renderSummary: function (config, context, el) {
                        clear(el);
                        el.appendChild(document.createTextNode(text('asset-tree.automation.link')));
                    },
                    renderDetailed: function (config, context, el) {
                        clear(el);
                        paragraph(el, text('asset-tree.automation.linkCard'));
                    },
                    renderCard: function (el) {
                        clear(el);
                        el.appendChild(document.createTextNode(text('asset-tree.automation.linkCard')));
                    }
                };
            },
            validate: function () { return {}; }
        };
    }

    function recipientOptions() {
        return [
            { value: 'custodian', label: text('asset-tree.automation.mailCustodian') },
            { value: 'issued', label: text('asset-tree.automation.mailIssued') },
            { value: 'approver', label: text('asset-tree.automation.mailApprover') },
            { value: 'requester', label: text('asset-tree.automation.mailRequester') },
            { value: 'assignee', label: text('asset-tree.automation.mailAssignee') },
            { value: 'user', label: text('asset-tree.automation.mailUser') }
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
            ['key', text('asset-tree.automation.mailKey')],
            ['name', text('asset-tree.automation.mailName')],
            ['status', text('asset-tree.automation.mailStatus')],
            ['previous', text('asset-tree.automation.mailPrevious')],
            ['plan', text('asset-tree.automation.mailPlan')],
            ['due', text('asset-tree.automation.mailDue')],
            ['detail', text('asset-tree.automation.mailDetail')],
            ['note', text('asset-tree.automation.mailNote')]
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
        box.placeholder = text('asset-tree.automation.mailFind');
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
            name: function () { return text('asset-tree.automation.mail'); },
            icon: function () { return '../' + plugin + ':asset-tree-automation/automation.svg'; },
            initialValue: function () {
                return { to: 'custodian', parts: 'key,name,status,previous,plan,due,detail,note', extra: '' };
            },
            categories: [],
            labels: [],
            getRenderer: function () {
                return {
                    summaryLabel: function () { return text('asset-tree.automation.mail'); },
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
                            paragraph(el, text('asset-tree.automation.mailCard'));
                            label(el, text('asset-tree.automation.mailTo'));
                            pick(el, config, 'to', recipientOptions(), paint);
                            if ((field(config, 'to') || 'custodian') === 'user') {
                                personFinder(el, config);
                            }
                            label(el, text('asset-tree.automation.mailLines'));
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
                            label(el, text('asset-tree.automation.mailExtra'));
                            input(el, config, 'extra', text('asset-tree.automation.mailExtraHint'));
                        }
                        paint();
                    },
                    renderCard: function (el) {
                        clear(el);
                        el.appendChild(document.createTextNode(text('asset-tree.automation.mailCard')));
                    }
                };
            },
            validate: function (rule, config) {
                if (field(config, 'to') === 'user' && !field(config, 'userKey')) {
                    var errors = {};
                    errors.userKey = text('asset-tree.automation.mailRequired');
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
            var options = [{ value: '', label: text('asset-tree.automation.anyIssueStatus') }];
            if (request.status === 200) {
                try {
                    var rows = JSON.parse(request.responseText) || [];
                    for (var i = 0; i < rows.length; i++) {
                        options.push({ value: rows[i].id || '', label: rows[i].name || rows[i].id || '' });
                    }
                } catch (ignore) {
                    options = [{ value: '', label: text('asset-tree.automation.anyIssueStatus') }];
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
            name: function () { return text('asset-tree.automation.issueStatus'); },
            icon: function () { return '../' + plugin + ':asset-tree-automation/automation.svg'; },
            initialValue: function () { return { issueStatus: '' }; },
            categories: [],
            labels: [],
            getRenderer: function () {
                return {
                    summaryLabel: function () { return text('asset-tree.automation.issueStatus'); },
                    renderSummary: function (config, context, el) {
                        clear(el);
                        var name = issueStatusWord(field(config, 'issueStatus'));
                        el.appendChild(document.createTextNode(name || text('asset-tree.automation.anyIssueStatus')));
                    },
                    renderDetailed: function (config, context, el) {
                        clear(el);
                        paragraph(el, text('asset-tree.automation.issueCard'));
                        label(el, text('asset-tree.automation.issueList'));
                        var box = document.createElement('select');
                        box.disabled = !!readOnly();
                        box.style.maxWidth = '100%';
                        function show(options) {
                            fillSelect(box, options, field(config, 'issueStatus'));
                        }
                        show([{ value: '', label: text('asset-tree.automation.anyIssueStatus') }]);
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
                        el.appendChild(document.createTextNode(text('asset-tree.automation.issueCard')));
                    }
                };
            },
            validate: function () { return {}; }
        };
    }

    function dayKinds() {
        return [
            { value: '', label: text('asset-tree.automation.anyKind') },
            { value: 'to', label: text('asset-tree.ui.kindTo') },
            { value: 'kts', label: text('asset-tree.ui.kindKts') },
            { value: 'verify', label: text('asset-tree.ui.kindVerify') },
            { value: 'staff', label: text('asset-tree.ui.kindStaff') },
            { value: 'event', label: text('asset-tree.ui.kindEvent') },
            { value: 'update', label: text('asset-tree.ui.kindUpdate') }
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
            return text('asset-tree.automation.anyKind');
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
            name: function () { return text('asset-tree.automation.day'); },
            icon: function () { return '../' + plugin + ':asset-tree-automation/automation.svg'; },
            initialValue: function () { return { kind: '' }; },
            categories: [],
            labels: [],
            getRenderer: function () {
                return {
                    summaryLabel: function () { return text('asset-tree.automation.day'); },
                    renderSummary: function (config, context, el) {
                        clear(el);
                        el.appendChild(document.createTextNode(kindWord(field(config, 'kind'))));
                    },
                    renderDetailed: function (config, context, el) {
                        clear(el);
                        paragraph(el, text('asset-tree.automation.dayCard'));
                        label(el, text('asset-tree.automation.dayKind'));
                        var current = field(config, 'kind');
                        var box = pick(el, config, 'kind', dayKinds());
                        if (!knownKind(current)) {
                            fillSelect(box, dayKinds(), '');
                        }
                        label(el, text('asset-tree.automation.dayCustom'));
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
                        el.appendChild(document.createTextNode(text('asset-tree.automation.dayCard')));
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
