package com.assetstree.jira.model;

import org.junit.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Jira formats every catalog string with MessageFormat. A raw "{" aborts /meta. */
public class I18nPatternsTest {
    @Test
    public void everyCatalogStringFormats() throws Exception {
        String[] names = {"/i18n/asset-tree.properties", "/i18n/asset-tree_ru_RU.properties"};
        for (String name : names) {
            Properties props = new Properties();
            InputStream input = I18nPatternsTest.class.getResourceAsStream(name);
            assertNotNull(name, input);
            props.load(new InputStreamReader(input, StandardCharsets.UTF_8));
            input.close();
            for (String key : props.stringPropertyNames()) {
                if (!key.startsWith("asset-tree.")) {
                    continue;
                }
                String pattern = props.getProperty(key);
                int slots = 0;
                for (int index = 0; index < 8; index++) {
                    if (pattern.contains("{" + index + "}")) {
                        slots = index + 1;
                    }
                }
                Object[] args = new Object[slots];
                for (int index = 0; index < slots; index++) {
                    args[index] = "x";
                }
                String formatted = new MessageFormat(pattern, Locale.US).format(args);
                assertNotNull(key, formatted);
            }
        }
    }

    @Test
    public void webhookSampleStaysJsonAfterFormatting() throws Exception {
        Properties english = load("/i18n/asset-tree.properties");
        Properties russian = load("/i18n/asset-tree_ru_RU.properties");
        assertEquals("{\"objectKey\":\"\",\"name\":\"Laptop\",\"typeKey\":\"laptop\"}",
                format(english.getProperty("asset-tree.ui.webhookSample")));
        assertEquals("{\"objectKey\":\"\",\"name\":\"\u041d\u043e\u0443\u0442\u0431\u0443\u043a\",\"typeKey\":\"laptop\"}",
                format(russian.getProperty("asset-tree.ui.webhookSample")));
        assertTrue(format(english.getProperty("asset-tree.ui.webhookRecipe")).contains("Automation's"));
    }

    private static Properties load(String name) throws Exception {
        Properties props = new Properties();
        InputStream input = I18nPatternsTest.class.getResourceAsStream(name);
        assertNotNull(name, input);
        props.load(new InputStreamReader(input, StandardCharsets.UTF_8));
        input.close();
        return props;
    }

    private static String format(String pattern) {
        return new MessageFormat(pattern, Locale.US).format(new Object[0]);
    }
}
