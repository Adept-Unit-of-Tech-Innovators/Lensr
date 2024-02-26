package com.example.lensr.ui;

import com.example.lensr.objects.lightsources.BeamSource;
import com.example.lensr.objects.lightsources.PanelSource;
import com.example.lensr.objects.lightsources.PointSource;
import com.example.lensr.saveloadkit.SaveState;
import com.jfoenix.controls.JFXToggleButton;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import static com.example.lensr.LensrStart.root;
import static com.example.lensr.LensrStart.toolbar;

public class ParameterToggle extends JFXToggleButton {
    public enum ParameterToChange {
        WhiteLight
    }
    ParameterToChange parameterToChange;
    Object currentSource;
    Text label = new Text();
    VBox switchAndLabel = new VBox();

    public ParameterToggle(Object source, String labelText, ParameterToChange parameterToChange) {
        this.currentSource = source;
        this.parameterToChange = parameterToChange;
        this.label.setText(labelText);

        // Set up the GUI
        hide();
        getStyleClass().add("switch");
        label.getStyleClass().add("label");

        switchAndLabel.getChildren().add(label);
        switchAndLabel.getChildren().add(this);
        switchAndLabel.setLayoutY(25);
        switchAndLabel.setLayoutX(875);
        switchAndLabel.setAlignment(javafx.geometry.Pos.CENTER);
        root.getChildren().add(switchAndLabel);

        setOnAction(event -> {
            if (currentSource instanceof BeamSource beamSource && parameterToChange == ParameterToChange.WhiteLight) {
                label.setText("White Light");
                beamSource.setWhiteLight(isSelected());
            }
            else if (currentSource instanceof PanelSource panelSource && parameterToChange == ParameterToChange.WhiteLight) {
                label.setText("White Light");
                panelSource.setWhiteLight(isSelected());
            }
            else if (currentSource instanceof PointSource pointSource && parameterToChange == ParameterToChange.WhiteLight) {
                label.setText("White Light");
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
        switchAndLabel.setVisible(true);
        switchAndLabel.toFront();
        toolbar.forEach(button -> button.setVisible(false));
    }


    public void hide() {
        // Update UI
        switchAndLabel.setVisible(false);
        toolbar.forEach(button -> button.setVisible(true));
    }
}
