package com.example.lensr;

import com.example.lensr.objects.*;
import com.jfoenix.controls.JFXSlider;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.text.Text;

import static com.example.lensr.LensrStart.root;
import static com.example.lensr.LensrStart.toolbar;

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
    Text label = new Text();
    HBox hBox = new HBox();
    VBox sliderAndLabel = new VBox();
    VBox inputField = new VBox();
    public ParameterSlider(Object source, ValueToChange valueToChange, SliderStyle sliderStyle) {
        sliderAndLabel.getChildren().add(label);
        sliderAndLabel.getChildren().add(this);
        inputField.getChildren().add(textField);
        hBox.getChildren().add(inputField);
        hBox.getChildren().add(sliderAndLabel);
        show();

        // Add the appropriate style class to the slider
        switch (sliderStyle) {
            case Primary:
                this.getStyleClass().add("primary-slider");
                hBox.setLayoutX(50);
                break;
            case Secondary:
                this.getStyleClass().add("secondary-slider");
                hBox.setLayoutX(350);
                break;
            case Tertiary:
                this.getStyleClass().add("tertiary-slider");
                hBox.setLayoutX(650);
                break;
        }

        double minVal = -100;
        double maxVal = -1;
        double startingVal = -100;

        if (valueToChange == ValueToChange.WaveLength && source instanceof BeamSource beamSource) {
            minVal = 380;
            maxVal = 780;
            startingVal = beamSource.getWavelength();
            label.setText("Wavelength");
        }
        else if (valueToChange == ValueToChange.Passband && source instanceof Filter filter) {
            minVal = 380;
            maxVal = 780;
            startingVal = filter.getPassband();
            filter.setPassband(startingVal); // temp for coloring the filter at the start
            label.setText("Passband");
        }
        else if (valueToChange == ValueToChange.PeakTransmission && source instanceof Filter filter) {
            minVal = 0;
            maxVal = 1;
            startingVal = filter.getPeakTransmission();
            label.setText("Peak transmission");
        }
        else if (valueToChange == ValueToChange.FWHM && source instanceof Filter filter) {
            minVal = 0;
            maxVal = 400;
            startingVal = filter.getFWHM();
            label.setText("FWHM");
        }
        else if (valueToChange == ValueToChange.Reflectivity && source instanceof LineMirror lineMirror) {
            minVal = 0;
            maxVal = 1;
            startingVal = lineMirror.getReflectivity();
            label.setText("Reflectivity");
        }
        else if (valueToChange == ValueToChange.Reflectivity && source instanceof EllipseMirror ellipseMirror) {
            minVal = 0;
            maxVal = 1;
            startingVal = ellipseMirror.getReflectivity();
            label.setText("Reflectivity");
        }
        else if (valueToChange == ValueToChange.Reflectivity && source instanceof FunnyMirror funnyMirror) {
            minVal = 0;
            maxVal = 1;
            startingVal = funnyMirror.getReflectivity();
            label.setText("Reflectivity");
        }

        // Set values for the slider
        this.currentSource = source;
        hBox.setLayoutY(25);
        setPrefHeight(40);
        setPrefWidth(150);

        setMin(minVal);
        setMax(maxVal);
        setValue(startingVal);

        textField.setLayoutY(32.5);
        textField.setPrefWidth(50);
        textField.setText(String.valueOf(Math.round(valueProperty().doubleValue() * 100.0) / 100.0));

        label.getStyleClass().add("label");

        hBox.setAlignment(javafx.geometry.Pos.CENTER);
        hBox.setSpacing(10);

        inputField.setPadding(new Insets(12, 0, 0, 0));
        inputField.setAlignment(Pos.CENTER);
        sliderAndLabel.setAlignment(javafx.geometry.Pos.CENTER);


        // Set the decimal format to 2 decimal places if the slider is for peak transmission
        if (valueToChange == ValueToChange.PeakTransmission) {
            setValueFactory(slider ->
                    Bindings.createStringBinding(() -> (Math.round(getValue() * 100.0) / 100.0) + "",
                            slider.valueProperty()));
        }


        root.getChildren().add(hBox);

        // Set the value of the slider to the appropriate value of the current source
        valueProperty().addListener((observable, oldValue, newValue) -> {
            double roundedValue = Math.round(newValue.doubleValue() * 100.0) / 100.0;
            textField.setText(String.valueOf(roundedValue));
            if (currentSource instanceof BeamSource beamSource && valueToChange == ValueToChange.WaveLength) {
                beamSource.setWavelength(roundedValue);
                return;
            }
            if (currentSource instanceof Filter filter && valueToChange == ValueToChange.Passband) {
                filter.setPassband(roundedValue);
                return;
            }
            if (currentSource instanceof Filter filter && valueToChange == ValueToChange.PeakTransmission) {
                filter.setPeakTransmission(roundedValue);
                return;
            }
            if (currentSource instanceof Filter filter && valueToChange == ValueToChange.FWHM) {
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
        hBox.setVisible(false);
        hBox.setDisable(true);
        toolbar.forEach(button -> {
            button.setVisible(true);
            button.toFront();
        });
    }

    public void show() {
        toFront();
        hBox.setVisible(true);
        hBox.setDisable(false);
        toolbar.forEach(button -> button.setVisible(false));
    }
}