package core;

/**
 * Created by lihan on 2018/10/24.
 */
public class TaskPlanCore {
    public static void main(String[] args) throws InterruptedException {
        String id = args[0];

//        Thread.sleep(2000);
//
//        System.out.println("执行订单收集与统筹任务...");
//
//        Thread.sleep(5000);
//
//        System.out.println("执行多星任务规划任务...");
//
//        Thread.sleep(5000);
//
//        System.out.println("执行单星任务规划任务...");
//
//        Thread.sleep(2000);
//
//        System.out.println("本次任务执行完成...");
//
//        MongoClient mongoClient = new MongoClient("localhost", 27017);
//        MongoDatabase mongoDatabase = mongoClient.getDatabase("TSS");
//
//        MongoCollection<Document> tasks = mongoDatabase.getCollection("main_task");
//
//        Document document = tasks.find(new Document("_id", id)).first();
//
//        tasks.updateOne(Filters.eq("_id", document.get("_id").toString()), new Document("$set", new Document("status", "PENDING")));
//
//        mongoClient.close();
//
//        System.exit(0);//正常退出
    }
}
