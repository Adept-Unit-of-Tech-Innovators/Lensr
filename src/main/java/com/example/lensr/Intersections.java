package com.example.lensr;

import javafx.geometry.Point2D;
import javafx.scene.shape.*;


public class Intersections {

    public static Point2D getRayIntersectionPoint(Line ray, Shape object) {
        // holy fucking shit we actually did it
        double intersectionX, intersectionY;

        // Temporarily set stroke width to a low number for intersection calculation
        ray.setStrokeWidth(0.01);
        object.setStrokeWidth(0.01);
        Shape intersectionShape = Shape.intersect(ray, object);
        ray.setStrokeWidth(0.5);
        object.setStrokeWidth(LensrStart.globalStrokeWidth);

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

        return new Point2D(intersectionX, intersectionY);
    }

    // Get the outline of a shape for ray intersection
    // This is the object ray will intersect with (ray should not intersect with the object fill)
    public static Shape getObjectOutline(Shape shape) {
        // Copying shape in a cursed way
        Shape copy = Shape.union(shape, shape);

        copy.setStrokeWidth(LensrStart.globalStrokeWidth);
        shape.setStrokeWidth(LensrStart.globalStrokeWidth);
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

    public static double getLineRefractionAngle(Line ray, Line lens, double refractiveIndex, boolean isEnteringLens)
    {
        double angleOfIncidence = Math.atan2(ray.getEndY() - ray.getStartY(), ray.getEndX() - ray.getStartX());
        double lensAngle = Math.atan2(lens.getEndY() - lens.getStartY(), lens.getEndX() - lens.getStartX());
        double normalAngle = lensAngle + Math.PI/2;

        System.out.println("Normal angle: " + Math.toDegrees(normalAngle));
        if(isEnteringLens) refractiveIndex = 1/refractiveIndex;
        System.out.println("Angle of refraction: " + Math.toDegrees(Math.asin(refractiveIndex * Math.sin(angleOfIncidence)) - normalAngle));
        return Math.asin(refractiveIndex * Math.sin(angleOfIncidence)) - normalAngle;
    }

    public static double getArcRefractionAngle(Line ray, Arc arc, double refractiveIndex)
    {
        double angleOfIncidence = Math.atan2(ray.getEndY() - ray.getStartY(), ray.getEndX() - ray.getStartX());
        //TODO: Calculate the normal angle
        double angleOfRefraction = Math.asin(refractiveIndex * Math.sin(angleOfIncidence));
        return angleOfIncidence + Math.toRadians(10);
    }


}