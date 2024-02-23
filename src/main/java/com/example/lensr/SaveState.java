package com.example.lensr;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import static com.example.lensr.LensrStart.*;

public class SaveState {
    public static void saveProject(String filename) {
        // Save the lineMirror object
        try {
            FileOutputStream fileOut = new FileOutputStream(filename);
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
            throw new RuntimeException("Error saving lineMirror", i);
        }
    }
}
