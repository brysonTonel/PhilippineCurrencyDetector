package com.example.philippinecurrencydetector;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import java.io.IOException;

class ModelFactory extends ViewModelProvider.NewInstanceFactory {

    @NonNull
    private final Application application;
    
    public ModelFactory(@NonNull Application application) {
        this.application = application;

    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(CameraActivityViewModel.class)) {
            try {
                return (T) new CameraActivityViewModel(application);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}