package com.example.lensr.objects;

import com.example.lensr.EditPoint;
import com.example.lensr.UserControls;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.transform.Rotate;

import java.util.ArrayList;
import java.util.List;

import static com.example.lensr.LensrStart.*;
import static com.example.lensr.LensrStart.lock;
import static com.example.lensr.MirrorMethods.updateLightSources;

public class PanelSource extends Line implements Editable {
    public List<OriginRay> originRays = new ArrayList<>();
    public List<EditPoint> objectEditPoints = new ArrayList<>();
    public Rotate rotate = new Rotate();
    double rotation = 0;
    public Rectangle hitbox;
    public Group group = new Group();
    public boolean hasBeenClicked;
    public boolean isEdited;
    public double wavelength = 580;
    public double brightness = 1.0;

    public PanelSource(double startX, double startY, double endX, double endY) {
        setStartX(startX);
        setStartY(startY);
        setEndX(endX);
        setEndY(endY);
    }


    public void create() {
        setStroke(Color.GRAY);
        setStrokeWidth(3);
        toBack();

        createRectangleHitbox();

        double dx = (getEndX() - getStartX()) / (panelRayCount - 1);
        double dy = (getEndY() - getStartY()) / (panelRayCount - 1);

        double angle = Math.atan2(getStartX() - getEndX(), getStartY() - getEndY());
        rotate.setPivotX((getEndX() + getStartX()) / 2);
        rotate.setPivotY((getEndY() + getStartY()) / 2);
        rotate.setAngle(Math.toDegrees(angle));

        // Create `panelRayCount` amount of rays distributed evenly across the panel
        for (int i = 0; i < panelRayCount; i++) {
            OriginRay originRay = new OriginRay(
                    getStartX() + dx * i,
                    getStartY() + dy * i,
                    getStartX() + dx * i + Math.cos(angle + Math.PI / 2) * SIZE,
                    getStartY() + dy * i + Math.sin(angle + Math.PI / 2) * SIZE
                    );
            originRay.setParentSource(this);
            originRay.setStrokeWidth(globalStrokeWidth);
            originRay.setWavelength(wavelength);
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
        // TODO: CHECK OUT WHAT `panelSource.clone()` IS
        PanelSource newPanelSource = new PanelSource(getStartX(), getStartY(), getEndX(), getEndY());
        newPanelSource.setStroke(Color.GRAY);
        newPanelSource.setStrokeWidth(3);
        newPanelSource.toBack();

        newPanelSource.createRectangleHitbox();

        newPanelSource.setWavelength(wavelength);

        for (OriginRay originRay : originRays) {
            OriginRay newOriginRay = new OriginRay(originRay.getStartX(), originRay.getStartY(), originRay.getEndX(), originRay.getEndY());
            newOriginRay.setParentSource(newPanelSource);
            newOriginRay.setWavelength(originRay.getWavelength());
            newOriginRay.setStrokeWidth(globalStrokeWidth);
            newOriginRay.setBrightness(brightness);
            newPanelSource.originRays.add(newOriginRay);
        }

        lightSources.add(newPanelSource);
        newPanelSource.group.getChildren().add(newPanelSource);
        newPanelSource.originRays.forEach(originRay -> newPanelSource.group.getChildren().add(originRay.group));
        newPanelSource.originRays.forEach(Node::toBack);
        root.getChildren().add(newPanelSource.group);

        newPanelSource.moveBy(10, 10);
        UserControls.closeCurrentEdit();
        newPanelSource.openObjectEdit();
    }

    public void update() {
        if (isEdited) {
            return;
        }

        rayCanvas.clear();

        for (OriginRay originRay : originRays) {
            group.getChildren().removeAll(originRay.rayReflections);
            mirrors.forEach(mirror -> {
                if (mirror instanceof LightSensor lightSensor) {
                    lightSensor.detectedRays.removeAll(originRay.rayReflections);
                    lightSensor.getDetectedRays().remove(originRay);
                }
            });
            originRay.rayReflections.clear();

            originRay.simulate();
        }
    }

    public void openObjectEdit() {
        wavelengthSlider.setCurrentSource(this);
        wavelengthSlider.show();
        whiteLightToggle.setCurrentSource(this);
        whiteLightToggle.show();

        // Defocus the text field
        root.requestFocus();

        rayCanvas.clear();

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
        update();
    }


    private void createRectangleHitbox() {
        hitbox = new Rectangle();
        hitbox.setHeight(mouseHitboxSize);
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
                Math.pow( getEndX() - getStartX(), 2) +
                        Math.pow(getEndY() -getStartY(), 2)
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
    }

    private void move() {
        new Thread(() -> {
            Point2D prevMousePos = mousePos;
            Point2D prevStart = new Point2D(getStartX(), getStartY());
            Point2D prevEnd = new Point2D(getEndX(), getEndY());

            while (isMousePressed) {
                double deltaX = mousePos.getX() - prevMousePos.getX();
                double deltaY = mousePos.getY() - prevMousePos.getY();

                // Update the UI on the JavaFX application thread
                Platform.runLater(() -> {
                    this.setStartX(prevStart.getX() + deltaX);
                    this.setStartY(prevStart.getY() + deltaY);
                    this.setEndX(prevEnd.getX() + deltaX);
                    this.setEndY(prevEnd.getY() + deltaY);

                    double dx = (getEndX() - getStartX()) / (panelRayCount - 1);
                    double dy = (getEndY() - getStartY()) / (panelRayCount - 1);

                    int i = 0;
                    for (OriginRay originRay : originRays) {
                        originRay.setStartX(getStartX() + dx * (i % panelRayCount));
                        originRay.setStartY(getStartY() + dy * (i % panelRayCount));
                        originRay.setEndX(getStartX() + dx * (i % panelRayCount) + Math.cos(Math.toRadians(rotate.getAngle()) + Math.PI / 2) * 2 * SIZE);
                        originRay.setEndY(getStartY() + dy * (i % panelRayCount) + Math.sin(Math.toRadians(rotate.getAngle()) + Math.PI / 2) * 2 * SIZE);
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

                    double dx = (getEndX() - getStartX()) / (panelRayCount - 1);
                    double dy = (getEndY() - getStartY()) / (panelRayCount - 1);

                    int i = 0;
                    for (OriginRay originRay : originRays) {
                        originRay.setStartX(getStartX() + dx * (i % panelRayCount));
                        originRay.setStartY(getStartY() + dy * (i % panelRayCount));
                        originRay.setEndX(getStartX() + dx * (i % panelRayCount) + Math.cos(Math.toRadians(rotate.getAngle()) + Math.PI / 2) * 2 * SIZE);
                        originRay.setEndY(getStartY() + dy * (i % panelRayCount) + Math.sin(Math.toRadians(rotate.getAngle()) + Math.PI / 2) * 2 * SIZE);
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

    public void setWhiteLight(boolean whiteLight) {
        Platform.runLater(() -> {
            group.getChildren().removeAll(originRays);
            originRays.forEach(originRay -> {
                group.getChildren().remove(originRay.group);
                mirrors.forEach(mirror -> {
                    if (mirror instanceof LightSensor lightSensor) {
                        lightSensor.detectedRays.removeAll(originRay.rayReflections);
                        lightSensor.getDetectedRays().remove(originRay);
                    }
                });
                originRay.rayReflections.clear();
            });
            originRays.clear();

            double dx = (getEndX() - getStartX()) / (panelRayCount - 1);
            double dy = (getEndY() - getStartY()) / (panelRayCount - 1);
            double angle = Math.toRadians(rotate.getAngle());

            int rayCount = whiteLight ? whiteLightRayCount : 1;
            for (int i = 0; i < rayCount; i++) {
                for (int j = 0; j < panelRayCount; j++) {
                    OriginRay originRay = new OriginRay(
                            getStartX() + dx * j,
                            getStartY() + dy * j,
                            getStartX() + dx * j + Math.cos(angle + Math.PI / 2) * SIZE,
                            getStartY() + dy * j + Math.sin(angle + Math.PI / 2) * SIZE
                    );
                    originRay.setParentSource(this);
                    originRay.setStrokeWidth(globalStrokeWidth);
                    originRay.setWavelength(whiteLight ? 380 + (400.0 / whiteLightRayCount * i) : wavelength);
                    originRay.setBrightness(brightness);

                    originRays.add(originRay);
                }
            }
            originRays.forEach(ray -> group.getChildren().add(ray.group));
            originRays.forEach(Node::toBack);
            update();
        });
    }

    public void setWavelength(double wavelength) {
        this.wavelength = wavelength;
        for (OriginRay originRay : originRays) {
            originRay.setWavelength(wavelength);
        }
    }

    public double getWavelength() {
        return this.wavelength;
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
