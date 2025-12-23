package ru.coursework.clothingapp.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.geometry.Insets;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import ru.coursework.clothingapp.model.*;
import ru.coursework.clothingapp.repository.*;
import ru.coursework.clothingapp.util.PriceCalculator;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class ExecutionController implements Initializable {

    private final MasterDao masterDao = new MasterDao();
    private final RepairDao repairDao = new RepairDao();
    private final OrderDao orderDao = new OrderDao();
    private final OrderStatusDao orderStatusDao = new OrderStatusDao();
    private Order currentOrder;

    @FXML private Label LabelOrderInfo;
    @FXML private Label LabelTotalCost;
    @FXML private ComboBox<Master> ComboBoxMaster;
    @FXML private TableView<Repair> TableViewRepairs;
    @FXML private TableColumn<Repair, String> TableColumnItem;
    @FXML private TableColumn<Repair, String> TableColumnDefect;
    @FXML private TableColumn<Repair, String> TableColumnOperation;
    @FXML private TableColumn<Repair, String> TableColumnPrice;
    @FXML private TableColumn<Repair, String> TableColumnPhotoBefore;
    @FXML private TableColumn<Repair, String> TableColumnPhotoAfter;
    @FXML private TableColumn<Repair, String> TableColumnStatus;
    @FXML private Button BtnComplete;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        ComboBoxMaster.setItems(FXCollections.observableArrayList(masterDao.findAll()));
        TableColumnItem.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getItem().getType()));
        TableColumnDefect.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDefect().getDefectType()));
        TableColumnOperation.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getOperationName()));
        TableColumnPrice.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPrice() + " ₽"));
        TableColumnPhotoBefore.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPhotoBefore() != null ? "✓" : "—"));
        TableColumnPhotoAfter.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPhotoAfter() != null ? "✓" : "—"));
        TableColumnStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isCompleted() ? "✓ Выполнено" : "⏳ В работе"));
    }

    public void setOrder(Order order) {
        this.currentOrder = order;
        LabelOrderInfo.setText(String.format("Заказ №%s | Клиент: %s | Изделий: %d",
            order.getOrderNumber(), order.getClient(), order.getItems().size()));
        TableViewRepairs.getItems().clear();
        order.getItems().forEach(item -> TableViewRepairs.getItems().addAll(repairDao.findByItem(item)));
        if (order.getMaster() != null) ComboBoxMaster.setValue(order.getMaster());
        updateTotalCost();
    }

    private void updateTotalCost() {
        if (currentOrder != null && LabelTotalCost != null) {
            LabelTotalCost.setText(String.format("%.2f ₽", currentOrder.getTotalCost()));
        }
    }

    @FXML
    void BtnAddRepairOnAction() {
        if (currentOrder.getItems().isEmpty()) {
            showError("В заказе нет изделий!");
            return;
        }
        createRepairDialog().showAndWait().ifPresent(repair -> {
            repair.getItem().getRepairs().add(repair);
            if (TableViewRepairs.getItems().isEmpty() && "Принят".equals(currentOrder.getStatus().getName())) {
                currentOrder.setStatus(getOrCreateStatus("В работе"));
            }
            orderDao.update(currentOrder);
            setOrder(orderDao.findOne(currentOrder.getId()));
            showInfo("Добавлена: " + repair.getOperationName() + " (" + repair.getPrice() + " ₽)");
        });
    }

    private Dialog<Repair> createRepairDialog() {
        Dialog<Repair> dialog = new Dialog<>();
        dialog.setTitle("Добавить операцию ремонта");

        ComboBox<Item> itemCombo = new ComboBox<>(FXCollections.observableArrayList(currentOrder.getItems()));
        ComboBox<Defect> defectCombo = new ComboBox<>();
        ComboBox<String> operationCombo = new ComboBox<>(FXCollections.observableArrayList(getOperationsList()));
        operationCombo.setEditable(true);
        TextField materialField = new TextField();
        materialField.setPromptText("Материал");
        Label priceLabel = new Label("—");
        priceLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: green;");

        if (!currentOrder.getItems().isEmpty()) {
            itemCombo.setValue(currentOrder.getItems().get(0));
            updateDefects(defectCombo, currentOrder.getItems().get(0));
        }

        itemCombo.setOnAction(e -> updateDefects(defectCombo, itemCombo.getValue()));
        Runnable calcPrice = () -> {
            if (operationCombo.getValue() != null && defectCombo.getValue() != null) {
                priceLabel.setText(PriceCalculator.calculateOperationCost(operationCombo.getValue(), defectCombo.getValue()) + " ₽");
            }
        };
        operationCombo.setOnAction(e -> calcPrice.run());
        defectCombo.setOnAction(e -> calcPrice.run());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 10, 10, 10));
        grid.addRow(0, new Label("Изделие:"), itemCombo);
        grid.addRow(1, new Label("Дефект:"), defectCombo);
        grid.addRow(2, new Label("Операция:"), operationCombo);
        grid.addRow(3, new Label("Материал:"), materialField);
        grid.addRow(4, new Label("Цена:"), priceLabel);

        dialog.getDialogPane().setContent(grid);
        ButtonType btnOk = new ButtonType("Добавить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnOk, new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE));
        calcPrice.run();

        dialog.setResultConverter(btn -> {
            if (btn == btnOk && itemCombo.getValue() != null && defectCombo.getValue() != null &&
                operationCombo.getValue() != null && !operationCombo.getValue().trim().isEmpty()) {
                Master master = ComboBoxMaster.getValue() != null ? ComboBoxMaster.getValue() : masterDao.findAll().get(0);
                BigDecimal price = PriceCalculator.calculateOperationCost(operationCombo.getValue(), defectCombo.getValue());
                String material = materialField.getText().trim().isEmpty() ? null : materialField.getText().trim();
                return new Repair(itemCombo.getValue(), defectCombo.getValue(), operationCombo.getValue().trim(), master, material, price);
            }
            return null;
        });
        return dialog;
    }

    private void updateDefects(ComboBox<Defect> combo, Item item) {
        if (item != null) {
            combo.setItems(FXCollections.observableArrayList(item.getDefects()));
            if (!item.getDefects().isEmpty()) combo.setValue(item.getDefects().get(0));
        }
    }

    private List<String> getOperationsList() {
        List<String> ops = new ArrayList<>(List.of("Зашить дыру", "Восстановить подкладку", "Заменить молнию", "Перешить шов", "Пришить пуговицу", "Удалить пятно"));
        repairDao.findDistinctOperations().forEach(op -> { if (!ops.contains(op)) ops.add(op); });
        return ops;
    }

    @FXML
    void BtnMarkCompletedOnAction() {
        if (currentOrder.getMaster() == null && ComboBoxMaster.getValue() == null) {
            showError("Назначьте мастера на заказ!");
            return;
        }
        if (ComboBoxMaster.getValue() != null) {
            currentOrder.setMaster(ComboBoxMaster.getValue());
            orderDao.update(currentOrder);
        }
        Repair selected = TableViewRepairs.getSelectionModel().getSelectedItem();
        if (selected == null) { showError("Выберите операцию!"); return; }
        if (selected.isCompleted()) { showError("Операция уже выполнена!"); return; }
        selected.setCompletedAt(LocalDate.now());
        repairDao.update(selected);
        setOrder(orderDao.findOne(currentOrder.getId()));
        showInfo("Операция выполнена");
    }

    @FXML
    void BtnCompleteOnAction() {
        long total = TableViewRepairs.getItems().size();
        long completed = TableViewRepairs.getItems().stream().filter(Repair::isCompleted).count();
        if (total == 0) { showError("У заказа нет операций ремонта!"); return; }
        if (completed < total) { showError("Не все операции выполнены! " + completed + "/" + total); return; }

        currentOrder.setStatus(getOrCreateStatus("Готов"));
        if (ComboBoxMaster.getValue() != null) currentOrder.setMaster(ComboBoxMaster.getValue());
        orderDao.update(currentOrder);
        showInfo("Заказ готов к выдаче!");
        ((Stage) BtnComplete.getScene().getWindow()).close();
    }

    @FXML void BtnUploadPhotoBeforeOnAction() { uploadPhoto(true); }
    @FXML void BtnUploadPhotoAfterOnAction() { uploadPhoto(false); }

    private void uploadPhoto(boolean isBefore) {
        Repair repair = TableViewRepairs.getSelectionModel().getSelectedItem();
        if (repair == null) { showError("Выберите операцию!"); return; }

        FileChooser fc = new FileChooser();
        fc.setTitle("Выберите фото " + (isBefore ? "ДО" : "ПОСЛЕ"));
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Изображения", "*.jpg", "*.jpeg", "*.png"));
        File file = fc.showOpenDialog(TableViewRepairs.getScene().getWindow());
        if (file == null) return;

        try {
            File dir = new File("photos");
            if (!dir.exists()) dir.mkdirs();
            String ext = file.getName().substring(file.getName().lastIndexOf('.'));
            String name = "repair_" + repair.getId() + "_" + (isBefore ? "before" : "after") + "_" + System.currentTimeMillis() + ext;
            Files.copy(file.toPath(), new File(dir, name).toPath(), StandardCopyOption.REPLACE_EXISTING);
            if (isBefore) repair.setPhotoBefore("photos/" + name);
            else repair.setPhotoAfter("photos/" + name);
            repairDao.update(repair);
            TableViewRepairs.refresh();
            showInfo("Фото загружено");
        } catch (Exception e) {
            showError("Ошибка: " + e.getMessage());
        }
    }

    private OrderStatus getOrCreateStatus(String name) {
        OrderStatus status = orderStatusDao.findByName(name);
        if (status == null) { status = new OrderStatus(name); orderStatusDao.save(status); }
        return status;
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).showAndWait();
    }

    private void showInfo(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg).showAndWait();
    }
}
