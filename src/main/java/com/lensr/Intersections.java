package com.lensr;

import com.lensr.objects.glass.LensArc;
import com.lensr.objects.lightsources.Ray;
import javafx.geometry.Point2D;
import javafx.scene.shape.*;

import java.util.ArrayList;
import java.util.List;


public class Intersections {
    public static Point2D getRayLineIntersectionPoint(Ray ray, Line line) {
        double raySlope;
        double lineSlope;
        double rayYIntercept;
        double lineYIntercept;
        double intersectionX;
        double intersectionY;

        double tolerance = -0.0001; // Tolerance for floating point comparison

        // Ray is vertical
        if (Math.abs(ray.getEndX() - ray.getStartX()) < -tolerance) {
            intersectionX = ray.getStartX();
            lineSlope = (line.getEndY() - line.getStartY()) / (line.getEndX() - line.getStartX());
            lineYIntercept = line.getStartY() - lineSlope * line.getStartX();
            intersectionY = lineSlope * intersectionX + lineYIntercept;
        }
        // Line is vertical
        else if (Math.abs(line.getEndX() - line.getStartX()) < -tolerance) {
            intersectionX = line.getStartX();
            raySlope = (ray.getEndY() - ray.getStartY()) / (ray.getEndX() - ray.getStartX());
            rayYIntercept = ray.getStartY() - raySlope * ray.getStartX();
            intersectionY = raySlope * intersectionX + rayYIntercept;
        } else {
            raySlope = (ray.getEndY() - ray.getStartY()) / (ray.getEndX() - ray.getStartX());
            lineSlope = (line.getEndY() - line.getStartY()) / (line.getEndX() - line.getStartX());
            rayYIntercept = ray.getStartY() - raySlope * ray.getStartX();

            // If the slopes are equal, the ray and the line are parallel and do not intersect
            if (Math.abs(raySlope - lineSlope) < -tolerance) {
                return null;
            }

            lineYIntercept = line.getStartY() - lineSlope * line.getStartX();
            intersectionX = (lineYIntercept - rayYIntercept) / (raySlope - lineSlope);
            intersectionY = raySlope * intersectionX + rayYIntercept;
        }

        // Check if the ray is facing the line
        if ((ray.getEndX() > ray.getStartX() && intersectionX < ray.getStartX()) ||
                (ray.getEndX() < ray.getStartX() && intersectionX > ray.getStartX()) ||
                (ray.getEndY() > ray.getStartY() && intersectionY < ray.getStartY()) ||
                (ray.getEndY() < ray.getStartY() && intersectionY > ray.getStartY())) {
            return null;
        }

        // Check if the intersection point is within the bounds of the line
        if (intersectionX - Math.min(line.getStartX(), line.getEndX()) < tolerance ||
                Math.max(line.getStartX(), line.getEndX()) - intersectionX < tolerance ||
                intersectionY - Math.min(line.getStartY(), line.getEndY()) < tolerance ||
                Math.max(line.getStartY(), line.getEndY()) - intersectionY < tolerance) {
            return null;
        }

        return new Point2D(intersectionX, intersectionY);
    }

    public static Point2D getRayEllipseIntersectionPoint(Ray ray, Ellipse ellipse) {
        List<Point2D> intersections = calculateIntersectionPoints(ray, ellipse);

        if (intersections == null) {
            return null;
        }

        // If there is only one intersection point, the ellipse is a line. Return the intersection point as a line
        if (intersections.size() == 1) {
            return intersections.get(0);
        }

        // Check if the intersection points are in the direction of the ray
        double rayVectorX = ray.getEndX() - ray.getStartX();
        double rayVectorY = ray.getEndY() - ray.getStartY();

        double intersection1VectorX = intersections.get(0).getX() - ray.getStartX();
        double intersection1VectorY = intersections.get(0).getY() - ray.getStartY();
        double dotProduct1 = rayVectorX * intersection1VectorX + rayVectorY * intersection1VectorY;

        double intersection2VectorX = intersections.get(1).getX() - ray.getStartX();
        double intersection2VectorY = intersections.get(1).getY() - ray.getStartY();
        double dotProduct2 = rayVectorX * intersection2VectorX + rayVectorY * intersection2VectorY;

        // If both intersection points are behind the rays start point, there is no intersection
        if (dotProduct1 < 0 && dotProduct2 < 0) {
            return null;
        }

        // Return the intersection point that is closest to the ray's start point and in the direction of the ray
        if (dotProduct1 >= 0 && (dotProduct2 < 0 || intersections.get(0).distance(ray.getStartX(), ray.getStartY()) < intersections.get(1).distance(ray.getStartX(), ray.getStartY()))) {
            return intersections.get(0);
        } else if (dotProduct2 >= 0 && (dotProduct1 < 0 || intersections.get(1).distance(ray.getStartX(), ray.getStartY()) < intersections.get(0).distance(ray.getStartX(), ray.getStartY()))) {
            return intersections.get(1);
        }
        return null;
    }

