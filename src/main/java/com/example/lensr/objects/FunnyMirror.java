package com.example.lensr.objects;

import com.example.lensr.EditPoint;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Shape;
import javafx.scene.transform.Scale;

import java.util.ArrayList;
import java.util.List;

import static com.example.lensr.LensrStart.*;
import static com.example.lensr.MirrorMethods.updateLightSources;

public class FunnyMirror extends Polyline implements Editable{
    public Group group = new Group();
    // The outline of the object for ray intersection
    public List<EditPoint> objectEditPoints = new ArrayList<>();
    public boolean isEdited;
    public boolean hasBeenClicked;
    // The percentage of light that is reflected, 0 - no light is reflected, 1 - perfect reflection
    public double reflectivity = 1;
    LineMirror closestIntersectionSegment;

    public FunnyMirror() {

    }

    public void draw() {
        setStrokeWidth(globalStrokeWidth);
        setStroke(mirrorColor);
        group.getChildren().add(this);
        root.getChildren().add(group);
        taskPool.execute(() -> {
            getPoints().addAll(mousePos.getX(), mousePos.getY());
            int index = 2;
            while (isMousePressed) {
                if ((Math.abs(getPoints().get(index - 2) - mousePos.getX()) > 10) || (Math.abs(getPoints().get(index - 1) - mousePos.getY()) > 10)) {
                    getPoints().addAll(mousePos.getX(), mousePos.getY());
                    index = index + 2;
                }
                if (!objectEditPoints.isEmpty()) {
                    // Update editPoints location
                    Bounds mirrorBounds = getLayoutBounds();

                    for (int i = 0; i < objectEditPoints.size(); i++) {
                        double x = (i == 1 || i == 2) ? mirrorBounds.getMaxX() : mirrorBounds.getMinX();
                        double y = (i == 2 || i == 3) ? mirrorBounds.getMaxY() : mirrorBounds.getMinY();

                        objectEditPoints.get(i).setCenterX(x);
                        objectEditPoints.get(i).setCenterY(y);
                    }
                }
            }
        });
    }

    public void scale(EditPoint anchorPoint) {
        taskPool.execute(() -> {
            double threshold = 1.0; // We only want to scale the object if the mouse has moved more than 1 pixel
            while (isMousePressed) {
                double originalWidth = getLayoutBounds().getWidth();
                double originalHeight = getLayoutBounds().getHeight();
                double widthToHeightRatio = originalWidth / originalHeight;

                double deltaX, deltaY;
                double tempDeltaX = Math.abs(mousePos.getX() - anchorPoint.getCenterX());
                double tempDeltaY = Math.abs(mousePos.getY() - anchorPoint.getCenterY());

                deltaX = (shiftPressed && widthToHeightRatio < 1) ? tempDeltaY * widthToHeightRatio : tempDeltaX;
                deltaY = (shiftPressed && widthToHeightRatio > 1) ? tempDeltaX / widthToHeightRatio : tempDeltaY;


                if (deltaX > threshold || deltaY > threshold) {
                    for (int i = 0; i < getPoints().size(); i += 2) {
                        double pointX = getPoints().get(i);
                        double pointY = getPoints().get(i + 1);

                        // Calculate adjusted point's coordinates using proportions
                        double newPointX = anchorPoint.getCenterX() + ((pointX - anchorPoint.getCenterX()) * deltaX / originalWidth);
                        double newPointY = anchorPoint.getCenterY() + ((pointY - anchorPoint.getCenterY()) * deltaY / originalHeight);

                        // Update the point's coordinates
                        int finalI = i;
                        Platform.runLater(() -> {
                            getPoints().set(finalI, newPointX);
                            getPoints().set(finalI + 1, newPointY);
                        });
                    }

                    // Update the editPoints location
                    if (!objectEditPoints.isEmpty()) {
                        Platform.runLater(() -> {
                            Bounds mirrorBounds = getLayoutBounds();

                            for (int i = 0; i < objectEditPoints.size(); i++) {
                                if (objectEditPoints.get(i).equals(anchorPoint)) continue;
                                double x = (i == 1 || i == 2) ? mirrorBounds.getMaxX() : mirrorBounds.getMinX();
                                double y = (i == 2 || i == 3) ? mirrorBounds.getMaxY() : mirrorBounds.getMinY();

                                objectEditPoints.get(i).setCenterX(x);
                                objectEditPoints.get(i).setCenterY(y);
                            }
                        });
                    }
                }

                // The higher the value, the faster you can move the mouse without deforming the object, but at the cost of responsiveness
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    @Override
    public void delete() {
        mirrors.remove(this);
        root.getChildren().remove(group);
    }


    public void setReflectivity(double reflectivity) {
        this.reflectivity = reflectivity;
    }


    public double getReflectivity() {
        return reflectivity;
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
                if (editPoint == objectEditPoints.get(0)) {
                    scale(objectEditPoints.get(2));
                } else if (editPoint == objectEditPoints.get(1)) {
                    scale(objectEditPoints.get(3));
                } else if (editPoint == objectEditPoints.get(2)) {
                    scale(objectEditPoints.get(0));
                } else if (editPoint == objectEditPoints.get(3)) {
                    scale(objectEditPoints.get(1));
                }
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
            objectEditPoints.clear();
        }
        editedShape = null;
        updateLightSources();
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
        return Shape.intersect(this, mouseHitbox).getLayoutBounds().getWidth() != -1;
    }
}
