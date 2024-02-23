package com.example.lensr.objects;

import com.example.lensr.EditPoint;
import com.example.lensr.SaveState;
import com.example.lensr.UserControls;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.transform.Rotate;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static com.example.lensr.LensrStart.*;
import static com.example.lensr.LensrStart.lock;
import static com.example.lensr.MirrorMethods.updateLightSources;

public class BeamSource extends Rectangle implements Editable, Serializable {
    private transient Group group = new Group();
    private transient List<OriginRay> originRays = new ArrayList<>();
    private transient List<EditPoint> objectEditPoints = new ArrayList<>();
    private transient Rotate rotate = new Rotate();
    private double rotation = 0;
    private transient boolean hasBeenClicked;
    private transient boolean isEdited;
    private double wavelength = 580;
    private boolean whiteLight;
    private double brightness = 1.0;

    public BeamSource(double centerX, double centerY) {
        setCenterX(centerX);
        setCenterY(centerY);
        setWidth(100);
        setHeight(50);
    }

    @Override
    public void create() {
        setFill(Color.GRAY);
        toBack();
        rotate.setPivotX(getCenterX());
        rotate.setPivotY(getCenterY());
        rotate.setAngle(Math.toDegrees(rotation));
        getTransforms().add(rotate);

        int rayCount = whiteLight ? whiteLightRayCount : 1;
        for (int i = 0; i < rayCount; i++) {
            OriginRay originRay = new OriginRay(
                    getCenterX() + Math.cos(rotation) * getWidth() / 2,
                    getCenterY() + Math.sin(rotation) * getWidth() / 2,
                    getCenterX() + SIZE * Math.cos(rotation),
                    getCenterY() + SIZE * Math.sin(rotation)
            );
            originRay.setParentSource(this);
            originRay.setStrokeWidth(globalStrokeWidth);
            originRay.setWavelength(whiteLight ? 380 + (400.0 / whiteLightRayCount * i) : wavelength);
            originRay.setBrightness(brightness);

            originRays.add(originRay);
        }

        group.getChildren().add(this);
        originRays.forEach(ray -> group.getChildren().add(ray.group));
        root.getChildren().add(group);
        originRays.forEach(Node::toBack);
    }

    @Override
    public void delete() {
        lightSources.remove(this);
        root.getChildren().remove(group);
    }

    @Override
    public void copy() {
        BeamSource newLightSource = new BeamSource(getCenterX() - getWidth() / 2, getCenterY() - getHeight() / 2);
        newLightSource.setWavelength(wavelength);
        newLightSource.rotate.setPivotX(rotate.getPivotX());
        newLightSource.rotate.setPivotY(rotate.getPivotY());
        newLightSource.rotate.setAngle(rotate.getAngle());
        newLightSource.getTransforms().addAll(newLightSource.rotate);
        // Reset the transformations of the group
        newLightSource.group.getTransforms().clear();

        originRays.forEach(originRay -> {
            OriginRay newOriginRay = new OriginRay(
                    newLightSource.getCenterX() + Math.cos(Math.toRadians(newLightSource.rotate.getAngle())) * newLightSource.getWidth() / 2,
                    newLightSource.getCenterY() + Math.sin(Math.toRadians(newLightSource.rotate.getAngle())) * newLightSource.getWidth() / 2,
                    newLightSource.getCenterX() + SIZE * Math.cos(Math.toRadians(newLightSource.rotate.getAngle())),
                    newLightSource.getCenterY() + SIZE * Math.sin(Math.toRadians(newLightSource.rotate.getAngle()))
            );
            newOriginRay.setParentSource(newLightSource);
            newOriginRay.setStrokeWidth(globalStrokeWidth);
            newOriginRay.setWavelength(wavelength);
            newOriginRay.setBrightness(brightness);

            newLightSource.originRays.add(newOriginRay);
        });

        lightSources.add(newLightSource);
        newLightSource.group.getChildren().add(newLightSource);
        newLightSource.originRays.forEach(originRay -> newLightSource.group.getChildren().add(originRay.group));
        root.getChildren().add(newLightSource.group);

        newLightSource.moveBy(10, 10);

        UserControls.closeCurrentEdit();
        newLightSource.openObjectEdit();
    }

    public void update() {
        if (isEdited) {
            return;
        }

        for (OriginRay originRay : originRays) {
            group.getChildren().removeAll(originRay.rayReflections);
            mirrors.forEach(mirror -> {
                if (mirror instanceof LightSensor lightSensor) {
                    lightSensor.detectedRays.removeAll(originRay.rayReflections);
                    lightSensor.getDetectedRays().remove(originRay);
                }
            });
            originRay.simulate();
        }
    }

