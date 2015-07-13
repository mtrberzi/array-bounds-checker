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

    void dataflowAddition(boolean b) {
	int[] array = new int[3];
	int x = -1;
	if (b) {
	    x = 0;
	}
	// now x is bounded by [-1, 0]
	x = x + 1;
	// now x is bounded by [0, 1]
	array[x] = 42;
    }

    void dataflowMinus(boolean b) {
	int[] array = new int[3];
	int x = -1;
	if (b) {
	    x = 0;
	}
	// x : [-1, 0]
	x = -x;
	// x : [0, 1]
	array[x] = 42;
    }
    
    void dataflowSubtraction(boolean b) {
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
        
    void dataflowGreaterThanOrEqual_LHS(boolean b) {
	int[] array = new int[5];
	int x = -3;
	if (b) {
	    x = 3;
	}
	// x : [-3, 3]
	if (x >= 0) {
	    // x : [0, 3]
	    array[x] = 42;
	}
    }
    

    void dataflowGreaterThanOrEqual_RHS(boolean b) {
	int[] array = new int[4];
	int x = 5;
	if (b) {
	    x = 2;
	}
	// x : [2, 5]
	if (3 >= x) {
	    // x : [2, 3]
	    array[x] = 42;
	}
    }
    
}
