package com.example.lensr;

import javafx.geometry.Point2D;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;

public class Intersections {


    public static Point2D getLineIntersectionPoint(Line ray, Line mirror) {
        double x1 = ray.getStartX();
        double y1 = ray.getStartY();
        double x2 = ray.getEndX();
        double y2 = ray.getEndY();

        double x3 = mirror.getStartX();
        double y3 = mirror.getStartY();
        double x4 = mirror.getEndX();
        double y4 = mirror.getEndY();

        double denominator = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);

        if (denominator == 0) {
            return null; // Lines are parallel, no intersection
        }

        double intersectionX = ((x1 * y2 - y1 * x2) * (x3 - x4) - (x1 - x2) * (x3 * y4 - y3 * x4)) / denominator;
        double intersectionY = ((x1 * y2 - y1 * x2) * (y3 - y4) - (y1 - y2) * (x3 * y4 - y3 * x4)) / denominator;

        // if (Math.floor(intersectionX) == Math.floor(ray.getStartX()) && Math.floor(intersectionY) == Math.floor(ray.getStartY())) return null;

        // Check if intersection point is within the line segments
        if (intersectionX >= Math.min(x1, x2) && intersectionX <= Math.max(x1, x2) &&
                intersectionY >= Math.min(y1, y2) && intersectionY <= Math.max(y1, y2) &&
                intersectionX >= Math.min(x3, x4) && intersectionX <= Math.max(x3, x4) &&
                intersectionY >= Math.min(y3, y4) && intersectionY <= Math.max(y3, y4)) {
            return new Point2D(intersectionX, intersectionY);
        }

        return null; // Intersection point is outside the line segments
    }


    public static double getLineReflectionAngle(Line ray, Line mirror) {
        double angleOfIncidence = Math.atan2(ray.getEndY() - ray.getStartY(), ray.getEndX() - ray.getStartX());

        // Calculate the angle of the mirror line
        double mirrorAngle = Math.atan2(mirror.getEndY() - mirror.getStartY(), mirror.getEndX() - mirror.getStartX());

        return 2 * mirrorAngle - angleOfIncidence;
    }


    public static Point2D getCircleIntersectionPoint(Line ray, Circle circle) {
        // If ray is pointing away from the circle, there is no intersection
        if ( (circle.getCenterX()- ray.getStartX()) * ray.getEndX() < 0 &&
                (circle.getCenterY() - ray.getStartY()) * ray.getEndY() < 0) return null;

        Vector lineVector = new Vector(ray.getEndX() - ray.getStartX(), ray.getEndY() - ray.getStartY());
        lineVector = lineVector.normalize();

        Vector startToCenterVector = new Vector(circle.getCenterX() - ray.getStartX(), circle.getCenterY() - ray.getStartY());

        double dotProduct = Vector.getDotProduct(lineVector, startToCenterVector);

        // Find the closest point on the line to the circle's center
        double closestPointX = ray.getStartX() + dotProduct * lineVector.x;
        double closestPointY = ray.getStartY() + dotProduct * lineVector.y;

        // Calculate the distance between the closest point on the line and the circle's center
        double distanceToCenter = Math.sqrt(Math.pow(circle.getCenterX() - closestPointX, 2) + Math.pow(circle.getCenterY() - closestPointY, 2));

        if (distanceToCenter > circle.getRadius()) {
            return null; // null = no intersection
        }

        // Calculate the distance from the closest point on the line to the intersection point
        double distanceToIntersection = Math.sqrt(circle.getRadius() * circle.getRadius() - distanceToCenter * distanceToCenter);

        double intersectionX = closestPointX - distanceToIntersection * lineVector.x;
        double intersectionY = closestPointY - distanceToIntersection * lineVector.y;

        // if (Math.floor(intersectionX) == Math.floor(ray.getStartX()) && Math.floor(intersectionY) == Math.floor(ray.getStartY())) return null;

        return new Point2D(intersectionX, intersectionY);
    }


    public static double getCircleReflectionAngle(Line ray, Point2D intersectionPoint, Circle circle) {
        double angleOfIncidence = Math.atan2(ray.getEndY() - ray.getStartY(), ray.getEndX() - ray.getStartX());

        // Calculate the angle of the normal vector at the intersection point
        double normalAngle = Math.atan2(intersectionPoint.getY() - circle.getCenterY(), intersectionPoint.getX() - circle.getCenterX());

        return 2 * normalAngle - angleOfIncidence;
    }

}
