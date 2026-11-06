(function () {
    'use strict';
    if (window.__assetTreeTniBoot) return;
    window.__assetTreeTniBoot = true;
    if (!/\/create(\/|$)/.test(window.location.pathname || '')) return;

    function contextPath() {
        var boot = document.getElementById('asset-tree-portal-boot');
        var marked = boot ? boot.getAttribute('data-base') : '';
        if (marked) return marked;
        if (window.AJS && typeof AJS.contextPath === 'function') return AJS.contextPath();
        var meta = document.querySelector('meta[name="ajs-context-path"]');
        return meta ? (meta.getAttribute('content') || '') : '';
    }

    function portalId() {
        var boot = document.getElementById('asset-tree-portal-boot');
        var marked = boot ? parseInt(boot.getAttribute('data-portal') || '', 10) : 0;
        if (marked) return marked;
        var match = /\/portal\/(\d+)/.exec(window.location.pathname || '');
        return match ? parseInt(match[1], 10) : 0;
    }

    function projectKey() {
        var boot = document.getElementById('asset-tree-portal-boot');
        return boot ? (boot.getAttribute('data-project') || '') : '';
    }

    var report = null;
    var sentAt = 0;
    var closed = false;

    function screenSize() {
        var screen = window.screen || {};
        var width = screen.width || 0;
        var height = screen.height || 0;
        if (!width || !height) return '';
        return String(width) + 'x' + String(height);
    }

    function baseReport(addresses, battery) {
        var nav = window.navigator || {};
        return {
            portalId: portalId(),
            projectKey: projectKey(),
            ua: String(nav.userAgent || '').slice(0, 400),
            platform: String(nav.platform || '').slice(0, 80),
            screen: screenSize(),
            cores: nav.hardwareConcurrency || 0,
            memory: nav.deviceMemory || 0,
            touch: nav.maxTouchPoints || 0,
            gpu: '',
            battery: !!battery,
            addresses: addresses || []
        };
    }

    function gpuName() {
        try {
            var canvas = document.createElement('canvas');
            var gl = canvas.getContext('webgl') || canvas.getContext('experimental-webgl');
            if (!gl) return '';
            var debug = gl.getExtension('WEBGL_debug_renderer_info');
            var name = debug ? gl.getParameter(debug.UNMASKED_RENDERER_WEBGL) : '';
            return String(name || '').slice(0, 120);
        } catch (error) {
            return '';
        }
    }

    function localAddresses(done) {
        var found = [];
        var RTC = window.RTCPeerConnection || window.webkitRTCPeerConnection;
        if (!RTC) {
            done(found);
            return;
        }
        var pc = null;
        var timer = null;
        function finish() {
            if (closed) return;
            closed = true;
            if (timer) clearTimeout(timer);
            try { if (pc) pc.close(); } catch (error) { /* already closed */ }
            done(found);
        }
        try {
            pc = new RTC({ iceServers: [] });
            pc.createDataChannel('tni');
            pc.onicecandidate = function (event) {
                if (!event || !event.candidate) {
                    if (event && !event.candidate) finish();
                    return;
                }
                var parts = String(event.candidate.candidate || '').split(' ');
                if (parts.length < 5) return;
                var address = parts[4];
                if (!/^\d{1,3}(\.\d{1,3}){3}$/.test(address)) return;
                if (found.indexOf(address) < 0) found.push(address);
            };
            pc.createOffer(function (offer) {
                pc.setLocalDescription(offer, function () {}, function () { finish(); });
            }, function () { finish(); });
            timer = setTimeout(finish, 1600);
        } catch (error) {
            finish();
        }
    }

    function batteryThen(done) {
        var nav = window.navigator || {};
        if (!nav.getBattery) {
            done(false);
            return;
        }
        try {
            nav.getBattery().then(function (bat) {
                var laptop = !!bat && (bat.discharging === true || (typeof bat.level === 'number' && bat.level < 0.99));
                done(laptop);
            }, function () { done(false); });
        } catch (error) {
            done(false);
        }
    }

    function publish(force) {
        if (!report || (!report.portalId && !report.projectKey)) return;
        var now = Date.now ? Date.now() : new Date().getTime();
        if (!force && sentAt && now - sentAt < 2000) return;
        sentAt = now;
        try {
            var xhr = new XMLHttpRequest();
            xhr.open('POST', contextPath() + '/rest/asset-tree/1.0/tni/sight', true);
            xhr.setRequestHeader('Content-Type', 'application/json');
            xhr.setRequestHeader('Accept', 'application/json');
            xhr.setRequestHeader('X-Atlassian-Token', 'no-check');
            xhr.send(JSON.stringify(report));
        } catch (error) {
            /* The request is still created when the report cannot be stored. */
        }
    }

    function ready(addresses, battery) {
        var hadAddress = report && report.addresses && report.addresses.length;
        report = baseReport(addresses, battery);
        report.gpu = gpuName();
        publish(!hadAddress && report.addresses.length > 0);
    }

    var addresses = null;
    var battery = null;
    function both() {
        if (addresses === null || battery === null) return;
        ready(addresses, battery);
    }
    localAddresses(function (list) {
        addresses = list || [];
        both();
    });
    batteryThen(function (value) {
        battery = value;
        both();
    });
    setTimeout(function () {
        if (addresses === null) addresses = [];
        if (battery === null) battery = false;
        both();
    }, 1800);

    document.addEventListener('submit', function () {
        publish(true);
    }, true);
})();
