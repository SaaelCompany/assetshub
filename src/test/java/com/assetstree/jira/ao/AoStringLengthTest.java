package com.assetstree.jira.ao;

import net.java.ao.schema.StringLength;

import org.junit.Test;

import java.io.File;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Active Objects rejects a string length above 450. Unlimited is the other legal value. */
public class AoStringLengthTest {
    @Test
    public void everyEntityStringFitsActiveObjects() throws Exception {
        assertEquals(450, StringLength.MAX_LENGTH);
        assertEquals(-1, StringLength.UNLIMITED);
        File dir = new File("src/main/java/com/assetstree/jira/ao");
        File[] files = dir.listFiles();
        assertTrue(files != null && files.length > 0);
        int checked = 0;
        for (int i = 0; i < files.length; i++) {
            String name = files[i].getName();
            if (!name.endsWith(".java")) {
                continue;
            }
            Class<?> type = Class.forName("com.assetstree.jira.ao." + name.substring(0, name.length() - 5));
            Method[] methods = type.getMethods();
            for (int m = 0; m < methods.length; m++) {
                Method method = methods[m];
                if (method.getParameterTypes().length != 0 || method.getReturnType() != String.class) {
                    continue;
                }
                if (method.getDeclaringClass() == Object.class) {
                    continue;
                }
                checked++;
                StringLength length = method.getAnnotation(StringLength.class);
                if (length == null) {
                    continue;
                }
                int value = length.value();
                assertTrue(type.getSimpleName() + "." + method.getName() + " is " + value,
                        value == StringLength.UNLIMITED || (value > 0 && value <= StringLength.MAX_LENGTH));
            }
        }
        assertTrue(checked > 0);
        assertEquals(StringLength.UNLIMITED,
                WebhookEntity.class.getMethod("getUrl").getAnnotation(StringLength.class).value());
    }
}
