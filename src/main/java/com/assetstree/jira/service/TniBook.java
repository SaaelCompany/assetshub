package com.assetstree.jira.service;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.assetstree.jira.ao.AssetEntity;
import com.assetstree.jira.ao.AssetIssueLinkEntity;
import com.assetstree.jira.ao.AssetTypeEntity;
import com.assetstree.jira.ao.ProjectViewEntity;
import com.assetstree.jira.ao.TniDeviceEntity;
import com.assetstree.jira.ao.TniWatchEntity;
import com.assetstree.jira.dto.ModuleSetupDto;
import com.assetstree.jira.model.ModuleSetup;
import com.assetstree.jira.model.ProjectKeys;
import com.assetstree.jira.model.ProjectModules;
import com.assetstree.jira.model.Statuses;
import com.assetstree.jira.model.TniClock;
import com.assetstree.jira.model.TniFacts;
import com.assetstree.jira.model.TniPolicy;
import com.assetstree.jira.model.TniTarget;
import com.assetstree.jira.model.TniText;
import net.java.ao.DBParam;
import net.java.ao.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Stores a browser sighting, turns it into an asset when a request is created,
 * and retires devices that stop answering after the storage period.
 */
public final class TniBook {
    private static final Logger log = LoggerFactory.getLogger(TniBook.class);
    private static final int PING_BUDGET = 20;

    public void remember(ActiveObjects ao, final String projectKey, final String userKey, final TniFacts facts, final long now) {
        if (ao == null || facts == null || blank(projectKey) || blank(userKey)) {
            return;
        }
        final String identity = TniFacts.identity(projectKey, userKey, facts);
        ao.executeInTransaction(new TransactionCallback<Void>() {
            @Override
            public Void doInTransaction() {
                TniDeviceEntity row = findIdentity(ao, projectKey, identity);
                if (row == null) {
                    ao.create(TniDeviceEntity.class,
                            new DBParam("PROJECT_KEY", projectKey),
                            new DBParam("IDENTITY", identity),
                            new DBParam("USER_KEY", clip(userKey, 255)),
                            new DBParam("ADDRESS", clip(facts.address, 64)),
                            new DBParam("KIND", facts.kind),
                            new DBParam("SUMMARY", facts.summary()),
                            new DBParam("LABEL", ""),
                            new DBParam("SIGHT_AT", Long.valueOf(now)),
                            new DBParam("SEEN_AT", Long.valueOf(0L)),
                            new DBParam("ASSET_ID", Integer.valueOf(0)),
                            new DBParam("PROBE_FROM", Long.valueOf(0L)),
                            new DBParam("PROBE_FAILS", Integer.valueOf(0)),
                            new DBParam("LAST_PROBE", Long.valueOf(0L)),
                            new DBParam("RETIRED", Integer.valueOf(0)),
                            new DBParam("ISSUE_KEY", ""));
                    return null;
                }
                row.setUserKey(clip(userKey, 255));
                row.setAddress(clip(facts.address, 64));
                row.setKind(facts.kind);
                row.setSummary(facts.summary());
                row.setSightAt(now);
                row.save();
                return null;
            }
        });
    }

    public void attach(ActiveObjects ao, TniHost host, String projectKey, String userKey, long issueId, String issueKey,
                       Integer parentId, long now) {
        if (ao == null || host == null || blank(projectKey) || blank(userKey) || issueId <= 0) {
            return;
        }
        if (!moduleOn(ao, projectKey)) {
            return;
        }
        if (Boolean.FALSE.equals(setup(ao, projectKey).getTniBrowser())) {
            return;
        }
        final Prepared prepared = prepare(ao, projectKey, userKey, now);
        if (prepared == null) {
            return;
        }
        final Integer placeId = parentId;
        final Outcome outcome = ao.executeInTransaction(new TransactionCallback<Outcome>() {
            @Override
            public Outcome doInTransaction() {
                return writeAsset(ao, host, prepared, projectKey, userKey, issueId, issueKey, placeId, now);
            }
        });
        if (outcome != null) {
            host.announce(outcome.projectKey, outcome.event, outcome.assetId, outcome.objectKey, outcome.name);
        }
    }

