package com.example.lensr.objects;

import com.example.lensr.EditPoint;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeType;

import java.util.ArrayList;
import java.util.List;

import static com.example.lensr.Intersections.getObjectOutline;
import static com.example.lensr.LensrStart.*;
import static com.example.lensr.MirrorMethods.*;

public class EllipseMirror extends Ellipse implements Editable {
    public Group group = new Group();
    // The outline of the object for ray intersection
    public Shape outline = getObjectOutline(this);
    List<EditPoint> objectEditPoints = new ArrayList<>();
    public boolean isEdited;
    public boolean hasBeenClicked;
    public double reflectivity = 1;

    public EllipseMirror(double centerX, double centerY, double radiusX, double radiusY) {
        setCenterX(centerX);
        setCenterY(centerY);
        setRadiusX(radiusX);
        setRadiusY(radiusY);
    }


    public void create() {
        setFill(Color.TRANSPARENT);
        setStroke(mirrorColor);
        setStrokeWidth(globalStrokeWidth);
        setStrokeType(StrokeType.OUTSIDE);

        group.getChildren().add(this);
        group.getChildren().add(outline);
        root.getChildren().add(group);
    }

    @Override
    public void openObjectEdit() {
        reflectivitySlider.setCurrentSource(this);
        reflectivitySlider.show();

        // Defocus the text field
        root.requestFocus();

        hasBeenClicked = true;
        isEdited = true;

        // Place edit points
        Bounds mirrorBounds = getLayoutBounds();
        objectEditPoints.add(new EditPoint(mirrorBounds.getMinX(), mirrorBounds.getMinY()));
        objectEditPoints.add(new EditPoint(mirrorBounds.getMaxX(), mirrorBounds.getMinY()));
        objectEditPoints.add(new EditPoint(mirrorBounds.getMaxX(), mirrorBounds.getMaxY()));
        objectEditPoints.add(new EditPoint(mirrorBounds.getMinX(), mirrorBounds.getMaxY()));

        // Define what happens when an edit point is clicked
        for (EditPoint editPoint : objectEditPoints) {
            editPoint.setOnClickEvent(event -> {
                // Scale the mirror with the opposite edit point as an anchor
                EditPoint oppositeEditPoint = objectEditPoints.get(((objectEditPoints.indexOf(editPoint)) + 2)  % 4);
                scale(oppositeEditPoint.getCenter());
            });
        }

        editPoints.addAll(objectEditPoints);
        group.getChildren().addAll(objectEditPoints);
        editedShape = group;
    }

    @Override
    public void closeObjectEdit() {
        reflectivitySlider.hide();
        isEdited = false;
        if (objectEditPoints != null && editedShape instanceof Group editedGroup) {
            editedGroup.getChildren().removeAll(objectEditPoints);
            editPoints.removeAll(objectEditPoints);
            objectEditPoints.clear();
        }
        editedShape = null;
        updateLightSources();
    }


    private void updateOutline() {
        Ellipse ellipse = new Ellipse(this.getCenterX(), this.getCenterY(), this.getRadiusX(), this.getRadiusY());
        ellipse.setFill(Color.TRANSPARENT);
        ellipse.setStroke(Color.BLACK);
        outline = getObjectOutline(ellipse);
        outline.setStrokeWidth(1);
    }


    public void setReflectivity(double reflectivity) {
        this.reflectivity = reflectivity;
    }


    public double getReflectivity() {
        return reflectivity;
    }



    public void scale(Point2D anchor) {
        new Thread(() -> {
            double centerX, centerY, radiusX, radiusY;

            while (isMousePressed) {
                // Resizing standard based on Photoshop and MS Paint :)
                if (altPressed && shiftPressed) {
                    centerX = anchor.getX();
                    centerY = anchor.getY();
                    radiusX = radiusY = Math.min( Math.abs(anchor.getX() - mousePos.getX()), Math.abs(anchor.getY() - mousePos.getY()) );
                }
                else if (altPressed) {
                    centerX = anchor.getX();
                    centerY = anchor.getY();
                    radiusX = Math.abs(mousePos.getX() - centerX);
                    radiusY = Math.abs(mousePos.getY() - centerY);
                }
                else if (shiftPressed) {
                    double minDistance = Math.min( Math.abs(anchor.getX() - mousePos.getX()), Math.abs(anchor.getY() - mousePos.getY()) ) / 2;
                    centerX = anchor.getX() + (mousePos.getX() > anchor.getX() ? minDistance : -minDistance);
                    centerY = anchor.getY() + (mousePos.getY() > anchor.getY() ? minDistance : -minDistance);
                    radiusX = radiusY = Math.min( Math.abs(centerX - mousePos.getX()), Math.abs(centerY - mousePos.getY()) );
                }
                else {
                    centerX = anchor.getX() + ( (mousePos.getX() - anchor.getX()) / 2);
                    centerY = anchor.getY() + ( (mousePos.getY() - anchor.getY()) / 2);
                    radiusX = Math.abs(mousePos.getX() - centerX);
                    radiusY = Math.abs(mousePos.getY() - centerY);
                }

                double finalCenterX = centerX;
                double finalCenterY = centerY;
                double finalRadiusX = radiusX;
                double finalRadiusY = radiusY;

                // Update the UI on the JavaFX application thread
                Platform.runLater(() -> {
                    setCenterX(finalCenterX);
                    setCenterY(finalCenterY);
                    setRadiusX(finalRadiusX);
                    setRadiusY(finalRadiusY);

                    // Update editPoints location
                    Bounds mirrorBounds = getLayoutBounds();

                    for (int i = 0; i < objectEditPoints.size(); i++) {
                        double x = (i == 1 || i == 2) ? mirrorBounds.getMaxX() : mirrorBounds.getMinX();
                        double y = (i == 2 || i == 3) ? mirrorBounds.getMaxY() : mirrorBounds.getMinY();

                        objectEditPoints.get(i).setCenterX(x);
                        objectEditPoints.get(i).setCenterY(y);
                    }

                    updateOutline();
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