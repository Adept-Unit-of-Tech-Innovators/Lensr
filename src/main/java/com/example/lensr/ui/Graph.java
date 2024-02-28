package com.example.lensr.ui;

import com.example.lensr.objects.misc.BrickwallFilter;
import com.example.lensr.objects.misc.GaussianRolloffFilter;
import com.example.lensr.objects.misc.LightSensor;
import javafx.scene.Group;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.example.lensr.LensrStart.*;

public class Graph extends Canvas {
    public Group group = new Group();
    List<Double> data = new ArrayList<>();
    Object dataSource;
    TextField dataStartTextField;
    TextField dataEndTextField;
    Text yAxisMaxLabel;
    Text yAxisMinLabel;
    double graphStart = 380;
    double graphEnd = 780;
    public Graph(double x, double y, double width, double height) {
        super(width, height);
        setTranslateX(x);
        setTranslateY(y);

        getGraphicsContext2D().setStroke(Color.BLACK);
        getGraphicsContext2D().setLineWidth(3);
        getGraphicsContext2D().strokeRect(0, 0, width, height);

        dataStartTextField = new TextField("380.0");
        dataStartTextField.setTranslateX(x);
        dataStartTextField.setTranslateY(y + height + 10);
        dataStartTextField.setPrefWidth(50);

        dataStartTextField.setOnAction(event -> {
            try {
                double textFieldValue = Double.parseDouble(dataStartTextField.getText());


                if (textFieldValue < 380) graphStart = 380;
                else if (textFieldValue > graphEnd) graphStart = Math.max(380, graphEnd - 1);
                else graphStart = textFieldValue;
            }
            catch (NumberFormatException ignored) {}

            dataStartTextField.setText(String.valueOf(graphStart));
            clear();
            drawGraph();
        });

        dataEndTextField = new TextField("780.0");
        dataEndTextField.setTranslateX(x + width - 50);
        dataEndTextField.setTranslateY(y + height + 10);
        dataEndTextField.setPrefWidth(50);

        dataEndTextField.setOnAction(event -> {
            try {
                double textFieldValue = Double.parseDouble(dataEndTextField.getText());

                if (textFieldValue > 780) graphEnd = 780;
                else if (textFieldValue < graphStart) graphEnd = Math.min(780, graphStart + 1);
                else graphEnd = textFieldValue;
            }
            catch (NumberFormatException ignored) {}

            dataEndTextField.setText(String.valueOf(graphEnd));
            clear();
            drawGraph();
        });

        yAxisMinLabel = new Text("0.0");
        yAxisMinLabel.setFill(Color.web("#DBDEDC"));
        yAxisMinLabel.setTranslateX(x - 25);
        yAxisMinLabel.setTranslateY(y + height);

        yAxisMaxLabel = new Text("1.0");
        yAxisMaxLabel.setFill(Color.web("#DBDEDC"));
        yAxisMaxLabel.setTranslateX(x - 25);
        yAxisMaxLabel.setTranslateY(y + 10);

        group.getChildren().addAll(dataStartTextField, dataEndTextField, yAxisMaxLabel, yAxisMinLabel);
        group.getChildren().add(this);
        group.setViewOrder(-1);

        root.requestFocus();
    }

    public void setDataSource(Object dataSource) {
        this.dataSource = dataSource;
    }

    private void getData(GaussianRolloffFilter filter) {
        data.clear();

        for (double wavelength = graphStart; wavelength < graphEnd; wavelength += (graphEnd - graphStart) / getWidth() ) {
            double sigma = filter.getFWHM() / (2 * Math.sqrt(2 * Math.log(2)));
            double exponent = -0.5 * Math.pow( (wavelength - filter.getPassband()) / sigma, 2);
            double transmission = filter.getPeakTransmission() * Math.pow(Math.E, exponent);
            data.add(transmission);
        }
    }

    private void getData(BrickwallFilter filter) {
        data.clear();

        for (double wavelength = graphStart; wavelength < graphEnd; wavelength += (graphEnd - graphStart) /  getWidth()) {
            if (filter.getStartPassband() <= wavelength &&  filter.getEndPassband() >= wavelength) {
                data.add(filter.getPeakTransmission());
            }
            else data.add(0.0);
        }
    }

    private void getData(LightSensor lightSensor) {
        data.clear();
        data = new ArrayList<>((int) getWidth());
        data.addAll(Collections.nCopies((int) getWidth(), 0.0));

        for (int i = 0; i < lightSensor.getDetectedRays().size(); i++) {
            int dataIndex = (int) Math.round( (lightSensor.getDetectedRays().get(i).getWavelength() - graphStart) * getWidth() / (graphEnd - graphStart));
            if (dataIndex < 0 || dataIndex >= getWidth()) continue;
            data.set(dataIndex, data.get(dataIndex) + lightSensor.getDetectedRays().get(i).getBrightness());
        }

    }

