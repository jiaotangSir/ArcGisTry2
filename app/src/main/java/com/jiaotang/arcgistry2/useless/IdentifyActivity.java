package com.jiaotang.arcgistry2.useless;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.esri.android.action.IdentifyResultSpinner;
import com.esri.android.action.IdentifyResultSpinnerAdapter;
import com.esri.android.map.Callout;
import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISTiledMapServiceLayer;
import com.esri.android.map.event.OnSingleTapListener;
import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.Point;
import com.esri.core.tasks.identify.IdentifyParameters;
import com.esri.core.tasks.identify.IdentifyResult;
import com.esri.core.tasks.identify.IdentifyTask;
import com.jiaotang.arcgistry2.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class IdentifyActivity extends AppCompatActivity {

    // create ArcGIS objects
    MapView mMapView = null;
    IdentifyParameters params = null;

    // create UI objects
    static ProgressDialog dialog;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_identify);

        mMapView = (MapView) findViewById(R.id.map);//设置UI和代码绑定

        // add demographic layer to the map
        mMapView.addLayer(new ArcGISTiledMapServiceLayer("http://services.arcgisonline.com/arcgis/rest/services/Demographics/USA_Diversity_Index/MapServer"));

        // set Identify Parameters
        params = new IdentifyParameters();
        params.setTolerance(20);
        params.setDPI(98);
        params.setLayers(new int[] { 4 });
        params.setLayerMode(IdentifyParameters.ALL_LAYERS);

        // Identify on single tap of map
        mMapView.setOnSingleTapListener(new OnSingleTapListener() {

            private static final long serialVersionUID = 1L;

            @Override
            public void onSingleTap(final float x, final float y) {

                if (!mMapView.isLoaded()) {
                    return;
                }

                // Add to Identify Parameters based on tapped location
                Point identifyPoint = mMapView.toMapPoint(x, y);

                params.setGeometry(identifyPoint);
                params.setSpatialReference(mMapView.getSpatialReference());
                params.setMapHeight(mMapView.getHeight());
                params.setMapWidth(mMapView.getWidth());
                params.setReturnGeometry(false);

                // add the area of extent to identify parameters
                Envelope env = new Envelope();
                mMapView.getExtent().queryEnvelope(env);
                params.setMapExtent(env);

                // execute the identify task off UI thread
                MyIdentifyTask mTask = new MyIdentifyTask(identifyPoint);
                mTask.execute(params);
            }

        });

    }

    private ViewGroup createIdentifyContent(final List<IdentifyResult> results) {

        // create a new LinearLayout in application context
        LinearLayout layout = new LinearLayout(this);

        // view height and widthwrap content
        layout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        // default orientation
        layout.setOrientation(LinearLayout.HORIZONTAL);

        // Spinner to hold the results of an identify operation
        IdentifyResultSpinner spinner = new IdentifyResultSpinner(this, results);

        // make view clickable
        spinner.setClickable(false);
        spinner.canScrollHorizontally(BIND_ADJUST_WITH_ACTIVITY);

        // MyIdentifyAdapter creates a bridge between spinner and it's data
        MyIdentifyAdapter adapter = new MyIdentifyAdapter(this, results);
        spinner.setAdapter(adapter);
        spinner.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        layout.addView(spinner);

        return layout;
    }

    /**
     * This class allows the user to customize the string shown in the callout.
     * By default its the display field name.
     *
     * A spinner adapter defines two different views; one that shows the data in
     * the spinner itself and one that shows the data in the drop down list when
     * spinner is pressed.
     *
     */
    public class MyIdentifyAdapter extends IdentifyResultSpinnerAdapter {
        String m_show = null;
        List<IdentifyResult> resultList;
        int currentDataViewed = -1;
        Context m_context;

        public MyIdentifyAdapter(Context context, List<IdentifyResult> results) {
            super(context, results);
            this.resultList = results;
            this.m_context = context;
        }

        // Get a TextView that displays identify results in the callout.
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            String LSP = System.getProperty("line.separator");
            StringBuilder outputVal = new StringBuilder();

            // Resource Object to access the Resource fields
            Resources res = getResources();

            // Get Name attribute from identify results
            IdentifyResult curResult = this.resultList.get(position);

            if (curResult.getAttributes().containsKey(
                    res.getString(R.string.NAME))) {
                outputVal.append("Place: "
                        + curResult.getAttributes()
                        .get(res.getString(R.string.NAME)).toString());
                outputVal.append(LSP);
            }

            if (curResult.getAttributes().containsKey(
                    res.getString(R.string.ID))) {
                outputVal.append("State ID: "
                        + curResult.getAttributes()
                        .get(res.getString(R.string.ID)).toString());
                outputVal.append(LSP);
            }

            if (curResult.getAttributes().containsKey(
                    res.getString(R.string.ST_ABBREV))) {
                outputVal.append("Abbreviation: "
                        + curResult.getAttributes()
                        .get(res.getString(R.string.ST_ABBREV))
                        .toString());
                outputVal.append(LSP);
            }

            if (curResult.getAttributes().containsKey(
                    res.getString(R.string.TOTPOP_CY))) {
                outputVal.append("Population: "
                        + curResult.getAttributes()
                        .get(res.getString(R.string.TOTPOP_CY))
                        .toString());
                outputVal.append(LSP);

            }

            if (curResult.getAttributes().containsKey(
                    res.getString(R.string.LANDAREA))) {
                outputVal.append("Area: "
                        + curResult.getAttributes()
                        .get(res.getString(R.string.LANDAREA))
                        .toString());
                outputVal.append(LSP);

            }

            // Create a TextView to write identify results
            TextView txtView;
            txtView = new TextView(this.m_context);
            txtView.setText(outputVal);
            txtView.setTextColor(Color.BLACK);
            txtView.setLayoutParams(new ListView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            txtView.setGravity(Gravity.CENTER_VERTICAL);

            return txtView;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.unpause();
    }

    private class MyIdentifyTask extends
            AsyncTask<IdentifyParameters, Void, IdentifyResult[]> {

        IdentifyTask task = new IdentifyTask(getResources()
                .getString(R.string.identify_task_url_for_avghouseholdsize));

        IdentifyResult[] M_Result;

        Point mAnchor;

        MyIdentifyTask(Point anchorPoint) {
            mAnchor = anchorPoint;
        }

        @Override
        protected void onPreExecute() {
            // create dialog while working off UI thread
            dialog = ProgressDialog.show(IdentifyActivity.this, "Identify Task",
                    "Identify query ...");

        }

        protected IdentifyResult[] doInBackground(IdentifyParameters... params) {

            // check that you have the identify parameters
            if (params != null && params.length > 0) {
                IdentifyParameters mParams = params[0];

                try {
                    // Run IdentifyTask with Identify Parameters

                    M_Result = task.execute(mParams);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            return M_Result;
        }

        @Override
        protected void onPostExecute(IdentifyResult[] results) {

            // dismiss dialog
            if (dialog.isShowing()) {
                dialog.dismiss();
            }

            ArrayList<IdentifyResult> resultList = new ArrayList<IdentifyResult>();

            IdentifyResult result_1;

            for (int index = 0; index < results.length; index++) {

                result_1 = results[index];
                String displayFieldName = result_1.getDisplayFieldName();
                Map<String, Object> attr = result_1.getAttributes();
                for (String key : attr.keySet()) {
                    if (key.equalsIgnoreCase(displayFieldName)) {
                        resultList.add(result_1);
                    }
                }
            }

            Callout callout = mMapView.getCallout();
            callout.setContent(createIdentifyContent(resultList));
            callout.show(mAnchor);


        }
    }
}
