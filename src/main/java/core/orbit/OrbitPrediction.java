package core.orbit;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import static java.lang.Math.sqrt;

class OrbitPrediction {
    public static double GRAVP = 1;
    public static double MOON = 1;
    public static double SUN = 1;
    public static double SRP = 1;
    public static double ADRAG = 0;
    public static double OnlyJ2 = 0;

    public static double j2 = 1082.626e-6;
    public static double j3 = -2.5356e-6;
    public static double j4 = -1.62336e-6;

    public static double j22 = 0;
    public static double j31 = 0;
    public static double j32 = 0;
    public static double j33 = 0;
    public static double j41 = 0;
    public static double j42 = 0;
    public static double j43 = 0;
    public static double j44 = 0;

    public static double l22 = -14.545;
    public static double l31 = 7.0805;
    public static double l32 = -17.4649;
    public static double l33 = 21.2097;
    public static double l41 = -138.756;
    public static double l42 = 31.0335;
    public static double l43 = -3.8459;
    public static double l44 = 30.792;

    public static double mu = 398600.4418e9;
    public static double mu_moon = 4.902799e6;
    public static double mu_sun = 1.327124e14;
    public static double PSR = 4.51e-6;
    public static double CR = 1.0;
    public static double AMRatio = 0.02;
    public static double C_D = 2.200000;
    public static double rho_0 = 0.36;
    public static double h0 = 37.4;
    public static double AUtokm = 1.49597870e8;
    public static double kmtom = 10e3;
    public static double rate = 4.178074216e-3;
    public static double DtR = 3.1415926 / 180;

    public static double DENSMOD = 2;
    public static double PI = 3.1415926;

    public static double R_earth = 6378136.3;
    public static double eccent = 0.08182;

