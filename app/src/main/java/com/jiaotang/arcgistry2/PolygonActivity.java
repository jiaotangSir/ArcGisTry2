package com.jiaotang.arcgistry2;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.android.map.Callout;
import com.esri.android.map.FeatureLayer;
import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.Layer;
import com.esri.android.map.MapOnTouchListener;
import com.esri.android.map.MapOptions;
import com.esri.android.map.MapView;
import com.esri.android.map.RasterLayer;
import com.esri.android.map.ags.ArcGISDynamicMapServiceLayer;
import com.esri.android.map.ags.ArcGISFeatureLayer;
import com.esri.android.map.ags.ArcGISImageServiceLayer;
import com.esri.android.map.ags.ArcGISTiledMapServiceLayer;
import com.esri.android.map.event.OnLongPressListener;
import com.esri.android.map.event.OnSingleTapListener;
import com.esri.android.map.event.OnStatusChangedListener;
import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.MultiPath;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.Polyline;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.internal.map.RasterLayerInternal;
import com.esri.core.map.Feature;
import com.esri.core.map.FeatureResult;
import com.esri.core.map.FeatureSet;
import com.esri.core.map.FeatureTemplate;
import com.esri.core.map.FeatureType;
import com.esri.core.map.Graphic;
import com.esri.core.portal.BaseMap;
import com.esri.core.renderer.Renderer;
import com.esri.core.renderer.SimpleRenderer;
import com.esri.core.symbol.SimpleFillSymbol;
import com.esri.core.symbol.SimpleLineSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.core.symbol.Symbol;
import com.esri.core.symbol.SymbolHelper;
import com.esri.core.tasks.SpatialRelationship;
import com.esri.core.tasks.ags.query.Query;
import com.esri.core.tasks.identify.IdentifyParameters;
import com.esri.core.tasks.identify.IdentifyResult;
import com.esri.core.tasks.identify.IdentifyTask;
import com.esri.core.tasks.query.QueryParameters;
import com.esri.core.tasks.query.QueryTask;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.Future;

import static com.jiaotang.arcgistry2.IdentifyActivity.dialog;

public class PolygonActivity extends AppCompatActivity implements View.OnClickListener {

    /**
     * 当坐标系是地理坐标系的时候，需要加偏移量；投影坐标系不需要偏移量
     */
    public static double ONE_METER_OFFSET = 0.00000899322;//维度每米偏移量

    private MapView mMapView = null;
    private GraphicsLayer graphicsLayer;
    private String queryLayer;

    private ImageView queryTool;
    private PopupWindow mPopWindow;
    private boolean isShowPop = false;


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
                    /**管线的图形层*/
                    graphicsLayer = new GraphicsLayer();
                    SimpleRenderer sr = new SimpleRenderer(
                            new SimpleFillSymbol(Color.RED));
                    graphicsLayer.setRenderer(sr);
                    mMapView.addLayer(graphicsLayer);

                    /**点、线、面的图形层*/
                    mGraphicsLayerEditing = new GraphicsLayer();
                    mMapView.addLayer(mGraphicsLayerEditing);

                    mMapView.setScale(12466.02891329973);//缩放值越大，显示的地图越多

                }
            }
        });

        /**添加单击、双击监听*/
        mMapView.setOnTouchListener(new MyTouchListener(this, mMapView));


//        /**地图单击点击事件*/
//        mMapView.setOnSingleTapListener(new OnSingleTapListener() {
//            @Override
//            public void onSingleTap(float v, float v1) {
//
//                Point p = mMapView.toMapPoint(v, v1);
//                mPoints.add(p);//加到点集合里
//                /**添加点到图形层*/
//                Graphic pointGraphic = new Graphic(p, new SimpleMarkerSymbol(Color.BLACK, 8, SimpleMarkerSymbol.STYLE.CIRCLE));
//                mGraphicsLayerEditing.addGraphic(pointGraphic);
//
//
//
//                /**如果是要画多边形*/
//                if (editMode == EditMode.POLYGON && mPoints.size()>2) {
//
//                    drawPolylineOrPolygon(true);
//
//                }
//
//            }
//        });


