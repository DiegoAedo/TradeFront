package front.trade.controller;

import cl.vc.module.protocolbuff.crypt.AESEncryption;
import cl.vc.module.protocolbuff.notification.NotificationMessage;
import front.trade.ws.SimpleWebSocketListener;
import front.trade.utils.EncryptionUtil;
import front.trade.Repository;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import cl.vc.module.protocolbuff.session.SessionsMessage;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.eclipse.jetty.websocket.common.extensions.compress.PerMessageDeflateExtension;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Slf4j
@Data
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

    public static SimpleWebSocketListener simpleWebSocketListener;

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

            // Llamar al método que realiza la validación de las credenciales
            performLogin(username, password, selectedEnv);
        }
    }

    // Método para realizar el login con WebSocket
    private void performLogin(String username, String password, SessionsMessage.Enviroment selectedEnv) {
        try {
            // Asegúrate de que el entorno esté configurado antes de usarlo
            SessionsMessage.Enviroment env = Repository.getEnviroment();
            if (env == null) {
                // Si el entorno es null, mostrar un mensaje de error y salir
                log.error("El entorno no ha sido configurado correctamente.");
                showErrorMessage("Error: el entorno no ha sido configurado.");
                return;  // Salir de la función si no hay entorno
            }

            // Recuperar la URL del WebSocket a partir del entorno configurado
            String enviromentUrl = Repository.getProperties().getProperty(env.name().toLowerCase());
            if (enviromentUrl == null || enviromentUrl.isEmpty()) {
                // Si la URL del entorno está vacía o es null, mostrar un error
                showErrorMessage("Error: la URL del entorno no está configurada.");
                return;
            }

            // Conectar con el WebSocket
            String credentials = AESEncryption.encrypt(username) + ":" + AESEncryption.encrypt(password);
            String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

            WebSocketClient client = new WebSocketClient();
            client.getPolicy().setMaxTextMessageSize(100 * 1024 * 1024);
            client.getPolicy().setMaxBinaryMessageSize(100 * 1024 * 1024);
            client.getPolicy().setIdleTimeout(300000);

            client.addBean(new PerMessageDeflateExtension());
            client.start();
            Repository.setCredencial(credentials);

            ClientUpgradeRequest request = new ClientUpgradeRequest();
            request.setHeader("Authorization", "Basic " + encodedCredentials);
            request.addExtensions("permessage-deflate");

            // Crear el listener de WebSocket
            simpleWebSocketListener = new SimpleWebSocketListener(client, Repository.clientActor, Repository.getActorSystem(),
                    NotificationMessage.Component.BLOTTER_FRONT, Repository.username, enviromentUrl, request);

            // Conectar al WebSocket usando la URL del entorno
            client.connect(simpleWebSocketListener, new URI(enviromentUrl), request).get(5, TimeUnit.SECONDS);

            // Verificar si la conexión fue exitosa
            if (simpleWebSocketListener.isConnected()) {
                Repository.setClientService(simpleWebSocketListener);
            } else {
                simpleWebSocketListener.setCloseFailure(true);
            }

        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            showErrorMessage("Error en la conexión: " + ex.getMessage());
        }
    }

    // Método para cambiar a la siguiente pantalla
    private void navigateToNextScreen() {
        try {
            // Cargar la siguiente pantalla
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/MainView.fxml"));
            AnchorPane mainScreen = loader.load();

            // Cargar la escena en el Stage
            Scene mainScene = new Scene(mainScreen);
            Repository.getPrincipal().setScene(mainScene);
            Repository.getPrincipal().setTitle("Aplicación Principal");
            Repository.getPrincipal().show();

        } catch (IOException e) {
            e.printStackTrace();
            showErrorMessage("Error al cargar la siguiente pantalla.");
        }
    }
}
