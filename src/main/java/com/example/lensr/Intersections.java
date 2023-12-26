package com.example.lensr;

import javafx.geometry.Point2D;
import javafx.scene.shape.Line;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeType;

public class Intersections {

    public static Point2D getRayIntersectionPoint(Line ray, Shape object, Point2D previousIntersectionPoint) {
        // holy fucking shit we actually did it
        double intersectionX, intersectionY;

        // Temporarily set stroke width to a low number for intersection calculation
        ray.setStrokeWidth(0.01);
        Shape intersectionShape = Shape.intersect(ray, object);
        ray.setStrokeWidth(0.5);

        // If intersection shape has negative dimensions there is no intersection
        if (intersectionShape.getLayoutBounds().getHeight() < 0) return null;

        double maxX = intersectionShape.getLayoutBounds().getMaxX();
        double maxY = intersectionShape.getLayoutBounds().getMaxY();
        double minX = intersectionShape.getLayoutBounds().getMinX();
        double minY = intersectionShape.getLayoutBounds().getMinY();

        // Set intersection X and Y to the closest point from ray origin to intersection shape
        if (Math.abs(ray.getStartX() - minX) > Math.abs(ray.getStartX() - maxX)) {
            intersectionX = maxX;
        } else intersectionX = minX;

        if (Math.abs(ray.getStartY() - minY) > Math.abs(ray.getStartY() - maxY)) {
            intersectionY = maxY;
        } else intersectionY = minY;

        Point2D roundedIntersectionPoint = new Point2D(
                Math.round(intersectionX * 10.0) / 10.0,
                Math.round(intersectionY * 10.0) / 10.0
        );

        // TODO: THIS IS MOST LIKELY THE CAUSE OF THE BUG WHERE RAY PASSES THROUGH THE MIRROR
        // TODO: IDK HOW TO FIX IT THO
        // The thought process behind this is that if the intersection point is the same as the previous intersection point,
        // Set the intersection point to the second intersection point
        // Unclear? Explanation here: https://imgur.com/a/YYqg9KU
        if (roundedIntersectionPoint.equals(previousIntersectionPoint)) {
            if (intersectionX == maxX) intersectionX = minX;
            else intersectionX = maxX;

            if (intersectionY == maxY) intersectionY = minY;
            else intersectionY = maxY;
        }

        return new Point2D(intersectionX, intersectionY);
    }


    // Get the outline of a shape for ray intersection
    // This is the object ray will intersect with (ray should not intersect with the object fill)
    public static Shape getObjectOutline(Shape shape) {
        // Copying shape in a cursed way
        Shape copy = Shape.union(shape, shape);
        copy.setStrokeWidth(1);
        shape.setStrokeWidth(1);
        shape.setStrokeType(StrokeType.OUTSIDE);
        copy.setStrokeType(StrokeType.INSIDE);

        return Shape.subtract(shape, copy);
    }


    public static double getLineReflectionAngle(Line ray, Line mirror) {
        double angleOfIncidence = Math.atan2(ray.getEndY() - ray.getStartY(), ray.getEndX() - ray.getStartX());

        // Calculate the angle of the mirror line
        double mirrorAngle = Math.atan2(mirror.getEndY() - mirror.getStartY(), mirror.getEndX() - mirror.getStartX());

        return 2 * mirrorAngle - angleOfIncidence;
    }


    public static double getEllipseReflectionAngle(Line ray, EllipseMirror ellipseMirror) {
        double angleOfIncidence = Math.atan2(ray.getEndY() - ray.getStartY(), ray.getEndX() - ray.getStartX());

        // Calculate the angle of the normal vector at the intersection point
        double x = ray.getEndX();
        double y = ray.getEndY();
        double centerX = ellipseMirror.getCenterX();
        double centerY = ellipseMirror.getCenterY();
        double semiMajorAxis = ellipseMirror.getRadiusX();
        double semiMinorAxis = ellipseMirror.getRadiusY();

        // Equation of an ellipse: (x - h)^2 / a^2 + (y - k)^2 / b^2 = 1
        // Derivative of the ellipse equation: f'(x) = -((x - h) / a^2) / ((y - k) / b^2)
        // Normal vector: (-f'(x), 1)
        // according to chatgpt at least
        double normalAngle = Math.atan2(((y - centerY) * semiMajorAxis * semiMajorAxis), ((x - centerX) * semiMinorAxis * semiMinorAxis));

        // Adjust the normal angle based on the quadrant of the point
        if (x > centerX) {
            normalAngle += Math.PI;
        }

        return 2 * normalAngle - angleOfIncidence;
    }

}
