package com.example.lensr;

import javafx.application.Application;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

import static com.example.lensr.Intersections.*;

public class LensrStart extends Application {
    public static final Object lock = new Object();
    public static final Color mirrorColor = Color.WHITE;
    public static final double globalStrokeWidth = 1;
    public static final int SIZE = 1000;
    public static Pane root = new Pane();
    public static Scene scene = new Scene(root, SIZE, SIZE);
    public static List<Ray> rays = new ArrayList<>();
    public static List<Ray> rayReflections = new ArrayList<>();
    public static List<Object> mirrors = new ArrayList<>();
    public static List<Object> lenses = new ArrayList<>();
    public static double mouseX;
    public static double mouseY;
    public static boolean xPressed = false;
    public static boolean zPressed = false;
    public static boolean shiftPressed = false;
    public static boolean altPressed = false;
    public static boolean isEditMode = false;
    public static boolean isMousePressed = false;
    public static Object editedShape;

    @Override
    public void start(Stage primaryStage) {
        // Create a ray
        Ray ray = new Ray(0, 0, 0, 0);
        ray.create();
        rays.add(ray);

        // Set background color
        root.setStyle("-fx-background-color: rgb(50, 50, 50);");

        WavelengthSlider wavelengthSlider = new WavelengthSlider(rays, rayReflections);

        UserControls.setUserControls();

        primaryStage.setTitle("rtx 5090ti testing place");
        primaryStage.setScene(scene);

        SphericalLens testLens = new SphericalLens(100, 300, 500, 500, 10);
        testLens.addToRoot();
        lenses.add(testLens);

        primaryStage.show();
    }


