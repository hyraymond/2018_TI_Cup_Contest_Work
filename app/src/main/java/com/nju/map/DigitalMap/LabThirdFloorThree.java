package com.nju.map.DigitalMap;

import com.nju.map.FLOYD;
import java.util.ArrayList;

/**
 * Created by YSL on 2017/9/27.
 */

public class LabThirdFloorThree extends Map{
    private int x;
    private int y;
    private boolean flag;
    private String destName;
    private int scale = 15;
    private int mapsNum = 3;
    private float ratio;
    private float offset;

    public LabThirdFloorThree(int screenWidth, int screenHeight){ratio=screenWidth/1080;if(ratio!=0)offset=screenHeight/2/ratio-840;}

    public void standardization(int X, int Y){
        flag=true;
        if(ratio!=0){
            X=(int)(X/ratio);
            Y=(int)(Y/ratio-offset);
        }
        if(X>=150&&X<=280&&Y>=320&&Y<=480){
            x=215;y=490;destName="乙437";
        }else if(X>=280&&X<=520&&Y>=320&&Y<=480){
            x=400;y=490;destName="露天平台";
        }else if(X>=520&&X<=740&&Y>=320&&Y<=480){
            x=630;y=490;destName="乙435";
        }else if(X>=740&&X<=1020&&Y>=320&&Y<=480){
            x=880;y=490;destName="乙433";
        }else if(X>=1020&&Y>=320&&Y<=480){
            x=1050;y=490;destName="乙431";
        }else if(X>=0&&X<=90&&Y>=1020&&Y<=1180){
            x=45;y=1190;destName="甲401";
        }else if(X>=150&&X<=220&&Y>=1020&&Y<=1180){
            x=185;y=1190;destName="乙423";
        }else if(X>=220&&X<=340&&Y>=1020&&Y<=1180){
            x=280;y=1190;destName="乙421";
        }else if(X>=340&&X<=400&&Y>=1020&&Y<=1180){
            x=370;y=1190;destName="乙419";
        }else if(X>=400&&X<=640&&Y>=1020&&Y<=1180){
            x=520;y=1190;destName="乙413/5/7";
        }else if(X>=640&&X<=740&&Y>=1020&&Y<=1180){
            x=690;y=1190;destName="乙411";
        }else if(X>=740&&X<=1020&&Y>=1020&&Y<=1180){
            x=880;y=1190;destName="乙409";
        }else if(X>=1020&&Y>=1020&&Y<=1180){
            x=1050;y=1190;destName="乙407";
        }else if(X>=0&&X<=150&&Y>=1200&&Y<=1360){
            x=75;y=1190;destName="甲402";
        }else if(X>=400&&X<=520&&Y>=1200&&Y<=1360){
            x=460;y=1190;destName="乙410";
        }else if(X>=640&&X<=740&&Y>=1200&&Y<=1360){
            x=690;y=1190;destName="乙408";
        }else if(X>=740&&X<=1020&&Y>=1200&&Y<=1360){
            x=880;y=1190;destName="乙406";
        }else if(X>=1020&&Y>=1200&&Y<=1360){
            x=1050;y=1190;destName="乙404";
        }else{
            flag=false;
        }
    }

    public void getDigitalPoint(ArrayList<Integer> digitalX, ArrayList<Integer> digitalY){
        digitalX.add(120);
        digitalX.add(120);
        digitalY.add(490);
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
        Xarray.add(1080);
        Xarray.add(1080);
        Yarray.add(490);
        Yarray.add(1190);
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
                Xarray.add(120);
                Yarray.add(490);
                break;
            case 1:
                Xarray.add(120);
                Yarray.add(1190);
                break;
            case 2:
                Xarray.add(dest_X);
                Yarray.add(dest_Y);
            default:
                break;
        }
    }

    public int[] floydPath(int x, int y, int num){
        int length;
        int[][] data=new int[3][3];
        if(y==490){
            length=Math.abs(120-x);
            int temp[][]={
                    {0,     700,length},
                    {700,   0,  0},
                    {length,0,  0}
            };
            data=temp;
        }else if(y==1190){
            length=Math.abs(120-x);
            int temp[][]={
                    {0,   700,   0},
                    {700, 0,     length},
                    {0,   length,0}
            };
            data=temp;
        }
        FLOYD pathWay=new FLOYD(data);
        int path[] = pathWay.getpath(num,2);
        return path;
    }

    public boolean canMove(int deltaX, int deltaY, int initX, int initY){
        int nextX=initX+deltaX;
        int nextY=initY+deltaY;
        if(nextY==490&&nextX>0&&nextX<1080) return true;
        if(nextY==1190&&nextX>0&&nextX<1080) return true;
        if(nextX==120&&nextY>490&&nextY<1190) return true;
        return false;
    }

    public int[] canExit(int init_X, int init_Y){
        int[] temp = new int[4];
        if(init_Y==490&&init_X>1080-3*scale){
            temp[0]=1;
            temp[1]=100;
            temp[2]=init_Y;
            temp[3]=2;
            return temp;
        }
        if(init_Y==1190&&init_X>1080-3*scale){
            temp[0]=1;
            temp[1]=100;
            temp[2]=init_Y;
            temp[3]=2;
            return temp;
        }
        temp[0]=0;
        return temp;
    }

    public int goLeft(){return 3;}

    public int goRight(){return 2;}

    public int goUp(){return 3;}

    public int goDown(){return 3;}
}
