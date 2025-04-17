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
}
