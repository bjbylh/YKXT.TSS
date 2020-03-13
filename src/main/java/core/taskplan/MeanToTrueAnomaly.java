package core.taskplan;

public class MeanToTrueAnomaly {

    /**
     * @param SemimajorAxis   轨道半长轴，单位：米
     * @param Eccentricity    轨道偏心率
     * @param Inclination     轨道倾角，单位：度
     * @param ArgumentPerigee 轨道近地点幅角，单位：度
     * @param RAAN            轨道升交点赤经，单位：度
     * @param MeanAnomaly     轨道平近点角，单位：度
     * @return 轨道真近点角，单位：度
     */
    public static double MeanToTrueAnomalyII(double SemimajorAxis, double Eccentricity, double Inclination, double ArgumentPerigee, double RAAN, double MeanAnomaly) {
        MeanAnomaly = MeanAnomaly * Math.PI / 180.0;
        double Es = MeanAnomaly + Eccentricity * Math.sin(MeanAnomaly);
        double EsBefore = Es;
        Boolean EsFlag = true;
        while (EsFlag) {
            EsBefore = Es;
            Es = MeanAnomaly + Eccentricity * Math.sin(EsBefore);
            if (Math.abs(Es - EsBefore) <= 0.00001) {
                EsFlag = false;
            }
        }
        double TrueAnomaly = 0;
        double y = Math.sqrt(1 - Eccentricity * Eccentricity) * Math.sin(Es);
        double x = Math.cos(Es) - Eccentricity;
        TrueAnomaly = Math.atan2(y, x);
        return TrueAnomaly * 180 / Math.PI;
    }

    public static void main(String[] args) {
        double v = MeanToTrueAnomaly.MeanToTrueAnomalyII(0.0, 0.719646, 0.0, 0.0, 0.0, 178.691);
        System.out.println(v);
    }
}
