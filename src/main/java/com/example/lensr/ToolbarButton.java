package com.example.lensr;

import com.jfoenix.controls.JFXButton;

public class ToolbarButton extends JFXButton {
    MutableValue variableToChange;
    MutableValue[] oppositeVariables;
    String label;

    public ToolbarButton(String label, MutableValue variableToChange, MutableValue[] oppositeVariables, int layoutX, int layoutY) {
        this.label = label;
        this.variableToChange = variableToChange;
        this.oppositeVariables = oppositeVariables;

        setText(label);
        setLayoutX(layoutX);
        setLayoutY(layoutY);
        getStyleClass().add("button");
        setOnAction(actionEvent -> {
            variableToChange.setValueAndCloseEdit(!variableToChange.getValue(), oppositeVariables);
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
