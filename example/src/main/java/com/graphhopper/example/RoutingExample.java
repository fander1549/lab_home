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
import java.util.ArrayList;
import java.util.Locale;

import static com.graphhopper.json.Statement.If;
import static com.graphhopper.json.Statement.Op.LIMIT;
import static com.graphhopper.json.Statement.Op.MULTIPLY;


import com.graphhopper.util.shapes.GHPoint3D;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.apache.commons.lang3.ObjectUtils;

import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;


import java.io.FileReader;
import java.io.IOException;
import java.io.File;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

public class   RoutingExample {
//asojdioaisjd
//tset23123
static  public String[][] trajectoryMatrix ;
static List<Set<String>> listOfSets = new ArrayList<>(79);
static Map<String, String> hashMap1 = new HashMap<>();
//tset
    public static void main(String[] args) throws CsvValidationException, IOException {
        trajectoryMatrix = new String[78][78];
        for (int i = 0; i < 78; i++) {
            for (int j = 0; j < 78; j++) {
                trajectoryMatrix[i][j] = "";
            }
        }
        for (int i = 0; i < 79; i++) {
            listOfSets.add(new HashSet<>());
        }
        String relDir = args.length == 1 ? args[0] : "";
        String data_catogary="chicago";
        String color="green";
        //hoppeer对象

        for (int month_index=2;month_index<=11;month_index++) {
            String osm="";
            if (data_catogary!="chicago")
            {
                osm = "NewYork2.osm.pbf";
            }
            else
            {
                osm="Chicago.osm.pbf";
            }

            GraphHopper hopper = createGraphHopperInstance(relDir + osm, data_catogary);
            // String csvFile ="/Users/rfande/PycharmProjects/STGAN/2014_yellow_02.csv";
            String csvFile = " ";

            if (data_catogary == "bike") {
                String month = " ";
                if (month_index > 9) {
                    month = Integer.toString(month_index);
                } else if (month_index >= 1 && month_index < 10) {
                    month = "0" + Integer.toString(month_index);
                }
                csvFile = "/Users/rfande/Downloads/2014-citibike-tripdata/" + "2014" + month + "-citibike-tripdata_1.csv";
            }

            else  if (data_catogary == "taxi") {
                String month = " ";
                if (month_index > 9) {
                    month = Integer.toString(month_index);
                } else if (month_index >= 1 && month_index < 10) {
                    month = "0" + Integer.toString(month_index);
                }
                csvFile="/Users/rfande/PycharmProjects/STGAN/2014_green_"+month+".csv";
            }

            else if (data_catogary=="chicago")
            {
                month_index=12;
                csvFile="/Users/rfande/Downloads/Taxi_Trips_-_2019_20240701.csv";
            }
                //csvFile = "/Users/rfande/Downloads/2014-citibike-tripdata/" + "2014" + month + "-citibike-tripdata_1.csv";

            System.out.println(csvFile);
            //   String csvFile = "example/src/main/java/com/graphhopper/example/2014_yellow_11.csv";
            //csvFile = "example/src/main/java/com/graphhopper/example/2014_green_10.csv";
            FileReader fr = new FileReader(csvFile);
            CSVReader reader = new CSVReader(fr);
            //初始化写入对象
            File file = new File(csvFile);
            String prefix = file.getName();
            String filePath=" ";
            String filePath2=" ";
            if (data_catogary=="bike")
                 filePath= "bike_route/" + prefix + "_" + "route.txt";
            else if (data_catogary=="taxi")
                 filePath= "taxi_route/" + prefix + "_" + "route.txt";
            else if (data_catogary=="chicago")
            {
                String  month = Integer.toString(month_index);
                filePath= "taxi_route/" + "taxi_trip" +"1-12" + "route2.txt";
                filePath2= "taxi_route/" + "taxi_trip" +"1-12" + "flow_data2.txt";
            }
            // String filePath3 = prefix+"_"+"time_4.25.txt";
            // String filePath =prefix+"_" +"route_temp.txt";
            //String filePath3 = prefix+"_"+"time_temp.txt";
            BufferedOutputStream outputStream = null;
            BufferedOutputStream outputStream_time2  = null;
            BufferedOutputStream instruction_matrix = null;
            try {
                // 在构造函数中初始化 BufferedWriter
                //writer = new BufferedWriter(new FileWriter(filePath));
                outputStream = new BufferedOutputStream(new FileOutputStream(filePath));
                if (data_catogary=="chicago")
                {
                    outputStream = new BufferedOutputStream(new FileOutputStream(filePath2));
                    instruction_matrix = new BufferedOutputStream(new FileOutputStream(filePath));
                }

                //outputStream_time = new BufferedOutputStream(new FileOutputStream(filePath2));

                //outputStream_time2 = new BufferedOutputStream(new FileOutputStream(filePath3));
            } catch (IOException e) {
                e.printStackTrace();
            }
            //执行路径规划操作
            //String csvFile = "/Users/rfande/Downloads/graphhopper-master/example/src/main/java/com/graphhopper/example/2014_yellow_10.csv";
            //String csvFile = "example/src/main/java/com/graphhopper/example/2014_yellow_10.csv";
            //csvFile = "example/src/main/java/com/graphhopper/example/2014_green_10.csv";
            String[] nextLine = reader.readNext();
            int i = 0;
            long startTime = System.currentTimeMillis();
            int count_route = 0;
            int count_time = 0;
            int count2 = 0;
            int retult = 0;
            int retult2 = 0;
            //nextLine = reader.readNext();
            if (data_catogary == "bike" ||data_catogary=="chicago") {
                nextLine = reader.readNext();
            }
            while (nextLine != null)//&&i<9000)//# i<14232487)//487)//2487)//87)//87)//2487)//)487)
            {
                i += 1;
                double lat1 = 0;
                double lon1 = 0;
                double lat2 = 0;
                double lon2 = 0;
                if (data_catogary == "taxi" && color=="green") {
                   //lat1 = Double.parseDouble(nextLine[4]);
                   //lon1 = Double.parseDouble(nextLine[3]);
                   // lat2 = Double.parseDouble(nextLine[6]);
                   // lon2 = Double.parseDouble(nextLine[5]);
                    lat1 = Double.parseDouble(nextLine[3]);
                    lon1 = Double.parseDouble(nextLine[2]);
                    lat2 = Double.parseDouble(nextLine[5]);
                    lon2 = Double.parseDouble(nextLine[4]);
                    retult = routing(data_catogary, hopper, lat1, lon1, lat2, lon2, nextLine[0], nextLine[1], nextLine[6], outputStream, outputStream_time2);
                }
                //nextLine = reader.readNext();
                else if (data_catogary == "bike") {
                    lat1 = Double.parseDouble(nextLine[5]);
                    lon1 = Double.parseDouble(nextLine[6]);
                    lat2 = Double.parseDouble(nextLine[9]);
                    lon2 = Double.parseDouble(nextLine[10]);
                    //bike数据不提供行程距离，随意nextline[0]随便填的
                    //byte[] byteArray = nextLine[5].getBytes(StandardCharsets.UTF_8);
                    // String cellStr = new String(byteArray, StandardCharsets.UTF_8);
                    //System.out.println("Cell as String: " + cellStr);
                    retult = routing(data_catogary, hopper, lat1, lon1, lat2, lon2, nextLine[1], nextLine[2], "00", outputStream, outputStream_time2);
                }
                else if (data_catogary == "chicago") {
                    if (nextLine[17]!="")
                    lat1 = Double.parseDouble(nextLine[17]);
                    if (nextLine[18]!="")
                    lon1 = Double.parseDouble(nextLine[18]);
                    if (nextLine[20]!="")
                    lat2 = Double.parseDouble(nextLine[20]);
                    if (nextLine[21]!="")
                    lon2 = Double.parseDouble(nextLine[21]);
                    //bike数据不提供行程距离，随意nextline[0]随便填的
                    //byte[] byteArray = nextLine[5].getBytes(StandardCharsets.UTF_8);
                    // String cellStr = new String(byteArray, StandardCharsets.UTF_8);
                    //System.out.println("Cell as String: " + cellStr);
                    Double time=0.0;
                   // retult = routing_chicago(data_catogary, hopper,nextLine, outputStream, instruction_matrix);

                    if (nextLine[4]==""){
                        System.out.println("Cell as String: ");
                    }


                }

              /*  if (lat1 == 0 || lat2 == 0 || lon1 == 0 || lon2 == 0 || lat1 <= 40.480007 || lat1 >= 40.9599994 || lat2 <= 40.480007 || lat2 >= 40.9599994 || lon1 <= -74.3599999 || lon1 >= -73.670001 || lon2 <= -74.3599999 || lon2 >= -73.670001) {
                    retult2 = 0;
                } else {
                    retult2 = 1;
                }
                if (retult == 1)
                    count_route += 1;
                if (retult2 == 1)
                    count2 += 1;*/

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
            instruction_matrix.close();
            //outputStream_time2.close();
            hopper.close();
        }
    }

    static GraphHopper createGraphHopperInstance(String ghLoc,String type) {
        GraphHopper hopper = new GraphHopper();
        String type_route="";
        if (type=="taxi"||type=="chicago")
            type_route="car";
        hopper.setOSMFile(ghLoc);
        // specify where to store graphhopper files
        hopper.setGraphHopperLocation("target/routing-graph-cache2"+type);
        // see docs/core/profiles.md to learn more about profiles 不考虑转向开销
        hopper.setProfiles(new Profile(type_route).setVehicle(type_route).setTurnCosts(false));
        // this enables speed mode for the profile we called car
        //Contraction Hierarchie算法
        hopper.getCHPreparationHandler().setCHProfiles(new CHProfile(type_route));
        // now this can take minutes if it imports or a few seconds for loading of course this is dependent on the area you import
        hopper.importOrLoad();
        return hopper;
    }
    public static int routing_chicago(String type ,GraphHopper hopper,String [] nextLine,BufferedOutputStream  outputStream,BufferedOutputStream matrix )  {
        String prof="car";
        if( type=="taxi")
        {
            prof="car";
        }
        else  if (type=="bike")
        {
            prof="bike";
        }
        double lat1 = 0;
        double lon1 = 0;
        double lat2 = 0;
        double lon2 = 0;
        int start_zone=-1;
        int end_zone=-1;


        if (nextLine[8]!=""&&nextLine[9]!="")
        {   start_zone = Integer.parseInt(nextLine[8]);
            end_zone = Integer.parseInt(nextLine[9]);

            ///listOfSets.get(start_zone).add(nextLine[17]+nextLine[18]);
            //listOfSets.get(end_zone).add(nextLine[20]+nextLine[21]);
            //if (nextLine[17]!="")
                lat1 = Double.parseDouble(nextLine[17]);
            //if (nextLine[18]!="")
                lon1 = Double.parseDouble(nextLine[18]);
            //if (nextLine[20]!="")
                lat2 = Double.parseDouble(nextLine[20]);
          //  if (nextLine[21]!="")
                lon2 = Double.parseDouble(nextLine[21]);
        }
        else{
            return 0;
        }
        String key=nextLine[17]+' '+nextLine[18]+' '+nextLine[20]+' '+nextLine[21];
        //if (trajectoryMatrix[start_zone][end_zone]!="1"&& nextLine[8]!=""&&nextLine[9]!="")
        if (hashMap1.containsKey(key)==false&& nextLine[8]!=""&&nextLine[9]!=""&&start_zone!=end_zone)
        {
            //trajectoryMatrix[start_zone][end_zone]="1";

            GHRequest req = new GHRequest(lat1,lon1, lat2,lon2).
                // note that we have to specify which profile we are using even when there is only one like here
                        setProfile(prof).
                // define the language for the turn instructions
                        setLocale(Locale.US);
        GHResponse rsp = hopper.route(req);
        // handle errors
        if (rsp.hasErrors())
        {
            lat1=lat1+1;
            return 0;
        }
        //throw new RuntimeException(rsp.getErrors().toString());
        // use the best path, see the GHResponse class for more possibilities.
        ResponsePath path = rsp.getBest();
        hashMap1.put(key,"2");
        // 输出路径相关信息  points, distance in meters and time in millis of the full path
        PointList pointList = path.getPoints();

        InstructionList instructions = path.getInstructions();
        int ins_size=instructions.size();
        ArrayList<GHPoint3D> GHpointList=new ArrayList<>();
        ArrayList<Double> distance_list=new ArrayList<>();
        ArrayList<Double> distance_ration_list=new ArrayList<>();
        double total_len= path.getDistance();
        int index=0;
        for (int i=0;i<ins_size-1;i++)
        {
            //System.out.println(instructions.get(i).getPoints().get(0).lat);
            double ins_dis=instructions.get(i).getDistance();
            //double ins_dis_ratio=ins_dis/
            GHPoint3D point1=instructions.get(i).getPoints().get(0);
            //ins_dis=instructions.get(i).getDistance();
            //taxi以250单位分段,bike是75分段
            int segement_num=0;
            if (type=="taxi" ||type=="chicago")
                segement_num= (int) (ins_dis/250)+1;
            else if (type=="bike"){  segement_num= (int) (ins_dis/75)+1;}

            GHPoint3D point2=instructions.get(i+1).getPoints().get(0);
            double segement_lat=(point2.lat-point1.lat)/(segement_num);
            double segement_lon=(point2.lon-point1.lon)/segement_num;
            double segement_len=ins_dis/segement_num;
            //加入第一个点

            GHPoint3D GHPoint3D_start=new GHPoint3D(point1.lat+0.5*segement_lat,0.5*segement_lon+point1.lon,0);
            GHpointList.add(GHPoint3D_start);
            distance_ration_list.add(segement_len/total_len);
            distance_list.add(segement_len);
            for (int j=1;j<segement_num;j++)
            {
                //加入后续点 如果有
                GHPoint3D GHPoint3D_j=new GHPoint3D(GHPoint3D_start.lat+j*segement_lat,GHPoint3D_start.lon+j*segement_lon,0);
                GHpointList.add(GHPoint3D_j);
                distance_list.add(segement_len);
                distance_ration_list.add(segement_len/total_len);
            }

        }
        //double sum2 = distance_list.stream().mapToDouble(Double::doubleValue). sum();
        //double sum = distance_ration_list.stream().mapToDouble(Double::doubleValue). sum();
        double distance = path.getDistance();
        String distance_str = Double.toString(distance);

        long timeInMs = path.getTime();
        String timeInMs_str = Long.toString(timeInMs);
        //  String data=pointList.toString();
        byte[]space = " ".getBytes();
        try {
            byte[] newLine = "\n".getBytes();
            byte[] fenge = "_".getBytes();
            //byte[] binaryData0 = Integer.toString(start_zone).getBytes();//start zone
            //byte[] binaryData1 = Integer.toString(end_zone).getBytes();//end zone
            byte[] binaryData8 = Integer.toString(start_zone).getBytes();//start zone
            byte[] binaryData9 = Integer.toString(end_zone).getBytes();//end z
            byte[] binaryData0 = Double.toString(lat1).getBytes();//start zone
            byte[] binaryData1 = Double.toString(lon1).getBytes();//end zone
            byte[] binaryData00 = Double.toString(lat2).getBytes();
            byte[] binaryData11 = Double.toString(lon2).getBytes();
            matrix.write(binaryData8);
            matrix.write(fenge);
            matrix.write(binaryData9);
            matrix.write(fenge);
            matrix.write(binaryData0);
            matrix.write(fenge);
            matrix.write(binaryData1);
            matrix.write(fenge);
            matrix.write(binaryData00);
            matrix.write(fenge);
            matrix.write(binaryData11);
            matrix.write(fenge);
            for (int i = 0; i < GHpointList.size(); i++) {
                byte[] lat = String.valueOf(GHpointList.get(i).lat).getBytes();
                byte[] lon = String.valueOf(GHpointList.get(i).lon).getBytes();
                byte[] ratio = String.valueOf(distance_ration_list.get(i)).getBytes();
                matrix.write(lat);
                matrix.write(space);
                matrix.write(lon);
                matrix.write(space);
                matrix.write(ratio);
                matrix.write(space);
            }
            matrix.write(newLine);
        } catch (IOException e) {
            e.printStackTrace();
        }

        }
            // 在需要写入数据的方法中使用 BufferedWriter

            //写入文件1
            //byte[] binaryData = data.getBytes();
            //outputStream.write(binaryData);
            //outputStream.write(newLine);


            //写入文件2
            //原始od数据
           // String time1="";
          //  String time2="";
        //    String length="";
        try {
            byte[] newLine = "\n".getBytes();
            //byte[] binaryData4=time1.getBytes();
            byte[] binaryData10 = key.getBytes();
            byte[] binaryData8 = Integer.toString(start_zone).getBytes();//start zone
            byte[] binaryData9 = Integer.toString(end_zone).getBytes();//end zone
            byte[] binaryData4=nextLine[2].getBytes();//start time
            //byte[] binaryData5=time2.getBytes();
            byte[] binaryData5=nextLine[3].getBytes();//end time
           // byte[] binaryData6=length.getBytes();
            byte[] binaryData6=nextLine[4].getBytes();//time cost

            byte[] binaryData7=nextLine[5].getBytes();//miles


            //路径规划得到的行驶距离
           // byte[] binaryData2 = distance_str.getBytes();
            //路径规划得到的时间
          //  byte[] binaryData3 = timeInMs_str.getBytes();

            byte[] gap = ",".getBytes();
            outputStream.write(binaryData8);
            outputStream.write(gap);
            outputStream.write(binaryData9);
            outputStream.write(gap);
            outputStream.write(binaryData10);
            outputStream.write(gap);
            outputStream.write(binaryData4);
            outputStream.write(gap);
            outputStream.write(binaryData5);
            outputStream.write(gap);
            outputStream.write(binaryData6);
            outputStream.write(gap);
            outputStream.write(binaryData7);
            //outputStream.write(gap);
         //   outputStream.write(binaryData2);
            outputStream.write(gap);
      //      outputStream.write(binaryData3);
            /*
            outputStream_time2.write(binaryData4);
            outputStream_time2.write(gap);
            outputStream_time2.write(binaryData5);
            outputStream_time2.write(gap);
            outputStream_time2.write(binaryData6);
            outputStream_time2.write(gap);
            outputStream_time2.write(binaryData2);
            outputStream_time2.write(gap);
            outputStream_time2.write(binaryData3);
            //写入换行符*/
            outputStream.write(newLine);
        }
        catch (IOException e) {
            e.printStackTrace();
        }



        //System.out.println(distance);
        //  System.out.println(pointList.size());
        //System.out.println(x);
        Translation tr = hopper.getTranslationMap().getWithFallBack(Locale.UK);
  //      InstructionList il = path.getInstructions();
        // iterate over all turn instructions
       // for (Instruction instruction : il) {
            // System.out.println("distance " + instruction.getDistance() + " for instruction: " + instruction.getTurnDescription(tr));
   //     }
      //  assert il.size() == 6;
      //  assert Helper.round(path.getDistance(), -2) == 900;

      //  #if ( pointList.size()>=1)
        //{
            return 1;
        //}
       // else
     //   {return 0;}

    }

    public static int routing(String type ,GraphHopper hopper,double lat1,double lon1,double lat2,double lon2,String time1,String time2,String length,BufferedOutputStream  outputStream,BufferedOutputStream outputStream_time2  )  {
        String prof="";
        if( type=="taxi")
        {
            prof="car";
        }
        else  if (type=="bike")
        {
            prof="bike";
        }
        GHRequest req = new GHRequest(lat1,lon1, lat2,lon2).
                // note that we have to specify which profile we are using even when there is only one like here
                        setProfile(prof).
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
        InstructionList instructions = path.getInstructions();
        int ins_size=instructions.size();
        ArrayList<GHPoint3D> GHpointList=new ArrayList<>();
        ArrayList<Double> distance_list=new ArrayList<>();
        ArrayList<Double> distance_ration_list=new ArrayList<>();
        double total_len= path.getDistance();
        int index=0;
        for (int i=0;i<ins_size-1;i++)
        {
            //System.out.println(instructions.get(i).getPoints().get(0).lat);
            double ins_dis=instructions.get(i).getDistance();
            //double ins_dis_ratio=ins_dis/
            GHPoint3D point1=instructions.get(i).getPoints().get(0);
            //ins_dis=instructions.get(i).getDistance();
            //taxi以250单位分段,bike是75分段
            int segement_num=0;
            if (type=="taxi")
                segement_num= (int) (ins_dis/250)+1;
            else if (type=="bike"){  segement_num= (int) (ins_dis/75)+1;}

                GHPoint3D point2=instructions.get(i+1).getPoints().get(0);
                double segement_lat=(point2.lat-point1.lat)/(segement_num);
                double segement_lon=(point2.lon-point1.lon)/segement_num;
                double segement_len=ins_dis/segement_num;
                //加入第一个点

                GHPoint3D GHPoint3D_start=new GHPoint3D(point1.lat+0.5*segement_lat,0.5*segement_lon+point1.lon,0);
                GHpointList.add(GHPoint3D_start);
                distance_ration_list.add(segement_len/total_len);
                distance_list.add(segement_len);
                for (int j=1;j<segement_num;j++)
                {
                    //加入后续点 如果有
                    GHPoint3D GHPoint3D_j=new GHPoint3D(GHPoint3D_start.lat+j*segement_lat,GHPoint3D_start.lon+j*segement_lon,0);
                    GHpointList.add(GHPoint3D_j);
                    distance_list.add(segement_len);
                    distance_ration_list.add(segement_len/total_len);
                }

            }
       //double sum2 = distance_list.stream().mapToDouble(Double::doubleValue). sum();
        //double sum = distance_ration_list.stream().mapToDouble(Double::doubleValue). sum();
        double distance = path.getDistance();
        String distance_str = Double.toString(distance);

        long timeInMs = path.getTime();
        String timeInMs_str = Long.toString(timeInMs);
      //  String data=pointList.toString();
        byte[]space = " ".getBytes();
        try {
            byte[] newLine = "\n".getBytes();
            byte[] fenge = "_".getBytes();
            for (int i=0;i<GHpointList.size();i++)
            {
                byte[] lat=String.valueOf(GHpointList.get(i).lat).getBytes();
                byte[] lon=String.valueOf(GHpointList.get(i).lon).getBytes();
                byte[] ratio=String.valueOf(distance_ration_list.get(i)).getBytes();
                outputStream.write(lat);
                outputStream.write(space);
                outputStream.write(lon);
                outputStream.write(space);
                outputStream.write(ratio);
                outputStream.write(space);
            }
            outputStream.write(fenge);


            // 在需要写入数据的方法中使用 BufferedWriter

            //写入文件1
            //byte[] binaryData = data.getBytes();
            //outputStream.write(binaryData);
            //outputStream.write(newLine);


            //写入文件2
            //原始od数据
            byte[] binaryData4=time1.getBytes();
            byte[] binaryData5=time2.getBytes();
            byte[] binaryData6=length.getBytes();
            //路径规划得到的行驶距离
            byte[] binaryData2 = distance_str.getBytes();
            //路径规划得到的时间
            byte[] binaryData3 = timeInMs_str.getBytes();
            byte[] gap = ",".getBytes();
            outputStream.write(binaryData4);
            outputStream.write(gap);
            outputStream.write(binaryData5);
            outputStream.write(gap);
            outputStream.write(binaryData6);
            outputStream.write(gap);
            outputStream.write(binaryData2);
            outputStream.write(gap);
            outputStream.write(binaryData3);
            /*
            outputStream_time2.write(binaryData4);
            outputStream_time2.write(gap);
            outputStream_time2.write(binaryData5);
            outputStream_time2.write(gap);
            outputStream_time2.write(binaryData6);
            outputStream_time2.write(gap);
            outputStream_time2.write(binaryData2);
            outputStream_time2.write(gap);
            outputStream_time2.write(binaryData3);
            //写入换行符*/
            outputStream.write(newLine);


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
