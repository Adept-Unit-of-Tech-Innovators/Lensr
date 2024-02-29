package com.lensr.objects.misc;

import com.lensr.ui.EditPoint;
import com.lensr.objects.Editable;
import com.lensr.saveloadkit.SaveState;
import com.lensr.UserControls;
import com.lensr.LensrStart;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class LightEater extends Circle implements Editable, Serializable {
    public transient Group group = new Group();
    private transient List<EditPoint> objectEditPoints = new ArrayList<>();
    private transient boolean isEdited;
    private transient boolean hasBeenClicked;

    public LightEater(double centerX, double centerY, double radius) {
        setCenterX(centerX);
        setCenterY(centerY);
        setRadius(radius);
    }

    @Override
    public void create() {
        setFill(Color.BLACK);
        setStroke(Color.BLACK);
        setStrokeWidth(LensrStart.globalStrokeWidth);
        setStrokeType(StrokeType.INSIDE);

        group.getChildren().add(this);
        LensrStart.root.getChildren().add(group);
    }

    @Override
    public void delete() {
        LensrStart.editPoints.removeAll(objectEditPoints);
        LensrStart.editedShape = null;
        LensrStart.mirrors.remove(this);
        LensrStart.root.getChildren().remove(group);
    }

    @Override
    public void copy() {
        LightEater newLightEater = new LightEater(getCenterX(), getCenterY(), getRadius());
        newLightEater.create();
        newLightEater.moveBy(10, 10);
        LensrStart.mirrors.add(newLightEater);
        UserControls.closeCurrentEdit();
        newLightEater.openObjectEdit();
    }

    @Override
    public void openObjectEdit() {
        hasBeenClicked = true;
        isEdited = true;

        // Place edit points
        Bounds mirrorBounds = getLayoutBounds();
        objectEditPoints.add(new EditPoint(mirrorBounds.getMinX(), mirrorBounds.getMinY(), EditPoint.Style.Primary));
        objectEditPoints.add(new EditPoint(mirrorBounds.getMaxX(), mirrorBounds.getMinY(), EditPoint.Style.Primary));
        objectEditPoints.add(new EditPoint(mirrorBounds.getMaxX(), mirrorBounds.getMaxY(), EditPoint.Style.Primary));
        objectEditPoints.add(new EditPoint(mirrorBounds.getMinX(), mirrorBounds.getMaxY(), EditPoint.Style.Primary));

        // Define what happens when an edit point is clicked
        for (EditPoint editPoint : objectEditPoints) {
            editPoint.setOnClickEvent(event -> {
                // Scale the mirror with the opposite edit point as an anchor
                EditPoint oppositeEditPoint = objectEditPoints.get(((objectEditPoints.indexOf(editPoint) + 2) % 4));
                scale(oppositeEditPoint.getCenter());
            });
        }

        objectEditPoints.add(new EditPoint(getCenterX(), getCenterY(), EditPoint.Style.Secondary));
        objectEditPoints.get(4).setOnClickEvent(event -> move());

        LensrStart.editPoints.addAll(objectEditPoints);
        group.getChildren().addAll(objectEditPoints);
        LensrStart.editedShape = group;
    }

    @Override
    public void closeObjectEdit() {
        isEdited = false;
        if (objectEditPoints != null && LensrStart.editedShape instanceof Group editedGroup) {
            editedGroup.getChildren().removeAll(objectEditPoints);
            LensrStart.editPoints.removeAll(objectEditPoints);
            objectEditPoints.clear();
        }
        LensrStart.editedShape = null;
        LensrStart.updateLightSources();
    }

    @Override
    public void moveBy(double x, double y) {
        setCenterX(getCenterX() + x);
        setCenterY(getCenterY() + y);

        objectEditPoints.forEach(editPoint -> {
            editPoint.setCenterX(editPoint.getCenterX() + x);
            editPoint.setCenterY(editPoint.getCenterY() + y);
        });
        SaveState.autoSave();
    }

    public void move() {
        new Thread(() -> {
            Point2D prevMousePos = LensrStart.mousePos;
            Point2D prevCenter = new Point2D(getCenterX(), getCenterY());

            while (LensrStart.isMousePressed && isEdited) {
                double x = prevCenter.getX() + (LensrStart.mousePos.getX() - prevMousePos.getX());
                double y = prevCenter.getY() + (LensrStart.mousePos.getY() - prevMousePos.getY());

                // Update the UI on the JavaFX application thread
                Platform.runLater(() -> {
                    setCenterX(x);
                    setCenterY(y);

                    // Update editPoints location
                    Bounds mirrorBounds = getLayoutBounds();

                    for (int i = 0; i < objectEditPoints.size() - 1; i++) {
                        double x1 = (i == 1 || i == 2) ? mirrorBounds.getMaxX() : mirrorBounds.getMinX();
                        double y1 = (i == 2 || i == 3) ? mirrorBounds.getMaxY() : mirrorBounds.getMinY();

                        objectEditPoints.get(i).setCenterX(x1);
                        objectEditPoints.get(i).setCenterY(y1);
                    }
                    objectEditPoints.get(4).setCenter(new Point2D(getCenterX(), getCenterY()));
                });

                synchronized (LensrStart.lock) {
                    try {
                        LensrStart.lock.wait(10); // Adjust the wait time as needed
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
            double centerX, centerY, radius;

            while (LensrStart.isMousePressed && isEdited) {
                // Resizing standard based on Photoshop and MS Paint :)
                if (LensrStart.altPressed) {
                    centerX = anchor.getX();
                    centerY = anchor.getY();
                    radius = Math.min(Math.abs(anchor.getX() - LensrStart.mousePos.getX()), Math.abs(anchor.getY() - LensrStart.mousePos.getY()));
                } else {
                    double minDistance = Math.min(Math.abs(anchor.getX() - LensrStart.mousePos.getX()), Math.abs(anchor.getY() - LensrStart.mousePos.getY())) / 2;
                    centerX = anchor.getX() + (LensrStart.mousePos.getX() > anchor.getX() ? minDistance : -minDistance);
                    centerY = anchor.getY() + (LensrStart.mousePos.getY() > anchor.getY() ? minDistance : -minDistance);
                    radius = Math.min(Math.abs(centerX - LensrStart.mousePos.getX()), Math.abs(centerY - LensrStart.mousePos.getY()));
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
                    Bounds mirrorBounds = getLayoutBounds();

                    for (int i = 0; i < objectEditPoints.size(); i++) {
                        double x = (i == 1 || i == 2) ? mirrorBounds.getMaxX() : mirrorBounds.getMinX();
                        double y = (i == 2 || i == 3) ? mirrorBounds.getMaxY() : mirrorBounds.getMinY();

                        objectEditPoints.get(i).setCenterX(x);
                        objectEditPoints.get(i).setCenterY(y);
                    }
                    objectEditPoints.get(4).setCenter(new Point2D(getCenterX(), getCenterY()));
                });

                synchronized (LensrStart.lock) {
                    try {
                        LensrStart.lock.wait(10); // Adjust the wait time as needed
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
        out.writeDouble(getCenterX());
        out.writeDouble(getCenterY());
        out.writeDouble(getRadius());
    }

    @Serial
    private void readObject(java.io.ObjectInputStream in) throws Exception {
        in.defaultReadObject();
        setCenterX(in.readDouble());
        setCenterY(in.readDouble());
        setRadius(in.readDouble());

        // Initialize transient fields
        group = new Group();
        objectEditPoints = new ArrayList<>();
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