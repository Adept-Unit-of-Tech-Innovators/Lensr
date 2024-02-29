package com.lensr.objects.mirrors;

import com.lensr.ui.EditPoint;
import com.lensr.objects.Editable;
import com.lensr.saveloadkit.SaveState;
import com.lensr.UserControls;
import com.lensr.LensrStart;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.geometry.Point2D;
import javafx.scene.shape.Arc;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Shape;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ArcMirror extends Arc implements Editable, Serializable {
    private transient Group group = new Group();
    public transient List<EditPoint> objectEditPoints = new ArrayList<>();
    private boolean isEdited;
    private boolean hasBeenClicked;
    double reflectivity = 0.9;
    private transient Point2D curvePoint = new Point2D(getCenterX(), getCenterY());
    private transient Point2D startPoint = new Point2D(getCenterX() + 1, getCenterY());
    private transient Point2D endPoint = new Point2D(getCenterX(), getCenterY() - 1);

    public ArcMirror(double x, double y) {
        super(x, y, 0, 0, 0, 0);
    }

    public void create() {
        setFill(Color.TRANSPARENT);
        setStroke(LensrStart.mirrorColor);
        setStrokeWidth(LensrStart.globalStrokeWidth);

        // Place edit points

        // Start and end points
        objectEditPoints.add(new EditPoint(startPoint.getX(), startPoint.getY(), EditPoint.Style.Primary));
        objectEditPoints.add(new EditPoint(endPoint.getX(), endPoint.getY(), EditPoint.Style.Primary));

        // Define what happens when an edit point is clicked
        objectEditPoints.get(0).setOnClickEvent(event -> scale(objectEditPoints.get(1).getCenter(), objectEditPoints.get(1), objectEditPoints.get(0)));
        objectEditPoints.get(1).setOnClickEvent(event -> scale(objectEditPoints.get(0).getCenter(), objectEditPoints.get(0), objectEditPoints.get(1)));

        // Curve point
        objectEditPoints.add(new EditPoint(curvePoint.getX(), curvePoint.getY(), EditPoint.Style.Quaternary));
        objectEditPoints.get(2).setOnClickEvent(event -> setVertex());
        curvePoint = new Point2D(objectEditPoints.get(2).getCenterX(), objectEditPoints.get(2).getCenterY());

        // Center point
        objectEditPoints.add(new EditPoint(getCenterX(), getCenterY(), EditPoint.Style.Secondary));
        objectEditPoints.get(3).setOnClickEvent(event -> move());

        objectEditPoints.forEach(editPoint -> {
            editPoint.setVisible(false);
            editPoint.hasBeenClicked = false;
        });

        group.getChildren().add(this);
        group.getChildren().addAll(objectEditPoints);
        LensrStart.root.getChildren().add(group);
    }

    @Override
    public void delete() {
        LensrStart.reflectivitySlider.hide();
        LensrStart.editPoints.removeAll(objectEditPoints);
        LensrStart.editedShape = null;
        LensrStart.mirrors.remove(this);
        LensrStart.root.getChildren().remove(group);
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
        LensrStart.mirrors.add(newMirror);
        UserControls.closeCurrentEdit();
        newMirror.openObjectEdit();
    }

    @Override
    public void openObjectEdit() {
        LensrStart.reflectivitySlider.setCurrentSource(this);
        LensrStart.reflectivitySlider.show();

        // Defocus the text field
        LensrStart.root.requestFocus();

        // Show the edit points (they're created once to "remember" which vertices to edit)
        objectEditPoints.forEach(editPoint -> {
            editPoint.setVisible(true);
            editPoint.toFront();
        });

        hasBeenClicked = true;
        isEdited = true;

        LensrStart.editPoints.addAll(objectEditPoints);
        LensrStart.editedShape = group;
    }

    @Override
    public void closeObjectEdit() {
        LensrStart.reflectivitySlider.hide();

        isEdited = false;
        if (objectEditPoints != null && LensrStart.editedShape instanceof Group) {
            LensrStart.editPoints.removeAll(objectEditPoints);
            objectEditPoints.forEach(editPoint -> {
                editPoint.setVisible(false);
                editPoint.hasBeenClicked = false;
            });
        }
        LensrStart.editedShape = null;
        LensrStart.updateLightSources();
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
        this.startPoint = new Point2D(objectEditPoints.get(0).getCenterX(), objectEditPoints.get(0).getCenterY());
        this.endPoint = new Point2D(objectEditPoints.get(1).getCenterX(), objectEditPoints.get(1).getCenterY());
        this.curvePoint = new Point2D(objectEditPoints.get(2).getCenterX(), objectEditPoints.get(2).getCenterY());
        SaveState.autoSave();
    }

    private void move() {
        new Thread(() -> {
            Point2D prevMousePos = LensrStart.mousePos;

            while (LensrStart.isMousePressed && isEdited) {
                double deltaX = LensrStart.mousePos.getX() - prevMousePos.getX();
                double deltaY = LensrStart.mousePos.getY() - prevMousePos.getY();

                // Update the UI on the JavaFX application thread
                Platform.runLater(() -> {
                    setCenterX(getCenterX() + deltaX);
                    setCenterY(getCenterY() + deltaY);

                    objectEditPoints.forEach(editPoint -> {
                        editPoint.setCenterX(editPoint.getCenterX() + deltaX);
                        editPoint.setCenterY(editPoint.getCenterY() + deltaY);
                    });
                    this.startPoint = new Point2D(objectEditPoints.get(0).getCenterX(), objectEditPoints.get(0).getCenterY());
                    this.endPoint = new Point2D(objectEditPoints.get(1).getCenterX(), objectEditPoints.get(1).getCenterY());
                    this.curvePoint = new Point2D(objectEditPoints.get(2).getCenterX(), objectEditPoints.get(2).getCenterY());
                });

                prevMousePos = LensrStart.mousePos;
                synchronized (LensrStart.lock) {
                    try {
                        LensrStart.lock.wait(10); // Adjust the sleep time as needed
                    } catch (InterruptedException e) {
                        throw new RuntimeException("A thread was interrupted while waiting.");
                    }
                }
            }
            SaveState.autoSave();
        }).start();
    }

    private void setVertex() {
        new Thread(() -> {
            while (LensrStart.isMousePressed && isEdited) {
                Point2D start = new Point2D(objectEditPoints.get(0).getCenterX(), objectEditPoints.get(0).getCenterY());
                Point2D end = new Point2D(objectEditPoints.get(1).getCenterX(), objectEditPoints.get(1).getCenterY());
                curvePoint = new Point2D(LensrStart.mousePos.getX(), LensrStart.mousePos.getY());
                Circle circumcircle = getCircumcircle(start, end, curvePoint);

                // Calculate the angles of the start, end and curve point relative to the circumcircle
                double curvePointAngle = (360 - Math.toDegrees(Math.atan2(curvePoint.getY() - circumcircle.getCenterY(), curvePoint.getX() - circumcircle.getCenterX()))) % 360;
                double startAngle = (360 - Math.toDegrees(Math.atan2(start.getY() - circumcircle.getCenterY(), start.getX() - circumcircle.getCenterX()))) % 360;
                double endAngle = (360 - Math.toDegrees(Math.atan2(end.getY() - circumcircle.getCenterY(), end.getX() - circumcircle.getCenterX()))) % 360;

                double length = calculateLength(startAngle, endAngle, curvePointAngle);

                if (length < 0) {
                    startAngle = endAngle;
                    length = -length;
                }

                double finalStartAngle = startAngle;
                double finalLength = length;


                Platform.runLater(() -> {
                    setCenterX(circumcircle.getCenterX());
                    setCenterY(circumcircle.getCenterY());
                    setRadiusX(circumcircle.getRadius());
                    setRadiusY(circumcircle.getRadius());
                    setStartAngle(finalStartAngle % 360);
                    setLength(finalLength);

                    // Update editPoints location
                    objectEditPoints.get(0).setCenterX(start.getX());
                    objectEditPoints.get(0).setCenterY(start.getY());
                    objectEditPoints.get(1).setCenterX(end.getX());
                    objectEditPoints.get(1).setCenterY(end.getY());
                    objectEditPoints.get(2).setCenterX(curvePoint.getX());
                    objectEditPoints.get(2).setCenterY(curvePoint.getY());
                    objectEditPoints.get(3).setCenterX(circumcircle.getCenterX());
                    objectEditPoints.get(3).setCenterY(circumcircle.getCenterY());
                    this.startPoint = new Point2D(objectEditPoints.get(0).getCenterX(), objectEditPoints.get(0).getCenterY());
                    this.endPoint = new Point2D(objectEditPoints.get(1).getCenterX(), objectEditPoints.get(1).getCenterY());
                    this.curvePoint = new Point2D(objectEditPoints.get(2).getCenterX(), objectEditPoints.get(2).getCenterY());
                });

                synchronized (LensrStart.lock) {
                    try {
                        LensrStart.lock.wait(10); // Adjust the wait time as needed
                    } catch (InterruptedException e) {
                        throw new RuntimeException("A thread was interrupted while waiting.");
                    }
                }
            }
            SaveState.autoSave();
        }).start();
    }

    public void scale(Point2D anchorPoint, EditPoint startPoint, EditPoint endPoint) {
        new Thread(() -> {
            double editPointX, editPointY, oppositeX, oppositeY;

            while (LensrStart.isMousePressed && isEdited) {
                if (LensrStart.altPressed && LensrStart.shiftPressed) {
                    // Shift-mode calculations for actually half the mirror
                    double deltaX = LensrStart.mousePos.getX() - anchorPoint.getX();
                    double deltaY = LensrStart.mousePos.getY() - anchorPoint.getY();
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
                } else if (LensrStart.altPressed) {
                    // Calculate first because funny java threading
                    editPointX = anchorPoint.getX() - (LensrStart.mousePos.getX() - anchorPoint.getX());
                    editPointY = anchorPoint.getY() - (LensrStart.mousePos.getY() - anchorPoint.getY());
                    oppositeX = LensrStart.mousePos.getX();
                    oppositeY = LensrStart.mousePos.getY();
                } else if (LensrStart.shiftPressed) {
                    editPointX = anchorPoint.getX();
                    editPointY = anchorPoint.getY();
                    double deltaX = LensrStart.mousePos.getX() - editPointX;
                    double deltaY = LensrStart.mousePos.getY() - editPointY;
                    double distance = Math.sqrt(Math.pow(deltaX, 2) + Math.pow(deltaY, 2));
                    double angle = Math.atan2(deltaY, deltaX);
                    double shiftedAngle = Math.round(angle * 4 / Math.PI) * Math.PI / 4;
                    oppositeX = editPointX + distance * Math.cos(shiftedAngle);
                    oppositeY = editPointY + distance * Math.sin(shiftedAngle);
                } else {
                    editPointX = anchorPoint.getX();
                    editPointY = anchorPoint.getY();
                    oppositeX = LensrStart.mousePos.getX();
                    oppositeY = LensrStart.mousePos.getY();
                }

                Point2D start = new Point2D(editPointX, editPointY);
                Point2D end = new Point2D(oppositeX, oppositeY);
                curvePoint = objectEditPoints.get(2).getCenter();

                // Get the circumcircle of the triangle formed by the start, end and curve point
                Circle circumcircle = getCircumcircle(start, end, curvePoint);

                // Calculate the angles of the start, end and curve point relative to the circumcircle
                double curvePointAngle = 360 - Math.toDegrees(Math.atan2(curvePoint.getY() - circumcircle.getCenterY(), curvePoint.getX() - circumcircle.getCenterX()));
                double startAngle = 360 - Math.toDegrees(Math.atan2(start.getY() - circumcircle.getCenterY(), start.getX() - circumcircle.getCenterX()));
                double endAngle = 360 - Math.toDegrees(Math.atan2(end.getY() - circumcircle.getCenterY(), end.getX() - circumcircle.getCenterX()));

                double length = calculateLength(startAngle, endAngle, curvePointAngle);

                if (length < 0) {
                    startAngle = endAngle;
                    length = -length;
                }

                double finalStartAngle = startAngle;
                double finalLength = length;

                Platform.runLater(() -> {
                    setCenterX(circumcircle.getCenterX());
                    setCenterY(circumcircle.getCenterY());
                    setRadiusX(circumcircle.getRadius());
                    setRadiusY(circumcircle.getRadius());
                    setStartAngle(finalStartAngle % 360);
                    setLength(finalLength);


                    // Update editPoints location
                    startPoint.setCenterX(start.getX());
                    startPoint.setCenterY(start.getY());
                    endPoint.setCenterX(end.getX());
                    endPoint.setCenterY(end.getY());
                    objectEditPoints.get(2).setCenterX(curvePoint.getX());
                    objectEditPoints.get(2).setCenterY(curvePoint.getY());
                    objectEditPoints.get(3).setCenterX(circumcircle.getCenterX());
                    objectEditPoints.get(3).setCenterY(circumcircle.getCenterY());
                    this.startPoint = new Point2D(objectEditPoints.get(0).getCenterX(), objectEditPoints.get(0).getCenterY());
                    this.endPoint = new Point2D(objectEditPoints.get(1).getCenterX(), objectEditPoints.get(1).getCenterY());
                    this.curvePoint = new Point2D(objectEditPoints.get(2).getCenterX(), objectEditPoints.get(2).getCenterY());
                });

                synchronized (LensrStart.lock) {
                    try {
                        LensrStart.lock.wait(10); // Adjust the sleep time as needed
                    } catch (InterruptedException e) {
                        throw new RuntimeException("A thread was interrupted while waiting.");
                    }
                }
            }

            SaveState.autoSave();
        }).start();
    }

    @Serial
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeDouble(getCenterX());
        out.writeDouble(getCenterY());
        out.writeDouble(getRadiusX());
        out.writeDouble(getRadiusY());
        out.writeDouble(getStartAngle());
        out.writeDouble(getLength());
        out.writeDouble(curvePoint.getX());
        out.writeDouble(curvePoint.getY());
        out.writeDouble(startPoint.getX());
        out.writeDouble(startPoint.getY());
        out.writeDouble(endPoint.getX());
        out.writeDouble(endPoint.getY());
    }

    @Serial
    private void readObject(java.io.ObjectInputStream in) throws Exception {
        in.defaultReadObject();
        setCenterX(in.readDouble());
        setCenterY(in.readDouble());
        setRadiusX(in.readDouble());
        setRadiusY(in.readDouble());
        setStartAngle(in.readDouble());
        setLength(in.readDouble());
        curvePoint = new Point2D(in.readDouble(), in.readDouble());
        startPoint = new Point2D(in.readDouble(), in.readDouble());
        endPoint = new Point2D(in.readDouble(), in.readDouble());

        // Initialize transient fields
        group = new Group();
        objectEditPoints = new ArrayList<>();
        isEdited = false;
        hasBeenClicked = false;
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
        double slope1 = ((start.getY() - end.getY()) / (start.getX() - end.getX()));
        double slope2 = ((end.getY() - curvePoint.getY()) / (end.getX() - curvePoint.getX()));

        // Cheaty? Sure. But it works.
        if (slope1 == 0) slope1 = 0.0000000001;
        if (slope2 == 0) slope2 = 0.0000000001;

        double perpSlope1 = -1 / slope1;
        double perpSlope2 = -1 / slope2;

        double circumcenterX = (midpoint2.getY() - midpoint1.getY() + perpSlope1 * midpoint1.getX() - perpSlope2 * midpoint2.getX()) / (perpSlope1 - perpSlope2);
        double circumcenterY = midpoint1.getY() + perpSlope1 * (circumcenterX - midpoint1.getX());

        return new Circle(circumcenterX, circumcenterY, radius);
    }

    private double calculateLength(double startAngle, double endAngle, double curveAngle) {
        double biggerAngle = Math.max(startAngle, endAngle);
        double smallerAngle = Math.min(startAngle, endAngle);

        double firstDistance = biggerAngle - smallerAngle;
        double secondDistance = 360 - firstDistance;

        boolean laysBetween = curveAngle >= smallerAngle && curveAngle <= biggerAngle;

        if (laysBetween) {
            if (startAngle < endAngle) {
                return firstDistance;
            } else {
                return -firstDistance;
            }
        } else {
            if (startAngle < endAngle) {
                return -secondDistance;
            } else {
                return secondDistance;
            }
        }
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
        return Shape.intersect(this, LensrStart.mouseHitbox).getLayoutBounds().getWidth() != -1;
    }
}