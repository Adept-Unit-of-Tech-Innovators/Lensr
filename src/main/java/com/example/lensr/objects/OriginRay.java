package com.example.lensr.objects;

import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.shape.Line;
import javafx.scene.shape.Shape;

import java.util.ArrayList;
import java.util.List;

import static com.example.lensr.Intersections.*;
import static com.example.lensr.LensrStart.*;

public class OriginRay extends Ray {
    Object parentSource;
    List<Ray> rayReflections = new ArrayList<>();

    public OriginRay(double startX, double startY, double endX, double endY) {
        super(startX, startY, endX, endY);
        setStartX(startX);
        setStartY(startY);
        setEndX(endX);
        setEndY(endY);
    }

    public void simulate() {
        // If the ray is not ending on the edge of the canvas, make it end on the intersection with a border of the canvas
        // That's some clever chat-gpt code right there
        if (getEndX() != SIZE || getEndY() != SIZE) {
            // Extend the ray to or past the edge of the canvas while following the same angle
            double originalEndX = getEndX();
            double originalEndY = getEndY();
            setEndX(originalEndX + SIZE * Math.cos(Math.atan2(originalEndY - getStartY(), originalEndX - getStartX())));
            setEndY(originalEndY + SIZE * Math.sin(Math.atan2(originalEndY - getStartY(), originalEndX - getStartX())));

            Point2D intersectionPoint = getRayIntersectionPoint(this, new Line(0, 0, SIZE, 0));
            if (intersectionPoint == null) {
                intersectionPoint = getRayIntersectionPoint(this, new Line(0, 0, 0, SIZE));
                if (intersectionPoint == null) {
                    intersectionPoint = getRayIntersectionPoint(this, new Line(0, SIZE, SIZE, SIZE));
                    if (intersectionPoint == null) {
                        intersectionPoint = getRayIntersectionPoint(this, new Line(SIZE, 0, SIZE, SIZE));
                    }
                }
            }
            if (intersectionPoint != null) {
                setEndX(intersectionPoint.getX());
                setEndY(intersectionPoint.getY());
            }
        }
        new Thread(() -> {
            int recursiveDepth = 0;
            Ray currentRay = this;
            while (true) {
                double shortestIntersectionDistance = Double.MAX_VALUE;
                Object closestIntersectionMirror = null;
                Point2D closestIntersectionPoint = new Point2D(Double.MAX_VALUE, Double.MAX_VALUE);

                for (Object mirror : mirrors) {
                    Point2D intersectionPoint = null;

                    if (mirror instanceof Shape currentMirror) {
                        // If the minimal distance to object bounds is higher than current shortest distance, this will not be the first object the ray intersects
                        double minimalPossibleDistance = currentRay.getMinimalDistanceToBounds(currentMirror.getLayoutBounds());
                        if (minimalPossibleDistance > shortestIntersectionDistance) continue;

                        if (currentMirror instanceof EllipseMirror ellipseMirror) {
                            intersectionPoint = getRayIntersectionPoint(currentRay, ellipseMirror.outline);
                        }
                        else {
                            intersectionPoint = getRayIntersectionPoint(currentRay, currentMirror);
                        }
                    }

                    if (intersectionPoint == null) continue;

                    // Round the intersection point to 2 decimal places
                    intersectionPoint = new Point2D(
                            Math.round(intersectionPoint.getX() * 100.0) / 100.0,
                            Math.round(intersectionPoint.getY() * 100.0) / 100.0
                    );

                    // If the intersection point is the same as the previous intersection point, skip it
                    Point2D previousIntersectionPoint = new Point2D(currentRay.getStartX(), currentRay.getStartY());
                    if (previousIntersectionPoint.equals(intersectionPoint)) continue;

                    // If this is the closest intersection point so far, set it as the closest intersection point
                    double intersectionDistance = Math.sqrt(
                            Math.pow(intersectionPoint.getX() - currentRay.getStartX(), 2) +
                                    Math.pow(intersectionPoint.getY() - currentRay.getStartY(), 2)
                    );

                    if (intersectionDistance < shortestIntersectionDistance) {
                        closestIntersectionPoint = intersectionPoint;
                        shortestIntersectionDistance = intersectionDistance;
                        closestIntersectionMirror = mirror;
                    }

                }

                // I hate that you have to do this
                Ray finalCurrentRay = currentRay;

                Platform.runLater(() -> {
                    Ray ray = new Ray(finalCurrentRay.getStartX(), finalCurrentRay.getStartY(), finalCurrentRay.getEndX(), finalCurrentRay.getEndY());
                    ray.setStrokeWidth(globalStrokeWidth);
                    ray.setStroke(finalCurrentRay.getStroke());
                    ray.setBrightness(finalCurrentRay.getBrightness());
                    ray.toBack();
                    // Don't add the first ray to the group (it's already added)
                    if (!(finalCurrentRay instanceof OriginRay)) {
                        this.rayReflections.add(ray);
                        if (parentSource instanceof BeamSource beamSource) {
                            beamSource.group.getChildren().add(ray);
                        }
                    }
                });

                // If there's no intersection, return
                if (closestIntersectionMirror == null) break;

                Point2D finalClosestIntersectionPoint = closestIntersectionPoint;

                currentRay.setEndX(finalClosestIntersectionPoint.getX());
                currentRay.setEndY(finalClosestIntersectionPoint.getY());

                // Limit recursive depth
                if (recursiveDepth >= 500) break;

                // If the ray is so dim, its basically invisible
                if (currentRay.getBrightness() < 0.001) break;

                Ray nextRay = new Ray(0, 0, 0, 0);
                nextRay.setStrokeWidth(globalStrokeWidth);
                nextRay.setWavelength(currentRay.getWavelength());

                double reflectedX = 0;
                double reflectedY = 0;

                if (closestIntersectionMirror instanceof LineMirror mirror) {
                    // Calculate the angle of incidence
                    double reflectionAngle = getLineReflectionAngle(currentRay, mirror);

                    // Calculate the reflected ray's endpoint based on the reflection angle
                    reflectedX = closestIntersectionPoint.getX() + SIZE * 2 * Math.cos(reflectionAngle);
                    reflectedY = closestIntersectionPoint.getY() + SIZE * 2 * Math.sin(reflectionAngle);

                    // Set the start point of the reflected ray slightly off the intersection point to prevent intersection with the same object
                    nextRay.setStartX(closestIntersectionPoint.getX() + Math.cos(reflectionAngle));
                    nextRay.setStartY(closestIntersectionPoint.getY() + Math.sin(reflectionAngle));

                    nextRay.setBrightness(currentRay.getBrightness() * mirror.getReflectivity());
                }
                else if (closestIntersectionMirror instanceof EllipseMirror mirror) {
                    // Calculate the angle of incidence
                    double reflectionAngle = getEllipseReflectionAngle(currentRay, mirror);

                    // Calculate the end point of the reflected ray
                    reflectedX = closestIntersectionPoint.getX() - SIZE * Math.cos(reflectionAngle);
                    reflectedY = closestIntersectionPoint.getY() - SIZE * Math.sin(reflectionAngle);

                    // Set the start point of the reflected ray slightly off the intersection point to prevent intersection with the same object
                    nextRay.setStartX(closestIntersectionPoint.getX() - Math.cos(reflectionAngle));
                    nextRay.setStartY(closestIntersectionPoint.getY() - Math.sin(reflectionAngle));

                    nextRay.setBrightness(currentRay.getBrightness() * mirror.getReflectivity());
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
                        double reflectionAngle = getLineReflectionAngle(currentRay, intersectionSegment);

                        // Calculate the reflected ray's endpoint based on the reflection angle
                        reflectedX = closestIntersectionPoint.getX() + SIZE * Math.cos(reflectionAngle);
                        reflectedY = closestIntersectionPoint.getY() + SIZE * Math.sin(reflectionAngle);

                        // Set the start point of the reflected ray slightly off the intersection point to prevent intersection with the same object
                        nextRay.setStartX(closestIntersectionPoint.getX() + Math.cos(reflectionAngle));
                        nextRay.setStartY(closestIntersectionPoint.getY() + Math.sin(reflectionAngle));

                        nextRay.setBrightness(currentRay.getBrightness() * mirror.getReflectivity());
                    }
                }
                else if (closestIntersectionMirror instanceof GaussianRolloffFilter filter) {
                    // Calculate the angle of incidence
                    double reflectionAngle = Math.atan2(currentRay.getEndY() - currentRay.getStartY(), currentRay.getEndX() - currentRay.getStartX());

                    // Calculate the reflected ray's endpoint based on the reflection angle
                    reflectedX = closestIntersectionPoint.getX() + SIZE * Math.cos(reflectionAngle);
                    reflectedY = closestIntersectionPoint.getY() + SIZE * Math.sin(reflectionAngle);

                    // Set the start point of the reflected ray slightly off the intersection point to prevent intersection with the same object
                    nextRay.setStartX(closestIntersectionPoint.getX() + Math.cos(reflectionAngle));
                    nextRay.setStartY(closestIntersectionPoint.getY() + Math.sin(reflectionAngle));

                    // Set the brightness of the ray for the Gaussian filter profile (standard for bandpass filters)
                    if (filter.getFWHM() == 0 && filter.getPassband() == nextRay.getWavelength()) {
                        nextRay.setBrightness(currentRay.getBrightness() * filter.getPeakTransmission());
                    }
                    else {
                        double sigma = filter.getFWHM() / (2 * Math.sqrt(2 * Math.log(2)));
                        double exponent = -0.5 * Math.pow( (currentRay.getWavelength() - filter.getPassband()) / sigma, 2);
                        double finalBrightness = currentRay.getBrightness() * filter.getPeakTransmission() * Math.pow(Math.E, exponent);
                        if (finalBrightness < 0.001) return;
                        nextRay.setBrightness(finalBrightness);
                    }
                }
                else if (closestIntersectionMirror instanceof BrickwallFilter filter) {
                    double reflectionAngle = Math.atan2(currentRay.getEndY() - currentRay.getStartY(), currentRay.getEndX() - currentRay.getStartX());

                    // Calculate the reflected ray's endpoint based on the reflection angle
                    reflectedX = closestIntersectionPoint.getX() + SIZE * Math.cos(reflectionAngle);
                    reflectedY = closestIntersectionPoint.getY() + SIZE * Math.sin(reflectionAngle);

                    // Set the start point of the reflected ray slightly off the intersection point to prevent intersection with the same object
                    nextRay.setStartX(closestIntersectionPoint.getX() + Math.cos(reflectionAngle));
                    nextRay.setStartY(closestIntersectionPoint.getY() + Math.sin(reflectionAngle));

                    // Set the brightness of the ray for the brickwall filter profile (standard for bandpass filters)
                    if (filter.getStartPassband() <= nextRay.getWavelength() && nextRay.getWavelength() <= filter.getEndPassband()) {
                        nextRay.setBrightness(currentRay.getBrightness() * filter.getTransmission());
                    }
                    else {
                        nextRay.setBrightness(0);
                    }
                }
                else if (closestIntersectionMirror instanceof LightEater) {
                    return;
                }

                nextRay.setEndX(reflectedX);
                nextRay.setEndY(reflectedY);

                recursiveDepth++;
                currentRay = nextRay;
            }
        }).start();
    }


    public void setParentSource(Object parentSource) {
        this.parentSource = parentSource;
    }


    public Object getParentSource() {
        return parentSource;
    }
}