    public void sweep(ActiveObjects ao, TniHost host, long now, TniReach.Probe probe) {
        if (ao == null || host == null) {
            return;
        }
        TniReach.Probe check = probe == null ? TniReach.LIVE : probe;
        retitleAll(ao, host);
        sweepWatches(ao, host, now, check);
        List<Integer> ids = ao.executeInTransaction(new TransactionCallback<List<Integer>>() {
            @Override
            public List<Integer> doInTransaction() {
                List<Integer> found = new ArrayList<Integer>();
                TniDeviceEntity[] rows = ao.find(TniDeviceEntity.class);
                if (rows == null) {
                    return found;
                }
                for (TniDeviceEntity row : rows) {
                    if (row != null && row.getAssetId() > 0 && row.getRetired() == 0 && row.getSeenAt() > 0) {
                        found.add(Integer.valueOf(row.getID()));
                    }
                }
                return found;
            }
        });
        int pings = 0;
        for (Integer id : ids) {
            if (id == null) {
                continue;
            }
            Glance look = ao.executeInTransaction(new TransactionCallback<Glance>() {
                @Override
                public Glance doInTransaction() {
                    return glance(ao, id.intValue(), now);
                }
            });
            if (look == null || look.action == TniClock.Action.WAIT) {
                continue;
            }
            if (look.action == TniClock.Action.PING) {
                if (pings >= PING_BUDGET) {
                    continue;
                }
                pings++;
                final TniReach.Report report = reachReport(check, look);
                final boolean reached = report.up();
                final int assetId = look.assetId;
                final String hostName = look.address;
                final String printed = report.output();
                Outcome outcome = ao.executeInTransaction(new TransactionCallback<Outcome>() {
                    @Override
                    public Outcome doInTransaction() {
                        return finishProbe(ao, host, id.intValue(), reached, now);
                    }
                });
                try {
                    host.notePing(assetId, "", hostName, reached, printed);
                } catch (RuntimeException ex) {
                    log.warn("TNI could not record a ping for {}", Integer.valueOf(assetId), ex);
                }
                after(ao, host, outcome);
                continue;
            }
            Outcome outcome = ao.executeInTransaction(new TransactionCallback<Outcome>() {
                @Override
                public Outcome doInTransaction() {
                    return retire(ao, host, id.intValue(), now);
                }
            });
            after(ao, host, outcome);
        }
        sweepTyped(ao, host, now, check);
    }

    public static boolean watchDue(long checkedAt, long now) {
        return checkedAt <= 0L || now - checkedAt >= TniClock.MIN_GAP;
    }

    private void sweepWatches(ActiveObjects ao, final TniHost host, final long now, final TniReach.Probe probe) {
        List<long[]> due = ao.executeInTransaction(new TransactionCallback<List<long[]>>() {
            @Override
            public List<long[]> doInTransaction() {
                List<long[]> found = new ArrayList<long[]>();
                TniWatchEntity[] rows = ao.find(TniWatchEntity.class);
                if (rows == null) {
                    return found;
                }
                for (int i = 0; i < rows.length; i++) {
                    TniWatchEntity row = rows[i];
                    if (row != null && row.getArmed() != 0 && watchDue(row.getCheckedAt(), now)) {
                        found.add(new long[]{row.getID(), row.getCheckedAt()});
                    }
                }
                Collections.sort(found, new Comparator<long[]>() {
                    @Override
                    public int compare(long[] left, long[] right) {
                        return Long.compare(left[1], right[1]);
                    }
                });
                return found;
            }
        });
        int pings = 0;
        for (int i = 0; i < due.size(); i++) {
            if (pings >= PING_BUDGET) {
                break;
            }
            final int rowId = (int) due.get(i)[0];
            Glance look = ao.executeInTransaction(new TransactionCallback<Glance>() {
                @Override
                public Glance doInTransaction() {
                    return watchGlance(ao, rowId);
                }
            });
            if (look == null || look.targets.isEmpty()) {
                continue;
            }
            pings++;
            final TniReach.Report report = reachReport(probe, look);
            final boolean reached = report.up();
            final String address = look.address;
            final int watched = look.assetId;
            ao.executeInTransaction(new TransactionCallback<Void>() {
                @Override
                public Void doInTransaction() {
                    TniWatchEntity row = ao.get(TniWatchEntity.class, rowId);
                    if (row != null && row.getArmed() != 0) {
                        row.setAddress(address);
                        row.setCheckedAt(now);
                        row.setUp(reached ? 1 : 2);
                        row.save();
                    }
                    return null;
                }
            });
            try {
                host.notePing(watched, "", address, reached, report.output());
            } catch (RuntimeException ex) {
                log.warn("TNI could not record a ping for {}", Integer.valueOf(watched), ex);
            }
        }
    }

    private void sweepTyped(ActiveObjects ao, TniHost host, final long now, final TniReach.Probe probe) {
        List<Integer> due = ao.executeInTransaction(new TransactionCallback<List<Integer>>() {
            @Override
            public List<Integer> doInTransaction() {
                return enrollTyped(ao, now);
            }
        });
        int pings = 0;
        for (int i = 0; i < due.size(); i++) {
            if (pings >= PING_BUDGET) {
                break;
            }
            final int rowId = due.get(i).intValue();
            Glance look = ao.executeInTransaction(new TransactionCallback<Glance>() {
                @Override
                public Glance doInTransaction() {
                    return glanceTyped(ao, rowId, now);
                }
            });
            if (look == null || look.action != TniClock.Action.PING) {
                if (look != null && look.action == TniClock.Action.DROP) {
                    Outcome outcome = ao.executeInTransaction(new TransactionCallback<Outcome>() {
                        @Override
                        public Outcome doInTransaction() {
                            return retireTyped(ao, host, rowId, now);
                        }
                    });
                    afterTyped(ao, host, outcome);
                }
                continue;
            }
            pings++;
            final TniReach.Report report = reachReport(probe, look);
            final boolean reached = report.up();
            final int assetId = look.assetId;
            final String hostName = look.address;
            final String printed = report.output();
            Outcome outcome = ao.executeInTransaction(new TransactionCallback<Outcome>() {
                @Override
                public Outcome doInTransaction() {
                    return finishTyped(ao, host, rowId, reached, now);
                }
            });
            try {
                host.notePing(assetId, "", hostName, reached, printed);
            } catch (RuntimeException ex) {
                log.warn("TNI could not record a ping for {}", Integer.valueOf(assetId), ex);
            }
            afterTyped(ao, host, outcome);
        }
    }

