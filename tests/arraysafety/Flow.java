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
	    //: error: (array.access.unsafe)
	    array[idx] = 9001;
	}
    }
    
}
