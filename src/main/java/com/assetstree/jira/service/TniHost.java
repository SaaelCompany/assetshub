package com.assetstree.jira.service;

/** Pieces of asset storage the TNI book cannot do on its own. */
public interface TniHost {
    String nextKey(String projectKey);

    int nextSort(String projectKey);

    void ensureTrash(String projectKey);

    String text(String key);

    String personName(String userKey);

    void mail(String userKey, String subject, String body);

    void announce(String projectKey, String event, int id, String objectKey, String name);

    void discard(int assetId, String actorKey);
}
