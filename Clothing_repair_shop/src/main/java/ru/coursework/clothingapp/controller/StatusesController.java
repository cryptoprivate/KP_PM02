package ru.coursework.clothingapp.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import ru.coursework.clothingapp.model.OrderStatus;
import ru.coursework.clothingapp.repository.OrderStatusDao;

public class StatusesController {

    @FXML private TableView<OrderStatus> statusesTable;
    @FXML private TableColumn<OrderStatus, String> idColumn;
    @FXML private TableColumn<OrderStatus, String> nameColumn;

    private final OrderStatusDao statusDao = new OrderStatusDao();
    private final ObservableList<OrderStatus> statusesList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getId().toString()));
        nameColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        statusesTable.setItems(statusesList);
        loadStatuses();
    }

    @FXML private void onAddClick() { showEditDialog(null); }

    @FXML
    private void onEditClick() {
        OrderStatus status = statusesTable.getSelectionModel().getSelectedItem();
        if (status == null) { showAlert("Выберите статус"); return; }
        showEditDialog(status);
    }

    @FXML
    private void onDeleteClick() {
        OrderStatus status = statusesTable.getSelectionModel().getSelectedItem();
        if (status == null) { showAlert("Выберите статус"); return; }

        if (confirmDelete("Удалить статус '" + status.getName() + "'?")) {
            try {
                statusDao.delete(status);
                loadStatuses();
            } catch (RuntimeException e) {
                if (e.getCause() instanceof org.hibernate.exception.ConstraintViolationException) {
                    showAlert("Невозможно удалить: статус связан с заказами.");
                } else {
                    showAlert("Ошибка: " + e.getMessage());
                }
            }
        }
    }

    private void loadStatuses() {
        statusesList.setAll(statusDao.findAll());
    }

    private void showEditDialog(OrderStatus status) {
        Dialog<OrderStatus> dialog = new Dialog<>();
        dialog.setTitle(status == null ? "Добавить статус" : "Редактировать статус");

        TextField nameField = new TextField();
        nameField.setPromptText("Название");
        if (status != null) nameField.setText(status.getName());

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20, 10, 10, 10));
        grid.addRow(0, new Label("Название:"), nameField);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(
            new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE),
            new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE)
        );

        dialog.setResultConverter(btn -> {
            if (btn.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                String name = nameField.getText().trim();
                if (name.isEmpty()) { showAlert("Название обязательно"); return null; }
                OrderStatus s = status != null ? status : new OrderStatus();
                s.setName(name);
                return s;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(s -> {
            if (status == null) statusDao.save(s); else statusDao.update(s);
            loadStatuses();
        });
    }

    private boolean confirmDelete(String msg) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, msg);
        confirm.setHeaderText("Удаление статуса");
        ButtonType yes = new ButtonType("Да", ButtonBar.ButtonData.OK_DONE);
        ButtonType no = new ButtonType("Нет", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(yes, no);
        return confirm.showAndWait().orElse(no) == yes;
    }

    private void showAlert(String msg) {
        new Alert(Alert.AlertType.WARNING, msg).showAndWait();
    }
}
