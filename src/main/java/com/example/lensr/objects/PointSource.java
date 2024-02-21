package com.example.lensr.objects;

import com.example.lensr.EditPoint;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Rotate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.example.lensr.LensrStart.*;

public class PointSource extends Rectangle implements Editable {
    
    public List<OriginRay> originRays = new ArrayList<>();
    EditPoint objectEditPoint;
    public Rotate rotate = new Rotate();
    double rotation = 0;
    public Rectangle hitbox;
    public Group group = new Group();
    public boolean hasBeenClicked;
    public boolean isEdited;
    public double wavelength = 580;
    public double brightness = 1.0;
    public int rayCount;
    
    public PointSource(double centerX, double centerY, int rayCount)
    {
        setWidth(1);
        setHeight(1);
        setCenter(centerX, centerY);
        this.rayCount = rayCount;
    }
    
    public void create()
    {
        setFill(Color.GRAY);
        toBack();
        
        createRectangleHitbox();
        
        double angleBetweenRays = 2 * Math.PI / rayCount;

        for (int i = 0; i < rayCount; i++) {
            OriginRay originRay = new OriginRay(getCenter().getX(), 
                    getCenter().getY(), 
                    getCenter().getX() + Math.cos(angleBetweenRays * i) * SIZE,
                    getCenter().getY() + Math.sin(angleBetweenRays * i) * SIZE);
            originRay.setParentSource(this);
            originRay.setStrokeWidth(globalStrokeWidth);
            originRay.setWavelength(wavelength);
            originRay.setBrightness(brightness);
            originRays.add(originRay);
        }
        
        objectEditPoint = new EditPoint(getCenter());
        objectEditPoint.setOnClickEvent(event -> move());
        
    }

    private void move() {
    }

    public void setCenter(double newCenterX, double newCenterY)
    {
        setX(newCenterX - getWidth()/2);
        setY(newCenterY - getHeight()/2);
    }
    private void createRectangleHitbox() {
        hitbox = new Rectangle();
        hitbox.setHeight(mouseHitboxSize);
        hitbox.setWidth(mouseHitboxSize);
        hitbox.setFill(Color.TRANSPARENT);
        hitbox.setStroke(Color.BLACK);
        hitbox.setStrokeWidth(0);
        hitbox.toBack();
        hitbox.getTransforms().add(rotate);
        updateHitbox();
    }
    private void updateHitbox() {
        Point2D center = getCenter();
        hitbox.setY(center.getY() - hitbox.getHeight() / 2);
        hitbox.setX(center.getX() - hitbox.getHeight() / 2);
        rotate.setPivotX(center.getY());
        rotate.setPivotY(center.getY());
//        rotation = Math.toDegrees(Math.atan2(getEndY() - getStartY(), getEndX() - getStartX()));
        
//        rotate.setAngle(rotation);
    }
    public Point2D getCenter() {
        return new Point2D(getX() + getWidth()/2, getY() + getHeight()/2);
    }

    @Override
    public void openObjectEdit() {
        
    }

    @Override
    public void closeObjectEdit() {

    }

    @Override
    public void setHasBeenClicked(boolean hasBeenClicked) {

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
        return false;
    }

    @Override
    public boolean intersectsMouseHitbox() {
        return false;
    }
}