//        String strMap2 = "http://119.80.161.75:6080/arcgis/rest/services/SY_guanxian/MapServer/2";
//        String strMap3 = "http://119.80.161.75:6080/arcgis/rest/services/YunGang/MapServer/13";
//
//
//        ArcGISFeatureLayer featureLayer3 = new ArcGISFeatureLayer(strMap3, ArcGISFeatureLayer.MODE.ONDEMAND);
//        mMapView.addLayer(featureLayer3);
//
//
//        ArcGISFeatureLayer featureLayer1 = new ArcGISFeatureLayer(strMap1, ArcGISFeatureLayer.MODE.ONDEMAND);
//        mMapView.addLayer(featureLayer1);
//
//        ArcGISFeatureLayer featureLayer2 = new ArcGISFeatureLayer(strMap2, ArcGISFeatureLayer.MODE.ONDEMAND);
//        mMapView.addLayer(featureLayer2);


//        arcGISTiledMapServiceLayer = new ArcGISTiledMapServiceLayer(strMapUrl);
//        mMapView.addLayer(arcGISTiledMapServiceLayer);
//
//        featureLayer = new ArcGISFeatureLayer("http://sampleserver3.arcgisonline.com/ArcGIS/rest/services/Petroleum/KSPetro/MapServer/1", ArcGISFeatureLayer.MODE.ONDEMAND);
//        mMapView.addLayer(featureLayer);


//        设置中心点、在setOnStatusChangedListener（）中设置比例
//        Point p = new Point(426159.10538837116,4409492.064011023);
//        mMapView.centerAt(p,true);
//        mMapView.setOnStatusChangedListener(new OnStatusChangedListener() {
//            @Override
//            public void onStatusChanged(Object o, STATUS status) {
//                mMapView.setScale(8660.370726616633);
//            }
//        });

//        Point p = new Point(-1.0993221149066223E7,4574520.971549229);
//        mMapView.centerAt(p,true);
//        mMapView.setOnStatusChangedListener(new OnStatusChangedListener() {
//            @Override
//            public void onStatusChanged(Object o, STATUS status) {
//                mMapView.setScale(9457585.547225516);
//
//            }
//        });

//        Point p1 = new Point(426159.10538837116,4409492.064011023);
//        Point p2 = new Point(426355.10538837116,4409492.064011023);
//        Point p3 = new Point(426159.10538837116,4410092.064011023);
//        mPoints.add(p1);
//        mPoints.add(p2);
//        mPoints.add(p3);

//        Point p1 = new Point(-1.0993221149066223E7,4574520.971549229);
//        Point p2 = new Point(-1.1109620541460985E7,4495697.749357637);
//        Point p3 = new Point(-1.0915055870349934E7,4521435.944358981);
//        mPoints.add(p1);
//        mPoints.add(p2);
//        mPoints.add(p3);
//
//
//        /**创建绘制图形层*/
//        mGraphicsLayerEditing = new GraphicsLayer();
//        mMapView.addLayer(mGraphicsLayerEditing);
//
////        addGraphicCricle();
//        /**画多边形*/
//        drawPolylineOrPolygon();


//        // set Identify Parameters
//        params = new IdentifyParameters();
//        params.setTolerance(20);
//        params.setDPI(98);
//        params.setLayers(new int[] { 4 });
//        params.setLayerMode(IdentifyParameters.ALL_LAYERS);
    }

    public void showTool(View view) {

        if (isShowPop) {
            mPopWindow.dismiss();
            isShowPop = false;
            return;
        }
        View contentView = LayoutInflater.from(PolygonActivity.this).inflate(R.layout.my_popwindow, null);
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

        mPopWindow.showAsDropDown(queryTool, -10, 0);
        isShowPop = true;
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id) {

            case R.id.queryPoint: {
                editMode = EditMode.POINT;

                mPoints.clear();
                mPolyline.setEmpty();
                multipath.setEmpty();
                graphicsLayer.removeAll();
                mGraphicsLayerEditing.removeAll();
                mPopWindow.dismiss();
                isShowPop = false;
            }
            break;
            case R.id.queryCircle: {
//                editMode = EditMode.POLYGON;
                editMode = EditMode.CIRCLE;


                Point p = mMapView.getCenter();
//                Log.d("data", "中心点的x=" + p.getX());
//                Log.d("data", "中心点的y=" + p.getY());
//                Log.d("data", "缩放大小：" + mMapView.getScale());

                mPoints.clear();
                multipath.setEmpty();
                graphicsLayer.removeAll();
                mGraphicsLayerEditing.removeAll();

                addGraphicCricle(500,p);
                drawPolylineOrPolygon(true);


                queryAllGuanXian();
                mPopWindow.dismiss();
                isShowPop = false;
            }
            break;
            case R.id.queryLine: {
                editMode = EditMode.POLYLINE;

                mPoints.clear();
                multipath.setEmpty();
                mPolyline.setEmpty();
                graphicsLayer.removeAll();
                mGraphicsLayerEditing.removeAll();
                mPopWindow.dismiss();
                isShowPop = false;
            }
            break;
            case R.id.queryPolygon: {
                editMode = EditMode.POLYGON;

                mPoints.clear();
                mPolyline.setEmpty();
                multipath.setEmpty();
                graphicsLayer.removeAll();
                mGraphicsLayerEditing.removeAll();
                mPopWindow.dismiss();
                isShowPop = false;
            }
            break;
            default:
                break;
        }
    }