    private List<Integer> enrollTyped(ActiveObjects ao, long now) {
        List<Integer> due = new ArrayList<Integer>();
        AssetTypeEntity[] types = ao.find(AssetTypeEntity.class);
        if (types == null) {
            return due;
        }
        for (int t = 0; t < types.length; t++) {
            AssetTypeEntity type = types[t];
            if (type == null || type.isLocation() || !type.isReach() || !moduleOn(ao, type.getProjectKey())) {
                continue;
            }
            AssetEntity[] assets = ao.find(AssetEntity.class, Query.select().where("TYPE_KEY = ?", type.getTypeKey()));
            if (assets == null) {
                continue;
            }
            for (int i = 0; i < assets.length; i++) {
                AssetEntity asset = assets[i];
                if (asset == null || TniPolicy.TRASH.equals(asset.getStatus()) || portalOwns(ao, asset.getID())) {
                    continue;
                }
                TniWatchEntity row = watchByAsset(ao, asset.getID());
                if (row == null) {
                    ao.create(TniWatchEntity.class,
                            new DBParam("ASSET_ID", Integer.valueOf(asset.getID())),
                            new DBParam("PROJECT_KEY", asset.getProjectKey()),
                            new DBParam("ADDRESS", ""),
                            new DBParam("ARMED", Integer.valueOf(0)),
                            new DBParam("CHECKED_AT", Long.valueOf(0L)),
                            new DBParam("UP", Integer.valueOf(0)),
                            new DBParam("EXEMPT", Integer.valueOf(0)),
                            new DBParam("SEEN_AT", Long.valueOf(now)),
                            new DBParam("PROBE_FROM", Long.valueOf(0L)),
                            new DBParam("PROBE_FAILS", Integer.valueOf(0)),
                            new DBParam("LAST_PROBE", Long.valueOf(0L)));
                    continue;
                }
                if (row.getExempt() != 0) {
                    continue;
                }
                if (row.getSeenAt() <= 0L) {
                    row.setSeenAt(now);
                    row.save();
                    continue;
                }
                due.add(Integer.valueOf(row.getID()));
            }
        }
        return due;
    }

    private boolean portalOwns(ActiveObjects ao, int assetId) {
        TniDeviceEntity[] rows = ao.find(TniDeviceEntity.class, Query.select().where("ASSET_ID = ?", Integer.valueOf(assetId)));
        if (rows == null) {
            return false;
        }
        for (int i = 0; i < rows.length; i++) {
            if (rows[i] != null && rows[i].getRetired() == 0 && rows[i].getAssetId() == assetId) {
                return true;
            }
        }
        return false;
    }

    private TniWatchEntity watchByAsset(ActiveObjects ao, int assetId) {
        TniWatchEntity[] rows = ao.find(TniWatchEntity.class, Query.select().where("ASSET_ID = ?", Integer.valueOf(assetId)));
        if (rows == null) {
            return null;
        }
        for (int i = 0; i < rows.length; i++) {
            if (rows[i] != null) {
                return rows[i];
            }
        }
        return null;
    }

    private Glance glanceTyped(ActiveObjects ao, int rowId, long now) {
        TniWatchEntity row = ao.get(TniWatchEntity.class, rowId);
        if (row == null || row.getExempt() != 0 || row.getSeenAt() <= 0L) {
            return null;
        }
        if (!moduleOn(ao, row.getProjectKey())) {
            return null;
        }
        AssetEntity asset = ao.get(AssetEntity.class, row.getAssetId());
        if (asset == null) {
            ao.delete(row);
            return null;
        }
        if (TniPolicy.TRASH.equals(asset.getStatus())) {
            return null;
        }
        ModuleSetupDto setup = setup(ao, row.getProjectKey());
        if (Boolean.FALSE.equals(setup.getTniPing())) {
            return null;
        }
        List<String> targets = targetsOf(ao, asset, row.getAddress());
        TniClock.Mark mark = TniClock.look(row.getSeenAt(), row.getProbeFrom(), row.getProbeFails(), row.getLastProbe(),
                TniPolicy.keepDays(days(setup.getTniKeep())), TniPolicy.probeDays(days(setup.getTniProbeDays())),
                TniPolicy.probeTries(days(setup.getTniProbeTries())), !targets.isEmpty(), now);
        if (mark.action == TniClock.Action.PING && mark.probeFrom != row.getProbeFrom()) {
            row.setProbeFrom(mark.probeFrom);
            row.save();
        }
        Glance glance = new Glance();
        glance.action = mark.action;
        glance.targets = targets;
        glance.address = TniTarget.shown(targets);
        glance.assetId = asset.getID();
        return glance;
    }

