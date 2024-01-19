package com.example.lensr;

import com.jfoenix.controls.JFXSlider;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

public class WavelengthSlider extends JFXSlider {

    Object currentSource;

    public WavelengthSlider(Object currentSource) {
        this.currentSource = currentSource;

        setLayoutX(800);
        setLayoutY(50);
        setPrefHeight(40);
        setPrefWidth(150);

        setMin(380);
        setMax(780);
        setValue(600);

        final double[] colourSliderValue = {0.0};

        // Laser changes
        valueProperty().addListener((observable, oldValue, newValue) -> {
            colourSliderValue[0] = newValue.doubleValue();

            Color color = new Color(0, 0, 0, 1);

            if (currentSource instanceof BeamSource beamSource) {
                beamSource.setWavelength(colourSliderValue[0]);
                color = (Color) beamSource.originRay.getStroke();
            }

            StackPane colorPreview = (StackPane) lookup(".animated-thumb");
            colorPreview.setStyle("-fx-background-color: " + getHexFromColor(color));
        });
        LensrStart.root.getChildren().add(this);
    }


    public static String getHexFromColor (Color color) {
        int red = (int) (color.getRed() * 255);
        int green = (int) (color.getGreen() * 255);
        int blue = (int) (color.getBlue() * 255);

        return String.format("#%02X%02X%02X", red, green, blue);
    }

    public void setCurrentSource (Object source) {
        currentSource = source;
    }

    public void hide() {
        setVisible(false);
    }

    public void show() {
        setVisible(true);
    }
}