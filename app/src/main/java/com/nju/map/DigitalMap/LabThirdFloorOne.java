package com.nju.map.DigitalMap;

import com.nju.map.FLOYD;
import com.nju.map.ThirdActivity;
import java.util.ArrayList;

/**
 * Created by YSL on 2017/9/12.
 */

public class LabThirdFloorOne extends Map{
    private int x;
    private int y;
    private boolean flag;
    private String destName;
    private int scale = 15;
    private int mapsNum = 3;
//    private int init_X = 300;
//    private int init_Y = 575;
    private float ratio;
    private float offset;

    public LabThirdFloorOne(int screenWidth, int screenHeight){ratio=screenWidth/1080;if(ratio!=0)offset=screenHeight/2/ratio-840;}

    public void standardization(int X, int Y){
        flag=true;
        if(ratio!=0){
            X=(int)(X/ratio);
            Y=(int)(Y/ratio-offset);
        }
        if(X>=0&&X<=130&&Y>=330&&Y<=480){
            x=65;y=490; destName="丙413";
        }else if(X>=270&&X<=450&&Y>=330&&Y<=480){
            x=360;y=490; destName="丙411";
        }else if(X>=450&&X<=710&&Y>=330&&Y<=480){
            x=580;y=490; destName="露天平台";
        }else if(X>=770&&X<=1050&&Y>=330&&Y<=480){
            x=910;y=490; destName="丙409";
        }else if(X>=0&&X<=130&&Y>=500&&Y<=650){
            x=65;y=490; destName="丙418";
        }else if(X>=330&&X<=450&&Y>=500&&Y<=650){
            x=360;y=490; destName="丙416";
        }else if(X>=450&&X<=710&&Y>=500&&Y<=650){
            x=580;y=490; destName="丙414";
        }else if(X>=770&&X<=1050&&Y>=500&&Y<=650){
            x=910;y=490; destName="丙412";
        }else if(X>=770&&X<=1050&&Y>=650&&Y<=1030){
            x=740;y=840; destName="丙410";
        }else if(X>=130&&X<=270&&Y>=1030&&Y<=1180){
            x=200;y=1190; destName="丙407";
        }else if(X>=330&&X<=580&&Y>=1030&&Y<=1180){
            x=455;y=1190; destName="丙405";
        }else if(X>=580&&X<=710&&Y>=1030&&Y<=1180){
            x=645;y=1190; destName="丙403";
        }else if(X>=770&&X<=1050&&Y>=1030&&Y<=1180){
            x=910;y=1190; destName="丙401";
        }else if(X>=120&&X<=270&&Y>=1200&&Y<=1380){
            x=200;y=1190; destName="丙408";
        }else if(X>=270&&X<=330&&Y>=1200&&Y<=1380){
            x=301;y=1190; destName="丙406";
        }else if(X>=580&&X<=710&&Y>=1200&&Y<=1380){
            x=645;y=1190; destName="丙404";
        }else if(X>=770&&X<=1050&&Y>=1200&&Y<=1380){
            x=910;y=1190; destName="丙402";
        }else{
            flag=false;
        }
    }

    public void getDigitalPoint(ArrayList<Integer> digitalX, ArrayList<Integer> digitalY){
        digitalX.add(300);
        digitalX.add(740);
        digitalX.add(740);
        digitalX.add(300);
        digitalY.add(490);
        digitalY.add(490);
        digitalY.add(1190);
        digitalY.add(1190);
    }

    public int getX(){return x;}

    public int getY(){return y;}

    public boolean getFlag(){return flag;}

    public String getDestName(){return destName;}

    public int getScale(){return scale;}

    public int getMapsNum(){return mapsNum;}

    public float getRatio(){return ratio;}

    public float getOffset(){return offset;}

    public int[] getExitable(int init_X, int init_Y, int nextIndex){
        ArrayList<Integer> Xarray = new ArrayList<>();
        ArrayList<Integer> Yarray = new ArrayList<>();
        Xarray.add(0);
        Yarray.add(490);
        int distance = 10000; //MAX
        int nearest = 0;
        int temp;
        for(int i=0;i<Xarray.size();++i){
            if((temp=Math.abs(init_X-Xarray.get(i))+Math.abs(init_Y-Yarray.get(i)))<distance){
                distance=temp;
                nearest=i;
            }
        }
        int[] result = new int[2];
        result[0]=Xarray.get(nearest);
        result[1]=Yarray.get(nearest);
        return result;
    }

    public void pathPointInfo(int index, ArrayList<Integer> Xarray, ArrayList<Integer> Yarray, int dest_X, int dest_Y){
        switch (index) {
            case 0:
                Xarray.add(300);
                Yarray.add(490);
                break;
            case 1:
                Xarray.add(740);
                Yarray.add(490);
                break;
            case 2:
                Xarray.add(740);
                Yarray.add(1190);
                break;
            case 3:
                Xarray.add(300);
                Yarray.add(1190);
                break;
            case 4:
                Xarray.add(dest_X);
                Yarray.add(dest_Y);
            default:
                break;
        }
    }

    public int[] floydPath(int x, int y, int num){
        int length1,length2;
        int[][] data=new int[5][5];
        if(x==740){
            length1=Math.abs(490-y);
            length2=Math.abs(1190-y);
            int temp[][]={
                    {0,   440, 0,   700, 0},
                    {440, 0,   700, 0,   length1},
                    {0,   700, 0,   440, length2},
                    {700, 0,   440, 0,   0},
                    {0,   length1,  length2, 0,   0}
            };
            data=temp;
        }else if(y==490){
            length1=Math.abs(300-x);
            length2=Math.abs(740-x);
            int temp[][]={
                    {0,   440, 0,   700, length1},
                    {440, 0,   700, 0,   length2},
                    {0,   700, 0,   440, 0},
                    {700, 0,   440, 0,   0},
                    {length1,   length2,   0,   0,   0}
            };
            data=temp;
        }else if(y==1190){
            length1=Math.abs(740-x);
            length2=Math.abs(300-x);
            int temp[][]={
                    {0,   440, 0,   700, 0},
                    {440, 0,   700, 0,   0},
                    {0,   700, 0,   440, length1},
                    {700, 0,   440, 0,   length2},
                    {0,   0,   length1,   length2,   0}
            };
            data=temp;
        }
        FLOYD pathWay=new FLOYD(data);
        int path[] = pathWay.getpath(num,4);
        return path;
    }

    public boolean canMove(int deltaX, int deltaY, int initX, int initY){
        int nextX=initX+deltaX;
        int nextY=initY+deltaY;
        if(nextY==490&&nextX>0&&nextX<1080) return true;
        if(nextX==300&&nextY>490&&nextY<1190) return true;
        if(nextY==1190&&nextX>120&&nextX<1080) return true;
        if(nextX==740&&nextY>490&&nextY<1190) return true;
        return false;
    }

    public int[] canExit(int init_X, int init_Y){
        int[] temp = new int[4];
        if(init_Y==490&&init_X<3*scale){
            temp[0]=1;
            temp[1]=980;
            temp[2]=init_Y;
            temp[3]=2;
            return temp;
        }
        temp[0]=0;
        return temp;
    }

    public int goLeft(){return 2;}

    public int goRight(){return 1;}

    public int goUp(){return 1;}

    public int goDown(){return 1;}
}