    private Outcome finishTyped(ActiveObjects ao, TniHost host, int rowId, boolean up, long now) {
        TniWatchEntity row = ao.get(TniWatchEntity.class, rowId);
        if (row == null || row.getExempt() != 0) {
            return null;
        }
        row.setCheckedAt(now);
        row.setUp(up ? 1 : 2);
        row.setLastProbe(now);
        if (up) {
            row.setSeenAt(now);
            row.setProbeFrom(0L);
            row.setProbeFails(0);
            row.save();
            return null;
        }
        int fails = row.getProbeFails() + 1;
        row.setProbeFails(fails);
        if (row.getProbeFrom() == 0L) {
            row.setProbeFrom(now);
        }
        row.save();
        int tries = TniPolicy.probeTries(days(setup(ao, row.getProjectKey()).getTniProbeTries()));
        if (fails >= tries) {
            return retireTyped(ao, host, rowId, now);
        }
        return null;
    }

    private Outcome retireTyped(ActiveObjects ao, TniHost host, int rowId, long now) {
        TniWatchEntity row = ao.get(TniWatchEntity.class, rowId);
        if (row == null) {
            return null;
        }
        AssetEntity asset = ao.get(AssetEntity.class, row.getAssetId());
        if (asset == null) {
            ao.delete(row);
            return null;
        }
        ModuleSetupDto setup = setup(ao, row.getProjectKey());
        boolean delete = TniPolicy.DELETE.equals(TniPolicy.action(setup.getTniAction()));
        Outcome outcome = new Outcome();
        outcome.projectKey = row.getProjectKey();
        outcome.assetId = asset.getID();
        outcome.objectKey = asset.getObjectKey() == null ? "" : asset.getObjectKey();
        outcome.name = asset.getName() == null ? "" : asset.getName();
        outcome.userKey = asset.getCustodianKey() == null ? "" : asset.getCustodianKey();
        outcome.probeDays = TniPolicy.probeDays(days(setup.getTniProbeDays()));
        outcome.mail = true;
        outcome.delete = delete;
        outcome.rowId = row.getID();
        if (delete) {
            return outcome;
        }
        try {
            host.ensureTrash(row.getProjectKey());
        } catch (RuntimeException ex) {
            log.warn("TNI could not add the Trash status in {}", row.getProjectKey(), ex);
        }
        asset.setStatus(TniPolicy.TRASH);
        asset.setUpdated(new Date(now));
        asset.save();
        ao.delete(row);
        outcome.event = "asset.updated";
        return outcome;
    }

    private void afterTyped(ActiveObjects ao, TniHost host, Outcome outcome) {
        if (outcome == null || host == null) {
            return;
        }
        if (outcome.mail) {
            String pattern = outcome.delete ? host.text("asset-tree.ui.tniMailDelete") : host.text("asset-tree.ui.tniMailTrash");
            String subject = AutomationNote.text(host.text("asset-tree.ui.tniMailSubject"), outcome.name, "", "");
            String body = AutomationNote.text(pattern, outcome.name, outcome.objectKey, String.valueOf(outcome.probeDays));
            if (!subject.isEmpty() && !body.isEmpty()) {
                host.mail(outcome.userKey, subject, body);
            }
        }
        if (outcome.delete) {
            try {
                host.discard(outcome.assetId, outcome.userKey);
            } catch (RuntimeException ex) {
                log.warn("TNI could not remove asset {}", Integer.valueOf(outcome.assetId), ex);
                return;
            }
        } else if (!blank(outcome.event)) {
            host.announce(outcome.projectKey, outcome.event, outcome.assetId, outcome.objectKey, outcome.name);
        }
        final int rowId = outcome.rowId;
        if (ao != null && rowId > 0 && outcome.delete) {
            ao.executeInTransaction(new TransactionCallback<Void>() {
                @Override
                public Void doInTransaction() {
                    TniWatchEntity row = ao.get(TniWatchEntity.class, rowId);
                    if (row != null) {
                        ao.delete(row);
                    }
                    return null;
                }
            });
        }
    }

    private Glance watchGlance(ActiveObjects ao, int rowId) {
        TniWatchEntity row = ao.get(TniWatchEntity.class, rowId);
        if (row == null || row.getArmed() == 0) {
            return null;
        }
        if (!moduleOn(ao, row.getProjectKey())) {
            return null;
        }
        ModuleSetupDto setup = setup(ao, row.getProjectKey());
        if (Boolean.FALSE.equals(setup.getTniPing())) {
            return null;
        }
        AssetEntity asset = ao.get(AssetEntity.class, row.getAssetId());
        if (asset == null) {
            ao.delete(row);
            return null;
        }
        Glance glance = new Glance();
        glance.targets = targetsOf(ao, asset, row.getAddress());
        glance.address = TniTarget.shown(glance.targets);
        glance.assetId = asset.getID();
        return glance;
    }

