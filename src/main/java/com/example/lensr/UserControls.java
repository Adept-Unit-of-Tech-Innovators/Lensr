package com.example.lensr;

import com.example.lensr.objects.*;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.control.Slider;
import javafx.scene.shape.Shape;

import java.util.ArrayList;
import java.util.List;

import static com.example.lensr.LensrStart.*;
import static com.example.lensr.ParameterSlider.*;
import static com.example.lensr.ParameterToggle.*;

public class UserControls {
    public static void setUserControls() {
        scene.setOnMousePressed(mouseEvent -> {
            if (!isEditMode) return;

            isMousePressed = true;
            mousePos = new Point2D(mouseEvent.getX(), mouseEvent.getY());
            mouseHitbox.setX(mousePos.getX() - mouseHitbox.getWidth() / 2);
            mouseHitbox.setY(mousePos.getY() - mouseHitbox.getHeight() / 2);

            if (editedShape == null && keyPressed != Key.None) {
                // If no object is being edited, place a new object and reset the hasBeenClicked variable
                placeNewObject();
                resetHasBeenClicked();
                return;
            }

            // Get all clickable objects
            // Priority is given to edit points, then light sources, then other objects
            List<Object> clickableObjects = new ArrayList<>();
            clickableObjects.addAll(editPoints);
            clickableObjects.addAll(lightSources);
            clickableObjects.addAll(mirrors);

            for (Object clickableObject : clickableObjects) {
                if (clickableObject instanceof EditPoint editPoint && !editPoint.hasBeenClicked && editPoint.intersects(mouseHitbox.getLayoutBounds())) {
                    editPoint.handleMousePressed();
                    return;
                }
                else if (clickableObject instanceof BeamSource beamSource && !beamSource.hasBeenClicked && beamSource.intersects(mouseHitbox.getLayoutBounds())) {
                    closeCurrentEdit();
                    beamSource.openObjectEdit();
                    return;
                }
                else if (clickableObject instanceof LineMirror lineMirror && !lineMirror.hasBeenClicked && lineMirror.isMouseOnHitbox()) {
                    closeCurrentEdit();
                    lineMirror.openObjectEdit();
                    return;
                }
                else if (clickableObject instanceof EllipseMirror ellipseMirror && !ellipseMirror.hasBeenClicked && Shape.intersect(ellipseMirror, mouseHitbox).getBoundsInLocal().getWidth() != -1) {
                    closeCurrentEdit();
                    ellipseMirror.openObjectEdit();
                    return;
                }
                else if (clickableObject instanceof FunnyMirror funnyMirror && !funnyMirror.hasBeenClicked && Shape.intersect(funnyMirror, mouseHitbox).getBoundsInLocal().getWidth() != -1) {
                    closeCurrentEdit();
                    funnyMirror.openObjectEdit();
                    return;
                }
                else if (clickableObject instanceof LightEater lightEater && !lightEater.hasBeenClicked && Shape.intersect(lightEater, mouseHitbox).getBoundsInLocal().getWidth() != -1) {
                    closeCurrentEdit();
                    lightEater.openObjectEdit();
                    return;
                }
                else if (clickableObject instanceof GaussianRolloffFilter filter && !filter.hasBeenClicked && filter.isMouseOnHitbox()) {
                    closeCurrentEdit();
                    filter.openObjectEdit();
                    return;
                }
                else if (clickableObject instanceof BrickwallFilter filter && !filter.hasBeenClicked && filter.isMouseOnHitbox()) {
                    closeCurrentEdit();
                    filter.openObjectEdit();
                    return;
                }
            }

            // If no clickable object was found, close the current edit and reset the hasBeenClicked variable
            closeCurrentEdit();
            resetHasBeenClicked();
        });

        scene.setOnMouseDragged(mouseEvent -> {
            mousePos = new Point2D(mouseEvent.getX(), mouseEvent.getY());
            mouseHitbox.setX(mousePos.getX() - mouseHitbox.getWidth() / 2);
            mouseHitbox.setY(mousePos.getY() - mouseHitbox.getHeight() / 2);

            // If mouse is on an edit point, change cursor to hand
            editPoints.stream()
                    .filter(editPoint -> editPoint.intersects(mouseHitbox.getLayoutBounds()))
                    .findFirst()
                    .ifPresentOrElse(editPoint -> scene.setCursor(Cursor.HAND), () -> scene.setCursor(Cursor.DEFAULT));
        });

        scene.setOnMouseReleased(mouseEvent -> {
            isMousePressed = false;
            if (editedShape == null) return;

            switch (keyPressed) {
                case X:
                    if (editedShape instanceof Group group && group.getChildren().get(0) instanceof EllipseMirror mirror && !mirror.isEdited) {
                        mirror.openObjectEdit();
                    }
                case Z:
                    if (editedShape instanceof Group group && group.getChildren().get(0) instanceof LineMirror mirror && !mirror.isEdited) {
                        mirror.openObjectEdit();
                    }
                case V:
                    if (editedShape instanceof Group group && group.getChildren().get(0) instanceof FunnyMirror mirror && !mirror.isEdited) {
                        mirror.openObjectEdit();
                    }
                case B:
                    if (editedShape instanceof Group group && group.getChildren().get(0) instanceof LightEater mirror && !mirror.isEdited) {
                        mirror.openObjectEdit();
                    }
                case N:
                    if (editedShape instanceof Group group && group.getChildren().get(0) instanceof GaussianRolloffFilter mirror && !mirror.isEdited) {
                        mirror.openObjectEdit();
                    }
                case M:
                    if (editedShape instanceof Group group && group.getChildren().get(0) instanceof BrickwallFilter filter && !filter.isEdited) {
                        filter.openObjectEdit();
                    }
                case C:
                    if (editedShape instanceof Group group && group.getChildren().get(0) instanceof BeamSource beamSource && !beamSource.isEdited) {
                        beamSource.openObjectEdit();
                    }
            }
            scene.setCursor(Cursor.DEFAULT);
        });

        scene.setOnKeyPressed(keyEvent -> {


            if (keyEvent.getCode().toString().equals("E")) {
                // If mode was switched during an edit, finish the edit

                keyPressed = Key.None;

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
                closeCurrentEdit();
            }
            else if (keyEvent.getCode().toString().equals("Z") && isEditMode) {
                if (keyPressed == Key.Z) {
                    keyPressed = Key.None;
                } else {
                    keyPressed = Key.Z;
                }
                closeCurrentEdit();
            }
            else if (keyEvent.getCode().toString().equals("V") && isEditMode) {
                if (keyPressed == Key.V) {
                    keyPressed = Key.None;
                } else {
                    keyPressed = Key.V;
                }
                closeCurrentEdit();
            }
            else if (keyEvent.getCode().toString().equals("B") && isEditMode) {
                if (keyPressed == Key.B) {
                    keyPressed = Key.None;
                } else {
                    keyPressed = Key.B;
                }
                closeCurrentEdit();
            }
            else if (keyEvent.getCode().toString().equals("N") && isEditMode) {
                if (keyPressed == Key.N) {
                    keyPressed = Key.None;
                } else {
                    keyPressed = Key.N;
                }
                closeCurrentEdit();
            }
            else if (keyEvent.getCode().toString().equals("M") && isEditMode) {
                if (keyPressed == Key.M) {
                    keyPressed = Key.None;
                } else {
                    keyPressed = Key.M;
                }
                closeCurrentEdit();
            }
            else if (keyEvent.getCode().toString().equals("C") && isEditMode) {
                if (keyPressed == Key.C) {
                    keyPressed = Key.None;
                } else {
                    keyPressed = Key.C;
                }
                closeCurrentEdit();
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
            mouseHitbox.setX(mousePos.getX() - mouseHitbox.getWidth() / 2);
            mouseHitbox.setY(mousePos.getY() - mouseHitbox.getHeight() / 2);

            // If mouse is on an edit point, change cursor to hand
            editPoints.stream()
                    .filter(editPoint -> editPoint.intersects(mouseHitbox.getLayoutBounds()))
                    .findFirst()
                    .ifPresentOrElse(editPoint -> scene.setCursor(Cursor.HAND), () -> scene.setCursor(Cursor.DEFAULT));
        });
    }


    public static void placeNewObject() {
        // Place objects
        switch (keyPressed) {
            case X:
                EllipseMirror ellipseMirror = new EllipseMirror(mousePos.getX(), mousePos.getY(), 0, 0);
                ellipseMirror.create();
                if (mirrors.stream().noneMatch(mirror -> mirror instanceof Slider)) {
                    reflectivitySlider = new ParameterSlider(ellipseMirror, ValueToChange.Reflectivity, SliderStyle.Primary);
                }
                ellipseMirror.scale(mousePos);
                mirrors.add(ellipseMirror);
                editedShape = ellipseMirror.group;
                break;
            case Z:
                LineMirror lineMirror = new LineMirror(mousePos.getX(), mousePos.getY(), mousePos.getX(), mousePos.getY());
                lineMirror.create();
                if (mirrors.stream().noneMatch(mirror -> mirror instanceof Slider)) {
                    reflectivitySlider = new ParameterSlider(lineMirror, ValueToChange.Reflectivity, SliderStyle.Primary);
                }
                lineMirror.scale(mousePos);
                mirrors.add(lineMirror);
                editedShape = lineMirror.group;
                break;
            case V:
                FunnyMirror funnyMirror = new FunnyMirror();
                funnyMirror.draw();
                if (mirrors.stream().noneMatch(mirror -> mirror instanceof Slider)) {
                    reflectivitySlider = new ParameterSlider(funnyMirror, ValueToChange.Reflectivity, SliderStyle.Primary);
                }
                mirrors.add(funnyMirror);
                editedShape = funnyMirror.group;
                break;
            case B:
                LightEater lightEater = new LightEater(mousePos.getX(), mousePos.getY(), 0);
                lightEater.create();
                lightEater.scale(mousePos);
                mirrors.add(lightEater);
                editedShape = lightEater.group;
                break;
            case N:
                GaussianRolloffFilter gaussianRolloffFilter = new GaussianRolloffFilter(mousePos.getX(), mousePos.getY(), mousePos.getX(), mousePos.getY());
                gaussianRolloffFilter.create();
                if (mirrors.stream().noneMatch(mirror -> mirror instanceof Slider)) {
                    peakTransmissionSlider = new ParameterSlider(gaussianRolloffFilter, ValueToChange.Transmission, SliderStyle.Primary);
                    passbandSlider = new ParameterSlider(gaussianRolloffFilter, ValueToChange.Passband, SliderStyle.Secondary);
                    FWHMSlider = new ParameterSlider(gaussianRolloffFilter, ValueToChange.FWHM, SliderStyle.Tertiary);
                }
                gaussianRolloffFilter.scale(mousePos);
                mirrors.add(gaussianRolloffFilter);
                editedShape = gaussianRolloffFilter.group;
                break;
            case M:
                BrickwallFilter brickwallFilter = new BrickwallFilter(mousePos.getX(), mousePos.getY(), mousePos.getX(), mousePos.getY());
                brickwallFilter.create();
                if (mirrors.stream().noneMatch(mirror -> mirror instanceof Slider)) {
                    peakTransmissionSlider = new ParameterSlider(brickwallFilter, ValueToChange.PeakTransmission, SliderStyle.Primary);
                    startPassbandSlider = new ParameterSlider(brickwallFilter, ValueToChange.StartPassband, SliderStyle.Secondary);
                    endPassbandSlider = new ParameterSlider(brickwallFilter, ValueToChange.EndPassband, SliderStyle.Tertiary);
                }
                brickwallFilter.scale(mousePos);
                mirrors.add(brickwallFilter);
                editedShape = brickwallFilter.group;
                break;
            case C:
                BeamSource beamSource = new BeamSource(mousePos.getX(), mousePos.getY());
                beamSource.create();
                if (lightSources.isEmpty()) {
                    wavelengthSlider = new ParameterSlider(beamSource, ValueToChange.Wavelength, SliderStyle.Primary);
                    whiteLightToggle = new ParameterToggle(beamSource, ParameterToChange.WhiteLight);
                }
                beamSource.openObjectEdit();
                lightSources.add(beamSource);
                editedShape = beamSource.group;
                break;
        }
    }


    public static void closeCurrentEdit() {
        // TODO: Implement an interface "Editable" for all objects that can be edited
        // TODO: This interface should have a method "closeObjectEdit()" that closes the edit and "openObjectEdit()" that opens it
        // TODO: This would make the code much more compact and readable
        if (editedShape instanceof Group group) {
            group.getChildren().stream()
                    .filter(node -> node instanceof LineMirror)
                    .map(node -> (LineMirror) node)
                    .findFirst()
                    .ifPresent(LineMirror::closeObjectEdit);

            group.getChildren().stream()
                    .filter(node -> node instanceof EllipseMirror)
                    .map(node -> (EllipseMirror) node)
                    .findFirst()
                    .ifPresent(EllipseMirror::closeObjectEdit);

            group.getChildren().stream()
                    .filter(node -> node instanceof FunnyMirror)
                    .map(node -> (FunnyMirror) node)
                    .findFirst()
                    .ifPresent(FunnyMirror::closeObjectEdit);

            group.getChildren().stream()
                    .filter(node -> node instanceof LightEater)
                    .map(node -> (LightEater) node)
                    .findFirst()
                    .ifPresent(LightEater::closeObjectEdit);

            group.getChildren().stream()
                    .filter(node -> node instanceof GaussianRolloffFilter)
                    .map(node -> (GaussianRolloffFilter) node)
                    .findFirst()
                    .ifPresent(GaussianRolloffFilter::closeObjectEdit);

            group.getChildren().stream()
                    .filter(node -> node instanceof BrickwallFilter)
                    .map(node -> (BrickwallFilter) node)
                    .findFirst()
                    .ifPresent(BrickwallFilter::closeObjectEdit);

            group.getChildren().stream()
                    .filter(node -> node instanceof BeamSource)
                    .map(node -> (BeamSource) node)
                    .findFirst().ifPresent(BeamSource::closeObjectEdit);


            group.getChildren().stream()
                    .filter(node -> node instanceof BeamSource)
                    .map(node -> (BeamSource) node)
                    .findFirst().ifPresent(BeamSource::closeObjectEdit);
        }
    }


    public static void resetHasBeenClicked() {
        List<Object> clickableObjects = new ArrayList<>();
        clickableObjects.addAll(editPoints);
        clickableObjects.addAll(lightSources);
        clickableObjects.addAll(mirrors);

        for (Object clickableObject : clickableObjects) {
            if (clickableObject instanceof EditPoint editPoint) {
                editPoint.hasBeenClicked = false;
            }
            else if (clickableObject instanceof BeamSource beamSource) {
                beamSource.hasBeenClicked = false;
            }
            else if (clickableObject instanceof LineMirror lineMirror) {
                lineMirror.hasBeenClicked = false;
            }
            else if (clickableObject instanceof EllipseMirror ellipseMirror) {
                ellipseMirror.hasBeenClicked = false;
            }
            else if (clickableObject instanceof FunnyMirror funnyMirror) {
                funnyMirror.hasBeenClicked = false;
            }
            else if (clickableObject instanceof LightEater lightEater) {
                lightEater.hasBeenClicked = false;
            }
            else if (clickableObject instanceof GaussianRolloffFilter filter) {
                filter.hasBeenClicked = false;
            }
            else if (clickableObject instanceof BrickwallFilter filter) {
                filter.hasBeenClicked = false;
            }
        }
    }
}