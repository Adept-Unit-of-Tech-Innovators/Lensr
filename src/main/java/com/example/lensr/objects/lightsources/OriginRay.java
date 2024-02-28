package com.example.lensr.objects.lightsources;

import com.example.lensr.objects.misc.BrickwallFilter;
import com.example.lensr.objects.misc.GaussianRolloffFilter;
import com.example.lensr.objects.misc.LightEater;
import com.example.lensr.objects.misc.LightSensor;
import com.example.lensr.objects.glass.*;
import com.example.lensr.objects.mirrors.ArcMirror;
import com.example.lensr.objects.mirrors.EllipseMirror;
import com.example.lensr.objects.mirrors.FunnyMirror;
import com.example.lensr.objects.mirrors.LineMirror;
import javafx.application.Platform;
import com.example.lensr.Tuple;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.shape.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static com.example.lensr.Intersections.*;
import static com.example.lensr.LensrStart.*;

public class OriginRay extends Ray {
    public Group group = new Group();
    Object parentSource;
    List<Ray> rayReflections = new ArrayList<>();
    List<Glass> intersectors = new ArrayList<>();

    public OriginRay(double startX, double startY, double endX, double endY) {
        super(startX, startY, endX, endY);
        setStartX(startX);
        setStartY(startY);
        setEndX(endX);
        setEndY(endY);

        group.getChildren().add(this);
    }

