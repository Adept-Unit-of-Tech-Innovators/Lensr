package com.example.lensr.objects;

import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Line;
import javafx.scene.shape.Shape;

import java.util.ArrayList;
import java.util.List;

import static com.example.lensr.Intersections.rotatePointAroundOtherByAngle;
import static com.example.lensr.LensrStart.*;

public class SphericalLens {

    public class LensLine extends Line {
        private SphericalLens parentLens;

        public LensLine(SphericalLens parentLens) {
            this.parentLens = parentLens;
        }

        public SphericalLens getParentLens() {
            return parentLens;
        }
        public void rotateAroundPointByAngle(Point2D staticPoint, double angle)
        {
            Point2D startPoint = rotatePointAroundOtherByAngle(new Point2D(getStartX(), getStartY()), staticPoint, angle);
            Point2D endPoint = rotatePointAroundOtherByAngle(new Point2D(getEndX(), getEndY()), staticPoint, angle);

            setStartX(startPoint.getX());
            setStartY(startPoint.getY());
            setEndX(endPoint.getX());
            setEndY(endPoint.getY());
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
        public void rotateAroundPointByAngle(Point2D staticPoint, double angle)
        {
            Point2D centerPoint = rotatePointAroundOtherByAngle(new Point2D(getCenterX(), getCenterY()), staticPoint, angle);

            setCenterX(centerPoint.getX());
            setCenterY(centerPoint.getY());

            setStartAngle(getStartAngle() + Math.toDegrees(angle));
        }
    }

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

    private double refractiveIndex;
    private double focalLength;

    private double closerCurvatureRadius;
    private double furtherCurvatureRadius;

    private Point2D focalPoint;

    public SphericalLens(double middleHeight, double middleWidth, double centerX, double centerY, double lensThickness, double refractiveIndex) {
        this.refractiveIndex = refractiveIndex;

        this.middleHeight = middleHeight;
        this.middleWidth = middleWidth;
        this.centerX = centerX;
        this.centerY = centerY;

        firstArc = new LensArc(this, lensThickness);
        secondArc = new LensArc(this, lensThickness);

        topLine = new LensLine(this);
        bottomLine = new LensLine(this);

        elements.add(firstArc);
        elements.add(secondArc);
        elements.add(topLine);
        elements.add(bottomLine);
        resize(lensThickness, middleWidth, middleHeight);
    }

    public void create() {
        root.getChildren().addAll(firstArc, secondArc, topLine, bottomLine);
    }

    public static Point2D calculateFocalPoint() {
        return null;
    }

    public double calculateFocalLength() {

        return 1 / ((refractiveIndex - 1) * (1 / firstArc.getRadiusY() - 1 / secondArc.getRadiusY() + (((refractiveIndex - 1) * middleWidth) / refractiveIndex * firstArc.getRadiusY() * secondArc.getRadiusY())));
    }

    public void arcAdjust(LensArc arc, double lensThickness, boolean isRightOriented) {
        arc.setType(ArcType.OPEN);
        arc.setFill(Color.TRANSPARENT);
        arc.setStroke(mirrorColor);
        arc.setStrokeWidth(globalStrokeWidth);
        int multiplier = 1;
        if (isRightOriented) {
            multiplier = -1;
        }

        double radius = (Math.pow(middleHeight, 2) / (8 * lensThickness) + lensThickness / 2);
        arc.setRadiusX(radius);
        arc.setRadiusY(radius);

        arc.setCenterX(centerX + (-middleWidth / 2 + radius - lensThickness) * multiplier);
        arc.setCenterY(centerY);

        double angleInRadians = 2 * Math.asin(middleHeight / (2 * radius));
        double angleInDegrees = Math.toDegrees(angleInRadians);

        arc.setStartAngle(180 - angleInDegrees / 2);
        if (isRightOriented) arc.setStartAngle(-angleInDegrees / 2);

        arc.setLength(angleInDegrees);
//        arc.updateArcBounds();


//        System.out.println("Radius: " + radius);
//        System.out.println("ArcX: " + arc.getCenterX());
//        System.out.println("ArcY: " + arc.getCenterY());
//        System.out.println("ArcLength: " + arc.getLength());
//        System.out.println("Rotation: " + arc.getRotate());
//        System.out.println("Start Angle: " + arc.getStartAngle());
//        System.out.println();


    }

    public void resize(double lensThickness, double newWidth, double newHeight) {
        middleHeight = newHeight;
        middleWidth = newWidth;

        arcAdjust(firstArc, lensThickness, false);
        arcAdjust(secondArc, lensThickness, true);

        topLine.setStartX(centerX - (middleWidth / 2));
        topLine.setStartY(centerY - (middleHeight / 2));
        topLine.setEndX(centerX + (middleWidth / 2));
        topLine.setEndY(centerY - (middleHeight / 2));
        topLine.setStroke(mirrorColor);

        bottomLine.setStartX(centerX - (middleWidth / 2));
        bottomLine.setStartY(centerY + (middleHeight / 2));
        bottomLine.setEndX(centerX + (middleWidth / 2));
        bottomLine.setEndY(centerY + (middleHeight / 2));
        bottomLine.setStroke(mirrorColor);
    }
    public Point2D getCenter()
    {
        return new Point2D(centerX, centerY);
    }

    public void rotate(double angle)
    {
        double angleToAdd = angle - angleOfRotation;

        Point2D centerPoint = new Point2D(centerX, centerY);

        topLine.rotateAroundPointByAngle(centerPoint, angleToAdd);
        bottomLine.rotateAroundPointByAngle(centerPoint, angleToAdd);
        firstArc.rotateAroundPointByAngle(centerPoint, angleToAdd);
        secondArc.rotateAroundPointByAngle(centerPoint, angleToAdd);

        angleOfRotation = angle;
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


}
