package com.example.lensr;

import com.jfoenix.controls.JFXSlider;
import com.jfoenix.controls.JFXTextField;
import javafx.beans.binding.Bindings;
import javafx.scene.control.TextField;

import static com.example.lensr.LensrStart.root;

public class ParameterSlider extends JFXSlider {

    enum ValueToChange {
        WaveLength,
        Passband,
        PeakTransmission,
        FWHM
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

        double minVal = 0;
        double maxVal = 1;

        if (valueToChange == ValueToChange.WaveLength || valueToChange == ValueToChange.Passband) {
            minVal = 380;
            maxVal = 780;
        }
        else if (valueToChange == ValueToChange.FWHM) {
            minVal = 0;
            maxVal = 400; // Passband max - passband min
        }

        // Set values for the slider
        this.currentSource = source;
        setLayoutX(800);
        setPrefHeight(40);
        setPrefWidth(150);

        setMin(minVal);
        setMax(maxVal);
        setValue((minVal + maxVal) / 2);

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
            textField.setText(String.valueOf(Math.round(newValue.doubleValue() * 100.0) / 100.0));
            if (currentSource instanceof BeamSource beamSource && valueToChange == ValueToChange.WaveLength) {
                beamSource.setWavelength(newValue.doubleValue());
                return;
            }
            if (currentSource instanceof Filter filter && valueToChange == ValueToChange.Passband) {
                filter.setPassband(newValue.doubleValue());
                return;
            }
            if (currentSource instanceof Filter filter && valueToChange == ValueToChange.PeakTransmission) {
                filter.setPeakTransmission(newValue.doubleValue());
                return;
            }
            if (currentSource instanceof Filter filter && valueToChange == ValueToChange.FWHM) {
                filter.setFWHM(newValue.doubleValue());
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