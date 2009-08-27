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

/**
 * Store options of SonicRead
 * 
 * @author Remco den Breeje, <stacium@gmail.com>
 */
public class SROptions {

    protected String previousExerciseDirectory;
    protected String previousImportDirectory;

    /**
     * Get the value of previousImportDirectory
     *
     * @return the value of previousImportDirectory
     */
    public String getPreviousImportDirectory() {
        return previousImportDirectory;
    }

    /**
     * Set the value of previousImportDirectory
     *
     * @param previousImportDirectory new value of previousImportDirectory
     */
    public void setPreviousImportDirectory(String previousImportDirectory) {
        this.previousImportDirectory = previousImportDirectory;
    }

    /**
     * Get the value of previousExerciseDirectory
     *
     * @return the value of previousExerciseDirectory
     */
    public String getPreviousExerciseDirectory() {
        return previousExerciseDirectory;
    }

    /**
     * Set the value of previousExerciseDirectory
     *
     * @param previousExerciseDirectory new value of previousExerciseDirectory
     */
    public void setPreviousExerciseDirectory(String previousExerciseDirectory) {
        this.previousExerciseDirectory = previousExerciseDirectory;
    }

    /**
     * Creates an instance of SROptions filled with default values.
     * @return the instance of SROptions
     */
    public static SROptions createDefaultInstance() {
        SROptions options = new SROptions ();
        options.previousExerciseDirectory = null;
        options.previousImportDirectory = null;
        return options;
    }
}
