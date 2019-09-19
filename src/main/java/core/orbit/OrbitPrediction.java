package core.orbit;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import common.mongo.DbDefine;
import common.mongo.MangoDBConnector;
import org.bson.Document;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;

import static java.lang.Math.acos;
import static java.lang.Math.sqrt;

class OrbitPrediction {
    private static double GRAVP = 1;
    private static double MOON = 1;
    private static double SUN = 1;
    private static double SRP = 1;
    private static double ADRAG = 0;
    private static double OnlyJ2 = 0;

    private static double j2 = 1082.626e-6;
    private static double j3 = -2.5356e-6;
    private static double j4 = -1.62336e-6;

    private static double j22 = 0;
    private static double j31 = 0;
    private static double j32 = 0;
    private static double j33 = 0;
    private static double j41 = 0;
    private static double j42 = 0;
    private static double j43 = 0;
    private static double j44 = 0;

    private static double l22 = -14.545;
    private static double l31 = 7.0805;
    private static double l32 = -17.4649;
    private static double l33 = 21.2097;
    private static double l41 = -138.756;
    private static double l42 = 31.0335;
    private static double l43 = -3.8459;
    private static double l44 = 30.792;

    private static double mu = 398600.4418e9;
    private static double mu_moon = 4.902799e6;
    private static double mu_sun = 1.327124e14;
    private static double PSR = 4.51e-6;
    private static double CR = 1.0;
    private static double AMRatio = 0.02;
    private static double C_D = 2.200000;
    private static double rho_0 = 0.36;
    private static double h0 = 37.4;
    private static double AUtokm = 1.49597870e8;
    private static double kmtom = 10e3;
    private static double rate = 4.178074216e-3;
    private static double DtR = 3.1415926 / 180;

    private static double DENSMOD = 2;
    private static double PI = 3.1415926;

    private static double R_earth = 6378136.3;
    private static double eccent = 0.08182;
    private static Object JsonToStringUtil;

    private static double GetDensity(double height) {
        double dsr;
        double dif;
        int i;
        double[][] feoc =
                {
                        {85.0, -0.508515E+01, -0.733375E-01, -0.825405E-03, 0.479193E-04},
                        {90.0, -0.546648E+01, -0.779976E-01, -0.106615E-03, 0.561803E-05},
                        {100.0, -0.625150E+01, -0.784445E-01, 0.619256E-04, 0.168843E-04},
                        {110.0, -0.701287E+01, -0.721407E-01, 0.568454E-03, 0.241761E-04},
                        {120.0, -0.765326E+01, -0.535188E-01, 0.129374E-02, -0.296655E-04},
                        {130.0, -0.808874E+01, -0.365437E-01, 0.403772E-03, -0.289208E-05},
                        {140.0, -0.841669E+01, -0.293359E-01, 0.317010E-03, -0.442685E-05},
                        {150.0, -0.868277E+01, -0.243238E-01, 0.184205E-03, -0.144722E-05},
                        {160.0, -0.890904E+01, -0.210738E-01, 0.140788E-03, -0.137461E-05},
                        {170.0, -0.910707E+01, -0.186705E-01, 0.995495E-04, -0.677454E-06},
                        {180.0, -0.928450E+01, -0.168827E-01, 0.792259E-04, -0.593218E-06},
                        {190.0, -0.944600E+01, -0.154761E-01, 0.614293E-04, -0.381119E-06},
                        {200.0, -0.959500E+01, -0.143619E-01, 0.499958E-04, -0.249568E-06},
                        {220.0, -0.986423E+01, -0.126615E-01, 0.350217E-04, -0.154281E-06},
                        {240.0, -0.101047E+02, -0.114458E-01, 0.257648E-04, -0.925137E-07},
                        {260.0, -0.103240E+02, -0.105262E-01, 0.202140E-04, -0.774691E-07},
                        {280.0, -0.105271E+02, -0.981064E-02, 0.155659E-04, -0.650883E-07},
                        {300.0, -0.107176E+02, -0.926611E-02, 0.116606E-04, -0.249222E-07},
                        {400.0, -0.115525E+02, -0.768166E-02, 0.418393E-05, -0.388723E-08},
                        {500.0, -0.122827E+02, -0.696149E-02, 0.301776E-05, 0.447753E-08},
                        {600.0, -0.129442E+02, -0.622361E-02, 0.436102E-05, 0.998741E-08},
                        {700.0, -0.135130E+02, -0.505179E-02, 0.735724E-05, -0.124098E-10},
                        {800.0, -0.139446E+02, -0.358071E-02, 0.735352E-05, -0.104955E-07},
                        {900.0, -0.142397E+02, -0.242487E-02, 0.420487E-05, -0.833682E-08},
                        {1000.0, 0.0, 0.0, 0.0, 0.0}
                };

        dsr = 0.0;
        dif = 0;
        i = 0;

        height = height * 0.001;//km

        if (height < 85.0) {
            height = 85;
        }

        if (height >= 999.99999) {
            dsr = -14.44847512 - 0.001352 * (height - 1000.0);
        } else {
            for (i = 0; i <= 23; i++) {
                if (height >= feoc[i][0] && height < feoc[i + 1][0]) {
                    dif = height - feoc[i][0];
                    dsr = feoc[i][1] + feoc[i][2] * dif + feoc[i][3] * dif * dif + feoc[i][4] * dif * dif * dif;
                    break;
                }
            }
        }

        return (Math.pow(10.0, dsr));
    }

    //四阶龙格库塔
    private static void RK4(double[] X, double[] xout, double t, int year, int month, int day, double h, double[] sa) {
        int i;
        double hh = h * 0.5, th = t + hh;
        //double y[6], k1[6], k2[6], k3[6], k4[6];
        double[] y = new double[6];
        double[] k1 = new double[6];
        double[] k2 = new double[6];
        double[] k3 = new double[6];
        double[] k4 = new double[6];
        double JD;
        double[] r_sat = new double[3];
        //double[] sa = new double[3];

        DXDT(t, X, k1, year, month, day);    //first step
        for (i = 0; i < 6; i++) {
            y[i] = X[i] + hh * k1[i];
        }
        DXDT(th, y, k2, year, month, day); //second step
        for (i = 0; i < 6; i++) {
            y[i] = X[i] + hh * k2[i];
        }
        DXDT(th, y, k3, year, month, day); //third step
        for (i = 0; i < 6; i++) {
            y[i] = X[i] + h * k3[i];
        }
        DXDT(t + h, y, k4, year, month, day); //fourth step
        for (i = 0; i < 6; i++) {
            xout[i] = X[i] + h * (k1[i] + 2 * k2[i] + 2 * k3[i] + k4[i]) / 6;
        }
        JD = JulianDate(year, month, day, t);
        //compute longitude, latitude and altitude
        r_sat[0] = xout[0];
        r_sat[1] = xout[1];
        r_sat[2] = xout[2];
        ECI_ECEF(JD, r_sat, sa);

    }

