package com.example.philippinecurrencydetector;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.philippinecurrencydetector.database.AppDatabase;
import com.example.philippinecurrencydetector.database.dao.CurrencyDao;
import com.example.philippinecurrencydetector.database.model.CurrencyModel;

import java.io.IOException;
import java.util.List;


public class CameraActivityViewModel extends AndroidViewModel {

    private final CurrencyDao currencyDao;
    private final MutableLiveData<List<CurrencyModel>> mutLiveDataMasterItems = new MutableLiveData<>();


    public CameraActivityViewModel(Application application) throws IOException {
        super(application);
        currencyDao = AppDatabase.getInstance(application).currencyDao();
    }

    public void insertItem(CurrencyModel currencyModel) throws Exception{

        try {
            currencyDao.insertCurrency(currencyModel);
        }catch (Exception e){
            Log.e("TAG",e.getLocalizedMessage());

        }
    }

    public LiveData<List<CurrencyModel>> getMasterItem() {
        return mutLiveDataMasterItems;
    }


    public void loadCurrencyItem() {
        currencyDao.getCurrencyItem().observeForever(mutLiveDataMasterItems::postValue);
    }

}
