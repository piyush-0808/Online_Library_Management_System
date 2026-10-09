package library.management.system.ui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

import library.management.system.model.User;
import library.management.system.service.UserService;
import library.management.system.util.MessageUtil;

public final class ProfileFrame extends BaseFrame {
    private static final long serialVersionUID = 1L;

    private final transient UserService userService;
    private final transient User loggedInUser;

    private JTextField nameField;
    private JTextField emailField;
    private JTextField phoneField;

    private JPasswordField oldPasswordField;
    private JPasswordField newPasswordField;
    private JPasswordField confirmPasswordField;

    public ProfileFrame(User user) {

        super(
                "Library Management System - Profile",
                800,
                650
        );

        this.loggedInUser = user;
        this.userService = new UserService();

        initializeUI();
        loadProfile();
    }

    private void initializeUI() {

        add(
                createHeader(
                        "My Profile",
                        "View and update your account information"
                ),
                BorderLayout.NORTH
        );

        JPanel mainPanel =
                new JPanel(
                        new GridBagLayout()
                );

        mainPanel.setOpaque(false);

        GridBagConstraints gbc =
                new GridBagConstraints();

        gbc.insets =
                new Insets(
                        8,
                        12,
                        8,
                        12
                );

        gbc.fill =
                GridBagConstraints.HORIZONTAL;

        gbc.weightx = 1;

        nameField =
                new JTextField();

        emailField =
                new JTextField();

        phoneField =
                new JTextField();

        oldPasswordField =
                new JPasswordField();

        newPasswordField =
                new JPasswordField();

        confirmPasswordField =
                new JPasswordField();

        UITheme.styleTextField(
                nameField
        );

        UITheme.styleTextField(
                emailField
        );

        UITheme.styleTextField(
                phoneField
        );

        UITheme.stylePasswordField(
                oldPasswordField
        );

        UITheme.stylePasswordField(
                newPasswordField
        );

        UITheme.stylePasswordField(
                confirmPasswordField
        );

        int row = 0;

        addField(
                mainPanel,
                gbc,
                row++,
                "Full Name",
                nameField
        );

        addField(
                mainPanel,
                gbc,
                row++,
                "Email",
                emailField
        );

        addField(
                mainPanel,
                gbc,
                row++,
                "Phone",
                phoneField
        );

        JButton updateButton =
                createButton(
                        "Update Profile"
                );

        updateButton.addActionListener(
                e -> updateProfile()
        );

        gbc.gridx = 1;
        gbc.gridy = row++;

        mainPanel.add(
                updateButton,
                gbc
        );

        JLabel passwordTitle =
                new JLabel(
                        "Change Password"
                );

        /*
         * UITheme.SUBHEADING_FONT is not available
         * in the current UITheme, so use a direct
         * Swing font here.
         */
        passwordTitle.setFont(
                new Font(
                        "Segoe UI",
                        Font.BOLD,
                        18
                )
        );

        passwordTitle.setForeground(
                UITheme.TEXT
        );

        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 2;

        mainPanel.add(
                passwordTitle,
                gbc
        );

        gbc.gridwidth = 1;

        addField(
                mainPanel,
                gbc,
                row++,
                "Current Password",
                oldPasswordField
        );

        addField(
                mainPanel,
                gbc,
                row++,
                "New Password",
                newPasswordField
        );

        addField(
                mainPanel,
                gbc,
                row++,
                "Confirm Password",
                confirmPasswordField
        );

        JButton changePasswordButton =
                createButton(
                        "Change Password"
                );

        changePasswordButton.addActionListener(
                e -> changePassword()
        );

        gbc.gridx = 1;
        gbc.gridy = row;

        mainPanel.add(
                changePasswordButton,
                gbc
        );

        JPanel card =
                createCard();

        card.add(
                mainPanel,
                BorderLayout.CENTER
        );

        contentPanel.add(
                card,
                BorderLayout.CENTER
        );
    }

