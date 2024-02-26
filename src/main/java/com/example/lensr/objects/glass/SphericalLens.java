package com.example.lensr.objects.glass;

import com.example.lensr.*;
import com.example.lensr.objects.Editable;
import com.example.lensr.saveloadkit.SaveState;
import com.example.lensr.ui.EditPoint;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;

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
    private transient double centerX, centerY, height, width, angleOfRotation;

    // Optical properties
    private transient double coefficientA, coefficientB, focalLength;
    private transient Point2D focalPoint;

    public SphericalLens(double middleHeight, double middleWidth, double centerX, double centerY, double coefficientA, double coefficientB) {
        this.height = middleHeight;
        this.width = middleWidth;
        this.centerX = centerX;
        this.centerY = centerY;
        this.coefficientA = coefficientA;
        this.coefficientB = coefficientB;
    }

    @Override
    public void create() {
        LensLine topLine = new LensLine(this, 0);
        LensLine bottomLine = new LensLine(this, Math.PI);
        topLine.scale();
        bottomLine.scale();

        LensArc firstArc = new LensArc(this, Map.of(bottomLine, "start", topLine, "start"), new Point2D(topLine.getStartX(), (topLine.getStartY() + bottomLine.getStartY())/2));
        LensArc secondArc = new LensArc(this, Map.of(topLine, "end", bottomLine, "end"), new Point2D(topLine.getEndX(), (topLine.getEndY() + bottomLine.getEndY())/2));

        elements.add(topLine);
        elements.add(bottomLine);
        elements.add(firstArc);
        elements.add(secondArc);

        resize(centerX, centerY, width, height, Math.toRadians(0));

        // Add the elements' edit points and set the elements' stroke
        for (Shape element : elements) {
            element.setStroke(mirrorColor);
            element.setStrokeWidth(globalStrokeWidth);
            if (element instanceof LensLine line) {
                EditPoint startEditPoint = new EditPoint(line.getStartX(), line.getStartY());
                EditPoint endEditPoint = new EditPoint(line.getEndX(), line.getEndY());
                double angleToStartPoint = Math.atan2(line.getStartY() - centerY, line.getStartX() - centerX);
                double angleToEndPoint = Math.atan2(line.getEndY() - centerY, line.getEndX() - centerX);
                EditPoint startRotatePoint = new EditPoint(line.getStartX() + 20 * Math.cos(angleToStartPoint), line.getStartY() + 20 * Math.sin(angleToStartPoint));
                EditPoint endRotatePoint = new EditPoint(line.getEndX() + 20 * Math.cos(angleToEndPoint), line.getEndY() + 20 * Math.sin(angleToEndPoint));

                // Set the scaling or rotating events for the edit points
                startEditPoint.setOnClickEvent(event -> scale(getOppositePoint(startEditPoint.getCenter(), getCenter())));
                endEditPoint.setOnClickEvent(event -> scale(getOppositePoint(endEditPoint.getCenter(), getCenter())));
                startRotatePoint.setOnClickEvent(event -> rotate());
                endRotatePoint.setOnClickEvent(event -> rotate());

                startRotatePoint.setFill(Color.BISQUE);
                endRotatePoint.setFill(Color.BISQUE);
                objectEditPoints.addAll(List.of(startEditPoint, endEditPoint, startRotatePoint, endRotatePoint));

                // Add the edit points to the map
                editPointParents.put(startEditPoint, new Tuple<>(line, "start"));
                editPointParents.put(endEditPoint, new Tuple<>(line, "end"));
                editPointParents.put(startRotatePoint, new Tuple<>(line, "start"));
                editPointParents.put(endRotatePoint, new Tuple<>(line, "end"));
            }
            if (element instanceof LensArc arc) {
                EditPoint arcVertex = new EditPoint(arc.getVertex());
                arcVertex.setFill(Color.HOTPINK);
                arcVertex.setOnClickEvent(event -> {
                    taskPool.execute(() -> {
                        while (isMousePressed) {
                            arc.scale(mousePos);
                            synchronized (lock) {
                                try {
                                    lock.wait(10); // Adjust the wait time as needed
                                } catch (InterruptedException e) {
                                    throw new RuntimeException("A thread was interrupted while waiting.");
                                }
                            }
                            alignEditPoints();
                        }
                    });

                });
                objectEditPoints.add(arcVertex);
                editPointParents.put(arcVertex, new Tuple<>(arc, "vertex"));
            }
        }

        // Center point
        EditPoint centerPoint = new EditPoint(getCenterX(), getCenterY());
        centerPoint.setFill(Color.BLUE);
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
        coefficientASlider.show();
        coefficientBSlider.show();

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

    public void resize(double newCenterX, double newCenterY, double newWidth, double newHeight, double newAngleOfRotation) {
        // Scale appropriately all the elements
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
                    arc.scale(arc.getVertex());
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
                if (altPressed && shiftPressed) {
                    newCenterX = anchor.getX();
                    newCenterY = anchor.getY();

                    double ratio = height / width;

                    newWidth = 2 * Math.abs(newCenterX - mousePos.getX()) / Math.cos(angleOfRotation);
                    newHeight = newWidth * ratio;
                }
                else if (altPressed) {
                    newCenterX = anchor.getX();
                    newCenterY = anchor.getY();

                    newWidth = 2 * Math.abs(newCenterX - mousePos.getX()) / ((angleOfRotation == 0) ? 1 : Math.cos(angleOfRotation));
                    newHeight = 2 * Math.abs(newCenterY - mousePos.getY()) / ((angleOfRotation == 0) ? 1 : Math.sin(angleOfRotation));
                }
                else if (shiftPressed) {
                    //TODO: Make this mf work
                    newCenterX = mousePos.getX() + (anchor.getX() - mousePos.getX()) / 2;
                    newCenterY = mousePos.getY() + (anchor.getY() - mousePos.getY()) / 2;

                    double ratio = height / width;

                    Point2D centerPoint = new Point2D(newCenterX, newCenterY);
                    Point2D unrotatedMousePos = rotatePointAroundOtherByAngle(mousePos, centerPoint, -angleOfRotation);
                    Point2D unrotatedAnchorPos = rotatePointAroundOtherByAngle(anchor, centerPoint, -angleOfRotation);

                    newWidth = Math.abs(unrotatedAnchorPos.getX() - unrotatedMousePos.getX());
                    newHeight = newWidth * ratio;

                    double middleX = anchor.getX() + ((anchor.getX() > mousePos.getX()) ? newWidth/2 : -newWidth/2) * Math.cos(angleOfRotation);
                    double middleY = anchor.getY() + ((anchor.getY() > mousePos.getY()) ? newWidth/2 : -newWidth/2) * Math.sin(angleOfRotation);

                    newCenterX = middleX + ((middleX > mousePos.getX()) ? newHeight/2 : -newHeight/2) * Math.cos(angleOfRotation + Math.PI/2);
                    newCenterY = middleY + ((middleY > mousePos.getY()) ? newHeight/2 : -newHeight/2) * Math.sin(angleOfRotation + Math.PI/2);
                }
                else {
                    newCenterX = mousePos.getX() + (anchor.getX() - mousePos.getX()) / 2;
                    newCenterY = mousePos.getY() + (anchor.getY() - mousePos.getY()) / 2;

                    Point2D centerPoint = new Point2D(newCenterX, newCenterY);
                    Point2D unrotatedMousePos = rotatePointAroundOtherByAngle(mousePos, centerPoint, -angleOfRotation);
                    Point2D unrotatedAnchorPos = rotatePointAroundOtherByAngle(anchor, centerPoint, -angleOfRotation);
                    
                    newWidth = Math.abs(unrotatedAnchorPos.getX() - unrotatedMousePos.getX());
                    newHeight = Math.abs(unrotatedAnchorPos.getY() - unrotatedMousePos.getY());
                }

                double finalCenterX = newCenterX;
                double finalCenterY = newCenterY;
                double finalWidth = newWidth;
                double finalHeight = newHeight;

                Platform.runLater(() -> {
                    resize(finalCenterX, finalCenterY, finalWidth, finalHeight, this.angleOfRotation);
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
                    resize(this.centerX, this.centerY, this.width, this.height, finalAngleOfRotation);
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
            if (editPoint.getFill() == Color.BLUE) {
                editPoint.setCenter(getCenter());
                continue;
            }
            Shape parent = editPointParents.get(editPoint).a();
            String type = editPointParents.get(editPoint).b();

            // Rotate points
            if (editPoint.getFill() == Color.BISQUE) {
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
            else if (editPoint.getFill() == Color.HOTPINK) {
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
        lenses.remove(this);
        root.getChildren().remove(group);
    }

    @Override
    public void copy() {
        SphericalLens newLens = new SphericalLens(height, width, centerX, centerY, coefficientA, coefficientB);
        newLens.create();
        newLens.moveBy(10, 10);
        lenses.add(newLens);
        UserControls.closeCurrentEdit();
        newLens.openObjectEdit();
    }

    @Override
    public void moveBy(double x, double y) {
        resize(centerX + x, centerY + y);
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
    }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

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


}