    public static Point2D getRayArcIntersectionPoint(Ray ray, Arc arc) {
        List<Point2D> intersections = calculateIntersectionPoints(ray, new Ellipse(arc.getCenterX(), arc.getCenterY(), arc.getRadiusX(), arc.getRadiusY()));
        if (intersections == null) {
            return null;
        }

        // Check if the intersection points are in the direction of the ray
        double rayVectorX = ray.getEndX() - ray.getStartX();
        double rayVectorY = ray.getEndY() - ray.getStartY();

        double intersection1VectorX = intersections.get(0).getX() - ray.getStartX();
        double intersection1VectorY = intersections.get(0).getY() - ray.getStartY();
        double dotProduct1 = rayVectorX * intersection1VectorX + rayVectorY * intersection1VectorY;

        double intersection2VectorX = intersections.get(1).getX() - ray.getStartX();
        double intersection2VectorY = intersections.get(1).getY() - ray.getStartY();
        double dotProduct2 = rayVectorX * intersection2VectorX + rayVectorY * intersection2VectorY;

        // If both intersection points are behind the rays start point, there is no intersection
        if (dotProduct1 < 0 && dotProduct2 < 0) {
            return null;
        }

        double intersection1Angle = (360 - Math.toDegrees(Math.atan2(intersections.get(0).getY() - arc.getCenterY(), intersections.get(0).getX() - arc.getCenterX()))) % 360;
        double intersection2Angle = (360 - Math.toDegrees(Math.atan2(intersections.get(1).getY() - arc.getCenterY(), intersections.get(1).getX() - arc.getCenterX()))) % 360;

        if (arc.getStartAngle() + arc.getLength() > 360 && intersection1Angle < arc.getStartAngle()) {
            intersection1Angle = intersection1Angle + 360;
        }
        if (arc.getStartAngle() + arc.getLength() > 360 && intersection2Angle < arc.getStartAngle()) {
            intersection2Angle = intersection2Angle + 360;
        }

        // Return the closest intersection point in the direction of the ray that lies on the arc
        if (dotProduct1 < 0 || (intersection1Angle < arc.getStartAngle() || intersection1Angle > arc.getStartAngle() + arc.getLength())) {
            intersections.set(0, null);
        }
        if (dotProduct2 < 0 || (intersection2Angle < arc.getStartAngle() || intersection2Angle > arc.getStartAngle() + arc.getLength())) {
            intersections.set(1, null);
        }

        if (intersections.get(0) != null && (intersections.get(1) == null || intersections.get(0).distance(ray.getStartX(), ray.getStartY()) < intersections.get(1).distance(ray.getStartX(), ray.getStartY()))) {
            return intersections.get(0);
        } else if (intersections.get(1) != null) {
            return intersections.get(1);
        }

        return null;
    }


