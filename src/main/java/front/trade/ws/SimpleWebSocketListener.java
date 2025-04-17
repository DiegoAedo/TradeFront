package front.trade.ws;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.routing.RoundRobinPool;
import front.trade.Repository;
import front.trade.adaptor.ParseMessageActor;
import front.trade.utils.Notifier;
import cl.vc.module.protocolbuff.generator.IDGenerator;
import cl.vc.module.protocolbuff.generator.TimeGenerator;
import cl.vc.module.protocolbuff.notification.NotificationMessage;
import cl.vc.module.protocolbuff.session.SessionsMessage;
import cl.vc.module.protocolbuff.tcp.InterfaceTcp;
import cl.vc.module.protocolbuff.tcp.TransportingObjects;
import cl.vc.module.protocolbuff.ws.vectortrade.MessageUtilVT;
import com.google.protobuf.Message;
import javafx.application.Platform;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.java_websocket.exceptions.WebsocketNotConnectedException;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


@Slf4j
public class SimpleWebSocketListener extends WebSocketAdapter implements InterfaceTcp {

    private static final int RECONNECT_DELAY = 7;

    private static final int CONNECTION_TIMEOUT = 600000;  // 5 minutos

    private static final int MAX_MESSAGE_SIZE = 100 * 1024 * 1024;  // 100 MB

    private ActorRef parseMessageActor;

    private ActorRef clientActor;

    private NotificationMessage.Component component;

    private String username;

    private ScheduledExecutorService schedulerPing = Executors.newScheduledThreadPool(1);

    private ScheduledExecutorService schedulersound = Executors.newScheduledThreadPool(1);

    @Setter
    @Getter
    private boolean autoReconnect = true;

    @Setter
    @Getter
    private boolean closeFailure = false;

    private ScheduledExecutorService scheduler;

    private WebSocketClient client;

    private Runnable tasks;

    private String enviroment;

    private ClientUpgradeRequest request;


