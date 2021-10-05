package com.nju.map.DigitalMap;

import com.nju.map.FLOYD;
import java.util.ArrayList;

/**
 * Created by YSL on 2017/9/24.
 */

public class LabThirdFloorTwo extends Map{
    private int x;
    private int y;
    private boolean flag;
    private String destName;
    private int scale = 15;
    private int mapsNum = 3;
    private float ratio;
    private float offset;

    public LabThirdFloorTwo(int screenWidth, int screenHeight){ratio=screenWidth/1080;if(ratio!=0)offset=screenHeight/2/ratio-840;}

    public void standardization(int X, int Y){
        flag=true;
        if(ratio!=0){
            X=(int)(X/ratio);
            Y=(int)(Y/ratio-offset);
        }
        if(X>=0&&X<=90&&Y>=320&&Y<=480){
            x=45;y=490;destName="乙431";
        }else if(X>=210&&X<=320&&Y>=320&&Y<=480){
            x=265;y=490;destName="乙429";
        }else if(X>=580&&X<=710&&Y>=320&&Y<=480){
            x=645;y=490;destName="丙417";
        }else if(X>=710&&X<=960&&Y>=320&&Y<=480){
            x=835;y=490;destName="丙415";
        }else if(X>=960&&X<=1080&&Y>=320&&Y<=480){
            x=1020;y=490;destName="丙413";
        }else if(X>=90&&X<=320&&Y>=500&&Y<=660){
            x=205;y=490;destName="乙418";
        }else if(X>=320&&X<=580&&Y>=500&&Y<=660){
            x=450;y=490;destName="乙416";
        }else if(X>=640&&X<=710&&Y>=500&&Y<=660){
            x=675;y=490;destName="丙422";
        }else if(X>=710&&X<=960&&Y>=500&&Y<=660){
            x=835;y=490;destName="丙420";
        }else if(X>=960&&X<=1080&&Y>=500&&Y<=660){
            x=1020;y=490;destName="丙418";
        }else if(X>=0&&X<=220&&Y>=710&&Y<=1180){
            x=110;y=1190;destName="乙407";
        }else if(X>=220&&X<=460&&Y>=710&&Y<=1180){
            x=340;y=1190;destName="乙405";
        }else if(X>=460&&X<=580&&Y>=710&&Y<=1180){
            x=520;y=1190;destName="乙403";
        }else if(X>=640&&X<=710&&Y>=710&&Y<=1180){
            x=675;y=1190;destName="乙401";
        }else if(X>=0&&X<=220&&Y>=1200&&Y<=1360){
            x=110;y=1190;destName="乙404";
        }else if(X>=220&&X<=460&&Y>=1200&&Y<=1360){
            x=340;y=1190;destName="露天平台";
        }else if(X>=460&&X<=580&&Y>=1200&&Y<=1360){
            x=520;y=1190;destName="乙402";
        }else {
            flag=false;
        }
    }

    public void getDigitalPoint(ArrayList<Integer> digitalX, ArrayList<Integer> digitalY){
        digitalX.add(610);
        digitalX.add(610);
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
        switch (nextIndex){
            case 1:
                Xarray.add(1080);
                Yarray.add(490);
                break;
            case 3:
                Xarray.add(0);
                Xarray.add(0);
                Yarray.add(490);
                Yarray.add(1190);
        }
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
                Xarray.add(610);
                Yarray.add(490);
                break;
            case 1:
                Xarray.add(610);
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
            length=Math.abs(610-x);
            int temp[][]={
                    {0,     700,length},
                    {700,   0,  0},
                    {length,0,  0}
            };
            data=temp;
        }else if(y==1190){
            length=Math.abs(610-x);
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
        if(nextY==1190&&nextX>0&&nextX<710) return true;
        if(nextX==610&&nextY>490&&nextY<1190) return true;
        return false;
    }

    public int[] canExit(int init_X, int init_Y){
        int[] temp = new int[4];
        if(init_Y==490&&init_X>1080-3*scale){
            temp[0]=1;
            temp[1]=100;
            temp[2]=init_Y;
            temp[3]=1;
            return temp;
        }
        if(init_Y==490&&init_X<3*scale){
            temp[0]=1;
            temp[1]=1080-3*scale;
            temp[2]=init_Y;
            temp[3]=3;
            return temp;
        }
        if(init_Y==1190&&init_X<3*scale){
            temp[0]=1;
            temp[1]=1080-3*scale;
            temp[2]=init_Y;
            temp[3]=3;
            return temp;
        }
        temp[0]=0;
        return temp;
    }

    public int goLeft(){return 3;}

    public int goRight(){return 1;}

    public int goUp(){return 2;}

    public int goDown(){return 2;}
}
