public class Combined {
    // combined dataflow + array-length checks

    public int get_safe(int[] arr, int x) {
	if (x >= 0 && x < arr.length) {
	    return arr[x];
	} else {
	    return 0;
	}
    }
    
}
