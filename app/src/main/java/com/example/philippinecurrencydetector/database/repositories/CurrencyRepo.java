package com.example.philippinecurrencydetector.database.repositories;

import android.app.Application;

import com.example.philippinecurrencydetector.database.AppDatabase;
import com.example.philippinecurrencydetector.database.dao.CurrencyDao;
import com.example.philippinecurrencydetector.database.model.CurrencyModel;

import java.io.IOException;

public class CurrencyRepo implements ICurrencyRepo{
    private final CurrencyDao currencyDao;

    public CurrencyRepo(Application application) throws IOException {
        currencyDao = AppDatabase.getInstance(application).currencyDao();
    }

    @Override
    public void insertCurrency(CurrencyModel currencyModel) {
        currencyDao.insertCurrency(currencyModel);
    }
}
