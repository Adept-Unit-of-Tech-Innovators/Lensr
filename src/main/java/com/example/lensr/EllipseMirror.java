package com.example.lensr;

import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeType;

import java.util.ArrayList;
import java.util.List;

import static com.example.lensr.Intersections.getObjectOutline;
import static com.example.lensr.LensrStart.*;

public class EllipseMirror extends Ellipse {
    Group group = new Group();
    // The outline of the object for ray intersection
    public Shape outline = getObjectOutline(this);
    List<Rectangle> editPoints = new ArrayList<>();
    // The percentage of light that is reflected, 0 - no light is reflected, 1 - perfect reflection
    double reflectivity = 0.99;
    boolean isEdited;
    boolean isEditPointClicked;



    public EllipseMirror(double centerX, double centerY, double radiusX, double radiusY) {
        setCenterX(centerX);
        setCenterY(centerY);
        setRadiusX(radiusX);
        setRadiusY(radiusY);
    }


    public void create() {
        setFill(Color.TRANSPARENT);
        setStroke(mirrorColor);
        setStrokeWidth(globalStrokeWidth);
        setStrokeType(StrokeType.OUTSIDE);

        setOnMouseClicked(mouseEvent -> {
            if (isEditMode && !isEdited) openObjectEdit();
        });

        group.getChildren().add(this);
        group.getChildren().add(outline);
        root.getChildren().add(group);
    }


    public void openObjectEdit() {
        xPressed.setValue(false);
        zPressed.setValue(false);

        for (Object mirror : mirrors) {
            if (mirror instanceof EllipseMirror ellipseMirror) {
                if (ellipseMirror.isEdited) {
                    ellipseMirror.isEditPointClicked = false;
                    ellipseMirror.closeObjectEdit();

                }
            }
            else {
                if (mirror instanceof LineMirror lineMirror) {
                    if (lineMirror.isEdited) {
                        lineMirror.isEditPointClicked = false;
                        lineMirror.closeObjectEdit();
                    }
                }
            }
        }
        isEdited = true;

        // Place edit points
        Bounds mirrorBounds = getLayoutBounds();
        editPoints.add(new Rectangle(mirrorBounds.getMinX() - editPointSize / 2, mirrorBounds.getMinY() - editPointSize / 2, editPointSize, editPointSize));
        editPoints.add(new Rectangle(mirrorBounds.getMaxX() - editPointSize / 2, mirrorBounds.getMinY() - editPointSize / 2, editPointSize, editPointSize));
        editPoints.add(new Rectangle(mirrorBounds.getMaxX() - editPointSize / 2, mirrorBounds.getMaxY() - editPointSize / 2, editPointSize, editPointSize));
        editPoints.add(new Rectangle(mirrorBounds.getMinX() - editPointSize / 2, mirrorBounds.getMaxY() - editPointSize / 2, editPointSize, editPointSize));

        for (Rectangle editPoint : editPoints) {
            editPoint.setFill(Color.RED);
            editPoint.setStrokeWidth(0);
            editPoint.setOnMouseEntered(mouseEvent -> {
                if (!isEditPointClicked) {
                    scene.setCursor(Cursor.HAND);
                }
            });
            editPoint.setOnMouseExited(mouseEvent -> {
                if (!isEditPointClicked) {
                    scene.setCursor(Cursor.DEFAULT);
                }
            });
            editPoint.setOnMousePressed(this::handleEditPointPressed);
            editPoint.setOnMouseReleased(this::handleEditPointReleased);
        }
        group.getChildren().addAll(editPoints);
        editedShape = group;
    }


    private void handleEditPointPressed(MouseEvent event) {
        isMousePressed = true;
        isEditPointClicked = true;
        scene.setCursor(Cursor.CLOSED_HAND);

        // Scale the mirror with the opposite edit point as an anchor
        //noinspection SuspiciousMethodCalls (it's very sussy)
        Rectangle oppositeEditPoint = editPoints.get(( (editPoints.indexOf(event.getSource()) + 2)  % 4));
        scale(new Point2D(oppositeEditPoint.getX() + editPointSize / 2, oppositeEditPoint.getY() + editPointSize / 2));
    }


    private void handleEditPointReleased(MouseEvent event) {
        isMousePressed = false;
        isEditPointClicked = false;

        for (Rectangle editPoint : editPoints) {
            if (editPoint.contains(mousePos)) {
                scene.setCursor(Cursor.HAND);
                break;
            }
            else scene.setCursor(Cursor.DEFAULT);
        }

        event.consume();
    }


