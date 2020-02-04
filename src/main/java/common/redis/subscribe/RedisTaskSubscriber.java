package common.redis.subscribe;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import common.ConfigManager;
import common.FilePathUtil;
import common.def.TaskType;
import common.def.TempletType;
import common.mongo.DbDefine;
import common.mongo.MangoDBConnector;
import common.redis.MsgType;
import common.redis.RedisPublish;
import common.xmlutil.XmlParser;
import core.taskplan.*;
import org.bson.Document;
import org.bson.conversions.Bson;
import redis.clients.jedis.JedisPubSub;
import srv.task.TaskInit;
import xml.FileBodyType;
import xml.FileHeaderType;
import xml.InterFaceFileType;
import xml.ObjectFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.ParseException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Created by lihan on 2018/11/15.
 */
public class RedisTaskSubscriber extends JedisPubSub {
    private Date startTime = Date.from(Instant.now().plusSeconds(24 * 60 * 60 * 10000));
    private Date endTime = Date.from(Instant.now().minusSeconds(24 * 60 * 60 * 10000));
    private Instant BASE_TIME = ZonedDateTime.of(1949, 12, 31, 0, 0, 0, 0, ZoneOffset.ofHours(8)).toInstant();
    private DateTimeFormatter sf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private OffsetDateTime odt = OffsetDateTime.now(ZoneId.ofOffset("UTC", ZoneOffset.UTC));
    private ZoneOffset zoneOffset = odt.getOffset();
    private static int message_ser = 0;
    private static final int MESSAGE_MAX = 999999;

    public RedisTaskSubscriber() {
    }

