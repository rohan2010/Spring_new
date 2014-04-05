import processing.core.PApplet;


@SuppressWarnings("serial")
public class SmartEarplugsMoviePlayer extends PApplet {

//	final int PLAY = 0;
//	final int PAUSE = 1;
//	Movie mMovie = null;
//	SmartEarplugs mEarplugs;
//	boolean earplugsPaused = false;
//	boolean keyPaused = false;
//	PImage imgToast = null;
//	float scale = 1.0f;
//
//	final int MONO = 1;
//	int ctrlMode = MONO;
//	
//	// labels
//	String[] modelLabels = {"PlugsIn", "PlugsOut"};
//
//	public void setup() {
//		size(displayWidth, displayHeight);
//
//		mEarplugs = new SmartEarplugs(SmartEarplugs.ControlMode.MONO);
//
//		mMovie = new Movie(this, "dm.mov");
//		mMovie.loop();
//	}
//
//	public void draw() {
//		background(0);
//		image(mMovie, (displayWidth - mMovie.width) / 2, (displayHeight - mMovie.height) / 2);
//		if (imgToast != null && imgToast.width > 5 && imgToast.height > 5) {
//			int left = (width - imgToast.width) / 2;
//			int top = (height - imgToast.height) / 2;
//			image(imgToast, left, top);
//			imgToast.resize((int) (imgToast.width * scale),
//					(int) (imgToast.height * scale));
//			scale *= 0.99f;
//		}
//
//		switch (ctrlMode) {
//		case MONO:
//			if (!keyPaused) {
//				if (mEarplugs.isPlugsIn()) {
//					// println("plugin");
//					if (earplugsPaused) {
//						mMovie.play();
//						toast(PLAY);
//						earplugsPaused = false;
//					}
//				} else {
//					// println("plugout");
//					if (!earplugsPaused) {
//						mMovie.pause();
//						toast(PAUSE);
//						earplugsPaused = true;
//					}
//				}
//			}
//			break;
//		}
//		
//	}
//
//	void toast(int iconCode) {
//		switch (iconCode) {
//		case PLAY:
//			imgToast = loadImage("play.png");
//			scale = 1.0f;
//			break;
//		case PAUSE:
//			imgToast = loadImage("pause.png");
//			scale = 1.0f;
//			break;
//		}
//	}
//
//	public void movieEvent(Movie m) {
//		m.read();
//	}
//
//	public void keyPressed() {
//		switch (key) {
//		case ' ':
//			if (keyPaused) {
//				mMovie.play();
//				toast(PLAY);
//				keyPaused = false;
//			} else {
//				mMovie.pause();
//				toast(PAUSE);
//				keyPaused = true;
//			}
//			break;
//		}
//	}
//	
//	public static void main(String args[]) {
//		PApplet.main(new String[] {"--present", "me.xiangchen.smartearplugs.SmartEarplugsMoviePlayer"});
//	}
}
