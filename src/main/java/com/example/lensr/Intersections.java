package com.example.lensr;

import com.example.lensr.objects.Ray;
import javafx.geometry.Point2D;
import javafx.scene.shape.*;


public class Intersections {

    public static Point2D getRayIntersectionPoint(Ray ray, Shape object) {
        // holy fucking shit we actually did it
        double intersectionX, intersectionY;

        // Temporarily set stroke width to a low number for intersection calculation
        ray.setStrokeWidth(0.01);
        object.setStrokeWidth(0.01);
        Shape intersectionShape = Shape.intersect(ray, object);
        ray.setStrokeWidth(LensrStart.globalStrokeWidth);
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

        Shape a = Shape.subtract(shape, copy);
        return a;
    }


    public static double getLineReflectionAngle(Ray ray, Line mirror) {
        double angleOfIncidence = Math.atan2(ray.getEndY() - ray.getStartY(), ray.getEndX() - ray.getStartX());

        // Calculate the angle of the mirror line
        double mirrorAngle = Math.atan2(mirror.getEndY() - mirror.getStartY(), mirror.getEndX() - mirror.getStartX());

        return 2 * mirrorAngle - angleOfIncidence;
    }


    public static double getEllipseReflectionAngle(Ray ray, Ellipse mirror) {
        double angleOfIncidence = Math.atan2(ray.getEndY() - ray.getStartY(), ray.getEndX() - ray.getStartX());

        // Calculate the angle of the normal vector at the intersection point
        double x = ray.getEndX();
        double y = ray.getEndY();
        double centerX = mirror.getCenterX();
        double centerY = mirror.getCenterY();
        double semiMajorAxis = mirror.getRadiusX();
        double semiMinorAxis = mirror.getRadiusY();

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

    public static double getLineRefractionAngle(Line ray, Line lens, double refractiveIndex, boolean isInLens)
    {
        double angleOfIncidence = Math.PI/2 - Math.atan2(ray.getEndY() - ray.getStartY(), ray.getEndX() - ray.getStartX());
        double lensAngle = Math.atan2(lens.getEndY() - lens.getStartY(), lens.getEndX() - lens.getStartX());
        double normalAngle = lensAngle + Math.PI/2;
//        if(lensAngle > Math.PI) normalAngle += Math.PI;

        if(true)
        {
            refractiveIndex = 1/refractiveIndex;
        }
        double refractedAngle = Math.asin(refractiveIndex * Math.sin(angleOfIncidence));

//        System.out.println("Angle of incidence: " + Math.toDegrees(angleOfIncidence));
//        System.out.println("Angle of lens: " + Math.toDegrees(lensAngle));
//        System.out.println("Normal angle: " + Math.toDegrees(normalAngle));
//        System.out.println("Refracted angle: " + Math.toDegrees(refractedAngle));
        return Math.PI/2 - refractedAngle;
    }

    public static double getArcRefractionAngle(Line ray, Arc arc, double refractiveIndex)
    {
        double angleOfIncidence = Math.atan2(ray.getEndY() - ray.getStartY(), ray.getEndX() - ray.getStartX());

        double centerX = arc.getCenterX();
        double centerY = arc.getCenterY();
        double pointX = ray.getEndX();
        double pointY = ray.getEndY();
        double radius = arc.getRadiusX();

        double normalAngle = Math.atan2((pointX - centerX) * radius, (pointY - centerY) * radius);
        double refractedAngle = Math.asin(1/refractiveIndex * Math.sin(normalAngle - angleOfIncidence));

        return refractedAngle;
    }
}