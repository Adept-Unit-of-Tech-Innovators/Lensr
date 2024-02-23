package com.example.lensr;

import com.example.lensr.objects.*;

public class MirrorMethods {
    // lol its so empty here now
    public static void updateLightSources() {
        LensrStart.rayCanvas.clear();
        for (Object lightSource : LensrStart.lightSources) {
            if (lightSource instanceof BeamSource beamSource) {
                beamSource.update();
            }
            else if (lightSource instanceof PanelSource panelSource) {
                panelSource.update();
            }
            else if (lightSource instanceof PointSource pointSource) {
                pointSource.update();
            }
        }
    }
}
