package com.example.lensr.objects.lightsources;

import com.example.lensr.ui.EditPoint;
import com.example.lensr.objects.Editable;
import com.example.lensr.objects.misc.LightSensor;
import com.example.lensr.saveloadkit.SaveState;
import com.example.lensr.UserControls;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static com.example.lensr.LensrStart.*;

public class PointSource extends Rectangle implements Editable, Serializable {
    private transient Group group = new Group();
    private transient List<OriginRay> originRays = new ArrayList<>();
    private transient List<EditPoint> objectEditPoints = new ArrayList<>();
    private transient Rectangle hitbox;
    private double startAngle = 0;
    public boolean hasBeenClicked;
    public boolean isEdited;
    public double wavelength = 580;
    public double brightness = 1.0;
    private double fieldOfView;
    private int rayCount;
    private boolean isFull;
    private boolean isWhiteLight;

    public PointSource(double centerX, double centerY, double fieldOfView, double startAngle, int rayCount, boolean isFull)
    {
        setWidth(10);
        setHeight(10);
        setCenter(centerX, centerY);
        this.fieldOfView = fieldOfView;
        this.startAngle = startAngle;
        this.rayCount = rayCount;
        this.isFull = isFull;
    }
    
    public void create()
    {
        setFill(Color.GRAY);
        toBack();
        
        createRectangleHitbox();
        
        double angleBetweenRays = fieldOfView / (rayCount - 1);

        for (int i = 0; i < rayCount; i++) {
            OriginRay originRay = new OriginRay(
                    getCenter().getX(),
                    getCenter().getY(), 
                    getCenter().getX() + (Math.cos(startAngle + angleBetweenRays * i)) * WIDTH * 1000,
                    getCenter().getY() + (Math.sin(startAngle + angleBetweenRays * i)) * WIDTH * 1000
            );
            originRay.setParentSource(this);
            originRay.setStrokeWidth(globalStrokeWidth);
            originRay.setWavelength(wavelength);
            originRay.setBrightness(brightness);
            originRays.add(originRay);
        }
        
        objectEditPoints.add(new EditPoint(getCenter()));
        objectEditPoints.add(new EditPoint(getCenter().getX() + Math.cos(startAngle + fieldOfView/2) * 100, getCenter().getY() + Math.sin(startAngle + fieldOfView/2) * 100));

        objectEditPoints.get(0).setOnClickEvent(event -> move());
        objectEditPoints.get(1).setOnClickEvent(event -> rotate());

        objectEditPoints.forEach(editPoint -> {
            editPoint.toFront();
            editPoint.setVisible(false);
            editPoint.hasBeenClicked = false;
        });

        lightSources.add(this);
        group.getChildren().add(this);
        originRays.forEach(ray -> group.getChildren().add(ray.group));
        group.getChildren().addAll(objectEditPoints);
        root.getChildren().add(group);
        originRays.forEach(Node::toBack);
    }

    private void move()
    {
        new Thread(() -> {
            double angleBetweenRays = fieldOfView / (rayCount - 1);


            while (isMousePressed && isEdited)
            {
                double newCenterX = mousePos.getX();
                double newCenterY = mousePos.getY();

                Platform.runLater(() -> {
                    setCenter(newCenterX, newCenterY);

                    int i = 0;
                    for(OriginRay originRay : originRays)
                    {
                        originRay.setStartX(newCenterX);
                        originRay.setStartY(newCenterY);
                        originRay.setEndX(newCenterX + (Math.cos(startAngle + angleBetweenRays * (i % rayCount))) * WIDTH * 1000);
                        originRay.setEndY(newCenterY + (Math.sin(startAngle + angleBetweenRays * (i % rayCount))) * WIDTH * 1000);
                        i++;
                    }

                    if(objectEditPoints != null) {
                        objectEditPoints.get(0).setCenter(new Point2D(newCenterX, newCenterY));
                        objectEditPoints.get(1).setCenter(new Point2D(newCenterX + Math.cos(startAngle + fieldOfView/2) * 100, newCenterY + Math.sin(startAngle + fieldOfView/2) * 100));
                    }

                    updateHitbox();
                });

                synchronized (lock) {
                    try {
                        lock.wait(10); // Adjust the sleep time as needed
                    } catch (InterruptedException e) {
                        throw new RuntimeException("A thread was interrupted while waiting.");
                    }
                }
            }
            SaveState.autoSave();
        }).start();
    }

