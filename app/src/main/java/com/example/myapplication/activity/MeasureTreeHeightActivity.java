package com.example.myapplication.activity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myapplication.R;

import java.util.Arrays;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.example.myapplication.Utils.CalculateTreeHeight.calculateTHFlat;

public class MeasureTreeHeightActivity extends AppCompatActivity implements View.OnTouchListener, View.OnClickListener, SensorEventListener {

    @BindView(R.id.tree_top)
    Button treeTop;
    @BindView(R.id.tree_bottom)
    Button treeBottom;
    @BindView(R.id.slant_angle)
    TextView slantAngle;
    @BindView(R.id.ruler_value)
    EditText rulerValue;
    @BindView(R.id.texture_view)
    TextureView textureView;
    @BindView(R.id.tree_height)
    Button treeHeight;
    //陀螺仪传感器
    private SensorManager sensorManager = null;
    private Sensor magneticSensor = null;//地磁传感器
    private Sensor accelerometerSensor = null;//加速传感器

    private CameraManager mCameraManager;
    private Handler childHandler, mainHandler;
    private String mCameraId;//摄像头Id,1是前置 0是后置
    private ImageReader mImageReader;
    private CameraCaptureSession mCameraCaptureSession;
    private CameraDevice mCameraDevice;

    private double ruler = 1.5;//标尺长,默认1.5m
    private double angle1;//俯角
    private double angle2;//仰角

    private float[] values = new float[3];//最终手机倾斜角度
    private float[] gravity = new float[3];//保存加速读传感器值
    private float[] geomagnetic = new float[3];//保存地磁传感器值
    private float[] r = new float[9];
    double azimuth;
    double pitch;
    double roll;

    private CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @RequiresApi(api = Build.VERSION_CODES.P)
        @Override
        public void onOpened(@NonNull CameraDevice camera) {//打开摄像头
            mCameraDevice = camera;
            //开启预览
            takePreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {//关闭摄像头
            if (null != mCameraDevice) {
                mCameraDevice.close();
                MeasureTreeHeightActivity.this.mCameraDevice = null;
            }

        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Toast.makeText(MeasureTreeHeightActivity.this, "摄像头开启失败", Toast.LENGTH_SHORT).show();
        }
    };

    private TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            initCamera2();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        ButterKnife.bind(this);
        initView();
    }

    private void initView() {
        //初始化传感器
        float[] R = new float[9];
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);


        treeBottom.setOnClickListener(this);
        treeTop.setOnClickListener(this);
        treeHeight.setOnClickListener(this);
    }

    private void initCamera2() {
        HandlerThread handlerThread = new HandlerThread("Camera2");
        handlerThread.start();
        childHandler = new Handler(handlerThread.getLooper());
        mainHandler = new Handler(getMainLooper());
        mCameraId = "" + CameraCharacteristics.LENS_FACING_FRONT;//后置摄像头
        //获取摄像头管理
        mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            //打开摄像头
            mCameraManager.openCamera(mCameraId, stateCallback, mainHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 开启摄像头预览
     */
    @RequiresApi(api = Build.VERSION_CODES.P)
    private void takePreview() {
        SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
        //设置TextureView的缓冲区大小
        surfaceTexture.setDefaultBufferSize(textureView.getWidth(), textureView.getHeight());
        //获取Surface显示预览数据
        Surface surface = new Surface(surfaceTexture);
        try {
            mImageReader = ImageReader.newInstance(textureView.getWidth(), textureView.getHeight(), ImageFormat.JPEG, 1);
            //创建预览需要的CaptureRequest.Builder
            final CaptureRequest.Builder previewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            // 设置Surface作为预览数据的显示界面
            previewRequestBuilder.addTarget(surface);
            // 创建CameraCaptureSession，该对象负责管理处理预览请求和拍照请求
            mCameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        //自动对焦
                        previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                        //显示预览
                        CaptureRequest previewRequest = previewRequestBuilder.build();
                        mCameraCaptureSession = session;
                        mCameraCaptureSession.setRepeatingRequest(previewRequest, null, childHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                }
            }, childHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //为传感器注册监听器
        sensorManager.registerListener(this, magneticSensor,
                SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, accelerometerSensor,
                SensorManager.SENSOR_DELAY_NORMAL);

        //设置监听SurefaceTexture的事件
        textureView.setSurfaceTextureListener(textureListener);
    }

    @Override
    protected void onPause() {
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
            case R.id.tree_height:
                if (rulerValue.getText().toString().length() > 0) {
                    ruler = Double.parseDouble(rulerValue.getText().toString());
                    treeHeight.setText(calculateTHFlat(ruler, angle1, angle2) + "");
                } else {
                    treeHeight.setText(calculateTHFlat(ruler, angle1, angle2) + "");
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
