package library.management.system.member;

import library.management.system.model.Person;
import library.management.system.model.Role;

public class Librarian extends Person {

    private long userId;
    private String username;

    public Librarian(
            String fullName,
            String email,
            String phone) {

        super(fullName, email, phone);
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public Role getRole() {
        return Role.LIBRARIAN;
    }

    @Override
    public String getWelcomeMessage() {
        return "Welcome, Librarian " + getFullName() + "!";
    }
}