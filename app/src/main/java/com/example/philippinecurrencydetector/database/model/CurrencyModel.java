package com.example.philippinecurrencydetector.database.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "CurrencyTable")
public class CurrencyModel {

    @ColumnInfo(name = "ID")
    @PrimaryKey(autoGenerate = true)
    private int id = 0;

    @ColumnInfo(name = "CURRENCY")
    private String currency;

    @ColumnInfo(name = "DATE")
    private String date;



    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @NonNull
    public String getCurrency() {
        return currency;
    }

    public void setCurrency(@NonNull String currency) {
        this.currency = currency;
    }

    @NonNull
    public String getDate() {
        return date;
    }

    public void setDate(@NonNull String date) {
        this.date = date;
    }
}
