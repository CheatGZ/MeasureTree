package com.example.myapplication.activity;

import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutCompat;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myapplication.R;
import com.example.myapplication.camera.CameraPreview;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.example.myapplication.Utils.CalculateTreeHeight.calculateTHFlat;

public class MeasureTreeHeightActivity extends AppCompatActivity implements View.OnTouchListener, View.OnClickListener, SensorEventListener {

    @BindView(R.id.cursor)
    ImageView cursor;
    @BindView(R.id.tree_top)
    Button treeTop;
    @BindView(R.id.tree_bottom)
    Button treeBottom;
    @BindView(R.id.confirm)
    Button confirm;
    @BindView(R.id.camera)
    SurfaceView camera;
    @BindView(R.id.frame_layout)
    FrameLayout frameLayout;
    @BindView(R.id.slant_angle)
    TextView slantAngle;
    @BindView(R.id.ruler_value)
    EditText rulerValue;
    //陀螺仪传感器
    private SensorManager sensorManager = null;
    private Sensor magneticSensor = null;//地磁传感器
    private Sensor accelerometerSensor = null;//加速传感器


    private Camera mCamera;
    private CameraPreview mPreview;

    private int mCurrentCameraId = 0; // 1是前置 0是后置
    private double ruler = 0.75;//标尺长
    private double angle1;//俯角
    private double angle2;//仰角

    private float[] values = new float[3];//最终手机倾斜角度
    private float[] gravity = new float[3];//保存加速读传感器值
    private float[] geomagnetic = new float[3];//保存地磁传感器值
    private float[] r = new float[9];
    double azimuth;
    double pitch;
    double roll;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        ButterKnife.bind(this);
        initSensor();
        initData();
    }

    private void initSensor() {
        float[] R = new float[9];
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    private void initData() {

        mPreview = new CameraPreview(this, camera);
        mPreview.setLayoutParams(new LinearLayoutCompat.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        frameLayout.addView(mPreview);
        mPreview.setKeepScreenOn(true);
        camera.setOnTouchListener(this);
        treeBottom.setOnClickListener(this);
        treeTop.setOnClickListener(this);
        confirm.setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //为传感器注册监听器
        sensorManager.registerListener(this, magneticSensor,
                SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, accelerometerSensor,
                SensorManager.SENSOR_DELAY_NORMAL);
        int numCams = Camera.getNumberOfCameras();
        if (numCams > 0) {
            mCurrentCameraId = 0;
            mCamera = Camera.open(mCurrentCameraId);
            mCamera.startPreview();
            mPreview.setCamera(mCamera);
            mPreview.reAutoFocus();
        }
    }

    @Override
    protected void onPause() {
        if (camera != null) {
            mCamera.stopPreview();
            mPreview.setCamera(null);
            mCamera.release();
            mCamera = null;
            mPreview.setNull();
        }
        super.onPause();
        sensorManager.unregisterListener(this); // 解除监听器注册
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tree_bottom:
                treeBottom.setText((90D + pitch) + "");
                angle1 = 90D + pitch;
                break;
            case R.id.tree_top:
                treeTop.setText((90D + pitch) + "");
                angle2 = 90D + pitch;
                break;
            case R.id.confirm:
                if(rulerValue.getText().toString().length()>0) {
                    ruler = Double.parseDouble(rulerValue.getText().toString());
                    confirm.setText(calculateTHFlat(ruler, angle1, angle2) + "");
                }else {
                    Toast.makeText(this,"请输入标尺长度",Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return false;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            geomagnetic = event.values;
        }
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            gravity = event.values;
            getValue();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    //获取方向传感器的值
    public void getValue() {
        // r从这里返回
        SensorManager.getRotationMatrix(r, null, gravity, geomagnetic);
        //values从这里返回
        SensorManager.getOrientation(r, values);
        //提取数据
        double azimuth = Math.toDegrees(values[0]);
        if (azimuth < 0) {
            azimuth = azimuth + 360;
        }
        pitch = Math.toDegrees(values[1]);
        double roll = Math.toDegrees(values[2]);

        slantAngle.invalidate();
        slantAngle.setText(pitch + "");
    }
}
