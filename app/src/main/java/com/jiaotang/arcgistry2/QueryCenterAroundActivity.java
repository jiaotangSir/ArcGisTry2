package com.jiaotang.arcgistry2;

import android.app.ProgressDialog;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.LocationDisplayManager;
import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISDynamicMapServiceLayer;
import com.esri.android.map.event.OnStatusChangedListener;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.map.Feature;
import com.esri.core.map.FeatureResult;
import com.esri.core.map.Graphic;
import com.esri.core.renderer.SimpleRenderer;
import com.esri.core.symbol.SimpleFillSymbol;
import com.esri.core.symbol.SimpleLineSymbol;
import com.esri.core.tasks.query.QueryParameters;
import com.esri.core.tasks.query.QueryTask;

import java.util.ArrayList;
import java.util.List;

public class QueryCenterAroundActivity extends AppCompatActivity {

    /**当坐标系是地理坐标系的时候，需要加偏移量；投影坐标系不需要偏移量*/
    public static double ONE_METER_OFFSET = 0.00000899322;//维度每米偏移量

    MapView mMapView;

    Polygon multipath;
    ArrayList<Point> mPoints = new ArrayList<Point>();

    private GraphicsLayer graphicsLayer;
    private String queryLayer;
    private ProgressDialog progress;

    private LocationDisplayManager locationDisplayManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_query_center_around);

        mMapView = (MapView)findViewById(R.id.around_map);

        String dynamicLayer = "http://119.80.161.7:6080/arcgis/rest/services/androidTest/MapServer";
        ArcGISDynamicMapServiceLayer agsDynlyr = new ArcGISDynamicMapServiceLayer(dynamicLayer);
        mMapView.addLayer(agsDynlyr);



//        /**设置中心点、在setOnStatusChangedListener（）中设置比例*/
//        Point p = new Point(89.24184041809562,42.96009782772187);
//        mMapView.centerAt(p,true);
//        mMapView.setOnStatusChangedListener(new OnStatusChangedListener() {
//            @Override
//            public void onStatusChanged(Object o, STATUS status) {
//                if (mMapView.isLoaded()) {
//                    mMapView.setScale(5645.827716557413);
//                }
//
//            }
//        });



        // get query service
//        queryLayer = "http://services.arcgisonline.com/ArcGIS/rest/services/Demographics/USA_Average_Household_Size/MapServer";
        queryLayer = "http://119.80.161.7:6080/arcgis/rest/services/androidTest_guanxian/MapServer/3";
        mMapView.setOnStatusChangedListener(new OnStatusChangedListener() {

            private static final long serialVersionUID = 1L;

            @Override
            public void onStatusChanged(Object source, STATUS status) {
                if (source == mMapView && status == STATUS.INITIALIZED) {
                    graphicsLayer = new GraphicsLayer();
                    SimpleRenderer sr = new SimpleRenderer(
                            new SimpleFillSymbol(Color.RED));
                    graphicsLayer.setRenderer(sr);
                    mMapView.addLayer(graphicsLayer);

                }
            }
        });

