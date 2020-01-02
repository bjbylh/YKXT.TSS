package core.taskplan;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import common.mongo.MangoDBConnector;
import org.bson.Document;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;

import static java.lang.Math.*;

//import common.mongo.MangoDBConnector;

public class OrderOverall {
    private static double Re = 6371393;                  //地球半径，单位为：米
    private static double ViewLength = 50000;              //视场宽度
    private static double MissionIntervalMin=100;        //任务最小间隔时间


    public static ArrayList<String> OrderOverallII(ArrayList<Document> ImageOrderjson) {
        ArrayList<String> missions = new ArrayList<>();
        //订单任务读取
        ArrayList<Document> ImageOrderjsonCopy = new ArrayList<>();
        for (Document d : ImageOrderjson) {
            Document copy = Document.parse(d.toJson());
            ImageOrderjsonCopy.add(copy);
        }
        ArrayList<Object> ImageRegion = new ArrayList<>();
        ArrayList<Object> Region = new ArrayList<>();
        ArrayList<Object> Name = new ArrayList<>();
        ArrayList<Object> ImageType = new ArrayList<>();
        ArrayList<Object> ExpectedCam = new ArrayList<>();
        ArrayList<Object> ExpectedStartTime = new ArrayList<>();
        ArrayList<Object> ExpectedEndTime = new ArrayList<>();
        ArrayList<Object> Priority = new ArrayList<>();
        ArrayList<Object> ImageMode = new ArrayList<>();
        ArrayList<Object> WorkMode = new ArrayList<>();
        ArrayList<Object> MinHeightOrbit = new ArrayList<>();
        ArrayList<Object> MaxHeightOrbit = new ArrayList<>();
        ArrayList<Object> MinStareTime = new ArrayList<>();
        ArrayList<Object> OrderState = new ArrayList<>();
        ArrayList<Object> UserAccount = new ArrayList<>();
        ArrayList<Object> ModifyTime = new ArrayList<>();
        ArrayList<Object> OrderSubmitTime = new ArrayList<>();
        ArrayList<Object> OrderNumber = new ArrayList<>();
        ArrayList<Object> MissioNumber = new ArrayList<>();

        ArrayList<Object> Instruction=new ArrayList<>();
        ArrayList<Object> StationNumber=new ArrayList<>();
        ArrayList<Object> RecordFileNo=new ArrayList<>();
        ArrayList<Object> AutoAsignRecordFile=new ArrayList<>();
        ArrayList<Object> MissionParams=new ArrayList<>();

        ArrayList<Object> ScanHeightOrbit=new ArrayList<>();
        ArrayList<Object> RollBias=new ArrayList<>();

        int OrderMissionNum = 0;
        for (Document document : ImageOrderjsonCopy) {
            ImageRegion.add(document.get("image_region"));
            Region.add(document.get("region"));
            Name.add(document.get("name"));
            ImageType.add(document.get("image_type"));
            ExpectedCam.add(document.get("expected_cam"));
            ExpectedStartTime.add(document.get("expected_start_time"));
            ExpectedEndTime.add(document.get("expected_end_time"));
            Priority.add(document.get("priority"));
            ImageMode.add(document.get("image_mode"));
            WorkMode.add(document.get("work_mode"));
            MinHeightOrbit.add(document.get("min_height_orbit"));
            MaxHeightOrbit.add(document.get("max_height_orbit"));
            MinStareTime.add(document.get("min_stare_time"));
            OrderState.add(document.get("order_state"));
            UserAccount.add(document.get("user_account"));
            ModifyTime.add(document.get("modify_time"));
            OrderSubmitTime.add(document.get("order_submit_time"));
            OrderNumber.add(document.get("order_number"));
            MissioNumber.add(document.get("mission_number"));

            Instruction.add(document.get("instruction_block_params"));
            StationNumber.add(document.get("station_number"));
            RecordFileNo.add(document.get("record_file_no"));
            AutoAsignRecordFile.add(document.get("auto_asign_record_file"));
            MissionParams.add(document.get("mission_params"));

            ScanHeightOrbit.add(document.get("scan_height_orbit"));
            RollBias.add(document.get("scan_roll_bias"));

            OrderMissionNum = OrderMissionNum + 1;
        }

        //筛选出成像模式为常规的订单
        ArrayList<Integer> ConventionalImagModeList = new ArrayList<Integer>();
        int ConventionalImageNum = 0;
        for (int i = 0; i < OrderMissionNum; i++) {
            if (ImageMode.get(i).toString().equals("常规")) {
                ConventionalImagModeList.add(ConventionalImageNum, i);
                ConventionalImageNum = ConventionalImageNum + 1;
            }
        }
        //统筹结果
        ArrayList<Integer> OverallResultList = new ArrayList<Integer>();
        ArrayList<ArrayList<Integer>> OverallMissionNumList = new ArrayList<ArrayList<Integer>>();
        for (int i = 0; i < OrderMissionNum; i++) {
            OverallResultList.add(i, 1);
            ArrayList<Integer> arrayList = new ArrayList<Integer>();
            arrayList.add(i);
            OverallMissionNumList.add(i, arrayList);
        }

        for (int i = 0; i < ConventionalImageNum; i++) {
            for (int j = i + 1; j < ConventionalImageNum; j++) {
                if (WorkMode.get(ConventionalImagModeList.get(i)).toString().equals(WorkMode.get(ConventionalImagModeList.get(j)).toString())) {
                    ArrayList<double[]> MissionTargetArea_iList = new ArrayList<double[]>();
                    ArrayList<double[]> MissionTargetArea_jList = new ArrayList<double[]>();
                    Document target_region_i = (Document) ImageRegion.get(ConventionalImagModeList.get(i));
                    MissionTargetArea_iList = GetRegionPoint(target_region_i);
                    Document target_region_j = (Document) ImageRegion.get(ConventionalImagModeList.get(j));
                    MissionTargetArea_jList = GetRegionPoint(target_region_j);
                    boolean CombineFlag = true;
                    for (int k = 0; k < MissionTargetArea_iList.size(); k++) {
                        for (int l = 0; l < MissionTargetArea_jList.size(); l++) {
                            double[] TargetArea_i = MissionTargetArea_iList.get(k);
                            double[] TargetArea_j = MissionTargetArea_jList.get(l);

                            double WA = TargetArea_i[1] * PI / 180.0;
                            double JA = TargetArea_i[0] * PI / 180.0;
                            double WB = TargetArea_j[1] * PI / 180.0;
                            double JB = TargetArea_j[0] * PI / 180.0;
                            double a = WA - WB;
                            double b = JA - JB;
                            double s = 2 * asin(sqrt(pow(sin(a / 2), 2) + cos(WA) * cos(WB) * pow(sin(b / 2), 2)));
                            s = s * Re;
                            if (s > ViewLength) {
                                CombineFlag = false;
                                break;
                            }
                        }
                        if (CombineFlag == false) {
                            break;
                        }
                    }
                    //判定指令部分
                    if (CombineFlag == true) {
                        if (Instruction.get(ConventionalImagModeList.get(i)) == Instruction.get(ConventionalImagModeList.get(j)) &&
                                StationNumber.get(ConventionalImagModeList.get(i))==StationNumber.get(ConventionalImagModeList.get(j)) &&
                                RecordFileNo.get(ConventionalImagModeList.get(i))==RecordFileNo.get(ConventionalImagModeList.get(j)) &&
                                AutoAsignRecordFile.get(ConventionalImagModeList.get(i))==AutoAsignRecordFile.get(ConventionalImagModeList.get(j)) &&
                                MissionParams.get(ConventionalImagModeList.get(i))==MissionParams.get(ConventionalImagModeList.get(j))) {
                            CombineFlag=true;
                        }else {
                            CombineFlag=false;
                        }
                    }
                    if (CombineFlag == true) {
                        CombinedCollection(target_region_i,target_region_j);
                        ImageRegion.set(ConventionalImagModeList.get(i), target_region_i);
                        //
                        int OveralNum=OverallResultList.get(ConventionalImagModeList.get(i))+1;
                        OverallResultList.set(ConventionalImagModeList.get(i),OveralNum);
                        OverallResultList.set(ConventionalImagModeList.get(j),0);
                        ArrayList<Integer> arrayList=OverallMissionNumList.get(ConventionalImagModeList.get(i));
                        arrayList.add(ConventionalImagModeList.get(j));
                        OverallMissionNumList.set(ConventionalImagModeList.get(i),arrayList);

                        ConventionalImageNum = ConventionalImageNum - 1;
                        for (int k = j; k < ConventionalImageNum; k++) {
                            ConventionalImagModeList.set(k,k+1);
                        }
                    }
                }
            }
        }

        //数据传出
        MongoClient mongoClient = MangoDBConnector.getClient();
        //获取名为"temp"的数据库
        MongoDatabase mongoDatabase = mongoClient.getDatabase("temp");

        MongoCollection<Document> image_order = mongoDatabase.getCollection("image_order");
        for (int i = 0; i < OrderMissionNum; i++) {
            if (OverallResultList.get(i) != 0) {
                SimpleDateFormat dateFormat=new SimpleDateFormat("yyyyMMddHHmmss");
                Date startTimeDate= (Date) ExpectedStartTime.get(i);
                String startTimestr=dateFormat.format(startTimeDate);
                String imageModelstr=ImageMode.get(i).toString();
                if (imageModelstr.equals("常规")) {
                    imageModelstr="CG";
                }else if (imageModelstr.equals("凝视")) {
                    imageModelstr="NS";
                }else if (imageModelstr.equals("临边观测")) {
                    imageModelstr="LB";
                }else if (imageModelstr.equals("恒星定标")) {
                    imageModelstr="DB";
                }else if (imageModelstr.equals("定标")) {
                    imageModelstr="DB";
                }
                String mission_number = "im_" + startTimestr+"_"+imageModelstr+"_"+ Instant.now().toEpochMilli();

                Document ImageMissionjson = new Document();
                ImageMissionjson.append("image_region", ImageRegion.get(i));
                ImageMissionjson.append("region", Region.get(i));
                ImageMissionjson.append("name", Name.get(i));
                ImageMissionjson.append("image_type", ImageType.get(i));
                ImageMissionjson.append("expected_cam", ExpectedCam.get(i));
                ImageMissionjson.append("expected_start_time", ExpectedStartTime.get(i));
                ImageMissionjson.append("expected_end_time", ExpectedEndTime.get(i));
                ImageMissionjson.append("priority", Priority.get(i));
                ImageMissionjson.append("image_mode", ImageMode.get(i));
                ImageMissionjson.append("work_mode", WorkMode.get(i));
                ImageMissionjson.append("min_height_orbit", MinHeightOrbit.get(i));
                ImageMissionjson.append("max_height_orbit", MaxHeightOrbit.get(i));
                ImageMissionjson.append("min_stare_time", MinStareTime.get(i));
                ImageMissionjson.append("mission_number", mission_number);
                ImageMissionjson.append("mission_state", "待规划");
                ImageMissionjson.append("mission_interval_min", Double.toString(MissionIntervalMin));

                ImageMissionjson.append("instruction_block_params",Instruction.get(i));
                ImageMissionjson.append("station_number",StationNumber.get(i));
                ImageMissionjson.append("record_file_no",RecordFileNo.get(i));
                ImageMissionjson.append("auto_asign_record_file",AutoAsignRecordFile.get(i));
                ImageMissionjson.append("mission_params",MissionParams.get(i));

                ImageMissionjson.append("scan_height_orbit",ScanHeightOrbit.get(i));
                ImageMissionjson.append("scan_roll_bias",RollBias.get(i));


                ArrayList<Object> OrderNumber_List = new ArrayList<>();
                for (int j = 0; j < OverallResultList.get(i); j++) {
                    if(ImageOrderjson.get(OverallMissionNumList.get(i).get(j)).containsKey("_id"))
                        ImageOrderjson.get(OverallMissionNumList.get(i).get(j)).remove("_id");
                    ArrayList<Integer> arrayList=OverallMissionNumList.get(i);
                    OrderNumber_List.add(OrderNumber.get(arrayList.get(j)));
                    ImageOrderjson.get(arrayList.get(j)).append("mission_number", mission_number);
                    ImageOrderjson.get(arrayList.get(j)).append("order_state", "待规划");
                    Document modifiers = new Document();
                    modifiers.append("$set", ImageOrderjson.get(arrayList.get(j)));
//
                    image_order.updateOne(new Document("order_number", ImageOrderjson.get(arrayList.get(j)).getString("order_number")), modifiers, new UpdateOptions().upsert(true));
                }
                ImageMissionjson.append("order_numbers", OrderNumber_List);
                MongoCollection<Document> image_mission = mongoDatabase.getCollection("image_mission");
                image_mission.insertOne(ImageMissionjson);
                missions.add(mission_number);
            }
        }

        return missions;
    }

