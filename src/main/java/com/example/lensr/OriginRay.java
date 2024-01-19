package com.example.lensr;

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
                    reflectedX = closestIntersectionPoint.getX() + SIZE * Math.cos(reflectionAngle);
                    reflectedY = closestIntersectionPoint.getY() + SIZE * Math.sin(reflectionAngle);

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
                else if (closestIntersectionMirror instanceof Filter filter) {
                    if (currentRay.getWavelength() >= ((Filter) closestIntersectionMirror).getPassband()) {
                        // Calculate the angle of incidence
                        double reflectionAngle = Math.atan2(currentRay.getEndY() - currentRay.getStartY(), currentRay.getEndX() - currentRay.getStartX());

                        // Calculate the reflected ray's endpoint based on the reflection angle
                        reflectedX = closestIntersectionPoint.getX() + SIZE * Math.cos(reflectionAngle);
                        reflectedY = closestIntersectionPoint.getY() + SIZE * Math.sin(reflectionAngle);

                        // Set the start point of the reflected ray slightly off the intersection point to prevent intersection with the same object
                        nextRay.setStartX(closestIntersectionPoint.getX() + Math.cos(reflectionAngle));
                        nextRay.setStartY(closestIntersectionPoint.getY() + Math.sin(reflectionAngle));

                            // Set the brightness of the ray assuming Gaussian filter profile (standard for bandpass filters)
                            double sigma = filter.getFWHM() / (2 * Math.sqrt(2 * Math.log(2)));
                            // ⠀⠀⠀⠀⠀⠀⠀⠀⠀⢀⣠⢴⣮⣽⣿⣿⣿⣿⣿⣯⣭⣭⣿⣢⢄⡀⠀⠀⠀⠀
                            //⠀⠀⠀⠀⠀⠀⠀⠀⣴⣿⣾⣿⣿⣿⣿⣾⣿⣿⣿⣿⣿⣿⣿⣿⣷⣿⢆⠀⠀⠀
                            //⠀⠀⠀⠀⠀⠀⢀⣾⣿⣿⣿⣿⣿⣿⡿⠛⠋⠙⣉⠛⣛⣿⣿⣿⠟⠛⢧⢷⠀⠀
                            //⠀⠀⠀⠀⠀⠀⡼⣿⣿⣿⣿⣿⣿⠯⠄⠀⠀⠀⠀⣦⣤⣽⣿⣟⣗⣄⠈⢣⡗⠀
                            //⠀⠀⠀⠀⠀⢠⢿⣿⣿⣿⣿⣿⣿⡴⠚⠉⠀⢀⣤⣬⣬⣿⣿⣿⠹⣿⡇⠀⣿⠀
                            //⠀⠀⠀⠀⠀⢸⢸⣿⣿⣿⣿⣿⠋⠀⠀⢠⠴⠟⣛⣿⣿⣿⣿⣿⣶⣾⣰⡀⢹⡢
                            //⠀⠀⠀⠀⠀⣸⢾⠟⠻⣿⣿⠇⠀⠀⠀⠐⢿⣿⣿⣿⣿⣿⣿⡟⢻⢻⣿⣿⣶⡇
                            //⠀⠀⠀⠀⢀⣾⣏⣐⡄⠀⣯⡀⠀⠀⠀⠀⠀⠙⢿⣿⣿⣿⣿⠄⠘⣿⣿⣿⣷⡅
                            //⠀⠀⠀⠀⢸⣤⣿⣿⠀⠀⣿⣷⡀⠀⠀⠀⣠⣶⣿⣿⣿⣿⠇⣄⣀⠸⡾⣷⡄⡇
                            //⠀⠀⠀⠀⠈⠣⣃⡈⢉⣸⣿⡻⣿⣮⣴⣾⡏⢀⣽⣿⣿⣿⣶⣶⣶⣴⣇⣿⠀⣱
                            //⠀⠀⠀⠀⠀⠀⡏⡏⠁⣿⢿⣆⣿⣿⣿⣿⣧⣿⣿⣿⣛⣿⣿⣿⣿⡦⣾⡟⢠⣃
                            //⠀⠀⠀⠀⠀⠀⣧⡇⢠⡏⢂⢹⣿⣿⣿⣿⣿⣿⣿⣿⡷⣬⣭⣙⡛⢳⣼⣿⣿⣎
                            //⠀⠀⠀⠀⠀⢠⢿⠀⠘⣿⣧⣵⣿⣿⣿⣿⣿⣿⣿⣿⣿⣶⣾⣿⣿⣥⣿⣿⢿⡿
                            //⠀⠀⠀⠀⠀⢸⡟⠀⠀⠙⠿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⡿⣸⣿⢻⡿⠀
                            //⠀⠀⠀⠀⠀⣯⡇⠀⠀⠀⠀⠈⠙⠻⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣷⢸⠁⠀
                            //⠀⠀⠀⢀⣴⠟⠀⠀⠀⠀⠀⠀⠀⠀⠈⠙⢿⣿⣿⣿⣿⣿⣿⣿⣿⣿⠀⣜⡆⠀
                            //⣒⠶⡛⠛⠁⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⢹⣿⣿⣿⣿⣿⣿⣿⡿⣠⡟⠀⠀
                            double exponent = -Math.pow(currentRay.getWavelength() - filter.getPassband(), 2) / (2 * Math.pow(sigma, 2));
                            nextRay.setBrightness(currentRay.getBrightness() * filter.getPeakTransmission() * Math.exp(exponent));
                    }
                    else {
                        return;
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