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
    public static int whiteLightRayCount = 1000;
    public static int panelRayCount = 30;
    public static Pane root = new Pane();
    public static Scene scene = new Scene(root, SIZE, SIZE);
    public static List<Object> lightSources = new ArrayList<>();
    public static List<Object> mirrors = new ArrayList<>();
    public static List<Rectangle> editPoints = new ArrayList<>();
    public static List<Object> lenses = new ArrayList<>();
    public static Point2D mousePos;
    public enum Key {
        None,
        Z,
        A,
        X,
        C,
        V,
        B,
        N,
        M,
        K,
        J,
        L,
        P,
        H,
        G
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
    public static ParameterSlider coefficientASlider;
    public static ParameterSlider coefficientBSlider;
    public static ParameterSlider numberOfRaysSlider;
    public static ParameterSlider fieldOfViewSlider;
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
        ToolbarButton arcMirrorButton = new ToolbarButton("Arc Mirror", Key.A, 150, 25);
        ToolbarButton ellipseMirrorButton = new ToolbarButton("Ellipse Mirror", Key.X, 275, 25);
        ToolbarButton funnyMirrorButton = new ToolbarButton("Funny Mirror", Key.V, 400, 25);
        ToolbarButton lightEaterButton = new ToolbarButton("Light Eater", Key.B, 525, 25);
        ToolbarButton gaussianFilterButton = new ToolbarButton("Gaussian Filter", Key.N, 650, 25);
        ToolbarButton brickwallFilterButton = new ToolbarButton("Brickwall Filter", Key.M, 775, 25);
        ToolbarButton sensorButton = new ToolbarButton("Light Sensor", Key.K, 25, 75);
        ToolbarButton lensButton = new ToolbarButton("Lens", Key.L, 150, 75);
        ToolbarButton prismButton = new ToolbarButton("Prism", Key.P, 275, 75);
        ToolbarButton beamButton = new ToolbarButton("Beam Source", Key.C, 400, 75);
        ToolbarButton panelButton = new ToolbarButton("Panel Source", Key.J, 525, 75);
        ToolbarButton fullPointButton = new ToolbarButton("Full PS", Key.H, 650, 75);
        ToolbarButton pointButton = new ToolbarButton("Part PS", Key.G, 775, 75);
        toolbar.add(lineMirrorButton);
        toolbar.add(arcMirrorButton);
        toolbar.add(ellipseMirrorButton);
        toolbar.add(funnyMirrorButton);
        toolbar.add(lightEaterButton);
        toolbar.add(gaussianFilterButton);
        toolbar.add(brickwallFilterButton);
        toolbar.add(sensorButton);
        toolbar.add(lensButton);
        toolbar.add(prismButton);
        toolbar.add(beamButton);
        toolbar.add(panelButton);
        toolbar.add(fullPointButton);
        toolbar.add(pointButton);

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