package io.github.kebritam;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ToStringCreatorTest {

    @Test
    void noAppend() {
        String result = new ToStringCreator()
                .create();

        assertEquals("[]", result);
    }

    @Test
    void appendPrimitiveTypes() {
        String result = new ToStringCreator()
                .append("int", 1)
                .append("double", 1.D)
                .append("float", 1.F)
                .append("boolean", false)
                .append("short", (short) 1)
                .append("byte", (byte) 1)
                .append("long", 12L)
                .create();

        assertEquals("[int:1, double:1.0, float:1.0, boolean:false, short:1, byte:1, long:12]", result);
    }

    @Test
    void appendNonPrimitiveType() {
        String result = new ToStringCreator()
                .append("string", "Hello there!")
                .append("class-object", ToStringCreator.class)
                .append("userClass", new Object() {
                    private final String packageName = Object.class.getPackageName();
                    private final int fieldsCount = Object.class.getFields().length;

                    @Override
                    public String toString() {
                        return "Object{" +
                                "package='" + packageName + '\'' +
                                ", fieldsCount=" + fieldsCount +
                                '}';
                    }
                })
                .create();

        assertEquals("[string:Hello there!, class-object:class org.kebritam.ToStringCreator, " +
                        "userClass:Object{package='java.lang', fieldsCount=0}]", result);
    }
}