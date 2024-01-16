package com.example.lensr;

import javafx.scene.Cursor;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.List;

import static com.example.lensr.LensrStart.*;

public class MirrorMethods {
    public static void setupObjectEdit() {

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
            editPoint.setStroke(Color.BLACK);
            editPoint.setStrokeWidth(1);
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
    public static void closeMirrorsEdit() {
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
    }


}
