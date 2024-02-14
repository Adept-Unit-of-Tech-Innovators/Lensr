package com.example.lensr;

import javafx.application.Application;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LensrStart extends Application {
    public static final Object lock = new Object();
    public static final Color mirrorColor = Color.WHITE;
    public static final double globalStrokeWidth = 1;
    public static final double editPointSize = 8;
    public static final int SIZE = 1000;
    public static int whiteLightRayCount = 30;
    public static int panelRayCount = 10;
    public static Pane root = new Pane();
    public static Scene scene = new Scene(root, SIZE, SIZE);
    public static List<Object> lightSources = new ArrayList<>();
    public static List<Object> mirrors = new ArrayList<>();
    public static List<Rectangle> editPoints = new ArrayList<>();
    public static Point2D mousePos;
    public enum Key {
        None,
        Z,
        X,
        C,
        V,
        B,
        N,
        M,
        K,
        J
    }
    public static Key keyPressed = Key.None;
    public static boolean shiftPressed = false;
    public static boolean altPressed = false;
    public static boolean isEditMode = false;
    public static boolean isMousePressed = false;
    public static Object editedShape;
    public static List<ToolbarButton> toolbar = new ArrayList<>();
    public static ParameterSlider wavelengthSlider;
    public static ParameterSlider passbandSlider;
    public static ParameterSlider peakTransmissionSlider;
    public static ParameterSlider FWHMSlider;
    public static ParameterSlider reflectivitySlider;
    public static ParameterSlider startPassbandSlider;
    public static ParameterSlider endPassbandSlider;
    public static ParameterToggle whiteLightToggle;
    public static final double mouseHitboxSize = 20;
    public static Rectangle mouseHitbox = new Rectangle(0, 0, mouseHitboxSize, mouseHitboxSize);
    public static ExecutorService taskPool = Executors.newFixedThreadPool(5);
    public static RayCanvas rayCanvas = new RayCanvas(SIZE, SIZE);

    @Override
    public void start(Stage primaryStage) {
        scene.getStylesheets().add(getClass().getResource("/main.css").toExternalForm());

        root.getChildren().add(rayCanvas);
        rayCanvas.toBack();

        // Create toolbar buttons
        ToolbarButton lineMirrorButton = new ToolbarButton("Line Mirror", Key.Z, 25, 25);
        ToolbarButton ellipseMirrorButton = new ToolbarButton("Ellipse Mirror", Key.X, 150, 25);
        ToolbarButton funnyMirrorButton = new ToolbarButton("Funny Mirror", Key.V, 275, 25);
        ToolbarButton lightEaterButton = new ToolbarButton("Light Eater", Key.B, 400, 25);
        ToolbarButton gaussianFilterButton = new ToolbarButton("Gaussian Filter", Key.N, 525, 25);
        ToolbarButton brickwallFilterButton = new ToolbarButton("Brickwall Filter", Key.M, 650, 25);
        ToolbarButton sensorButton = new ToolbarButton("Light Sensor", Key.K, 775, 25);
        ToolbarButton beamButton = new ToolbarButton("Beam Source", Key.C, 25, 75);
        ToolbarButton panelButton = new ToolbarButton("Panel Source", Key.J, 150, 75);
        toolbar.add(lineMirrorButton);
        toolbar.add(ellipseMirrorButton);
        toolbar.add(funnyMirrorButton);
        toolbar.add(lightEaterButton);
        toolbar.add(gaussianFilterButton);
        toolbar.add(brickwallFilterButton);
        toolbar.add(sensorButton);
        toolbar.add(beamButton);
        toolbar.add(panelButton);

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