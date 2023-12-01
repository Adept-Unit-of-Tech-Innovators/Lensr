package com.example.lensr;

import javafx.scene.shape.Line;

import static com.example.lensr.LensrStart.*;

public class Ray extends Line {
    double brightness;
    int wavelength;
    public Ray(double startX, double startY, double endX, double endY) {
        setStartX(startX);
        setStartY(startY);
        setEndX(endX);
        setEndY(endY);

        root.getChildren().add(this);
    }


    public void setWavelength(int wavelength) {
        this.wavelength = wavelength;

        // TODO: change rays color based on the wavelength
    }


    public int getWavelength() {
        return wavelength;
    }


    public void setBrightness(double brightness) {
        this.brightness = brightness;
    }


    public double getBrightness() {
        return brightness;
    }
}
