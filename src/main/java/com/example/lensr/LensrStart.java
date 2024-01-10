package com.example.lensr;

import javafx.application.Application;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LensrStart extends Application {
    public static final Object lock = new Object();
    public static final Color mirrorColor = Color.WHITE;
    public static final double globalStrokeWidth = 0.5;
    public static final double editPointSize = 8;
    public static final int SIZE = 1000;
    public static Pane root = new Pane();
    public static Scene scene = new Scene(root, SIZE, SIZE);
    public static List<Ray> rays = new ArrayList<>();
    public static List<Object> mirrors = new ArrayList<>();
    public static Point2D mousePos;
    public static MutableValue xPressed = new MutableValue(false);
    public static MutableValue zPressed = new MutableValue(false);
    public static MutableValue vPressed = new MutableValue(false);
    public static boolean shiftPressed = false;
    public static boolean altPressed = false;
    public static boolean isEditMode = false;
    public static boolean isMousePressed = false;
    public static Object editedShape;
    public static List<ToolbarButton> toolbar = new ArrayList<>();

    @Override
    public void start(Stage primaryStage) {
        scene.getStylesheets().add(getClass().getResource("/main.css").toExternalForm());

        // Commented out because it doesn't work properly yet as it works with the old ray system
        // WavelengthSlider wavelengthSlider = new WavelengthSlider(rays, rayReflections);

        // Create toolbar buttons
        ToolbarButton lineMirrorButton = new ToolbarButton("Line Mirror", zPressed, new MutableValue[]{xPressed, vPressed}, 25, 25);
        ToolbarButton ellipseMirrorButton = new ToolbarButton("Ellipse Mirror", xPressed, new MutableValue[]{zPressed, vPressed}, 150, 25);
        ToolbarButton funnyMirrorButton = new ToolbarButton("Funny Mirror", vPressed, new MutableValue[]{zPressed, xPressed}, 275, 25);
        toolbar.add(lineMirrorButton);
        toolbar.add(ellipseMirrorButton);
        toolbar.add(funnyMirrorButton);

        lineMirrorButton.setOnAction(actionEvent -> {
            lineMirrorButton.variableToChange.setValueAndCloseEdit(!lineMirrorButton.variableToChange.getValue());
            Arrays.asList(lineMirrorButton.oppositeVariables).forEach(item -> item.setValue(false));
            toolbar.forEach(ToolbarButton::updateRender);

        });

        ellipseMirrorButton.setOnAction(actionEvent -> {
            ellipseMirrorButton.variableToChange.setValueAndCloseEdit(!ellipseMirrorButton.variableToChange.getValue());
            Arrays.asList(ellipseMirrorButton.oppositeVariables).forEach(item -> item.setValue(false));
            toolbar.forEach(ToolbarButton::updateRender);

        });

        funnyMirrorButton.setOnAction(actionEvent -> {
            funnyMirrorButton.variableToChange.setValueAndCloseEdit(!funnyMirrorButton.variableToChange.getValue());
            Arrays.asList(funnyMirrorButton.oppositeVariables).forEach(item -> item.setValue(false));
            toolbar.forEach(ToolbarButton::updateRender);
        });

        lineMirrorButton.addToRoot();
        ellipseMirrorButton.addToRoot();
        funnyMirrorButton.addToRoot();
        for (ToolbarButton button : toolbar) {
            button.disableProperty().setValue(true);
        }

        UserControls.setUserControls();

        primaryStage.setTitle("rtx 5090ti testing place");
        primaryStage.setScene(scene);

        primaryStage.show();
    }


    public static void main(String[] args) {
        launch();
    }
}