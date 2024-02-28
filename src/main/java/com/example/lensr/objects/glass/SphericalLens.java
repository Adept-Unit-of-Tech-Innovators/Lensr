package com.example.lensr.objects.glass;

import com.example.lensr.*;
import com.example.lensr.objects.Editable;
import com.example.lensr.saveloadkit.SaveState;
import com.example.lensr.ui.EditPoint;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.example.lensr.Intersections.rotatePointAroundOtherByAngle;
import static com.example.lensr.LensrStart.*;

public class SphericalLens extends Group implements Glass, Editable, Serializable {

    private transient Group group = new Group();
    private transient List<EditPoint> objectEditPoints = new ArrayList<>();
    public transient List<Shape> elements = new ArrayList<>();
    private transient Map<EditPoint, Tuple<Shape, String>> editPointParents = new HashMap<>();
    private transient boolean isEdited, hasBeenClicked;
    private double centerX, centerY, height, width, angleOfRotation;
    private transient Point2D arc1Vertex = null;
    private transient Point2D arc2Vertex = null;
    private double transparency = 0.5;

    // Optical properties
    private double coefficientA, coefficientB, focalLength;
    private transient Point2D focalPoint;

    public SphericalLens(double middleHeight, double middleWidth, double centerX, double centerY, double coefficientA, double coefficientB, Point2D arc1Vertex, Point2D arc2Vertex) {
        this.height = middleHeight;
        this.width = middleWidth;
        this.centerX = centerX;
        this.centerY = centerY;
        this.coefficientA = coefficientA;
        this.coefficientB = coefficientB;
        this.arc1Vertex = arc1Vertex;
        this.arc2Vertex = arc2Vertex;
    }

    @Override
    public void create() {
        LensLine topLine = new LensLine(this, 0);
        LensLine bottomLine = new LensLine(this, Math.PI);
        topLine.scale();
        bottomLine.scale();

        LensArc firstArc = new LensArc(this, Map.of(bottomLine, "start", topLine, "start"), arc1Vertex);
        LensArc secondArc = new LensArc(this, Map.of(topLine, "end", bottomLine, "end"), arc2Vertex);

        elements.add(topLine);
        elements.add(bottomLine);
        elements.add(firstArc);
        elements.add(secondArc);

        resize(centerX, centerY, width, height, Math.toRadians(0), new Point2D(centerX, centerY));

        // Add the elements' edit points and set the elements' stroke
        for (Shape element : elements) {
            element.setStroke(mirrorColor);
            element.setStrokeWidth(globalStrokeWidth);
            if (element instanceof LensLine line) {
                EditPoint startEditPoint = new EditPoint(line.getStartX(), line.getStartY(), EditPoint.Style.Primary);
                EditPoint endEditPoint = new EditPoint(line.getEndX(), line.getEndY(), EditPoint.Style.Primary);
                double angleToStartPoint = Math.atan2(line.getStartY() - centerY, line.getStartX() - centerX);
                double angleToEndPoint = Math.atan2(line.getEndY() - centerY, line.getEndX() - centerX);
                EditPoint startRotatePoint = new EditPoint(line.getStartX() + 20 * Math.cos(angleToStartPoint), line.getStartY() + 20 * Math.sin(angleToStartPoint), EditPoint.Style.Tertiary);
                EditPoint endRotatePoint = new EditPoint(line.getEndX() + 20 * Math.cos(angleToEndPoint), line.getEndY() + 20 * Math.sin(angleToEndPoint), EditPoint.Style.Tertiary);

                // Set the scaling or rotating events for the edit points
                startEditPoint.setOnClickEvent(event -> scale(getOppositePoint(startEditPoint.getCenter(), getCenter())));
                endEditPoint.setOnClickEvent(event -> scale(getOppositePoint(endEditPoint.getCenter(), getCenter())));
                startRotatePoint.setOnClickEvent(event -> rotate());
                endRotatePoint.setOnClickEvent(event -> rotate());

                objectEditPoints.addAll(List.of(startEditPoint, endEditPoint, startRotatePoint, endRotatePoint));

                // Add the edit points to the map
                editPointParents.put(startEditPoint, new Tuple<>(line, "start"));
                editPointParents.put(endEditPoint, new Tuple<>(line, "end"));
                editPointParents.put(startRotatePoint, new Tuple<>(line, "start"));
                editPointParents.put(endRotatePoint, new Tuple<>(line, "end"));
            }
            if (element instanceof LensArc arc) {
                EditPoint arcVertex = new EditPoint(arc.getVertex().getX(), arc.getVertex().getY(), EditPoint.Style.Quaternary);
                arcVertex.setOnClickEvent(event -> taskPool.execute(() -> {
                    while (isMousePressed) {
                        arc.scale(mousePos);
                        synchronized (lock) {
                            try {
                                lock.wait(10); // Adjust the wait time as needed
                            } catch (InterruptedException e) {
                                throw new RuntimeException("A thread was interrupted while waiting.");
                            }
                        }
                        if (arc == firstArc) arc1Vertex = new Point2D(arc.getVertex().getX(), arc.getVertex().getY());
                        else arc2Vertex = new Point2D(arc.getVertex().getX(), arc.getVertex().getY());
                        alignEditPoints();
                    }
                    SaveState.autoSave();
                }));
                objectEditPoints.add(arcVertex);
                editPointParents.put(arcVertex, new Tuple<>(arc, "vertex"));
            }
        }

        // Center point
        EditPoint centerPoint = new EditPoint(getCenterX(), getCenterY(), EditPoint.Style.Secondary);
        centerPoint.setOnClickEvent(event -> move());
        objectEditPoints.add(centerPoint);

        // Hide all edit points
        objectEditPoints.forEach(editPoint -> editPoint.setVisible(false));

        // Add everything to the group
        group.getChildren().add(this);
        group.getChildren().addAll(elements);
        group.getChildren().addAll(objectEditPoints);
        root.getChildren().add(group);
    }

