package com.taskplanner;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.sql.Connection;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import java.util.List;
import java.util.ArrayList;
import java.util.Date;

public class Main extends JFrame {

    private DefaultListModel<Task> listModel = new DefaultListModel<>();
    private JTable taskTable;
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
    private static final String url = "jdbc:sqlite:tasks.db";
    private JButton finishedTasksButton = new JButton("Finished Tasks");
    private JTable finishedTaskTable;

    public Main() {
        // window-settings
        // setExtendedState(JFrame.MAXIMIZED_BOTH); // fullscreen
        setSize(1000, 650);
        setLocationRelativeTo(null);
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
        inputPanel.add(finishedTasksButton);

        add(inputPanel, BorderLayout.SOUTH);

        taskTable = new JTable();
        taskTable.setFont(new Font("SansSerif", Font.PLAIN, 20));
        taskTable.setRowHeight(28);

        JScrollPane scroll = new JScrollPane(taskTable);
        add(scroll, BorderLayout.CENTER);

        scroll.getViewport().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = taskTable.rowAtPoint(e.getPoint());
                if (row == -1) {
                    taskTable.clearSelection();
                    taskTable.getSelectionModel().setAnchorSelectionIndex(-1);
                    taskTable.getSelectionModel().setLeadSelectionIndex(-1);
                    taskTable.repaint();
                }
            }
        });

        addButton.addActionListener(e -> addTask());
        deleteButton.addActionListener(e -> deleteTask());
        editButton.addActionListener(e -> editTask());
        finishedTasksButton.addActionListener(e -> showFinishedTasks());

        refreshTaskList();
    }

    public class Task {
        public int id;
        public String task;
        public LocalDate startDate;
        public LocalDate finishDate;
        public boolean isDone;

        @Override
        public String toString() {
            String sd = startDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            String fd = finishDate != null ? finishDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) : "";
            return task + " (" + sd + ") (" + fd + ") " + isDone;
        }
    }

    public void refreshTaskList() {
        ensureTableExists();
        List<Task> tasks = loadTasksFromDatabase();

        TaskTableModel model = new TaskTableModel(tasks, "tasks");
        taskTable.setModel(model);

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);

        for (int col = 0; col < taskTable.getColumnCount() - 1; col++) {
            taskTable.getColumnModel().getColumn(col).setCellRenderer(center);
        }
        taskTable.setFont(new Font("SansSerif", Font.PLAIN, 24));

        model.addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.UPDATE && e.getColumn() == 3) {
                int modelRow = e.getFirstRow();
                Task t = model.getTaskAtModelRow(modelRow);

                if (t.isDone) { // move only when checked
                    moveToFinishedTasks(t);
                    refreshTaskList();
                }
            }
        });
    }

    public void refreshFinishedTaskList(JTable table) {
        ensureFinishedTableExists();
        List<Task> ftasks = loadTasksFromFinishedDatabase();

        TaskTableModel fmodel = new TaskTableModel(ftasks, "finishedtasks");
        table.setModel(fmodel);

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);

        for (int col = 0; col < table.getColumnCount() - 1; col++) {
            table.getColumnModel().getColumn(col).setCellRenderer(center);
        }
        table.setFont(new Font("SansSerif", Font.PLAIN, 24));

        fmodel.addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.UPDATE && e.getColumn() == 3) {
                int modelRow = e.getFirstRow();
                Task t = fmodel.getTaskAtModelRow(modelRow);

                if (!t.isDone) { // move back only when unchecked
                    moveToTasks(t);
                    refreshFinishedTaskList(table);
                    refreshTaskList();
                }
            }
        });
    }

    public List<Task> loadTasksFromDatabase() {
        List<Task> result = new ArrayList<>();

        // database connection

        try (
                Connection conn = DriverManager.getConnection(url);
                PreparedStatement statement = conn
                        .prepareStatement(
                                "SELECT id, task, task_date, finish_date, is_done FROM tasks ORDER BY task_date ASC");
                ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                Task t = new Task();
                t.task = rs.getString("task");
                t.id = rs.getInt("id");
                t.startDate = LocalDate.parse(rs.getString("task_date"));

                String fd = rs.getString("finish_date");
                t.finishDate = (fd == null || fd.isBlank()) ? null : LocalDate.parse(fd);

                t.isDone = rs.getInt("is_done") == 1;
                result.add(t);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    public List<Task> loadTasksFromFinishedDatabase() {
        List<Task> result = new ArrayList<>();

        // database connection

        try (
                Connection conn = DriverManager.getConnection(url);
                PreparedStatement statement = conn
                        .prepareStatement(
                                "SELECT id, task, task_date, finish_date, is_done FROM finishedtasks ORDER BY task_date ASC");
                ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                Task t = new Task();
                t.task = rs.getString("task");
                t.id = rs.getInt("id");
                t.startDate = LocalDate.parse(rs.getString("task_date"));

                String fd = rs.getString("finish_date");
                t.finishDate = (fd == null || fd.isBlank()) ? null : LocalDate.parse(fd);

                t.isDone = rs.getInt("is_done") == 1;
                result.add(t);
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
        int row = taskTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a task to delete");
            return;
        }
        Task selected = ((TaskTableModel) taskTable.getModel()).tasks.get(row);

        if (selected == null) {
            return;
        }

    }

    public void editTask() {
        int row = taskTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a task to edit");
            return;
        }

        Task selected = ((TaskTableModel) taskTable.getModel()).tasks.get(row);

        JDialog editdialog = new JDialog(this, "Edit Task", true);
        editdialog.setSize(800, 500);
        editdialog.setLayout(new BorderLayout());

        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));

        JTextField nameField = new JTextField(selected.task);
        Date startAsDate = java.sql.Date.valueOf(selected.startDate);
        Date finishAsDate = selected.finishDate != null ? java.sql.Date.valueOf(selected.finishDate) : new Date();
        JSpinner startDateNew = new JSpinner(
                new SpinnerDateModel(startAsDate, null, null, java.util.Calendar.DAY_OF_MONTH)); // new startDate after
        startDateNew.setEditor(new JSpinner.DateEditor(startDateNew, "dd.MM.yyyy"));
        // edit
        JSpinner finishDateNew = new JSpinner(
                new SpinnerDateModel(finishAsDate, null, null, java.util.Calendar.DAY_OF_MONTH));
        finishDateNew.setEditor(new JSpinner.DateEditor(finishDateNew, "dd.MM.yyyy"));

        panel.add(new JLabel("Task Name: "));
        panel.add(nameField);

        panel.add(new JLabel("Start Date: "));
        panel.add(startDateNew);

        panel.add(new JLabel("Finish date: "));
        panel.add(finishDateNew);

        editdialog.add(panel, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel();
        JButton saveBtn = new JButton("Save");
        JButton cancelBtn = new JButton("Cancel");

        btnPanel.add(saveBtn);
        btnPanel.add(cancelBtn);
        editdialog.add(btnPanel, BorderLayout.SOUTH);

        cancelBtn.addActionListener(e -> editdialog.dispose());

        saveBtn.addActionListener(e -> {

            String newName = nameField.getText().trim(); // Name after edit
            // Conversion of new start Date after edit
            Date selectedStartDateNew = (Date) startDateNew.getValue();
            java.sql.Date sqlStartDateNew = new java.sql.Date(selectedStartDateNew.getTime());
            String startDateNewString = sqlStartDateNew.toString();

            // Conversion of new finish Date after edit
            Date selectedFinishDateNew = (Date) finishDateNew.getValue();
            java.sql.Date sqlFinishDateNew = new java.sql.Date(selectedFinishDateNew.getTime());
            String finishDateNewString = sqlFinishDateNew.toString();

            String sql = "UPDATE tasks SET task = ?, task_date = ?, finish_date = ? WHERE id = ?";

            try (Connection conn = DriverManager.getConnection(url);
                    PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setString(1, newName);
                statement.setString(2, startDateNewString);
                statement.setString(3, finishDateNewString);
                statement.setInt(4, selected.id);
                statement.executeUpdate();
            } catch (Exception f) {
                f.printStackTrace();
            }
            editdialog.dispose();
            refreshTaskList();
        });

        editdialog.setLocationRelativeTo(this);
        editdialog.setVisible(true);
    }

    public void showFinishedTasks() {
        JDialog showFinishedTasksDialog = new JDialog(this, "Finished Tasks", true);
        showFinishedTasksDialog.setSize(1000, 650);
        showFinishedTasksDialog.setLayout(new BorderLayout());

        JPanel btnPanel = new JPanel();
        JButton closeBtn = new JButton("Close Window");
        btnPanel.add(closeBtn);
        showFinishedTasksDialog.add(btnPanel, BorderLayout.SOUTH);

        closeBtn.addActionListener(e -> showFinishedTasksDialog.dispose());

        finishedTaskTable = new JTable();
        finishedTaskTable.setFont(new Font("SansSerif", Font.PLAIN, 20));
        finishedTaskTable.setRowHeight(28);

        JScrollPane scrollFinished = new JScrollPane(finishedTaskTable);
        showFinishedTasksDialog.add(scrollFinished, BorderLayout.CENTER);

        refreshFinishedTaskList(finishedTaskTable);

        showFinishedTasksDialog.setLocationRelativeTo(this);
        showFinishedTasksDialog.setVisible(true);
    }

    public void moveToFinishedTasks(Task t) {
        String taskName = t.task;
        String dateString = t.startDate.toString();
        String finishDateString = (t.finishDate == null) ? null : t.finishDate.toString();
        int id = t.id;

        if (taskName.isEmpty()) {
            return;
        }
        try (
                Connection conn = DriverManager.getConnection(url);
                PreparedStatement statement = conn
                        .prepareStatement(
                                "INSERT INTO finishedtasks (task, task_date, finish_date, is_done) VALUES (?, ?, ?, ?)");) {
            statement.setString(1, taskName);
            statement.setString(2, dateString);
            statement.setString(3, finishDateString);
            statement.setInt(4, 1);
            statement.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
        try (
                Connection conn = DriverManager.getConnection(url);
                PreparedStatement statement = conn
                        .prepareStatement(
                                "DELETE FROM tasks WHERE id = ?");) {
            statement.setInt(1, id);
            statement.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
        if (finishedTaskTable != null) {
            refreshFinishedTaskList(finishedTaskTable);
        }
        refreshTaskList();
    }

    public void moveToTasks(Task t) {
        String taskName = t.task;
        String dateString = t.startDate.toString();
        String finishDateString = (t.finishDate == null) ? null : t.finishDate.toString();
        int id = t.id;

        if (taskName.isEmpty()) {
            return;
        }
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

        try (
                Connection conn = DriverManager.getConnection(url);
                PreparedStatement statement = conn
                        .prepareStatement(
                                "DELETE FROM finishedtasks WHERE id = ?");) {
            statement.setInt(1, id);
            statement.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
        if (finishedTaskTable != null) {
            refreshFinishedTaskList(finishedTaskTable);
        }
        refreshTaskList();
    }

    public class TaskTableModel extends AbstractTableModel {
        private final String[] columns = { "Task", "Start Date", "Finish Date", "Done" };
        private final List<Task> tasks;
        private final String tableName;

        public TaskTableModel(List<Task> tasks, String tableName) {
            this.tasks = tasks;
            this.tableName = tableName;
        }

        public Task getTaskAtModelRow(int modelRow) {
            return tasks.get(modelRow);
        }

        @Override
        public int getRowCount() {
            return tasks.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int col) {
            return columns[col];
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return switch (columnIndex) {
                case 0 -> String.class; // Task name
                case 1 -> LocalDate.class; // Start date
                case 2 -> LocalDate.class; // Finish date
                case 3 -> Boolean.class; // Done
                default -> Object.class;
            };
        }

        @Override
        public Object getValueAt(int row, int col) {
            Task t = tasks.get(row);
            return switch (col) {
                case 0 -> t.task;
                case 1 -> t.startDate;
                case 2 -> t.finishDate;
                case 3 -> t.isDone;
                default -> "";
            };
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return col == 3;
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            if (col != 3)
                return;

            Task t = tasks.get(row);
            boolean newDone = (Boolean) value;

            t.isDone = newDone;

            String sql = "UPDATE " + tableName + " SET is_done = ? WHERE id = ?";

            try (Connection conn = DriverManager.getConnection(url);
                    PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, t.isDone ? 1 : 0);
                stmt.setInt(2, t.id);
                stmt.executeUpdate();
            } catch (Exception e) {
                e.printStackTrace();
            }

            fireTableRowsUpdated(row, row);
        }
    }

    public void ensureTableExists() {

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

    public void ensureFinishedTableExists() {

        String sql = "CREATE TABLE IF NOT EXISTS finishedtasks ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "task TEXT NOT NULL,"
                + "task_date TEXT NOT NULL,"
                + "finish_date TEXT,"
                + "is_done INTEGER DEFAULT 1"
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
