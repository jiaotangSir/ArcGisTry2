package com.jiaotang.arcgistry2;

import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;

import com.esri.core.geometry.MultiPath;
import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.Polyline;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.map.Feature;
import com.esri.core.map.FeatureResult;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.SimpleLineSymbol;
import com.esri.core.symbol.Symbol;
import com.esri.core.tasks.query.QueryParameters;
import com.esri.core.tasks.query.QueryTask;

import java.util.Map;

/**
 * Created by Administrator on 2017/9/25.
 */

public class Singleton {

    public PolygonActivity pa;
    private PolygonActivity.EditMode editMode;
    private Polyline polyline;
    private Polygon polygon;
    private int symbolId;
    private String[] parm;




//    private static Singleton instance = null;
//    public synchronized static  Singleton getInstance() {
//        if (instance == null) {
//            instance = new Singleton();
//        }
//        return instance;
//    }

    public void setParm(PolygonActivity p, PolygonActivity.EditMode e, Polyline p1, Polygon p2, int id, String[] parm) {
        pa = p;
        editMode = e;
        polyline = p1;
        polygon = p2;
        symbolId = id;
        this.parm = parm;
    }

    public void startQuery() {
        AsyncQueryTask ayncQuery1 = new AsyncQueryTask();
        ayncQuery1.execute(parm);
    }


    public class AsyncQueryTask extends AsyncTask<String, Void, FeatureResult> {



        AsyncQueryTask() {

        }

        @Override
        protected void onPreExecute() {
//            progress = new ProgressDialog(PolygonActivity.this);

//            progress = ProgressDialog.show(PolygonActivity.this, "",
//                    "Please wait....query task is executing");

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
            SpatialReference sr = SpatialReference.create(2436);

//            if (editMode == PolygonActivity.EditMode.POLYGON || editMode == PolygonActivity.EditMode.POINT) {
//                qParameters.setGeometry(polygon);
//            } else if (editMode == PolygonActivity.EditMode.POLYLINE) {
//                qParameters.setGeometry(polyline);
//            }

            if (editMode == PolygonActivity.EditMode.POLYLINE) {
                qParameters.setGeometry(polyline);
            }else{
                qParameters.setGeometry(polygon);
            }


            qParameters.setOutSpatialReference(sr);
            qParameters.setReturnGeometry(true);
            qParameters.setWhere(whereClause);
            qParameters.setOutFields(new String[] {"*"});//其中*号表示查询所有字段

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
            Log.d("data", "此时的symbolId为：" + symbolId);

            if (results != null) {
                int size = (int) results.featureCount();
                for (Object element : results) {
//                    progress.incrementProgressBy(size / 100);
                    if (element instanceof Feature) {
                        Feature feature = (Feature) element;
                        // turn feature into graphic
//                        Geometry geometry = feature.getGeometry();
//                        Symbol s = feature.getSymbol();
//                        Map<String, Object> attr = feature.getAttributes();
//                        String ssss = (String) attr.get("NAME");
//                        String st = (String) attr.get("ST_ABBREV");
//                        Graphic graphic = new Graphic(feature.getGeometry(),
//								feature.getSymbol(), feature.getAttributes());

                        SimpleLineSymbol lineSymbol = null;
                        switch (symbolId) {
                            case 2:
                                lineSymbol = new SimpleLineSymbol(Color.parseColor("#00FFFF"),2);
                                break;
                            case 4:
                                lineSymbol = new SimpleLineSymbol(Color.parseColor("#00FFFF"),2);
                                break;
                            case 7:
                                lineSymbol = new SimpleLineSymbol(Color.parseColor("#B97C0D"),2);
                                break;
                            case 9:
                                lineSymbol = new SimpleLineSymbol(Color.parseColor("#B97C0D"),2);
                                break;
                            case 11:
                                lineSymbol = new SimpleLineSymbol(Color.parseColor("#B97C0D"),2);
                                break;
                            case 13:
                                lineSymbol = new SimpleLineSymbol(Color.parseColor("#B97C0D"),2);
                                break;
                            case 16:
                                lineSymbol = new SimpleLineSymbol(Color.parseColor("#EC008C"),2);
                                break;
                            case 19:
                                lineSymbol = new SimpleLineSymbol(Color.parseColor("#FBBE9F"),2);
                                break;

                            default:
                                break;

                        }

                        Map<String,Object> m = feature.getAttributes();
                        Symbol s = feature.getSymbol();

                        Graphic graphic = new Graphic(feature.getGeometry(),
                                lineSymbol, feature.getAttributes());
                        pa.showGuanXian(graphic);
                        // add graphic to layer
//                        graphicsLayer.addGraphic(graphic);


//                        editMode = PolygonActivity.EditMode.NONE;
                    }
                }
                // update message with results
                message = String.valueOf(results.featureCount())
                        + " results have returned from query.";

            }
//            progress.dismiss();
//            Toast toast = Toast.makeText(PolygonActivity.this, message,
//                    Toast.LENGTH_LONG);
//            toast.show();

        }

    }
}