    private void after(ActiveObjects ao, TniHost host, Outcome outcome) {
        if (outcome == null || host == null) {
            return;
        }
        if (outcome.mail) {
            String pattern = outcome.delete ? host.text("asset-tree.ui.tniMailDelete") : host.text("asset-tree.ui.tniMailTrash");
            String subject = AutomationNote.text(host.text("asset-tree.ui.tniMailSubject"), outcome.name, "", "");
            String body = AutomationNote.text(pattern, outcome.name, outcome.objectKey, String.valueOf(outcome.probeDays));
            if (!subject.isEmpty() && !body.isEmpty()) {
                host.mail(outcome.userKey, subject, body);
            }
        }
        if (outcome.delete) {
            try {
                host.discard(outcome.assetId, outcome.userKey);
            } catch (RuntimeException ex) {
                log.warn("TNI could not remove asset {}", Integer.valueOf(outcome.assetId), ex);
                return;
            }
            final int rowId = outcome.rowId;
            if (ao != null && rowId > 0) {
                ao.executeInTransaction(new TransactionCallback<Void>() {
                    @Override
                    public Void doInTransaction() {
                        TniDeviceEntity row = ao.get(TniDeviceEntity.class, rowId);
                        if (row != null) {
                            ao.delete(row);
                        }
                        return null;
                    }
                });
            }
        } else if (!blank(outcome.event)) {
            host.announce(outcome.projectKey, outcome.event, outcome.assetId, outcome.objectKey, outcome.name);
        }
    }

    private Glance glance(ActiveObjects ao, int id, long now) {
        TniDeviceEntity row = ao.get(TniDeviceEntity.class, id);
        if (row == null || row.getRetired() != 0 || row.getAssetId() <= 0 || row.getSeenAt() <= 0) {
            return null;
        }
        if (!moduleOn(ao, row.getProjectKey())) {
            return null;
        }
        AssetEntity asset = ao.get(AssetEntity.class, row.getAssetId());
        if (asset == null) {
            ao.delete(row);
            return null;
        }
        ModuleSetupDto setup = setup(ao, row.getProjectKey());
        if (Boolean.FALSE.equals(setup.getTniPing())) {
            return null;
        }
        TniFacts facts = TniFacts.parse(row.getSummary());
        List<String> targets = TniTarget.list(asset.getName(), row.getAddress(), facts.host, "");
        TniClock.Mark mark = TniClock.look(row.getSeenAt(), row.getProbeFrom(), row.getProbeFails(), row.getLastProbe(),
                TniPolicy.keepDays(days(setup.getTniKeep())), TniPolicy.probeDays(days(setup.getTniProbeDays())),
                TniPolicy.probeTries(days(setup.getTniProbeTries())), !targets.isEmpty(), now);
        if (mark.action == TniClock.Action.PING && mark.probeFrom != row.getProbeFrom()) {
            row.setProbeFrom(mark.probeFrom);
            row.save();
        }
        Glance glance = new Glance();
        glance.action = mark.action;
        glance.assetId = asset.getID();
        glance.targets = targets;
        glance.address = TniTarget.shown(targets);
        return glance;
    }

    private Outcome finishProbe(ActiveObjects ao, TniHost host, int id, boolean up, long now) {
        TniDeviceEntity row = ao.get(TniDeviceEntity.class, id);
        if (row == null || row.getRetired() != 0) {
            return null;
        }
        if (up) {
            row.setSeenAt(now);
            row.setProbeFrom(0L);
            row.setProbeFails(0);
            row.setLastProbe(now);
            row.save();
            return null;
        }
        int fails = row.getProbeFails() + 1;
        row.setProbeFails(fails);
        row.setLastProbe(now);
        if (row.getProbeFrom() == 0L) {
            row.setProbeFrom(now);
        }
        row.save();
        int tries = TniPolicy.probeTries(days(setup(ao, row.getProjectKey()).getTniProbeTries()));
        if (fails >= tries) {
            return retire(ao, host, id, now);
        }
        return null;
    }

    private Outcome retire(ActiveObjects ao, TniHost host, int id, long now) {
        TniDeviceEntity row = ao.get(TniDeviceEntity.class, id);
        if (row == null || row.getRetired() != 0) {
            return null;
        }
        AssetEntity asset = ao.get(AssetEntity.class, row.getAssetId());
        if (asset == null) {
            ao.delete(row);
            return null;
        }
        ModuleSetupDto setup = setup(ao, row.getProjectKey());
        boolean delete = TniPolicy.DELETE.equals(TniPolicy.action(setup.getTniAction()));
        Outcome outcome = new Outcome();
        outcome.projectKey = row.getProjectKey();
        outcome.assetId = asset.getID();
        outcome.objectKey = asset.getObjectKey() == null ? "" : asset.getObjectKey();
        outcome.name = asset.getName() == null ? "" : asset.getName();
        outcome.userKey = blank(asset.getCustodianKey()) ? row.getUserKey() : asset.getCustodianKey();
        outcome.probeDays = TniPolicy.probeDays(days(setup.getTniProbeDays()));
        outcome.mail = true;
        outcome.delete = delete;
        outcome.rowId = row.getID();
        if (delete) {
            return outcome;
        }
        try {
            host.ensureTrash(row.getProjectKey());
        } catch (RuntimeException ex) {
            log.warn("TNI could not add the Trash status in {}", row.getProjectKey(), ex);
        }
        asset.setStatus(TniPolicy.TRASH);
        asset.setUpdated(new Date(now));
        asset.save();
        row.setRetired(1);
        row.save();
        outcome.event = "asset.updated";
        return outcome;
    }

