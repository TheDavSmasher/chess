package server.websocket;

import chess.ChessGame;
import chess.ChessMove;
import chess.ChessPosition;
import chess.InvalidMoveException;
import com.google.gson.*;
import model.dataAccess.AuthData;
import model.dataAccess.GameData;
import model.response.result.ServiceException;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.eclipse.jetty.websocket.api.Session;
import service.GameService;
import service.UserService;
import webSocketMessages.serverMessages.ErrorMessage;
import webSocketMessages.serverMessages.Notification;
import webSocketMessages.userCommands.*;

import java.io.IOException;
import java.lang.reflect.Type;

@WebSocket
public class WSServer {
    @OnWebSocketMessage
    public void onMessage(Session session, String message) {
        Gson gson = new Gson();
        UserGameCommand gameCommand = gson.fromJson(message, UserGameCommand.class);
        switch (gameCommand.getCommandType()){
            case JOIN_PLAYER -> join(gson.fromJson(message, JoinPlayerCommand.class), session);
            case JOIN_OBSERVER -> observe(gson.fromJson(message, JoinObserverCommand.class), session);
            case MAKE_MOVE -> move(gson.fromJson(message, MakeMoveCommand.class), session);
            case LEAVE -> leave(gson.fromJson(message, LeaveCommand.class), session);
            case RESIGN -> resign(gson.fromJson(message, ResignCommand.class), session);
        }
    }

    private final ConnectionManager connectionManager = new ConnectionManager();

    private void join(JoinPlayerCommand command, Session session) {
        try {
            String username = enter(command.getAuthString(), command.getGameID(), command.getColor(), session);
            Notification notification = new Notification(username + " has joined the game as " + command.getColor() + ".");
            connectionManager.notifyOthers(command.getGameID(), command.getAuthString(), notification);
        } catch (ServiceException e) {
            sendError(session, e.getMessage());
        }
    }

    private void observe(JoinObserverCommand command, Session session) {
        try {
            String username = enter(command.getAuthString(), command.getGameID(), null,session);
            Notification notification = new Notification(username + " is now observing the game.");
            connectionManager.notifyOthers(command.getGameID(), command.getAuthString(), notification);
        } catch (ServiceException e) {
            sendError(session, e.getMessage());
        }
    }

    private String enter(String authToken, int gameID, ChessGame.TeamColor color, Session session) throws ServiceException {
        AuthData auth = UserService.getUser(authToken);
        if (auth == null) {
            throw new ServiceException("You are unauthorized.");
        }
        GameData data = GameService.getGame(authToken, gameID);
        if (data == null) throw new ServiceException("Game does not exist.");
        if (color != null && ((color == ChessGame.TeamColor.WHITE && data.whiteUsername() != null && !data.whiteUsername().equals(auth.username()))
                            ||color == ChessGame.TeamColor.BLACK && data.blackUsername() != null && !data.blackUsername().equals(auth.username()))) {
            throw new ServiceException("Color is already taken.");
        }
        connectionManager.addToGame(gameID, authToken, auth.username(), session);
        connectionManager.loadNewGame(data.game(), authToken);
        return auth.username();
    }

