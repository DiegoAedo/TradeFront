package front.trade;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import front.trade.controller.*;
import cl.vc.module.protocolbuff.blotter.BlotterMessage;
import cl.vc.module.protocolbuff.generator.IDGenerator;
import cl.vc.module.protocolbuff.mkd.MarketDataMessage;
import cl.vc.module.protocolbuff.routing.RoutingMessage;
import cl.vc.module.protocolbuff.session.SessionsMessage;
import cl.vc.module.protocolbuff.tcp.InterfaceTcp;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.*;
import java.util.prefs.Preferences;

@Slf4j
public class Repository {

    @Getter
    @Setter
    public static String credencial;
    @Getter
    public static ActorRef clientActor;
    @Getter
    public static ActorSystem actorSystem = ActorSystem.create();
    @Getter
    @Setter
    public static String credencialPath;
    @Getter
    public static Scene login;
    @Getter
    @Setter
    private static LoginController loginController;
    @Setter
    @Getter
    public static SessionsMessage.Enviroment enviroment;
    @Getter
    private static Properties properties = new Properties();
    @Getter
    @Setter
    public static String dolarSymbol;
    @Getter
    @Setter
    private static boolean sound = false;
    @Getter
    @Setter
    private static InterfaceTcp clientService;
    @Getter
    @Setter
    public static String username;
    @Getter
    public static HashMap<String, MarketDataMessage.Subscribe> subscribeIdStatistic = new HashMap<>();
    @Getter
    public static HashMap<String, MarketDataMessage.Subscribe> subscribeIdStatisticBook = new HashMap<>();
    @Getter
    public static HashMap<String, MarketDataMessage.Subscribe> subscribeIdStatisticBookEmergente = new HashMap<>();
    @Getter
    public static Stage principal;
    @Getter
    private static String STYLE = "/blotter/css/style.css";
    @Getter
    @Setter
    private static boolean notification = true;

    static {
        // Aseguramos que el entorno sea cargado correctamente al iniciar la aplicación
        // Aseguramos que el entorno sea cargado correctamente al iniciar la aplicación
        try {
            // Cargar propiedades desde el archivo de configuración
            properties.load(Repository.class.getResourceAsStream("/enviroment/application.properties"));

            // Obtener el entorno configurado en el archivo de propiedades
            String environmentProperty = properties.getProperty("environment");  // 'environment' debe existir en el archivo
            if (environmentProperty != null) {
                // Establecer el entorno en el Repository
                enviroment = SessionsMessage.Enviroment.valueOf(environmentProperty.toUpperCase());
                log.info("El entorno se ha configurado como: " + enviroment.name());
            } else {
                log.error("El entorno no se ha configurado correctamente en el archivo de propiedades.");
            }
        } catch (Exception e) {
            log.error("Error al cargar las propiedades del entorno", e);
        }

    }


        public static void enviasubscripcionAll() {
        Repository.getSubscribeIdStatistic().entrySet().forEach(s -> {
            Repository.getClientService().sendMessage(s.getValue());
        });

        Repository.getSubscribeIdStatisticBook().entrySet().forEach(s -> {
            Repository.getClientService().sendMessage(s.getValue());
        });

        Repository.getSubscribeIdStatisticBookEmergente().entrySet().forEach(s -> {
            Repository.getClientService().sendMessage(s.getValue());
        });
    }

    public static void subscribeDolar() {
        MarketDataMessage.Subscribe subscribe = MarketDataMessage.Subscribe.newBuilder()
                .setId(IDGenerator.getID())
                .setSettlType(RoutingMessage.SettlType.T2)
                .setSecurityType(RoutingMessage.SecurityType.CS)
                .setSymbol(Repository.getDolarSymbol())
                .setStatistic(true)
                .setBook(true)
                .setSecurityExchange(MarketDataMessage.SecurityExchangeMarketData.DATATEC_XBCL)
                .build();
        Repository.getClientService().sendMessage(subscribe);
        log.info("envia subscripcion dolar connect");
    }

    // Método para obtener la URL del entorno configurado
    public static String getEnvironmentUrl() {
        if (enviroment == null) {
            log.error("El entorno no ha sido configurado correctamente.");
            return null;
        }

        // Usamos el nombre del entorno para obtener la URL correspondiente del archivo de propiedades
        String enviromentUrl = properties.getProperty(enviroment.name().toLowerCase());  // "production", "test", "localhost", etc.
        if (enviromentUrl == null) {
            log.error("No se ha configurado una URL para el entorno: " + enviroment.name());
        }
        return enviromentUrl;
    }

}
