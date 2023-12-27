package com.example.lensr;

public class MutableValue {
    private boolean value;

    public MutableValue(boolean value) {
        this.value = value;
    }

    public boolean getValue() {
        return value;
    }

    public void setValue(boolean value) {
        this.value = value;
        for (Object mirror : LensrStart.mirrors) {
            if (mirror instanceof LineMirror lineMirror) {
                lineMirror.closeObjectEdit();
            }
            if (mirror instanceof EllipseMirror ellipseMirror) {
                ellipseMirror.closeObjectEdit();
            }
        }
    }

    public void setValue(boolean value, MutableValue opositeValue) {
        this.value = value;
        opositeValue.setValue(false);
    }
}
