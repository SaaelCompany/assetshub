package com.assetstree.jira.service;

import com.assetstree.jira.model.TniComputer;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TniSccmTest {
    @Test
    public void readsASitePage() {
        String json = "{"
                + "\"@odata.nextLink\":\"https://evil.example/AdminService/next\","
                + "\"value\":["
                + "{\"ResourceId\":16777219,\"Name\":\"PC-01\",\"ResourceDomainORWorkgroup\":\"CORP\","
                + "\"UserName\":\"ivanov\",\"OperatingSystemNameandVersion\":\"Windows 10\","
                + "\"ClientVersion\":\"5.00.9040.1044\",\"DistinguishedName\":\"CN=PC-01,OU=PCs,DC=corp,DC=local\"},"
                + "{\"ResourceId\":42,\"Name\":\"Quote \\\"lab\\\"\",\"UserName\":\"\"}"
                + "]}";
        TniSccm.Page page = TniSccm.parse(json);
        List<TniComputer> rows = page.getComputers();
        assertEquals(2, rows.size());
        assertEquals("sccm:16777219", rows.get(0).externalKey());
        assertEquals("PC-01", rows.get(0).getName());
        assertTrue(rows.get(0).getDetail().contains("Windows 10"));
        assertTrue(rows.get(0).getDetail().contains("CORP"));
        assertTrue(rows.get(0).getDetail().contains("ivanov"));
        assertEquals("sccm:42", rows.get(1).externalKey());
        assertEquals("Quote \"lab\"", rows.get(1).getName());
        assertNull(TniSccm.sameHost("sccm.corp.local", page.getNext()));
        assertEquals("https://sccm.corp.local/AdminService/wmi/SMS_R_System?$skip=500",
                TniSccm.sameHost("sccm.corp.local", "https://sccm.corp.local/AdminService/wmi/SMS_R_System?$skip=500"));
    }
}
