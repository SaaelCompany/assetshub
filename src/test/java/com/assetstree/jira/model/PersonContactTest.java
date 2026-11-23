package com.assetstree.jira.model;

import com.assetstree.jira.dto.UserProfileDto;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class PersonContactTest {
    @Test
    public void deskPhoneAndJobTitleWinOverWeakerAttributes() {
        Map<String, String> attributes = new LinkedHashMap<String, String>();
        attributes.put("mobile", "+7 900");
        attributes.put("telephoneNumber", "+7 495");
        attributes.put("facsimileTelephoneNumber", "+7 496");
        attributes.put("employeeType", "512");
        attributes.put("personalTitle", "г-н");
        attributes.put("title", "Медицинский инженер");
        attributes.put("description", "не должность");
        UserProfileDto dto = new UserProfileDto();
        PersonContact.apply(dto, attributes);
        assertEquals("+7 495", dto.getPhone());
        assertEquals("Медицинский инженер", dto.getTitle());
    }

    @Test
    public void russianPropertyNamesFillPhoneAndTitle() {
        Map<String, String> attributes = new LinkedHashMap<String, String>();
        attributes.put("Мобильный", "1234");
        attributes.put("Должность", "Инженер");
        attributes.put("Отдел", "Аптека");
        UserProfileDto dto = new UserProfileDto();
        PersonContact.apply(dto, attributes);
        assertEquals("1234", dto.getPhone());
        assertEquals("Инженер", dto.getTitle());
        assertEquals("Аптека", dto.getDepartment());
    }

    @Test
    public void employeeTypeAndAShortDescriptionFillTheJobWhenTitleIsAbsent() {
        Map<String, String> attributes = new LinkedHashMap<String, String>();
        attributes.put("employeeType", "Врач-терапевт");
        attributes.put("description", "Длинное описание, которое не должно победить должность");
        UserProfileDto dto = new UserProfileDto();
        PersonContact.apply(dto, attributes);
        assertEquals("Врач-терапевт", dto.getTitle());
        attributes.clear();
        attributes.put("description", "Заведующий отделением");
        dto = new UserProfileDto();
        PersonContact.apply(dto, attributes);
        assertEquals("Заведующий отделением", dto.getTitle());
    }

    @Test
    public void aPanelOfLabeledLinesFillsTheJobTheDepartmentAndThePhone() {
        Map<String, String> attributes = new LinkedHashMap<String, String>();
        attributes.put("AD Атрибуты автора",
                "Должность: Заместитель руководителя Единого информационного центра ОП\n"
                        + "Почта: e.burakova@bzklinika.ru\n"
                        + "Отдел: КРС Информационный центр\n"
                        + "Мобильный: 79233040561");
        UserProfileDto dto = new UserProfileDto();
        PersonContact.apply(dto, attributes);
        assertEquals("Заместитель руководителя Единого информационного центра ОП", dto.getTitle());
        assertEquals("КРС Информационный центр", dto.getDepartment());
        assertEquals("79233040561", dto.getPhone());
        assertEquals("e.burakova@bzklinika.ru", dto.getEmail());
    }

    @Test
    public void aFilledValueStays() {
        Map<String, String> attributes = new LinkedHashMap<String, String>();
        attributes.put("title", "Другая");
        attributes.put("telephoneNumber", "+7 000");
        UserProfileDto dto = new UserProfileDto();
        dto.setTitle("Своя");
        dto.setPhone("+7 111");
        PersonContact.apply(dto, attributes);
        assertEquals("Своя", dto.getTitle());
        assertEquals("+7 111", dto.getPhone());
    }
}