    private static ArrayList<String> OrderOverallIIOld(ArrayList<Document> ImageOrderjson) {
        ArrayList<String> missions = new ArrayList<>();
        //订单任务读取
        ArrayList<Document> ImageOrderjsonCopy = new ArrayList<>();
        for (Document d : ImageOrderjson) {
            Document copy = Document.parse(d.toJson());
            ImageOrderjsonCopy.add(copy);
        }
        ArrayList<Object> ImageRegion = new ArrayList<>();
        ArrayList<Object> Region = new ArrayList<>();
        ArrayList<Object> Name = new ArrayList<>();
        ArrayList<Object> ImageType = new ArrayList<>();
        ArrayList<Object> ExpectedCam = new ArrayList<>();
        ArrayList<Object> ExpectedStartTime = new ArrayList<>();
        ArrayList<Object> ExpectedEndTime = new ArrayList<>();
        ArrayList<Object> Priority = new ArrayList<>();
        ArrayList<Object> ImageMode = new ArrayList<>();
        ArrayList<Object> WorkMode = new ArrayList<>();
        ArrayList<Object> MinHeightOrbit = new ArrayList<>();
        ArrayList<Object> MaxHeightOrbit = new ArrayList<>();
        ArrayList<Object> MinStareTime = new ArrayList<>();
        ArrayList<Object> OrderState = new ArrayList<>();
        ArrayList<Object> UserAccount = new ArrayList<>();
        ArrayList<Object> ModifyTime = new ArrayList<>();
        ArrayList<Object> OrderSubmitTime = new ArrayList<>();
        ArrayList<Object> OrderNumber = new ArrayList<>();
        ArrayList<Object> MissioNumber = new ArrayList<>();
        int OrderMissionNum = 0;
        for (Document document : ImageOrderjsonCopy) {
            ImageRegion.add(document.get("image_region"));
            Region.add(document.get("region"));
            Name.add(document.get("name"));
            ImageType.add(document.get("image_type"));
            ExpectedCam.add(document.get("expected_cam"));
            ExpectedStartTime.add(document.get("expected_start_time"));
            ExpectedEndTime.add(document.get("expected_end_time"));
            Priority.add(document.get("priority"));
            ImageMode.add(document.get("image_mode"));
            WorkMode.add(document.get("work_mode"));
            MinHeightOrbit.add(document.get("min_height_orbit"));
            MaxHeightOrbit.add(document.get("max_height_orbit"));
            MinStareTime.add(document.get("min_stare_time"));
            OrderState.add(document.get("order_state"));
            UserAccount.add(document.get("user_account"));
            ModifyTime.add(document.get("modify_time"));
            OrderSubmitTime.add(document.get("order_submit_time"));
            OrderNumber.add(document.get("order_number"));
            MissioNumber.add(document.get("mission_number"));

            OrderMissionNum = OrderMissionNum + 1;
        }

        //筛选出成像模式为常规的订单
        int[] ConventionalImagMode = new int[OrderMissionNum];
        int ConventionalImageNum = 0;
        for (int i = 0; i < OrderMissionNum; i++) {
            if (ImageMode.get(i).toString().equals("常规")) {
                ConventionalImagMode[ConventionalImageNum] = i;
                ConventionalImageNum = ConventionalImageNum + 1;
            }
        }
        //统筹结果
        int[] OverallResult = new int[OrderMissionNum];
        int[][] OverallMissionNum = new int[OrderMissionNum][100];
        for (int i = 0; i < OrderMissionNum; i++) {
            OverallResult[i] = 1;
            OverallMissionNum[i][0] = i;
        }

        for (int i = 0; i < ConventionalImageNum; i++) {
            for (int j = i + 1; j < ConventionalImageNum; j++) {
                if (WorkMode.get(ConventionalImagMode[i]).toString().equals(WorkMode.get(ConventionalImagMode[j]).toString())) {
                    double[][] MissionTargetArea_i = new double[100][2];
                    ;
                    int TargetNum_i = 0;
                    Document target_region = (Document) ImageRegion.get(ConventionalImagMode[i]);
                    ArrayList<Document> features = (ArrayList<Document>) target_region.get("features");
                    for (Document document1 : features) {
                        Document geometry = (Document) document1.get("geometry");
                        ArrayList<ArrayList<Double>> coordinates = (ArrayList<ArrayList<Double>>) geometry.get("coordinates");
                        TargetNum_i = coordinates.size();
                        int Num = 0;
                        for (ArrayList<Double> document2 : coordinates) {
                            MissionTargetArea_i[Num][0] = document2.get(0);
                            MissionTargetArea_i[Num][1] = document2.get(1);
                            Num = Num + 1;
                        }
                    }
                    double[][] MissionTargetArea_j = new double[100][2];
                    ;
                    int TargetNum_j = 0;
                    Document target_regionj = (Document) ImageRegion.get(ConventionalImagMode[j]);
                    ArrayList<Document> featuresj = (ArrayList<Document>) target_regionj.get("features");
                    for (Document document1 : featuresj) {
                        Document geometry = (Document) document1.get("geometry");
                        ArrayList<ArrayList<Double>> coordinates = (ArrayList<ArrayList<Double>>) geometry.get("coordinates");
                        TargetNum_j = coordinates.size();
                        int Num = 0;

                        //String str=coordinates.toString();
                        for (ArrayList<Double> document2 : coordinates) {
                            MissionTargetArea_j[Num][0] = document2.get(0);
                            MissionTargetArea_j[Num][1] = document2.get(1);
                            Num = Num + 1;
                        }
                    }
                    boolean CombineFlag = true;
                    for (int k = 0; k < TargetNum_i; k++) {
                        for (int l = 0; l < TargetNum_j; l++) {
                            double WA = MissionTargetArea_i[k][1] * PI / 180.0;
                            double JA = MissionTargetArea_i[k][0] * PI / 180.0;
                            double WB = MissionTargetArea_j[l][1] * PI / 180.0;
                            double JB = MissionTargetArea_j[l][0] * PI / 180.0;
                            double a = WA - WB;
                            double b = JA - JB;
                            double s = 2 * asin(sqrt(pow(sin(a / 2), 2) + cos(WA) * cos(WB) * pow(sin(b / 2), 2)));
                            s = s * Re;
                            if (s > ViewLength) {
                                CombineFlag = false;
                                break;
                            }
                        }
                        if (CombineFlag == false) {
                            break;
                        }
                    }
                    if (CombineFlag == true) {
                        int Num = 0;
                        for (Document document1 : features) {
                            Document geometry = (Document) document1.get("geometry");
                            ArrayList<ArrayList<Double>> coordinates = (ArrayList<ArrayList<Double>>) geometry.get("coordinates");
                            for (Document document2 : featuresj) {
                                Document geometryj = (Document) document2.get("geometry");
                                ArrayList<ArrayList<Double>> coordinatesj = (ArrayList<ArrayList<Double>>) geometryj.get("coordinates");
                                coordinates.addAll(coordinatesj);
                            }
                            geometry.append("coordinates", coordinates);
                            features.get(Num).append("geometry", geometry);
                            Num = Num + 1;
                        }
                        target_region.append("features", features);
                        ImageRegion.set(ConventionalImagMode[i], target_region);

                        //
                        OverallResult[ConventionalImagMode[i]] = OverallResult[ConventionalImagMode[i]] + 1;
                        OverallResult[ConventionalImagMode[j]] = 0;
                        OverallMissionNum[ConventionalImagMode[i]][OverallResult[ConventionalImagMode[i]] - 1] = ConventionalImagMode[j];

                        ConventionalImageNum = ConventionalImageNum - 1;
                        for (int k = j; k < ConventionalImageNum; k++) {
                            ConventionalImagMode[k] = ConventionalImagMode[k + 1];
                        }
                    }
                }
            }
        }

        //数据传出
        MongoClient mongoClient = MangoDBConnector.getClient();
        //获取名为"temp"的数据库
        MongoDatabase mongoDatabase = mongoClient.getDatabase("temp");

        MongoCollection<Document> image_order = mongoDatabase.getCollection("image_order");
        for (int i = 0; i < OrderMissionNum; i++) {
            if (OverallResult[i] != 0) {
                Document ImageMissionjson = new Document();
                String mission_number = "im_" + Instant.now().toEpochMilli();
                ImageMissionjson.append("image_region", ImageRegion.get(i));
                ImageMissionjson.append("region", Region.get(i));
                ImageMissionjson.append("name", Name.get(i));
                ImageMissionjson.append("image_type", ImageType.get(i));
                ImageMissionjson.append("expected_cam", ExpectedCam.get(i));
                ImageMissionjson.append("expected_start_time", ExpectedStartTime.get(i));
                ImageMissionjson.append("expected_end_time", ExpectedEndTime.get(i));
                ImageMissionjson.append("priority", Priority.get(i));
                ImageMissionjson.append("image_mode", ImageMode.get(i));
                ImageMissionjson.append("work_mode", WorkMode.get(i));
                ImageMissionjson.append("min_height_orbit", MinHeightOrbit.get(i));
                ImageMissionjson.append("max_height_orbit", MaxHeightOrbit.get(i));
                ImageMissionjson.append("min_stare_time", MinStareTime.get(i));
                ImageMissionjson.append("mission_number", mission_number);
                ImageMissionjson.append("mission_state", "待规划");
                ImageMissionjson.append("mission_interval_min", "40");
                ArrayList<Object> OrderNumber_List = new ArrayList<>();
                for (int j = 0; j < OverallResult[i]; j++) {
                    OrderNumber_List.add(OrderNumber.get(OverallMissionNum[i][j]));
                    ImageOrderjson.get(OverallMissionNum[i][j]).append("mission_number", mission_number);
                    ImageOrderjson.get(OverallMissionNum[i][j]).append("order_state", "待规划");
                    Document modifiers = new Document();
                    modifiers.append("$set", ImageOrderjson.get(OverallMissionNum[i][j]));
//
                    image_order.updateOne(new Document("order_number", ImageOrderjson.get(OverallMissionNum[i][j]).getString("order_number")), modifiers, new UpdateOptions().upsert(true));
                }
                ImageMissionjson.append("order_numbers", OrderNumber_List);
                MongoCollection<Document> image_mission = mongoDatabase.getCollection("image_mission");
                image_mission.insertOne(ImageMissionjson);
                missions.add(mission_number);
            }
        }
        mongoClient.close();

        return missions;
    }

