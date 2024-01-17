package com.example.lensr;

import javafx.geometry.Point2D;
import javafx.scene.Group;

import java.util.Timer;

import static com.example.lensr.LensrStart.*;

public class UserControls {

    public static void setUserControls() {
        scene.setOnMousePressed(mouseEvent -> {
            if (!isEditMode) return;
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

            if (editedShape instanceof Group group && group.getChildren().get(0) instanceof LightEater mirror
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

            if (!mirrors.isEmpty()) {
                for (Object mirror : mirrors) {
                    if (mirror instanceof LineMirror lineMirror && lineMirror.isMouseOnHitbox) {
                        lineMirror.openObjectEdit();
                        return;
                    }
                    if (mirror instanceof  EllipseMirror ellipseMirror && ellipseMirror.contains(mousePos)) {
                        ellipseMirror.openObjectEdit();
                        return;
                    }
                    if (mirror instanceof FunnyMirror funnyMirror && funnyMirror.contains(mousePos)) {
                        funnyMirror.openObjectEdit();
                        return;
                    }
                }
                for (Ray ray : rays) {
                    if (ray.laserPointer.contains(mousePos) && !ray.isEdited) {
                        ray.openObjectEdit();
                    }
                }
            }

            if (editedShape != null) {
                return;
            }

            // Place objects
            switch (keyPressed) {
                case X:
                    EllipseMirror ellipseMirror = new EllipseMirror(mousePos.getX(), mousePos.getY(), 0, 0);
                    ellipseMirror.create();
                    ellipseMirror.scale(mousePos);
                    mirrors.add(ellipseMirror);
                    editedShape = ellipseMirror.group;
                    return;
                case Z:
                    LineMirror lineMirror = new LineMirror(mousePos.getX(), mousePos.getY(), mousePos.getX(), mousePos.getY());
                    lineMirror.create();
                    lineMirror.scale(mousePos);
                    mirrors.add(lineMirror);
                    editedShape = lineMirror.group;
                    return;
                case V:
                    FunnyMirror funnyMirror = new FunnyMirror();
                    funnyMirror.draw();
                    mirrors.add(funnyMirror);
                    editedShape = funnyMirror.group;
                    return;
                case B:
                    LightEater lightEater = new LightEater(mousePos.getX(), mousePos.getY(), 0);
                    lightEater.create();
                    lightEater.scale(mousePos);
                    mirrors.add(lightEater);
                    editedShape = lightEater.group;
                    return;
                case C:
                    Ray ray = new Ray(mousePos.getX(), mousePos.getY(), SIZE, mousePos.getY());
                    ray.create();
                    ray.createLaserPointer();
                    for (Ray ray1 : rays) {
                        if (ray1.isEdited) {
                            ray1.closeObjectEdit();
                            ray.simulateRay();
                        }
                    }
                    if (rays.isEmpty()) {
                        wavelengthSlider = new WavelengthSlider(ray);
                    }
                    rays.add(ray);
                    editedShape = ray.group;
            }
        });

        scene.setOnMouseDragged(mouseEvent -> mousePos = new Point2D(mouseEvent.getX(), mouseEvent.getY()));

        scene.setOnMouseReleased(mouseEvent -> {
            isMousePressed = false;
            if (editedShape == null) return;

            switch (keyPressed) {
                case X:
                    if (!mirrors.isEmpty() && mirrors.get(mirrors.size() - 1) instanceof EllipseMirror mirror && !mirror.isEdited) {
                        mirror.openObjectEdit();
                    }
                case Z:
                    if (!mirrors.isEmpty() && mirrors.get(mirrors.size() - 1) instanceof LineMirror mirror && !mirror.isEdited) {
                        mirror.openObjectEdit();
                    }
                case V:
                    if (!mirrors.isEmpty() && mirrors.get(mirrors.size() - 1) instanceof FunnyMirror mirror && !mirror.isEdited) {
                        mirror.openObjectEdit();
                    }
                case B:
                    if (!mirrors.isEmpty() && mirrors.get(mirrors.size() - 1) instanceof LightEater mirror) {
                        mirror.openObjectEdit();
                    }
                case C:
                    if (!rays.isEmpty() && !rays.get(rays.size() - 1).isEdited) {
                        rays.get(rays.size() - 1).openObjectEdit();
                    }
            }
        });

        scene.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode().toString().equals("E")) {
                // If mode was switched during an edit, finish the edit

                keyPressed = Key.None;
                MirrorMethods.closeMirrorsEdit();

                if (isEditMode) {
                    for (ToolbarButton button : toolbar) {
                        button.disableProperty().setValue(true);
                    }
                }
                else {
                    for (ToolbarButton button : toolbar) {
                        button.disableProperty().setValue(false);
                    }
                }

                isEditMode = !isEditMode;
            }

            if (keyEvent.getCode().toString().equals("SHIFT") && isEditMode) {
                shiftPressed = true;
            }
            if (keyEvent.getCode().toString().equals("ALT") && isEditMode) {
                altPressed = true;
            }
            if (keyEvent.getCode().toString().equals("X") && isEditMode) {
                if (keyPressed == Key.X) {
                    keyPressed = Key.None;
                } else {
                    keyPressed = Key.X;
                }
                MirrorMethods.closeMirrorsEdit();
            }
            else if (keyEvent.getCode().toString().equals("Z") && isEditMode) {
                if (keyPressed == Key.Z) {
                    keyPressed = Key.None;
                } else {
                    keyPressed = Key.Z;
                }
                MirrorMethods.closeMirrorsEdit();
            }
            else if (keyEvent.getCode().toString().equals("V") && isEditMode) {
                if (keyPressed == Key.V) {
                    keyPressed = Key.None;
                } else {
                    keyPressed = Key.V;
                }
                MirrorMethods.closeMirrorsEdit();
            }
            else if (keyEvent.getCode().toString().equals("B") && isEditMode) {
                if (keyPressed == Key.B) {
                    keyPressed = Key.None;
                } else {
                    keyPressed = Key.B;
                }
                MirrorMethods.closeMirrorsEdit();
            }
            else if (keyEvent.getCode().toString().equals("C") && isEditMode) {
                if (keyPressed == Key.C) {
                    keyPressed = Key.None;
                } else {
                    keyPressed = Key.C;
                }
                MirrorMethods.closeMirrorsEdit();
            }
            toolbar.forEach(ToolbarButton::updateRender);
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