package com.example.lensr.ui;

import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;

import static com.example.lensr.LensrStart.*;
public class EditPoint extends Rectangle {
    public boolean hasBeenClicked;
    public Style style;
    public enum Style {
        Primary,
        Secondary,
        Tertiary,
        Quaternary
    }
    EventHandler<MouseEvent> onClickEvent;
    public EditPoint(double centerX, double centerY, Style style) {
        this.style = style;
        setWidth(editPointSize);
        setHeight(editPointSize);
        setCenterX(centerX);
        setCenterY(centerY);
        if (style == Style.Primary)
            setFill(Color.web("#ed4245"));
        else if (style == Style.Secondary)
            setFill(Color.rgb(77, 102, 204));
        else if (style == Style.Tertiary)
            setFill(Color.web("#37a45a"));
        else if (style == Style.Quaternary)
            setFill(Color.web("#edd642"));
        setStrokeWidth(1);
        setStroke(Color.web("3A3C42"));
        setStrokeType(StrokeType.OUTSIDE);
        toFront();
    }


    public void handleMousePressed() {
        isMousePressed = true;
        hasBeenClicked = true;
        scene.setCursor(Cursor.CLOSED_HAND);

        if (onClickEvent != null) onClickEvent.handle(null);
    }


    public void setOnClickEvent(EventHandler<MouseEvent> onClickEvent) {
        this.onClickEvent = onClickEvent;
    }


    public void setCenterX(double centerX) {
        setX(centerX - getWidth() / 2);
    }


    public void setCenterY(double centerY) {
        setY(centerY - getHeight() / 2);
    }


    public void setCenter(Point2D center) {
        setX(center.getX() - getWidth() / 2);
        setY(center.getY() - getHeight() / 2);
    }


    public double getCenterX() {
        return getX() + getWidth() / 2;
    }


    public double getCenterY() {
        return getY() + getHeight() / 2;
    }


    public Point2D getCenter() {
        return new Point2D(getX() + editPointSize / 2, getY() + editPointSize / 2);
    }
}