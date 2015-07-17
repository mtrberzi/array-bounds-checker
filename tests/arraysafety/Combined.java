public class Combined {
    // combined dataflow + array-length checks

    public int get_safe(int[] arr, int x) {
	if (x >= 0 && x < arr.length) {
	    return arr[x];
	} else {
	    return 0;
	}
    }
    /*
    public int max_safe(int[] arr) {
	int max = Integer.MIN_VALUE;
	int i;
	for(i = 0; i < arr.length; i = i + 1) {
	    if (arr[i] > max) {
		max = arr[i];
	    }
	}
	return max;
    }
    */
    
}
