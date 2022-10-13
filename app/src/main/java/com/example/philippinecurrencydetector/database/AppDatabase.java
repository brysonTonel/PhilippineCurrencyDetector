package com.example.philippinecurrencydetector.database;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.philippinecurrencydetector.BuildConfig;
import com.example.philippinecurrencydetector.database.dao.CurrencyDao;
import com.example.philippinecurrencydetector.database.model.CurrencyModel;

import java.io.File;
import java.io.IOException;

@Database(
        entities = {
                CurrencyModel.class
        }, version = 1)


public abstract class AppDatabase extends RoomDatabase {

    public static final String DATABASE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath();

    public static final String DATA_PATH = "/data/" + "currency" + "/";
    public static AppDatabase instance;

    public abstract CurrencyDao currencyDao();

    public static synchronized AppDatabase getInstance(final Context context)  {


        if (instance == null) {
            File folder = new File(DATABASE_PATH + DATA_PATH);
            if (!folder.exists()) {
                Log.e("NOT EXIST", "NOT EXIST");
                folder.mkdirs();
            }
            instance = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, DATABASE_PATH + DATA_PATH + "data.db")
                    .allowMainThreadQueries()
                    .build();

        }

        return instance;


    }
}
