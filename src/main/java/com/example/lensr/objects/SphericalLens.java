package com.example.lensr.objects;

import com.example.lensr.EditPoint;
import com.example.lensr.MirrorMethods;
import com.example.lensr.UserControls;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static com.example.lensr.Intersections.rotatePointAroundOtherByAngle;
import static com.example.lensr.LensrStart.*;

public class SphericalLens extends Group implements Editable, Serializable {

    private transient Group group = new Group();
    private transient List<EditPoint> objectEditPoints = new ArrayList<>();
    private transient List<EditPoint> arcEditPoints = new ArrayList<>();
    private transient EditPoint upperRotateField;
    private transient EditPoint lowerRotateField;
    private transient boolean isEdited;
    private transient boolean hasBeenClicked;
    private double middleHeight;
    private double middleWidth;
    private double angleOfRotation = Math.PI/4;
    private double centerX;
    private double centerY;
    private double lensThickness1;
    private double lensThickness2;
    private transient LensArc firstArc;
    private transient LensArc secondArc;
    private transient LensLine topLine;
    private transient LensLine bottomLine;
    public transient List<Shape> elements = new ArrayList<>();

    private double coeficientA;
    private double coeficientB;
    private double focalLength;
    private transient Point2D focalPoint;

    public SphericalLens(double middleHeight, double middleWidth, double centerX, double centerY, double lensThickness1, double lensThickness2, double coeficientA, double coeficientB) {
        this.middleHeight = middleHeight;
        this.middleWidth = middleWidth;
        this.centerX = centerX;
        this.centerY = centerY;
        this.lensThickness1 = lensThickness1;
        this.lensThickness2 = lensThickness2;
        this.coeficientA = coeficientA;
        this.coeficientB = coeficientB;
    }

    @Override
    public void create() {
        firstArc = new LensArc(this, lensThickness1);
        secondArc = new LensArc(this, lensThickness2);

        topLine = new LensLine(this);
        bottomLine = new LensLine(this);

        elements.add(firstArc);
        elements.add(secondArc);
        elements.add(topLine);
        elements.add(bottomLine);

        resize(centerX, centerY, middleWidth, middleHeight, angleOfRotation);

        for (Shape element : elements) {
            element.setStroke(mirrorColor);
            element.setStrokeWidth(globalStrokeWidth);
        }

        // Setup positions for all different edit points

        // Corner points
        objectEditPoints.add(new EditPoint(topLine.getStartX(), topLine.getStartY()));
        objectEditPoints.add(new EditPoint(topLine.getEndX(), topLine.getEndY()));
        objectEditPoints.add(new EditPoint(bottomLine.getEndX(), bottomLine.getEndY()));
        objectEditPoints.add(new EditPoint(bottomLine.getStartX(), bottomLine.getStartY()));

        // Center point
        objectEditPoints.add(new EditPoint(getCenterX(), getCenterY()));

        // Rotate fields
        upperRotateField = new EditPoint(0 ,0);
        upperRotateField.setFill(Color.TRANSPARENT);
        upperRotateField.setStroke(Color.TRANSPARENT);

        lowerRotateField = new EditPoint(0, 0);
        lowerRotateField.setFill(Color.TRANSPARENT);
        lowerRotateField.setStroke(Color.TRANSPARENT);

        // Arc edit points
        arcEditPoints.add(new EditPoint(firstArc.getVertex()));
        arcEditPoints.add(new EditPoint(secondArc.getVertex()));

        arcEditPoints.get(0).setFill(Color.HOTPINK);
        arcEditPoints.get(1).setFill(Color.HOTPINK);

        // Set what will happen after clicking an edit point
        for (EditPoint editPoint : objectEditPoints) {
            editPoint.setOnClickEvent(event -> {
                EditPoint oppositeEditPoint = objectEditPoints.get(((objectEditPoints.indexOf(editPoint)) + 2)  % 4);
                scale(oppositeEditPoint.getCenter());
            });
        }

        objectEditPoints.get(4).setOnClickEvent(event -> move());

        upperRotateField.setOnClickEvent(event -> rotate());
        lowerRotateField.setOnClickEvent(event -> rotate());

        arcEditPoints.get(0).setOnClickEvent(event -> scaleArc(firstArc));
        arcEditPoints.get(1).setOnClickEvent(event -> scaleArc(secondArc));

        objectEditPoints.add(upperRotateField);
        objectEditPoints.add(lowerRotateField);
        objectEditPoints.addAll(arcEditPoints);

        // Hide all edit points
        objectEditPoints.forEach(editPoint -> editPoint.setVisible(false));
        arcEditPoints.forEach(editPoint -> editPoint.setVisible(false));

        // Add everything to the group
        group.getChildren().add(this);
        group.getChildren().addAll(elements);
        group.getChildren().addAll(objectEditPoints);
        root.getChildren().add(group);
    }

