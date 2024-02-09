package com.example.lensr.objects;

import com.example.lensr.EditPoint;
import com.example.lensr.MirrorMethods;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.effect.Light;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;

import java.util.ArrayList;
import java.util.List;

import static com.example.lensr.Intersections.rotatePointAroundOtherByAngle;
import static com.example.lensr.LensrStart.*;

public class SphericalLens implements Editable{

    public class LensLine extends Line {
        private SphericalLens parentLens;

        public LensLine(SphericalLens parentLens) {
            this.parentLens = parentLens;
        }

        public SphericalLens getParentLens() {
            return parentLens;
        }
        public void adjust()
        {
            int multiplier = 1;
            if(this == bottomLine) multiplier = -1;

            double middleTopX = centerX + (middleHeight/2 * Math.cos(angleOfRotation + Math.PI/2)) * multiplier;
            double middleTopY = centerY + (middleHeight/2 * Math.sin(angleOfRotation + Math.PI/2)) * multiplier;

            setStartX(middleTopX - middleWidth/2 * Math.cos(angleOfRotation));
            setStartY(middleTopY - middleWidth/2 * Math.sin(angleOfRotation));

            setEndX(middleTopX + middleWidth/2 * Math.cos(angleOfRotation));
            setEndY(middleTopY + middleWidth/2 * Math.sin(angleOfRotation));
        }

        public Point2D getStart()
        {
            return new Point2D(getStartX(), getStartY());
        }
        public Point2D getEnd()
        {
            return new Point2D(getEndX(), getEndY());
        }
    }

    public class LensArc extends Arc {
        private double thickness;
        private SphericalLens parentLens;

        public LensArc(SphericalLens parentLens, double thickness) {
            this.parentLens = parentLens;
            this.thickness = thickness;
        }

        public Line getChord() {
            Line chord = new Line(topLine.getEndX(), topLine.getEndY(), bottomLine.getEndX(), bottomLine.getEndY());
            if (this == firstArc)
                chord = new Line(topLine.getStartX(), topLine.getStartY(), bottomLine.getStartX(), bottomLine.getStartY());
            return chord;
        }
        public Point2D getMiddle()
        {
            return new Point2D(getCenterX(), getCenterY());
        }
        public SphericalLens getParentLens() {
            return parentLens;
        }
        public void adjust()
        {
            setType(ArcType.OPEN);
            setFill(Color.TRANSPARENT);

            int multiplier = 1;
            if (this == secondArc) multiplier = -1;

            double radius = Math.pow(middleHeight, 2) / (8 * thickness) + thickness / 2;

            setRadiusX(radius);
            setRadiusY(radius);

            setCenterX(centerX + (radius - middleWidth/2 - thickness) * Math.cos(angleOfRotation) * multiplier);
            setCenterY(centerY + (radius - middleWidth/2 - thickness) * Math.sin(angleOfRotation) * multiplier);

            double angleInRadians = 2 * Math.asin(middleHeight / (2 * radius));

            double angleInDegrees = Math.toDegrees(angleInRadians);

            if(radius < thickness) angleInDegrees = 360 - angleInDegrees;


            setStartAngle(180 - angleInDegrees / 2 - Math.toDegrees(angleOfRotation));
            if (this == secondArc) setStartAngle(-angleInDegrees / 2 - Math.toDegrees(angleOfRotation));

            System.out.println(angleInDegrees);
            setLength(angleInDegrees);
        }
    }

    public Group group = new Group();
    List<EditPoint> objectEditPoints = new ArrayList<>();
    public boolean isEdited;
    public boolean hasBeenClicked;
    private double middleHeight;
    private double middleWidth;
    private double angleOfRotation;
    private double centerX;
    private double centerY;
    private LensArc firstArc;
    private LensArc secondArc;
    private LensLine topLine;
    private LensLine bottomLine;
    public List<Shape> elements = new ArrayList<>();
    public EditPoint rotatePoint;

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

