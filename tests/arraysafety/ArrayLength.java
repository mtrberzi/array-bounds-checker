public class ArrayLength {

    void simple_neg(int[] arr, boolean b) {
	int x = 0;
	if (b) {
	    x = 9001;
	}
	// x : [0, 9001]
	if (x < arr.length) {
	    // x : [0, 9001] < {arr}
	    arr[x] = 42;
	}
    }

    int simple_pos(int[] arr, boolean b) {
	int x = 0;
	if (b) {
	    x = 9001;
	}
	// x : [0, 9001]
	if (x < arr.length) {
	    // x : [0, 9001] < {arr}
	    return -1;
	} else {
	    // x : [0, 9001]
	    //:: warning: (array.access.unknown)
	    return arr[x];
	}
    }

    
}
