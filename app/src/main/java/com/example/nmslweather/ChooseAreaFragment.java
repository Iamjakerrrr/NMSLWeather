package com.example.nmslweather;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.nmslweather.db.City;
import com.example.nmslweather.db.County;
import com.example.nmslweather.db.Province;
import com.example.nmslweather.util.HttpUtil;
import com.example.nmslweather.util.Utility;

import org.litepal.LitePal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ChooseAreaFragment extends Fragment {
    private static final int LEVEL_PROVINCE = 0;
    private static final int LEVEL_CITY = 1;
    private static final int LEVEL_COUNTY = 2;

    private Button backButton;
    private TextView titleText;
    private ListView listView;

    private ArrayAdapter<String> adapter;

    private List<String> dataList = new ArrayList<>();
    private List<Province> provinceList = new ArrayList<>();
    private List<City> cityList = new ArrayList<>();
    private List<County> countyList = new ArrayList<>();
    private RelativeLayout progressBarLayout;

    private int currentLevel;
    private Province selectedProvince;
    private City selectedCity;

    private static Resources resources = MyApplication.getContext().getResources();
    private static String STRING_PROVINCE = resources.getString(R.string.province);
    private static String STRING_CITY = resources.getString(R.string.city);
    private static String STRING_COUNTY = resources.getString(R.string.county);
    private static String STRING_URL_OF_CHINA = resources.getString(R.string.url_of_china);
    private static String STRING_FAILED_TO_LOAD = resources.getString(R.string.failed_to_load);

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_area, container, false);
        backButton = view.findViewById(R.id.back_button);
        titleText = view.findViewById(R.id.title_text);
        listView = view.findViewById(R.id.list_view);
        adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_list_item_1, dataList);
        listView.setAdapter(adapter);
        progressBarLayout = view.findViewById(R.id.progress_bar_layout);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (currentLevel == LEVEL_PROVINCE) {
                selectedProvince = provinceList.get(position);
                queryCities();
            } else if (currentLevel == LEVEL_CITY) {
                selectedCity = cityList.get(position);
                queryCounties();
            } else if (currentLevel == LEVEL_COUNTY) {
                String weatherId = countyList.get(position).getWeatherId();
                Intent intent = new Intent(getActivity(), WeatherActivity.class);
                intent.putExtra("weather_id", weatherId);
                startActivity(intent);
                getActivity().finish();
            }
        });

        backButton.setOnClickListener(v -> {
            if (currentLevel == LEVEL_COUNTY) {
                queryCities();
            } else if (currentLevel == LEVEL_CITY) {
                queryProvinces();
            }
        });
        queryProvinces();
    }

    private void queryProvinces() {
        titleText.setText("中国");
        backButton.setVisibility(View.GONE);
        provinceList = LitePal.findAll(Province.class);
        if (provinceList.size() > 0) {
            dataList.clear();
            for (Province p : provinceList) {
                dataList.add(p.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_PROVINCE;
        } else {
            String address = STRING_URL_OF_CHINA;
            queryFromServer(address, STRING_PROVINCE);
        }
    }

    private void queryCities() {
        titleText.setText(selectedProvince.getProvinceName());
        backButton.setVisibility(View.VISIBLE);
        cityList = LitePal.where("provinceId = ?", String.valueOf(
                selectedProvince.getId())).find(City.class);
        if (cityList.size() > 0) {
            dataList.clear();
            for (City c : cityList) {
                dataList.add(c.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_CITY;
        } else {
            String address = STRING_URL_OF_CHINA +
                    selectedProvince.getProvinceCode();
            queryFromServer(address, STRING_CITY);
        }
    }

    private void queryCounties() {
        titleText.setText(selectedCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        countyList = LitePal.where("cityId = ?", String.valueOf(
                selectedCity.getId())).find(County.class);
        if (countyList.size() > 0) {
            dataList.clear();
            for (County c : countyList) {
                dataList.add(c.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_COUNTY;
        } else {
            String address = STRING_URL_OF_CHINA + selectedProvince.getProvinceCode()
                    + "/" + selectedCity.getCityCode();
            queryFromServer(address, STRING_COUNTY);
        }
    }

    private void queryFromServer(String address, final String type) {
        progressBarLayout.setVisibility(View.VISIBLE);
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                getActivity().runOnUiThread(() -> {
                    progressBarLayout.setVisibility(View.INVISIBLE);
                    Toast.makeText(getContext(), STRING_FAILED_TO_LOAD, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText = response.body().string();
                boolean result = false;
                if (type.equals(STRING_PROVINCE)) {
                    result = Utility.handleProvinceResponse(responseText);
                } else if (type.equals(STRING_CITY)) {
                    result = Utility.handleCityResponse(responseText, selectedProvince.getId());
                } else if (type.equals(STRING_COUNTY)) {
                    result = Utility.handleCountyResponse(responseText, selectedCity.getId());
                }

                if (result) {
                    getActivity().runOnUiThread(() -> {
                        progressBarLayout.setVisibility(View.INVISIBLE);
                        if (type.equals(STRING_PROVINCE)) {
                            queryProvinces();
                        } else if (type.equals(STRING_CITY)) {
                            queryCities();
                        } else if (type.equals(STRING_COUNTY)) {
                            queryCounties();
                        }
                    });
                }
            }
        });
    }
}