    @Override
    public void openObjectEdit() {
        // Set up the sliders
        coefficientASlider.setCurrentSource(this);
        coefficientBSlider.setCurrentSource(this);
        transparencySlider.setCurrentSource(this);
        coefficientASlider.show();
        coefficientBSlider.show();
        transparencySlider.show();

        objectEditPoints.forEach(editPoint -> {
            editPoint.setVisible(true);
            editPoint.toFront();
        });

        hasBeenClicked = true;
        isEdited = true;

        // Defocus the text field
        root.requestFocus();

        editPoints.addAll(objectEditPoints);
        editedShape = group;
    }

    @Override
    public void closeObjectEdit() {
        coefficientASlider.hide();
        coefficientBSlider.hide();
        transparencySlider.hide();
        isEdited = false;
        if (objectEditPoints != null && editedShape instanceof Group) {
            editPoints.removeAll(objectEditPoints);
            objectEditPoints.forEach(editPoint -> {
                editPoint.setVisible(false);
                editPoint.hasBeenClicked = false;
            });
        }

        editedShape = null;
        updateLightSources();
    }

    public void resize(double newCenterX, double newCenterY, double newWidth, double newHeight, double newAngleOfRotation, Point2D anchor) {
        // Scale appropriately all the elements
        Point2D oldCenter = new Point2D(getCenterX(), getCenterY());
        centerX = newCenterX;
        centerY = newCenterY;
        height = newHeight;
        width = newWidth;
        double rotationDelta =  newAngleOfRotation - angleOfRotation;
        angleOfRotation = newAngleOfRotation;

        for (Shape element : elements) {
            if (element instanceof LensLine line) {
                line.scale();
            }
            else if (element instanceof LensArc arc) {
                if (Math.abs(rotationDelta) == 0) {
                    // Calculate the new position of the vertex from the change in center
                    if (arc.getAnchors().a().equals(anchor) || arc.getAnchors().b().equals(anchor)) {
                        arc.scale(arc.getVertex());
                        if (arc == elements.get(2)) arc1Vertex = new Point2D(arc.getVertex().getX(), arc.getVertex().getY());
                        else arc2Vertex = new Point2D(arc.getVertex().getX(), arc.getVertex().getY());
                        continue;
                    }
                    double centerDeltaX = 2*(newCenterX - oldCenter.getX());
                    double centerDeltaY = 2*(newCenterY - oldCenter.getY());
                    Point2D oldMiddlePoint = (arc.getAnchors().a().midpoint(arc.getAnchors().b()));
                    Point2D adjustedAnchor1 = new Point2D(arc.getAnchors().a().getX() + centerDeltaX, arc.getAnchors().a().getY() + centerDeltaY);
                    Point2D adjustedAnchor2 = new Point2D(arc.getAnchors().b().getX() + centerDeltaX, arc.getAnchors().b().getY() + centerDeltaY);
                    Point2D newMiddlePoint = adjustedAnchor1.midpoint(adjustedAnchor2);
                    double deltaX = newMiddlePoint.getX() - oldMiddlePoint.getX();
                    double deltaY = newMiddlePoint.getY() - oldMiddlePoint.getY();
                    Point2D newVertex = new Point2D(arc.getVertex().getX() + deltaX, arc.getVertex().getY() + deltaY);
                    arc.scale(newVertex);
                    if (arc == elements.get(2)) arc1Vertex = new Point2D(arc.getVertex().getX(), arc.getVertex().getY());
                    else arc2Vertex = new Point2D(arc.getVertex().getX(), arc.getVertex().getY());
                    continue;
                }
                // Calculate the new position of the vertex of the arc
                Point2D relativeVertex = rotatePointAroundOtherByAngle(arc.getVertex(), new Point2D(centerX, centerY), rotationDelta);
                Point2D adjustedVertex = new Point2D(relativeVertex.getX() + centerX, relativeVertex.getY() + centerY);
                arc.scale(adjustedVertex);
            }
        }
    }

