package library.management.system.model;

public class User {

    private long userId;
    private String username;
    private String passwordHash;
    private String salt;

    private Role role;
    private MemberStatus status;

    private String fullName;
    private String email;
    private String phone;

    public User() {
    }

    public User(
        long userId,
        String username,
        String passwordHash,
        String salt,
        Role role,
        MemberStatus status) {

    this(
            userId,
            username,
            passwordHash,
            salt,
            role,
            status,
            null,
            null,
            null
    );
}

    public User(
            long userId,
            String username,
            String passwordHash,
            String salt,
            Role role,
            MemberStatus status,
            String fullName,
            String email,
            String phone) {

        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.role = role;
        this.status = status;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
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

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public MemberStatus getStatus() {
        return status;
    }

    public void setStatus(MemberStatus status) {
        this.status = status;
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

    @Override
    public String toString() {
        return "User{"
                + "userId=" + userId
                + ", username='" + username + '\''
                + ", role=" + role
                + ", status=" + status
                + ", fullName='" + fullName + '\''
                + ", email='" + email + '\''
                + ", phone='" + phone + '\''
                + '}';
    }
}