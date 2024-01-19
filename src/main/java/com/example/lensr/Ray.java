package com.example.lensr;

import javafx.geometry.Bounds;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

public class Ray extends Line {
    double brightness = 1.0;
    double wavelength;

    public Ray(double startX, double startY, double endX, double endY) {
        setStartX(startX);
        setStartY(startY);
        setEndX(endX);
        setEndY(endY);
    }

    public void setWavelength(double wavelength) {
        this.wavelength = wavelength;

        double factor;
        double red;
        double green;
        double blue;

        int intensityMax = 255;
        double Gamma = 0.8;

        // adjusting to transform between different colors for example green and yellow with addition of red and absence of blue
        // what
        if ((wavelength >= 380) && (wavelength < 440)) {
            red = -(wavelength - 440.0) / (440.0 - 380.0);
            green = 0.0;
            blue = 1.0;
        } else if ((wavelength >= 440) && (wavelength < 490)) {
            red = 0.0;
            green = (wavelength - 440.0) / (490.0 - 440.0);
            blue = 1.0;
        } else if ((wavelength >= 490) && (wavelength < 510)) {
            red = 0.0;
            green = 1.0;
            blue = -(wavelength - 510.0) / (510.0 - 490.0);
        } else if ((wavelength >= 510) && (wavelength < 580)) {
            red = (wavelength - 510.0) / (580.0 - 510.0);
            green = 1.0;
            blue = 0.0;
        } else if ((wavelength >= 580) && (wavelength < 645)) {
            red = 1.0;
            green = -(wavelength - 645.0) / (645.0 - 580.0);
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

        setStroke(Color.rgb((int) red, (int) green, (int) blue));
    }


    public double getWavelength() {
        return wavelength;
    }


    // Brightness = Opacity
    public void setBrightness(double brightness) {
        this.brightness = brightness;

        Color strokeColor = (Color) this.getStroke();
        Color updatedColor = new Color(
                strokeColor.getRed(),
                strokeColor.getGreen(),
                strokeColor.getBlue(),
                brightness);

        this.setStroke(updatedColor);
    }


    public double getBrightness() {
        return brightness;
    }

    public double getMinimalDistanceToBounds(Bounds bounds) {
        // Get the start position of the ray
        double startX = getStartX();
        double startY = getStartY();

        // Calculate the minimal distance from the start position of the ray to the given bounds
        double distanceX;
        double distanceY;

        if (startX < bounds.getMinX()) {
            distanceX = bounds.getMinX() - startX;
        } else if (startX > bounds.getMaxX()) {
            distanceX = startX - bounds.getMaxX();
        } else {
            distanceX = 0; // Start X is within the bounds
        }

        if (startY < bounds.getMinY()) {
            distanceY = bounds.getMinY() - startY;
        } else if (startY > bounds.getMaxY()) {
            distanceY = startY - bounds.getMaxY();
        } else {
            distanceY = 0; // Start Y is within the bounds
        }

        // Calculate the Euclidean distance from the start position to the bounds
        return Math.sqrt(distanceX * distanceX + distanceY * distanceY);
    }
}