module com.example.lensr {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.jfoenix;
    requires java.desktop;

    opens com.example.lensr to javafx.fxml;
    exports com.example.lensr;
    exports com.example.lensr.objects;
    opens com.example.lensr.objects to javafx.fxml;
}