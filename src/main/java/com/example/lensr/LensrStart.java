package com.example.lensr;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.stage.Stage;

import static com.example.lensr.Intersections.*;

public class LensrStart extends Application {
    public static final int SIZE = 1000;

    @Override
    public void start(Stage primaryStage) {
        Pane pane = new Pane();
        Scene scene = new Scene(pane, SIZE, SIZE);

        // Crate line (laser)
        Line[] ray = {new Line(0, 0, 0,0)};
        ray[0].setStroke(Color.RED);
        ray[0].setStrokeWidth(2);

        // Create a circle (mirror)
        Circle circle = new Circle(500, 320, 100);

        Line[] reflectedRay = {new Line(0, 0, 0, 0)};

        scene.setOnMouseClicked(mouseEvent -> {
            ray[0].setStartX(mouseEvent.getSceneX());
            ray[0].setStartY(mouseEvent.getSceneY());

            // Recalculate ray intersections after it position changed
            EventHandler<? super MouseEvent> mouseMoved = scene.getOnMouseMoved();
            mouseMoved.handle(mouseEvent);
        });

        scene.setOnMouseMoved(mouseEvent -> {
            double endX = (mouseEvent.getX() - ray[0].getStartX()) * SIZE;
            double endY = (mouseEvent.getY() - ray[0].getStartY()) * SIZE;
            ray[0].setEndX(endX);
            ray[0].setEndY(endY);

            Point2D intersectionPoint = getCircleIntersectionPoint(ray[0], circle);
            if (intersectionPoint != null) {
                ray[0].setEndX(intersectionPoint.getX());
                ray[0].setEndY(intersectionPoint.getY());

                double reflectionAngle = getCircleReflectionAngle(ray[0], intersectionPoint, circle);

                // Calculate the end point of the reflected ray
                double reflectedX = intersectionPoint.getX() - SIZE * Math.cos(reflectionAngle);
                double reflectedY = intersectionPoint.getY() - SIZE * Math.sin(reflectionAngle);

                reflectedRay[0].setStartX(intersectionPoint.getX());
                reflectedRay[0].setStartY(intersectionPoint.getY());
                reflectedRay[0].setEndX(reflectedX);
                reflectedRay[0].setEndY(reflectedY);

                reflectedRay[0].setStroke(Color.GREEN);
                reflectedRay[0].setStrokeWidth(2);
                reflectedRay[0].setVisible(true);
            } else {
                reflectedRay[0].setVisible(false);
            }

            reflectedRay[0].toFront();
            ray[0].toFront();
        });

        pane.getChildren().addAll(reflectedRay[0], circle, ray[0]);

        primaryStage.setTitle("rtx 5090ti testing place");
        primaryStage.setScene(scene);
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch();
    }
}