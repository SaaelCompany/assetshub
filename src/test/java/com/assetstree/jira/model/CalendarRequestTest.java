package com.assetstree.jira.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class CalendarRequestTest {
    @Test
    public void subjectKeepsTheKindNameAndDate() {
        CalendarRequest request = CalendarRequest.compose("ТО", "Замена фильтра", "2026-11-17",
                "09:00", "11:00", "Иванов", "PC-1", "Склад", "Принести фильтр",
                "Вид", "Дата", "Время", "Исполнитель", "Оборудование", "Заметка", "Работа календаря");
        assertEquals("ТО: Замена фильтра, 2026-11-17", request.getSummary());
        assertEquals("Вид: ТО\nДата: 2026-11-17\nВремя: 09:00\u201311:00\nИсполнитель: Иванов\n"
                + "Оборудование: PC-1 Склад\nЗаметка:\nПринести фильтр", request.getDescription());
    }

    @Test
    public void sameKindAndNameAreWrittenOnce() {
        assertEquals("Поверка, 2026-11-17", CalendarRequest.summary("Поверка", "поверка", "2026-11-17", "Работа"));
        assertEquals("2026-11-17", CalendarRequest.summary("", "  ", "2026-11-17", "Работа"));
        assertEquals("Работа", CalendarRequest.summary("", "", "", "Работа"));
    }

    @Test
    public void emptyEquipmentAndABrokenLineStayOut() {
        String description = CalendarRequest.description("КТС", "2026-11-17", "08:00", "", "",
                "", "", "Смотреть\nщит", "Вид", "Дата", "Время", "Исполнитель", "Оборудование", "Заметка");
        assertEquals("Вид: КТС\nДата: 2026-11-17\nВремя: 08:00\nЗаметка:\nСмотреть\nщит", description);
        assertFalse(description.contains("Оборудование"));
        assertEquals("ТО: Замена фильтра", CalendarRequest.summary("ТО", "Замена\nфильтра", "", "Работа"));
    }
}