    //读取任务区域目标点
    private static ArrayList<double[]> GetRegionPoint(Document target_region) {
        ArrayList<double[]> CoordinatesList = new ArrayList<double[]>();
        Document geomety = new Document();
        if (target_region.get("type").equals("Feature")) {
            geomety = (Document) target_region.get("geometry");
            CoordinatesList = GetGeometryPoint(geomety);
        } else if (target_region.get("type").equals("FeatureCollection")) {
            ArrayList<Document> features = (ArrayList<Document>) target_region.get("features");
            for (Document subfeatures : features) {
                geomety = (Document) subfeatures.get("geometry");
                ArrayList<double[]> subCoordinatesList = new ArrayList<double[]>();
                subCoordinatesList = GetGeometryPoint(geomety);
                for (double[] subsubCoordinatesList : subCoordinatesList) {
                    CoordinatesList.add(subsubCoordinatesList);
                }
            }
        } else if (target_region.get("type").equals("GeometryCollection")) {
            ArrayList<Document> geometries = (ArrayList<Document>) target_region.get("geometries");
            for (Document subgeometries : geometries) {
                ArrayList<double[]> subCoordinatesList = new ArrayList<double[]>();
                subCoordinatesList = GetGeometryPoint(subgeometries);
                for (double[] subsubCoordinatesList : subCoordinatesList) {
                    CoordinatesList.add(subsubCoordinatesList);
                }
            }
        } else {

        }

        return CoordinatesList;
    }

