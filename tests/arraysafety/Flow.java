public class Flow {

    void safeConditionalAccess(int[] array, int idx) {
	if (idx >= 0 && idx < array.length) {
	    array[idx] = 42;
	}
    }

    void unsafeConditionalAccess(int[] array, int idx) {
	if (idx >= 0 && idx < array.length) {
	    array[idx] = 42;
	} else {
	    //:: error: (array.access.unsafe)
	    array[idx] = 9001;
	}
    }

    int safeForLoopAccess(int[] array) {
	int sum = 0;
	for (int i = 0; i < array.length; ++i) {
	    sum += array[i];
	}
	return sum;
    }

    int unsafeForLoopAccess(int[] array) {
	int sum = 0;
	for (int i = 0; i <= array.length; ++i) {
	    //:: error: (array.access.unsafe)
	    sum += array[i];
	}
	return sum;
    }

}