    //带摄动的动力学
    private static void DXDT(double t, double[] X, double[] DX, int year, int month, int day)   // 轨道动力学
    {
        int im;
        int int_in;
        //double [3]r_sat,sa[3],P[61][61] = {0.0},CN[5],SN[5],TN[5];
        double[] r_sat = new double[3];
        double[] sa = new double[3];
        double[][] P = new double[61][61];
        double[] CN = new double[5];
        double[] SN = new double[5];
        double[] TN = new double[5];

        //double C[5][5]={0.0},S[5][5] ={0.0};
        double[][] C = new double[5][5];
        double[][] S = new double[5][5];
        C[2][0] = -j2;
        C[3][0] = -j3;
        C[4][0] = -j4;
        C[3][1] = -j31 * Math.cos(l31 * PI / 180);
        C[4][1] = -j41 * Math.cos(l41 * PI / 180);
        C[2][2] = -j22 * Math.cos(l22 * PI / 180);
        C[3][2] = -j32 * Math.cos(l32 * PI / 180);
        C[4][2] = -j42 * Math.cos(l42 * PI / 180);
        C[3][3] = -j33 * Math.cos(l33 * PI / 180);
        C[4][3] = -j43 * Math.cos(l43 * PI / 180);
        C[4][4] = -j44 * Math.cos(l44 * PI / 180);
        S[3][1] = -j31 * Math.sin(l31 * PI / 180);
        S[4][1] = -j41 * Math.sin(l41 * PI / 180);
        S[2][2] = -j22 * Math.sin(l22 * PI / 180);
        S[3][2] = -j32 * Math.sin(l32 * PI / 180);
        S[4][2] = -j42 * Math.sin(l42 * PI / 180);
        S[3][3] = -j33 * Math.sin(l33 * PI / 180);
        S[4][3] = -j43 * Math.sin(l43 * PI / 180);
        S[4][4] = -j44 * Math.sin(l44 * PI / 180);

        double JD, Rij, SRij, R2, Dr, Dp, Dl, dPhi_dr, dPhi_dlambda, dPhi_dphi;
        double idr, idphi, idlambda, C1, C2, C3;
        double radius, r_cubed, r5;
        double Lon, Lat, sphi;
        double PCSR, rdrs, ratm, theta;
        //double r_satmoon[3],r_satsun[3];
        double[] r_satmoon = new double[3];
        double[] r_satsun = new double[3];

        double rad_sun, rad_moon, mag_rsun, rm3, rsm, rsm3, rs3, rss, rss3;
        rss = 1.0;


        double[] r_sun = new double[3];
        double[] r_moon = new double[3];
        double[] rs_unit = new double[3];
        double[] rss_unit = new double[3];
        double[] su = new double[2];

        double omega, altitude, vr2, vr_mag;
        double[] v_rel = new double[3];

        double wmol, rho;
        rho = 1.0;
        double[] an = new double[6];
        //double sf[3], te[2];
        double[] sf = new double[3];
        double[] te = new double[2];

        /*...Create position vector*/
        double r_x = X[0],
                r_y = X[1],
                r_z = X[2];
        r_sat[0] = X[0];
        r_sat[1] = X[1];
        r_sat[2] = X[2];
        /*...Compute the radial distance from Earth to Sat*/
        R2 = r_x * r_x + r_y * r_y + r_z * r_z;  // 卫星地心之间距离平方
        radius = Math.sqrt(R2);                 // 卫星地心之间距离
        r_cubed = radius * R2;               // 卫星地心之间距离三次方
        /*...Transfer velocity from satellite state vector*/
        DX[0] = X[3];
        DX[1] = X[4];
        DX[2] = X[5];
        /*...initialize acceleration for this integration step*/
        for (int i = 3; i <= 5; i++) {
            DX[i] = 0.0;
        }
        /*...Compute Julian date*/
        JD = JulianDate(year, month, day, t);
        //compute longitude, latitude and altitude
        ECI_ECEF(JD, r_sat, sa);
        Lon = sa[0];
        Lat = sa[1];
        sphi = Math.sin(Lat);
        if (GRAVP == 1)              // 地球摄动
        {
            if (OnlyJ2 == 1)    //只考虑J2摄动
            {
                //Compute J2
                r5 = R2 * r_cubed;
                C1 = -1.5 * j2 * R_earth * R_earth * mu / r5;
                C2 = 1 - 5 * r_z * r_z / R2;
                DX[3] = DX[3] + X[0] * C1 * C2;
                DX[4] = DX[4] + X[1] * C1 * C2;
                DX[5] = DX[5] + X[2] * C1 * (C2 + 2);
            } else {
                /*...Aspheric gravity computation*/
                Rij = r_x * r_x + r_y * r_y;
                SRij = Math.sqrt(Rij);
                /*...Compute Normalized legendre polynomials*/
                legendre(4, sphi, P);
                /*...Compute Cos[m],Sin[m] and Tan[m]*/
                trigfunc(4, Lat, Lon, CN, SN, TN);
                /*...Compute partials*/
                C1 = R_earth / radius;
                C2 = C1;
                dPhi_dr = 0;
                dPhi_dphi = 0;
                dPhi_dlambda = 0;
                for (int i = 2; i <= 4; i++) {
                    idr = 0;
                    idphi = 0;
                    idlambda = 0;
                    int_in = i + 1;
                    for (int j = 0; j <= i; j++) {
                        im = j + 1;
                        C3 = C[i][j] * CN[j] + S[i][j] * SN[j];
                        idr = idr + C3 * P[i][j];
                        if (im <= i) {
                            idphi = idphi + C3 * (P[i][im] - TN[j] * P[i][j]);
                        }
                        if (im > i) {
                            idphi = idphi - C3 * TN[j] * P[i][j];
                        }
                        if (j != 0) {
                            idlambda = idlambda + j * (S[i][j] * CN[j] - C[i][j] * SN[j]) * P[i][j];
                        }
                    }
                    C2 = C2 * C1;
                    dPhi_dr = dPhi_dr + C2 * int_in * idr;
                    dPhi_dphi = dPhi_dphi + C2 * idphi;
                    dPhi_dlambda = dPhi_dlambda + C2 * idlambda;
                }
                C3 = mu / radius;
                dPhi_dr = -dPhi_dr * C3 / radius;        // dPhi/dr
                dPhi_dphi = dPhi_dphi * C3;              // dPhi/dphi
                dPhi_dlambda = dPhi_dlambda * C3;
                Dr = dPhi_dr / radius;
                Dp = r_z / (R2 * SRij) * dPhi_dphi;
                Dl = dPhi_dlambda / Rij;
                DX[3] = DX[3] + (Dr - Dp) * r_x - (Dl * r_y);
                DX[4] = DX[4] + (Dr - Dp) * r_y + (Dl * r_x);
                DX[5] = DX[5] + Dr * r_z + SRij * dPhi_dphi / R2;
            }
        }
        if (MOON == 1)  // 月球摄动
        {
            /*...Compute derivative due to the Moon*/
            /*...Compute Moon’s position*/
            rad_moon = Moon(JD, r_moon);
            rm3 = rad_moon * rad_moon * rad_moon;
            //vminus(r_sat,r_moon,r_satmoon);
            for (int i = 0; i < 3; i++)      // 卫星到月球距离
            {
                r_satmoon[i] = r_sat[i] - r_moon[i];
            }

            rsm = r_satmoon[0] * r_satmoon[0] + r_satmoon[1] * r_satmoon[1] + r_satmoon[2] * r_satmoon[2];
            rsm = Math.sqrt(rsm);
            rsm3 = rsm * rsm * rsm;
            for (int i = 0; i <= 2; i++) {
                DX[i + 3] = DX[i + 3] - mu_moon * (r_satmoon[i] / rsm3 + r_moon[i] / rm3);
            }
        }
        if (SUN == 1)          //太阳摄动
        {
            /*...Compute derivative due to the Sun*/
            /*...Compute Sun’s position*/
            rad_sun = Sun(JD, r_sun, su);
            rs3 = rad_sun * rad_sun * rad_sun;
            //vminus(r_sat,r_sun,r_satsun);

            for (int i = 0; i < 3; i++)      // 卫星到太阳距离
            {
                r_satsun[i] = r_sat[i] - r_sun[i];
            }
            rss = r_satsun[0] * r_satsun[0] + r_satsun[1] * r_satsun[1]
                    + r_satsun[2] * r_satsun[2];
            rss = Math.sqrt(rss);
            rss3 = rss * rss * rss;
            for (int i = 0; i <= 2; i++) {
                DX[i + 3] = DX[i + 3] - mu_sun * (r_satsun[i] / rss3 + r_sun[i] / rs3);
            }
        }
        if (SRP == 1)   //太阳辐射压
        {
            /*...Compute derivative due to SRP*/
            PCSR = PSR * CR;
            ratm = R_earth + 90000;
            mag_rsun = r_sun[0] * r_sun[0] + r_sun[1] * r_sun[1] + r_sun[2] * r_sun[2];
            mag_rsun = Math.sqrt(mag_rsun);
            for (int i = 0; i <= 2; i++) {
                rs_unit[i] = r_sun[i] / mag_rsun;
                rss_unit[i] = r_satsun[i] / rss;
            }
            /*...check for shadow condition*/
            rdrs = rs_unit[0] * X[0] + rs_unit[1] * X[1] + rs_unit[2] * X[2];
            theta = Math.acos(rdrs / radius);
            if (rdrs >= 0.0) {
                for (int i = 0; i <= 2; i++) {
                    DX[i + 3] = DX[i + 3] - PCSR * AMRatio * rss_unit[i];
                }
            } else if (radius * Math.sin(theta) < ratm) {
                //Satellite in Earth’s shadow
                for (int i = 0; i <= 2; i++) {
                    DX[i + 3] = DX[i + 3] + 0;
                }
            }
        }
        if (ADRAG == 1)       //大气阻力
        {
            /*...Compute derivative due to Drag*/
            omega = rate * DtR;
            /*...Compute atmospheric density*/
            altitude = sa[2];
            if (DENSMOD == 1) {
                rho = Exp_model(altitude);
            }
            if (DENSMOD == 2) {
                rho = GetDensity(altitude);
            }
            /*...Compute the velocity relative to the rotating atmosphere*/
            v_rel[0] = X[3] + omega * X[1];
            v_rel[1] = X[4] - omega * X[0];
            v_rel[2] = X[5];
            vr2 = v_rel[0] * v_rel[0] + v_rel[1] * v_rel[1] + v_rel[2] * v_rel[2];
            vr_mag = Math.sqrt(vr2);
            //Note: vr2 * v_rel(unit vector) = vr_mag*v_rel
            for (int i = 0; i <= 2; i++) {
                DX[i + 3] = DX[i + 3] - 0.5 * rho * (C_D * AMRatio) * vr_mag * v_rel[i];
            }
        }
        /*...Compute Two-Body derivatives*/
        for (int i = 0; i <= 2; i++) {
            DX[i + 3] = DX[i + 3] - mu * X[i] / r_cubed;
        }
    }

