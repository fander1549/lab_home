package com.graphhopper.example;

import com.graphhopper.GraphHopper;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

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
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;




    public class test {

    public static void main(String[] args) throws CsvValidationException, IOException {
        String relDir = args.length == 1 ? args[0] : "";

        int count=0;
        //执行路径规划操作
        //String csvFile = "/Users/rfande/Downloads/graphhopper-master/example/src/main/java/com/graphhopper/example/2014_yellow_10.csv";
        String filePath = "2014_yellow_10.csv_time33.txt";
        //FileReader fr = new FileReader(csvFile);
       // CSVReader reader = new CSVReader(fr);
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            // 逐行读取文件内容
            while ((line = br.readLine()) != null) {
                count+=1;
                // 在这里可以对每一行的数据进行处理
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(count);

        }}
        /*System.out.println(count);
        System.out.println(count2);
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
        hopper.close();*/

