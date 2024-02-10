package com.example.lensr.objects;

import com.example.lensr.RayCanvas;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.shape.*;

import java.util.ArrayList;
import java.util.List;

import static com.example.lensr.Intersections.*;
import static com.example.lensr.LensrStart.*;

public class OriginRay extends Ray {
    public Group group = new Group();
    Object parentSource;
    List<Ray> rayReflections = new ArrayList<>();
    private final RayCanvas rayRenderer = new RayCanvas(SIZE, SIZE);

    public OriginRay(double startX, double startY, double endX, double endY) {
        super(startX, startY, endX, endY);
        setStartX(startX);
        setStartY(startY);
        setEndX(endX);
        setEndY(endY);

        group.getChildren().add(this);
        group.getChildren().add(rayRenderer);
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

            Point2D intersectionPoint = getRayLineIntersectionPoint(this, new Line(0, 0, SIZE, 0));
            if (intersectionPoint == null) {
                intersectionPoint = getRayLineIntersectionPoint(this, new Line(0, 0, 0, SIZE));
                if (intersectionPoint == null) {
                    intersectionPoint = getRayLineIntersectionPoint(this, new Line(0, SIZE, SIZE, SIZE));
                    if (intersectionPoint == null) {
                        intersectionPoint = getRayLineIntersectionPoint(this, new Line(SIZE, 0, SIZE, SIZE));
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

                    if (mirror instanceof Shape currentMirror && currentRay.getMinimalDistanceToBounds(currentMirror.getLayoutBounds()) < shortestIntersectionDistance) {
                        if (currentMirror instanceof Ellipse ellipse) {
                            intersectionPoint = getRayEllipseIntersectionPoint(currentRay, ellipse);
                        }
                        else if (currentMirror instanceof Circle circle) {
                            intersectionPoint = getRayEllipseIntersectionPoint(currentRay, new Ellipse(circle.getCenterX(), circle.getCenterY(), circle.getRadius(), circle.getRadius()));
                        }
                        else if (currentMirror instanceof Line line) {
                            intersectionPoint = getRayLineIntersectionPoint(currentRay, line);
                        }
                        else if (currentMirror instanceof FunnyMirror funnyMirror) {
                            for (int i = 0; i + 2 < funnyMirror.getPoints().size(); i = i + 2) {
                                Line segment = new Line(funnyMirror.getPoints().get(i), funnyMirror.getPoints().get(i+1), funnyMirror.getPoints().get(i+2), funnyMirror.getPoints().get(i+3));
                                Point2D segmentIntersectionPoint = getRayLineIntersectionPoint(currentRay, segment);
                                if (segmentIntersectionPoint != null) {
                                    if (intersectionPoint == null) {
                                        intersectionPoint = segmentIntersectionPoint;

                                        // This is a cursed way to do this but it works
                                        LineMirror lineMirror = new LineMirror(segment.getStartX(), segment.getStartY(), segment.getEndX(), segment.getEndY());
                                        lineMirror.setReflectivity(funnyMirror.getReflectivity());
                                        funnyMirror.setClosestIntersectionSegment(lineMirror);
                                    }
                                    else {
                                        double distanceToSegmentIntersectionPoint = Math.sqrt(
                                                Math.pow(segmentIntersectionPoint.getX() - currentRay.getStartX(), 2) +
                                                        Math.pow(segmentIntersectionPoint.getY() - currentRay.getStartY(), 2)
                                        );
                                        double intersectionDistance = Math.sqrt(
                                                Math.pow(intersectionPoint.getX() - currentRay.getStartX(), 2) +
                                                        Math.pow(intersectionPoint.getY() - currentRay.getStartY(), 2)
                                        );
                                        if (distanceToSegmentIntersectionPoint < intersectionDistance) {
                                            intersectionPoint = segmentIntersectionPoint;

                                            // This is a cursed way to do this but it works
                                            LineMirror lineMirror = new LineMirror(segment.getStartX(), segment.getStartY(), segment.getEndX(), segment.getEndY());
                                            lineMirror.setReflectivity(funnyMirror.getReflectivity());
                                            funnyMirror.setClosestIntersectionSegment(lineMirror);
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (intersectionPoint == null) continue;

                    // If this is the closest intersection point so far, set it as the closest intersection point
                    double intersectionDistance = Math.sqrt(
                            Math.pow(intersectionPoint.getX() - currentRay.getStartX(), 2) +
                                    Math.pow(intersectionPoint.getY() - currentRay.getStartY(), 2)
                    );

                    if (intersectionDistance < shortestIntersectionDistance) {
                        closestIntersectionPoint = intersectionPoint;
                        shortestIntersectionDistance = intersectionDistance;
                        closestIntersectionMirror = mirror;
                        if (mirror instanceof FunnyMirror funnyMirror) {
                            closestIntersectionMirror = funnyMirror.getClosestIntersectionSegment();
                        }
                    }

                }

                if (!(currentRay instanceof OriginRay)) {
                    rayReflections.add(currentRay);
                }

                // If there's no intersection, break
                if (closestIntersectionMirror == null) break;

                currentRay.setEndX(closestIntersectionPoint.getX());
                currentRay.setEndY(closestIntersectionPoint.getY());

                // Limit recursive depth
                if (recursiveDepth >= 5000) break;

                Ray nextRay = new Ray(0, 0, 0, 0);
                nextRay.setStrokeWidth(globalStrokeWidth);
                nextRay.setWavelength(currentRay.getWavelength());

                double reflectedX = 0;
                double reflectedY = 0;

                if (closestIntersectionMirror instanceof LineMirror mirror) {
                    // Calculate the angle of incidence
                    double reflectionAngle = getLineReflectionAngle(currentRay, mirror);
                    reflectionAngle += nextRay.getWavelength() / 1000;

                    // Calculate the reflected ray's endpoint based on the reflection angle
                    reflectedX = closestIntersectionPoint.getX() + SIZE * 2 * Math.cos(reflectionAngle);
                    reflectedY = closestIntersectionPoint.getY() + SIZE * 2 * Math.sin(reflectionAngle);

                    // Set the start point of the reflected ray slightly off the intersection point to prevent intersection with the same object
                    nextRay.setStartX(closestIntersectionPoint.getX() + 0.001 * Math.cos(reflectionAngle));
                    nextRay.setStartY(closestIntersectionPoint.getY() + 0.001 * Math.sin(reflectionAngle));

                    nextRay.setBrightness(currentRay.getBrightness() * mirror.getReflectivity());
                }
                else if (closestIntersectionMirror instanceof EllipseMirror mirror) {
                    // Calculate the angle of incidence
                    double reflectionAngle = getEllipseReflectionAngle(currentRay, mirror);

                    // If the ellipse radius is 0, the ellipse is a line
                    if (mirror.getRadiusX() == 0) {
                        reflectionAngle = getLineReflectionAngle(currentRay, new Line(mirror.getCenterX(), mirror.getCenterY() - mirror.getRadiusY(), mirror.getCenterX(), mirror.getCenterY() + mirror.getRadiusY())) + Math.PI;
                    }
                    else if (mirror.getRadiusY() == 0) {
                        reflectionAngle = getLineReflectionAngle(currentRay, new Line(mirror.getCenterX() - mirror.getRadiusX(), mirror.getCenterY(), mirror.getCenterX() + mirror.getRadiusX(), mirror.getCenterY())) + Math.PI;
                    }

                    // Calculate the end point of the reflected ray
                    reflectedX = closestIntersectionPoint.getX() - SIZE * Math.cos(reflectionAngle);
                    reflectedY = closestIntersectionPoint.getY() - SIZE * Math.sin(reflectionAngle);

                    // Set the start point of the reflected ray slightly off the intersection point to prevent intersection with the same object
                    nextRay.setStartX(closestIntersectionPoint.getX() - 0.001 *  Math.cos(reflectionAngle));
                    nextRay.setStartY(closestIntersectionPoint.getY() - 0.001 * Math.sin(reflectionAngle));

                    nextRay.setBrightness(currentRay.getBrightness() * mirror.getReflectivity());
                }
                else if (closestIntersectionMirror instanceof GaussianRolloffFilter filter) {
                    // Calculate the angle of incidence
                    double reflectionAngle = Math.atan2(currentRay.getEndY() - currentRay.getStartY(), currentRay.getEndX() - currentRay.getStartX());

                    // Calculate the reflected ray's endpoint based on the reflection angle
                    reflectedX = closestIntersectionPoint.getX() + SIZE * Math.cos(reflectionAngle);
                    reflectedY = closestIntersectionPoint.getY() + SIZE * Math.sin(reflectionAngle);

                    // Set the start point of the reflected ray slightly off the intersection point to prevent intersection with the same object
                    nextRay.setStartX(closestIntersectionPoint.getX() + 0.001 * Math.cos(reflectionAngle));
                    nextRay.setStartY(closestIntersectionPoint.getY() + 0.001 * Math.sin(reflectionAngle));

                    // Set the brightness of the ray for the Gaussian filter profile (standard for bandpass filters)
                    if (filter.getFWHM() == 0 && filter.getPassband() == nextRay.getWavelength()) {
                        nextRay.setBrightness(currentRay.getBrightness() * filter.getPeakTransmission());
                    }
                    else {
                        double sigma = filter.getFWHM() / (2 * Math.sqrt(2 * Math.log(2)));
                        double exponent = -0.5 * Math.pow( (currentRay.getWavelength() - filter.getPassband()) / sigma, 2);
                        double finalBrightness = currentRay.getBrightness() * filter.getPeakTransmission() * Math.pow(Math.E, exponent);
                        nextRay.setBrightness(finalBrightness);
                    }
                }
                else if (closestIntersectionMirror instanceof BrickwallFilter filter) {
                    double reflectionAngle = Math.atan2(currentRay.getEndY() - currentRay.getStartY(), currentRay.getEndX() - currentRay.getStartX());

                    // Calculate the reflected ray's endpoint based on the reflection angle
                    reflectedX = closestIntersectionPoint.getX() + SIZE * Math.cos(reflectionAngle);
                    reflectedY = closestIntersectionPoint.getY() + SIZE * Math.sin(reflectionAngle);

                    // Set the start point of the reflected ray slightly off the intersection point to prevent intersection with the same object
                    nextRay.setStartX(closestIntersectionPoint.getX() + 0.001 * Math.cos(reflectionAngle));
                    nextRay.setStartY(closestIntersectionPoint.getY() + 0.001 *  Math.sin(reflectionAngle));

                    // Set the brightness of the ray for the brickwall filter profile
                    if (filter.getStartPassband() <= nextRay.getWavelength() && nextRay.getWavelength() <= filter.getEndPassband()) {
                        nextRay.setBrightness(currentRay.getBrightness() * filter.getTransmission());
                    }
                    else {
                        break;
                    }
                }
                else if (closestIntersectionMirror instanceof LightSensor sensor) {
                    sensor.addRay(currentRay);

                    double reflectionAngle = Math.atan2(currentRay.getEndY() - currentRay.getStartY(), currentRay.getEndX() - currentRay.getStartX());

                    // Calculate the reflected ray's endpoint based on the reflection angle
                    reflectedX = closestIntersectionPoint.getX() + SIZE * Math.cos(reflectionAngle);
                    reflectedY = closestIntersectionPoint.getY() + SIZE * Math.sin(reflectionAngle);

                    // Set the start point of the reflected ray slightly off the intersection point to prevent intersection with the same object
                    nextRay.setStartX(closestIntersectionPoint.getX() + 0.001 * Math.cos(reflectionAngle));
                    nextRay.setStartY(closestIntersectionPoint.getY() + 0.001 * Math.sin(reflectionAngle));

                    nextRay.setBrightness(currentRay.getBrightness());
                }
                else if (closestIntersectionMirror instanceof LightEater) {
                    break;
                }

                nextRay.setEndX(reflectedX);
                nextRay.setEndY(reflectedY);

                recursiveDepth++;
                currentRay = nextRay;
            }
            rayRenderer.drawRays(rayReflections);
        }).start();
    }

    public RayCanvas getRenderer() {
        return rayRenderer;
    }

    public void setParentSource(Object parentSource) {
        this.parentSource = parentSource;
    }


    public Object getParentSource() {
        return parentSource;
    }
}