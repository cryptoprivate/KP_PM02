package ru.coursework.clothingapp.controller;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import ru.coursework.clothingapp.model.*;
import ru.coursework.clothingapp.repository.*;
import ru.coursework.clothingapp.util.PriceCalculator;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class OrderEditController implements Initializable {

    private final ClientDao clientDao = new ClientDao();
    private final MasterDao masterDao = new MasterDao();
    private final OrderStatusDao statusDao = new OrderStatusDao();
    private final ItemDao itemDao = new ItemDao();
    private final DefectDao defectDao = new DefectDao();
    private final OrderDao orderDao = new OrderDao();

    private Order currentOrder;
    private Item selectedItem;

    @FXML private Button BtnSave, BtnCancel;
    @FXML private ComboBox<Client> ComboBoxClient;
    @FXML private ComboBox<Master> ComboBoxMaster;
    @FXML private TextField TextFieldOrderNumber;
    @FXML private DatePicker DatePickerAccept, DatePickerDue;
    @FXML private TableView<Item> TableViewItems;
    @FXML private TableColumn<Item, String> TableColumnType, TableColumnSize;
    @FXML private TableView<Defect> TableViewDefects;
    @FXML private TableColumn<Defect, String> TableColumnDefectType, TableColumnDescription;
    @FXML private TableColumn<Defect, Number> TableColumnSeverity;
    @FXML private Label LabelTotalCost;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        ComboBoxClient.setItems(FXCollections.observableArrayList(clientDao.findAll()));
        ComboBoxMaster.setItems(FXCollections.observableArrayList(masterDao.findAll()));

        TableColumnType.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getType()));
        TableColumnSize.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSize()));
        TableColumnDefectType.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDefectType()));
        TableColumnDescription.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDescription()));
        TableColumnSeverity.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getSeverity()));

        TableViewItems.getSelectionModel().selectedItemProperty().addListener((_, _, item) -> {
            selectedItem = item;
            TableViewDefects.getItems().clear();
            if (item != null) TableViewDefects.getItems().addAll(item.getDefects());
        });
    }

    @FXML
    void BtnCancelOnAction() {
        ((Stage) BtnCancel.getScene().getWindow()).close();
    }

    @FXML
    void BtnSaveOnAction() {
        String error = validateFields();
        if (!error.isEmpty()) { showError(error); return; }

        try {
            currentOrder.setClient(ComboBoxClient.getValue());
            currentOrder.setOrderNumber(TextFieldOrderNumber.getText().trim());
            currentOrder.setAcceptDate(DatePickerAccept.getValue());
            currentOrder.setDueDate(DatePickerDue.getValue());
            currentOrder.setMaster(ComboBoxMaster.getValue());

            if (currentOrder.getId() == null) {
                currentOrder.setStatus(getOrCreateStatus("Принят"));
                orderDao.save(currentOrder);
            } else {
                orderDao.update(currentOrder);
            }
            showInfo("Заказ сохранён");
            ((Stage) BtnSave.getScene().getWindow()).close();
        } catch (Exception e) {
            showError("Ошибка: " + e.getMessage());
        }
    }

    private String validateFields() {
        StringBuilder err = new StringBuilder();
        if (ComboBoxClient.getValue() == null) err.append("Выберите клиента\n");
        if (TextFieldOrderNumber.getText().trim().isEmpty()) err.append("Укажите номер заказа\n");
        if (DatePickerAccept.getValue() == null) err.append("Выберите дату приёма\n");
        if (DatePickerDue.getValue() == null) err.append("Выберите дату готовности\n");
        if (currentOrder.getItems().isEmpty()) err.append("Добавьте изделие\n");
        if (DatePickerAccept.getValue() != null && DatePickerDue.getValue() != null &&
            DatePickerAccept.getValue().isAfter(DatePickerDue.getValue())) {
            err.append("Дата готовности не может быть раньше даты приёма\n");
        }
        return err.toString();
    }

    public void setOrder(Order order) {
        this.currentOrder = order;
        ComboBoxClient.setValue(order.getClient());
        TextFieldOrderNumber.setText(order.getOrderNumber());
        DatePickerAccept.setValue(order.getAcceptDate());
        DatePickerDue.setValue(order.getDueDate());
        ComboBoxMaster.setValue(order.getMaster());
        TableViewItems.setItems(FXCollections.observableArrayList(order.getItems()));
        updateTotalCost();
    }

    public void initNewOrder() {
        this.currentOrder = new Order();
        TextFieldOrderNumber.setText("ORD-" + LocalDate.now().getYear() + "-" + String.format("%03d", (int)(Math.random() * 1000)));
        DatePickerAccept.setValue(LocalDate.now());
        DatePickerDue.setValue(LocalDate.now().plusDays(7));
        updateTotalCost();
    }

    private void updateTotalCost() {
        BigDecimal total = PriceCalculator.calculateEstimate(currentOrder);
        if (LabelTotalCost != null) {
            LabelTotalCost.setText(String.format("Смета: %.2f ₽", total));
        }
    }

    @FXML
    void BtnAddItemOnAction() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Добавить изделие");

        ComboBox<String> typeCombo = new ComboBox<>(FXCollections.observableArrayList(itemDao.findDistinctTypes()));
        typeCombo.setEditable(true);
        typeCombo.setPromptText("Тип изделия");
        TextField sizeField = new TextField();
        sizeField.setPromptText("Размер");

        GridPane grid = createGrid();
        grid.addRow(0, new Label("Тип:*"), typeCombo);
        grid.addRow(1, new Label("Размер:"), sizeField);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        if (dialog.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            if (typeCombo.getValue() == null || typeCombo.getValue().trim().isEmpty()) {
                showError("Введите тип изделия!");
                return;
            }
            Item item = new Item(currentOrder, typeCombo.getValue().trim(),
                sizeField.getText().trim().isEmpty() ? null : sizeField.getText().trim());
            currentOrder.getItems().add(item);
            TableViewItems.getItems().add(item);
            updateTotalCost();
        }
    }

    @FXML
    void BtnRemoveItemOnAction() {
        if (selectedItem == null) { showError("Выберите изделие"); return; }
        currentOrder.getItems().remove(selectedItem);
        TableViewItems.getItems().remove(selectedItem);
        selectedItem = null;
        updateTotalCost();
    }

    @FXML
    void BtnAddDefectOnAction() {
        if (selectedItem == null) { showError("Сначала выберите изделие!"); return; }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Добавить дефект");

        ComboBox<String> typeCombo = new ComboBox<>(FXCollections.observableArrayList(defectDao.findDistinctTypes()));
        typeCombo.setEditable(true);
        typeCombo.setPromptText("Тип дефекта");
        TextArea descField = new TextArea();
        descField.setPromptText("Описание");
        descField.setPrefRowCount(2);
        Spinner<Integer> severitySpinner = new Spinner<>(1, 5, 3);

        GridPane grid = createGrid();
        grid.addRow(0, new Label("Тип:*"), typeCombo);
        grid.addRow(1, new Label("Описание:"), descField);
        grid.addRow(2, new Label("Серьёзность:*"), severitySpinner);
        grid.addRow(3, new Label(), new Label("(+20% за каждую единицу)"));
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        if (dialog.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            if (typeCombo.getValue() == null || typeCombo.getValue().trim().isEmpty()) {
                showError("Введите тип дефекта!");
                return;
            }
            Defect defect = new Defect(selectedItem, typeCombo.getValue().trim(),
                descField.getText().trim().isEmpty() ? null : descField.getText().trim(), severitySpinner.getValue());
            selectedItem.getDefects().add(defect);
            TableViewDefects.getItems().add(defect);
            updateTotalCost();
        }
    }

    @FXML
    void BtnRemoveDefectOnAction() {
        Defect selected = TableViewDefects.getSelectionModel().getSelectedItem();
        if (selected == null) { showError("Выберите дефект"); return; }
        selectedItem.getDefects().remove(selected);
        TableViewDefects.getItems().remove(selected);
        updateTotalCost();
    }

    private GridPane createGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 10, 10, 10));
        return grid;
    }

    private OrderStatus getOrCreateStatus(String name) {
        OrderStatus status = statusDao.findByName(name);
        if (status == null) { status = new OrderStatus(name); statusDao.save(status); }
        return status;
    }

    private void showError(String msg) { new Alert(Alert.AlertType.ERROR, msg).showAndWait(); }
    private void showInfo(String msg) { new Alert(Alert.AlertType.INFORMATION, msg).showAndWait(); }
}