    private Outcome writeAsset(ActiveObjects ao, TniHost host, Prepared prepared, String projectKey, String userKey,
                               long issueId, String issueKey, Integer parentId, long now) {
        TniDeviceEntity row = ao.get(TniDeviceEntity.class, prepared.rowId);
        if (row == null) {
            return null;
        }
        ModuleSetupDto setup = setup(ao, projectKey);
        TniFacts facts = TniFacts.parse(row.getSummary());
        String typeKey = TniPolicy.typeFor(setup, facts.kind);
        AssetTypeEntity type = equipmentType(ao, projectKey, typeKey);
        if (type == null) {
            log.info("TNI has no equipment type for {} in {}", facts.kind, projectKey);
            return null;
        }
        String kindLabel = kindLabel(host, facts.kind);
        String person = host.personName(userKey);
        String name = TniText.name(kindLabel, person, facts.address, facts.host);
        String description = TniText.describe(facts, host.text("asset-tree.ui.tniIntro"),
                host.text("asset-tree.ui.tniSeen"), host.text("asset-tree.ui.tniHost"),
                host.text("asset-tree.ui.tniAddress"), host.text("asset-tree.ui.tniSystem"),
                host.text("asset-tree.ui.tniBrowser"), host.text("asset-tree.ui.tniScreen"),
                host.text("asset-tree.ui.tniCores"), host.text("asset-tree.ui.tniMemory"),
                host.text("asset-tree.ui.tniVideo"), host.text("asset-tree.ui.tniRequest"), issueKey);
        AssetEntity asset = row.getAssetId() > 0 ? ao.get(AssetEntity.class, row.getAssetId()) : null;
        if (asset == null) {
            asset = findExternal(ao, projectKey, row.getIdentity());
        }
        boolean created = asset == null;
        Integer place = placeParent(ao, projectKey, parentId);
        if (created) {
            String objectKey;
            try {
                objectKey = host.nextKey(projectKey);
            } catch (RuntimeException ex) {
                log.warn("TNI could not number an asset in {}", projectKey, ex);
                return null;
            }
            Date stamp = new Date(now);
            asset = ao.create(AssetEntity.class,
                    new DBParam("NAME", name),
                    new DBParam("OBJECT_KEY", objectKey),
                    new DBParam("DESCRIPTION", clip(description, 10000)),
                    new DBParam("TYPE_KEY", type.getTypeKey()),
                    new DBParam("STATUS", Statuses.IN_USE),
                    new DBParam("SORT_ORDER", Integer.valueOf(host.nextSort(projectKey))),
                    new DBParam("CREATED", stamp),
                    new DBParam("UPDATED", stamp),
                    new DBParam("CREATED_BY", clip(userKey, 255)),
                    new DBParam("UPDATED_BY", clip(userKey, 255)),
                    new DBParam("PROJECT_KEY", projectKey));
            asset.setCustodianKey(clip(userKey, 255));
            asset.setIssuedKey(clip(userKey, 255));
            asset.setExternalKey(external(row.getIdentity()));
            if (place != null) {
                asset.setParentId(place);
            } else {
                String site = TniSeat.move(ao, asset, setup.getTniSites(), facts.host);
                if (!site.isEmpty()) {
                    host.note(asset.getID(), userKey, "move", "", site);
                }
            }
            asset.save();
        } else {
            String chosen = TniText.preferred(asset.getName(), row.getLabel(), kindLabel, person, facts.address, facts.host);
            if (!chosen.equals(asset.getName() == null ? "" : asset.getName())) {
                asset.setName(chosen);
            }
            name = chosen;
            if (TniText.owned(asset.getDescription())) {
                asset.setDescription(clip(description, 10000));
            }
            if (!type.getTypeKey().equals(asset.getTypeKey())) {
                asset.setTypeKey(type.getTypeKey());
            }
            if (blank(asset.getCustodianKey())) {
                asset.setCustodianKey(clip(userKey, 255));
            }
            if (blank(asset.getIssuedKey())) {
                asset.setIssuedKey(clip(userKey, 255));
            }
            if (blank(asset.getExternalKey())) {
                asset.setExternalKey(external(row.getIdentity()));
            }
            if (TniPolicy.TRASH.equals(asset.getStatus()) || row.getRetired() == 1) {
                asset.setStatus(Statuses.IN_USE);
            }
            if (place != null && (asset.getParentId() == null || asset.getParentId().intValue() <= 0)) {
                asset.setParentId(place);
            } else if (asset.getParentId() == null || asset.getParentId().intValue() <= 0) {
                String site = TniSeat.move(ao, asset, setup.getTniSites(), facts.host);
                if (!site.isEmpty()) {
                    host.note(asset.getID(), userKey, "move", "", site);
                }
            }
            asset.setUpdated(new Date(now));
            asset.setUpdatedBy(clip(userKey, 255));
            asset.save();
        }
        link(ao, asset, issueId);
        row.setAssetId(asset.getID());
        row.setUserKey(clip(userKey, 255));
        row.setIssueKey(clip(issueKey, 64));
        String station = TniText.shortHost(facts.host);
        if (created || (!station.isEmpty() && station.equals(name))) {
            row.setLabel(clip(name, 255));
        }
        row.setSeenAt(now);
        row.setProbeFrom(0L);
        row.setProbeFails(0);
        row.setLastProbe(0L);
        row.setRetired(0);
        row.setSightAt(now);
        row.save();
        Outcome outcome = new Outcome();
        outcome.projectKey = projectKey;
        outcome.event = created ? "asset.created" : "asset.updated";
        outcome.assetId = asset.getID();
        outcome.objectKey = asset.getObjectKey() == null ? "" : asset.getObjectKey();
        outcome.name = asset.getName() == null ? name : asset.getName();
        return outcome;
    }

