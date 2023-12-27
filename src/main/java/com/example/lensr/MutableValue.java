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

    public void setValue(boolean value, MutableValue opositeValue) {
        this.value = value;
        opositeValue.setValue(false);
    }
}
