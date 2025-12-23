package ru.coursework.clothingapp.repository;

import ru.coursework.clothingapp.model.Client;

public class ClientDao extends BaseDao<Client> {
    public ClientDao() {
        super(Client.class);
    }
}
