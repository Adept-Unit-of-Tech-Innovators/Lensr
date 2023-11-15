module com.example.lensr {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.lensr to javafx.fxml;
    exports com.example.lensr;
}