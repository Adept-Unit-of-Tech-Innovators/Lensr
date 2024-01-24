package com.example.lensr;

import com.example.lensr.objects.*;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.control.Slider;

import static com.example.lensr.LensrStart.*;
import static com.example.lensr.MirrorMethods.updateLightSources;
import static com.example.lensr.ParameterSlider.*;

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
                updateLightSources();

                return;
            }

            // Close ellipse mirror edit if editing it
            if (editedShape instanceof Group group && group.getChildren().get(0) instanceof EllipseMirror mirror
                    && !group.getLayoutBounds().contains(mousePos))
            {
                mirror.closeObjectEdit();
                mirror.isEditPointClicked.setValue(false);
                editedShape = null;
                updateLightSources();
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
                updateLightSources();
                return;
            }

            // Close light eater edit if editing it
            if (editedShape instanceof Group group && group.getChildren().get(0) instanceof LightEater mirror
                    && !mirror.contains(mousePos) && mirror.editPoints.stream().noneMatch(rectangle ->
                    rectangle.contains(mousePos)))
            {
                mirror.closeObjectEdit();
                mirror.isEditPointClicked.setValue(false);
                editedShape = null;
                updateLightSources();
                return;
            }


            if (editedShape instanceof Group group && group.getChildren().get(0) instanceof GaussianRolloffFilter filter
                    && !filter.contains(mousePos) && filter.editPoints.stream().noneMatch(rectangle ->
                    rectangle.contains(mousePos)))
            {
                filter.closeObjectEdit();
                filter.isEditPointClicked.setValue(false);
                editedShape = null;
                updateLightSources();
                return;
            }

            if (editedShape instanceof Group group && group.getChildren().get(0) instanceof BrickwallFilter filter
                    && !filter.contains(mousePos) && filter.editPoints.stream().noneMatch(rectangle ->
                    rectangle.contains(mousePos)))
            {
                filter.closeObjectEdit();
                filter.isEditPointClicked.setValue(false);
                editedShape = null;
                updateLightSources();
                return;
            }

            // Close ray edit if editing it
            if (editedShape instanceof Group group && group.getChildren().get(1) instanceof BeamSource beamSource
                    && !beamSource.contains(mousePos) && beamSource.editPoints.stream().noneMatch(rectangle ->
                    rectangle.contains(mousePos)))
            {
                beamSource.closeObjectEdit();
                beamSource.isEditPointClicked.setValue(false);
                beamSource.isEdited = false;
                editedShape = null;
                beamSource.update();
                return;
            }

            // Open object edit if clicked
            if (!mirrors.isEmpty()) {
                for (Object mirror : mirrors) {
                    if (mirror instanceof LineMirror lineMirror && lineMirror.isMouseOnHitbox) {
                        lineMirror.openObjectEdit();
                        return;
                    }
                    if (mirror instanceof EllipseMirror ellipseMirror && ellipseMirror.contains(mousePos)) {
                        ellipseMirror.openObjectEdit();
                        return;
                    }
                    if (mirror instanceof FunnyMirror funnyMirror && funnyMirror.contains(mousePos)) {
                        funnyMirror.openObjectEdit();
                        return;
                    }
                    if (mirror instanceof LightEater lightEater && lightEater.contains(mousePos)) {
                        lightEater.openObjectEdit();
                        return;
                    }
                    if (mirror instanceof GaussianRolloffFilter filter && filter.isMouseOnHitbox) {
                        filter.openObjectEdit();
                        return;
                    }
                    if (mirror instanceof BrickwallFilter filter && filter.isMouseOnHitbox) {
                        filter.openObjectEdit();
                        return;
                    }
                }
            }
            // Same for light sources
            if (!lightSources.isEmpty()) {
                for (Object lightSource : lightSources) {
                    if (lightSource instanceof BeamSource beamSource && beamSource.contains(mousePos) && !beamSource.isEdited) {
                        beamSource.openObjectEdit();
                        return;
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
                    }
                    lightSources.add(beamSource);
                    editedShape = beamSource.group;
                    break;
            }
        });

        scene.setOnMouseDragged(mouseEvent -> mousePos = new Point2D(mouseEvent.getX(), mouseEvent.getY()));

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
            else if (keyEvent.getCode().toString().equals("N") && isEditMode) {
                if (keyPressed == Key.N) {
                    keyPressed = Key.None;
                } else {
                    keyPressed = Key.N;
                }
                MirrorMethods.closeMirrorsEdit();
            }
            else if (keyEvent.getCode().toString().equals("M") && isEditMode) {
                if (keyPressed == Key.M) {
                    keyPressed = Key.None;
                } else {
                    keyPressed = Key.M;
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