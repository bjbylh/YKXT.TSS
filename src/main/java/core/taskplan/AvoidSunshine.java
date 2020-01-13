package core.taskplan;

import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import common.mongo.MangoDBConnector;
import org.bson.Document;

import javax.swing.*;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import static java.lang.Math.*;

public class AvoidSunshine {
    private static double AUtokm = 1.49597870e8;
    private static double kmtom = 10e3;
    private static int TimeZone = -8;                     //北京时区到世界时-8

    //阳光规避弧段
    private static int[] SunAvoidTimePeriod=new int[10];
    private static int SunAvoidTimePeriodNum;

    public static void AvoidSunshineII(FindIterable<Document> Orbitjson, long orbitDataCount,Instant timeRaw ){
        //读入模板
        //连接数据库
        MongoClient mongoClient = MangoDBConnector.getClient();
        //获取名为"temp"的数据库
        MongoDatabase mongoDatabase = mongoClient.getDatabase("temp");
        //获取名为“satellite_resource”的表
        MongoCollection<Document> sate_res=mongoDatabase.getCollection("satellite_resource");
        //获取的表存在Document中
        Document first=sate_res.find().first();
        //将表中properties内容存入properties列表中
        ArrayList<Document> properties=(ArrayList<Document>) first.get("properties");
        double sun_angle_threshold=25*PI/180.0;//阳光夹角门限
        double sun_middle_day_duration=1800;//正午规避时长
        for (Document document:properties){
            if (document.getString("key").equals("sun_angle_threshold")){
                sun_angle_threshold=Double.parseDouble(document.get("value").toString())*PI/180.0;
            }else if (document.getString("key").equals("sun_middle_day_duration")) {
                sun_middle_day_duration=Double.parseDouble(document.get("value").toString());
            }
        }

        //读入轨道数据
        //轨道数据
        ArrayList<Date> OrbitTimeDateList = new ArrayList<>();
        ArrayList<double[]> OrbitTimeList = new ArrayList<>();
        ArrayList<double[]> OrbitSatPositionGEIList = new ArrayList<>();
        ArrayList<double[]> OrbitSatVelocityGEIList = new ArrayList<>();
        ArrayList<double[]> OrbitSatPositionLLAList = new ArrayList<>();

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

        Boolean SatFlyFlag_tBefore=true;//卫星正飞true/倒飞false判定
        Boolean SatFlyFlag_t=true;
        Boolean SatSunFlag_tBefore=false;
        Boolean SatSunFlag_t=false;
        //初始值
        if (OrbitalDataNum > 0) {
            Boolean MiddleNightFlag=false;
            //判断当前时刻是白天false/黑夜true
            double Time_JD=JD(OrbitTimeList.get(0));
            double[] r_sun=new double[3];//地心惯性坐标系下太阳位置
            double[] su=new double[2];//赤经和赤纬
            double rad_sun;//太阳地球的距离
            rad_sun=Sun(Time_JD,r_sun,su);
            double a=r_sun[0]* OrbitSatPositionGEIList.get(0)[0]+r_sun[1]*OrbitSatPositionGEIList.get(0)[1]+r_sun[2]*OrbitSatPositionGEIList.get(0)[2];
            double r_Sat=sqrt(OrbitSatPositionGEIList.get(0)[0]*OrbitSatPositionGEIList.get(0)[0]+OrbitSatPositionGEIList.get(0)[1]*OrbitSatPositionGEIList.get(0)[1]+OrbitSatPositionGEIList.get(0)[2]*OrbitSatPositionGEIList.get(0)[2]);
            double r_Sun=sqrt(r_sun[0]*r_sun[0]+r_sun[1]*r_sun[1]+r_sun[2]*r_sun[2]);
            double theta=acos(a/(r_Sun*r_Sat));
            if (theta <= PI/2) {
                MiddleNightFlag=false;
            }else {
                MiddleNightFlag=true;
            }
            if (MiddleNightFlag) {
                //午夜规避
                //卫星飞行方向
                double[] r_sun_n=new double[]{r_sun[0]/sqrt(r_sun[0]*r_sun[0]+r_sun[1]*r_sun[1]+r_sun[2]*r_sun[2]),
                        r_sun[1]/sqrt(r_sun[0]*r_sun[0]+r_sun[1]*r_sun[1]+r_sun[2]*r_sun[2]),
                        r_sun[2]/sqrt(r_sun[0]*r_sun[0]+r_sun[1]*r_sun[1]+r_sun[2]*r_sun[2])};
                double[] v_sat_n=new double[]{OrbitSatVelocityGEIList.get(0)[0]/sqrt(OrbitSatVelocityGEIList.get(0)[0]*OrbitSatVelocityGEIList.get(0)[0]+OrbitSatVelocityGEIList.get(0)[1]*OrbitSatVelocityGEIList.get(0)[1]+OrbitSatVelocityGEIList.get(0)[2]*OrbitSatVelocityGEIList.get(0)[2]),
                        OrbitSatVelocityGEIList.get(0)[1]/sqrt(OrbitSatVelocityGEIList.get(0)[0]*OrbitSatVelocityGEIList.get(0)[0]+OrbitSatVelocityGEIList.get(0)[1]*OrbitSatVelocityGEIList.get(0)[1]+OrbitSatVelocityGEIList.get(0)[2]*OrbitSatVelocityGEIList.get(0)[2]),
                        OrbitSatVelocityGEIList.get(0)[2]/sqrt(OrbitSatVelocityGEIList.get(0)[0]*OrbitSatVelocityGEIList.get(0)[0]+OrbitSatVelocityGEIList.get(0)[1]*OrbitSatVelocityGEIList.get(0)[1]+OrbitSatVelocityGEIList.get(0)[2]*OrbitSatVelocityGEIList.get(0)[2])};
                double CosTheta_SunVel=(r_sun_n[0]*v_sat_n[0]+r_sun_n[1]*v_sat_n[1]+r_sun_n[2]*v_sat_n[2])/
                        (sqrt(r_sun_n[0]*r_sun_n[0]+r_sun_n[1]*r_sun_n[1]+r_sun_n[2]*r_sun_n[2])*sqrt(v_sat_n[0]*v_sat_n[0]+v_sat_n[1]*v_sat_n[1]+v_sat_n[2]*v_sat_n[2]));
                if (CosTheta_SunVel > 0) {
                    SatFlyFlag_tBefore=false;
                    SatFlyFlag_t=false;
                }else {
                    SatFlyFlag_tBefore=true;
                    SatFlyFlag_t=true;
                }
                //惯性系下卫星到太阳的矢量
                double[] r_SatToSun_GEI=new double[]{r_sun[0]-OrbitSatPositionGEIList.get(0)[0],r_sun[1]-OrbitSatPositionGEIList.get(0)[1],r_sun[2]-OrbitSatPositionGEIList.get(0)[2]};
                //轨道系下卫星到太阳的矢量
                double[] r_SatToSun_ORF=new double[3];
                GEIToORF_Ellipse(OrbitSatPositionGEIList.get(0), OrbitSatVelocityGEIList.get(0), r_SatToSun_GEI, r_SatToSun_ORF);
                double r_SatToSun=sqrt(r_SatToSun_ORF[0]*r_SatToSun_ORF[0]+r_SatToSun_ORF[1]*r_SatToSun_ORF[1]+r_SatToSun_ORF[2]*r_SatToSun_ORF[2]);
                double[] r_SatToSun_n_ORF=new double[]{r_SatToSun_ORF[0]/r_SatToSun,r_SatToSun_ORF[1]/r_SatToSun,r_SatToSun_ORF[2]/r_SatToSun};
                double Theta_SatToSunxz=atan2(r_SatToSun_n_ORF[0],r_SatToSun_n_ORF[2]);
                if (abs(Theta_SatToSunxz) < sun_angle_threshold) {
                    SatSunFlag_tBefore=true;
                    SatSunFlag_t=true;
                }else {
                    SatSunFlag_tBefore=false;
                    SatSunFlag_t=false;
                }
            }else {
                SatSunFlag_tBefore=false;
                SatSunFlag_t=false;
                //正午规避
                double[] r_sun_n=new double[]{r_sun[0]/sqrt(r_sun[0]*r_sun[0]+r_sun[1]*r_sun[1]+r_sun[2]*r_sun[2]),
                        r_sun[1]/sqrt(r_sun[0]*r_sun[0]+r_sun[1]*r_sun[1]+r_sun[2]*r_sun[2]),
                        r_sun[2]/sqrt(r_sun[0]*r_sun[0]+r_sun[1]*r_sun[1]+r_sun[2]*r_sun[2])};
                double[] v_sat_n=new double[]{OrbitSatVelocityGEIList.get(0)[0]/sqrt(OrbitSatVelocityGEIList.get(0)[0]*OrbitSatVelocityGEIList.get(0)[0]+OrbitSatVelocityGEIList.get(0)[1]*OrbitSatVelocityGEIList.get(0)[1]+OrbitSatVelocityGEIList.get(0)[2]*OrbitSatVelocityGEIList.get(0)[2]),
                        OrbitSatVelocityGEIList.get(0)[1]/sqrt(OrbitSatVelocityGEIList.get(0)[0]*OrbitSatVelocityGEIList.get(0)[0]+OrbitSatVelocityGEIList.get(0)[1]*OrbitSatVelocityGEIList.get(0)[1]+OrbitSatVelocityGEIList.get(0)[2]*OrbitSatVelocityGEIList.get(0)[2]),
                        OrbitSatVelocityGEIList.get(0)[2]/sqrt(OrbitSatVelocityGEIList.get(0)[0]*OrbitSatVelocityGEIList.get(0)[0]+OrbitSatVelocityGEIList.get(0)[1]*OrbitSatVelocityGEIList.get(0)[1]+OrbitSatVelocityGEIList.get(0)[2]*OrbitSatVelocityGEIList.get(0)[2])};
                double CosTheta_SunVel=(r_sun_n[0]*v_sat_n[0]+r_sun_n[1]*v_sat_n[1]+r_sun_n[2]*v_sat_n[2])/
                        (sqrt(r_sun_n[0]*r_sun_n[0]+r_sun_n[1]*r_sun_n[1]+r_sun_n[2]*r_sun_n[2])*sqrt(v_sat_n[0]*v_sat_n[0]+v_sat_n[1]*v_sat_n[1]+v_sat_n[2]*v_sat_n[2]));
                if (CosTheta_SunVel > 0) {
                    SatFlyFlag_tBefore=false;
                    SatFlyFlag_t=false;
                }else {
                    SatFlyFlag_tBefore=true;
                    SatFlyFlag_t=true;
                }
            }
        }

        ArrayList<int[]> SunAvoidTimePeriodDayList=new ArrayList<>();
        ArrayList<int[]> SunAvoidTimePeriodNightList=new ArrayList<>();
        int[] SunAvoidTimePeriodNightListChild=new int[2];
        for (int i = 0; i < OrbitalDataNum; i++) {
            Boolean MiddleNightFlag=false;
            if (i>=OrbitTimeList.size() || i>=OrbitSatPositionGEIList.size() || i>OrbitSatVelocityGEIList.size()) {
                break;
            }
            //判断当前时刻是白天false/黑夜true
            double Time_JD=JD(OrbitTimeList.get(i));
            double[] r_sun=new double[3];//地心惯性坐标系下太阳位置
            double[] su=new double[2];//赤经和赤纬
            double rad_sun;//太阳地球的距离
            rad_sun=Sun(Time_JD,r_sun,su);
            double a=r_sun[0]* OrbitSatPositionGEIList.get(i)[0]+r_sun[1]*OrbitSatPositionGEIList.get(i)[1]+r_sun[2]*OrbitSatPositionGEIList.get(i)[2];
            double r_Sat=sqrt(OrbitSatPositionGEIList.get(i)[0]*OrbitSatPositionGEIList.get(i)[0]+OrbitSatPositionGEIList.get(i)[1]*OrbitSatPositionGEIList.get(i)[1]+OrbitSatPositionGEIList.get(i)[2]*OrbitSatPositionGEIList.get(i)[2]);
            double r_Sun=sqrt(r_sun[0]*r_sun[0]+r_sun[1]*r_sun[1]+r_sun[2]*r_sun[2]);
            double theta=acos(a/(r_Sun*r_Sat));
            if (theta <= PI/2) {
                MiddleNightFlag=false;
            }else {
                MiddleNightFlag=true;
            }
            if (MiddleNightFlag) {
                //午夜规避
                //卫星飞行方向
                double[] r_sun_n=new double[]{r_sun[0]/sqrt(r_sun[0]*r_sun[0]+r_sun[1]*r_sun[1]+r_sun[2]*r_sun[2]),
                        r_sun[1]/sqrt(r_sun[0]*r_sun[0]+r_sun[1]*r_sun[1]+r_sun[2]*r_sun[2]),
                        r_sun[2]/sqrt(r_sun[0]*r_sun[0]+r_sun[1]*r_sun[1]+r_sun[2]*r_sun[2])};
                double[] v_sat_n=new double[]{OrbitSatVelocityGEIList.get(i)[0]/sqrt(OrbitSatVelocityGEIList.get(i)[0]*OrbitSatVelocityGEIList.get(i)[0]+OrbitSatVelocityGEIList.get(i)[1]*OrbitSatVelocityGEIList.get(i)[1]+OrbitSatVelocityGEIList.get(i)[2]*OrbitSatVelocityGEIList.get(i)[2]),
                        OrbitSatVelocityGEIList.get(i)[1]/sqrt(OrbitSatVelocityGEIList.get(i)[0]*OrbitSatVelocityGEIList.get(i)[0]+OrbitSatVelocityGEIList.get(i)[1]*OrbitSatVelocityGEIList.get(i)[1]+OrbitSatVelocityGEIList.get(i)[2]*OrbitSatVelocityGEIList.get(i)[2]),
                        OrbitSatVelocityGEIList.get(i)[2]/sqrt(OrbitSatVelocityGEIList.get(i)[0]*OrbitSatVelocityGEIList.get(i)[0]+OrbitSatVelocityGEIList.get(i)[1]*OrbitSatVelocityGEIList.get(i)[1]+OrbitSatVelocityGEIList.get(i)[2]*OrbitSatVelocityGEIList.get(i)[2])};
                double CosTheta_SunVel=(r_sun_n[0]*v_sat_n[0]+r_sun_n[1]*v_sat_n[1]+r_sun_n[2]*v_sat_n[2])/
                        (sqrt(r_sun_n[0]*r_sun_n[0]+r_sun_n[1]*r_sun_n[1]+r_sun_n[2]*r_sun_n[2])*sqrt(v_sat_n[0]*v_sat_n[0]+v_sat_n[1]*v_sat_n[1]+v_sat_n[2]*v_sat_n[2]));
                if (CosTheta_SunVel > 0) {
                    SatFlyFlag_tBefore=SatFlyFlag_t;
                    SatFlyFlag_t=false;
                }else {
                    SatFlyFlag_tBefore=SatFlyFlag_t;
                    SatFlyFlag_t=true;
                }
                //惯性系下卫星到太阳的矢量
                double[] r_SatToSun_GEI=new double[]{r_sun[0]-OrbitSatPositionGEIList.get(i)[0],r_sun[1]-OrbitSatPositionGEIList.get(i)[1],r_sun[2]-OrbitSatPositionGEIList.get(i)[2]};
                //轨道系下卫星到太阳的矢量
                double[] r_SatToSun_ORF=new double[3];
                GEIToORF_Ellipse(OrbitSatPositionGEIList.get(i), OrbitSatVelocityGEIList.get(i), r_SatToSun_GEI, r_SatToSun_ORF);
                double r_SatToSun=sqrt(r_SatToSun_ORF[0]*r_SatToSun_ORF[0]+r_SatToSun_ORF[1]*r_SatToSun_ORF[1]+r_SatToSun_ORF[2]*r_SatToSun_ORF[2]);
                double[] r_SatToSun_n_ORF=new double[]{r_SatToSun_ORF[0]/r_SatToSun,r_SatToSun_ORF[1]/r_SatToSun,r_SatToSun_ORF[2]/r_SatToSun};
                double Theta_SatToSunxz=atan2(r_SatToSun_n_ORF[0],r_SatToSun_n_ORF[2]);
                if (abs(Theta_SatToSunxz) < sun_angle_threshold) {
                    SatSunFlag_tBefore=SatSunFlag_t;
                    SatSunFlag_t=true;
                }else {
                    SatSunFlag_tBefore=SatSunFlag_t;
                    SatSunFlag_t=false;
                }
                if (SatSunFlag_tBefore == false && SatSunFlag_t==true) {
                    SunAvoidTimePeriodNightListChild[0]=i-1;
                    if (SunAvoidTimePeriodNightListChild[0] < 0) {
                        SunAvoidTimePeriodNightListChild[0]=0;
                    }
                }else if (SatSunFlag_tBefore == true && SatSunFlag_t==false) {
                    SunAvoidTimePeriodNightListChild[1]=i;
                    int[] nighttime=new int[]{SunAvoidTimePeriodNightListChild[0],SunAvoidTimePeriodNightListChild[1]};
                    SunAvoidTimePeriodNightList.add(nighttime);
                }
                if (SatSunFlag_tBefore == true && SatSunFlag_t==true && i==OrbitalDataNum-1) {
                    SunAvoidTimePeriodNightListChild[1]=i;
                    int[] nighttime=new int[]{SunAvoidTimePeriodNightListChild[0],SunAvoidTimePeriodNightListChild[1]};
                    SunAvoidTimePeriodNightList.add(nighttime);
                }
            }else {
                if (SatSunFlag_tBefore == true && SatSunFlag_t==true) {
                    SunAvoidTimePeriodNightListChild[1]=i;
                    int[] nighttime=new int[]{SunAvoidTimePeriodNightListChild[0],SunAvoidTimePeriodNightListChild[1]};
                    SunAvoidTimePeriodNightList.add(nighttime);
                }
                SatSunFlag_tBefore=false;
                SatSunFlag_t=false;
                //正午规避
                double[] r_sun_n=new double[]{r_sun[0]/sqrt(r_sun[0]*r_sun[0]+r_sun[1]*r_sun[1]+r_sun[2]*r_sun[2]),
                        r_sun[1]/sqrt(r_sun[0]*r_sun[0]+r_sun[1]*r_sun[1]+r_sun[2]*r_sun[2]),
                        r_sun[2]/sqrt(r_sun[0]*r_sun[0]+r_sun[1]*r_sun[1]+r_sun[2]*r_sun[2])};
                double[] v_sat_n=new double[]{OrbitSatVelocityGEIList.get(i)[0]/sqrt(OrbitSatVelocityGEIList.get(i)[0]*OrbitSatVelocityGEIList.get(i)[0]+OrbitSatVelocityGEIList.get(i)[1]*OrbitSatVelocityGEIList.get(i)[1]+OrbitSatVelocityGEIList.get(i)[2]*OrbitSatVelocityGEIList.get(i)[2]),
                        OrbitSatVelocityGEIList.get(i)[1]/sqrt(OrbitSatVelocityGEIList.get(i)[0]*OrbitSatVelocityGEIList.get(i)[0]+OrbitSatVelocityGEIList.get(i)[1]*OrbitSatVelocityGEIList.get(i)[1]+OrbitSatVelocityGEIList.get(i)[2]*OrbitSatVelocityGEIList.get(i)[2]),
                        OrbitSatVelocityGEIList.get(i)[2]/sqrt(OrbitSatVelocityGEIList.get(i)[0]*OrbitSatVelocityGEIList.get(i)[0]+OrbitSatVelocityGEIList.get(i)[1]*OrbitSatVelocityGEIList.get(i)[1]+OrbitSatVelocityGEIList.get(i)[2]*OrbitSatVelocityGEIList.get(i)[2])};
                double CosTheta_SunVel=(r_sun_n[0]*v_sat_n[0]+r_sun_n[1]*v_sat_n[1]+r_sun_n[2]*v_sat_n[2])/
                        (sqrt(r_sun_n[0]*r_sun_n[0]+r_sun_n[1]*r_sun_n[1]+r_sun_n[2]*r_sun_n[2])*sqrt(v_sat_n[0]*v_sat_n[0]+v_sat_n[1]*v_sat_n[1]+v_sat_n[2]*v_sat_n[2]));
                if (CosTheta_SunVel > 0) {
                    SatFlyFlag_tBefore=SatFlyFlag_t;
                    SatFlyFlag_t=false;
                }else {
                    SatFlyFlag_tBefore=SatFlyFlag_t;
                    SatFlyFlag_t=true;
                }

                if ((SatFlyFlag_tBefore==true && SatFlyFlag_t==false)||(SatFlyFlag_tBefore==false && SatFlyFlag_t==true)) {
                    int[] daytime=new int[]{i-(int) (sun_middle_day_duration/2),i+(int) (sun_middle_day_duration/2)};
                    if (daytime[0] < 0) {
                        daytime[0]=0;
                    }
                    if (daytime[1] >= OrbitalDataNum) {
                        daytime[1]=OrbitalDataNum-1;
                    }
                    SunAvoidTimePeriodDayList.add(daytime);
                }
            }
        }

        //数据传出
        ArrayList<Document> avoid_sunshine_lp = new ArrayList<>();
        MongoCollection<Document> avoidance_sunlight = mongoDatabase.getCollection("avoidance_sunlight");
        for (int i = 0; i < SunAvoidTimePeriodDayList.size(); i++) {
            Document jsonObject1 = new Document();
            jsonObject1.append("amount_window", SunAvoidTimePeriodDayList.size()+SunAvoidTimePeriodNightList.size());
            jsonObject1.append("window_number", i + 1);
            jsonObject1.append("tag", "HighNoon");
            jsonObject1.append("start_time", OrbitTimeDateList.get(SunAvoidTimePeriodDayList.get(i)[0]));
            jsonObject1.append("end_time", OrbitTimeDateList.get(SunAvoidTimePeriodDayList.get(i)[1]));
            avoid_sunshine_lp.add(jsonObject1);
        }
        for (int i = 0; i < SunAvoidTimePeriodNightList.size(); i++) {
            Document jsonObject1 = new Document();
            jsonObject1.append("amount_window", SunAvoidTimePeriodDayList.size()+SunAvoidTimePeriodNightList.size());
            jsonObject1.append("window_number", i +SunAvoidTimePeriodDayList.size()+ 1);
            jsonObject1.append("tag", "MidNight");
            jsonObject1.append("start_time", OrbitTimeDateList.get(SunAvoidTimePeriodNightList.get(i)[0]));
            jsonObject1.append("end_time", OrbitTimeDateList.get(SunAvoidTimePeriodNightList.get(i)[1]));
            avoid_sunshine_lp.add(jsonObject1);
        }
        Document AvoidSunInfo = new Document();
        AvoidSunInfo.append("time_point", Date.from(timeRaw));
        AvoidSunInfo.append("avoid_window", avoid_sunshine_lp);
        Document modifiers = new Document();
        modifiers.append("$set", AvoidSunInfo);
        avoidance_sunlight.updateOne(new Document("time_point", AvoidSunInfo.getDate("time_point")), modifiers, new UpdateOptions().upsert(true));

        mongoClient.close();
    }


