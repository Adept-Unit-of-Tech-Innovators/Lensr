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
import static com.example.lensr.MirrorMethods.updateLightSources;

public class LightSensor extends Line implements Editable {
    public Group group = new Group();
    Rotate rotate = new Rotate();
    // Extended hitbox for easier editing
    public Rectangle hitbox;
    public List<EditPoint> objectEditPoints = new ArrayList<>();
    List<Ray> detectedRays = new ArrayList<>();
    public Graph graph;
    double rotation = 0;
    public boolean isEdited;
    public boolean hasBeenClicked;

    public LightSensor(double startX, double startY, double endX, double endY) {
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
        LightSensor newSensor = new LightSensor(getStartX(), getStartY(), getEndX(), getEndY());
        newSensor.create();
        newSensor.moveBy(10, 10);
        mirrors.add(newSensor);
        UserControls.closeCurrentEdit();
        newSensor.openObjectEdit();
    }

    @Override
    public void openObjectEdit() {
        // Defocus the text field
        root.requestFocus();

        graph.drawGraph();
        graph.show();

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
