import org.checkerframework.checker.arraysafety.qual.SafeArrayIndex;
import org.checkerframework.checker.arraysafety.qual.UnsafeArrayIndex;

public class Simple {
    
    void safeStore() {
	int[] foo = new int[3];
	foo[0] = 42;
    }

    void unsafeStore() {
	int[] foo = new int[3];
	//:: error: (array.access.unsafe)
	foo[-1] = 9001;
    }
}
