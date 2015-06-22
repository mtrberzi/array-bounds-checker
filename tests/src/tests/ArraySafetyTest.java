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
	return testFiles("arraysafety", "all-systems");
    }
}
