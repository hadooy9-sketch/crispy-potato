package qasystem.model;

import java.util.ArrayList;
import java.util.HashMap;

public class QASystem {
    private HashMap<String, User> users;
    private ArrayList<Question> questions;
    private User currentUser;
    
    public User findUser(String username) {

        return users.get(username);

    }

    
}
