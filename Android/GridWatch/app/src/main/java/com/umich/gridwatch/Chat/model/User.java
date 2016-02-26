package com.umich.gridwatch.Chat.model;

import java.io.Serializable;

/**
 * Created by Lincoln on 07/01/16.
 */
public class User implements Serializable {
    String id, name, email, country, city,state;

    public User() {
    }

    public User(String id, String name, String email, String country, String city, String state) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.country = country;
        this.city = city;
        this.state = state;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
