package com.example.lensr.objects;

public interface Editable {
    void openObjectEdit();
    void closeObjectEdit();
    void setHasBeenClicked(boolean hasBeenClicked);
    void delete();
    boolean getHasBeenClicked();
    boolean intersectsMouseHitbox();
}
