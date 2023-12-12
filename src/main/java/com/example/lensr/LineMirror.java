package com.example.lensr;

import javafx.application.Platform;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Shape;

import static com.example.lensr.LensrStart.*;

public class LineMirror extends Line{
    // The percentage of light that is reflected, 0 - no light is reflected, 1 - perfect reflection
    double reflectivity = 1;
    // How much light is scattered instead of reflected, 0 - all light is scattered, 1 - all light is perfectly reflected
    // Not sure if we should implement this as the lower the specular, the less the object behaves like a mirror. Mirrors always have high specular.
    double specular;


    public LineMirror(double mouseX, double mouseY) {
        setStartX(mouseX);
        setStartY(mouseY);
        setEndX(mouseX);
        setEndY(mouseY);
    }


    public void createMirror() {
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


    public double getLength() {
        return Math.sqrt(
                Math.pow( getEndX() - getStartX(), 2) +
                        Math.pow(getEndY() -getStartY(), 2)
        );
    }



    public void removeIfOverlaps() {
        // Remove the mirror if its size is 0
        if (this.getLength() == 0) {
            root.getChildren().remove(this);
            mirrors.remove(this);
            return;
        }

        for (Object mirror : mirrors) {
            if (mirror.equals(this)) continue;

            if (mirror instanceof Shape mirrorShape) {
                // If the mirror overlaps with another object, remove it
                if (Shape.intersect(this , mirrorShape).getLayoutBounds().getWidth() >= 0) {
                    root.getChildren().remove(this);
                    mirrors.remove(this);
                    return;
                }
            }
        }
    }

    public void scale() {
        new Thread(() -> {
            while (isMousePressed) {
                if (altPressed && shiftPressed) {
                    // Shift-mode calculations for actually half the mirror
                    double middleX = (this.getStartX() + this.getEndX()) / 2, middleY = (this.getStartY() + this.getEndY()) / 2;
                    double deltaX = mouseX - middleX;
                    double deltaY = mouseY - middleY;
                    double distance = Math.sqrt(Math.pow(deltaX, 2) + Math.pow(deltaY, 2));
                    double angle = Math.atan2(deltaY, deltaX);
                    double shiftedAngle = Math.round(angle * 4 / Math.PI) * Math.PI / 4;
                    double snappedX = middleX + distance * Math.cos(shiftedAngle);
                    double snappedY = middleY + distance * Math.sin(shiftedAngle);
                    // Alt-mode calculations to determine the "other half of the mirror"
                    double startX = middleX-(snappedX-middleX), startY = middleY-(snappedY-middleY), endX = snappedX, endY = snappedY;
                    // Apply all at once to avoid delays
                    this.setStartX(startX);
                    this.setStartY(startY);
                    this.setEndX(endX);
                    this.setEndY(endY);
                }
                else if (altPressed) {
                    // Calculate first because funny java threading
                    double middleX = (this.getStartX() + this.getEndX()) / 2, middleY = (this.getStartY() + this.getEndY()) / 2;
                    double startX = middleX-(mouseX-middleX), startY = middleY-(mouseY-middleY), endX = mouseX, endY = mouseY;
                    // Apply all at once to avoid delays
                    this.setStartX(startX);
                    this.setStartY(startY);
                    this.setEndX(endX);
                    this.setEndY(endY);
                }
                else if (shiftPressed) {
                    double deltaX = mouseX - this.getStartX();
                    double deltaY = mouseY - this.getStartY();
                    double distance = Math.sqrt(Math.pow(deltaX, 2) + Math.pow(deltaY, 2));
                    double angle = Math.atan2(deltaY, deltaX);
                    double shiftedAngle = Math.round(angle * 4 / Math.PI) * Math.PI / 4;
                    this.setEndX(this.getStartX() + distance * Math.cos(shiftedAngle));
                    this.setEndY(this.getStartY() + distance * Math.sin(shiftedAngle));
                }
                else {
                    this.setEndX(mouseX);
                    this.setEndY(mouseY);
                }

                // Update the UI on the JavaFX application thread
                Platform.runLater(() -> {
                    // Update UI components or perform other UI-related tasks
                    // Example: circle.setRadius(radius);
                });
                synchronized (lock) {
                    try {
                        lock.wait(10); // Adjust the sleep time as needed
                    } catch (InterruptedException e) {
                        throw new RuntimeException("A thread was interrupted while waiting.");
                    }
                }
            }
        }).start();
    }

}