    public void move() {
        taskPool.execute(() -> {
            while (isMousePressed) {
                double x = mousePos.getX()-centerX;
                double y = mousePos.getY()-centerY;

                // Update the UI on the JavaFX application thread
                Platform.runLater(() -> {
                    centerX += x;
                    centerY += y;
                    for (Shape element : elements) {
                        if (element instanceof LensArc arc) {
                            arc.move(x, y);
                        }
                        else if (element instanceof LensLine line) {
                            line.move(x, y);
                        }
                    }
                    alignEditPoints();
                });

                synchronized (lock) {
                    try {
                        lock.wait(10); // Adjust the wait time as needed
                    } catch (InterruptedException e) {
                        throw new RuntimeException("A thread was interrupted while waiting.");
                    }
                }
            }
            SaveState.autoSave();
        });
    }

    public void scale(Point2D anchor) {
        taskPool.execute(() -> {
            double newCenterX, newCenterY, newWidth, newHeight;

            while (isMousePressed) {
                newCenterX = mousePos.getX() + (anchor.getX() - mousePos.getX()) / 2;
                newCenterY = mousePos.getY() + (anchor.getY() - mousePos.getY()) / 2;

                Point2D centerPoint = new Point2D(newCenterX, newCenterY);
                Point2D unrotatedMousePos = rotatePointAroundOtherByAngle(mousePos, centerPoint, -angleOfRotation);
                Point2D unrotatedAnchorPos = rotatePointAroundOtherByAngle(anchor, centerPoint, -angleOfRotation);

                newWidth = Math.abs(unrotatedAnchorPos.getX() - unrotatedMousePos.getX());
                newHeight = Math.abs(unrotatedAnchorPos.getY() - unrotatedMousePos.getY());

                double finalCenterX = newCenterX;
                double finalCenterY = newCenterY;
                double finalWidth = newWidth;
                double finalHeight = newHeight;

                Platform.runLater(() -> {
                    resize(finalCenterX, finalCenterY, finalWidth, finalHeight, this.angleOfRotation, anchor);
                    alignEditPoints();
                });

                synchronized (lock) {
                    try {
                        lock.wait(10); // Adjust the wait time as needed
                    } catch (InterruptedException e) {
                        throw new RuntimeException("A thread was interrupted while waiting.");
                    }
                }
            }
            SaveState.autoSave();
        });
    }

