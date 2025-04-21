package front.trade.controller;

import front.trade.Repository;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class PrincipalController {

    @FXML
    private Label welcomeLabel;

    // Método que se llamará después de que se cargue la pantalla principal
    public void initialize() {
        // Aquí puedes personalizar el mensaje de bienvenida según el usuario
        welcomeLabel.setText("Hola, " + Repository.username); // Muestra el nombre de usuario después del login
    }
}

