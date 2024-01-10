package com.example.lensr;

import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.transform.Rotate;

import java.util.ArrayList;
import java.util.List;

import static com.example.lensr.Intersections.*;
import static com.example.lensr.LensrStart.*;
import static java.awt.geom.Point2D.distance;

public class Ray extends Line {
    Rectangle laserPointer;
    Rotate rotate = new Rotate();
    double rotation;
    List<Rectangle> editPoints = new ArrayList<>();
    double brightness = 1.0;
    int wavelength;
    boolean isEdited;
    Group group = new Group();
    MutableValue isEditPointClicked = new MutableValue(false);
    List<Ray> rayReflections = new ArrayList<>();

    public Ray(double startX, double startY, double endX, double endY) {
        setStartX(startX);
        setStartY(startY);
        setEndX(endX);
        setEndY(endY);
    }

    public void create() {
        setStroke(Color.RED);
        setStrokeWidth(globalStrokeWidth);

        createLaserPointer();

        group.getChildren().add(this);
        group.getChildren().add(laserPointer);
        root.getChildren().add(group);
    }

    public void update() {
        if (isEdited) return;

        updateLaserPointer();

        for (Ray ray : rayReflections) {
            root.getChildren().remove(ray.group);
        }
        rayReflections.clear();

        simulateRay(this, 0);
    }


    public void simulateRay(Ray parentRay, int recursiveDepth) {
        // Get first mirror the object will intersect with
        double shortestIntersectionDistance = Double.MAX_VALUE;
        Object closestIntersectionMirror = null;
        Point2D closestIntersectionPoint = new Point2D(Double.MAX_VALUE, Double.MAX_VALUE);

        for (Object mirror : mirrors) {
            Point2D intersectionPoint = null;

            if (mirror instanceof Shape currentMirror) {
                // If the minimal distance to object bounds is higher than current shortest distance, this will not be the first object the ray intersects
                double minimalPossibleDistance = getMinimalDistanceToBounds(currentMirror.getLayoutBounds());
                if (minimalPossibleDistance > shortestIntersectionDistance) continue;

                intersectionPoint = getRayIntersectionPoint(this, currentMirror);
            }
            if (intersectionPoint == null) continue;

            // Round the intersection point to 2 decimal places
            intersectionPoint = new Point2D(
                    Math.round(intersectionPoint.getX() * 100.0) / 100.0,
                    Math.round(intersectionPoint.getY() * 100.0) / 100.0
            );

            // If the intersection point is the same as the previous intersection point, skip it
            Point2D previousIntersectionPoint = new Point2D(getStartX(), getStartY());
            if (previousIntersectionPoint.equals(intersectionPoint)) continue;

            // If this is the closest intersection point so far, set it as the closest intersection point
            double intersectionDistance = Math.sqrt(
                    Math.pow(intersectionPoint.getX() - getStartX(), 2) +
                            Math.pow(intersectionPoint.getY() - getStartY(), 2)
            );

            if (intersectionDistance < shortestIntersectionDistance) {
                closestIntersectionPoint = intersectionPoint;
                shortestIntersectionDistance = intersectionDistance;
                closestIntersectionMirror = mirror;
            }

        }

        // If there's no intersection, return
        if (closestIntersectionMirror == null) return;

        setEndX(closestIntersectionPoint.getX());
        setEndY(closestIntersectionPoint.getY());

        // Limit recursive depth
        if (recursiveDepth >= 500) return;

        // If the ray is so dim, its basically invisible
        if (getBrightness() < 0.001) return;

        Ray nextRay = new Ray(0, 0, 0, 0);
        nextRay.create();
        nextRay.setStroke(getStroke());
        nextRay.setStrokeWidth(globalStrokeWidth);

        double reflectedX = 0;
        double reflectedY = 0;

        if (closestIntersectionMirror instanceof LineMirror mirror) {
            // Calculate the angle of incidence
            double reflectionAngle = getLineReflectionAngle(this, mirror);

            // Calculate the reflected ray's endpoint based on the reflection angle
            reflectedX = closestIntersectionPoint.getX() + SIZE * Math.cos(reflectionAngle);
            reflectedY = closestIntersectionPoint.getY() + SIZE * Math.sin(reflectionAngle);

            // Set the start point of the reflected ray slightly off the intersection point to prevent intersection with the same object
            nextRay.setStartX(closestIntersectionPoint.getX() + Math.cos(reflectionAngle));
            nextRay.setStartY(closestIntersectionPoint.getY() + Math.sin(reflectionAngle));

            nextRay.setBrightness(this.getBrightness() * mirror.getReflectivity());
        }
        else if (closestIntersectionMirror instanceof EllipseMirror mirror) {
            // Calculate the angle of incidence
            double reflectionAngle = getEllipseReflectionAngle(this, mirror);

            // Calculate the end point of the reflected ray
            reflectedX = closestIntersectionPoint.getX() - SIZE * Math.cos(reflectionAngle);
            reflectedY = closestIntersectionPoint.getY() - SIZE * Math.sin(reflectionAngle);

            // Set the start point of the reflected ray slightly off the intersection point to prevent intersection with the same object
            nextRay.setStartX(closestIntersectionPoint.getX() - Math.cos(reflectionAngle));
            nextRay.setStartY(closestIntersectionPoint.getY() - Math.sin(reflectionAngle));

            nextRay.setBrightness(getBrightness() * mirror.getReflectivity());
        }
        else if (closestIntersectionMirror instanceof FunnyMirror mirror) {
            Line intersectionSegment = null;
            for (int i = 0; i + 2 < mirror.getPoints().size(); i = i + 2) {
                // Find the mirror's segment that the ray intersects
                Line segment = new Line(mirror.getPoints().get(i), mirror.getPoints().get(i+1), mirror.getPoints().get(i+2), mirror.getPoints().get(i+3));
                if (segment.contains(closestIntersectionPoint)) {
                    intersectionSegment = segment;
                    break;
                }
            }
            if (intersectionSegment != null) {
                double reflectionAngle = getLineReflectionAngle(this, intersectionSegment);

                // Calculate the reflected ray's endpoint based on the reflection angle
                reflectedX = closestIntersectionPoint.getX() + SIZE * Math.cos(reflectionAngle);
                reflectedY = closestIntersectionPoint.getY() + SIZE * Math.sin(reflectionAngle);

                // Set the start point of the reflected ray slightly off the intersection point to prevent intersection with the same object
                nextRay.setStartX(closestIntersectionPoint.getX() + Math.cos(reflectionAngle));
                nextRay.setStartY(closestIntersectionPoint.getY() + Math.sin(reflectionAngle));

                nextRay.setBrightness(getBrightness() * mirror.getReflectivity());
            }
        }

        nextRay.setEndX(reflectedX);
        nextRay.setEndY(reflectedY);

        parentRay.rayReflections.add(nextRay);
        nextRay.simulateRay(parentRay, + 1);
    }


