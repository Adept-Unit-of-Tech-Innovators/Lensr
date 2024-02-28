package com.example.lensr.ui;

import com.example.lensr.objects.lightsources.BeamSource;
import com.example.lensr.objects.lightsources.PanelSource;
import com.example.lensr.objects.lightsources.PointSource;
import com.example.lensr.saveloadkit.SaveState;
import com.jfoenix.controls.JFXToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import static com.example.lensr.LensrStart.*;

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
        switchAndLabel.setLayoutX(WIDTH - 155);
        switchAndLabel.setSpacing(10);
        switchAndLabel.setAlignment(javafx.geometry.Pos.CENTER);
        switchAndLabel.setViewOrder(-3);
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
    }


    public void hide() {
        // Update UI
        switchAndLabel.setVisible(false);
    }
}