    public void closeObjectEdit() {
        isEdited = false;
        removeIfOverlaps();
        if (editPoints != null && editedShape instanceof Group editedGroup) {
            editedGroup.getChildren().removeAll(editPoints);
            editPoints.clear();
        }
    }


    private void updateOutline() {
        Ellipse ellipse = new Ellipse(this.getCenterX(), this.getCenterY(), this.getRadiusX(), this.getRadiusY());
        ellipse.setFill(Color.TRANSPARENT);
        ellipse.setStroke(Color.BLACK);
        outline = getObjectOutline(ellipse);
        outline.setStrokeWidth(1);
    }


    public void setReflectivity(double reflectivity) {
        this.reflectivity = reflectivity;
    }


    public double getReflectivity() {
        return reflectivity;
    }


    public void removeIfOverlaps() {
        // Remove the mirror if its size is 0
        if (this.getRadiusX() == 0 && this.getRadiusY() == 0) {
            root.getChildren().remove(group);
            mirrors.remove(this);
            return;
        }

        for (Object mirror : mirrors) {
            if (mirror.equals(this)) continue;

            if (mirror instanceof Shape mirrorShape) {
                // If the mirror overlaps with another object, remove it
                if (Shape.intersect(this , mirrorShape).getLayoutBounds().getWidth() >= 0) {
                    root.getChildren().remove(group);
                    mirrors.remove(this);
                    return;
                }
            }
        }
    }


    public void scale(Point2D anchor) {
        new Thread(() -> {
            double centerX, centerY, radiusX, radiusY;

            while (isMousePressed) {
                boolean intersects = false;

                // Resizing standard based on Photoshop and MS Paint :)
                if (altPressed && shiftPressed) {
                    centerX = anchor.getX();
                    centerY = anchor.getY();
                    radiusX = radiusY = Math.min( Math.abs(anchor.getX() - mousePos.getX()), Math.abs(anchor.getY() - mousePos.getY()) );
                }
                else if (altPressed) {
                    centerX = anchor.getX();
                    centerY = anchor.getY();
                    radiusX = Math.abs(mousePos.getX() - centerX);
                    radiusY = Math.abs(mousePos.getY() - centerY);
                }
                else if (shiftPressed) {
                    double minDistance = Math.min( Math.abs(anchor.getX() - mousePos.getX()), Math.abs(anchor.getY() - mousePos.getY()) ) / 2;
                    centerX = anchor.getX() + (mousePos.getX() > anchor.getX() ? minDistance : -minDistance);
                    centerY = anchor.getY() + (mousePos.getY() > anchor.getY() ? minDistance : -minDistance);
                    radiusX = radiusY = Math.min( Math.abs(centerX - mousePos.getX()), Math.abs(centerY - mousePos.getY()) );
                }
                else {
                    centerX = anchor.getX() + ( (mousePos.getX() - anchor.getX()) / 2);
                    centerY = anchor.getY() + ( (mousePos.getY() - anchor.getY()) / 2);
                    radiusX = Math.abs(mousePos.getX() - centerX);
                    radiusY = Math.abs(mousePos.getY() - centerY);
                }
                
                Ellipse intersectionChecker = new Ellipse(centerX, centerY, radiusX, radiusY);

                for (Object mirror : mirrors) {
                    if (mirror.equals(this)) continue;

                    if (mirror instanceof Shape mirrorShape) {
                        // If the mirror overlaps with another object, remove it
                        if (Shape.intersect(intersectionChecker , mirrorShape).getLayoutBounds().getWidth() >= 0) {
                            intersects = true;
                            break;
                        }
                    }
                }

                if (!intersects) {
                    setCenterX(centerX);
                    setCenterY(centerY);
                    setRadiusX(radiusX);
                    setRadiusY(radiusY);
                }

                // Update editPoints location
                if (isEditPointClicked) {
                    Bounds mirrorBounds = getLayoutBounds();
                    int offset = -4;

                    for (int i = 0; i < editPoints.size(); i++) {
                        double x = (i == 1 || i == 2) ? mirrorBounds.getMaxX() : mirrorBounds.getMinX();
                        double y = (i == 2 || i == 3) ? mirrorBounds.getMaxY() : mirrorBounds.getMinY();

                        editPoints.get(i).setX(x + offset);
                        editPoints.get(i).setY(y + offset);
                    }
                }

               updateOutline();

                // Update the UI on the JavaFX application thread
                Platform.runLater(() -> {
                    // Update UI components or perform other UI-related tasks
                });

                synchronized (lock) {
                    try {
                        lock.wait(10); // Adjust the wait time as needed
                    } catch (InterruptedException e) {
                        throw new RuntimeException("A thread was interrupted while waiting.");
                    }
                }
            }

        }).start();
    }
}