package com.example.lensr.objects;

import com.example.lensr.EditPoint;
import com.example.lensr.UserControls;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.geometry.Point2D;
import javafx.scene.shape.Arc;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Shape;

import java.util.ArrayList;
import java.util.List;

import static com.example.lensr.LensrStart.*;
import static com.example.lensr.MirrorMethods.*;

public class ArcMirror extends Arc implements Editable {
    public Group group = new Group();
    public List<EditPoint> objectEditPoints = new ArrayList<>();
    public boolean isEdited;
    public boolean hasBeenClicked;
    double reflectivity = 0.9;
    private Point2D curvePoint;
    public ArcMirror(double x, double y) {
        super(x, y, 0, 0, 0, 0);
    }

    public void create() {
        setFill(Color.TRANSPARENT);
        setStroke(mirrorColor);
        setStrokeWidth(globalStrokeWidth);

        // Place edit points (initially at the center)
        objectEditPoints.add(new EditPoint(getCenterX(), getCenterY()));
        objectEditPoints.add(new EditPoint(getCenterX(), getCenterY()));

        // Define what happens when an edit point is clicked
        objectEditPoints.get(0).setOnClickEvent(event -> scale(objectEditPoints.get(1).getCenter()));
        objectEditPoints.get(1).setOnClickEvent(event -> {
            System.out.println("end");
            scale(objectEditPoints.get(0).getCenter());
        });
        objectEditPoints.get(0).setFill(Color.HOTPINK);

        objectEditPoints.add(new EditPoint(getCenterX(), getCenterY()));
        objectEditPoints.get(2).setOnClickEvent(event -> setVertex());
        objectEditPoints.get(2).setFill(Color.GREEN);
        curvePoint = new Point2D(objectEditPoints.get(2).getCenterX(), objectEditPoints.get(2).getCenterY());

        objectEditPoints.add(new EditPoint(getCenterX(), getCenterY()));
        objectEditPoints.get(3).setOnClickEvent(event -> move());
        objectEditPoints.get(3).setFill(Color.BLUE);

        group.getChildren().add(this);
        group.getChildren().addAll(objectEditPoints);
        root.getChildren().add(group);
    }

    @Override
    public void delete() {
        mirrors.remove(this);
        root.getChildren().remove(group);
    }

    @Override
    public void copy() {
        ArcMirror newMirror = new ArcMirror(0, 0);
        newMirror.setCenterX(getCenterX());
        newMirror.setCenterY(getCenterY());
        newMirror.setRadiusX(getRadiusX());
        newMirror.setRadiusY(getRadiusY());
        newMirror.setStartAngle(getStartAngle());
        newMirror.setLength(getLength());
        newMirror.setReflectivity(reflectivity);
        newMirror.create();
        newMirror.moveBy(10, 10);
        mirrors.add(newMirror);
        UserControls.closeCurrentEdit();
        newMirror.openObjectEdit();
    }

