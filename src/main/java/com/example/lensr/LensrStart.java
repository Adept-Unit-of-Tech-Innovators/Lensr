package com.example.lensr;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

import static com.example.lensr.Intersections.*;
import static com.example.lensr.CircleMirror.*;
import static com.example.lensr.LineMirror.*;

public class LensrStart extends Application {
    public static Pane root = new Pane();
    public static final int SIZE = 1000;
    public static List<Object> mirrors = new ArrayList<>();
    public static List<Ray> rayReflections = new ArrayList<>();
    public double mouseX;
    public double mouseY;
    public boolean xPressed = false;
    public boolean zPressed = false;
    public final Object lock = new Object();

    @Override
    public void start(Stage primaryStage) {
        Scene scene = new Scene(root, SIZE, SIZE);

        // Crate a ray
        Ray ray = new Ray(0, 0, 0, 0);
        ray.setStroke(Color.RED);
        ray.setStrokeWidth(0.5);

        scene.setOnMouseClicked(mouseEvent -> {
            ray.setStartX(mouseEvent.getSceneX());
            ray.setStartY(mouseEvent.getSceneY());

            // Recalculate ray intersections after it position changed
            EventHandler<? super MouseEvent> mouseMoved = scene.getOnMouseMoved();
            mouseMoved.handle(mouseEvent);
        });


        scene.setOnKeyPressed(keyEvent -> {
            if (xPressed || zPressed) {
                return;
            }
            if (keyEvent.getCode().toString().equals("X")) {
                xPressed = true;
                CircleMirror newMirror = new CircleMirror(this.mouseX, this.mouseY, 1);
                mirrors.add(newMirror);
                scaleCircle(newMirror);
                updateRay(ray);
            }
            if (keyEvent.getCode().toString().equals("Z")) {
                zPressed = true;
                LineMirror newMirror = new LineMirror(this.mouseX, this.mouseY);
                mirrors.add(newMirror);
                scaleLine(newMirror);
                updateRay(ray);
            }
        });

        scene.setOnKeyReleased(keyEvent -> {
            if (keyEvent.getCode().toString().equals("X")) {
                xPressed = false;
                removeCircleMirrorIfOverlaps();
            }
            if (keyEvent.getCode().toString().equals("Z")) {
                zPressed = false;
                removeLineMirrorIfOverlaps();
            }
        });

        scene.setOnMouseMoved(mouseEvent -> {
            this.mouseX = mouseEvent.getX();
            this.mouseY = mouseEvent.getY();
            if (!xPressed && !zPressed) {
                updateRay(ray);
            }
        });

        primaryStage.setTitle("rtx 5090ti testing place");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void drawRaysRecursively(Ray currentRay, Object previousMirror, int recursiveDepth) {
        // Limit recursive depth
        if (recursiveDepth > 500) return;

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
                double minimalPossibleDistance = getMinimalDistanceToBounds(currentRay, currentMirror.getLayoutBounds());
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

            else if (mirror instanceof CircleMirror currentMirror) {
                // If the minimal distance to object bounds is higher than current shortest distance, this will not be the first object the ray intersects
                double minimalPossibleDistance = getMinimalDistanceToBounds(currentRay, currentMirror.getLayoutBounds());
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

        if (closestIntersectionMirror == null) return;

        Ray nextRay = new Ray(0, 0, 0, 0);
        nextRay.setStroke(Color.RED);
        nextRay.setStrokeWidth(0.5);

        if (closestIntersectionMirror instanceof LineMirror mirror) {
            currentRay.setEndX(closestIntersectionPoint.getX());
            currentRay.setEndY(closestIntersectionPoint.getY());

            // Calculate the angle of incidence
            double reflectionAngle = getLineReflectionAngle(currentRay, mirror);

            // Calculate the reflected ray's endpoint based on the reflection angle
            double reflectedX = closestIntersectionPoint.getX() + SIZE * Math.cos(reflectionAngle);
            double reflectedY = closestIntersectionPoint.getY() + SIZE * Math.sin(reflectionAngle);

            nextRay.setStartX(closestIntersectionPoint.getX());
            nextRay.setStartY(closestIntersectionPoint.getY());
            nextRay.setEndX(reflectedX);
            nextRay.setEndY(reflectedY);
        }
        else if (closestIntersectionMirror instanceof CircleMirror mirror) {
            currentRay.setEndX(closestIntersectionPoint.getX());
            currentRay.setEndY(closestIntersectionPoint.getY());

            double reflectionAngle = getCircleReflectionAngle(currentRay, closestIntersectionPoint, mirror);

            // Calculate the end point of the reflected ray
            double reflectedX = closestIntersectionPoint.getX() - SIZE * Math.cos(reflectionAngle);
            double reflectedY = closestIntersectionPoint.getY() - SIZE * Math.sin(reflectionAngle);

            nextRay.setStartX(closestIntersectionPoint.getX());
            nextRay.setStartY(closestIntersectionPoint.getY());
            nextRay.setEndX(reflectedX);
            nextRay.setEndY(reflectedY);
        }
        rayReflections.add(nextRay);
        drawRaysRecursively(nextRay, closestIntersectionMirror, recursiveDepth + 1);
    }

    public void updateRay(Ray ray) {
        double endX = (mouseX - ray.getStartX()) * SIZE;
        double endY = (mouseY - ray.getStartY()) * SIZE;
        ray.setEndX(endX);
        ray.setEndY(endY);

        root.getChildren().removeAll(rayReflections);

        drawRaysRecursively(ray, null, 0);
    }

    public void scaleCircle(CircleMirror circleMirror) {
        new Thread(() -> {
            while (xPressed) {
                double radius = Math.sqrt(Math.pow(mouseX - circleMirror.getCenterX(), 2) + Math.pow(mouseY - circleMirror.getCenterY(), 2));
                circleMirror.setRadius(radius);

                // Update the UI on the JavaFX application thread
                Platform.runLater(() -> {
                    // Update UI components or perform other UI-related tasks
                    // Example: circle.setRadius(radius);
                });

                synchronized (lock) {
                    try {
                        lock.wait(10); // Adjust the wait time as needed
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    public void scaleLine(LineMirror lineMirror) {
        new Thread(() -> {
            while (zPressed) {
                double endX = mouseX;
                double endY = mouseY;
                lineMirror.setEndX(endX);
                lineMirror.setEndY(endY);

                // Update the UI on the JavaFX application thread
                Platform.runLater(() -> {
                    // Update UI components or perform other UI-related tasks
                    // Example: circle.setRadius(radius);
                });
                synchronized (lock) {
                    try {
                        lock.wait(10); // Adjust the sleep time as needed
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }


    public static double getMinimalDistanceToBounds(Ray ray, Bounds bounds) {
        // Get the start position of the ray
        double startX = ray.getStartX();
        double startY = ray.getStartY();

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


    public static void main(String[] args) {
        launch();
    }
}