    //地心赤道坐标系星下点位置
    private static void P2subsatP(double[] P, double[] subsat) {
        double omega = 1.0027 * 180 / 43200;
        double x = P[0];
        double y = P[1];
        double z = P[2];

        //地心赤道坐标系星下点位置
        subsat[0] = R_earth * (P[0] / sqrt(P[0] * P[0] + P[1] * P[1] + P[2] * P[2]));
        subsat[1] = R_earth * (P[1] / sqrt(P[0] * P[0] + P[1] * P[1] + P[2] * P[2]));
        subsat[2] = R_earth * (P[2] / sqrt(P[0] * P[0] + P[1] * P[1] + P[2] * P[2]));
    }

    private static void trigfunc(int mm, double lat, double lon, double[] CN, double[] SN, double[] TN) {
        //if(mm ==0)
        CN[0] = 1;
        SN[0] = 0;
        TN[0] = 0;
        //if(mm == 1)
        CN[1] = Math.cos(lon);
        SN[1] = Math.sin(lon);
        TN[1] = Math.tan(lat);
        for (int i = 2; i <= mm; i++) {
            CN[i] = 2 * CN[1] * CN[i - 1] - CN[i - 2];
            SN[i] = 2 * CN[1] * SN[i - 1] - SN[i - 2];
            TN[i] = TN[1] + TN[i - 1];
        }
        return;
    }

    //惯性系转地心地固
    private static void ECI_ECEF(double JD, double[] R, double[] sa) {
        double gast, Range, Radius, sphi, x, y, z;
        double[] R_ECEF = new double[3];
        //Compute GAST
        gast = app_sidereal_time(JD);
        gast = gast * DtR;
        //Rotate ECI vector to ECEF frame
        R_ECEF[0] = Math.cos(gast) * R[0] + Math.sin(gast) * R[1];
        R_ECEF[1] = -Math.sin(gast) * R[0] + Math.cos(gast) * R[1];
        R_ECEF[2] = R[2];
        //calculate geocentric lon, lat in radians
        x = R_ECEF[0];
        y = R_ECEF[1];
        z = R_ECEF[2];
        Range = x * x + y * y + z * z;
        Radius = Math.sqrt(Range);
        sa[0] = Math.atan2(y, x);//GeoLon
        sphi = z / Radius;
        sa[1] = Math.asin(sphi);//GeoLat
        //Altitude
        //*(sa+2) = Radius - R_earth;
        sa[2] = Radius - Math.sqrt(R_earth * R_earth * (1.0 - eccent * eccent) / (1.0 - Math.pow(eccent * Math.cos(sa[1]), 2)));
        return;
    }

    //儒略日
    private static double JulianDate(int year, int month, int day, double UT) {
        double JD, C1, C2, C3;
        C1 = 367.0 * year;
        C2 = (int) ((7 * (year + (int) ((month + 9) / 12))) * 0.25);
        C3 = (int) (275 * month / 9);
        UT = UT / 3600.0;
        JD = (C1 - C2 + C3) + day + 1721013.5 + UT / 24.0;
        return JD;
    }

    private static void legendre(int nn, double x, double[][] p) {
        double y;
        y = Math.sqrt(1 - x * x);
        p[0][0] = 1;
        p[1][0] = x;
        p[1][1] = y;
        for (int i = 2; i <= nn; i++) {
            for (int j = 0; j <= i; j++) {
                if (j == 0) {
                    p[i][0] = ((2 * i - 1) * x * p[i - 1][0] - (i - 1) * p[i - 2][0]) / i;
                } else if (i > j && j != 0) {
                    p[i][j] = (2 * i - 1) * y * p[i - 1][j - 1];
                    if (i - 2 >= j) {
                        p[i][j] += p[i - 2][j];
                    }
                } else if (i == j && j != 0) {
                    p[i][i] = (2 * i - 1) * y * p[i - 1][i - 1];
                }
            }
        }
        return;
    }

