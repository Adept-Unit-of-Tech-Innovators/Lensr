package com.lensr;

import com.lensr.objects.Editable;
import com.lensr.objects.glass.Prism;
import com.lensr.objects.glass.SphericalLens;
import com.lensr.objects.lightsources.RaySource;
import com.lensr.objects.lightsources.BeamSource;
import com.lensr.objects.lightsources.PointSource;
import com.lensr.objects.mirrors.ArcMirror;
import com.lensr.objects.mirrors.EllipseMirror;
import com.lensr.objects.mirrors.FunnyMirror;
import com.lensr.objects.mirrors.LineMirror;
import com.lensr.objects.misc.BrickwallFilter;
import com.lensr.objects.misc.GaussianRolloffFilter;
import com.lensr.objects.misc.LightEater;
import com.lensr.objects.misc.LightSensor;
import com.lensr.saveloadkit.Actions;
import com.lensr.saveloadkit.SaveState;
import com.lensr.ui.EditPoint;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Group;

import java.util.ArrayList;
import java.util.List;

public class UserControls {
    public static void setUserControls() {
        LensrStart.scene.setOnMousePressed(mouseEvent -> {
            if (mouseEvent.isSecondaryButtonDown()) {
                LensrStart.keyPressed = Key.None;
                return;
            }
            if (!LensrStart.isEditMode) return;

            LensrStart.isMousePressed = true;
            LensrStart.mousePos = new Point2D(mouseEvent.getX(), mouseEvent.getY());
            LensrStart.mouseHitbox.setX(LensrStart.mousePos.getX() - LensrStart.mouseHitbox.getWidth() / 2);
            LensrStart.mouseHitbox.setY(LensrStart.mousePos.getY() - LensrStart.mouseHitbox.getHeight() / 2);

            if (LensrStart.editedShape == null && LensrStart.keyPressed != Key.None) {
                // If no object is being edited, place a new object and reset the hasBeenClicked variable
                placeNewObject();
                resetHasBeenClicked();
                return;
            }

            // Get all clickable objects
            // Priority is given to edit points, then light sources, then other objects
            List<Object> clickableObjects = new ArrayList<>();
            clickableObjects.addAll(LensrStart.editPoints);
            clickableObjects.addAll(LensrStart.lightSources);
            clickableObjects.addAll(LensrStart.mirrors);
            clickableObjects.addAll(LensrStart.lenses);

            for (Object clickableObject : clickableObjects) {
                if (clickableObject instanceof EditPoint editPoint && !editPoint.hasBeenClicked && editPoint.intersects(LensrStart.mouseHitbox.getLayoutBounds())) {
                    editPoint.handleMousePressed();
                    return;
                } else if (clickableObject instanceof Editable editable && !editable.getHasBeenClicked() && editable.intersectsMouseHitbox()) {
                    closeCurrentEdit();
                    editable.openObjectEdit();
                    return;
                }
            }

            // If no clickable object was found, close the current edit and reset the hasBeenClicked variable
            closeCurrentEdit();
            resetHasBeenClicked();
        });

        LensrStart.scene.setOnMouseDragged(mouseEvent -> {
            LensrStart.mousePos = new Point2D(mouseEvent.getX(), mouseEvent.getY());
            LensrStart.mouseHitbox.setX(LensrStart.mousePos.getX() - LensrStart.mouseHitbox.getWidth() / 2);
            LensrStart.mouseHitbox.setY(LensrStart.mousePos.getY() - LensrStart.mouseHitbox.getHeight() / 2);

            // If mouse is on an edit point, change cursor to hand
            LensrStart.editPoints.stream()
                    .filter(editPoint -> editPoint.intersects(LensrStart.mouseHitbox.getLayoutBounds()))
                    .findFirst()
                    .ifPresentOrElse(editPoint -> LensrStart.scene.setCursor(Cursor.HAND), () -> LensrStart.scene.setCursor(Cursor.DEFAULT));
        });

        LensrStart.scene.setOnMouseReleased(mouseEvent -> {
            LensrStart.isMousePressed = false;
            LensrStart.scene.setCursor(Cursor.DEFAULT);
        });

        LensrStart.scene.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode().toString().equals("SHIFT") && LensrStart.isEditMode) {
                LensrStart.shiftPressed = true;
            }
            if (keyEvent.getCode().toString().equals("ALT") && LensrStart.isEditMode) {
                LensrStart.altPressed = true;
            }
            if (keyEvent.getCode().toString().equals("DELETE") && LensrStart.isEditMode) {
                deleteCurrentObject();
            }
            if ((keyEvent.getCode().toString().equals("ESCAPE") || keyEvent.getCode().toString().equals("ENTER")) && LensrStart.isEditMode) {
                closeCurrentEdit();
            }
            if (keyEvent.getCode().toString().equals("UP") && LensrStart.isEditMode) {
                if (LensrStart.editedShape instanceof Group group) {
                    group.getChildren().stream()
                            .filter(node -> node instanceof Editable)
                            .map(node -> (Editable) node)
                            .findFirst()
                            .ifPresent(editable -> {
                                if (LensrStart.shiftPressed) editable.moveBy(0, -1);
                                else editable.moveBy(0, -10);
                            });
                }
            }
            if (keyEvent.getCode().toString().equals("DOWN") && LensrStart.isEditMode) {
                if (LensrStart.editedShape instanceof Group group) {
                    group.getChildren().stream()
                            .filter(node -> node instanceof Editable)
                            .map(node -> (Editable) node)
                            .findFirst()
                            .ifPresent(editable -> {
                                if (LensrStart.shiftPressed) editable.moveBy(0, 1);
                                else editable.moveBy(0, 10);
                            });
                }
            }
            if (keyEvent.getCode().toString().equals("LEFT") && LensrStart.isEditMode) {
                if (LensrStart.editedShape instanceof Group group) {
                    group.getChildren().stream()
                            .filter(node -> node instanceof Editable)
                            .map(node -> (Editable) node)
                            .findFirst()
                            .ifPresent(editable -> {
                                if (LensrStart.shiftPressed) editable.moveBy(-1, 0);
                                else editable.moveBy(-10, 0);
                            });
                }
            }
            if (keyEvent.getCode().toString().equals("RIGHT") && LensrStart.isEditMode) {
                if (LensrStart.editedShape instanceof Group group) {
                    group.getChildren().stream()
                            .filter(node -> node instanceof Editable)
                            .map(node -> (Editable) node)
                            .findFirst()
                            .ifPresent(editable -> {
                                if (LensrStart.shiftPressed) editable.moveBy(1, 0);
                                else editable.moveBy(10, 0);
                            });
                }
            }

