package com.example.lensr;

import com.example.lensr.objects.Ray;
import javafx.scene.canvas.Canvas;
import javafx.scene.paint.Color;

import java.util.List;

public class RayCanvas extends Canvas {
    public RayCanvas(double width, double height) {
        super(width, height);
    }

    public void drawRays(List<Ray> rays) {
        for (Ray ray : rays) {
            Color rayColor = (Color) ray.getStroke();
            rayColor = new Color(rayColor.getRed(), rayColor.getGreen(), rayColor.getBlue(), ray.getBrightness());
            getGraphicsContext2D().setStroke(rayColor);
            getGraphicsContext2D().setLineWidth(ray.getStrokeWidth());
            getGraphicsContext2D().strokeLine(ray.getStartX(), ray.getStartY(), ray.getEndX(), ray.getEndY());
        }
    }

    public void clear() {
        getGraphicsContext2D().clearRect(0, 0, getWidth(), getHeight());
    }
}
