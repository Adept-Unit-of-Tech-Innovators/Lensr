package com.example.lensr;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

import static com.example.lensr.Intersections.*;

public class LensrStart extends Application {
    public static Pane root = new Pane();
    public static final int SIZE = 1000;
    public static List<Object> mirrors = new ArrayList<>();
    public static List<Line> rayReflections = new ArrayList<>();
    public double mouseX;
    public double mouseY;
    public boolean Xpressed = false;

    @Override
    public void start(Stage primaryStage) {
        Scene scene = new Scene(root, SIZE, SIZE);

        // Crate line (laser)
        Line[] ray = {new Line(0, 0, 0,0)};
        ray[0].setStroke(Color.RED);
        ray[0].setStrokeWidth(1);


        // Create a circle mirror
        Circle circleMirror = new Circle(500, 320, 100);

        circleMirror.setFill(Color.WHITE);
        circleMirror.setStroke(Color.BLACK);
        circleMirror.setStrokeWidth(1);
        mirrors.add(circleMirror);


        // Create a line mirror
        Line lineMirror = new Line(300, 700, 700, 500);

        lineMirror.setStrokeWidth(1);
        mirrors.add(lineMirror);


        Line[] reflectedRay = {new Line(0, 0, 0, 0)};

        scene.setOnMouseClicked(mouseEvent -> {
            ray[0].setStartX(mouseEvent.getSceneX());
            ray[0].setStartY(mouseEvent.getSceneY());

            // Recalculate ray intersections after it position changed
            EventHandler<? super MouseEvent> mouseMoved = scene.getOnMouseMoved();
            mouseMoved.handle(mouseEvent);
        });

        scene.setOnKeyPressed(keyEvent -> {
            if (Xpressed) {
                return;
            }
            if (keyEvent.getCode().toString().equals("X")) {
                Xpressed = true;
                Circle newMirror = new CircleMirror(this.mouseX, this.mouseY, 1).createCircle();
                mirrors.add(newMirror);
                root.getChildren().add(newMirror);
                scaleCircle(newMirror);
                updateRay(ray);
                System.out.println("added mirror");
            }
        });

        scene.setOnKeyReleased(keyEvent -> {
            if (keyEvent.getCode().toString().equals("X")) {
                Xpressed = false;
            }
        });

        scene.setOnMouseMoved(mouseEvent -> {
            this.mouseX = mouseEvent.getX();
            this.mouseY = mouseEvent.getY();
            updateRay(ray);
        });

        root.getChildren().addAll(reflectedRay[0], ray[0], lineMirror, circleMirror);

        primaryStage.setTitle("rtx 5090ti testing place");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void drawRaysRecursively(Line currentRay, Object previousMirror) {
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

        Line nextRay = new Line(0, 0, 0, 0);
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
        root.getChildren().add(nextRay);
        drawRaysRecursively(nextRay, closestIntersectionMirror);
    }

    public void updateRay(Line[] ray) {
        double endX = (mouseX - ray[0].getStartX()) * SIZE;
        double endY = (mouseY - ray[0].getStartY()) * SIZE;
        ray[0].setEndX(endX);
        ray[0].setEndY(endY);

        root.getChildren().removeAll(rayReflections);
        drawRaysRecursively(ray[0], null);
    }

    public void scaleCircle(Circle circle) {
        new Thread(() -> {
            while (Xpressed) {
                double radius = Math.sqrt(Math.pow(mouseX - circle.getCenterX(), 2) + Math.pow(mouseY - circle.getCenterY(), 2));
                circle.setRadius(radius);

                // Update the UI on the JavaFX application thread
                Platform.runLater(() -> {
                    // Update UI components or perform other UI-related tasks
                    // Example: circle.setRadius(radius);
                });

                try {
                    Thread.sleep(10); // Adjust the sleep time as needed
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }


    public static void main(String[] args) {
        launch();
    }
}