    @Override
    public void openObjectEdit() {
        reflectivitySlider.setCurrentSource(this);
        reflectivitySlider.show();

        // Defocus the text field
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
        reflectivitySlider.hide();

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

    public void setReflectivity(double reflectivity) {
        this.reflectivity = reflectivity;
    }

    public double getReflectivity() {
        return reflectivity;
    }

    @Override
    public void moveBy(double x, double y) {
        setCenterX(getCenterX() + x);
        setCenterY(getCenterY() + y);

        objectEditPoints.forEach(editPoint -> {
            editPoint.setCenterX(editPoint.getCenterX() + x);
            editPoint.setCenterY(editPoint.getCenterY() + y);
        });
    }

    private void move() {
        new Thread(() -> {
            Point2D prevMousePos = mousePos;

            while (isMousePressed && isEdited) {
                double deltaX = mousePos.getX() - prevMousePos.getX();
                double deltaY = mousePos.getY() - prevMousePos.getY();

                // Update the UI on the JavaFX application thread
                Platform.runLater(() -> moveBy(deltaX, deltaY));

                prevMousePos = mousePos;
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

    private void setVertex() {
        new Thread(() -> {
            while (isMousePressed && isEdited) {
                Point2D start = new Point2D(objectEditPoints.get(0).getCenterX(), objectEditPoints.get(0).getCenterY());
                Point2D end = new Point2D(objectEditPoints.get(1).getCenterX(), objectEditPoints.get(1).getCenterY());
                curvePoint = new Point2D(mousePos.getX(), mousePos.getY());
                Circle circumcircle = getCircumcircle(start, end, curvePoint);

                double curvePointAngle = (360 - Math.toDegrees(Math.atan2(curvePoint.getY() - circumcircle.getCenterY(), curvePoint.getX() - circumcircle.getCenterX())));
                double startAngle = (360 - Math.toDegrees(Math.atan2(start.getY() - circumcircle.getCenterY(), start.getX() - circumcircle.getCenterX())));
                double endAngle = (360 - Math.toDegrees(Math.atan2(end.getY() - circumcircle.getCenterY(), end.getX() - circumcircle.getCenterX())));

                Platform.runLater(() -> {
                    setCenterX(circumcircle.getCenterX());
                    setCenterY(circumcircle.getCenterY());
                    setRadiusX(circumcircle.getRadius());
                    setRadiusY(circumcircle.getRadius());
                    setStartAngle(startAngle);
                    if ((curvePointAngle < startAngle && curvePointAngle > endAngle - startAngle) ||
                            (curvePointAngle > startAngle && curvePointAngle < endAngle - startAngle)
                    ) {
                        System.out.println("Left");
                        setLength(endAngle - startAngle);
                    }
                    else {
                        System.out.println("Right");
                        setLength(endAngle - startAngle);
                    }
                    //if (curvePointAngle >= Math.min(startAngle, endAngle) && curvePointAngle > Math.max(startAngle, endAngle)) {
                    System.out.println(curvePointAngle + " " + startAngle + " " + endAngle + " " + getLength());



                    // Update editPoints location
                    objectEditPoints.get(0).setCenterX(start.getX());
                    objectEditPoints.get(0).setCenterY(start.getY());
                    objectEditPoints.get(1).setCenterX(end.getX());
                    objectEditPoints.get(1).setCenterY(end.getY());
                    objectEditPoints.get(2).setCenterX(curvePoint.getX());
                    objectEditPoints.get(2).setCenterY(curvePoint.getY());
                    objectEditPoints.get(3).setCenterX(circumcircle.getCenterX());
                    objectEditPoints.get(3).setCenterY(circumcircle.getCenterY());
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

    public void scale(Point2D anchorPoint) {
        new Thread(() -> {
            double editPointX, editPointY, oppositeX, oppositeY;

            while (isMousePressed && isEdited) {
                if (altPressed && shiftPressed) {
                    // Shift-mode calculations for actually half the mirror
                    double deltaX = mousePos.getX() - anchorPoint.getX();
                    double deltaY = mousePos.getY() - anchorPoint.getY();
                    double distance = Math.sqrt(Math.pow(deltaX, 2) + Math.pow(deltaY, 2));
                    double angle = Math.atan2(deltaY, deltaX);
                    double shiftedAngle = Math.round(angle * 4 / Math.PI) * Math.PI / 4;
                    double snappedX = anchorPoint.getX() + distance * Math.cos(shiftedAngle);
                    double snappedY = anchorPoint.getY() + distance * Math.sin(shiftedAngle);

                    // Alt-mode calculations to determine the "other half of the mirror"
                    editPointX = anchorPoint.getX() - (snappedX - anchorPoint.getX());
                    editPointY = anchorPoint.getY() - (snappedY - anchorPoint.getY());
                    oppositeX = snappedX;
                    oppositeY = snappedY;
                }
                else if (altPressed) {
                    // Calculate first because funny java threading
                    editPointX = anchorPoint.getX() - (mousePos.getX() - anchorPoint.getX());
                    editPointY = anchorPoint.getY() - (mousePos.getY() - anchorPoint.getY());
                    oppositeX = mousePos.getX();
                    oppositeY = mousePos.getY();
                }
                else if (shiftPressed) {
                    editPointX = anchorPoint.getX();
                    editPointY = anchorPoint.getY();
                    double deltaX = mousePos.getX() - editPointX;
                    double deltaY = mousePos.getY() - editPointY;
                    double distance = Math.sqrt(Math.pow(deltaX, 2) + Math.pow(deltaY, 2));
                    double angle = Math.atan2(deltaY, deltaX);
                    double shiftedAngle = Math.round(angle * 4 / Math.PI) * Math.PI / 4;
                    oppositeX = editPointX + distance * Math.cos(shiftedAngle);
                    oppositeY = editPointY + distance * Math.sin(shiftedAngle);
                }
                else {
                    editPointX = anchorPoint.getX();
                    editPointY = anchorPoint.getY();
                    oppositeX = mousePos.getX();
                    oppositeY = mousePos.getY();
                }

                Point2D start = new Point2D(editPointX, editPointY);
                Point2D end = new Point2D(oppositeX, oppositeY);
                curvePoint = objectEditPoints.get(2).getCenter();

                Circle circumcircle = getCircumcircle(start, end, curvePoint);

                // Update the UI on the JavaFX application thread
                Platform.runLater(() -> {
                    setCenterX(circumcircle.getCenterX());
                    setCenterY(circumcircle.getCenterY());
                    setRadiusX(circumcircle.getRadius());
                    setRadiusY(circumcircle.getRadius());

                    // TODO: This doesn't work. You might need to swap the start and end points. Check if length can be negative. Good luck soldier.
                    setStartAngle(-Math.toDegrees(Math.atan2(start.getY() - circumcircle.getCenterY(), start.getX() - circumcircle.getCenterX())));
                    setLength(-Math.toDegrees(Math.atan2(end.getY() - circumcircle.getCenterY(), end.getX() - circumcircle.getCenterX())) - getStartAngle());

                    // Update editPoints location
                    objectEditPoints.get(0).setCenterX(start.getX());
                    objectEditPoints.get(0).setCenterY(start.getY());
                    objectEditPoints.get(1).setCenterX(end.getX());
                    objectEditPoints.get(1).setCenterY(end.getY());
                    objectEditPoints.get(2).setCenterX(curvePoint.getX());
                    objectEditPoints.get(2).setCenterY(curvePoint.getY());
                    objectEditPoints.get(3).setCenterX(circumcircle.getCenterX());
                    objectEditPoints.get(3).setCenterY(circumcircle.getCenterY());
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

    public Circle getCircumcircle(Point2D start, Point2D end, Point2D curvePoint) {
        // Find the arc starting at the start point, ending at the end point and passing through the curve point
        // To do this, we treat those points as a triangle and find the circumcenter and circumradius
        double a = Math.sqrt(Math.pow(start.getX() - curvePoint.getX(), 2) + Math.pow(start.getY() - curvePoint.getY(), 2));
        double b = Math.sqrt(Math.pow(end.getX() - curvePoint.getX(), 2) + Math.pow(end.getY() - curvePoint.getY(), 2));
        double c = Math.sqrt(Math.pow(start.getX() - end.getX(), 2) + Math.pow(start.getY() - end.getY(), 2));

        double p = (a + b + c) / 2;
        double r = Math.sqrt((p - a) * (p - b) * (p - c) / p);
        double radius = (a * b * c) / (4 * r * p);

        // Calculate the midpoints of two sides of the triangle
        Point2D midpoint1 = new Point2D((start.getX() + end.getX()) / 2, (start.getY() + end.getY()) / 2);
        Point2D midpoint2 = new Point2D((end.getX() + curvePoint.getX()) / 2, (end.getY() + curvePoint.getY()) / 2);

        // Calculate the slope of the two lines
        double slope1 =((start.getY() - end.getY()) / (start.getX() - end.getX()));
        double slope2 =((end.getY() - curvePoint.getY()) / (end.getX() - curvePoint.getX()));

        // Cheaty? Sure. But it works.
        if (slope1 == 0) slope1 = 0.0000000001;
        if (slope2 == 0) slope2 = 0.0000000001;

        double perpSlope1 = -1 / slope1;
        double perpSlope2 = -1 / slope2;

        double circumcenterX = (midpoint2.getY() - midpoint1.getY() + perpSlope1 * midpoint1.getX() - perpSlope2 * midpoint2.getX()) / (perpSlope1 - perpSlope2);
        double circumcenterY = midpoint1.getY() + perpSlope1 * (circumcenterX - midpoint1.getX());

        return new Circle(circumcenterX, circumcenterY, radius);
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