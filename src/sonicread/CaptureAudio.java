/*
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, 
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Remco den Breeje, <stacium@gmail.com>
 */

package sonicread;

import javax.sound.sampled.*;

/**
 * Helper class that enables SonicRead to read data from the audio API 
 * @author Remco den Breeje
 */
public class CaptureAudio extends Audio {
  private AudioFormat audioFormat;
  private TargetDataLine targetDataLine;

  /** 
   * Constructor
   * @param numFrames    How many samples will be taken to calculate the audio 
   * input level value.
   */
  public CaptureAudio() throws Exception {
    super();
    try{
      audioFormat = getAudioFormat();
      DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, audioFormat);
      targetDataLine = (TargetDataLine)AudioSystem.getLine(dataLineInfo);
      targetDataLine.open(audioFormat);
              
    }catch (Exception e) {
      throw new Exception("Error while accessing sound device.");
    }
  }

  private AudioFormat getAudioFormat(){
    float sampleRate = 44100.0f;
    int sampleSizeInBits = 16;
    int channels = 1;
    boolean signed = true;
    boolean bigEndian = false;
    return new AudioFormat(sampleRate,
                           sampleSizeInBits,
                           channels,
                           signed,
                           bigEndian);
  }

  @Override
  public void Start() {
    clippingSamples = 0;
    targetDataLine.start();
  }
  
  @Override
  public void Stop() {
    targetDataLine.stop();
  }
  
  public void Close() {
    targetDataLine.close();
  }

  /**
   * Try to read audio from input buffer, and store it in a class buffer
   * @return true if bytes are fetched, false if not.
   */
  public boolean ReadSample() {
    int tmp;
    if(bufferWriteIx > 0 && (bufferReadIx < bufferWriteIx))
      return true;
    bufferWriteIx = targetDataLine.read(buffer, 0, buffer.length);
    bufferReadIx = 0;
    return(bufferWriteIx > 0);
  }

}
