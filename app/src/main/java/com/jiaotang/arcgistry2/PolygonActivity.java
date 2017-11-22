package com.jiaotang.arcgistry2;

import android.content.Context;
import android.graphics.Color;
import android.location.LocationManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.android.map.Callout;
import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.LocationDisplayManager;
import com.esri.android.map.MapOnTouchListener;
import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISDynamicMapServiceLayer;
import com.esri.android.map.event.OnStatusChangedListener;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.Polyline;
import com.esri.core.map.Graphic;
import com.esri.core.renderer.SimpleRenderer;
import com.esri.core.symbol.SimpleFillSymbol;
import com.esri.core.symbol.SimpleLineSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PolygonActivity extends AppCompatActivity implements View.OnClickListener {

    public static final int GONGSHUI = 2;//供水
    public static final int SHANGSHUI = 4;//上水
    public static final int FEISHUI = 7;//废水
    public static final int WUSHUI = 9;//污水
    public static final int YUSHUI = 11;//雨水
    public static final int ZANGSHUI = 13;//脏水
    public static final int TIANRANQI = 16;//天然气
    public static final int RELI = 19;//热力

    /**
     * 当坐标系是地理坐标系的时候，需要加偏移量；投影坐标系不需要偏移量
     */
    public static double ONE_METER_OFFSET = 0.00000899322;//维度每米偏移量

    private MapView mMapView = null;
    private GraphicsLayer graphicLayer2;
    private String queryLayer;

    private ImageView queryTool;
    private PopupWindow mPopWindow;//查询范围的弹出框
    private PopupWindow controlPopWindow;//图层控制的弹出框
    private boolean isShowQueryPop = false;//是否显示查询范围的弹出框
    private boolean isShowControlPop = false;//是否显示图层控制弹出框



    public enum EditMode {
        /**
         * 编辑类型
         */
        NONE, POINT, POLYLINE, POLYGON, CIRCLE, SAVING
    }

    EditMode editMode = EditMode.NONE;

    GraphicsLayer mGraphicsLayerEditing;


    Polygon multipath = new Polygon();//多边形
    Polyline mPolyline = new Polyline();//折线


    ArrayList<Point> mPoints = new ArrayList<>();


    private GraphicsLayer graphicLayer16 = null;
    private GraphicsLayer graphicLayer4 = null;
    private GraphicsLayer graphicLayer7 = null;
    private GraphicsLayer graphicLayer9 = null;
    private GraphicsLayer graphicLayer11 = null;
    private GraphicsLayer graphicLayer13 = null;
    private GraphicsLayer graphicLayer19 = null;
    private List<GraphicsLayer> graphicsLayerList = new ArrayList<>();

    /**定位类*/
    LocationDisplayManager locationDisplayManager;


//    private ProgressDialog progress;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_polygon);


        mMapView = (MapView) this.findViewById(R.id.polygon_map);//设置UI和代码绑定
        queryTool = (ImageView) findViewById(R.id.queryTool);

        String dd1 = "http://sampleserver1.arcgisonline.com/ArcGIS/rest/services/Demographics/ESRI_Population_World/MapServer";
        String dd2 = "http://119.80.161.75:6080/arcgis/rest/services/YunGang/MapServer";
        String dd3 = "http://119.80.161.7:6080/arcgis/rest/services/androidTest_guanxian/MapServer/3";

        ArcGISDynamicMapServiceLayer agsDynlyr = new ArcGISDynamicMapServiceLayer(dd2);
        mMapView.addLayer(agsDynlyr);

        mMapView.centerAt(new Point(427041.42241601326, 4409694.396274698), true);

//        ArcGISFeatureLayer arcGISFeatureLayer = new ArcGISFeatureLayer(dd3, ArcGISFeatureLayer.MODE.ONDEMAND);
//        mMapView.addLayer(arcGISFeatureLayer);

        queryLayer = "http://119.80.161.75:6080/arcgis/rest/services/SY_guanxian/MapServer";
//        queryLayer = "http://services.arcgisonline.com/ArcGIS/rest/services/Demographics/USA_Average_Household_Size/MapServer";

        mMapView.setOnStatusChangedListener(new OnStatusChangedListener() {

            private static final long serialVersionUID = 1L;

            @Override
            public void onStatusChanged(Object source, STATUS status) {
                if (source == mMapView && status == STATUS.INITIALIZED) {
//                    /**管线的图形层*/
//                    graphicLayer2 = new GraphicsLayer();
//                    SimpleRenderer sr = new SimpleRenderer(
//                            new SimpleFillSymbol(Color.RED));
//                    graphicLayer2.setRenderer(sr);
//                    mMapView.addLayer(graphicLayer2);
                    /**点、线、面的图形层*/
                    mGraphicsLayerEditing = new GraphicsLayer();
                    mMapView.addLayer(mGraphicsLayerEditing);

                    mMapView.setScale(12466.02891329973);//缩放值越大，显示的地图越多
                }
            }
        });
        /**添加单击、双击监听*/
        mMapView.setOnTouchListener(new MyTouchListener(this, mMapView));
        initGraphicLayerList();//初始化图形图层数组graphicsLayerList；
    }

    private void initGraphicLayerList() {
        graphicsLayerList.add(graphicLayer2);
        graphicsLayerList.add(graphicLayer4);
        graphicsLayerList.add(graphicLayer7);
        graphicsLayerList.add(graphicLayer9);
        graphicsLayerList.add(graphicLayer11);
        graphicsLayerList.add(graphicLayer13);
        graphicsLayerList.add(graphicLayer16);
        graphicsLayerList.add(graphicLayer19);
    }

    /**显示查询范围工具*/
    public void showTool(View view) {

        if (isShowQueryPop) {
            mPopWindow.dismiss();
            isShowQueryPop = false;
            return;
        }

        if (mPopWindow == null) {
            View contentView = LayoutInflater.from(PolygonActivity.this).inflate(R.layout.query_popwindow, null);
            mPopWindow = new PopupWindow(contentView);
            mPopWindow.setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
            mPopWindow.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);

            View v1 = contentView.findViewById(R.id.queryCircle);
            View v2 = contentView.findViewById(R.id.queryLine);
            View v3 = contentView.findViewById(R.id.queryPolygon);
            View v4 = contentView.findViewById(R.id.queryPoint);
            v1.setOnClickListener(this);
            v2.setOnClickListener(this);
            v3.setOnClickListener(this);
            v4.setOnClickListener(this);
        }

        mPopWindow.showAsDropDown(queryTool, -10, 0);
        isShowQueryPop = true;
    }

    /**清除所有graphicLayer*/
    private void clearAllGraphicLayer() {
        mPoints.clear();//清空记录点
        multipath.setEmpty();//清空多边形
        mPopWindow.dismiss();//关掉查询框
        isShowQueryPop = false;//设置查询框是否显示标识为false

        mMapView.getCallout().hide();//隐藏管线信息弹出框

        if (graphicLayer2 != null) {
            graphicLayer2.removeAll();
        }
        if (graphicLayer4 != null) {
            graphicLayer4.removeAll();
        }
        if (graphicLayer7 != null) {
            graphicLayer7.removeAll();
        }
        if (graphicLayer9 != null) {
            graphicLayer9.removeAll();
        }
        if (graphicLayer11 != null) {
            graphicLayer11.removeAll();
        }
        if (graphicLayer13 != null) {
            graphicLayer13.removeAll();
        }
        if (graphicLayer16 != null) {
            graphicLayer16.removeAll();
        }
        if (graphicLayer19 != null) {
            graphicLayer19.removeAll();
        }

        mGraphicsLayerEditing.removeAll();

    }
    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id) {

            /**显示“点”的按钮点击事件*/
            case R.id.queryPoint: {
                editMode = EditMode.POINT;

                clearAllGraphicLayer();
                mPolyline.setEmpty();
            }
            break;

            /**“显示“圆形”的按钮点击事件*/
            case R.id.queryCircle: {
                editMode = EditMode.CIRCLE;

                Point p = mMapView.getCenter();

                clearAllGraphicLayer();

                addGraphicCricle(500,p);
                drawPolylineOrPolygon(true);

                queryAllGuanXian();

            }
            break;

            /**显示“折线”的按钮点击事件*/
            case R.id.queryLine: {
                editMode = EditMode.POLYLINE;

                mPolyline.setEmpty();
                clearAllGraphicLayer();
            }
            break;

            /**显示“多边形”的按钮点击事件*/
            case R.id.queryPolygon: {
                editMode = EditMode.POLYGON;

                mPolyline.setEmpty();
                clearAllGraphicLayer();
            }
            break;
            default:
                break;
        }
    }


    /**画多边形*/
    private void drawPolylineOrPolygon(boolean isDrawPolygon) {
        Log.d("data", "执行了方法：drawPolylineOrPolygon");
        Graphic graphic;

        // Create and add graphics layer if it doesn't already exist
        if (mGraphicsLayerEditing == null) {
            mGraphicsLayerEditing = new GraphicsLayer();
            mMapView.addLayer(mGraphicsLayerEditing);
        } else {
            mGraphicsLayerEditing.removeAll();
        }

        if (editMode == EditMode.POLYLINE) {
            if (mPoints.size() > 1) {

                mPolyline.setEmpty();
                mPolyline.startPath(mPoints.get(0));
                for (int i = 1; i < mPoints.size(); i++) {
                    mPolyline.lineTo(mPoints.get(i));
                }

                graphic = new Graphic(mPolyline, new SimpleLineSymbol(Color.BLACK, 3));
                mGraphicsLayerEditing.addGraphic(graphic);
            }


        } else {
            if (mPoints.size() > 1) {

                multipath.setEmpty();
                multipath.startPath(mPoints.get(0));
                for (int i = 1; i < mPoints.size(); i++) {
                    multipath.lineTo(mPoints.get(i));
                }

                /**根据布尔值isDrawPolygon，来判断是否需要画出多边形*/
                if (isDrawPolygon) {
                    SimpleFillSymbol simpleFillSymbol = new SimpleFillSymbol(Color.parseColor("#FAFAD2"));
                    simpleFillSymbol.setAlpha(100);
                    simpleFillSymbol.setOutline(new SimpleLineSymbol(Color.BLACK, 1));
                    graphic = new Graphic(multipath, (simpleFillSymbol));

                    mGraphicsLayerEditing.addGraphic(graphic);
                }

            }
        }


    }

    /**
     * 以自身为圆点，500米为半径画圆，得到这个圆的所有边缘坐标集合mPoints
     */
    private void addGraphicCricle(double r,Point circlePoint) {
//        Point locationPoint = locationDisplayManager.getPoint();//获取自身定位点坐标

        double locationX = circlePoint.getX();
        double locationY = circlePoint.getY();

        /**这里的是在投影坐标系下的圆坐标计算公式*/
        for (double i = 0; i < 120; i++) {
            //sin值
            double sin = Math.sin(Math.PI * 2 * i / 120);
            //cos值
            double cos = Math.cos(Math.PI * 2 * i / 120);

            //经度= 中心点的经度+半径*sin值
            double x = locationX + r * sin;
            //纬度= 中心点的纬度+半径*cos值
            double y = locationY + r * cos;

            Point p = new Point(x, y);

            mPoints.add(p);
        }


        /**下面的是在大地坐标系下的圆坐标计算公式*/
//        for (double i=0; i<120; i++) {
//            //sin值
//            double sin = Math.sin(Math.PI * 2 * i / 120);
//            //cos值
//            double cos = Math.cos(Math.PI * 2 * i / 120);
////            //纬度= 中心点的纬度+半径*cos值
////            double lat = locationY + r*cos*ONE_METER_OFFSET;
////            //经度= 中心点的经度+半径*sin值
////            double lon = locationX + r *sin*calcLongitudeOffset(lat);
//            //纬度= 中心点的纬度+半径*cos值
//            double lat = locationY + r*cos*ONE_METER_OFFSET;
//            //经度= 中心点的经度+半径*sin值
//            double lon = locationX + r *sin*ONE_METER_OFFSET;
//            Point p = new Point(lon,lat);
//            mPoints.add(p);
//        }

    }

    /**
     * 由于距离较近，这个偏移量可以暂时不用（根据维度计算当前经度的偏移量）
     */
    private double calcLongitudeOffset(double latitude) {
        return ONE_METER_OFFSET / Math.cos(latitude * Math.PI / 180.0f);
    }

    /**
     * 查询所有管线
     */
    private void queryAllGuanXian() {
        /**查询第一条管线*/
        String targetLayer1 = queryLayer.concat("/2");
        String[] queryArray1 = {targetLayer1, null};//查询条件设置为null，表示查询所有数据

        Singleton singleton1 = new Singleton();
        singleton1.setParm(this, editMode, mPolyline, multipath, GONGSHUI, queryArray1);
        singleton1.startQuery();

        /**查询第2条管线*/
        String targetLayer2 = queryLayer.concat("/4");
        String[] queryArray2 = {targetLayer2, null};//查询条件设置为null，表示查询所有数据

        Singleton singleton2 = new Singleton();
        singleton2.setParm(this, editMode, mPolyline, multipath, SHANGSHUI, queryArray2);
        singleton2.startQuery();

        /**查询第3条管线*/
        String targetLayer3 = queryLayer.concat("/7");
        String[] queryArray3 = {targetLayer3, null};//查询条件设置为null，表示查询所有数据

        Singleton singleton3 = new Singleton();
        singleton3.setParm(this, editMode, mPolyline, multipath, FEISHUI, queryArray3);
        singleton3.startQuery();

        /**查询第4条管线*/
        String targetLayer4 = queryLayer.concat("/9");
        String[] queryArray4 = {targetLayer4, null};//查询条件设置为null，表示查询所有数据

        Singleton singleton4 = new Singleton();
        singleton4.setParm(this, editMode, mPolyline, multipath, WUSHUI, queryArray4);
        singleton4.startQuery();

        /**查询第5条管线*/
        String targetLayer5 = queryLayer.concat("/11");
        String[] queryArray5 = {targetLayer5, null};//查询条件设置为null，表示查询所有数据

        Singleton singleton5 = new Singleton();
        singleton5.setParm(this, editMode, mPolyline, multipath, YUSHUI, queryArray5);
        singleton5.startQuery();

        /**查询第6条管线*/
        String targetLayer6 = queryLayer.concat("/13");
        String[] queryArray6 = {targetLayer6, null};//查询条件设置为null，表示查询所有数据

        Singleton singleton6 = new Singleton();
        singleton6.setParm(this, editMode, mPolyline, multipath, ZANGSHUI, queryArray6);
        singleton6.startQuery();

        /**查询第7条管线*/
        String targetLayer7 = queryLayer.concat("/16");
        String[] queryArray7 = {targetLayer7, null};//查询条件设置为null，表示查询所有数据

        Singleton singleton7 = new Singleton();
        singleton7.setParm(this, editMode, mPolyline, multipath, TIANRANQI, queryArray7);
        singleton7.startQuery();

        /**查询第8条管线*/
        String targetLayer8 = queryLayer.concat("/19");
        String[] queryArray8 = {targetLayer8, null};//查询条件设置为null，表示查询所有数据

        Singleton singleton8 = new Singleton();
        singleton8.setParm(this, editMode, mPolyline, multipath, RELI, queryArray8);
        singleton8.startQuery();


//        /**查询第一条管线*/
//        String targetLayer1 = queryLayer.concat("/2");
//        //String[] queryArray = { targetLayer, "AVGHHSZ_CY>3.5" };//查询条件设置为AVGHHSZ_CY>3.5，只查询符合该条件的数据
//        String[] queryArray1 = { targetLayer1, null };//查询条件设置为null，表示查询所有数据
//
//        AsyncQueryTask ayncQuery1 = new AsyncQueryTask();
//        ayncQuery1.setSymbolId(2);
//        ayncQuery1.execute(queryArray1);
//
//        /**查询第二条管线*/
//        String targetLayer2 = queryLayer.concat("/4");
//        String[] queryArray2 = { targetLayer2, null };//查询条件设置为null，表示查询所有数据
//        AsyncQueryTask ayncQuery2 = new AsyncQueryTask();
//        ayncQuery2.setSymbolId(4);
//        ayncQuery2.execute(queryArray2);
//
//        /**查询第三条管线*/
//        String targetLayer3 = queryLayer.concat("/7");
//        String[] queryArray3 = { targetLayer3, null };//查询条件设置为null，表示查询所有数据
//        AsyncQueryTask ayncQuery3 = new AsyncQueryTask();
//        ayncQuery3.setSymbolId(7);
//        ayncQuery3.execute(queryArray3);
//        /**查询第四条管线*/
//        String targetLayer4 = queryLayer.concat("/9");
//        String[] queryArray4 = { targetLayer4, null };//查询条件设置为null，表示查询所有数据
//        AsyncQueryTask ayncQuery4 = new AsyncQueryTask();
//        ayncQuery4.setSymbolId(9);
//        ayncQuery4.execute(queryArray4);
//        /**查询第5条管线*/
//        String targetLayer5 = queryLayer.concat("/13");
//        String[] queryArray5 = { targetLayer5, null };//查询条件设置为null，表示查询所有数据
//        AsyncQueryTask ayncQuery5 = new AsyncQueryTask();
//        ayncQuery5.setSymbolId(13);
//        ayncQuery5.execute(queryArray5);
//        /**查询第6条管线*/
//        String targetLayer6 = queryLayer.concat("/11");
//        String[] queryArray6 = { targetLayer6, null };//查询条件设置为null，表示查询所有数据
//        AsyncQueryTask ayncQuery6 = new AsyncQueryTask();
//        ayncQuery6.setSymbolId(11);
//        ayncQuery6.execute(queryArray6);
//
//        /**查询第7条管线*/
//        String targetLayer7 = queryLayer.concat("/16");
//        String[] queryArray7 = { targetLayer7, null };//查询条件设置为null，表示查询所有数据
//        AsyncQueryTask ayncQuery7 = new AsyncQueryTask();
//        ayncQuery7.setSymbolId(16);
//        ayncQuery7.execute(queryArray7);
//        /**查询第8条管线*/
//        String targetLayer8 = queryLayer.concat("/19");
//        String[] queryArray8 = { targetLayer8, null };//查询条件设置为null，表示查询所有数据
//        AsyncQueryTask ayncQuery8 = new AsyncQueryTask();
//        ayncQuery8.setSymbolId(19);
//        ayncQuery8.execute(queryArray8);

    }


    /**singleton查询完毕后调用此方法，显示管线*/
    public void showGuanXian(Graphic graphic,int singletonSymbolId) {
        switch (singletonSymbolId) {
            case GONGSHUI:
                if (graphicLayer2 == null) {
                    graphicLayer2 = new GraphicsLayer();
                    mMapView.addLayer(graphicLayer2);
                }
                graphicLayer2.addGraphic(graphic);
                break;
            case SHANGSHUI:
                if (graphicLayer4 == null) {
                    graphicLayer4 = new GraphicsLayer();
                    mMapView.addLayer(graphicLayer4);
                }
                graphicLayer4.addGraphic(graphic);
                break;
            case FEISHUI:
                if (graphicLayer7 == null) {
                    graphicLayer7 = new GraphicsLayer();
                    mMapView.addLayer(graphicLayer7);
                }
                graphicLayer7.addGraphic(graphic);
                break;
            case WUSHUI:
                if (graphicLayer9 == null) {
                    graphicLayer9 = new GraphicsLayer();
                    mMapView.addLayer(graphicLayer9);
                }
                graphicLayer9.addGraphic(graphic);
                break;
            case YUSHUI:
                if (graphicLayer11 == null) {
                    graphicLayer11 = new GraphicsLayer();
                    mMapView.addLayer(graphicLayer11);
                }
                graphicLayer11.addGraphic(graphic);
                break;
            case ZANGSHUI:
                if (graphicLayer13 == null) {
                    graphicLayer13 = new GraphicsLayer();
                    mMapView.addLayer(graphicLayer13);
                }
                graphicLayer13.addGraphic(graphic);
                break;

            case TIANRANQI:
                if (graphicLayer16 == null) {
                    graphicLayer16 = new GraphicsLayer();
                    mMapView.addLayer(graphicLayer16);
                }
                graphicLayer16.addGraphic(graphic);
                break;
            case RELI:
                if (graphicLayer19 == null) {
                    graphicLayer19 = new GraphicsLayer();
                    mMapView.addLayer(graphicLayer19);
                }
                graphicLayer19.addGraphic(graphic);
                break;
        }

        editMode = PolygonActivity.EditMode.NONE;
    }

    /**图层控制*/
    public void controlLayer(View view) {
        if (isShowControlPop) {
            controlPopWindow.dismiss();
            isShowControlPop = false;
            return;
        }


        if (controlPopWindow == null) {
            View contentView = LayoutInflater.from(this).inflate(R.layout.control_popwindow, null);

            controlPopWindow = new PopupWindow(this);
            controlPopWindow.setContentView(contentView);
            controlPopWindow.setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
            controlPopWindow.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);

            CheckBox cb_gongshui = (CheckBox) contentView.findViewById(R.id.cb_gongshui);
            CheckBox cb_shangshui = (CheckBox) contentView.findViewById(R.id.cb_shangshui);
            CheckBox cb_feishui = (CheckBox) contentView.findViewById(R.id.cb_feishui);
            CheckBox cb_wushui = (CheckBox) contentView.findViewById(R.id.cb_wushui);
            CheckBox cb_yushui = (CheckBox) contentView.findViewById(R.id.cb_yushui);
            CheckBox cb_zangshui = (CheckBox) contentView.findViewById(R.id.cb_zangshui);
            CheckBox cb_tianranqi = (CheckBox) contentView.findViewById(R.id.cb_tianranqi);
            CheckBox cb_reli = (CheckBox) contentView.findViewById(R.id.cb_reli);

            cb_gongshui.setOnCheckedChangeListener(new MyCheckedChangeListener());
            cb_shangshui.setOnCheckedChangeListener(new MyCheckedChangeListener());
            cb_feishui.setOnCheckedChangeListener(new MyCheckedChangeListener());
            cb_wushui.setOnCheckedChangeListener(new MyCheckedChangeListener());
            cb_yushui.setOnCheckedChangeListener(new MyCheckedChangeListener());
            cb_zangshui.setOnCheckedChangeListener(new MyCheckedChangeListener());
            cb_tianranqi.setOnCheckedChangeListener(new MyCheckedChangeListener());
            cb_reli.setOnCheckedChangeListener(new MyCheckedChangeListener());
        }


        controlPopWindow.showAsDropDown((Button)findViewById(R.id.btn_controlLayer), -10, 0);
        isShowControlPop = true;

    }
