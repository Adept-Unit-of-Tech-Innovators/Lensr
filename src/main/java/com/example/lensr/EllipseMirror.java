package com.example.lensr;

import javafx.application.Platform;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Shape;

import static com.example.lensr.LensrStart.*;

public class EllipseMirror extends Ellipse {
    // The percentage of light that is reflected, 0 - no light is reflected, 1 - perfect reflection
    double reflectivity = 0.8;
    // How much light is scattered instead of reflected, 0 - all light is scattered, 1 - all light is perfectly reflected
    // Not sure if we should implement this as the lower the specular, the less the object behaves like a mirror. Mirrors always have high specular.
    double specular;


    public EllipseMirror(double mouseX, double mouseY, int radiusX, int radiusY) {
        setCenterX(mouseX);
        setCenterY(mouseY);
        setRadiusX(radiusX);
        setRadiusY(radiusY);

        setFill(Color.TRANSPARENT);
        setStroke(mirrorColor);
        setStrokeWidth(globalStrokeWidth);

        root.getChildren().add(this);
    }


    public void setReflectivity(double reflectivity) {
        this.reflectivity = reflectivity;
    }


    public double getReflectivity() {
        return reflectivity;
    }


    public static void removeEllipseMirrorIfOverlaps() {
        // Remove the mirror if its size is 0
        if (mirrors.get(mirrors.size() - 1) instanceof EllipseMirror ellipseMirror &&
                ellipseMirror.getRadiusX() == 0 &&
                ellipseMirror.getRadiusY() == 0
        ) {
            root.getChildren().remove(ellipseMirror);
            mirrors.remove(ellipseMirror);
            return;
        }

        for (Object mirror : mirrors) {
            if (mirror.equals(mirrors.get(mirrors.size() - 1))) break;

            if (mirror instanceof Shape mirrorShape && mirrors.get(mirrors.size() - 1) instanceof Shape newMirror) {

                // If the mirror overlaps with another object, remove it
                if (Shape.intersect(newMirror, mirrorShape).getLayoutBounds().getWidth() >= 0) {
                    root.getChildren().remove(newMirror);
                    mirrors.remove(newMirror);
                    return;
                }
            }
        }
    }


    public void scaleEllipse() {
        new Thread(() -> {
            while (xPressed) {

                if (shiftPressed) {
                    double radius = Math.sqrt(Math.pow(mouseX - getCenterX(), 2) + Math.pow(mouseY - getCenterY(), 2));
                    setRadiusX(radius);
                    setRadiusY(radius);
                } else {
                    double radiusX = Math.abs(mouseX - getCenterX());
                    double radiusY = Math.abs(mouseY - getCenterY());
                    setRadiusX(radiusX);
                    setRadiusY(radiusY);
                }

                // Update the UI on the JavaFX application thread
                Platform.runLater(() -> {
                    // Update UI components or perform other UI-related tasks
                });

                synchronized (lock) {
                    try {
                        lock.wait(10); // Adjust the wait time as needed
                    } catch (InterruptedException e) {
                        throw new RuntimeException("A thread was interrupted while waiting.");
                    }
                }
            }
        }).start();
    }
}

