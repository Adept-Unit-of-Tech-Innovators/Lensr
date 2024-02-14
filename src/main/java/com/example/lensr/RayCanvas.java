package com.example.lensr;

import com.example.lensr.objects.Ray;
import javafx.scene.canvas.Canvas;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.util.List;

public class RayCanvas extends Canvas {
    WritableImage writableImage = new WritableImage((int) getWidth(), (int) getHeight());
    public RayCanvas(double width, double height) {
        super(width, height);
        setMouseTransparent(true);
        setBlendMode(BlendMode.LIGHTEN);
    }

    public void drawRays(List<Ray> rays) {
        if (rays.get(0).getWavelength() > 600) return;
        for (Ray ray : rays) {
            //customStrokeLine(ray);
            Color rayColor = (Color) ray.getStroke();
            rayColor = new Color(rayColor.getRed(), rayColor.getGreen(), rayColor.getBlue(), ray.getBrightness());
            getGraphicsContext2D().setStroke(rayColor);
            getGraphicsContext2D().setLineWidth(ray.getStrokeWidth());
            getGraphicsContext2D().strokeLine(ray.getStartX(), ray.getStartY(), ray.getEndX(), ray.getEndY());
        }
        //getGraphicsContext2D().drawImage(writableImage, 0, 0);
    }

    public void customStrokeLine(Ray ray) {
        getGraphicsContext2D().drawImage(writableImage, 0, 0);
        javafx.scene.image.PixelWriter pixelWriter = writableImage.getPixelWriter();
        javafx.scene.image.PixelReader pixelReader = writableImage.getPixelReader();

        double startX = ray.getStartX();
        double startY = ray.getStartY();
        double endX = ray.getEndX();
        double endY = ray.getEndY();

        double dx = endX - startX;
        double dy = endY - startY;
        double slope = dy / dx;

        Color rayColor = (Color) ray.getStroke();
        rayColor = new Color(rayColor.getRed(), rayColor.getGreen(), rayColor.getBlue(), ray.getBrightness());

        for (double x = startX; x <= endX; x++) {
            double y = startY + slope * (x - startX);

            if (x >= 0 && x < getWidth() && y >= 0 && y < getHeight()) {
                Color currentColor = pixelReader.getColor((int) x, (int) y);
                Color newColor = Color.color(
                        Math.max(currentColor.getRed(), rayColor.getRed()),
                        Math.max(currentColor.getGreen(), rayColor.getGreen()),
                        Math.max(currentColor.getBlue(), rayColor.getBlue()),
                        Math.max(currentColor.getOpacity(), rayColor.getOpacity())
                );
                pixelWriter.setColor((int) x, (int) y, newColor);
            }
        }
    }

    public void drawRay(Ray ray) {
        Color rayColor = (Color) ray.getStroke();
        rayColor = new Color(rayColor.getRed(), rayColor.getGreen(), rayColor.getBlue(), ray.getBrightness());
        getGraphicsContext2D().setStroke(rayColor);
        getGraphicsContext2D().setLineWidth(ray.getStrokeWidth());
        getGraphicsContext2D().strokeLine(ray.getStartX(), ray.getStartY(), ray.getEndX(), ray.getEndY());
    }

    public void clear() {
        getGraphicsContext2D().clearRect(0, 0, getWidth(), getHeight());
    }
}