    public void clear() {
        getGraphicsContext2D().clearRect(0, 0, getWidth(), getHeight());
    }

    private void updateStroke() {
        Paint stroke = getGraphicsContext2D().getStroke();
        getGraphicsContext2D().setStroke(Color.BLACK);
        getGraphicsContext2D().setLineWidth(1);
        getGraphicsContext2D().strokeRect(0, 0, getWidth(), getHeight());
        getGraphicsContext2D().setStroke(stroke);
    }

    private void drawGraphGrid(double maxValue) {
        getGraphicsContext2D().setLineWidth(1);
        getGraphicsContext2D().setStroke(Color.GRAY);
        for (int i = 0; i < 10; i++) {
            double x = i * getWidth() / 10;
            getGraphicsContext2D().strokeLine(x, 0, x, getHeight());
        }
        for (int i = 0; i < 10; i++) {
            double y = i * getHeight() / 10;
            getGraphicsContext2D().strokeLine(0, y, getWidth(), y);
        }
        yAxisMaxLabel.setText(String.valueOf(maxValue));
        updateStroke();
    }

    public void drawGraph() {
        clear();

        double maxValue = -1;

        // Get graph data
        if (dataSource instanceof GaussianRolloffFilter filter) {
            getData(filter);
            maxValue = 1;
        }
        else if (dataSource instanceof BrickwallFilter filter) {
            getData(filter);
            maxValue = 1;
        }
        else if (dataSource instanceof LightSensor lightSensor) {
            getData(lightSensor);
            maxValue = data.stream().max(Double::compareTo).orElse(1.0);
            maxValue = Math.round(maxValue * 100) / 100.0;
        }

        if (data.isEmpty()) return;

        drawGraphGrid(maxValue);

        // Draw graph using graphical context
        getGraphicsContext2D().setLineWidth(1);
        getGraphicsContext2D().moveTo(0, getHeight() + 1);

        for (int i = 0; i < data.size(); i++) {
            setWavelength((i * (graphEnd - graphStart) / getWidth()) + graphStart);
            double x = i * getWidth() / data.size();
            double y = (getHeight() - 2) - data.get(i) * getHeight() / maxValue + 1;
            getGraphicsContext2D().strokeLine(x, getHeight() - 1, x, y + 2);
        }
        updateStroke();
    }

    private void setWavelength(double wavelength) {
        double factor;
        double red;
        double green;
        double blue;

        int intensityMax = 255;
        double Gamma = 0.8;

        // adjusting to transform between different colors for example green and yellow with addition of red and absence of blue
        // what
        if ((wavelength >= 380) && (wavelength < 440)) {
            red = -(wavelength - 440.0) / (440.0 - 380.0);
            green = 0.0;
            blue = 1.0;
        } else if ((wavelength >= 440) && (wavelength < 490)) {
            red = 0.0;
            green = (wavelength - 440.0) / (490.0 - 440.0);
            blue = 1.0;
        } else if ((wavelength >= 490) && (wavelength < 510)) {
            red = 0.0;
            green = 1.0;
            blue = -(wavelength - 510.0) / (510.0 - 490.0);
        } else if ((wavelength >= 510) && (wavelength < 580)) {
            red = (wavelength - 510.0) / (580.0 - 510.0);
            green = 1.0;
            blue = 0.0;
        } else if ((wavelength >= 580) && (wavelength < 645)) {
            red = 1.0;
            green = -(wavelength - 645.0) / (645.0 - 580.0);
            blue = 0.0;
        } else if ((wavelength >= 645) && (wavelength < 781)) {
            red = 1.0;
            green = 0.0;
            blue = 0.0;
        } else {
            red = 0.0;
            green = 0.0;
            blue = 0.0;
        }

        // Let the intensity fall off near the vision limits
        if ((wavelength >= 380) && (wavelength < 420)) {
            factor = 0.3 + 0.7 * (wavelength - 380) / (420 - 380);
        } else if ((wavelength >= 420) && (wavelength < 701)) {
            factor = 1.0;
        }
        else if ((wavelength >= 701) && (wavelength < 781)) {
            factor = 0.3 + 0.7 * (780 - wavelength) / (780 - 700);
        } else {
            factor = 0.0;
        }

        if (red != 0) {
            red = Math.round(intensityMax * Math.pow(red * factor, Gamma));
        }

        if (green != 0) {
            green = Math.round(intensityMax * Math.pow(green * factor, Gamma));
        }

        if (blue != 0) {
            blue = Math.round(intensityMax * Math.pow(blue * factor, Gamma));
        }

        getGraphicsContext2D().setStroke(Color.rgb((int) red, (int) green, (int) blue));
    }

    public void hide() {
        group.setVisible(false);
    }

    public void show() {
        group.setVisible(true);
    }
}