    private void link(ActiveObjects ao, AssetEntity asset, long issueId) {
        AssetIssueLinkEntity[] existing = ao.find(AssetIssueLinkEntity.class,
                Query.select().where("ASSET_ID = ? AND ISSUE_ID = ?", Integer.valueOf(asset.getID()), Long.valueOf(issueId)));
        if (existing != null && existing.length > 0) {
            return;
        }
        String before = asset.getStatus() == null ? Statuses.IN_USE : asset.getStatus();
        ao.create(AssetIssueLinkEntity.class,
                new DBParam("ASSET_ID", Integer.valueOf(asset.getID())),
                new DBParam("ISSUE_ID", Long.valueOf(issueId)),
                new DBParam("STATUS_BEFORE", before));
    }

    private Prepared prepare(ActiveObjects ao, String projectKey, String userKey, long now) {
        return ao.executeInTransaction(new TransactionCallback<Prepared>() {
            @Override
            public Prepared doInTransaction() {
                TniDeviceEntity[] rows = ao.find(TniDeviceEntity.class,
                        Query.select().where("PROJECT_KEY = ? AND USER_KEY = ?", projectKey, userKey));
                TniDeviceEntity latest = null;
                if (rows != null) {
                    for (TniDeviceEntity row : rows) {
                        if (row == null || now - row.getSightAt() > TniClock.SIGHT_MS) {
                            continue;
                        }
                        if (latest == null || row.getSightAt() > latest.getSightAt()) {
                            latest = row;
                        }
                    }
                }
                if (latest == null) {
                    return null;
                }
                Prepared prepared = new Prepared();
                prepared.rowId = latest.getID();
                return prepared;
            }
        });
    }

    private Integer placeParent(ActiveObjects ao, String projectKey, Integer parentId) {
        if (parentId == null || parentId.intValue() <= 0) {
            return null;
        }
        AssetEntity parent = ao.get(AssetEntity.class, parentId.intValue());
        if (parent == null || !ProjectKeys.same(parent.getProjectKey(), projectKey)) {
            return null;
        }
        AssetTypeEntity[] types = ao.find(AssetTypeEntity.class, Query.select().where("TYPE_KEY = ?", parent.getTypeKey()));
        if (types == null || types.length == 0 || types[0] == null || !types[0].isLocation()) {
            return null;
        }
        return Integer.valueOf(parent.getID());
    }

    private AssetEntity findExternal(ActiveObjects ao, String projectKey, String identity) {
        String key = external(identity);
        AssetEntity[] rows = ao.find(AssetEntity.class,
                Query.select().where("PROJECT_KEY = ? AND EXTERNAL_KEY = ?", projectKey, key));
        if (rows == null || rows.length == 0) {
            return null;
        }
        return rows[0];
    }

    private AssetTypeEntity equipmentType(ActiveObjects ao, String projectKey, String typeKey) {
        if (typeKey == null || typeKey.isEmpty()) {
            return null;
        }
        AssetTypeEntity[] rows = ao.find(AssetTypeEntity.class, Query.select().where("TYPE_KEY = ?", typeKey));
        if (rows == null || rows.length == 0 || rows[0] == null) {
            return null;
        }
        AssetTypeEntity type = rows[0];
        if (type.isLocation() || !ProjectKeys.same(type.getProjectKey(), projectKey)) {
            return null;
        }
        return type;
    }

    private TniDeviceEntity findIdentity(ActiveObjects ao, String projectKey, String identity) {
        TniDeviceEntity[] rows = ao.find(TniDeviceEntity.class,
                Query.select().where("PROJECT_KEY = ? AND IDENTITY = ?", projectKey, identity));
        if (rows == null || rows.length == 0) {
            return null;
        }
        return rows[0];
    }

    private boolean moduleOn(ActiveObjects ao, String projectKey) {
        ProjectViewEntity[] rows = ao.find(ProjectViewEntity.class, Query.select().where("PROJECT_KEY = ?", projectKey));
        if (rows == null || rows.length == 0 || rows[0] == null) {
            return false;
        }
        return ProjectModules.has(rows[0].getModuleList(), ProjectModules.TNI);
    }

    private ModuleSetupDto setup(ActiveObjects ao, String projectKey) {
        ProjectViewEntity[] rows = ao.find(ProjectViewEntity.class, Query.select().where("PROJECT_KEY = ?", projectKey));
        String stored = rows == null || rows.length == 0 || rows[0] == null ? "" : rows[0].getOptionList();
        return ModuleSetup.read(stored);
    }

    private static String kindLabel(TniHost host, String kind) {
        String key = TniPolicy.LAPTOP.equals(kind) ? "asset-tree.ui.tniKindLaptop"
                : TniPolicy.PHONE.equals(kind) ? "asset-tree.ui.tniKindPhone"
                : TniPolicy.TABLET.equals(kind) ? "asset-tree.ui.tniKindTablet"
                : "asset-tree.ui.tniKindComputer";
        String label = host.text(key);
        return label.isEmpty() ? kind : label;
    }

