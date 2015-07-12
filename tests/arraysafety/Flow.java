public class Flow {

    /*
     * If dataflow refinement isn't working correctly,
     * we'll find a bound on x of [8, 8]
     * instead of [8, 10].
     */
    void conditionalRefinementFromValue(boolean b) {
	int[] array = new int[9];
	int x = 8;
	if (b) {
	    x = 10;
	}
	//:: error: (array.access.unsafe)
	array[x] = 9001;
    }

    void conditionalRefinementMinus(boolean b) {
	int[] array = new int[9];
	int x = 8;
	if (b) {
	    x = 9;
	}
	// now x is bounded by [8, 9]
	x = x - 1;
	// now x is bounded by [7, 8]
	array[x] = 42;
    }
    
}
