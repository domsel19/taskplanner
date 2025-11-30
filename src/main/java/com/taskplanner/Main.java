package com.taskplanner;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Connection;
import java.util.Set;
import java.awt.*;
import javax.swing.*;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;

public class Main extends JFrame {

    private DefaultListModel<String> listModel = new DefaultListModel<>();
    private JList<String> taskList = new JList<>(listModel);
    private JTextField taskInput = new JTextField(30);
    private JButton addButton = new JButton("Add Task");

    public Main() {
        // window-settings
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setTitle("Task Planner");
        Image icon = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/task.png"));
        setIconImage(icon);

        setLayout(new BorderLayout());

        JPanel inputPanel = new JPanel();
        inputPanel.add(taskInput);
        inputPanel.add(addButton);

        add(inputPanel, BorderLayout.NORTH);

        add(new JScrollPane(taskList), BorderLayout.CENTER);

        addButton.addActionListener(e -> addTask());

        refreshTaskList();
    }

    public void refreshTaskList() {
        ensureTableExists();
        List<String> tasks = loadTasksFromDatabase();

        listModel.clear();
        for (String t : tasks) {
            listModel.addElement(t);
        }
    }

    public List<String> loadTasksFromDatabase() {
        List<String> result = new ArrayList<>();

        // database connection
        String url = "jdbc:sqlite:tasks.db";
        try (
                Connection conn = DriverManager.getConnection(url);
                PreparedStatement statement = conn.prepareStatement("SELECT task FROM tasks");
                ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                result.add(rs.getString("task"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    public void addTask() {
        String taskName = taskInput.getText().trim();

        if (taskName.isEmpty()) {
            return;
        }
        String url = "jdbc:sqlite:tasks.db";
        try (
                Connection conn = DriverManager.getConnection(url);
                PreparedStatement statement = conn.prepareStatement("INSERT INTO tasks (task) VALUES (?)");) {
            statement.setString(1, taskName);
            statement.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }

        refreshTaskList();
        taskInput.setText("");
    }

    public void ensureTableExists() {
        String url = "jdbc:sqlite:tasks.db";

        String sql = "CREATE TABLE IF NOT EXISTS tasks ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "task TEXT NOT NULL"
                + ");";

        try (Connection conn = DriverManager.getConnection(url);
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Main window = new Main();
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setVisible(true);
    }
}
