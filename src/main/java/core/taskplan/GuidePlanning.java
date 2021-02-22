package core.taskplan;

import com.mongodb.client.FindIterable;
import org.bson.Document;

import javax.swing.*;
import java.text.SimpleDateFormat;
import java.util.*;

import static java.lang.Math.*;

public class GuidePlanning {

    private int TimeZone = -8;                     //北京时区到世界时-8
    private double Re = 6371393;                  //地球半径，单位为：米
    private double AUtokm = 1.49597870e8;
    private double kmtom = 10e3;
    private double R_earth = 6378136.3;
    private double eccent = 0.08182;
    private double[] CamViewAll={4.45 * Math.PI / 180.0, 4.45 * Math.PI / 180.0, 8.65 * Math.PI / 180.0, 8.65 * Math.PI / 180.0};
    private double[] SatelliteManeuverEuler={30 * Math.PI / 180.0, 30 * Math.PI / 180.0, 30 * Math.PI / 180.0};

    /**
     * 引导任务规划
     * @param Orbitjson:轨道数据，从引导信息开始时刻开始，输入5分钟，300组数组
     * @param missionFlag：任务模式，missionFlag=0，inputData输入为J2000坐标系下x,y,z,vx,vy,vz。
     *                   missionFlag=1，inputData输入为偏移量，第一个数据为北向偏移量，向北为正，向南为负；第二个数据为向东偏移量，向东为正，向西为负
     * @param inputData：输入的任务数据，missionFlag=0时inputData有六个数据；missionFlag=1时inputData有二个数据
     * @return：输出为Map类型，key=0时表示不可见，value="";key=1时表示目标在视场范围内，不需要机动，value=""；key=2时表示需要机动，value="指令序列"
     */
    public Map<Integer,String> GuidePlanningII(FindIterable<Document> Orbitjson, int missionFlag, ArrayList<Double> inputData){

        Map<Integer,String> Result=new HashMap<>();

        //轨道数据
        ArrayList<Date> OrbitTimeDateList = new ArrayList<>();
        ArrayList<double[]> OrbitTimeList = new ArrayList<>();
        ArrayList<double[]> OrbitSatPositionGEIList = new ArrayList<>();
        ArrayList<double[]> OrbitSatVelocityGEIList = new ArrayList<>();
        ArrayList<double[]> OrbitSatPositionLLAList = new ArrayList<>();
        ArrayList<double[]> OrbitSatPositionECEFList=new ArrayList<>();

        int OrbitalDataNum = 0;
        try {
            for (Document document : Orbitjson) {
                try {
                    //读取轨道时间戳
                    Date time_point = document.getDate("time_point");
                    OrbitTimeDateList.add(OrbitalDataNum, time_point);
                    double[] OrbitTime_iList = new double[6];
                    OrbitTime_iList = DateToDouble(time_point);
                    OrbitTimeList.add(OrbitalDataNum, OrbitTime_iList);
                    //读取惯性系轨道位置
                    double[] SatPositionGEI = new double[3];
                    SatPositionGEI[0] = Double.parseDouble(document.get("P_x").toString());
                    SatPositionGEI[1] = Double.parseDouble(document.get("P_y").toString());
                    SatPositionGEI[2] = Double.parseDouble(document.get("P_z").toString());
                    OrbitSatPositionGEIList.add(OrbitalDataNum, SatPositionGEI);
                    //读取惯性系轨道速度
                    double[] SatVelocityGEI = new double[3];
                    SatVelocityGEI[0] = Double.parseDouble(document.get("Vx").toString());
                    SatVelocityGEI[1] = Double.parseDouble(document.get("Vy").toString());
                    SatVelocityGEI[2] = Double.parseDouble(document.get("Vz").toString());
                    OrbitSatVelocityGEIList.add(OrbitalDataNum, SatVelocityGEI);
                    //读取卫星经纬高
                    double[] SatPositionLLA = new double[3];
                    SatPositionLLA[0] = Double.parseDouble(document.get("lon").toString());
                    SatPositionLLA[1] = Double.parseDouble(document.get("lat").toString());
                    SatPositionLLA[2] = Double.parseDouble(document.get("H").toString());
                    OrbitSatPositionLLAList.add(OrbitalDataNum, SatPositionLLA);
                    //读取地心地固坐标系卫星位置
                    double[] SatPositionECEF=new double[3];
                    Document PositionECEFTemp= (Document) document.get("Position_EarthCenteredEarthFixed");
                    SatPositionECEF[0] = Double.parseDouble(PositionECEFTemp.get("P_x").toString());
                    SatPositionECEF[1] = Double.parseDouble(PositionECEFTemp.get("P_y").toString());
                    SatPositionECEF[2] = Double.parseDouble(PositionECEFTemp.get("P_z").toString());
                    OrbitSatPositionECEFList.add(OrbitalDataNum, SatPositionECEF);

                    //轨道个数加1
                    OrbitalDataNum = OrbitalDataNum + 1;
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (OrbitalDataNum == 0) {
            Result.put(0,"");
            return Result;
        }

        /*可见标志，0-不可见；1-在相机可见区域内，不需要生成指令；2-在成像区域内，需要生成指令*/
        int VisibilityAllFlag=0;
        Date GuideTime=OrbitTimeDateList.get(0);
        double[] TargetLLA=new double[3];

        double JD=JD(OrbitTimeList.get(0));
        /*任务类型：0-根据目标点进行观测；1-根据偏移量进行观测*/
        if (missionFlag == 0) {
            double Target_GEI[]={inputData.get(0),inputData.get(1),inputData.get(2)};
            double TargetVel_GEI[]={inputData.get(3),inputData.get(4),inputData.get(5)};
            double Target_ECEF[]=new double[3];
            ICRSToECEF(JD, Target_GEI, Target_ECEF);
            ECEFToLLA(Target_ECEF,TargetLLA);

            /*判断目标和卫星是否在同一侧*/
            int sideFlag=SideJudge(Target_ECEF, OrbitSatPositionECEFList.get(0));
            if (sideFlag == 1) {
                /*判断目标是否在相机视场范围内*/
               int visibilityFlag=VisibilityJudge_Cam(Target_ECEF, OrbitSatPositionECEFList.get(0),OrbitSatPositionLLAList.get(0));
               if (visibilityFlag == 1) {
                   VisibilityAllFlag=1;
               }else {
                   /*判断目标是否在成像区域内*/
                   int visibilityCamSatFlag=VisibilityJudge_CamAndSat(Target_ECEF, OrbitSatPositionECEFList.get(0),OrbitSatPositionLLAList.get(0));
                   if (visibilityCamSatFlag == 1) {
                       VisibilityAllFlag=2;
                   }else {
                       VisibilityAllFlag=0;
                   }
               }
            }else {
                VisibilityAllFlag=0;
            }

            /*判定机动后成像时间*/
            if (VisibilityAllFlag == 2) {
                int time_JiDong=VisibilityJudge_JiDongTime(Target_ECEF, OrbitSatPositionECEFList.get(0),OrbitSatPositionLLAList.get(0));
                Date datetemp=new Date(GuideTime.getTime()+time_JiDong*1000);
                GuideTime=datetemp;

                /*判断机动后是否可见*/
                int time_Long= (int) ((OrbitTimeDateList.get(OrbitTimeDateList.size()-1).getTime()-OrbitTimeDateList.get(0).getTime())/1000);
                if (time_JiDong > time_Long) {
                    VisibilityAllFlag=0;
                }else {
                    for (int i = 0; i < 3; i++) {
                        Target_GEI[i]=Target_GEI[i]+TargetVel_GEI[i]*time_JiDong;
                    }
                    ICRSToECEF(JD, Target_GEI, Target_ECEF);
                    ECEFToLLA(Target_ECEF,TargetLLA);
                    sideFlag=SideJudge(Target_ECEF, OrbitSatPositionECEFList.get(time_JiDong));
                    if (sideFlag == 1) {
                        int visibilityCamSatFlag=VisibilityJudge_CamAndSat(Target_ECEF, OrbitSatPositionECEFList.get(time_JiDong),OrbitSatPositionLLAList.get(time_JiDong));
                        if (visibilityCamSatFlag == 0) {
                            VisibilityAllFlag=0;
                        }
                    }else {
                        VisibilityAllFlag=0;
                    }
                }
            }
        }else {
            double Offset[]={inputData.get(0),inputData.get(1)};
            PointOffset(OrbitSatPositionLLAList.get(0),Offset,TargetLLA);
            VisibilityAllFlag=2;
        }


        /*输出返回值*/
        if (VisibilityAllFlag == 0) {
            Result.put(VisibilityAllFlag,"");
        }else if (VisibilityAllFlag == 1) {
            Result.put(VisibilityAllFlag,"");
        }else if (VisibilityAllFlag == 2) {
            //String Zhiling="FFFFFFFFFFFFFFFFFFFFFFFFFFFF";
            String ZhilingTou="EB90762599";
			String Zhiling="1B03C000002D100280210118AB10";
            /*15~18凝视跟踪持续时间，当量1us*/
            float chixuTime= (float) 600*1000000;
            String strtemptime = Integer.toHexString(Float.floatToIntBits(chixuTime));
            if (strtemptime.length() < 8) {
                for (int j = strtemptime.length() + 1; j <= 8; j++) {
                    strtemptime = "0" + strtemptime;
                }
            }
            /*19~22凝视点地理经度，单位度*/
            float lon= (float) TargetLLA[0];
            String strtemplon = Integer.toHexString(Float.floatToIntBits(lon));
            if (strtemplon.length() < 8) {
                for (int j = strtemplon.length() + 1; j <= 8; j++) {
                    strtemplon = "0" + strtemplon;
                }
            }
            /*23~26凝视点地理纬度，单位度*/
            float lat= (float) TargetLLA[1];
            String strtemplat = Integer.toHexString(Float.floatToIntBits(lat));
            if (strtemplat.length() < 8) {
                for (int j = strtemplat.length() + 1; j <= 8; j++) {
                    strtemplat = "0" + strtemplat;
                }
            }
            /*27~30凝视点高程，单位米*/
            float alt= (float) TargetLLA[2];
            String strtempalt = Integer.toHexString(Float.floatToIntBits(alt));
            if (strtempalt.length() < 8) {
                for (int j = strtempalt.length() + 1; j <= 8; j++) {
                    strtempalt = "0" + strtempalt;
                }
            }

            Zhiling=Zhiling+strtemptime+strtemplon+strtemplat+strtempalt;
            /*ISO校验*/
            String total = Zhiling + ISO(Zhiling);
            /**/
            total=total+"D386";
            for (int j = total.length() / 2; j < 62; j++) {
                total = total + "A5";
            }
            byte[] MainBuff = hexStringToBytes(total);
            int a=CRC16_CCITT_FALSE(MainBuff);
            String CRCCode = String.format("%04X", a).toUpperCase();
            if (CRCCode.length() > 4) {
                CRCCode = CRCCode.substring(CRCCode.length() - 4);
            } else if (CRCCode.length() < 4) {
                for (int j = CRCCode.length(); j < 4; j++) {
                    CRCCode = "0" + CRCCode;
                }
            }

            total = ZhilingTou + total + CRCCode;

            Result.put(VisibilityAllFlag,total.toUpperCase());
        }

        return Result;
    }

    //判定地面目标是否与卫星在同一侧
    //返回1表示在同一侧，返回0代表不在同一侧
    private int SideJudge(double Target_ECEF[], double SatPosition_ECEF[]) {
        double theta_max = Math.asin(Re / Math.sqrt(Math.pow(SatPosition_ECEF[0], 2) + Math.pow(SatPosition_ECEF[1], 2) + Math.pow(SatPosition_ECEF[2], 2)));
        double a = Target_ECEF[0] * SatPosition_ECEF[0] + Target_ECEF[1] * SatPosition_ECEF[1] + Target_ECEF[2] * SatPosition_ECEF[2];
        double b = Math.sqrt(Math.pow(Target_ECEF[0], 2) + Math.pow(Target_ECEF[1], 2) + Math.pow(Target_ECEF[2], 2));
        double c = Math.sqrt(Math.pow(SatPosition_ECEF[0], 2) + Math.pow(SatPosition_ECEF[1], 2) + Math.pow(SatPosition_ECEF[2], 2));
        double theta = Math.acos(a / (b * c));

        theta_max = PI / 2 - theta_max;

        int Side_Flag;
        if (theta_max >= theta)
            Side_Flag = 1;
        else
            Side_Flag = 0;
        return Side_Flag;
    }

    //判断目标点是否可见
    //返回1表示可见，返回0表示不可见
    private int VisibilityJudge(double Target_GEI[], double SatPosition_GEI[], double SatVelocity_GEI[], double ViewInstall[], double ViewAngle[], double Euler_Max[]) {
        int Visibility_Flag;

        double[] SatTarget_GEI = {Target_GEI[0] - SatPosition_GEI[0], Target_GEI[1] - SatPosition_GEI[1], Target_GEI[2] - SatPosition_GEI[2]};
        double[] SatTarget_ORF = new double[3];
        GEIToORF(SatPosition_GEI, SatVelocity_GEI, SatTarget_GEI, SatTarget_ORF);

        double theta_xz = Math.atan(SatTarget_ORF[0] / SatTarget_ORF[2]);
        double theta_yz = Math.atan(SatTarget_ORF[1] / SatTarget_ORF[2]);

        double OpticalTheta_xz = Math.atan(Math.cos(ViewInstall[0]) / Math.cos(ViewInstall[2]));
        double OpticalTheta_yz = Math.atan(Math.cos(ViewInstall[1]) / Math.cos(ViewInstall[2]));
        double ViewAng_Min = ViewAngle[0];
        for (int i = 1; i < 4; i++) {
            if (ViewAng_Min > ViewAngle[i])
                ViewAng_Min = ViewAngle[i];
        }
        double[] ViewTheta_xz = {OpticalTheta_xz - ViewAngle[2] - Euler_Max[1], OpticalTheta_xz + ViewAngle[2] + Euler_Max[1]};
        double[] ViewTheta_yz = {OpticalTheta_yz - ViewAngle[0] - Euler_Max[0], OpticalTheta_yz + ViewAngle[0] + Euler_Max[0]};
        //double[] ViewTheta_xz = {OpticalTheta_xz - ViewAng_Min - Euler_Max[1], OpticalTheta_xz + ViewAng_Min + Euler_Max[1]};
        //double[] ViewTheta_yz = {OpticalTheta_yz - ViewAng_Min - Euler_Max[0], OpticalTheta_yz + ViewAng_Min + Euler_Max[0]};
        //double[] ViewTheta_xz = {OpticalTheta_xz-   Euler_Max[1], OpticalTheta_xz +  Euler_Max[1]};
        //double[] ViewTheta_yz = {OpticalTheta_yz-   Euler_Max[0], OpticalTheta_yz +  Euler_Max[0]};

        if (theta_xz >= ViewTheta_xz[0] && theta_xz <= ViewTheta_xz[1] && theta_yz >= ViewTheta_yz[0] && theta_yz <= ViewTheta_yz[1])
            Visibility_Flag = 1;
        else
            Visibility_Flag = 0;

        return Visibility_Flag;
    }

    //判断目标点是否可见
    //返回1表示可见，返回0表示不可见
    private int VisibilityJudge_Cam(double Target_ECEF[], double SatPosition_ECEF[],double SatPosition_LLA[]) {
        int Visibility_Flag;

        double[] SatTarget_ECEF = {Target_ECEF[0] - SatPosition_ECEF[0], Target_ECEF[1] - SatPosition_ECEF[1], Target_ECEF[2] - SatPosition_ECEF[2]};
        double[] SatTarget_ESD = new double[3];
        ECEFToESD(SatPosition_LLA, SatTarget_ECEF, SatTarget_ESD);

        double theta_xz = Math.atan(SatTarget_ESD[0] / SatTarget_ESD[2]);
        double theta_yz = Math.atan(SatTarget_ESD[1] / SatTarget_ESD[2]);

        double OpticalTheta_xz = 0;
        double OpticalTheta_yz = 0;
        double[] ViewTheta_xz = {OpticalTheta_xz - (CamViewAll[2]*0.8), OpticalTheta_xz + (CamViewAll[2]*0.8)};
        double[] ViewTheta_yz = {OpticalTheta_yz - (CamViewAll[0]*0.8), OpticalTheta_yz + (CamViewAll[0]*0.8)};

        if (theta_xz >= ViewTheta_xz[0] && theta_xz <= ViewTheta_xz[1] && theta_yz >= ViewTheta_yz[0] && theta_yz <= ViewTheta_yz[1])
            Visibility_Flag = 1;
        else
            Visibility_Flag = 0;

        return Visibility_Flag;
    }

    //判断目标点是否可见
    //返回1表示可见，返回0表示不可见
    private int VisibilityJudge_CamAndSat(double Target_ECEF[], double SatPosition_ECEF[],double SatPosition_LLA[]) {
        int Visibility_Flag;

        double[] SatTarget_ECEF = {Target_ECEF[0] - SatPosition_ECEF[0], Target_ECEF[1] - SatPosition_ECEF[1], Target_ECEF[2] - SatPosition_ECEF[2]};
        double[] SatTarget_ESD = new double[3];
        ECEFToESD(SatPosition_LLA, SatTarget_ECEF, SatTarget_ESD);

        double theta_xz = Math.atan(SatTarget_ESD[0] / SatTarget_ESD[2]);
        double theta_yz = Math.atan(SatTarget_ESD[1] / SatTarget_ESD[2]);

        double OpticalTheta_xz = 0;
        double OpticalTheta_yz = 0;
        double[] ViewTheta_xz = {OpticalTheta_xz - (CamViewAll[2]*0.8)-SatelliteManeuverEuler[1], OpticalTheta_xz + (CamViewAll[2]*0.8)+SatelliteManeuverEuler[1]};
        double[] ViewTheta_yz = {OpticalTheta_yz - (CamViewAll[0]*0.8)-SatelliteManeuverEuler[0], OpticalTheta_yz + (CamViewAll[0]*0.8)+SatelliteManeuverEuler[0]};

        if (theta_xz >= ViewTheta_xz[0] && theta_xz <= ViewTheta_xz[1] && theta_yz >= ViewTheta_yz[0] && theta_yz <= ViewTheta_yz[1])
            Visibility_Flag = 1;
        else
            Visibility_Flag = 0;

        return Visibility_Flag;
    }

    //判断目标点是否可见
    //返回1表示可见，返回0表示不可见
    private int VisibilityJudge_JiDongTime(double Target_ECEF[], double SatPosition_ECEF[],double SatPosition_LLA[]) {
        int Visibility_Flag;

        double[] SatTarget_ECEF = {Target_ECEF[0] - SatPosition_ECEF[0], Target_ECEF[1] - SatPosition_ECEF[1], Target_ECEF[2] - SatPosition_ECEF[2]};
        double[] SatTarget_ESD = new double[3];
        ECEFToESD(SatPosition_LLA, SatTarget_ECEF, SatTarget_ESD);

        double theta_xz = Math.atan(SatTarget_ESD[0] / SatTarget_ESD[2]);
        double theta_yz = Math.atan(SatTarget_ESD[1] / SatTarget_ESD[2]);

        double jd_Ang=abs(theta_xz);
        if (jd_Ang < abs(theta_yz)) {
            jd_Ang=abs(theta_yz);
        }

        double AngAcc=0.00086*PI/180.0;
        double time=sqrt(jd_Ang/AngAcc);
        int timeInt= (int) ceil(time);

        return timeInt;
    }

    private void PointOffset(double Target_LLA[],double Offset[],double TargetNew_LLA[]){
        double lonPlus=(Offset[1]/(2*PI*Re*cos(Target_LLA[1]*PI/180.0)))*360.0;
        double latPlus=(Offset[0]/Re)*360.0;
        
        TargetNew_LLA[0]=Target_LLA[0]+lonPlus;
        TargetNew_LLA[1]=Target_LLA[1]+latPlus;
        TargetNew_LLA[2]=0;
        if (TargetNew_LLA[0] > 180) {
            TargetNew_LLA[0]=TargetNew_LLA[0]-360;
        }else if (TargetNew_LLA[0] < -180) {
            TargetNew_LLA[0]=TargetNew_LLA[0]+360;
        }
        if (TargetNew_LLA[1] > 90) {
            TargetNew_LLA[1]=180-TargetNew_LLA[1];
        }else if (TargetNew_LLA[1] < -90) {
            TargetNew_LLA[1]=-180+TargetNew_LLA[1];
        }
    }

    //惯性坐标系转到轨道坐标系，大椭圆轨道
    private void GEIToORF(double SatPosition_GEI[], double SatVelocity_GEI[], double Position_GEI[], double Position_ORF[]) {
        double r = Math.sqrt(Math.pow(SatPosition_GEI[0], 2) + Math.pow(SatPosition_GEI[1], 2) + Math.pow(SatPosition_GEI[2], 2));
        double v = Math.sqrt(Math.pow(SatVelocity_GEI[0], 2) + Math.pow(SatVelocity_GEI[1], 2) + Math.pow(SatVelocity_GEI[2], 2));
        double[] zs = {-SatPosition_GEI[0] / r, -SatPosition_GEI[1] / r, -SatPosition_GEI[2] / r};
        double[] xs = {SatVelocity_GEI[0] / v, SatVelocity_GEI[1] / v, SatVelocity_GEI[2] / v};
        double[] ys = new double[3];
        ys = VectorCross(zs, xs);
        //System.out.println(Double.toString(xs[0])+","+Double.toString(xs[1])+","+Double.toString(xs[2]));
        //System.out.println(Double.toString(ys[0])+","+Double.toString(ys[1])+","+Double.toString(ys[2]));
        //System.out.println(Double.toString(zs[0])+","+Double.toString(zs[1])+","+Double.toString(zs[2]));
        double r_ys=sqrt(pow(ys[0],2)+pow(ys[1],2)+pow(ys[2],2));
        ys[0]=ys[0]/r_ys;
        ys[1]=ys[1]/r_ys;
        ys[2]=ys[2]/r_ys;
        xs=VectorCross(ys,zs);
        /*
        double[][] OR = {{xs[0], ys[0], zs[0]},
                {xs[1], ys[1], zs[1]},
                {xs[2], ys[2], zs[2]}};
         */
        double[][] OR = {{xs[0], xs[1], xs[2]},
                {ys[0], ys[1], ys[2]},
                {zs[0], zs[1], zs[2]}};
        double[][] pS_GEI = {{Position_GEI[0]}, {Position_GEI[1]}, {Position_GEI[2]}};
        double[][] pS_ORF = new double[3][1];
        pS_ORF = MatrixMultiplication(OR, pS_GEI);
        Position_ORF[0] = pS_ORF[0][0];
        Position_ORF[1] = pS_ORF[1][0];
        Position_ORF[2] = pS_ORF[2][0];
    }

    //地固坐标系转到惯性坐标系
    private void ECEFToICRS(double JD, double position_ECEF[], double position_GEI[]) {
        double T = (JD - 2451545.0) / 36525.0;

        //岁差角
        double Zeta_A = 2.5976176 + 2306.0809506*T + 0.3019015*T*T + 0.0179663*T*T*T - 0.0000327*T*T*T*T - 0.0000002*T*T*T*T*T;//秒
        double Theta_A = 2004.1917476*T - 0.4269353*T*T - 0.041825*T*T*T - 0.0000601*T*T*T*T - 0.0000001*T*T*T*T*T;
        double Z_A = -2.5976176 + 2306.0803226*T + 1.094779*T*T + 0.0182273*T*T*T + 0.000047*T*T*T*T - 0.0000003*T*T*T*T*T;
        Zeta_A = Zeta_A/3600.0;//度
        Theta_A = Theta_A/3600.0;
        Z_A = Z_A/3600.0;
        //岁差矩阵
        double[][] R3Z_A={{cos(-Z_A*PI/180.0),sin(-Z_A*PI/180.0),0},
                {-sin(-Z_A*PI/180.0),cos(-Z_A*PI/180.0),0},
                {0,0,1}};
        double[][] R2Theta_A={{cos(Theta_A*PI/180.0),0,-sin(Theta_A*PI/180.0)},
                {0,1,0},
                {sin(Theta_A*PI/180.0),0,cos(Theta_A*PI/180.0)}};
        double[][] R3_Zeta_A={{cos(-Zeta_A*PI/180.0),sin(-Zeta_A*PI/180.0),0},
                {-sin(-Zeta_A*PI/180.0),cos(-Zeta_A*PI/180.0),0},
                {0,0,1}};
        double[][] PR=new double[3][3];
        double[][] PR_mid=new double[3][3];
        PR_mid=MatrixMultiplication(R3Z_A,R2Theta_A);
        PR=MatrixMultiplication(PR_mid,R3_Zeta_A);

        //章动计算
        double Epsilon_A = 84381.448 - 46.8150*T - 0.00059*T*T + 0.001813*T*T*T;
        Epsilon_A = Epsilon_A/3600.0;
        // http://blog.sina.com.cn/s/blog_852e40660100w1m6.html
        double L = 280.4665+36000.7698*T;
        double dL = 218.3165+481267.8813*T;
        double Omega = 125.04452-1934.136261*T;
        double DeltaPsi = -17.20*sin(Omega*PI/180.0)-1.32*sin(2*L*PI/180.0)-0.23*sin(2*dL*PI/180.0)+0.21*sin(2*Omega*PI/180.0);
        double DeltaEpsilon = 9.20*cos(Omega*PI/180.0)+0.57*cos(2*L*PI/180.0)+0.10*cos(2*dL*PI/180.0)-0.09*cos(2*Omega*PI/180.0);
        DeltaPsi = DeltaPsi/3600.0;
        DeltaEpsilon = DeltaEpsilon/3600.0;

        //章动矩阵
        double[][] R1_DEA={{1,0,0},
                {0,cos(-(DeltaEpsilon+Epsilon_A)*PI/180.0),sin(-(DeltaEpsilon+Epsilon_A)*PI/180.0)},
                {0,-sin(-(DeltaEpsilon+Epsilon_A)*PI/180.0),cos(-(DeltaEpsilon+Epsilon_A)*PI/180.0)}};
        double[][] R3_DeltaPsi={{cos(-DeltaPsi*PI/180.0),sin(-DeltaPsi*PI/180.0),0},
                {-sin(-DeltaPsi*PI/180.0),cos(-DeltaPsi*PI/180.0),0},
                {0,0,1}};
        double[][] R1_Epsilon={{1,0,0},
                {0,cos(Epsilon_A*PI/180.0),sin(Epsilon_A*PI/180.0)},
                {0,-sin(Epsilon_A*PI/180.0),cos(Epsilon_A*PI/180.0)}};
        double[][] NR=new double[3][3];
        double[][] NR_mid=new double[3][3];
        NR_mid=MatrixMultiplication(R1_DEA,R3_DeltaPsi);
        NR=MatrixMultiplication(NR_mid,R1_Epsilon);

        //地球自转
        double GMST = 280.46061837 + 360.98564736629*(JD-2451545.0) + 0.000387933*T*T - T*T*T/38710000.0;
        GMST = GMST%360;
        double GAST = GMST + DeltaPsi*cos((DeltaEpsilon + Epsilon_A)*PI/180.0);
        GAST = GAST%360;
        double[][] ER={{cos(GAST*PI/180.0),sin(GAST*PI/180.0),0},
                {-sin(GAST*PI/180.0),cos(GAST*PI/180.0),0},
                {0,0,1}};

        //极移坐标
        //  https://www.iers.org/IERS/EN/DataProducts/EarthOrientationData/eop.html
        // https://datacenter.iers.org/data/html/finals.all.html
        double Xp = 0.001674*0.955;
        double Yp = 0.001462*0.955;
        // 极移矩阵
        double[][] R1_YP={{1,0,0},
                {0,cos(-Yp*PI/180.0),sin(-Yp*PI/180.0)},
                {0,-sin(-Yp*PI/180.0),cos(-Yp*PI/180.0)}};
        double[][] R2_XP={{cos(-Xp*PI/180.0),0,-sin(-Xp*PI/180.0)},
                {0,1,0},
                {sin(-Xp*PI/180.0),0,cos(-Xp*PI/180.0)}};
        double[][] EP=new double[3][3];
        EP=MatrixMultiplication(R1_YP,R2_XP);

        // 空固坐标系到地固坐标系的转换矩阵
        double[][] EPER=new double[3][3];
        double[][] EPERNR=new double[3][3];
        double[][] ECEF;
        EPER=MatrixMultiplication(EP,ER);
        EPERNR=MatrixMultiplication(EPER,NR);
        ECEF=MatrixMultiplication(EPERNR,PR);
        //地固坐标系到惯性坐标系的转换矩阵
        double[][] R_inv = new double[3][3];
        R_inv = MatrixInverse(ECEF);
        double[][] p_ECEF = {{position_ECEF[0]}, {position_ECEF[1]}, {position_ECEF[2]}};
        double[][] pp_GEI = new double[3][1];
        pp_GEI = MatrixMultiplication(R_inv, p_ECEF);

        position_GEI[0] = pp_GEI[0][0];
        position_GEI[1] = pp_GEI[1][0];
        position_GEI[2] = pp_GEI[2][0];
    }

    //惯性坐标系转到地固坐标系
    private void ICRSToECEF(double JD, double position_GEI[], double position_ECEF[]) {
        double T = (JD - 2451545.0) / 36525.0;

        //岁差角
        double Zeta_A = 2.5976176 + 2306.0809506*T + 0.3019015*T*T + 0.0179663*T*T*T - 0.0000327*T*T*T*T - 0.0000002*T*T*T*T*T;//秒
        double Theta_A = 2004.1917476*T - 0.4269353*T*T - 0.041825*T*T*T - 0.0000601*T*T*T*T - 0.0000001*T*T*T*T*T;
        double Z_A = -2.5976176 + 2306.0803226*T + 1.094779*T*T + 0.0182273*T*T*T + 0.000047*T*T*T*T - 0.0000003*T*T*T*T*T;
        Zeta_A = Zeta_A/3600.0;//度
        Theta_A = Theta_A/3600.0;
        Z_A = Z_A/3600.0;
        //岁差矩阵
        double[][] R3Z_A={{cos(-Z_A*PI/180.0),sin(-Z_A*PI/180.0),0},
                {-sin(-Z_A*PI/180.0),cos(-Z_A*PI/180.0),0},
                {0,0,1}};
        double[][] R2Theta_A={{cos(Theta_A*PI/180.0),0,-sin(Theta_A*PI/180.0)},
                {0,1,0},
                {sin(Theta_A*PI/180.0),0,cos(Theta_A*PI/180.0)}};
        double[][] R3_Zeta_A={{cos(-Zeta_A*PI/180.0),sin(-Zeta_A*PI/180.0),0},
                {-sin(-Zeta_A*PI/180.0),cos(-Zeta_A*PI/180.0),0},
                {0,0,1}};
        double[][] PR=new double[3][3];
        double[][] PR_mid=new double[3][3];
        PR_mid=MatrixMultiplication(R3Z_A,R2Theta_A);
        PR=MatrixMultiplication(PR_mid,R3_Zeta_A);

        //章动计算
        double Epsilon_A = 84381.448 - 46.8150*T - 0.00059*T*T + 0.001813*T*T*T;
        Epsilon_A = Epsilon_A/3600.0;
        // http://blog.sina.com.cn/s/blog_852e40660100w1m6.html
        double L = 280.4665+36000.7698*T;
        double dL = 218.3165+481267.8813*T;
        double Omega = 125.04452-1934.136261*T;
        double DeltaPsi = -17.20*sin(Omega*PI/180.0)-1.32*sin(2*L*PI/180.0)-0.23*sin(2*dL*PI/180.0)+0.21*sin(2*Omega*PI/180.0);
        double DeltaEpsilon = 9.20*cos(Omega*PI/180.0)+0.57*cos(2*L*PI/180.0)+0.10*cos(2*dL*PI/180.0)-0.09*cos(2*Omega*PI/180.0);
        DeltaPsi = DeltaPsi/3600.0;
        DeltaEpsilon = DeltaEpsilon/3600.0;

        //章动矩阵
        double[][] R1_DEA={{1,0,0},
                {0,cos(-(DeltaEpsilon+Epsilon_A)*PI/180.0),sin(-(DeltaEpsilon+Epsilon_A)*PI/180.0)},
                {0,-sin(-(DeltaEpsilon+Epsilon_A)*PI/180.0),cos(-(DeltaEpsilon+Epsilon_A)*PI/180.0)}};
        double[][] R3_DeltaPsi={{cos(-DeltaPsi*PI/180.0),sin(-DeltaPsi*PI/180.0),0},
                {-sin(-DeltaPsi*PI/180.0),cos(-DeltaPsi*PI/180.0),0},
                {0,0,1}};
        double[][] R1_Epsilon={{1,0,0},
                {0,cos(Epsilon_A*PI/180.0),sin(Epsilon_A*PI/180.0)},
                {0,-sin(Epsilon_A*PI/180.0),cos(Epsilon_A*PI/180.0)}};
        double[][] NR=new double[3][3];
        double[][] NR_mid=new double[3][3];
        NR_mid=MatrixMultiplication(R1_DEA,R3_DeltaPsi);
        NR=MatrixMultiplication(NR_mid,R1_Epsilon);

        //地球自转
        double GMST = 280.46061837 + 360.98564736629*(JD-2451545.0) + 0.000387933*T*T - T*T*T/38710000.0;
        GMST = GMST%360;
        double GAST = GMST + DeltaPsi*cos((DeltaEpsilon + Epsilon_A)*PI/180.0);
        GAST = GAST%360;
        double[][] ER={{cos(GAST*PI/180.0),sin(GAST*PI/180.0),0},
                {-sin(GAST*PI/180.0),cos(GAST*PI/180.0),0},
                {0,0,1}};

        //极移坐标
        //  https://www.iers.org/IERS/EN/DataProducts/EarthOrientationData/eop.html
        // https://datacenter.iers.org/data/html/finals.all.html
        double Xp = 0.001674*0.955;
        double Yp = 0.001462*0.955;
        // 极移矩阵
        double[][] R1_YP={{1,0,0},
                {0,cos(-Yp*PI/180.0),sin(-Yp*PI/180.0)},
                {0,-sin(-Yp*PI/180.0),cos(-Yp*PI/180.0)}};
        double[][] R2_XP={{cos(-Xp*PI/180.0),0,-sin(-Xp*PI/180.0)},
                {0,1,0},
                {sin(-Xp*PI/180.0),0,cos(-Xp*PI/180.0)}};
        double[][] EP=new double[3][3];
        EP=MatrixMultiplication(R1_YP,R2_XP);

        // 空固坐标系到地固坐标系的转换矩阵
        double[][] EPER=new double[3][3];
        double[][] EPERNR=new double[3][3];
        double[][] ECEF;
        EPER=MatrixMultiplication(EP,ER);
        EPERNR=MatrixMultiplication(EPER,NR);
        ECEF=MatrixMultiplication(EPERNR,PR);

        double[][] p_GEI = {{position_GEI[0]}, {position_GEI[1]}, {position_GEI[2]}};
        double[][] pp_ECEF = new double[3][1];
        pp_ECEF = MatrixMultiplication(ECEF, p_GEI);

        position_ECEF[0] = pp_ECEF[0][0];
        position_ECEF[1] = pp_ECEF[1][0];
        position_ECEF[2] = pp_ECEF[2][0];
    }

    //地固坐标系到卫星东南地坐标系
    private void ECEFToESD(double[] Satellite_LLA, double[] Target_ECEF, double[] Target_ESD) {
        double B = Satellite_LLA[1] * Math.PI / 180.0;//经度
        double L = Satellite_LLA[0] * Math.PI / 180.0;//纬度
        double[][] R_ECEFToNED = {{-sin(B) * cos(L), -sin(B) * sin(L), cos(B)},
                {-sin(L), cos(L), 0},
                {-cos(B) * cos(L), -cos(B) * sin(L), -sin(B)}};
        double[][] Target_ECEF_mid = new double[3][1];
        Target_ECEF_mid[0][0] = Target_ECEF[0];
        Target_ECEF_mid[1][0] = Target_ECEF[1];
        Target_ECEF_mid[2][0] = Target_ECEF[2];
        double[][] Target_NED_mid = new double[3][1];
        Target_NED_mid = MatrixMultiplication(R_ECEFToNED, Target_ECEF_mid);
        double Ang_z = -PI / 2;
        double[][] R_NEDToESD = {{cos(Ang_z), -sin(Ang_z), 0},
                {sin(Ang_z), cos(Ang_z), 0},
                {0, 0, 1}};
        double[][] Target_ESD_mid = new double[3][1];
        Target_ESD_mid = MatrixMultiplication(R_NEDToESD, Target_NED_mid);
        Target_ESD[0] = Target_ESD_mid[0][0];
        Target_ESD[1] = Target_ESD_mid[1][0];
        Target_ESD[2] = Target_ESD_mid[2][0];
    }

    //惯性系转地心地固
    private void ECEFToLLA(double[] ECEF, double[] LLA) {
        double Range, Radius, sphi, x, y, z;
        //calculate geocentric lon, lat in radians
        x = ECEF[0];
        y = ECEF[1];
        z = ECEF[2];
        Range = x * x + y * y + z * z;
        Radius = Math.sqrt(Range);
        LLA[0] = Math.atan2(y, x)*180.0/PI;//GeoLon
        sphi = z / Radius;
        LLA[1] = Math.asin(sphi)*180.0/PI;//GeoLat
        //Altitude
        //*(sa+2) = Radius - R_earth;
        LLA[2] = Radius - Math.sqrt(R_earth * R_earth * (1.0 - eccent * eccent) / (1.0 - Math.pow(eccent * Math.cos(LLA[1]), 2)));
    }

    //儒略日计算
    private double JD(double Time[]) {
        double year_UT = Time[0];
        double month_UT = Time[1];
        double day_UT = Time[2];
        double hour_UT = Time[3];
        double minute_UT = Time[4];
        double second_UT = Time[5];

        double D = day_UT;
        double M, Y, B;
        double JD;
        if (month_UT == 1 || month_UT == 2) {
            M = month_UT + 12;
            Y = year_UT - 1;
        } else {
            Y = year_UT;
            M = month_UT;
        }
        B = 0;
        if (Y > 1582 || (Y == 1582 && M > 10) || (Y == 1582 && M == 10 && D >= 15)) {
            B = 2 - (int) Math.floor(Y / 100.0) + (int) Math.floor(Y / 400.0);
        }
        JD = (int) Math.floor(365.25 * (Y + 4716)) + (int) Math.floor(30.6 * (M + 1)) + D + B - 1524.5;
        JD = JD - 0.5 + hour_UT / 24.0 + minute_UT / 1440 + second_UT / 86400;
        JD = JD + 0.5;
        return JD;
    }

    public void LLAToGEI(double Time[],double position_LLA[],double position_GEI[]){
        double jd=JD(Time);
        double Position_ECEF[]=new double[3];
        LLAToECEF(position_LLA, Position_ECEF);
        ECEFToICRS(jd, Position_ECEF, position_GEI);
    }

    //地固直角坐标系转换为地心地固坐标系
    private void LLAToECEF(double Position_LLA[], double Position_ECEF[]) {
        double L = Position_LLA[0] * Math.PI / 180.0;
        double B = Position_LLA[1] * Math.PI / 180.0;
        double H = Position_LLA[2];

        Position_ECEF[0] = (Re + H) * Math.cos(B) * Math.cos(L);
        Position_ECEF[1] = (Re + H) * Math.cos(B) * Math.sin(L);
        Position_ECEF[2] = (Re + H) * Math.sin(B);
    }

    //矩阵乘法
    private double[][] MatrixMultiplication(double A[][], double B[][]) {
        int A_rowNum = A.length;
        int A_columnNum = A[0].length;
        int B_rowNum = B.length;
        int B_columnNum = B[0].length;
        if (A_columnNum != B_rowNum)
            JOptionPane.showMessageDialog(null, "乘法矩阵维数不一致", "矩阵乘法错误", JOptionPane.ERROR_MESSAGE);
        double[][] Result = new double[A_rowNum][B_columnNum];
        for (int i = 0; i < A_rowNum; i++) {
            for (int j = 0; j < B_columnNum; j++) {
                Result[i][j] = 0;
                for (int k = 0; k < A_columnNum; k++)
                    Result[i][j] = Result[i][j] + A[i][k] * B[k][j];
            }
        }
        return Result;
    }

    //矩阵求逆
    private double[][] MatrixInverse(double A[][]) {
        int A_rowNum = A.length;
        int A_columnNum = A[0].length;
        if (A_rowNum != A_columnNum)
            JOptionPane.showMessageDialog(null, "求逆矩阵不是方阵", "矩阵求逆错误", JOptionPane.ERROR_MESSAGE);

        double[][] Result = new double[A_rowNum][A_columnNum];
        double Matrix_R = MatrixResult(A);
        if (Matrix_R == 0)
            JOptionPane.showMessageDialog(null, "求逆矩阵的值为零", "矩阵求逆错误", JOptionPane.ERROR_MESSAGE);
        for (int i = 0; i < A_rowNum; i++) {
            for (int j = 0; j < A_columnNum; j++) {
                if ((i + j) % 2 == 0) {
                    Result[i][j] = MatrixResult(MatrixCofactor(A, i + 1, j + 1)) / Matrix_R;
                } else {
                    Result[i][j] = -MatrixResult(MatrixCofactor(A, i + 1, j + 1)) / Matrix_R;
                }
            }
        }
        Result = MatrixTransposition(Result);
        return Result;
    }

    //求矩阵(h,v)位置的余子式，用于矩阵求逆
    private double[][] MatrixCofactor(double[][] A, int h, int v) {
        int A_rowNum = A.length;
        int A_columnNum = A[0].length;
        if (A_rowNum != A_columnNum)
            JOptionPane.showMessageDialog(null, "求余子式矩阵不是方阵", "矩阵求余子式错误", JOptionPane.ERROR_MESSAGE);
        double[][] Cofactor = new double[A_rowNum - 1][A_columnNum - 1];
        for (int i = 0; i < A_rowNum - 1; i++) {
            if (i < h - 1) {
                for (int j = 0; j < A_columnNum - 1; j++) {
                    if (j < v - 1)
                        Cofactor[i][j] = A[i][j];
                    else
                        Cofactor[i][j] = A[i][j + 1];
                }
            } else {
                for (int j = 0; j < A_columnNum - 1; j++) {
                    if (j < v - 1)
                        Cofactor[i][j] = A[i + 1][j];
                    else
                        Cofactor[i][j] = A[i + 1][j + 1];
                }
            }
        }
        return Cofactor;
    }

    //计算行列式的值
    private double MatrixResult(double A[][]) {
        int A_rowNum = A.length;
        int A_columnNum = A[0].length;
        if (A_rowNum != A_columnNum)
            JOptionPane.showMessageDialog(null, "求矩阵值的矩阵不是方阵", "矩阵求值错误", JOptionPane.ERROR_MESSAGE);
        //一维矩阵计算
        if (A_rowNum == 1)
            return A[0][0];

        //二维矩阵计算
        if (A_rowNum == 2)
            return A[0][0] * A[1][1] - A[0][1] * A[1][0];

        //计算二维以上矩阵
        double result = 0;
        double[] nums = new double[A_rowNum];
        for (int i = 0; i < A_rowNum; i++) {
            if (i % 2 == 0) {
                nums[i] = A[0][i] * MatrixResult(MatrixCofactor(A, 1, i + 1));
            } else {
                nums[i] = -A[0][i] * MatrixResult(MatrixCofactor(A, 1, i + 1));
            }
        }
        for (int i = 0; i < A_rowNum; i++)
            result = result + nums[i];

        return result;
    }

    //矩阵的转置
    private double[][] MatrixTransposition(double A[][]) {
        double[][] Result = new double[A[0].length][A.length];
        for (int i = 0; i < A.length; i++) {
            for (int j = 0; j < A[0].length; j++)
                Result[j][i] = A[i][j];
        }
        return Result;
    }

    //矢量叉乘
    private double[] VectorCross(double A[], double B[]) {
        if (A.length != 3 || B.length != 3)
            JOptionPane.showMessageDialog(null, "求矢量的叉乘输入不合法", "求矢量叉乘错误", JOptionPane.ERROR_MESSAGE);
        double[] Result = new double[3];
        Result[0] = A[1] * B[2] - A[2] * B[1];
        Result[1] = A[2] * B[0] - A[0] * B[2];
        Result[2] = A[0] * B[1] - A[1] * B[0];

        return Result;
    }

    private double[] DateToDouble(Date Time_Date) {
        //时间转换为doubule型
        double[] Time = new double[6];
        Calendar cal = Calendar.getInstance();
        cal.setTime(Time_Date);
        cal.add(Calendar.HOUR_OF_DAY, TimeZone);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String StringTime = sdf.format(cal.getTime());
        Time[0] = Double.parseDouble(StringTime.substring(0, 4));
        Time[1] = Double.parseDouble(StringTime.substring(5, 7));
        Time[2] = Double.parseDouble(StringTime.substring(8, 10));
        Time[3] = Double.parseDouble(StringTime.substring(11, 13));
        Time[4] = Double.parseDouble(StringTime.substring(14, 16));
        Time[5] = Double.parseDouble(StringTime.substring(17, 19));

        return Time;
    }

    //ISO和校验算法
    private String ISO(String Frame) {
        int C0 = 0;
        int C1 = 0;
        for (int i = 0; i < Frame.length(); i = i + 2) {
            int B = Integer.parseInt(Frame.substring(i, i + 2), 16);
            C0 = (C0 + B) % 255;
            C1 = (C1 + C0) % 255;
        }
//        int CK1 = 0 - ((C0 + C1) % 255);
        int CK1 = (-(C0 + C1)) % 255;
        if (CK1 < 0) {
            CK1 = CK1 + 255;
        }
        int CK2 = C1;
        String CK1tot = String.format("%02X", CK1).toUpperCase();
        String CK2tot = String.format("%02X", CK2).toUpperCase();
        String CK1str = CK1tot;
        String CK2str = CK2tot;
        if (CK1tot.length() > 2) {
            CK1str = CK1tot.substring(CK1tot.length() - 2);
        }
        if (CK2tot.length() > 2) {
            CK2str = CK2tot.substring(CK2tot.length() - 2);
        }
        if (CK1str.equals("00")) {
            CK1str = "FF";
        }
        if (CK2str.equals("00")) {
            CK2str = "FF";
        }
        return CK1str + CK2str;
    }

    private int CRC16_CCITT_FALSE(byte[] buffer) {
        int wCRCin = 0xffff;
        int wCPoly = 0x1021;
        for (byte b : buffer) {
            for (int i = 0; i < 8; i++) {
                boolean bit = ((b >> (7 - i) & 1) == 1);
                boolean c15 = ((wCRCin >> 15 & 1) == 1);
                wCRCin <<= 1;
                if (c15 ^ bit)
                    wCRCin ^= wCPoly;
            }
        }
        wCRCin &= 0xffff;
        return wCRCin ^= 0x0000;
    }

    //16进制字符串转化为byte[]数组
    private byte[] hexStringToBytes(String hexString) {
        if (hexString == null || hexString.equals("")) {
            return null;
        }
        hexString = hexString.toUpperCase();
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] d = new byte[length];
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
        }
        return d;
    }

    //char字符数据类型转化为byte字节数据类型
    private byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }
}
