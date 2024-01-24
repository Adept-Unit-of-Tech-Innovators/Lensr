package com.example.lensr.objects;

import com.example.lensr.MirrorMethods;
import com.example.lensr.MutableValue;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Rotate;

import java.util.ArrayList;
import java.util.List;

import static com.example.lensr.LensrStart.*;
import static com.example.lensr.LensrStart.lock;

public class BeamSource extends Rectangle {
    public List<OriginRay> originRays = new ArrayList<>();
    public MutableValue isEditPointClicked = new MutableValue(false);
    public List<Rectangle> editPoints = new ArrayList<>();
    public Rotate rotate = new Rotate();
    public Group group = new Group();
    public double wavelength = 580;
    public double brightness = 1.0;
    public boolean isEdited;

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

        OriginRay originRay = new OriginRay(
                getCenterX(),
                getCenterY(),
                getCenterX() + Math.cos(angle) * SIZE,
                getCenterY() + Math.sin(angle) * SIZE
        );
        originRay.setParentSource(this);
        originRay.setStrokeWidth(globalStrokeWidth);
        originRay.setWavelength(wavelength);
        originRay.setBrightness(brightness);

        originRays.add(originRay);

        group.getChildren().addAll(originRays);
        root.getChildren().add(group);
        originRays.forEach(Node::toBack);
        update();
    }

    public void update() {
        if (isEdited) {
            return;
        }

        for (OriginRay originRay : originRays) {
            group.getChildren().removeAll(originRay.rayReflections);
            originRay.rayReflections.clear();

            originRay.simulate();
        }
    }

    public void openObjectEdit() {
        wavelengthSlider.setCurrentSource(this);
        wavelengthSlider.show();
        whiteLightToggle.setCurrentSource(this);
        whiteLightToggle.show();

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
                for (OriginRay originRay : originRays) {
                    group.getChildren().removeAll(originRay.rayReflections);
                    originRay.rayReflections.clear();
                }
            });

            while (isMousePressed) {
                double deltaX = mousePos.getX() - getCenterX();
                double deltaY = mousePos.getY() - getCenterY();

                setX(getX() + deltaX);
                setY(getY() + deltaY);

                rotate.setPivotX(getCenterX());
                rotate.setPivotY(getCenterY());

                for (OriginRay originRay : originRays) {
                    originRay.setStartX(getCenterX());
                    originRay.setStartY(getCenterY());

                    originRay.setEndX(getCenterX() + Math.cos(Math.toRadians(rotate.getAngle())) * SIZE);
                    originRay.setEndY(getCenterY() + Math.sin(Math.toRadians(rotate.getAngle())) * SIZE);
                }

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
                for (OriginRay originRay : originRays) {
                    group.getChildren().removeAll(originRay.rayReflections);
                    originRay.rayReflections.clear();
                }
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

                for (OriginRay originRay : originRays) {
                    originRay.setEndX(originRay.getStartX() + Math.cos(angle) * SIZE);
                    originRay.setEndY(originRay.getStartY() + Math.sin(angle) * SIZE);
                }

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
        whiteLightToggle.hide();
        isEdited = false;
        if (editPoints != null && editedShape instanceof Group editedGroup) {
            editedGroup.getChildren().removeAll(editPoints);
            editPoints.clear();
        }
        originRays.forEach(OriginRay::simulate);
    }

    private double getCenterX() {
        return getX() + getWidth() / 2;
    }

    private double getCenterY() {
        return getY() + getHeight() / 2;
    }


    public void setWavelength(double wavelength) {
        this.wavelength = wavelength;
        for (OriginRay originRay : originRays) {
            originRay.setWavelength(wavelength);
        }
    }


    public void setWhiteLight(boolean whiteLight) {
        group.getChildren().removeAll(originRays);
        originRays.forEach(originRay -> {
            group.getChildren().removeAll(originRay.rayReflections);
            originRay.rayReflections.clear();
        });
        originRays.clear();
        if (whiteLight) {
            for (int i = 0; i < whiteLightRayCount; i++) {
                OriginRay originRay = new OriginRay(
                        getCenterX(),
                        getCenterY(),
                        getCenterX() + SIZE * Math.cos(Math.toRadians(rotate.getAngle())),
                        getCenterY() + SIZE * Math.sin(Math.toRadians(rotate.getAngle()))
                );
                originRay.setParentSource(this);
                originRay.setStrokeWidth(globalStrokeWidth);
                originRay.setWavelength(380 + (400.0/whiteLightRayCount*i));
                originRay.setBrightness(brightness);

                originRays.add(originRay);
            }
            group.getChildren().addAll(originRays);
            originRays.forEach(Node::toBack);
        } else {
            OriginRay originRay = new  OriginRay(
                    getCenterX(),
                    getCenterY(),
                    getCenterX() + SIZE * Math.cos(Math.toRadians(rotate.getAngle())),
                    getCenterY() + SIZE * Math.sin(Math.toRadians(rotate.getAngle()))
            );
            originRay.setParentSource(this);
            originRay.setStrokeWidth(globalStrokeWidth);
            originRay.setWavelength(wavelength);
            originRay.setBrightness(brightness);

            originRays.add(originRay);
            group.getChildren().addAll(originRays);
            originRays.forEach(Node::toBack);
        }
        update();
    }


    public double getWavelength() {
        return wavelength;
    }
}
