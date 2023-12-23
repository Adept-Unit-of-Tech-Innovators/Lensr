package com.example.lensr;

import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.geometry.Point2D;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.transform.Rotate;

import java.util.ArrayList;
import java.util.List;

import static com.example.lensr.LensrStart.*;

public class LineMirror extends Line{
    // The percentage of light that is reflected, 0 - no light is reflected, 1 - perfect reflection
    double reflectivity = 1;
    // How much light is scattered instead of reflected, 0 - all light is scattered, 1 - all light is perfectly reflected
    // Not sure if we should implement this as the lower the specular, the less the object behaves like a mirror. Mirrors always have high specular.
    double specular;
    Point2D anchor;
    Group group = new Group();
    Rectangle hitbox = new Rectangle();
    double rotation = 0;
    Rotate rotate = new Rotate();
    List<Rectangle> editPoints = new ArrayList<>();
    boolean isEdited;
    boolean isEditPointClicked;

    public LineMirror(double mouseX, double mouseY) {
        anchor = new Point2D(mouseX, mouseY);
        setStartX(mouseX);
        setStartY(mouseY);
        setEndX(mouseX);
        setEndY(mouseY);

    }


    public void create() {
        setFill(Color.TRANSPARENT);
        setStroke(mirrorColor);
        setStrokeWidth(globalStrokeWidth);

        setOnMouseClicked(mouseEvent -> {
            if (isEditMode && !isEdited) {
                openObjectEdit();
            }
        });

        group.getChildren().add(this);
        createRectangleHitbox();
        root.getChildren().add(group);
    }

    private void openObjectEdit() {
        xPressed = false;
        zPressed = false;

        for (Object mirror : mirrors) {
            if (mirror instanceof EllipseMirror ellipseMirror) {
                if (ellipseMirror.isEdited) {
                    ellipseMirror.isEditPointClicked = false;
                    ellipseMirror.closeObjectEdit();

                }
            }
            else {
                if (mirror instanceof LineMirror lineMirror) {
                    if (lineMirror.isEdited) {
                        lineMirror.isEditPointClicked = false;
                        lineMirror.closeObjectEdit();
                    }
                }
            }
        }
        isEdited = true;

        Bounds mirrorBounds = getLayoutBounds();
        editPoints.add(new Rectangle(getStartX(), getStartY(), 8, 8));
        editPoints.add(new Rectangle(getEndX(), getEndY(), 8, 8));

        for (Rectangle editPoint : editPoints) {
            editPoint.setFill(Color.RED);
            editPoint.setStrokeWidth(0);
            editPoint.setOnMousePressed(this::handleEditPointPressed);
            editPoint.setOnMouseReleased(this::handleEditPointReleased);
        }
        group.getChildren().addAll(editPoints);
        editedShape = group;
    }

    private void handleEditPointPressed(MouseEvent event) {
        isMousePressed = true;
        isEditPointClicked = true;
        scene.setCursor(Cursor.HAND);

        // Scale the mirror with the opposite edit point as an anchor
        // sussy
        int editPointIndex = editPoints.indexOf(event.getSource());
        anchor = new Point2D(editPoints.get(1 - editPointIndex).getX(), editPoints.get(1 - editPointIndex).getY());
        scale();
    }


    private void handleEditPointReleased(MouseEvent event) {
        isMousePressed = false;
        isEditPointClicked = false;
        scene.setCursor(Cursor.DEFAULT);
        event.consume();
    }

    public void closeObjectEdit() {
        isEdited = false;
        removeIfOverlaps();
        if (editPoints != null && editedShape instanceof Group group) {
            group.getChildren().removeAll(editPoints);
            editPoints.clear();
        }
    }


    private void createRectangleHitbox() {
        hitbox = new Rectangle();
        hitbox.setHeight(30);
        hitbox.setFill(Color.TRANSPARENT);
        hitbox.setStroke(Color.TRANSPARENT);
        hitbox.setStrokeWidth(0);
        hitbox.toBack();
        hitbox.getTransforms().add(rotate);

        updateHitbox();

        hitbox.setOnMouseClicked(this.getOnMouseClicked());
        group.getChildren().add(hitbox);
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

    public void setReflectivity(double reflectivity) {
        this.reflectivity = reflectivity;
    }


    public double getReflectivity() {
        return reflectivity;
    }


    public double getLength() {
        return Math.sqrt(
                Math.pow( getEndX() - getStartX(), 2) +
                        Math.pow(getEndY() -getStartY(), 2)
        );
    }


    public void removeIfOverlaps() {
        // Remove the mirror if its size is 0
        if (this.getLength() == 0) {
            root.getChildren().remove(group);
            mirrors.remove(this);
            return;
        }

        for (Object mirror : mirrors) {
            if (mirror.equals(this)) continue;

            if (mirror instanceof Shape mirrorShape) {
                // If the mirror overlaps with another object, remove it
                if (Shape.intersect(this , mirrorShape).getLayoutBounds().getWidth() >= 0) {
                    root.getChildren().remove(group);
                    mirrors.remove(this);
                    return;
                }
            }
        }
    }


    public void scale() {
        new Thread(() -> {
            while (isMousePressed) {
                boolean intersects = false;
                double startX = getStartX(), startY = getStartY(), endX, endY;

                if (!altPressed) {
                    startX = (anchor.getX());
                    startY = (anchor.getY());
                }
                if (altPressed && shiftPressed) {
                    // Shift-mode calculations for actually half the mirror
                    double deltaX = mouseX - anchor.getX();
                    double deltaY = mouseY - anchor.getY();
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
                    startX = anchor.getX() - (mouseX - anchor.getX());
                    startY = anchor.getY() - (mouseY - anchor.getY());
                    endX = mouseX;
                    endY = mouseY;
                }
                else if (shiftPressed) {
                    double deltaX = mouseX - startX;
                    double deltaY = mouseY - startY;
                    double distance = Math.sqrt(Math.pow(deltaX, 2) + Math.pow(deltaY, 2));
                    double angle = Math.atan2(deltaY, deltaX);
                    double shiftedAngle = Math.round(angle * 4 / Math.PI) * Math.PI / 4;
                    endX = startX + distance * Math.cos(shiftedAngle);
                    endY = startY + distance * Math.sin(shiftedAngle);
                }
                else {
                    endX = mouseX;
                    endY = mouseY;
                }

                Line intersectionChecker = new Line(startX, startY, endX, endY);

                for (Object mirror : mirrors) {
                    if (mirror.equals(this)) continue;

                    if (mirror instanceof Shape mirrorShape) {
                        // If the mirror overlaps with another object, remove it
                        if (Shape.intersect(intersectionChecker , mirrorShape).getLayoutBounds().getWidth() >= 0) {
                            intersects = true;
                            break;
                        }
                    }
                }

                if (!intersects) {
                    // Apply all at once to avoid delays
                    this.setStartX(startX);
                    this.setStartY(startY);
                    this.setEndX(endX);
                    this.setEndY(endY);
                }

                // Update editPoints location
                if (isEditPointClicked) {
                    editPoints.get(0).setX(getStartX());
                    editPoints.get(0).setY(getStartY());
                    editPoints.get(1).setX(getEndX());
                    editPoints.get(1).setY(getEndY());
                }

                updateHitbox();

                // Update the UI on the JavaFX application thread
                Platform.runLater(() -> {
                    // Update UI components or perform other UI-related tasks
                    // Example: circle.setRadius(radius);
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

}



