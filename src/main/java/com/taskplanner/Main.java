package com.taskplanner;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.sql.Connection;
import java.awt.*;
import javax.swing.*;
import javax.swing.event.ChangeListener;

import java.util.List;
import java.util.ArrayList;
import java.util.Date;

public class Main extends JFrame {

    private DefaultListModel<String> listModel = new DefaultListModel<>();
    private JList<String> taskList = new JList<>(listModel);
    private JTextField taskInput = new JTextField(30);
    private JSpinner dateInput = new JSpinner(
            new SpinnerDateModel(new Date(), null, null, java.util.Calendar.DAY_OF_MONTH));
    private JSpinner.DateEditor editor = new JSpinner.DateEditor(dateInput, "dd.MM.yyyy");
    LocalDate ldfinish = LocalDate.now().plusDays(7);
    Date finishdate = java.sql.Date.valueOf(ldfinish);
    private JSpinner finishdateInput = new JSpinner(
            new SpinnerDateModel(finishdate, null, null, java.util.Calendar.DAY_OF_MONTH));
    private JSpinner.DateEditor editorfinish = new JSpinner.DateEditor(finishdateInput, "dd.MM.yyyy");
    private JButton addButton = new JButton("Add Task");
    private JButton deleteButton = new JButton("Delete Task");
    private JButton editButton = new JButton("Edit Task");

    public Main() {
        // window-settings
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setTitle("Task Planner");
        Image icon = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/task.png"));
        setIconImage(icon);

        dateInput.setEditor(editor);
        finishdateInput.setEditor(editorfinish);

        setLayout(new BorderLayout());

        JPanel inputPanel = new JPanel();
        inputPanel.add(taskInput);
        inputPanel.add(addButton);
        inputPanel.add(editButton);
        inputPanel.add(deleteButton);
        inputPanel.add(dateInput);
        inputPanel.add(finishdateInput);

        add(inputPanel, BorderLayout.SOUTH);

        add(new JScrollPane(taskList), BorderLayout.CENTER);

        addButton.addActionListener(e -> addTask());
        deleteButton.addActionListener(e -> deleteTask());
        editButton.addActionListener(e -> editTask());

        refreshTaskList();
    }

    public void refreshTaskList() {
        ensureTableExists();
        List<String> tasks = loadTasksFromDatabase();

        taskList.setFont(new Font("SansSerif", Font.PLAIN, 24));
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
                PreparedStatement statement = conn
                        .prepareStatement(
                                "SELECT task, task_date, finish_date, is_done FROM tasks ORDER BY task_date ASC");
                ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                String task = rs.getString("task");
                String date = rs.getString("task_date");
                String finishdate = rs.getString("finish_date");
                if (finishdate == null)
                    finishdate = "";
                Boolean is_done = rs.getInt("is_done") == 1;
                LocalDate ld = LocalDate.parse(date);
                String formattedDate = ld.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));

                String formattedFinishDate = "";
                if (finishdate != null && !finishdate.isBlank()) {
                    LocalDate ldfinish = LocalDate.parse(finishdate);
                    formattedFinishDate = ldfinish.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
                }
                String display = task + " (" + formattedDate + ")" + " " + " (" + formattedFinishDate + ")" + " "
                        + is_done;
                result.add(display);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    public void addTask() {
        String taskName = taskInput.getText().trim();
        // Conversion of Date to String
        Date selectedDate = (Date) dateInput.getValue();
        java.sql.Date sqlDate = new java.sql.Date(selectedDate.getTime());
        String dateString = sqlDate.toString();

        // Conversion of finish Date to String
        Date finishDate = (Date) finishdateInput.getValue();
        java.sql.Date sqlfinishDate = new java.sql.Date(finishDate.getTime());
        String finishDateString = sqlfinishDate.toString();

        if (taskName.isEmpty()) {
            return;
        }
        String url = "jdbc:sqlite:tasks.db";
        try (
                Connection conn = DriverManager.getConnection(url);
                PreparedStatement statement = conn
                        .prepareStatement(
                                "INSERT INTO tasks (task, task_date, finish_date, is_done) VALUES (?, ?, ?, ?)");) {
            statement.setString(1, taskName);
            statement.setString(2, dateString);
            statement.setString(3, finishDateString);
            statement.setInt(4, 0);
            statement.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }

        refreshTaskList();
        taskInput.setText("");
    }

    public void deleteTask() {
        String selectedRow = taskList.getSelectedValue();
        String taskName = selectedRow.split(" \\(")[0];

        if (taskName == null) {
            return;
        }

        String sql = "DELETE FROM tasks WHERE task = ?";

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:tasks.db");
                PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, taskName);
            int rows = statement.executeUpdate();
            System.out.println("Deleted rows:" + rows);
        } catch (Exception e) {
            e.printStackTrace();
        }
        refreshTaskList();
    }

    public void editTask() {
        String selectedRow = taskList.getSelectedValue();
        String taskName = selectedRow.split(" \\(")[0];

    }

    public void ensureTableExists() {
        String url = "jdbc:sqlite:tasks.db";

        String sql = "CREATE TABLE IF NOT EXISTS tasks ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "task TEXT NOT NULL,"
                + "task_date TEXT NOT NULL,"
                + "finish_date TEXT,"
                + "is_done INTEGER DEFAULT 0"
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
