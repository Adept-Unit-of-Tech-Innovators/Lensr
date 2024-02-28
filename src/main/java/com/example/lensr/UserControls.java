package com.example.lensr;

import com.example.lensr.objects.*;
import com.example.lensr.objects.glass.Prism;
import com.example.lensr.objects.glass.SphericalLens;
import com.example.lensr.objects.lightsources.RaySource;
import com.example.lensr.objects.lightsources.BeamSource;
import com.example.lensr.objects.lightsources.PointSource;
import com.example.lensr.objects.mirrors.ArcMirror;
import com.example.lensr.objects.mirrors.EllipseMirror;
import com.example.lensr.objects.mirrors.FunnyMirror;
import com.example.lensr.objects.mirrors.LineMirror;
import com.example.lensr.objects.misc.BrickwallFilter;
import com.example.lensr.objects.misc.GaussianRolloffFilter;
import com.example.lensr.objects.misc.LightEater;
import com.example.lensr.objects.misc.LightSensor;
import com.example.lensr.saveloadkit.Actions;
import com.example.lensr.saveloadkit.SaveState;
import com.example.lensr.ui.EditPoint;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Group;

import java.util.ArrayList;
import java.util.List;

import static com.example.lensr.LensrStart.*;

public class UserControls {
    public static void setUserControls() {
        scene.setOnMousePressed(mouseEvent -> {
            if (mouseEvent.isSecondaryButtonDown()) {
                keyPressed = Key.None;
                return;
            }
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
            if (keyEvent.getCode().toString().equals("SHIFT") && isEditMode) {
                shiftPressed = true;
            }
            if (keyEvent.getCode().toString().equals("ALT") && isEditMode) {
                altPressed = true;
            }
            if (keyEvent.getCode().toString().equals("DELETE") && isEditMode) {
                deleteCurrentObject();
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

            // Control shortcuts
            if (keyEvent.getCode().toString().equals("D") && keyEvent.isControlDown() && isEditMode) {
                copyCurrentObject();
            }

            if (keyEvent.getCode().toString().equals("N") && keyEvent.isControlDown() && isEditMode) {
                Actions.clear();
                return;
            }

            if (keyEvent.getCode().toString().equals("Z") && keyEvent.isControlDown() && !keyEvent.isShiftDown() && isEditMode) {
                Actions.undo();
                return;
            }

            if (keyEvent.getCode().toString().equals("Y") && keyEvent.isControlDown() && isEditMode || keyEvent.getCode().toString().equals("Z") && keyEvent.isControlDown() && keyEvent.isShiftDown() && isEditMode) {
                Actions.redo();
                return;
            }

            if (keyEvent.getCode().toString().equals("S") && keyEvent.isControlDown() && isEditMode) {
                if (Actions.lastSave == null) {
                    Actions.exportProject();
                }
                else {
                    SaveState.saveProject(Actions.lastSave.getAbsolutePath());
                }
                return;
            }

            if (keyEvent.getCode().toString().equals("O") && keyEvent.isControlDown() && isEditMode) {
                Actions.importProject();
                return;
            }

            // Object shortcuts
            if (keyEvent.getCode().toString().equals("Q") && isEditMode) {
                if (keyPressed == Key.Q) {
                    keyPressed = Key.None;
                } else {
                    keyPressed = Key.Q;
                }
                closeCurrentEdit();
            }
            else if (keyEvent.getCode().toString().equals("W") && isEditMode) {
                if (keyPressed == Key.W) {
                    keyPressed = Key.None;
                } else {
                    keyPressed = Key.W;
                }
                closeCurrentEdit();
            }
            else if (keyEvent.getCode().toString().equals("E") && isEditMode) {
                if (keyPressed == Key.E) {
                    keyPressed = Key.None;
                } else {
                    keyPressed = Key.E;
                }
                closeCurrentEdit();
            }
            else if (keyEvent.getCode().toString().equals("R") && isEditMode) {
                if (keyPressed == Key.R) {
                    keyPressed = Key.None;
                } else {
                    keyPressed = Key.R;
                }
                closeCurrentEdit();
            }
            else if (keyEvent.getCode().toString().equals("A") && isEditMode) {
                if (keyPressed == Key.A) {
                    keyPressed = Key.None;
                } else {
                    keyPressed = Key.A;
                }
                closeCurrentEdit();
            }
            else if (keyEvent.getCode().toString().equals("S") && isEditMode) {
                if (keyPressed == Key.S) {
                    keyPressed = Key.None;
                } else {
                    keyPressed = Key.S;
                }
                closeCurrentEdit();
            }
            else if (keyEvent.getCode().toString().equals("D") && isEditMode) {
                if (keyPressed == Key.D) {
                    keyPressed = Key.None;
                } else {
                    keyPressed = Key.D;
                }
                closeCurrentEdit();
            }
            else if (keyEvent.getCode().toString().equals("F") && isEditMode) {
                if (keyPressed == Key.F) {
                    keyPressed = Key.None;
                }
                else {
                    keyPressed = Key.F;
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
            else if (keyEvent.getCode().toString().equals("X") && isEditMode) {
                if (keyPressed == Key.X) {
                    keyPressed = Key.None;
                } else {
                    keyPressed = Key.X;
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
            else if (keyEvent.getCode().toString().equals("V") && isEditMode) {
                if (keyPressed == Key.V) {
                    keyPressed = Key.None;
                } else {
                    keyPressed = Key.V;
                }
                closeCurrentEdit();
            }
            else if (keyEvent.getCode().toString().equals("L") && isEditMode) {
                if (keyPressed == Key.L) {
                    keyPressed = Key.None;
                } else {
                    keyPressed = Key.L;
                }
                closeCurrentEdit();
            }
            else if (keyEvent.getCode().toString().equals("P") && isEditMode) {
                if (keyPressed == Key.P) {
                    keyPressed = Key.None;
                } else {
                    keyPressed = Key.P;
                }
                closeCurrentEdit();
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
            case Q:
                RaySource raySource = new RaySource(mousePos.getX(), mousePos.getY());
                raySource.create();
                raySource.openObjectEdit();
                raySource.rotate();
                lightSources.add(raySource);
                break;
            case W:
                BeamSource beamSource = new BeamSource(mousePos.getX(), mousePos.getY(), mousePos.getX() + 100, mousePos.getY() + 100);
                beamSource.create();
                beamSource.openObjectEdit();
                beamSource.scale(mousePos);
                lightSources.add(beamSource);
                break;
            case E:
                PointSource fullPointSource = new PointSource(mousePos.getX(), mousePos.getY(), 2 * Math.PI, 0, 6, true);
                fullPointSource.create();
                fullPointSource.openObjectEdit();
                fullPointSource.rotate();
                lightSources.add(fullPointSource);
                break;
            case R:
                PointSource pointSource = new PointSource(mousePos.getX(), mousePos.getY(), Math.PI/2, 0, 6, false);
                pointSource.create();
                pointSource.openObjectEdit();
                pointSource.rotate();
                lightSources.add(pointSource);
                break;
            case A:
                LineMirror lineMirror = new LineMirror(mousePos.getX(), mousePos.getY(), mousePos.getX(), mousePos.getY());
                lineMirror.create();
                lineMirror.openObjectEdit();
                lineMirror.scale(mousePos);
                mirrors.add(lineMirror);
                break;
            case S:
                EllipseMirror ellipseMirror = new EllipseMirror(mousePos.getX(), mousePos.getY(), 0, 0);
                ellipseMirror.create();
                ellipseMirror.openObjectEdit();
                ellipseMirror.scale(mousePos);
                mirrors.add(ellipseMirror);
                break;
            case D:
                ArcMirror arcMirror = new ArcMirror(mousePos.getX(), mousePos.getY());
                arcMirror.create();
                arcMirror.openObjectEdit();
                arcMirror.scale(arcMirror.objectEditPoints.get(1).getCenter(), arcMirror.objectEditPoints.get(0), arcMirror.objectEditPoints.get(1));
                mirrors.add(arcMirror);
                break;
            case F:
                FunnyMirror funnyMirror = new FunnyMirror();
                funnyMirror.openObjectEdit();
                funnyMirror.draw();
                mirrors.add(funnyMirror);
                break;
            case L:
                SphericalLens sphericalLens = new SphericalLens(50, 50, mousePos.getX(), mousePos.getY(),1.5, 0.004, new Point2D(mousePos.getX()-50, mousePos.getY()), new Point2D(mousePos.getX()+50, mousePos.getY()));
                sphericalLens.create();
                sphericalLens.openObjectEdit();
                sphericalLens.scale(mousePos);
                lenses.add(sphericalLens);
                break;
            case P:
                Prism prism = new Prism(mousePos.getX(), mousePos.getY(),  1.5, 0.004);
                prism.create();
                prism.openObjectEdit();
                prism.draw();
                lenses.add(prism);
                break;
            case Z:
                GaussianRolloffFilter gaussianRolloffFilter = new GaussianRolloffFilter(mousePos.getX(), mousePos.getY(), mousePos.getX(), mousePos.getY());
                gaussianRolloffFilter.create();
                gaussianRolloffFilter.openObjectEdit();
                gaussianRolloffFilter.scale(mousePos);
                mirrors.add(gaussianRolloffFilter);
                break;
            case X:
                BrickwallFilter brickwallFilter = new BrickwallFilter(mousePos.getX(), mousePos.getY(), mousePos.getX(), mousePos.getY());
                brickwallFilter.create();
                brickwallFilter.openObjectEdit();
                brickwallFilter.scale(mousePos);
                mirrors.add(brickwallFilter);
                break;
            case C:
                LightSensor lightSensor = new LightSensor(mousePos.getX(), mousePos.getY(), mousePos.getX(), mousePos.getY());
                lightSensor.create();
                lightSensor.openObjectEdit();
                lightSensor.scale(mousePos);
                mirrors.add(lightSensor);
                break;
            case V:
                LightEater lightEater = new LightEater(mousePos.getX(), mousePos.getY(), 0);
                lightEater.create();
                lightEater.openObjectEdit();
                lightEater.scale(mousePos);
                mirrors.add(lightEater);
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

    public static void deleteCurrentObject() {
        if (editedShape instanceof Group group) {
            group.getChildren().stream()
                    .filter(node -> node instanceof Editable)
                    .map(node -> (Editable) node)
                    .findFirst()
                    .ifPresent(editable -> {
                        editable.delete();
                        updateLightSources();
                    });
        }
        SaveState.autoSave();
    }

    public static void copyCurrentObject() {
        if (editedShape instanceof Group group) {
            group.getChildren().stream()
                    .filter(node -> node instanceof Editable)
                    .map(node -> (Editable) node)
                    .findFirst()
                    .ifPresent(Editable::copy);
        }
        SaveState.autoSave();
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