            // Control shortcuts
            if (keyEvent.getCode().toString().equals("D") && keyEvent.isControlDown() && LensrStart.isEditMode) {
                copyCurrentObject();
            }

            if (keyEvent.getCode().toString().equals("N") && keyEvent.isControlDown() && LensrStart.isEditMode) {
                Actions.clear();
                return;
            }

            if (keyEvent.getCode().toString().equals("Z") && keyEvent.isControlDown() && !keyEvent.isShiftDown() && LensrStart.isEditMode) {
                Actions.undo();
                return;
            }

            if (keyEvent.getCode().toString().equals("Y") && keyEvent.isControlDown() && LensrStart.isEditMode || keyEvent.getCode().toString().equals("Z") && keyEvent.isControlDown() && keyEvent.isShiftDown() && LensrStart.isEditMode) {
                Actions.redo();
                return;
            }

            if (keyEvent.getCode().toString().equals("S") && keyEvent.isControlDown() && LensrStart.isEditMode) {
                if (Actions.lastSave == null) {
                    Actions.exportProject();
                } else {
                    SaveState.saveProject(Actions.lastSave.getAbsolutePath());
                }
                return;
            }

            if (keyEvent.getCode().toString().equals("O") && keyEvent.isControlDown() && LensrStart.isEditMode) {
                Actions.importProject();
                return;
            }