//    public class AsyncQueryTask extends AsyncTask<String, Void, FeatureResult> {
//
//        int symbolId;
//
//        AsyncQueryTask() {
//
//        }
//
//        public int getSymbolId() {
//            return symbolId;
//        }
//
//        public void setSymbolId(int symbolId) {
//            this.symbolId = symbolId;
//        }
//
//        @Override
//        protected void onPreExecute() {
////            progress = new ProgressDialog(PolygonActivity.this);
//
////            progress = ProgressDialog.show(PolygonActivity.this, "",
////                    "Please wait....query task is executing");
//
//        }
//
//        /**
//         * First member in string array is the query URL; second member is the
//         * where clause.
//         */
//        @Override
//        protected FeatureResult doInBackground(String... queryArray) {
//
//            if (queryArray == null || queryArray.length <= 1)
//                return null;
//
//            String url = queryArray[0];
//            QueryParameters qParameters = new QueryParameters();
//            String whereClause = queryArray[1];
//            SpatialReference sr = SpatialReference.create(2436);
//            if (editMode == EditMode.POLYGON) {
//                qParameters.setGeometry(multipath);
//            } else if (editMode == EditMode.POLYLINE) {
//                qParameters.setGeometry(mPolyline);
//            }
//
//            qParameters.setOutSpatialReference(sr);
//            qParameters.setReturnGeometry(true);
//            qParameters.setWhere(whereClause);
//
//            QueryTask qTask = new QueryTask(url);
//
//            try {
//                return qTask.execute(qParameters);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            return null;
//
//        }
//
//        @Override
//        protected void onPostExecute(FeatureResult results) {
//
//            String message = "No result comes back";
//
//            if (results != null) {
//                int size = (int) results.featureCount();
//                for (Object element : results) {
////                    progress.incrementProgressBy(size / 100);
//                    if (element instanceof Feature) {
//                        Feature feature = (Feature) element;
//                        // turn feature into graphic
////                        Geometry geometry = feature.getGeometry();
////                        Symbol s = feature.getSymbol();
////                        Map<String, Object> attr = feature.getAttributes();
////                        String ssss = (String) attr.get("NAME");
////                        String st = (String) attr.get("ST_ABBREV");
////                        Graphic graphic = new Graphic(feature.getGeometry(),
////								feature.getSymbol(), feature.getAttributes());
//
//                        SimpleLineSymbol lineSymbol = null;
//                        switch (symbolId) {
//                            case 2:
//                                lineSymbol = new SimpleLineSymbol(Color.RED,2);
//                                break;
//                            case 4:
//                                lineSymbol = new SimpleLineSymbol(Color.BLUE,2);
//                                break;
//                            case 7:
//                                lineSymbol = new SimpleLineSymbol(Color.GREEN,2);
//                                break;
//                            case 9:
//                                lineSymbol = new SimpleLineSymbol(Color.GREEN,2);
//                                break;
//                            case 11:
//                                lineSymbol = new SimpleLineSymbol(Color.GREEN,2);
//                                break;
//                            case 13:
//                                lineSymbol = new SimpleLineSymbol(Color.GREEN,2);
//                                break;
//                            case 16:
//                                lineSymbol = new SimpleLineSymbol(Color.BLUE,2);
//                                break;
//                            case 19:
//                                lineSymbol = new SimpleLineSymbol(Color.parseColor("#FF00FF"),2);
//                                break;
//
//                            default:
//                                break;
//
//                        }
//
//
//
//                        Graphic graphic = new Graphic(feature.getGeometry(),
//                                lineSymbol, feature.getAttributes());
//                        // add graphic to layer
//                        graphicsLayer.addGraphic(graphic);
//
//                        editMode = EditMode.NONE;
//                    }
//                }
//                // update message with results
//                message = String.valueOf(results.featureCount())
//                        + " results have returned from query.";
//
//            }
////            progress.dismiss();
////            Toast toast = Toast.makeText(PolygonActivity.this, message,
////                    Toast.LENGTH_LONG);
////            toast.show();
//
//        }
//
//    }



    /**
     * 自定义单击、双击监听事件
     */
    public class MyTouchListener extends MapOnTouchListener {
        public MyTouchListener(Context context, MapView view) {
            super(context, view);
        }

        /**弹出框*/
        public void showPop(Map<String,Object> attr,Point point) {

            Callout callout = mMapView.getCallout();

            View view = LayoutInflater.from(PolygonActivity.this).inflate(R.layout.callout_layout, null);

            TextView tvMessage = (TextView) view.findViewById(R.id.callout_message);

            StringBuilder message = new StringBuilder();
            if (attr.get("ID") != null) {
                int id = (int) attr.get("ID");
                message.append("ID：");
                message.append(id);
                message.append("\n");
            }
            if (attr.get("ET_ID" )!= null) {
                message.append("ET_ID：");
                message.append((String) attr.get("ET_ID" ));
                message.append("\n");
            }
            if (attr.get("上点号" )!= null) {
                message.append("上点号：");
                message.append((String) attr.get("上点号" ));
                message.append("\n");
            }
            if (attr.get("本点号" )!= null) {
                message.append("本点号：");
                message.append((String) attr.get("本点号" ));
                message.append("\n");
            }
            if (attr.get("管线材质" )!= null) {
                message.append("管线材质：");
                message.append((String) attr.get("管线材质" ));
                message.append("\n");
            }
            if (attr.get("埋设方式" )!= null) {
                message.append("埋设方式：");
                message.append((String) attr.get("埋设方式" ));
                message.append("\n");
            }
            if (attr.get("管径" )!= null) {
                message.append("管径：");
                message.append((String) attr.get("管径" ));
                message.append("\n");
            }
            if (attr.get("上点埋深" )!= null) {
                message.append("上点埋深：");
                message.append((double) attr.get("上点埋深" ));
                message.append("\n");
            }
            if (attr.get("本点埋深" )!= null) {
                message.append("本点埋深：");
                message.append((double) attr.get("本点埋深" ));
                message.append("\n");
            }
            if (attr.get("上点管顶高" )!= null) {
                message.append("上点管顶高：");
                message.append((double) attr.get("上点管顶高" ));
                message.append("\n");
            }
            if (attr.get("本点管顶高" )!= null) {
                message.append("本点管顶高：");
                message.append((double) attr.get("本点管顶高" ));
                message.append("\n");
            }
            if (attr.get("权属单位" )!= null) {
                message.append("权属单位：");
                message.append((String) attr.get("权属单位" ));
                message.append("\n");
            }
            if (attr.get("埋设年代" )!= null) {
                message.append("埋设年代：");
                message.append((String) attr.get("埋设年代" ));
                message.append("\n");
            }
            if (attr.get("备注" )!= null) {
                message.append("备注：");
                message.append((String) attr.get("备注" ));
                message.append("\n");
            }
            if (attr.get("管段编号" )!= null) {
                message.append("管段编号：");
                message.append((String) attr.get("管段编号" ));
                message.append("\n");
            }
            if (attr.get("上点特征" )!= null) {
                message.append("上点特征：");
                message.append((String) attr.get("上点特征" ));
                message.append("\n");
            }
            if (attr.get("本点特征" )!= null) {
                message.append("本点特征：");
                message.append((String) attr.get("本点特征" ));
                message.append("\n");
            }
            if (attr.get("所属区域" )!= null) {
                message.append("所属区域：");
                message.append((int) attr.get("所属区域" ));
                message.append("\n");
            }

            tvMessage.setText(message.toString());
            callout.setContent(view);
            callout.show(point);

        }

        /**点击graphicLayer，查询该点数据信息*/
        public void clickGraphicLayer(MotionEvent point) {
            Point p = mMapView.toMapPoint(point.getX(), point.getY());

//            for (int i=0; i<graphicsLayerList.size();i++) {
//                GraphicsLayer graphicsLayer = graphicsLayerList.get(i);
//                if (graphicsLayer != null) {
//                    int[] graphicIds = graphicsLayer.getGraphicIDs(point.getX(), point.getY(),8,1);
//
//                    Graphic graphic = graphicsLayer.getGraphic(graphicIds[0]);
//                    Map<String,Object> attr = graphic.getAttributes();
//                    showPop(attr,p);
//                    Log.d("data", "管线ID为:" + attr.get("ET_ID"));
//
//                    if (graphicIds.length > 0) {
//                        return;
//                    }
//                }
//
//            }

            if (graphicLayer2 != null && graphicLayer2.isVisible()) {
                /**点击显示graphicLayer2详细信息*/
                int[] graphicIds = graphicLayer2.getGraphicIDs(point.getX(), point.getY(),8,1);
                for (int i=0; i<graphicIds.length; i++) {
                    Graphic graphic = graphicLayer2.getGraphic(graphicIds[i]);
                    Map<String,Object> attr = graphic.getAttributes();
                    showPop(attr,p);
                    Log.d("data", "管线ID为:" + attr.get("ET_ID"));
                }
                if (graphicIds.length > 0) {
                    return;
                }
            }
            /**点击显示graphicLayer4详细信息*/
            if (graphicLayer4 != null&& graphicLayer4.isVisible()) {
                int[] graphicIds = graphicLayer4.getGraphicIDs(point.getX(), point.getY(),8,1);
                for (int i=0; i<graphicIds.length; i++) {
                    Graphic graphic = graphicLayer4.getGraphic(graphicIds[i]);
                    Map<String,Object> attr = graphic.getAttributes();
                    showPop(attr,p);
                    Log.d("data", "管线材质为:" + attr.get("管线材质"));
                }
                if (graphicIds.length > 0) {
                    return;
                }
            }
            /**点击显示graphicLayer7详细信息*/
            if (graphicLayer7 != null&& graphicLayer7.isVisible()) {
                int[] graphicIds = graphicLayer7.getGraphicIDs(point.getX(), point.getY(),8,1);
                for (int i=0; i<graphicIds.length; i++) {
                    Graphic graphic = graphicLayer7.getGraphic(graphicIds[i]);
                    Map<String,Object> attr = graphic.getAttributes();
                    showPop(attr,p);
                    Log.d("data", "管线材质为:" + attr.get("管线材质"));
                }
                if (graphicIds.length > 0) {
                    return;
                }
            }
            /**点击显示graphicLayer9详细信息*/
            if (graphicLayer9 != null&& graphicLayer9.isVisible()) {
                int[] graphicIds = graphicLayer9.getGraphicIDs(point.getX(), point.getY(),8,1);
                for (int i=0; i<graphicIds.length; i++) {
                    Graphic graphic = graphicLayer9.getGraphic(graphicIds[i]);
                    Map<String,Object> attr = graphic.getAttributes();
                    showPop(attr,p);
                    Log.d("data", "管线材质为:" + attr.get("管线材质"));
                }
                if (graphicIds.length > 0) {
                    return;
                }
            }
            /**点击显示graphicLayer11详细信息*/
            if (graphicLayer11 != null&& graphicLayer11.isVisible()) {
                int[] graphicIds = graphicLayer11.getGraphicIDs(point.getX(), point.getY(),8,1);
                for (int i=0; i<graphicIds.length; i++) {
                    Graphic graphic = graphicLayer11.getGraphic(graphicIds[i]);
                    Map<String,Object> attr = graphic.getAttributes();
                    showPop(attr,p);
                    Log.d("data", "管线材质为:" + attr.get("管线材质"));
                }
                if (graphicIds.length > 0) {
                    return;
                }
            }
            /**点击显示graphicLayer13详细信息*/
            if (graphicLayer13 != null&& graphicLayer13.isVisible()) {
                int[] graphicIds = graphicLayer13.getGraphicIDs(point.getX(), point.getY(),8,1);
                for (int i=0; i<graphicIds.length; i++) {
                    Graphic graphic = graphicLayer13.getGraphic(graphicIds[i]);
                    Map<String,Object> attr = graphic.getAttributes();
                    showPop(attr,p);
                    Log.d("data", "管线材质为:" + attr.get("管线材质"));
                }
                if (graphicIds.length > 0) {
                    return;
                }
            }

            /**点击显示graphicLayer16详细信息*/
            if (graphicLayer16 != null&& graphicLayer16.isVisible()) {
                int[] graphicIds = graphicLayer16.getGraphicIDs(point.getX(), point.getY(),8,1);
                for (int i=0; i<graphicIds.length; i++) {
                    Graphic graphic = graphicLayer16.getGraphic(graphicIds[i]);
                    Map<String,Object> attr = graphic.getAttributes();
                    showPop(attr,p);
                    Log.d("data", "管线材质为:" + attr.get("管线材质"));
                }
                if (graphicIds.length > 0) {
                    return;
                }
            }
            /**点击显示graphicLayer19详细信息*/
            if (graphicLayer19 != null&& graphicLayer19.isVisible()) {
                int[] graphicIds = graphicLayer19.getGraphicIDs(point.getX(), point.getY(),8,1);
                for (int i=0; i<graphicIds.length; i++) {
                    Graphic graphic = graphicLayer19.getGraphic(graphicIds[i]);
                    Map<String,Object> attr = graphic.getAttributes();
                    showPop(attr,p);
                    Log.d("data", "管线材质为:" + attr.get("管线材质"));
                }

            }



        }

        @Override
        public boolean onSingleTap(MotionEvent point) {

            if (controlPopWindow != null) {
                controlPopWindow.dismiss();//隐藏图层控制
                isShowControlPop = false;
            }
            mMapView.getCallout().hide();

            clickGraphicLayer(point);

            Point p = mMapView.toMapPoint(point.getX(), point.getY());

            /**单击时判断编辑类型是点、线、面中的哪一种*/
            switch (editMode) {
                case POLYGON:

                    mPoints.add(p);//加到点集合里

                    if (mPoints.size() < 3) { /**当点的数量少于3个的时候，显示点*/

                        Graphic pointGraphic1 = new Graphic(p, new SimpleMarkerSymbol(Color.BLACK, 8, SimpleMarkerSymbol.STYLE.CIRCLE));
                        mGraphicsLayerEditing.addGraphic(pointGraphic1);

                    } else {
                        /**当点的数量大于等于3个的时候，显示多边形*/
                        drawPolylineOrPolygon(true);
                        Toast.makeText(PolygonActivity.this, "双击完成操作", Toast.LENGTH_SHORT).show();
                    }


                    break;

                case POLYLINE:

                    mPoints.add(p);//加到点集合里
                    if (mPoints.size() < 2) { /**当点的数量少于2个的时候，显示点*/

                        Graphic pointGraphic2 = new Graphic(p, new SimpleMarkerSymbol(Color.BLACK, 8, SimpleMarkerSymbol.STYLE.CIRCLE));
                        mGraphicsLayerEditing.addGraphic(pointGraphic2);
                    } else {/**当点的数量大于等于2个的时候，显示折线*/
                        drawPolylineOrPolygon(true);
                        Toast.makeText(PolygonActivity.this, "双击完成操作", Toast.LENGTH_SHORT).show();
                    }


                    break;
                case POINT:
                    addGraphicCricle(500,p);
                    drawPolylineOrPolygon(true);

                    queryAllGuanXian();

                    break;

                default:
                    break;
            }

            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent point) {
            if (editMode == EditMode.POLYGON && mPoints.size() > 2) {
                queryAllGuanXian();
                mPoints.clear();
                return false;
            }
            if (editMode == EditMode.POLYLINE && mPoints.size() > 1) {
                queryAllGuanXian();
                mPoints.clear();
                return false;
            }

            return true;
        }

    }

    /**checkBox点击事件（即图层控制）*/
    private class MyCheckedChangeListener implements CompoundButton.OnCheckedChangeListener{

        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            if (graphicLayer2!=null && compoundButton.getId() == R.id.cb_gongshui) {
                graphicLayer2.setVisible(b);
            } else if (graphicLayer4!=null && compoundButton.getId() == R.id.cb_shangshui) {
                graphicLayer4.setVisible(b);
            }else if (graphicLayer7!=null && compoundButton.getId() == R.id.cb_feishui) {
                graphicLayer7.setVisible(b);
            }else if (graphicLayer9!=null && compoundButton.getId() == R.id.cb_wushui) {
                graphicLayer9.setVisible(b);
            }else if (graphicLayer11!=null && compoundButton.getId() == R.id.cb_yushui) {
                graphicLayer11.setVisible(b);
            }else if (graphicLayer13!=null && compoundButton.getId() == R.id.cb_zangshui) {
                graphicLayer13.setVisible(b);
            } else if (graphicLayer16!=null && compoundButton.getId() == R.id.cb_tianranqi) {
                graphicLayer16.setVisible(b);
            } else if (graphicLayer19!=null && compoundButton.getId() == R.id.cb_reli) {
                graphicLayer19.setVisible(b);
            }
        }
    }
}
