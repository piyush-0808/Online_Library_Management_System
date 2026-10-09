package library.management.system.model;

/**
 * Abstract parent class for all people in the library system.
 *
 * Inheritance:
 * Librarian and Member inherit common person information.
 *
 * Polymorphism:
 * getRole() and getWelcomeMessage() are implemented differently
 * by child classes.
 */
public abstract class Person {

    private String fullName;
    private String email;
    private String phone;

    protected Person(String fullName, String email, String phone) {
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public abstract Role getRole();

    public abstract String getWelcomeMessage();
}