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
 * Remco den Breeje, <stacium@gmail.com>
 */

package sonicread;

import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 *
 * @author remco
 */
public class ImportAudio extends Audio {
    
    private AudioInputStream stream;

    public ImportAudio(File file) throws Exception {
        super();
        
        // try to read the audio file
        if(!file.exists()) {
            throw new Exception("Wave file could not be found");
        }
        try {
            stream = AudioSystem.getAudioInputStream(file);
        } catch (UnsupportedAudioFileException e) {
            throw new Exception(e.getMessage());
        } catch (IOException e) {
            throw new Exception(e.getMessage());
        }
        
        // check format
        AudioFormat format = stream.getFormat();
        System.out.println(format.toString());
        if( format.getSampleRate() != 44100.0 )
            throw new Exception("Unsupported format; Signal must be mono");
        if( format.getChannels() != 1 )
            throw new Exception("Unsupported format; Signal must be mono");
        if( format.getSampleSizeInBits() != 16)
            throw new Exception("Unsupported format; Sample size must be 16 bits");
        if( format.getEncoding() != Encoding.PCM_SIGNED)
            throw new Exception("Unsupported format; Data must be PCM signed");
        if( format.isBigEndian() == true )
            throw new Exception("Unsupported format; Data must be stored in little-endian byte order");
    }

    public void Start()  {
        
    }

    public void Stop() {
        
    }

    public void Close() {
        try {
            stream.close();
        } catch (IOException ex) {
            // suppress
        }
    }

   /**
   * Try to read audio from audio file, and store it in a class buffer
   * @return true if bytes are fetched, false if not.
   */
    @Override
    public boolean ReadSample() {
        int tmp;
        if(bufferWriteIx > 0 && (bufferReadIx < bufferWriteIx))
            return true;
        try {
            bufferWriteIx = stream.read(buffer, 0, buffer.length);
        } catch (IOException ex) {
            // suppress for now
        }
        bufferReadIx = 0;
        return(bufferWriteIx > 0);
    }

}
