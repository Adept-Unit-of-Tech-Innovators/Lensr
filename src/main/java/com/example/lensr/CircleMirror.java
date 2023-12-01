package com.example.lensr;

import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Shape;

import static com.example.lensr.LensrStart.*;

public class CircleMirror extends Circle {
    // The percentage of light that is reflected, 0 - no light is reflected, 1 - perfect reflection
    double reflectivity;
    // How much light is scattered instead of reflected, 0 - all light is scattered, 1 - all light is perfectly reflected
    // Not sure if we should implement this as the lower the specular, the less the object behaves like a mirror. Mirrors always have high specular.
    double specular;

    public CircleMirror(double mouseX, double mouseY, int radius) {
        setCenterX(mouseX);
        setCenterY(mouseY);
        setRadius(radius);

        setFill(Color.TRANSPARENT);
        setStroke(Color.BLACK);
        setStrokeWidth(0.5);

        root.getChildren().add(this);
    }


    public static void removeCircleMirrorIfOverlaps() {
        if (mirrors.get(mirrors.size() - 1) instanceof CircleMirror circleMirror && circleMirror.getRadius() == 0) {
            root.getChildren().remove(circleMirror);
            mirrors.remove(circleMirror);
            return;
        }
        for (Object mirror : mirrors) {
            if (mirror.equals(mirrors.get(mirrors.size() - 1))) break;

            if ((mirror instanceof Shape mirrorShape) && (mirrors.get(mirrors.size()-1) instanceof Shape newMirror)) {
                if (Shape.intersect(newMirror, mirrorShape).getBoundsInLocal().getWidth() >= 0) {
                    root.getChildren().remove(newMirror);
                    mirrors.remove(newMirror);
                    break;
                }
            }
        }
    }
}