    public void simulate() {
        taskPool.execute(() -> {
            rayReflections.clear();

            Deque<Ray> stack = new ArrayDeque<>();
            stack.push(this);

            while (!stack.isEmpty()) {
                Ray currentRay = stack.pop();
                double shortestIntersectionDistance = Double.MAX_VALUE;
                Object closestIntersectionObject = null;
                Point2D closestIntersectionPoint = new Point2D(Double.MAX_VALUE, Double.MAX_VALUE);

                for (Object mirror : mirrors) {
                    Point2D intersectionPoint = null;
                    Shape currObject = null;

                    // Funny Mirror has to be handled differently because of a javaFX bug
                    if (mirror instanceof FunnyMirror funnyMirror && currentRay.getMinimalDistanceToBounds(funnyMirror.getFunnyMirrorBounds()) < shortestIntersectionDistance) {
                        double shortestSegmentIntersectionDistance = Double.MAX_VALUE;
                        Point2D closestSegmentIntersectionPoint = null;

                        // Iterate through the segments of the funny mirror to find the closest intersection point
                        for (int i = 0; i + 2 < funnyMirror.getPoints().size(); i = i + 2) {
                            LineMirror segment = new LineMirror(funnyMirror.getPoints().get(i), funnyMirror.getPoints().get(i + 1), funnyMirror.getPoints().get(i + 2), funnyMirror.getPoints().get(i + 3));
                            Point2D segmentIntersectionPoint = null;
                            if (currentRay.getMinimalDistanceToBounds(segment.getLayoutBounds()) < shortestSegmentIntersectionDistance && currentRay.getMinimalDistanceToBounds(segment.getLayoutBounds()) < shortestIntersectionDistance) {
                                segmentIntersectionPoint = getRayLineIntersectionPoint(currentRay, segment);
                            }

                            if (segmentIntersectionPoint == null) {
                                continue;
                            }

                            if (segmentIntersectionPoint.distance(currentRay.getStartX(), currentRay.getStartY()) < shortestSegmentIntersectionDistance) {
                                currObject = segment;
                                closestSegmentIntersectionPoint = segmentIntersectionPoint;
                                shortestSegmentIntersectionDistance = segmentIntersectionPoint.distance(currentRay.getStartX(), currentRay.getStartY());
                            }
                        }
                        intersectionPoint = closestSegmentIntersectionPoint;
                    }
                    else if (mirror instanceof Shape currentMirror && currentRay.getMinimalDistanceToBounds(currentMirror.getLayoutBounds()) < shortestIntersectionDistance) {
                        if (currentMirror instanceof Ellipse ellipse) {
                            intersectionPoint = getRayEllipseIntersectionPoint(currentRay, ellipse);
                        }
                        else if (currentMirror instanceof Circle circle) {
                            intersectionPoint = getRayEllipseIntersectionPoint(currentRay, new Ellipse(circle.getCenterX(), circle.getCenterY(), circle.getRadius(), circle.getRadius()));
                        }
                        else if (currentMirror instanceof Line line) {
                            intersectionPoint = getRayLineIntersectionPoint(currentRay, line);
                        }
                        else if (currentMirror instanceof Arc arcMirror) {
                            intersectionPoint = getRayArcIntersectionPoint(currentRay, arcMirror);
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
                        if (currObject != null) closestIntersectionObject = currObject;
                        else closestIntersectionObject = mirror;
                    }
                }


                // Iterate through lenses
                for (Object lens : lenses) {
                    Point2D intersectionPoint = null;
                    Shape currObject = null;
                    if (lens instanceof SphericalLens currentSphericalLens) {
                        for (Shape element : currentSphericalLens.elements) {
                            if (currentRay.getMinimalDistanceToBounds(element.getLayoutBounds()) >= 0) {
                                if (element instanceof LensArc arc && getRayArcIntersectionPoint(currentRay, arc) != null) {
                                    intersectionPoint = getRayArcIntersectionPoint(currentRay, arc);
                                    currObject = arc;
                                } else if (element instanceof LensLine line && getRayLineIntersectionPoint(currentRay, line) != null) {
                                    intersectionPoint = getRayLineIntersectionPoint(currentRay, line);
                                    currObject = line;
                                }
                            }

                            if (intersectionPoint == null) continue;

                            double intersectionDistance = Math.sqrt(
                                    Math.pow(intersectionPoint.getX() - currentRay.getStartX(), 2) +
                                            Math.pow(intersectionPoint.getY() - currentRay.getStartY(), 2)
                            );

                            if (intersectionDistance < shortestIntersectionDistance) {
                                closestIntersectionPoint = intersectionPoint;
                                shortestIntersectionDistance = intersectionDistance;
                                closestIntersectionObject = currObject;
                            }
                        }
                    }

                    else if (lens instanceof Prism prism) {
                        for (int i = 0; i < prism.getPoints().size(); i = i + 2) {
                            PrismSegment segment;
                            if (i + 2 < prism.getPoints().size()) {
                                segment = new PrismSegment(prism.getPoints().get(i), prism.getPoints().get(i + 1), prism.getPoints().get(i + 2), prism.getPoints().get(i + 3), prism);
                            }
                            else {
                                segment = new PrismSegment(prism.getPoints().get(i), prism.getPoints().get(i + 1), prism.getPoints().get(0), prism.getPoints().get(1), prism);
                            }
                            intersectionPoint = getRayLineIntersectionPoint(currentRay, segment);
                            currObject = segment;

                            if (intersectionPoint == null) continue;

                            double intersectionDistance = Math.sqrt(
                                    Math.pow(intersectionPoint.getX() - currentRay.getStartX(), 2) +
                                            Math.pow(intersectionPoint.getY() - currentRay.getStartY(), 2)
                            );

                            if (intersectionDistance < shortestIntersectionDistance) {
                                closestIntersectionPoint = intersectionPoint;
                                shortestIntersectionDistance = intersectionDistance;
                                closestIntersectionObject = currObject;
                            }
                        }
                    }
                }

                if (!(currentRay instanceof OriginRay)) {
                    rayReflections.add(currentRay);
                }

                // If there's no intersection, return
                if (closestIntersectionObject == null) continue;

                currentRay.setEndX(closestIntersectionPoint.getX());
                currentRay.setEndY(closestIntersectionPoint.getY());

                // batch draw rays (every 500 rays)
                if (!rayReflections.isEmpty() && rayReflections.size() % 500 == 0) {
                    final List<Ray> drawnRays = new ArrayList<>(rayReflections.subList(rayReflections.size() - 500, rayReflections.size()));
                    Platform.runLater(() -> rayCanvas.drawRays(drawnRays));
                }

                // Limit the number of rays reflection to 5000
                if (rayReflections.size() >= 5000) break;

                Ray nextRay = new Ray(0, 0, 0, 0);
                nextRay.setStrokeWidth(globalStrokeWidth);
                nextRay.setWavelength(currentRay.getWavelength());

                double reflectedX = 0;
                double reflectedY = 0;

                // LineMirror interaction
                if (closestIntersectionObject instanceof LineMirror mirror) {
                    // Calculate the angle of incidence
                    double reflectionAngle = getLineReflectionAngle(currentRay, mirror);

                    // Calculate the reflected ray's endpoint based on the reflection angle
                    reflectedX = closestIntersectionPoint.getX() + WIDTH * 1000 * Math.cos(reflectionAngle);
                    reflectedY = closestIntersectionPoint.getY() + WIDTH * 1000 * Math.sin(reflectionAngle);

                    // Set the start point of the reflected ray slightly off the intersection point to prevent intersection with the same object
                    nextRay.setStartX(closestIntersectionPoint.getX() + 0.001 * Math.cos(reflectionAngle));
                    nextRay.setStartY(closestIntersectionPoint.getY() + 0.001 * Math.sin(reflectionAngle));

                    nextRay.setBrightness(currentRay.getBrightness() * mirror.getReflectivity());
                }

                // ArcMirror interaction
                if (closestIntersectionObject instanceof ArcMirror mirror) {
                    // Calculate the angle of incidence
                    double reflectionAngle = getArcReflectionAngle(currentRay, mirror);

                    // Calculate the reflected ray's endpoint based on the reflection angle
                    reflectedX = closestIntersectionPoint.getX() - WIDTH * 1000 * Math.cos(reflectionAngle);
                    reflectedY = closestIntersectionPoint.getY() - WIDTH * 1000 * Math.sin(reflectionAngle);

                    // Set the start point of the reflected ray slightly off the intersection point to prevent intersection with the same object
                    nextRay.setStartX(closestIntersectionPoint.getX() - 0.001 * Math.cos(reflectionAngle));
                    nextRay.setStartY(closestIntersectionPoint.getY() - 0.001 * Math.sin(reflectionAngle));

                    nextRay.setBrightness(currentRay.getBrightness() * mirror.getReflectivity());
                }

                // EllipseMirror interaction
                else if (closestIntersectionObject instanceof EllipseMirror mirror) {
                    // Calculate the angle of incidence
                    double reflectionAngle = getEllipseReflectionAngle(currentRay, mirror);

                    // If the ellipse radius is 0, the ellipse is a line
                    if (mirror.getRadiusX() == 0) {
                        reflectionAngle = getLineReflectionAngle(currentRay, new Line(mirror.getCenterX(), mirror.getCenterY() - mirror.getRadiusY(), mirror.getCenterX(), mirror.getCenterY() + mirror.getRadiusY())) + Math.PI;
                    } else if (mirror.getRadiusY() == 0) {
                        reflectionAngle = getLineReflectionAngle(currentRay, new Line(mirror.getCenterX() - mirror.getRadiusX(), mirror.getCenterY(), mirror.getCenterX() + mirror.getRadiusX(), mirror.getCenterY())) + Math.PI;
                    }

                    // Calculate the end point of the reflected ray
                    reflectedX = closestIntersectionPoint.getX() - 1000 * WIDTH * Math.cos(reflectionAngle);
                    reflectedY = closestIntersectionPoint.getY() - 1000 * WIDTH * Math.sin(reflectionAngle);

                    // Set the start point of the reflected ray slightly off the intersection point to prevent intersection with the same object
                    nextRay.setStartX(closestIntersectionPoint.getX() - 0.001 * Math.cos(reflectionAngle));
                    nextRay.setStartY(closestIntersectionPoint.getY() - 0.001 * Math.sin(reflectionAngle));

                    nextRay.setBrightness(currentRay.getBrightness() * mirror.getReflectivity());
                }

                // GaussianRolloffFilter interaction
                else if (closestIntersectionObject instanceof GaussianRolloffFilter filter) {
                    // Calculate the angle of incidence
                    double reflectionAngle = Math.atan2(currentRay.getEndY() - currentRay.getStartY(), currentRay.getEndX() - currentRay.getStartX());

                    // Calculate the reflected ray's endpoint based on the reflection angle
                    reflectedX = closestIntersectionPoint.getX() + 1000 * WIDTH * Math.cos(reflectionAngle);
                    reflectedY = closestIntersectionPoint.getY() + 1000 * WIDTH * Math.sin(reflectionAngle);

                    // Set the start point of the reflected ray slightly off the intersection point to prevent intersection with the same object
                    nextRay.setStartX(closestIntersectionPoint.getX() + 0.001 * Math.cos(reflectionAngle));
                    nextRay.setStartY(closestIntersectionPoint.getY() + 0.001 * Math.sin(reflectionAngle));

                    // Set the brightness of the ray for the Gaussian filter profile (standard for bandpass filters)
                    if (filter.getFWHM() == 0 && filter.getPassband() == nextRay.getWavelength()) {
                        nextRay.setBrightness(currentRay.getBrightness() * filter.getPeakTransmission());
                    } else {
                        double sigma = filter.getFWHM() / (2 * Math.sqrt(2 * Math.log(2)));
                        double exponent = -0.5 * Math.pow((currentRay.getWavelength() - filter.getPassband()) / sigma, 2);
                        double finalBrightness = currentRay.getBrightness() * filter.getPeakTransmission() * Math.pow(Math.E, exponent);
                        nextRay.setBrightness(finalBrightness);
                    }
                }

                // BrickwallFilter interaction
                else if (closestIntersectionObject instanceof BrickwallFilter filter) {
                    double reflectionAngle = Math.atan2(currentRay.getEndY() - currentRay.getStartY(), currentRay.getEndX() - currentRay.getStartX());

                    // Calculate the reflected ray's endpoint based on the reflection angle
                    reflectedX = closestIntersectionPoint.getX() + 1000 * WIDTH * Math.cos(reflectionAngle);
                    reflectedY = closestIntersectionPoint.getY() + 1000 * WIDTH * Math.sin(reflectionAngle);

                    // Set the start point of the reflected ray slightly off the intersection point to prevent intersection with the same object
                    nextRay.setStartX(closestIntersectionPoint.getX() + 0.001 * Math.cos(reflectionAngle));
                    nextRay.setStartY(closestIntersectionPoint.getY() + 0.001 * Math.sin(reflectionAngle));

                    // Set the brightness of the ray for the brickwall filter profile
                    if (filter.getStartPassband() <= nextRay.getWavelength() && nextRay.getWavelength() <= filter.getEndPassband()) {
                        nextRay.setBrightness(currentRay.getBrightness() * filter.getPeakTransmission());
                    } else {
                        continue;
                    }
                }

                // LightSensor interaction
                else if (closestIntersectionObject instanceof LightSensor sensor) {
                    sensor.addRay(currentRay);

                    double reflectionAngle = Math.atan2(currentRay.getEndY() - currentRay.getStartY(), currentRay.getEndX() - currentRay.getStartX());

                    // Calculate the reflected ray's endpoint based on the reflection angle
                    reflectedX = closestIntersectionPoint.getX() + 1000 * WIDTH * Math.cos(reflectionAngle);
                    reflectedY = closestIntersectionPoint.getY() + 1000 * WIDTH * Math.sin(reflectionAngle);

                    // Set the start point of the reflected ray slightly off the intersection point to prevent intersection with the same object
                    nextRay.setStartX(closestIntersectionPoint.getX() + 0.001 * Math.cos(reflectionAngle));
                    nextRay.setStartY(closestIntersectionPoint.getY() + 0.001 * Math.sin(reflectionAngle));

                    nextRay.setBrightness(currentRay.getBrightness());
                }

                // LightEater interaction
                else if (closestIntersectionObject instanceof LightEater) {
                    continue;
                }

                // LensArc interaction
                else if (closestIntersectionObject instanceof LensArc arc) {
                    SphericalLens currSphericalLens = arc.getParentLens();

                    boolean inLens = intersectors.contains(currSphericalLens);
                    Tuple<Double, Double> currentCoefficients = getCurrentCoefficients();
                    Tuple<Double, Double> newCoefficients = getNewCoefficients(currSphericalLens, inLens);
                    double currentRefractiveIndex = currentCoefficients.a() + currentCoefficients.b() / Math.pow(currentRay.getWavelength() / 1000, 2);
                    double newRefractiveIndex = newCoefficients.a() + newCoefficients.b() / Math.pow(currentRay.getWavelength() / 1000, 2);

                    boolean totalInternalReflection = determineTIR(currentRay, arc, currentRefractiveIndex, newRefractiveIndex);
                    double refractionAngle = getArcRefractionAngle(currentRay, arc, currentRefractiveIndex, newRefractiveIndex);

                    if (inLens && !totalInternalReflection) intersectors.remove(currSphericalLens);
                    else if (!inLens && !totalInternalReflection) intersectors.add(currSphericalLens);

                    // Calculate the reflected ray's endpoint based on the reflection angle
                    reflectedX = closestIntersectionPoint.getX() - 1000 * WIDTH * Math.cos(refractionAngle);
                    reflectedY = closestIntersectionPoint.getY() - 1000 * WIDTH * Math.sin(refractionAngle);

                    // Set the start point of the reflected ray slightly off the intersection point to prevent intersection with the same object
                    nextRay.setStartX(closestIntersectionPoint.getX() - 0.001 * Math.cos(refractionAngle));
                    nextRay.setStartY(closestIntersectionPoint.getY() - 0.001 * Math.sin(refractionAngle));

                }

                // LensLine interaction
                else if (closestIntersectionObject instanceof LensLine line) {
                    SphericalLens currSphericalLens = line.getParentLens();

                    boolean inLens = intersectors.contains(currSphericalLens);
                    Tuple<Double, Double> currentCoefficients = getCurrentCoefficients();
                    Tuple<Double, Double> newCoefficients = getNewCoefficients(currSphericalLens, inLens);
                    double currentRefractiveIndex = currentCoefficients.a() + currentCoefficients.b() / Math.pow(currentRay.getWavelength() / 1000, 2);
                    double newRefractiveIndex = newCoefficients.a() + newCoefficients.b() / Math.pow(currentRay.getWavelength() / 1000, 2);

                    boolean totalInternalReflection = determineTIR(currentRay, line, currentRefractiveIndex, newRefractiveIndex);
                    double refractionAngle = getLineRefractionAngle(currentRay, line, currentRefractiveIndex, newRefractiveIndex);

                    if (inLens && !totalInternalReflection) intersectors.remove(currSphericalLens);
                    else if (!inLens && !totalInternalReflection) intersectors.add(currSphericalLens);


                    if (totalInternalReflection) {
                        // Calculate the reflected ray's endpoint based on the reflection angle
                        reflectedX = closestIntersectionPoint.getX() + 1000 * WIDTH * Math.cos(refractionAngle);
                        reflectedY = closestIntersectionPoint.getY() + 1000 * WIDTH * Math.sin(refractionAngle);

                        // Set the start point of the reflected ray slightly off the intersection point to prevent intersection with the same object
                        nextRay.setStartX(closestIntersectionPoint.getX() + 0.001 * Math.cos(refractionAngle));
                        nextRay.setStartY(closestIntersectionPoint.getY() + 0.001 * Math.sin(refractionAngle));
                    } else {
                        // Calculate the reflected ray's endpoint based on the reflection angle
                        reflectedX = closestIntersectionPoint.getX() - 1000 * WIDTH * Math.cos(refractionAngle);
                        reflectedY = closestIntersectionPoint.getY() - 1000 * WIDTH * Math.sin(refractionAngle);

                        // Set the start point of the reflected ray slightly off the intersection point to prevent intersection with the same object
                        nextRay.setStartX(closestIntersectionPoint.getX() - 0.001 * Math.cos(refractionAngle));
                        nextRay.setStartY(closestIntersectionPoint.getY() - 0.001 * Math.sin(refractionAngle));
                    }
                }

                // Prism segment interaction
                else if (closestIntersectionObject instanceof PrismSegment segment) {
                    Prism currPrism = segment.getParentPrism();

                    boolean inPrism = intersectors.contains(currPrism);
                    Tuple<Double, Double> currentCoefficients = getCurrentCoefficients();
                    Tuple<Double, Double> newCoefficients = getNewCoefficients(currPrism, inPrism);
                    double currentRefractiveIndex = currentCoefficients.a() + currentCoefficients.b() / Math.pow(currentRay.getWavelength() / 1000, 2);
                    double newRefractiveIndex = newCoefficients.a() + newCoefficients.b() / Math.pow(currentRay.getWavelength() / 1000, 2);

                    boolean totalInternalReflection = determineTIR(currentRay, segment, currentRefractiveIndex, newRefractiveIndex);
                    double refractionAngle = getLineRefractionAngle(currentRay, segment, currentRefractiveIndex, newRefractiveIndex);

                    if (inPrism && !totalInternalReflection) intersectors.remove(currPrism);
                    else if (!inPrism && !totalInternalReflection) intersectors.add(currPrism);

                    if (totalInternalReflection) {
                        // Calculate the reflected ray's endpoint based on the reflection angle
                        reflectedX = closestIntersectionPoint.getX() + 1000 * WIDTH * Math.cos(refractionAngle);
                        reflectedY = closestIntersectionPoint.getY() + 1000 * WIDTH * Math.sin(refractionAngle);

                        // Set the start point of the reflected ray slightly off the intersection point to prevent intersection with the same object
                        nextRay.setStartX(closestIntersectionPoint.getX() + 0.001 * Math.cos(refractionAngle));
                        nextRay.setStartY(closestIntersectionPoint.getY() + 0.001 * Math.sin(refractionAngle));
                    } else {
                        // Calculate the reflected ray's endpoint based on the reflection angle
                        reflectedX = closestIntersectionPoint.getX() - 1000 * WIDTH * Math.cos(refractionAngle);
                        reflectedY = closestIntersectionPoint.getY() - 1000 * WIDTH * Math.sin(refractionAngle);

                        // Set the start point of the reflected ray slightly off the intersection point to prevent intersection with the same object
                        nextRay.setStartX(closestIntersectionPoint.getX() - 0.001 * Math.cos(refractionAngle));
                        nextRay.setStartY(closestIntersectionPoint.getY() - 0.001 * Math.sin(refractionAngle));
                    }
                }

                nextRay.setEndX(reflectedX);
                nextRay.setEndY(reflectedY);
                stack.push(nextRay);
            }

            if (rayReflections.isEmpty() || rayReflections.size() % 500 == 0) return;

            Platform.runLater(() -> {
                int remainingRays = (int) (rayReflections.size() - (Math.floor(rayReflections.size() / 500.0) * 500));
                rayCanvas.drawRays(rayReflections.subList(rayReflections.size() - remainingRays, rayReflections.size()));
                rayReflections.clear();
            });
        });
    }

    public void setParentSource(Object parentSource) {
        this.parentSource = parentSource;
    }

    public Object getParentSource() {
        return parentSource;
    }

    // Get current and new coefficients for the lens and prism interactions
    private Tuple<Double, Double> getCurrentCoefficients() {
        if (intersectors.isEmpty()) return new Tuple<>(1.0, 0.0);
        return new Tuple<>(intersectors.get(intersectors.size() - 1).getCoefficientA(), intersectors.get(intersectors.size() - 1).getCoefficientB());
    }

    private Tuple<Double, Double> getNewCoefficients(Glass currentSphericalLens, boolean isInTheLens) {
        if (intersectors.size() == 1 && intersectors.get(0) == currentSphericalLens) return new Tuple<>(1.0, 0.0);
        if (!isInTheLens || intersectors.size() < 2) return new Tuple<>(currentSphericalLens.getCoefficientA(), currentSphericalLens.getCoefficientB());
        return new Tuple<>(intersectors.get(intersectors.size() - 2).getCoefficientA(), intersectors.get(intersectors.size() - 2).getCoefficientB());
    }

    private boolean determineTIR(Ray ray, LensArc arc, double currRefractiveIndex, double newRefractiveIndex) {
        double angleOfIncidence = Math.atan2(ray.getEndY() - ray.getStartY(), ray.getEndX() - ray.getStartX());
        double criticalAngle = currRefractiveIndex > newRefractiveIndex ? Math.asin(newRefractiveIndex/currRefractiveIndex) : Double.MAX_VALUE;
        double centerX = arc.getCenterX();
        double centerY = arc.getCenterY();
        double pointX = ray.getEndX();
        double pointY = ray.getEndY();

        double normalAngle = determineNormalAngle(Math.atan((pointY - centerY) / (pointX - centerX)), Math.atan((pointY - centerY) / (pointX - centerX)) + Math.PI, angleOfIncidence);
        double normalizedAngleOfIncidence = normalizeIntersectionAngle(angleOfIncidence, normalAngle);

        return Math.abs(normalizedAngleOfIncidence) > criticalAngle;
    }

    private boolean determineTIR(Ray ray, Line line, double currRefractiveIndex, double newRefractiveIndex) {
        double angleOfIncidence = Math.atan2(ray.getEndY() - ray.getStartY(), ray.getEndX() - ray.getStartX());
        double criticalAngle = currRefractiveIndex > newRefractiveIndex ? Math.asin(newRefractiveIndex/currRefractiveIndex) : Double.MAX_VALUE;
        double lineAngle = Math.atan2(line.getEndY() - line.getStartY(), line.getEndX() - line.getStartX());

        double normalAngle = determineNormalAngle(lineAngle - Math.PI/2, lineAngle + Math.PI/2, angleOfIncidence);
        double normalizedAngleOfIncidence = normalizeIntersectionAngle(angleOfIncidence, normalAngle);

        return Math.abs(normalizedAngleOfIncidence) > criticalAngle;
    }
}