package com.example.lensr.saveloadkit;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import static com.example.lensr.LensrStart.*;

public class SaveState {
    private static final int MAX_AUTOSAVES = 1000;
    public static void saveProject(String filename) {
        try {
            FileOutputStream fileOut = FileUtils.openOutputStream(new File(filename), false);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            for (Object mirror : mirrors) {
                out.writeObject(mirror);
            }
            for (Object lightSource : lightSources) {
                out.writeObject(lightSource);
            }
            for (Object lens : lenses) {
                out.writeObject(lens);
            }
            out.close();
            fileOut.close();
        } catch (IOException i) {
            throw new RuntimeException("Error saving Project", i);
        }
    }

    // Used for undo/redo
    public static void autoSave() {
        StringBuilder filename = new StringBuilder("autosaves/autosave");
        filename.append(System.currentTimeMillis());
        filename.append(".lensr");
        saveProject(filename.toString());
        File file = new File(filename.toString());
        file.deleteOnExit();
        undoSaves.push(file);

        if (undoSaves.size() > MAX_AUTOSAVES) {
            File oldestAutosave = undoSaves.remove(0);
            oldestAutosave.delete();
        }

        redoSaves.clear();
    }
}
