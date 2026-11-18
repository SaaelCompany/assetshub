package com.assetstree.jira.model;

import org.junit.Test;

import java.util.Calendar;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class StatusMomentTest {
    @Test
    public void anEmptyMomentIsNow() {
        Date now = new Date(1_700_000_000_000L);
        assertEquals(now, StatusMoment.at(null, now));
    }

    @Test
    public void aPastMomentIsKept() {
        Date now = new Date(1_700_000_000_000L);
        Date past = StatusMoment.at(Long.valueOf(now.getTime() - 3600_000L), now);
        assertEquals(now.getTime() - 3600_000L, past.getTime());
    }

    @Test
    public void aFutureMomentIsRejected() {
        Date now = new Date(1_700_000_000_000L);
        try {
            StatusMoment.at(Long.valueOf(now.getTime() + 10 * 60_000L), now);
            fail("future");
        } catch (AssetException ex) {
            assertEquals(400, ex.getStatus());
            assertEquals("asset-tree.error.statusWhen", ex.getMessageKey());
        }
    }

    @Test
    public void aMomentBefore2000IsRejected() {
        try {
            StatusMoment.at(Long.valueOf(0L), new Date());
            fail("old");
        } catch (AssetException ex) {
            assertEquals("asset-tree.error.statusWhen", ex.getMessageKey());
        }
    }

    @Test
    public void aWrittenDateKeepsTheClock() {
        Date parsed = StatusMoment.fromValue("2026-11-18 14:00:00.0");
        Calendar clock = Calendar.getInstance();
        clock.setTime(parsed);
        assertEquals(2026, clock.get(Calendar.YEAR));
        assertEquals(Calendar.NOVEMBER, clock.get(Calendar.MONTH));
        assertEquals(18, clock.get(Calendar.DAY_OF_MONTH));
        assertEquals(14, clock.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, clock.get(Calendar.MINUTE));
    }

    @Test
    public void aLocalDateTimeKeepsTheHour() {
        Date parsed = StatusMoment.fromValue("2026-11-18T11:00");
        Calendar clock = Calendar.getInstance();
        clock.setTime(parsed);
        assertEquals(11, clock.get(Calendar.HOUR_OF_DAY));
    }

    @Test
    public void anEmptyOrUnknownValueIsMissing() {
        assertNull(StatusMoment.fromValue(null));
        assertNull(StatusMoment.fromValue(""));
        assertNull(StatusMoment.fromValue("soon"));
    }

    @Test
    public void aDateValueIsCopied() {
        Date source = new Date(1_700_000_000_000L);
        assertEquals(source.getTime(), StatusMoment.fromValue(source).getTime());
    }
}
