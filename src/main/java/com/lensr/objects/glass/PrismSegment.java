package com.lensr.objects.glass;

import javafx.scene.shape.Line;

public class PrismSegment extends Line {
    Prism parentPrism;

    public PrismSegment(double startX, double startY, double endX, double endY, Prism parentPrism) {
        setStartX(startX);
        setStartY(startY);
        setEndX(endX);
        setEndY(endY);
        this.parentPrism = parentPrism;
    }

    public Prism getParentPrism() {
        return parentPrism;
    }
}
