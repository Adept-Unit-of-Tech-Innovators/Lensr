package com.example.lensr.objects;

import com.example.lensr.MutableValue;
import javafx.concurrent.Task;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.List;

import static com.example.lensr.LensrStart.*;
import static com.example.lensr.MirrorMethods.setupEditPoints;
import static com.example.lensr.MirrorMethods.setupObjectEdit;

public class FunnyMirror extends Polyline {
    public Group group = new Group();
    // The outline of the object for ray intersection
    public List<Rectangle> editPoints = new ArrayList<>();
    // The percentage of light that is reflected, 0 - no light is reflected, 1 - perfect reflection
    public double reflectivity = 1;
    public boolean isEdited;
    public MutableValue isEditPointClicked = new MutableValue(false);

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
        reflectivitySlider.show();
        setupObjectEdit();
        isEdited = true;

        // Place edit points
        Bounds mirrorBounds = getLayoutBounds();
        editPoints.add(new Rectangle(mirrorBounds.getMinX() - editPointSize / 2, mirrorBounds.getMinY() - editPointSize / 2, editPointSize, editPointSize));
        editPoints.add(new Rectangle(mirrorBounds.getMaxX() - editPointSize / 2, mirrorBounds.getMinY() - editPointSize / 2, editPointSize, editPointSize));
        editPoints.add(new Rectangle(mirrorBounds.getMaxX() - editPointSize / 2, mirrorBounds.getMaxY() - editPointSize / 2, editPointSize, editPointSize));
        editPoints.add(new Rectangle(mirrorBounds.getMinX() - editPointSize / 2, mirrorBounds.getMaxY() - editPointSize / 2, editPointSize, editPointSize));

        setupEditPoints(editPoints, isEditPointClicked);
//        for (Rectangle editPoint : editPoints) {
//            editPoint.setOnMousePressed(this::handleEditPointPressed);
//            editPoint.setOnMouseReleased(this::executeEditPointRelease);
//        }
        group.getChildren().addAll(editPoints);
        editedShape = group;
    }

    public void closeObjectEdit() {
        reflectivitySlider.hide();
        isEdited = false;
        if (editPoints != null && editedShape instanceof Group editedGroup) {
            editedGroup.getChildren().removeAll(editPoints);
            editPoints.clear();
        }
        editedShape = null;
    }
}
