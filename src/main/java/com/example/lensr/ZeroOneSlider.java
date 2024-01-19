package com.example.lensr;

import com.jfoenix.controls.JFXSlider;
import com.jfoenix.skins.JFXSliderSkin;
import javafx.beans.binding.Bindings;
import javafx.scene.control.Label;
import javafx.scene.control.Skin;

import java.text.DecimalFormat;

public class ZeroOneSlider extends JFXSlider {

    Object currentSource;
    Label thumbLabel;

    public ZeroOneSlider(Object source) {
        this.getStyleClass().add("secondary-slider");
        this.currentSource = source;
        setLayoutX(800);
        setLayoutY(100);
        setPrefHeight(40);
        setPrefWidth(150);

        setMin(0);
        setMax(1);
        setValue(0.5);

        setValueFactory(slider ->
                		Bindings.createStringBinding(
                			() -> (Math.round(getValue() * 100.0) / 100.0) + "",
                                                 			slider.valueProperty()
                 		)
      );

        LensrStart.root.getChildren().add(this);

        valueProperty().addListener((observable, oldValue, newValue) -> {
            if (currentSource instanceof Filter filter) {
                filter.setPeakTransmission(newValue.doubleValue());
            }
        });
    }

    public void setCurrentSource (Object source) {
        currentSource = source;
    }

    public void hide() {
        setVisible(false);
    }

    public void show() {
        setVisible(true);
    }
}