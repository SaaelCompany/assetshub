(function () {
    'use strict';

    function contextPath() {
        if (window.AJS && typeof AJS.contextPath === 'function') {
            return AJS.contextPath();
        }
        var meta = document.querySelector('meta[name="ajs-context-path"]');
        return meta ? (meta.getAttribute('content') || '') : '';
    }

    function restBase() {
        return contextPath() + '/rest/asset-tree/1.0';
    }

    function ajax(method, path, body, callback) {
        var xhr = new XMLHttpRequest();
        xhr.open(method, restBase() + path, true);
        xhr.setRequestHeader('Content-Type', 'application/json');
        xhr.setRequestHeader('Accept', 'application/json');
        xhr.setRequestHeader('X-Atlassian-Token', 'no-check');
        xhr.onreadystatechange = function () {
            if (xhr.readyState !== 4) return;
            var payload = null;
            if (xhr.responseText) {
                try { payload = JSON.parse(xhr.responseText); } catch (error) { payload = { message: xhr.responseText }; }
            }
            callback(xhr.status, payload);
        };
        xhr.send(body ? JSON.stringify(body) : null);
    }

    function el(tag, className, text) {
        var node = document.createElement(tag);
        if (className) node.className = className;
        if (text !== undefined && text !== null) node.textContent = text;
        return node;
    }

    function t(i18n, key) {
        return (i18n && i18n[key]) || key;
    }

    function numericId(value) {
        var text = value === undefined || value === null ? '' : String(value).replace(/^\s+|\s+$/g, '');
        return /^[0-9]+$/.test(text) ? text : '';
    }

    /* The issue viewer inserts this panel after the first scripts have run, and the
       velocity context sometimes has no $issue. Either source is enough. */
    function pageIssueId() {
        try {
            if (window.JIRA && JIRA.Issue && typeof JIRA.Issue.getIssueId === 'function') {
                var id = numericId(JIRA.Issue.getIssueId());
                if (id) return id;
            }
        } catch (error) { /* the issue module is not on this page */ }
        var meta = document.querySelector('meta[name="ajs-issue-id"]');
        return meta ? numericId(meta.getAttribute('content')) : '';
    }

    function pageIssueKey() {
        try {
            if (window.JIRA && JIRA.Issue && typeof JIRA.Issue.getIssueKey === 'function') {
                var key = JIRA.Issue.getIssueKey();
                if (key) return String(key);
            }
        } catch (error) { /* the issue module is not on this page */ }
        var meta = document.querySelector('meta[name="ajs-issue-key"]');
        return meta ? (meta.getAttribute('content') || '') : '';
    }

    function hasClass(node, name) {
        return !!(node && node.className && (' ' + node.className + ' ').indexOf(' ' + name + ' ') !== -1);
    }

    function rootsOf(context) {
        if (!context || context === document) return [document];
        if (context.jquery && typeof context.get === 'function') return context.get();
        if (context.nodeType) return [context];
        if (context.length && context[0] && context[0].nodeType) {
            var nodes = [];
            for (var i = 0; i < context.length; i++) nodes.push(context[i]);
            return nodes;
        }
        return [document];
    }

    function findPanels(context) {
        var roots = rootsOf(context);
        var found = [];
        for (var i = 0; i < roots.length; i++) {
            var root = roots[i];
            if (!root || !root.querySelectorAll) continue;
            if (hasClass(root, 'asset-tree-panel')) found.push(root);
            var nested = root.querySelectorAll('.asset-tree-panel');
            for (var j = 0; j < nested.length; j++) {
                if (found.indexOf(nested[j]) === -1) found.push(nested[j]);
            }
        }
        return found;
    }

    function boot(context) {
        var panels = findPanels(context);
        for (var i = 0; i < panels.length; i++) mount(panels[i]);
    }

    function mount(panel) {
        if (!panel || panel.getAttribute('data-ready') === '1') return;
        var issueId = numericId(panel.getAttribute('data-issue-id')) || pageIssueId();
        if (!issueId) return;
        panel.setAttribute('data-issue-id', issueId);
        if (!panel.getAttribute('data-issue-key')) panel.setAttribute('data-issue-key', pageIssueKey());
        panel.setAttribute('data-ready', '1');
        var projectKey = panel.getAttribute('data-project-key') || '';
        ajax('GET', '/meta', null, function (status, meta) {
            var i18n = status === 200 && meta ? meta.i18n : {};
            ajax('GET', '/issues/' + issueId + '/context', null, function (contextStatus, context) {
                var canEdit = contextStatus === 200 && context ? !!context.canEdit : false;
                if (contextStatus === 200 && context && context.projectKey) projectKey = context.projectKey;
                render(panel, issueId, i18n, canEdit, projectKey);
                loadLinks(panel, issueId, i18n, canEdit, projectKey);
            });
        });
    }

    function render(panel, issueId, i18n, canEdit, projectKey) {
        var list = panel.querySelector('.asset-tree-panel-list');
        if (!panel.querySelector('.asset-tree-panel-hint')) {
            panel.insertBefore(el('p', 'asset-tree-panel-hint', t(i18n, 'panelHint')), panel.firstChild);
        }
        if (canEdit && !panel.querySelector('input')) {
            var find = el('div', 'asset-tree-panel-find');
            var input = el('input');
            input.type = 'search';
            input.placeholder = t(i18n, 'panelSearch');
            var results = el('ul', 'asset-tree-panel-results');
            var timer = null;
            var searchToken = 0;
            input.addEventListener('input', function () {
                clearTimeout(timer);
                var query = input.value.replace(/^\s+|\s+$/g, '');
                timer = setTimeout(function () {
                    var seq = ++searchToken;
                    if (query.length < 1) {
                        results.innerHTML = '';
                        return;
                    }
                    ajax('GET', '/issues/' + issueId + '/search?q=' + encodeURIComponent(query), null, function (status, payload) {
                        if (seq !== searchToken || input.value.replace(/^\s+|\s+$/g, '') !== query) return;
                        results.innerHTML = '';
                        if (status !== 200) return;
                        (payload || []).slice(0, 8).forEach(function (asset) {
                            var item = el('li');
                            var hit = el('button', 'asset-tree-panel-hit');
                            hit.appendChild(el('span', null, asset.name));
                            hit.appendChild(el('span', 'asset-tree-panel-key', asset.objectKey));
                            hit.addEventListener('click', function () {
                                ajax('POST', '/issues/' + issueId + '/assets', { assetId: asset.id }, function (linkStatus) {
                                    if (linkStatus >= 200 && linkStatus < 300) {
                                        input.value = '';
                                        results.innerHTML = '';
                                        loadLinks(panel, issueId, i18n, canEdit, projectKey);
                                    }
                                });
                            });
                            item.appendChild(hit);
                            results.appendChild(item);
                        });
                    });
                }, 220);
            });
            find.appendChild(input);
            var before = list || panel.querySelector('.asset-tree-panel-empty') || panel.querySelector('.asset-tree-panel-open');
            if (before) {
                panel.insertBefore(find, before);
                panel.insertBefore(results, before);
            } else {
                panel.appendChild(find);
                panel.appendChild(results);
            }
        }
        ensureList(panel);
        if (!panel.querySelector('.asset-tree-panel-open')) {
            var open = el('a', 'asset-tree-panel-open', t(i18n, 'openTree'));
            open.href = contextPath() + '/plugins/servlet/asset-tree' + (projectKey ? '?project=' + encodeURIComponent(projectKey) : '');
            panel.appendChild(open);
        }
    }

    function ensureList(panel) {
        var list = panel.querySelector('.asset-tree-panel-list');
        if (list) return list;
        list = el('ul', 'asset-tree-panel-list');
        list.id = 'asset-tree-panel-list';
        var empty = panel.querySelector('.asset-tree-panel-empty');
        var open = panel.querySelector('.asset-tree-panel-open');
        if (empty) {
            panel.insertBefore(list, empty);
            panel.removeChild(empty);
        } else if (open) {
            panel.insertBefore(list, open);
        } else {
            panel.appendChild(list);
        }
        return list;
    }

    function loadLinks(panel, issueId, i18n, canEdit, projectKey) {
        var list = ensureList(panel);
        var token = (panel._linksToken || 0) + 1;
        panel._linksToken = token;
        ajax('GET', '/issues/' + issueId + '/assets', null, function (status, payload) {
            if (panel._linksToken !== token || !panel.parentNode) return;
            list = ensureList(panel);
            list.innerHTML = '';
            if (status !== 200) {
                list.appendChild(el('li', 'asset-tree-panel-error', (payload && payload.message) || t(i18n, 'errorTitle')));
                return;
            }
            var assets = payload || [];
            if (!assets.length) {
                list.appendChild(el('li', 'asset-tree-panel-empty', t(i18n, 'panelEmpty')));
                return;
            }
            assets.forEach(function (asset) {
                var item = el('li');
                var line = el('div', 'asset-tree-panel-line');
                var link = el('a', null, asset.objectKey + '  ' + asset.name + (asset.location ? ' · ' + asset.location : ''));
                link.href = contextPath() + '/plugins/servlet/asset-tree?project=' + encodeURIComponent(asset.projectKey || projectKey || '') + '#' + asset.id;
                line.appendChild(link);
                if (canEdit) {
                    var remove = el('button', null, '×');
                    remove.setAttribute('aria-label', t(i18n, 'unlink'));
                    remove.addEventListener('click', function () {
                        ajax('DELETE', '/assets/' + asset.id + '/issues/' + issueId, null, function () {
                            loadLinks(panel, issueId, i18n, canEdit, projectKey);
                        });
                    });
                    line.appendChild(remove);
                }
                item.appendChild(line);
                item.appendChild(assetCard(asset, i18n));
                list.appendChild(item);
            });
        });
    }

    function assetCard(asset, i18n) {
        var card = el('div', 'asset-tree-panel-card');
        card.appendChild(el('strong', null, asset.name || ''));
        (asset.attributes || []).forEach(function (attr) {
            if (!attr || !attr.name || attr.showOnIssue === false) return;
            card.appendChild(cardRow(attr.name, attr.displayValue || attr.value || ''));
        });
        var person = asset.custodian && asset.custodian.displayName ? asset.custodian.displayName : '';
        if (person) card.appendChild(cardRow(t(i18n, 'custodian'), person));
        return card;
    }

    function cardRow(label, value) {
        var row = el('div', 'asset-tree-panel-card-row');
        row.appendChild(el('span', null, label));
        row.appendChild(el('span', null, value));
        return row;
    }

    var listening = false;

    function listen() {
        if (listening) return;
        if (!(window.JIRA && typeof JIRA.bind === 'function' && JIRA.Events && JIRA.Events.NEW_CONTENT_ADDED)) return;
        listening = true;
        JIRA.bind(JIRA.Events.NEW_CONTENT_ADDED, function (event, context) {
            boot(context || document);
        });
    }

    function watch() {
        var polls = 0;
        var timer = setInterval(function () {
            polls++;
            listen();
            boot(document);
            var panels = document.querySelectorAll('.asset-tree-panel');
            var pending = false;
            for (var i = 0; i < panels.length; i++) {
                if (panels[i].getAttribute('data-ready') !== '1') pending = true;
            }
            if (polls >= 24 || (panels.length && !pending)) clearInterval(timer);
        }, 250);
    }

    function start() {
        listen();
        var run = function () {
            listen();
            boot(document);
            watch();
        };
        if (window.AJS && AJS.toInit) {
            AJS.toInit(run);
        } else if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', run);
        } else {
            run();
        }
    }

    start();
})();