    //月球矢量
    private static double Moon(double JD, double[] r_moon) {
        double rad_moon;
        double T_TDB, M_moon, lambda_moon, uM_moon, D_sun,
                lambda_ecliptic, phi_ecliptic, e, parallax;
        /*...Compute Julian centuries*/
        T_TDB = (JD - 2451545.0) / 36525.0;
        /*...Compute Moon’s mean anomaly(k=360degrees)*/
        M_moon = 134.9629814 + (1325.0 * 360 + 198.8673981) * T_TDB
                + 0.0086972 * T_TDB * T_TDB + 1.778e-05 * T_TDB * T_TDB * T_TDB;
        /*...Compute Moon’s longitude*/
        lambda_moon = 218.32 + 481267.8813 * T_TDB;
        /*...Compute Moon’s mean argument of latitude*/
        uM_moon = 93.27191030 + (1342.0 * 360 + 82.0175381) * T_TDB
                - 0.0036825 * T_TDB * T_TDB + 3.06e-06 * T_TDB * T_TDB * T_TDB;
        /*...Compute the mean elongation of the Sun*/
        D_sun = 297.8503631 + (1236.0 * 360 + 307.111480) * T_TDB
                - 0.00191417 * T_TDB * T_TDB + 5.28e-6 * T_TDB * T_TDB * T_TDB;
        /*...Compute the ecliptic longitude*/
        lambda_ecliptic = lambda_moon + 6.29 * Math.sin(M_moon * PI / 180.0) -
                1.27 * Math.sin((M_moon - 2 * D_sun) * PI / 180.0)
                + 0.66 * Math.sin(2 * D_sun * PI / 180.0)
                + 0.21 * Math.sin(2 * M_moon * PI / 180.0)
                - 0.19 * Math.sin(M_moon * PI / 180.0)
                - 0.11 * Math.sin(2 * uM_moon * PI / 180.0);
        //lambda_ecliptic = quadrant(lambda_ecliptic);

        /*...Compute the ecliptic latitude*/
        phi_ecliptic = 5.13 * Math.sin(uM_moon * PI / 180.0)
                + 0.28 * Math.sin((M_moon + uM_moon) * PI / 180.0)
                - 0.28 * Math.sin((uM_moon - M_moon) * PI / 180.0)
                - 0.17 * Math.sin((uM_moon - 2 * D_sun) * PI / 180.0);
        /*...Compute the mean obliquity of the ecliptic*/
        e = 23.439291 - 0.0130042 * T_TDB - 1.64e-07 * T_TDB * T_TDB
                + 5.04e-07 * T_TDB * T_TDB * T_TDB;
        /*...Compute parallax*/
        parallax = 0.9508 + 0.0518 * Math.cos(M_moon * PI / 180.0)
                + 0.0095 * Math.cos((M_moon - 2 * D_sun) * PI / 180.0) +
                0.0078 * Math.cos(2 * D_sun * PI / 180.0)
                + 0.0028 * Math.cos(2 * M_moon * PI / 180.0);
        /*...Compute radial distance from Earth to the Moon(in Earth radii)*/
        rad_moon = 1 / Math.sin(parallax * PI / 180.0); //in ER
        r_moon[0] = rad_moon * Math.cos(phi_ecliptic * PI / 180.0)
                * Math.cos(lambda_ecliptic * PI / 180.0);
        r_moon[1] = rad_moon * (Math.cos(e * PI / 180.0) * Math.cos(phi_ecliptic * PI / 180.0)
                * Math.sin(lambda_ecliptic * PI / 180.0)
                - Math.sin(e * PI / 180.0) * Math.sin(phi_ecliptic * PI / 180.0));
        r_moon[2] = rad_moon * (Math.sin(e * PI / 180.0) * Math.cos(phi_ecliptic * PI / 180.0)
                * Math.sin(lambda_ecliptic * PI / 180.0)
                + Math.cos(e * PI / 180.0) * Math.sin(phi_ecliptic * PI / 180.0));
        rad_moon = rad_moon * R_earth; //in meters
        r_moon[0] = r_moon[0] * R_earth;
        r_moon[1] = r_moon[1] * R_earth;
        r_moon[2] = r_moon[2] * R_earth;
        return rad_moon;
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

    private static double Exp_model(double h) {
        double rho = rho_0 * Math.exp(h / h0);
        return rho;

    }

    private static double app_sidereal_time(double JD) {
        double T_TDB = (JD - 2451545.0) / 36525.0;
        //double hour = (JD - (int) JD) * 24;
        double hour = (JD - (int) JD - 0.5) * 24;
        return mod(6.697374558 + 2400.05133691 * T_TDB + 2.586222 * 0.00001 * Math.pow(T_TDB, 2)
                - 1.722222 * 0.000000001 * Math.pow(T_TDB, 3) + 1.002737791737697 * hour, 24) * 15;
    }

    //求余
    private static double mod(double x, double y) {
        int n;
        n = (int) (x / y);
        return x - n * y;
    }


    //轨道外推
    public static void OrbitPredictorII(Instant timeRaw, LocalDateTime start, LocalDateTime end, double step, double[] orbit0, JsonObject json) {

        JsonArray properties = json.getAsJsonArray("properties");
        //载荷参数初始话
        int ViewNum = 4;
        double[][] ViewInstall = {{90 * PI / 180.0, 86.3 * PI / 180.0, 3.7 * PI / 180.0}, {90 * PI / 180.0, 93.7 * PI / 180.0, 3.7 * PI / 180.0}, {90 * PI / 180.0, 85.6 * PI / 180.0, 4.4 * PI / 180.0}, {90 * PI / 180.0, 94.4 * PI / 180.0, 4.4 * PI / 180.0}};
        double[][] ViewAng = {{3 * PI / 180.0, 3 * PI / 180.0, 3 * PI / 180.0, 3 * PI / 180.0}, {3 * PI / 180.0, 3 * PI / 180.0, 3 * PI / 180.0, 3 * PI / 180.0}, {3 * PI / 180.0, 3 * PI / 180.0, 3 * PI / 180.0, 3 * PI / 180.0}, {90 * PI / 180.0, 90 * PI / 180.0, 0 * PI / 180.0, 3 * PI / 180.0}};
        double RollMax = 5 * PI / 180.0;
        //double[][] ViewInstall=new double[ViewNum][3];
        //double[][] ViewAng=new double[ViewNum][4];

        for (int i = 0; i < properties.size(); i++) {
            JsonObject sub_properties = properties.get(i).getAsJsonObject();
            //载荷总数
            if (sub_properties.get("key").getAsString().equals("amount_load")) {
                ViewNum = Integer.parseInt(sub_properties.get("value").getAsString());
            }
            //载荷1
            else if (sub_properties.get("key").getAsString().equals("in_side_sight") && sub_properties.get("group").getAsString().equals("payload1")) {
                ViewAng[0][0] = Double.parseDouble(sub_properties.get("value").getAsString()) * PI / 180.0;
            } else if (sub_properties.get("key").getAsString().equals("out_side_sight") && sub_properties.get("group").getAsString().equals("payload1")) {
                ViewAng[0][1] = Double.parseDouble(sub_properties.get("value").getAsString()) * PI / 180.0;
            } else if (sub_properties.get("key").getAsString().equals("up_side_sight") && sub_properties.get("group").getAsString().equals("payload1")) {
                ViewAng[0][2] = Double.parseDouble(sub_properties.get("value").getAsString()) * PI / 180.0;
            } else if (sub_properties.get("key").getAsString().equals("down_side_sight") && sub_properties.get("group").getAsString().equals("payload1")) {
                ViewAng[0][3] = Double.parseDouble(sub_properties.get("value").getAsString()) * PI / 180.0;
            }
            //载荷2
            else if (sub_properties.get("key").getAsString().equals("in_side_sight") && sub_properties.get("group").getAsString().equals("payload2")) {
                ViewAng[1][0] = Double.parseDouble(sub_properties.get("value").getAsString()) * PI / 180.0;
            } else if (sub_properties.get("key").getAsString().equals("out_side_sight") && sub_properties.get("group").getAsString().equals("payload2")) {
                ViewAng[1][1] = Double.parseDouble(sub_properties.get("value").getAsString()) * PI / 180.0;
            } else if (sub_properties.get("key").getAsString().equals("up_side_sight") && sub_properties.get("group").getAsString().equals("payload2")) {
                ViewAng[1][2] = Double.parseDouble(sub_properties.get("value").getAsString()) * PI / 180.0;
            } else if (sub_properties.get("key").getAsString().equals("down_side_sight") && sub_properties.get("group").getAsString().equals("payload2")) {
                ViewAng[1][3] = Double.parseDouble(sub_properties.get("value").getAsString()) * PI / 180.0;
            }
            //载荷3
            else if (sub_properties.get("key").getAsString().equals("in_side_sight") && sub_properties.get("group").getAsString().equals("payload3")) {
                ViewAng[2][0] = Double.parseDouble(sub_properties.get("value").getAsString()) * PI / 180.0;
            } else if (sub_properties.get("key").getAsString().equals("out_side_sight") && sub_properties.get("group").getAsString().equals("payload3")) {
                ViewAng[2][1] = Double.parseDouble(sub_properties.get("value").getAsString()) * PI / 180.0;
            } else if (sub_properties.get("key").getAsString().equals("up_side_sight") && sub_properties.get("group").getAsString().equals("payload3")) {
                ViewAng[2][2] = Double.parseDouble(sub_properties.get("value").getAsString()) * PI / 180.0;
            } else if (sub_properties.get("key").getAsString().equals("down_side_sight") && sub_properties.get("group").getAsString().equals("payload3")) {
                ViewAng[2][3] = Double.parseDouble(sub_properties.get("value").getAsString()) * PI / 180.0;
            }
            //载荷4
            else if (sub_properties.get("key").getAsString().equals("in_side_sight") && sub_properties.get("group").getAsString().equals("payload4")) {
                ViewAng[3][0] = Double.parseDouble(sub_properties.get("value").getAsString()) * PI / 180.0;
            } else if (sub_properties.get("key").getAsString().equals("out_side_sight") && sub_properties.get("group").getAsString().equals("payload4")) {
                ViewAng[3][1] = Double.parseDouble(sub_properties.get("value").getAsString()) * PI / 180.0;
            } else if (sub_properties.get("key").getAsString().equals("up_side_sight") && sub_properties.get("group").getAsString().equals("payload4")) {
                ViewAng[3][2] = Double.parseDouble(sub_properties.get("value").getAsString()) * PI / 180.0;
            } else if (sub_properties.get("key").getAsString().equals("down_side_sight") && sub_properties.get("group").getAsString().equals("payload4")) {
                ViewAng[3][3] = Double.parseDouble(sub_properties.get("value").getAsString()) * PI / 180.0;
            }
            //安装矩阵，4个为一样的
            //第一个
            else if (sub_properties.get("key").getAsString().equals("angle_sight_Xaxis") && sub_properties.get("group").getAsString().equals("payload1")) {
                ViewInstall[0][0] = Double.parseDouble(sub_properties.get("value").getAsString()) * PI / 180.0;
            } else if (sub_properties.get("key").getAsString().equals("angle_sight_Yaxis") && sub_properties.get("group").getAsString().equals("payload1")) {
                ViewInstall[0][1] = Double.parseDouble(sub_properties.get("value").getAsString()) * PI / 180.0;
            } else if (sub_properties.get("key").getAsString().equals("angle_sight_Zaxis") && sub_properties.get("group").getAsString().equals("payload1")) {
                ViewInstall[0][2] = Double.parseDouble(sub_properties.get("value").getAsString()) * PI / 180.0;
            }
            //第二个
            else if (sub_properties.get("key").getAsString().equals("angle_sight_Xaxis") && sub_properties.get("group").getAsString().equals("payload2")) {
                ViewInstall[1][0] = Double.parseDouble(sub_properties.get("value").getAsString()) * PI / 180.0;
            } else if (sub_properties.get("key").getAsString().equals("angle_sight_Yaxis") && sub_properties.get("group").getAsString().equals("payload2")) {
                ViewInstall[1][1] = Double.parseDouble(sub_properties.get("value").getAsString()) * PI / 180.0;
            } else if (sub_properties.get("key").getAsString().equals("angle_sight_Zaxis") && sub_properties.get("group").getAsString().equals("payload2")) {
                ViewInstall[1][2] = Double.parseDouble(sub_properties.get("value").getAsString()) * PI / 180.0;
            }
            //第三个
            else if (sub_properties.get("key").getAsString().equals("angle_sight_Xaxis") && sub_properties.get("group").getAsString().equals("payload3")) {
                ViewInstall[2][0] = Double.parseDouble(sub_properties.get("value").getAsString()) * PI / 180.0;
            } else if (sub_properties.get("key").getAsString().equals("angle_sight_Yaxis") && sub_properties.get("group").getAsString().equals("payload3")) {
                ViewInstall[2][1] = Double.parseDouble(sub_properties.get("value").getAsString()) * PI / 180.0;
            } else if (sub_properties.get("key").getAsString().equals("angle_sight_Zaxis") && sub_properties.get("group").getAsString().equals("payload3")) {
                ViewInstall[2][2] = Double.parseDouble(sub_properties.get("value").getAsString()) * PI / 180.0;
            }
            //第四个
            else if (sub_properties.get("key").getAsString().equals("angle_sight_Xaxis") && sub_properties.get("group").getAsString().equals("payload4")) {
                ViewInstall[3][0] = Double.parseDouble(sub_properties.get("value").getAsString()) * PI / 180.0;
            } else if (sub_properties.get("key").getAsString().equals("angle_sight_Yaxis") && sub_properties.get("group").getAsString().equals("payload4")) {
                ViewInstall[3][1] = Double.parseDouble(sub_properties.get("value").getAsString()) * PI / 180.0;
            } else if (sub_properties.get("key").getAsString().equals("angle_sight_Zaxis") && sub_properties.get("group").getAsString().equals("payload4")) {
                ViewInstall[3][2] = Double.parseDouble(sub_properties.get("value").getAsString()) * PI / 180.0;
            }
            //最大侧摆角
            else if (sub_properties.get("key").getAsString().equals("roll_angle_max")) {
                RollMax = Double.parseDouble(sub_properties.get("value").getAsString()) * PI / 180.0;
            }

        }


        //轨道六根数转化为惯性系下位置速度
        double[] Position0 = new double[3];
        double[] Velocity0 = new double[3];
        OrbitSixToGEI(orbit0, Position0, Velocity0);
        double[] orbit = {Position0[0], Position0[1], Position0[2], Velocity0[0], Velocity0[1], Velocity0[2]};


        int year = start.getYear();
        int month = start.getMonthValue();
        int day = start.getDayOfMonth();
        int hour = start.getHour();
        int minute = start.getMinute();
        int second = start.getSecond();
        double[] start_hms = {(double) year, (double) month, (double) day, (double) hour, (double) minute, (double) second};
        double[] start_ymd = {(double) year, (double) month, (double) day, 8, 0, 0};
        double[] end_hmsymd = {(double) end.getYear(), (double) end.getMonthValue(), (double) end.getDayOfMonth(), (double) end.getHour(), (double) end.getMinute(), (double) end.getSecond()};
        double x = (JD(start_hms) - JD(start_ymd)) * (24 * 60 * 60);
        double xmax = (JD(end_hmsymd) - JD(start_ymd)) * (24 * 60 * 60);

        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime current = start;
        double[] err = new double[6];
        double[] y = new double[6];
        int j = 0;
        double[] sa = new double[3];
        double[] subsat = new double[3];

        double[] Y = new double[]{orbit[0], orbit[1], orbit[2], orbit[3], orbit[4], orbit[5]};
        double h = step;
        JsonArray orbit_attitud = new JsonArray();
        long step2 = (long) h;

        //将数据存入数组
        double Num = (xmax - x) / h;
        int OrbitalDataNum = (new Double(Num)).intValue();
        double[][] Orbital_Time = new double[OrbitalDataNum][6];
        double[][] Orbital_SatPosition = new double[OrbitalDataNum][3];
        double[][] Orbital_SatVelocity = new double[OrbitalDataNum][3];
        double[][] Orbital_SatPositionLLA = new double[OrbitalDataNum][3];
        double[][] Orbital_Subpotion = new double[OrbitalDataNum][3];
        double[][] Orbital_ViewArea = new double[OrbitalDataNum][4 * ViewNum];

        ArrayList<Document> os = new ArrayList<>();

        System.out.println("计算轨道数据中...");

        MongoClient mongoClient = MangoDBConnector.getClient();
        MongoDatabase mongoDatabase = mongoClient.getDatabase(DbDefine.DB_NAME);
        MongoCollection<Document> orbitDB = mongoDatabase.getCollection("orbit_attitude");
        long cnt = 0;
        while (x < xmax) {
            JsonObject jsonObject = new JsonObject();
            if (h > (xmax - x)) {
                h = xmax - x;
            }
            RK4(Y, y, x, year, month, day, h, sa);
            P2subsatP(y, subsat);
            Y[0] = y[0];
            Y[1] = y[1];
            Y[2] = y[2];
            Y[3] = y[3];
            Y[4] = y[4];
            Y[5] = y[5];
            current = current.plusSeconds(step2);

            //substring(n,m):截取string第n+1到m位置的字符
            //String StringTime = "2019-06-27 04:00:00";
            LocalDateTime Dtime_point = current;
            String time_point = df.format(Dtime_point.plusHours(-8));
            String StringTime = time_point.toString();
            String StringTime_GetYear = StringTime.substring(0, 4);
            String StringTime_GetMonth = StringTime.substring(5, 7);
            String StringTime_GetDay = StringTime.substring(8, 10);
            String StringTime_GetHour = StringTime.substring(11, 13);
            String StringTime_GetMinute = StringTime.substring(14, 16);
            String StringTime_GetSecond = StringTime.substring(17, 19);
            Orbital_Time[j][0] = Double.parseDouble(StringTime_GetYear);
            Orbital_Time[j][1] = Double.parseDouble(StringTime_GetMonth);
            Orbital_Time[j][2] = Double.parseDouble(StringTime_GetDay);
            Orbital_Time[j][3] = Double.parseDouble(StringTime_GetHour);
            Orbital_Time[j][4] = Double.parseDouble(StringTime_GetMinute);
            Orbital_Time[j][5] = Double.parseDouble(StringTime_GetSecond);
            Orbital_SatPosition[j][0] = y[0];
            Orbital_SatPosition[j][1] = y[1];
            Orbital_SatPosition[j][2] = y[2];
            Orbital_SatVelocity[j][0] = y[3];
            Orbital_SatVelocity[j][1] = y[4];
            Orbital_SatVelocity[j][2] = y[5];
            Orbital_SatPositionLLA[j][0] = sa[0] * 180.0 / PI;
            Orbital_SatPositionLLA[j][1] = sa[1] * 180.0 / PI;
            Orbital_SatPositionLLA[j][2] = sa[2];
            Orbital_Subpotion[j][0] = subsat[0];
            Orbital_Subpotion[j][1] = subsat[1];
            Orbital_Subpotion[j][2] = subsat[2];

            //可见走廊
            double Time_UTC = 0;
            ViewArea(Orbital_SatPosition[j], Orbital_SatVelocity[j], ViewInstall, ViewAng, ViewNum, RollMax, Orbital_Time[j], Time_UTC, Orbital_ViewArea[j]);
            x += h;

            //数据输出
            jsonObject.addProperty("time_point", time_point);
            jsonObject.addProperty("P_x", y[0]);
            jsonObject.addProperty("P_y", y[1]);
            jsonObject.addProperty("P_z", y[2]);
            jsonObject.addProperty("lon", Orbital_SatPositionLLA[j][0]);
            jsonObject.addProperty("lat", Orbital_SatPositionLLA[j][1]);
            jsonObject.addProperty("H", Orbital_SatPositionLLA[j][2]);
            jsonObject.addProperty("Vx", y[3]);
            jsonObject.addProperty("Vy", y[4]);
            jsonObject.addProperty("Vz", y[5]);
            jsonObject.addProperty("satellite_point_x", subsat[0]);
            jsonObject.addProperty("satellite_point_y", subsat[1]);
            jsonObject.addProperty("satellite_point_z", subsat[2]);

            JsonArray orbit_attitud_lp = new JsonArray();

            for (int i = 0; i < ViewNum; i++) {
                JsonObject jsonObject1 = new JsonObject();
                jsonObject1.addProperty("load_amount", ViewNum);
                jsonObject1.addProperty("load_number", i + 1);
                jsonObject1.addProperty("visible_left_lon", Orbital_ViewArea[j][4 * i + 0]);
                jsonObject1.addProperty("visible_left_lat", Orbital_ViewArea[j][4 * i + 1]);
                jsonObject1.addProperty("visible_right_lon", Orbital_ViewArea[j][4 * i + 2]);
                jsonObject1.addProperty("visible_right_lat", Orbital_ViewArea[j][4 * i + 3]);
                orbit_attitud_lp.add(jsonObject1);
            }

            jsonObject.add("load_properties", orbit_attitud_lp);

            Document doc = Document.parse(jsonObject.toString());
            doc.append("time_point", Date.from(timeRaw.plusMillis((long) (1000 * j * step))));

            os.add(doc);
            cnt++;

            if(os.size() > 10000){
                orbitDB.insertMany(os);
                os.clear();
                cnt = 0;
            }

            j++;
        }

        if(os.size() > 0) {
            orbitDB.insertMany(os);
            os.clear();
        }
        mongoClient.close();

        //阳光规避计算及输出
//        JsonArray avoidance_sunlight=new JsonArray();
//        int[] SunAvoidTimePeriod=new int[10];
//        int SunAvoidTimePeriodNum=AvoidSunshineIITest( Orbital_Time,Orbital_SatPosition,SunAvoidTimePeriod);
//        for (int i = 0; i < SunAvoidTimePeriodNum; i++) {
//            LocalDateTime SunAvoidStar=start;
//            SunAvoidStar=SunAvoidStar.plusSeconds((long) (h*SunAvoidTimePeriod[2*i]));
//            String Startime_point=df.format(SunAvoidStar.plusHours(-8));
//            LocalDateTime SunAvoidEnd=start;
//            SunAvoidEnd=SunAvoidEnd.plusSeconds((long) (h*SunAvoidTimePeriod[2*i+1]));
//            String Endtime_point=df.format(SunAvoidEnd.plusHours(-8));
//            JsonObject SunAvoidjsonObject=new JsonObject();
//            SunAvoidjsonObject.addProperty("amount_window",SunAvoidTimePeriodNum);
//            SunAvoidjsonObject.addProperty("window_number",i+1);
//            SunAvoidjsonObject.addProperty("start_time",Startime_point);
//            SunAvoidjsonObject.addProperty("end_time",Endtime_point);
//            avoidance_sunlight.add(SunAvoidjsonObject);
//        }


    }

    public static LocalDateTime dateConvertToLocalDateTime(Date date) {
        return date.toInstant().atOffset(ZoneOffset.of("+8")).toLocalDateTime();
    }

    //计算卫星可见走廊
    private static void ViewArea(double Position[], double Velocity[], double ViewInstall[][], double ViewAng[][], int ViewNum, double RollMax, double Time[], double Time_UTC, double ViewAreaPoint[]) {


        double[] nv = {Velocity[0] / Math.sqrt(Math.pow(Velocity[0], 2) + Math.pow(Velocity[1], 2) + Math.pow(Velocity[2], 2)),
                Velocity[1] / Math.sqrt(Math.pow(Velocity[0], 2) + Math.pow(Velocity[1], 2) + Math.pow(Velocity[2], 2)),
                Velocity[2] / Math.sqrt(Math.pow(Velocity[0], 2) + Math.pow(Velocity[1], 2) + Math.pow(Velocity[2], 2))};
        double r = Math.sqrt(Math.pow(Position[0], 2) + Math.pow(Position[1], 2) + Math.pow(Position[2], 2));
        double theta = Math.asin(R_earth / r);

        double alpha, beta, theta_V, theta_xz, theta_yz, ViewAng_min;
        double R[][] = new double[3][3];
        double r_beta[] = new double[3];
        double SubSat[] = new double[3];
        double SubSat_GEI[] = new double[3];
        for (int j = 0; j < ViewNum; j++) {
            theta_xz = Math.atan(Math.cos(ViewInstall[j][0]) / Math.cos(ViewInstall[j][2]));
            theta_yz = Math.atan(Math.cos(ViewInstall[j][1]) / Math.cos(ViewInstall[j][2]));
            if ((ViewAng[j][2] + theta_yz + RollMax) >= theta) {
                theta_V = Math.asin(R_earth / r);
                beta = -(Math.PI / 2 - theta_V);
            } else {
                alpha = Math.asin((Math.sin(theta_yz + ViewAng[j][2] + RollMax) * r) / R_earth);
                beta = -(alpha - (theta_yz + ViewAng[j][2] + RollMax));
            }
            R[0][0] = nv[0] * nv[0] * (1 - Math.cos(beta)) + Math.cos(beta);
            R[0][1] = nv[0] * nv[1] * (1 - Math.cos(beta)) + nv[2] * Math.sin(beta);
            R[0][2] = nv[0] * nv[2] * (1 - Math.cos(beta)) - nv[1] * Math.sin(beta);
            R[1][0] = nv[0] * nv[1] * (1 - Math.cos(beta)) - nv[2] * Math.sin(beta);
            R[1][1] = nv[1] * nv[1] * (1 - Math.cos(beta)) + Math.cos(beta);
            R[1][2] = nv[1] * nv[2] * (1 - Math.cos(beta)) + nv[0] * Math.sin(beta);
            R[2][0] = nv[0] * nv[2] * (1 - Math.cos(beta)) + nv[1] * Math.sin(beta);
            R[2][1] = nv[1] * nv[2] * (1 - Math.cos(beta)) - nv[0] * Math.sin(beta);
            R[2][2] = nv[2] * nv[2] * (1 - Math.cos(beta)) + Math.cos(beta);
            r_beta[0] = Position[0] * R[0][0] + Position[1] * R[1][0] + Position[2] * R[2][0];
            r_beta[1] = Position[0] * R[0][1] + Position[1] * R[1][1] + Position[2] * R[2][1];
            r_beta[2] = Position[0] * R[0][2] + Position[1] * R[1][2] + Position[2] * R[2][2];
            PosionToSubSat(r_beta, Time, Time_UTC, SubSat, SubSat_GEI);
            ViewAreaPoint[4 * j + 0] = SubSat[0];
            ViewAreaPoint[4 * j + 1] = SubSat[1];

            if (Math.abs(theta_yz - ViewAng[j][3] - RollMax) >= theta) {
                theta_V = Math.asin(R_earth / r);
                beta = Math.PI / 2 - theta_V;
                beta = -beta;
            } else {
                alpha = Math.asin((Math.sin(theta_yz - ViewAng[j][3] - RollMax) * r) / R_earth);
                beta = alpha - (theta_yz - ViewAng[j][3] - RollMax);
                beta = -beta;
            }
            R[0][0] = nv[0] * nv[0] * (1 - Math.cos(beta)) + Math.cos(beta);
            R[0][1] = nv[0] * nv[1] * (1 - Math.cos(beta)) + nv[2] * Math.sin(beta);
            R[0][2] = nv[0] * nv[2] * (1 - Math.cos(beta)) - nv[1] * Math.sin(beta);
            R[1][0] = nv[0] * nv[1] * (1 - Math.cos(beta)) - nv[2] * Math.sin(beta);
            R[1][1] = nv[1] * nv[1] * (1 - Math.cos(beta)) + Math.cos(beta);
            R[1][2] = nv[1] * nv[2] * (1 - Math.cos(beta)) + nv[0] * Math.sin(beta);
            R[2][0] = nv[0] * nv[2] * (1 - Math.cos(beta)) + nv[1] * Math.sin(beta);
            R[2][1] = nv[1] * nv[2] * (1 - Math.cos(beta)) - nv[0] * Math.sin(beta);
            R[2][2] = nv[2] * nv[2] * (1 - Math.cos(beta)) + Math.cos(beta);
            r_beta[0] = Position[0] * R[0][0] + Position[1] * R[1][0] + Position[2] * R[2][0];
            r_beta[1] = Position[0] * R[0][1] + Position[1] * R[1][1] + Position[2] * R[2][1];
            r_beta[2] = Position[0] * R[0][2] + Position[1] * R[1][2] + Position[2] * R[2][2];
            PosionToSubSat(r_beta, Time, Time_UTC, SubSat, SubSat_GEI);
            ViewAreaPoint[4 * j + 2] = SubSat[0];
            ViewAreaPoint[4 * j + 3] = SubSat[1];
        }
    }

    private static int AvoidSunshineIITest(double[][] Orbital_Time, double[][] Orbital_SatPosition, int[] SunAvoidTimePeriod) {
        int Flag_tBefore = 0;
        int Avoid_Flag = 0;
        int Flag_t = 0;
        int SunAvoidTimePeriodNum = 0;
        for (int i = 0; i < Orbital_SatPosition.length; i++) {
            double Time_JD = JD(Orbital_Time[i]);
            double[] r_sun = new double[3];//地心惯性坐标系下太阳位置
            double[] su = new double[2];//赤经和赤纬
            double rad_sun;//太阳地球的距离
            rad_sun = Sun(Time_JD, r_sun, su);
            double a = r_sun[0] * Orbital_SatPosition[i][0] + r_sun[1] * Orbital_SatPosition[i][1] + r_sun[2] * Orbital_SatPosition[i][2];
            double r_Sat = sqrt(Orbital_SatPosition[i][0] * Orbital_SatPosition[i][0] + Orbital_SatPosition[i][1] * Orbital_SatPosition[i][1] + Orbital_SatPosition[i][2] * Orbital_SatPosition[i][2]);
            double theta = acos(a / (rad_sun * r_Sat));

            if (theta >= 175 * PI / 180.0) {
                Avoid_Flag = 1;
                Flag_tBefore = Flag_t;
                Flag_t = Avoid_Flag;
            } else {
                Avoid_Flag = 0;
                Flag_tBefore = Flag_t;
                Flag_t = Avoid_Flag;
            }

            //判定开始结束时间
            if (Flag_tBefore == 0 && Flag_t == 1) {
                SunAvoidTimePeriod[2 * SunAvoidTimePeriodNum] = i;
            } else if (Flag_tBefore == 1 && Flag_t == 0) {
                SunAvoidTimePeriod[2 * SunAvoidTimePeriodNum + 1] = i - 1;
                SunAvoidTimePeriodNum = SunAvoidTimePeriodNum + 1;
            }
            if (i == Orbital_SatPosition.length - 1 && Flag_t == 1) {
                SunAvoidTimePeriod[2 * SunAvoidTimePeriodNum + 1] = i;
                SunAvoidTimePeriodNum = SunAvoidTimePeriodNum + 1;
            }
        }
        return SunAvoidTimePeriodNum;
    }

    //由卫星位置计算星下点的经纬度，以及卫星位置的经纬高，以及卫星星下点在地心赤道惯性系中的位置
    private static void PosionToSubSat(double posion_GEI[], double Time[], double Time_UTC, double SubSat[], double SubSat_GEI[]) {
        double omega = 1.0027 * 180 / 43200;//地球自转角速度，单位为：度/秒


        double x = posion_GEI[0];
        double y = posion_GEI[1];
        double z = posion_GEI[2];

        double Dec;
        if (x != 0 || y != 0) {
            Dec = Math.atan(z / (Math.pow((Math.pow(x, 2) + Math.pow(y, 2)), 0.5))) * 180 / Math.PI;
        } else {
            if (z > 0)
                Dec = 90;
            else
                Dec = -90;
        }
        double RA = 0;
        if (x > 0)
            RA = Math.atan(y / x) * 180 / Math.PI;
        if (x < 0) {
            if (y >= 0)
                RA = 180 + Math.atan(y / x) * 180 / Math.PI;
            else
                RA = -180 + Math.atan(y / x) * 180 / Math.PI;
        }
        if (x == 0) {
            if (y >= 0)
                RA = 90;
            else
                RA = -90;
        }

        double lat = Dec;
        double GAST = Time_GAST(Time);
        GAST = GAST + omega * Time_UTC;
        double lon = RA - GAST;

        //限定范围
        if (lon > 180)
            lon = lon - 360;
        else if (lon <= -180)
            lon = lon + 360;

        SubSat[0] = lon;
        SubSat[1] = lat;
        SubSat[2] = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2)) - R_earth;

