package com.example.lensr;

import javafx.application.Application;
import javafx.geometry.Point2D;
import javafx.scene.Group;
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
    public static final double globalStrokeWidth = 0.5;
    public static final double editPointSize = 8;
    public static final int SIZE = 1000;
    public static Pane root = new Pane();
    public static Scene scene = new Scene(root, SIZE, SIZE);
    public static List<Ray> rays = new ArrayList<>();
    public static List<Ray> rayReflections = new ArrayList<>();
    public static List<Object> mirrors = new ArrayList<>();
    public static double mouseX;
    public static double mouseY;
    public static MutableValue xPressed = new MutableValue(false);
    public static MutableValue zPressed = new MutableValue(false);
    public static boolean shiftPressed = false;
    public static boolean altPressed = false;
    public static boolean isEditMode = false;
    public static boolean isMousePressed = false;
    public static Object editedShape;
    public static List<ToolbarButton> toolbar = new ArrayList<>();

    @Override
    public void start(Stage primaryStage) {
        // Create a ray
        Ray ray = new Ray(0, 0, 0, 0);
        ray.create();
        rays.add(ray);

        scene.getStylesheets().add(getClass().getResource("/main.css").toExternalForm());

        WavelengthSlider wavelengthSlider = new WavelengthSlider(rays, rayReflections);

        // Create toolbar buttons
        ToolbarButton lineMirrorButton = new ToolbarButton("Line Mirror", zPressed, xPressed, 25, 25);
        ToolbarButton elipseMirrorButton = new ToolbarButton("Elipse Mirror", xPressed, zPressed, 150, 25);
        toolbar.add(lineMirrorButton);
        toolbar.add(elipseMirrorButton);

        lineMirrorButton.setOnAction(actionEvent -> {
            lineMirrorButton.variableToChange.setValue(!lineMirrorButton.variableToChange.getValue(), lineMirrorButton.oppositeVariable);
            lineMirrorButton.updateRender();
            elipseMirrorButton.updateRender();

        });

        elipseMirrorButton.setOnAction(actionEvent -> {
            elipseMirrorButton.variableToChange.setValue(!elipseMirrorButton.variableToChange.getValue(), elipseMirrorButton.oppositeVariable);
            elipseMirrorButton.updateRender();
            lineMirrorButton.updateRender();

        });

        lineMirrorButton.addToRoot();
        elipseMirrorButton.addToRoot();

        UserControls.setUserControls();

        primaryStage.setTitle("rtx 5090ti testing place");
        primaryStage.setScene(scene);

        primaryStage.show();
    }


    public static void drawRaysRecursively(Ray currentRay, int recursiveDepth) {
        // Get first mirror the object will intersect with
        double shortestIntersectionDistance = Double.MAX_VALUE;
        Object closestIntersectionMirror = null;
        Point2D closestIntersectionPoint = new Point2D(Double.MAX_VALUE, Double.MAX_VALUE);

        for (Object mirror : mirrors) {
            Point2D intersectionPoint = null;

            if (mirror instanceof LineMirror currentMirror) {
                // If the minimal distance to object bounds is higher than current shortest distance, this will not be the first object the ray intersects
                double minimalPossibleDistance = currentRay.getMinimalDistanceToBounds(currentMirror.getLayoutBounds());
                if (minimalPossibleDistance > shortestIntersectionDistance) continue;

                intersectionPoint = getRayIntersectionPoint(currentRay, currentMirror);
            }
            else if (mirror instanceof EllipseMirror currentMirror) {
                // If the minimal distance to object bounds is higher than current shortest distance, this will not be the first object the ray intersects
                double minimalPossibleDistance = currentRay.getMinimalDistanceToBounds(currentMirror.getLayoutBounds());
                if (minimalPossibleDistance > shortestIntersectionDistance) continue;

                intersectionPoint = getRayIntersectionPoint(currentRay, currentMirror.outline);
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

        // If there's no intersection, return
        if (closestIntersectionMirror == null) return;

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

        nextRay.setEndX(reflectedX);
        nextRay.setEndY(reflectedY);

        rayReflections.add(nextRay);
        drawRaysRecursively(nextRay, recursiveDepth + 1);
    }

    public static void main(String[] args) {
        launch();
    }
}