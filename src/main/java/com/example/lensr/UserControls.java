package com.example.lensr;

import com.example.lensr.objects.*;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.control.Slider;

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
            clickableObjects.addAll(lenses);

            for (Object clickableObject : clickableObjects) {
                if (clickableObject instanceof EditPoint editPoint && !editPoint.hasBeenClicked && editPoint.intersects(mouseHitbox.getLayoutBounds())) {
                    editPoint.handleMousePressed();
                    return;
                }
                else if (clickableObject instanceof Editable editable && !editable.getHasBeenClicked() && editable.intersectsMouseHitbox()) {
                    closeCurrentEdit();
                    editable.openObjectEdit();
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
            if (keyEvent.getCode().toString().equals("DELETE") && isEditMode) {
                if (editedShape instanceof Group group) {
                    group.getChildren().stream()
                            .filter(node -> node instanceof Editable)
                            .map(node -> (Editable) node)
                            .findFirst()
                            .ifPresent(editable -> {
                                editable.closeObjectEdit();
                                editable.delete();
                            });
                }
            }
            if (( keyEvent.getCode().toString().equals("ESCAPE") || keyEvent.getCode().toString().equals("ENTER") ) && isEditMode) {
                closeCurrentEdit();
            }
            if (keyEvent.getCode().toString().equals("UP") && isEditMode) {
                if (editedShape instanceof Group group) {
                    group.getChildren().stream()
                            .filter(node -> node instanceof Editable)
                            .map(node -> (Editable) node)
                            .findFirst()
                            .ifPresent(editable -> {
                                if (shiftPressed) editable.moveBy(0, -1);
                                else editable.moveBy(0, -10);
                            });
                }
            }
            if (keyEvent.getCode().toString().equals("DOWN") && isEditMode) {
                if (editedShape instanceof Group group) {
                    group.getChildren().stream()
                            .filter(node -> node instanceof Editable)
                            .map(node -> (Editable) node)
                            .findFirst()
                            .ifPresent(editable -> {
                                if (shiftPressed) editable.moveBy(0, 1);
                                else editable.moveBy(0, 10);
                            });
                }
            }
            if (keyEvent.getCode().toString().equals("LEFT") && isEditMode) {
                if (editedShape instanceof Group group) {
                    group.getChildren().stream()
                            .filter(node -> node instanceof Editable)
                            .map(node -> (Editable) node)
                            .findFirst()
                            .ifPresent(editable -> {
                                if (shiftPressed) editable.moveBy(-1, 0);
                                else editable.moveBy(-10, 0);
                            });
                }
            }
            if (keyEvent.getCode().toString().equals("RIGHT") && isEditMode) {
                if (editedShape instanceof Group group) {
                    group.getChildren().stream()
                            .filter(node -> node instanceof Editable)
                            .map(node -> (Editable) node)
                            .findFirst()
                            .ifPresent(editable -> {
                                if (shiftPressed) editable.moveBy(1, 0);
                                else editable.moveBy(10, 0);
                            });
                }
            }

            if (keyEvent.getCode().toString().equals("D") && keyEvent.isControlDown() && isEditMode) {
                if (editedShape instanceof Group group) {
                    group.getChildren().stream()
                            .filter(node -> node instanceof Editable)
                            .map(node -> (Editable) node)
                            .findFirst()
                            .ifPresent(Editable::copy);
                }
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
            else if (keyEvent.getCode().toString().equals("K") && isEditMode) {
                if (keyPressed == Key.K) {
                    keyPressed = Key.None;
                }
                else {
                    keyPressed = Key.K;
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
            else if (keyEvent.getCode().toString().equals("J") && isEditMode) {
                if (keyPressed == Key.J) {
                    keyPressed = Key.None;
                } else {
                    keyPressed = Key.J;
                }
                closeCurrentEdit();
            }
            else if(keyEvent.getCode().toString().equals("L") && isEditMode)
            {
                if (keyPressed == Key.L) {
                    keyPressed = Key.None;
                } else {
                    keyPressed = Key.L;
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
                ellipseMirror.openObjectEdit();
                ellipseMirror.scale(mousePos);
                mirrors.add(ellipseMirror);
                break;
            case Z:
                LineMirror lineMirror = new LineMirror(mousePos.getX(), mousePos.getY(), mousePos.getX(), mousePos.getY());
                lineMirror.create();
                if (mirrors.stream().noneMatch(mirror -> mirror instanceof Slider)) {
                    reflectivitySlider = new ParameterSlider(lineMirror, ValueToChange.Reflectivity, SliderStyle.Primary);
                }
                lineMirror.openObjectEdit();
                lineMirror.scale(mousePos);
                mirrors.add(lineMirror);
                break;
            case V:
                FunnyMirror funnyMirror = new FunnyMirror();
                if (mirrors.stream().noneMatch(mirror -> mirror instanceof Slider)) {
                    reflectivitySlider = new ParameterSlider(funnyMirror, ValueToChange.Reflectivity, SliderStyle.Primary);
                }
                funnyMirror.openObjectEdit();
                funnyMirror.draw();
                mirrors.add(funnyMirror);
                break;
            case B:
                LightEater lightEater = new LightEater(mousePos.getX(), mousePos.getY(), 0);
                lightEater.create();
                lightEater.openObjectEdit();
                lightEater.scale(mousePos);
                mirrors.add(lightEater);
                break;
            case N:
                GaussianRolloffFilter gaussianRolloffFilter = new GaussianRolloffFilter(mousePos.getX(), mousePos.getY(), mousePos.getX(), mousePos.getY());
                gaussianRolloffFilter.create();
                if (mirrors.stream().noneMatch(mirror -> mirror instanceof Slider)) {
                    peakTransmissionSlider = new ParameterSlider(gaussianRolloffFilter, ValueToChange.PeakTransmission, SliderStyle.Primary);
                    passbandSlider = new ParameterSlider(gaussianRolloffFilter, ValueToChange.Passband, SliderStyle.Secondary);
                    FWHMSlider = new ParameterSlider(gaussianRolloffFilter, ValueToChange.FWHM, SliderStyle.Tertiary);
                }
                gaussianRolloffFilter.openObjectEdit();
                gaussianRolloffFilter.scale(mousePos);
                mirrors.add(gaussianRolloffFilter);
                break;
            case M:
                BrickwallFilter brickwallFilter = new BrickwallFilter(mousePos.getX(), mousePos.getY(), mousePos.getX(), mousePos.getY());
                brickwallFilter.create();
                if (mirrors.stream().noneMatch(mirror -> mirror instanceof Slider)) {
                    peakTransmissionSlider = new ParameterSlider(brickwallFilter, ValueToChange.Transmission, SliderStyle.Primary);
                    startPassbandSlider = new ParameterSlider(brickwallFilter, ValueToChange.StartPassband, SliderStyle.Secondary);
                    endPassbandSlider = new ParameterSlider(brickwallFilter, ValueToChange.EndPassband, SliderStyle.Tertiary);
                }
                brickwallFilter.openObjectEdit();
                brickwallFilter.scale(mousePos);
                mirrors.add(brickwallFilter);
                break;
            case K:
                LightSensor lightSensor = new LightSensor(mousePos.getX(), mousePos.getY(), mousePos.getX(), mousePos.getY());
                lightSensor.create();
                lightSensor.openObjectEdit();
                lightSensor.scale(mousePos);
                mirrors.add(lightSensor);
                break;
            case C:
                BeamSource beamSource = new BeamSource(mousePos.getX(), mousePos.getY());
                beamSource.create();
                if (lightSources.isEmpty()) {
                    wavelengthSlider = new ParameterSlider(beamSource, ValueToChange.Wavelength, SliderStyle.Primary);
                    whiteLightToggle = new ParameterToggle(beamSource, ParameterToChange.WhiteLight);
                }
                beamSource.openObjectEdit();
                beamSource.rotate();
                lightSources.add(beamSource);
                break;
            case J:
                PanelSource panelSource = new PanelSource(mousePos.getX(), mousePos.getY(), mousePos.getX() + 100, mousePos.getY() + 100);
                panelSource.create();
                if (lightSources.isEmpty()) {
                    wavelengthSlider = new ParameterSlider(panelSource, ValueToChange.Wavelength, SliderStyle.Primary);
                    whiteLightToggle = new ParameterToggle(panelSource, ParameterToChange.WhiteLight);
                }
                panelSource.openObjectEdit();
                panelSource.scale(mousePos);
                lightSources.add(panelSource);
                break;
            case L:
                SphericalLens sphericalLens = new SphericalLens(50, 50, mousePos.getX(), mousePos.getY(), -20, -20,  1.52);
                sphericalLens.create();
                if(lenses.stream().noneMatch(lens -> lens instanceof Slider))
                {
                    refractiveIndexSlider = new ParameterSlider(sphericalLens, ValueToChange.RefractiveIndex, SliderStyle.Primary);
                }
                sphericalLens.openObjectEdit();
                lenses.add(sphericalLens);
                sphericalLens.scale(mousePos);
                editedShape = sphericalLens;
                break;
        }
    }


    public static void closeCurrentEdit() {
        if (editedShape instanceof Editable) {
            ((Editable) editedShape).closeObjectEdit();
        }
        else if (editedShape instanceof Group group) {
            group.getChildren().stream()
                    .filter(node -> node instanceof Editable)
                    .map(node -> (Editable) node)
                    .findFirst()
                    .ifPresent(Editable::closeObjectEdit);
        }
    }


    public static void resetHasBeenClicked() {
        List<Object> clickableObjects = new ArrayList<>();
        clickableObjects.addAll(editPoints);
        clickableObjects.addAll(lightSources);
        clickableObjects.addAll(mirrors);
        clickableObjects.addAll(lenses);

        for (Object clickableObject : clickableObjects) {
            if (clickableObject instanceof EditPoint editPoint) {
                editPoint.hasBeenClicked = false;
            } else if (clickableObject instanceof Editable editable) {
                editable.setHasBeenClicked(false);
            }
        }
    }
}