    @Override
    public void onMessage(String channel, String message) {
        System.out.println(String.format("receive redis published message, channel %s, message %s", channel, message));

        try {
            JsonParser parse = new JsonParser();  //创建json解析器
            JsonObject msg = (JsonObject) parse.parse(message);

            String asString = msg.getAsJsonObject("Head").get("type").getAsString();
            String id = msg.getAsJsonObject("Head").get("id").getAsString();

            JsonObject json = msg.getAsJsonObject("Data");

            if (asString.equals(MsgType.NEW_TASK.name())) {

                procNewTask(json, id);

            } else if (asString.equals(MsgType.CHECK_QUERY.name())) {

                procCheckQuery(json, id);

            } else if (asString.equals(MsgType.ORBIT_DATA_IMPORT.name())) {

                procOrbitDataImport(json, id);

            } else if (asString.equals(MsgType.INS_CLEAR.name())) {

                procInsClear(json, id);

            } else if (asString.equals(MsgType.FILE_CLEAR.name())) {

                procFileClear(json, id);

            } else if (asString.equals(MsgType.INS_GEN.name())) {

                //procBlackCali(json, id);
                procInsGen(json, id);

            } else if (asString.equals(MsgType.TRANSMISSION_EXPORT.name())) {

                procTransmissionExport(json, id);

            } else if (asString.equals(MsgType.BLACK_CALI.name())) {

                procBlackCali(json, id);

            } else if (asString.equals(MsgType.MANUAL_LOOP.name())) {

                proManualLoop(json, id);

            } else return;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSubscribe(String channel, int subscribedChannels) {
        System.out.println(String.format("subscribe redis channel success, channel %s, subscribedChannels %d",
                channel, subscribedChannels));
    }

    @Override
    public void onUnsubscribe(String channel, int subscribedChannels) {
        System.out.println(String.format("unsubscribe redis channel, channel %s, subscribedChannels %d",
                channel, subscribedChannels));

    }

    private void procInsClear(JsonObject json, String id) {
        try {
            String[] content = json.get("content").getAsString().split(",");
            int isTimeSpan = json.get("type").getAsInt();
            String exeStartTime = json.get("exe_time").getAsString();

            LocalDateTime exeStartTime_r = LocalDateTime.parse(exeStartTime, sf);

            Instant exeStartTime_i = exeStartTime_r.toInstant(zoneOffset);
            int type = -1;
            Instant start_i = Instant.now();
            Instant end_i = Instant.now();
            HashSet<Integer> insnos = new HashSet<>();
            if (isTimeSpan == 1) {
                int rawtype = Integer.parseInt(content[0]);

                String start = content[1];
                LocalDateTime start_r = LocalDateTime.parse(start, sf);
                start_i = start_r.toInstant(zoneOffset);

                String end = content[2];
                LocalDateTime end_r = LocalDateTime.parse(end, sf);
                end_i = end_r.toInstant(zoneOffset);


                if (rawtype == 34)
                    type = 0;
                else if (rawtype == 17)
                    type = 1;
                else if (rawtype == 51)
                    type = 2;
                else if (rawtype == 68)
                    type = 3;
                else
                    throw new Exception("错误的数据类型");
            } else {
                for (String insno : content) {
                    insnos.add(Integer.parseInt(insno));
                }
            }

            String rst = InsClearInsGenInf.InsClearInsGenInfII(isTimeSpan, type, exeStartTime_i, start_i, end_i, insnos, ConfigManager.getInstance().fetchInsFilePath());

            RedisPublish.CommonReturn(id, true, rst, MsgType.INS_CLEAR_FINISHED);

        } catch (Exception e) {
            String message = e.getMessage();
            RedisPublish.CommonReturn(id, false, message, MsgType.INS_CLEAR_FINISHED);
        }
    }

    private void proManualLoop(JsonObject json, String id) {
        try {
            String mission_num = json.get("content").getAsString();

            Document image_mission = null;
            MongoClient mongoClient = MangoDBConnector.getClient();
            //获取名为"temp"的数据库
            MongoDatabase mongoDatabase = mongoClient.getDatabase(DbDefine.DB_NAME);

            FindIterable<Document> image_mission1 = mongoDatabase.getCollection("image_mission").find();

            for (Document d : image_mission1) {
                if (d.getString("mission_number").equals(mission_num)) {
                    image_mission = d;
                    break;
                }

            }

            String s = SingleInsGeneration.SingleInsGeneration(image_mission, ConfigManager.getInstance().fetchInsFilePath());

            mongoClient.close();
            RedisPublish.CommonReturn(id, true, s, MsgType.MANUAL_LOOP_FINISHED);


        } catch (Exception e) {
            String message = e.getMessage();
            RedisPublish.CommonReturn(id, false, message, MsgType.MANUAL_LOOP_FINISHED);
        }
    }

    private void procBlackCali(JsonObject json, String id) {
        try {
            String order_number = json.get("image_order_num").getAsString();

            Document image_order = null;
            MongoClient mongoClient = MangoDBConnector.getClient();
            //获取名为"temp"的数据库
            MongoDatabase mongoDatabase = mongoClient.getDatabase(DbDefine.DB_NAME);

            FindIterable<Document> image_order1 = mongoDatabase.getCollection("image_order").find();

            for (Document d : image_order1) {
                if (d.getString("order_number").equals(order_number)) {
                    image_order = d;
                    break;
                }

            }

            String s = CalibrateInsGenInf.CalibrateInsGenInfII(image_order, ConfigManager.getInstance().fetchInsFilePath());

            mongoClient.close();
            RedisPublish.CommonReturn(id, true, s, MsgType.BLACK_CALI_FINISHED);


        } catch (Exception e) {
            String message = e.getMessage();
            RedisPublish.CommonReturn(id, false, message, MsgType.BLACK_CALI_FINISHED);
        }
    }

    private void procFileClear(JsonObject json, String id) {
        try {
            String mission_number = json.get("content").getAsString();

            MongoClient mongoClient = MangoDBConnector.getClient();
            //获取名为"temp"的数据库
            MongoDatabase mongoDatabase = mongoClient.getDatabase(DbDefine.DB_NAME);

            FindIterable<Document> image_missions = mongoDatabase.getCollection("image_mission").find();

            for (Document d : image_missions) {
                if (d.getString("mission_number").contains(mission_number)) {

                    String rst = FileClearInsGenInf.FileClearInsGenInfII(d, ConfigManager.getInstance().fetchInsFilePath());
                    //TODO
                    RedisPublish.CommonReturn(id, true, rst, MsgType.FILE_CLEAR_FINISHED);

                    break;
                }
            }

            mongoClient.close();
        } catch (Exception e) {
            String message = e.getMessage();
            RedisPublish.CommonReturn(id, false, message, MsgType.FILE_CLEAR_FINISHED);
        }
    }

    private void procInsGen(JsonObject json, String id) {
        try {
            String order_number = json.get("image_order_num").getAsString();
            String station_mission_num = json.get("station_mission_num").getAsString();

            Document image_order = null;
            Document station_mission = null;
            MongoClient mongoClient = MangoDBConnector.getClient();
            //获取名为"temp"的数据库
            MongoDatabase mongoDatabase = mongoClient.getDatabase(DbDefine.DB_NAME);

            FindIterable<Document> image_order1 = mongoDatabase.getCollection("image_order").find();

            for (Document d : image_order1) {
                if (d.getString("order_number").equals(order_number)) {
                    image_order = d;
                    break;
                }

            }

            if (image_order != null) {
                try {
                    if (image_order.getString("mission_number") != null) {
                        MongoCollection<Document> image_mission = mongoDatabase.getCollection("image_mission");
                        Document filter = new Document();
                        filter.append("mission_number", image_order.getString("mission_number"));
                        Document first = image_mission.find(filter).first();

                        if (first != null && first.containsKey("instruction_info")) {

                            ArrayList<Document> instruction_infos = (ArrayList<Document>) first.get("instruction_info");
                            if (instruction_infos.size() > 0) {
                                for (Document doc : instruction_infos) {
                                    doc.append("valid", false);
                                }
                            }
                        }

                        Document modifiers = new Document();
                        modifiers.append("$set", first);
//
                        image_mission.updateOne(new Document("_id", first.getObjectId("_id")), modifiers, new UpdateOptions().upsert(true));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            FindIterable<Document> station_mission1 = mongoDatabase.getCollection("station_mission").find();

            for (Document d : station_mission1) {
                if (d.getString("mission_number").equals(station_mission_num)) {
                    station_mission = d;
                    break;
                }

            }

            if (station_mission != null) {
                try {
                    if (station_mission.getString("transmission_number") != null) {
                        MongoCollection<Document> transmission_mission = mongoDatabase.getCollection("transmission_mission");
                        Document filter = new Document();
                        filter.append("transmission_number", station_mission.getString("transmission_number"));
                        Document first = transmission_mission.find(filter).first();

                        if (first != null && first.containsKey("instruction_info")) {

                            ArrayList<Document> instruction_infos = (ArrayList<Document>) first.get("instruction_info");
                            if (instruction_infos.size() > 0) {
                                for (Document doc : instruction_infos) {
                                    doc.append("valid", false);
                                }
                            }
                        }

                        Document modifiers = new Document();
                        modifiers.append("$set", first);
//
                        transmission_mission.updateOne(new Document("_id", first.getObjectId("_id")), modifiers, new UpdateOptions().upsert(true));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            String s = InsGenWithoutTaskPlanInf.InsGenWithoutTaskPlanInf(image_order, station_mission, ConfigManager.getInstance().fetchInsFilePath());

            mongoClient.close();
            RedisPublish.CommonReturn(id, true, s, MsgType.INS_GEN_FINISHED);


        } catch (Exception e) {
            String message = e.getMessage();
            RedisPublish.CommonReturn(id, false, message, MsgType.INS_GEN_FINISHED);
        }
    }


    private void procTransmissionExport(JsonObject json, String id) {
        try {
            Instant createTime = Instant.now();
            LocalDateTime localDateTime = LocalDateTime.now();
            String dir = String.valueOf(createTime.toEpochMilli());
            String f = ConfigManager.getInstance().fetchXmlFilePath() + dir;

            String ids = json.get("content").getAsString();
            String[] transmission_numbers_array = ids.split(",");

            ArrayList<String> transmission_numbers = new ArrayList<>();
            for (String s : transmission_numbers_array) {
                transmission_numbers.add(s);
            }

            MongoClient mongoClient = MangoDBConnector.getClient();
            //获取名为"temp"的数据库
            MongoDatabase mongoDatabase = mongoClient.getDatabase(DbDefine.DB_NAME);

            MongoCollection<Document> sate_res = mongoDatabase.getCollection("satellite_resource");

            String sat_code = sate_res.find().first().getString("sat_code");


            //卫星资源表
            FindIterable<Document> transmission_missions = mongoDatabase.getCollection("transmission_mission").find();

//            ObjectFactory objectFactory = new ObjectFactory();

//            DtplanType dtplanType = objectFactory.createDtplanType();
//
//            HeadType headType = objectFactory.createHeadType();
//            headType.setCreationTime(Instant.now().toString());
//
//            dtplanType.setHead(headType);
//
//            PlanType planType = objectFactory.createPlanType();
//            for (Document transmission_mission : transmission_missions) {
//                if (transmission_numbers.contains(transmission_mission.getString("transmission_number"))) {
//
//                    if (transmission_mission.containsKey("transmission_window")) {
//                        ArrayList<Document> transmission_window = (ArrayList<Document>) transmission_mission.get("transmission_window");
//
//                        for (Document window : transmission_window) {
//                            MissionType missionType = objectFactory.createMissionType();
//                            missionType.setTplanID(transmission_mission.getString("transmission_number"));
//                            missionType.setStationID(window.getString("station_name"));
//                            missionType.setStartTime(Instant.ofEpochMilli(window.getDate("start_time").getTime()).toString());
//                            missionType.setEndTime(Instant.ofEpochMilli(window.getDate("end_time").getTime()).toString());
//                            missionType.setSatelliteID(sat_code);
//                            planType.setMission(missionType);
//                        }
//                    }
//                }
//            }
//            dtplanType.setPlan(planType);

            for (Document transmission_mission : transmission_missions) {
                if (transmission_numbers.contains(transmission_mission.getString("transmission_number"))) {

                    if (transmission_mission.containsKey("transmission_window")) {
                        ArrayList<Document> transmission_window = (ArrayList<Document>) transmission_mission.get("transmission_window");

                        for (Document window : transmission_window) {
//
                            ObjectFactory objectFactory = new ObjectFactory();
                            InterFaceFileType interFaceFileType = objectFactory.createInterFaceFileType();
                            FileHeaderType fileHeaderType = objectFactory.createFileHeaderType();
                            fileHeaderType.setMessageType("TRTASK");
                            fileHeaderType.setMessageID(String.format("%06d", message_ser));
                            fileHeaderType.setOriginatorAddress("MPSS");
                            fileHeaderType.setRecipientAddress("TRGS-JD-11");
                            fileHeaderType.setCreationTime(localDateTime.toString());

                            interFaceFileType.setFileHeader(fileHeaderType);

                            FileBodyType fileBodyType = objectFactory.createFileBodyType();
                            fileBodyType.setTrPlanID(String.format("%09d", message_ser));
                            fileBodyType.setSatellite(sat_code);
                            fileBodyType.setTmType("2");
                            fileBodyType.setSensorType("GF/DGP");
                            fileBodyType.setDownlinkChannel("7");
                            fileBodyType.setReceptionType("LIVERECEPTION");
                            fileBodyType.setOrbitID("0");
                            fileBodyType.setIsQuickView("FALSE");
                            fileBodyType.setIsCloud("FALSE");

                            String start_time = window.getDate("start_time").toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().toString();
                            String end_time = window.getDate("end_time").toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().toString();

                            if (start_time.length() == 16)
                                start_time += ":00";

                            if (end_time.length() == 16)
                                end_time += ":00";

                            fileBodyType.setReceiveStartTime(start_time);
                            fileBodyType.setSatelliteCaptureStartTime(start_time);
                            fileBodyType.setReceiveStopTime(end_time);
                            fileBodyType.setSatelliteCaptureStopTime(end_time);
                            fileBodyType.setTaskCount("1");

                            interFaceFileType.setFileBody(fileBodyType);

                            String date = String.valueOf(localDateTime.getYear()) + String.format("%02d", localDateTime.getMonth().getValue()) + String.format("%02d", localDateTime.getDayOfMonth());
                            String filename = "MPSS_TRGS-JD-11_" + sat_code + "_" + date + "_" + String.format("%06d", message_ser) + ".TRTASK";

                            File file = new File(FilePathUtil.getRealFilePath(f + "//" + filename));
                            Writer w = new FileWriter(file);
                            w.write(interFaceFileType.toString());
                            w.close();

                            if (message_ser == MESSAGE_MAX)
                                message_ser = 0;
                            else message_ser++;
                        }
                    }
                }
            }


            mongoClient.close();
            RedisPublish.CommonReturn(id, true, f, MsgType.TRANSMISSION_EXPORT_FINISHED);


        } catch (Exception e) {
            String message = e.getMessage();
            RedisPublish.CommonReturn(id, false, message, MsgType.ORBIT_DATA_IMPORT_FINISHED);
        }
    }

    public static void main(String[] args) throws IOException {
        LocalDateTime localDateTime = LocalDateTime.now();
        MongoClient mongoClient = MangoDBConnector.getClient();
        //获取名为"temp"的数据库
        MongoDatabase mongoDatabase = mongoClient.getDatabase(DbDefine.DB_NAME);

        MongoCollection<Document> sate_res = mongoDatabase.getCollection("satellite_resource");

        String sat_code = sate_res.find().first().getString("sat_code");


        //卫星资源表
        FindIterable<Document> transmission_missions = mongoDatabase.getCollection("transmission_mission").find();


        for (Document transmission_mission : transmission_missions) {

            if (transmission_mission.containsKey("transmission_window")) {
                ArrayList<Document> transmission_window = (ArrayList<Document>) transmission_mission.get("transmission_window");

                for (Document window : transmission_window) {
//
                    ObjectFactory objectFactory = new ObjectFactory();
                    InterFaceFileType interFaceFileType = objectFactory.createInterFaceFileType();
                    FileHeaderType fileHeaderType = objectFactory.createFileHeaderType();
                    fileHeaderType.setMessageType("TRTASK");
                    fileHeaderType.setMessageID(String.format("%06d", message_ser));
                    fileHeaderType.setOriginatorAddress("MPSS");
                    fileHeaderType.setRecipientAddress("TRGS-JD-11");
                    fileHeaderType.setCreationTime(localDateTime.toString());

                    interFaceFileType.setFileHeader(fileHeaderType);

                    FileBodyType fileBodyType = objectFactory.createFileBodyType();
                    fileBodyType.setTrPlanID(String.format("%09d", message_ser));
                    fileBodyType.setSatellite(sat_code);
                    fileBodyType.setTmType("2");
                    fileBodyType.setSensorType("GF/DGP");
                    fileBodyType.setDownlinkChannel("7");
                    fileBodyType.setReceptionType("LIVERECEPTION");
                    fileBodyType.setOrbitID("0");
                    fileBodyType.setIsQuickView("FALSE");
                    fileBodyType.setIsCloud("FALSE");

                    String start_time = window.getDate("start_time").toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().toString();
                    String end_time = window.getDate("end_time").toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().toString();

                    if (start_time.length() == 16)
                        start_time += ":00";

                    if (end_time.length() == 16)
                        end_time += ":00";

                    fileBodyType.setReceiveStartTime(start_time);
                    fileBodyType.setSatelliteCaptureStartTime(start_time);
                    fileBodyType.setReceiveStopTime(end_time);
                    fileBodyType.setSatelliteCaptureStopTime(end_time);
                    fileBodyType.setTaskCount("1");

                    interFaceFileType.setFileBody(fileBodyType);

                    System.out.println(interFaceFileType.toString());

                    if (message_ser == MESSAGE_MAX)
                        message_ser = 0;
                    else message_ser++;
                }
            }
        }


        mongoClient.close();
    }

    private void procOrbitDataImport(JsonObject json, String id) {
        try {
            String xmlString = json.get("content").getAsString();
            xmlString = xmlString.replace("\r", "").replace("\n", "");
            HashMap<String, String> parser = XmlParser.parser(xmlString);

            if (parser.containsKey("ERROR")) {
                String message = parser.get("ERROR");
                RedisPublish.CommonReturn(id, false, message, MsgType.ORBIT_DATA_IMPORT_FINISHED);
                return;
            }

            MongoClient mongoClient = MangoDBConnector.getClient();
            //获取名为"temp"的数据库
            MongoDatabase mongoDatabase = mongoClient.getDatabase(DbDefine.DB_NAME);

            //卫星资源表
            MongoCollection<Document> Data_Satllitejson = mongoDatabase.getCollection("satellite_resource");
            Document Satllitejson = Data_Satllitejson.find().first();
            Document newSateInfo = Document.parse(Satllitejson.toJson());

            ArrayList<Document> properties = (ArrayList<Document>) newSateInfo.get("properties");
            for (Document d : properties) {
                if (d.getString("group").equals("轨道参数")) {
                    if (d.getString("key").equals("update_time")) {
                        int jd = Integer.parseInt(parser.get("JD"));
                        long js = (long) (Double.parseDouble(parser.get("JS")) * 1000);
                        Instant fileTime = BASE_TIME.plusSeconds(jd * 86400L).plusMillis(js);
                        d.append("value", Date.from(fileTime));
                    } else if (d.getString("key").equals("a")) {
                        d.append("value", String.valueOf(Double.parseDouble(parser.get("A")) / 1000.0));
                    } else if (d.getString("key").equals("e")) {
                        d.append("value", parser.get("E"));
                    } else if (d.getString("key").equals("i")) {
                        d.append("value", parser.get("I"));
                    } else if (d.getString("key").equals("RAAN")) {
                        d.append("value", parser.get("O"));
                    } else if (d.getString("key").equals("perigee_angle")) {
                        d.append("value", parser.get("W"));
                    } else if (d.getString("key").equals("true_anomaly")) {//todo
                        d.append("value", parser.get("M"));
                    } else continue;
                }
            }
            Data_Satllitejson.updateOne(Filters.eq("_id", Satllitejson.getObjectId("_id")), new Document("$set", newSateInfo));

            mongoClient.close();

            RedisPublish.CommonReturn(id, true, "", MsgType.ORBIT_DATA_IMPORT_FINISHED);

            TaskInit.initRTTaskForOrbitForecast("新建任务调度任务指令");


        } catch (Exception e) {
            String message = e.getMessage();
            RedisPublish.CommonReturn(id, false, message, MsgType.ORBIT_DATA_IMPORT_FINISHED);
        }
    }

    private void procNewTask(JsonObject json, String id) {
        try {

            if (json.get("tasktype").getAsString().equals(TaskType.CRONTAB.name()) && json.get("templet").getAsString().equals(TempletType.TASK_PLAN.name()))
                TaskInit.initCronTaskForTaskPlan(json.get("name").getAsString(), json.get("firsttime").getAsString(), json.get("cycle").getAsString(), json.get("count").getAsString());

            else if (json.get("tasktype").getAsString().equals(TaskType.REALTIME.name()) && json.get("templet").getAsString().equals(TempletType.TASK_PLAN.name())) {
                String imageorder = json.get("imageorder").getAsString();
                String stationmission = json.get("stationmission").getAsString();

                TaskInit.initRTTaskForTaskPlan(json.get("name").getAsString(), imageorder, stationmission, id);
            } else if (json.get("tasktype").getAsString().equals(TaskType.CRONTAB.name()) && json.get("templet").getAsString().equals(TempletType.ORBIT_FORECAST.name()))
                TaskInit.initCronTaskForOrbitForecast(json.get("name").getAsString(), json.get("firsttime").getAsString(), json.get("cycle").getAsString(), json.get("count").getAsString());

            else if (json.get("tasktype").getAsString().equals(TaskType.REALTIME.name()) && json.get("templet").getAsString().equals(TempletType.ORBIT_FORECAST.name()))
                TaskInit.initRTTaskForOrbitForecast(json.get("name").getAsString());
            else {
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void procCheckQuery(JsonObject json, String id) {
        ArrayList<String> order_numbners = new ArrayList<>();

        String ordermission = json.get("imageorder").getAsString();
        String[] mns = ordermission.split(",");

        //Map<String, Boolean> stringBooleanMap = new TreeMap<>();

        if (mns.length > 0) {
            for (String mn : mns)
                order_numbners.add(mn);
            //stringBooleanMap.put("mn", true);
        }
        ArrayList<String> station_numbners = new ArrayList<>();

        String stationmission = json.get("stationmission").getAsString();
        mns = stationmission.split(",");

        //Map<String, Boolean> stringBooleanMap = new TreeMap<>();

        if (mns.length > 0) {
            for (String mn : mns)
                station_numbners.add(mn);
            //stringBooleanMap.put("mn", true);
        }
        //RedisPublish.checkResult(stringBooleanMap);

        MongoClient mongoClient = MangoDBConnector.getClient();
        //获取名为"temp"的数据库
        MongoDatabase mongoDatabase = mongoClient.getDatabase(DbDefine.DB_NAME);

        //卫星资源表
        MongoCollection<Document> Data_Satllitejson = mongoDatabase.getCollection("satellite_resource");
        Document Satllitejson = Data_Satllitejson.find().first();


        //地面站资源表
        MongoCollection<Document> Data_GroundStationjson = mongoDatabase.getCollection("groundstation_resource");
        FindIterable<Document> D_GroundStationjson = Data_GroundStationjson.find();
        ArrayList<Document> GroundStationjson = new ArrayList<>();
        for (Document document : D_GroundStationjson) {
            GroundStationjson.add(document);
        }


        MongoCollection<Document> Data_Orderjson = mongoDatabase.getCollection("image_order");
        FindIterable<Document> D_Orderjson = Data_Orderjson.find();
        ArrayList<Document> Missionjson = new ArrayList<>();
        for (Document document : D_Orderjson) {
            if (order_numbners.contains(document.getString("order_number"))) {
                Missionjson.add(document);
                Date expected_start_time = document.getDate("expected_start_time");
                if (expected_start_time.before(startTime))
                    startTime = expected_start_time;

                Date expected_end_time = document.getDate("expected_end_time");
                if (expected_end_time.after(endTime))
                    endTime = expected_end_time;
            }
        }
        MongoCollection<Document> station_mission = mongoDatabase.getCollection("station_mission");
        FindIterable<Document> documents = station_mission.find();
        ArrayList<Document> StationMissionjson = new ArrayList<>();
        for (Document document : documents) {
            if (station_numbners.contains(document.getString("mission_number"))) {
                StationMissionjson.add(document);
                Date expected_start_time = document.getDate("expected_start_time");
                if (expected_start_time.before(startTime))
                    startTime = expected_start_time;

                Date expected_end_time = document.getDate("expected_end_time");
                if (expected_end_time.after(endTime))
                    endTime = expected_end_time;
            }
        }
        //轨道数据表
        MongoCollection<Document> Data_Orbitjson = mongoDatabase.getCollection("orbit_attitude");

        Bson queryBson = Filters.and(Filters.gte("time_point", startTime), Filters.lte("time_point", endTime));

        FindIterable<Document> D_orbitjson = Data_Orbitjson.find(Filters.and(queryBson));

        long count = Data_Orbitjson.count(Filters.and(queryBson));

        try {
            Map<Integer, Map<String, Boolean>> integerMapMap = VisibilityCalculation.VisibilityCalculationEmergency(Satllitejson, D_orbitjson, count, GroundStationjson, Missionjson, StationMissionjson);
            RedisPublish.checkResult(id, integerMapMap);

        } catch (ParseException e) {
            e.printStackTrace();
        }

        mongoClient.close();
    }
}
