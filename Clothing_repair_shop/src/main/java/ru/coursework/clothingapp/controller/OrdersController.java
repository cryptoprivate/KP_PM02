package ru.coursework.clothingapp.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import ru.coursework.clothingapp.ClothingRepairShopApp;
import ru.coursework.clothingapp.model.Order;
import ru.coursework.clothingapp.model.OrderStatus;
import ru.coursework.clothingapp.model.Repair;
import ru.coursework.clothingapp.repository.OrderDao;
import ru.coursework.clothingapp.repository.OrderStatusDao;
import ru.coursework.clothingapp.repository.RepairDao;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

import ru.coursework.clothingapp.service.ReportService;

public class OrdersController implements Initializable {

    private final OrderDao orderDao = new OrderDao();
    private final OrderStatusDao orderStatusDao = new OrderStatusDao();
    private final RepairDao repairDao = new RepairDao();
    private final ReportService reportService = new ReportService();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @FXML private TabPane mainTabPane;
    @FXML private DatePicker DatePickerStart, DatePickerEnd;
    @FXML private ComboBox<OrderStatus> ComboBoxStatus;
    @FXML private TextField TextFieldOrderSearch;
    @FXML private TableView<Order> TableViewOrders;
    @FXML private TableColumn<Order, String> TableColumnId, TableColumnOrderNumber, TableColumnClient;
    @FXML private TableColumn<Order, String> TableColumnStatus, TableColumnMaster, TableColumnAcceptDate;
    @FXML private TableColumn<Order, String> TableColumnDueDate, TableColumnTotalCost;
    @FXML private Label LabelRecordCount;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupTable();
        loadStatuses();
        filterData();
        setupListeners();

