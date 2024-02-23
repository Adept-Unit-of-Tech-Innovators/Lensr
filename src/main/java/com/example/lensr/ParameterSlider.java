package com.example.lensr;

import com.example.lensr.objects.*;
import com.jfoenix.controls.JFXSlider;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.TextField;
import javafx.scene.effect.BlendMode;
import javafx.scene.layout.*;
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
        Reflectivity,
        CoefficientA,
        CoefficientB,
    }

    enum SliderStyle {
        Primary,
        Secondary,
        Tertiary    
    }

    // Local variables
    SliderStyle sliderStyle;
    ValueToChange valueToChange;
    double minVal;
    double maxVal;
    double startingVal = getValue();
    Object currentSource;

    // GUI elements
    TextField textField = new TextField();
    Text label = new Text();
    HBox hBox = new HBox();
    VBox sliderAndLabel = new VBox();
    VBox inputField = new VBox();

    public ParameterSlider(Object source, ValueToChange valueToChange, SliderStyle sliderStyle) {
        this.valueToChange = valueToChange;
        this.sliderStyle = sliderStyle;
        this.currentSource = source;
        setCorrectValues();

        // Set up the GUI
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

        // Set values for the slider
        this.currentSource = source;
        hBox.setLayoutY(25);
        setPrefHeight(40);
        setPrefWidth(150);

        textField.setLayoutY(32.5);
        textField.setPrefWidth(50);
        textField.setText(String.valueOf(Math.round(valueProperty().doubleValue() * 1000.0) / 1000.0));

        label.getStyleClass().add("label");

        hBox.setAlignment(javafx.geometry.Pos.CENTER);
        hBox.setSpacing(10);

        inputField.setPadding(new Insets(12, 0, 0, 0));
        inputField.setAlignment(Pos.CENTER);
        sliderAndLabel.setAlignment(javafx.geometry.Pos.CENTER);


        // Set the decimal format to 2 decimal places if the slider is for peak transmission
        if (valueToChange == ValueToChange.PeakTransmission) {
            setValueFactory(slider ->
                    Bindings.createStringBinding(() -> (Math.round(getValue() * 1000.0) / 1000.0) + "",
                            slider.valueProperty()));
        }

        hBox.setBlendMode(BlendMode.SRC_ATOP);
        root.getChildren().add(hBox);

        // Set the value of the slider to the appropriate value of the current source
        valueProperty().addListener((observable, oldValue, newValue) -> {
            double roundedValue = Math.round(newValue.doubleValue() * 1000.0) / 1000.0;
            textField.setText(String.valueOf(roundedValue));
            if (currentSource instanceof BeamSource beamSource && valueToChange == ValueToChange.Wavelength) {
                beamSource.setWavelength(roundedValue);
                return;
            }
            if (currentSource instanceof PanelSource panelSource && valueToChange == ValueToChange.Wavelength) {
                panelSource.setWavelength(roundedValue);
                return;
            }
            if (currentSource instanceof GaussianRolloffFilter filter && valueToChange == ValueToChange.PeakTransmission) {
                filter.setPeakTransmission(roundedValue);
                filter.graph.drawGraph();
                return;
            }
            if (currentSource instanceof GaussianRolloffFilter filter && valueToChange == ValueToChange.Passband) {
                filter.setPassband(roundedValue);
                filter.graph.drawGraph();
                return;
            }
            if (currentSource instanceof GaussianRolloffFilter filter && valueToChange == ValueToChange.FWHM) {
                filter.setFWHM(roundedValue);
                filter.graph.drawGraph();
                return;
            }
            if (currentSource instanceof BrickwallFilter filter && valueToChange == ValueToChange.Transmission) {
                filter.setTransmission(roundedValue);
                filter.graph.drawGraph();
                return;
            }
            if (currentSource instanceof BrickwallFilter filter && valueToChange == ValueToChange.StartPassband) {
                filter.setStartPassband(roundedValue);
                filter.graph.drawGraph();
                return;
            }
            if (currentSource instanceof BrickwallFilter filter && valueToChange == ValueToChange.EndPassband) {
                filter.setEndPassband(roundedValue);
                filter.graph.drawGraph();
                return;
            }
            if (currentSource instanceof ArcMirror arcMirror && valueToChange == ValueToChange.Reflectivity) {
                arcMirror.setReflectivity(roundedValue);
                return;
            }
            if (currentSource instanceof EllipseMirror ellipseMirror && valueToChange == ValueToChange.Reflectivity) {
                ellipseMirror.setReflectivity(roundedValue);
                return;
            }
            if (currentSource instanceof FunnyMirror funnyMirror && valueToChange == ValueToChange.Reflectivity) {
                funnyMirror.setReflectivity(roundedValue);
            }
            if (currentSource instanceof SphericalLens sphericalLens && valueToChange == ValueToChange.CoefficientA) {
                sphericalLens.setCoefficientA(roundedValue);
            }
            if (currentSource instanceof SphericalLens sphericalLens && valueToChange == ValueToChange.CoefficientB) {
                sphericalLens.setCoefficientB(roundedValue);
            }
            if (currentSource instanceof Prism prism && valueToChange == ValueToChange.CoefficientA) {
                prism.setCoefficientA(roundedValue);
            }
            if (currentSource instanceof Prism prism && valueToChange == ValueToChange.CoefficientB) {
                prism.setCoefficientB(roundedValue);
            }
        });

        textField.setOnAction(actionEvent -> {
            try {
                double value = Double.parseDouble(textField.getText());
                setValue(value);
            } catch (NumberFormatException e) {
                textField.setText(String.valueOf(Math.round(getValue() * 1000.0) / 1000.0));
            }
        });
    }

    public void setCurrentSource (Object source) {
        currentSource = source;
        setCorrectValues();
    }

    public void hide() {
        // Set the value of the slider to the appropriate value of the text field
        try { setValue(Double.parseDouble(textField.getText())); }
        catch (NumberFormatException e) { textField.setText(String.valueOf(Math.round(getValue() * 1000.0) / 1000.0)); }
        textField.setFocusTraversable(false);
        // Update UI
        hBox.setVisible(false);
        hBox.setDisable(true);
        toolbar.forEach(button -> {
            button.setVisible(true);
            button.toFront();
        });
    }

    public void show() {
        // Update UI
        hBox.setVisible(true);
        hBox.setDisable(false);
        toolbar.forEach(button -> button.setVisible(false));
        toFront();
    }

    private void setCorrectValues () {
        // Default values
        minVal = 0;
        maxVal = 1;

        if (valueToChange == ValueToChange.Wavelength && currentSource instanceof BeamSource beamSource) {
            minVal = 380;
            maxVal = 780;
            startingVal = beamSource.getWavelength();
            label.setText("Wavelength");
        }
        if (valueToChange == ValueToChange.Wavelength && currentSource instanceof PanelSource panelSource) {
            minVal = 380;
            maxVal = 780;
            startingVal = panelSource.getWavelength();
            label.setText("Wavelength");
        }
        else if (valueToChange == ValueToChange.PeakTransmission && currentSource instanceof GaussianRolloffFilter filter) {
            minVal = 0;
            maxVal = 1;
            startingVal = filter.getPeakTransmission();
            label.setText("Peak transmission");
        }
        else if (valueToChange == ValueToChange.Passband && currentSource instanceof GaussianRolloffFilter filter) {
            minVal = 380;
            maxVal = 780;
            startingVal = filter.getPassband();
            filter.setPassband(startingVal);
            label.setText("Passband");
        }
        else if (valueToChange == ValueToChange.FWHM && currentSource instanceof GaussianRolloffFilter filter) {
            maxVal = 400;
            startingVal = filter.getFWHM();
            label.setText("FWHM");
        }
        else if (valueToChange == ValueToChange.Transmission && currentSource instanceof BrickwallFilter filter) {
            startingVal = filter.getTransmission();
            label.setText("Transmission");
        }
        else if (valueToChange == ValueToChange.StartPassband && currentSource instanceof BrickwallFilter filter) {
            minVal = 380;
            maxVal = 780;
            startingVal = filter.getStartPassband();
            filter.setStartPassband(startingVal);
            label.setText("Start passband");
        }
        else if (valueToChange == ValueToChange.EndPassband && currentSource instanceof BrickwallFilter filter) {
            minVal = 380;
            maxVal = 780;
            startingVal = filter.getEndPassband();
            filter.setEndPassband(startingVal);
            label.setText("End passband");
        }
        else if (valueToChange == ValueToChange.Reflectivity && currentSource instanceof ArcMirror arcMirror) {
            minVal = 0;
            maxVal = 1;
            startingVal = arcMirror.getReflectivity();
            label.setText("Reflectivity");
        }
        else if (valueToChange == ValueToChange.Reflectivity && currentSource instanceof EllipseMirror ellipseMirror) {
            minVal = 0;
            maxVal = 1;
            startingVal = ellipseMirror.getReflectivity();
            label.setText("Reflectivity");
        }
        else if (valueToChange == ValueToChange.Reflectivity && currentSource instanceof FunnyMirror funnyMirror) {
            minVal = 0;
            maxVal = 1;
            startingVal = funnyMirror.getReflectivity();
            label.setText("Reflectivity");
        }
        else if (valueToChange == ValueToChange.CoefficientA && currentSource instanceof SphericalLens sphericalLens) {
            minVal = 1;
            maxVal = 3;
            startingVal = sphericalLens.getCoefficientA();
            label.setText("Coefficient A");
        }
        else if (valueToChange == ValueToChange.CoefficientB && currentSource instanceof SphericalLens sphericalLens) {
            minVal = 0.000;
            maxVal = 0.020;
            startingVal = sphericalLens.getCoefficientB();
            label.setText("Coefficient B");
        }
        else if (valueToChange == ValueToChange.CoefficientA && currentSource instanceof Prism prism) {
            minVal = 1;
            maxVal = 3;
            startingVal = prism.getCoefficientA();
            label.setText("Coefficient A");
        }
        else if (valueToChange == ValueToChange.CoefficientB && currentSource instanceof Prism prism) {
            minVal = 0.000;
            maxVal = 0.020;
            startingVal = prism.getCoefficientB();
            label.setText("Coefficient B");
        }
        setMin(minVal);
        setMax(maxVal);
        setValue(startingVal);
    }
}