    public void rotate() {
        taskPool.execute(() -> {
            double newAngleOfRotation;

            double mouseAngle = Math.atan2(centerY - mousePos.getY(), centerX - mousePos.getX());
            while (isMousePressed) {
                int snapValue = 1;

                newAngleOfRotation = Math.atan2(centerY - mousePos.getY(), centerX - mousePos.getX()) - mouseAngle;
                if (shiftPressed) snapValue = 45;
                else if (altPressed) snapValue = 90;

                // Calculate the final angle of rotation
                double finalAngleOfRotation = Math.toRadians(Math.floor(Math.toDegrees(newAngleOfRotation) / snapValue) * snapValue);

                Platform.runLater(() -> {
                    resize(this.centerX, this.centerY, this.width, this.height, finalAngleOfRotation, new Point2D(centerX, centerY));
                    alignEditPoints();
                });

                synchronized (lock) {
                    try {
                        lock.wait(10); // Adjust the wait time as needed
                    } catch (InterruptedException e) {
                        throw new RuntimeException("A thread was interrupted while waiting.");
                    }
                }
            }
            SaveState.autoSave();
        });
    }

    public void alignEditPoints() {
        for (EditPoint editPoint : objectEditPoints) {

            // Center point
            if (editPoint.style == EditPoint.Style.Secondary) {
                editPoint.setCenter(getCenter());
                continue;
            }
            Shape parent = editPointParents.get(editPoint).a();
            String type = editPointParents.get(editPoint).b();

            // Rotate points
            if (editPoint.style == EditPoint.Style.Tertiary) {
                // Align the rotate points with the corners and then move them outwards
                Point2D closestPoint = new Point2D(Double.MAX_VALUE, Double.MAX_VALUE);
                if (parent instanceof LensLine line) {
                    if (type.equals("start")) {
                        double angleToStartpoint = Math.atan2(line.getStartY() - centerY, line.getStartX() - centerX);
                        closestPoint = new Point2D(line.getStartX()+20 * Math.cos(angleToStartpoint), line.getStartY()+20 * Math.sin(angleToStartpoint));
                    }
                    else {
                        double angleToEndpoint = Math.atan2(line.getEndY() - centerY, line.getEndX() - centerX);
                        closestPoint = new Point2D(line.getEndX()+20 * Math.cos(angleToEndpoint), line.getEndY()+20 * Math.sin(angleToEndpoint));
                    }
                }

                editPoint.setCenter(closestPoint);
            }

            // Arc points
            else if (editPoint.style == EditPoint.Style.Quaternary) {
                // Align the point to the vertex of the arc
                if (parent instanceof LensArc arc) {
                    editPoint.setCenter(arc.getVertex());
                }
            }

            // Scale points
            else {
                // Align the scale points with the corners
                Point2D closestPoint = new Point2D(Double.MAX_VALUE, Double.MAX_VALUE);
                if (parent instanceof LensLine line) {
                    if (type.equals("start")) {
                        closestPoint = new Point2D(line.getStartX(), line.getStartY());
                    }
                    else {
                        closestPoint = new Point2D(line.getEndX(), line.getEndY());
                    }
                }

                editPoint.setCenter(closestPoint);
            }

        }
    }

    @Override
    public void setHasBeenClicked(boolean hasBeenClicked) {
        this.hasBeenClicked = hasBeenClicked;
    }

    @Override
    public void delete() {
        coefficientASlider.hide();
        coefficientBSlider.hide();
        transparencySlider.hide();
        editPoints.removeAll(objectEditPoints);
        editedShape = null;
        lenses.remove(this);
        root.getChildren().remove(group);
    }