    private static ArrayList<double[]> GetGeometryPoint(Document geometry) {
        ArrayList<double[]> CoordinatesList = new ArrayList<double[]>();
        if (geometry.get("type").equals("Point")) {
            ArrayList<Double> coordinates = (ArrayList<Double>) geometry.get("coordinates");
            double[] Target = new double[2];
            Target[0] = coordinates.get(0);
            Target[1] = coordinates.get(1);
            CoordinatesList.add(Target);
        } else if (geometry.get("type").equals("LineString")) {
            ArrayList<ArrayList<Double>> coordinates = (ArrayList<ArrayList<Double>>) geometry.get("coordinates");
            for (ArrayList<Double> document : coordinates) {
                double[] Target = new double[2];
                Target[0] = document.get(0);
                Target[1] = document.get(1);
                CoordinatesList.add(Target);
            }
        } else if (geometry.get("type").equals("Polygon")) {
            ArrayList<ArrayList<ArrayList<Double>>> coordinates = (ArrayList<ArrayList<ArrayList<Double>>>) geometry.get("coordinates");
            for (ArrayList<ArrayList<Double>> subcoordinates : coordinates) {
                for (ArrayList<Double> subsubcoordinates : subcoordinates) {
                    double[] Target = new double[2];
                    Target[0] = subsubcoordinates.get(0);
                    Target[1] = subsubcoordinates.get(1);
                    CoordinatesList.add(Target);
                }
            }
        } else if (geometry.get("type").equals("MultiPoint")) {
            ArrayList<ArrayList<Double>> coordinates = (ArrayList<ArrayList<Double>>) geometry.get("coordinates");
            for (ArrayList<Double> document : coordinates) {
                double[] Target = new double[2];
                Target[0] = document.get(0);
                Target[1] = document.get(1);
                CoordinatesList.add(Target);
            }
        } else if (geometry.get("type").equals("MultiLineString")) {
            ArrayList<ArrayList<ArrayList<Double>>> coordinates = (ArrayList<ArrayList<ArrayList<Double>>>) geometry.get("coordinates");
            for (ArrayList<ArrayList<Double>> subcoordinates : coordinates) {
                for (ArrayList<Double> subsubcoordinates : subcoordinates) {
                    double[] Target = new double[2];
                    Target[0] = subsubcoordinates.get(0);
                    Target[1] = subsubcoordinates.get(1);
                    CoordinatesList.add(Target);
                }
            }
        } else if (geometry.get("type").equals("MultiPolygon")) {
            ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> coordinates = (ArrayList<ArrayList<ArrayList<ArrayList<Double>>>>) geometry.get("coordinates");
            for (ArrayList<ArrayList<ArrayList<Double>>> subcoordinates : coordinates) {
                for (ArrayList<ArrayList<Double>> subsubcoordinates : subcoordinates) {
                    for (ArrayList<Double> subsubsubcoordinates : subsubcoordinates) {
                        double[] Target = new double[2];
                        Target[0] = subsubsubcoordinates.get(0);
                        Target[1] = subsubsubcoordinates.get(1);
                        CoordinatesList.add(Target);
                    }
                }
            }
        }

        return CoordinatesList;
    }

