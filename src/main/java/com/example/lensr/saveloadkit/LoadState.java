package com.example.lensr.saveloadkit;

import com.example.lensr.objects.*;
import com.example.lensr.objects.glass.Glass;
import com.example.lensr.objects.lightsources.BeamSource;
import com.example.lensr.objects.lightsources.PanelSource;
import com.example.lensr.objects.lightsources.PointSource;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static com.example.lensr.LensrStart.*;

public class LoadState {
    public static void loadProject(String filename) {
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

        List<Serializable> newObjects = new ArrayList<>();
        try {
            // Load every object from the file
            FileInputStream fileIn = new FileInputStream(filename);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            while (true) {
                try {
                    newObjects.add((Serializable) in.readObject());
                } catch (IOException e) {
                    break;
                }
            }
            in.close();
            fileIn.close();
        } catch (IOException i) {
            throw new RuntimeException("Error loading project", i);
        } catch (ClassNotFoundException c) {
            throw new RuntimeException("Error loading project: Object class not found", c);
        }

        for (Serializable object : newObjects) {
            if (object instanceof Editable editable) {
                editable.create();
                if (editable instanceof BeamSource || editable instanceof PanelSource || editable instanceof PointSource) {
                    lightSources.add(editable);
                }
                else if (editable instanceof Glass) {
                    lenses.add(editable);
                }
                else {
                    mirrors.add(editable);
                }
            }
        }
        // After loading, simulate the new scene
        updateLightSources();
    }
}
