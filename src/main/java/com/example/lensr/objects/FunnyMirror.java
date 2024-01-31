package com.example.lensr.objects;

import com.example.lensr.EditPoint;
import javafx.concurrent.Task;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Shape;

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
        Task<Void> task = new Task<>() {
            @Override
            public Void call() {
                List<Double> points = getPoints();
                // Add initial point
                points.add(mousePos.getX());
                points.add(mousePos.getY());
                int index = 2;
                while (isMousePressed) {
                    if (isCancelled()) {
                        break;
                    }
                    if ((Math.abs(points.get(index - 2) - mousePos.getX()) > 10) || (Math.abs(points.get(index - 1) - mousePos.getY()) > 10)) {
                        points.add(mousePos.getX());
                        points.add(mousePos.getY());
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
                return null;
            }
        };
        new Thread(task).start();
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
                // TODO: Implement FunnyMirror scaling
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
