package com.graphhopper.example;
import java.io.BufferedWriter;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileWriter;
//import java.io.IOException;
import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.ResponsePath;
import com.graphhopper.config.CHProfile;
import com.graphhopper.config.LMProfile;
import com.graphhopper.config.Profile;
import com.graphhopper.util.*;
import com.graphhopper.util.shapes.GHPoint;

import java.io.FileNotFoundException;
import java.util.Locale;

import static com.graphhopper.json.Statement.If;
import static com.graphhopper.json.Statement.Op.LIMIT;
import static com.graphhopper.json.Statement.Op.MULTIPLY;


import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import java.io.FileReader;
import java.io.IOException;
import java.io.File;

public class RoutingExample {
//asojdioaisjd
//tset23123

//tset
    public static void main(String[] args) throws CsvValidationException, IOException {
        String relDir = args.length == 1 ? args[0] : "";
        //hoppeer对象
        GraphHopper hopper = createGraphHopperInstance(relDir + "NewYork.osm.pbf");

        String csvFile = "example/src/main/java/com/graphhopper/example/2014_yellow_10.csv";
        //csvFile = "example/src/main/java/com/graphhopper/example/2014_green_10.csv";
        FileReader fr = new FileReader(csvFile);
        CSVReader reader = new CSVReader(fr);
        //初始化写入对象
        File file = new File(csvFile);
        String prefix=file.getName();
        String filePath =prefix+"_" +"route3333.txt";
        String filePath2 = prefix+"_"+"time3333.txt";
        String filePath3 = prefix+"_"+"time3333.txt";
        BufferedOutputStream outputStream  = null;
        //BufferedOutputStream outputStream_time  = null;
        BufferedOutputStream outputStream_time2  = null;
        try {
            // 在构造函数中初始化 BufferedWriter
            //writer = new BufferedWriter(new FileWriter(filePath));
            outputStream = new BufferedOutputStream(new FileOutputStream(filePath));
            //outputStream_time = new BufferedOutputStream(new FileOutputStream(filePath2));
            outputStream_time2 = new BufferedOutputStream(new FileOutputStream(filePath3));
        } catch (IOException e) {
            e.printStackTrace();
        }
        //执行路径规划操作
        //String csvFile = "/Users/rfande/Downloads/graphhopper-master/example/src/main/java/com/graphhopper/example/2014_yellow_10.csv";
        //String csvFile = "example/src/main/java/com/graphhopper/example/2014_yellow_10.csv";
        //csvFile = "example/src/main/java/com/graphhopper/example/2014_green_10.csv";
        String[] nextLine=reader.readNext();
        int i=0;
        long startTime = System.currentTimeMillis();
        int count_route=0;
        int count_time=0;
        int count2=0;
        int retult=0;
        int retult2=0;
        //nextLine = reader.readNext();
        while (nextLine != null&& i<14232487)//87)//87)//2487)//)487)
        {
            i += 1;
            double lat1 = Double.parseDouble(nextLine[4]);
            double lon1 = Double.parseDouble(nextLine[3]);
            double lat2 = Double.parseDouble(nextLine[6]);
            double lon2 = Double.parseDouble(nextLine[5]);
            retult = routing(hopper, lat1, lon1, lat2, lon2, nextLine[0],nextLine[1],nextLine[2],outputStream,outputStream_time2);
            //nextLine = reader.readNext();

            if (lat1 == 0 || lat2 == 0 || lon1 == 0 || lon2 == 0 || lat1 <= 40.480007 ||lat1>=40.9599994 ||lat2 <= 40.480007 ||lat2>=40.9599994||lon1<=-74.3599999||lon1>=-73.670001||lon2<=-74.3599999||lon2>=-73.670001)
            {retult2=0 ;}
            else
            {retult2=1;}
            if (retult==1)
                count_route+=1;
            if (retult2==1)
                count2+=1;

          /*  if (retult==1)
            {
                count_time+=1;
                byte[] binaryData = (nextLine[0]+',').getBytes();
                byte[] binaryData1 = (nextLine[1]+',').getBytes();
                byte[] binaryData2 = nextLine[2].getBytes();
                outputStream_time2.write(binaryData);
                outputStream_time2.write(binaryData1);
                outputStream_time2.write(binaryData2);
                byte[] newLine = "\n".getBytes();
                outputStream_time2.write(newLine);
            }*/
            nextLine = reader.readNext();
        }
        System.out.println(count_route);
        System.out.println(count_time);
        System.out.println(i);
        long startTime2 = System.currentTimeMillis();
        long executionTime = startTime2 - startTime;
        System.out.println("代码段执行时间: " + executionTime + " 毫秒");
        //1speedModeVersusFlexibleMode(hopper);
        //备选路径
        //1alternativeRoute(hopper);
        //传递了包含地图数据文件路径的字符串作为参数。
        //1customizableRouting(relDir + "NewYork.osm.pbf");
        // release resources to properly shutdown or start a new instance
        outputStream.close();
        outputStream_time2.close();
        hopper.close();
    }

