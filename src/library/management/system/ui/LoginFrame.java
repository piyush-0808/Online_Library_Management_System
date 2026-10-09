package library.management.system.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.SwingUtilities;

import library.management.system.model.Role;
import library.management.system.model.User;
import library.management.system.service.AuthenticationService;
import library.management.system.util.MessageUtil;
import library.management.system.util.Validator;

public final class LoginFrame extends BaseFrame {
    private static final long serialVersionUID = 1L;

    private final transient AuthenticationService authenticationService;

    private JTextField usernameField;
    private JPasswordField passwordField;

    private JButton loginButton;
    private JButton registerButton;

    public LoginFrame() {

        super(
                "Library Management System - Login",
                1000,
                650
        );

        authenticationService =
                new AuthenticationService();

        initializeUI();
    }

    private void initializeUI() {

        JPanel header =
                createHeader(
                        "Library Management System",
                        "Secure access to your library account"
                );

        add(
                header,
                BorderLayout.NORTH
        );

        JPanel outerPanel =
                new JPanel(
                        new GridBagLayout()
                );

        outerPanel.setOpaque(false);

        JPanel loginCard =
                createCard();

        loginCard.setPreferredSize(
                new Dimension(
                        470,
                        410
                )
        );

        JPanel formPanel =
                new JPanel(
                        new GridBagLayout()
                );

        formPanel.setOpaque(false);

        GridBagConstraints gbc =
                new GridBagConstraints();

        gbc.gridx = 0;

        gbc.fill =
                GridBagConstraints.HORIZONTAL;

        gbc.weightx = 1.0;

        gbc.insets =
                new Insets(
                        7,
                        0,
                        7,
                        0
                );

        JLabel title =
                new JLabel(
                        "Welcome Back"
                );

        title.setFont(
                new Font(
                        "Segoe UI",
                        Font.BOLD,
                        25
                )
        );

        title.setForeground(
                UITheme.TEXT
        );

        gbc.gridy = 0;

        formPanel.add(
                title,
                gbc
        );

        JLabel subtitle =
                new JLabel(
                        "Sign in to continue"
                );

        subtitle.setFont(
                UITheme.NORMAL_FONT
        );

        subtitle.setForeground(
                UITheme.MUTED_TEXT
        );

        gbc.gridy = 1;

        formPanel.add(
                subtitle,
                gbc
        );

        JLabel usernameLabel =
                new JLabel(
                        "Username"
                );

        UITheme.styleFormLabel(
                usernameLabel
        );

        gbc.gridy = 2;

        gbc.insets =
                new Insets(
                        25,
                        0,
                        5,
                        0
                );

        formPanel.add(
                usernameLabel,
                gbc
        );

        usernameField =
                new JTextField();

        usernameField.setPreferredSize(
                new Dimension(
                        100,
                        42
                )
        );

        UITheme.styleTextField(
                usernameField
        );

        gbc.gridy = 3;

        gbc.insets =
                new Insets(
                        0,
                        0,
                        10,
                        0
                );

        formPanel.add(
                usernameField,
                gbc
        );

        JLabel passwordLabel =
                new JLabel(
                        "Password"
                );

        UITheme.styleFormLabel(
                passwordLabel
        );

        gbc.gridy = 4;

        gbc.insets =
                new Insets(
                        5,
                        0,
                        5,
                        0
                );

        formPanel.add(
                passwordLabel,
                gbc
        );

        passwordField =
                new JPasswordField();

        passwordField.setPreferredSize(
                new Dimension(
                        100,
                        42
                )
        );

        UITheme.stylePasswordField(
                passwordField
        );

        gbc.gridy = 5;

        gbc.insets =
                new Insets(
                        0,
                        0,
                        18,
                        0
                );

        formPanel.add(
                passwordField,
                gbc
        );

        loginButton =
                createButton(
                        "Sign In"
                );

        loginButton.setPreferredSize(
                new Dimension(
                        100,
                        44
                )
        );

        UITheme.styleButton(
                loginButton
        );

        gbc.gridy = 6;

        gbc.insets =
                new Insets(
                        5,
                        0,
                        8,
                        0
                );

        formPanel.add(
                loginButton,
                gbc
        );

        registerButton =
                new JButton(
                        "Create New Account"
                );

        registerButton.setFont(
                UITheme.NORMAL_FONT
        );

        registerButton.setForeground(
                UITheme.ACCENT
        );

        registerButton.setContentAreaFilled(
                false
        );

        registerButton.setBorder(
                BorderFactory.createEmptyBorder(
                        8,
                        8,
                        8,
                        8
                )
        );

        registerButton.setFocusPainted(false);

        registerButton.setCursor(
                new java.awt.Cursor(
                        java.awt.Cursor.HAND_CURSOR
                )
        );

        gbc.gridy = 7;

        gbc.insets =
                new Insets(
                        2,
                        0,
                        5,
                        0
                );

        formPanel.add(
                registerButton,
                gbc
        );

        loginCard.add(
                formPanel,
                BorderLayout.CENTER
        );

        outerPanel.add(
                loginCard
        );

        contentPanel.add(
                outerPanel,
                BorderLayout.CENTER
        );

        /*
         * Login button.
         */
        loginButton.addActionListener(
                e -> authenticate()
        );

        /*
         * Enter key triggers login.
         */
        passwordField.addActionListener(
                e -> authenticate()
        );

        usernameField.addActionListener(
                e -> passwordField.requestFocusInWindow()
        );

        /*
         * Registration screen.
         */
        registerButton.addActionListener(
                e -> openRegistration()
        );
    }