//        /**定位*/
//        locationDisplayManager =  mMapView.getLocationDisplayManager();//获取定位类
//        locationDisplayManager.setShowLocation(true);
//        locationDisplayManager.setAutoPanMode(LocationDisplayManager.AutoPanMode.LOCATION);//设置模式
//        locationDisplayManager.setShowPings(true);
//        locationDisplayManager.start();//开始定位



    }


    public void showAroundFeature(View view) {

        Point p = mMapView.getCenter();
        Log.d("data", "中心点的x=" + p.getX());
        Log.d("data", "中心点的y=" + p.getY());
        Log.d("data", "缩放大小：" + mMapView.getScale());

//        Point locationPoint = locationDisplayManager.getPoint();//获取定位点坐标

        Point locationPoint = new Point(89.24184041809562,42.96009782772187);

        Log.d("data", "定位点的x=" + locationPoint.getX() + "   y=" + locationPoint.getY());

//        /**自己加的*/
//        Point p1 = new Point(-1.2814436505670335E7,4661545.076323241);
//        Point p2 = new Point(-1.14382984677914E7, 5967231.216474611);
//        Point p3 = new Point(-1.0728500953517005E7,4520734.241320791);
//        mPoints.add(p1);
//        mPoints.add(p2);
//        mPoints.add(p3);

        addGraphicCricle();

//        multipath = new Polygon();
//        multipath.startPath(mPoints.get(0));
//        for (int i = 1; i < mPoints.size(); i++) {
//            multipath.lineTo(mPoints.get(i));
//        }
//        /**自己加的*/

        drawPolylineOrPolygon();/**画多边形*/

//        String targetLayer = queryLayer.concat("/3");
//        String[] queryArray = { targetLayer, "AVGHHSZ_CY>3.5" };
        String[] queryArray = { queryLayer, "管径>200.00" };
        AsyncQueryTask ayncQuery = new AsyncQueryTask();
        ayncQuery.execute(queryArray);
    }

    /**画多边形*/
    private void drawPolylineOrPolygon() {
        Log.d("data", "执行了方法：drawPolylineOrPolygon");
        Graphic graphic;

        if (mPoints.size() > 1) {

            multipath = new Polygon();
            multipath.startPath(mPoints.get(0));
            for (int i = 1; i < mPoints.size(); i++) {
                multipath.lineTo(mPoints.get(i));
            }


            SimpleFillSymbol simpleFillSymbol = new SimpleFillSymbol(Color.YELLOW);
            simpleFillSymbol.setAlpha(100);
            simpleFillSymbol.setOutline(new SimpleLineSymbol(Color.BLACK, 1));
            graphic = new Graphic(multipath, (simpleFillSymbol));

            graphicsLayer.addGraphic(graphic);
        }
    }


    /**以自身为圆点，3000米为半径画圆，得到这个圆的所有边缘坐标集合mPoints*/
    private void addGraphicCricle() {
//        Point locationPoint = locationDisplayManager.getPoint();//获取自身定位点坐标
        Point locationPoint = new Point(89.24223436230119,42.95837880150215);
        double locationX = locationPoint.getX();
        double locationY = locationPoint.getY();

        double r = 300;



        for (double i=0; i<120; i++) {
            //sin值
            double sin = Math.sin(Math.PI * 2 * i / 120);
            //cos值
            double cos = Math.cos(Math.PI * 2 * i / 120);
//            //纬度= 中心点的纬度+半径*cos值
//            double lat = locationY + r*cos*ONE_METER_OFFSET;
//            //经度= 中心点的经度+半径*sin值
//            double lon = locationX + r *sin*calcLongitudeOffset(lat);
            //纬度= 中心点的纬度+半径*cos值
            double lat = locationY + r*cos*ONE_METER_OFFSET;
            //经度= 中心点的经度+半径*sin值
            double lon = locationX + r *sin*ONE_METER_OFFSET;
            Point p = new Point(lon,lat);
            mPoints.add(p);
        }
    }

    private class AsyncQueryTask extends AsyncTask<String, Void, FeatureResult> {

        @Override
        protected void onPreExecute() {
            progress = new ProgressDialog(QueryCenterAroundActivity.this);

            progress = ProgressDialog.show(QueryCenterAroundActivity.this, "",
                    "Please wait....query task is executing");

        }

        /**
         * First member in string array is the query URL; second member is the
         * where clause.
         */
        @Override
        protected FeatureResult doInBackground(String... queryArray) {

            if (queryArray == null || queryArray.length <= 1)
                return null;

            String url = queryArray[0];
            QueryParameters qParameters = new QueryParameters();
            String whereClause = queryArray[1];
            SpatialReference sr = SpatialReference.create(4490);

//			qParameters.setGeometry(mMapView.getExtent());
            qParameters.setGeometry(multipath);

            qParameters.setOutSpatialReference(sr);
            qParameters.setReturnGeometry(true);
            qParameters.setWhere(whereClause);

            QueryTask qTask = new QueryTask(url);

            try {
                return qTask.execute(qParameters);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;

        }

        @Override
        protected void onPostExecute(FeatureResult results) {

            String message = "No result comes back";

            if (results != null) {
                int size = (int) results.featureCount();
                for (Object element : results) {
                    progress.incrementProgressBy(size / 100);
                    if (element instanceof Feature) {
                        Feature feature = (Feature) element;
                        // turn feature into graphic
                        Graphic graphic = new Graphic(feature.getGeometry(),
                                feature.getSymbol(), feature.getAttributes());
                        // add graphic to layer
                        graphicsLayer.addGraphic(graphic);


//                        drawPolylineOrPolygon();/**画多边形*/
                    }
                }
                // update message with results
                message = String.valueOf(results.featureCount())
                        + " results have returned from query.";

            }
            progress.dismiss();
            Toast toast = Toast.makeText(QueryCenterAroundActivity.this, message,
                    Toast.LENGTH_LONG);
            toast.show();

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

}
