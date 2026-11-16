(function () {
    var plugin = 'com.assetstree.jira.asset-tree';
    var registered = false;

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

    function label(parent, caption) {
        var node = document.createElement('label');
        node.style.display = 'block';
        node.style.margin = '8px 0 4px';
        node.appendChild(document.createTextNode(caption));
        parent.appendChild(node);
    }

    function select(parent, config, options) {
        var box = document.createElement('select');
        box.disabled = !!readOnly();
        var current = field(config, 'event');
        for (var i = 0; i < options.length; i++) {
            var item = document.createElement('option');
            item.value = options[i].value;
            item.appendChild(document.createTextNode(options[i].label));
            if (item.value === current) {
                item.selected = true;
            }
            box.appendChild(item);
        }
        box.addEventListener('change', function () {
            var value = config.value || {};
            value.event = box.value;
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

    function paragraph(parent, caption) {
        var node = document.createElement('p');
        node.appendChild(document.createTextNode(caption));
        parent.appendChild(node);
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
                        var names = {
                            '': AJS.I18n.getText('asset-tree.automation.changedAny'),
                            'asset.created': AJS.I18n.getText('asset-tree.automation.changedCreated'),
                            'asset.updated': AJS.I18n.getText('asset-tree.automation.changedUpdated'),
                            'asset.deleted': AJS.I18n.getText('asset-tree.automation.changedDeleted'),
                            'asset.restored': AJS.I18n.getText('asset-tree.automation.changedRestored')
                        };
                        var chosen = field(config, 'event');
                        el.appendChild(document.createTextNode(names[chosen] || chosen));
                    },
                    renderDetailed: function (config, context, el) {
                        clear(el);
                        paragraph(el, AJS.I18n.getText('asset-tree.automation.changedCard'));
                        label(el, AJS.I18n.getText('asset-tree.automation.event'));
                        select(el, config, [
                            { value: '', label: AJS.I18n.getText('asset-tree.automation.changedAny') },
                            { value: 'asset.created', label: AJS.I18n.getText('asset-tree.automation.changedCreated') },
                            { value: 'asset.updated', label: AJS.I18n.getText('asset-tree.automation.changedUpdated') },
                            { value: 'asset.deleted', label: AJS.I18n.getText('asset-tree.automation.changedDeleted') },
                            { value: 'asset.restored', label: AJS.I18n.getText('asset-tree.automation.changedRestored') }
                        ]);
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
                        el.appendChild(document.createTextNode(field(config, 'status')));
                    },
                    renderDetailed: function (config, context, el) {
                        clear(el);
                        paragraph(el, AJS.I18n.getText('asset-tree.automation.linkedCard'));
                        label(el, AJS.I18n.getText('asset-tree.automation.linkedStatus'));
                        input(el, config, 'status', 'in_use');
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
                        el.appendChild(document.createTextNode(field(config, 'status')));
                    },
                    renderDetailed: function (config, context, el) {
                        clear(el);
                        paragraph(el, AJS.I18n.getText('asset-tree.automation.statusCard'));
                        label(el, AJS.I18n.getText('asset-tree.automation.statusField'));
                        input(el, config, 'status', 'in_use');
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
        CodeBarrel.Automation.registerComponent(changed());
        CodeBarrel.Automation.registerComponent(linked());
        CodeBarrel.Automation.registerComponent(statusAction());
    }

    register(0);
}());
