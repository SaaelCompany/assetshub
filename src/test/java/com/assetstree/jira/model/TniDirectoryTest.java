package com.assetstree.jira.model;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TniDirectoryTest {
    @Test
    public void formatsActiveDirectoryGuid() {
        byte[] raw = new byte[] {
                0x33, 0x22, 0x11, 0x00,
                0x55, 0x44,
                0x77, 0x66,
                (byte) 0x88, (byte) 0x99, (byte) 0xaa, (byte) 0xbb,
                (byte) 0xcc, (byte) 0xdd, (byte) 0xee, (byte) 0xff
        };
        assertEquals("00112233-4455-6677-8899-aabbccddeeff", TniDirectory.guid(raw));
    }

    @Test
    public void readsSccmExport() {
        String csv = "Name,Resource ID,Operating System,User Name,IP Address,Domain,Manufacturer,Model\n"
                + "PC-01,101,Windows 10,ivanov,10.8.0.15,corp.local,Dell,OptiPlex\n"
                + "PC-02,102.0,\"Windows 11, Enterprise\",petrov,10.8.0.16,corp.local,HP,EliteBook\n";
        List<TniComputer> rows = TniDirectory.parseSheet(csv);
        assertEquals(2, rows.size());
        assertEquals("sccm:101", rows.get(0).externalKey());
        assertEquals("PC-01", rows.get(0).getName());
        assertTrue(rows.get(0).getDetail().contains("Windows 10"));
        assertTrue(rows.get(0).getDetail().contains("Dell OptiPlex"));
        assertEquals("sccm:102", rows.get(1).externalKey());
        assertTrue(rows.get(1).getDetail().contains("Windows 11, Enterprise"));
    }

    @Test
    public void readsAdExportAndSkipsTypeLine() {
        String csv = "#TYPE Selected.Microsoft.ActiveDirectory.Management.ADComputer\n"
                + "Name;DNSHostName;OperatingSystem;DistinguishedName;ObjectGUID;Enabled\n"
                + "PC-03;pc-03.corp.local;Windows 10;CN=PC-03,OU=Office,DC=corp,DC=local;{11111111-1111-1111-1111-111111111111};True\n"
                + "OLD;old.corp.local;Windows 7;CN=OLD,OU=Office,DC=corp,DC=local;;False\n";
        List<TniComputer> rows = TniDirectory.parseSheet(csv);
        assertEquals(2, rows.size());
        assertEquals("ad:11111111-1111-1111-1111-111111111111", rows.get(0).externalKey());
        assertEquals("pc-03.corp.local", rows.get(0).getHost());
        assertFalse(rows.get(0).isDisabled());
        assertEquals("ad:old.corp.local", rows.get(1).externalKey());
        assertTrue(rows.get(1).isDisabled());
    }

    @Test
    public void readsRussianHeaders() {
        String csv = "Имя компьютера;Операционная система;Пользователь\n"
                + "Бухгалтерия;Windows 10;sidorov\n";
        List<TniComputer> rows = TniDirectory.parseSheet(csv);
        assertEquals(1, rows.size());
        assertEquals("ad:бухгалтерия", rows.get(0).externalKey());
        assertEquals("Бухгалтерия", rows.get(0).getName());
    }

    @Test
    public void rejectsAPublicHostAndAMissingHeader() {
        try {
            TniDirectory.host("ldap://dc.corp.local");
            fail("host");
        } catch (AssetException ex) {
            assertEquals(TniDirectory.CONNECT, ex.getMessageKey());
        }
        try {
            TniDirectory.base("corp local");
            fail("base");
        } catch (AssetException ex) {
            assertEquals(TniDirectory.BASE, ex.getMessageKey());
        }
        try {
            TniDirectory.parseSheet("Hello,world\n1,2\n");
            fail("sheet");
        } catch (AssetException ex) {
            assertEquals(TniDirectory.SHEET, ex.getMessageKey());
        }
        StringBuilder big = new StringBuilder("Name,Resource ID\n");
        for (int i = 0; i < 2001; i++) {
            big.append("PC-").append(i).append(',').append(i).append('\n');
        }
        try {
            TniDirectory.parseSheet(big.toString());
            fail("rows");
        } catch (AssetException ex) {
            assertEquals(TniDirectory.ROWS, ex.getMessageKey());
        }
    }

    @Test
    public void keepsTheDefaultPorts() {
        assertEquals(389, TniDirectory.port(null, false));
        assertEquals(636, TniDirectory.port(Integer.valueOf(0), true));
        assertEquals("dc-01.corp.local", TniDirectory.host(" dc-01.corp.local "));
        assertEquals("DC=corp,DC=local", TniDirectory.base("DC=corp,DC=local"));
        assertEquals("DC=budzdorov,DC=ru", TniDirectory.base("budzdorov.ru"));
        assertEquals("", TniDirectory.base("BUDZDOROV"));
        assertEquals("", TniDirectory.base(""));
    }

    @Test
    public void acceptsABom() {
        String csv = "\uFEFFName,DNSHostName\nPC-04,pc-04.corp.local\n";
        assertEquals("ad:pc-04.corp.local", TniDirectory.parseSheet(csv).get(0).externalKey());
        assertEquals(3, "\uFEFF".getBytes(StandardCharsets.UTF_8).length);
    }
}
