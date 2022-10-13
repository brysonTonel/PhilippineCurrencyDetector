package com.example.philippinecurrencydetector.database.repositories;

import com.example.philippinecurrencydetector.database.model.CurrencyModel;

public interface ICurrencyRepo {
    void insertCurrency(CurrencyModel currencyModel);

}
