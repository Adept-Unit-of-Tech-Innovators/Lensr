package com.example.lensr;

import com.example.lensr.objects.*;
import com.jfoenix.controls.JFXSlider;
import javafx.beans.binding.Bindings;
import javafx.scene.control.TextField;

import static com.example.lensr.LensrStart.root;

public class ParameterSlider extends JFXSlider {

    enum ValueToChange {
        WaveLength,
        Passband,
        PeakTransmission,
        FWHM,
        Reflectivity
    }

    enum SliderStyle {
        Primary,
        Secondary,
        Tertiary
    }

    Object currentSource;
    TextField textField = new TextField();
    public ParameterSlider(Object source, ValueToChange valueToChange, SliderStyle sliderStyle) {

        // Add the appropriate style class to the slider
        switch (sliderStyle) {
            case Primary:
                this.getStyleClass().add("primary-slider");
                setLayoutY(25);
                textField.setLayoutY(30);
                break;
            case Secondary:
                this.getStyleClass().add("secondary-slider");
                setLayoutY(75);
                textField.setLayoutY(80);
                break;
            case Tertiary:
                this.getStyleClass().add("tertiary-slider");
                setLayoutY(125);
                textField.setLayoutY(130);
                break;
        }

        double minVal = -100;
        double maxVal = -1;
        double startingVal = -100;

        if (valueToChange == ValueToChange.WaveLength && source instanceof BeamSource beamSource) {
            minVal = 380;
            maxVal = 780;
            startingVal = beamSource.getWavelength();
        }
        else if (valueToChange == ValueToChange.Passband && source instanceof GaussianRolloffFilter filter) {
            minVal = 380;
            maxVal = 780;
            startingVal = filter.getPassband();
            filter.setPassband(startingVal); // temp for coloring the filter at the start
        }
        else if (valueToChange == ValueToChange.PeakTransmission && source instanceof GaussianRolloffFilter filter) {
            minVal = 0;
            maxVal = 1;
            startingVal = filter.getPeakTransmission();
        }
        else if (valueToChange == ValueToChange.FWHM && source instanceof GaussianRolloffFilter filter) {
            minVal = 0;
            maxVal = 400;
            startingVal = filter.getFWHM();
        }
        else if (valueToChange == ValueToChange.Reflectivity && source instanceof LineMirror lineMirror) {
            minVal = 0;
            maxVal = 1;
            startingVal = lineMirror.getReflectivity();
        }
        else if (valueToChange == ValueToChange.Reflectivity && source instanceof EllipseMirror ellipseMirror) {
            minVal = 0;
            maxVal = 1;
            startingVal = ellipseMirror.getReflectivity();
        }
        else if (valueToChange == ValueToChange.Reflectivity && source instanceof FunnyMirror funnyMirror) {
            minVal = 0;
            maxVal = 1;
            startingVal = funnyMirror.getReflectivity();
        }

        // Set values for the slider
        this.currentSource = source;
        setLayoutX(800);
        setPrefHeight(40);
        setPrefWidth(150);

        setMin(minVal);
        setMax(maxVal);
        setValue(startingVal);

        textField.setLayoutX(725);
        textField.setPrefWidth(50);
        textField.setText(String.valueOf(Math.round(valueProperty().doubleValue() * 100.0) / 100.0));


        // Set the decimal format to 2 decimal places if the slider is for peak transmission
        if (valueToChange == ValueToChange.PeakTransmission) {
            setValueFactory(slider ->
                    Bindings.createStringBinding(() -> (Math.round(getValue() * 100.0) / 100.0) + "",
                            slider.valueProperty()));
        }


        root.getChildren().add(this);
        root.getChildren().add(textField);

        // Set the value of the slider to the appropriate value of the current source
        valueProperty().addListener((observable, oldValue, newValue) -> {
            double roundedValue = Math.round(newValue.doubleValue() * 100.0) / 100.0;
            textField.setText(String.valueOf(roundedValue));
            if (currentSource instanceof BeamSource beamSource && valueToChange == ValueToChange.WaveLength) {
                beamSource.setWavelength(roundedValue);
                return;
            }
            if (currentSource instanceof GaussianRolloffFilter filter && valueToChange == ValueToChange.Passband) {
                filter.setPassband(roundedValue);
                return;
            }
            if (currentSource instanceof GaussianRolloffFilter filter && valueToChange == ValueToChange.PeakTransmission) {
                filter.setPeakTransmission(roundedValue);
                return;
            }
            if (currentSource instanceof GaussianRolloffFilter filter && valueToChange == ValueToChange.FWHM) {
                filter.setFWHM(roundedValue);
                return;
            }
            if (currentSource instanceof LineMirror lineMirror && valueToChange == ValueToChange.Reflectivity) {
                lineMirror.setReflectivity(roundedValue);
                return;
            }
            if (currentSource instanceof EllipseMirror ellipseMirror && valueToChange == ValueToChange.Reflectivity) {
                ellipseMirror.setReflectivity(roundedValue);
                return;
            }
            if (currentSource instanceof FunnyMirror funnyMirror && valueToChange == ValueToChange.Reflectivity) {
                funnyMirror.setReflectivity(roundedValue);
            }
        });

        textField.setOnAction(actionEvent -> {
            double value = Double.parseDouble(textField.getText());
            setValue(value);
        });
    }

    public void setCurrentSource (Object source) {
        currentSource = source;
    }

    public void hide() {
        setVisible(false);
        textField.setDisable(true);
        textField.setVisible(false);
    }

    public void show() {
        setVisible(true);
        textField.setDisable(false);
        textField.setVisible(true);
    }
}