//    /**“显示“圆形”的按钮点击事件*/
//    public void showCircle(View view) {
//
//        editMode = EditMode.POLYGON;
//        Point p = mMapView.getCenter();
//        Log.d("data", "中心点的x=" + p.getX());
//        Log.d("data", "中心点的y=" + p.getY());
//        Log.d("data", "缩放大小：" + mMapView.getScale());
//
//        mPoints.clear();
//        multipath.setEmpty();
//        graphicsLayer.removeAll();
//        mGraphicsLayerEditing.removeAll();
//
//        addGraphicCricle(500);
//        drawPolylineOrPolygon(true);
//
//
//        queryAllGuanXian();
////        params.setGeometry(multipath);
////        params.setSpatialReference(mMapView.getSpatialReference());
////        params.setMapHeight(mMapView.getHeight());
////        params.setMapWidth(mMapView.getWidth());
////        params.setReturnGeometry(false);
////
////        // add the area of extent to identify parameters
////        Envelope env = new Envelope();
////        mMapView.getExtent().queryEnvelope(env);
////        params.setMapExtent(env);
////
////        // execute the identify task off UI thread
////        MyIdentifyTask mTask = new MyIdentifyTask(multipath);
////        mTask.execute(params);
//    }

    /**
     * 显示“点”的按钮点击事件
     */
//    public void showPoint(View view) {
//        editMode = EditMode.POLYGON;
//
//        mPoints.clear();
//        multipath.setEmpty();
//        mPolyline.setEmpty();
//        graphicsLayer.removeAll();
//        mGraphicsLayerEditing.removeAll();
//
//        addGraphicCricle(25);
//        drawPolylineOrPolygon(true);
//
//
//        queryAllGuanXian();
//    }

    /**显示“多边形”的按钮点击事件*/
//    public void showPolygon(View view) {
//        editMode = EditMode.POLYGON;
//
//        mPoints.clear();
//        mPolyline.setEmpty();
//        multipath.setEmpty();
//        graphicsLayer.removeAll();
//        mGraphicsLayerEditing.removeAll();
//    }
    /**显示“折线”的按钮点击事件*/
//    public void showPolyline(View view) {
//        editMode = EditMode.POLYLINE;
//
//        mPoints.clear();
//        multipath.setEmpty();
//        mPolyline.setEmpty();
//        graphicsLayer.removeAll();
//        mGraphicsLayerEditing.removeAll();
//
//    }