    public static void drawRaysRecursively(Ray currentRay, Object previousMirror, int recursiveDepth) {

        // Get first mirror the object will intersect with
        double shortestIntersectionDistance = Double.MAX_VALUE;
        Object closestIntersectionObject = null;
        Point2D closestIntersectionPoint = new Point2D(Double.MAX_VALUE, Double.MAX_VALUE);

        for (Object mirror : mirrors) {
            // The ray cannot reflect of the same mirror twice in a row
            // TODO: It actually can, pls fix
            if (mirror == previousMirror) continue;

            if (mirror instanceof LineMirror currentMirror) {
                // If the minimal distance to object bounds is higher than current shortest distance, this will not be the first object the ray intersects
                double minimalPossibleDistance = currentRay.getMinimalDistanceToBounds(currentMirror.getLayoutBounds());
                if (minimalPossibleDistance > shortestIntersectionDistance) {
                    continue;
                }

                Point2D intersectionPoint = getRayIntersectionPoint(currentRay, currentMirror);

                if (intersectionPoint != null) {
                    double intersectionDistance = Math.sqrt(
                            Math.pow(intersectionPoint.getX() - currentRay.getStartX(), 2) +
                                    Math.pow(intersectionPoint.getY() - currentRay.getStartY(), 2)
                    );

                    if (intersectionDistance < shortestIntersectionDistance) {
                        closestIntersectionPoint = intersectionPoint;
                        shortestIntersectionDistance = intersectionDistance;
                        closestIntersectionObject = currentMirror;
                    }
                }
            }
            else if (mirror instanceof EllipseMirror currentMirror) {
                // If the minimal distance to object bounds is higher than current shortest distance, this will not be the first object the ray intersects
                double minimalPossibleDistance = currentRay.getMinimalDistanceToBounds(currentMirror.getLayoutBounds());
                if (minimalPossibleDistance > shortestIntersectionDistance) continue;

                Point2D intersectionPoint = getRayIntersectionPoint(currentRay, currentMirror);

                if (intersectionPoint != null) {
                    double intersectionDistance = Math.sqrt(
                            Math.pow(intersectionPoint.getX() - currentRay.getStartX(), 2) +
                                    Math.pow(intersectionPoint.getY() - currentRay.getStartY(), 2)
                    );

                    if (intersectionDistance < shortestIntersectionDistance) {
                        closestIntersectionPoint = intersectionPoint;
                        shortestIntersectionDistance = intersectionDistance;
                        closestIntersectionObject = currentMirror;
                    }
                }
            }
        }

        for(Object lens : lenses)
        {
            if(lens instanceof SphericalLens currentSphericalLens)
            {
                //Calculate minimal distances to all parts of the lens separately
                double minimalPossibleDistanceToTop = currentRay.getMinimalDistanceToBounds(currentSphericalLens.getTopLine().getLayoutBounds());
                double minimalPossibleDistanceToBottom = currentRay.getMinimalDistanceToBounds(currentSphericalLens.getBottomLine().getLayoutBounds());
                double minimalPossibleDistanceToFirstArc = currentRay.getMinimalDistanceToBounds(currentSphericalLens.getFirstArc().getLayoutBounds());
                double minimalPossibleDistanceToSecondArc = currentRay.getMinimalDistanceToBounds(currentSphericalLens.getSecondArc().getLayoutBounds());

                //determining, which minimal distance is the smallest - which part is the closest
                double minimalDistance = Math.min(Math.min(Math.min(minimalPossibleDistanceToTop, minimalPossibleDistanceToBottom), minimalPossibleDistanceToFirstArc), minimalPossibleDistanceToSecondArc);
                if(minimalDistance > shortestIntersectionDistance) continue;

                if(minimalDistance == minimalPossibleDistanceToFirstArc && !currentRay.isInsideLens)
                {
                    Point2D intersectionPoint = getRayIntersectionPoint(currentRay, currentSphericalLens.getFirstArc());
                    if (intersectionPoint != null) {
                        double intersectionDistance = Math.sqrt(
                                Math.pow(intersectionPoint.getX() - currentRay.getStartX(), 2) +
                                        Math.pow(intersectionPoint.getY() - currentRay.getStartY(), 2)
                        );

                        if (intersectionDistance < shortestIntersectionDistance) {
                            closestIntersectionPoint = intersectionPoint;
                            shortestIntersectionDistance = intersectionDistance;
                            closestIntersectionObject = currentSphericalLens;
                        }
                    }
//                    System.out.println("first");
//                    System.out.println(closestIntersectionPoint.getX() + " " + closestIntersectionPoint.getY());

                }
                else if(minimalDistance == minimalPossibleDistanceToSecondArc && !currentRay.isInsideLens )
                {
                    Point2D intersectionPoint = getRayIntersectionPoint(currentRay, currentSphericalLens.getSecondArc());
                    if (intersectionPoint != null) {
                        double intersectionDistance = Math.sqrt(
                                Math.pow(intersectionPoint.getX() - currentRay.getStartX(), 2) +
                                        Math.pow(intersectionPoint.getY() - currentRay.getStartY(), 2)
                        );

                        if (intersectionDistance < shortestIntersectionDistance) {
                            closestIntersectionPoint = intersectionPoint;
                            shortestIntersectionDistance = intersectionDistance;
                            closestIntersectionObject = currentSphericalLens;
                        }
                    }
//                    System.out.println("second");
//                    System.out.println(closestIntersectionPoint.getX() + " " + closestIntersectionPoint.getY());

                }
            }
        }

        // If there's no intersection, return
        if (closestIntersectionObject == null) return;

        currentRay.setEndX(closestIntersectionPoint.getX());
        currentRay.setEndY(closestIntersectionPoint.getY());

        // Limit recursive depth
        if (recursiveDepth >= 500) return;

        // If the ray is so dim, its basically invisible
        if (currentRay.getBrightness() < 0.001) return;

        Ray nextRay = new Ray(0, 0, 0, 0);
        nextRay.create();
        nextRay.setStroke(currentRay.getStroke());
        nextRay.setStrokeWidth(globalStrokeWidth);

        nextRay.setStartX(closestIntersectionPoint.getX());
        nextRay.setStartY(closestIntersectionPoint.getY());


        double reflectedX = 0;
        double reflectedY = 0;

        if (closestIntersectionObject instanceof LineMirror mirror) {
            // Calculate the angle of incidence
            double reflectionAngle = getLineReflectionAngle(currentRay, mirror);

            // Calculate the reflected ray's endpoint based on the reflection angle
            reflectedX = closestIntersectionPoint.getX() + SIZE * Math.cos(reflectionAngle);
            reflectedY = closestIntersectionPoint.getY() + SIZE * Math.sin(reflectionAngle);

            nextRay.setBrightness(currentRay.getBrightness() * mirror.getReflectivity());
        }
        else if (closestIntersectionObject instanceof EllipseMirror mirror) {
            // Calculate the angle of incidence
            double reflectionAngle = getEllipseReflectionAngle(currentRay, mirror);

            // Calculate the end point of the reflected ray
            reflectedX = closestIntersectionPoint.getX() - SIZE * Math.cos(reflectionAngle);
            reflectedY = closestIntersectionPoint.getY() - SIZE * Math.sin(reflectionAngle);

            nextRay.setBrightness(currentRay.getBrightness() * mirror.getReflectivity());
        }
        else if (closestIntersectionObject instanceof SphericalLens sphericalLens && !currentRay.isInsideLens)
        {
            double refractionAngle = getSphericalRefractionAngle(currentRay, sphericalLens.getFirstArc(), sphericalLens.getRefractiveIndex());

            reflectedX = closestIntersectionPoint.getX() + SIZE * Math.cos(refractionAngle);
            reflectedY = closestIntersectionPoint.getY() + SIZE * Math.sin(refractionAngle);
            System.out.println("lens detected");

        }
        nextRay.isInsideLens = true;

        nextRay.setEndX(reflectedX);
        nextRay.setEndY(reflectedY);

        rayReflections.add(nextRay);
        System.out.println(rayReflections.size());
        drawRaysRecursively(nextRay, closestIntersectionObject, recursiveDepth + 1);
    }

    public static void main(String[] args) {
        launch();
    }
}