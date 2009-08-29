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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Vector;

/**
 * Infinite Impulse Response Filter implementation in Java
 */
public class IIR {

    private FIR b, a;
    private double output;

    public IIR(Double[] numd, Double[] dend) {
        b = new FIR(numd);
        a = new FIR(dend);
    }

    public double filter(double val) {
        b.add(val);
        a.filter();
        b.filter();
        output = b.output() - a.output();
        a.add(output);
        return output;
    }

    public double output() {
        return output;
    }

    /**
    * Finite Impulse Response Filter implementation in Java
    */
    class FIR {

        Vector<Double> coefficients;
        LinkedList<Double> state;
        private double output;

        public FIR(Double[] coef) {
            state = new LinkedList<Double>();
            coefficients = new Vector(Arrays.asList(coef));
        }

        public void add(double val) {
            state.addFirst(val);
            while (state.size() > coefficients.size()) {
                state.removeLast();
            }
        }

        public double filter() {
            output = 0.0;
            for (int ii = 0; ii < min(state.size(), coefficients.size()); ii++) {
                output += state.get(ii) * coefficients.get(ii);
            }
            return output;
        }

        public double output() {
            return output;
        }

        private int min(int a, int b) {
            return (a < b) ? a : b;
        }
    }
}
