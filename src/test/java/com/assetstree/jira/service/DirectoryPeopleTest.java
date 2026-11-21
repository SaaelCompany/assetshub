package com.assetstree.jira.service;

import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DirectoryPeopleTest {
    @Test
    public void theFilterNamesThePersonAndSkipsComputers() {
        String filter = DirectoryPeople.filter("sAMAccountName", "ivanov", "ivanov@example.com");
        assertTrue(filter.contains("(sAMAccountName=ivanov)"));
        assertTrue(filter.contains("(mail=ivanov@example.com)"));
        assertTrue(filter.contains("(!(objectClass=computer))"));
        assertEquals("", DirectoryPeople.filter("sAMAccountName", " ", ""));
    }

    @Test
    public void aStarInTheNameCannotWidenTheSearch() {
        String filter = DirectoryPeople.filter("cn", "a*b)(cn=*", "");
        assertTrue(filter.contains("(cn=a\\*b\\)\\(cn=\\*)"));
        assertTrue(DirectoryPeople.filter("bad name", "ivanov", "").contains("(sAMAccountName=ivanov)"));
    }

    @Test
    public void theSearchBaseKeepsTheUserContainer() {
        assertEquals("CN=Users,DC=clinic,DC=ru", DirectoryPeople.searchBase("CN=Users", "DC=clinic,DC=ru"));
        assertEquals("DC=clinic,DC=ru", DirectoryPeople.searchBase("", "DC=clinic,DC=ru"));
        assertEquals("CN=Users,DC=clinic,DC=ru", DirectoryPeople.searchBase("CN=Users,DC=clinic,DC=ru", "DC=clinic,DC=ru"));
    }

    @Test
    public void attributesPickTheDeskPhoneAndTheJob() {
        Map<String, String> attributes = new LinkedHashMap<String, String>();
        attributes.put("mobile", "+7 900");
        attributes.put("telephoneNumber", "+7 495");
        attributes.put("title", "Инженер");
        attributes.put("department", "Аптека");
        DirectoryPeople.Found found = DirectoryPeople.fromAttributes(attributes);
        assertEquals("+7 495", found.phone);
        assertEquals("Инженер", found.title);
        assertEquals("Аптека", found.department);
    }

    @Test
    public void aMappedDirectoryAttributeIsRequestedWithTheUsualOnes() {
        Map<String, String> stored = new LinkedHashMap<String, String>();
        stored.put("ldap.user.title", "extensionAttribute12");
        String[] names = DirectoryPeople.attributeNames(stored);
        boolean title = false;
        boolean extra = false;
        for (int i = 0; i < names.length; i++) {
            if ("title".equals(names[i])) {
                title = true;
            }
            if ("extensionAttribute12".equals(names[i])) {
                extra = true;
            }
        }
        assertTrue(title);
        assertTrue(extra);
    }
}
