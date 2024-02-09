package com.example.lensr.objects;

import com.example.lensr.EditPoint;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;

import java.util.ArrayList;
import java.util.List;

import static com.example.lensr.LensrStart.*;
import static com.example.lensr.MirrorMethods.*;

public class LightEater extends Circle implements Editable{
    public Group group = new Group();
    public List<EditPoint> objectEditPoints = new ArrayList<>();
    public boolean isEdited;
    public boolean hasBeenClicked;

    public LightEater(double centerX, double centerY, double radius) {
        setCenterX(centerX);
        setCenterY(centerY);
        setRadius(radius);
    }


    public void create() {
        setFill(Color.BLACK);
        setStroke(Color.BLACK);
        setStrokeWidth(globalStrokeWidth);
        setStrokeType(StrokeType.INSIDE);

        group.getChildren().add(this);
        root.getChildren().add(group);
    }

    @Override
    public void delete() {
        mirrors.remove(this);
        root.getChildren().remove(group);
    }

    @Override
    public void openObjectEdit() {
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
                EditPoint oppositeEditPoint = objectEditPoints.get(((objectEditPoints.indexOf(editPoint) + 2) % 4));
                scale(oppositeEditPoint.getCenter());
            });
        }

        editPoints.addAll(objectEditPoints);
        group.getChildren().addAll(objectEditPoints);
        editedShape = group;
    }

    @Override
    public void closeObjectEdit() {
        isEdited = false;
        if (objectEditPoints != null && editedShape instanceof Group editedGroup) {
            editedGroup.getChildren().removeAll(objectEditPoints);
            editPoints.removeAll(objectEditPoints);
            objectEditPoints.clear();
        }
        editedShape = null;
        updateLightSources();
    }


    public void scale(Point2D anchor) {
        new Thread(() -> {
            double centerX, centerY, radius;

            while (isMousePressed) {
                // Resizing standard based on Photoshop and MS Paint :)
                if (altPressed) {
                    centerX = anchor.getX();
                    centerY = anchor.getY();
                    radius  = Math.min( Math.abs(anchor.getX() - mousePos.getX()), Math.abs(anchor.getY() - mousePos.getY()) );
                }
                else {
                    double minDistance = Math.min( Math.abs(anchor.getX() - mousePos.getX()), Math.abs(anchor.getY() - mousePos.getY()) ) / 2;
                    centerX = anchor.getX() + (mousePos.getX() > anchor.getX() ? minDistance : -minDistance);
                    centerY = anchor.getY() + (mousePos.getY() > anchor.getY() ? minDistance : -minDistance);
                    radius = Math.min( Math.abs(centerX - mousePos.getX()), Math.abs(centerY - mousePos.getY()) );
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