    static GraphHopper createGraphHopperInstance(String ghLoc) {
        GraphHopper hopper = new GraphHopper();
        hopper.setOSMFile(ghLoc);
        // specify where to store graphhopper files
        hopper.setGraphHopperLocation("target/routing-graph-cache2");
        // see docs/core/profiles.md to learn more about profiles 不考虑转向开销
        hopper.setProfiles(new Profile("car").setVehicle("car").setTurnCosts(false));
        // this enables speed mode for the profile we called car
        //Contraction Hierarchie算法
        hopper.getCHPreparationHandler().setCHProfiles(new CHProfile("car"));
        // now this can take minutes if it imports or a few seconds for loading of course this is dependent on the area you import
        hopper.importOrLoad();
        return hopper;
    }

    public static int routing(GraphHopper hopper,double lat1,double lon1,double lat2,double lon2,String time1,String time2,String length,BufferedOutputStream  outputStream,BufferedOutputStream outputStream_time2  )  {


        GHRequest req = new GHRequest(lat1,lon1, lat2,lon2).
                // note that we have to specify which profile we are using even when there is only one like here
                        setProfile("car").
                // define the language for the turn instructions
                        setLocale(Locale.US);
        GHResponse rsp = hopper.route(req);

        // handle errors
        if (rsp.hasErrors())
        {lat1=lat1+1;
            return 0;}
            //throw new RuntimeException(rsp.getErrors().toString());

        // use the best path, see the GHResponse class for more possibilities.
        ResponsePath path = rsp.getBest();

        // 输出路径相关信息  points, distance in meters and time in millis of the full path
        PointList pointList = path.getPoints();
        double distance = path.getDistance();
        String distance_str = Double.toString(distance);
        long timeInMs = path.getTime();
        String timeInMs_str = Long.toString(timeInMs);
        String data=pointList.toString();

        try {
            // 在需要写入数据的方法中使用 BufferedWriter
            byte[] newLine = "\n".getBytes();
            //写入文件1
            byte[] binaryData = data.getBytes();
            outputStream.write(binaryData);
            outputStream.write(newLine);


            //写入文件2
            byte[] binaryData4=time1.getBytes();
            byte[] binaryData5=time2.getBytes();
            byte[] binaryData6=length.getBytes();
            byte[] binaryData2 = distance_str.getBytes();
            byte[] binaryData3 = timeInMs_str.getBytes();
            byte[] gap = ",".getBytes();
            outputStream_time2.write(binaryData4);
            outputStream_time2.write(gap);
            outputStream_time2.write(binaryData5);
            outputStream_time2.write(gap);
            outputStream_time2.write(binaryData6);
            outputStream_time2.write(gap);
            outputStream_time2.write(binaryData2);
            outputStream_time2.write(gap);
            outputStream_time2.write(binaryData3);
            //写入换行符
            outputStream_time2.write(newLine);


        } catch (IOException e) {
            e.printStackTrace();
        }


        //System.out.println(distance);
      //  System.out.println(pointList.size());
        //System.out.println(x);
        Translation tr = hopper.getTranslationMap().getWithFallBack(Locale.UK);
        InstructionList il = path.getInstructions();
        // iterate over all turn instructions
        for (Instruction instruction : il) {
            // System.out.println("distance " + instruction.getDistance() + " for instruction: " + instruction.getTurnDescription(tr));
        }
        assert il.size() == 6;
        assert Helper.round(path.getDistance(), -2) == 900;

        if ( pointList.size()>=1)
        {
            return 1;
        }
        else
        {return 0;}

    }

