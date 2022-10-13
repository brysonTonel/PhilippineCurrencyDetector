package com.example.philippinecurrencydetector.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.philippinecurrencydetector.database.model.CurrencyModel;

import java.util.List;

@Dao
public interface CurrencyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertCurrency(CurrencyModel currencyModel);

    @Query("SELECT * FROM CURRENCYTABLE ORDER BY DATE DESC")
    LiveData<List<CurrencyModel>> getCurrencyItem();

}
