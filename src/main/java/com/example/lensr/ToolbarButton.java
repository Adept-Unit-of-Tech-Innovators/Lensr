package com.example.lensr;

import com.jfoenix.controls.JFXButton;

public class ToolbarButton extends JFXButton {
    LensrStart.Key valueToSet;
    String label;

    public ToolbarButton(String label, LensrStart.Key valueToSet, int layoutX, int layoutY) {
        this.label = label;
        this.valueToSet = valueToSet;

        setText(label);
        setLayoutX(layoutX);
        setLayoutY(layoutY);
        getStyleClass().add("button");
        setOnAction(actionEvent -> {
            LensrStart.keyPressed = valueToSet;
            updateRender();
        });
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
