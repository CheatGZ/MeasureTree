package com.example.myapplication.Utils;

import android.util.Log;

import static java.lang.Math.cos;
import static java.lang.Math.tan;

public class CalculateTreeHeight {
    private static double VERTICAL_ANGLE = 90D;
    private static double HORIZONTAL_ANGLE = 0D;

    /**
     * @param ruler  标尺的长度，即手机到地面的距离
     * @param angle1 手机焦点瞄准树根时的角度
     * @param angle2 手机焦点瞄准树顶时的角度
     * @return 返回树的高度
     */
    public static double calculateTHFlat(double ruler, double angle1, double angle2) {
        double distence;//手机与树的距离
        double treeTopHeight;//手机到树顶的高度
        double treeBottomHeight;//手机到树底的高度
        double treeHeight;//树的高度

        treeBottomHeight = ruler;
        distence = treeBottomHeight / tan(angle1 * (Math.PI / 180));
//        Log.d("cheatGZ1  ", "角度值 " + angle1 + "tan值 " + tan(angle1 * (Math.PI / 180)) + "  距离值" + distence + "");
        treeTopHeight = distence * tan(angle2 * (Math.PI / 180));
//        Log.d("cheatGZ", "角度值 " + angle2 + "tan值 " + cos(angle1 * (Math.PI / 180)) + " " + treeTopHeight);
        treeHeight = treeTopHeight + treeBottomHeight;

        return treeHeight;
    }
}