            // Object shortcuts
            if (keyEvent.getCode().toString().equals("Q") && LensrStart.isEditMode) {
                if (LensrStart.keyPressed == Key.Q) {
                    LensrStart.keyPressed = Key.None;
                } else {
                    LensrStart.keyPressed = Key.Q;
                }
                closeCurrentEdit();
            } else if (keyEvent.getCode().toString().equals("W") && LensrStart.isEditMode) {
                if (LensrStart.keyPressed == Key.W) {
                    LensrStart.keyPressed = Key.None;
                } else {
                    LensrStart.keyPressed = Key.W;
                }
                closeCurrentEdit();
            } else if (keyEvent.getCode().toString().equals("E") && LensrStart.isEditMode) {
                if (LensrStart.keyPressed == Key.E) {
                    LensrStart.keyPressed = Key.None;
                } else {
                    LensrStart.keyPressed = Key.E;
                }
                closeCurrentEdit();
            } else if (keyEvent.getCode().toString().equals("R") && LensrStart.isEditMode) {
                if (LensrStart.keyPressed == Key.R) {
                    LensrStart.keyPressed = Key.None;
                } else {
                    LensrStart.keyPressed = Key.R;
                }
                closeCurrentEdit();
            } else if (keyEvent.getCode().toString().equals("A") && LensrStart.isEditMode) {
                if (LensrStart.keyPressed == Key.A) {
                    LensrStart.keyPressed = Key.None;
                } else {
                    LensrStart.keyPressed = Key.A;
                }
                closeCurrentEdit();
            } else if (keyEvent.getCode().toString().equals("S") && LensrStart.isEditMode) {
                if (LensrStart.keyPressed == Key.S) {
                    LensrStart.keyPressed = Key.None;
                } else {
                    LensrStart.keyPressed = Key.S;
                }
                closeCurrentEdit();
            } else if (keyEvent.getCode().toString().equals("D") && LensrStart.isEditMode) {
                if (LensrStart.keyPressed == Key.D) {
                    LensrStart.keyPressed = Key.None;
                } else {
                    LensrStart.keyPressed = Key.D;
                }
                closeCurrentEdit();
            } else if (keyEvent.getCode().toString().equals("F") && LensrStart.isEditMode) {
                if (LensrStart.keyPressed == Key.F) {
                    LensrStart.keyPressed = Key.None;
                } else {
                    LensrStart.keyPressed = Key.F;
                }
                closeCurrentEdit();
            } else if (keyEvent.getCode().toString().equals("Z") && LensrStart.isEditMode) {
                if (LensrStart.keyPressed == Key.Z) {
                    LensrStart.keyPressed = Key.None;
                } else {
                    LensrStart.keyPressed = Key.Z;
                }
                closeCurrentEdit();
            } else if (keyEvent.getCode().toString().equals("X") && LensrStart.isEditMode) {
                if (LensrStart.keyPressed == Key.X) {
                    LensrStart.keyPressed = Key.None;
                } else {
                    LensrStart.keyPressed = Key.X;
                }
                closeCurrentEdit();
            } else if (keyEvent.getCode().toString().equals("C") && LensrStart.isEditMode) {
                if (LensrStart.keyPressed == Key.C) {
                    LensrStart.keyPressed = Key.None;
                } else {
                    LensrStart.keyPressed = Key.C;
                }
                closeCurrentEdit();
            } else if (keyEvent.getCode().toString().equals("V") && LensrStart.isEditMode) {
                if (LensrStart.keyPressed == Key.V) {
                    LensrStart.keyPressed = Key.None;
                } else {
                    LensrStart.keyPressed = Key.V;
                }
                closeCurrentEdit();
            } else if (keyEvent.getCode().toString().equals("L") && LensrStart.isEditMode) {
                if (LensrStart.keyPressed == Key.L) {
                    LensrStart.keyPressed = Key.None;
                } else {
                    LensrStart.keyPressed = Key.L;
                }
                closeCurrentEdit();
            } else if (keyEvent.getCode().toString().equals("P") && LensrStart.isEditMode) {
                if (LensrStart.keyPressed == Key.P) {
                    LensrStart.keyPressed = Key.None;
                } else {
                    LensrStart.keyPressed = Key.P;
                }
                closeCurrentEdit();
            }
        });

        LensrStart.scene.setOnKeyReleased(keyEvent -> {
            if (keyEvent.getCode().toString().equals("SHIFT")) {
                LensrStart.shiftPressed = false;
            }
            if (keyEvent.getCode().toString().equals("ALT")) {
                LensrStart.altPressed = false;
            }
        });

        LensrStart.scene.setOnMouseMoved(mouseEvent -> {
            LensrStart.mousePos = new Point2D(mouseEvent.getX(), mouseEvent.getY());
            LensrStart.mouseHitbox.setX(LensrStart.mousePos.getX() - LensrStart.mouseHitbox.getWidth() / 2);
            LensrStart.mouseHitbox.setY(LensrStart.mousePos.getY() - LensrStart.mouseHitbox.getHeight() / 2);

            // If mouse is on an edit point, change cursor to hand
            LensrStart.editPoints.stream()
                    .filter(editPoint -> editPoint.intersects(LensrStart.mouseHitbox.getLayoutBounds()))
                    .findFirst()
                    .ifPresentOrElse(editPoint -> LensrStart.scene.setCursor(Cursor.HAND), () -> LensrStart.scene.setCursor(Cursor.DEFAULT));
        });
    }

    public static void placeNewObject() {
        // Place objects
        switch (LensrStart.keyPressed) {
            case Q:
                RaySource raySource = new RaySource(LensrStart.mousePos.getX(), LensrStart.mousePos.getY());
                raySource.create();
                raySource.openObjectEdit();
                raySource.rotate();
                LensrStart.lightSources.add(raySource);
                break;
            case W:
                BeamSource beamSource = new BeamSource(LensrStart.mousePos.getX(), LensrStart.mousePos.getY(), LensrStart.mousePos.getX() + 100, LensrStart.mousePos.getY() + 100);
                beamSource.create();
                beamSource.openObjectEdit();
                beamSource.scale(LensrStart.mousePos);
                LensrStart.lightSources.add(beamSource);
                break;
            case E:
                PointSource fullPointSource = new PointSource(LensrStart.mousePos.getX(), LensrStart.mousePos.getY(), 2 * Math.PI, 0, 6, true);
                fullPointSource.create();
                fullPointSource.openObjectEdit();
                fullPointSource.rotate();
                LensrStart.lightSources.add(fullPointSource);
                break;
            case R:
                PointSource pointSource = new PointSource(LensrStart.mousePos.getX(), LensrStart.mousePos.getY(), Math.PI / 2, 0, 6, false);
                pointSource.create();
                pointSource.openObjectEdit();
                pointSource.rotate();
                LensrStart.lightSources.add(pointSource);
                break;
            case A:
                LineMirror lineMirror = new LineMirror(LensrStart.mousePos.getX(), LensrStart.mousePos.getY(), LensrStart.mousePos.getX(), LensrStart.mousePos.getY());
                lineMirror.create();
                lineMirror.openObjectEdit();
                lineMirror.scale(LensrStart.mousePos);
                LensrStart.mirrors.add(lineMirror);
                break;
            case S:
                EllipseMirror ellipseMirror = new EllipseMirror(LensrStart.mousePos.getX(), LensrStart.mousePos.getY(), 0, 0);
                ellipseMirror.create();
                ellipseMirror.openObjectEdit();
                ellipseMirror.scale(LensrStart.mousePos);
                LensrStart.mirrors.add(ellipseMirror);
                break;
            case D:
                ArcMirror arcMirror = new ArcMirror(LensrStart.mousePos.getX(), LensrStart.mousePos.getY());
                arcMirror.create();
                arcMirror.openObjectEdit();
                arcMirror.scale(arcMirror.objectEditPoints.get(1).getCenter(), arcMirror.objectEditPoints.get(0), arcMirror.objectEditPoints.get(1));
                LensrStart.mirrors.add(arcMirror);
                break;
            case F:
                FunnyMirror funnyMirror = new FunnyMirror();
                funnyMirror.openObjectEdit();
                funnyMirror.draw();
                LensrStart.mirrors.add(funnyMirror);
                break;
            case L:
                SphericalLens sphericalLens = new SphericalLens(50, 50, LensrStart.mousePos.getX(), LensrStart.mousePos.getY(), 1.5, 0.004, new Point2D(LensrStart.mousePos.getX() - 50, LensrStart.mousePos.getY()), new Point2D(LensrStart.mousePos.getX() + 50, LensrStart.mousePos.getY()));
                sphericalLens.create();
                sphericalLens.openObjectEdit();
                sphericalLens.scale(LensrStart.mousePos);
                LensrStart.lenses.add(sphericalLens);
                break;
            case P:
                Prism prism = new Prism(LensrStart.mousePos.getX(), LensrStart.mousePos.getY(), 1.5, 0.004);
                prism.create();
                prism.openObjectEdit();
                prism.draw();
                LensrStart.lenses.add(prism);
                break;
            case Z:
                GaussianRolloffFilter gaussianRolloffFilter = new GaussianRolloffFilter(LensrStart.mousePos.getX(), LensrStart.mousePos.getY(), LensrStart.mousePos.getX(), LensrStart.mousePos.getY());
                gaussianRolloffFilter.create();
                gaussianRolloffFilter.openObjectEdit();
                gaussianRolloffFilter.scale(LensrStart.mousePos);
                LensrStart.mirrors.add(gaussianRolloffFilter);
                break;
            case X:
                BrickwallFilter brickwallFilter = new BrickwallFilter(LensrStart.mousePos.getX(), LensrStart.mousePos.getY(), LensrStart.mousePos.getX(), LensrStart.mousePos.getY());
                brickwallFilter.create();
                brickwallFilter.openObjectEdit();
                brickwallFilter.scale(LensrStart.mousePos);
                LensrStart.mirrors.add(brickwallFilter);
                break;
            case C:
                LightSensor lightSensor = new LightSensor(LensrStart.mousePos.getX(), LensrStart.mousePos.getY(), LensrStart.mousePos.getX(), LensrStart.mousePos.getY());
                lightSensor.create();
                lightSensor.openObjectEdit();
                lightSensor.scale(LensrStart.mousePos);
                LensrStart.mirrors.add(lightSensor);
                break;
            case V:
                LightEater lightEater = new LightEater(LensrStart.mousePos.getX(), LensrStart.mousePos.getY(), 0);
                lightEater.create();
                lightEater.openObjectEdit();
                lightEater.scale(LensrStart.mousePos);
                LensrStart.mirrors.add(lightEater);
                break;
        }
    }

    public static void closeCurrentEdit() {
        if (LensrStart.editedShape instanceof Editable) {
            ((Editable) LensrStart.editedShape).closeObjectEdit();
        } else if (LensrStart.editedShape instanceof Group group) {
            group.getChildren().stream()
                    .filter(node -> node instanceof Editable)
                    .map(node -> (Editable) node)
                    .findFirst()
                    .ifPresent(Editable::closeObjectEdit);
        }
    }

    public static void deleteCurrentObject() {
        if (LensrStart.editedShape instanceof Group group) {
            group.getChildren().stream()
                    .filter(node -> node instanceof Editable)
                    .map(node -> (Editable) node)
                    .findFirst()
                    .ifPresent(editable -> {
                        editable.delete();
                        LensrStart.updateLightSources();
                    });
        }
        SaveState.autoSave();
    }

    public static void copyCurrentObject() {
        if (LensrStart.editedShape instanceof Group group) {
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
        clickableObjects.addAll(LensrStart.editPoints);
        clickableObjects.addAll(LensrStart.lightSources);
        clickableObjects.addAll(LensrStart.mirrors);
        clickableObjects.addAll(LensrStart.lenses);

        for (Object clickableObject : clickableObjects) {
            if (clickableObject instanceof EditPoint editPoint) {
                editPoint.hasBeenClicked = false;
            } else if (clickableObject instanceof Editable editable) {
                editable.setHasBeenClicked(false);
            }
        }
    }
}