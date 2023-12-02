package com.example.lensr;

import javafx.application.Platform;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Shape;

import static com.example.lensr.LensrStart.*;

public class CircleMirror extends Circle {
    // The percentage of light that is reflected, 0 - no light is reflected, 1 - perfect reflection
    double reflectivity = 0.8;
    // How much light is scattered instead of reflected, 0 - all light is scattered, 1 - all light is perfectly reflected
    // Not sure if we should implement this as the lower the specular, the less the object behaves like a mirror. Mirrors always have high specular.
    double specular;


    public CircleMirror(double mouseX, double mouseY, int radius) {
        setCenterX(mouseX);
        setCenterY(mouseY);
        setRadius(radius);

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


    public static void removeCircleMirrorIfOverlaps() {
        // Remove the mirror if its size is 0
        if (mirrors.get(mirrors.size() - 1) instanceof CircleMirror circleMirror && circleMirror.getRadius() == 0) {
            root.getChildren().remove(circleMirror);
            mirrors.remove(circleMirror);
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


    public static void scaleCircle(CircleMirror circleMirror) {
        new Thread(() -> {
            while (xPressed) {
                double radius = Math.sqrt(Math.pow(mouseX - circleMirror.getCenterX(), 2) + Math.pow(mouseY - circleMirror.getCenterY(), 2));
                circleMirror.setRadius(radius);

                // Update the UI on the JavaFX application thread
                Platform.runLater(() -> {
                    // Update UI components or perform other UI-related tasks
                    // Example: circle.setRadius(radius);
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

