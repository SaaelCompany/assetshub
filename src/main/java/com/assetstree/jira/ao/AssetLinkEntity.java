package com.assetstree.jira.ao;

import net.java.ao.Entity;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;

/**
 * A directed link. {@code FROM_ID} is the card that owns the sentence
 * ("this depends on", "this is installed on", "this is part of").
 */
@Table("AST_LINK")
public interface AssetLinkEntity extends Entity {
    @Indexed
    int getFromId();

    void setFromId(int fromId);

    @Indexed
    int getToId();

    void setToId(int toId);

    @NotNull
    @StringLength(32)
    String getLinkType();

    void setLinkType(String linkType);

    /** Phrase shown on the target card. Empty uses the built-in reverse label. */
    @StringLength(80)
    String getInbound();

    void setInbound(String inbound);

    /** Lines of {@code key=value}. Empty when the link has no attributes. */
    @StringLength(StringLength.UNLIMITED)
    String getAttrs();

    void setAttrs(String attrs);
}
