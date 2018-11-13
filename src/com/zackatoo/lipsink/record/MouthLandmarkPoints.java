/*
 * This class inherits from an ArrayList for Point2fVectorVector. There is one per frame of recorded video
 */

package com.zackatoo.lipsink.record;

import org.bytedeco.javacpp.opencv_core.Point2f;
import org.bytedeco.javacpp.opencv_core.Point2fVector;
import org.bytedeco.javacpp.opencv_core.Point2fVectorVector;

import java.util.ArrayList;
import java.util.Arrays;

public class MouthLandmarkPoints extends ArrayList<Point2fVectorVector>
{
    // Indexes for the important points inside Point2FVector
    private static final long MOUTH_START_INDEX = 48;
    private static final long MOUTH_END_INDEX = 67;
    private static final long FACE_TOP_INDEX = 27;
    private static final long FACE_BOTTOM_INDEX = 8;
    private static final long FACE_LEFT_INDEX = 16;
    private static final long FACE_RIGHT_INDEX = 0;

    private Integer frameWidth;
    private Integer frameHeight;

    // Converts points from the Point2VectorVector class into an ArrayList of frames
    // Only converts the largest face (to remove any background faces and helps eliminate false positives)
    // Point2fVectorVector has all the face points, but the converted points are only for the mouth
    // If one frame is null where the landmark detection didn't find anything it averages the bordering frames
    public ArrayList<Frame> manufacturePoints()
    {
        ArrayList<Frame> frames = new ArrayList<>();

        for (Point2fVectorVector currentFrame : this)
        {
            if (currentFrame == null)
            {
                frames.add(null);
            }
            else
            {
                ArrayList<Frame> allFaces = getAllFacesInFrame(currentFrame);
                Frame largestFace = getLargestFace(allFaces);
                frames.add(largestFace);
            }
        }


        checkForMissingFrames(frames);

        return frames;
    }

    private ArrayList<Frame> getAllFacesInFrame(Point2fVectorVector currentFrame)
    {
        ArrayList<Frame> allFaces = new ArrayList<>();

        for (long i = 0; i < currentFrame.size(); i++)
        {
            Point2fVector vector = currentFrame.get(i);
            ArrayList<Point> mouth = getMouthFromFrame(vector);
            ArrayList<Point> faceCardinals = new ArrayList<>(Arrays.asList(
                    getPoint(vector.get(FACE_TOP_INDEX)),
                    getPoint(vector.get(FACE_BOTTOM_INDEX)),
                    getPoint(vector.get(FACE_LEFT_INDEX)),
                    getPoint(vector.get(FACE_RIGHT_INDEX))
            ));

            allFaces.add(new Frame() {{mouthPoints = mouth; faceCardinalPoints = faceCardinals;}});
        }

        return allFaces;
    }

    private ArrayList<Point> getMouthFromFrame(Point2fVector currentFace)
    {
        ArrayList<Point> mouth = new ArrayList<>();

        for (long i = MOUTH_START_INDEX; i <= MOUTH_END_INDEX; i++)
        {
            Point2f point = currentFace.get(i);
            mouth.add(getPoint(point));
        }

        return mouth;
    }

    private Point getPoint(Point2f point)
    {
        int _x = Math.round(point.x());
        int _y = Math.round(point.y());
        return new Point() {{x = _x; y = _y;}};
    }

    private Frame getLargestFace(ArrayList<Frame> allFaces)
    {
        double largest = 0;
        Frame largestFrame = null;

        for (Frame i : allFaces)
        {
            double height = i.faceCardinalPoints.get(1).y - i.faceCardinalPoints.get(0).y;
            double width = i.faceCardinalPoints.get(3).x - i.faceCardinalPoints.get(2).x;
            double length = Math.hypot(width, height);

            if (length > largest)
            {
                largest = length;
                largestFrame = i;
            }
        }

        return largestFrame;
    }

    // Fills in any missing frames with approximate points
    private void checkForMissingFrames(ArrayList<Frame> frames)
    {
        if (findNotEmptyFrame(0, frames) == frames.size())
        {
            applyDefaultFrame(frames);
        }
        else
        {
            for (int i = 0; i < frames.size(); i++)
            {
                if (frames.get(i) == null)
                {
                    int next = findNotEmptyFrame(i + 1, frames);
                    if (next == frames.size())
                    {
                        duplicateFrame(i, frames.size() - 1,i - 1, frames);
                    }
                    else
                    {
                        applyLinearApproximation(i - 1, next, frames);
                    }

                    i = next;
                }
            }
        }
    }

