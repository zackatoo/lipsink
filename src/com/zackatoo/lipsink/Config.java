/*
 * Config class for settings
 * Follows the singleton model of only allowing one instance of the class to be in creation at one time
 * the getConfig() method is the only way to get the instance of the class
 */

package com.zackatoo.lipsink;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;

public class Config
{
    // These public data members are accessed by creating a new Config object using getConfig
    // They are given default values in the constructor, but will be updated as soon as readConfig is called
    // If readConfig fails then these default values will be used
    public int frameRate;
    public String faceClassifierPath;
    public String landmarkModelPath;
    public String landmarkColor;
    public String recordAudioPath;

    private static Config singleton = null;

    private Config()
    {
        frameRate = 24;
        faceClassifierPath = "resources/faceDetection/haarcascade_frontalface_alt2.xml";
        landmarkModelPath = "resources/faceDetection/lbfmodel.yaml";
        landmarkColor = "YELLOW";
        recordAudioPath = "audio/recorded.wav";
    }

    // Only way to get an instance of the class
    public static Config getConfig()
    {
        if (singleton == null)
        {
            singleton = new Config();
            Config.readConfig();
        }
        return singleton;
    }

    public static void readConfig()
    {
        File configFile = new File("resources/configuration/config.txt");

        if (!configFile.exists())
        {
            writeConfig();
            return;
        }

        Scanner reader = null;

        try
        {
            reader = new Scanner(configFile);
        }
        catch (IOException e)
        {
            System.out.println("Error opening reader for config: " + e.getMessage());
            return;
        }
        StringBuilder sb = new StringBuilder();
        String line;

        while (reader.hasNextLine())
        {
            line = reader.nextLine().trim();
            if (line.length() != 0 && line.charAt(0) != '#')
            {
                sb.append(line);
            }
        }

        reader.close();

        Gson gson = new Gson();
        singleton = gson.fromJson(sb.toString(), Config.class);
    }

    public static void writeConfig()
    {
        File configFile = new File("resources/configuration/config.txt");
        configFile.getParentFile().mkdirs();

        if (!configFile.exists())
        {
            try
            {
                configFile.createNewFile();
            }
            catch (IOException e)
            {
                System.err.println("Error in creating file: " + e.getMessage());
            }
        }

        Gson gson = new Gson();
        PrintWriter writer = null;
        try
        {
            writer = new PrintWriter("resources/configuration/config.txt");
        }
        catch (FileNotFoundException e)
        {
            System.err.println("Error in opening writer for config: " + e.getMessage());
            return;
        }

        writer.write("# LipSink configuration file - do not edit manually\n");
        writer.write("# Written in JSON format\n\n");
        writer.write(gson.toJson(singleton));

        writer.close();
    }
}
