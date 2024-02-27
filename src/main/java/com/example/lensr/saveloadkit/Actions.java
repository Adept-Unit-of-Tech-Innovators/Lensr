package com.example.lensr.saveloadkit;

import com.example.lensr.objects.Editable;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.example.lensr.LensrStart.*;

public class Actions {
    public static File lastSave = null;
    public static void clear() {
        SaveState.autoSave();
        // Delete all objects
        List<Object> currentObjects = new ArrayList<>();
        currentObjects.addAll(mirrors);
        currentObjects.addAll(lightSources);
        currentObjects.addAll(lenses);
        currentObjects.forEach(object -> {
            if (object instanceof Editable editable) {
                editable.closeObjectEdit();
                editable.delete();
            }
        });
    }

    public static void undo() {
        if (undoSaves.size() > 1) {
            redoSaves.push(undoSaves.pop());
            LoadState.loadProject("autosaves/" + undoSaves.peek().getName());
        }
    }

    public static void redo() {
        if (!redoSaves.isEmpty()) {
            LoadState.loadProject("autosaves/" + redoSaves.peek().getName());
            undoSaves.push(redoSaves.pop());
        }
    }

    public static void exportProject() {
        Window window  = scene.getWindow();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Project");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Lensr Project", "*.lensr"));
        fileChooser.setInitialFileName("project");

        try {
            File file = fileChooser.showSaveDialog(window);
            if (file == null) {
                return;
            }
            if (file.getParentFile() != null) {
                fileChooser.setInitialDirectory(file.getParentFile());
            }
            else {
                fileChooser.setInitialDirectory(new File(System.getProperty("user.dir")));
            }
            SaveState.saveProject(file.getAbsolutePath());
            lastSave = file;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void importProject() {
        Window window  = scene.getWindow();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Project");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Lensr Project", "*.lensr"));

        try {
            File file = fileChooser.showOpenDialog(window);
            if (file == null) {
                return;
            }
            if (file.getParentFile() != null) {
                fileChooser.setInitialDirectory(file.getParentFile());
            }
            else {
                fileChooser.setInitialDirectory(new File(System.getProperty("user.dir")));
            }
            LoadState.loadProject(file.getAbsolutePath());
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }
}
