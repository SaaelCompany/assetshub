package com.assetstree.jira.ao;

import net.java.ao.Entity;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.Table;

/**
 * Symmetric link between two equipment cards. The pair is stored once,
 * with the smaller id in {@code LOW_ID}, so the same two objects cannot be linked twice.
 */
@Table("AST_PEER")
public interface AssetPeerEntity extends Entity {
    @Indexed
    int getLowId();

    void setLowId(int lowId);

    @Indexed
    int getHighId();

    void setHighId(int highId);
}
