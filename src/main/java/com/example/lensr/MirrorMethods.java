package com.example.lensr;

import com.example.lensr.objects.*;

public class MirrorMethods {
    // lol its so empty here now
    public static void updateLightSources() {
        for (Object lightSource : LensrStart.lightSources) {
            if (lightSource instanceof BeamSource beamSource) {
                beamSource.update();
            }
            else if (lightSource instanceof PanelSource panelSource) {
                panelSource.update();
            }
        }
    }
}