    private static List<Point2D> calculateIntersectionPoints(Ray ray, Ellipse ellipse) {
        double raySlope = (ray.getEndY() - ray.getStartY()) / (ray.getEndX() - ray.getStartX());
        double rayYIntercept = ray.getStartY() - raySlope * ray.getStartX();

        // Convert the ellipse to standard form (x - h)^2 / a^2 + (y - k)^2 / b^2 = 1
        double h = ellipse.getCenterX();
        double k = ellipse.getCenterY();
        double x = ellipse.getRadiusX();
        double y = ellipse.getRadiusY();

        // a = y^2 + x^2 * m^2
        // b = 2 * x^2 * m * (b - k) - 2 * h * y^2
        // c = y^2 * h^2 + x^2 * (b - k)^2 - x^2 * y^2
        double a = y * y + x * x * raySlope * raySlope;
        double b = 2 * x * x * raySlope * (rayYIntercept - k) - 2 * h * y * y;
        double c = y * y * h * h + x * x * (rayYIntercept - k) * (rayYIntercept - k) - x * x * y * y;

        double discriminant = b * b - 4 * a * c;

        double tolerance = 0.0001; // Tolerance for floating point comparison

        // Ellipse is a vertical line
        if (x == 0) {
            Point2D intersection = getRayLineIntersectionPoint(ray, new Line(ellipse.getCenterX(), ellipse.getCenterY() - ellipse.getRadiusY(), ellipse.getCenterX(), ellipse.getCenterY() + ellipse.getRadiusY()));
            if (intersection != null) {
                return new ArrayList<>(List.of(intersection));
            }
        }
        // Ellipse is a horizontal line
        else if (y == 0) {
            Point2D intersection = getRayLineIntersectionPoint(ray, new Line(ellipse.getCenterX() - ellipse.getRadiusX(), ellipse.getCenterY(), ellipse.getCenterX() + ellipse.getRadiusX(), ellipse.getCenterY()));
            if (intersection != null) {
                return new ArrayList<>(List.of(intersection));
            }
        }
        // Ray is vertical
        else if (Math.abs(ray.getEndX() - ray.getStartX()) < tolerance) {
            // Ray is outside the ellipse
            if (ray.getStartX() < h - x || ray.getStartX() > h + x) {
                return null;
            }
            double intersectionY1 = k + y * Math.sqrt(1 - Math.pow((ray.getStartX() - h) / x, 2));
            double intersectionY2 = k - y * Math.sqrt(1 - Math.pow((ray.getStartX() - h) / x, 2));

            double intersectionX1 = ray.getStartX();
            double intersectionX2 = ray.getStartX();

            return new ArrayList<>(List.of(new Point2D(intersectionX1, intersectionY1), new Point2D(intersectionX2, intersectionY2)));
        } else if (discriminant >= 0) {
            double intersection1X = (-b + Math.sqrt(discriminant)) / (2 * a);
            double intersection2X = (-b - Math.sqrt(discriminant)) / (2 * a);

            double intersection1Y = raySlope * intersection1X + rayYIntercept;
            double intersection2Y = raySlope * intersection2X + rayYIntercept;

            return new ArrayList<>(List.of(new Point2D(intersection1X, intersection1Y), new Point2D(intersection2X, intersection2Y)));
        }
        return null;
    }


    public static double getLineReflectionAngle(Ray ray, Line mirror) {
        double angleOfIncidence = Math.atan2(ray.getEndY() - ray.getStartY(), ray.getEndX() - ray.getStartX());

        // Calculate the angle of the mirror line
        double mirrorAngle = Math.atan2(mirror.getEndY() - mirror.getStartY(), mirror.getEndX() - mirror.getStartX());

        return 2 * mirrorAngle - angleOfIncidence;
    }

    public static double getEllipseReflectionAngle(Ray ray, Ellipse mirror) {
        double angleOfIncidence = Math.atan2(ray.getEndY() - ray.getStartY(), ray.getEndX() - ray.getStartX());

        // Calculate the angle of the normal vector at the intersection point
        double x = ray.getEndX();
        double y = ray.getEndY();
        double centerX = mirror.getCenterX();
        double centerY = mirror.getCenterY();
        double semiMajorAxis = mirror.getRadiusX();
        double semiMinorAxis = mirror.getRadiusY();

        // Equation of an ellipse: (x - h)^2 / a^2 + (y - k)^2 / b^2 = 1
        // Derivative of the ellipse equation: f'(x) = -((x - h) / a^2) / ((y - k) / b^2)
        // Normal vector: (-f'(x), 1)
        // according to chatgpt at least
        double normalAngle = Math.atan2(((y - centerY) * semiMajorAxis * semiMajorAxis), ((x - centerX) * semiMinorAxis * semiMinorAxis));

        // Adjust the normal angle based on the quadrant of the point
        if (x > centerX) {
            normalAngle += Math.PI;
        }

        return 2 * normalAngle - angleOfIncidence;
    }

    public static double getArcReflectionAngle(Ray ray, Arc mirror) {
        double angleOfIncidence = Math.atan2(ray.getEndY() - ray.getStartY(), ray.getEndX() - ray.getStartX());

        // Calculate the angle of the normal vector at the intersection point
        double x = ray.getEndX();
        double y = ray.getEndY();
        double centerX = mirror.getCenterX();
        double centerY = mirror.getCenterY();
        double semiMajorAxis = mirror.getRadiusX();
        double semiMinorAxis = mirror.getRadiusY();

        // Equation of an ellipse: (x - h)^2 / a^2 + (y - k)^2 / b^2 = 1
        // Derivative of the ellipse equation: f'(x) = -((x - h) / a^2) / ((y - k) / b^2)
        // Normal vector: (-f'(x), 1)
        // according to chatgpt at least
        double normalAngle = Math.atan2(((y - centerY) * semiMajorAxis * semiMajorAxis), ((x - centerX) * semiMinorAxis * semiMinorAxis));

        // Adjust the normal angle based on the quadrant of the point
        if (x > centerX) {
            normalAngle += Math.PI;
        }

        return 2 * normalAngle - angleOfIncidence;
    }

