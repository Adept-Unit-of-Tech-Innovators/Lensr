package com.example.lensr.objects;

import javafx.geometry.Point2D;
import javafx.scene.shape.Line;

public class LensLine extends Line {
    private final SphericalLens parentLens;

    public LensLine(SphericalLens parentLens) {
        this.parentLens = parentLens;
    }

    public SphericalLens getParentLens() {
        return parentLens;
    }

    public void adjust() {
        int multiplier = 1;
        if (this == parentLens.getBottomLine()) multiplier = -1;

        double middleTopX = parentLens.getCenterX() + (parentLens.getMiddleHeight() / 2 * Math.cos(parentLens.getAngleOfRotation() + Math.PI / 2)) * multiplier;
        double middleTopY = parentLens.getCenterY() + (parentLens.getMiddleHeight() / 2 * Math.sin(parentLens.getAngleOfRotation() + Math.PI / 2)) * multiplier;

        setStartX(middleTopX - parentLens.getMiddleWidth() / 2 * Math.cos(parentLens.getAngleOfRotation()));
        setStartY(middleTopY - parentLens.getMiddleWidth() / 2 * Math.sin(parentLens.getAngleOfRotation()));

        setEndX(middleTopX + parentLens.getMiddleWidth() / 2 * Math.cos(parentLens.getAngleOfRotation()));
        setEndY(middleTopY + parentLens.getMiddleWidth() / 2 * Math.sin(parentLens.getAngleOfRotation()));
    }

    public Point2D getStart() {
        return new Point2D(getStartX(), getStartY());
    }

    public Point2D getEnd() {
        return new Point2D(getEndX(), getEndY());
    }
}
