package front.trade;

import front.trade.controller.LoginController;
import front.trade.utils.Notifier;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.extern.slf4j.Slf4j;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class MainApp extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        try {
            // Cargar propiedades desde el archivo de configuración
            Repository.getProperties().load(LoginController.class.getResourceAsStream("/enviroment/application.properties"));

            // Cargar el archivo FXML de Login
            URL fxmlURL = getClass().getResource("/view/login.fxml");
            if (fxmlURL == null) {
                log.error("El archivo FXML no se encontró en la ruta: /view/login.fxml");
                return;
            }
            FXMLLoader loader = new FXMLLoader(fxmlURL);
            AnchorPane root = loader.load();
            LoginController loginController = loader.getController();

            // Cargar el archivo CSS
            URL cssURL = getClass().getResource("/css/login.css");
            if (cssURL == null) {
                log.error("El archivo CSS no se encontró en la ruta: /css/login.css");
                return;
            }
            String css = cssURL.toExternalForm();
            root.getStylesheets().add(css);

            // Crear la escena y asignarla al escenario
            Scene scene = new Scene(root);
            primaryStage.setScene(scene);

            // Título de la ventana
            primaryStage.setTitle("");

            // Icono de la ventana
            URL iconURL = getClass().getResource("/images/Login.png");
            if (iconURL != null) {
                Image icon = new Image(iconURL.toExternalForm());
                primaryStage.getIcons().add(icon);
            } else {
                log.warn("El ícono no se encontró en la ruta: /images/Login.png");
            }

            // Evento para cerrar la ventana correctamente
            primaryStage.setOnCloseRequest(event -> {
                Platform.exit();
                System.exit(0);
            });

            // Inicializar el repositorio y establecer la escena
            Repository.principal = primaryStage;
            Repository.login = scene;

            // Iniciar la aplicación
            primaryStage.show();
            log.info("Aplicación iniciada correctamente.");

            // Notificaciones (si tienes implementadas)
            //Notifier.setStage(primaryStage);

            // Programar el apagado de la aplicación (si es necesario)
            scheduleAppShutdown(primaryStage);

            // Programar una tarea recurrente (ejemplo: actualización de información o actividad de fondo)
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleAtFixedRate(() -> {
                // Puedes añadir alguna acción periódica aquí (por ejemplo, actualización de la UI)
                // Notifier.setCoolingDown(false);  // Si tienes alguna lógica de enfriamiento o actualizaciones
            }, 0, 3, TimeUnit.SECONDS);

        } catch (Exception ex) {
            log.error("Error al iniciar la aplicación", ex);
        }
    }
    private void scheduleAppShutdown(Stage principal) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        Runnable shutdownTask = () -> {
            Platform.runLater(() -> showShutdownMessage(principal));
            scheduleAppShutdown(principal);
        };

        LocalDateTime now = LocalDateTime.now();
        ZonedDateTime zonedNow = now.atZone(ZoneId.of("America/Santiago"));
        ZonedDateTime zonedNext7AM = zonedNow.withHour(7).withMinute(0).withSecond(0).withNano(0);

        if (zonedNow.compareTo(zonedNext7AM) > 0) {
            zonedNext7AM = zonedNext7AM.plusDays(1);
        }

        long delay = ChronoUnit.MILLIS.between(zonedNow, zonedNext7AM);

        scheduler.schedule(shutdownTask, delay, TimeUnit.MILLISECONDS);
    }

    private void showShutdownMessage(Stage principal) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Alcanzaste el límite de tiempo. La aplicación se va a cerrar.", ButtonType.OK);
        alert.setTitle("Aviso");
        alert.setHeaderText(null);
        alert.initOwner(principal);


        Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
        alertStage.initStyle(StageStyle.UNDECORATED);


        alert.getDialogPane().getStylesheets().add(getClass().getResource("/blotter/css/style.css").toExternalForm());
        alert.getDialogPane().getStyleClass().add("your-dialog-class");


        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get().getButtonData() == ButtonBar.ButtonData.OK_DONE) {
            Platform.exit();
            System.exit(0);
        }
    }
}


