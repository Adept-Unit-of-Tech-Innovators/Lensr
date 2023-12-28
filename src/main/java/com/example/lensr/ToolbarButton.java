package com.example.lensr;

import com.jfoenix.controls.JFXButton;

public class ToolbarButton extends JFXButton {
    MutableValue variableToChange;
    MutableValue oppositeVariable;
    String label;

    public ToolbarButton(String label, MutableValue variableToChange, MutableValue oppositeVariable, int layoutX, int layoutY) {
        this.label = label;
        this.variableToChange = variableToChange;
        this.oppositeVariable = oppositeVariable;

        setText(label);
        setLayoutX(layoutX);
        setLayoutY(layoutY);
        getStyleClass().add("button");
        setOnAction(actionEvent -> {
            variableToChange.setValueAndCloseEdit(!variableToChange.getValue(), oppositeVariable);
            updateRender();
        });
    }

    public void addToRoot() {
        LensrStart.root.getChildren().add(this);
    }

    public void updateRender() {
        if (variableToChange.getValue()) {
            getStyleClass().add("button-selected");
        } else {
            getStyleClass().remove("button-selected");
        }
    }
}
