package com.example.lensr;

import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;

import java.util.ArrayList;
import java.util.List;

import static com.example.lensr.LensrStart.*;
import static com.example.lensr.MirrorMethods.*;

public class LightEater extends Circle {
    Group group = new Group();
    List<Rectangle> editPoints = new ArrayList<>();
    boolean isEdited;
    MutableValue isEditPointClicked = new MutableValue(false);


    public LightEater(double centerX, double centerY, double radius) {
        setCenterX(centerX);
        setCenterY(centerY);
        setRadius(radius);
    }


    public void create() {
        setFill(Color.BLACK);
        setStroke(Color.BLACK);
        setStrokeWidth(globalStrokeWidth);
        setStrokeType(StrokeType.OUTSIDE);

        setOnMouseClicked(mouseEvent -> {
            if (isEditMode && !isEdited) openObjectEdit();
        });

        group.getChildren().add(this);
        root.getChildren().add(group);
    }


    public void openObjectEdit() {
        setupObjectEdit();
        isEdited = true;

        // Place edit points
        Bounds mirrorBounds = getLayoutBounds();
        editPoints.add(new Rectangle(mirrorBounds.getMinX() - editPointSize / 2, mirrorBounds.getMinY() - editPointSize / 2, editPointSize, editPointSize));
        editPoints.add(new Rectangle(mirrorBounds.getMaxX() - editPointSize / 2, mirrorBounds.getMinY() - editPointSize / 2, editPointSize, editPointSize));
        editPoints.add(new Rectangle(mirrorBounds.getMaxX() - editPointSize / 2, mirrorBounds.getMaxY() - editPointSize / 2, editPointSize, editPointSize));
        editPoints.add(new Rectangle(mirrorBounds.getMinX() - editPointSize / 2, mirrorBounds.getMaxY() - editPointSize / 2, editPointSize, editPointSize));

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
        Rectangle oppositeEditPoint = editPoints.get(( (editPoints.indexOf(event.getSource()) + 2)  % 4));
        scale(new Point2D(oppositeEditPoint.getX() + editPointSize / 2, oppositeEditPoint.getY() + editPointSize / 2));
    }


    private void executeEditPointRelease(MouseEvent event) {
        handleEditPointReleased(event, isEditPointClicked, editPoints);
    }


    public void closeObjectEdit() {
        isEdited = false;
        if (editPoints != null && editedShape instanceof Group editedGroup) {
            editedGroup.getChildren().removeAll(editPoints);
            editPoints.clear();
        }
    }


    public void scale(Point2D anchor) {
        new Thread(() -> {
            double centerX, centerY, radius;

            while (isMousePressed) {
                // Resizing standard based on Photoshop and MS Paint :)
                if (altPressed) {
                    centerX = anchor.getX();
                    centerY = anchor.getY();
                    radius  = Math.min( Math.abs(anchor.getX() - mousePos.getX()), Math.abs(anchor.getY() - mousePos.getY()) );
                }
                else {
                    double minDistance = Math.min( Math.abs(anchor.getX() - mousePos.getX()), Math.abs(anchor.getY() - mousePos.getY()) ) / 2;
                    centerX = anchor.getX() + (mousePos.getX() > anchor.getX() ? minDistance : -minDistance);
                    centerY = anchor.getY() + (mousePos.getY() > anchor.getY() ? minDistance : -minDistance);
                    radius = Math.min( Math.abs(centerX - mousePos.getX()), Math.abs(centerY - mousePos.getY()) );
                }

                double finalCenterX = centerX;
                double finalCenterY = centerY;
                double finalRadius = radius;

                // Update the UI on the JavaFX application thread
                Platform.runLater(() -> {
                    setCenterX(finalCenterX);
                    setCenterY(finalCenterY);
                    setRadius(finalRadius);

                    // Update editPoints location
                    if (isEditPointClicked.getValue()) {
                        Bounds mirrorBounds = getLayoutBounds();
                        int offset = -4;

                        for (int i = 0; i < editPoints.size(); i++) {
                            double x = (i == 1 || i == 2) ? mirrorBounds.getMaxX() : mirrorBounds.getMinX();
                            double y = (i == 2 || i == 3) ? mirrorBounds.getMaxY() : mirrorBounds.getMinY();

                            editPoints.get(i).setX(x + offset);
                            editPoints.get(i).setY(y + offset);
                        }
                    }
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