package application;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;



import javafx.event.Event;

import javafx.concurrent.Task;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
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

import static org.opencv.imgcodecs.Imgcodecs.CV_LOAD_IMAGE_COLOR;
import static org.opencv.imgcodecs.Imgcodecs.imread;

public class Controller {
	
	@FXML
	private ImageView imageView; // the image display window in the GUI

	@FXML
	private Slider frameSlider;

	@FXML
	private Label fpsLabel;

	@FXML
	private Button playWithoutSoundButton, playWithSoundButton, stopButton;

	private static final String CLICK_PATH = "BlindHelper/src/application/Click.mp3";
	private static final String STOP_HOVER_PATH = "BlindHelper/src/application/STOP.mp3";
	private static final String OPENFILE_PATH = "BlindHelper/src/application/OPEN_FILE.mp3";
	private static final String PLAY_FRAME_PATH = "BlindHelper/src/application/PLAY_FRAME.mp3";
	private static final String PLAY_VIDEO_PATH = "BlindHelper/src/application/PLAY_VIDEO_W_SOUND.mp3";
	private static final String PLAY_VIDEO_WO_SOUND_PATH = "BlindHelper/src/application/PLAY_VIDEO_WO_SOUND.mp3";
	private static final String TITLE_PATH = "BlindHelper/src/application/TITLE.mp3";
	private static final String VIDEO_LOAD_PATH = "BlindHelper/src/application/video_loaded.mp3";
	
	
	private DynamicMatArray video;
	private Task task = new Task() {
		@Override
		protected Object call() throws Exception {
			return null;
		}
	};
	private MediaPlayer clickPlayer = new MediaPlayer(new Media(new File(CLICK_PATH).toURI().toString()));
	

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
		numberOfQuantizionLevels = 16;
		
		numberOfSamplesPerColumn = 500;
		
		// assign frequencies for each particular row
		freq = new double[height]; // Be sure you understand why it is height rather than width
		freq[height/2-1] = 440.0; // 440KHz - Sound of A (La)
		for (int m = height/2; m < height; m++) {
			freq[m] = freq[m-1] * Math.pow(2, 1.0/12.0); 
		}
		for (int m = height/2-2; m >=0; m--) {
			freq[m] = freq[m+1] * Math.pow(2, -1.0/12.0); 
		}

		startFrame = 0;
		this.currentMediaPath = "";
		this.video = new DynamicMatArray();
		frameSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
			startFrame = (int) Math.round((Double) newValue);
			displayImage(video.get(startFrame));
		});
		updateSlider();
	}


	
	
	@FXML
	protected void openMedia(ActionEvent event) throws InterruptedException {
		// This method opens an image and display it using the GUI
		// You should modify the logic so that it opens and displays a video
		final String mediaFilename = getMediaFilename();
		if (!mediaFilename.isEmpty()){
			video = new DynamicMatArray(); // Resets the dynamic array
			readInMedia(this.currentMediaPath);
			if (video.getLength() > 0){
				if (video.getLength() == 1) {
					playWithoutSoundButton.setDisable(true);
					playWithSoundButton.setDisable(true);
					stopButton.setDisable(true);
				} else {
					playWithoutSoundButton.setDisable(false);
					playWithSoundButton.setDisable(false);
					stopButton.setDisable(false);
				}
				displayImage(this.video.get(0));
				updateSlider();
				startFrame = 0;
			}
		}
		// You don't have to understand how mat2Image() works.
		// In short, it converts the image from the Mat format to the Image format
		// The Mat format is used by the opencv library, and the Image format is used by JavaFX
		// BTW, you should be able to explain briefly what opencv and JavaFX are after finishing this assignment
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
//					if (i % 2 == 1){playClick();}
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

	@FXML
	protected void playFrame(ActionEvent event) throws LineUnavailableException {
		playImage(video.get(startFrame));
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
		MediaPlayer sound;
		if (mediaFilepath.contains(".mp4") || mediaFilepath.contains(".gif")) {
			VideoCapture capture = new VideoCapture(this.currentMediaPath);
			if (capture.isOpened()) {
				Mat frame = new Mat();
				while (capture.read(frame)) {
					this.video.addMat(frame);
					frame = new Mat();
				}
			}
			capture.release();
		} else {
			Mat image = imread(mediaFilepath, CV_LOAD_IMAGE_COLOR);
			video.addMat(image);
		}
		
		sound = new MediaPlayer(new Media(new File(VIDEO_LOAD_PATH).toURI().toString()));
		sound.play();
	}

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
		MediaPlayer clickPlayer = new MediaPlayer(new Media(new File(CLICK_PATH).toURI().toString()));
		clickPlayer.play();
	}
	
	@FXML
	protected void hoverStop(Event event) {
		MediaPlayer sound = new MediaPlayer(new Media(new File(STOP_HOVER_PATH).toURI().toString()));
		sound.play();
		System.out.println("Hovered: Stop");
	}
	
	
	@FXML
	protected void hoverOpenFile(Event event) {
		MediaPlayer sound = new MediaPlayer(new Media(new File(OPENFILE_PATH).toURI().toString()));
		sound.play();
		System.out.println("Hovered: Open File");
	}
	
	@FXML
	protected void hoverPlayFrame(Event event) {
		MediaPlayer sound = new MediaPlayer(new Media(new File(PLAY_FRAME_PATH).toURI().toString()));
		sound.play();
		System.out.println("Hovered: Play Frame");
	}
	
	@FXML
	protected void hoverPlayVideo(Event event) {
		MediaPlayer sound = new MediaPlayer(new Media(new File(PLAY_VIDEO_PATH).toURI().toString()));
		sound.play();
		System.out.println("Hovered: Play Video");
	}
	
	@FXML
	protected void hoverPlayVideoWithoutSound(Event event) {
		MediaPlayer sound = new MediaPlayer(new Media(new File(PLAY_VIDEO_WO_SOUND_PATH).toURI().toString()));
		sound.play();
		System.out.println("Hovered: Play Video without sound");
	}
	
	@FXML
	protected void hoverTitle(Event event) {
		MediaPlayer sound = new MediaPlayer(new Media(new File(TITLE_PATH).toURI().toString()));
		sound.play();
		System.out.println("Hovered: Title");
	}
	
}
