package com.example.lensr.objects;

import com.example.lensr.EditPoint;
import com.example.lensr.MirrorMethods;
import com.example.lensr.UserControls;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeType;

import java.util.ArrayList;
import java.util.List;

import static com.example.lensr.LensrStart.*;

public class Prism extends Polygon implements Editable{

    public Group group = new Group();
    List<EditPoint> objectEditPoints = new ArrayList<>();
    public boolean isEdited;
    public boolean hasBeenClicked;
    // TODO: Add the properties of the Prism class

    public Prism(double x, double y) {
        super(x, y, x, y, x, y);
    }

    public void create() {
        setFill(Color.TRANSPARENT);
        setStroke(mirrorColor);
        setStrokeWidth(globalStrokeWidth);
        setStrokeType(StrokeType.CENTERED);

        // Place edit points
        objectEditPoints.add(new EditPoint(getPoints().get(0), getPoints().get(1)));
        objectEditPoints.add(new EditPoint(getPoints().get(2), getPoints().get(3)));
        objectEditPoints.add(new EditPoint(getPoints().get(4), getPoints().get(5)));

        objectEditPoints.get(0).setOnClickEvent(event -> scale(0, 1));
        objectEditPoints.get(1).setOnClickEvent(event -> scale(2, 3));
        objectEditPoints.get(2).setOnClickEvent(event -> scale(4, 5));

        objectEditPoints.add(new EditPoint(getCenterX(), getCenterY()));
        objectEditPoints.get(3).setOnClickEvent(event -> move());

        objectEditPoints.forEach(editPoint -> editPoint.setVisible(false));

        group.getChildren().add(this);
        group.getChildren().addAll(objectEditPoints);
        root.getChildren().addAll(group);
    }

    @Override
    public void delete() {
        mirrors.remove(this);
        root.getChildren().remove(group);
    }

    @Override
    public void copy() {
        Prism newPrism = new Prism(getPoints().get(0), getPoints().get(1));
        newPrism.getPoints().setAll(getPoints());
        newPrism.create();
        newPrism.moveBy(10, 10);
        mirrors.add(newPrism);
        UserControls.closeCurrentEdit();
        newPrism.openObjectEdit();
    }

    @Override
    public void openObjectEdit() {
        // TODO: Implement the sliders for the Prism class

        // Defocus the text fields
        root.requestFocus();

        // Show the edit points (they're created once to "remember" which vertices to edit)
        objectEditPoints.forEach(editPoint -> {
            editPoint.setVisible(true);
            editPoint.toFront();
        });

        hasBeenClicked = true;
        isEdited = true;

        editPoints.addAll(objectEditPoints);
        editedShape = group;
    }

    @Override
    public void closeObjectEdit() {
        // TODO: Implement the closing of the sliders for the Prism class

        isEdited = false;
        if (objectEditPoints != null && editedShape instanceof Group) {
            editPoints.removeAll(objectEditPoints);
            objectEditPoints.forEach(editPoint -> {
                editPoint.setVisible(false);
                editPoint.hasBeenClicked = false;
            });
        }
        editedShape = null;
        MirrorMethods.updateLightSources();
    }

    @Override
    public void moveBy(double x, double y) {
        List<Double> newPoints = new ArrayList<>();
        for (int i = 0; i < getPoints().size(); i++) {
            newPoints.add(getPoints().get(i) + (i % 2 == 0 ? x : y));
        }
        getPoints().setAll(newPoints);

        objectEditPoints.forEach(editPoint -> {
            editPoint.setCenterX(editPoint.getCenterX() + x);
            editPoint.setCenterY(editPoint.getCenterY() + y);
        });
    }

    public void draw() {
        new Thread(() -> {
            while (isMousePressed) {
                Platform.runLater(() -> {
                    getPoints().set(2, mousePos.getX());
                    getPoints().set(3, getPoints().get(1));
                    getPoints().set(4, mousePos.getX() - (mousePos.getX() - getPoints().get(0)) / 2);
                    getPoints().set(5, mousePos.getY());

                    objectEditPoints.get(0).setCenterX(getPoints().get(0));
                    objectEditPoints.get(0).setCenterY(getPoints().get(1));
                    objectEditPoints.get(1).setCenterX(getPoints().get(2));
                    objectEditPoints.get(1).setCenterY(getPoints().get(3));
                    objectEditPoints.get(2).setCenterX(getPoints().get(4));
                    objectEditPoints.get(2).setCenterY(getPoints().get(5));
                    objectEditPoints.get(3).setCenterX(getCenterX());
                    objectEditPoints.get(3).setCenterY(getCenterY());
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

    public void scale(int anchorXIndex, int anchorYIndex) {
        new Thread(() -> {
            Point2D prevMousePos = mousePos;

            while (isMousePressed) {
                double deltaX = mousePos.getX() - prevMousePos.getX();
                double deltaY = mousePos.getY() - prevMousePos.getY();

                // Update the UI on the JavaFX application thread
                Platform.runLater(() -> {
                    getPoints().set(anchorXIndex, getPoints().get(anchorXIndex) + deltaX);
                    getPoints().set(anchorYIndex, getPoints().get(anchorYIndex) + deltaY);

                    int draggedEditPointIndex = anchorXIndex / 2;
                    objectEditPoints.get(draggedEditPointIndex).setCenterX(getPoints().get(anchorXIndex));
                    objectEditPoints.get(draggedEditPointIndex).setCenterY(getPoints().get(anchorYIndex));
                    objectEditPoints.get(3).setCenterX(getCenterX());
                    objectEditPoints.get(3).setCenterY(getCenterY());
                });

                prevMousePos = mousePos;
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

    public void move() {
        new Thread(() -> {
            Point2D prevMousePos = mousePos;

            while (isMousePressed) {
                double deltaX = mousePos.getX() - prevMousePos.getX();
                double deltaY = mousePos.getY() - prevMousePos.getY();

                // Update the UI on the JavaFX application thread
                Platform.runLater(() -> moveBy(deltaX, deltaY));

                prevMousePos = mousePos;
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

    public double getCenterX() {
        return (getPoints().get(0) + getPoints().get(2) + getPoints().get(4)) / 3;        // centroid formula
    }

    public double getCenterY() {
        return (getPoints().get(1) + getPoints().get(3) + getPoints().get(5)) / 3;         // centroid formula
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
        return Shape.intersect(this, mouseHitbox).getBoundsInLocal().getWidth() != -1;
    }
}
