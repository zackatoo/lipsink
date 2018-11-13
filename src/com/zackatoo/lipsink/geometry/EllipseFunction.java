package com.zackatoo.lipsink.geometry;

public class EllipseFunction
{
    private int centerX;
    private int centerY;
    private int width;
    private int height;

    public EllipseFunction(int centerX, int centerY, int width, int height)
    {
        this.centerX = centerX;
        this.centerY = centerY;
        this.width = width;
        this.height = height;
    }

    // Returns an array of integers of length two which are the y coordinates
    public int[] value(int x)
    {
        double a = Math.pow(width / 2.0f, 2);
        double b = Math.pow(height / 2.0f, 2);
        double displacedX = Math.pow(x - centerX, 2);
        int[] y = new int[2];

        y[0] = (int)Math.round(Math.sqrt(b - b * displacedX / a));
        y[1] = centerY - y[0];
        y[0] = centerY + y[0];

        return y;
    }
}
