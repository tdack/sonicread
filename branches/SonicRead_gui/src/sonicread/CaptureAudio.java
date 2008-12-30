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
public class CaptureAudio {
  private AudioFormat audioFormat;
  private TargetDataLine targetDataLine;
  private int[] frames;
  private int frameWriteIx;
  private byte[] buffer;
  private int bufferWriteIx;
  private int bufferReadIx;

  /** 
   * Constructor
   * @param numFrames    How many samples will be taken to calculate the audio 
   * input level value.
   */
  public CaptureAudio(int numFrames){
    try{
      buffer = new byte[8*1024];
      frames = new int[numFrames];
      bufferWriteIx = bufferReadIx = frameWriteIx = 0;
      audioFormat = getAudioFormat();
      DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, audioFormat);
      targetDataLine = (TargetDataLine)AudioSystem.getLine(dataLineInfo);
      targetDataLine.open(audioFormat);

    }catch (Exception e) {
      e.printStackTrace();
      System.exit(0);
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

  public void Start() {
    targetDataLine.start();
  }
  
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
  
  /**
   * Get a value from the audio input buffer
   * @return audio input sample
   */
  public int GetSample() {
    int val = 0;
    if(bufferReadIx < bufferWriteIx)
    {
      val = (int)((int)buffer[bufferReadIx + 1] << 8);
      val += (int)buffer[bufferReadIx];
      bufferReadIx += 2;
      
      /* add audio sample to frame buffer */
      frames[frameWriteIx++] = bufferReadIx;
      frameWriteIx %= frames.length;
    }
    return(val);
  }
  
  /** Savely convert double variable to an int in deciBells */
  private int ToDB(double val) {
      val = Math.abs(val);
      if(val < 1)
          return -100;
      val /= 32767;
      val = 20 * Math.log10(val);
      val = Math.round(val);
      return (int)val;
  }
  
  /**
   * Get the audio input level value in decibells
   * @return audio input level in dB (returns a value from -100 to 0)
   */
  public int GetLevel() {
    double rms = 0;
    for(int ii = 0; ii < frames.length; ii++)
    {
        rms += frames[ii]*frames[ii];
    }
    rms = Math.sqrt(rms / frames.length);
    return ToDB(rms);
  }
}
