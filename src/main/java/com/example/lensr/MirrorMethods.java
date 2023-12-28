package com.example.lensr;

import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

import java.util.List;

import static com.example.lensr.LensrStart.*;

public class MirrorMethods {
    public static void setupObjectEdit() {
        xPressed.setValueAndCloseEdit(false);
        zPressed.setValueAndCloseEdit(false);

        for (Object mirror : mirrors) {
            if (mirror instanceof EllipseMirror ellipseMirror) {
                if (ellipseMirror.isEdited) {
                    ellipseMirror.isEditPointClicked.setValue(false);
                    ellipseMirror.closeObjectEdit();

                }
            }
            else {
                if (mirror instanceof LineMirror lineMirror) {
                    if (lineMirror.isEdited) {
                        lineMirror.isEditPointClicked.setValue(false);
                        lineMirror.closeObjectEdit();
                    }
                }
            }
        }
    }

    public static void setupEditPoints(List<Rectangle> editPoints, MutableValue isEditPointClicked) {
        for (Rectangle editPoint : editPoints) {
            editPoint.setFill(Color.RED);
            editPoint.setStrokeWidth(0);
            editPoint.setOnMouseEntered(mouseEvent -> {
                if (!isEditPointClicked.getValue()) {
                    scene.setCursor(Cursor.HAND);
                }
            });
            editPoint.setOnMouseExited(mouseEvent -> {
                if (!isEditPointClicked.getValue()) {
                    scene.setCursor(Cursor.DEFAULT);
                }
            });
        }
    }

    public static void handleEditPointReleased(MouseEvent event, MutableValue isEditPointClicked, List<Rectangle> editPoints) {
        isMousePressed = false;
        isEditPointClicked.setValue(false);

        for (Rectangle editPoint : editPoints) {
            if (editPoint.contains(mousePos)) {
                scene.setCursor(Cursor.HAND);
                break;
            }
            else scene.setCursor(Cursor.DEFAULT);
        }

        event.consume();
    }

    public static void iterateOverlaps(Shape currentMirror, Group group) {
        for (Object mirror : mirrors) {
            if (mirror.equals(currentMirror)) continue;

            if (mirror instanceof Shape mirrorShape) {
                // If the mirror overlaps with another object, remove it
                if (Shape.intersect(currentMirror , mirrorShape).getLayoutBounds().getWidth() >= 0) {
                    root.getChildren().remove(group);
                    mirrors.remove(currentMirror);
                    return;
                }
            }
        }
    }

}
