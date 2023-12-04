package com.example.lensr;

import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;

import static com.example.lensr.LensrStart.*;
import static com.example.lensr.EllipseMirror.*;
import static com.example.lensr.LineMirror.*;

public class UserControls {
    // ok this type of commenting is super cool please use it (If you're seeing "/**" click those 3 lines "Toggle rendered view" on the left side)
    // They only work in IntelliJ tho :(
    /**
     * Mouse click - Set ray origin <br>
     * Mouse move - Set ray direction <br>
     * X - Spawn a new Ellipse <br>
     * Z - Spawn a new Line <br>
     */

    public static void setUserControls() {
        scene.setOnMouseClicked(mouseEvent -> {
            rays.get(0).setStartX(mouseEvent.getSceneX());
            rays.get(0).setStartY(mouseEvent.getSceneY());

            // Recalculate ray intersections after it position changed
            EventHandler<? super MouseEvent> mouseMoved = scene.getOnMouseMoved();
            mouseMoved.handle(mouseEvent);
        });

        scene.setOnKeyPressed(keyEvent -> {
            if (xPressed || zPressed) {
                return;
            }
            if (keyEvent.getCode().toString().equals("SHIFT")) {
                shiftPressed = true;
            }

            if (xPressed || zPressed) return;

            else if (keyEvent.getCode().toString().equals("X")) {
                xPressed = true;
                EllipseMirror newMirror = new EllipseMirror(mouseX, mouseY, 0, 0);
                mirrors.add(newMirror);
                newMirror.scaleEllipse();
                updateRay(rays.get(0));
            }
            else if (keyEvent.getCode().toString().equals("Z")) {
                zPressed = true;
                LineMirror newMirror = new LineMirror(mouseX, mouseY);
                mirrors.add(newMirror);
                scaleLine(newMirror);
                updateRay(rays.get(0));
            }
        });

        scene.setOnKeyReleased(keyEvent -> {
            if (keyEvent.getCode().toString().equals("SHIFT")) {
                shiftPressed = false;
            }

            if (keyEvent.getCode().toString().equals("X")) {
                xPressed = false;
                removeEllipseMirrorIfOverlaps();
            }
            else if (keyEvent.getCode().toString().equals("Z")) {
                zPressed = false;
                removeLineMirrorIfOverlaps();
            }
        });

        scene.setOnMouseMoved(mouseEvent -> {
            mouseX = mouseEvent.getX();
            mouseY = mouseEvent.getY();

            if (!xPressed && !zPressed) {
                updateRay(rays.get(0));
            }
        });

    }
}
