package com.example.lensr.objects.glass;

import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.example.lensr.LensrStart.mirrorColor;

public class LensArc extends Arc {
    public Map<LensLine, String> borderingLines;
    private final SphericalLens parentLens;

    public LensArc(SphericalLens parentLens, Map<LensLine, String> borderingLines, Point2D vertex) {
        this.parentLens = parentLens;
        this.borderingLines = borderingLines;

        scale(vertex);

        setType(ArcType.OPEN);
        setFill(Color.TRANSPARENT);
        setStroke(mirrorColor);
    }

    public Point2D getVertex() {
        double angle = (getStartAngle() + getLength() / 2) % 360;
        if (angle < 0) angle += 360;
        double angleInRadians = Math.toRadians(angle);
        double x = getCenterX() + getRadiusX() * Math.cos(angleInRadians);
        double y = getCenterY() - getRadiusY() * Math.sin(angleInRadians);
        return new Point2D(x, y);
    }

    public SphericalLens getParentLens() {
        return parentLens;
    }

    public void scale(Point2D vertex) {
        List<Point2D> points = new ArrayList<>();
        for (LensLine line : borderingLines.keySet()) {
            if (borderingLines.get(line).equals("start")) {
                points.add(line.getStart());
            } else {
                points.add(line.getEnd());
            }
        }
        Point2D start = points.get(0);
        Point2D end = points.get(1);

        if (start.equals(end)) {
            end = new Point2D(end.getX(), end.getY()+0.01);
        }
        Point2D curvePoint = vertex;

        Circle circumcircle = getCircumcircle(start, end, curvePoint);

        // Calculate the angles of the start, end and curve point relative to the circumcircle
        double curvePointAngle = (360 - Math.toDegrees(Math.atan2(curvePoint.getY() - circumcircle.getCenterY(), curvePoint.getX() - circumcircle.getCenterX()))) % 360;
        double startAngle = (360 - Math.toDegrees(Math.atan2(start.getY() - circumcircle.getCenterY(), start.getX() - circumcircle.getCenterX()))) % 360;
        double endAngle = (360 - Math.toDegrees(Math.atan2(end.getY() - circumcircle.getCenterY(), end.getX() - circumcircle.getCenterX()))) % 360;

        double finalLength = calculateLength(startAngle, endAngle, curvePointAngle);

        setCenterX(circumcircle.getCenterX());
        setCenterY(circumcircle.getCenterY());
        setRadiusX(circumcircle.getRadius());
        setRadiusY(circumcircle.getRadius());
        setStartAngle(startAngle);
        setLength(finalLength);
    }

    public void move(double x, double y) {
        setCenterX(getCenterX() + x);
        setCenterY(getCenterY() + y);
    }



    public Circle getCircumcircle(Point2D start, Point2D end, Point2D curvePoint) {
        // Check if curvePoint lies on the line between start and end
        if (curvePoint.distance(start) + curvePoint.distance(end) == start.distance(end)) {
            // If curvePoint lies on the line, slightly adjust it
            curvePoint = new Point2D(curvePoint.getX() + 0.01, curvePoint.getY() + 0.01);
        }

        // Find the arc starting at the start point, ending at the end point and passing through the curve point
        // To do this, we treat those points as a triangle and find the circumcenter and circumradius
        double a = Math.sqrt(Math.pow(start.getX() - curvePoint.getX(), 2) + Math.pow(start.getY() - curvePoint.getY(), 2));
        double b = Math.sqrt(Math.pow(end.getX() - curvePoint.getX(), 2) + Math.pow(end.getY() - curvePoint.getY(), 2));
        double c = Math.sqrt(Math.pow(start.getX() - end.getX(), 2) + Math.pow(start.getY() - end.getY(), 2));

        double p = (a + b + c) / 2;
        double r = Math.sqrt((p - a) * (p - b) * (p - c) / p);
        double radius = (a * b * c) / (4 * r * p);

        // Calculate the midpoints of two sides of the triangle
        Point2D midpoint1 = new Point2D((start.getX() + end.getX()) / 2, (start.getY() + end.getY()) / 2);
        Point2D midpoint2 = new Point2D((end.getX() + curvePoint.getX()) / 2, (end.getY() + curvePoint.getY()) / 2);

        // Calculate the slope of the two lines
        double slope1 = ((start.getY() - end.getY()) / (start.getX() - end.getX()));
        double slope2 = ((end.getY() - curvePoint.getY()) / (end.getX() - curvePoint.getX()));

        // Cheaty? Sure. But it works.
        if (slope1 == 0) slope1 = 0.0000000001;
        if (slope2 == 0) slope2 = 0.0000000001;

        double perpSlope1 = -1 / slope1;
        double perpSlope2 = -1 / slope2;

        double circumcenterX = (midpoint2.getY() - midpoint1.getY() + perpSlope1 * midpoint1.getX() - perpSlope2 * midpoint2.getX()) / (perpSlope1 - perpSlope2);
        double circumcenterY = midpoint1.getY() + perpSlope1 * (circumcenterX - midpoint1.getX());

        return new Circle(circumcenterX, circumcenterY, radius);
    }

    private double calculateLength (double startAngle, double endAngle, double curveAngle) {
        double biggerAngle = Math.max(startAngle, endAngle);
        double smallerAngle = Math.min(startAngle, endAngle);

        double firstDistance = biggerAngle - smallerAngle;
        double secondDistance = 360 - firstDistance;

        boolean laysBetween = curveAngle >= smallerAngle && curveAngle <= biggerAngle;

        if (laysBetween) {
            if (startAngle < endAngle) {
                return firstDistance;
            } else {
                return -firstDistance;
            }
        } else {
            if (startAngle < endAngle) {
                return -secondDistance;
            } else {
                return secondDistance;
            }
        }
    }
}
