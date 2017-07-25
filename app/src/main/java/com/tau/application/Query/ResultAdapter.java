package com.tau.application.Query;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.tau.application.DoctorMain;
import com.tau.application.R;
import com.tau.application.Utils.Utils;

public class ResultAdapter extends ArrayAdapter<String> {

    public ResultAdapter(Context context, String[] diseases) {
        super(context, R.layout.custom_row_diseases , diseases);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater tauInflater = LayoutInflater.from(getContext());
        View customView = tauInflater.inflate(R.layout.custom_row_diseases, parent, false);

        try{
            final String disease = getItem(position);
            final String score = getItem(position+1);
            TextView diseaseText = (TextView) customView.findViewById(R.id.disease_text_list);
            TextView diseaseScore = (TextView) customView.findViewById(R.id.disease_score_list);

            //even
            if((position & 0x01) == 0){
                diseaseText.setText(disease);
                diseaseScore.setText(score);
            }
        }catch (Exception e){}
        return customView;
    }
}