        if (mainTabPane != null) {
            mainTabPane.getSelectionModel().selectedItemProperty().addListener((_, _, tab) -> {
                if (tab != null && "Заказы".equals(tab.getText())) { loadStatuses(); filterData(); }
            });
        }
    }

    private void setupTable() {
        TableColumnId.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getId())));
        TableColumnOrderNumber.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getOrderNumber()));
        TableColumnClient.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getClient().toString()));
        TableColumnStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus().getName()));
        TableColumnMaster.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getMaster() != null ? c.getValue().getMaster().toString() : "—"));
        TableColumnAcceptDate.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAcceptDate().format(DATE_FORMAT)));
        TableColumnDueDate.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDueDate().format(DATE_FORMAT)));
        TableColumnTotalCost.setCellValueFactory(c -> {
            BigDecimal total = c.getValue().getTotalCost();
            return new SimpleStringProperty(total.compareTo(BigDecimal.ZERO) > 0 ? String.format("%.2f ₽", total) : "—");
        });

        TableViewOrders.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                Order sel = TableViewOrders.getSelectionModel().getSelectedItem();
                if (sel != null) openDialog("order-edit-view.fxml", "Редактировать заказ", sel);
            }
        });
    }

    private void setupListeners() {
        ComboBoxStatus.getSelectionModel().selectedItemProperty().addListener((_, _, _) -> filterData());
        DatePickerStart.valueProperty().addListener((_, _, _) -> filterData());
        DatePickerEnd.valueProperty().addListener((_, _, _) -> filterData());
        TextFieldOrderSearch.textProperty().addListener((_, _, _) -> filterData());
    }

    @FXML void BtnCreateOnAction() { openDialog("order-edit-view.fxml", "Новый заказ", null); }

    @FXML
    void BtnDeleteOnAction() {
        Order order = getSelectedOrder("удаления");
        if (order == null) return;
        if (confirmAction("Удаление заказа", "Удалить запись?")) {
            orderDao.delete(order);
            filterData();
            showInfo("Заказ удалён");
        }
    }

    @FXML
    void BtnExecuteOnAction() {
        Order order = getSelectedOrder("исполнения");
        if (order != null) openDialog("execution-view.fxml", "Исполнение заказа №" + order.getOrderNumber(), order);
    }

    @FXML
    void BtnIssueOnAction() {
        Order order = getSelectedOrder("выдачи");
        if (order == null) return;

        try {
            Order fresh = orderDao.findOne(order.getId());
            if (!"Готов".equals(fresh.getStatus().getName())) {
                showError("Заказ должен быть в статусе 'Готов'! Текущий: " + fresh.getStatus().getName());
                return;
            }
            List<Repair> repairs = fresh.getItems().stream().flatMap(i -> repairDao.findByItem(i).stream()).toList();
            if (repairs.isEmpty()) { showError("Нет операций ремонта!"); return; }
            long uncompleted = repairs.stream().filter(r -> r.getCompletedAt() == null).count();
            if (uncompleted > 0) { showError("Не все операции выполнены! " + uncompleted + "/" + repairs.size()); return; }

            if (confirmAction("Выдача заказа", String.format("Выдать заказ?\n\nСумма: %.2f ₽", fresh.getTotalCost()))) {
                fresh.setStatus(getOrCreateStatus("Выдан"));
                orderDao.update(fresh);
                filterData();
                showInfo(String.format("Заказ выдан!\nОплачено: %.2f ₽", fresh.getTotalCost()));
            }
        } catch (Exception e) {
            showError("Ошибка: " + e.getMessage());
        }
    }

    @FXML
    void BtnResetOnAction() {
        DatePickerStart.setValue(null);
        DatePickerEnd.setValue(null);
        ComboBoxStatus.getSelectionModel().clearSelection();
        TextFieldOrderSearch.clear();
        filterData();
    }

    @FXML
    void BtnReportOnAction() {
        List<Order> orders = TableViewOrders.getItems();
        if (orders.isEmpty()) { showError("Нет заказов для отчёта"); return; }
        try {
            String path = reportService.generateOrdersReport(orders, "Отчёт по заказам");
            showInfo("Отчёт сохранён: " + path);
            Desktop.getDesktop().open(new File(path));
        } catch (Exception e) {
            showError("Ошибка: " + e.getMessage());
        }
    }

    void filterData() {
        List<Order> orders = orderDao.findAll();
        int totalCount = orders.size();
        LocalDate start = DatePickerStart.getValue(), end = DatePickerEnd.getValue();
        OrderStatus status = ComboBoxStatus.getSelectionModel().getSelectedItem();
        String query = TextFieldOrderSearch.getText().trim().toLowerCase();

        if (start != null) orders = orders.stream().filter(o -> !o.getAcceptDate().isBefore(start)).toList();
        if (end != null) orders = orders.stream().filter(o -> !o.getAcceptDate().isAfter(end)).toList();
        if (status != null) orders = orders.stream().filter(o -> o.getStatus().getId().equals(status.getId())).toList();
        if (!query.isEmpty()) {
            orders = orders.stream().filter(o -> matchesSearch(o, query)).toList();
        }
        TableViewOrders.getItems().setAll(orders);
        updateRecordCount(orders.size(), totalCount);
    }

    private boolean matchesSearch(Order o, String q) {
        String data = String.join(" ",
            String.valueOf(o.getId()), o.getOrderNumber(),
            o.getClient().toString(), o.getStatus().getName(),
            o.getMaster() != null ? o.getMaster().toString() : "",
            o.getAcceptDate().format(DATE_FORMAT), o.getDueDate().format(DATE_FORMAT),
            String.format("%.2f", o.getTotalCost())
        ).toLowerCase();
        return data.contains(q);
    }

    private void loadStatuses() {
        ComboBoxStatus.getItems().setAll(orderStatusDao.findAllUnique());
    }

    private Order getSelectedOrder(String action) {
        Order order = TableViewOrders.getSelectionModel().getSelectedItem();
        if (order == null) showError("Выберите заказ для " + action);
        return order;
    }

    private void openDialog(String fxml, String title, Order order) {
        try {
            FXMLLoader loader = new FXMLLoader(ClothingRepairShopApp.class.getResource(fxml));
            Stage stage = new Stage();
            stage.setTitle(title);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setScene(new Scene(loader.load()));

            if (fxml.contains("order-edit")) {
                stage.setWidth(1000); stage.setHeight(750);
                OrderEditController ctrl = loader.getController();
                if (order != null) ctrl.setOrder(order); else ctrl.initNewOrder();
            } else if (fxml.contains("execution")) {
                stage.setWidth(1100); stage.setHeight(700);
                ((ExecutionController) loader.getController()).setOrder(order);
            }
            stage.showAndWait();
            filterData();
        } catch (IOException e) {
            showError("Ошибка: " + e.getMessage());
        }
    }

    private OrderStatus getOrCreateStatus(String name) {
        OrderStatus status = orderStatusDao.findByName(name);
        if (status == null) { status = new OrderStatus(name); orderStatusDao.save(status); }
        return status;
    }

    private boolean confirmAction(String header, String msg) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, msg);
        confirm.setHeaderText(header);
        ButtonType yes = new ButtonType("Да", ButtonBar.ButtonData.OK_DONE);
        ButtonType no = new ButtonType("Нет", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(yes, no);
        return confirm.showAndWait().orElse(no) == yes;
    }

    private void showError(String msg) { new Alert(Alert.AlertType.ERROR, msg).showAndWait(); }
    private void showInfo(String msg) { new Alert(Alert.AlertType.INFORMATION, msg).showAndWait(); }
    private void updateRecordCount(int shown, int total) {
        if (LabelRecordCount != null) LabelRecordCount.setText("Записей найдено: " + shown + " из " + total);
    }
}
