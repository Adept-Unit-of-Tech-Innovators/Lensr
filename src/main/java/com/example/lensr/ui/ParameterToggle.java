package com.example.lensr.ui;

import com.example.lensr.objects.lightsources.BeamSource;
import com.example.lensr.objects.lightsources.PanelSource;
import com.example.lensr.objects.lightsources.PointSource;
import com.example.lensr.saveloadkit.SaveState;
import com.jfoenix.controls.JFXToggleButton;

import static com.example.lensr.LensrStart.root;
import static com.example.lensr.LensrStart.toolbar;

public class ParameterToggle extends JFXToggleButton {
    public enum ParameterToChange {
        WhiteLight
    }
    ParameterToChange parameterToChange;
    Object currentSource;

    public ParameterToggle(Object source, ParameterToChange parameterToChange) {
        this.currentSource = source;
        this.parameterToChange = parameterToChange;

        // Set up the GUI
        // TODO: Make this not look abhorrent
        hide();
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
            else if(currentSource instanceof PointSource pointSource && parameterToChange == ParameterToChange.WhiteLight) {
                setText("White Light");
                pointSource.setWhiteLight(isSelected());
            }
            SaveState.autoSave();
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
