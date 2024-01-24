package com.example.lensr;

import com.example.lensr.objects.*;
import com.jfoenix.controls.JFXSlider;
import javafx.beans.binding.Bindings;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;

import static com.example.lensr.LensrStart.root;
import static com.example.lensr.LensrStart.toolbar;

public class ParameterSlider extends JFXSlider {

    enum ValueToChange {
        Wavelength,
        PeakTransmission,
        Passband,
        FWHM,
        Transmission,
        StartPassband,
        EndPassband,
        Reflectivity
    }

    enum SliderStyle {
        Primary,
        Secondary,
        Tertiary
    }

    Object currentSource;
    TextField textField = new TextField();
    Text label = new Text();
    public ParameterSlider(Object source, ValueToChange valueToChange, SliderStyle sliderStyle) {
        show();

        // Add the appropriate style class to the slider
        switch (sliderStyle) {
            case Primary:
                this.getStyleClass().add("primary-slider");
                setLayoutX(100);
                textField.setLayoutX(35);
                break;
            case Secondary:
                this.getStyleClass().add("secondary-slider");
                setLayoutX(350);
                textField.setLayoutX(285);
                break;
            case Tertiary:
                this.getStyleClass().add("tertiary-slider");
                setLayoutX(600);
                textField.setLayoutX(535);
                break;
        }

        double minVal = 0;
        double maxVal = 1;
        double startingVal = 0.5;

        if (valueToChange == ValueToChange.Wavelength && source instanceof BeamSource beamSource) {
            minVal = 380;
            maxVal = 780;
            startingVal = beamSource.getWavelength();
            label.setText("Wavelength");
        }
        else if (valueToChange == ValueToChange.PeakTransmission && source instanceof GaussianRolloffFilter filter) {
            startingVal = filter.getPeakTransmission();
            label.setText("Peak transmission");
        }
        else if (valueToChange == ValueToChange.Passband && source instanceof GaussianRolloffFilter filter) {
            minVal = 380;
            maxVal = 780;
            startingVal = filter.getPassband();
            filter.setPassband(startingVal);
            label.setText("Passband");
        }
        else if (valueToChange == ValueToChange.FWHM && source instanceof GaussianRolloffFilter filter) {
            maxVal = 400;
            startingVal = filter.getFWHM();
            label.setText("FWHM");
        }
        else if (valueToChange == ValueToChange.Transmission && source instanceof BrickwallFilter filter) {
            startingVal = filter.getTransmission();
            label.setText("Transmission");
        }
        else if (valueToChange == ValueToChange.StartPassband && source instanceof BrickwallFilter filter) {
            minVal = 380;
            maxVal = 780;
            startingVal = filter.getStartPassband();
            filter.setStartPassband(startingVal);
            label.setText("Start passband");
        }
        else if (valueToChange == ValueToChange.EndPassband && source instanceof BrickwallFilter filter) {
            minVal = 380;
            maxVal = 780;
            startingVal = filter.getEndPassband();
            filter.setEndPassband(startingVal);
            label.setText("End passband");
        }
        else if (valueToChange == ValueToChange.Reflectivity && source instanceof LineMirror lineMirror) {
            startingVal = lineMirror.getReflectivity();
            label.setText("Reflectivity");
        }
        else if (valueToChange == ValueToChange.Reflectivity && source instanceof EllipseMirror ellipseMirror) {
            startingVal = ellipseMirror.getReflectivity();
            label.setText("Reflectivity");
        }
        else if (valueToChange == ValueToChange.Reflectivity && source instanceof FunnyMirror funnyMirror) {
            startingVal = funnyMirror.getReflectivity();
            label.setText("Reflectivity");
        }

        // Set values for the slider
        this.currentSource = source;
        setLayoutY(25);
        setPrefHeight(40);
        setPrefWidth(150);

        setMin(minVal);
        setMax(maxVal);
        setValue(startingVal);

        textField.setLayoutY(32.5);
        textField.setPrefWidth(50);
        textField.setText(String.valueOf(Math.round(valueProperty().doubleValue() * 100.0) / 100.0));

        label.getStyleClass().add("label");
        label.setLayoutY(this.getLayoutY()-5);
        label.setLayoutX(this.getLayoutX() + this.getWidth()/2);


        // Set the decimal format to 2 decimal places if the slider is for peak transmission
        if (valueToChange == ValueToChange.PeakTransmission) {
            setValueFactory(slider ->
                    Bindings.createStringBinding(() -> (Math.round(getValue() * 100.0) / 100.0) + "",
                            slider.valueProperty()));
        }


        root.getChildren().add(this);
        root.getChildren().add(textField);
        root.getChildren().add(label);

        // Set the value of the slider to the appropriate value of the current source
        valueProperty().addListener((observable, oldValue, newValue) -> {
            double roundedValue = Math.round(newValue.doubleValue() * 100.0) / 100.0;
            textField.setText(String.valueOf(roundedValue));
            if (currentSource instanceof BeamSource beamSource && valueToChange == ValueToChange.Wavelength) {
                beamSource.setWavelength(roundedValue);
                return;
            }
            if (currentSource instanceof GaussianRolloffFilter filter && valueToChange == ValueToChange.PeakTransmission) {
                filter.setPeakTransmission(roundedValue);
                return;
            }
            if (currentSource instanceof GaussianRolloffFilter filter && valueToChange == ValueToChange.Passband) {
                filter.setPassband(roundedValue);
                return;
            }
            if (currentSource instanceof GaussianRolloffFilter filter && valueToChange == ValueToChange.FWHM) {
                filter.setFWHM(roundedValue);
                return;
            }
            if (currentSource instanceof BrickwallFilter filter && valueToChange == ValueToChange.Transmission) {
                filter.setTransmission(roundedValue);
                return;
            }
            if (currentSource instanceof BrickwallFilter filter && valueToChange == ValueToChange.StartPassband) {
                filter.setStartPassband(roundedValue);
                return;
            }
            if (currentSource instanceof BrickwallFilter filter && valueToChange == ValueToChange.EndPassband) {
                filter.setEndPassband(roundedValue);
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
        toolbar.forEach(button -> button.setVisible(true));
        label.setVisible(false);
    }

    public void show() {
        setVisible(true);
        textField.setDisable(false);
        textField.setVisible(true);
        toolbar.forEach(button -> button.setVisible(false));
        label.setVisible(true);
    }
}