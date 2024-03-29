package com.lensr.objects.lightsources;

import com.lensr.ui.EditPoint;
import com.lensr.objects.Editable;
import com.lensr.objects.misc.LightSensor;
import com.lensr.saveloadkit.SaveState;
import com.lensr.UserControls;
import com.lensr.LensrStart;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.transform.Rotate;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class BeamSource extends Line implements Editable, Serializable {
    private transient Group group = new Group();
    private transient List<OriginRay> originRays = new ArrayList<>();
    private transient List<EditPoint> objectEditPoints = new ArrayList<>();
    private transient Rotate rotate = new Rotate();
    private double rotation = 0;
    private transient Rectangle hitbox;
    private transient boolean hasBeenClicked;
    private transient boolean isEdited;
    private double wavelength = 580;
    private int rayCount = 25;
    private boolean isWhiteLight;
    private double brightness = 1.0;

    public BeamSource(double startX, double startY, double endX, double endY) {
        setStartX(startX);
        setStartY(startY);
        setEndX(endX);
        setEndY(endY);
    }

    @Override
    public void create() {
        setStroke(Color.GRAY);
        setStrokeWidth(3);
        toBack();

        createRectangleHitbox();

        double dx = (getEndX() - getStartX()) / (rayCount - 1);
        double dy = (getEndY() - getStartY()) / (rayCount - 1);

        double angle = Math.atan2(getEndY() - getStartY(), getEndX() - getStartX());
        rotate.setPivotX((getEndX() + getStartX()) / 2);
        rotate.setPivotY((getEndY() + getStartY()) / 2);
        rotate.setAngle(Math.toDegrees(angle));

        int whiteRayCount = isWhiteLight ? LensrStart.whiteLightRayCount : 1;
        for (int i = 0; i < whiteRayCount; i++) {
            for (int j = 0; j < rayCount; j++) {
                OriginRay originRay = new OriginRay(
                        getStartX() + dx * j,
                        getStartY() + dy * j,
                        getStartX() + dx * j + Math.cos(angle + Math.PI / 2) * LensrStart.WIDTH * 1000,
                        getStartY() + dy * j + Math.sin(angle + Math.PI / 2) * LensrStart.WIDTH * 1000

                );
                originRay.setParentSource(this);
                originRay.setStrokeWidth(LensrStart.globalStrokeWidth);
                originRay.setWavelength(isWhiteLight ? 380 + (400.0 / LensrStart.whiteLightRayCount * i) : wavelength);
                originRay.setBrightness(brightness);

                originRays.add(originRay);
            }
        }

        // Place edit points
        objectEditPoints.add(new EditPoint(getStartX(), getStartY(), EditPoint.Style.Primary));
        objectEditPoints.add(new EditPoint(getEndX(), getEndY(), EditPoint.Style.Primary));

        // Define what happens when an edit point is clicked
        for (EditPoint editPoint : objectEditPoints) {
            editPoint.setOnClickEvent(event -> {
                // Scale the mirror with the opposite edit point as an anchor
                EditPoint oppositeEditPoint = objectEditPoints.get(1 - objectEditPoints.indexOf(editPoint));
                scale(oppositeEditPoint.getCenter());
            });
        }
        objectEditPoints.add(new EditPoint((getStartX() + getEndX()) / 2, (getStartY() + getEndY()) / 2, EditPoint.Style.Secondary));
        objectEditPoints.get(2).setOnClickEvent(event -> move());

        objectEditPoints.forEach(editPoint -> editPoint.setVisible(false));

        group.getChildren().add(this);
        originRays.forEach(ray -> group.getChildren().add(ray.group));
        group.getChildren().addAll(objectEditPoints);
        LensrStart.root.getChildren().add(group);
        originRays.forEach(Node::toBack);
    }

    @Override
    public void delete() {
        LensrStart.wavelengthSlider.hide();
        LensrStart.numberOfRaysSlider.hide();
        LensrStart.whiteLightToggle.hide();
        LensrStart.brightnessSlider.hide();
        LensrStart.editPoints.removeAll(objectEditPoints);
        LensrStart.editedShape = null;
        LensrStart.lightSources.remove(this);
        originRays.clear();
        LensrStart.root.getChildren().remove(group);
    }

    @Override
    public void copy() {
        // TODO: CHECK OUT WHAT `panelSource.clone()` IS
        BeamSource newBeamSource = new BeamSource(getStartX(), getStartY(), getEndX(), getEndY());
        newBeamSource.setStroke(Color.GRAY);
        newBeamSource.setStrokeWidth(3);
        newBeamSource.toBack();

        newBeamSource.createRectangleHitbox();

        newBeamSource.setWavelength(wavelength);
        newBeamSource.setRayCount(rayCount);
        newBeamSource.setBrightness(brightness);

        for (OriginRay originRay : originRays) {
            OriginRay newOriginRay = new OriginRay(originRay.getStartX(), originRay.getStartY(), originRay.getEndX(), originRay.getEndY());
            newOriginRay.setParentSource(newBeamSource);
            newOriginRay.setWavelength(originRay.getWavelength());
            newOriginRay.setStrokeWidth(LensrStart.globalStrokeWidth);
            newOriginRay.setBrightness(brightness);
            newBeamSource.originRays.add(newOriginRay);
        }

        newBeamSource.objectEditPoints.add(new EditPoint(newBeamSource.getStartX(), newBeamSource.getStartY(), EditPoint.Style.Primary));
        newBeamSource.objectEditPoints.add(new EditPoint(newBeamSource.getEndX(), newBeamSource.getEndY(), EditPoint.Style.Primary));
        for (EditPoint editPoint : objectEditPoints) {
            editPoint.setOnClickEvent(event -> {
                // Scale the mirror with the opposite edit point as an anchor
                EditPoint oppositeEditPoint = objectEditPoints.get(1 - objectEditPoints.indexOf(editPoint));
                scale(oppositeEditPoint.getCenter());
            });
        }
        newBeamSource.objectEditPoints.add(new EditPoint(newBeamSource.getStartX() + newBeamSource.getEndX() / 2, newBeamSource.getStartY() + newBeamSource.getEndY() / 2, EditPoint.Style.Secondary));
        newBeamSource.objectEditPoints.get(2).setOnClickEvent(event -> newBeamSource.move());
        newBeamSource.objectEditPoints.forEach(editPoint -> editPoint.setVisible(false));


        LensrStart.lightSources.add(newBeamSource);
        newBeamSource.group.getChildren().addAll(newBeamSource.objectEditPoints);
        newBeamSource.group.getChildren().add(newBeamSource);
        newBeamSource.originRays.forEach(originRay -> newBeamSource.group.getChildren().add(originRay.group));
        newBeamSource.originRays.forEach(Node::toBack);
        LensrStart.root.getChildren().add(newBeamSource.group);

        newBeamSource.moveBy(10, 10);
        UserControls.closeCurrentEdit();
        newBeamSource.openObjectEdit();
    }

    public void update() {
        if (isEdited) {
            return;
        }

        // Reset the end point of the ray to ensure deterministic behavior
        double dx = (getEndX() - getStartX()) / (rayCount - 1);
        double dy = (getEndY() - getStartY()) / (rayCount - 1);

        double angle = Math.atan2(getEndY() - getStartY(), getEndX() - getStartX());

        int whiteRayCount = isWhiteLight ? LensrStart.whiteLightRayCount : 1;
        for (int i = 0; i < whiteRayCount; i++) {
            for (int j = 0; j < rayCount; j++) {
                originRays.get(i * rayCount + j).setEndX(getStartX() + dx * j + Math.cos(angle + Math.PI / 2) * LensrStart.WIDTH * 1000);
                originRays.get(i * rayCount + j).setEndY(getStartY() + dy * j + Math.sin(angle + Math.PI / 2) * LensrStart.WIDTH * 1000);
            }
        }

        for (OriginRay originRay : originRays) {
            group.getChildren().removeAll(originRay.rayReflections);
            originRay.simulate();
        }
    }

    public void openObjectEdit() {
        LensrStart.wavelengthSlider.setCurrentSource(this);
        LensrStart.wavelengthSlider.show();
        LensrStart.brightnessSlider.setCurrentSource(this);
        LensrStart.brightnessSlider.show();
        LensrStart.numberOfRaysSlider.setCurrentSource(this);
        LensrStart.numberOfRaysSlider.show();
        LensrStart.whiteLightToggle.setCurrentSource(this);
        LensrStart.whiteLightToggle.setSelected(isWhiteLight);
        LensrStart.whiteLightToggle.show();

        // Defocus the text field
        LensrStart.root.requestFocus();

        objectEditPoints.forEach(editPoint -> {
            editPoint.setVisible(true);
            editPoint.toFront();
        });

        Platform.runLater(() -> LensrStart.rayCanvas.clear());

        hasBeenClicked = true;
        isEdited = true;

        LensrStart.editPoints.addAll(objectEditPoints);
        LensrStart.editedShape = group;
    }

    @Override
    public void closeObjectEdit() {
        LensrStart.wavelengthSlider.hide();
        LensrStart.numberOfRaysSlider.hide();
        LensrStart.brightnessSlider.hide();
        LensrStart.whiteLightToggle.hide();

        isEdited = false;
        if (objectEditPoints != null && LensrStart.editedShape instanceof Group) {
            LensrStart.editPoints.removeAll(objectEditPoints);
            objectEditPoints.forEach(editPoint -> {
                editPoint.setVisible(false);
                editPoint.hasBeenClicked = false;
            });
        }
        LensrStart.editedShape = null;
        LensrStart.updateLightSources();
    }

    private void createRectangleHitbox() {
        hitbox = new Rectangle();
        hitbox.setHeight(LensrStart.mouseHitboxSize);
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

        rotate.setPivotX((getStartX() + getEndX()) / 2);
        rotate.setPivotY((getStartY() + getEndY()) / 2);

        objectEditPoints.forEach(editPoint -> {
            editPoint.setCenterX(editPoint.getCenterX() + x);
            editPoint.setCenterY(editPoint.getCenterY() + y);
        });
        updateHitbox();

        originRays.forEach(originRay -> {
            originRay.setStartX(originRay.getStartX() + x);
            originRay.setStartY(originRay.getStartY() + y);
            originRay.setEndX(originRay.getEndX() + x);
            originRay.setEndY(originRay.getEndY() + y);
        });
        SaveState.autoSave();
    }

    private void move() {
        new Thread(() -> {
            Point2D prevMousePos = LensrStart.mousePos;
            Point2D prevStart = new Point2D(getStartX(), getStartY());
            Point2D prevEnd = new Point2D(getEndX(), getEndY());

            while (LensrStart.isMousePressed && isEdited) {
                double deltaX = LensrStart.mousePos.getX() - prevMousePos.getX();
                double deltaY = LensrStart.mousePos.getY() - prevMousePos.getY();

                // Update the UI on the JavaFX application thread
                Platform.runLater(() -> {
                    this.setStartX(prevStart.getX() + deltaX);
                    this.setStartY(prevStart.getY() + deltaY);
                    this.setEndX(prevEnd.getX() + deltaX);
                    this.setEndY(prevEnd.getY() + deltaY);

                    double dx = (getEndX() - getStartX()) / (rayCount - 1);
                    double dy = (getEndY() - getStartY()) / (rayCount - 1);

                    int i = 0;
                    for (OriginRay originRay : originRays) {
                        originRay.setStartX(getStartX() + dx * (i % rayCount));
                        originRay.setStartY(getStartY() + dy * (i % rayCount));
                        originRay.setEndX(getStartX() + dx * (i % rayCount) + Math.cos(Math.toRadians(rotate.getAngle()) + Math.PI / 2) * 1000 * LensrStart.WIDTH);
                        originRay.setEndY(getStartY() + dy * (i % rayCount) + Math.sin(Math.toRadians(rotate.getAngle()) + Math.PI / 2) * 1000 * LensrStart.WIDTH);
                        i++;
                    }

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

                synchronized (LensrStart.lock) {
                    try {
                        LensrStart.lock.wait(10); // Adjust the sleep time as needed
                    } catch (InterruptedException e) {
                        throw new RuntimeException("A thread was interrupted while waiting.");
                    }
                }
            }
            SaveState.autoSave();
        }).start();
    }

    public void scale(Point2D anchor) {
        new Thread(() -> {
            double startX, startY, endX, endY;

            while (LensrStart.isMousePressed && isEdited) {
                if (LensrStart.altPressed && LensrStart.shiftPressed) {
                    // Shift-mode calculations for actually half the mirror
                    double deltaX = LensrStart.mousePos.getX() - anchor.getX();
                    double deltaY = LensrStart.mousePos.getY() - anchor.getY();
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
                } else if (LensrStart.altPressed) {
                    // Calculate first because funny java threading
                    startX = anchor.getX() - (LensrStart.mousePos.getX() - anchor.getX());
                    startY = anchor.getY() - (LensrStart.mousePos.getY() - anchor.getY());
                    endX = LensrStart.mousePos.getX();
                    endY = LensrStart.mousePos.getY();
                } else if (LensrStart.shiftPressed) {
                    startX = anchor.getX();
                    startY = anchor.getY();
                    double deltaX = LensrStart.mousePos.getX() - startX;
                    double deltaY = LensrStart.mousePos.getY() - startY;
                    double distance = Math.sqrt(Math.pow(deltaX, 2) + Math.pow(deltaY, 2));
                    double angle = Math.atan2(deltaY, deltaX);
                    double shiftedAngle = Math.round(angle * 4 / Math.PI) * Math.PI / 4;
                    endX = startX + distance * Math.cos(shiftedAngle);
                    endY = startY + distance * Math.sin(shiftedAngle);
                } else {
                    startX = anchor.getX();
                    startY = anchor.getY();
                    endX = LensrStart.mousePos.getX();
                    endY = LensrStart.mousePos.getY();
                }

                double finalStartX = startX;
                double finalStartY = startY;
                double finalEndX = endX;
                double finalEndY = endY;

                // Update the UI on the JavaFX application thread
                Platform.runLater(() -> {
                    // TODO: Might be worth rethinking the variable names here
                    if (anchor.equals(objectEditPoints.get(0).getCenter())) {
                        this.setStartX(finalStartX);
                        this.setStartY(finalStartY);
                        this.setEndX(finalEndX);
                        this.setEndY(finalEndY);
                    } else {
                        this.setStartX(finalEndX);
                        this.setStartY(finalEndY);
                        this.setEndX(finalStartX);
                        this.setEndY(finalStartY);
                    }

                    double dx = (getEndX() - getStartX()) / (rayCount - 1);
                    double dy = (getEndY() - getStartY()) / (rayCount - 1);


                    int i = 0;
                    for (OriginRay originRay : originRays) {
                        originRay.setStartX(getStartX() + dx * (i % rayCount));
                        originRay.setStartY(getStartY() + dy * (i % rayCount));
                        originRay.setEndX(getStartX() + dx * (i % rayCount) + Math.cos(Math.toRadians(rotate.getAngle()) + Math.PI / 2) * 1000 * LensrStart.WIDTH);
                        originRay.setEndY(getStartY() + dy * (i % rayCount) + Math.sin(Math.toRadians(rotate.getAngle()) + Math.PI / 2) * 1000 * LensrStart.WIDTH);
                        i++;
                    }

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

                synchronized (LensrStart.lock) {
                    try {
                        LensrStart.lock.wait(10); // Adjust the sleep time as needed
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
        out.writeDouble(getStartX());
        out.writeDouble(getStartY());
        out.writeDouble(getEndX());
        out.writeDouble(getEndY());
    }

    @Serial
    private void readObject(java.io.ObjectInputStream in) throws Exception {
        in.defaultReadObject();
        setStartX(in.readDouble());
        setStartY(in.readDouble());
        setEndX(in.readDouble());
        setEndY(in.readDouble());

        // Initialize transient fields
        group = new Group();
        rotate = new Rotate();
        originRays = new ArrayList<>();
        hitbox = new Rectangle();
        objectEditPoints = new ArrayList<>();
        rotation = 0;
        isEdited = false;
        hasBeenClicked = false;
    }

    public void setWhiteLight(boolean whiteLight) {
        this.isWhiteLight = whiteLight;
        Platform.runLater(() -> {
            group.getChildren().removeAll(originRays);
            originRays.forEach(originRay -> {
                group.getChildren().remove(originRay.group);
                LensrStart.mirrors.forEach(mirror -> {
                    if (mirror instanceof LightSensor lightSensor) {
                        lightSensor.detectedRays.removeAll(originRay.rayReflections);
                        lightSensor.getDetectedRays().remove(originRay);
                    }
                });
                originRay.rayReflections.clear();
            });
            originRays.clear();

            double dx = (getEndX() - getStartX()) / (rayCount - 1);
            double dy = (getEndY() - getStartY()) / (rayCount - 1);
            double angle = Math.toRadians(rotate.getAngle());

            int whiteRayCount = whiteLight ? LensrStart.whiteLightRayCount : 1;
            for (int i = 0; i < whiteRayCount; i++) {
                for (int j = 0; j < rayCount; j++) {
                    OriginRay originRay = new OriginRay(
                            getStartX() + dx * j,
                            getStartY() + dy * j,
                            getStartX() + dx * j + Math.cos(angle + Math.PI / 2) * LensrStart.WIDTH * 1000,
                            getStartY() + dy * j + Math.sin(angle + Math.PI / 2) * LensrStart.WIDTH * 1000
                    );
                    originRay.setParentSource(this);
                    originRay.setStrokeWidth(LensrStart.globalStrokeWidth);
                    originRay.setWavelength(whiteLight ? 380 + (400.0 / LensrStart.whiteLightRayCount * i) : wavelength);
                    originRay.setBrightness(brightness);

                    originRays.add(originRay);
                }
            }
            originRays.forEach(ray -> group.getChildren().add(ray.group));
            originRays.forEach(Node::toBack);
        });
        LensrStart.updateLightSources();
    }

    public void setRayCount(int rayCount) {
        this.rayCount = rayCount;
        Platform.runLater(() -> {
            group.getChildren().removeAll(originRays);
            originRays.forEach(originRay -> {
                group.getChildren().remove(originRay.group);
                LensrStart.mirrors.forEach(mirror -> {
                    if (mirror instanceof LightSensor lightSensor) {
                        lightSensor.detectedRays.removeAll(originRay.rayReflections);
                        lightSensor.getDetectedRays().remove(originRay);
                    }
                });
                originRay.rayReflections.clear();
            });
            originRays.clear();

            double dx = (getEndX() - getStartX()) / (rayCount - 1);
            double dy = (getEndY() - getStartY()) / (rayCount - 1);
            double angle = Math.toRadians(rotate.getAngle());

            int whiteRayCount = isWhiteLight ? LensrStart.whiteLightRayCount : 1;
            for (int i = 0; i < whiteRayCount; i++) {
                for (int j = 0; j < rayCount; j++) {
                    OriginRay originRay = new OriginRay(
                            getStartX() + dx * j,
                            getStartY() + dy * j,
                            getStartX() + dx * j + Math.cos(angle + Math.PI / 2) * LensrStart.WIDTH * 1000,
                            getStartY() + dy * j + Math.sin(angle + Math.PI / 2) * LensrStart.WIDTH * 1000
                    );
                    originRay.setParentSource(this);
                    originRay.setStrokeWidth(LensrStart.globalStrokeWidth);
                    originRay.setWavelength(isWhiteLight ? 380 + (400.0 / LensrStart.whiteLightRayCount * i) : wavelength);
                    originRay.setBrightness(brightness);

                    originRays.add(originRay);
                }
            }
            originRays.forEach(ray -> group.getChildren().add(ray.group));
            originRays.forEach(Node::toBack);
        });
        LensrStart.updateLightSources();
    }

    public int getRayCount() {
        return rayCount;
    }

    public void setWavelength(double wavelength) {
        this.wavelength = wavelength;
        for (OriginRay originRay : originRays) {
            originRay.setWavelength(wavelength);
        }
        setBrightness(brightness);
    }

    public void setBrightness(double brightness) {
        this.brightness = brightness;
        for (OriginRay originRay : originRays) {
            originRay.setBrightness(brightness);
        }
    }

    public double getWavelength() {
        return this.wavelength;
    }

    public double getBrightness() {
        return this.brightness;
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
        return Shape.intersect(this, LensrStart.mouseHitbox).getLayoutBounds().getWidth() != -1;
    }
}
