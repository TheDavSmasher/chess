package server;

import service.UserService;
import service.request.AuthRequest;
import service.result.ServiceException;
import service.result.UnauthorizedException;
import spark.Request;
import spark.Response;
import spark.Spark;

public class LogoutHandler extends ObjectSerializer {

    @Override
    public String handle(Request request, Response response) {
        response.type("application/json");
        AuthRequest logoutRequest = new AuthRequest(getAuthToken(request));
        try {
            UserService.logout(logoutRequest);
        } catch (UnauthorizedException e) {
            Spark.halt(401, "{ \"message\": \"Error: unauthorized\" }");
        } catch (ServiceException e) {
            Spark.halt(500, "{ \"message\": \"Error: " + e.getMessage() + "\" }");
        }
        response.status(200);
        return "{}";
    }
}
