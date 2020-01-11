package com.example.myapplication.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.myapplication.R;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {


    private static final int REQUEST_CAMERA = 1;//相机权限

    @BindView(R.id.start)
    Button start;

    View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.start:
                    String[] permissions;
                    //读取内存权限
                    permissions = new String[]{
                            Manifest.permission.CAMERA
                    };
                    if (checkPermission(Manifest.permission.CAMERA)) {
                        //如果已经获得权限
                        Intent intent = new Intent(MainActivity.this, MeasureTreeHeightActivity.class);
                        startActivity(intent);
                    } else {
                        //否则去获取权限
                        getPermission(Manifest.permission.CAMERA, permissions);
                    }

            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        start.setOnClickListener(onClickListener);
    }

    //检查某个权限是否已经获得
    private boolean checkPermission(String permission) {
        //检查权限（NEED_PERMISSION）是否被授权 PackageManager.PERMISSION_GRANTED表示同意授权
        if (ActivityCompat.checkSelfPermission(this, permission)
                == PackageManager.PERMISSION_GRANTED)
            return true;
        else
            return false;
    }

    //获取权限
    private void getPermission(String permission, String[] permissions) {

        //申请权限
        ActivityCompat.requestPermissions(
                this,
                permissions,
                REQUEST_CAMERA);

        //用户已经拒绝过一次，再次弹出权限申请对话框需要给用户一个解释
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission))
            Toast.makeText(this, "请开通相关权限，否则无法正常使用本应用！", Toast.LENGTH_SHORT).show();

    }
}
