package front.trade.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;

public class LoginController {

    @FXML
    private Button loginButton;  // Este es el botón del archivo FXML

    @FXML
    public void initialize() {
        // Aquí puedes inicializar cualquier cosa o escuchar eventos
        loginButton.setOnAction(e -> {
            System.out.println("Botón de inicio de sesión presionado");
        });
    }
}
