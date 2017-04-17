package murphd40.ep4j.test;

import murphd40.ep4j.Equation;
import org.junit.Test;

/**
 * Created by David on 17/04/2017.
 */
public class EquationTest {

    @Test
    public void equationTest() {
        Equation equation = Equation.parse("1 + 2*4.7  + .7");
        System.out.println(equation.evaluate());
    }

}
