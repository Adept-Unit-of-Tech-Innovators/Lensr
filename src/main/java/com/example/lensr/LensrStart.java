package com.example.lensr;

import com.example.lensr.objects.lightsources.RaySource;
import com.example.lensr.objects.lightsources.BeamSource;
import com.example.lensr.objects.lightsources.PointSource;
import com.example.lensr.objects.misc.LightSensor;
import com.example.lensr.ui.*;
import javafx.application.Application;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.control.MenuBar;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Screen;
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
    public static int WIDTH = (int) Screen.getPrimary().getBounds().getWidth()*3/4;
    public static int HEIGHT = (int) Screen.getPrimary().getBounds().getHeight()*3/4;
    public static int whiteLightRayCount = 50;
    public static Pane root = new Pane();
    public static Scene scene = new Scene(root, WIDTH, HEIGHT);
    public static List<Object> lightSources = new ArrayList<>();
    public static List<Object> mirrors = new ArrayList<>();
    public static List<Rectangle> editPoints = new ArrayList<>();
    public static List<Object> lenses = new ArrayList<>();
    public static Point2D mousePos;
    public enum Key {
        None,
        Q,
        W,
        E,
        R,
        A,
        S,
        D,
        F,
        Z,
        X,
        C,
        V,
        L,
        P
    }
    public static Key keyPressed = Key.None;
    public static boolean shiftPressed = false;
    public static boolean altPressed = false;
    public static boolean isEditMode = true;
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
    public static ParameterSlider numberOfRaysSlider = new ParameterSlider(null, ValueToChange.NumberOfRays, SliderStyle.Tertiary);
    public static ParameterSlider fieldOfViewSlider = new ParameterSlider(null, ValueToChange.FieldOfView, SliderStyle.Quaternary);
    public static ParameterSlider brightnessSlider = new ParameterSlider(null, ValueToChange.Brightness, SliderStyle.Secondary);
    public static ParameterSlider transparencySlider = new ParameterSlider(null, ValueToChange.Transparency, SliderStyle.Tertiary);
    public static ParameterToggle whiteLightToggle = new ParameterToggle(null, "White Light", ParameterToChange.WhiteLight);
    public static final double mouseHitboxSize = 20;
    public static Rectangle mouseHitbox = new Rectangle(0, 0, mouseHitboxSize, mouseHitboxSize);
    public static ExecutorService taskPool = Executors.newFixedThreadPool(5);
    public static RayCanvas rayCanvas = new RayCanvas(WIDTH, HEIGHT);
    public static Stack<File> undoSaves = new Stack<>();
    public static Stack<File> redoSaves = new Stack<>();
    @Override
    public void start(Stage primaryStage) {
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/main.css")).toExternalForm());

        // Resize menuBar when window is resized
        primaryStage.widthProperty().addListener((obs, oldVal, newVal) -> {
            WIDTH = newVal.intValue();
            menuBar.setPrefWidth(WIDTH);
            rayCanvas.setWidth(WIDTH);
        });

        primaryStage.heightProperty().addListener((obs, oldVal, newVal) -> {
            HEIGHT = newVal.intValue();
            rayCanvas.setHeight(HEIGHT);
        });

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
        ToolbarMenu file = new ToolbarMenu("File", fileActions);
        menuBar.getMenus().add(file);

        List<String> editActions = new ArrayList<>();
        editActions.add("Unselect (RMB)");
        editActions.add("Delete (Delete)");
        editActions.add("Duplicate (Ctrl+D)");
        ToolbarMenu edit = new ToolbarMenu("Edit", editActions);
        menuBar.getMenus().add(edit);

        LinkedHashMap<String, Key> lightSourceActions = new LinkedHashMap<>();
        lightSourceActions.put("Ray Source (Q)", Key.Q);
        lightSourceActions.put("Beam Source (W)", Key.W);
        lightSourceActions.put("Full Point Source (E)", Key.E);
        lightSourceActions.put("Partial Point Source (R)", Key.R);
        ToolbarMenu lightSources = new ToolbarMenu("Light Sources", lightSourceActions);
        menuBar.getMenus().add(lightSources);

        LinkedHashMap<String, Key> mirrorActions = new LinkedHashMap<>();
        mirrorActions.put("Line Mirror (A)", Key.A);
        mirrorActions.put("Ellipse Mirror (S)", Key.S);
        mirrorActions.put("Arc Mirror (D)", Key.D);
        mirrorActions.put("Funny Mirror (F)", Key.F);
        ToolbarMenu mirrors = new ToolbarMenu("Mirrors", mirrorActions);
        menuBar.getMenus().add(mirrors);

        LinkedHashMap<String, Key> glassActions = new LinkedHashMap<>();
        glassActions.put("Lens (L)", Key.L);
        glassActions.put("Prism (P)", Key.P);
        ToolbarMenu glass = new ToolbarMenu("Glass", glassActions);
        menuBar.getMenus().add(glass);

        LinkedHashMap<String, Key> miscActions = new LinkedHashMap<>();
        miscActions.put("Gaussian Filter (Z)", Key.Z);
        miscActions.put("Brickwall Filter (X)", Key.X);
        miscActions.put("Light Sensor (C)", Key.C);
        miscActions.put("Light Eater (V)", Key.V);
        ToolbarMenu misc = new ToolbarMenu("Misc", miscActions);
        menuBar.getMenus().add(misc);

        root.getChildren().add(menuBar);
        menuBar.setViewOrder(-2);
        menuBar.setPrefWidth(WIDTH);

        UserControls.setUserControls();

        primaryStage.setTitle("Lensr: Also try Minecraft!");
        primaryStage.setScene(scene);

        primaryStage.show();
    }

    public static void updateLightSources() {
        mirrors.stream().filter(mirror -> mirror instanceof LightSensor).forEach(mirror -> ((LightSensor) mirror).getDetectedRays().clear());
        LensrStart.rayCanvas.clear();
        for (Object lightSource : LensrStart.lightSources) {
            if (lightSource instanceof RaySource raySource) {
                raySource.update();
            }
            else if (lightSource instanceof BeamSource beamSource) {
                beamSource.update();
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