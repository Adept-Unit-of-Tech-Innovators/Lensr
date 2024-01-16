package com.example.lensr;

import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.shape.Shape;

import static com.example.lensr.LensrStart.*;

public class UserControls {

    public static void setUserControls() {
        scene.setOnMousePressed(mouseEvent -> {
            isMousePressed = true;
            mousePos = new Point2D(mouseEvent.getX(), mouseEvent.getY());

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

            // Close funny mirror edit if editing it
            if (editedShape instanceof Group group && group.getChildren().get(0) instanceof FunnyMirror mirror
                    && !mirror.contains(mousePos) && mirror.editPoints.stream().noneMatch(rectangle ->
                    rectangle.contains(mousePos)))
            {
                mirror.closeObjectEdit();
                mirror.isEditPointClicked.setValue(false);
                editedShape = null;
                rays.forEach(Ray::update);
                return;
            }

            // Close ray edit if editing it
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


            // Place objects
            switch (keyPressed) {
                case X:
                    EllipseMirror ellipseMirror = new EllipseMirror(mousePos.getX(), mousePos.getY(), 0, 0);
                    ellipseMirror.create();
                    mirrors.add(ellipseMirror);
                    ellipseMirror.scale(mousePos);
                    return;
                case Z:
                    LineMirror lineMirror = new LineMirror(mousePos.getX(), mousePos.getY(), mousePos.getX(), mousePos.getY());
                    lineMirror.create();
                    mirrors.add(lineMirror);
                    lineMirror.scale(mousePos);
                    return;
                case V:
                    FunnyMirror funnyMirror = new FunnyMirror();
                    funnyMirror.draw();
                    mirrors.add(funnyMirror);
                case C:
                    Ray ray = new Ray(mousePos.getX(), mousePos.getY(), SIZE, mousePos.getY());
                    ray.create();
                    ray.createLaserPointer();
                    rays.add(ray);
                    for (Ray ray1 : rays) {
                        if (ray1.isEdited) {
                            ray1.closeObjectEdit();
                            ray.simulateRay();
                        }
                    }
            }
        });

        scene.setOnMouseDragged(mouseEvent -> mousePos = new Point2D(mouseEvent.getX(), mouseEvent.getY()));

        scene.setOnMouseReleased(mouseEvent -> {
            isMousePressed = false;
            switch (keyPressed) {
                case X:
                    if (!mirrors.isEmpty() && mirrors.get(mirrors.size() - 1) instanceof EllipseMirror mirror) {
                        mirror.openObjectEdit();

                    }
                case Z:
                    if (!mirrors.isEmpty() && mirrors.get(mirrors.size() - 1) instanceof LineMirror mirror) {
                        mirror.openObjectEdit();
                    }
                case V:
                    if (!mirrors.isEmpty() && mirrors.get(mirrors.size() - 1) instanceof FunnyMirror mirror) {
                        mirror.openObjectEdit();
                    }
                case C:
                    if (!rays.isEmpty()) {
                        rays.get(rays.size() - 1).openObjectEdit();
                    }
            }
        });

        scene.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode().toString().equals("E")) {
                // If mode was switched during an edit, finish the edit

                keyPressed = Key.None;
                for (Object mirror : LensrStart.mirrors) {
                    if (mirror instanceof LineMirror lineMirror) {
                        lineMirror.closeObjectEdit();
                    }
                    if (mirror instanceof EllipseMirror ellipseMirror) {
                        ellipseMirror.closeObjectEdit();
                    }
                    if (mirror instanceof FunnyMirror funnyMirror) {
                        funnyMirror.closeObjectEdit();
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
                keyPressed = Key.X;
                MirrorMethods.closeMirrorsEdit();
            }
            else if (keyEvent.getCode().toString().equals("Z") && isEditMode) {
                keyPressed = Key.Z;
                MirrorMethods.closeMirrorsEdit();
            }
            else if (keyEvent.getCode().toString().equals("V") && isEditMode) {
                keyPressed = Key.V;
                MirrorMethods.closeMirrorsEdit();
            }
            else if (keyEvent.getCode().toString().equals("C") && isEditMode) {
                keyPressed = Key.C;
                MirrorMethods.closeMirrorsEdit();
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