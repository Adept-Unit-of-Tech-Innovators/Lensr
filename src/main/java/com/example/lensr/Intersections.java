package com.example.lensr;

import javafx.geometry.Point2D;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;

public class Intersections {


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


    public static double getCircleReflectionAngle(Line ray, Point2D intersectionPoint, Circle circle) {
        double angleOfIncidence = Math.atan2(ray.getEndY() - ray.getStartY(), ray.getEndX() - ray.getStartX());

        // Calculate the angle of the normal vector at the intersection point
        double normalAngle = Math.atan2(intersectionPoint.getY() - circle.getCenterY(), intersectionPoint.getX() - circle.getCenterX());

        return 2 * normalAngle - angleOfIncidence;
    }

}
