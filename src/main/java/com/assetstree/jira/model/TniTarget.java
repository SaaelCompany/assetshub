package com.assetstree.jira.model;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Addresses one availability check may try, in the order a reply is most likely. */
public final class TniTarget {
    private TniTarget() {
    }

    public static List<String> list(String assetName, String address, String host, String saved) {
        LinkedHashSet<String> found = new LinkedHashSet<String>();
        add(found, TniAddress.local(address));
        add(found, TniAddress.local(saved));
        add(found, TniAddress.station(host));
        add(found, TniAddress.station(TniText.shortHost(host)));
        add(found, TniAddress.station(saved));
        add(found, TniAddress.station(assetName));
        List<String> targets = new ArrayList<String>();
        for (String item : found) {
            if (targets.size() >= 4) {
                break;
            }
            targets.add(item);
        }
        return targets;
    }

    /** A DNS name when one is known, otherwise the first address. */
    public static String shown(List<String> targets) {
        if (targets == null || targets.isEmpty()) {
            return "";
        }
        for (int i = 0; i < targets.size(); i++) {
            String target = targets.get(i);
            if (target != null && !target.isEmpty() && TniAddress.local(target).isEmpty()) {
                return target;
            }
        }
        return targets.get(0);
    }

    private static void add(Set<String> found, String value) {
        if (value != null && !value.isEmpty()) {
            found.add(value);
        }
    }
}