    //太阳矢量
    private static double Sun(double JD, double[] r_sun, double[] su) {
        double rad_sun;
        double T_TDB, L_sun, M_sun, C, lambda_sun, e, ecc, v;
        /*...Compute Julian centuries*/
        T_TDB = (JD - 2451545.0) / 36525.0;
        /*...Compute the Sun’s mean anomaly*/
        M_sun = 357.52911 + 35999.05029 * T_TDB - 0.0001537 * T_TDB * T_TDB;
        //M_sun = quadrant(M_sun);
        /*...Compute the Mean geometric longitude of the sun*/
        L_sun = 280.46646 + 36000.76983 * T_TDB + 0.0003032 * T_TDB * T_TDB;
        //L_sun = quadrant(L_sun);
        /*...Compute the centre*/
        C = (1.914602 - 0.004817 * T_TDB - 0.000014 * T_TDB * T_TDB) * Math.sin(M_sun * PI / 180.0)
                + (0.019993 - 0.000101 * T_TDB) * Math.sin(2 * M_sun * PI / 180.0) +
                0.000289 * Math.sin(3 * M_sun * PI / 180.0);
        /*...Compute true geometric longitude*/
        lambda_sun = L_sun + C;
        /*...Compute the mean obliquity of the ecliptic*/
        e = 23.439291 - 0.0130042 * T_TDB - 1.64e-07 * T_TDB * T_TDB
                + 5.04e-07 * T_TDB * T_TDB * T_TDB;
        /*...Compute the Eccentricty of Earth’s orbit*/
        ecc = 0.016708634 - 0.000042037 * T_TDB - 0.0000001267 * T_TDB * T_TDB;
        /*...Compute Sun’s true anomaly*/
        v = M_sun + C;
        /*...Compute radial distance from Earth to the Sun*/
        rad_sun = 1.000001018 * (1 - ecc * ecc) / (1 - ecc * Math.cos(v * PI / 180.0)); //in AU
        /*...Compute position of the sun in ECI*/
        r_sun[0] = rad_sun * Math.cos(lambda_sun * PI / 180.0);
        r_sun[1] = rad_sun * Math.cos(e * PI / 180.0) * Math.sin(lambda_sun * PI / 180.0);
        r_sun[2] = rad_sun * Math.sin(e * PI / 180.0) * Math.sin(lambda_sun * PI / 180.0);
        rad_sun = rad_sun * AUtokm * kmtom; //in meters
        for (int i = 0; i <= 2; i++) {
            r_sun[i] = r_sun[i] * AUtokm * kmtom;
        }
        /*...Compute the right ascension and declination*/
        su[0] = Math.atan2(Math.cos(e * PI / 180.0) * Math.sin(lambda_sun * PI / 180.0),
                Math.cos(lambda_sun * PI / 180.0));
        //isu = su[0];
        //su[0] = QuadRad(isu);
        //su[0] =isu*180/PI;
        su[1] = Math.asin(Math.sin(e * PI / 180.0) * Math.sin(lambda_sun * PI / 180.0));
        return rad_sun;
    }

