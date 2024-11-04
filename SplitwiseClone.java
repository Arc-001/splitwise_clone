import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.sql.*;
import java.util.*;
import java.util.List;


// Helper class to store expense information
class Expense {
    private int id;  // Added id field
    private String name;
    private double amount;

    public Expense(int id, String name, double amount) {
        this.id = id;
        this.name = name;
        this.amount = amount;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public double getAmount() { return amount; }

    @Override
    public String toString() {
        return String.format("%s: $%.2f", name, amount);
    }
}

    // Helper class to manage group information
class Group {
    private String name;
    private Set<String> members;

    public Group(String name) {
        this.name = name;
        this.members = new HashSet<>();
    }

    public String getName() { return name; }
    public Set<String> getMembers() { return members; }

    public boolean addMember(String member) {
        return members.add(member);
    }
}

// Custom button class with rounded corners
class RoundedButton extends JButton {
    public RoundedButton(String text) {
        super(text);
        setOpaque(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setFont(new Font("Segoe UI", Font.BOLD, 14));
        setForeground(Color.WHITE);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                setBackground(new Color(80, 140, 190));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setBackground(new Color(100, 160, 210));
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (getModel().isPressed()) {
            g2.setColor(new Color(60, 120, 170));
        } else if (getModel().isRollover()) {
            g2.setColor(new Color(80, 140, 190));
        } else {
            g2.setColor(new Color(100, 160, 210));
        }

        g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 15, 15));
        g2.dispose();

        super.paintComponent(g);
    }
}
//jdbc:mysql://localhost:3306/splitwise_clone
//jdbc:sqlserver://server:port;DatabaseName=dbname
class DatabaseManager {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/splitwise_clone";
    private static final String USER = "root";
    private static final String PASS = "DB!d43m0n";

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "MySQL JDBC Driver not found. Include it in your library path.");
        }
        return DriverManager.getConnection(DB_URL, USER, PASS);
    }

    public static void initializeDatabase() {
        try (Connection conn = getConnection()) {
            Statement stmt = conn.createStatement();

            // Create expenses table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS expenses (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(255) NOT NULL,
                    amount DECIMAL(10,2) NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // Create expense_groups table (renamed from groups)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS expense_groups (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(255) NOT NULL UNIQUE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // Create participants table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS participants (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(255) NOT NULL UNIQUE,
                    email VARCHAR(255),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // Create group_members junction table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS group_members (
                    group_id INT,
                    participant_id INT,
                    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (group_id, participant_id),
                    FOREIGN KEY (group_id) REFERENCES expense_groups(id),
                    FOREIGN KEY (participant_id) REFERENCES participants(id)
                )
            """);

            // Create expense_shares table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS expense_shares (
                    expense_id INT,
                    participant_id INT,
                    share_amount DECIMAL(10,2) NOT NULL,
                    is_paid BOOLEAN DEFAULT FALSE,
                    PRIMARY KEY (expense_id, participant_id),
                    FOREIGN KEY (expense_id) REFERENCES expenses(id),
                    FOREIGN KEY (participant_id) REFERENCES participants(id)
                )
            """);

            // Create indexes
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_expense_created ON expenses(created_at)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_group_name ON expense_groups(name)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_participant_name ON participants(name)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_expense_shares_paid ON expense_shares(is_paid)");

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Failed to initialize database: " + e.getMessage());
        }
    }
}


class SplitwiseClone extends JFrame {
    private JScrollPane expenseScrollPane;
    private JTextField expenseNameField, expenseAmountField, participantField, groupNameField;
    private JTextArea expenseList;
    private RoundedButton addExpenseButton, addParticipantButton, calculateButton, createGroupButton, addToGroupButton;
    private JList<String> participantList, groupList, groupMemberList;
    private DefaultListModel<String> participantListModel, groupListModel, groupMemberListModel;
    private ArrayList<Expense> expenses;
    private HashMap<String, Group> groups;

