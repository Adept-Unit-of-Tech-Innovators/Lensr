package com.lensr.objects.glass;

import javafx.geometry.Point2D;
import javafx.scene.shape.Line;

public class LensLine extends Line {
    private final SphericalLens parentLens;
    private final double relativePosition;

    public LensLine(SphericalLens parentLens, double relativePosition) {
        this.parentLens = parentLens;
        this.relativePosition = relativePosition;
    }

    public SphericalLens getParentLens() {
        return parentLens;
    }

    public void scale() {
        double middleTopX = parentLens.getCenterX() + parentLens.getHeight() / 2 * Math.cos(parentLens.getAngleOfRotation() + Math.PI / 2 + relativePosition);
        double middleTopY = parentLens.getCenterY() + parentLens.getHeight() / 2 * Math.sin(parentLens.getAngleOfRotation() + Math.PI / 2 + relativePosition);

        setStartX(middleTopX - parentLens.getWidth() / 2 * Math.cos(parentLens.getAngleOfRotation()));
        setStartY(middleTopY - parentLens.getWidth() / 2 * Math.sin(parentLens.getAngleOfRotation()));

        setEndX(middleTopX + parentLens.getWidth() / 2 * Math.cos(parentLens.getAngleOfRotation()));
        setEndY(middleTopY + parentLens.getWidth() / 2 * Math.sin(parentLens.getAngleOfRotation()));
    }

    public void move(double deltaX, double deltaY) {
        setStartX(getStartX() + deltaX);
        setStartY(getStartY() + deltaY);
        setEndX(getEndX() + deltaX);
        setEndY(getEndY() + deltaY);
    }

    public Point2D getStart() {
        return new Point2D(getStartX(), getStartY());
    }

    public Point2D getEnd() {
        return new Point2D(getEndX(), getEndY());
    }
}