//    private void queryFeature(Geometry geometry) {
//        try {
//            QueryParameters args = new QueryParameters();
//            args.setReturnGeometry(true);// 是否返回Geometry
//            args.setGeometry(geometry); // 查询范围面
//            args.setInSpatialReference(SpatialReference
//                    .create(SpatialReference.WKID_WGS84));
//            args.setSpatialRelationship(SpatialRelationship.WITHIN);
//            //获取查询结果result
//            Future<FeatureResult> result = featureLayer.getFeatureTable()
//                    .queryFeatures(args, null);
//
//
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//    }

    /**
     * 画多边形
     */
    private void drawPolylineOrPolygon(boolean isDrawPolygon) {
        Log.d("data", "执行了方法：drawPolylineOrPolygon");
        Graphic graphic;
//        MultiPath multipath;

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
        singleton1.setParm(this, editMode, mPolyline, multipath, 2, queryArray1);
        singleton1.startQuery();

        /**查询第2条管线*/
        String targetLayer2 = queryLayer.concat("/4");
        String[] queryArray2 = {targetLayer2, null};//查询条件设置为null，表示查询所有数据

        Singleton singleton2 = new Singleton();
        singleton2.setParm(this, editMode, mPolyline, multipath, 4, queryArray2);
        singleton2.startQuery();

        /**查询第3条管线*/
        String targetLayer3 = queryLayer.concat("/7");
        String[] queryArray3 = {targetLayer3, null};//查询条件设置为null，表示查询所有数据

        Singleton singleton3 = new Singleton();
        singleton3.setParm(this, editMode, mPolyline, multipath, 7, queryArray3);
        singleton3.startQuery();

        /**查询第4条管线*/
        String targetLayer4 = queryLayer.concat("/9");
        String[] queryArray4 = {targetLayer4, null};//查询条件设置为null，表示查询所有数据

        Singleton singleton4 = new Singleton();
        singleton4.setParm(this, editMode, mPolyline, multipath, 9, queryArray4);
        singleton4.startQuery();

        /**查询第5条管线*/
        String targetLayer5 = queryLayer.concat("/11");
        String[] queryArray5 = {targetLayer5, null};//查询条件设置为null，表示查询所有数据

        Singleton singleton5 = new Singleton();
        singleton5.setParm(this, editMode, mPolyline, multipath, 11, queryArray5);
        singleton5.startQuery();

        /**查询第6条管线*/
        String targetLayer6 = queryLayer.concat("/13");
        String[] queryArray6 = {targetLayer6, null};//查询条件设置为null，表示查询所有数据

        Singleton singleton6 = new Singleton();
        singleton6.setParm(this, editMode, mPolyline, multipath, 13, queryArray6);
        singleton6.startQuery();

        /**查询第7条管线*/
        String targetLayer7 = queryLayer.concat("/16");
        String[] queryArray7 = {targetLayer7, null};//查询条件设置为null，表示查询所有数据

        Singleton singleton7 = new Singleton();
        singleton7.setParm(this, editMode, mPolyline, multipath, 16, queryArray7);
        singleton7.startQuery();

        /**查询第8条管线*/
        String targetLayer8 = queryLayer.concat("/19");
        String[] queryArray8 = {targetLayer8, null};//查询条件设置为null，表示查询所有数据

        Singleton singleton8 = new Singleton();
        singleton8.setParm(this, editMode, mPolyline, multipath, 19, queryArray8);
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

//    private class MyIdentifyTask extends
//            AsyncTask<IdentifyParameters, Void, IdentifyResult[]> {
//
//        IdentifyTask task = new IdentifyTask(strMapUrl);
//
//        IdentifyResult[] M_Result;
//
////        Point mAnchor;
//        Polygon p;
//
//
//        MyIdentifyTask(Polygon polygon) {
////            mAnchor = anchorPoint;
//            p = polygon;
//        }
//
//        @Override
//        protected void onPreExecute() {
//            // create dialog while working off UI thread
//            dialog = ProgressDialog.show(PolygonActivity.this, "Identify Task",
//                    "Identify query ...");
//
//        }
//
//        protected IdentifyResult[] doInBackground(IdentifyParameters... params) {
//
//            // check that you have the identify parameters
//            if (params != null && params.length > 0) {
//                IdentifyParameters mParams = params[0];
//
//                try {
//                    // Run IdentifyTask with Identify Parameters
//
//                    M_Result = task.execute(mParams);
//
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//
//            return M_Result;
//        }
//
//        @Override
//        protected void onPostExecute(IdentifyResult[] results) {
//
//            // dismiss dialog
//            if (dialog.isShowing()) {
//                dialog.dismiss();
//            }
//
//            ArrayList<IdentifyResult> resultList = new ArrayList<IdentifyResult>();
//
//            IdentifyResult result_1;
//
//            for (int index = 0; index < results.length; index++) {
//
//                result_1 = results[index];
//                String displayFieldName = result_1.getDisplayFieldName();
//                Map<String, Object> attr = result_1.getAttributes();
//                for (String key : attr.keySet()) {
//                    if (key.equalsIgnoreCase(displayFieldName)) {
//                        resultList.add(result_1);
//                    }
//                }
//            }
//
//            IdentifyResult curResult = resultList.get(1);
//            if (curResult.getAttributes().containsKey(
//                    "Name")) {
//                Log.d("data",curResult.getAttributes().get("Name").toString());
//            }
//
//
//
////            Callout callout = mMapView.getCallout();
////            callout.setContent(createIdentifyContent(resultList));
////            callout.show(mAnchor);
//
//
//        }
//    }


    public void showGuanXian(Graphic graphic) {
        graphicsLayer.addGraphic(graphic);
        editMode = PolygonActivity.EditMode.NONE;
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

        @Override
        public boolean onSingleTap(MotionEvent point) {
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

}
