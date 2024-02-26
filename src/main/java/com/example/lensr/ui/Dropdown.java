package com.example.lensr.ui;

import com.example.lensr.LensrStart;
import com.jfoenix.controls.JFXComboBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.ListCell;


import static com.example.lensr.LensrStart.*;

import java.util.HashMap;

public class Dropdown extends ComboBox {
    public HashMap<String, LensrStart.Key> options = new HashMap<>();
    public Dropdown(String label, HashMap<String, LensrStart.Key> options, int layoutX, int layoutY) {
        setPromptText(label);
        setLayoutX(layoutX);
        setLayoutY(layoutY);
        getItems().addAll(options.keySet());
        this.options = options;
        getStyleClass().add("combo-box");

        // Set the button cell to display the prompt text when no item is selected
        setButtonCell(new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(getPromptText());
                } else {
                    setText(item);
                }
            }
        });


        // Set the functionality for the dropdown
        setOnAction(event -> {
            String selectedOption = (String) getValue();
            if (options.containsKey(selectedOption)) {
                Key key = options.get(selectedOption);
                if (keyPressed != key) {
                    keyPressed = key;
                    for (Control control : toolbar) {
                        if (control instanceof Dropdown dropdown && control != this) {
                            dropdown.getStyleClass().remove("combo-box-selected");
                            dropdown.setValue(dropdown.getPromptText());
                        }
                    }
                    getStyleClass().add("combo-box-selected");
                } else {
                    keyPressed = Key.None;
                    getStyleClass().remove("combo-box-selected");
                }
            }
        });
        toFront();
    }

    public void updateRender() {
        if (options.containsValue(keyPressed)) {
            setValue(options.entrySet().stream().filter(entry -> entry.getValue() == keyPressed).findFirst().get().getKey());
            getStyleClass().add("combo-box-selected");
        } else {
            getStyleClass().remove("combo-box-selected");
            setValue(getPromptText());
        }
    }
}
