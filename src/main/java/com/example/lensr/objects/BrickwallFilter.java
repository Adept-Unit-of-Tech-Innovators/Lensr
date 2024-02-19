package com.example.lensr.objects;

import com.example.lensr.EditPoint;
import com.example.lensr.Graph;
import com.example.lensr.UserControls;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.transform.Rotate;

import java.util.ArrayList;
import java.util.List;

import static com.example.lensr.LensrStart.*;
import static com.example.lensr.LensrStart.lock;
import static com.example.lensr.MirrorMethods.*;

public class BrickwallFilter extends Line implements Editable {
    public Group group = new Group();
    Rotate rotate = new Rotate();
    // Extended hitbox for easier editing
    Rectangle hitbox;
    public List<EditPoint> objectEditPoints = new ArrayList<>();
    public Graph graph;
    double rotation = 0;
    public boolean isEdited;
    public boolean hasBeenClicked;
    double startPassband = 480;
    double endPassband = 680;
    double transmission = 0.8;

    public BrickwallFilter(double startX, double startY, double endX, double endY) {
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
        graph = new Graph(700, 100, 200, 150);
        graph.setDataSource(this);

        group.getChildren().add(this);
        group.getChildren().add(hitbox);
        group.getChildren().add(graph.group);
        root.getChildren().add(group);
    }

    @Override
    public void delete() {
        mirrors.remove(this);
        root.getChildren().remove(group);
    }

    @Override
    public void copy() {
        BrickwallFilter brickwallFilter = new BrickwallFilter(getStartX(), getStartY(), getEndX(), getEndY());
        brickwallFilter.setTransmission(transmission);
        brickwallFilter.setStartPassband(startPassband);
        brickwallFilter.setEndPassband(endPassband);
        brickwallFilter.create();
        brickwallFilter.moveBy(10, 10);
        mirrors.add(brickwallFilter);
        UserControls.closeCurrentEdit();
        brickwallFilter.openObjectEdit();
    }

    @Override
    public void openObjectEdit() {
        // Setup sliders
        peakTransmissionSlider.setCurrentSource(this);
        startPassbandSlider.setCurrentSource(this);
        endPassbandSlider.setCurrentSource(this);
        peakTransmissionSlider.show();
        startPassbandSlider.show();
        endPassbandSlider.show();

        graph.drawGraph();
        graph.show();

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

        objectEditPoints.add(new EditPoint((getStartX() + getEndX()) / 2, (getStartY() + getEndY()) / 2));
        objectEditPoints.get(2).setOnClickEvent(event -> move());

        editPoints.addAll(objectEditPoints);
        group.getChildren().addAll(objectEditPoints);
        editedShape = group;
    }

    @Override
    public void closeObjectEdit() {
        // Hide sliders
        peakTransmissionSlider.hide();
        startPassbandSlider.hide();
        endPassbandSlider.hide();

        graph.clear();
        graph.hide();

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


    public double getLength() {
        return Math.sqrt(
                Math.pow(getEndX() - getStartX(), 2) +
                        Math.pow(getEndY() - getStartY(), 2)
        );
    }

    @Override
    public void moveBy(double x, double y) {
        setStartX(getStartX() + x);
        setStartY(getStartY() + y);
        setEndX(getEndX() + x);
        setEndY(getEndY() + y);

        objectEditPoints.forEach(editPoint -> {
            editPoint.setCenterX(editPoint.getCenterX() + x);
            editPoint.setCenterY(editPoint.getCenterY() + y);
        });
        updateHitbox();
    }

    private void move() {
        new Thread(() -> {
            Point2D prevMousePos = mousePos;
            Point2D prevStart = new Point2D(getStartX(), getStartY());
            Point2D prevEnd = new Point2D(getEndX(), getEndY());

            while (isMousePressed && isEdited) {
                double deltaX = mousePos.getX() - prevMousePos.getX();
                double deltaY = mousePos.getY() - prevMousePos.getY();

                // Update the UI on the JavaFX application thread
                Platform.runLater(() -> {
                    this.setStartX(prevStart.getX() + deltaX);
                    this.setStartY(prevStart.getY() + deltaY);
                    this.setEndX(prevEnd.getX() + deltaX);
                    this.setEndY(prevEnd.getY() + deltaY);

                    // Update editPoints location
                    if (!objectEditPoints.isEmpty()) {
                        objectEditPoints.get(0).setCenterX(getStartX());
                        objectEditPoints.get(0).setCenterY(getStartY());
                        objectEditPoints.get(1).setCenterX(getEndX());
                        objectEditPoints.get(1).setCenterY(getEndY());
                        objectEditPoints.get(2).setCenterX((getStartX() + getEndX()) / 2);
                        objectEditPoints.get(2).setCenterY((getStartY() + getEndY()) / 2);
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

    public void scale(Point2D anchor) {
        new Thread(() -> {
            double startX, startY, endX, endY;

            while (isMousePressed && isEdited) {
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
                        objectEditPoints.get(2).setCenterX((getStartX() + getEndX()) / 2);
                        objectEditPoints.get(2).setCenterY((getStartY() + getEndY()) / 2);
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


    private void setWavelengthColor(double passband) {
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


    public void setTransmission(double transmission) {
        this.transmission = transmission;
    }


    public double getTransmission() {
        return transmission;
    }


    public void setStartPassband(double startPassband) {
        this.startPassband = startPassband;
        setWavelengthColor((startPassband + endPassband) / 2);
    }


    public double getStartPassband() {
        return startPassband;
    }


    public void setEndPassband(double endPassband) {
        this.endPassband = endPassband;
        setWavelengthColor((startPassband + endPassband) / 2);
    }


    public double getEndPassband() {
        return endPassband;
    }

    @Override
    public void setHasBeenClicked(boolean hasBeenClicked) {
        this.hasBeenClicked = hasBeenClicked;
    }

    @Override
    public boolean getHasBeenClicked() {
        return hasBeenClicked;
    }

    @Override
    public boolean intersectsMouseHitbox() {
        return Shape.intersect(this, mouseHitbox).getLayoutBounds().getWidth() != -1;
    }
}