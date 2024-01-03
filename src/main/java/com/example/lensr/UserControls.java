package com.example.lensr;

import javafx.geometry.Point2D;
import javafx.scene.Group;

import static com.example.lensr.LensrStart.*;

public class UserControls {

    public static void setUserControls() {
        scene.setOnMousePressed(mouseEvent -> {
            isMousePressed = true;
            mousePos = new Point2D(mouseEvent.getX(), mouseEvent.getY());

            // Close ellipse mirror edit if editing it
            if (editedShape instanceof Group group && group.getChildren().get(0) instanceof EllipseMirror mirror
                    && !group.getLayoutBounds().contains(mousePos))
            {
                mirror.closeObjectEdit();
                mirror.isEditPointClicked.setValue(false);
                editedShape = null;
                return;
            }

            // Close line mirror edit if editing it
            if (editedShape instanceof Group group && group.getChildren().get(0) instanceof LineMirror mirror
                    && !mirror.isMouseOnHitbox && mirror.editPoints.stream().noneMatch(rectangle ->
                    rectangle.contains(mousePos)))
            {
                mirror.closeObjectEdit();
                mirror.isEditPointClicked.setValue(false);
                editedShape = null;
                return;
            }

            if (editedShape instanceof Group group && group.getChildren().get(0) instanceof Ray ray
                    && !ray.laserPointer.contains(mousePos) && ray.editPoints.stream().noneMatch(rectangle ->
                    rectangle.contains(mousePos)))
            {
                ray.closeObjectEdit();
                ray.isEditPointClicked.setValue(false);
                ray.isEdited = false;
                editedShape = null;
                ray.update();
                return;
            }


            // Place mirrors
            if (xPressed.getValue()) {
                EllipseMirror newMirror = new EllipseMirror(mousePos.getX(), mousePos.getY(), 0, 0);
                newMirror.create();
                mirrors.add(newMirror);
                newMirror.scale(mousePos);
            }
            if (zPressed.getValue()) {
                LineMirror newMirror = new LineMirror(mousePos.getX(), mousePos.getY());
                newMirror.create();
                mirrors.add(newMirror);
                newMirror.scale(mousePos);
            }
        });

        scene.setOnMouseDragged(mouseEvent -> mousePos = new Point2D(mouseEvent.getX(), mouseEvent.getY()));

        scene.setOnMouseReleased(mouseEvent -> {
            isMousePressed = false;
            if (xPressed.getValue()) {
                if (mirrors.get(mirrors.size() - 1) instanceof EllipseMirror ellipseMirror) {
                    ellipseMirror.openObjectEdit();
                }
            }
            if (zPressed.getValue()) {
                if (!mirrors.isEmpty() && mirrors.get(mirrors.size() - 1) instanceof LineMirror lineMirror) {
                    lineMirror.openObjectEdit();
                }
            }
        });

        scene.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode().toString().equals("E")) {
                // If mode was switched during an edit, finish the edit
                if (xPressed.getValue()) {
                    xPressed.setValueAndCloseEdit(false);
                }
                else if (zPressed.getValue()) {
                    zPressed.setValueAndCloseEdit(false);
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
                xPressed.setValueAndCloseEdit(!xPressed.getValue());
                zPressed.setValueAndCloseEdit(false);
            }
            else if (keyEvent.getCode().toString().equals("Z") && isEditMode) {
                zPressed.setValueAndCloseEdit(!zPressed.getValue());
                xPressed.setValueAndCloseEdit(false);
            }
            else if (keyEvent.getCode().toString().equals("C") && isEditMode) {
                rays.get(0).setStartX(mousePos.getX());
                rays.get(0).setStartY(mousePos.getY());

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
        });

        scene.setOnMouseMoved(mouseEvent -> {
            if (!isEditMode) return;

            mousePos = new Point2D(mouseEvent.getX(), mouseEvent.getY());
        });
    }
}