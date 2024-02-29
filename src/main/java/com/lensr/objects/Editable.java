package com.lensr.objects;

public interface Editable {
    void openObjectEdit();
    void closeObjectEdit();
    void setHasBeenClicked(boolean hasBeenClicked);
    void delete();
    void copy();
    void moveBy(double x, double y);
    boolean getHasBeenClicked();
    boolean intersectsMouseHitbox();
    void create();
}
