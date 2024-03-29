package com.lensr.objects.misc;

import com.lensr.ui.EditPoint;
import com.lensr.objects.Editable;
import com.lensr.objects.lightsources.Ray;
import com.lensr.ui.Graph;
import com.lensr.saveloadkit.SaveState;
import com.lensr.UserControls;
import com.lensr.LensrStart;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.Group;
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

public class LightSensor extends Line implements Editable, Serializable {
    public transient Group group = new Group();
    private transient Rotate rotate = new Rotate();
    // Extended hitbox for easier editing
    private transient Rectangle hitbox;
    private transient List<EditPoint> objectEditPoints = new ArrayList<>();
    public transient List<Ray> detectedRays = new ArrayList<>();
    public transient Graph graph;
    private transient double rotation = 0;
    private transient boolean isEdited;
    private transient boolean hasBeenClicked;

    public LightSensor(double startX, double startY, double endX, double endY) {
        setStartX(startX);
        setStartY(startY);
        setEndX(endX);
        setEndY(endY);
    }

    @Override
    public void create() {
        setFill(Color.TRANSPARENT);
        setStroke(LensrStart.mirrorColor);
        setStrokeWidth(LensrStart.globalStrokeWidth);

        createRectangleHitbox();
        graph = new Graph(LensrStart.WIDTH - 225, 60, 200, 150);
        graph.setDataSource(this);
        graph.hide();

        group.getChildren().add(this);
        group.getChildren().add(hitbox);
        group.getChildren().add(graph.group);
        LensrStart.root.getChildren().add(group);
    }

    @Override
    public void delete() {
        graph.hide();
        LensrStart.editPoints.removeAll(objectEditPoints);
        LensrStart.editedShape = null;
        LensrStart.mirrors.remove(this);
        LensrStart.root.getChildren().remove(group);
    }

    @Override
    public void copy() {
        LightSensor newSensor = new LightSensor(getStartX(), getStartY(), getEndX(), getEndY());
        newSensor.create();
        newSensor.moveBy(10, 10);
        LensrStart.mirrors.add(newSensor);
        UserControls.closeCurrentEdit();
        newSensor.openObjectEdit();
    }

    @Override
    public void openObjectEdit() {
        // Defocus the text field
        LensrStart.root.requestFocus();

        graph.drawGraph();
        graph.show();

        hasBeenClicked = true;
        isEdited = true;

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

        LensrStart.editPoints.addAll(objectEditPoints);
        group.getChildren().addAll(objectEditPoints);
        LensrStart.editedShape = group;
    }

    @Override
    public void closeObjectEdit() {
        graph.clear();
        graph.hide();

        isEdited = false;
        if (objectEditPoints != null && LensrStart.editedShape instanceof Group editedGroup) {
            editedGroup.getChildren().removeAll(objectEditPoints);
            LensrStart.editPoints.removeAll(objectEditPoints);
            objectEditPoints.clear();
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


    public void addRay(Ray ray) {
        detectedRays.add(ray);
    }


    public List<Ray> getDetectedRays() {
        return detectedRays;
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
        }).start();
        SaveState.autoSave();
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
        hitbox = new Rectangle();
        objectEditPoints = new ArrayList<>();
        detectedRays = new ArrayList<>();
        rotation = 0;
        isEdited = false;
        hasBeenClicked = false;
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
