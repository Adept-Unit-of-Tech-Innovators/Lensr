package com.example.lensr.objects;

import com.example.lensr.EditPoint;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Rotate;

import java.util.ArrayList;
import java.util.List;

import static com.example.lensr.LensrStart.*;
import static com.example.lensr.LensrStart.lock;
import static com.example.lensr.MirrorMethods.*;

public class GaussianRolloffFilter extends Line {
    public Group group = new Group();
    Rotate rotate = new Rotate();
    // Extended hitbox for easier editing
    Rectangle hitbox;
    public List<EditPoint> objectEditPoints = new ArrayList<>();
    double rotation = 0;
    public boolean isEdited;
    public boolean hasBeenClicked;
    double passband = 580;
    double peakTransmission = 0.8;
    double FWHM = 20;

    public GaussianRolloffFilter(double startX, double startY, double endX, double endY) {
        setStartX(startX);
        setStartY(startY);
        setEndX(endX);
        setEndY(endY);
    }


    public void create() {
        setFill(Color.TRANSPARENT);
        setStroke(mirrorColor);
        setStrokeWidth(globalStrokeWidth);

        createRectangleHitbox();

        group.getChildren().add(this);
        group.getChildren().add(hitbox);
        root.getChildren().add(group);
    }

    public void openObjectEdit() {
        // Setup sliders
        peakTransmissionSlider.setCurrentSource(this);
        passbandSlider.setCurrentSource(this);
        FWHMSlider.setCurrentSource(this);
        peakTransmissionSlider.show();
        passbandSlider.show();
        FWHMSlider.show();

        // Defocus the text field
        root.requestFocus();

        hasBeenClicked = true;
        isEdited = true;

        // Place edit points
        objectEditPoints.add(new EditPoint(getStartX(), getStartY()));
        objectEditPoints.add(new EditPoint(getEndX(), getEndY()));

        // Define what happens when an edit point is clicked
        for (EditPoint editPoint : objectEditPoints) {
            editPoint.setOnClickEvent(event -> {
                // Scale the mirror with the opposite edit point as an anchor
                EditPoint oppositeEditPoint = objectEditPoints.get(1 - objectEditPoints.indexOf(editPoint));
                scale(oppositeEditPoint.getCenter());
            });
        }

        editPoints.addAll(objectEditPoints);
        group.getChildren().addAll(objectEditPoints);
        editedShape = group;
    }


    public void closeObjectEdit() {
        passbandSlider.hide();
        peakTransmissionSlider.hide();
        FWHMSlider.hide();

        isEdited = false;
        if (objectEditPoints != null && editedShape instanceof Group editedGroup) {
            editedGroup.getChildren().removeAll(objectEditPoints);
            editPoints.removeAll(objectEditPoints);
            objectEditPoints.clear();
        }
        editedShape = null;
        updateLightSources();
    }


    private void createRectangleHitbox() {
        hitbox = new Rectangle();
        hitbox.setHeight(30);
        hitbox.setFill(Color.TRANSPARENT);
        hitbox.setStroke(Color.BLACK);
        hitbox.setStrokeWidth(0);
        hitbox.toBack();
        hitbox.getTransforms().add(rotate);
        updateHitbox();
    }


    private void updateHitbox() {
        hitbox.setY(getStartY() - hitbox.getHeight() / 2);
        hitbox.setX(getStartX() - hitbox.getHeight() / 2);
        rotate.setPivotX(getStartX());
        rotate.setPivotY(getStartY());
        rotation = Math.toDegrees(Math.atan2(getEndY() - getStartY(), getEndX() - getStartX()));
        hitbox.setWidth(getLength() + hitbox.getHeight());
        rotate.setAngle(rotation);
    }

    public void setPeakTransmission(double peakTransmission) {
        this.peakTransmission = peakTransmission;
    }


    public double getPeakTransmission() {
        return peakTransmission;
    }


    public double getLength() {
        return Math.sqrt(
                Math.pow( getEndX() - getStartX(), 2) +
                        Math.pow(getEndY() -getStartY(), 2)
        );
    }


