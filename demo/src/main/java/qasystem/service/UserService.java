package qasystem.service;
import qasystem.model.QASystem;



public class UserService {
    private QASystem qaSystem;
    public boolean login(String username,String password);

    public UserService(QASystem qaSystem) {

        this.qaSystem = qaSystem;
    }

    
}