    @Override
    public void copy() {
        SphericalLens newLens = new SphericalLens(height, width, centerX, centerY, coefficientA, coefficientB, arc1Vertex, arc2Vertex);
        newLens.create();
        newLens.moveBy(10, 10);
        lenses.add(newLens);
        UserControls.closeCurrentEdit();
        newLens.openObjectEdit();
    }

    @Override
    public void moveBy(double x, double y) {
        for (Shape element : elements) {
            if (element instanceof LensArc arc) {
                arc.move(x, y);
            }
            else if (element instanceof LensLine line) {
                line.move(x, y);
            }
        }
        centerX += x;
        centerY += y;
        alignEditPoints();
        SaveState.autoSave();
    }

    @Override
    public boolean getHasBeenClicked() {
        return hasBeenClicked;
    }

    @Override
    public boolean intersectsMouseHitbox() {
        Shape bounds = new Rectangle(0.0, 0.0, 0.0, 0.0);
        for (Shape element : elements) {
            if (element instanceof LensArc arc) {
                bounds = Shape.union(bounds, arc);
            }
        }

        bounds = Shape.union(bounds, getMiddleBounds());
        return Shape.intersect(bounds, mouseHitbox).getLayoutBounds().getWidth() != -1;
    }

    @Serial
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeDouble(arc1Vertex.getX());
        out.writeDouble(arc1Vertex.getY());
        out.writeDouble(arc2Vertex.getX());
        out.writeDouble(arc2Vertex.getY());
    }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        arc1Vertex = new Point2D(in.readDouble(), in.readDouble());
        arc2Vertex = new Point2D(in.readDouble(), in.readDouble());

        // Initialize transient fields
        group = new Group();
        elements = new ArrayList<>();
        objectEditPoints = new ArrayList<>();
        editPointParents = new HashMap<>();
        isEdited = false;
        hasBeenClicked = false;
        focalLength = calculateFocalLength();
        focalPoint = calculateFocalPoint();
    }

    public Point2D getOppositePoint(Point2D point, Point2D center) {
        // Calculate the vector from the center of the lens to the given point
        double vectorX = point.getX() - center.getX();
        double vectorY = point.getY() - center.getY();

        // Multiply this vector by -1 to get the vector in the opposite direction
        double oppositeVectorX = -vectorX;
        double oppositeVectorY = -vectorY;

        // Add this new vector to the center of the lens to get the opposite point
        double oppositePointX = center.getX() + oppositeVectorX;
        double oppositePointY = center.getY() + oppositeVectorY;

        return new Point2D(oppositePointX, oppositePointY);
    }

    public Shape getMiddleBounds() {
        List<Double> points = new ArrayList<>();
        for (Shape element : elements) {
            if (element instanceof LensLine line) {
                points.add(line.getStartX());
                points.add(line.getStartY());
                points.add(line.getEndX());
                points.add(line.getEndY());
            }
        }
        return new Polygon(points.stream().mapToDouble(d -> d).toArray());
    }

    // TODO: Implement visible focal point
    public static Point2D calculateFocalPoint() {
        return null;
    }

    public double calculateFocalLength() {
        return 0;
    }

    public Point2D getCenter() {
        return new Point2D(centerX, centerY);
    }
    public double getCoefficientA() {
        return coefficientA;
    }
    public double getCoefficientB() {
        return coefficientB;
    }
    public void setCoefficientA(double coeficientA) {
        this.coefficientA = coeficientA;
    }
    public void setCoefficientB(double coeficientB) {
        this.coefficientB = coeficientB;
    }
    public double getAngleOfRotation() {return angleOfRotation;}
    public double getCenterX() {return centerX;}
    public double getCenterY() {return centerY;}
    public double getHeight() {return height;}
    public double getWidth() {return width;}


    public double getTransparency() {
        return transparency;
    }

    public void setTransparency(double transparency) {
        this.transparency = transparency;
    }
}
