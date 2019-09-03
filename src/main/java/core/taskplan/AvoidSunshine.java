package core.taskplan;

import com.google.gson.JsonObject;
import com.mongodb.client.FindIterable;
import org.bson.Document;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import static java.lang.Math.*;

public class AvoidSunshine {
    private static double AUtokm = 1.49597870e8;
    private static double kmtom = 10e3;

    //阳光规避弧段
    private static int[] SunAvoidTimePeriod=new int[10];
    private static int SunAvoidTimePeriodNum;

    public static void AvoidSunshineII(FindIterable<Document> Orbitjson, long orbitDataCount ){

        //读入轨道数据
        String StringTime;
        double[][] Time = new double[(int) orbitDataCount][6];
        double[][] SatPosition_GEI = new double[(int) orbitDataCount][3];
        double[][] SatVelocity_GEI = new double[(int) orbitDataCount][3];
        double[][] SatPosition_LLA = new double[(int) orbitDataCount][3];

        int OrbitalDataNum=0;
        for(Document  document: Orbitjson){
            Date time_point = document.getDate("time_point");
            //时间转换为doubule型
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Calendar cal = Calendar.getInstance();
            cal.setTime(time_point);
            cal.add(Calendar.HOUR_OF_DAY, -8);
            StringTime = sdf.format(cal.getTime());
            Time[OrbitalDataNum][0] = Double.parseDouble(StringTime.substring(0,4));
            Time[OrbitalDataNum][1] = Double.parseDouble(StringTime.substring(5,7));
            Time[OrbitalDataNum][2] = Double.parseDouble(StringTime.substring(8,10));
            Time[OrbitalDataNum][3] = Double.parseDouble(StringTime.substring(11,13));
            Time[OrbitalDataNum][4] = Double.parseDouble(StringTime.substring(14,16));
            Time[OrbitalDataNum][5] = Double.parseDouble(StringTime.substring(17,19));

            SatPosition_GEI[OrbitalDataNum][0]=Double.parseDouble(document.get("P_x").toString());
            SatPosition_GEI[OrbitalDataNum][1]=Double.parseDouble(document.get("P_y").toString());
            SatPosition_GEI[OrbitalDataNum][2]=Double.parseDouble(document.get("P_z").toString());
            SatVelocity_GEI[OrbitalDataNum][0]=Double.parseDouble(document.get("Vx").toString());
            SatVelocity_GEI[OrbitalDataNum][1]=Double.parseDouble(document.get("Vy").toString());
            SatVelocity_GEI[OrbitalDataNum][2]=Double.parseDouble(document.get("Vz").toString());
            SatPosition_LLA[OrbitalDataNum][0]=Double.parseDouble(document.get("lon").toString());
            SatPosition_LLA[OrbitalDataNum][1]=Double.parseDouble(document.get("lat").toString());
            SatPosition_LLA[OrbitalDataNum][2]=Double.parseDouble(document.get("H").toString());

            OrbitalDataNum=OrbitalDataNum+1;
        }

        int Flag_tBefore=0;
        int Avoid_Flag=0;
        int Flag_t=0;
        SunAvoidTimePeriodNum=0;
        for (int i = 0; i < OrbitalDataNum; i++) {
            double Time_JD=JD(Time[i]);
            double[] r_sun=new double[3];//地心惯性坐标系下太阳位置
            double[] su=new double[2];//赤经和赤纬
            double rad_sun;//太阳地球的距离
            rad_sun=Sun(Time_JD,r_sun,su);
            double a=r_sun[0]*SatPosition_GEI[i][0]+r_sun[1]*SatPosition_GEI[i][1]+r_sun[2]*SatPosition_GEI[i][2];
            double r_Sat=sqrt(SatPosition_GEI[i][0]*SatPosition_GEI[i][0]+SatPosition_GEI[i][1]*SatPosition_GEI[i][1]+SatPosition_GEI[i][2]*SatPosition_GEI[i][2]);
            double theta=acos(a/(rad_sun*r_Sat));

            if (theta >= 175*PI/180.0) {
                Avoid_Flag=1;
                Flag_tBefore=Flag_t;
                Flag_t=Avoid_Flag;
            }else {
                Avoid_Flag=0;
                Flag_tBefore=Flag_t;
                Flag_t=Avoid_Flag;
            }

            //判定开始结束时间
            if (Flag_tBefore==0 && Flag_t==1){
                SunAvoidTimePeriod[2*SunAvoidTimePeriodNum]=i;
            }
            else if (Flag_tBefore==1 && Flag_t==0){
                SunAvoidTimePeriod[2*SunAvoidTimePeriodNum+1]=i-1;
                SunAvoidTimePeriodNum=SunAvoidTimePeriodNum+1;
            }
            if (i==OrbitalDataNum-1 && Flag_t==1){
                SunAvoidTimePeriod[2*SunAvoidTimePeriodNum+1]=i;
                SunAvoidTimePeriodNum=SunAvoidTimePeriodNum+1;
            }
        }

        //数据传出
        JsonObject jsonObject = new JsonObject();
        if (SunAvoidTimePeriodNum == 0) {
            jsonObject.addProperty("Avoid_reason","无规避弧段");
        }else {
            for (int i = 0; i < SunAvoidTimePeriodNum; i++) {
                //索引号转化为Data型时间
                //索引号转化为Data型时间
                Date Star_time = new Date(),end_time = new Date();
                int s_Num=0;
                for(Document  document: Orbitjson) {
                    if (s_Num==SunAvoidTimePeriod[2*i]){
                        Star_time = document.getDate("time_point");
                    }
                    else if (s_Num==SunAvoidTimePeriod[2*i+1]){
                        end_time=document.getDate("time_point");
                    }
                    s_Num=s_Num+1;
                }
                jsonObject.addProperty("Avoid_window",SunAvoidTimePeriodNum);
                jsonObject.addProperty("Avoid_window_number",i);
                jsonObject.addProperty("Avoid_window_start_time", String.valueOf(Star_time));
                jsonObject.addProperty("Avoid_window_end_time", String.valueOf(end_time));
            }
        }
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
}
