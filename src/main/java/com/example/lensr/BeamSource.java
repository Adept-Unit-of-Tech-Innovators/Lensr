package com.example.lensr;

import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Rotate;

import java.util.ArrayList;
import java.util.List;

import static com.example.lensr.LensrStart.*;
import static com.example.lensr.LensrStart.lock;

public class BeamSource extends Rectangle {
    OriginRay originRay = new OriginRay(mousePos.getX(), mousePos.getY(), SIZE, mousePos.getY());
    List<Rectangle> editPoints = new ArrayList<>();
    boolean isEdited;
    Group group = new Group();
    MutableValue isEditPointClicked = new MutableValue(false);

    public BeamSource(double x, double y) {
        setX(x);
        setY(y);
        setWidth(100);
        setHeight(50);
        setFill(Color.GRAY);
    }

    public void create() {
        group.getChildren().add(this);
        group.getChildren().add(originRay);
        root.getChildren().add(group);
        toFront();
        update();
    }

    public void update() {
        if (isEdited) {
            return;
        }
        double angleInRadians = Math.toRadians(getRotate()); // Get the current rotation of in radians

        double newEndX = getCenterX() + SIZE * Math.cos(angleInRadians);
        double newEndY = getCenterY() + SIZE * Math.sin(angleInRadians);

        // Update originRay's properties
        originRay.setStartX(getCenterX());
        originRay.setStartY(getCenterY());
        originRay.setEndX(newEndX);
        originRay.setEndY(newEndY);
        originRay.toBack();

        for (Ray ray : originRay.rayReflections) {
            group.getChildren().remove(ray);
        }

        originRay.simulate();

        for (Ray ray : originRay.rayReflections) {
            group.getChildren().add(ray);
        }
    }

    public void openObjectEdit() {
        wavelengthSlider.setCurrentRay(originRay);
        wavelengthSlider.show();

        MirrorMethods.setupObjectEdit();
        isEdited = true;

        // Place edit points
        editPoints.add(new Rectangle(getCenterX() - editPointSize / 2, getCenterY() - editPointSize / 2, editPointSize,editPointSize));
        editPoints.add(new Rectangle(
                getCenterX() + Math.cos(Math.toRadians(getTotalRotation())) * 100 - editPointSize / 2,
                getCenterY() + Math.sin(Math.toRadians(getTotalRotation())) * 100 - editPointSize / 2,
                editPointSize, editPointSize
        ));

        editPoints.get(0).setOnMousePressed(mouseEvent -> {
            isMousePressed = true;
            isEditPointClicked.setValue(true);
            moveToMouse();
        });
        editPoints.get(1).setOnMousePressed(mouseEvent -> {
            isMousePressed = true;
            isEditPointClicked.setValue(true);
            rotateToMouse();
        });
        editPoints.get(0).setOnMouseReleased(this::executeEditPointRelease);
        editPoints.get(1).setOnMouseReleased(this::executeEditPointRelease);

        editPoints.get(0).toFront();
        editPoints.get(1).toFront();

        MirrorMethods.setupEditPoints(editPoints, isEditPointClicked);
        group.getChildren().addAll(editPoints);
        editedShape = group;
    }

    private void moveToMouse() {
        new Thread(() -> {
            Platform.runLater(() -> originRay.rayReflections.clear());

            while (isMousePressed) {

                setTranslateX(mousePos.getX() - getCenterX());
                setTranslateY(mousePos.getY() - getCenterY());

                originRay.setTranslateX(mousePos.getX() - getCenterX());
                originRay.setTranslateY(mousePos.getY() - getCenterY());

                editPoints.get(0).setTranslateX(mousePos.getX() - getCenterX());
                editPoints.get(0).setTranslateY(mousePos.getY() - getCenterY());
                editPoints.get(1).setTranslateX(mousePos.getX() - getCenterX());
                editPoints.get(1).setTranslateY(mousePos.getY() - getCenterY());

                synchronized (lock) {
                    try {
                        lock.wait(10);
                    } catch (InterruptedException e) {
                        throw new RuntimeException("A thread was interrupted while waiting.");
                    }
                }
            }
        }).start();
    }


    private void rotateToMouse() {
        new Thread(() -> {
            Platform.runLater(() -> originRay.rayReflections.clear());
            while (isMousePressed) {
                // Rotate the light source
                Rotate rotate = new Rotate();
                double angle = Math.toDegrees(Math.atan2(mousePos.getY() - getCenterY(), mousePos.getX() - getCenterX()));
                rotate.setAngle(angle - getTotalRotation());
                rotate.setPivotX(getCenterX());
                rotate.setPivotY(getCenterY());

                // Adjust the ray and edit point positions
                getTransforms().add(rotate);
                editPoints.get(1).setX(getCenterX() + Math.cos(Math.toRadians(angle)) * 100 - editPointSize / 2);
                editPoints.get(1).setY(getCenterY() + Math.sin(Math.toRadians(angle)) * 100 - editPointSize / 2);
                originRay.getTransforms().add(rotate);

                synchronized (lock) {
                    try {
                        lock.wait(10);
                    } catch (InterruptedException e) {
                        throw new RuntimeException("A thread was interrupted while waiting.");
                    }
                }
            }
        }).start();
    }

    private void executeEditPointRelease(MouseEvent event) {
        MirrorMethods.handleEditPointReleased(event, isEditPointClicked, editPoints);
    }


    public void closeObjectEdit() {
        wavelengthSlider.hide();
        isEdited = false;
        if (editPoints != null && editedShape instanceof Group editedGroup) {
            editedGroup.getChildren().removeAll(editPoints);
            editPoints.clear();
        }
        originRay.simulate();
    }

    private double getCenterX() {
        return getX() + getWidth() / 2;
    }

    private double getCenterY() {
        return getY() + getHeight() / 2;
    }

    private double getTotalRotation() {
        double totalRotation = getRotate();

        for (var transform : getTransforms()) {
            if (transform instanceof Rotate) {
                totalRotation += ((Rotate) transform).getAngle();
            }
        }

        return totalRotation;
    }
}
