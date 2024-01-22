package com.example.lensr;

import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Rotate;

import java.util.ArrayList;
import java.util.List;

import static com.example.lensr.LensrStart.*;
import static com.example.lensr.LensrStart.lock;
import static com.example.lensr.MirrorMethods.*;

public class Filter extends Line {
    Group group = new Group();
    Rotate rotate = new Rotate();
    // Extended hitbox for easier editing
    Rectangle hitbox;
    boolean isMouseOnHitbox;
    List<Rectangle> editPoints = new ArrayList<>();
    // The percentage of light that is reflected, 0 - no light is reflected, 1 - perfect reflection
    double rotation = 0;
    boolean isEdited;
    MutableValue isEditPointClicked = new MutableValue(false);
    double passband = 580;
    double peakTransmission = 0.9;
    double FWHM = 20;

    public Filter(double startX, double startY, double endX, double endY) {
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
        passbandSlider.setCurrentSource(this);
        passbandSlider.show();
        peakTransmissionSlider.setCurrentSource(this);
        peakTransmissionSlider.show();
        FWHMSlider.setCurrentSource(this);
        FWHMSlider.show();

        setupObjectEdit();
        isEdited = true;

        // Place edit points
        editPoints.add(new Rectangle(getStartX() - editPointSize / 2, getStartY() - editPointSize / 2, editPointSize,editPointSize));
        editPoints.add(new Rectangle(getEndX() - editPointSize / 2, getEndY() - editPointSize / 2, editPointSize, editPointSize));

        setupEditPoints(editPoints, isEditPointClicked);
        for (Rectangle editPoint : editPoints) {
            editPoint.setOnMousePressed(this::handleEditPointPressed);
            editPoint.setOnMouseReleased(this::executeEditPointRelease);
        }
        group.getChildren().addAll(editPoints);
        editedShape = group;
    }


    private void handleEditPointPressed(MouseEvent event) {
        isMousePressed = true;
        isEditPointClicked.setValue(true);
        scene.setCursor(Cursor.CLOSED_HAND);

        // Scale the mirror with the opposite edit point as an anchor
        //noinspection SuspiciousMethodCalls (it's very sussy)
        Rectangle oppositeEditPoint = editPoints.get(1 - editPoints.indexOf(event.getSource()));
        scale(new Point2D(oppositeEditPoint.getX() + editPointSize / 2, oppositeEditPoint.getY() + editPointSize / 2));
    }

    private void executeEditPointRelease(MouseEvent event) {
        handleEditPointReleased(event, isEditPointClicked, editPoints);
    }


    public void closeObjectEdit() {
        // Hide sliders
        passbandSlider.hide();
        peakTransmissionSlider.hide();
        FWHMSlider.hide();

        isEdited = false;
        if (editPoints != null && editedShape instanceof Group editedGroup) {
            editedGroup.getChildren().removeAll(editPoints);
            editPoints.clear();
        }
        editedShape = null;
    }


    private void createRectangleHitbox() {
        hitbox = new Rectangle();
        hitbox.setHeight(30);
        hitbox.setFill(Color.TRANSPARENT);
        hitbox.setStroke(Color.BLACK);
        hitbox.setStrokeWidth(0);
        hitbox.toBack();
        hitbox.getTransforms().add(rotate);
        hitbox.setOnMouseClicked(this.getOnMouseClicked());
        hitbox.setOnMouseEntered(mouseEvent -> isMouseOnHitbox = true);
        hitbox.setOnMouseExited(mouseEvent -> isMouseOnHitbox = false);
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
                // Resizing standard based on Photoshop and MS Paint :)
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
                    if (isEditPointClicked.getValue()) {
                        editPoints.get(0).setX(getStartX() - editPointSize / 2);
                        editPoints.get(0).setY(getStartY() - editPointSize / 2);
                        editPoints.get(1).setX(getEndX() - editPointSize / 2);
                        editPoints.get(1).setY(getEndY() - editPointSize / 2);
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
}
