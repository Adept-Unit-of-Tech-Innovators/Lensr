package com.example.lensr.objects;

import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;

public class LensArc extends Arc {
    private double thickness;
    private final SphericalLens parentLens;

    public LensArc(SphericalLens parentLens, double thickness) {
        this.parentLens = parentLens;
        this.thickness = thickness;
    }

    public Point2D getVertex() {
        int multiplier = -1;
        if (this == parentLens.getSecondArc()) multiplier = 1;

        return new Point2D(parentLens.getCenterX() + (parentLens.getMiddleWidth() / 2 + thickness) * Math.cos(parentLens.getAngleOfRotation()) * multiplier, parentLens.getCenterY() + (parentLens.getMiddleWidth() / 2 + thickness) * Math.sin(parentLens.getAngleOfRotation()) * multiplier);
    }

    public SphericalLens getParentLens() {
        return parentLens;
    }

    public void adjust() {
        setType(ArcType.OPEN);
        setFill(Color.TRANSPARENT);

        int multiplier = 1;
        if (this == parentLens.getSecondArc()) multiplier = -1;

        // Calculate radius of the arc (circular segment formula)
        double radius = Math.pow(parentLens.getMiddleHeight(), 2) / (8 * Math.abs(thickness)) + Math.abs(thickness) / 2;

        setRadiusX(radius);
        setRadiusY(radius);

        // Calculate the center point of the circle
        double arcCenterX = parentLens.getCenterX() + (radius - parentLens.getMiddleWidth() / 2 - Math.abs(thickness)) * Math.cos(parentLens.getAngleOfRotation()) * multiplier;
        double arcCenterY = parentLens.getCenterY() + (radius - parentLens.getMiddleWidth() / 2 - Math.abs(thickness)) * Math.sin(parentLens.getAngleOfRotation()) * multiplier;

        if (thickness < 0) {
            arcCenterX = parentLens.getCenterX() - (radius + parentLens.getMiddleWidth() / 2 - Math.abs(thickness)) * Math.cos(parentLens.getAngleOfRotation()) * multiplier;
            arcCenterY = parentLens.getCenterY() - (radius + parentLens.getMiddleWidth() / 2 - Math.abs(thickness)) * Math.sin(parentLens.getAngleOfRotation()) * multiplier;
        }
        setCenterX(arcCenterX);
        setCenterY(arcCenterY);

        // Calculate the ¿central angle?
        double angleInRadians = 2 * Math.asin(parentLens.getMiddleHeight() / (2 * radius));
        double angleInDegrees = Math.toDegrees(angleInRadians);
        if (radius < Math.abs(thickness)) angleInDegrees = 360 - angleInDegrees;

        // Set the angle at which the arc starts
        double startAngle = 180 - angleInDegrees / 2 - Math.toDegrees(parentLens.getAngleOfRotation());
        if (this == parentLens.getSecondArc())
            startAngle = -angleInDegrees / 2 - Math.toDegrees(parentLens.getAngleOfRotation());
        if (thickness < 0) startAngle += 180;

        setStartAngle(startAngle);
        setLength(angleInDegrees);
    }

    public double getThickness() {
        return thickness;
    }
    public void setThickness(double newThickness) {
        thickness = newThickness;
        adjust();
    }
}
