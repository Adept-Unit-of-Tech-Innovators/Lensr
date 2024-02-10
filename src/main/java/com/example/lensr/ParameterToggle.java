package com.example.lensr;

import com.example.lensr.objects.BeamSource;
import com.example.lensr.objects.PanelSource;
import com.jfoenix.controls.JFXToggleButton;

import static com.example.lensr.LensrStart.root;
import static com.example.lensr.LensrStart.toolbar;

public class ParameterToggle extends JFXToggleButton {
    enum ParameterToChange {
        WhiteLight
    }
    ParameterToChange parameterToChange;
    Object currentSource;

    public ParameterToggle(Object source, ParameterToChange parameterToChange) {
        this.currentSource = source;
        this.parameterToChange = parameterToChange;

        // Set up the GUI
        // TODO: Make this not look abhorrent
        show();
        setLayoutX(850);
        setLayoutY(50);

        root.getChildren().add(this);
        setOnAction(event -> {
            if (currentSource instanceof BeamSource beamSource && parameterToChange == ParameterToChange.WhiteLight) {
                setText("White Light");
                beamSource.setWhiteLight(isSelected());
            }
            else if (currentSource instanceof PanelSource panelSource && parameterToChange == ParameterToChange.WhiteLight) {
                setText("White Light");
                panelSource.setWhiteLight(isSelected());
            }
        });
    }


    public void setCurrentSource(Object currentSource) {
        this.currentSource = currentSource;
    }


    public void show() {
        // Update UI
        toFront();
        setVisible(true);
        toolbar.forEach(button -> button.setVisible(false));
    }


    public void hide() {
        // Update UI
        setVisible(false);
        toolbar.forEach(button -> button.setVisible(true));
    }
}