    public void setWavelength(int wavelength) {
        this.wavelength = wavelength;

        // TODO: change rays color based on the wavelength
    }


    public int getWavelength() {
        return wavelength;
    }


    // Brightness = Opacity
    public void setBrightness(double brightness) {
        this.brightness = brightness;

        Color strokeColor = (Color) this.getStroke();
        Color updatedColor = new Color(
                strokeColor.getRed(),
                strokeColor.getGreen(),
                strokeColor.getBlue(),
                brightness);

        this.setStroke(updatedColor);
    }


    public double getBrightness() {
        return brightness;
    }


    public void openObjectEdit() {
        MirrorMethods.setupObjectEdit();
        isEdited = true;

        // Place edit points
        editPoints.add(new Rectangle(getStartX() - editPointSize / 2, getStartY() - editPointSize / 2, editPointSize,editPointSize));
        editPoints.add(new Rectangle(
                getStartX() + Math.cos(Math.toRadians(rotation)) * 100 - editPointSize / 2,
                getStartY() + Math.sin(Math.toRadians(rotation)) * 100 - editPointSize / 2,
                editPointSize, editPointSize
        ));

        editPoints.get(0).setOnMousePressed(mouseEvent -> {
            isMousePressed = true;
            isEditPointClicked.setValue(true);
            moveToMouse();
        });
        editPoints.get(1).setOnMousePressed(mouseEvent -> {
            isMousePressed = true;
            isEditPointClicked.setValue(true);
            rotateToMouse();
        });
        editPoints.get(0).setOnMouseReleased(this::executeEditPointRelease);
        editPoints.get(1).setOnMouseReleased(this::executeEditPointRelease);

        editPoints.get(0).toFront();
        editPoints.get(1).toFront();

        MirrorMethods.setupEditPoints(editPoints, isEditPointClicked);
        group.getChildren().addAll(editPoints);
        editedShape = group;
    }