    public static double getLineRefractionAngle(Ray ray, Line line, double currRefractiveIndex, double newRefractiveIndex) {
        double angleOfIncidence = Math.atan2(ray.getEndY() - ray.getStartY(), ray.getEndX() - ray.getStartX());
        double lineAngle = Math.atan2(line.getEndY() - line.getStartY(), line.getEndX() - line.getStartX());

        double normalAngle = determineNormalAngle(lineAngle - Math.PI / 2, lineAngle + Math.PI / 2, angleOfIncidence);
        double reversedNormalAngle = normalAngle < 0 ? normalAngle + Math.PI : normalAngle - Math.PI;
        double normalizedAngleOfIncidence = normalizeIntersectionAngle(angleOfIncidence, normalAngle);


        return reversedNormalAngle + Math.asin(currRefractiveIndex / newRefractiveIndex * Math.sin(normalizedAngleOfIncidence));

    }

    public static double getArcRefractionAngle(Ray ray, LensArc arc, double currRefractiveIndex, double newRefractiveIndex) {
        // 12 f-ing hours of sine/cosine f-ing javafx radians -pi to pi, and it finally f-ing works
        double angleOfIncidence = Math.atan2(ray.getEndY() - ray.getStartY(), ray.getEndX() - ray.getStartX());

        double centerX = arc.getCenterX();
        double centerY = arc.getCenterY();
        double pointX = ray.getEndX();
        double pointY = ray.getEndY();


        double normalAngle = determineNormalAngle(Math.atan2((pointY - centerY), (pointX - centerX)), Math.atan2((centerY - pointY), (centerX - pointX)), angleOfIncidence);

        double reversedNormalAngle = normalAngle < 0 ? normalAngle + Math.PI : normalAngle - Math.PI;
        double normalizedAngleOfIncidence = normalizeIntersectionAngle(angleOfIncidence, normalAngle);


        return reversedNormalAngle + Math.asin(currRefractiveIndex / newRefractiveIndex * Math.sin(normalizedAngleOfIncidence));
    }

    public static Point2D rotatePointAroundOtherByAngle(Point2D rotatedPoint, Point2D staticPoint, double angle) {
        double x = rotatedPoint.getX() - staticPoint.getX();
        double y = rotatedPoint.getY() - staticPoint.getY();

        double newX = x * Math.cos(angle) - y * Math.sin(angle);
        double newY = x * Math.sin(angle) + y * Math.cos(angle);

        return new Point2D(newX, newY);
    }

    public static double determineNormalAngle(double angle1, double angle2, double angleOfIncidence) {
        // Determine length between first angle and angle of incidence
        double length1;
        double biggerAngle1 = Math.abs(angle1) > Math.abs(angleOfIncidence) ? angle1 : angleOfIncidence;
        double smallerAngle1 = biggerAngle1 == angleOfIncidence ? angle1 : angleOfIncidence;
        if (angle1 > 0 && angleOfIncidence > 0) {
            length1 = biggerAngle1 - smallerAngle1;
        } else if (angle1 < 0 && angleOfIncidence < 0) {
            length1 = Math.abs(Math.abs(biggerAngle1) - Math.abs(smallerAngle1));
        } else {
            length1 = Math.abs(Math.max(angle1, angleOfIncidence) - Math.min(angle1, angleOfIncidence));
            if (length1 > Math.PI) {
                length1 = 2 * Math.PI - length1;
            }

        }

        // Determine length between second angle and angle of incidence
        double length2;
        double biggerAngle2 = Math.abs(angle2) > Math.abs(angleOfIncidence) ? angle2 : angleOfIncidence;
        double smallerAngle2 = biggerAngle2 == angleOfIncidence ? angle2 : angleOfIncidence;
        if (angle2 > 0 && angleOfIncidence > 0) {
            length2 = biggerAngle2 - smallerAngle2;
        } else if (angle2 < 0 && angleOfIncidence < 0) {
            length2 = Math.abs(Math.abs(biggerAngle2) - Math.abs(smallerAngle2));
        } else {
            length2 = Math.abs(Math.max(angle2, angleOfIncidence) - Math.min(angle2, angleOfIncidence));
            if (length2 > Math.PI) {
                length2 = 2 * Math.PI - length2;
            }
        }

        return length1 < length2 ? angle1 : angle2;

    }

    public static double normalizeIntersectionAngle(double intersectionAngle, double normalAngle) {
        double normalizedAngle = intersectionAngle - normalAngle;
        if (Math.abs(normalizedAngle) < Math.PI) {
            return normalizedAngle;
        }
        double direction = normalizedAngle < 0 ? 1 : -1;
        return direction * (2 * Math.PI - Math.abs(normalizedAngle));
    }
}
