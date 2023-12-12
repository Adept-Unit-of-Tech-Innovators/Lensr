package com.example.lensr;

import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;

import static com.example.lensr.LensrStart.*;
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
        scene.setOnMousePressed(mouseEvent -> {
            isMousePressed = true;
            mouseX = mouseEvent.getX();
            mouseY = mouseEvent.getY();
            if (xPressed) {
                EllipseMirror newMirror = new EllipseMirror(mouseX, mouseY, 0, 0);
                newMirror.createMirror();
                mirrors.add(newMirror);
                newMirror.scaleEllipse(mouseX, mouseY);
            }
        });

        scene.setOnMouseDragged(mouseEvent -> {
            mouseX = mouseEvent.getX();
            mouseY = mouseEvent.getY();
        });

        scene.setOnMouseReleased(mouseEvent -> {
            isMousePressed = false;
            if (xPressed) {
                if (mirrors.get(mirrors.size() - 1) instanceof EllipseMirror ellipseMirror) {
                    ellipseMirror.removeEllipseMirrorIfOverlaps();
                }
                updateRay(rays.get(0));
            }
        });

        scene.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode().toString().equals("E")) {
                // If mode was switched during an edit, finish the edit
                if (xPressed) {
                    xPressed = false;
                    if (mirrors.get(0) instanceof EllipseMirror ellipseMirror) {
                        ellipseMirror.removeEllipseMirrorIfOverlaps();
                    }
                }
                else if (zPressed) {
                    zPressed = false;
                    removeLineMirrorIfOverlaps();
                }

                if (isEditMode) {
                    for (Object mirror : mirrors) {
                        if (mirror instanceof EllipseMirror ellipseMirror) {
                            ellipseMirror.closeObjectEdit();
                        }
                    }
                }

                isEditMode = !isEditMode;
            }

            if (keyEvent.getCode().toString().equals("SHIFT")) {
                shiftPressed = true;
            }
            if (keyEvent.getCode().toString().equals("ALT")) {
                altPressed = true;
            }

            if (xPressed || zPressed || !isEditMode) return;

            if (keyEvent.getCode().toString().equals("X")) {
                xPressed = !xPressed;
            }
            else if (keyEvent.getCode().toString().equals("Z")) {
                zPressed = true;

                LineMirror newMirror = new LineMirror(mouseX, mouseY);
                newMirror.createMirror();
                mirrors.add(newMirror);
                scaleLine(newMirror);
                updateRay(rays.get(0));
            }
            else if (keyEvent.getCode().toString().equals("C")) {
            if (!isEditMode) return;

            rays.get(0).setStartX(mouseX);
            rays.get(0).setStartY(mouseY);

            // Recalculate ray intersections after it position changed
            EventHandler<? super MouseEvent> mouseMoved = scene.getOnMouseMoved();
                    updateRay(rays.get(0));

            }
        });

        scene.setOnKeyReleased(keyEvent -> {
            if (keyEvent.getCode().toString().equals("SHIFT")) {
                shiftPressed = false;
            }
            if (keyEvent.getCode().toString().equals("ALT")) {
                altPressed = false;
            }

//            if (!isEditMode || (!xPressed && !zPressed) ) return;
            else if (keyEvent.getCode().toString().equals("Z")) {
                zPressed = false;
                removeLineMirrorIfOverlaps();
            }
        });

        scene.setOnMouseMoved(mouseEvent -> {
            if (!isEditMode) return;

            mouseX = mouseEvent.getX();
            mouseY = mouseEvent.getY();

            if (!xPressed && !zPressed) {
                updateRay(rays.get(0));
            }
        });
    }
}
