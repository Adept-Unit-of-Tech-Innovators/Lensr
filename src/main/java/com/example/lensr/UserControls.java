package com.example.lensr;

import javafx.geometry.Point2D;
import javafx.scene.Group;

import static com.example.lensr.LensrStart.*;

public class UserControls {

    public static void setUserControls() {
        scene.setOnMousePressed(mouseEvent -> {
            isMousePressed = true;
            mouseX = mouseEvent.getX();
            mouseY = mouseEvent.getY();

            // Close ellipse mirror edit if editing it
            if (editedShape instanceof Group group && group.getChildren().get(0) instanceof EllipseMirror mirror
                    && !group.getLayoutBounds().contains(new Point2D(mouseX, mouseY)))
            {
                    mirror.closeObjectEdit();
                    mirror.isEditPointClicked = false;
                    editedShape = null;
                    return;
            }

            // Close line mirror edit if editing it
            if (editedShape instanceof Group group && group.getChildren().get(0) instanceof LineMirror mirror
                    && !mirror.hitbox.contains(new Point2D(mouseX, mouseY)) && mirror.editPoints.stream().noneMatch(rectangle ->
                    rectangle.contains(new Point2D(mouseX, mouseY))))
            {
                mirror.closeObjectEdit();
                mirror.isEditPointClicked = false;
                editedShape = null;
                return;
            }

            // Place mirrors
            if (xPressed.getValue()) {
                EllipseMirror newMirror = new EllipseMirror(mouseX, mouseY, 0, 0);
                newMirror.create();
                mirrors.add(newMirror);
                newMirror.scale(mouseX, mouseY);
            }
            if (zPressed.getValue()) {
                LineMirror newMirror = new LineMirror(mouseX, mouseY);
                newMirror.create();
                mirrors.add(newMirror);
                newMirror.scale();
            }
        });

        scene.setOnMouseDragged(mouseEvent -> {
            mouseX = mouseEvent.getX();
            mouseY = mouseEvent.getY();
        });

        scene.setOnMouseReleased(mouseEvent -> {
            isMousePressed = false;
            if (xPressed.getValue()) {
                if (mirrors.get(mirrors.size() - 1) instanceof EllipseMirror ellipseMirror) {
                    ellipseMirror.removeIfOverlaps();
                }
                rays.get(0).update();
            }
            if (zPressed.getValue()) {
                if (mirrors.get(mirrors.size() - 1) instanceof LineMirror lineMirror) {
                    lineMirror.removeIfOverlaps();
                }
                rays.get(0).update();
            }
        });

        scene.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode().toString().equals("E")) {
                // If mode was switched during an edit, finish the edit
                if (xPressed.getValue()) {
                    xPressed.setValue(false);
                    if (!mirrors.isEmpty() && mirrors.get(0) instanceof EllipseMirror ellipseMirror) {
                        ellipseMirror.removeIfOverlaps();
                    }
                }
                else if (zPressed.getValue()) {
                    zPressed.setValue(false);
                    if (!mirrors.isEmpty() && mirrors.get(0) instanceof LineMirror lineMirror) {
                        lineMirror.removeIfOverlaps();
                    }
                }

                if (isEditMode) {
                    for (ToolbarButton button : toolbar) {
                        button.disableProperty().setValue(true);
                    }
                    for (Object mirror : mirrors) {
                        if (mirror instanceof EllipseMirror ellipseMirror) {
                            ellipseMirror.closeObjectEdit();
                        }
                    }
                }
                else {
                    for (ToolbarButton button : toolbar) {
                        button.disableProperty().setValue(false);
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
            if (keyEvent.getCode().toString().equals("X") && isEditMode) {
                xPressed.setValue(!xPressed.getValue());
                zPressed.setValue(false);
            }
            else if (keyEvent.getCode().toString().equals("Z") && isEditMode) {
                zPressed.setValue(!zPressed.getValue());
                xPressed.setValue(false);
            }
            else if (keyEvent.getCode().toString().equals("C") && isEditMode) {
                rays.get(0).setStartX(mouseX);
                rays.get(0).setStartY(mouseY);

                // Recalculate ray intersections after it position changed
                scene.getOnMouseMoved();
                rays.get(0).update();
            }
            for (ToolbarButton button : toolbar) {
                button.updateRender();
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
        });

        scene.setOnMouseMoved(mouseEvent -> {
            if (!isEditMode) return;

            mouseX = mouseEvent.getX();
            mouseY = mouseEvent.getY();

            if (!xPressed.getValue() && !zPressed.getValue()) {
                rays.get(0).update();
            }
        });
    }
}
