package com.lensr.ui;

import com.lensr.Key;
import com.lensr.UserControls;
import com.lensr.saveloadkit.Actions;
import com.lensr.saveloadkit.SaveState;
import com.lensr.LensrStart;
import javafx.scene.control.*;


import java.util.LinkedHashMap;
import java.util.List;

public class ToolbarMenu extends Menu {
    public ToolbarMenu(String label, LinkedHashMap<String, Key> options) {
        setText(label);
        options.forEach((key, value) -> {
            MenuItem item = new MenuItem(key);
            getItems().add(item);
            item.setOnAction(event -> LensrStart.keyPressed = value);
        });
    }

    public ToolbarMenu(String label, List<String> options) {
        setText(label);
        options.forEach(option -> {
            MenuItem item = new MenuItem(option);
            getItems().add(item);
            item.setOnAction(event -> {
                switch (option) {
                    case "New (Ctrl+N)" -> Actions.clear();
                    case "Undo (Ctrl+Z)" -> Actions.undo();
                    case "Redo (Ctrl+Y)" -> Actions.redo();
                    case "Save (Ctrl+S)" -> {
                        if (Actions.lastSave == null) {
                            Actions.exportProject();
                        } else {
                            SaveState.saveProject(Actions.lastSave.getAbsolutePath());
                        }
                    }
                    case "Export (Ctrl+E)" -> Actions.exportProject();
                    case "Open (Ctrl+O)" -> Actions.importProject();
                    case "Delete (Delete)" -> UserControls.deleteCurrentObject();
                    case "Duplicate (Ctrl+D)" -> UserControls.copyCurrentObject();
                    case "Unselect (RMB)" -> LensrStart.keyPressed = Key.None;
                }
            });

        });
    }

}
