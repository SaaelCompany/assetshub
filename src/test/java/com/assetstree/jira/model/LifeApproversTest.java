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
    public void anAssetWithoutAPlaceUsesTheProjectApprover() {
        assertEquals("smirnov", LifeApprovers.resolve("2:ivanov", "smirnov", new int[0]));
        assertEquals("smirnov", LifeApprovers.resolve("2:ivanov", "smirnov", null));
    }
}
