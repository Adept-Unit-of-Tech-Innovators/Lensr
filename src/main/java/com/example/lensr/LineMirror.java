package com.example.lensr;

import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
public class LineMirror {
    double startX;
    double startY;
    double endX;
    double endY;

    public LineMirror(double mouseX, double mouseY) {
        this.startX = mouseX;
        this.startY = mouseY;
        this.endX = mouseX;
        this.endY = mouseY;
    }

    public Line createLine() {
        Line lineMirror = new Line(startX, startY, endX, endY);
        lineMirror.setStrokeWidth(0.5);
        lineMirror.setStroke(Color.LIGHTGREEN);

        return lineMirror;
    }

}



