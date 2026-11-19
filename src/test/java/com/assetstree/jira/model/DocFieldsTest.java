package com.assetstree.jira.model;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;

public class DocFieldsTest {
    @Test
    public void aSerialFieldFillsTheRussianToken() {
        DocFields.Source source = new DocFields.Source();
        source.name = "Атол 55Ф";
        source.fields.put("serial", "00106228866670");
        source.labels.put("serial", "Серийный номер");
        source.life = "5 лет";
        source.mol = "Иванов И.И.";
        source.role = "Медицинский инженер";
        source.phone = "+7 495 000-00-00";
        source.email = "eng@example.com";
        source.department = "Инженерная служба";
        Map<String, String> values = DocFields.values(source);
        assertEquals("Атол 55Ф", values.get(DocxFill.normalize("наименование")));
        assertEquals("00106228866670", values.get(DocxFill.normalize("серийный")));
        assertEquals("00106228866670", values.get("serial"));
        assertEquals("5 лет", values.get(DocxFill.normalize("срок")));
        assertEquals("Иванов И.И.", values.get(DocxFill.normalize("мол")));
        assertEquals("Медицинский инженер", values.get(DocxFill.normalize("должность")));
        assertEquals("+7 495 000-00-00", values.get(DocxFill.normalize("телефон")));
        assertEquals("eng@example.com", values.get(DocxFill.normalize("почта")));
        assertEquals("Инженерная служба", values.get(DocxFill.normalize("отдел")));
    }

    @Test
    public void issuedPersonAndCardFieldsFillTheirOwnTokens() {
        DocFields.Source source = new DocFields.Source();
        source.mol = "Иванов И.И.";
        source.custodian = "Иванов И.И.";
        source.issued = "Петров П.П.";
        source.email = "";
        source.department = "";
        source.fields.put("mail", "desk@example.com");
        source.labels.put("mail", "Почта");
        source.fields.put("unit", "Бухгалтерия");
        source.labels.put("unit", "Отдел");
        source.fields.put("note", "Кабинет 12");
        source.labels.put("note", "Примечание");
        Map<String, String> values = DocFields.values(source);
        assertEquals("Иванов И.И.", values.get(DocxFill.normalize("мол")));
        assertEquals("Иванов И.И.", values.get(DocxFill.normalize("ответственный")));
        assertEquals("Петров П.П.", values.get(DocxFill.normalize("кому выдано")));
        assertEquals("desk@example.com", values.get(DocxFill.normalize("почта")));
        assertEquals("Бухгалтерия", values.get(DocxFill.normalize("отдел")));
        assertEquals("Кабинет 12", values.get(DocxFill.normalize("Примечание")));
        source.molRole = "Заведующий";
        source.molEmail = "mol@example.com";
        source.molDepartment = "Аптека";
        source.issuedRole = "Инженер";
        source.issuedEmail = "eng@example.com";
        source.issuedDepartment = "ИТ";
        values = DocFields.values(source);
        assertEquals("Заведующий", values.get(DocxFill.normalize("должность мол")));
        assertEquals("mol@example.com", values.get(DocxFill.normalize("почта мол")));
        assertEquals("Аптека", values.get(DocxFill.normalize("отдел мол")));
        assertEquals("Инженер", values.get(DocxFill.normalize("должность кому выдано")));
        assertEquals("eng@example.com", values.get(DocxFill.normalize("почта кому выдано")));
        assertEquals("ИТ", values.get(DocxFill.normalize("отдел кому выдано")));
    }

    @Test
    public void aDepartmentPlaceIsTheDepartmentOnADocument() {
        assertEquals(true, DocFields.departmentPlace("department", "Отделение"));
        assertEquals(true, DocFields.departmentPlace("room", "Отдел кадров"));
        assertEquals(false, DocFields.departmentPlace("branch", "Филиал"));
    }
}
