package library.management.system.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

import library.management.system.member.Member;
import library.management.system.model.MemberType;
import library.management.system.service.MemberService;
import library.management.system.util.MessageUtil;
import library.management.system.util.Validator;

public final class RegistrationFrame extends BaseFrame {
    private static final long serialVersionUID = 1L;

    private final transient MemberService memberService;

    private JTextField fullNameField;
    private JTextField usernameField;
    private JTextField emailField;
    private JTextField phoneField;
    private JTextField departmentField;

    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;

    private JComboBox<MemberType> memberTypeCombo;

    private JButton registerButton;
    private JButton backButton;

    public RegistrationFrame() {

        super(
                "Library Management System - Registration",
                1050,
                700
        );

        memberService = new MemberService();

        initializeUI();
    }

    private void initializeUI() {

        JPanel header =
                createHeader(
                        "Create Library Account",
                        "Register as a Student or Faculty member"
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

        JPanel card =
                createCard();

        card.setPreferredSize(
                new Dimension(
                        720,
                        540
                )
        );

        JPanel formPanel =
                new JPanel(
                        new GridBagLayout()
                );

        formPanel.setOpaque(false);

        GridBagConstraints gbc =
                new GridBagConstraints();

        gbc.fill =
                GridBagConstraints.HORIZONTAL;

        gbc.weightx = 1.0;

        gbc.insets =
                new Insets(
                        5,
                        8,
                        5,
                        8
                );

        JLabel title =
                new JLabel(
                        "Member Registration"
                );

        title.setFont(
                new Font(
                        "Segoe UI",
                        Font.BOLD,
                        24
                )
        );

        title.setForeground(
                UITheme.TEXT
        );

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;

        formPanel.add(
                title,
                gbc
        );

        JLabel subtitle =
                new JLabel(
                        "Your account will remain pending until approved by a librarian."
                );

        subtitle.setFont(
                UITheme.SMALL_FONT
        );

        subtitle.setForeground(
                UITheme.MUTED_TEXT
        );

        gbc.gridy = 1;

        gbc.insets =
                new Insets(
                        0,
                        8,
                        18,
                        8
                );

        formPanel.add(
                subtitle,
                gbc
        );

        /*
         * Full name
         */
        addField(
                formPanel,
                gbc,
                2,
                "Full Name",
                fullNameField = new JTextField()
        );

        /*
         * Username
         */
        addField(
                formPanel,
                gbc,
                3,
                "Username",
                usernameField = new JTextField()
        );

        /*
         * Email
         */
        addField(
                formPanel,
                gbc,
                4,
                "Email",
                emailField = new JTextField()
        );

        /*
         * Phone
         */
        addField(
                formPanel,
                gbc,
                5,
                "Phone",
                phoneField = new JTextField()
        );

        /*
         * Department
         */
        addField(
                formPanel,
                gbc,
                6,
                "Department",
                departmentField = new JTextField()
        );

        /*
         * Member type
         */
        JLabel typeLabel =
                createLabel(
                        "Member Type"
                );

        UITheme.styleFormLabel(
                typeLabel
        );

        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.gridwidth = 1;

        formPanel.add(
                typeLabel,
                gbc
        );

        memberTypeCombo =
                new JComboBox<>(
                        MemberType.values()
                );

        UITheme.styleComboBox(
                memberTypeCombo
        );

        gbc.gridx = 1;

        formPanel.add(
                memberTypeCombo,
                gbc
        );

        /*
         * Password
         */
        addField(
                formPanel,
                gbc,
                8,
                "Password",
                passwordField =
                        new JPasswordField()
        );

        /*
         * Confirm password
         */
        addField(
                formPanel,
                gbc,
                9,
                "Confirm Password",
                confirmPasswordField =
                        new JPasswordField()
        );

        /*
         * Buttons.
         */
        JPanel buttonPanel =
                new JPanel();

        buttonPanel.setOpaque(false);

        registerButton =
                createButton(
                        "Create Account",
                        UITheme.ACCENT
                );

        backButton =
                createButton(
                        "Back",
                        UITheme.PRIMARY
                );

        buttonPanel.add(
                registerButton
        );

        buttonPanel.add(
                backButton
        );

        gbc.gridx = 0;
        gbc.gridy = 10;
        gbc.gridwidth = 2;

        gbc.insets =
                new Insets(
                        20,
                        8,
                        5,
                        8
                );

        formPanel.add(
                buttonPanel,
                gbc
        );

        card.add(
                formPanel,
                BorderLayout.CENTER
        );

        outerPanel.add(
                card
        );

        contentPanel.add(
                outerPanel,
                BorderLayout.CENTER
        );

        registerButton.addActionListener(
                e -> register()
        );

        backButton.addActionListener(
                e -> goBack()
        );

        confirmPasswordField.addActionListener(
                e -> register()
        );
    }

    private void addField(
            JPanel panel,
            GridBagConstraints gbc,
            int row,
            String labelText,
            JTextField field) {

        JLabel label =
                createLabel(
                        labelText
                );

        UITheme.styleFormLabel(
                label
        );

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;

        gbc.insets =
                new Insets(
                        5,
                        8,
                        5,
                        8
                );

        panel.add(
                label,
                gbc
        );

        field.setPreferredSize(
                new Dimension(
                        100,
                        38
                )
        );

        if (field instanceof JPasswordField password) {

            UITheme.stylePasswordField(
                    password
            );

        } else {

            UITheme.styleTextField(
                    field
            );
        }

        gbc.gridx = 1;

        panel.add(
                field,
                gbc
        );
    }

    private void register() {

        String fullName =
                fullNameField
                        .getText()
                        .trim();

        String username =
                usernameField
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

        String department =
                departmentField
                        .getText()
                        .trim();

        String password =
                new String(
                        passwordField.getPassword()
                );

        String confirmPassword =
                new String(
                        confirmPasswordField.getPassword()
                );

        MemberType memberType =
                (MemberType) memberTypeCombo
                        .getSelectedItem();

        if (!Validator.isNotBlank(fullName)) {

            showValidation(
                    "Full name is required."
            );

            return;
        }

        if (!Validator.isValidUsername(username)) {

            showValidation(
                    "Username must be 3-30 characters and "
                            + "contain only letters, numbers, underscore or dot."
            );

            return;
        }

        if (!Validator.isValidEmail(email)) {

            showValidation(
                    "Please enter a valid email address."
            );

            return;
        }

        if (!Validator.isValidPhone(phone)) {

            showValidation(
                    "Phone number must contain exactly 10 digits."
            );

            return;
        }

        if (!Validator.isNotBlank(department)) {

            showValidation(
                    "Department is required."
            );

            return;
        }

        if (memberType == null) {

            showValidation(
                    "Please select a member type."
            );

            return;
        }

        if (!Validator.isValidPassword(password)) {

            showValidation(
                    "Password must contain at least 6 characters."
            );

            return;
        }

        if (!password.equals(confirmPassword)) {

            showValidation(
                    "Passwords do not match."
            );

            return;
        }

        registerButton.setEnabled(false);
        backButton.setEnabled(false);

        SwingWorker<Member, Void> worker =
                new SwingWorker<>() {

                    @Override
                    protected Member doInBackground()
                            throws Exception {

                        return memberService.registerMember(
                                username,
                                password,
                                fullName,
                                email,
                                phone,
                                memberType,
                                department
                        );
                    }

                    @Override
                    protected void done() {

                        registerButton.setEnabled(true);
                        backButton.setEnabled(true);

                        try {

                            Member member =
                                    get();

                            MessageUtil.showSuccess(
                                    RegistrationFrame.this,
                                    "Registration successful!\n\n"
                                            + "Member ID: "
                                            + member.getMemberId()
                                            + "\n\n"
                                            + "Your account is pending "
                                            + "librarian approval."
                            );

                            clearForm();

                            goBack();

                        } catch (Exception e) {

                            Throwable cause =
                                    e.getCause() != null
                                            ? e.getCause()
                                            : e;

                            String message =
                                    cause.getMessage();

                            if (message == null
                                    || message.isBlank()) {

                                message =
                                        "Registration failed.";
                            }

                            MessageUtil.showError(
                                    RegistrationFrame.this,
                                    message
                            );

                            System.err.println(
                                    "Registration error: "
                                            + message
                            );
                        }
                    }
                };

        worker.execute();
    }

    private void showValidation(
            String message) {

        MessageUtil.showWarning(
                this,
                message
        );
    }

    private void clearForm() {

        fullNameField.setText("");
        usernameField.setText("");
        emailField.setText("");
        phoneField.setText("");
        departmentField.setText("");

        passwordField.setText("");
        confirmPasswordField.setText("");

        memberTypeCombo.setSelectedIndex(0);
    }

    private void goBack() {

        new LoginFrame().showFrame();

        dispose();
    }
}