    private void moveToMouse() {
        new Thread(() -> {
            double rotationTemp = Math.atan2(getEndY() - getStartY(), getEndX() - getStartX());
            while (isMousePressed) {
                double deltaX = mousePos.getX() - getStartX();
                double deltaY = mousePos.getY() - getStartY();

                setStartX(mousePos.getX());
                setStartY(mousePos.getY());

                setEndX(getStartX() + Math.cos(rotationTemp) * SIZE);
                setEndY(getStartY() + Math.sin(rotationTemp) * SIZE);


                editPoints.get(0).setX(editPoints.get(0).getX() + deltaX);
                editPoints.get(0).setY(editPoints.get(0).getY() + deltaY);
                editPoints.get(1).setX(editPoints.get(1).getX() + deltaX);
                editPoints.get(1).setY(editPoints.get(1).getY() + deltaY);

                updateLaserPointer();

                Platform.runLater(() -> {
                    for (Ray ray : rayReflections) {
                        root.getChildren().remove(ray.group);
                    }
                });

                synchronized (lock) {
                    try {
                        lock.wait(10);
                    } catch (InterruptedException e) {
                        throw new RuntimeException("A thread was interrupted while waiting.");
                    }
                }
            }
        }).start();
    }


    private void rotateToMouse() {
        new Thread(() -> {
            while (isMousePressed) {
                double rot = (Math.atan2(mousePos.getY() - getStartY(), mousePos.getX() - getStartX()));

                editPoints.get(1).setX(getStartX() + Math.cos(rot) * 100 - editPointSize / 2);
                editPoints.get(1).setY(getStartY() + Math.sin(rot) * 100 - editPointSize / 2);

                setEndX(getStartX() + Math.cos(rot) * SIZE);
                setEndY(getStartY() + Math.sin(rot) * SIZE);

                updateLaserPointer();

                Platform.runLater(() -> {
                    for (Ray ray : rayReflections) {
                        root.getChildren().remove(ray.group);
                    }
                });

                synchronized (lock) {
                    try {
                        lock.wait(10);
                    } catch (InterruptedException e) {
                        throw new RuntimeException("A thread was interrupted while waiting.");
                    }
                }
            }
        }).start();
    }


    private void executeEditPointRelease(MouseEvent event) {
        MirrorMethods.handleEditPointReleased(event, isEditPointClicked, editPoints);
    }


    public void closeObjectEdit() {
        isEdited = false;
        if (editPoints != null && editedShape instanceof Group editedGroup) {
            editedGroup.getChildren().removeAll(editPoints);
            editPoints.clear();
        }
    }


    private void createLaserPointer() {
        laserPointer = new Rectangle();
        laserPointer.setWidth(100);
        laserPointer.setHeight(50);
        laserPointer.setFill(Color.GRAY);
        laserPointer.getTransforms().add(rotate);
        laserPointer.toFront();
        laserPointer.setOnMouseClicked(mouseEvent -> {
            if (isEditMode && !isEdited) openObjectEdit();
        });
        updateLaserPointer();
    }


    private void updateLaserPointer() {
        laserPointer.setX(getStartX() - laserPointer.getWidth() / 2);
        laserPointer.setY(getStartY() - laserPointer.getHeight() / 2);
        rotate.setPivotX(getStartX());
        rotate.setPivotY(getStartY());
        rotation = Math.toDegrees(Math.atan2(getEndY() - getStartY(), getEndX() - getStartX()));
        rotate.setAngle(rotation);
        toBack();
    }


    public double getMinimalDistanceToBounds(Bounds bounds) {
        // Get the start position of the ray
        double startX = getStartX();
        double startY = getStartY();

        // Calculate the minimal distance from the start position of the ray to the given bounds
        double distanceX;
        double distanceY;

        if (startX < bounds.getMinX()) {
            distanceX = bounds.getMinX() - startX;
        } else if (startX > bounds.getMaxX()) {
            distanceX = startX - bounds.getMaxX();
        } else {
            distanceX = 0; // Start X is within the bounds
        }

        if (startY < bounds.getMinY()) {
            distanceY = bounds.getMinY() - startY;
        } else if (startY > bounds.getMaxY()) {
            distanceY = startY - bounds.getMaxY();
        } else {
            distanceY = 0; // Start Y is within the bounds
        }

        // Calculate the Euclidean distance from the start position to the bounds
        return Math.sqrt(distanceX * distanceX + distanceY * distanceY);
    }
}