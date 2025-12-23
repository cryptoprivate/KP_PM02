package ru.coursework.clothingapp.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import ru.coursework.clothingapp.model.Client;
import ru.coursework.clothingapp.repository.ClientDao;


public class ClientsController {

    @FXML private TableView<Client> clientsTable;
    @FXML private TableColumn<Client, String> idColumn, surnameColumn, nameColumn, patronymicColumn, phoneColumn;
    @FXML private TextField TextFieldClientSearch;
    @FXML private Label LabelRecordCount;

    private final ClientDao clientDao = new ClientDao();
    private final ObservableList<Client> clientsList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getId().toString()));
        surnameColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSurname()));
        nameColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        patronymicColumn.setCellValueFactory(c -> new SimpleStringProperty(nullTo(c.getValue().getPatronymic(), "—")));
        phoneColumn.setCellValueFactory(c -> new SimpleStringProperty(nullTo(c.getValue().getPhone(), "—")));

        FilteredList<Client> filtered = new FilteredList<>(clientsList);
        TextFieldClientSearch.textProperty().addListener((_, _, val) -> {
            String q = val == null ? "" : val.toLowerCase().trim();
            filtered.setPredicate(c -> q.isEmpty() || matchesSearch(c, q));
            updateRecordCount(filtered.size(), clientsList.size());
        });

        SortedList<Client> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(clientsTable.comparatorProperty());
        clientsTable.setItems(sorted);
        loadClients();
    }

    private boolean matchesSearch(Client c, String q) {
        return String.join(" ", String.valueOf(c.getId()), c.getSurname(), c.getName(),
            nullTo(c.getPatronymic(), ""), nullTo(c.getPhone(), "")).toLowerCase().contains(q);
    }

    @FXML private void onAddClick() { showEditDialog(null); }

    @FXML
    private void onEditClick() {
        Client client = clientsTable.getSelectionModel().getSelectedItem();
        if (client == null) { showAlert("Выберите клиента"); return; }
        showEditDialog(client);
    }

    @FXML
    private void onDeleteClick() {
        Client client = clientsTable.getSelectionModel().getSelectedItem();
        if (client == null) { showAlert("Выберите клиента"); return; }

        if (confirmDelete("Удалить " + client.getSurname() + " " + client.getName() + "?")) {
            try {
                clientDao.delete(client);
                loadClients();
            } catch (RuntimeException e) {
                if (e.getCause() instanceof org.hibernate.exception.ConstraintViolationException) {
                    showAlert("Невозможно удалить: клиент связан с заказами.");
                } else {
                    showAlert("Ошибка: " + e.getMessage());
                }
            }
        }
    }

    private void loadClients() {
        clientsList.setAll(clientDao.findAll());
        updateRecordCount(clientsTable.getItems().size(), clientsList.size());
    }

    private void showEditDialog(Client client) {
        Dialog<Client> dialog = new Dialog<>();
        dialog.setTitle(client == null ? "Добавить клиента" : "Редактировать клиента");

        TextField surnameField = new TextField(), nameField = new TextField();
        TextField patronymicField = new TextField(), phoneField = new TextField();
        surnameField.setPromptText("Фамилия"); nameField.setPromptText("Имя");
        patronymicField.setPromptText("Отчество"); phoneField.setPromptText("Телефон");

        if (client != null) {
            surnameField.setText(client.getSurname());
            nameField.setText(client.getName());
            patronymicField.setText(nullTo(client.getPatronymic(), ""));
            phoneField.setText(nullTo(client.getPhone(), ""));
        }

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20, 10, 10, 10));
        grid.addRow(0, new Label("Фамилия:*"), surnameField);
        grid.addRow(1, new Label("Имя:*"), nameField);
        grid.addRow(2, new Label("Отчество:"), patronymicField);
        grid.addRow(3, new Label("Телефон:"), phoneField);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(
            new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE),
            new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE)
        );

        dialog.setResultConverter(btn -> {
            if (btn.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                String surname = surnameField.getText().trim(), name = nameField.getText().trim();
                if (surname.isEmpty() || name.isEmpty()) { showAlert("Фамилия и имя обязательны"); return null; }
                Client c = client != null ? client : new Client();
                c.setSurname(surname);
                c.setName(name);
                c.setPatronymic(emptyToNull(patronymicField.getText()));
                c.setPhone(emptyToNull(phoneField.getText()));
                return c;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(c -> {
            if (client == null) clientDao.save(c); else clientDao.update(c);
            loadClients();
        });
    }

    private boolean confirmDelete(String msg) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, msg);
        confirm.setHeaderText("Удаление клиента");
        ButtonType yes = new ButtonType("Да", ButtonBar.ButtonData.OK_DONE);
        ButtonType no = new ButtonType("Нет", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(yes, no);
        return confirm.showAndWait().orElse(no) == yes;
    }

    private void showAlert(String msg) { new Alert(Alert.AlertType.WARNING, msg).showAndWait(); }
    private String nullTo(String s, String def) { return s != null ? s : def; }
    private String emptyToNull(String s) { return s.trim().isEmpty() ? null : s.trim(); }
    private void updateRecordCount(int shown, int total) {
        if (LabelRecordCount != null) LabelRecordCount.setText("Записей найдено: " + shown + " из " + total);
    }
}
