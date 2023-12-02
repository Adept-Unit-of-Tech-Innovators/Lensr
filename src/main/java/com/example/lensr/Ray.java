package com.example.lensr;

import javafx.geometry.Bounds;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

import static com.example.lensr.LensrStart.*;

public class Ray extends Line {
    double brightness = 1.0;
    int wavelength;

    public Ray(double startX, double startY, double endX, double endY) {
        setStartX(startX);
        setStartY(startY);
        setEndX(endX);
        setEndY(endY);

        setStroke(Color.RED);
        setStrokeWidth(globalStrokeWidth);

        root.getChildren().add(this);
    }


    public void setWavelength(int wavelength) {
        this.wavelength = wavelength;

        // TODO: change rays color based on the wavelength
    }


    public int getWavelength() {
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
