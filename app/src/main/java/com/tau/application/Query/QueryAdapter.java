package com.tau.application.Query;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
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

public class QueryAdapter extends ArrayAdapter<String> {

    Context mCTX;
    int LIMIT;

    public QueryAdapter(Context context, String[] genes, int limit) {
        super(context, R.layout.custom_row ,genes);
        mCTX = context;
        LIMIT = limit;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater tauInflater = LayoutInflater.from(getContext());
        View customView = tauInflater.inflate(R.layout.custom_row, parent, false);

        final String singleGeneItem = getItem(position);
        TextView tauText = (TextView) customView.findViewById(R.id.tauText);
        ImageView geneImage = (ImageView) customView.findViewById(R.id.geneImage);

        tauText.setText(singleGeneItem);
        geneImage.setImageResource(R.mipmap.gene);

        Button bt = (Button) customView.findViewById(R.id.button);
        bt.setText("More Information");
        bt.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v)
            {
                Utils.log("Running algorithm for gene " + singleGeneItem);
                DoctorMain.getInstance().runAlgorithm(mCTX, singleGeneItem, LIMIT);
            }
        });

        return customView;
    }
}