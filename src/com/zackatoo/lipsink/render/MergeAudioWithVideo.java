/* Does not work with 64 bit machines so instead the audio and video are just saved seperately
 * 
 * Adapted from https://stackoverflow.com/questions/14013874/combine-audio-with-videowithout-ffmpeg-java
 */

package com.zackatoo.lipsink.render;



import java.io.File;
import java.io.IOException;
import javax.media.CannotRealizeException;
import javax.media.ConfigureCompleteEvent;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.DataSink;
import javax.media.EndOfMediaEvent;
import javax.media.Format;
import javax.media.IncompatibleSourceException;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.NoDataSinkException;
import javax.media.NoDataSourceException;
import javax.media.NoProcessorException;
import javax.media.PrefetchCompleteEvent;
import javax.media.Processor;
import javax.media.ProcessorModel;
import javax.media.RealizeCompleteEvent;
import javax.media.ResourceUnavailableEvent;
import javax.media.control.TrackControl;
import javax.media.datasink.DataSinkErrorEvent;
import javax.media.datasink.DataSinkEvent;
import javax.media.datasink.DataSinkListener;
import javax.media.datasink.EndOfStreamEvent;
import javax.media.format.VideoFormat;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;
import javax.media.protocol.FileTypeDescriptor;

public class MergeAudioWithVideo implements ControllerListener, DataSinkListener
{
    public void mergeFiles(String pathToAudioFile, String pathToVideoFile, String exportDirectory)
    {
        File audioFile = new File(pathToAudioFile);
        File videoFile = new File(pathToVideoFile);
        File directory = new File(exportDirectory);

        try {
            DataSource videoDataSource = javax.media.Manager.createDataSource(videoFile.toURI().toURL()); //your video file
            DataSource audioDataSource = javax.media.Manager.createDataSource(audioFile.toURI().toURL()); // your audio file
            DataSource mixedDataSource = null; // data source to combine video with audio
            DataSource arrayDataSource[] = new DataSource[2]; //data source array
            DataSource outputDataSource = null; // file to output

            DataSink outputDataSink = null; // datasink for output file

            MediaLocator videoLocator = new MediaLocator(videoFile.toURI().toURL()); //media locator for video
            MediaLocator audioLocator = new MediaLocator(audioFile.toURI().toURL()); //media locator for audio

            FileTypeDescriptor outputType = new FileTypeDescriptor(FileTypeDescriptor.QUICKTIME); //output video format type

            Format outputFormat[] = new Format[2]; //format array
            VideoFormat videoFormat = new VideoFormat(VideoFormat.JPEG); // output video codec MPEG does not work on windows
            javax.media.format.AudioFormat audioMediaFormat = new javax.media.format.AudioFormat(
                    javax.media.format.AudioFormat.LINEAR, 44100, 16, 1); //audio format


            outputFormat[0] = videoFormat;
            outputFormat[1] = audioMediaFormat;

            //create processors for each file
            Processor videoProcessor = Manager.createProcessor(videoDataSource);
            Processor audioProcessor = Manager.createProcessor(audioDataSource);
            Processor processor = null;

            //start video and audio processors
            videoProcessor.realize();
            audioProcessor.realize();
            //wait till they are realized
            while(videoProcessor.getState() != 300 && audioProcessor.getState() != 300) {
                Thread.sleep(100);
            }
            //get processors dataoutputs to merge
            arrayDataSource[0] = videoProcessor.getDataOutput();
            arrayDataSource[1] = audioProcessor.getDataOutput();

            videoProcessor.start();
            audioProcessor.start();

            //create merging data source
            mixedDataSource = javax.media.Manager.createMergingDataSource(arrayDataSource);
            mixedDataSource.connect();
            mixedDataSource.start();
            //init final processor to create merged file
            ProcessorModel processorModel = new ProcessorModel(mixedDataSource, outputFormat, outputType);
            processor = Manager.createRealizedProcessor(processorModel);
            processor.addControllerListener(this);
            processor.configure();
            //wait till configured
            while(processor.getState() < 180) {
                Thread.sleep(20);
            }

            processor.setContentDescriptor(new ContentDescriptor(FileTypeDescriptor.QUICKTIME));

            TrackControl tcs[] = processor.getTrackControls();
            Format f[] = tcs[0].getSupportedFormats();

            tcs[0].setFormat(f[0]);

            processor.realize();
            //wait till realized
            while(processor.getState() < 300) {
                Thread.sleep(20);
            }
            //create merged file and start writing media to it
            outputDataSource = processor.getDataOutput();
            MediaLocator outputLocator = new MediaLocator("file:/"+directory.getAbsolutePath()+"/yourmovfile.mov");
            outputDataSink = Manager.createDataSink(outputDataSource, outputLocator);
            outputDataSink.open();
            outputDataSink.addDataSinkListener(this);
            outputDataSink.start();
            processor.start();

            while(processor.getState() < 500) {
                Thread.sleep(100);
            }
            //wait until writing is done
            waitForFileDone();
            //dispose processor and datasink
            outputDataSink.stop();
            processor.stop();

            outputDataSink.close();
            processor.close();

        } catch (NoDataSourceException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IncompatibleSourceException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoDataSinkException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoProcessorException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (CannotRealizeException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    Object waitFileSync = new Object();
    boolean fileDone = false;
    boolean fileSuccess = true;
    Object waitSync = new Object();
    boolean stateTransitionOK = true;

    boolean waitForFileDone() {
        synchronized (waitFileSync) {
            try {
                while (!fileDone)
                    waitFileSync.wait();
            } catch (Exception e) {
            }
        }
        return fileSuccess;
    }


    public void dataSinkUpdate(DataSinkEvent evt) {

        if (evt instanceof EndOfStreamEvent) {
            synchronized (waitFileSync) {
                fileDone = true;
                waitFileSync.notifyAll();
            }
        } else if (evt instanceof DataSinkErrorEvent) {
            synchronized (waitFileSync) {
                fileDone = true;
                fileSuccess = false;
                waitFileSync.notifyAll();
            }
        }
    }

    @Override
    public void controllerUpdate(ControllerEvent evt) {
        if (evt instanceof ConfigureCompleteEvent
                || evt instanceof RealizeCompleteEvent
                || evt instanceof PrefetchCompleteEvent) {
            synchronized (waitSync) {
                stateTransitionOK = true;
                waitSync.notifyAll();
            }
        } else if (evt instanceof ResourceUnavailableEvent) {
            synchronized (waitSync) {
                stateTransitionOK = false;
                waitSync.notifyAll();
            }
        } else if (evt instanceof EndOfMediaEvent) {
            evt.getSourceController().stop();
            evt.getSourceController().close();
        }

    }
}

/*
import java.io.*;
import javax.media.*;
import javax.media.control.TrackControl;
import javax.media.datasink.*;
import javax.media.format.*;
import javax.media.protocol.*;
import java.net.*;

public class MergeAudioWithVideo{



    //merge the sound and video files
    public static void merging(String audioFileName, String videoFileName){


        //Declare and initialize StateHelper objects: sha,shv, and shm
        //sha for audio processor, shvfor audio process, and shm for merge processor
        StateHelper sha=null;
        StateHelper shv=null;
        StateHelper shm=null;

        //Declare and initialize processor objects for audio, video, and merged data
        Processor audioProcessor=null;
        Processor videoProcessor=null;
        Processor mergeProcessor=null;

        //create MediaLocator objects for audio and video files
        MediaLocator audioLocator=null;
        MediaLocator videoLocator=null;
        MediaLocator outLocator=null;
        try{
            File audioFile=new File(audioFileName);
            audioLocator=new MediaLocator(audioFile.toURI().toURL());

            File videoFile=new File(videoFileName);
            videoLocator=new MediaLocator(videoFile.toURI().toURL());

            //Create MediaLocator for merged output file
            File outFile=new File(System.currentTimeMillis()+"mergedvideo.mov");
            outLocator=new MediaLocator(outFile.toURI().toURL());
        }catch(MalformedURLException me){System.exit(-1);}

        //create datasources
        DataSource audioDataSource=null;
        DataSource videoDataSource=null;
        DataSource mergedDataSource=null;
        DataSource arrayDataSource[]=null;
        try{
            audioDataSource = Manager.createDataSource(audioLocator); // your audio file
            videoDataSource = Manager.createDataSource(videoLocator); //your video file
            mergedDataSource = null; // data source to combine video with audio
            arrayDataSource= new DataSource[2]; //data source array
        }catch(IOException ie){System.exit(-1);}
        catch(NoDataSourceException ie){System.exit(-1);}
        //format array for input audio and video
        Format[] formats=new Format[2];
        formats[0]=new AudioFormat(AudioFormat.IMA4_MS); //create audio format object
        formats[1]=new VideoFormat(VideoFormat.JPEG); //create video format object

        //create media file content type object
        FileTypeDescriptor outftd=new FileTypeDescriptor(FileTypeDescriptor.QUICKTIME);

        //create processor objects for video and audio
        try{
            videoProcessor = Manager.createProcessor(videoDataSource);
            shv=new StateHelper(videoProcessor);
            audioProcessor = Manager.createProcessor(audioDataSource);
            sha=new StateHelper(audioProcessor);
        }catch(IOException ie){System.exit(-1);}
        catch(NoProcessorException ne){System.exit(-1);}

        //Configure processors
        if (!shv.configure(10000))
            System.exit(-1);
        if (!sha.configure(10000))
            System.exit(-1);
        //Realize processors

        if (!shv.realize(10000))
            System.exit(-1);
        if (!sha.realize(10000))
            System.exit(-1);

        //return data sources from processors so they can be merged
        arrayDataSource[0]=audioProcessor.getDataOutput();
        arrayDataSource[1]=videoProcessor.getDataOutput();

        //start the processors
        videoProcessor.start();
        audioProcessor.start();

        //create merged data source, connect, and start it
        try{
            mergedDataSource=Manager.createMergingDataSource(arrayDataSource);
            mergedDataSource.connect();
            mergedDataSource.start();
        }catch(IOException ie){System.exit(-1);}
        catch(IncompatibleSourceException id){System.exit(-1);}
        //processor for merged output
        try{
            mergeProcessor=Manager.createRealizedProcessor(new     ProcessorModel(mergedDataSource,formats,outftd));
            shm=new StateHelper(mergeProcessor);
        }catch(IOException ie){System.exit(-1);}
        catch(NoProcessorException ie){System.exit(-1);}
        catch(CannotRealizeException ie){System.exit(-1);}
        //set output file content type
        mergeProcessor.setContentDescriptor(new ContentDescriptor(FileTypeDescriptor.QUICKTIME));
        //query supported formats
        TrackControl tcs[] =mergeProcessor.getTrackControls();
        Format f[] = tcs[0].getSupportedFormats();
        if (f == null || f.length <= 0)
            System.exit(100);
        //set track format
        tcs[0].setFormat(f[0]);

        //get datasource from the mergeProcessor so it is ready to write to a file by DataSink filewriter
        DataSource source =mergeProcessor.getDataOutput();
        //create DataSink filewrite for writing
        DataSink filewriter = null;
        try {
            filewriter = Manager.createDataSink(source, outLocator);
            filewriter.open();
        } catch (NoDataSinkException e) {
            System.exit(100);
        } catch (IOException e) {
            System.exit(100);
        } catch (SecurityException e) {
            System.exit(100);
        }

        // now start the filewriter and mergeProcessor
        try {
            mergeProcessor.start();
            filewriter.start();
        } catch (IOException e) {
            System.exit(-1);
        }
        // wait 2 seconds for end of media stream
        shm.waitToEndOfMedia(2000);
        shm.close();
        filewriter.close();


    }


}

//The StateHelper class help you determine the states of the processors
class StateHelper implements ControllerListener {
    Processor p = null;
    boolean configured = false;
    boolean realized = false;
    boolean prefetched = false;
    boolean eom = false;
    boolean failed = false;
    boolean closed = false;
    public StateHelper(Processor pr) {
        p= pr;
        p.addControllerListener(this);
    }

    public boolean configure(int timeOutMillis) {
        long startTime = System.currentTimeMillis();
        synchronized (this) {
            p.configure();
            while (!configured && !failed) {
                try {
                    wait(timeOutMillis);
                } catch (InterruptedException ie) {}
                if (System.currentTimeMillis() - startTime > timeOutMillis)
                    break;
            }

        }
        return configured;
    }
    public boolean realize(int timeOutMillis) {
        long startTime = System.currentTimeMillis();
        synchronized (this) {
            p.realize();
            while (!realized && !failed) {
                try {
                    wait(timeOutMillis);
                } catch (InterruptedException ie) {}
                if (System.currentTimeMillis() - startTime > timeOutMillis)
                    break;
            }
        }
        return realized;
    }

    public boolean prefetch(int timeOutMillis) {
        long startTime = System.currentTimeMillis();
        synchronized (this) {
            p.prefetch();
            while (!prefetched && !failed) {
                try {
                    wait(timeOutMillis);
                } catch (InterruptedException ie) {}
                if (System.currentTimeMillis() - startTime > timeOutMillis)
                    break;
            }
        }
        return prefetched && !failed;
    }

    public boolean waitToEndOfMedia(int timeOutMillis) {
        long startTime = System.currentTimeMillis();
        eom = false;
        synchronized (this) {
            while (!eom && !failed) {
                try {
                    wait(timeOutMillis);
                } catch (InterruptedException ie){}
                if (System.currentTimeMillis() - startTime > timeOutMillis)
                    break;
            }
        }
        return eom && !failed;
    }

    public void close() {
        synchronized (this) {
            p.close();
            while (!closed) {
                try {
                    wait(100);
                } catch (InterruptedException ie) {}
            }

        }
        p.removeControllerListener(this);
    }

    public synchronized void controllerUpdate(ControllerEvent ce) {
        if (ce instanceof RealizeCompleteEvent) {
            realized = true;
        } else if (ce instanceof ConfigureCompleteEvent) {
            configured = true;
        } else if (ce instanceof PrefetchCompleteEvent) {
            prefetched = true;
        } else if (ce instanceof EndOfMediaEvent) {
            eom = true;
        } else if (ce instanceof ControllerErrorEvent) {
            failed = true;
        } else if (ce instanceof ControllerClosedEvent) {
            closed = true;
        } else {
            return;
        }
        notifyAll();
    }

}*/
