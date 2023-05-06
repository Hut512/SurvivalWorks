package de.survivalworkers.core.client.engine.vk.util;

import java.util.List;

public class Util {
    private Util(){

    }

    public static float[] toArrayFloat(List<Float> list){
        float[] arr = new float[list.size()];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = list.get(i);
        }
        return arr;
    }

    public static int[] toArrayInt(List<Integer> list) {
        int[] arr = new int[list.size()];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = list.get(i);
        }
        return arr;
    }
}