    // Approximates where the points should be using a linear model
    private void applyLinearApproximation(int startingIndex, int endingIndex, ArrayList<Frame> frames)
    {
        Frame startingFrame = frames.get(startingIndex);
        Frame endingFrame = frames.get(endingIndex);
        int duration = endingIndex - startingIndex;
        for (int frameIndex = startingIndex + 1; frameIndex < endingIndex - 1; frameIndex++)
        {
            Frame frame = new Frame();
            ArrayList<Point> mouthPoints = new ArrayList<>();
            ArrayList<Point> cardinalPoints = new ArrayList<>();

            for (int pointIndex = 0; pointIndex < startingFrame.mouthPoints.size(); pointIndex++)
            {
                Point startingPoint = startingFrame.mouthPoints.get(pointIndex);
                Point endingPoint = endingFrame.mouthPoints.get(pointIndex);
                mouthPoints.add(approximatePoint(startingPoint, endingPoint, frameIndex, duration));
            }

            for (int pointIndex = 0; pointIndex < startingFrame.mouthPoints.size(); pointIndex++)
            {
                Point startingPoint = startingFrame.faceCardinalPoints.get(pointIndex);
                Point endingPoint = endingFrame.faceCardinalPoints.get(pointIndex);
                mouthPoints.add(approximatePoint(startingPoint, endingPoint, frameIndex, duration));
            }

            frame.mouthPoints = mouthPoints;
            frame.faceCardinalPoints = cardinalPoints;
            frames.set(frameIndex, frame);
        }
    }

    private Point approximatePoint(Point start, Point end, int progress, int duration)
    {
        Point point = new Point();
        point.x = Math.round(progress * ((start.x - end.x) / duration) + start.x);
        point.y = Math.round(progress * ((start.y - end.y) / duration) + start.y);
        return point;
    }

    private void duplicateFrame(int startingIndex, int endingIndex, int duplicateIndex, ArrayList<Frame> frames)
    {
        for (int i = startingIndex; i <= endingIndex; i++)
        {
            frames.set(i, frames.get(duplicateIndex));
        }
    }

    // Finds the next non-empty frame or the end of the array
    private int findNotEmptyFrame(int startingIndex, ArrayList<Frame> frames)
    {
        int i = startingIndex;
        for (; i < frames.size(); i++)
        {
            if (frames.get(i) != null)
            {
                return i;
            }
        }

        return i;
    }

    private void applyDefaultFrame(ArrayList<Frame> frames)
    {
        // These default points are only ever used if the entire video has zero faces in it (in which case
        // the output should not be used but the program has to output something)
        int[] defaultMouthX = {293, 300, 307, 312, 318, 325, 333, 325, 317, 312, 306, 299, 295, 307, 312, 318, 330, 318, 312, 307};
        int[] defaultMouthY = {275, 272, 270, 271, 270, 271, 273, 279, 281, 281, 281, 279, 275, 274, 275, 274, 274, 275, 276, 275};
        int[] defaultCardinalX = {312, 312, 364, 262};
        int[] defaultCardinalY = {224, 304, 224, 227};

        Frame defaultFrame = new Frame();

        ArrayList<Point> defaultMouthPoints = new ArrayList<>();

        for (int i = 0; i < defaultMouthX.length; i++)
        {
            Point point = new Point();
            point.x = defaultMouthX[i];
            point.y = defaultMouthY[i];
            defaultMouthPoints.add(point);
        }

        defaultFrame.mouthPoints = defaultMouthPoints;
        ArrayList<Point> defaultCardinalPoints = new ArrayList<>();

        for (int i = 0; i < defaultCardinalX.length; i++)
        {
            Point point = new Point();
            point.x = defaultCardinalX[i];
            point.y = defaultCardinalY[i];
            defaultCardinalPoints.add(point);
        }

        defaultFrame.faceCardinalPoints = defaultCardinalPoints;

        for (int i = 0; i < frames.size(); i++)
        {
            frames.set(i, defaultFrame);
        }
    }

    public Integer getFrameWidth()
    {
        return frameWidth;
    }

    public void setFrameWidth(Integer frameWidth)
    {
        this.frameWidth = frameWidth;
    }

    public Integer getFrameHeight()
    {
        return frameHeight;
    }

    public void setFrameHeight(Integer frameHeight)
    {
        this.frameHeight = frameHeight;
    }

    public class Frame
    {
        // Holds all the mouth points for each frame
        public ArrayList<Point> mouthPoints = new ArrayList<>();
        // Holds the top, bottom, left and right points of the entire face in that order
        public ArrayList<Point> faceCardinalPoints = new ArrayList<>();

        public Integer width = 0;
        public Integer height = 0;

        public Frame()
        {
            width = frameWidth;
            height = frameHeight;
        }
    }

    public class Point
    {
        public int x = 0;
        public int y = 0;
    }
}