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
        scene.setOnMouseClicked(mouseEvent -> {
            if (!isEditMode) return;

            rays.get(0).setStartX(mouseEvent.getSceneX());
            rays.get(0).setStartY(mouseEvent.getSceneY());

            // Recalculate ray intersections after it position changed
            EventHandler<? super MouseEvent> mouseMoved = scene.getOnMouseMoved();
            mouseMoved.handle(mouseEvent);
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
                xPressed = true;

                double startMouseX = mouseX;
                double startMouseY = mouseY;

                EllipseMirror newMirror = new EllipseMirror(mouseX, mouseY, 0, 0);
                newMirror.createMirror();
                mirrors.add(newMirror);
                newMirror.scaleEllipse(startMouseX, startMouseY);
                updateRay(rays.get(0));
            }
            else if (keyEvent.getCode().toString().equals("Z")) {
                zPressed = true;

                LineMirror newMirror = new LineMirror(mouseX, mouseY);
                newMirror.createMirror();
                mirrors.add(newMirror);
                scaleLine(newMirror);
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

            if (!isEditMode || (!xPressed && !zPressed) ) return;

            if (keyEvent.getCode().toString().equals("X")) {
                xPressed = false;
                if (mirrors.get(0) instanceof EllipseMirror ellipseMirror) {
                    ellipseMirror.removeEllipseMirrorIfOverlaps();
                }
            }
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
        scene.setOnMouseDragged(mouseEvent -> {
            mouseX = mouseEvent.getX();
            mouseY = mouseEvent.getY();
        });
    }
}
