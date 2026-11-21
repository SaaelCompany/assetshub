package com.assetstree.jira.service;

import com.assetstree.jira.dto.UserProfileDto;
import com.assetstree.jira.model.PersonContact;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class AdSyncContactTest {
    @Test
    public void theDirectoryAttributeNameFillsTheJobWhenTheJiraNameDoesNot() {
        Map<String, String> attributes = new LinkedHashMap<String, String>();
        AdSyncContact.keep(attributes, "Роль в клинике", "title", "Медицинский инженер");
        AdSyncContact.keep(attributes, "Подразделение", "department", "Аптека");
        AdSyncContact.keep(attributes, "Сотовый", "mobile", "+7 900");
        UserProfileDto dto = new UserProfileDto();
        PersonContact.apply(dto, attributes);
        assertEquals("Медицинский инженер", dto.getTitle());
        assertEquals("Аптека", dto.getDepartment());
        assertEquals("+7 900", dto.getPhone());
    }

    @Test
    public void anExistingValueIsNotReplaced() {
        Map<String, String> contact = new LinkedHashMap<String, String>();
        contact.put("title", "Своя");
        AdSyncContact.keep(contact, "Должность", "title", "Чужая");
        assertEquals("Своя", contact.get("title"));
        assertEquals("Чужая", contact.get("Должность"));
    }
}
