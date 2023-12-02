package com.example.lensr;

import javafx.geometry.Point2D;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Shape;

public class Intersections {

    public static Point2D getRayIntersectionPoint(Line ray, Shape object) {
        // holy fucking shit we actually did it
        double intersectionX, intersectionY;

        ray.setStrokeWidth(0.1);
        Shape intersectionShape = Shape.intersect(ray, object);
        ray.setStrokeWidth(0.5);

        if (intersectionShape.getLayoutBounds().getHeight() < 0) return null;

        double maxX = intersectionShape.getLayoutBounds().getMaxX();
        double maxY = intersectionShape.getLayoutBounds().getMaxY();
        double minX = intersectionShape.getLayoutBounds().getMinX();
        double minY = intersectionShape.getLayoutBounds().getMinY();

        if (Math.abs(ray.getStartX() - minX) > Math.abs(ray.getStartX() - maxX)) {
            intersectionX = maxX;
        } else intersectionX = minX;

        if (Math.abs(ray.getStartY() - minY) > Math.abs(ray.getStartY() - maxY)) {
            intersectionY = maxY;
        } else intersectionY = minY;

        return new Point2D(intersectionX, intersectionY);
    }


    public static double getLineReflectionAngle(Line ray, Line mirror) {
        double angleOfIncidence = Math.atan2(ray.getEndY() - ray.getStartY(), ray.getEndX() - ray.getStartX());

        // Calculate the angle of the mirror line
        double mirrorAngle = Math.atan2(mirror.getEndY() - mirror.getStartY(), mirror.getEndX() - mirror.getStartX());

        return 2 * mirrorAngle - angleOfIncidence;
    }


    public static double getCircleReflectionAngle(Line ray, Circle circle) {
        double angleOfIncidence = Math.atan2(ray.getEndY() - ray.getStartY(), ray.getEndX() - ray.getStartX());

        // Calculate the angle of the normal vector at the intersection point
        double normalAngle = Math.atan2(ray.getEndY() - circle.getCenterY(), ray.getEndX() - circle.getCenterX());

        return 2 * normalAngle - angleOfIncidence;
    }

}
