module com.example.lensr {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.jfoenix;
    requires java.desktop;
    requires org.apache.commons.io;

    opens com.example.lensr to javafx.fxml;
    exports com.example.lensr;
    exports com.example.lensr.objects;
    opens com.example.lensr.objects to javafx.fxml;
    exports com.example.lensr.ui;
    opens com.example.lensr.ui to javafx.fxml;
    exports com.example.lensr.saveloadkit;
    opens com.example.lensr.saveloadkit to javafx.fxml;
    exports com.example.lensr.objects.glass;
    opens com.example.lensr.objects.glass to javafx.fxml;
    exports com.example.lensr.objects.mirrors;
    opens com.example.lensr.objects.mirrors to javafx.fxml;
    exports com.example.lensr.objects.lightsources;
    opens com.example.lensr.objects.lightsources to javafx.fxml;
    exports com.example.lensr.objects.misc;
    opens com.example.lensr.objects.misc to javafx.fxml;
}