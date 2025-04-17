package front.trade;

import front.trade.controller.LoginController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.net.URL;

@Slf4j
public class MainApp extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {

        Repository.getProperties().load(LoginController.class.getResourceAsStream("/enviroment/application.properties"));

        // Cargar el archivo FXML
        URL fxmlURL = getClass().getResource("/view/login.fxml");
        if (fxmlURL == null) {
            log.error("El archivo FXML no se encontró en la ruta: /view/login.fxml");
            return;
        }


        FXMLLoader loader = new FXMLLoader(fxmlURL);
        AnchorPane root = loader.load();

        // Cargar el archivo CSS
        URL cssURL = getClass().getResource("/css/login.css");
        if (cssURL == null) {
            log.error("El archivo CSS no se encontró en la ruta: /css/login.css");
            return;
        }
        String css = cssURL.toExternalForm();
        root.getStylesheets().add(css);


        Scene scene = new Scene(root);

        primaryStage.setTitle("");


        URL iconURL = getClass().getResource("/images/Login.png");
        if (iconURL != null) {
            Image icon = new Image(iconURL.toExternalForm());
            primaryStage.getIcons().add(icon);
        } else {
            log.warn("El ícono no se encontró en la ruta: /images/Login.png");
        }

        primaryStage.setScene(scene);
        primaryStage.show();
        log.info("Aplicación iniciada correctamente.");
    }
}

