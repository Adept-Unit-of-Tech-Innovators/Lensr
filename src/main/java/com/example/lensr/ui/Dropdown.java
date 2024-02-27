package com.example.lensr.ui;

import com.example.lensr.LensrStart;
import com.example.lensr.saveloadkit.Actions;
import com.example.lensr.saveloadkit.SaveState;
import javafx.scene.control.*;


import static com.example.lensr.LensrStart.*;

import java.util.HashMap;
import java.util.List;

public class Dropdown extends Menu {
    public Dropdown(String label, HashMap<String, LensrStart.Key> options) {
        setText(label);
        options.forEach((key, value) -> {
            MenuItem item = new MenuItem(key);
            getItems().add(item);
            item.setOnAction(event -> keyPressed = value);
        });
    }

    public Dropdown(String label, List<String> options) {
        setText(label);
        options.forEach(option -> {
            MenuItem item = new MenuItem(option);
            getItems().add(item);
            item.setOnAction(event -> {
                if (option.equals("New (Ctrl+N)")) {
                    Actions.clear();
                }
                if (option.equals("Undo (Ctrl+Z)")) {
                    Actions.undo();
                }
                else if (option.equals("Redo (Ctrl+Y)")) {
                    Actions.redo();
                }
                else if (option.equals("Save (Ctrl+S)")) {
                    if (Actions.lastSave == null) {
                        Actions.exportProject();
                    }
                    else {
                        SaveState.saveProject(Actions.lastSave.getName());
                    }
                }
                else if (option.equals("Export (Ctrl+E)")) {
                    Actions.exportProject();
                }
                else if (option.equals("Open (Ctrl+O)")) {
                    Actions.importProject();
                }
            });
        });
    }

}
