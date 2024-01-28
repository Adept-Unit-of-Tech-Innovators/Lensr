package com.example.lensr.objects;

import com.example.lensr.EditPoint;
import javafx.concurrent.Task;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.shape.Polyline;

import java.util.ArrayList;
import java.util.List;

import static com.example.lensr.LensrStart.*;
import static com.example.lensr.MirrorMethods.updateLightSources;

public class FunnyMirror extends Polyline {
    public Group group = new Group();
    // The outline of the object for ray intersection
    public List<EditPoint> objectEditPoints = new ArrayList<>();
    public boolean isEdited;
    public boolean hasBeenClicked;
    // The percentage of light that is reflected, 0 - no light is reflected, 1 - perfect reflection
    public double reflectivity = 1;

    public FunnyMirror() {

    }

    public void draw() {
        setStrokeWidth(globalStrokeWidth);
        setStroke(mirrorColor);
        group.getChildren().add(this);
        root.getChildren().add(group);
        Task<Void> task = new Task<Void>() {
            @Override
            public Void call() throws Exception {
                List<Double> points = getPoints();
                int index = 0;
                while (isMousePressed) {
                    if (isCancelled()) {
                        break;
                    }
                    if (index < 2) {
                        points.add(mousePos.getX());
                        points.add(mousePos.getY());
                        index = index + 2;
                        continue;
                    }
                    if ((Math.abs(points.get(index - 2) - mousePos.getX()) > 10) || (Math.abs(points.get(index - 1) - mousePos.getY()) > 10)) {
                        points.add(mousePos.getX());
                        points.add(mousePos.getY());
                        index = index + 2;
                    }
                }
                return null;
            }
        };
        new Thread(task).start();
        this.setTranslateX(0);
        this.setTranslateY(0);


    }

    public void setReflectivity(double reflectivity) {
        this.reflectivity = reflectivity;
    }


    public double getReflectivity() {
        return reflectivity;
    }

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
}
