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

    void unsafeStore_ConstantPropagation() {
	int[] foo = new int[3];
	//:: error: (array.access.unsafe)
	foo[4] = 9001;
    }

    int safeLoad() {
	int[] foo = new int[3];
	return foo[0];
    }

    int unsafeLoad() {
	int[] foo = new int[3];
	//:: error: (array.access.unsafe)
	return foo[-1];
    }

    int unsafeLoad_ConstantPropagation() {
	int[] foo = new int[3];
	//:: error: (array.access.unsafe)
	return foo[4];
    }
}
