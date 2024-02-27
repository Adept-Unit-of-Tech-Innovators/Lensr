package com.example.lensr;

import com.example.lensr.objects.lightsources.BeamSource;
import com.example.lensr.objects.lightsources.PanelSource;
import com.example.lensr.objects.lightsources.PointSource;
import com.example.lensr.ui.*;
import javafx.application.Application;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.control.MenuBar;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.File;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.example.lensr.ui.ParameterSlider.*;
import static com.example.lensr.ui.ParameterToggle.*;

public class LensrStart extends Application {
    public static final Object lock = new Object();
    public static final Color mirrorColor = Color.WHITE;
    public static final double globalStrokeWidth = 1;
    public static final double editPointSize = 8;
    public static final int SIZE = 1000;
    public static int whiteLightRayCount = 50;
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
    public static MenuBar menuBar = new MenuBar();
    public static ParameterSlider wavelengthSlider = new ParameterSlider(null, ValueToChange.Wavelength, SliderStyle.Primary);
    public static ParameterSlider passbandSlider = new ParameterSlider(null, ValueToChange.Passband, SliderStyle.Secondary);
    public static ParameterSlider peakTransmissionSlider = new ParameterSlider(null, ValueToChange.PeakTransmission, SliderStyle.Primary);
    public static ParameterSlider FWHMSlider = new ParameterSlider(null, ValueToChange.FWHM, SliderStyle.Tertiary);
    public static ParameterSlider reflectivitySlider = new ParameterSlider(null, ValueToChange.Reflectivity, SliderStyle.Primary);
    public static ParameterSlider startPassbandSlider = new ParameterSlider(null, ValueToChange.StartPassband, SliderStyle.Secondary);
    public static ParameterSlider endPassbandSlider = new ParameterSlider(null, ValueToChange.EndPassband, SliderStyle.Tertiary);
    public static ParameterSlider coefficientASlider = new ParameterSlider(null, ValueToChange.CoefficientA, SliderStyle.Primary);
    public static ParameterSlider coefficientBSlider = new ParameterSlider(null, ValueToChange.CoefficientB, SliderStyle.Secondary);
    public static ParameterSlider numberOfRaysSlider = new ParameterSlider(null, ValueToChange.NumberOfRays, SliderStyle.Secondary);
    public static ParameterSlider fieldOfViewSlider = new ParameterSlider(null, ValueToChange.FieldOfView, SliderStyle.Tertiary);
    public static ParameterSlider brightnessSlider = new ParameterSlider(null, ValueToChange.Brightness, SliderStyle.Quaternary);
    public static ParameterToggle whiteLightToggle = new ParameterToggle(null, "White Light", ParameterToChange.WhiteLight);
    public static final double mouseHitboxSize = 20;
    public static Rectangle mouseHitbox = new Rectangle(0, 0, mouseHitboxSize, mouseHitboxSize);
    public static ExecutorService taskPool = Executors.newFixedThreadPool(5);
    public static RayCanvas rayCanvas = new RayCanvas(SIZE, SIZE);
    public static Stack<File> undoSaves = new Stack<>();
    public static Stack<File> redoSaves = new Stack<>();

    @Override
    public void start(Stage primaryStage) {
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/main.css")).toExternalForm());

        root.getChildren().add(rayCanvas);
        rayCanvas.toBack();

        // Create toolbar buttons
        List<String> fileActions = new ArrayList<>();
        fileActions.add("New (Ctrl+N)");
        fileActions.add("Undo (Ctrl+Z)");
        fileActions.add("Redo (Ctrl+Y)");
        fileActions.add("Save (Ctrl+S)");
        fileActions.add("Export (Ctrl+E)");
        fileActions.add("Open (Ctrl+O)");
        Dropdown file = new Dropdown("File", fileActions);
        menuBar.getMenus().add(file);

        HashMap<String, Key> lightSourceActions = new HashMap<>();
        lightSourceActions.put("Beam Source", Key.C);
        lightSourceActions.put("Panel Source", Key.J);
        lightSourceActions.put("Full Point Source", Key.H);
        lightSourceActions.put("Partial Point Source", Key.G);
        Dropdown lightSources = new Dropdown("Light Sources", lightSourceActions);
        menuBar.getMenus().add(lightSources);

        HashMap<String, Key> mirrorActions = new HashMap<>();
        mirrorActions.put("Line Mirror", Key.Z);
        mirrorActions.put("Arc Mirror", Key.A);
        mirrorActions.put("Ellipse Mirror", Key.X);
        mirrorActions.put("Funny Mirror", Key.V);
        Dropdown mirrors = new Dropdown("Mirrors", mirrorActions);
        menuBar.getMenus().add(mirrors);

        HashMap<String, Key> glassActions = new HashMap<>();
        glassActions.put("Lens", Key.L);
        glassActions.put("Prism", Key.P);
        Dropdown glass = new Dropdown("Glass", glassActions);
        menuBar.getMenus().add(glass);

        HashMap<String, Key> miscActions = new HashMap<>();
        miscActions.put("Light Eater", Key.B);
        miscActions.put("Gaussian Filter", Key.N);
        miscActions.put("Brickwall Filter", Key.M);
        miscActions.put("Light Sensor", Key.K);
        Dropdown misc = new Dropdown("Misc", miscActions);
        menuBar.getMenus().add(misc);

        root.getChildren().add(menuBar);
        menuBar.setPrefWidth(SIZE);

        UserControls.setUserControls();

        primaryStage.setTitle("rtx 5090ti testing place");
        primaryStage.setScene(scene);

        primaryStage.show();
    }

    public static void updateLightSources() {
        LensrStart.rayCanvas.clear();
        for (Object lightSource : LensrStart.lightSources) {
            if (lightSource instanceof BeamSource beamSource) {
                beamSource.update();
            }
            else if (lightSource instanceof PanelSource panelSource) {
                panelSource.update();
            }
            else if (lightSource instanceof PointSource pointSource) {
                pointSource.update();
            }
        }
    }


    public static void main(String[] args) {
        launch();
    }
}