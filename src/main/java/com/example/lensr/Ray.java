package com.example.lensr;

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

import static com.example.lensr.Intersections.*;
import static com.example.lensr.LensrStart.*;

public class Ray extends Line {
    double brightness = 1.0;
    int wavelength;

    public Ray(double startX, double startY, double endX, double endY) {
        setStartX(startX);
        setStartY(startY);
        setEndX(endX);
        setEndY(endY);
    }

    public void create() {
        setStroke(Color.RED);
        setStrokeWidth(globalStrokeWidth);

        root.getChildren().add(this);
    }

    public void update() {
        double endX = (mousePos.getX() - this.getStartX()) * SIZE;
        double endY = (mousePos.getY() - this.getStartY()) * SIZE;
        this.setEndX(endX);
        this.setEndY(endY);

        root.getChildren().removeAll(rayReflections);

        simulateRay(0);
    }


    public void simulateRay(int recursiveDepth) {
        // Get first mirror the object will intersect with
        double shortestIntersectionDistance = Double.MAX_VALUE;
        Object closestIntersectionMirror = null;
        Point2D closestIntersectionPoint = new Point2D(Double.MAX_VALUE, Double.MAX_VALUE);

        for (Object mirror : mirrors) {
            Point2D intersectionPoint = null;

            if (mirror instanceof LineMirror currentMirror) {
                // If the minimal distance to object bounds is higher than current shortest distance, this will not be the first object the ray intersects
                double minimalPossibleDistance = getMinimalDistanceToBounds(currentMirror.getLayoutBounds());
                if (minimalPossibleDistance > shortestIntersectionDistance) continue;

                intersectionPoint = getRayIntersectionPoint(this, currentMirror);
            }
            else if (mirror instanceof EllipseMirror currentMirror) {
                // If the minimal distance to object bounds is higher than current shortest distance, this will not be the first object the ray intersects
                double minimalPossibleDistance = getMinimalDistanceToBounds(currentMirror.getLayoutBounds());
                if (minimalPossibleDistance > shortestIntersectionDistance) continue;

                intersectionPoint = getRayIntersectionPoint(this, currentMirror.outline);
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

        nextRay.setEndX(reflectedX);
        nextRay.setEndY(reflectedY);

        rayReflections.add(nextRay);
        nextRay.simulateRay(recursiveDepth + 1);
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
