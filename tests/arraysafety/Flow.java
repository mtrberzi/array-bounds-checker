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

}
