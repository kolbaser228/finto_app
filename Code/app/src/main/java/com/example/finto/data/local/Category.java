package com.example.finto.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "categories")
public class Category {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public String name;
    public String icon;

    public Category(String name, String icon) {
        this.name = name;
        this.icon = icon;
    }
}