    private static String external(String identity) {
        return "tni:" + (identity == null ? "" : identity);
    }

    private static int days(Integer raw) {
        return raw == null ? 0 : raw.intValue();
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String clip(String value, int max) {
        String text = value == null ? "" : value.trim();
        return text.length() <= max ? text : text.substring(0, max);
    }

    private static final class Prepared {
        private int rowId;
    }

    public String retitle(ActiveObjects ao, final TniHost host, final int assetId) {
        if (ao == null || host == null || assetId <= 0) {
            return "";
        }
        String titled = ao.executeInTransaction(new TransactionCallback<String>() {
            @Override
            public String doInTransaction() {
                return retitleRow(ao, host, assetId);
            }
        });
        return titled == null ? "" : titled;
    }

    private void retitleAll(ActiveObjects ao, final TniHost host) {
        List<Integer> ids = ao.executeInTransaction(new TransactionCallback<List<Integer>>() {
            @Override
            public List<Integer> doInTransaction() {
                List<Integer> found = new ArrayList<Integer>();
                TniDeviceEntity[] rows = ao.find(TniDeviceEntity.class);
                if (rows == null) {
                    return found;
                }
                for (int i = 0; i < rows.length; i++) {
                    TniDeviceEntity row = rows[i];
                    if (row != null && row.getAssetId() > 0 && row.getRetired() == 0) {
                        found.add(Integer.valueOf(row.getAssetId()));
                    }
                }
                return found;
            }
        });
        for (int i = 0; i < ids.size(); i++) {
            try {
                retitle(ao, host, ids.get(i).intValue());
            } catch (RuntimeException ex) {
                log.warn("TNI could not rename asset {}", ids.get(i), ex);
            }
        }
    }

    private String retitleRow(ActiveObjects ao, TniHost host, int assetId) {
        TniDeviceEntity row = deviceByAsset(ao, assetId);
        AssetEntity asset = ao.get(AssetEntity.class, assetId);
        if (row == null || asset == null) {
            return asset == null || asset.getName() == null ? "" : asset.getName();
        }
        TniFacts facts = TniFacts.parse(row.getSummary());
        String kindLabel = kindLabel(host, facts.kind);
        String person = host.personName(blank(row.getUserKey()) ? asset.getCustodianKey() : row.getUserKey());
        String next = TniText.preferred(asset.getName(), row.getLabel(), kindLabel, person, facts.address, facts.host);
        String current = asset.getName() == null ? "" : asset.getName();
        if (!next.equals(current)) {
            asset.setName(next);
            asset.setUpdated(new Date());
            asset.save();
        }
        String station = TniText.shortHost(facts.host);
        if (!station.isEmpty() && station.equals(next) && !station.equals(row.getLabel() == null ? "" : row.getLabel())) {
            row.setLabel(clip(next, 255));
            row.save();
        }
        return next;
    }

    private TniReach.Report reachReport(TniReach.Probe check, Glance look) {
        List<String> targets = look == null || look.targets == null || look.targets.isEmpty()
                ? (look == null || look.address == null || look.address.isEmpty()
                ? Collections.<String>emptyList() : Collections.singletonList(look.address))
                : look.targets;
        if (check == TniReach.LIVE) {
            return TniReach.askAll(targets);
        }
        for (int i = 0; i < targets.size(); i++) {
            String address = targets.get(i);
            try {
                if (check != null && check.up(address)) {
                    return TniReach.Report.up("");
                }
            } catch (RuntimeException ex) {
                log.warn("TNI could not reach {}", address, ex);
            }
        }
        return TniReach.Report.down("");
    }

    private List<String> targetsOf(ActiveObjects ao, AssetEntity asset, String saved) {
        String ip = "";
        String host = "";
        TniDeviceEntity device = deviceByAsset(ao, asset.getID());
        if (device != null) {
            ip = device.getAddress() == null ? "" : device.getAddress();
            host = TniFacts.parse(device.getSummary()).host;
        }
        return TniTarget.list(asset.getName(), ip, host, saved);
    }

    private TniDeviceEntity deviceByAsset(ActiveObjects ao, int assetId) {
        TniDeviceEntity[] rows = ao.find(TniDeviceEntity.class, Query.select().where("ASSET_ID = ?", Integer.valueOf(assetId)));
        if (rows == null) {
            return null;
        }
        TniDeviceEntity fallback = null;
        for (int i = 0; i < rows.length; i++) {
            TniDeviceEntity row = rows[i];
            if (row == null) {
                continue;
            }
            if (row.getRetired() != 0) {
                if (fallback == null) {
                    fallback = row;
                }
                continue;
            }
            if (!TniFacts.parse(row.getSummary()).host.isEmpty()) {
                return row;
            }
            fallback = row;
        }
        return fallback;
    }

    private static final class Glance {
        private TniClock.Action action = TniClock.Action.WAIT;
        private String address = "";
        private List<String> targets = new ArrayList<String>();
        private int assetId;
    }

    private static final class Outcome {
        private String projectKey = "";
        private String event = "";
        private int assetId;
        private String objectKey = "";
        private String name = "";
        private String userKey = "";
        private int probeDays;
        private boolean mail;
        private boolean delete;
        private int rowId;
    }
}
