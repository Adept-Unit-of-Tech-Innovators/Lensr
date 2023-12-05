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
    public static double mouseX;
    public static double mouseY;
    public static boolean xPressed = false;
    public static boolean zPressed = false;
    public static boolean shiftPressed = false;
    public static boolean altPressed = false;
    public static boolean isEditMode = false;

    @Override
    public void start(Stage primaryStage) {

        // Create a ray
        Ray ray = new Ray(0, 0, 0, 0);
        rays.add(ray);

        UserControls.setUserControls();

        scene.setFill(Color.rgb(30, 30, 30));

        primaryStage.setTitle("rtx 5090ti testing place");
        primaryStage.setScene(scene);
        primaryStage.show();
    }


    public static void drawRaysRecursively(Ray currentRay, Object previousMirror, int recursiveDepth) {

        // Get first mirror the object will intersect with
        double shortestIntersectionDistance = Double.MAX_VALUE;
        Object closestIntersectionMirror = null;
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
                        closestIntersectionMirror = currentMirror;
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
                        closestIntersectionMirror = currentMirror;
                    }
                }
            }
        }

        // If there's no intersection, return
        if (closestIntersectionMirror == null) return;

        currentRay.setEndX(closestIntersectionPoint.getX());
        currentRay.setEndY(closestIntersectionPoint.getY());

        // Limit recursive depth
        if (recursiveDepth >= 500) return;

        // If the ray is so dim, its basically invisible
        if (currentRay.getBrightness() < 0.001) return;

        Ray nextRay = new Ray(0, 0, 0, 0);
        nextRay.setStroke(currentRay.getStroke());
        nextRay.setStrokeWidth(globalStrokeWidth);

        nextRay.setStartX(closestIntersectionPoint.getX());
        nextRay.setStartY(closestIntersectionPoint.getY());

        double reflectedX = 0;
        double reflectedY = 0;

        if (closestIntersectionMirror instanceof LineMirror mirror) {
            // Calculate the angle of incidence
            double reflectionAngle = getLineReflectionAngle(currentRay, mirror);

            // Calculate the reflected ray's endpoint based on the reflection angle
            reflectedX = closestIntersectionPoint.getX() + SIZE * Math.cos(reflectionAngle);
            reflectedY = closestIntersectionPoint.getY() + SIZE * Math.sin(reflectionAngle);

            nextRay.setBrightness(currentRay.getBrightness() * mirror.getReflectivity());
        }
        else if (closestIntersectionMirror instanceof EllipseMirror mirror) {
            // Calculate the angle of incidence
            double reflectionAngle = getEllipseReflectionAngle(currentRay, mirror);

            // Calculate the end point of the reflected ray
            reflectedX = closestIntersectionPoint.getX() - SIZE * Math.cos(reflectionAngle);
            reflectedY = closestIntersectionPoint.getY() - SIZE * Math.sin(reflectionAngle);

            nextRay.setBrightness(currentRay.getBrightness() * mirror.getReflectivity());
        }

        nextRay.setEndX(reflectedX);
        nextRay.setEndY(reflectedY);

        rayReflections.add(nextRay);
        drawRaysRecursively(nextRay, closestIntersectionMirror, recursiveDepth + 1);
    }

    public static void updateRay(Ray ray) {
        double endX = (mouseX - ray.getStartX()) * SIZE;
        double endY = (mouseY - ray.getStartY()) * SIZE;
        ray.setEndX(endX);
        ray.setEndY(endY);

        root.getChildren().removeAll(rayReflections);

        drawRaysRecursively(ray, null, 0);
    }


    public static void main(String[] args) {
        launch();
    }
}