    // TODO: Implement visible focal point
    public static Point2D calculateFocalPoint() {
        return null;
    }

    public double calculateFocalLength() {
            return 0;
//        return 1 / ((refractiveIndex - 1) * (1 / firstArc.getRadiusY() - 1 / secondArc.getRadiusY() + (((refractiveIndex - 1) * middleWidth) / refractiveIndex * firstArc.getRadiusY() * secondArc.getRadiusY())));
    }

    public void resize(double newCenterX, double newCenterY, double newWidth, double newHeight, double newAngleOfRotation) {
        centerX = newCenterX;
        centerY = newCenterY;
        middleHeight = newHeight;
        middleWidth = newWidth;
        angleOfRotation = newAngleOfRotation;

        firstArc.adjust();
        secondArc.adjust();

        topLine.adjust();
        bottomLine.adjust();
    }

    public void resize(double newCenterX, double newCenterY, double newWidth, double newHeight) {resize(newCenterX, newCenterY, newWidth, newHeight, angleOfRotation);}

    public void resize(double newAngleOfRotation) {resize(centerX, centerY, middleWidth, middleHeight, newAngleOfRotation);}

    public void resize(double newCenterX, double newCenterY) {resize(newCenterX, newCenterY, middleWidth, middleHeight, angleOfRotation);}

    @Override
    public void openObjectEdit() {
        // Setup the sliders
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
        adjustRotateField();

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
        MirrorMethods.updateLightSources();
    }

