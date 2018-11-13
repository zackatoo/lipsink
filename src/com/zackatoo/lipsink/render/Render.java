package com.zackatoo.lipsink.render;

import com.zackatoo.lipsink.Config;
import com.zackatoo.lipsink.LipSinkController;
import com.zackatoo.lipsink.geometry.EllipseFunction;
import com.zackatoo.lipsink.panels.exportpanel.ExportController;
import com.zackatoo.lipsink.record.MouthLandmarkPoints.Point;
import com.zackatoo.lipsink.record.MouthLandmarkPoints.Frame;
import javafx.concurrent.Task;
import marvin.image.MarvinImage;
import marvin.io.MarvinImageIO;

import java.awt.*;
import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Vector;

import marvin.plugin.MarvinImagePlugin;
import marvin.util.MarvinPluginLoader;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

public class Render extends Task<Boolean>
{
    private static final int BACKGROUND_COLOR = 0xFFFFFF;

    // Average time it takes to render a single frame in milliseconds
    private static final int AVERAGE_RENDER_TIME = 44;

    private int currentFrame = 0;
    private int totalFrames = 0;

    private int renderWidth;
    private int renderHeight;
    private int frameRate;

    private float recordWidth;
    private float recordHeight;

    private String exportDirectory;
    private ArrayList<Frame> recordedFrames;
    private Vector<String> compiledPaths;
    private String renderDirectoryPath;
    private File renderDirectory;
    private String audioPath;

    private Config config;

    private MarvinImagePlugin boundaryFill;

    protected Boolean call()
    {
        initialize();

        // Uncomment to show performance readings
        // long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < recordedFrames.size(); i++)
        {
            createFrame(recordedFrames.get(i), i);
            incr();
            updateMessage("Estimated Time Remaining: " + getRemainingTime());
        }

        // System.out.println("Total time: " + (System.currentTimeMillis() - startTime) + "  total frames: " + (totalFrames - 1) + "  average: " + ((System.currentTimeMillis() - startTime) / (totalFrames - 1)));

        finish();

        return true;
    }

    private String getRemainingTime()
    {
        // time is in seconds
        int time = (int)Math.ceil(AVERAGE_RENDER_TIME * (totalFrames - 1 - currentFrame) / 1000.0f);

        // Converts it into appropriate time units. No one wants to know that it will take 66472s
        if (time > 60)
        {
            if (time > 3600)
            {
                int hours = (int)Math.ceil(time / 3600.0f);
                int minutes = time % 3600;
                return hours + "h " + minutes + "m";
            }

            return (int)Math.ceil(time / 60.0f) + "m";
        }

        return time + "s";
    }

    private void initialize()
    {
        compiledPaths = new Vector<>();
        recordedFrames = LipSinkController.getRecordedFrames();
        exportDirectory = ExportController.getExportDirectory();
        totalFrames = recordedFrames.size() + 1;

        renderDirectoryPath = "render-" + System.currentTimeMillis() + "/";
        renderDirectory = new File(renderDirectoryPath);
        renderDirectory.mkdir();

        renderWidth = ExportController.getWidth();
        renderHeight = ExportController.getHeight();
        frameRate = ExportController.getFrameRate();

        recordWidth = recordedFrames.get(0).width;
        recordHeight = recordedFrames.get(0).height;

        scaleFramesToSize();
        stableizeFrames();
        currentFrame = 0;

        boundaryFill = MarvinPluginLoader.loadImagePlugin("org.marvinproject.image.fill.boundaryFill.jar");

        config = Config.getConfig();
    }

    private void createFrame(Frame frame, int index)
    {
        MarvinImage image = new MarvinImage(renderWidth, renderHeight);
        image.clear(BACKGROUND_COLOR);

        //createLiteralTranslationFrame(image, frame);
        //createSimpleFrame(image, frame);
        createElipseFrame(image, frame);

        String path = renderDirectoryPath + "renderedImage-" + index + ".jpg";
        MarvinImageIO.saveImage(image, path);
        compiledPaths.add(path);
    }

