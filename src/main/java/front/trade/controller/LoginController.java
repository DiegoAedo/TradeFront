package front.trade.controller;

import front.trade.Repository;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import cl.vc.module.protocolbuff.session.SessionsMessage;

public class LoginController {

    @FXML
    private Label titleLabel;
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private CheckBox chkGuardarContrasena;
    @FXML
    private Button loginButton;
    @FXML
    private ComboBox<SessionsMessage.Enviroment> enviromentComboBox;
    @FXML
    private Label errorLabel;  // Etiqueta de error para mostrar el mensaje

    @FXML
    public void initialize() {
        // Cargar los ambientes por defecto
        enviromentComboBox.setItems(FXCollections.observableArrayList(SessionsMessage.Enviroment.values()));
        enviromentComboBox.getItems().remove(SessionsMessage.Enviroment.UNRECOGNIZED);
        enviromentComboBox.getItems().remove(SessionsMessage.Enviroment.LOCALHOST);
        enviromentComboBox.getItems().remove(SessionsMessage.Enviroment.QA);

        // Establecer el valor por defecto
        enviromentComboBox.getSelectionModel().selectFirst();

        // Establecer el comportamiento para el ComboBox con texto amigable
        enviromentComboBox.setCellFactory(param -> new ListCell<SessionsMessage.Enviroment>() {
            @Override
            protected void updateItem(SessionsMessage.Enviroment item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    switch (item) {
                        case PRODUCTION:
                            setText("Producción");
                            break;
                        case TEST:
                            setText("Pruebas");
                            break;
                        case LOCALHOST:
                            setText("Local");
                            break;
                        case PRODUCTION_VPN:
                            setText("VPN");
                            break;
                        case QA:
                            setText("QA");
                            break;
                        default:
                            setText(item.name());
                    }
                }
            }
        });

        // Establecer el comportamiento para el ComboBox con texto amigable
        enviromentComboBox.setConverter(new javafx.util.StringConverter<SessionsMessage.Enviroment>() {
            @Override
            public String toString(SessionsMessage.Enviroment enviroment) {
                if (enviroment == null) {
                    return null;
                }
                switch (enviroment) {
                    case PRODUCTION:
                        return "Producción";
                    case TEST:
                        return "Pruebas";
                    case LOCALHOST:
                        return "Local";
                    case PRODUCTION_VPN:
                        return "VPN";
                    case QA:
                        return "QA";
                    default:
                        return enviroment.name();
                }
            }

            @Override
            public SessionsMessage.Enviroment fromString(String string) {
                switch (string) {
                    case "Producción":
                        return SessionsMessage.Enviroment.PRODUCTION;
                    case "Pruebas":
                        return SessionsMessage.Enviroment.TEST;
                    case "Local":
                        return SessionsMessage.Enviroment.LOCALHOST;
                    case "VPN":
                        return SessionsMessage.Enviroment.PRODUCTION_VPN;
                    case "QA":
                        return SessionsMessage.Enviroment.QA;
                    default:
                        return SessionsMessage.Enviroment.valueOf(string);
                }
            }
        });

        // Establecer un ChangeListener para el TextField de username
        usernameField.textProperty().addListener((observable, oldValue, newValue) -> {
            // Eliminar espacios al final y convertir a minúsculas
            String formattedText = newValue.trim().toLowerCase();
            usernameField.setText(formattedText);

            // Limpiar el mensaje de error y los estilos de los campos al escribir
            errorLabel.setText("");  // Limpiar mensaje de error
            highlightEmptyFields(false, false);  // Limpiar los bordes

            // Actualizar el ComboBox de ambientes
            updateEnviromentComboBox(formattedText);
        });

        // Establecer un ChangeListener para el TextField de password
        passwordField.textProperty().addListener((observable, oldValue, newValue) -> {
            // Limpiar el mensaje de error y los estilos de los campos al escribir
            errorLabel.setText("");  // Limpiar mensaje de error
            highlightEmptyFields(false, false);  // Limpiar los bordes
        });

        // Lógica del botón de login
        loginButton.setOnAction(e -> login());
    }

    // Lógica para actualizar el ComboBox de ambientes
    private void updateEnviromentComboBox(String username) {
        enviromentComboBox.getItems().clear();

        if (username.contains("test")) {
            enviromentComboBox.setItems(FXCollections.observableArrayList(SessionsMessage.Enviroment.TEST));
            enviromentComboBox.getSelectionModel().selectFirst();
        } else if (username.contains("vt") || username.contains("daedo")) {
            // Mostrar todos los ambientes
            enviromentComboBox.setItems(FXCollections.observableArrayList(SessionsMessage.Enviroment.values()));
            enviromentComboBox.getItems().remove(SessionsMessage.Enviroment.UNRECOGNIZED);
            enviromentComboBox.getSelectionModel().selectFirst();
        } else {
            // Mostrar los ambientes por defecto
            enviromentComboBox.setItems(FXCollections.observableArrayList(SessionsMessage.Enviroment.values()));
            enviromentComboBox.getItems().remove(SessionsMessage.Enviroment.UNRECOGNIZED);
            enviromentComboBox.getItems().remove(SessionsMessage.Enviroment.LOCALHOST);
            enviromentComboBox.getItems().remove(SessionsMessage.Enviroment.QA);
            enviromentComboBox.getSelectionModel().selectFirst();
        }
    }

    // Mostrar mensaje de error
    private void showErrorMessage(String message) {
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-text-fill: red;");
    }

    // Resaltar los campos vacíos
    private void highlightEmptyFields(boolean isUsernameEmpty, boolean isPasswordEmpty) {
        if (isUsernameEmpty) {
            usernameField.setStyle("-fx-border-color: red;");
        } else {
            usernameField.setStyle("-fx-border-color: none;");
        }

        if (isPasswordEmpty) {
            passwordField.setStyle("-fx-border-color: red;");
        } else {
            passwordField.setStyle("-fx-border-color: none;");
        }
    }

    // Lógica para el login
    public void login() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        // Si los campos están vacíos, mostramos un mensaje de error
        if (username.isEmpty() || password.isEmpty()) {
            showErrorMessage("Por favor complete todos los campos.");
            highlightEmptyFields(username.isEmpty(), password.isEmpty());
        } else {
            // Realiza la lógica de conexión con el ambiente seleccionado
            SessionsMessage.Enviroment selectedEnv = enviromentComboBox.getValue();
            // Aquí podrías agregar la lógica para conectarte al ambiente seleccionado
        }
    }
}