    public void move() {
        taskPool.execute(() -> {
            while (isMousePressed) {
                double x = mousePos.getX();
                double y = mousePos.getY();

                // Update the UI on the JavaFX application thread
                Platform.runLater(() -> {
                    resize(x,y);
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

        });
    }

    public void scale(Point2D anchor) {
        taskPool.execute(() -> {
            double newCenterX, newCenterY, newWidth, newHeight;

            while (isMousePressed) {
                if (altPressed && shiftPressed) {
                    newCenterX = anchor.getX();
                    newCenterY = anchor.getY();

                    double ratio = middleHeight/middleWidth;

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

                    double ratio = middleHeight/middleWidth;

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
                    resize(finalCenterX, finalCenterY, finalWidth, finalHeight);

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


        });
    }

    public void rotate() {
        taskPool.execute(() -> {
            double newAngleOfRotation;

            double mouseAngle = Math.atan2(centerY - mousePos.getY(), centerX - mousePos.getX());
            while (isMousePressed)
            {
                // Get the angle between center of lens and position of the mouse

                newAngleOfRotation = Math.atan2(centerY - mousePos.getY(), centerX - mousePos.getX()) - mouseAngle;
                double finalAngleOfRotation = newAngleOfRotation;

                Platform.runLater(() -> {
                    resize(finalAngleOfRotation);

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


        });
    }

    public void scaleArc(LensArc arc) {
        taskPool.execute(() -> {
            double newThickness;
            while (isMousePressed)
            {
                // "Unrotate" position of the mouse around the center of the lens, so we can easily get distance between mouse and center on the rotated X axis
                Point2D unrotatedMousePos = rotatePointAroundOtherByAngle(mousePos, getCenter(), -angleOfRotation);
                newThickness = (arc == secondArc ? unrotatedMousePos.getX() - centerX + middleWidth/2 : centerX - unrotatedMousePos.getX() - middleWidth * 3/2);

                double finalThickness = newThickness;
                if (arc == firstArc) lensThickness1 = finalThickness;
                else lensThickness2 = finalThickness;

                Platform.runLater(() -> {
                    arc.setThickness(finalThickness);

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
        });
    }

    @Serial
    private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
        out.defaultWriteObject();
    }

    @Serial
    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
        in.defaultReadObject();

        // Initialize transient fields
        group = new Group();
        objectEditPoints = new ArrayList<>();
        arcEditPoints = new ArrayList<>();
        upperRotateField = new EditPoint(0 ,0);
        lowerRotateField = new EditPoint(0, 0);
        isEdited = false;
        hasBeenClicked = false;
        firstArc = new LensArc(this, lensThickness1);
        secondArc = new LensArc(this, lensThickness2);
        topLine = new LensLine(this);
        bottomLine = new LensLine(this);
        elements = new ArrayList<>();
        elements.add(firstArc);
        elements.add(secondArc);
        elements.add(topLine);
        elements.add(bottomLine);
        focalLength = calculateFocalLength();
    }

    public void alignEditPoints() {
        objectEditPoints.get(0).setCenter(topLine.getStart());
        objectEditPoints.get(1).setCenter(topLine.getEnd());
        objectEditPoints.get(2).setCenter(bottomLine.getEnd());
        objectEditPoints.get(3).setCenter(bottomLine.getStart());
        objectEditPoints.get(4).setCenter(getCenter());

        adjustRotateField();

        arcEditPoints.get(0).setCenter(firstArc.getVertex());
        arcEditPoints.get(1).setCenter(secondArc.getVertex());
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
        SphericalLens newLens = new SphericalLens(middleHeight, middleWidth, centerX, centerY, firstArc.getThickness(), secondArc.getThickness(), coeficientA, coeficientB);
        newLens.create();
        newLens.moveBy(10, 10);
        lenses.add(newLens);
        UserControls.closeCurrentEdit();
        newLens.openObjectEdit();
    }

    @Override
    public void moveBy(double x, double y) {
        resize(centerX + x, centerY + y);
    }

    @Override
    public boolean getHasBeenClicked() {
        return hasBeenClicked;
    }

    @Override
    public boolean intersectsMouseHitbox() {
        Shape bounds = Shape.union(firstArc, secondArc);
        bounds = Shape.union(bounds, getMiddleBounds());

        return Shape.intersect(bounds, mouseHitbox).getLayoutBounds().getWidth() != -1;
    }

    public void adjustRotateField() {
        upperRotateField.setWidth(middleWidth);
        upperRotateField.setRotate(Math.toDegrees(angleOfRotation));
        double rotateFieldHeight = 50;
        upperRotateField.setCenter(new Point2D(centerX + (middleHeight/2 + rotateFieldHeight /4) * Math.cos(angleOfRotation - Math.PI/2), centerY + (middleHeight/2 + rotateFieldHeight /4) * Math.sin(angleOfRotation - Math.PI/2)));

        lowerRotateField.setWidth(middleWidth);
        lowerRotateField.setCenter(new Point2D(centerX + (middleHeight/2 + rotateFieldHeight /4) * Math.cos(angleOfRotation + Math.PI/2), centerY + (middleHeight/2 + rotateFieldHeight /4) * Math.sin(angleOfRotation + Math.PI/2)));
        lowerRotateField.setRotate(Math.toDegrees(angleOfRotation));

    }

    public Shape getMiddleBounds() {
        return new Polygon(topLine.getStartX(), topLine.getStartY(), topLine.getEndX(), topLine.getEndY(), bottomLine.getEndX(), bottomLine.getEndY(), bottomLine.getStartX(), bottomLine.getStartY());
    }

    public Point2D getCenter() {
        return new Point2D(centerX, centerY);
    }
    public double getCoeficientA() {
        return coeficientA;
    }
    public double getCoeficientB() {
        return coeficientB;
    }
    public void setCoefficientA(double coeficientA) {
        this.coeficientA = coeficientA;
    }
    public void setCoefficientB(double coeficientB) {
        this.coeficientB = coeficientB;
    }
    public LensArc getFirstArc() {return firstArc;}
    public LensArc getSecondArc() {return secondArc;}
    public LensLine getTopLine() {return topLine;}
    public LensLine getBottomLine() {return bottomLine;}
    public double getAngleOfRotation() {return angleOfRotation;}
    public double getCenterX() {return centerX;}
    public double getCenterY() {return centerY;}
    public double getMiddleHeight() {return middleHeight;}
    public double getMiddleWidth() {return middleWidth;}
}