    public void scale(Point2D anchor) {
        new Thread(() -> {
            double startX, startY, endX, endY;

            while (isMousePressed) {

                if (altPressed && shiftPressed) {
                    // Shift-mode calculations for actually half the mirror
                    double deltaX = mousePos.getX() - anchor.getX();
                    double deltaY = mousePos.getY() - anchor.getY();
                    double distance = Math.sqrt(Math.pow(deltaX, 2) + Math.pow(deltaY, 2));
                    double angle = Math.atan2(deltaY, deltaX);
                    double shiftedAngle = Math.round(angle * 4 / Math.PI) * Math.PI / 4;
                    double snappedX = anchor.getX() + distance * Math.cos(shiftedAngle);
                    double snappedY = anchor.getY() + distance * Math.sin(shiftedAngle);

                    // Alt-mode calculations to determine the "other half of the mirror"
                    startX = anchor.getX() - (snappedX - anchor.getX());
                    startY = anchor.getY() - (snappedY - anchor.getY());
                    endX = snappedX;
                    endY = snappedY;
                }
                else if (altPressed) {
                    // Calculate first because funny java threading
                    startX = anchor.getX() - (mousePos.getX() - anchor.getX());
                    startY = anchor.getY() - (mousePos.getY() - anchor.getY());
                    endX = mousePos.getX();
                    endY = mousePos.getY();
                }
                else if (shiftPressed) {
                    startX = anchor.getX();
                    startY = anchor.getY();
                    double deltaX = mousePos.getX() - startX;
                    double deltaY = mousePos.getY() - startY;
                    double distance = Math.sqrt(Math.pow(deltaX, 2) + Math.pow(deltaY, 2));
                    double angle = Math.atan2(deltaY, deltaX);
                    double shiftedAngle = Math.round(angle * 4 / Math.PI) * Math.PI / 4;
                    endX = startX + distance * Math.cos(shiftedAngle);
                    endY = startY + distance * Math.sin(shiftedAngle);
                }
                else {
                    startX = anchor.getX();
                    startY = anchor.getY();
                    endX = mousePos.getX();
                    endY = mousePos.getY();
                }

                double finalStartX = startX;
                double finalStartY = startY;
                double finalEndX = endX;
                double finalEndY = endY;

                // Update the UI on the JavaFX application thread
                Platform.runLater(() -> {
                    this.setStartX(finalStartX);
                    this.setStartY(finalStartY);
                    this.setEndX(finalEndX);
                    this.setEndY(finalEndY);

                    // Update editPoints location
                    if (!objectEditPoints.isEmpty()) {
                        objectEditPoints.get(0).setCenterX(getStartX());
                        objectEditPoints.get(0).setCenterY(getStartY());
                        objectEditPoints.get(1).setCenterX(getEndX());
                        objectEditPoints.get(1).setCenterY(getEndY());
                    }

                    updateHitbox();
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

    public double getPassband() {
        return passband;
    }


    public void setPassband(double passband) {
        this.passband = passband;

        double factor;
        double red;
        double green;
        double blue;

        int intensityMax = 255;
        double Gamma = 0.8;

        // adjusting to transform between different colors for example green and yellow with addition of red and absence of blue
        // what
        if ((passband >= 380) && (passband < 440)) {
            red = -(passband - 440.0) / (440.0 - 380.0);
            green = 0.0;
            blue = 1.0;
        } else if ((passband >= 440) && (passband < 490)) {
            red = 0.0;
            green = (passband - 440.0) / (490.0 - 440.0);
            blue = 1.0;
        } else if ((passband >= 490) && (passband < 510)) {
            red = 0.0;
            green = 1.0;
            blue = -(passband - 510.0) / (510.0 - 490.0);
        } else if ((passband >= 510) && (passband < 580)) {
            red = (passband - 510.0) / (580.0 - 510.0);
            green = 1.0;
            blue = 0.0;
        } else if ((passband >= 580) && (passband < 645)) {
            red = 1.0;
            green = -(passband - 645.0) / (645.0 - 580.0);
            blue = 0.0;
        } else if ((passband >= 645) && (passband < 781)) {
            red = 1.0;
            green = 0.0;
            blue = 0.0;
        } else {
            red = 0.0;
            green = 0.0;
            blue = 0.0;
        }
        // Let the intensity fall off near the vision limits
        if ((passband >= 380) && (passband < 420)) {
            factor = 0.3 + 0.7 * (passband - 380) / (420 - 380);
        } else if ((passband >= 420) && (passband < 701)) {
            factor = 1.0;
        }
        else if ((passband >= 701) && (passband < 781)) {
            factor = 0.3 + 0.7 * (780 - passband) / (780 - 700);
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


    public double getFWHM() {
        return FWHM;
    }


    public void setFWHM(double FWHM) {
        this.FWHM = FWHM;
    }

    public boolean isMouseOnHitbox() {
        // Get the mouse position in the scene's coordinate system
        Point2D mousePosInScene = mousePos;

        // Transform the mouse position to the rectangle's local coordinate system
        Point2D mousePosInRectangle = hitbox.sceneToLocal(mousePosInScene);

        // Check if the transformed mouse position is within the rectangle's bounds
        return hitbox.getBoundsInLocal().contains(mousePosInRectangle);
    }
}
