package com.example.lensr.objects;

import com.example.lensr.EditPoint;
import com.example.lensr.MirrorMethods;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;

import java.util.ArrayList;
import java.util.List;

import static com.example.lensr.Intersections.rotatePointAroundOtherByAngle;
import static com.example.lensr.LensrStart.*;

public class SphericalLens extends Group implements Editable {

    List<EditPoint> objectEditPoints = new ArrayList<>();
    List<EditPoint> arcEditPoints = new ArrayList<>();
    public Shape rotateField;
    public EditPoint rotatePoint;
    public boolean isEdited;
    public boolean hasBeenClicked;
    private double middleHeight;
    private double middleWidth;
    private double angleOfRotation;
    private double centerX;
    private double centerY;
    private final LensArc firstArc;
    private final LensArc secondArc;
    private final LensLine topLine;
    private final LensLine bottomLine;
    public List<Shape> elements = new ArrayList<>();

    private double refractiveIndex;
    private double focalLength;

    private Point2D focalPoint;

    public SphericalLens(double middleHeight, double middleWidth, double centerX, double centerY, double lensThickness, double refractiveIndex) {
        this.refractiveIndex = refractiveIndex;

        firstArc = new LensArc(this, lensThickness);
        secondArc = new LensArc(this, lensThickness);

        topLine = new LensLine(this);
        bottomLine = new LensLine(this);

        elements.add(firstArc);
        elements.add(secondArc);
        elements.add(topLine);
        elements.add(bottomLine);

        resize(centerX, centerY, middleWidth, middleHeight,Math.toRadians(45));
    }

    public void create() {
        for (Shape element : elements) {
            element.setStroke(mirrorColor);
            element.setStrokeWidth(globalStrokeWidth);
        }

        getChildren().addAll(elements);
        root.getChildren().addAll(this);
    }

    // TODO: Implement visible focal point
    public static Point2D calculateFocalPoint() {
        return null;
    }

