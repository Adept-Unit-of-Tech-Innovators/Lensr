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

public class HelloApplication extends Application {

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

                double angleOfIncidence = Math.atan2(ray[0].getEndY() - ray[0].getStartY(), ray[0].getEndX() - ray[0].getStartX());

                // Calculate the angle of the normal vector at the intersection point
                double normalAngle = Math.atan2(intersectionPoint.getY() - circle.getCenterY(), intersectionPoint.getX() - circle.getCenterX());

                double angleOfReflection = 2 * normalAngle - angleOfIncidence;

                // Calculate the end point of the reflected ray
                double reflectedX = intersectionPoint.getX() - SIZE * Math.cos(angleOfReflection);
                double reflectedY = intersectionPoint.getY() - SIZE * Math.sin(angleOfReflection);

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

        primaryStage.setTitle("rtx 5090ti test grounds");
        primaryStage.setScene(scene);
        primaryStage.show();
    }


    public static Point2D getCircleIntersectionPoint(Line line, Circle circle) {
        // If ray is pointing away from the circle, there is no intersection
        if ( (circle.getCenterX()- line.getStartX()) * line.getEndX() < 0 &&
             (circle.getCenterY() - line.getStartY()) * line.getEndY() < 0) return null;

        Vector lineVector = new Vector(line.getEndX() - line.getStartX(), line.getEndY() - line.getStartY());
        lineVector = lineVector.normalize();

        Vector startToCenterVector = new Vector(circle.getCenterX() - line.getStartX(), circle.getCenterY() - line.getStartY());

        double dotProduct = Vector.getDotProduct(lineVector, startToCenterVector);

        // Find the closest point on the line to the circle's center
        double closestPointX = line.getStartX() + dotProduct * lineVector.x;
        double closestPointY = line.getStartY() + dotProduct * lineVector.y;

        // Calculate the distance between the closest point on the line and the circle's center
        double distanceToCenter = Math.sqrt(Math.pow(circle.getCenterX() - closestPointX, 2) + Math.pow(circle.getCenterY() - closestPointY, 2));

        if (distanceToCenter > circle.getRadius()) {
            return null; // null = no intersection
        }

        // Calculate the distance from the closest point on the line to the intersection point
        double distanceToIntersection = Math.sqrt(circle.getRadius() * circle.getRadius() - distanceToCenter * distanceToCenter);

        double intersectionX = closestPointX - distanceToIntersection * lineVector.x;
        double intersectionY = closestPointY - distanceToIntersection * lineVector.y;

        return new Point2D(intersectionX, intersectionY);
    }


    public static double getTangentAngle(Point2D point) {
        double angle = Math.atan2(point.getX(), point.getY());
        angle = Math.toDegrees(angle);

        // normalize angle to 0-360 deg
        if (angle < 0) angle += 360;

        return angle;
    }


    public static void main(String[] args) {
        launch();
    }
}