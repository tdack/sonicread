/*
 * SonicReadApp.java
 */

package sonicread;

import java.io.IOException;
import org.jdesktop.application.Application;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.Task;


/**
 * The main class of the application.
 */
public class SonicReadApp extends SingleFrameApplication {
    

    /**
     * At startup create and show the main frame of the application.
     */
    @Override protected void startup() {
        
        show(new SonicReadView(this));
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
            int val;
            CreateHsr hsr = new CreateHsr();
            SonicLink sonic = new SonicLink();
            CaptureAudio audio = new CaptureAudio();
            audio.Start();
            
            while(audio.ReadSample())
            {
                if((val = sonic.decode(audio.GetSample())) == -2)
                {
                    System.out.format("decode error\n");
                    throw new IOException("decode error");
                    //return null;
                }
                
                /* Got byte? */
                if(val >= 0)
                {
                    if(!hsr.AddData(val))
                    {
                        System.out.format("HALTED - Error while checking data\n");
                        break;
                    }
                }

                if(hsr.IsDone())
                {
                      System.out.format("\nDone.\n");
                      //hsr.WriteSrd();
                      //System.out.format("Written data to exercise.srd\n");
                      hsr.WriteHsr();
                      System.out.format("Written data to exercise.hsr\n");
                      break;
                }
            }

            
            //setProgress(Math.min(offset, fileLength), 0, fileLength);
            //setProgress(100, 0, 50);
            /*
            int fileLength = (int) file.length();
            int nChars = -1;
            // progress updates after every blockSize chars read
            int blockSize = Math.max(1024, fileLength / 100);
            int p = blockSize;
            char[] buffer = new char[32];
            StringBuilder contents = new StringBuilder();
            BufferedReader rdr = new BufferedReader(new FileReader(file));
            while (!isCancelled() && (nChars = rdr.read(buffer)) != -1) {
                contents.append(buffer, 0, nChars);
                if (contents.length() > p) {
                    p += blockSize;
                    setProgress(contents.length(), 0, fileLength);
                }
            }
             */
            
            System.out.format("boe\n");
            if (!isCancelled()) {
                return "boe";
            } else {
                return null;
            }
        }
    }
}
