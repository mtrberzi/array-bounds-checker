package tests;

import java.io.File;
import java.util.Collection;

import org.checkerframework.framework.test.ParameterizedCheckerTest;
import org.junit.runners.Parameterized.Parameters;

public class ArraySafetyTest extends ParameterizedCheckerTest {

    public ArraySafetyTest(File testFile) {
	super(testFile,
	      org.checkerframework.checker.arraysafety.ArraySafetyChecker.class,
	      "arraysafety",
	      "-Anomsgtext");
    }
    
    @Parameters
    public static Collection<Object[]> data() {
	return testFiles("arraysafety"); // TODO can we do the all-systems test from outside the checker framework directory? we at least need access to the source
    }
}
