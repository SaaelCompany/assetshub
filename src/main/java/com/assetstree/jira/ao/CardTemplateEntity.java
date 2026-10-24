package com.assetstree.jira.ao;

import net.java.ao.Entity;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;
import net.java.ao.schema.Unique;

@Table("AST_CARD")
public interface CardTemplateEntity extends Entity {
    @NotNull
    @Indexed
    @Unique
    @StringLength(80)
    String getProjectKey();

    void setProjectKey(String projectKey);

    @StringLength(16)
    String getCardSize();

    void setCardSize(String cardSize);

    Double getWidthMm();

    void setWidthMm(Double widthMm);

    Double getHeightMm();

    void setHeightMm(Double heightMm);

    Integer getGridMm();

    void setGridMm(Integer gridMm);

    @StringLength(8)
    String getQrPlace();

    void setQrPlace(String qrPlace);

    /** One printed line per row: name, key, type, place, or attr:field. */
    @StringLength(StringLength.UNLIMITED)
    String getLines();

    void setLines(String lines);

    /** Free-form blocks. Empty means the line layout above is still in use. */
    @StringLength(StringLength.UNLIMITED)
    String getCanvas();

    void setCanvas(String canvas);
}
