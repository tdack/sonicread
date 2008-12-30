/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * This code is based on the work of Tomás Oliveira e Silva
 * http://www.ieeta.pt/~tos/software/polar_s410.html
 * 
 * Remco den Breeje, <stacium@gmail.com>
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
        launch(SonicReadApp.class, args);
    }
    
    
   /**
     * A Task that loads the contents of a file into a String.
     */
    static class SonicListenTask extends Task<String, Void> {

        /**
         * Construct a LoadTextFileTask.
         *
         * @param file the file to load from.
         */
        SonicListenTask(Application application) {
            super(application);
        }

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
            int sampleCount = 0;
            CreateHsr hsr = new CreateHsr();
            SonicLink sonic = new SonicLink();
            CaptureAudio audio = new CaptureAudio(1000);
            
            audio.Start();
            
            setMessage("Waiting for start byte");
            while(audio.ReadSample())
            {                
                try {
                    val = sonic.decode(audio.GetSample());
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
                      break;
                }
                
                if(isCancelled())
                {
                    break;
                }
                
                /* show information */
                if((sampleCount++ % 1000) == 0)
                {
                    /* db meter from -55 dB to -5 dB */
                    srv.setDbLevel(110 + 2*Math.max(-55, Math.min(-5, audio.GetLevel())));
                    srv.setClipped(audio.SignalClipped());
                }
            }

            // if we get here, we're done
            srv.setDbLevel(0);
            audio.Stop();
            audio.Close();
            
            if (!isCancelled()) {
                return "string";
            } else {
                return null;
            }
        }
    }
}
