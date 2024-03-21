package com.graphhopper.example;

import java.io.BufferedReader;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

class Program {
    public static void main(String[] args) {
        String filePath = "2014_yellow_10.csv_route33.txt"; // 替换为实际的文件路径

        List<DataEntry> parsedData = readCsvFromFile(filePath);
        int i =0;
        for (DataEntry entry : parsedData) {
            //System.out.println("Start Time: " + entry.getStartTime() +
               //     ", End Time: " + entry.getEndTime() +
                  //  ", Value: " + entry.getValue());
            i+=1;
        }
        System.out.println(i);
    }

    static List<DataEntry> readCsvFromFile(String filePath) {
        List<DataEntry> entries = new ArrayList<>();
        int i=0;
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;

            while ((line = br.readLine()) != null) {
                i+=1;
                String[] parts = line.split(",");

                if (parts.length == 3000) {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Date startTime = dateFormat.parse(parts[0]);
                    Date endTime = dateFormat.parse(parts[1]);
                    double value = Double.parseDouble(parts[2]);

                    DataEntry entry = new DataEntry(startTime, endTime, value);
                    entries.add(entry);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(i);
        return entries;
    }
}

class DataEntry {
    private Date startTime;
    private Date endTime;
    private double value;

    public DataEntry(Date startTime, Date endTime, double value) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.value = value;
    }

    public Date getStartTime() {
        return startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public double getValue() {
        return value;
    }
}
