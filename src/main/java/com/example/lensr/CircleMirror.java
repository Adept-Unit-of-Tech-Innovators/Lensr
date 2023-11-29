package com.example.lensr;

import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class CircleMirror {
    double centreX;
    double centreY;
    double radius;

    public CircleMirror(double mouseX, double mouseY, int radius) {
        this.centreX = mouseX;
        this.centreY = mouseY;
        this.radius = radius;
    }

    public Circle createCircle() {
        Circle circleMirror = new Circle(centreX, centreY, radius);
        circleMirror.setFill(Color.WHITE);
        circleMirror.setStroke(Color.BLACK);
        circleMirror.setStrokeWidth(1);

        return circleMirror;
    }

}