    public double calculateFocalLength() {

        return 1 / ((refractiveIndex - 1) * (1 / firstArc.getRadiusY() - 1 / secondArc.getRadiusY() + (((refractiveIndex - 1) * middleWidth) / refractiveIndex * firstArc.getRadiusY() * secondArc.getRadiusY())));
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
    @Override
    public void openObjectEdit()
    {
        refractiveIndexSlider.setCurrentSource(this);
        refractiveIndexSlider.show();

        hasBeenClicked = true;
        isEdited = true;

        // Defocus the text field
        root.requestFocus();

        // Setup positions for all different edit points
        objectEditPoints.add(new EditPoint(topLine.getStartX(), topLine.getStartY()));
        objectEditPoints.add(new EditPoint(topLine.getEndX(), topLine.getEndY()));
        objectEditPoints.add(new EditPoint(bottomLine.getEndX(), bottomLine.getEndY()));
        objectEditPoints.add(new EditPoint(bottomLine.getStartX(), bottomLine.getStartY()));

//        rotatePoint = new EditPoint(centerX + (middleHeight + 50) * Math.cos(angleOfRotation - Math.PI/2), centerY + (middleHeight + 50) * Math.sin(angleOfRotation - Math.PI/2));
//        rotatePoint.setFill(Color.PURPLE);

        rotateField = createRotateField();
        rotateField.setFill(Color.TRANSPARENT); // TODO: actual code by franio
        root.getChildren().add(rotateField);

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

//        rotatePoint.setOnClickEvent(event -> rotate());

        rotateField.setOnMouseClicked(mouseEvent -> rotate());

        arcEditPoints.get(0).setOnClickEvent(event -> scaleArc(firstArc));
        arcEditPoints.get(1).setOnClickEvent(event -> scaleArc(secondArc));

        editPoints.addAll(objectEditPoints);
        getChildren().addAll(objectEditPoints);

//        editPoints.add(rotatePoint);
//        getChildren().add(rotatePoint);

        editPoints.addAll(arcEditPoints);
        getChildren().addAll(arcEditPoints);

        editedShape = this;
    }
    @Override
    public void closeObjectEdit() {
        refractiveIndexSlider.hide();
        isEdited = false;
        if(objectEditPoints != null && editedShape instanceof Group editedGroup)
        {
            editedGroup.getChildren().removeAll(objectEditPoints);
            editPoints.removeAll(objectEditPoints);
            objectEditPoints.clear();

//            editedGroup.getChildren().remove(rotatePoint);
//            editPoints.remove(rotatePoint);

            editedGroup.getChildren().removeAll(arcEditPoints);
            editPoints.removeAll(arcEditPoints);
            arcEditPoints.clear();
        }

        editedShape = null;
        MirrorMethods.updateLightSources();
    }

    public void scale(Point2D anchor)
    {
        new Thread(() -> {
            double newCenterX, newCenterY, newWidth, newHeight;

            while (isMousePressed)
            {
                if(altPressed && shiftPressed) // Scale along center & preserve ratio
                {
                    newCenterX = anchor.getX();
                    newCenterY = anchor.getY();

                    double ratio = middleHeight/middleWidth;

                    newWidth = 2 * Math.abs(newCenterX - mousePos.getX()) / Math.cos(angleOfRotation);
                    newHeight = newWidth * ratio;
                }
                else if (altPressed) { // Scale along center
                    newCenterX = anchor.getX();
                    newCenterY = anchor.getY();

                    newWidth = 2 * Math.abs(newCenterX - mousePos.getX()) / ((angleOfRotation == 0) ? 1 : Math.cos(angleOfRotation));
                    newHeight = 2 * Math.abs(newCenterY - mousePos.getY()) / ((angleOfRotation == 0) ? 1 : Math.sin(angleOfRotation));
                }
                else if (shiftPressed) { // Scale with preserving width to height ratio
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

                    alignEditPoints(finalCenterX, finalCenterY, finalHeight, angleOfRotation);
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

    public void rotate()
    {
        new Thread(() -> {
            double newAngleOfRotation;

            double mouseAngle = Math.atan2(centerY - mousePos.getY(), centerX - mousePos.getX());
            while (isMousePressed)
            {
                // Get the angle between center of lens and position of the mouse
//                newAngleOfRotation = Math.atan2(centerY - mousePos.getY(), centerX - mousePos.getX()) - Math.PI/2;

                newAngleOfRotation = Math.atan2(centerY - mousePos.getY(), centerX - mousePos.getX()) - mouseAngle;
                double finalAngleOfRotation = newAngleOfRotation;

                Platform.runLater(() -> {
                    resize(finalAngleOfRotation);

                    alignEditPoints(centerX, centerY, middleHeight, finalAngleOfRotation);
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

    public void scaleArc(LensArc arc)
    {
        new Thread(() -> {
            double newThickness;
            while (isMousePressed)
            {
                // "Unrotate" position of the mouse around the center of the lens, so we can easily get distance between mouse and center on the rotated X axis
                Point2D unrotatedMousePos = rotatePointAroundOtherByAngle(mousePos, getCenter(), -angleOfRotation);
                newThickness = (arc == secondArc ? unrotatedMousePos.getX() - centerX + middleWidth/2 : centerX - unrotatedMousePos.getX() - middleWidth * 3/2);

                double finalThickness = newThickness;

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
        }).start();

    }

    public void alignEditPoints() {alignEditPoints(centerX, centerY, middleHeight, angleOfRotation);}
    public void alignEditPoints(double finalCenterX, double finalCenterY, double finalHeight, double finalAngleOfRotation)
    {
//        rotatePoint.setCenterX(finalCenterX + (finalHeight/2 + 50) * Math.cos(finalAngleOfRotation - Math.PI/2));
//        rotatePoint.setCenterY(finalCenterY + (finalHeight/2 + 50) * Math.sin(finalAngleOfRotation - Math.PI/2));

        rotateField = createRotateField();

        objectEditPoints.get(0).setCenter(topLine.getStart());
        objectEditPoints.get(1).setCenter(topLine.getEnd());
        objectEditPoints.get(2).setCenter(bottomLine.getEnd());
        objectEditPoints.get(3).setCenter(bottomLine.getStart());

        arcEditPoints.get(0).setCenter(firstArc.getVertex());
        arcEditPoints.get(1).setCenter(secondArc.getVertex());
    }

    @Override
    public void setHasBeenClicked(boolean hasBeenClicked) {
        this.hasBeenClicked = hasBeenClicked;
    }

    @Override
    public void delete() {

    }

    @Override
    public void copy() {

    }

    @Override
    public void moveBy(double x, double y) {

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
    public Shape createRotateField()
    {
        Shape middleBounds = getMiddleBounds();

//        middleBounds.setScaleX(1.05);
        middleBounds.setScaleY(2.05);
        middleBounds.setFill(Color.TRANSPARENT);

        return Shape.subtract(middleBounds, getMiddleBounds());
    }
    public Shape getMiddleBounds() {
        return new Polygon(topLine.getStartX(), topLine.getStartY(), topLine.getEndX(), topLine.getEndY(), bottomLine.getEndX(), bottomLine.getEndY(), bottomLine.getStartX(), bottomLine.getStartY());
    }

    public Point2D getCenter()
    {
        return new Point2D(centerX, centerY);
    }
    public double getRefractiveIndex() {
        return refractiveIndex;
    }
    public void setRefractiveIndex(double refractiveIndex)
    {
        this.refractiveIndex = refractiveIndex;
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
