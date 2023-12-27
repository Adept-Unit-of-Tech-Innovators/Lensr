package com.example.lensr;

import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeType;

import java.util.ArrayList;
import java.util.List;

import static com.example.lensr.Intersections.getObjectOutline;
import static com.example.lensr.LensrStart.*;

public class EllipseMirror extends Ellipse {
    // The outline of the object for ray intersection
    public Shape outline = getObjectOutline(this);
    // The percentage of light that is reflected, 0 - no light is reflected, 1 - perfect reflection
    double reflectivity = 0.99;
    // How much light is scattered instead of reflected, 0 - all light is scattered, 1 - all light is perfectly reflected
    // Not sure if we should implement this as the lower the specular, the less the object behaves like a mirror. Mirrors always have high specular.
    double specular;
    List<Rectangle> editPoints = new ArrayList<>();
    Group group = new Group();
    boolean isEdited;
    boolean isEditPointClicked;



    public EllipseMirror(double mouseX, double mouseY, double radiusX, double radiusY) {
        setCenterX(mouseX);
        setCenterY(mouseY);
        setRadiusX(radiusX);
        setRadiusY(radiusY);
    }


    public void create() {
        setFill(Color.TRANSPARENT);
        setStroke(mirrorColor);
        setStrokeWidth(globalStrokeWidth);
        setStrokeType(StrokeType.OUTSIDE);

        setOnMouseClicked(mouseEvent -> {
            if (isEditMode && !isEdited) openObjectEdit();
        });

        group.getChildren().add(this);
        group.getChildren().add(outline);
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
        Bounds mirrorBounds = getLayoutBounds();
        editPoints.add(new Rectangle(mirrorBounds.getMinX() - editPointSize / 2, mirrorBounds.getMinY() - editPointSize / 2, editPointSize, editPointSize));
        editPoints.add(new Rectangle(mirrorBounds.getMaxX() - editPointSize / 2, mirrorBounds.getMinY() - editPointSize / 2, editPointSize, editPointSize));
        editPoints.add(new Rectangle(mirrorBounds.getMaxX() - editPointSize / 2, mirrorBounds.getMaxY() - editPointSize / 2, editPointSize, editPointSize));
        editPoints.add(new Rectangle(mirrorBounds.getMinX() - editPointSize / 2, mirrorBounds.getMaxY() - editPointSize / 2, editPointSize, editPointSize));

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
        // sus
        int editPointIndex = editPoints.indexOf(event.getSource());
        scale(editPoints.get( (editPointIndex + 2) % 4).getX() + 4, editPoints.get( (editPointIndex + 2) % 4).getY() + 4);
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


    public void setReflectivity(double reflectivity) {
        this.reflectivity = reflectivity;
    }


    public double getReflectivity() {
        return reflectivity;
    }


    public void removeIfOverlaps() {
        // Remove the mirror if its size is 0
        if (this.getRadiusX() == 0 && this.getRadiusY() == 0) {
            root.getChildren().remove(this);
            mirrors.remove(this);
            return;
        }

        for (Object mirror : mirrors) {
            if (mirror.equals(this)) continue;

            if (mirror instanceof Shape mirrorShape) {
                // If the mirror overlaps with another object, remove it
                if (Shape.intersect(this , mirrorShape).getLayoutBounds().getWidth() >= 0) {
                    root.getChildren().remove(this);
                    mirrors.remove(this);
                    return;
                }
            }
        }
    }


    public void scale(double anchorX, double anchorY) {
        new Thread(() -> {
            double centerX, centerY, radiusX, radiusY;

            while (isMousePressed || isEditPointClicked) {
                boolean intersects = false;

                // Resizing standard based on Photoshop and MS Paint :)
                if (altPressed) {
                    centerX = anchorX;
                    centerY = anchorY ;

                    if (shiftPressed) {
                        radiusX = Math.min( Math.abs(anchorX - mouseX), Math.abs(anchorY - mouseY) );
                        //noinspection SuspiciousNameCombination
                        radiusY = radiusX;
                    } else {
                        radiusX = Math.abs(mouseX - centerX);
                        radiusY = Math.abs(mouseY - centerY);
                    }
                }
                else {
                    if (shiftPressed) {
                        double minDistance = Math.min( Math.abs(anchorX - mouseX), Math.abs(anchorY - mouseY) ) / 2;
                        centerX = (anchorX + (mouseX > anchorX ? minDistance : -minDistance));
                        centerY = (anchorY + (mouseY > anchorY ? minDistance : -minDistance));

                        radiusX = Math.min( Math.abs(centerX - mouseX), Math.abs(centerY - mouseY) );
                        //noinspection SuspiciousNameCombination
                        radiusY = radiusX;
                    }
                    else {
                        centerX = (anchorX + ( (mouseX - anchorX) / 2) );
                        centerY = (anchorY + ( (mouseY - anchorY) / 2) );

                        radiusX = Math.abs(mouseX - centerX);
                        radiusY = Math.abs(mouseY - centerY);
                    }
                }

                Ellipse intersectionChecker = new Ellipse(centerX, centerY, radiusX, radiusY);

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
                double prevCenterX = getCenterX(), prevCenterY = getCenterY();
                double prevRadiusX = getRadiusX(), prevRadiusY = getRadiusY();

                if (intersects) {
                    setCenterX(prevCenterX); setCenterY(prevCenterY);
                    setRadiusX(prevRadiusX); setRadiusY(prevRadiusY);
                }
                else {
                    setCenterX(centerX); setCenterY(centerY);
                    setRadiusX(radiusX); setRadiusY(radiusY);
                }

                // Update editPoints location
                if (isEditPointClicked) {
                    Bounds mirrorBounds = getLayoutBounds();
                    int offset = -4;

                    for (int i = 0; i < editPoints.size(); i++) {
                        double x = (i == 1 || i == 2) ? mirrorBounds.getMaxX() : mirrorBounds.getMinX();
                        double y = (i == 2 || i == 3) ? mirrorBounds.getMaxY() : mirrorBounds.getMinY();

                        editPoints.get(i).setX(x + offset);
                        editPoints.get(i).setY(y + offset);
                    }
                }

                // Update shape outline
                Ellipse ellipse = new Ellipse(this.getCenterX(), this.getCenterY(), this.getRadiusX(), this.getRadiusY());
                ellipse.setFill(Color.TRANSPARENT);
                ellipse.setStroke(Color.BLACK);
                outline = getObjectOutline(ellipse);
                outline.setStrokeWidth(1);

                // Update the UI on the JavaFX application thread
                Platform.runLater(() -> {
                    // Update UI components or perform other UI-related tasks
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
}

