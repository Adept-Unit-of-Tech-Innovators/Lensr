package com.example.lensr.ui;

import com.example.lensr.LensrStart;
import com.jfoenix.controls.JFXButton;

public class ToolbarButton extends JFXButton {
    public LensrStart.Key valueToSet;
    String label;

    public ToolbarButton(String label, LensrStart.Key valueToSet, int layoutX, int layoutY) {
        this.label = label;
        this.valueToSet = valueToSet;

        setText(label);
        setLayoutX(layoutX);
        setLayoutY(layoutY);
        getStyleClass().add("button");
        toFront();
    }

    public void addToRoot() {
        LensrStart.root.getChildren().add(this);
    }

    public void updateRender() {
        if (LensrStart.keyPressed == valueToSet) {
            getStyleClass().add("button-selected");
        } else {
            getStyleClass().remove("button-selected");
        }
    }
}
