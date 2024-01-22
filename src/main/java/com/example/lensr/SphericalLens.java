package com.example.lensr;

import javafx.geometry.Point2D;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Line;
import javafx.scene.shape.Shape;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.List;

import static com.example.lensr.LensrStart.*;

public class SphericalLens {
    public class LensLine extends Line{}
    public class LensArc extends Arc
    {
        private double thickness;
        private Shape bounds;

        public LensArc(double thickness) {
            this.thickness = thickness;
        }
        public Line getChord()
        {
            Line chord = new Line(topLine.getEndX(), topLine.getEndY(), bottomLine.getEndX(), bottomLine.getEndY());
            if(this == firstArc) chord = new Line(topLine.getStartX(), topLine.getStartY(), bottomLine.getStartX(), bottomLine.getStartY());
            return chord;
        }
        void updateArcBounds()
        {
            Shape newBounds = Intersections.getObjectOutline(this);
            Line chord = getChord();
            bounds = Shape.subtract(newBounds, chord);
        }
        public Shape getBounds()
        {
            return this.bounds;
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
    List<Shape> elements = new ArrayList<>();

    private double refractiveIndex;
    private double focalLength;

    private double closerCurvatureRadius;
    private double furtherCurvatureRadius;

    private Point2D focalPoint;
//TO DO:
    //Calculate arc's rotation angle, position and length depending on middleLength

    public SphericalLens(double middleHeight, double middleWidth, double centerX, double centerY, double lensThickness) {
        this.middleHeight = middleHeight;
        this.middleWidth = middleWidth;
        this.centerX = centerX;
        this.centerY = centerY;

        firstArc = new LensArc(lensThickness);
        secondArc = new LensArc(lensThickness);

        topLine = new LensLine();
        bottomLine = new LensLine();

        elements.add(firstArc);
        elements.add(secondArc);
        elements.add(topLine);
        elements.add(bottomLine);
        resize(lensThickness, middleWidth, middleHeight);
    }

    public static Point2D calculateFocalPoint()
    {
        return null;
    }
    public double calculateFocalLength()
    {

        return 1 / ((refractiveIndex - 1) * (1 / firstArc.getRadiusY() - 1 / secondArc.getRadiusY() + (((refractiveIndex - 1) * middleWidth) / refractiveIndex * firstArc.getRadiusY() * secondArc.getRadiusY())));
    }


    public void addToRoot()
    {
        root.getChildren().addAll(firstArc, secondArc, topLine, bottomLine);
    }

    public void arcAdjust(LensArc arc, double lensThickness, boolean isRightOriented)
    {
        arc.setType(ArcType.OPEN);
        arc.setFill(Color.TRANSPARENT);
        arc.setStroke(mirrorColor);
        arc.setStrokeWidth(globalStrokeWidth);
        int multiplier = 1;
        if(isRightOriented)
        {
            multiplier = -1;
        }

        double radius = (Math.pow(middleHeight, 2) / (8 * lensThickness) + lensThickness / 2);
        arc.setRadiusX(radius);
        arc.setRadiusY(radius);

        arc.setCenterX(centerX + (- middleWidth / 2 + radius - lensThickness) * multiplier);
        arc.setCenterY(centerY);

        double angleInRadians = 2 * Math.asin(middleHeight / (2*radius));
        double angleInDegrees = Math.toDegrees(angleInRadians);

        System.out.println("Angle: in radians - " + angleInRadians + ", in degrees - " + angleInDegrees);

        arc.setStartAngle(180 - angleInDegrees/2);
        if(isRightOriented) arc.setStartAngle(-angleInDegrees/2);

        arc.setLength(angleInDegrees);
        arc.updateArcBounds();



//        System.out.println("Radius: " + radius);
//        System.out.println("ArcX: " + arc.getCenterX());
//        System.out.println("ArcY: " + arc.getCenterY());
//        System.out.println("ArcLength: " + arc.getLength());
//        System.out.println("Rotation: " + arc.getRotate());
//        System.out.println("Start Angle: " + arc.getStartAngle());
//        System.out.println();


    }

    public void resize(double lensThickness, double newWidth, double newHeight)
    {
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
