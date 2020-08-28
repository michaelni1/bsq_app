
package com.bsq.bsquared;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class GeoLocApi {
    public static ArrayList<String> autoComplete(String input, Context context) {
        ArrayList<String> arrayList = new ArrayList<String>();
        HttpURLConnection connection = null;
        StringBuilder jsonResult = new StringBuilder();

        //get saved latitude
        SharedPreferences get_pref = context.getSharedPreferences("latitude", Context.MODE_PRIVATE);
        String latitude = get_pref.getString("latitude", "");

        //get saved longitude
        SharedPreferences get_pref2 = context.getSharedPreferences("longitude", Context.MODE_PRIVATE);
        String longitude = get_pref2.getString("longitude", "");

        try {
            StringBuilder stringBuilder = new StringBuilder("https://autosuggest.search.hereapi.com/v1/autosuggest?limit=5&");

            stringBuilder.append("at=").append(latitude).append(",").append(longitude);
            stringBuilder.append("&q=").append(input);
            stringBuilder.append("&apiKey=mqC4KyqcYFR0cWR0EUshppJeeZjKkayqaCdaK1vZrUI");

            String url_string = stringBuilder.toString().replace(" ", "%20");
            URL url = new URL(url_string);

            connection = (HttpURLConnection) url.openConnection();
            InputStreamReader inputStreamReader = new InputStreamReader(connection.getInputStream());

            int read;
            char[] buff = new char[1024];

            while ((read = inputStreamReader.read(buff)) != -1) {
                jsonResult.append(buff, 0, read);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        try {
            JSONObject jsonObject = new JSONObject(jsonResult.toString());
            JSONArray items = jsonObject.getJSONArray("items");

            for (int i = 0; i < items.length(); ++i) {
                String result_type = items.getJSONObject(i).getString("resultType");
                if (result_type.equals("place") || result_type.equals("houseNumber")) {
                    if (items.getJSONObject(i).has("address")) {
                        String address = items.getJSONObject(i).getJSONObject("address").getString("label");
                        arrayList.add(address);
                    }
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return arrayList;
    }
}