    //合并两个区域为GeometryCollection
    private static void CombinedCollection(Document TargetRegion_i, Document TargetRegion_j) {
        boolean Geometry_i = TargetRegion_i.get("type").equals("GeometryCollection");
        boolean Geometry_j = TargetRegion_j.get("type").equals("GeometryCollection");
        if (Geometry_i == true && Geometry_j == false) {
            ArrayList<Document> geometries_i = (ArrayList<Document>) TargetRegion_i.get("geometries");
            ArrayList<Document> geometries_j = new ArrayList<Document>();
            if (TargetRegion_j.get("type").equals("Feature")) {
                geometries_j = (ArrayList<Document>) TargetRegion_j.get("geometry");
            } else {
                ArrayList<Document> features = (ArrayList<Document>) TargetRegion_j.get("features");
                for (Document subfeatures : features) {
                    geometries_j.add((Document) subfeatures.get("geometry"));
                }
            }
            geometries_i.addAll(geometries_j);
            TargetRegion_i.append("geometries", geometries_i);
        } else if (Geometry_i == true && Geometry_j == true) {
            ArrayList<Document> geometries_i = (ArrayList<Document>) TargetRegion_i.get("geometries");
            ArrayList<Document> geometries_j = (ArrayList<Document>) TargetRegion_j.get("geometries");
            geometries_i.addAll(geometries_j);
            TargetRegion_i.append("geometries", geometries_i);
        } else if (Geometry_i == false && Geometry_j == false) {
            boolean GeometryFeature_i = TargetRegion_i.get("type").equals("Feature");
            boolean GeometryFeature_j = TargetRegion_j.get("type").equals("Feature");
            if (GeometryFeature_i == true && Geometry_j == false) {
                ArrayList<Document> geometries_i = (ArrayList<Document>) TargetRegion_i.get("geometry");
                ArrayList<Document> geometries_j = new ArrayList<Document>();
                ArrayList<Document> features = (ArrayList<Document>) TargetRegion_j.get("features");
                for (Document subfeatures : features) {
                    geometries_j.add((Document) subfeatures.get("geometry"));
                }
                geometries_i.addAll(geometries_j);
                TargetRegion_i.append("type", "GeometryCollection");
                TargetRegion_i.append("geometries", geometries_i);
            } else if (GeometryFeature_i == true && GeometryFeature_j == true) {
                ArrayList<Document> geometries_i= (ArrayList<Document>) TargetRegion_i.get("geometry");
                ArrayList<Document> geometries_j= (ArrayList<Document>) TargetRegion_j.get("geometry");
                geometries_i.addAll(geometries_j);
                TargetRegion_i.append("type", "GeometryCollection");
                TargetRegion_i.append("geometries", geometries_i);
            } else if (GeometryFeature_i == false && GeometryFeature_j == false) {
                ArrayList<Document> geometries_i=new ArrayList<Document>();
                ArrayList<Document> geometries_j=new ArrayList<Document>();
                ArrayList<Document> features= (ArrayList<Document>) TargetRegion_i.get("features");
                for (Document subfeatures:features) {
                    geometries_i.add((Document) subfeatures.get("geometry"));
                }
                features= (ArrayList<Document>) TargetRegion_j.get("features");
                for (Document subfeatures:features) {
                    geometries_j.add((Document) subfeatures.get("geometry"));
                }
                geometries_i.addAll(geometries_j);
                TargetRegion_i.append("type", "GeometryCollection");
                TargetRegion_i.append("geometries", geometries_i);
            } else if (GeometryFeature_i == false && GeometryFeature_j == true) {
                ArrayList<Document> geometries_i=new ArrayList<Document>();
                ArrayList<Document> geometries_j= (ArrayList<Document>) TargetRegion_j.get("geometry");
                ArrayList<Document> features= (ArrayList<Document>) TargetRegion_i.get("features");
                for (Document subfeatures:features) {
                    geometries_i.add((Document) subfeatures.get("geometry"));
                }
                geometries_i.addAll(geometries_j);
                TargetRegion_i.append("type", "GeometryCollection");
                TargetRegion_i.append("geometries", geometries_i);
            }
        } else if (Geometry_i == false && Geometry_j == true) {
            if (TargetRegion_i.get("type").equals("Feature")) {
                ArrayList<Document> geometry_i = (ArrayList<Document>) TargetRegion_i.get("geometry");
                ArrayList<Document> geometry_j = (ArrayList<Document>) TargetRegion_j.get("geometries");
                geometry_i.addAll(geometry_j);
                TargetRegion_i.append("type", "GeometryCollection");
                TargetRegion_i.append("geometries", geometry_i);
            } else {
                ArrayList<Document> geometry_i = new ArrayList<Document>();
                ArrayList<Document> geometry_j = (ArrayList<Document>) TargetRegion_j.get("geometries");
                ArrayList<Document> features = (ArrayList<Document>) TargetRegion_i.get("features");
                for (Document subfeatures : features) {
                    geometry_i.add((Document) subfeatures.get("geometry"));
                }
                geometry_i.addAll(geometry_j);
                TargetRegion_i.append("type", "GeometryCollection");
                TargetRegion_i.append("geometries", geometry_i);
            }
        }



    }

}
