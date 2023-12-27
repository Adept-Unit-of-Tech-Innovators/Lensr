package com.example.lensr;

import com.jfoenix.controls.JFXSlider;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

import java.util.List;

public class WavelengthSlider extends JFXSlider {

    public static JFXSlider slider = new JFXSlider(380, 780, 400);

    public WavelengthSlider(List<Ray> rays, List<Ray> rayReflections) {

        slider.setLayoutX(800);
        slider.setLayoutY(50);
        slider.setPrefHeight(40);
        slider.setPrefWidth(150);

        slider.setMin(380);
        slider.setMax(780);
        slider.setValue(400);

        final double[] colourSliderValue = {0.0};

        // Laser changes
        slider.valueProperty().addListener((observable, oldValue, newValue) -> {
            colourSliderValue[0] = newValue.doubleValue();

            Color color = getColorFromWavelength(colourSliderValue[0]);

            StackPane colorPreview = (StackPane) slider.lookup(".animated-thumb");
            colorPreview.setStyle("-fx-background-color: " + getHexFromColor(color));

            // TODO: This will need to be changed once we add multiple rays
            for (Ray ray : rays) {
                ray.setStroke(color);
            }

            for (Ray rayReflection : rayReflections) {
                double brightness = rayReflection.getBrightness();
                rayReflection.setStroke(color);
                rayReflection.setBrightness(brightness);
            }

        });
        LensrStart.root.getChildren().add(slider);
    }

    public static Color getColorFromWavelength(double wavelength) {
        double factor;
        double red;
        double green;
        double blue;

        int intensityMax = 255;
        double Gamma = 0.8;

        // adjusting to transform between different colours for example green and yellow with addition of red and absence of blue
        // what
        if ((wavelength >= 380) && (wavelength < 440)) {
            red = -(wavelength - 440) / (440 - 380);
            green = 0.0;
            blue = 1.0;
        } else if ((wavelength >= 440) && (wavelength < 490)) {
            red = 0.0;
            green = (wavelength - 440) / (490 - 440);
            blue = 1.0;
        } else if ((wavelength >= 490) && (wavelength < 510)) {
            red = 0.0;
            green = 1.0;
            blue = -(wavelength - 510) / (510 - 490);
        } else if ((wavelength >= 510) && (wavelength < 580)) {
            red = (wavelength - 510) / (580 - 510);
            green = 1.0;
            blue = 0.0;
        } else if ((wavelength >= 580) && (wavelength < 645)) {
            red = 1.0;
            green = -(wavelength - 645) / (645 - 580);
            blue = 0.0;
        } else if ((wavelength >= 645) && (wavelength < 781)) {
            red = 1.0;
            green = 0.0;
            blue = 0.0;
        } else {
            red = 0.0;
            green = 0.0;
            blue = 0.0;
        }

        // Let the intensity fall off near the vision limits
        if ((wavelength >= 380) && (wavelength < 420)) {
            factor = 0.3 + 0.7 * (wavelength - 380) / (420 - 380);
        } else if ((wavelength >= 420) && (wavelength < 701)) {
            factor = 1.0;
        }
        else if ((wavelength >= 701) && (wavelength < 781)) {
            factor = 0.3 + 0.7 * (780 - wavelength) / (780 - 700);
        } else {
            factor = 0.0;
        }

        if (red != 0) {
            red = Math.round(intensityMax * Math.pow(red * factor, Gamma));
        }

        if (green != 0) {
            green = Math.round(intensityMax * Math.pow(green * factor, Gamma));
        }

        if (blue != 0) {
            blue = Math.round(intensityMax * Math.pow(blue * factor, Gamma));
        }

        return Color.rgb((int) red, (int) green, (int) blue);
    }

    public static String getHexFromColor (Color color) {
        return "#" + Integer.toHexString(color.hashCode());
    }

}