    public SimpleWebSocketListener(WebSocketClient client, ActorRef clientActor, ActorSystem actorSystem,
                                   NotificationMessage.Component component,
                                   String username, String enviroment, ClientUpgradeRequest request) {

        try {

            this.client = client;
            this.enviroment = enviroment;
            this.request = request;

            client.getPolicy().setMaxTextMessageSize(MAX_MESSAGE_SIZE);
            client.getPolicy().setMaxBinaryMessageSize(MAX_MESSAGE_SIZE);
            client.getPolicy().setIdleTimeout(CONNECTION_TIMEOUT);


            parseMessageActor = actorSystem.actorOf(new RoundRobinPool(20).props(ParseMessageActor.props(clientActor)));
            this.clientActor = clientActor;
            this.username = username;
            this.component = component;

            Runnable task = new Runnable() {
                public void run() {
                    SessionsMessage.Ping ping = SessionsMessage.Ping.newBuilder().setId(IDGenerator.getID()).build();
                    //todo eliminar esto
                    //  sendMessage(ping);
                }
            };
            schedulerPing.scheduleAtFixedRate(task, 1, 45, TimeUnit.SECONDS);


            tasks = new Runnable() {
                public void run() {
                    Repository.setSound(true);
                    schedulersound.shutdown();
                }
            };

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

    }

    @Override
    public void onWebSocketConnect(Session sess) {
        super.onWebSocketConnect(sess);

        Platform.runLater(() -> {
            if (schedulersound.isShutdown() || schedulersound.isTerminated()) {
                schedulersound = Executors.newScheduledThreadPool(1);
            }
            schedulersound.scheduleAtFixedRate(tasks, 7, 7, TimeUnit.SECONDS);

            if (scheduler != null) {
                scheduler.shutdown();
            }
        });
    }

    @Override
    public void onWebSocketText(String message) {
        log.error(" ################## Received STRING: " + message);
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {

        super.onWebSocketClose(statusCode, reason);

        log.error("######## DESCONEXION reason {} {}", reason, statusCode);

        if (statusCode == 1006 || statusCode == 1000) {

            try {

                Repository.setSound(false);
                try {

                    client.connect(this, new URI(enviroment), request).get(5, TimeUnit.SECONDS);

                } catch (InterruptedException e) {
                    initiateReconnection();
                    return;
                }

                if (this.isConnected()) {

                    log.info("conectado de nuevo ");

                    Repository.setClientService(this);
                    SessionsMessage.Connect connect = SessionsMessage.Connect.newBuilder().setUsername(Repository.getUsername()).build();
                    Repository.getClientService().sendMessage(connect);

                    Repository.enviasubscripcionAll();
                    Repository.subscribeDolar();

                    try {
                        schedulersound.scheduleAtFixedRate(tasks, 13, 13, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        Thread.sleep(5000);
                        Repository.setSound(true);
                        schedulersound.shutdown();
                    }

                }

            } catch (Exception e) {
                initiateReconnection();
                log.error(e.getMessage(), e);
            }

            return;

        }

        SessionsMessage.Disconnect disconnect = SessionsMessage.Disconnect.newBuilder().setTokenKeycloak(username)
                .setText(reason)
                .setComponent(NotificationMessage.Component.BLOTTER_SERVICES).build();

        clientActor.tell(disconnect, ActorRef.noSender());


        if (closeFailure == true) {

            log.info("no se hace la reconexion");

            if (scheduler != null) {
                scheduler.shutdown();
            }
            autoReconnect = false;
            return;
        }

        handleDisconnection(reason);

        if (autoReconnect) {
            autoReconnect = false;
            initiateReconnection();
        }


    }

    private void handleDisconnection(String reason) {
        try {

            NotificationMessage.Notification notification = NotificationMessage.Notification.newBuilder()
                    .setComments(reason)
                    .setComponent(component)
                    .setTypeState(NotificationMessage.TypeState.DISCONNECTION)
                    .setLevel(NotificationMessage.Level.FATAL)
                    .setTime(TimeGenerator.getTimeProto())
                    .setTitle("Error Services").build();

            TransportingObjects transportingObjectss = new TransportingObjects(notification);
            clientActor.tell(transportingObjectss, ActorRef.noSender());

        } catch (Exception e) {
            log.error("Error handling disconnection", e);
        }
    }


    private void initiateReconnection() {

        try {

            scheduler = Executors.newScheduledThreadPool(2);

            scheduler.scheduleWithFixedDelay(() -> {
                try {

                    log.info("Attempting to reconnect...");

                    Notifier.INSTANCE.notifyWarning("Desconexión", "Intentando reconectar");

                    if (this.getSession() == null || !this.getSession().isOpen()) {
                        try {
                            client.connect(this, new URI(enviroment), request).get(5, TimeUnit.SECONDS);
                        } catch (Exception e) {
                            client.start();
                        }
                    }

                    if (this.getSession() != null && this.getSession().isOpen()) {
                        autoReconnect = true;
                        log.info("Reconnection successful.");

                        SessionsMessage.Connect connect = SessionsMessage.Connect.newBuilder().setUsername(Repository.getUsername()).build();
                        Repository.getClientService().sendMessage(connect);

                        /*
                        if (Repository.getPrincipalController() != null && Repository.getPrincipalController().getMarketDataViewerController() != null) {
                            Repository.getPrincipalController().getMarketDataViewerController().requestPortfolio();
                        }

                         */

                        scheduler.shutdown();

                    }
                } catch (Exception e) {
                    log.error("Reconnection attempt failed", e);
                }
            }, 0, RECONNECT_DELAY, TimeUnit.SECONDS);

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

    }

    @Override
    public void onWebSocketBinary(byte[] payload, int offset, int len) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(payload, offset, len);
        parseMessageActor.tell(byteBuffer, ActorRef.noSender());
    }

    @Override
    public void onWebSocketError(Throwable cause) {
        log.error("############### ERROR  WEBSOCKET ########## {}", cause.getMessage());
        log.error(cause.getMessage(), cause);
    }


    @Override
    public void sendMessage(String s) {
        log.info("llego un string xD");
    }

    @Override
    public void sendMessage(Message message) {
        try {
            if (getSession() != null && getSession().isOpen()) {
                ByteBuffer message1 = MessageUtilVT.serializeMessageByteBuffer(message);
                getSession().getRemote().sendBytesByFuture(message1);
            } else {
                log.error("WebSocket is not connected. Cannot send message: {}", message);
            }

        } catch (WebsocketNotConnectedException e) {
            log.error("WebSocket not connected: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Failed to send message: {}", e.getMessage(), e);
        }

    }

    @Override
    public void stopService() {
        try {

        } catch (Exception e) {
            log.error("Unexpected error while closing WebSocket session: {}", e.getMessage(), e);
        }

    }

    public void stopServiceForce() {
        try {

            log.info("Boton reconectar !!!!!!!!!!");

            if (getSession() != null && getSession().isOpen()) {
                log.info("Closing WebSocket session...");
                getSession().close();
                log.info("WebSocket session closed.");
            } else if (getSession() == null) {
                client.connect(this, new URI(enviroment), request).get(5, TimeUnit.SECONDS);

            } else{
                log.warn("WebSocket session is already closed or not initialized.");
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }


    @Override
    public void startService() {

    }
}
