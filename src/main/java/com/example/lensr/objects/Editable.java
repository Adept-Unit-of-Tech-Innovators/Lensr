package com.example.lensr.objects;

public interface Editable {
    void openObjectEdit();
    void closeObjectEdit();
    void setHasBeenClicked(boolean hasBeenClicked);
    boolean getHasBeenClicked();
    boolean intersectsMouseHitbox();
}
