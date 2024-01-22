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
    MutableValue isEditPointClicked = new MutableValue(false);
    List<Rectangle> editPoints = new ArrayList<>();
    Rotate rotate = new Rotate();
    Group group = new Group();
    double wavelength = 580;
    double brightness = 1.0;
    boolean isEdited;

    public BeamSource(double x, double y) {
        setX(x);
        setY(y);
        setWidth(100);
        setHeight(50);
        setFill(Color.GRAY);
        getTransforms().add(rotate);
    }

    public void create() {
        group.getChildren().add(this);

        double angle = Math.toRadians(getRotate()); // Get the current rotation of in radians

        originRay.setParentSource(this);
        originRay.setStartX(getCenterX());
        originRay.setStartY(getCenterY());
        originRay.setEndX(getCenterX() + SIZE * Math.cos(angle));
        originRay.setEndY(getCenterY() + SIZE * Math.sin(angle));
        originRay.setStrokeWidth(globalStrokeWidth);
        originRay.setWavelength(wavelength);
        originRay.setBrightness(brightness);

        group.getChildren().add(originRay);
        root.getChildren().add(group);
        toFront();
        update();
    }

    public void update() {
        if (isEdited) {
            return;
        }

        group.getChildren().removeAll(originRay.rayReflections);
        originRay.rayReflections.clear();

        originRay.simulate();
    }

    public void openObjectEdit() {
        wavelengthSlider.setCurrentSource(this);
        wavelengthSlider.show();

        MirrorMethods.setupObjectEdit();
        isEdited = true;

        // Place edit points
        editPoints.add(new Rectangle(getCenterX() - editPointSize / 2, getCenterY() - editPointSize / 2, editPointSize,editPointSize));
        editPoints.add(new Rectangle(
                getCenterX() + Math.cos(Math.toRadians(rotate.getAngle())) * 100 - editPointSize / 2,
                getCenterY() + Math.sin(Math.toRadians(rotate.getAngle())) * 100 - editPointSize / 2,
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
            Platform.runLater(() -> {
                group.getChildren().removeAll(originRay.rayReflections);
                originRay.rayReflections.clear();
            });

            while (isMousePressed) {
                double deltaX = mousePos.getX() - getCenterX();
                double deltaY = mousePos.getY() - getCenterY();

                setX(getX() + deltaX);
                setY(getY() + deltaY);

                rotate.setPivotX(getCenterX());
                rotate.setPivotY(getCenterY());

                originRay.setStartX(getCenterX());
                originRay.setStartY(getCenterY());

                originRay.setEndX(getCenterX() + Math.cos(Math.toRadians(rotate.getAngle())) * SIZE);
                originRay.setEndY(getCenterY() + Math.sin(Math.toRadians(rotate.getAngle())) * SIZE);

                editPoints.get(0).setX(editPoints.get(0).getX() + deltaX);
                editPoints.get(0).setY(editPoints.get(0).getY() + deltaY);
                editPoints.get(1).setX(editPoints.get(1).getX() + deltaX);
                editPoints.get(1).setY(editPoints.get(1).getY() + deltaY);

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
            Platform.runLater(() -> {
                group.getChildren().removeAll(originRay.rayReflections);
                originRay.rayReflections.clear();
            });

            while (isMousePressed) {
                double angle = Math.atan2(mousePos.getY() - getCenterY(), mousePos.getX() - getCenterX());

                // Rotate the light source
                rotate.setAngle(Math.toDegrees(angle));
                rotate.setPivotX(getCenterX());
                rotate.setPivotY(getCenterY());

                // Adjust the ray and edit point positions
                editPoints.get(1).setX(getCenterX() + Math.cos(angle) * 100 - editPointSize / 2);
                editPoints.get(1).setY(getCenterY() + Math.sin(angle) * 100 - editPointSize / 2);

                originRay.setEndX(originRay.getStartX() + Math.cos(angle) * SIZE);
                originRay.setEndY(originRay.getStartY() + Math.sin(angle) * SIZE);

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


    public void setWavelength(double wavelength) {
        this.wavelength = wavelength;
        originRay.setWavelength(wavelength);
    }


    public double getWavelength() {
        return wavelength;
    }
}