    private void addField(
            JPanel panel,
            GridBagConstraints gbc,
            int row,
            String labelText,
            java.awt.Component component) {

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.3;

        JLabel label =
                new JLabel(
                        labelText
                );

        label.setForeground(
                UITheme.TEXT
        );

        panel.add(
                label,
                gbc
        );

        gbc.gridx = 1;
        gbc.weightx = 1;

        panel.add(
                component,
                gbc
        );
    }

    private void loadProfile() {

        if (loggedInUser == null) {
            return;
        }

        nameField.setText(
                safe(
                        loggedInUser.getFullName()
                )
        );

        emailField.setText(
                safe(
                        loggedInUser.getEmail()
                )
        );

        phoneField.setText(
                safe(
                        loggedInUser.getPhone()
                )
        );
    }

    private void updateProfile() {

        String name =
                nameField
                        .getText()
                        .trim();

        String email =
                emailField
                        .getText()
                        .trim();

        String phone =
                phoneField
                        .getText()
                        .trim();

        if (name.isEmpty()
                || email.isEmpty()
                || phone.isEmpty()) {

            MessageUtil.showWarning(
                    this,
                    "Please fill all profile fields."
            );

            return;
        }

        SwingWorker<Boolean, Void> worker =
                new SwingWorker<>() {

                    @Override
                    protected Boolean doInBackground()
                            throws Exception {

                        return userService.updateProfile(
                                loggedInUser.getUserId(),
                                name,
                                email,
                                phone
                        );
                    }

                    @Override
                    protected void done() {

                        try {

                            if (get()) {

                                loggedInUser.setFullName(
                                        name
                                );

                                loggedInUser.setEmail(
                                        email
                                );

                                loggedInUser.setPhone(
                                        phone
                                );

                                MessageUtil.showSuccess(
                                        ProfileFrame.this,
                                        "Profile updated successfully."
                                );

                            } else {

                                MessageUtil.showWarning(
                                        ProfileFrame.this,
                                        "Profile could not be updated."
                                );
                            }

                        } catch (Exception e) {

                            showError(e);
                        }
                    }
                };

        worker.execute();
    }

    private void changePassword() {

        String oldPassword =
                new String(
                        oldPasswordField.getPassword()
                );

        String newPassword =
                new String(
                        newPasswordField.getPassword()
                );

        String confirmPassword =
                new String(
                        confirmPasswordField.getPassword()
                );

        if (oldPassword.isEmpty()
                || newPassword.isEmpty()
                || confirmPassword.isEmpty()) {

            MessageUtil.showWarning(
                    this,
                    "Please fill all password fields."
            );

            return;
        }

        if (!newPassword.equals(
                confirmPassword)) {

            MessageUtil.showError(
                    this,
                    "New password and confirmation do not match."
            );

            return;
        }

        SwingWorker<Boolean, Void> worker =
                new SwingWorker<>() {

                    @Override
                    protected Boolean doInBackground()
                            throws Exception {

                        return userService.changePassword(
                                loggedInUser.getUserId(),
                                oldPassword,
                                newPassword
                        );
                    }

                    @Override
                    protected void done() {

                        try {

                            if (get()) {

                                MessageUtil.showSuccess(
                                        ProfileFrame.this,
                                        "Password changed successfully."
                                );

                                oldPasswordField.setText(
                                        ""
                                );

                                newPasswordField.setText(
                                        ""
                                );

                                confirmPasswordField.setText(
                                        ""
                                );

                            } else {

                                MessageUtil.showError(
                                        ProfileFrame.this,
                                        "Password could not be changed."
                                );
                            }

                        } catch (Exception e) {

                            showError(e);
                        }
                    }
                };

        worker.execute();
    }

    private void showError(
            Exception exception) {

        Throwable cause =
                exception.getCause() != null
                        ? exception.getCause()
                        : exception;

        String message =
                cause.getMessage();

        if (message == null
                || message.isBlank()) {

            message =
                    "An unexpected error occurred.";
        }

        MessageUtil.showError(
                this,
                message
        );

        System.err.println(
                "Profile operation error: "
                        + message
        );
    }

    private String safe(
            String value) {

        return value == null
                ? ""
                : value;
    }
}