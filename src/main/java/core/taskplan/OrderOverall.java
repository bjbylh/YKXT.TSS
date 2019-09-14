package core.taskplan;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import common.mongo.DbDefine;
import common.mongo.MangoDBConnector;
import org.bson.Document;

import java.time.Instant;
import java.util.ArrayList;

import static java.lang.Math.*;

public class OrderOverall {
    private static double Re = 6371393;                  //地球半径，单位为：米
    private static double ViewLength = 50000;              //视场宽度


    public static ArrayList<String> OrderOverallII(ArrayList<Document> ImageOrderjson) {
        ArrayList<String> missions = new ArrayList<>();
        //订单任务读取
        ArrayList<Document> ImageOrderjsonCopy = new ArrayList<>();
        for(Document d : ImageOrderjson){
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
        MongoDatabase mongoDatabase = mongoClient.getDatabase(DbDefine.DB_NAME);

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
                    ImageOrderjson.get(OverallMissionNum[i][j]).append("mission_number",mission_number);
                    ImageOrderjson.get(OverallMissionNum[i][j]).append("order_state","待规划");
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
}
