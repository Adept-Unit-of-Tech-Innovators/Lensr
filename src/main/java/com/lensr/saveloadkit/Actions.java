package com.lensr.saveloadkit;

import com.lensr.objects.Editable;
import com.lensr.LensrStart;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Actions {
    public static File lastSave = null;

    public static void clear() {
        SaveState.autoSave();
        // Delete all objects
        List<Object> currentObjects = new ArrayList<>();
        currentObjects.addAll(LensrStart.mirrors);
        currentObjects.addAll(LensrStart.lightSources);
        currentObjects.addAll(LensrStart.lenses);
        currentObjects.forEach(object -> {
            if (object instanceof Editable editable) {
                editable.closeObjectEdit();
                editable.delete();
            }
        });
    }

    public static void undo() {
        if (LensrStart.undoSaves.size() > 1) {
            LensrStart.redoSaves.push(LensrStart.undoSaves.pop());
            LoadState.loadProject("src/main/autosaves/" + LensrStart.undoSaves.peek().getName());
        }
    }

    public static void redo() {
        if (!LensrStart.redoSaves.isEmpty()) {
            LoadState.loadProject("src/main/autosaves/" + LensrStart.redoSaves.peek().getName());
            LensrStart.undoSaves.push(LensrStart.redoSaves.pop());
        }
    }

    public static void exportProject() {
        Window window = LensrStart.scene.getWindow();
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
            } else {
                fileChooser.setInitialDirectory(new File(System.getProperty("user.dir")));
            }
            SaveState.saveProject(file.getAbsolutePath());
            lastSave = file;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void importProject() {
        Window window = LensrStart.scene.getWindow();
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
            } else {
                fileChooser.setInitialDirectory(new File(System.getProperty("user.dir")));
            }
            LoadState.loadProject(file.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
