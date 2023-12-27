package com.example.lensr;

import javafx.application.Platform;
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
    Group group = new Group();
    Rotate rotate = new Rotate();
    // Extended hitbox for easier editing
    Rectangle hitbox;
    List<Rectangle> editPoints = new ArrayList<>();
    // The percentage of light that is reflected, 0 - no light is reflected, 1 - perfect reflection
    double reflectivity = 1;
    double rotation = 0;
    boolean isEdited;
    boolean isEditPointClicked;

    public LineMirror(double startX, double startY) {
        setStartX(startX);
        setStartY(startY);
        setEndX(startX);
        setEndY(startY);
    }


    public void create() {
        setFill(Color.TRANSPARENT);
        setStroke(mirrorColor);
        setStrokeWidth(globalStrokeWidth);

        setOnMouseClicked(mouseEvent -> {
            if (isEditMode && !isEdited) openObjectEdit();
        });

        hitbox = createRectangleHitbox();

        group.getChildren().add(this);
        group.getChildren().add(hitbox);
        root.getChildren().add(group);
    }

    private void openObjectEdit() {
        xPressed.setValue(false);
        zPressed.setValue(false);

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

        // Place edit points
        editPoints.add(new Rectangle(getStartX() - editPointSize / 2, getStartY() - editPointSize / 2, editPointSize,editPointSize));
        editPoints.add(new Rectangle(getEndX() - editPointSize / 2, getEndY() - editPointSize / 2, editPointSize, editPointSize));

        for (Rectangle editPoint : editPoints) {
            editPoint.setFill(Color.RED);
            editPoint.setStrokeWidth(0);
            editPoint.setOnMouseEntered(mouseEvent -> {
                if (!isEditPointClicked) {
                    scene.setCursor(Cursor.HAND);
                }
            });
            editPoint.setOnMouseExited(mouseEvent -> {
                if (!isEditPointClicked) {
                    scene.setCursor(Cursor.DEFAULT);
                }
            });
            editPoint.setOnMousePressed(this::handleEditPointPressed);
            editPoint.setOnMouseReleased(this::handleEditPointReleased);
        }
        group.getChildren().addAll(editPoints);
        editedShape = group;
    }


    private void handleEditPointPressed(MouseEvent event) {
        isMousePressed = true;
        isEditPointClicked = true;
        scene.setCursor(Cursor.CLOSED_HAND);

        // Scale the mirror with the opposite edit point as an anchor
        //noinspection SuspiciousMethodCalls (it's very sussy)
        Rectangle oppositeEditPoint = editPoints.get(1 - editPoints.indexOf(event.getSource()));
        scale(new Point2D(oppositeEditPoint.getX() + editPointSize / 2, oppositeEditPoint.getY() + editPointSize / 2));
    }


    private void handleEditPointReleased(MouseEvent event) {
        isMousePressed = false;
        isEditPointClicked = false;

        for (Rectangle editPoint : editPoints) {
            if (editPoint.contains(mousePos)) {
                scene.setCursor(Cursor.HAND);
                break;
            }
            else scene.setCursor(Cursor.DEFAULT);
        }

        event.consume();
    }

    public void closeObjectEdit() {
        isEdited = false;
        removeIfOverlaps();
        if (editPoints != null && editedShape instanceof Group editedGroup) {
            editedGroup.getChildren().removeAll(editPoints);
            editPoints.clear();
        }
    }


    private Rectangle createRectangleHitbox() {
        hitbox = new Rectangle();
        hitbox.setHeight(30);
        hitbox.setFill(Color.TRANSPARENT);
        hitbox.setStroke(Color.TRANSPARENT);
        hitbox.setStrokeWidth(0);
        hitbox.toBack();
        hitbox.getTransforms().add(rotate);
        hitbox.setOnMouseClicked(this.getOnMouseClicked());
        updateHitbox();
        return hitbox;
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


    public void scale(Point2D anchor) {
        new Thread(() -> {
            double startX, startY, endX, endY;

            while (isMousePressed) {
                boolean intersects = false;

                // Resizing standard based on Photoshop and MS Paint :)
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
                    this.setStartX(startX);
                    this.setStartY(startY);
                    this.setEndX(endX);
                    this.setEndY(endY);
                }

                // Update editPoints location
                if (isEditPointClicked) {
                    editPoints.get(0).setX(getStartX() - editPointSize / 2);
                    editPoints.get(0).setY(getStartY() - editPointSize / 2);
                    editPoints.get(1).setX(getEndX() - editPointSize / 2);
                    editPoints.get(1).setY(getEndY() - editPointSize / 2);
                }

                updateHitbox();

                // Update the UI on the JavaFX application thread
                Platform.runLater(() -> {
                    // Update UI components or perform other UI-related tasks
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