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
    }

    public void setValueAndCloseEdit(boolean value) {
        this.value = value;
        for (Object mirror : LensrStart.mirrors) {
            if (mirror instanceof LineMirror lineMirror) {
                lineMirror.closeObjectEdit();
            }
            if (mirror instanceof EllipseMirror ellipseMirror) {
                ellipseMirror.closeObjectEdit();
            }
            if (mirror instanceof FunnyMirror funnyMirror) {
                funnyMirror.closeObjectEdit();
            }
        }
    }

    public void setValueAndCloseEdit(boolean value, MutableValue[] oppositeValues) {
        this.value = value;
        for (MutableValue oppositeValue : oppositeValues) {
            oppositeValue.setValueAndCloseEdit(false);
        }
    }
}
