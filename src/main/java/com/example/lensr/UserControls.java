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
                rays.forEach(Ray::update);
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
                rays.forEach(Ray::update);
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
                LineMirror newMirror = new LineMirror(mousePos.getX(), mousePos.getY(),mousePos.getX(), mousePos.getY());
                newMirror.create();
                mirrors.add(newMirror);
                newMirror.scale(mousePos);
            }
            if (vPressed.getValue()) {
                FunnyMirror newMirror = new FunnyMirror();
                newMirror.draw();
                mirrors.add(newMirror);
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
                zPressed.setValue(false);
                vPressed.setValue(false);
            }
            else if (keyEvent.getCode().toString().equals("Z") && isEditMode) {
                zPressed.setValueAndCloseEdit(!zPressed.getValue());
                xPressed.setValue(false);
                vPressed.setValue(false);
            }
            else if (keyEvent.getCode().toString().equals("V") && isEditMode) {
                vPressed.setValueAndCloseEdit(!vPressed.getValue());
                zPressed.setValue(false);
                xPressed.setValue(false);
            }
            else if (keyEvent.getCode().toString().equals("C") && isEditMode) {
                Ray ray = new Ray(mousePos.getX(), mousePos.getY(), SIZE, mousePos.getY());
                ray.create();

                // Recalculate ray intersections after it position changed
                scene.getOnMouseMoved();
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
            mousePos = new Point2D(mouseEvent.getX(), mouseEvent.getY());
        });
    }
}