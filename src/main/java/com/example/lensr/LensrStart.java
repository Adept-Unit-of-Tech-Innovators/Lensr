package com.example.lensr;

import javafx.application.Application;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class LensrStart extends Application {
    public static final Object lock = new Object();
    public static final Color mirrorColor = Color.WHITE;
    public static final double globalStrokeWidth = 0.5;
    public static final double editPointSize = 8;
    public static final int SIZE = 1000;
    public static Pane root = new Pane();
    public static Scene scene = new Scene(root, SIZE, SIZE);
    public static List<Object> lightSources = new ArrayList<>();
    public static List<Object> mirrors = new ArrayList<>();
    public static Point2D mousePos;
    public enum Key {
        None,
        Z,
        X,
        C,
        V,
        B,
        N
    }
    public static Key keyPressed = Key.None;
    public static boolean shiftPressed = false;
    public static boolean altPressed = false;
    public static boolean isEditMode = false;
    public static boolean isMousePressed = false;
    public static boolean mouseEventHandled = false;
    public static Object editedShape;
    public static List<ToolbarButton> toolbar = new ArrayList<>();
    public static ParameterSlider wavelengthSlider;
    public static ParameterSlider passbandSlider;
    public static ParameterSlider peakTransmissionSlider;
    public static ParameterSlider FWHMSlider;

    @Override
    public void start(Stage primaryStage) {
        scene.getStylesheets().add(getClass().getResource("/main.css").toExternalForm());

        // Create toolbar buttons
        ToolbarButton lineMirrorButton = new ToolbarButton("Line Mirror", Key.Z, 25, 25);
        ToolbarButton ellipseMirrorButton = new ToolbarButton("Ellipse Mirror", Key.X, 150, 25);
        ToolbarButton funnyMirrorButton = new ToolbarButton("Funny Mirror", Key.V, 275, 25);
        ToolbarButton lightEaterButton = new ToolbarButton("Light Eater", Key.B, 400, 25);
        ToolbarButton FilterButton = new ToolbarButton("Filter", Key.N, 525, 25);
        ToolbarButton rayButton = new ToolbarButton("Ray", Key.C, 650, 25);
        toolbar.add(lineMirrorButton);
        toolbar.add(ellipseMirrorButton);
        toolbar.add(funnyMirrorButton);
        toolbar.add(lightEaterButton);
        toolbar.add(FilterButton);
        toolbar.add(rayButton);

        for (ToolbarButton button : toolbar) {
            button.setOnAction(actionEvent -> {
                if (keyPressed != button.valueToSet) {
                    keyPressed = button.valueToSet;
                }
                else {
                    keyPressed = Key.None;
                }
                toolbar.forEach(ToolbarButton::updateRender);

            });
            button.addToRoot();
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