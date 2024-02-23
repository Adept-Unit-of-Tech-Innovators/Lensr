package com.example.lensr;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class LoadState {
    public static List<Serializable> loadProject(String filename) {
        List<Serializable> objects = new ArrayList<>();
        try {
            // Load every object from the file
            FileInputStream fileIn = new FileInputStream(filename);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            while (true) {
                try {
                    objects.add((Serializable) in.readObject());
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
        return objects;
    }
}