        double r = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2));
        SubSat_GEI[0] = R_earth * x / r;
        SubSat_GEI[1] = R_earth * y / r;
        SubSat_GEI[2] = R_earth * z / r;
    }

    //计算参考时间的格林尼治赤经
    private static double Time_GAST(double Time[]) {
        /*
        double year_UT=Time[0];
        double month_UT=Time[1];
        double day_UT=Time[2];
        double hour_UT=Time[3];
        double minute_UT=Time[4];
        double second_UT=Time[5];
         */
        //double JD=JulianDate(2019,8,14,0);
        double JD = JD(Time);
        double D = JD - 2451545.0;
        double T = D / 36525.0;
        double GMST = 280.46061837 + 360.98564736629 * (JD - 2451545.0) + 0.000387933 * Math.pow(T, 2) - Math.pow(T, 3) / 38710000.0;
        GMST = GMST % 360;
        double Epsilon_m = (23 + 26 / 60.0 + 21.448 / 3600.0) - (46.8150 / 3600.0) * T - (0.00059 / 3600.0) * Math.pow(T, 2) + (0.001813 / 3600.0) * Math.pow(T, 3);

        double L = 280.4665 + 36000.7698 * T;
        double dL = 218.3165 + 481267.8813 * T;
        double Omega = 125.04452 - 1934.136261 * T;
        double dPsi = -17.20 * Math.sin(Omega * Math.PI / 180.0) - 1.32 * Math.sin(2 * L * Math.PI / 180.0) - 0.23 * Math.sin(2 * dL * Math.PI / 180.0) + 0.21 * Math.sin(2 * Omega * Math.PI / 180.0);
        double dEpsilon = 9.20 * Math.cos(Omega * Math.PI / 180.0) + 0.57 * Math.cos(2 * L * Math.PI / 180.0) + 0.10 * Math.cos(2 * dL * Math.PI / 180.0) - 0.09 * Math.cos(2 * Omega * Math.PI / 180.0);
        dPsi = dPsi / 3600.0;
        dEpsilon = dEpsilon / 3600.0;

        double GAST = GMST + dPsi * Math.cos((Epsilon_m + dEpsilon) * Math.PI / 180.0);
        GAST = GAST % 360;

        return GAST;
    }

    //儒略日计算
    private static double JD(double Time[]) {
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

    //轨道六根数转化为惯性系下速度位置
    private static void OrbitSixToGEI(double ClassicalOrbitalElements[], double Position[], double Velocity[]) {
        double sma = ClassicalOrbitalElements[0] * 1000;
        double ecc = ClassicalOrbitalElements[1];
        double inc = ClassicalOrbitalElements[2] * PI / 180.0;
        double argper = ClassicalOrbitalElements[4] * PI / 180.0;
        double raan = ClassicalOrbitalElements[3] * PI / 180.0;
        double tanom = ClassicalOrbitalElements[5] * PI / 180.0;

        //double mu=398600.4415;//地球引力常数（m^3/s^2）

        double slr = sma * (1 - ecc * ecc);
        double rm = slr / (1 + ecc * Math.cos(tanom));

        double arglat = argper + tanom;
        double sarglat = Math.sin(arglat);
        double carglat = Math.cos(arglat);

        double c4 = Math.sqrt(mu / slr);
        double c5 = ecc * Math.cos(argper) + carglat;
        double c6 = ecc * Math.sin(argper) + sarglat;

        double sinc = Math.sin(inc);
        double cinc = Math.cos(inc);
        double sraan = Math.sin(raan);
        double craan = Math.cos(raan);

        Position[0] = rm * (craan * carglat - sraan * sarglat * cinc);
        Position[1] = rm * (sraan * carglat + craan * sarglat * cinc);
        Position[2] = rm * sarglat * sinc;

        Velocity[0] = -c4 * (c6 * craan + c5 * sraan * cinc);
        Velocity[1] = -c4 * (c6 * sraan - c5 * craan * cinc);
        Velocity[2] = c4 * c5 * sinc;
    }

    private static void main(String[] arr) {
        JsonObject json = new JsonObject();
        String start0 = "2018-08-01 21:22:22";
        String end0 = "2018-08-01 22:22:22";
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime start = LocalDateTime.parse(start0, df);
        LocalDateTime end = LocalDateTime.parse(end0, df);
        double[] Y = new double[]{6678140, 0, -1.438, 0, 7725.76, -0.000410};
        double h = 2.0;//步长
//        OrbitPredictorII(start, end, h, Y, json);
    }

}
