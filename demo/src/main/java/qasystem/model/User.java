package qasystem.model;

import java.util.EnumSet;

public class User {
    private String username;
    private  String password;
    private EnumSet<Role> roles;
    // Constructor
    public User(String username, String password) {

    this.username = username;
    this.password = password;
    this.roles = EnumSet.noneOf(Role.class);

}

    // Methods
    public String getUsername(){
        return  username;
    }

    public boolean checkPassword(String password){
        return  this.password.equals(password);
    }
        

    public boolean  changePassword(String oldPassword,String newPassword){
        if (this.checkPassword(oldPassword)) {
            this.password = newPassword;
            return  true;
        }
        return  false;
    }

    public void addRole(Role role){
        roles.add(role);
    }

    public boolean removeRole(Role role){
             return roles.remove(role);
    }

    public boolean hasRole(Role role){
       return roles.contains(role);
    }

    
}
