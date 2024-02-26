package com.example.lensr.ui;

import com.example.lensr.objects.lightsources.Ray;
import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.effect.BlendMode;
import javafx.scene.paint.Color;

import java.util.List;

public class RayCanvas extends Canvas {
    public RayCanvas(double width, double height) {
        super(width, height);
        setMouseTransparent(true);
        setBlendMode(BlendMode.LIGHTEN);
        getGraphicsContext2D().setGlobalBlendMode(BlendMode.LIGHTEN);
    }

    public void drawRays(List<Ray> rays) {
        final List<Ray> raysCopy = List.copyOf(rays);
        Platform.runLater(() -> {
            for (Ray ray : raysCopy) {
                Color rayColor = (Color) ray.getStroke();
                rayColor = new Color(rayColor.getRed(), rayColor.getGreen(), rayColor.getBlue(), ray.getBrightness());
                getGraphicsContext2D().setStroke(rayColor);
                getGraphicsContext2D().setLineWidth(ray.getStrokeWidth());
                getGraphicsContext2D().strokeLine(ray.getStartX(), ray.getStartY(), ray.getEndX(), ray.getEndY());
            }
        });
    }

    public void clear() {
        // For some reason clearRect doesn't work with the LIGHTEN blend mode, so it has to be reset
        getGraphicsContext2D().setGlobalBlendMode(BlendMode.SRC_OVER);
        getGraphicsContext2D().clearRect(0, 0, getWidth(), getHeight());
        getGraphicsContext2D().setGlobalBlendMode(BlendMode.LIGHTEN);
    }
}
