/*
 * SonicReadApp.java
 */

package sonicread;

import org.jdesktop.application.Application;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.Task;


/**
 * The main class of the application.
 */
public class SonicReadApp extends SingleFrameApplication {
    
    static private SonicReadView srv;    

    /**
     * At startup create and show the main frame of the application.
     */
    @Override protected void startup() {
        srv = new SonicReadView(this);
        show(srv);
        
        //show(new SonicReadView(this));
    }

    /**
     * This method is to initialize the specified window by injecting resources.
     * Windows shown in our application come fully initialized from the GUI
     * builder, so this additional configuration is not needed.
     */
    @Override protected void configureWindow(java.awt.Window root) {
    }

    /**
     * A convenient static getter for the application instance.
     * @return the instance of SonicReadApp
     */
    public static SonicReadApp getApplication() {
        return Application.getInstance(SonicReadApp.class);
    }

    /**
     * Main method launching the application.
     */
    public static void main(String[] args) {
        System.out.format("main\n");
        launch(SonicReadApp.class, args);
    }
    
    
   /**
     * A Task that loads the contents of a file into a String.
     */
    //static class SonicListenTask extends Task<String, Void> {
    static class SonicListenTask extends Task<String, Void> {

        //private final File file;

        /**
         * Construct a LoadTextFileTask.
         *
         * @param file the file to load from.
         */
        SonicListenTask(Application application) { //, File file) {
            super(application);
            //this.file = file;
        }

        /**
         * Return the file being loaded.
         *
         * @return the value of the read-only file property.
         */
        //public final File getFile() {
            //return file;
        //}

        /**
         * Load the file into a String and return it.  The
         * {@code progress} property is updated as the file is loaded.
         * <p>
         * If this task is cancelled before the entire file has been
         * read, null is returned.
         *
         * @return the contents of the {code file} as a String or null
         */
        @Override
        protected String doInBackground() throws Exception {
            int val = -1;
            short tmp;
            CreateHsr hsr = new CreateHsr();
            SonicLink sonic = new SonicLink();
            CaptureAudio audio = new CaptureAudio();
            
            //setMessage("Trying to capture audio data");
            audio.Start();
            
            setMessage("Waiting for start byte");
            while(audio.ReadSample())
            {
                tmp = audio.GetSample();
                srv.setDbLevel(60 + (int)(20*Math.log10( Math.max(1, Math.abs((double)tmp)) / (double)32767) + 0.5));
                
                try {
                    val = sonic.decode(tmp);
                }
                catch (Exception e) {
                    sonic.restart();
                    setMessage(e.getMessage());
                }
                
                /* Got byte? */
                if(val >= 0)
                {
                    try {
                        hsr.AddData(val);
                    }
                    catch (Exception e) {
                        audio.Stop();
                        audio.Close();
                        throw new Exception(String.format("Error while checking data: %s", e.getMessage()));
                    }
                }
                
                if(hsr.IsStarted())
                {
                    int pg = hsr.GetProgress();
                    if(pg < 33)
                        setMessage("Processing data");
                    else if(pg < 66)
                        setMessage(String.format("Found a %s", hsr.GetMonitorType()));
                    else
                        setMessage(String.format("Fetching %d bytes", hsr.GetNumberOfBytes()));
                    setProgress(pg);
                }

                if(hsr.IsDone())
                {
                      System.out.format("\nDone.\n");
                      //hsr.WriteHsr();
                      //System.out.format("Written data to exercise.hsr\n");
                      break;
                }
                
                if(isCancelled())
                {
                    break;
                }
            }

            // if we get here, we're done
            srv.setDbLevel(0);
            audio.Stop();
            audio.Close();
            
            if (!isCancelled()) {
                return "string";
            } else {
                return "ok";
                //return null;
                //throw new Exception("Cancelled");
            }
        }
    }
}
