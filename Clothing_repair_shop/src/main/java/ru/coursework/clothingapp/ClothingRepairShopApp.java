package ru.coursework.clothingapp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ru.coursework.clothingapp.util.HibernateSessionFactoryUtil;

import java.io.IOException;

public class ClothingRepairShopApp extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(ClothingRepairShopApp.class.getResource("orders-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1200, 700);
        stage.setTitle("Мастерская по ремонту одежды");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        HibernateSessionFactoryUtil.shutdown();
    }

    public static void main(String[] args) {
        launch();
    }
}
