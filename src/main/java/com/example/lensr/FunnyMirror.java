package com.example.lensr;

import javafx.concurrent.Task;
import javafx.scene.shape.Polyline;

import java.util.List;

import static com.example.lensr.LensrStart.*;
public class FunnyMirror extends Polyline {

    public FunnyMirror() {

    }

    public void draw() {
        root.getChildren().add(this);
        setStrokeWidth(1);
        setStroke(mirrorColor);
        Task<Void> task = new Task<Void>() {
            @Override
            public Void call() throws Exception {
                List<Double> points = getPoints();
                int index = 0;
                while (isMousePressed) {
                    if (isCancelled()) {
                        break;
                    }
                    if (index < 2) {
                        points.add(mousePos.getX());
                        points.add(mousePos.getY());
                        index = index + 2;
                        continue;
                    }
                    if ((Math.abs(points.get(index - 2) - mousePos.getX()) > 100) || (Math.abs(points.get(index - 1) - mousePos.getY()) > 100)) {
                        points.add(mousePos.getX());
                        points.add(mousePos.getY());
                        index = index + 2;
                    }
                }
                return null;
            }
        };
        new Thread(task).start();

    }
}
