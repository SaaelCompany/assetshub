package com.assetstree.jira.service;

public final class AssetBridge {
    private static volatile AssetService service;

    private AssetBridge() {
    }

    public static AssetService service() {
        AssetService current = service;
        if (current != null) {
            return current;
        }
        synchronized (AssetBridge.class) {
            if (service == null) {
                service = new AssetServiceImpl();
                ServiceDueScheduler.ensureStarted();
            }
            return service;
        }
    }

    public static String label(String assetId) {
        AssetService current = service;
        if (current == null || assetId == null || assetId.trim().isEmpty()) {
            return assetId == null ? "" : assetId;
        }
        try {
            return current.describeAsset(assetId.trim());
        } catch (RuntimeException ex) {
            return assetId;
        }
    }
}
