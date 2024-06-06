package io.github.kebritam;

public class ToStringCreator {

    private boolean isFirstField;
    private final StringBuilder builder = new StringBuilder();

    public ToStringCreator() {
        isFirstField = true;
        builder.append('[');
    }

    public ToStringCreator append(String fieldName, byte value) {
        appendFieldName(fieldName)
                .append(value);
        return this;
    }

    public ToStringCreator append(String fieldName, short value) {
        appendFieldName(fieldName)
                .append(value);
        return this;
    }

    public ToStringCreator append(String fieldName, int value) {
        appendFieldName(fieldName)
                .append(value);
        return this;
    }

    public ToStringCreator append(String fieldName, long value) {
        appendFieldName(fieldName)
                .append(value);
        return this;
    }

    public ToStringCreator append(String fieldName, float value) {
        appendFieldName(fieldName)
                .append(value);
        return this;
    }

    public ToStringCreator append(String fieldName, double value) {
        appendFieldName(fieldName)
                .append(value);
        return this;
    }

    public ToStringCreator append(String fieldName, boolean value) {
        appendFieldName(fieldName)
                .append(value);
        return this;
    }

    public ToStringCreator append(String fieldName, Object value) {
        appendFieldName(fieldName)
                .append(value);
        return this;
    }

    private StringBuilder appendFieldName(String fieldName) {
        if (isFirstField)
            isFirstField = false;
        else
            builder.append(", ");

        return builder.append(fieldName)
                .append(':');
    }

    public String create() {
        return builder
                .append(']')
                .toString();
    }
}
