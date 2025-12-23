module ru.coursework.clothingapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires jakarta.persistence;
    requires org.hibernate.orm.core;
    requires java.naming;
    requires java.desktop;
    requires org.hibernate.validator;
    requires org.postgresql.jdbc;
    requires itextpdf;
    opens ru.coursework.clothingapp.model to org.hibernate.orm.core, javafx.base;
    exports ru.coursework.clothingapp;
    exports ru.coursework.clothingapp.controller;
    exports ru.coursework.clothingapp.service;
    opens ru.coursework.clothingapp.controller to javafx.fxml;
    opens ru.coursework.clothingapp.util to org.hibernate.orm.core;
}