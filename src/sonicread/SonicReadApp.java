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
import java.util.logging.Level;
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

    // Getter/Setter functions
    public SROptions getOptions() {
        return options;
    }

    /**
     * At startup create and show the main frame of the application.
     */
    @Override protected void startup() {
        // would be nice to put this in a SRDOcument class..
        getContext().getLocalStorage().setDirectory(new File(System.getProperty ("user.home") + "/.sonicread"));
        loadOptions();
        // Create new SonicReadView instance and show it
        srv = new SonicReadView(this, options);
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
            //options = (SROptions) context.getSAFContext ().getLocalStorage ().load (FILENAME_OPTIONS);
        }
        catch (Exception e) {
            //LOGGER.log(Level.WARNING, "Failed to load application options from '" + FILENAME_OPTIONS + "', using default values ...", e);
            System.out.format("Failed to load application options from '" + FILENAME_OPTIONS + "', using default values (%s)\n", e.toString());
            e.printStackTrace();
        }

        // use default options at first start or on load errors
        if (options == null) {
            System.out.format("Default!\n");
            options = SROptions.createDefaultInstance ();
        }
    }

    /** {@inheritDoc} */
    public void storeOptions () {
        try  {
            getContext().getLocalStorage().save(options, FILENAME_OPTIONS);
            //context.getSAFContext ().getLocalStorage ().save (options, FILENAME_OPTIONS);
            System.out.print(options.getPreviousImportDirectory());
        }
        catch (Exception e) {
            //LOGGER.log(Level.SEVERE, "Failed to write application options to '" +
            //    FILENAME_OPTIONS + "' ...", ioe);
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
         * Construct a LoadTextFileTask.
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
         * Display message in status bar
         * @param message to be displayed
         */
        private void setStatusMessage(java.lang.String message)
        {
            //System.out.format(">>>>>>>>> %s\n", message);
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
            setStatusMessage("Waiting for start byte");
            while(audio.ReadSample())
            {                
                try {
                    val = sonic.decode(audio.GetSample());
                }
                catch (Exception e) {
                    // only restart when sampling from audio card
                    if(ImportingWav()) {
                        break;
                    }
                    sonic.restart();
                    setStatusMessage(e.getMessage());
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
                        setStatusMessage("Processing data");
                    else if(pg < 66)
                        setStatusMessage(String.format("Found a %s", hsr.GetMonitorType()));
                    else
                        setStatusMessage(String.format("Fetching %d bytes", hsr.GetNumberOfBytes()));
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
