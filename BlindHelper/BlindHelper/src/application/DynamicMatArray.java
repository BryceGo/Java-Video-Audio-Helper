package application;

import org.opencv.core.Mat;

/**
 * Created by Tyler on 2017-10-23.
 */
public class DynamicMatArray {

    private Mat[] matArray = new Mat[0];
    private int size = 0;

    public void addMat(Mat mat){
        Mat[] newArray = new Mat[this.size+1];
        System.arraycopy(this.matArray, 0, newArray, 0, this.size);
        newArray[this.size] = mat;
        this.matArray = newArray;
        this.size++;
    }

    public int getLength(){return this.size;}

    public Mat get(int i){return this.matArray[i];}

}
