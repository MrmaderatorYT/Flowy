module com.ccs.flowybrowser {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.bootstrapfx.core;
    requires jdk.jsobject;
    requires javafx.web;

    opens com.ccs.flowybrowser to javafx.fxml;
    exports com.ccs.flowybrowser;
}