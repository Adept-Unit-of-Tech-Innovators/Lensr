package com.example.lensr;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Shape;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

import static com.example.lensr.Intersections.*;

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

        // Crate line (laser)
        Ray ray = new Ray(0, 0, 0, 0);
        ray.setStroke(Color.RED);
        ray.setStrokeWidth(0.5);


        // Create a circle mirror
        Circle circleMirror = new Circle(500, 320, 100);

        circleMirror.setFill(Color.WHITE);
        circleMirror.setStroke(Color.BLACK);
        circleMirror.setStrokeWidth(0.5);
        mirrors.add(circleMirror);


        // Create a line mirror
        Line lineMirror = new Line(300, 300, 700, 500);

        lineMirror.setStrokeWidth(0.5);
        mirrors.add(lineMirror);

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
                Circle newMirror = new CircleMirror(this.mouseX, this.mouseY, 1).createCircle();
                mirrors.add(newMirror);
                root.getChildren().add(newMirror);
                scaleCircle(newMirror);
                updateRay(ray);
            }
            if (keyEvent.getCode().toString().equals("Z")) {
                zPressed = true;
                Line newMirror = new LineMirror(this.mouseX, this.mouseY).createLine();
                mirrors.add(newMirror);
                root.getChildren().add(newMirror);
                scaleLine(newMirror);
                updateRay(ray);
            }
        });

        scene.setOnKeyReleased(keyEvent -> {
            if (keyEvent.getCode().toString().equals("X")) {
                xPressed = false;
                removeMirrorIfOverlaps();
            }
            if (keyEvent.getCode().toString().equals("Z")) {
                zPressed = false;
                removeMirrorIfOverlaps();
            }
        });

        scene.setOnMouseMoved(mouseEvent -> {
            this.mouseX = mouseEvent.getX();
            this.mouseY = mouseEvent.getY();
            if (!xPressed && !zPressed) {
                updateRay(ray);
            }
        });

        root.getChildren().addAll(lineMirror, circleMirror);

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

        for (Object mirror : mirrors) {
            // The ray cannot reflect of the same mirror twice in a row
            // TODO: It actually can, pls fix
            if (mirror == previousMirror) continue;

            if (mirror instanceof Line currentMirror) {
                Point2D intersectionPoint = getRayIntersectionPoint(currentRay, currentMirror);

                if (intersectionPoint != null) {
                    double intersectionDistance = Math.sqrt(
                            Math.pow(intersectionPoint.getX() - currentRay.getStartX(), 2) +
                                    Math.pow(intersectionPoint.getY() - currentRay.getStartY(), 2)
                    );

                    if (intersectionDistance < shortestIntersectionDistance) {
                        shortestIntersectionDistance = intersectionDistance;
                        closestIntersectionMirror = currentMirror;
                    }
                }
            }

            else if (mirror instanceof Circle currentMirror) {
                Point2D intersectionPoint = getRayIntersectionPoint(currentRay, currentMirror);

                if (intersectionPoint != null) {
                    double intersectionDistance = Math.sqrt(
                            Math.pow(intersectionPoint.getX() - currentRay.getStartX(), 2) +
                                    Math.pow(intersectionPoint.getY() - currentRay.getStartY(), 2)
                    );

                    if (intersectionDistance < shortestIntersectionDistance) {
                        shortestIntersectionDistance = intersectionDistance;
                        closestIntersectionMirror = currentMirror;
                    }
                }
            }
        }

        if (closestIntersectionMirror == null) return;

        Ray nextRay = new Ray(0, 0, 0, 0);
        nextRay.setVisible(false);
        nextRay.setStroke(Color.RED);
        nextRay.setStrokeWidth(1);

        if (closestIntersectionMirror instanceof Line mirror) {
            Point2D intersectionPoint = getRayIntersectionPoint(currentRay, mirror);

            currentRay.setEndX(intersectionPoint.getX());
            currentRay.setEndY(intersectionPoint.getY());

            // Calculate the angle of incidence
            double reflectionAngle = getLineReflectionAngle(currentRay, mirror);

            // Calculate the reflected ray's endpoint based on the reflection angle
            double reflectedX = intersectionPoint.getX() + SIZE * Math.cos(reflectionAngle);
            double reflectedY = intersectionPoint.getY() + SIZE * Math.sin(reflectionAngle);

            nextRay.setStartX(intersectionPoint.getX());
            nextRay.setStartY(intersectionPoint.getY());
            nextRay.setEndX(reflectedX);
            nextRay.setEndY(reflectedY);
        }
        else if (closestIntersectionMirror instanceof Circle mirror) {
            Point2D intersectionPoint = getRayIntersectionPoint(currentRay, mirror);

            currentRay.setEndX(intersectionPoint.getX());
            currentRay.setEndY(intersectionPoint.getY());

            double reflectionAngle = getCircleReflectionAngle(currentRay, intersectionPoint, mirror);

            // Calculate the end point of the reflected ray
            double reflectedX = intersectionPoint.getX() - SIZE * Math.cos(reflectionAngle);
            double reflectedY = intersectionPoint.getY() - SIZE * Math.sin(reflectionAngle);

            nextRay.setStartX(intersectionPoint.getX());
            nextRay.setStartY(intersectionPoint.getY());
            nextRay.setEndX(reflectedX);
            nextRay.setEndY(reflectedY);
        }
        nextRay.setVisible(true);
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

    public void scaleCircle(Circle circle) {
        new Thread(() -> {
            while (xPressed) {
                double radius = Math.sqrt(Math.pow(mouseX - circle.getCenterX(), 2) + Math.pow(mouseY - circle.getCenterY(), 2));
                circle.setRadius(radius);

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

    public void scaleLine(Line line) {
        new Thread(() -> {
            while (zPressed) {
                double endX = mouseX;
                double endY = mouseY;
                line.setEndX(endX);
                line.setEndY(endY);

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

    public void removeMirrorIfOverlaps() {
        for (Object mirror : mirrors) {
            if (mirror.equals(mirrors.get(mirrors.size()-1))) break;
            if ((mirror instanceof Shape mirrorShape) && (mirrors.get(mirrors.size()-1) instanceof Shape newMirror)) {
                if (Shape.intersect(newMirror, mirrorShape).getBoundsInLocal().getWidth() >= 0) {
                    root.getChildren().remove(newMirror);
                    mirrors.remove(newMirror);
                    break;
                }
            }
        }
    }


    public static void main(String[] args) {
        launch();
    }
}