    @Override
    public void openObjectEdit() {
        wavelengthSlider.setCurrentSource(this);
        wavelengthSlider.show();
        whiteLightToggle.setCurrentSource(this);
        whiteLightToggle.show();

        // Defocus the text fields
        root.requestFocus();

        Platform.runLater(() -> rayCanvas.clear());

        hasBeenClicked = true;
        isEdited = true;

        // Place edit points
        objectEditPoints.add(new EditPoint(getCenterX(), getCenterY()));
        objectEditPoints.add(new EditPoint(
                getCenterX() + Math.cos(Math.toRadians(rotate.getAngle())) * 100,
                getCenterY() + Math.sin(Math.toRadians(rotate.getAngle())) * 100
        ));

        // Define what happens when an edit point is clicked
        objectEditPoints.get(0).setOnClickEvent(event -> move());
        objectEditPoints.get(1).setOnClickEvent(event -> rotate());

        editPoints.addAll(objectEditPoints);
        group.getChildren().addAll(objectEditPoints);
        editedShape = group;
    }

    @Override
    public void moveBy(double x, double y) {
        setX(getX() + x);
        setY(getY() + y);

        rotate.setPivotX(getCenterX());
        rotate.setPivotY(getCenterY());

        objectEditPoints.forEach(editPoint -> {
            editPoint.setCenterX(editPoint.getCenterX() + x);
            editPoint.setCenterY(editPoint.getCenterY() + y);
        });

        originRays.forEach(originRay -> {
            originRay.setStartX(getCenterX() + Math.cos(Math.toRadians(rotate.getAngle())) * getWidth() / 2);
            originRay.setStartY(getCenterY() + Math.sin(Math.toRadians(rotate.getAngle())) * getWidth() / 2);

            originRay.setEndX(getCenterX() + Math.cos(Math.toRadians(rotate.getAngle())) * 4 * SIZE);
            originRay.setEndY(getCenterY() + Math.sin(Math.toRadians(rotate.getAngle())) * 4 * SIZE);
        });
        SaveState.autoSave();
    }

    private void move() {
        new Thread(() -> {
            Platform.runLater(() -> {
                for (OriginRay originRay : originRays) {
                    group.getChildren().removeAll(originRay.rayReflections);
                    originRay.rayReflections.clear();
                }
            });

            Point2D prevMousePos = mousePos;

            while (isMousePressed && isEdited) {
                double deltaX = mousePos.getX() - prevMousePos.getX();
                double deltaY = mousePos.getY() - prevMousePos.getY();

                setX(getX() + deltaX);
                setY(getY() + deltaY);

                rotate.setPivotX(getCenterX());
                rotate.setPivotY(getCenterY());

                for (OriginRay originRay : originRays) {
                    originRay.setStartX(getCenterX() + Math.cos(Math.toRadians(rotate.getAngle())) * getWidth() / 2);
                    originRay.setStartY(getCenterY() + Math.sin(Math.toRadians(rotate.getAngle())) * getWidth() / 2);

                    originRay.setEndX(getCenterX() + Math.cos(Math.toRadians(rotate.getAngle())) * 4 * SIZE);
                    originRay.setEndY(getCenterY() + Math.sin(Math.toRadians(rotate.getAngle())) * 4 * SIZE);
                }

                objectEditPoints.get(0).setX(objectEditPoints.get(0).getX() + deltaX);
                objectEditPoints.get(0).setY(objectEditPoints.get(0).getY() + deltaY);
                objectEditPoints.get(1).setX(objectEditPoints.get(1).getX() + deltaX);
                objectEditPoints.get(1).setY(objectEditPoints.get(1).getY() + deltaY);

                prevMousePos = mousePos;

                synchronized (lock) {
                    try {
                        lock.wait(10);
                    } catch (InterruptedException e) {
                        throw new RuntimeException("A thread was interrupted while waiting.");
                    }
                }
            }
            SaveState.autoSave();
        }).start();
    }