    public void rotate()
    {
        new Thread(() -> {
            while (isMousePressed && isEdited)
            {
                double mouseAngle = Math.atan2(mousePos.getY() - getCenter().getY(), mousePos.getX() - getCenter().getX());
                double newStartAngle = mouseAngle - fieldOfView/2;
                Platform.runLater(() -> {
                    setStartAngle(newStartAngle);

                    if(objectEditPoints != null) {
                        objectEditPoints.get(1).setCenter(new Point2D(getCenter().getX() + Math.cos(mouseAngle)  * 100, getCenter().getY() + Math.sin(mouseAngle) * 100));
                    }
                });

                synchronized (lock) {
                    try {
                        lock.wait(10); // Adjust the sleep time as needed
                    } catch (InterruptedException e) {
                        throw new RuntimeException("A thread was interrupted while waiting.");
                    }
                }
            }
            SaveState.autoSave();
        }).start();
    }

    @Serial
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeDouble(getX());
        out.writeDouble(getY());
        out.writeDouble(getWidth());
        out.writeDouble(getHeight());
    }

    @Serial
    private void readObject(java.io.ObjectInputStream in) throws Exception {
        in.defaultReadObject();
        setX(in.readDouble());
        setY(in.readDouble());
        setWidth(in.readDouble());
        setHeight(in.readDouble());

        // Initialize transient fields
        group = new Group();
        originRays = new ArrayList<>();
        hitbox = new Rectangle();
        objectEditPoints = new ArrayList<>();
        isEdited = false;
        hasBeenClicked = false;
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
        updateHitbox();
    }

    private void updateHitbox() {
        Point2D center = getCenter();
        hitbox.setY(center.getY() - hitbox.getHeight() / 2);
        hitbox.setX(center.getX() - hitbox.getHeight() / 2);
    }

    public Point2D getCenter() {
        return new Point2D(getX() + getWidth()/2, getY() + getHeight()/2);
    }

    public void setRayCount(int newRayCount)
    {
        rayCount = newRayCount;
        int rayNumber = isWhiteLight ? whiteLightRayCount : 1;

        double angleBetweenRays = fieldOfView / (rayCount - 1);

        originRays.forEach(ray -> {
            root.getChildren().remove(ray.group);
            group.getChildren().remove(ray.group);
        });
        originRays.clear();

        for (int i = 0; i < rayNumber; i++) {
            for (int j = 0; j < rayCount; j++) {
                OriginRay originRay = new OriginRay(
                        getCenter().getX(),
                        getCenter().getY(),
                        getCenter().getX() + Math.cos(startAngle + angleBetweenRays * j) * WIDTH * 1000,
                        getCenter().getY() + Math.sin(startAngle + angleBetweenRays * j) * WIDTH * 1000
                );
                originRay.setParentSource(this);
                originRay.setStrokeWidth(globalStrokeWidth);
                originRay.setWavelength(isWhiteLight ? 380 + (400.0 / whiteLightRayCount * i) : wavelength);
                originRay.setBrightness(brightness);

                originRays.add(originRay);
            }
        }
        originRays.forEach(ray -> group.getChildren().add(ray.group));
        objectEditPoints.forEach(Node::toFront);
    }

    public void setFieldOfView(double fieldOfView)
    {
        double middleAngle = startAngle + this.fieldOfView / 2;
        this.fieldOfView = fieldOfView;
        setStartAngle(middleAngle - fieldOfView/2);
    }

    public void setStartAngle(double startAngle)
    {
        this.startAngle = startAngle;
        double angleBetweenRays = fieldOfView / (rayCount - 1);
        for (int i = 0; i < originRays.size(); i++) {
            originRays.get(i).setEndX(getCenter().getX() + Math.cos(startAngle + angleBetweenRays * (i % rayCount)) * WIDTH * 1000);
            originRays.get(i).setEndY(getCenter().getY() + Math.sin(startAngle + angleBetweenRays * (i % rayCount)) * WIDTH * 1000);
        }
        update();
    }

    public void update() {
        if (isEdited) {
            return;
        }

        // Reset the end point of the ray to ensure deterministic behavior
        int whiteRayCount = isWhiteLight ? whiteLightRayCount : 1;
        for (int i = 0; i < whiteRayCount; i++) {
            for (int j = 0; j < rayCount; j++) {
                OriginRay originRay = originRays.get(i * rayCount + j);
                originRay.setEndX(getCenter().getX() + Math.cos(startAngle + fieldOfView / (rayCount - 1) * j) * WIDTH * 1000);
                originRay.setEndY(getCenter().getY() + Math.sin(startAngle + fieldOfView / (rayCount - 1) * j) * WIDTH * 1000);
            }
        }

        for (OriginRay originRay : originRays) {
            group.getChildren().removeAll(originRay.rayReflections);
            mirrors.forEach(mirror -> {
                if (mirror instanceof LightSensor lightSensor) {
                    lightSensor.detectedRays.removeAll(originRay.rayReflections);
                    lightSensor.getDetectedRays().remove(originRay);
                }
            });

            originRay.simulate();
        }
    }

    @Override
    public void openObjectEdit() {
        wavelengthSlider.setCurrentSource(this);
        wavelengthSlider.show();
        whiteLightToggle.setCurrentSource(this);
        whiteLightToggle.setSelected(isWhiteLight);
        whiteLightToggle.show();
        numberOfRaysSlider.setCurrentSource(this);
        numberOfRaysSlider.show();
        brightnessSlider.setCurrentSource(this);
        brightnessSlider.show();

        wavelengthSlider.setDisable(isWhiteLight);

        if(!isFull) {
            fieldOfViewSlider.setCurrentSource(this);
            fieldOfViewSlider.show();
        }
        root.requestFocus();

        objectEditPoints.forEach(editPoint -> {
            editPoint.setVisible(true);
            editPoint.toFront();
        });

        Platform.runLater(() -> rayCanvas.clear());

        hasBeenClicked = true;
        isEdited = true;

        editPoints.addAll(objectEditPoints);
        editedShape = group;

        updateLightSources();
    }

    @Override
    public void closeObjectEdit() {
        wavelengthSlider.hide();
        whiteLightToggle.hide();
        numberOfRaysSlider.hide();
        brightnessSlider.hide();
        if(fieldOfViewSlider != null) fieldOfViewSlider.hide();

        isEdited = false;
        if(objectEditPoints != null && editedShape instanceof Group)
        {
            editPoints.removeAll(objectEditPoints);

            objectEditPoints.forEach(editPoint -> {
                editPoint.setVisible(false);
                editPoint.hasBeenClicked = false;
            });
        }
        editedShape = null;
        updateLightSources();
    }

    @Override
    public void delete() {
        wavelengthSlider.hide();
        numberOfRaysSlider.hide();
        brightnessSlider.hide();
        if(!isFull) fieldOfViewSlider.hide();
        whiteLightToggle.hide();
        editPoints.removeAll(objectEditPoints);
        editedShape = null;
        lightSources.remove(this);
        originRays.clear();
        root.getChildren().remove(group);
    }

    @Override
    public void copy() {
        PointSource newPointSource = new PointSource(getCenter().getX(), getCenter().getY(), fieldOfView, startAngle, rayCount, isFull);
        newPointSource.create();

        newPointSource.setWavelength(wavelength);
        newPointSource.setBrightness(brightness);
        newPointSource.setWhiteLight(isWhiteLight);
        newPointSource.setRayCount(rayCount);
        newPointSource.setFieldOfView(fieldOfView);

        newPointSource.moveBy(10, 10);
        UserControls.closeCurrentEdit();
        newPointSource.openObjectEdit();
    }

    @Override
    public void moveBy(double x, double y)
    {
        setCenter(getCenter().getX() + x, getCenter().getY() + y);
        objectEditPoints.get(0).setCenter(new Point2D(x, y));
        objectEditPoints.get(1).setCenter(new Point2D(x + Math.cos(startAngle + fieldOfView/2) * 100, y + Math.sin(startAngle + fieldOfView/2) * 100));

        updateHitbox();

        originRays.forEach(originRay -> {
            originRay.setStartX(originRay.getStartX() + x);
            originRay.setStartY(originRay.getStartY() + y);
            originRay.setEndX(originRay.getEndX() + x);
            originRay.setEndY(originRay.getEndY() + y);
        });
        SaveState.autoSave();
    }

    public double getFieldOfView()
    {
        return fieldOfView;
    }
    public double getRayCount()
    {
        return rayCount;
    }
    public boolean getIsFull()
    {
        return isFull;
    }
    public double getWavelength()
    {
        return wavelength;
    }
    public boolean getIsWhiteLight() {return isWhiteLight;}
    public void setWhiteLight(boolean whiteLight)
    {
        isWhiteLight = whiteLight;
        wavelengthSlider.setDisable(isWhiteLight);
        Platform.runLater(() -> {
            group.getChildren().removeAll(originRays);
            originRays.forEach(originRay -> {
                group.getChildren().remove(originRay.group);
                mirrors.forEach(mirror -> {
                    if (mirror instanceof LightSensor lightSensor) {
                        lightSensor.detectedRays.removeAll(originRay.rayReflections);
                        lightSensor.getDetectedRays().remove(originRay);
                    }
                });
                originRay.rayReflections.clear();
            });
            originRays.clear();

            double angleBetweenRays = fieldOfView/rayCount;

            int rayNumber = whiteLight ? whiteLightRayCount : 1;
            for (int i = 0; i < rayNumber; i++) {
                for (int j = 0; j < rayCount; j++) {
                    OriginRay originRay = new OriginRay(
                            getCenter().getX(),
                            getCenter().getY(),
                            getCenter().getX() + Math.cos(startAngle + angleBetweenRays * j) * WIDTH * 1000,
                            getCenter().getY() + Math.sin(startAngle + angleBetweenRays * j) * WIDTH * 1000
                    );
                    originRay.setParentSource(this);
                    originRay.setStrokeWidth(globalStrokeWidth);
                    originRay.setWavelength(whiteLight ? 380 + (400.0 / whiteLightRayCount * i) : wavelength);
                    originRay.setBrightness(brightness);

                    originRays.add(originRay);
                }
            }
            originRays.forEach(ray -> group.getChildren().add(ray.group));
            originRays.forEach(Node::toBack);
        });
        updateLightSources();
    }

    public void setWavelength(double wavelength) {
        this.wavelength = wavelength;
        for (OriginRay originRay : originRays) {
            originRay.setWavelength(wavelength + 10 * originRays.indexOf(originRay));
        }
        setBrightness(brightness);
    }

    public void setBrightness(double brightness) {
        this.brightness = brightness;
        for (OriginRay originRay : originRays) {
            originRay.setBrightness(brightness);
        }
    }

    public double getBrightness() {
        return this.brightness;
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
        return Shape.intersect(this, mouseHitbox).getLayoutBounds().getWidth() != -1;
    }
}