    // Uses interpolation to fill in where the mouth should be
    private void createLiteralTranslationFrame(MarvinImage image, Frame frame)
    {
        double[] mouthTopX = new double[7];
        double[] mouthTopY = new double[7];

        double[] mouthBottomX = new double[7];
        double[] mouthBottomY = new double[7];

        double[] mouthInsideTopX = new double[5];
        double[] mouthInsideTopY = new double[5];

        double[] mouthInsideBottomX = new double[5];
        double[] mouthInsideBottomY = new double[5];

        for (int i = 0; i <= 6; i++)
        {
            mouthTopX[i] = frame.mouthPoints.get(i).x;
            mouthTopY[i] = frame.mouthPoints.get(i).y;
        }

        mouthBottomX[0] = frame.mouthPoints.get(0).x;
        mouthBottomY[0] = frame.mouthPoints.get(0).y;

        for (int i = 5; i >= 0; i--)
        {
            mouthBottomX[6 - i] = frame.mouthPoints.get(i + 6).x;
            mouthBottomY[6 - i] = frame.mouthPoints.get(i + 6).y;
        }

        for (int i = 0; i <= 4; i++)
        {
            mouthInsideTopX[i] = frame.mouthPoints.get(i + 12).x;
            mouthInsideTopY[i] = frame.mouthPoints.get(i + 12).y;
        }

        mouthInsideBottomX[0] = frame.mouthPoints.get(12).x;
        mouthInsideBottomY[0] = frame.mouthPoints.get(12).y;

        for (int i = 3; i >= 0; i--)
        {
            mouthInsideBottomX[4 - i] = frame.mouthPoints.get(i + 16).x;
            mouthInsideBottomY[4 - i] = frame.mouthPoints.get(i + 16).y;
        }

        SplineInterpolator interpolator = new SplineInterpolator();
        PolynomialSplineFunction splineTop = interpolator.interpolate(mouthTopX, mouthTopY);
        PolynomialSplineFunction splineBottom = interpolator.interpolate(mouthBottomX, mouthBottomY);
        PolynomialSplineFunction splineInsideTop = interpolator.interpolate(mouthInsideTopX, mouthInsideTopY);
        PolynomialSplineFunction splineInsideBottom = interpolator.interpolate(mouthInsideBottomX, mouthInsideBottomY);

        for (int i = frame.mouthPoints.get(0).x; i < frame.mouthPoints.get(6).x; i++)
        {
            int height = (int)Math.round(splineTop.value(i));
            drawCircle(image, i, height, 0);
        }

        for (int i = frame.mouthPoints.get(0).x; i < frame.mouthPoints.get(6).x; i++)
        {
            int height = (int)Math.round(splineBottom.value(i));
            drawCircle(image, i, height, 0);
        }

        int[] fillPoint = middlePoint(frame.mouthPoints.get(3), frame.mouthPoints.get(8));
        boundaryFill.setAttribute("x", fillPoint[0]);
        boundaryFill.setAttribute("y", fillPoint[1]);
        boundaryFill.setAttribute("color", Color.red);
        boundaryFill.process(image, image);

        for (int i = frame.mouthPoints.get(12).x; i < frame.mouthPoints.get(16).x; i++)
        {
            int height = (int)Math.round(splineInsideTop.value(i));
            drawCircle(image, i, height, 0);
        }

        for (int i = frame.mouthPoints.get(12).x; i < frame.mouthPoints.get(16).x; i++)
        {
            int height = (int)Math.round(splineInsideBottom.value(i));
            drawCircle(image, i, height, 0);
        }

        fillPoint = middlePoint(frame.mouthPoints.get(14), frame.mouthPoints.get(18));
        if (image.getIntColor(fillPoint[0], fillPoint[1]) != 0)
        {
            boundaryFill.setAttribute("x", fillPoint[0]);
            boundaryFill.setAttribute("y", fillPoint[1]);
            boundaryFill.setAttribute("color", new Color(0xff1645));
            boundaryFill.process(image, image);
        }
    }

    // Almost the same as a literal translation, but only uses 3 points for top and bottom lip
    // Also uses the interior points of the lip
    private void createSimpleFrame(MarvinImage image, Frame frame)
    {
        double[] mouthTopX = new double[3];
        double[] mouthTopY = new double[3];

        double[] mouthBottomX = new double[3];
        double[] mouthBottomY = new double[3];

        // Since the inner mouth has a slight dip at the topmost point so it uses the Y of the heighest point
        mouthTopX[0] = frame.mouthPoints.get(12).x;
        mouthTopY[0] = frame.mouthPoints.get(12).y;
        mouthTopX[1] = frame.mouthPoints.get(14).x;
        mouthTopY[1] = frame.mouthPoints.get(15).y;
        mouthTopX[2] = frame.mouthPoints.get(16).x;
        mouthTopY[2] = frame.mouthPoints.get(16).y;

        mouthBottomX[0] = frame.mouthPoints.get(12).x;
        mouthBottomY[0] = frame.mouthPoints.get(12).y;
        mouthBottomX[1] = frame.mouthPoints.get(18).x;
        mouthBottomY[1] = frame.mouthPoints.get(17).y;
        mouthBottomX[2] = frame.mouthPoints.get(16).x;
        mouthBottomY[2] = frame.mouthPoints.get(16).y;

        SplineInterpolator interpolator = new SplineInterpolator();
        PolynomialSplineFunction splineTop = interpolator.interpolate(mouthTopX, mouthTopY);
        PolynomialSplineFunction splineBottom = interpolator.interpolate(mouthBottomX, mouthBottomY);

        for (int i = frame.mouthPoints.get(12).x; i < frame.mouthPoints.get(16).x; i++)
        {
            int height = (int)Math.round(splineTop.value(i));
            drawCircle(image, i, height, 0);
            height = (int)Math.round(splineBottom.value(i));
            drawCircle(image, i, height, 0);
        }
    }

