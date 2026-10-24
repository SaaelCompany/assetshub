package com.assetstree.jira.ao;

import net.java.ao.Entity;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;
import net.java.ao.schema.Unique;

@Table("AST_FIELD")
public interface AssetFieldEntity extends Entity {
    @NotNull
    @Indexed
    @StringLength(64)
    String getTypeKey();

    void setTypeKey(String typeKey);

    @NotNull
    @StringLength(40)
    String getFieldKey();

    void setFieldKey(String fieldKey);

    @NotNull
    @Unique
    @StringLength(120)
    String getScopeKey();

    void setScopeKey(String scopeKey);

    @NotNull
    @StringLength(255)
    String getLabel();

    void setLabel(String label);

    @NotNull
    @StringLength(16)
    String getKind();

    void setKind(String kind);

    boolean isRequired();

    void setRequired(boolean required);

    boolean isIssueHidden();

    void setIssueHidden(boolean issueHidden);

    int getPosition();

    void setPosition(int position);

    @StringLength(StringLength.UNLIMITED)
    String getOptions();

    void setOptions(String options);
}
