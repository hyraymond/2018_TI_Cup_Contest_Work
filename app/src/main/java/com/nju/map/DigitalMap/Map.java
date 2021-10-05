package com.nju.map.DigitalMap;

import java.util.ArrayList;

/**
 * Created by YSL on 2017/10/6.
 * 抽象类，所有地图的父类，每张地图都要实现如下方法
 */

public abstract class Map {
    //构造函数，数字化点击位置
    public abstract void standardization(int X, int Y);
    //添加数字点
    public abstract void getDigitalPoint(ArrayList<Integer> digitalX, ArrayList<Integer> digitalY);
    //获取规范化后坐标X值
    public abstract int getX();
    //获取规范化后坐标Y值
    public abstract int getY();
    //获取是否是可点击位置
    public abstract boolean getFlag();
    //获取所点击位置的名字
    public abstract String getDestName();
    //获取地图比例
    public abstract int getScale();
    //获取地图集包含的张数
    public abstract int getMapsNum();
    //获取X方向上的伸缩比例
    public abstract float getRatio();
    //获取Y方向上的偏置
    public abstract float getOffset();
    //获取移动到下一张图最近的出口坐标
    public abstract int[] getExitable(int init_X, int init_Y, int nextIndex);
    //路径点上所包含的数字点的具体坐标
    public abstract void pathPointInfo(int index, ArrayList<Integer> Xarray, ArrayList<Integer> Yarray, int dest_X, int dest_Y);
    //利用佛洛依德算法求最短路径,距离为0代表两点之间无连通
    public abstract int[] floydPath(int x, int y, int num);
    //判断当前位置是否能够移动
    public abstract boolean canMove(int deltaX, int deltaY, int initX, int initY);
    //返回一个int数组，第一位代表是否需要换图（1），2,3位是换图后的新坐标，第三位是地图编号
    public abstract int[] canExit(int init_X, int init_Y);
    //切换至左边一张地图
    public abstract int goLeft();
    //切换至右边一张地图
    public abstract int goRight();
    //切换至上边一张地图
    public abstract int goUp();
    //切换至下边一张地图
    public abstract int goDown();
}