    private static final Color BACKGROUND_COLOR = new Color(240, 240, 250);
    private static final Color ACCENT_COLOR = new Color(70, 130, 180);
    private static final Color BUTTON_COLOR = new Color(100, 160, 210);
    private static final Font LABEL_FONT = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font INPUT_FONT = new Font("Segoe UI", Font.PLAIN, 14);

    public SplitwiseClone() {
        expenses = new ArrayList<>();
        groups = new HashMap<>();

        // Initialize database
        DatabaseManager.initializeDatabase();

        setTitle("Splitwise Clone");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(BACKGROUND_COLOR);

        setupUI();
        loadDataFromDatabase();
    }

    private void setupUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        JPanel inputPanel = new JPanel(new GridBagLayout());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        JPanel listPanel = new JPanel(new GridLayout(1, 3, 10, 0));

        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(BACKGROUND_COLOR);
        inputPanel.setBackground(BACKGROUND_COLOR);
        buttonPanel.setBackground(BACKGROUND_COLOR);

        // Initialize components
        expenseNameField = createStyledTextField();
        expenseAmountField = createStyledTextField();
        participantField = createStyledTextField();
        groupNameField = createStyledTextField();
        expenseList = createStyledTextArea();
        expenseScrollPane = new JScrollPane(expenseList);
        expenseScrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(ACCENT_COLOR),
                "Expenses",
                TitledBorder.CENTER,
                TitledBorder.TOP,
                LABEL_FONT,
                ACCENT_COLOR
        ));

        addExpenseButton = new RoundedButton("Add Expense");
        addParticipantButton = new RoundedButton("Add Participant");
        calculateButton = new RoundedButton("Calculate Split");
        createGroupButton = new RoundedButton("Create Group");
        addToGroupButton = new RoundedButton("Add to Group");
        mainPanel.add(expenseScrollPane, BorderLayout.EAST);

        participantListModel = new DefaultListModel<>();
        groupListModel = new DefaultListModel<>();
        groupMemberListModel = new DefaultListModel<>();

        participantList = new JList<>(participantListModel);
        groupList = new JList<>(groupListModel);
        groupMemberList = new JList<>(groupMemberListModel);

        setupListStyles();
        layoutComponents(mainPanel, inputPanel, buttonPanel, listPanel);
        setupEventListeners();
    }

    private void setupListStyles() {
        participantList.setFont(INPUT_FONT);
        groupList.setFont(INPUT_FONT);
        groupMemberList.setFont(INPUT_FONT);
    }
    private void addLabelAndField(JPanel panel, String labelText, JTextField textField, GridBagConstraints gbc, int gridy) {
        gbc.gridx = 0;
        gbc.gridy = gridy;
        panel.add(new JLabel(labelText), gbc);

        gbc.gridx = 1;
        panel.add(textField, gbc);
    }
    private void layoutComponents(JPanel mainPanel, JPanel inputPanel, JPanel buttonPanel, JPanel listPanel) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        addLabelAndField(inputPanel, "Expense Name:", expenseNameField, gbc, 0);
        addLabelAndField(inputPanel, "Expense Amount:", expenseAmountField, gbc, 1);
        addLabelAndField(inputPanel, "Participant Name:", participantField, gbc, 2);
        addLabelAndField(inputPanel, "Group Name:", groupNameField, gbc, 3);

        buttonPanel.add(addExpenseButton);
        buttonPanel.add(addParticipantButton);
        buttonPanel.add(calculateButton);
        buttonPanel.add(createGroupButton);
        buttonPanel.add(addToGroupButton);

        listPanel.add(createStyledScrollPane(participantList, "Participants"));
        listPanel.add(createStyledScrollPane(groupList, "Groups"));
        listPanel.add(createStyledScrollPane(groupMemberList, "Group Members"));

        mainPanel.add(inputPanel, BorderLayout.NORTH);
        mainPanel.add(buttonPanel, BorderLayout.CENTER);
        mainPanel.add(listPanel, BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);
    }

    private void setupEventListeners() {
        addExpenseButton.addActionListener(e -> addExpense());
        addParticipantButton.addActionListener(e -> addParticipant());
        calculateButton.addActionListener(e -> calculateSplit());
        createGroupButton.addActionListener(e -> createGroup());
        addToGroupButton.addActionListener(e -> addToGroup());
        groupList.addListSelectionListener(e -> updateGroupMemberList());
    }

    private void loadDataFromDatabase() {
        try (Connection conn = DatabaseManager.getConnection()) {
            // Load participants
            String sql = "SELECT name FROM participants";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    participantListModel.addElement(rs.getString("name"));
                }
            }

            // Load groups
            sql = "SELECT name FROM expense_groups";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    String groupName = rs.getString("name");
                    groupListModel.addElement(groupName);
                    loadGroupMembers(conn, groupName);
                }
            }

            // Load expenses
            sql = "SELECT name, amount FROM expenses ORDER BY created_at DESC";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    expenses.add(new Expense(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getDouble("amount")
                    ));
                }
            }
            updateExpenseList();
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Failed to load data from database: " + e.getMessage());
        }
    }

    private void loadGroupMembers(Connection conn, String groupName) throws SQLException {
        String sql = """
            SELECT p.name 
            FROM participants p 
            JOIN group_members gm ON p.id = gm.participant_id 
            JOIN expense_groups g ON gm.group_id = g.id 
            WHERE g.name = ?
        """;

        Group group = new Group(groupName);
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, groupName);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                group.addMember(rs.getString("name"));
            }
        }
        groups.put(groupName, group);
    }

    private void addExpense() {
        String name = expenseNameField.getText().trim();
        String amountText = expenseAmountField.getText().trim();

        if (name.isEmpty() || amountText.isEmpty()) {
            showError("Please enter both expense name and amount.");
            return;
        }

        try {
            double amount = Double.parseDouble(amountText);

            try (Connection conn = DatabaseManager.getConnection()) {
                String sql = "INSERT INTO expenses (name, amount) VALUES (?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setString(1, name);
                    stmt.setDouble(2, amount);
                    stmt.executeUpdate();

                    // Get the generated ID
                    try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            int id = generatedKeys.getInt(1);
                            expenses.add(new Expense(id, name, amount));
                            updateExpenseList();
                            expenseNameField.setText("");
                            expenseAmountField.setText("");
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                showError("Failed to add expense: " + e.getMessage());
            }
        } catch (NumberFormatException e) {
            showError("Please enter a valid number for the expense amount.");
        }
    }

    private void addParticipant() {
        String name = participantField.getText().trim();
        if (name.isEmpty()) {
            showError("Please enter a participant name.");
            return;
        }

        if (!participantListModel.contains(name)) {
            try (Connection conn = DatabaseManager.getConnection()) {
                String sql = "INSERT INTO participants (name) VALUES (?)";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, name);
                    stmt.executeUpdate();
                }

                participantListModel.addElement(name);
                participantField.setText("");
            } catch (SQLException e) {
                e.printStackTrace();
                showError("Failed to add participant: " + e.getMessage());
            }
        } else {
            showError("This participant has already been added.");
        }
    }

    private void createGroup() {
        String name = groupNameField.getText().trim();
        if (name.isEmpty()) {
            showError("Please enter a group name.");
            return;
        }

        if (!groups.containsKey(name)) {
            try (Connection conn = DatabaseManager.getConnection()) {
                String sql = "INSERT INTO expense_groups (name) VALUES (?)";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, name);
                    stmt.executeUpdate();
                }

                groups.put(name, new Group(name));
                groupListModel.addElement(name);
                groupNameField.setText("");
            } catch (SQLException e) {
                e.printStackTrace();
                showError("Failed to create group: " + e.getMessage());
            }
        } else {
            showError("This group already exists.");
        }
    }

    private void addToGroup() {
        String groupName = groupList.getSelectedValue();
        String participantName = participantList.getSelectedValue();

        if (groupName == null || participantName == null) {
            showError("Please select both a group and a participant.");
            return;
        }

        Group group = groups.get(groupName);
        if (group.getMembers().contains(participantName)) {
            showError("Participant is already a member of the group.");
            return;
        }

        try (Connection conn = DatabaseManager.getConnection()) {
            // Get IDs for group and participant
            int groupId = getGroupId(conn, groupName);
            int participantId = getParticipantId(conn, participantName);

            if (groupId == -1 || participantId == -1) {
                showError("Failed to find group or participant in database.");
                return;
            }

            // Add to group_members table
            String sql = "INSERT INTO group_members (group_id, participant_id) VALUES (?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, groupId);
                stmt.setInt(2, participantId);
                stmt.executeUpdate();
            }

            if (group.addMember(participantName)) {
                updateGroupMemberList();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Failed to add member to group: " + e.getMessage());
        }
    }


    private int getGroupId(Connection conn, String groupName) throws SQLException {
        String sql = "SELECT id FROM expense_groups WHERE name = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, groupName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        }
        return -1;
    }

    private int getParticipantId(Connection conn, String participantName) throws SQLException {
        String sql = "SELECT id FROM participants WHERE name = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, participantName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        }
        return -1;
    }

    private void calculateSplit() {
        String selectedGroup = groupList.getSelectedValue();
        if (selectedGroup == null) {
            showError("Please select a group to calculate the split.");
            return;
        }

        Group group = groups.get(selectedGroup);
        if (group.getMembers().isEmpty() || expenses.isEmpty()) {
            showError("Add group members and expenses first.");
            return;
        }

        try (Connection conn = DatabaseManager.getConnection()) {
            // Calculate total expenses
            double totalExpense = 0;
            String sql = "SELECT SUM(amount) as total FROM expenses";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    totalExpense = rs.getDouble("total");
                }
            }

            // Get group members count
            sql = """
            SELECT COUNT(*) as member_count 
            FROM group_members gm 
            JOIN expense_groups g ON gm.group_id = g.id 
            WHERE g.name = ?
        """;
            int memberCount = 0;
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, selectedGroup);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    memberCount = rs.getInt("member_count");
                }
            }

            if (memberCount == 0) {
                showError("No members in the selected group.");
                return;
            }

            double splitAmount = totalExpense / memberCount;

            // Show results
            StringBuilder result = new StringBuilder("Expense Split for group " + selectedGroup + ":\n\n");
            for (String member : group.getMembers()) {
                result.append(member)
                        .append(": $")
                        .append(String.format("%.2f", splitAmount))
                        .append("\n");

                // Store split in database
                int participantId = getParticipantId(conn, member);
                if (participantId != -1) {
                    sql = "INSERT INTO expense_shares (expense_id, participant_id, share_amount) VALUES (?, ?, ?)";
                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.setInt(1, 1); // Assuming first expense for simplicity
                        stmt.setInt(2, participantId);
                        stmt.setDouble(3, splitAmount);
                        stmt.executeUpdate();
                    }
                }
            }

            JTextArea textArea = new JTextArea(result.toString());
            textArea.setEditable(false);
            textArea.setFont(INPUT_FONT);
            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(300, 200));

            JOptionPane.showMessageDialog(
                    this,
                    scrollPane,
                    "Expense Split Results",
                    JOptionPane.INFORMATION_MESSAGE
            );
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Failed to calculate split: " + e.getMessage());
        }
    }


    private JTextField createStyledTextField() {
        JTextField field = new JTextField(20);
        field.setFont(INPUT_FONT);
        field.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(ACCENT_COLOR, 1),
                new EmptyBorder(5, 5, 5, 5)
        ));
        return field;
    }

    private JTextArea createStyledTextArea() {
        JTextArea area = new JTextArea(10, 40);
        area.setFont(INPUT_FONT);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(ACCENT_COLOR, 1),
                new EmptyBorder(5, 5, 5, 5)
        ));
        return area;
    }

    private JScrollPane createStyledScrollPane(JComponent component, String title) {
        JScrollPane scrollPane = new JScrollPane(component);
        scrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(ACCENT_COLOR),
                title,
                TitledBorder.CENTER,
                TitledBorder.TOP,
                LABEL_FONT,
                ACCENT_COLOR
        ));
        return scrollPane;
    }

    private void updateExpenseList() {
        StringBuilder sb = new StringBuilder("Recent Expenses:\n\n");
        for (Expense expense : expenses) {
            sb.append(expense.toString()).append("\n");
        }
        expenseList.setText(sb.toString());
    }

    private void updateGroupMemberList() {
        String selectedGroup = groupList.getSelectedValue();
        groupMemberListModel.clear();

        if (selectedGroup != null && groups.containsKey(selectedGroup)) {
            Group group = groups.get(selectedGroup);
            for (String member : group.getMembers()) {
                groupMemberListModel.addElement(member);
            }
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                "Error",
                JOptionPane.ERROR_MESSAGE
        );
    }

    // Additional utility methods for database operations
    private void markExpenseAsPaid(int expenseId, int participantId) {
        try (Connection conn = DatabaseManager.getConnection()) {
            String sql = "UPDATE expense_shares SET is_paid = TRUE WHERE expense_id = ? AND participant_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, expenseId);
                stmt.setInt(2, participantId);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Failed to mark expense as paid: " + e.getMessage());
        }
    }

    private void deleteExpense(int expenseId) {
        try (Connection conn = DatabaseManager.getConnection()) {
            // First delete related expense shares
            String sql = "DELETE FROM expense_shares WHERE expense_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, expenseId);
                stmt.executeUpdate();
            }

            // Then delete the expense
            sql = "DELETE FROM expenses WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, expenseId);
                stmt.executeUpdate();
            }

            // Update the local list
            expenses.removeIf(e -> e.getId() == expenseId);
            updateExpenseList();
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Failed to delete expense: " + e.getMessage());
        }
    }

    private void deleteGroup(int groupId) {
        try (Connection conn = DatabaseManager.getConnection()) {
            // First delete group members
            String sql = "DELETE FROM group_members WHERE group_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, groupId);
                stmt.executeUpdate();
            }

            // Then delete the group
            sql = "DELETE FROM expense_groups WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, groupId);
                stmt.executeUpdate();
            }

            // Update the UI
            String groupName = groupList.getSelectedValue();
            if (groupName != null) {
                groups.remove(groupName);
                groupListModel.removeElement(groupName);
                groupMemberListModel.clear();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Failed to delete group: " + e.getMessage());
        }
    }

    private void generateExpenseReport(String groupName) {
        try (Connection conn = DatabaseManager.getConnection()) {
            StringBuilder report = new StringBuilder();
            report.append("Expense Report for ").append(groupName).append("\n\n");

            // Get total expenses for the group
            String sql = """
                SELECT SUM(es.share_amount) as total, 
                       COUNT(DISTINCT e.id) as expense_count,
                       SUM(CASE WHEN es.is_paid THEN es.share_amount ELSE 0 END) as paid_amount
                FROM expenses e
                JOIN expense_shares es ON e.id = es.expense_id
                JOIN group_members gm ON es.participant_id = gm.participant_id
                JOIN expense_groups g ON gm.group_id = g.id
                WHERE g.name = ?
            """;

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, groupName);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    report.append(String.format("Total Expenses: $%.2f\n", rs.getDouble("total")))
                            .append(String.format("Number of Expenses: %d\n", rs.getInt("expense_count")))
                            .append(String.format("Amount Paid: $%.2f\n", rs.getDouble("paid_amount")))
                            .append(String.format("Amount Remaining: $%.2f\n\n",
                                    rs.getDouble("total") - rs.getDouble("paid_amount")));
                }
            }

            // Show the report
            JTextArea textArea = new JTextArea(report.toString());
            textArea.setEditable(false);
            textArea.setFont(INPUT_FONT);
            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(400, 300));

            JOptionPane.showMessageDialog(
                    this,
                    scrollPane,
                    "Expense Report",
                    JOptionPane.INFORMATION_MESSAGE
            );
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Failed to generate report: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            SplitwiseClone app = new SplitwiseClone();
            app.setLocationRelativeTo(null);
            app.setVisible(true);
        });
    }
}