    public static double GetDensity(double height) {
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
    public static void RK4(double[] X, double[] xout, double t, int year, int month, int day, double h, double[] sa) {
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
    public static void DXDT(double t, double[] X, double[] DX, int year, int month, int day)   // 轨道动力学
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
    public static void P2subsatP(double[] P, double[] subsat) {
        double omega = 1.0027 * 180 / 43200;
        double x = P[0];
        double y = P[1];
        double z = P[2];

        //地心赤道坐标系星下点位置
        subsat[0] = R_earth * (P[0] / sqrt(P[0] * P[0] + P[1] * P[1] + P[2] * P[2]));
        subsat[1] = R_earth * (P[1] / sqrt(P[0] * P[0] + P[1] * P[1] + P[2] * P[2]));
        subsat[2] = R_earth * (P[2] / sqrt(P[0] * P[0] + P[1] * P[1] + P[2] * P[2]));
    }

    public static void trigfunc(int mm, double lat, double lon, double[] CN, double[] SN, double[] TN) {
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
    public static void ECI_ECEF(double JD, double[] R, double[] sa) {
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
    public static double JulianDate(int year, int month, int day, double UT) {
        double JD, C1, C2, C3;
        C1 = 367.0 * year;
        C2 = (int) ((7 * (year + (int) ((month + 9) / 12))) * 0.25);
        C3 = (int) (275 * month / 9);
        UT = UT / 3600.0;
        JD = (C1 - C2 + C3) + day + 1721013.5 + UT / 24.0;
        return JD;
    }

    public static void legendre(int nn, double x, double[][] p) {
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
    public static double Moon(double JD, double[] r_moon) {
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
    public static double Sun(double JD, double[] r_sun, double[] su) {
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

    public static double Exp_model(double h) {
        double rho = rho_0 * Math.exp(h / h0);
        return rho;

    }

    public static double app_sidereal_time(double JD) {
        double T_TDB = (JD - 2451545.0) / 36525.0;
        double hour = (JD - (int) JD) * 24;
        return mod(6.697374558 + 2400.05133691 * T_TDB + 2.586222 * 0.00001 * Math.pow(T_TDB, 2)
                - 1.722222 * 0.000000001 * Math.pow(T_TDB, 3) + 1.002737791737697 * hour, 24) * 15;
    }

    //求余
    public static double mod(double x, double y) {
        int n;
        n = (int) (x / y);
        return x - n * y;
    }

    //更改输入输出
    public static JsonArray OrbitPredictorII(LocalDateTime start, LocalDateTime end, double step, double[] orbit0, JsonObject json) {

        int year = start.getYear();
        int month = start.getMonthValue();
        int day = start.getDayOfMonth();
        double x = start.toEpochSecond(ZoneOffset.of("+8"));//获取秒数
        double xmax = end.toEpochSecond(ZoneOffset.of("+8"));
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime current = start;
//        double[] err = new double[6];
        double[] y = new double[6];
        int j = 0;
        double[] sa = new double[3];
        double[] subsat = new double[3];

        double[] Y = new double[]{orbit0[0], orbit0[1], orbit0[2], orbit0[3], orbit0[4], orbit0[5]};
        double h = step;
        JsonArray orbit_attitud = new JsonArray();
        long step2 = (long) h;
        while (x < xmax) {

            JsonObject jsonObject = new JsonObject();
            if (h > (xmax - x)) {
                h = xmax - x;
            }
            RK4(Y, y, x, year, month, day, h, sa);
            P2subsatP(y, subsat);
            x += h;
//            current = current.plusSeconds(step2);
//            String time_point = df.format(current);
//
//            jsonObject.addProperty("time_point", time_point);
            jsonObject.addProperty("P_x", y[0]);
            jsonObject.addProperty("P_y", y[1]);
            jsonObject.addProperty("P_z", y[2]);
            jsonObject.addProperty("lon", sa[0]);
            jsonObject.addProperty("lat", sa[1]);
            jsonObject.addProperty("H", sa[2]);
            jsonObject.addProperty("Vx", y[3]);
            jsonObject.addProperty("Vy", y[4]);
            jsonObject.addProperty("Vz", y[5]);
            jsonObject.addProperty("satellite_point_x", subsat[0]);
            jsonObject.addProperty("satellite_point_y", subsat[1]);
            jsonObject.addProperty("satellite_point_z", subsat[2]);

            jsonObject.addProperty("visible_left_lon", 0);
            jsonObject.addProperty("visible_left_lat", 0);
            jsonObject.addProperty("visible_right_lon", 0);

            JsonArray orbit_attitud_lp = new JsonArray();
            JsonObject jsonObject1 = new JsonObject();
            jsonObject1.addProperty("load_amount", 2);
            jsonObject1.addProperty("load_number", 1);
            jsonObject1.addProperty("width_along_track", 1);
            jsonObject1.addProperty("width_vertical_track", 1);
            jsonObject1.addProperty("width_along_track_true", 1);
            jsonObject1.addProperty("width_vertical_track_true", 1);
            orbit_attitud_lp.add(jsonObject1);
            JsonObject jsonObject2 = new JsonObject();
            jsonObject2.addProperty("load_amount", 2);
            jsonObject2.addProperty("load_number", 2);
            jsonObject2.addProperty("width_along_track", 2);
            jsonObject2.addProperty("width_vertical_track", 2);
            jsonObject2.addProperty("width_along_track_true", 2);
            jsonObject2.addProperty("width_vertical_track_true", 2);
            orbit_attitud_lp.add(jsonObject2);
            jsonObject.add("load_properties", orbit_attitud_lp);

            jsonObject.addProperty("yaw_angle", 0);
            jsonObject.addProperty("roll_angle", 0);
            jsonObject.addProperty("pitch_angle", 0);
            jsonObject.addProperty("V_yaw_angle", 0);
            jsonObject.addProperty("V_roll_angle", 0);
            jsonObject.addProperty("V_pitch_angle", 0);
            orbit_attitud.add(jsonObject);

            j++;
        }
//        System.out.println(orbit_attitud);
        //System.out.print("/n");
        return orbit_attitud;
    }

    public static LocalDateTime dateConvertToLocalDateTime(Date date) {
        return date.toInstant().atOffset(ZoneOffset.of("+8")).toLocalDateTime();
    }

    public static void main(String[] arr) {
        JsonObject json = new JsonObject();
        String start0 = "2018-08-01 21:22:22";
        String end0 = "2018-08-01 22:22:22";
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime start = LocalDateTime.parse(start0, df);
        LocalDateTime end = LocalDateTime.parse(end0, df);
        double[] Y = new double[]{6678140, 0, -1.438, 0, 7725.76, -0.000410};
        double h = 2.0;//步长
        OrbitPredictorII(start, end, h, Y, json);
    }

}