    private void createElipseFrame(MarvinImage image, Frame frame)
    {
        int width = frame.mouthPoints.get(6).x - frame.mouthPoints.get(0).x;
        int height = frame.mouthPoints.get(4).y - frame.mouthPoints.get(8).y;
        int centerX = renderWidth / 2;
        int centerY = renderHeight / 2;
        int leftEnd = centerX - width / 2;
        int rightEnd = centerX + width / 2;

        EllipseFunction ellipse = new EllipseFunction(centerX, centerY, width, height);

        for (int i = leftEnd; i < rightEnd; i++)
        {
            int[] heights = ellipse.value(i);
            drawCircle(image, i, heights[0], 0);
            drawCircle(image, i, heights[1], 0);
        }
    }

    private static int[] middlePoint(Point p1, Point p2)
    {
        int[] point = new int[2];

        point[0] = Math.round((p1.x + p2.x) / 2.0f);
        point[1] = Math.round((p1.y + p2.y) / 2.0f);

        return point;
    }

    private void drawCircle(MarvinImage image, int x, int y, int color)
    {
        for (int i = -2; i <= 2; i++)
        {
            for (int j = -2; j <= 2; j++)
            {
                if (Math.abs(i) != 2 || Math.abs(j) != 2)
                {
                    image.setIntColor(x + i, y + j, color);
                }
            }
        }
    }

    // Scales the recorded points to the user's width and height
    private void scaleFramesToSize()
    {
        resize();

        for (Frame i : recordedFrames)
        {
            double widthOfFace = i.faceCardinalPoints.get(2).x - i.faceCardinalPoints.get(3).x;
            double heightOfFace = i.faceCardinalPoints.get(1).y - i.faceCardinalPoints.get(0).y;

            for (Point j : i.mouthPoints)
            {
                j.x = (int)Math.floor(renderWidth * ((j.x - i.faceCardinalPoints.get(3).x) / widthOfFace));
                j.y = (int)Math.floor(renderHeight * ((j.y - i.faceCardinalPoints.get(3).y) / heightOfFace));
            }
        }
    }

    private void resize()
    {
        for (Frame i : recordedFrames)
        {
            for (Point j : i.mouthPoints)
            {
                j.x = Math.round(j.x / recordWidth * renderWidth);
                j.y = Math.round(j.y / recordHeight * renderHeight);
            }
            for (Point j : i.faceCardinalPoints)
            {
                j.x = Math.round(j.x / recordWidth * renderWidth);
                j.y = Math.round(j.y / recordHeight * renderHeight);
            }
        }
    }

    // Averages each point with the 3 frames behind it to minimize jittering
    // TODO: better stabilize frames. Either they are very jittery or can't read the lips
    private void stableizeFrames()
    {
        for (int i = 0; i < recordedFrames.size(); i++)
        {
            Frame currentFrame = recordedFrames.get(i);
            // Gets the previous index unless it is zero
            int indexPrevious = (i - 1 < 0) ? 0 : i - 1;
            int index2ndPrevious = (i - 2 < 0) ? 0 : i - 2;
            int index3rdPrevious = (i - 3 < 0) ? 0 : i - 3;

            for (int j = 0; j < currentFrame.mouthPoints.size(); j++)
            {
                Point currentPoint = currentFrame.mouthPoints.get(j);
                Point previousPoint = recordedFrames.get(indexPrevious).mouthPoints.get(j);
                Point previous2ndPoint = recordedFrames.get(index2ndPrevious).mouthPoints.get(j);
                Point previous3rdPoint = recordedFrames.get(index3rdPrevious).mouthPoints.get(j);
                currentPoint.x = Math.round((currentPoint.x + previousPoint.x + previous2ndPoint.x + previous3rdPoint.x) / 4.0f);
                currentPoint.y = Math.round((currentPoint.y + previousPoint.y) / 2.0f);
            }
        }
    }

    private void incr()
    {
        currentFrame++;
        this.updateProgress(currentFrame, totalFrames);
    }

    private void finish()
    {
        compile();

        // MergerAudioWithVideo throws an exception because it can't load a 32bit .dll on a 64bit laptop

        //MergeAudioWithVideo merger = new MergeAudioWithVideo();
        //merger.mergeFiles(config.recordAudioPath, "compiled.mov", "rendered/");

        deleteDirectory(renderDirectory);

        this.updateProgress(totalFrames, totalFrames);
    }

    private void compile()
    {
        incr();
        updateMessage("Compiling Movie...");
        try
        {
            new JpegImagesToMovie(renderWidth, renderHeight, frameRate, "rendered/compiled.mov", compiledPaths);
        }
        catch (MalformedURLException e)
        {
            // TODO: Handle exception by popup message to user in export controller
        }
        catch (MovieCompileException e)
        {
            // TODO: Handle
        }
    }

    private static void deleteDirectory(File directory)
    {
        File[] files = directory.listFiles();

        if (files != null)
        {
            for (File i : files)
            {
                if (i.isDirectory())
                {
                    deleteDirectory(i);
                }
                else
                {
                    i.delete();
                }
            }
        }

        directory.delete();
    }
}
