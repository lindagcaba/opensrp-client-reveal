package org.smartregister.reveal.contract;

import android.content.Context;
import android.support.annotation.StringRes;
import android.support.v4.util.Pair;

import com.mapbox.geojson.Geometry;
import com.mapbox.mapboxsdk.geometry.LatLng;

import org.json.JSONObject;
import org.smartregister.domain.Campaign;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by samuelgithengi on 11/27/18.
 */
public interface ListTaskContract {

    interface ListTaskView {

        void showProgressDialog();

        void hideProgressDialog();

        Context getContext();

        void showOperationalAreaSelector(Pair<String, ArrayList<String>> locationHierarchy);

        void setCampaign(String campaign);

        void setOperationalArea(String operationalArea);

        void setDistrict(String district);

        void setFacility(String facility);

        void setOperator();

        void showCampaignSelector(List<String> campaigns, String entireTreeString);

        void setGeoJsonSource(String structuresGeoJson, Geometry operationalAreaGeometry);

        void lockNavigationDrawerForSelection();

        void unlockNavigationDrawer();

        void displayNotification(@StringRes int message);
    }

    interface PresenterCallBack {

        void onCampaignsFetched(List<Campaign> campaigns);

        void onStructuresFetched(JSONObject structuresGeoJson, Geometry operationalAreaGeometry);
    }
}
