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
import ru.coursework.clothingapp.model.Master;
import ru.coursework.clothingapp.repository.MasterDao;


public class MastersController {

    @FXML private TableView<Master> mastersTable;
    @FXML private TableColumn<Master, String> idColumn, surnameColumn, nameColumn, patronymicColumn, specializationColumn, phoneColumn;
    @FXML private TextField TextFieldMasterSearch;
    @FXML private Label LabelRecordCount;

    private final MasterDao masterDao = new MasterDao();
    private final ObservableList<Master> mastersList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getId().toString()));
        surnameColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSurname()));
        nameColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        patronymicColumn.setCellValueFactory(c -> new SimpleStringProperty(nullTo(c.getValue().getPatronymic(), "—")));
        specializationColumn.setCellValueFactory(c -> new SimpleStringProperty(nullTo(c.getValue().getSpecialization(), "—")));
        phoneColumn.setCellValueFactory(c -> new SimpleStringProperty(nullTo(c.getValue().getPhone(), "—")));

        FilteredList<Master> filtered = new FilteredList<>(mastersList);
        TextFieldMasterSearch.textProperty().addListener((_, _, val) -> {
            String q = val == null ? "" : val.toLowerCase().trim();
            filtered.setPredicate(m -> q.isEmpty() || matchesSearch(m, q));
            updateRecordCount(filtered.size(), mastersList.size());
        });

        SortedList<Master> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(mastersTable.comparatorProperty());
        mastersTable.setItems(sorted);
        loadMasters();
    }

    private boolean matchesSearch(Master m, String q) {
        return String.join(" ", String.valueOf(m.getId()), m.getSurname(), m.getName(),
            nullTo(m.getPatronymic(), ""), nullTo(m.getSpecialization(), ""), nullTo(m.getPhone(), ""))
            .toLowerCase().contains(q);
    }

    @FXML private void onAddClick() { showEditDialog(null); }

    @FXML
    private void onEditClick() {
        Master master = mastersTable.getSelectionModel().getSelectedItem();
        if (master == null) { showAlert("Выберите мастера"); return; }
        showEditDialog(master);
    }

    @FXML
    private void onDeleteClick() {
        Master master = mastersTable.getSelectionModel().getSelectedItem();
        if (master == null) { showAlert("Выберите мастера"); return; }

        if (confirmDelete("Удалить " + master.getSurname() + " " + master.getName() + "?")) {
            try {
                masterDao.delete(master);
                loadMasters();
            } catch (RuntimeException e) {
                if (e.getCause() instanceof org.hibernate.exception.ConstraintViolationException) {
                    showAlert("Невозможно удалить: мастер связан с заказами.");
                } else {
                    showAlert("Ошибка: " + e.getMessage());
                }
            }
        }
    }

    private void loadMasters() {
        mastersList.setAll(masterDao.findAll());
        updateRecordCount(mastersTable.getItems().size(), mastersList.size());
    }

    private void showEditDialog(Master master) {
        Dialog<Master> dialog = new Dialog<>();
        dialog.setTitle(master == null ? "Добавить мастера" : "Редактировать мастера");

        TextField surnameField = new TextField(), nameField = new TextField();
        TextField patronymicField = new TextField(), specializationField = new TextField(), phoneField = new TextField();
        surnameField.setPromptText("Фамилия"); nameField.setPromptText("Имя");
        patronymicField.setPromptText("Отчество"); specializationField.setPromptText("Специализация"); phoneField.setPromptText("Телефон");

        if (master != null) {
            surnameField.setText(master.getSurname());
            nameField.setText(master.getName());
            patronymicField.setText(nullTo(master.getPatronymic(), ""));
            specializationField.setText(nullTo(master.getSpecialization(), ""));
            phoneField.setText(nullTo(master.getPhone(), ""));
        }

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20, 10, 10, 10));
        grid.addRow(0, new Label("Фамилия:*"), surnameField);
        grid.addRow(1, new Label("Имя:*"), nameField);
        grid.addRow(2, new Label("Отчество:"), patronymicField);
        grid.addRow(3, new Label("Специализация:"), specializationField);
        grid.addRow(4, new Label("Телефон:"), phoneField);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(
            new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE),
            new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE)
        );

        dialog.setResultConverter(btn -> {
            if (btn.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                String surname = surnameField.getText().trim(), name = nameField.getText().trim();
                if (surname.isEmpty() || name.isEmpty()) { showAlert("Фамилия и имя обязательны"); return null; }
                Master m = master != null ? master : new Master();
                m.setSurname(surname);
                m.setName(name);
                m.setPatronymic(emptyToNull(patronymicField.getText()));
                m.setSpecialization(emptyToNull(specializationField.getText()));
                m.setPhone(emptyToNull(phoneField.getText()));
                return m;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(m -> {
            if (master == null) masterDao.save(m); else masterDao.update(m);
            loadMasters();
        });
    }

    private boolean confirmDelete(String msg) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, msg);
        confirm.setHeaderText("Удаление мастера");
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
