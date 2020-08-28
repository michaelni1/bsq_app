package com.bsq.bsquared;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;

import java.util.ArrayList;

public class LocAutoSuggestAdapter extends ArrayAdapter implements Filterable {
    ArrayList<String> results = new ArrayList<String>();
    int resource;
    Context context;

    public LocAutoSuggestAdapter(Context context, int resource_id) {
        super(context, resource_id);
        this.context = context;
        this.resource = resource_id;
    }

    @Override
    public String getItem(int pos) {
        return results.get(pos);
    }

    @Override
    public int getCount() {
        return results.size();
    }

    @Override
    public Filter getFilter() {
        Filter filter = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                FilterResults filterResults = new FilterResults();
                ArrayList<String> queryResults;

                if (charSequence != null && charSequence.length() > 0) {
                    queryResults = GeoLocApi.autoComplete(charSequence.toString(), context);
                }
                else {
                    queryResults = new ArrayList<String>();
                }

                filterResults.values = queryResults;
                filterResults.count = queryResults.size();

                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                results = (ArrayList<String>) filterResults.values;

                if (filterResults.count > 0) {
                    notifyDataSetChanged();
                }
                else {
                    notifyDataSetInvalidated();
                }
            }
        };
        return filter;
    }
}
