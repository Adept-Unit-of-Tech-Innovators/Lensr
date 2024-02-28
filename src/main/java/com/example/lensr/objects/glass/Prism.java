package com.example.lensr.objects.glass;

import com.example.lensr.ui.EditPoint;
import com.example.lensr.objects.Editable;
import com.example.lensr.objects.mirrors.LineMirror;
import com.example.lensr.saveloadkit.SaveState;
import com.example.lensr.UserControls;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeType;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static com.example.lensr.LensrStart.*;

public class Prism extends Polygon implements Glass, Editable, Serializable {
    private transient Group group = new Group();
    private transient List<EditPoint> objectEditPoints = new ArrayList<>();
    public boolean isEdited;
    public boolean hasBeenClicked;
    private transient LineMirror closestIntersectionSegment;
    public double coefficientA;
    public double coefficientB;

    public Prism(double x, double y, double coefficientA, double coefficientB) {
        super(x, y, x, y, x, y);
        this.coefficientA = coefficientA;
        this.coefficientB = coefficientB;
    }

    public void create() {
        setFill(Color.TRANSPARENT);
        setStroke(mirrorColor);
        setStrokeWidth(globalStrokeWidth);
        setStrokeType(StrokeType.CENTERED);

        // Place edit points
        objectEditPoints.add(new EditPoint(getPoints().get(0), getPoints().get(1), EditPoint.Style.Primary));
        objectEditPoints.add(new EditPoint(getPoints().get(2), getPoints().get(3), EditPoint.Style.Primary));
        objectEditPoints.add(new EditPoint(getPoints().get(4), getPoints().get(5), EditPoint.Style.Primary));

        objectEditPoints.get(0).setOnClickEvent(event -> scale(0, 1));
        objectEditPoints.get(1).setOnClickEvent(event -> scale(2, 3));
        objectEditPoints.get(2).setOnClickEvent(event -> scale(4, 5));

        objectEditPoints.add(new EditPoint(getCenterX(), getCenterY(), EditPoint.Style.Secondary));
        objectEditPoints.get(3).setOnClickEvent(event -> move());

        objectEditPoints.forEach(editPoint -> editPoint.setVisible(false));

        group.getChildren().add(this);
        group.getChildren().addAll(objectEditPoints);
        root.getChildren().addAll(group);
    }

    @Override
    public void delete() {
        coefficientASlider.hide();
        coefficientBSlider.hide();
        editPoints.removeAll(objectEditPoints);
        editedShape = null;
        lenses.remove(this);
        root.getChildren().remove(group);
    }

    @Override
    public void copy() {
        Prism newPrism = new Prism(getPoints().get(0), getPoints().get(1), coefficientA, coefficientB);
        newPrism.getPoints().setAll(getPoints());
        newPrism.create();
        newPrism.moveBy(10, 10);
        lenses.add(newPrism);
        UserControls.closeCurrentEdit();
        newPrism.openObjectEdit();
    }

    @Override
    public void openObjectEdit() {
        coefficientASlider.setCurrentSource(this);
        coefficientBSlider.setCurrentSource(this);
        coefficientASlider.show();
        coefficientBSlider.show();

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
        coefficientASlider.hide();
        coefficientBSlider.hide();
        isEdited = false;
        if (objectEditPoints != null && editedShape instanceof Group) {
            editPoints.removeAll(objectEditPoints);
            objectEditPoints.forEach(editPoint -> {
                editPoint.setVisible(false);
                editPoint.hasBeenClicked = false;
            });
        }

        editedShape = null;
        updateLightSources();
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
        SaveState.autoSave();
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
            SaveState.autoSave();
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
            SaveState.autoSave();
        }).start();
    }

    public void move() {
        new Thread(() -> {
            Point2D prevMousePos = mousePos;

            while (isMousePressed) {
                double deltaX = mousePos.getX() - prevMousePos.getX();
                double deltaY = mousePos.getY() - prevMousePos.getY();

                // Update the UI on the JavaFX application thread
                Platform.runLater(() -> {
                    List<Double> newPoints = new ArrayList<>();
                    for (int i = 0; i < getPoints().size(); i++) {
                        newPoints.add(getPoints().get(i) + (i % 2 == 0 ? deltaX : deltaY));
                    }
                    getPoints().setAll(newPoints);

                    objectEditPoints.forEach(editPoint -> {
                        editPoint.setCenterX(editPoint.getCenterX() + deltaX);
                        editPoint.setCenterY(editPoint.getCenterY() + deltaY);
                    });
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
            SaveState.autoSave();
        }).start();
    }

    @Serial
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        for (int i = 0; i < getPoints().size(); i++) {
            out.writeDouble(getPoints().get(i));
        }
    }

    @Serial
    private void readObject(java.io.ObjectInputStream in) throws Exception {
        in.defaultReadObject();
        for (int i = 0; i < 6; i++) {
            getPoints().add(i, in.readDouble());
        }

        // Initialize transient fields
        group = new Group();
        objectEditPoints = new ArrayList<>();
        isEdited = false;
        hasBeenClicked = false;
        closestIntersectionSegment = null;
    }


    public double getCenterX() {
        return (getPoints().get(0) + getPoints().get(2) + getPoints().get(4)) / 3;        // centroid formula
    }

    public double getCenterY() {
        return (getPoints().get(1) + getPoints().get(3) + getPoints().get(5)) / 3;         // centroid formula
    }

    public double getCoefficientA() {
        return coefficientA;
    }
    public double getCoefficientB() {
        return coefficientB;
    }
    public void setCoefficientA(double coeficientA) {
        this.coefficientA = coeficientA;
    }
    public void setCoefficientB(double coeficientB) {
        this.coefficientB = coeficientB;
    }
    public void setClosestIntersectionSegment(LineMirror closestIntersectionSegment) {
        this.closestIntersectionSegment = closestIntersectionSegment;
    }

    public LineMirror getClosestIntersectionSegment() {
        return closestIntersectionSegment;
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
