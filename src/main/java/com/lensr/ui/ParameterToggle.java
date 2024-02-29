package com.lensr.ui;

import com.lensr.objects.lightsources.RaySource;
import com.lensr.objects.lightsources.BeamSource;
import com.lensr.objects.lightsources.PointSource;
import com.lensr.saveloadkit.SaveState;
import com.jfoenix.controls.JFXToggleButton;
import com.lensr.LensrStart;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

public class ParameterToggle extends JFXToggleButton {
    public enum ParameterToChange {
        WhiteLight
    }

    ParameterToChange parameterToChange;
    Object currentSource;
    Text label = new Text();
    HBox switchAndLabel = new HBox();

    public ParameterToggle(Object source, String labelText, ParameterToChange parameterToChange) {
        this.currentSource = source;
        this.parameterToChange = parameterToChange;
        this.label.setText(labelText);

        // Set up the GUI
        hide();
        getStyleClass().add("switch");
        label.getStyleClass().add("label");
        label.setFill(Color.web("#DBDEDC"));

        switchAndLabel.getChildren().add(label);
        switchAndLabel.getChildren().add(this);
        switchAndLabel.setLayoutY(-2);
        switchAndLabel.setLayoutX(LensrStart.WIDTH - 155);
        switchAndLabel.setSpacing(10);
        switchAndLabel.setAlignment(javafx.geometry.Pos.CENTER);
        switchAndLabel.setViewOrder(-3);
        LensrStart.root.getChildren().add(switchAndLabel);

        setOnAction(event -> {
            if (currentSource instanceof RaySource raySource && parameterToChange == ParameterToChange.WhiteLight) {
                label.setText("White Light");
                raySource.setWhiteLight(isSelected());
            } else if (currentSource instanceof BeamSource beamSource && parameterToChange == ParameterToChange.WhiteLight) {
                label.setText("White Light");
                beamSource.setWhiteLight(isSelected());
            } else if (currentSource instanceof PointSource pointSource && parameterToChange == ParameterToChange.WhiteLight) {
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
    }


    public void hide() {
        // Update UI
        switchAndLabel.setVisible(false);
    }
}
