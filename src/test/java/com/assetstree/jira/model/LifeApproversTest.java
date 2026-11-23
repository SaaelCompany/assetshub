package com.assetstree.jira.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LifeApproversTest {
    @Test
    public void nearestBranchWinsAndAnotherBranchDoesNot() {
        String places = LifeApprovers.normalize("4:petrova,2:ivanov,bad,0:nobody,9:not a user");
        assertEquals("4:petrova,2:ivanov", places);
        assertEquals("ivanov", LifeApprovers.resolve(places, "smirnov", new int[] {9, 2}));
        assertEquals("petrova", LifeApprovers.resolve(places, "ivanov", new int[] {4}));
        assertEquals("", LifeApprovers.resolve(places, "smirnov", new int[] {7, 8}));
    }

    @Test
    public void assigningOnePlaceKeepsTheOthers() {
        assertEquals("4:petrova,2:anna", LifeApprovers.assign("4:petrova,2:ivanov", 2, "anna"));
        assertEquals("2:ivanov", LifeApprovers.assign("4:petrova,2:ivanov", 4, ""));
        assertEquals("2:ivanov", LifeApprovers.assign("2:ivanov", 2, "not a user"));
    }

    @Test
    public void anAssetWithoutAPlaceUsesTheProjectApprover() {
        assertEquals("smirnov", LifeApprovers.resolve("2:ivanov", "smirnov", new int[0]));
        assertEquals("smirnov", LifeApprovers.resolve("2:ivanov", "smirnov", null));
    }
}
