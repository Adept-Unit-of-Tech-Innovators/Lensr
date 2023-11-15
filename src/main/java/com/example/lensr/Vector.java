package com.example.lensr;

public class Vector {
    public double x, y;
    public Vector(double x, double y) {
        this.x = x;
        this.y = y;
    }
    public Vector normalize() {
        double magnitude = getMagnitude();

        double normalizedX = x / magnitude;
        double normalizedY = y / magnitude;

        return new Vector(normalizedX, normalizedY);
    }


    // Calculate the magnitude (length) of a vector; |v| = √(x^2 + y^2)
    public double getMagnitude() {
        return Math.sqrt( Math.pow(x, 2) + Math.pow(y, 2) );
    }


    // Calculate dot product of 2 vectors using: a · b = ax × bx + ay × by
    public static double getDotProduct(Vector vector1, Vector vector2) {
        return vector1.x * vector2.x + vector1.y * vector2.y;
    }

}
