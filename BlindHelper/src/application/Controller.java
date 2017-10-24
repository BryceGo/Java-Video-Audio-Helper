package application;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import javafx.concurrent.Task;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.media.Media;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.image.ImageView;
import org.opencv.videoio.VideoCapture;
import utilities.Utilities;

import java.io.File;

public class Controller {
	
	@FXML
	private ImageView imageView; // the image display window in the GUI

	@FXML
	private Slider frameSlider;

	@FXML
	private Label fpsLabel;
	
	private DynamicMatArray video;
	private Task task = new Task() {
		@Override
		protected Object call() throws Exception {
			return null;
		}
	};
	
	private int width;
	private int height;
	private int sampleRate; // sampling frequency
	private int sampleSizeInBits;
	private int numberOfChannels;
	private double[] freq; // frequencies for each particular row
	private int numberOfQuantizionLevels;
	private int numberOfSamplesPerColumn;

	private String currentMediaPath;
	private int startFrame = 0;

	@FXML
	private void initialize() {
		// Optional: You should modify the logic so that the user can change these values
		// You may also do some experiments with different values
		width = 64;
		height = 64;
		sampleRate = 8000;
		sampleSizeInBits = 8;
		numberOfChannels = 1;

		startFrame = 0;
		
		numberOfQuantizionLevels = 16;
		
		numberOfSamplesPerColumn = 500;

		this.currentMediaPath = "";
		this.video = new DynamicMatArray();

		updateSlider();
		frameSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            startFrame = (int) Math.round((Double) newValue);
            displayImage(video.get(startFrame));
        });

		hideFPSLabel();
		
		// assign frequencies for each particular row
		freq = new double[height]; // Be sure you understand why it is height rather than width
		freq[height/2-1] = 440.0; // 440KHz - Sound of A (La)
		for (int m = height/2; m < height; m++) {
			freq[m] = freq[m-1] * Math.pow(2, 1.0/12.0); 
		}
		for (int m = height/2-2; m >=0; m--) {
			freq[m] = freq[m+1] * Math.pow(2, -1.0/12.0); 
		}
	}
	
	private String getMediaFilename() {
		// This method should return the filename of the image to be played
		// You should insert your code here to allow user to select the file
		Stage stage = new Stage();
		stage.setTitle("File Chooser");

		final FileChooser fileChooser = new FileChooser();
		configureFileChooser(fileChooser);
		File file = fileChooser.showOpenDialog(stage);

		if (file != null) {
			this.currentMediaPath = file.getPath();
		}
		return this.currentMediaPath;
	}

	private void changeSliderValue(int frame){frameSlider.setValue(frame);}

	private void configureFileChooser(final FileChooser fileChooser){
		fileChooser.setTitle("Choose Image");
		fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));

		fileChooser.getExtensionFilters().addAll(
				new FileChooser.ExtensionFilter("Media File", "*.*"),
				new FileChooser.ExtensionFilter("JPG", "*.jpg"),
				new FileChooser.ExtensionFilter("PNG", "*.png"),
				new FileChooser.ExtensionFilter("MP4", "*.mp4"));
	}
	
	@FXML
	protected void openMedia(ActionEvent event) throws InterruptedException {
		// This method opens an image and display it using the GUI
		// You should modify the logic so that it opens and displays a video
		final String mediaFilename = getMediaFilename();
		if (!mediaFilename.isEmpty()){
			video = new DynamicMatArray(); // Resets the dynamic array
			readInMedia(this.currentMediaPath);
			if (video.getLength() <= 1){hideFPSLabel();} else {showFPSLabel();}
			displayImage(this.video.get(0));
			updateSlider();
		}
		// You don't have to understand how mat2Image() works.
		// In short, it converts the image from the Mat format to the Image format
		// The Mat format is used by the opencv library, and the Image format is used by JavaFX
		// BTW, you should be able to explain briefly what opencv and JavaFX are after finishing this assignment
	}

	private void updateSlider(){
		if (video.getLength() <= 1){
			frameSlider.setDisable(true);
		}
		else {
			frameSlider.setDisable(false);
			frameSlider.setMin(0);
			frameSlider.setMax(video.getLength());
			frameSlider.setSnapToTicks(true);
			frameSlider.setBlockIncrement(1);
			frameSlider.setShowTickMarks(true);
			frameSlider.setShowTickLabels(true);
		}
	}

	private void readInMedia(String mediaFilepath){
		VideoCapture capture = new VideoCapture(this.currentMediaPath);
		if (capture.isOpened()){
			Mat frame = new Mat();
			while (capture.read(frame)){
				this.video.addMat(frame);
				frame = new Mat();
			}
		}
		capture.release();
	}

	@FXML
	protected void playVideoWithSound(ActionEvent event) throws LineUnavailableException {
		// This method "plays" the image opened by the user
		// You should modify the logic so that it plays a video rather than an image

		if (task.isRunning()){task.cancel();}

		final int numberFrames = video.getLength();
		task = new Task<Void>() {

			@Override
			protected Void call() throws LineUnavailableException {
				for (int i = startFrame; i < numberFrames; i++){
					changeSliderValue(i);
					if (isCancelled()){break;}
					playImage(video.get(i));
					displayImage(video.get(i + 1));
					if (i % 2 == 1){playClick();}
				}
				displayImage(video.get(startFrame));
				return null;
			}
		};
		new Thread(task).start();
	}

	@FXML
	protected void playVideoWithoutSound(ActionEvent event){
		if (task.isRunning()){task.cancel();}

		final int numberFrames = video.getLength();
		task = new Task<Void>() {

			@Override
			protected Void call() throws LineUnavailableException, InterruptedException {
				for (int i = startFrame; i < numberFrames; i++){
					changeSliderValue(i);
					if (isCancelled()){break;}
					displayImage(video.get(i + 1));
					if (i % 2 == 1){playClick();}
					Thread.sleep(33);
				}
				displayImage(video.get(startFrame));
				return null;
			}
		};
		new Thread(task).start();
	}

	@FXML
	protected void stopVideo(ActionEvent event){task.cancel();}

	private void displayImage(Mat image){
		imageView.setImage(Utilities.mat2Image(image));
	}

	private void playImage (Mat image)throws LineUnavailableException{

		if (image != null) {
			// convert the image from RGB to grayscale
			Mat grayImage = new Mat();
			Imgproc.cvtColor(image, grayImage, Imgproc.COLOR_BGR2GRAY);

			// resize the image
			Mat resizedImage = new Mat();
			Imgproc.resize(grayImage, resizedImage, new Size(width, height));

			// quantization
			double[][] roundedImage = new double[resizedImage.rows()][resizedImage.cols()];
			for (int row = 0; row < resizedImage.rows(); row++) {
				for (int col = 0; col < resizedImage.cols(); col++) {
					roundedImage[row][col] = (double) Math.floor(resizedImage.get(row, col)[0] / numberOfQuantizionLevels) / numberOfQuantizionLevels;
				}
			}

			// I used an AudioFormat object and a SourceDataLine object to perform audio output. Feel free to try other options
			AudioFormat audioFormat = new AudioFormat(sampleRate, sampleSizeInBits, numberOfChannels, true, true);
			SourceDataLine sourceDataLine = AudioSystem.getSourceDataLine(audioFormat);
			sourceDataLine.open(audioFormat, sampleRate);
			sourceDataLine.start();

			for (int col = 0; col < width; col++) {
				byte[] audioBuffer = new byte[numberOfSamplesPerColumn];
				for (int t = 1; t <= numberOfSamplesPerColumn; t++) {
					double signal = 0;
					for (int row = 0; row < height; row++) {
						int m = height - row - 1; // Be sure you understand why it is height rather width, and why we subtract 1
						int time = t + col * numberOfSamplesPerColumn;
						double ss = Math.sin(2 * Math.PI * freq[m] * (double) time / sampleRate);
						signal += roundedImage[row][col] * ss;
					}
					double normalizedSignal = signal / height; // signal: [-height, height];  normalizedSignal: [-1, 1]
					audioBuffer[t - 1] = (byte) (normalizedSignal * 0x7F); // Be sure you understand what the weird number 0x7F is for
				}
				sourceDataLine.write(audioBuffer, 0, numberOfSamplesPerColumn);
			}
			sourceDataLine.drain();
			sourceDataLine.close();
		}
	}

	private void playClick(){
		// TODO Implement This Using the Click.mp3 file in src/application
	}

	private void hideFPSLabel(){fpsLabel.setText("");}

	private void showFPSLabel(){fpsLabel.setText("All Videos Assumed To Be 30 FPS");}
}