    public void rotate() {
        new Thread(() -> {
            Platform.runLater(() -> {
                for (OriginRay originRay : originRays) {
                    group.getChildren().removeAll(originRay.rayReflections);
                    originRay.rayReflections.clear();
                }
            });

            while (isMousePressed && isEdited) {
                rotation = Math.atan2(mousePos.getY() - getCenterY(), mousePos.getX() - getCenterX());

                // Rotate the light source
                rotate.setAngle(Math.toDegrees(rotation));
                rotate.setPivotX(getCenterX());
                rotate.setPivotY(getCenterY());

                // Adjust the ray and edit point positions
                if (!objectEditPoints.isEmpty()) {
                    objectEditPoints.get(1).setCenterX(getCenterX() + Math.cos(rotation) * 100);
                    objectEditPoints.get(1).setCenterY(getCenterY() + Math.sin(rotation) * 100);
                }


                for (OriginRay originRay : originRays) {
                    originRay.setStartX(getCenterX() + Math.cos(rotation) * this.getWidth() / 2);
                    originRay.setStartY(getCenterY() + Math.sin(rotation) * this.getWidth() / 2);
                    originRay.setEndX(originRay.getStartX() + Math.cos(rotation) * 4 * SIZE);
                    originRay.setEndY(originRay.getStartY() + Math.sin(rotation) * 4 * SIZE);
                }

                synchronized (lock) {
                    try {
                        lock.wait(10);
                    } catch (InterruptedException e) {
                        throw new RuntimeException("A thread was interrupted while waiting.");
                    }
                }
            }
            SaveState.autoSave();
        }).start();
    }

    @Serial
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeDouble(getX());
        out.writeDouble(getY());
        out.writeDouble(getWidth());
        out.writeDouble(getHeight());
    }

    @Serial
    private void readObject(java.io.ObjectInputStream in) throws Exception {
        in.defaultReadObject();
        setX(in.readDouble());
        setY(in.readDouble());
        setWidth(in.readDouble());
        setHeight(in.readDouble());

        // Initialize transient fields
        group = new Group();
        rotate = new Rotate();
        originRays = new ArrayList<>();
        objectEditPoints = new ArrayList<>();
        isEdited = false;
        hasBeenClicked = false;
    }

    @Override
    public void closeObjectEdit() {
        wavelengthSlider.hide();
        whiteLightToggle.hide();
        isEdited = false;
        if (objectEditPoints != null && editedShape instanceof Group editedGroup) {
            editedGroup.getChildren().removeAll(objectEditPoints);
            editPoints.removeAll(objectEditPoints);
            objectEditPoints.clear();
        }
        editedShape = null;
        updateLightSources();
    }

    private double getCenterX() {
        return getX() + getWidth() / 2;
    }

    private double getCenterY() {
        return getY() + getHeight() / 2;
    }

    public void setWavelength(double wavelength) {
        this.wavelength = wavelength;
        if (!whiteLight) {
            for (OriginRay originRay : originRays) {
                originRay.setWavelength(wavelength);
            }
        }
    }

    public void setWhiteLight(boolean whiteLight) {
        this.whiteLight = whiteLight;
        Platform.runLater(() -> {
            group.getChildren().removeAll(originRays);
            originRays.forEach(originRay -> {
                group.getChildren().removeAll(originRay.rayReflections);
                group.getChildren().removeAll(originRay.group);
                mirrors.forEach(mirror -> {
                    if (mirror instanceof LightSensor lightSensor) {
                        lightSensor.detectedRays.removeAll(originRay.rayReflections);
                        lightSensor.getDetectedRays().remove(originRay);
                    }
                });
                originRay.rayReflections.clear();
            });

            originRays.clear();

            int rayCount = whiteLight ? whiteLightRayCount : 1;
            for (int i = 0; i < rayCount; i++) {
                OriginRay originRay = new OriginRay(
                            getCenterX() + Math.cos(Math.toRadians(rotate.getAngle())) * getWidth() / 2,
                        getCenterY() + Math.sin(Math.toRadians(rotate.getAngle())) * getWidth() / 2,
                        getCenterX() + SIZE * Math.cos(Math.toRadians(rotate.getAngle())),
                        getCenterY() + SIZE * Math.sin(Math.toRadians(rotate.getAngle()))
                );
                originRay.setParentSource(this);
                originRay.setStrokeWidth(globalStrokeWidth);
                originRay.setWavelength(whiteLight ? 380 + (400.0 / whiteLightRayCount * i) : wavelength);
                originRay.setBrightness(brightness);

                originRays.add(originRay);
            }
            originRays.forEach(ray -> group.getChildren().add(ray.group));
            originRays.forEach(Node::toBack);
            updateLightSources();
        });
    }

    public double getWavelength() {
        return wavelength;
    }

    @Override
    public void setHasBeenClicked(boolean hasBeenClicked) {
        this.hasBeenClicked = hasBeenClicked;
    }

    @Override
    public boolean getHasBeenClicked() {
        return hasBeenClicked;
    }

    private void setCenterY(double centerY) {
        setY(centerY + getHeight() / 2);
    }

    private void setCenterX(double centerX) {
        setX(centerX + getWidth() / 2);
    }

    @Override
    public boolean intersectsMouseHitbox() {
        return Shape.intersect(this, mouseHitbox).getLayoutBounds().getWidth() != -1;
    }
}