    private void move(MakeMoveCommand command, Session session) {
        try {
            Connection connection = connectionManager.getFromUsers(command.getAuthString());
            if (connection == null) {
                sendError(session, "You are unauthorized");
                return;
            }
            GameData gameData = GameService.getGame(command.getAuthString(), command.getGameID());
            if (userIsPlayer(gameData, connection.username) == null) {
                sendError(session, "You need to be a player to make a move");
                return;
            }
            ChessGame game = gameData.game();
            if (userIsPlayer(gameData, connection.username) != game.getTeamTurn()) {
                sendError(session, "It is not your turn to make a move.");
                return;
            }
            game.makeMove(command.getMove());
            String gameJson = new Gson().toJson(game);
            GameService.updateGameState(command.getAuthString(), command.getGameID(), gameJson);

            connectionManager.loadNewGame(game, command.getGameID());
            ChessMove move = command.getMove();
            Notification moveNotification = new Notification(connection.username + " has moved piece at " +
                    positionAsString(move.getStartPosition()) + " to " + positionAsString(move.getEndPosition()) + ".");
            connectionManager.notifyOthers(command.getGameID(), command.getAuthString(), moveNotification);

            if (game.isInCheck(game.getTeamTurn())) {
                String opponent = (game.getTeamTurn() == ChessGame.TeamColor.WHITE) ? gameData.whiteUsername() : gameData.blackUsername();
                Notification checkNotification = new Notification(opponent + " is now in check.");
                connectionManager.notifyOthers(command.getGameID(), command.getAuthString(), checkNotification);
            } else if (game.isInCheckmate(game.getTeamTurn())) {
                String opponent = (game.getTeamTurn() == ChessGame.TeamColor.WHITE) ? gameData.whiteUsername() : gameData.blackUsername();
                Notification checkmateNotification = new Notification(opponent + " is now in checkmate.");
                connectionManager.notifyOthers(command.getGameID(), command.getAuthString(), checkmateNotification);
            }

        } catch (ServiceException | InvalidMoveException e) {
            sendError(session, e.getMessage());
        }
    }

    private ChessGame.TeamColor userIsPlayer(GameData data, String username) {
        if (data.whiteUsername() != null && data.whiteUsername().equals(username)) return ChessGame.TeamColor.WHITE;
        if (data.blackUsername() != null && data.blackUsername().equals(username)) return ChessGame.TeamColor.BLACK;
        return null;
    }

    private String positionAsString(ChessPosition position) {
        String end = "";
        switch (position.getColumn()) {
            case 1 -> end += "A";
            case 2 -> end += "B";
            case 3 -> end += "C";
            case 4 -> end += "D";
            case 5 -> end += "E";
            case 6 -> end += "F";
            case 7 -> end += "G";
            case 8 -> end += "H";
        }
        end += position.getRow();
        return end;
    }

    private void leave(LeaveCommand command, Session session) {
        try {
            Connection connection = connectionManager.getFromUsers(command.getAuthString());
            if (connection == null) {
                sendError(session, "You are unauthorized");
                return;
            }
            GameService.leaveGame(command.getAuthString(), command.getGameID());
            connectionManager.removeFromGame(command.getGameID(), command.getAuthString());
            Notification notification = new Notification(connection.username + " has left the game.");
            connectionManager.notifyOthers(command.getGameID(), command.getAuthString(), notification);
        } catch (ServiceException e) {
            sendError(session, e.getMessage());
        }
    }

    private void resign(ResignCommand command, Session session) {
        try {
            Connection connection = connectionManager.getFromUsers(command.getAuthString());
            if (connection == null) {
                sendError(session, "You are unauthorized");
                return;
            }
            GameService.leaveGame(command.getAuthString(), command.getGameID());
            GameData gameData = GameService.getGame(command.getAuthString(), command.getGameID());
            if (!gameData.game().gameInPlay()) {
                sendError(session, "Game is already finished. You cannot resign anymore.");
                return;
            }
            if (userIsPlayer(gameData, connection.username) == null) {
                sendError(session, "You need to be a player to resign.");
                return;
            }
            gameData.game().endGame();
            String gameJson = new Gson().toJson(gameData.game());
            GameService.updateGameState(command.getAuthString(), command.getGameID(), gameJson);
            Notification notification = new Notification(connection.username + " has resigned the game.");
            connectionManager.notifyAll(command.getGameID(), notification);
            connectionManager.removeFromGame(command.getGameID(), command.getAuthString());
        } catch (ServiceException e) {
            sendError(session, e.getMessage());
        }
    }

    private void sendError(Session session, String message) {
        try {
            ErrorMessage error = new ErrorMessage(message);
            session.getRemote().sendString(new Gson().toJson(error));
        } catch (IOException ignored) {}
    }
}