    private void authenticate() {

        String username =
                usernameField
                        .getText()
                        .trim();

        String password =
                new String(
                        passwordField.getPassword()
                );

        if (!Validator.isValidUsername(username)) {

            MessageUtil.showWarning(
                    this,
                    "Please enter a valid username."
            );

            usernameField.requestFocusInWindow();

            return;
        }

        if (!Validator.isNotBlank(password)) {

            MessageUtil.showWarning(
                    this,
                    "Please enter your password."
            );

            passwordField.requestFocusInWindow();

            return;
        }

        setLoginEnabled(false);

        SwingWorker<User, Void> worker =
                new SwingWorker<>() {

                    @Override
                    protected User doInBackground()
                            throws Exception {

                        return authenticationService.authenticate(
                                username,
                                password
                        );
                    }

                    @Override
                    protected void done() {

                        try {

                            User user = get();

                            setLoginEnabled(true);

                            passwordField.setText("");

                            openDashboard(user);

                        } catch (Exception e) {

                            setLoginEnabled(true);

                            passwordField.setText("");

                            Throwable cause =
                                    e.getCause() != null
                                            ? e.getCause()
                                            : e;

                            String message =
                                    cause.getMessage();

                            if (message == null
                                    || message.isBlank()) {

                                message =
                                        "Login failed.";
                            }

                            MessageUtil.showError(
                                    LoginFrame.this,
                                    message
                            );

                            System.err.println(
                                    "Login error: "
                                            + message
                            );
                        }
                    }
                };

        worker.execute();
    }

    private void openDashboard(
            User user) {

        if (user.getRole() == Role.LIBRARIAN) {

            LibrarianDashboardFrame dashboard =
                    new LibrarianDashboardFrame(
                            user
                    );

            dashboard.showFrame();

            dispose();

        } else {

            MemberDashboardFrame dashboard =
                    new MemberDashboardFrame(
                            user
                    );

            dashboard.showFrame();

            dispose();
        }
    }

    private void openRegistration() {

        RegistrationFrame registrationFrame =
                new RegistrationFrame();

        registrationFrame.showFrame();

        dispose();
    }

    private void setLoginEnabled(
            boolean enabled) {

        loginButton.setEnabled(enabled);

        registerButton.setEnabled(enabled);

        usernameField.setEnabled(enabled);

        passwordField.setEnabled(enabled);
    }

    public static void main(
            String[] args) {

        SwingUtilities.invokeLater(
                () -> {

                    LoginFrame frame =
                            new LoginFrame();

                    frame.showFrame();
                }
        );
    }
}