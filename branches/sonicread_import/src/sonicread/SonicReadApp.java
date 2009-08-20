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

import java.io.File;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.MemoryHandler;
import org.jdesktop.application.Application;
import org.jdesktop.application.ApplicationContext;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.Task;


/**
 * The main class of the application.
 */
public class SonicReadApp extends SingleFrameApplication {
    
    static private SonicReadView srv;
    public SROptions options;
    private static final String FILENAME_OPTIONS = "sr-options.xml";
    static Vector<String> events;
    
    // Getter/Setter functions
    public SROptions getOptions() {
        return options;
    }

    public static void log(String event) {
        String s;
        Date date = new Date();
        Format formatter;
        formatter = new SimpleDateFormat("k:mm:ss:");
        s = formatter.format(date);

        String string = String.format("T%s: %s", s, event);
        events.add(string);
        System.out.println(string);
        SonicReadView.updateEventsBox(events);
    }

    /**
     * At startup create and show the main frame of the application.
     */
    @Override protected void startup() {
        // would be nice to put this in a SRDOcument class..
        events = new Vector<String>();
        getContext().getLocalStorage().setDirectory(new File(System.getProperty ("user.home") + "/.sonicread"));
        loadOptions();

        // Create new SonicReadView instance and show it
        srv = new SonicReadView(this);
        show(srv);
        log("SonicRead started\n");
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
     * Shuts down the SonicRead application and persists the state.
     */
    @Override
    protected void shutdown () {
        storeOptions ();
        super.shutdown ();
    }

    /**
     * Main method launching the application.
     */
    public static void main(String[] args) {
        launch(SonicReadApp.class, args);
    }

    /** {@inheritDoc} */
    public void loadOptions () {
        try {
            options = (SROptions) getContext().getLocalStorage().load(FILENAME_OPTIONS);
        }
        catch (Exception e) {
            System.out.format("Failed to load application options from '" + FILENAME_OPTIONS + "', using default values (%s)\n", e.toString());
            e.printStackTrace();
        }

        // use default options at first start or on load errors
        if (options == null) {
            options = SROptions.createDefaultInstance ();
        }
    }

    /** {@inheritDoc} */
    public void storeOptions () {
        try  {
            getContext().getLocalStorage().save(options, FILENAME_OPTIONS);
            System.out.print(options.getPreviousImportDirectory());
        }
        catch (Exception e) {
            System.out.format("Failed to write application options to '" + FILENAME_OPTIONS + "' (%s)\n", e.toString());
            e.printStackTrace();
        }
    }
    
    
   /**
     * A Task that tries to fetch SonicLink data
     */
    static class SonicListenTask extends Task<CreateHsr, Void> {
        
        private File importWavFile;

        /**
         * Constructor
         *
         * @param file the file to load from.
         */
        SonicListenTask(Application application, File file) {
            super(application);
            this.importWavFile = file;
        }
        
        /**
         * Are we importing data?
         * @return True if a wav file is being read. False if not.
         */
        private boolean ImportingWav() {
            return this.importWavFile != null;
        }

        /**
         * Store message in events log and display in status bar
         * @param message to be displayed
         */
        private void setLogStatusMessage(java.lang.String message)
        {
            log(message);
            setMessage(message);
        }

        /**
         * Load the SonicLink data into a CreateHsr class and return it.  
         * {@code progress} property is updated as the data is loaded.
         * <p>
         * If this task is cancelled before the SonicLink data has been
         * successfully read, null is returned.
         *
         * @return the SonicLink data as a CreateHsr class or null
         */
        @Override
        protected CreateHsr doInBackground() throws Exception {
            int val = -1;
            int sampleCount = 0;
            
            CreateHsr hsr = new CreateHsr();
            SonicLink sonic = new SonicLink();
            Audio audio;
            if(ImportingWav()) {
                audio = new ImportAudio(this.importWavFile);
            }
            else {
                audio = new CaptureAudio();
            }
            
            audio.Start();
            log(String.format("Started %s", ImportingWav() ? "importing wav file": "listening"));
            setLogStatusMessage("Waiting for start byte");
            while(audio.ReadSample())
            {                
                try {
                    val = sonic.decode(audio.GetSample());
                }
                catch (Exception e) {
                    log(e.getMessage());
                    setMessage(e.getMessage());
                    // only restart when sampling from audio card
                    if(ImportingWav()) {
                        break;
                    }
                    sonic.restart();
                }
                
                /* Got byte? */
                if(val >= 0)
                {
                    try {
                        hsr.AddData(val);
                    }
                    catch (Exception e) {
                        setLogStatusMessage(String.format("Error while checking data: %s", e.getMessage()));
                        break; // stop
                    }
                }

                /* Display user information */
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

                if(hsr.IsDone() || isCancelled())
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
            log(String.format("Stopped %s", ImportingWav() ? "importing wav file": "listening"));


            if (!isCancelled() && hsr.IsDone()) {
                return hsr;
            } else {
                return null;
            }
        }
    }
    
    static class SonicSaveFileTask extends Task<Void, Void> {
        
        private final File file;
        private final CreateHsr hsr;

        /**
         * Construct a SonicSaveFileTask.
         *
         * @param file The file to save to
         * @param text The Hsr class that contains the data that will be written
         */
        SonicSaveFileTask(Application app, File file, CreateHsr hsr) {
            super(app);
            this.file = file;
            this.hsr = hsr;
        }
        
        public File getFile() {
            return file;
        }
        
        
        @Override
        protected Void doInBackground() throws Exception {
            if(hsr == null)
                throw new Exception("Hsr not set yet");
            
            hsr.WriteHsr(file);
            return null;
        }
    }
}