        group.getChildren().addAll(elements);
        root.getChildren().addAll(group);
    }

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

        root.requestFocus();

        objectEditPoints.add(new EditPoint(topLine.getStartX(), topLine.getStartY()));
        objectEditPoints.add(new EditPoint(topLine.getEndX(), topLine.getEndY()));
        objectEditPoints.add(new EditPoint(bottomLine.getStartX(), bottomLine.getStartY()));
        objectEditPoints.add(new EditPoint(bottomLine.getEndX(), bottomLine.getEndY()));

        rotatePoint = new EditPoint(centerX + Math.max(middleHeight/2 * Math.cos(angleOfRotation - Math.PI/2), 50 * Math.cos(angleOfRotation - Math.PI/2)), centerY + Math.max(middleHeight/2 * Math.cos(angleOfRotation - Math.PI/2), 50 * Math.cos(angleOfRotation - Math.PI/2)));
        rotatePoint.setFill(Color.PURPLE);
        root.getChildren().add(rotatePoint);

        //TODO: Add separate edit points for arcs
        //TODO: Add edit point for rotation
        for (EditPoint editPoint : objectEditPoints) {
            editPoint.setOnClickEvent(event -> {
                EditPoint oppositeEditPoint = objectEditPoints.get(((objectEditPoints.indexOf(editPoint)) + 2)  % 4);
                scale(oppositeEditPoint.getCenter());

            });
        }
        rotatePoint.setOnClickEvent(event -> rotate());

        editPoints.addAll(objectEditPoints);
        group.getChildren().addAll(objectEditPoints);

        editPoints.add(rotatePoint);
        group.getChildren().add(rotatePoint);
        editedShape = group;
    }
    @Override
    public void closeObjectEdit() {
        System.out.println("edit closed");
        refractiveIndexSlider.hide();
        isEdited = false;
        if(objectEditPoints != null && editedShape instanceof Group editedGroup)
        {
            editedGroup.getChildren().removeAll(objectEditPoints);
            editPoints.removeAll(objectEditPoints);
            objectEditPoints.clear();
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
                if(altPressed && shiftPressed)
                {
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


                    rotatePoint.setCenterX(finalCenterX + Math.max(finalHeight * Math.cos(angleOfRotation - Math.PI/2), 50 * Math.cos(angleOfRotation - Math.PI/2)));
                    rotatePoint.setCenterY(finalCenterY + Math.max(finalHeight * Math.sin(angleOfRotation - Math.PI/2), 50 * Math.sin(angleOfRotation - Math.PI/2)));

                    objectEditPoints.get(0).setCenter(topLine.getStart());
                    objectEditPoints.get(1).setCenter(topLine.getEnd());
                    objectEditPoints.get(2).setCenter(bottomLine.getStart());
                    objectEditPoints.get(3).setCenter(bottomLine.getEnd());

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

            while (isMousePressed)
            {
                newAngleOfRotation = Math.atan2(centerY - mousePos.getY(), centerX - mousePos.getX());

                double finalAngleOfRotation = newAngleOfRotation;

                Platform.runLater(() -> {
                    resize(finalAngleOfRotation);

                    rotatePoint.setCenterX(centerX + Math.max(middleHeight * Math.cos(finalAngleOfRotation + Math.PI/2), 50 * Math.cos(finalAngleOfRotation + Math.PI/2)));
                    rotatePoint.setCenterY(centerY + Math.max(middleHeight * Math.sin(finalAngleOfRotation + Math.PI/2), 50 * Math.sin(finalAngleOfRotation + Math.PI/2)));

                    objectEditPoints.get(0).setCenter(topLine.getStart());
                    objectEditPoints.get(1).setCenter(topLine.getEnd());
                    objectEditPoints.get(2).setCenter(bottomLine.getStart());
                    objectEditPoints.get(3).setCenter(bottomLine.getEnd());
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
        return false;
    }

    public Point2D getCenter()
    {
        return new Point2D(centerX, centerY);
    }


    public LensArc getFirstArc() {
        return firstArc;
    }

    public LensArc getSecondArc() {
        return secondArc;
    }

    public Line getTopLine() {
        return topLine;
    }

    public Line getBottomLine() {
        return bottomLine;
    }

    public double getRefractiveIndex() {
        return refractiveIndex;
    }
    public void setRefractiveIndex(double refractiveIndex)
    {
        this.refractiveIndex = refractiveIndex;
    }


}