    public static void speedModeVersusFlexibleMode(GraphHopper hopper) {
        GHRequest req = new GHRequest(42.508552, 1.532936, 42.507508, 1.528773).
                setProfile("car").setAlgorithm(Parameters.Algorithms.ASTAR_BI).putHint(Parameters.CH.DISABLE, true);
        GHResponse res = hopper.route(req);
        if (res.hasErrors())
            throw new RuntimeException(res.getErrors().toString());
        assert Helper.round(res.getBest().getDistance(), -2) == 900;
    }

    public static void alternativeRoute(GraphHopper hopper) {
        // calculate alternative routes between two points (supported with and without CH)
        GHRequest req = new GHRequest().setProfile("car").
                addPoint(new GHPoint(42.502904, 1.514714)).addPoint(new GHPoint(42.508774, 1.537094)).
                setAlgorithm(Parameters.Algorithms.ALT_ROUTE);
        req.getHints().putObject(Parameters.Algorithms.AltRoute.MAX_PATHS, 3);
        GHResponse res = hopper.route(req);
        if (res.hasErrors())
            throw new RuntimeException(res.getErrors().toString());
        assert res.getAll().size() == 2;
        assert Helper.round(res.getBest().getDistance(), -2) == 2200;
    }

    /**
     * To customize profiles in the config.yml file you can use a json or yml file or embed it directly. See this list:
     * web/src/test/resources/com/graphhopper/application/resources and https://www.graphhopper.com/?s=customizable+routing
     */
    public static void customizableRouting(String ghLoc) {
        GraphHopper hopper = new GraphHopper();
        hopper.setOSMFile(ghLoc);
        hopper.setGraphHopperLocation("target/routing-custom-graph-cache2");
        CustomModel serverSideCustomModel = new CustomModel();
        hopper.setProfiles(new Profile("car_custom").setCustomModel(serverSideCustomModel).setVehicle("car"));

        // The hybrid mode uses the "landmark algorithm" and is up to 15x faster than the flexible mode (Dijkstra).
        // Still it is slower than the speed mode ("contraction hierarchies algorithm") ...
        // landmark算法
        hopper.getLMPreparationHandler().setLMProfiles(new LMProfile("car_custom"));
        hopper.importOrLoad();

        // ... but for the hybrid mode we can customize the route calculation even at request time:
        // 1. a request with default preferences

        //GHRequest req = new GHRequest(42.508552, 1.532936, 42.507508, 1.528773).
        //        GHRequest req = new GHRequest(40.7177772,-73.927656860351563, 40.7410319519042969,-73.966835021972656).
        GHRequest req = new GHRequest().setProfile("car_custom").
                addPoint(new GHPoint(40.7177772,-73.927656860351563)).addPoint(new GHPoint(40.7410319519042969,-73.966835021972656));

        GHResponse res = hopper.route(req);
        ResponsePath path = res.getBest();
        PointList pointList = path.getPoints();

        double distance = path.getDistance();
        long timeInMs = path.getTime();
        System.out.println("1231827398127398132");
        System.out.println(distance);
        System.out.println(pointList);


        if (res.hasErrors())
            throw new RuntimeException(res.getErrors().toString());

        assert Math.round(res.getBest().getTime() / 1000d) == 94;

        // 2. now avoid primary roads and reduce maximum speed, see docs/core/custom-models.md for an in-depth explanation
        // and also the blog posts https://www.graphhopper.com/?s=customizable+routing
        CustomModel model = new CustomModel();
        model.addToPriority(If("road_class == PRIMARY", MULTIPLY, "0.5"));

        // unconditional limit to 100km/h
        model.addToPriority(If("true", LIMIT, "100"));

        req.setCustomModel(model);
        res = hopper.route(req);
        if (res.hasErrors())
            throw new RuntimeException(res.getErrors().toString());

        assert Math.round(res.getBest().getTime() / 1000d) == 164;
    }
}
