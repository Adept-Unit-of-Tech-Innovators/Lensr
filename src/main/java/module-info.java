module com.lensr {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.jfoenix;
    requires java.desktop;
    requires org.apache.commons.io;

    opens com.lensr to javafx.fxml;
    exports com.lensr;
    exports com.lensr.objects;
    opens com.lensr.objects to javafx.fxml;
    exports com.lensr.ui;
    opens com.lensr.ui to javafx.fxml;
    exports com.lensr.saveloadkit;
    opens com.lensr.saveloadkit to javafx.fxml;
    exports com.lensr.objects.glass;
    opens com.lensr.objects.glass to javafx.fxml;
    exports com.lensr.objects.mirrors;
    opens com.lensr.objects.mirrors to javafx.fxml;
    exports com.lensr.objects.lightsources;
    opens com.lensr.objects.lightsources to javafx.fxml;
    exports com.lensr.objects.misc;
    opens com.lensr.objects.misc to javafx.fxml;
}