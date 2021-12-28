package ru.croccode.hypernull.bot;

import ru.croccode.hypernull.domain.MatchMode;
import ru.croccode.hypernull.geometry.Offset;
import ru.croccode.hypernull.geometry.Point;
import ru.croccode.hypernull.io.SocketSession;
import ru.croccode.hypernull.message.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;

public class MegaBot implements Bot {
    private static final MatchMode FRIENDLY = null;
    private Offset moveOffset;
    private Move move = new Move();
    private int botID;
    private final Random rnd = new Random(System.currentTimeMillis());
    ;

    @Override
    public Register onHello(Hello hello) {
        Register register = new Register();
        register.setMode(FRIENDLY);
        register.setBotName("MegaBot");
        return register;
    }

    @Override
    public void onMatchStarted(MatchStarted matchStarted) {
        botID = matchStarted.getYourId();
    }

    @Override
    public Move onUpdate(Update update) {
        Point pointBot = update.getBots().get(botID);
        Set<Point> pointCoins = update.getCoins();
        Set<Point> pointBlocks = update.getBlocks();
        if (pointCoins != null) {
            Point firstCoordinate = new ArrayList<Point>(pointCoins).get(0);
            double lengthToCoin = Math.sqrt(Math.pow((pointBot.x() - firstCoordinate.x()), 2) + Math.pow((pointBot.y() - firstCoordinate.y()), 2));
            Point coordinateToNearestCoin = null;
            for (Point point : pointCoins) {
                if (lengthToCoin >= Math.sqrt(Math.pow((pointBot.x() - point.x()), 2) + Math.pow((pointBot.y() - point.y()), 2))) {
                    lengthToCoin = Math.sqrt(Math.pow((pointBot.x() - point.x()), 2) + Math.pow((pointBot.y() - point.y()), 2));
                    coordinateToNearestCoin = point;
                }
            }
            double currentLength;
            Point pointAfterOffset1;
            boolean checker;
            while (true) {
                moveOffset = new Offset(
                        rnd.nextInt(3) - 1,
                        rnd.nextInt(3) - 1
                );
                checker = true;
                pointAfterOffset1 = pointBot.apply(moveOffset);
                currentLength = Math.sqrt(Math.pow((pointAfterOffset1.x() - coordinateToNearestCoin.x()), 2) + Math.pow((pointAfterOffset1.y() - coordinateToNearestCoin.y()), 2));
                Point finalPointAfterOffset = pointAfterOffset1;
                checker = pointBlocks.stream().noneMatch((p) -> p.x() == finalPointAfterOffset.x() && p.y() == finalPointAfterOffset.y());
                if ((currentLength <= lengthToCoin) && checker == true) {
                    move.setOffset(moveOffset);
                    return move;
                }
            }
        } else {
            Point pointAfterOffset2;
            boolean check = false;
            boolean stop = true;
            if (pointBlocks != null) {
                while (stop) {
                    moveOffset = new Offset(
                            rnd.nextInt(3) - 1,
                            rnd.nextInt(3) - 1
                    );
                    pointAfterOffset2 = pointBot.apply(moveOffset);
                    Point finalPointAfterOffset = pointAfterOffset2;
                    check = pointBlocks.stream().noneMatch((p) -> p.x() == finalPointAfterOffset.x() && p.y() == finalPointAfterOffset.y());
                    if (check == true) stop = false;
                }
            } else {
                moveOffset = new Offset(1,1);
                move.setOffset(moveOffset);
                return move;
            }
        }
        move.setOffset(moveOffset);
        return move;
    }

    @Override
    public void onMatchOver(MatchOver matchOver) {

    }

    public static void main(String[] args) throws IOException {
        Socket socket = new Socket();
        socket.setTcpNoDelay(true);
        socket.setSoTimeout(300_000);
        socket.connect(new InetSocketAddress("localhost", 2021));

        SocketSession session = new SocketSession(socket);
        MegaBot bot = new MegaBot();
        new BotMatchRunner(bot, session).run();
    }
}
