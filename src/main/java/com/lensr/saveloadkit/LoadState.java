package com.lensr.saveloadkit;

import com.lensr.objects.Editable;
import com.lensr.objects.glass.Glass;
import com.lensr.objects.lightsources.RaySource;
import com.lensr.objects.lightsources.BeamSource;
import com.lensr.objects.lightsources.PointSource;
import com.lensr.LensrStart;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class LoadState {
    public static void loadProject(String filename) {
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
                if (editable instanceof RaySource || editable instanceof BeamSource || editable instanceof PointSource) {
                    LensrStart.lightSources.add(editable);
                } else if (editable instanceof Glass) {
                    LensrStart.lenses.add(editable);
                } else {
                    LensrStart.mirrors.add(editable);
                }
            }
        }
        // After loading, simulate the new scene
        LensrStart.updateLightSources();
    }
}