    //儒略日计算
    private static double JD(double Time[]){
        double year_UT=Time[0];
        double month_UT=Time[1];
        double day_UT=Time[2];
        double hour_UT=Time[3];
        double minute_UT=Time[4];
        double second_UT=Time[5];

        double D=day_UT;
        double M,Y,B;
        double JD;
        if (month_UT==1 || month_UT==2){
            M=month_UT+12;
            Y=year_UT-1;
        }
        else {
            Y=year_UT;
            M=month_UT;
        }
        B=0;
        if (Y>1582 || (Y==1582 && M>10) || (Y==1582 && M==10 && D>=15)){
            B=2-(int)Math.floor(Y/100.0)+(int)Math.floor(Y/400.0);
        }
        JD=(int)Math.floor(365.25*(Y+4716))+(int)Math.floor(30.6*(M+1))+D+B-1524.5;
        JD=JD-0.5+hour_UT/24.0+minute_UT/1440+second_UT/86400;
        JD=JD+0.5;
        return JD;
    }

    private static double[] DateToDouble(Date Time_Date) {
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

    //惯性坐标系转到轨道坐标系，大椭圆轨道
    private static void GEIToORF_Ellipse(double SatPosition_GEI[], double SatVelocity_GEI[], double Position_GEI[], double Position_ORF[]) {
        double r = Math.sqrt(Math.pow(SatPosition_GEI[0], 2) + Math.pow(SatPosition_GEI[1], 2) + Math.pow(SatPosition_GEI[2], 2));
        double v = Math.sqrt(Math.pow(SatVelocity_GEI[0], 2) + Math.pow(SatVelocity_GEI[1], 2) + Math.pow(SatVelocity_GEI[2], 2));
        double[] zs = {-SatPosition_GEI[0] / r, -SatPosition_GEI[1] / r, -SatPosition_GEI[2] / r};
        double[] xs = {SatVelocity_GEI[0] / v, SatVelocity_GEI[1] / v, SatVelocity_GEI[2] / v};
        double[] ys = new double[3];
        ys = VectorCross(zs, xs);
        double r_ys=sqrt(pow(ys[0],2)+pow(ys[1],2)+pow(ys[2],2));
        ys[0]=ys[0]/r_ys;
        ys[1]=ys[1]/r_ys;
        ys[2]=ys[2]/r_ys;
        xs=VectorCross(ys,zs);
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

    //矢量叉乘
    private static double[] VectorCross(double A[], double B[]) {
        if (A.length != 3 || B.length != 3)
            JOptionPane.showMessageDialog(null, "求矢量的叉乘输入不合法", "求矢量叉乘错误", JOptionPane.ERROR_MESSAGE);
        double[] Result = new double[3];
        Result[0] = A[1] * B[2] - A[2] * B[1];
        Result[1] = A[2] * B[0] - A[0] * B[2];
        Result[2] = A[0] * B[1] - A[1] * B[0];

        return Result;
    }

    //矩阵乘法
    private static double[][] MatrixMultiplication(double A[][], double B[][]) {
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
}
