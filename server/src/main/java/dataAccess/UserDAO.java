package dataAccess;

import model.dataAccess.UserData;

public interface UserDAO {
    UserData getUser(String username) throws DataAccessException;
    UserData getUser(String username, String password) throws DataAccessException;
    void createUser(String username, String password, String email) throws DataAccessException;
    void clear() throws DataAccessException;
}
