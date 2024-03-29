package server;

import com.google.gson.Gson;
import service.UserService;
import model.request.UserEnterRequest;
import model.response.result.ServiceException;
import model.response.result.UnauthorizedException;
import model.response.UserEnterResponse;
import spark.Request;
import spark.Response;
import spark.Spark;

public class LoginHandler extends ObjectSerializer {

    @Override
    public String handle(Request request, Response response) {
        Gson gson = new Gson();
        response.type("application/json");
        UserEnterRequest loginRequest = gson.fromJson(request.body(), UserEnterRequest.class);
        UserEnterResponse loginResponse = null;
        try {
            loginResponse = UserService.login(loginRequest);
        } catch (UnauthorizedException e) {
            Spark.halt(401, "{ \"message\": \"Error: unauthorized\" }");
        } catch (ServiceException e) {
            Spark.halt(500, "{ \"message\": \"Error: " + e.getMessage() + "\" }");
        }
        response.status(200);
        return gson.toJson(loginResponse);
    }
}
