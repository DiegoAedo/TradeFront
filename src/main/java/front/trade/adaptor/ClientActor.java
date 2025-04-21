package front.trade.adaptor;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import cl.vc.algos.bkt.proto.BktStrategyProtos;
import front.trade.Repository;
import front.trade.controller.*;
import front.trade.utils.Notifier;
import cl.vc.module.protocolbuff.blotter.BlotterMessage;
import cl.vc.module.protocolbuff.generator.TimeGenerator;
import cl.vc.module.protocolbuff.generator.TopicGenerator;
import cl.vc.module.protocolbuff.mkd.MarketDataMessage;
import cl.vc.module.protocolbuff.notification.NotificationMessage;
import cl.vc.module.protocolbuff.routing.RoutingMessage;
import cl.vc.module.protocolbuff.session.SessionsMessage;
import cl.vc.module.protocolbuff.tcp.TransportingObjects;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tab;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class ClientActor extends AbstractActor {

    private final static HashMap<String, RoutingMessage.Order> ordersById = new HashMap<>();
    private ClientActor clientActor;

    public static Props props() {
        return Props.create(ClientActor.class);
    }

    @Override
    public void preStart() {
        try {

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(TransportingObjects.class, this::onTransportingObjects)
                .match(BlotterMessage.PortfolioResponse.class, this::onPortfolioResponse)
                .match(BlotterMessage.PreselectResponse.class, this::onPreselectResponse)
                .match(BlotterMessage.SnapshotPositionHistory.class, this::onSnapshotPositionHistory)
                .match(BlotterMessage.SnapshotPositions.class, this::onPositionSnappshot)
                .match(BlotterMessage.Patrimonio.class, this::onPatrimonio)
                .match(BlotterMessage.Balance.class, this::onBalance)
                .match(NotificationMessage.Notification.class, this::onNotification)
                .match(MarketDataMessage.Snapshot.class, this::onSnappshot)
                .match(RoutingMessage.Order.class, this::onOrder)
                .match(SessionsMessage.Connect.class, this::onConnect)
                .match(SessionsMessage.Pong.class, this::onPong)
                .match(SessionsMessage.Ping.class, this::onPing)
                .match(SessionsMessage.Disconnect.class, this::onDisconnect)
                .match(RoutingMessage.OrderCancelReject.class, this::onCancelReject)
                .match(MarketDataMessage.Incremental.class, this::onIncremental)
                .match(MarketDataMessage.IncrementalBook.class, this::onIncrementalBook)
                .match(MarketDataMessage.Rejected.class, this::onmkdReject)
                .match(MarketDataMessage.Statistic.class, this::onStatistic)
                .match(MarketDataMessage.Trade.class, this::onTrade)
                .match(MarketDataMessage.News.class, this::onNews)
                .match(MarketDataMessage.SnapshotNews.class, this::onSnapshotNews)
                .match(NotificationMessage.NotificationResponse.class, this::onNotificationResponse)
                .match(BktStrategyProtos.SnapshotBasket.class, this::onBasketMessage)
                .match(BlotterMessage.UserList.class, this::onUserList)
                .match(BlotterMessage.User.class, this::onUser)
                .match(BlotterMessage.Multibook.class, this::onMultibook)
                .match(MarketDataMessage.TradeGeneral.class, this::onTradeGeneral)
                .match(MarketDataMessage.SnapshotTradeGeneral.class, this::onSnapshotTradeGeneral)
                .match(BlotterMessage.SnapshotSimultaneas.class, this::onSnapshotSimultaneas)
                .match(MarketDataMessage.SecurityList.class, this::onSecurityList).build();


    }


}


