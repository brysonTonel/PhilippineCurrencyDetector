package com.example.philippinecurrencydetector;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.philippinecurrencydetector.database.model.CurrencyModel;

import java.util.List;

public class CurrencyAdapter extends RecyclerView.Adapter<CurrencyAdapter.ViewHolder>{

    private List<CurrencyModel> currencyModels;



    public void setCurrencyModelsItem(List<CurrencyModel> currencyModels) {
        this.currencyModels = currencyModels;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CurrencyAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.card_view_item_currency, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull CurrencyAdapter.ViewHolder holder, int position) {
        holder.bind(currencyModels.get(position));
    }

    @Override
    public int getItemCount() {
        if (currencyModels == null) {
            return 0;
        }
        return currencyModels.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private  TextView tvCurrency, tvDateTime;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDateTime = itemView.findViewById(R.id.tv_datetime);
            tvCurrency = itemView.findViewById(R.id.tv_currency);

        }

        public void bind(CurrencyModel currencyModel) {
            tvCurrency.setText(currencyModel.getCurrency());
            tvDateTime.setText(currencyModel.getDate());



        }
    }
}
