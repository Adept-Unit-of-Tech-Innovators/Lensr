package com.lensr.objects.mirrors;

import com.lensr.ui.EditPoint;
import com.lensr.objects.Editable;
import com.lensr.saveloadkit.SaveState;
import com.lensr.UserControls;
import com.lensr.LensrStart;
import javafx.application.Platform;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Shape;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class FunnyMirror extends Polyline implements Editable, Serializable {
    public transient Group group = new Group();
    // The outline of the object for ray intersection
    private transient List<EditPoint> objectEditPoints = new ArrayList<>();
    private transient boolean isEdited;
    private transient boolean hasBeenClicked;
    // The percentage of light that is reflected, 0 - no light is reflected, 1 - perfect reflection
    public double reflectivity = 1;

    public FunnyMirror() {

    }

    // This method is used for loading the object from a file
    @Override
    public void create() {
        setStrokeWidth(LensrStart.globalStrokeWidth);
        setStroke(LensrStart.mirrorColor);
        group.getChildren().add(this);
        LensrStart.root.getChildren().add(group);
    }

    public void draw() {
        setStrokeWidth(LensrStart.globalStrokeWidth);
        setStroke(LensrStart.mirrorColor);
        group.getChildren().add(this);
        LensrStart.root.getChildren().add(group);
        LensrStart.taskPool.execute(() -> {
            getPoints().addAll(LensrStart.mousePos.getX(), LensrStart.mousePos.getY());
            int index = 2;
            while (LensrStart.isMousePressed && isEdited) {
                if ((Math.abs(getPoints().get(index - 2) - LensrStart.mousePos.getX()) > 10) || (Math.abs(getPoints().get(index - 1) - LensrStart.mousePos.getY()) > 10)) {
                    getPoints().addAll(LensrStart.mousePos.getX(), LensrStart.mousePos.getY());
                    index = index + 2;
                }
                if (!objectEditPoints.isEmpty()) {
                    // Update editPoints location
                    Bounds mirrorBounds = getFunnyMirrorBounds();

                    for (int i = 0; i < objectEditPoints.size(); i++) {
                        double x = (i == 1 || i == 2) ? mirrorBounds.getMaxX() : mirrorBounds.getMinX();
                        double y = (i == 2 || i == 3) ? mirrorBounds.getMaxY() : mirrorBounds.getMinY();

                        objectEditPoints.get(i).setCenterX(x);
                        objectEditPoints.get(i).setCenterY(y);
                    }
                    objectEditPoints.get(4).setCenter(new Point2D(mirrorBounds.getCenterX(), mirrorBounds.getCenterY()));
                }

                // The higher the value, the faster you can move the mouse without deforming the object, but at the cost of responsiveness
                synchronized (LensrStart.lock) {
                    try {
                        LensrStart.lock.wait(20);
                    } catch (InterruptedException e) {
                        throw new RuntimeException("A thread was interrupted while waiting.");
                    }
                }
            }
            SaveState.autoSave();
        });
    }

    @Override
    public void moveBy(double x, double y) {
        for (int i = 0; i < getPoints().size(); i += 2) {
            getPoints().set(i, getPoints().get(i) + x);
            getPoints().set(i + 1, getPoints().get(i + 1) + y);
        }
        objectEditPoints.forEach(editPoint -> {
            editPoint.setCenterX(editPoint.getCenterX() + x);
            editPoint.setCenterY(editPoint.getCenterY() + y);
        });
        SaveState.autoSave();
    }

    public void move() {
        LensrStart.taskPool.execute(() -> {
            Point2D prevMousePos = new Point2D(LensrStart.mousePos.getX(), LensrStart.mousePos.getY());

            while (LensrStart.isMousePressed && isEdited) {
                double deltaX = (LensrStart.mousePos.getX() - prevMousePos.getX());
                double deltaY = (LensrStart.mousePos.getY() - prevMousePos.getY());

                for (int i = 0; i < getPoints().size(); i += 2) {
                    double pointX = getPoints().get(i);
                    double pointY = getPoints().get(i + 1);

                    // Update the point's coordinates
                    int finalI = i;
                    Platform.runLater(() -> {
                        getPoints().set(finalI, pointX + deltaX);
                        getPoints().set(finalI + 1, pointY + deltaY);
                    });
                }

                // Update the editPoints location
                if (!objectEditPoints.isEmpty()) {
                    Platform.runLater(() -> {
                        Bounds mirrorBounds = getFunnyMirrorBounds();

                        for (int i = 0; i < objectEditPoints.size(); i++) {
                            double x = (i == 1 || i == 2) ? mirrorBounds.getMaxX() : mirrorBounds.getMinX();
                            double y = (i == 2 || i == 3) ? mirrorBounds.getMaxY() : mirrorBounds.getMinY();

                            objectEditPoints.get(i).setCenterX(x);
                            objectEditPoints.get(i).setCenterY(y);
                        }
                        objectEditPoints.get(4).setCenter(new Point2D(mirrorBounds.getCenterX(), mirrorBounds.getCenterY()));
                    });
                }
                prevMousePos = new Point2D(LensrStart.mousePos.getX(), LensrStart.mousePos.getY());

                // The higher the value, the faster you can move the mouse without deforming the object, but at the cost of responsiveness
                synchronized (LensrStart.lock) {
                    try {
                        LensrStart.lock.wait(20);
                    } catch (InterruptedException e) {
                        throw new RuntimeException("A thread was interrupted while waiting.");
                    }
                }
            }
            SaveState.autoSave();
        });
    }

    public void scale(EditPoint anchorPoint) {
        LensrStart.taskPool.execute(() -> {
            double threshold = 1.0; // We only want to scale the object if the mouse has moved more than 1 pixel
            while (LensrStart.isMousePressed && isEdited) {
                double originalWidth = getFunnyMirrorBounds().getWidth();
                double originalHeight = getFunnyMirrorBounds().getHeight();
                double widthToHeightRatio = originalWidth / originalHeight;

                double deltaX, deltaY;
                double tempDeltaX = Math.abs(LensrStart.mousePos.getX() - anchorPoint.getCenterX());
                double tempDeltaY = Math.abs(LensrStart.mousePos.getY() - anchorPoint.getCenterY());

                deltaX = (LensrStart.shiftPressed && widthToHeightRatio < 1) ? tempDeltaY * widthToHeightRatio : tempDeltaX;
                deltaY = (LensrStart.shiftPressed && widthToHeightRatio > 1) ? tempDeltaX / widthToHeightRatio : tempDeltaY;


                if (deltaX > threshold || deltaY > threshold) {
                    for (int i = 0; i < getPoints().size(); i += 2) {
                        double pointX = getPoints().get(i);
                        double pointY = getPoints().get(i + 1);

                        // Calculate adjusted point's coordinates using proportions
                        double newPointX = anchorPoint.getCenterX() + ((pointX - anchorPoint.getCenterX()) * deltaX / originalWidth);
                        double newPointY = anchorPoint.getCenterY() + ((pointY - anchorPoint.getCenterY()) * deltaY / originalHeight);

                        // Update the point's coordinates
                        int finalI = i;
                        Platform.runLater(() -> {
                            getPoints().set(finalI, newPointX);
                            getPoints().set(finalI + 1, newPointY);
                        });
                    }

                    // Update the editPoints location
                    if (!objectEditPoints.isEmpty()) {
                        Platform.runLater(() -> {
                            Bounds mirrorBounds = getFunnyMirrorBounds();

                            for (int i = 0; i < objectEditPoints.size(); i++) {
                                if (objectEditPoints.get(i).equals(anchorPoint)) continue;
                                double x = (i == 1 || i == 2) ? mirrorBounds.getMaxX() : mirrorBounds.getMinX();
                                double y = (i == 2 || i == 3) ? mirrorBounds.getMaxY() : mirrorBounds.getMinY();

                                objectEditPoints.get(i).setCenterX(x);
                                objectEditPoints.get(i).setCenterY(y);
                            }
                            objectEditPoints.get(4).setCenter(new Point2D(mirrorBounds.getCenterX(), mirrorBounds.getCenterY()));
                        });
                    }
                }

                // The higher the value, the faster you can move the mouse without deforming the object, but at the cost of responsiveness
                synchronized (LensrStart.lock) {
                    try {
                        LensrStart.lock.wait(20);
                    } catch (InterruptedException e) {
                        throw new RuntimeException("A thread was interrupted while waiting.");
                    }
                }
            }
            SaveState.autoSave();
        });
    }

    @Serial
    private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
        out.defaultWriteObject();
        out.writeInt(getPoints().size());
        for (Double point : getPoints()) {
            out.writeDouble(point);
        }
    }

    @Serial
    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
        in.defaultReadObject();
        int size = in.readInt();
        getPoints().clear();
        for (int i = 0; i < size; i++) {
            getPoints().add(in.readDouble());
        }

        // Initialize transient fields
        group = new Group();
        objectEditPoints = new ArrayList<>();
        isEdited = false;
        hasBeenClicked = false;
    }

    @Override
    public void delete() {
        LensrStart.reflectivitySlider.hide();
        LensrStart.editPoints.removeAll(objectEditPoints);
        LensrStart.editedShape = null;
        LensrStart.mirrors.remove(this);
        LensrStart.root.getChildren().remove(group);
    }

    @Override
    public void copy() {
        FunnyMirror mirror = new FunnyMirror();
        this.getPoints().forEach(point -> mirror.getPoints().add(point));
        mirror.setReflectivity(getReflectivity());

        mirror.setStrokeWidth(LensrStart.globalStrokeWidth);
        mirror.setStroke(LensrStart.mirrorColor);
        mirror.group.getChildren().add(mirror);
        LensrStart.root.getChildren().add(mirror.group);

        LensrStart.mirrors.add(mirror);
        mirror.moveBy(10, 10);
        UserControls.closeCurrentEdit();
        mirror.openObjectEdit();
    }

    public void setReflectivity(double reflectivity) {
        this.reflectivity = reflectivity;
    }


    public double getReflectivity() {
        return reflectivity;
    }

    @Override
    public void openObjectEdit() {
        LensrStart.reflectivitySlider.setCurrentSource(this);
        LensrStart.reflectivitySlider.show();

        // Defocus the text field
        LensrStart.root.requestFocus();

        hasBeenClicked = true;
        isEdited = true;

        // Place edit points
        Bounds mirrorBounds = getFunnyMirrorBounds();
        objectEditPoints.add(new EditPoint(mirrorBounds.getMinX(), mirrorBounds.getMinY(), EditPoint.Style.Primary));
        objectEditPoints.add(new EditPoint(mirrorBounds.getMaxX(), mirrorBounds.getMinY(), EditPoint.Style.Primary));
        objectEditPoints.add(new EditPoint(mirrorBounds.getMaxX(), mirrorBounds.getMaxY(), EditPoint.Style.Primary));
        objectEditPoints.add(new EditPoint(mirrorBounds.getMinX(), mirrorBounds.getMaxY(), EditPoint.Style.Primary));

        // Define what happens when an edit point is clicked
        for (EditPoint editPoint : objectEditPoints) {
            editPoint.setOnClickEvent(event -> {
                // Scale the mirror with the opposite edit point as an anchor
                EditPoint oppositeEditPoint = objectEditPoints.get(((objectEditPoints.indexOf(editPoint)) + 2) % 4);
                scale(oppositeEditPoint);
            });
        }

        objectEditPoints.add(new EditPoint(mirrorBounds.getCenterX(), mirrorBounds.getCenterY(), EditPoint.Style.Secondary));
        objectEditPoints.get(4).setOnClickEvent(event -> move());

        LensrStart.editPoints.addAll(objectEditPoints);
        group.getChildren().addAll(objectEditPoints);
        LensrStart.editedShape = group;
    }

    @Override
    public void closeObjectEdit() {
        LensrStart.reflectivitySlider.hide();
        isEdited = false;
        if (objectEditPoints != null && LensrStart.editedShape instanceof Group editedGroup) {
            editedGroup.getChildren().removeAll(objectEditPoints);
            LensrStart.editPoints.removeAll(objectEditPoints);
            objectEditPoints.clear();
        }
        LensrStart.editedShape = null;
        LensrStart.updateLightSources();
    }

    public Bounds getFunnyMirrorBounds() {
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double maxY = Double.MIN_VALUE;

        for (int i = 0; i < getPoints().size(); i += 2) {
            double x = getPoints().get(i);
            double y = getPoints().get(i + 1);

            if (x < minX) minX = x;
            if (x > maxX) maxX = x;
            if (y < minY) minY = y;
            if (y > maxY) maxY = y;
        }

        return new BoundingBox(minX, minY, maxX - minX, maxY - minY);
    }

    @Override
    public void setHasBeenClicked(boolean hasBeenClicked) {
        this.hasBeenClicked = hasBeenClicked;
    }

    @Override
    public boolean getHasBeenClicked() {
        return hasBeenClicked;
    }

    @Override
    public boolean intersectsMouseHitbox() {
        return Shape.intersect(this, LensrStart.mouseHitbox).getLayoutBounds().getWidth() != -1;
    }
}
