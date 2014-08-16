package net.sourceforge.meid;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import net.sourceforge.meid.R;


import android.R.string;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.hardware.Camera.Face;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.location.Location;
import android.media.CamcorderProfile;
import android.media.ExifInterface;
import android.media.MediaMetadataRetriever;
import android.media.MediaRecorder;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.ScaleGestureDetector;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;
import android.widget.ZoomControls;
import android.widget.SeekBar.OnSeekBarChangeListener;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
public class Preview extends SurfaceView implements SurfaceHolder.Callback {
	private static final String TAG = "Preview";

	private static final String TAG_GPS_IMG_DIRECTION = "GPSImgDirection";
	private static final String TAG_GPS_IMG_DIRECTION_REF = "GPSImgDirectionRef";

	private Paint p = new Paint();
	private DecimalFormat decimalFormat = new DecimalFormat("#0.0");
    private Camera.CameraInfo camera_info = new Camera.CameraInfo();
    private Matrix camera_to_preview_matrix = new Matrix();
    private Matrix preview_to_camera_matrix = new Matrix();
	private RectF face_rect = new RectF();
	private Rect text_bounds = new Rect();
    private int display_orientation = 0;

	private boolean ui_placement_right = true;

	private boolean app_is_paused = true;
	private SurfaceHolder mHolder = null;
	private boolean has_surface = false;
	private boolean has_aspect_ratio = false;
	private double aspect_ratio = 0.0f;
	private Camera camera = null;
	private int cameraId = 0;
	private boolean is_video = false;
	private MediaRecorder video_recorder = null;
	private boolean video_start_time_set = false;
	private long video_start_time = 0;
	private String video_name = null;
	int i = 0;
	private final int PHASE_NORMAL = 0;
	private final int PHASE_TIMER = 1;
	private final int PHASE_TAKING_PHOTO = 2;
	private final int PHASE_PREVIEW_PAUSED = 3; // the paused state after taking a photo
	private int phase = PHASE_NORMAL;
	/*private boolean is_taking_photo = false;
	private boolean is_taking_photo_on_timer = false;*/
	private Timer takePictureTimer = new Timer();
	private TimerTask takePictureTimerTask = null;
	private Timer beepTimer = new Timer();
	private TimerTask beepTimerTask = null;
	private long take_photo_time = 0;
	private int remaining_burst_photos = 0;
	private int n_burst = 1;
	private String [] image_name=null;

	private boolean is_preview_started = false;
	//private boolean is_preview_paused = false; // whether we are in the paused state after taking a photo
	private String preview_image_name = null;
	private Bitmap thumbnail = null; // thumbnail of last picture taken
	private boolean thumbnail_anim = false; // whether we are displaying the thumbnail animation
	private long thumbnail_anim_start_ms = -1; // time that the thumbnail animation started
	private RectF thumbnail_anim_src_rect = new RectF();
	private RectF thumbnail_anim_dst_rect = new RectF();
	private Matrix thumbnail_anim_matrix = new Matrix();
	private int [] gui_location = new int[2];

	private int current_orientation = 0; // orientation received by onOrientationChanged
	private int current_rotation = 0; // orientation relative to camera's orientation (used for parameters.setOrientation())
	private boolean has_level_angle = false;
	private double level_angle = 0.0f;
	
	private float free_memory_gb = -1.0f;
	private long last_free_memory_time = 0;

	private boolean has_zoom = false;
	private int zoom_factor = 0;
	private int max_zoom_factor = 0;
	private ScaleGestureDetector scaleGestureDetector;
	private List<Integer> zoom_ratios = null;
	private boolean touch_was_multitouch = false;

	private List<String> supported_flash_values = null; // our "values" format
	private int current_flash_index = -1; // this is an index into the supported_flash_values array, or -1 if no flash modes available

	private List<String> supported_focus_values = null; // our "values" format
	private int current_focus_index = -1; // this is an index into the supported_focus_values array, or -1 if no focus modes available

	private List<String> color_effects = null;
	private List<String> scene_modes = null;
	private List<String> white_balances = null;
	private List<String> exposures = null;
	private int min_exposure = 0;
	private int max_exposure = 0;

	private List<Camera.Size> sizes = null;
	private int current_size_index = -1; // this is an index into the sizes array, or -1 if sizes not yet set
	
	private List<Integer> video_quality = null;
	private int current_video_quality = -1; // this is an index into the video_quality array, or -1 if not found (though this shouldn't happen?)
	
	private Location location = null;
	public boolean has_set_location = false;
	private Bitmap location_bitmap = null;
	private Rect location_dest = new Rect();

	class ToastBoxer {
		public Toast toast = null;

		ToastBoxer() {
		}
	}
	private ToastBoxer switch_camera_toast = new ToastBoxer();
	private ToastBoxer switch_video_toast = new ToastBoxer();
	private ToastBoxer flash_toast = new ToastBoxer();
	private ToastBoxer focus_toast = new ToastBoxer();
	private ToastBoxer take_photo_toast = new ToastBoxer();
	private ToastBoxer stopstart_video_toast = new ToastBoxer();
	private ToastBoxer change_exposure_toast = new ToastBoxer();
	
	private int ui_rotation = 0;

	private boolean supports_face_detection = false;
	private boolean using_face_detection = false;
	private Face [] faces_detected = null;
	private boolean has_focus_area = false;
	private int focus_screen_x = 0;
	private int focus_screen_y = 0;
	private long focus_complete_time = -1;
	private int focus_success = FOCUS_DONE;
	private static final int FOCUS_WAITING = 0;
	private static final int FOCUS_SUCCESS = 1;
	private static final int FOCUS_FAILED = 2;
	private static final int FOCUS_DONE = 3;

	private IntentFilter battery_ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
	private boolean has_battery_frac = false;
	private float battery_frac = 0.0f;
	private long last_battery_time = 0;

	// accelerometer and geomagnetic sensor info
	private final float sensor_alpha = 0.8f; // for filter
    private boolean has_gravity = false;
    private float [] gravity = new float[3];
    private boolean has_geomagnetic = false;
    private float [] geomagnetic = new float[3];
    private float [] deviceRotation = new float[9];
    private float [] cameraRotation = new float[9];
    private float [] deviceInclination = new float[9];
    private boolean has_geo_direction = false;
    private float [] geo_direction = new float[3];

    // for testing:
	public int count_cameraStartPreview = 0;
	public int count_cameraAutoFocus = 0;
	public int count_cameraTakePicture = 0;
	public boolean has_received_location = false;
	public boolean test_low_memory = false;
	public boolean test_have_angle = false;
	public float test_angle = 0.0f;
	public String test_last_saved_image = null;

	Preview(Context context) {
		this(context, null);
	}

	@SuppressWarnings("deprecation")
	Preview(Context context, Bundle savedInstanceState) {
		super(context);
		if( MyDebug.LOG ) {
			Log.d(TAG, "new Preview");
		}

		// Install a SurfaceHolder.Callback so we get notified when the
		// underlying surface is created and destroyed.
		mHolder = getHolder();
		mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS); // deprecated

	    scaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());

        if( savedInstanceState != null ) {
    		if( MyDebug.LOG )
    			Log.d(TAG, "have savedInstanceState");
    		cameraId = savedInstanceState.getInt("cameraId", 0);
			if( MyDebug.LOG )
				Log.d(TAG, "found cameraId: " + cameraId);
    		if( cameraId < 0 || cameraId >= Camera.getNumberOfCameras() ) {
    			if( MyDebug.LOG )
    				Log.d(TAG, "cameraID not valid for " + Camera.getNumberOfCameras() + " cameras!");
    			cameraId = 0;
    		}
        }

    	location_bitmap = BitmapFactory.decodeResource(this.getResources(), R.drawable.earth);
	}
	
	/*private void previewToCamera(float [] coords) {
		float alpha = coords[0] / (float)this.getWidth();
		float beta = coords[1] / (float)this.getHeight();
		coords[0] = 2000.0f * alpha - 1000.0f;
		coords[1] = 2000.0f * beta - 1000.0f;
	}*/

	/*private void cameraToPreview(float [] coords) {
		float alpha = (coords[0] + 1000.0f) / 2000.0f;
		float beta = (coords[1] + 1000.0f) / 2000.0f;
		coords[0] = alpha * (float)this.getWidth();
		coords[1] = beta * (float)this.getHeight();
	}*/

	private void calculateCameraToPreviewMatrix() {
		camera_to_preview_matrix.reset();
		// from http://developer.android.com/reference/android/hardware/Camera.Face.html#rect
		Camera.getCameraInfo(cameraId, camera_info);
		// Need mirror for front camera.
		boolean mirror = (camera_info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT);
		camera_to_preview_matrix.setScale(mirror ? -1 : 1, 1);
		// This is the value for android.hardware.Camera.setDisplayOrientation.
		camera_to_preview_matrix.postRotate(display_orientation);
		// Camera driver coordinates range from (-1000, -1000) to (1000, 1000).
		// UI coordinates range from (0, 0) to (width, height).
		camera_to_preview_matrix.postScale(this.getWidth() / 2000f, this.getHeight() / 2000f);
		camera_to_preview_matrix.postTranslate(this.getWidth() / 2f, this.getHeight() / 2f);
	}

	private void calculatePreviewToCameraMatrix() {
		calculateCameraToPreviewMatrix();
		if( !camera_to_preview_matrix.invert(preview_to_camera_matrix) ) {
    		if( MyDebug.LOG )
    			Log.d(TAG, "calculatePreviewToCameraMatrix failed to invert matrix!?");
		}
	}

	private ArrayList<Camera.Area> getAreas(float x, float y) {
		float [] coords = {x, y};
		calculatePreviewToCameraMatrix();
		preview_to_camera_matrix.mapPoints(coords);
		float focus_x = coords[0];
		float focus_y = coords[1];
		
		int focus_size = 50;
		if( MyDebug.LOG ) {
			Log.d(TAG, "x, y: " + x + ", " + y);
			Log.d(TAG, "focus x, y: " + focus_x + ", " + focus_y);
		}
		Rect rect = new Rect();
		rect.left = (int)focus_x - focus_size;
		rect.right = (int)focus_x + focus_size;
		rect.top = (int)focus_y - focus_size;
		rect.bottom = (int)focus_y + focus_size;
		if( rect.left < -1000 ) {
			rect.left = -1000;
			rect.right = rect.left + 2*focus_size;
		}
		else if( rect.right > 1000 ) {
			rect.right = 1000;
			rect.left = rect.right - 2*focus_size;
		}
		if( rect.top < -1000 ) {
			rect.top = -1000;
			rect.bottom = rect.top + 2*focus_size;
		}
		else if( rect.bottom > 1000 ) {
			rect.bottom = 1000;
			rect.top = rect.bottom - 2*focus_size;
		}

	    ArrayList<Camera.Area> areas = new ArrayList<Camera.Area>();
	    areas.add(new Camera.Area(rect, 1000));
	    return areas;
	}

	@Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleGestureDetector.onTouchEvent(event);
		MainActivity main_activity = (MainActivity)this.getContext();
		main_activity.clearSeekBar();
        //invalidate();
		/*if( MyDebug.LOG ) {
			Log.d(TAG, "touch event: " + event.getAction());
		}*/
		if( event.getPointerCount() != 1 ) {
			//multitouch_time = System.currentTimeMillis();
			touch_was_multitouch = true;
			return true;
		}
		if( event.getAction() != MotionEvent.ACTION_UP ) {
			if( event.getAction() == MotionEvent.ACTION_DOWN && event.getPointerCount() == 1 ) {
				touch_was_multitouch = false;
			}
			return true;
		}
		if( touch_was_multitouch ) {
			return true;
		}
		if( this.isTakingPhotoOrOnTimer() ) {
			return true;
		}

		// note, we always try to force start the preview (in case is_preview_paused has become false)
        startCameraPreview();

        if( camera != null && !this.using_face_detection ) {
            Camera.Parameters parameters = camera.getParameters();
			String focus_mode = parameters.getFocusMode();
    		this.has_focus_area = false;
            if( parameters.getMaxNumFocusAreas() != 0 && ( focus_mode.equals(Camera.Parameters.FOCUS_MODE_AUTO) || focus_mode.equals(Camera.Parameters.FOCUS_MODE_MACRO) || focus_mode.equals(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE) || focus_mode.equals(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO) ) ) {
        		if( MyDebug.LOG )
        			Log.d(TAG, "set focus (and metering?) area");
				this.has_focus_area = true;
				this.focus_screen_x = (int)event.getX();
				this.focus_screen_y = (int)event.getY();

				ArrayList<Camera.Area> areas = getAreas(event.getX(), event.getY());
			    parameters.setFocusAreas(areas);

			    // also set metering areas
			    if( parameters.getMaxNumMeteringAreas() == 0 ) {
	        		if( MyDebug.LOG )
	        			Log.d(TAG, "metering areas not supported");
			    }
			    else {
			    	parameters.setMeteringAreas(areas);
			    }

			    try {
	        		if( MyDebug.LOG )
	        			Log.d(TAG, "set focus areas parameters");
			    	camera.setParameters(parameters);
	        		if( MyDebug.LOG )
	        			Log.d(TAG, "done");
			    }
			    catch(RuntimeException e) {
			    	// just in case something has gone wrong
	        		if( MyDebug.LOG )
	        			Log.d(TAG, "failed to set parameters for focus area");
	        		e.printStackTrace();
			    }
            }
            else if( parameters.getMaxNumMeteringAreas() != 0 ) {
        		if( MyDebug.LOG )
        			Log.d(TAG, "set metering area");
        		// don't set has_focus_area in this mode
				ArrayList<Camera.Area> areas = getAreas(event.getX(), event.getY());
		    	parameters.setMeteringAreas(areas);

			    try {
			    	camera.setParameters(parameters);
			    }
			    catch(RuntimeException e) {
			    	// just in case something has gone wrong
	        		if( MyDebug.LOG )
	        			Log.d(TAG, "failed to set parameters for focus area");
	        		e.printStackTrace();
			    }
            }
        }
        
		tryAutoFocus();
		return true;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
    	@Override
    	public boolean onScale(ScaleGestureDetector detector) {
    		if( Preview.this.camera != null && Preview.this.has_zoom ) {
    			Preview.this.scaleZoom(detector.getScaleFactor());
    		}
    		return true;
    	}
    }
    
    public void clearFocusAreas() {
		if( MyDebug.LOG )
			Log.d(TAG, "clearFocusAreas()");
        Camera.Parameters parameters = camera.getParameters();
        boolean update_parameters = false;
        if( parameters.getMaxNumFocusAreas() > 0 ) {
        	parameters.setFocusAreas(null);
        	update_parameters = true;
        }
        if( parameters.getMaxNumMeteringAreas() > 0 ) {
        	parameters.setMeteringAreas(null);
        	update_parameters = true;
        }
        if( update_parameters ) {
        	camera.setParameters(parameters);
        }
		has_focus_area = false;
		focus_success = FOCUS_DONE;
        //Log.d(TAG, "camera parameters null? " + (camera.getParameters().getFocusAreas()==null));
    }

    /*private void setCameraParameters() {
	}*/
	
	public void surfaceCreated(SurfaceHolder holder) {
		if( MyDebug.LOG )
			Log.d(TAG, "surfaceCreated()");
		// The Surface has been created, acquire the camera and tell it where
		// to draw.
		this.has_surface = true;
		this.openCamera();
		this.setWillNotDraw(false); // see http://stackoverflow.com/questions/2687015/extended-surfaceviews-ondraw-method-never-called
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		if( MyDebug.LOG )
			Log.d(TAG, "surfaceDestroyed()");
		// Surface will be destroyed when we return, so stop the preview.
		// Because the CameraDevice object is not a shared resource, it's very
		// important to release it when the activity is paused.
		this.has_surface = false;
		this.closeCamera();
	}
	
	private void stopVideo() {
		if( MyDebug.LOG )
			Log.d(TAG, "stopVideo()");
		if( video_recorder != null ) { // check again, just to be safe
    		if( MyDebug.LOG )
    			Log.d(TAG, "stop video recording");
    		showToast(stopstart_video_toast, "Stopped recording video");
			/*is_taking_photo = false;
			is_taking_photo_on_timer = false;*/
    		this.phase = PHASE_NORMAL;
			try {
				video_recorder.stop();
			}
			catch(RuntimeException e) {
				// stop() can throw a RuntimeException if stop is called too soon after start - we have no way to detect this, so have to catch it
	    		if( MyDebug.LOG )
	    			Log.d(TAG, "runtime exception when stopping video");
			}
    		video_recorder.reset();
    		video_recorder.release(); 
    		video_recorder = null;
    		reconnectCamera();
    		if( video_name != null ) {
    			File file = new File(video_name);
    			if( file != null ) {
    				// need to scan when finished, so we update for the completed file
    				MainActivity main_activity = (MainActivity)this.getContext();
    	            main_activity.broadcastFile(file);
    			}
    			// create thumbnail
    			{
	            	long time_s = System.currentTimeMillis();
	            	Bitmap old_thumbnail = thumbnail;
    	    	    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
					try {
						retriever.setDataSource(video_name);
						thumbnail = retriever.getFrameAtTime(-1);
					}
    	    	    catch(IllegalArgumentException ex) {
    	    	    	// corrupt video file?
    	    	    }
    	    	    catch(RuntimeException ex) {
    	    	    	// corrupt video file?
    	    	    }
    	    	    finally {
    	    	    	try {
    	    	    		retriever.release();
    	    	    	}
    	    	    	catch(RuntimeException ex) {
    	    	    		// ignore
    	    	    	}
    	    	    }
    	    	    if( thumbnail != null && thumbnail != old_thumbnail ) {
    	    			MainActivity main_activity = (MainActivity)Preview.this.getContext();
    	    	    	ImageButton galleryButton = (ImageButton) main_activity.findViewById(R.id.gallery);
    	    	    	int width = thumbnail.getWidth();
    	    	    	int height = thumbnail.getHeight();
    					if( MyDebug.LOG )
    						Log.d(TAG, "    video thumbnail size " + width + " x " + height);
    	    	    	if( width > galleryButton.getWidth() ) {
    	    	    		float scale = (float) galleryButton.getWidth() / width;
    	    	    		int new_width = Math.round(scale * width);
    	    	    		int new_height = Math.round(scale * height);
        					if( MyDebug.LOG )
        						Log.d(TAG, "    scale video thumbnail to " + new_width + " x " + new_height);
    	    	    		Bitmap scaled_thumbnail = Bitmap.createScaledBitmap(thumbnail, new_width, new_height, true);
    	        		    // careful, as scaled_thumbnail is sometimes not a copy!
    	        		    if( scaled_thumbnail != thumbnail ) {
    	        		    	thumbnail.recycle();
    	        		    	thumbnail = scaled_thumbnail;
    	        		    }
    	    	    	}
    	    	    	main_activity.updateGalleryIconToBitmap(thumbnail);
        	    		if( old_thumbnail != null ) {
        	    			// only recycle after we've set the new thumbnail
        	    			old_thumbnail.recycle();
        	    		}
    	    	    }
					if( MyDebug.LOG )
						Log.d(TAG, "    time to create thumbnail: " + (System.currentTimeMillis() - time_s));
    			}
    			video_name = null;
    		}
		}
	}
	
	private void reconnectCamera() {
        if( camera != null ) { // just to be safe
    		try {
				camera.reconnect();
		        this.startCameraPreview();
			}
    		catch (IOException e) {
        		if( MyDebug.LOG )
        			Log.e(TAG, "failed to reconnect to camera");
				e.printStackTrace();
	    	    showToast(null, "Failed to reconnect to camera");
	    	    closeCamera();
			}
			tryAutoFocus();
		}
	}

	private void closeCamera() {
		if( MyDebug.LOG ) {
			Log.d(TAG, "closeCamera()");
		}
		has_focus_area = false;
		focus_success = FOCUS_DONE;
        has_set_location = false;
		MainActivity main_activity = (MainActivity)this.getContext();
		main_activity.clearSeekBar();
		//if( is_taking_photo_on_timer ) {
		if( this.isOnTimer() ) {
			takePictureTimerTask.cancel();
			if( beepTimerTask != null ) {
				beepTimerTask.cancel();
			}
			/*is_taking_photo_on_timer = false;
			is_taking_photo = false;*/
    		this.phase = PHASE_NORMAL;
			if( MyDebug.LOG )
				Log.d(TAG, "cancelled camera timer");
		}
		if( camera != null ) {
			if( video_recorder != null ) {
				stopVideo();
			}
			//camera.setPreviewCallback(null);
			this.setPreviewPaused(false);
			camera.stopPreview();
			/*this.is_taking_photo = false;
			this.is_taking_photo_on_timer = false;*/
    		this.phase = PHASE_NORMAL;
			this.is_preview_started = false;
			showGUI(true);
			camera.release();
			camera = null;
		}
	}
	
	private void openCamera() {
		long debug_time = 0;
		if( MyDebug.LOG ) {
			Log.d(TAG, "openCamera()");
			Log.d(TAG, "cameraId: " + cameraId);
			debug_time = System.currentTimeMillis();
		}
		// need to init everything now, in case we don't open the camera (but these may already be initialised from an earlier call - e.g., if we are now switching to another camera)
        has_set_location = false;
		has_focus_area = false;
		focus_success = FOCUS_DONE;
		scene_modes = null;
		has_zoom = false;
		zoom_factor = 0;
		max_zoom_factor = 0;
		zoom_ratios = null;
		faces_detected = null;
		supports_face_detection = false;
		using_face_detection = false;
		color_effects = null;
		white_balances = null;
		exposures = null;
		min_exposure = 0;
		max_exposure = 0;
		sizes = null;
		current_size_index = -1;
		video_quality = null;
		current_video_quality = -1;
		supported_flash_values = null;
		current_flash_index = -1;
		supported_focus_values = null;
		current_focus_index = -1;
		showGUI(true);
		if( !this.has_surface ) {
			if( MyDebug.LOG ) {
				Log.d(TAG, "preview surface not yet available");
			}
			return;
		}
		if( this.app_is_paused ) {
			if( MyDebug.LOG ) {
				Log.d(TAG, "don't open camera as app is paused");
			}
			return;
		}
		try {
			camera = Camera.open(cameraId);
		}
		catch(RuntimeException e) {
			if( MyDebug.LOG )
				Log.e(TAG, "Failed to open camera: " + e.getMessage());
			e.printStackTrace();
			camera = null;
		}
		if( MyDebug.LOG ) {
			//Log.d(TAG, "time after opening camera: " + (System.currentTimeMillis() - debug_time));
		}
		if( camera != null ) {
			Activity activity = (Activity)this.getContext();
	        this.setCameraDisplayOrientation(activity);
	        new OrientationEventListener(activity) {
				@Override
				public void onOrientationChanged(int orientation) {
					Preview.this.onOrientationChanged(orientation);
				}
	        }.enable();
			if( MyDebug.LOG ) {
				//Log.d(TAG, "time after setting orientation: " + (System.currentTimeMillis() - debug_time));
			}

			if( MyDebug.LOG )
				Log.d(TAG, "call setPreviewDisplay");
			try {
				camera.setPreviewDisplay(mHolder);
			}
			catch(IOException e) {
				if( MyDebug.LOG )
					Log.e(TAG, "Failed to set preview display: " + e.getMessage());
				e.printStackTrace();
			}
			if( MyDebug.LOG ) {
				//Log.d(TAG, "time after setting preview display: " + (System.currentTimeMillis() - debug_time));
			}

			SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getContext());

			Camera.Parameters parameters = camera.getParameters();

			// get available scene modes
			// note, important to set scene mode first - apparently this can affect the other supported features
			scene_modes = parameters.getSupportedSceneModes();
			String scene_mode = setupValuesPref(scene_modes, "preference_scene_mode", Camera.Parameters.SCENE_MODE_AUTO);
			if( scene_mode != null ) {
	        	parameters.setSceneMode(scene_mode);
			}

			this.has_zoom = parameters.isZoomSupported();
			if( MyDebug.LOG )
				Log.d(TAG, "has_zoom? " + has_zoom);
		    ZoomControls zoomControls = (ZoomControls) activity.findViewById(R.id.zoom);
		    SeekBar zoomSeekBar = (SeekBar) activity.findViewById(R.id.zoom_seekbar);
		    this.zoom_factor = 0;
			if( this.has_zoom ) {
				this.max_zoom_factor = parameters.getMaxZoom();
				try {
					this.zoom_ratios = parameters.getZoomRatios();
				}
				catch(NumberFormatException e) {
	        		// crash java.lang.NumberFormatException: Invalid int: " 500" reported in v1.4 on device "es209ra", Android 4.1, 3 Jan 2014
					// this is from java.lang.Integer.invalidInt(Integer.java:138) - unclear if this is a bug in Open Camera, all we can do for now is catch it
		    		if( MyDebug.LOG )
		    			Log.e(TAG, "NumberFormatException in getZoomRatios()");
					e.printStackTrace();
					this.has_zoom = false;
					this.zoom_ratios = null;
				}
			}

			if( this.has_zoom ) {
			    zoomControls.setIsZoomInEnabled(true);
		        zoomControls.setIsZoomOutEnabled(true);
		        zoomControls.setZoomSpeed(20);

		        zoomControls.setOnZoomInClickListener(new OnClickListener(){
		            public void onClick(View v){
		            	zoomIn();
		            }
		        });
			    zoomControls.setOnZoomOutClickListener(new OnClickListener(){
			    	public void onClick(View v){
			    		zoomOut();
			        }
			    });
				zoomControls.setVisibility(View.VISIBLE);
				
				zoomSeekBar.setMax(max_zoom_factor);
				zoomSeekBar.setProgress(max_zoom_factor-zoom_factor);
				zoomSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
					@Override
					public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
						zoomTo(max_zoom_factor-progress, false);
					}

					@Override
					public void onStartTrackingTouch(SeekBar seekBar) {
					}

					@Override
					public void onStopTrackingTouch(SeekBar seekBar) {
					}
				});
				zoomSeekBar.setVisibility(View.VISIBLE);
			}
			else {
				zoomControls.setVisibility(View.GONE);
				zoomSeekBar.setVisibility(View.GONE);
			}
			
			// get face detection supported
			this.faces_detected = null;
			this.supports_face_detection = parameters.getMaxNumDetectedFaces() > 0;
			if( this.supports_face_detection ) {
				this.using_face_detection = sharedPreferences.getBoolean("preference_face_detection", false);
			}
			else {
				this.using_face_detection = false;
			}
			if( MyDebug.LOG ) {
				Log.d(TAG, "supports_face_detection?: " + supports_face_detection);
				Log.d(TAG, "using_face_detection?: " + using_face_detection);
			}
			if( this.using_face_detection ) {
				class MyFaceDetectionListener implements Camera.FaceDetectionListener {
				    @Override
				    public void onFaceDetection(Face[] faces, Camera camera) {
				    	faces_detected = new Face[faces.length];
				    	System.arraycopy(faces, 0, faces_detected, 0, faces.length);				    	
				    }
				}
				camera.setFaceDetectionListener(new MyFaceDetectionListener());
			}

			// get available color effects
			color_effects = parameters.getSupportedColorEffects();
			String color_effect = setupValuesPref(color_effects, "preference_color_effect", Camera.Parameters.EFFECT_NONE);
			if( color_effect != null ) {
	        	parameters.setColorEffect(color_effect);
			}

			// get available white balances
			white_balances = parameters.getSupportedWhiteBalance();
			String white_balance = setupValuesPref(white_balances, "preference_white_balance", Camera.Parameters.WHITE_BALANCE_AUTO);
			if( white_balance != null ) {
	        	parameters.setWhiteBalance(white_balance);
			}

			// get min/max exposure
			exposures = null;
			min_exposure = parameters.getMinExposureCompensation();
			max_exposure = parameters.getMaxExposureCompensation();
			if( min_exposure != 0 || max_exposure != 0 ) {
				exposures = new Vector<String>();
				for(int i=min_exposure;i<=max_exposure;i++) {
					exposures.add("" + i);
				}
				String exposure_s = setupValuesPref(exposures, "preference_exposure", "0");
				if( exposure_s != null ) {
					try {
						int exposure = Integer.parseInt(exposure_s);
						if( MyDebug.LOG )
							Log.d(TAG, "exposure: " + exposure);
						parameters.setExposureCompensation(exposure);
					}
					catch(NumberFormatException exception) {
						if( MyDebug.LOG )
							Log.d(TAG, "exposure invalid format, can't parse to int");
					}
				}
			}
		    View exposureButton = (View) activity.findViewById(R.id.exposure);
		    exposureButton.setVisibility(exposures != null ? View.VISIBLE : View.GONE);

			// get available sizes
	        sizes = parameters.getSupportedPictureSizes();
			if( MyDebug.LOG ) {
				for(int i=0;i<sizes.size();i++) {
		        	Camera.Size size = sizes.get(i);
		        	Log.d(TAG, "supported picture size: " + size.width + " , " + size.height);
				}
			}
			current_size_index = -1;
			String resolution_value = sharedPreferences.getString(getResolutionPreferenceKey(cameraId), "");
			if( MyDebug.LOG )
				Log.d(TAG, "resolution_value: " + resolution_value);
			if( resolution_value.length() > 0 ) {
				// parse the saved size, and make sure it is still valid
				int index = resolution_value.indexOf(' ');
				if( index == -1 ) {
					if( MyDebug.LOG )
						Log.d(TAG, "resolution_value invalid format, can't find space");
				}
				else {
					String resolution_w_s = resolution_value.substring(0, index);
					String resolution_h_s = resolution_value.substring(index+1);
					if( MyDebug.LOG ) {
						Log.d(TAG, "resolution_w_s: " + resolution_w_s);
						Log.d(TAG, "resolution_h_s: " + resolution_h_s);
					}
					try {
						int resolution_w = Integer.parseInt(resolution_w_s);
						if( MyDebug.LOG )
							Log.d(TAG, "resolution_w: " + resolution_w);
						int resolution_h = Integer.parseInt(resolution_h_s);
						if( MyDebug.LOG )
							Log.d(TAG, "resolution_h: " + resolution_h);
						// now find size in valid list
						for(int i=0;i<sizes.size() && current_size_index==-1;i++) {
				        	Camera.Size size = sizes.get(i);
				        	if( size.width == resolution_w && size.height == resolution_h ) {
				        		current_size_index = i;
								if( MyDebug.LOG )
									Log.d(TAG, "set current_size_index to: " + current_size_index);
				        	}
						}
						if( current_size_index == -1 ) {
							if( MyDebug.LOG )
								Log.e(TAG, "failed to find valid size");
						}
					}
					catch(NumberFormatException exception) {
						if( MyDebug.LOG )
							Log.d(TAG, "resolution_value invalid format, can't parse w or h to int");
					}
				}
			}

			if( current_size_index == -1 ) {
				// set to largest
				Camera.Size current_size = null;
				for(int i=0;i<sizes.size();i++) {
		        	Camera.Size size = sizes.get(i);
		        	if( current_size == null || size.width*size.height > current_size.width*current_size.height ) {
		        		current_size_index = i;
		        		current_size = size;
		        	}
		        }
			}
			if( current_size_index != -1 ) {
				Camera.Size current_size = sizes.get(current_size_index);
	    		if( MyDebug.LOG )
	    			Log.d(TAG, "Current size index " + current_size_index + ": " + current_size.width + ", " + current_size.height);

	    		// now save, so it's available for PreferenceActivity
				resolution_value = current_size.width + " " + current_size.height;
				if( MyDebug.LOG ) {
					Log.d(TAG, "save new resolution_value: " + resolution_value);
				}
				SharedPreferences.Editor editor = sharedPreferences.edit();
				editor.putString(getResolutionPreferenceKey(cameraId), resolution_value);
				editor.apply();

				// now set the size
	        	parameters.setPictureSize(current_size.width, current_size.height);
			}
			
			
    		/*if( MyDebug.LOG )
    			Log.d(TAG, "Current image quality: " + parameters.getJpegQuality());*/
			int image_quality = getImageQuality();
			parameters.setJpegQuality(image_quality);
    		if( MyDebug.LOG )
    			Log.d(TAG, "image quality: " + image_quality);

    		if( MyDebug.LOG ) {
    			//Log.d(TAG, "time after reading camera parameters: " + (System.currentTimeMillis() - debug_time));
    		}

			// get available sizes
	        video_quality = new Vector<Integer>();
	        // if we add more, remember to update MyPreferenceActivity.onCreate() code
	        if( CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_1080P) )
	        	video_quality.add(CamcorderProfile.QUALITY_1080P);
	        if( CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_720P) )
	        	video_quality.add(CamcorderProfile.QUALITY_720P);
	        if( CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_480P) )
	        	video_quality.add(CamcorderProfile.QUALITY_480P);
	        if( CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_CIF) )
	        	video_quality.add(CamcorderProfile.QUALITY_CIF);
	        if( CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_QVGA) )
	        	video_quality.add(CamcorderProfile.QUALITY_QVGA);
	        if( CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_QCIF) )
	        	video_quality.add(CamcorderProfile.QUALITY_QCIF);
			if( MyDebug.LOG ) {
				for(int i=0;i<video_quality.size();i++) {
		        	Log.d(TAG, "supported video quality: " + video_quality.get(i).intValue());
				}
			}

			current_video_quality = -1;
			String video_quality_value_s = sharedPreferences.getString(getVideoQualityPreferenceKey(cameraId), "");
			if( MyDebug.LOG )
				Log.d(TAG, "video_quality_value: " + video_quality_value_s);
			if( video_quality_value_s.length() > 0 ) {
				// parse the saved video quality, and make sure it is still valid
				try {
					int video_quality_value = Integer.parseInt(video_quality_value_s);
					if( MyDebug.LOG )
						Log.d(TAG, "video_quality_value: " + video_quality_value);
					// now find value in valid list
					for(int i=0;i<video_quality.size() && current_video_quality==-1;i++) {
			        	Integer value = video_quality.get(i);
			        	if( value.intValue() == video_quality_value ) {
			        		current_video_quality = i;
							if( MyDebug.LOG )
								Log.d(TAG, "set current_video_quality to: " + current_video_quality);
			        	}
					}
					if( current_video_quality == -1 ) {
						if( MyDebug.LOG )
							Log.e(TAG, "failed to find valid video_quality");
					}
				}
				catch(NumberFormatException exception) {
					if( MyDebug.LOG )
						Log.d(TAG, "video_quality invalid format, can't parse to int");
				}
			}
			if( current_video_quality == -1 && video_quality.size() > 0 ) {
				// default to highest quality
				current_video_quality = 0;
				if( MyDebug.LOG )
					Log.d(TAG, "set video_quality value to " + video_quality.get(current_video_quality).intValue());
			}
			if( current_video_quality != -1 ) {
	    		// now save, so it's available for PreferenceActivity
				SharedPreferences.Editor editor = sharedPreferences.edit();
				editor.putString(getVideoQualityPreferenceKey(cameraId), "" + video_quality.get(current_video_quality).intValue());
				editor.apply();
			}

    		// update parameters
    		camera.setParameters(parameters);

    		// we do flash and focus after setting parameters, as these are done by calling separate functions, that themselves set the parameters directly
			List<String> supported_flash_modes = parameters.getSupportedFlashModes(); // Android format
		    View flashButton = (View) activity.findViewById(R.id.flash);
			current_flash_index = -1;
			if( supported_flash_modes != null && supported_flash_modes.size() > 1 ) {
				if( MyDebug.LOG )
					Log.d(TAG, "flash modes: " + supported_flash_modes);
				supported_flash_values = getSupportedFlashModes(supported_flash_modes); // convert to our format (also resorts)

				String flash_value = sharedPreferences.getString(getFlashPreferenceKey(cameraId), "");
				if( flash_value.length() > 0 ) {
					if( MyDebug.LOG )
						Log.d(TAG, "found existing flash_value: " + flash_value);
					if( !updateFlash(flash_value) ) {
						if( MyDebug.LOG )
							Log.d(TAG, "flash value no longer supported!");
						updateFlash(0);
					}
				}
				else {
					if( MyDebug.LOG )
						Log.d(TAG, "found no existing flash_value");
					updateFlash(0);
				}
			}
			else {
				if( MyDebug.LOG )
					Log.d(TAG, "flash not supported");
				supported_flash_values = null;
			}
			flashButton.setVisibility(supported_flash_values != null ? View.VISIBLE : View.GONE);

			List<String> supported_focus_modes = parameters.getSupportedFocusModes(); // Android format
		    View focusModeButton = (View) activity.findViewById(R.id.focus_mode);
			current_focus_index = -1;
			if( supported_focus_modes != null && supported_focus_modes.size() > 1 ) {
				if( MyDebug.LOG )
					Log.d(TAG, "focus modes: " + supported_focus_modes);
				supported_focus_values = getSupportedFocusModes(supported_focus_modes); // convert to our format (also resorts)

				String focus_value = sharedPreferences.getString(getFocusPreferenceKey(cameraId), "");
				if( focus_value.length() > 0 ) {
					if( MyDebug.LOG )
						Log.d(TAG, "found existing focus_value: " + focus_value);
					if( !updateFocus(focus_value, false, false) ) { // don't need to save, as this is the value that's already saved
						if( MyDebug.LOG )
							Log.d(TAG, "focus value no longer supported!");
						updateFocus(0, false, true);
					}
				}
				else {
					if( MyDebug.LOG )
						Log.d(TAG, "found no existing focus_value");
					updateFocus(0, false, true);
				}
			}
			else {
				if( MyDebug.LOG )
					Log.d(TAG, "focus not supported");
				supported_focus_values = null;
			}
			focusModeButton.setVisibility(supported_focus_values != null ? View.VISIBLE : View.GONE);
			
			// must be done after setting parameters, as this function may set parameters
			updateParametersFromLocation();

			// now switch to video if saved
			boolean saved_is_video = sharedPreferences.getBoolean(getIsVideoPreferenceKey(), false);
			if( MyDebug.LOG ) {
				Log.d(TAG, "saved_is_video: " + saved_is_video);
			}
			if( saved_is_video != this.is_video ) {
				this.switchVideo(false, false);
			}

			// Must set preview size before starting camera preview
			// and must do it after setting photo vs video mode
    		setPreviewSize(); // need to call this when we switch cameras, not just when we run for the first time
			// Must call startCameraPreview after checking if face detection is present - probably best to call it after setting all parameters that we want
			startCameraPreview();
			if( MyDebug.LOG ) {
				//Log.d(TAG, "time after starting camera preview: " + (System.currentTimeMillis() - debug_time));
			}

	    	final Handler handler = new Handler();
			handler.postDelayed(new Runnable() {
				@Override
				public void run() {
					tryAutoFocus(); // so we get the autofocus when starting up - we do this on a delay, as calling it immediately means the autofocus doesn't seem to work properly sometimes (at least on Galaxy Nexus)
				}
			}, 500);
		}

		if( MyDebug.LOG ) {
			Log.d(TAG, "total time: " + (System.currentTimeMillis() - debug_time));
		}
	}

	private String setupValuesPref(List<String> values, String key, String default_value) {
		if( MyDebug.LOG )
			Log.d(TAG, "setupValuesPref, key: " + key);
		if( values != null && values.size() > 0 ) {
			if( MyDebug.LOG ) {
				for(int i=0;i<values.size();i++) {
		        	Log.d(TAG, "supported value: " + values.get(i));
				}
			}
			SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getContext());
			String value = sharedPreferences.getString(key, default_value);
			if( MyDebug.LOG )
				Log.d(TAG, "value: " + value);
			// make sure result is valid
			if( !values.contains(value) ) {
				if( MyDebug.LOG )
					Log.d(TAG, "value not valid!");
				if( values.contains(default_value) )
					value = default_value;
				else
					value = values.get(0);
				if( MyDebug.LOG )
					Log.d(TAG, "value is now: " + value);
			}

    		// now save, so it's available for PreferenceActivity
			SharedPreferences.Editor editor = sharedPreferences.edit();
			editor.putString(key, value);
			editor.apply();

        	return value;
		}
		else {
			if( MyDebug.LOG )
				Log.d(TAG, "values not supported");
			return null;
		}
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		if( MyDebug.LOG )
			Log.d(TAG, "surfaceChanged " + w + ", " + h);
		// surface size is now changed to match the aspect ratio of camera preview - so we shouldn't change the preview to match the surface size, so no need to restart preview here

        if( mHolder.getSurface() == null ) {
            // preview surface does not exist
            return;
        }
        if( camera == null ) {
            return;
        }

		MainActivity main_activity = (MainActivity)Preview.this.getContext();
		main_activity.layoutUI(); // need to force a layoutUI update (e.g., so UI is oriented correctly when app goes idle, device is then rotated, and app is then resumed
	}
	
	private void setPreviewSize() {
		if( MyDebug.LOG )
			Log.d(TAG, "setPreviewSize()");
		if( camera == null ) {
			return;
		}
		if( is_preview_started ) {
			if( MyDebug.LOG )
				Log.d(TAG, "setPreviewSize() shouldn't be called when preview is running");
			throw new RuntimeException();
		}
		// set optimal preview size
    	Camera.Parameters parameters = camera.getParameters();
		if( MyDebug.LOG )
			Log.d(TAG, "current preview size: " + parameters.getPreviewSize().width + ", " + parameters.getPreviewSize().height);
        List<Camera.Size> preview_sizes = parameters.getSupportedPreviewSizes();
        if( preview_sizes.size() > 0 ) {
	        /*Camera.Size best_size = preview_sizes.get(0);
	        for(Camera.Size size : preview_sizes) {
	    		if( MyDebug.LOG )
	    			Log.d(TAG, "    supported preview size: " + size.width + ", " + size.height);
	        	if( size.width*size.height > best_size.width*best_size.height ) {
	        		best_size = size;
	        	}
	        }*/
        	Camera.Size best_size = getOptimalPreviewSize(preview_sizes);
            parameters.setPreviewSize(best_size.width, best_size.height);
    		if( MyDebug.LOG )
    			Log.d(TAG, "new preview size: " + parameters.getPreviewSize().width + ", " + parameters.getPreviewSize().height);
    		this.setAspectRatio( ((double)parameters.getPreviewSize().width) / (double)parameters.getPreviewSize().height );

    		/*List<int []> fps_ranges = parameters.getSupportedPreviewFpsRange();
    		if( MyDebug.LOG ) {
		        for(int [] fps_range : fps_ranges) {
	    			Log.d(TAG, "    supported fps range: " + fps_range[Camera.Parameters.PREVIEW_FPS_MIN_INDEX] + " to " + fps_range[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]);
		        }
    		}
    		int [] fps_range = fps_ranges.get(fps_ranges.size()-1);
	        parameters.setPreviewFpsRange(fps_range[Camera.Parameters.PREVIEW_FPS_MIN_INDEX], fps_range[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]);*/
            camera.setParameters(parameters);
        }
	}
	
	public CamcorderProfile getCamcorderProfile() {
    	CamcorderProfile profile = CamcorderProfile.get(this.cameraId, current_video_quality != -1 ? video_quality.get(current_video_quality).intValue() : CamcorderProfile.QUALITY_HIGH);
    	return profile;
	}

	public double getTargetRatio(Point display_size) {
        double targetRatio = 0.0f;
		Activity activity = (Activity)this.getContext();
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
		String preview_size = sharedPreferences.getString("preference_preview_size", "preference_preview_size_display");
		if( preview_size.equals("preference_preview_size_wysiwyg") ) {
	        if( this.is_video ) {
	        	if( MyDebug.LOG )
	        		Log.d(TAG, "set preview aspect ratio from photo size (wysiwyg)");
	        	CamcorderProfile profile = getCamcorderProfile();
	        	if( MyDebug.LOG )
	        		Log.d(TAG, "video size: " + profile.videoFrameWidth + " x " + profile.videoFrameHeight);
	        	targetRatio = ((double)profile.videoFrameWidth) / (double)profile.videoFrameHeight;
	        }
	        else {
	        	if( MyDebug.LOG )
	        		Log.d(TAG, "set preview aspect ratio from video size (wysiwyg)");
	        	Camera.Parameters parameters = camera.getParameters();
	        	Camera.Size picture_size = parameters.getPictureSize();
	        	if( MyDebug.LOG )
	        		Log.d(TAG, "picture_size: " + picture_size.width + " x " + picture_size.height);
	        	targetRatio = ((double)picture_size.width) / (double)picture_size.height;
	        }
		}
		else {
        	if( MyDebug.LOG )
        		Log.d(TAG, "set preview aspect ratio from display size");
        	// base target ratio from display size - means preview will fill the device's display as much as possible
        	// but if the preview's aspect ratio differs from the actual photo/video size, the preview will show a cropped version of what is actually taken
            targetRatio = ((double)display_size.x) / (double)display_size.y;
		}
		if( MyDebug.LOG )
			Log.d(TAG, "targetRatio: " + targetRatio);
		return targetRatio;
	}
	
	public Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes) {
		if( MyDebug.LOG )
			Log.d(TAG, "getOptimalPreviewSize()");
		final double ASPECT_TOLERANCE = 0.05;
        if( sizes == null )
        	return null;
        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;
        Point display_size = new Point();
		Activity activity = (Activity)this.getContext();
        {
            Display display = activity.getWindowManager().getDefaultDisplay();
            display.getSize(display_size);
    		if( MyDebug.LOG )
    			Log.d(TAG, "display_size: " + display_size.x + " x " + display_size.y);
        }
        double targetRatio = getTargetRatio(display_size);
        int targetHeight = Math.min(display_size.y, display_size.x);
        if( targetHeight <= 0 ) {
            targetHeight = display_size.y;
        }
        // Try to find an size match aspect ratio and size
        for(Camera.Size size : sizes) {
    		if( MyDebug.LOG )
    			Log.d(TAG, "    supported preview size: " + size.width + ", " + size.height);
            double ratio = (double)size.width / size.height;
            if( Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE )
            	continue;
            if( Math.abs(size.height - targetHeight) < minDiff ) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }
        if( optimalSize == null ) {
        	// can't find match for aspect ratio, so find closest one
    		if( MyDebug.LOG )
    			Log.d(TAG, "no preview size matches the aspect ratio");
            minDiff = Double.MAX_VALUE;
            for(Camera.Size size : sizes) {
                double ratio = (double)size.width / size.height;
                if( Math.abs(ratio - targetRatio) < minDiff ) {
                    optimalSize = size;
                    minDiff = Math.abs(ratio - targetRatio);
                }
            }
        }
		if( MyDebug.LOG ) {
			Log.d(TAG, "chose optimalSize: " + optimalSize.width + " x " + optimalSize.height);
			Log.d(TAG, "optimalSize ratio: " + ((double)optimalSize.width / optimalSize.height));
		}
        return optimalSize;
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
    	if( !this.has_aspect_ratio ) {
    		super.onMeasure(widthSpec, heightSpec);
    		return;
    	}
        int previewWidth = MeasureSpec.getSize(widthSpec);
        int previewHeight = MeasureSpec.getSize(heightSpec);

        // Get the padding of the border background.
        int hPadding = getPaddingLeft() + getPaddingRight();
        int vPadding = getPaddingTop() + getPaddingBottom();

        // Resize the preview frame with correct aspect ratio.
        previewWidth -= hPadding;
        previewHeight -= vPadding;

        boolean widthLonger = previewWidth > previewHeight;
        int longSide = (widthLonger ? previewWidth : previewHeight);
        int shortSide = (widthLonger ? previewHeight : previewWidth);
        if (longSide > shortSide * aspect_ratio) {
            longSide = (int) ((double) shortSide * aspect_ratio);
        } else {
            shortSide = (int) ((double) longSide / aspect_ratio);
        }
        if (widthLonger) {
            previewWidth = longSide;
            previewHeight = shortSide;
        } else {
            previewWidth = shortSide;
            previewHeight = longSide;
        }


        // Add the padding of the border.
        previewWidth += hPadding;
        previewHeight += vPadding;

        // Ask children to follow the new preview dimension.
        super.onMeasure(MeasureSpec.makeMeasureSpec(previewWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(previewHeight, MeasureSpec.EXACTLY));
    }

    private void setAspectRatio(double ratio) {
        if( ratio <= 0.0 )
        	throw new IllegalArgumentException();

        has_aspect_ratio = true;
        if( aspect_ratio != ratio ) {
        	aspect_ratio = ratio;
    		if( MyDebug.LOG )
    			Log.d(TAG, "new aspect ratio: " + aspect_ratio);
            requestLayout();
        }
    }

    // for the Preview - from http://developer.android.com/reference/android/hardware/Camera.html#setDisplayOrientation(int)
	// note, if orientation is locked to landscape this is only called when setting up the activity, and will always have the same orientation
	private void setCameraDisplayOrientation(Activity activity) {
		if( MyDebug.LOG )
			Log.d(TAG, "setCameraDisplayOrientation()");
	    Camera.CameraInfo info = new Camera.CameraInfo();
	    Camera.getCameraInfo(cameraId, info);
	    int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
	    int degrees = 0;
	    switch (rotation) {
	    	case Surface.ROTATION_0: degrees = 0; break;
	        case Surface.ROTATION_90: degrees = 90; break;
	        case Surface.ROTATION_180: degrees = 180; break;
	        case Surface.ROTATION_270: degrees = 270; break;
	    }
		if( MyDebug.LOG )
			Log.d(TAG, "    degrees = " + degrees);

	    int result = 0;
	    if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
	        result = (info.orientation + degrees) % 360;
	        result = (360 - result) % 360;  // compensate the mirror
	    }
	    else {  // back-facing
	        result = (info.orientation - degrees + 360) % 360;
	    }
		if( MyDebug.LOG ) {
			Log.d(TAG, "    info orientation is " + info.orientation);
			Log.d(TAG, "    setDisplayOrientation to " + result);
		}
	    camera.setDisplayOrientation(result);
	    this.display_orientation = result;
	}
	
	// for taking photos - from http://developer.android.com/reference/android/hardware/Camera.Parameters.html#setRotation(int)
	private void onOrientationChanged(int orientation) {
		/*if( MyDebug.LOG ) {
			Log.d(TAG, "onOrientationChanged()");
			Log.d(TAG, "orientation: " + orientation);
		}*/
		if( orientation == OrientationEventListener.ORIENTATION_UNKNOWN )
			return;
		if( camera == null )
			return;
	    Camera.getCameraInfo(cameraId, camera_info);
	    orientation = (orientation + 45) / 90 * 90;
	    this.current_orientation = orientation % 360;
	    int new_rotation = 0;
	    if (camera_info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
	    	new_rotation = (camera_info.orientation - orientation + 360) % 360;
	    }
	    else {  // back-facing camera
	    	new_rotation = (camera_info.orientation + orientation) % 360;
	    }
	    if( new_rotation != current_rotation ) {
			/*if( MyDebug.LOG ) {
				Log.d(TAG, "    current_orientation is " + current_orientation);
				Log.d(TAG, "    info orientation is " + camera_info.orientation);
				Log.d(TAG, "    set Camera rotation from " + current_rotation + " to " + new_rotation);
			}*/
	    	this.current_rotation = new_rotation;
	    }
	 }

	@Override
	public void onDraw(Canvas canvas) {
		/*if( MyDebug.LOG )
			Log.d(TAG, "onDraw()");*/
		if( this.app_is_paused ) {
    		/*if( MyDebug.LOG )
    			Log.d(TAG, "onDraw(): app is paused");*/
			return;
		}
		/*if( true ) // test
			return;*/
		/*if( MyDebug.LOG )
			Log.d(TAG, "ui_rotation: " + ui_rotation);*/

		MainActivity main_activity = (MainActivity)this.getContext();
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getContext());
		if( camera != null && sharedPreferences.getBoolean("preference_grid_3x3", false) ) {
			p.setColor(Color.WHITE);
			canvas.drawLine(canvas.getWidth()/3.0f, 0.0f, canvas.getWidth()/3.0f, canvas.getHeight()-1.0f, p);
			canvas.drawLine(2.0f*canvas.getWidth()/3.0f, 0.0f, 2.0f*canvas.getWidth()/3.0f, canvas.getHeight()-1.0f, p);
			canvas.drawLine(0.0f, canvas.getHeight()/3.0f, canvas.getWidth()-1.0f, canvas.getHeight()/3.0f, p);
			canvas.drawLine(0.0f, 2.0f*canvas.getHeight()/3.0f, canvas.getWidth()-1.0f, 2.0f*canvas.getHeight()/3.0f, p);
		}

		// note, no need to check preferences here, as we do that when setting thumbnail_anim
		if( camera != null && this.thumbnail_anim && this.thumbnail != null ) {
			long time = System.currentTimeMillis() - this.thumbnail_anim_start_ms;
			final long duration = 500;
			if( time > duration ) {
				this.thumbnail_anim = false;
			}
			else {
				thumbnail_anim_src_rect.left = 0;
				thumbnail_anim_src_rect.top = 0;
				thumbnail_anim_src_rect.right = this.thumbnail.getWidth();
				thumbnail_anim_src_rect.bottom = this.thumbnail.getHeight();
			    View galleryButton = (View) main_activity.findViewById(R.id.gallery);
				float alpha = ((float)time)/(float)duration;

				int st_x = canvas.getWidth()/2;
				int st_y = canvas.getHeight()/2;
				int nd_x = galleryButton.getLeft() + galleryButton.getWidth()/2;
				int nd_y = galleryButton.getTop() + galleryButton.getHeight()/2;
				int thumbnail_x = (int)( (1.0f-alpha)*st_x + alpha*nd_x );
				int thumbnail_y = (int)( (1.0f-alpha)*st_y + alpha*nd_y );

				float st_w = canvas.getWidth();
				float st_h = canvas.getHeight();
				float nd_w = galleryButton.getWidth();
				float nd_h = galleryButton.getHeight();
				//int thumbnail_w = (int)( (1.0f-alpha)*st_w + alpha*nd_w );
				//int thumbnail_h = (int)( (1.0f-alpha)*st_h + alpha*nd_h );
				float correction_w = st_w/nd_w - 1.0f;
				float correction_h = st_h/nd_h - 1.0f;
				int thumbnail_w = (int)(st_w/(1.0f+alpha*correction_w));
				int thumbnail_h = (int)(st_h/(1.0f+alpha*correction_h));
				thumbnail_anim_dst_rect.left = thumbnail_x - thumbnail_w/2;
				thumbnail_anim_dst_rect.top = thumbnail_y - thumbnail_h/2;
				thumbnail_anim_dst_rect.right = thumbnail_x + thumbnail_w/2;
				thumbnail_anim_dst_rect.bottom = thumbnail_y + thumbnail_h/2;
				//canvas.drawBitmap(this.thumbnail, thumbnail_anim_src_rect, thumbnail_anim_dst_rect, p);
				thumbnail_anim_matrix.setRectToRect(thumbnail_anim_src_rect, thumbnail_anim_dst_rect, Matrix.ScaleToFit.FILL);
				//thumbnail_anim_matrix.reset();
				if( ui_rotation == 90 || ui_rotation == 270 ) {
					float ratio = ((float)thumbnail.getWidth())/(float)thumbnail.getHeight();
					thumbnail_anim_matrix.preScale(ratio, 1.0f/ratio, thumbnail.getWidth()/2, thumbnail.getHeight()/2);
				}
				thumbnail_anim_matrix.preRotate(ui_rotation, thumbnail.getWidth()/2, thumbnail.getHeight()/2);
				canvas.drawBitmap(this.thumbnail, thumbnail_anim_matrix, p);
			}
		}
		
		canvas.save();
		canvas.rotate(ui_rotation, canvas.getWidth()/2, canvas.getHeight()/2);

		final float scale = getResources().getDisplayMetrics().density;
		int text_y = (int) (20 * scale + 0.5f); // convert dps to pixels
		// fine tuning to adjust placement of text with respect to the GUI, depending on orientation
		int text_base_y = 0;
		if( ui_rotation == 0 ) {
			text_base_y = canvas.getHeight() - (int)(0.5*text_y);
		}
		else if( ui_rotation == 180 ) {
			text_base_y = canvas.getHeight() - (int)(2.5*text_y);
		}
		else if( ui_rotation == 90 || ui_rotation == 270 ) {
			//text_base_y = canvas.getHeight() + (int)(0.5*text_y);
			ImageButton view = (ImageButton)main_activity.findViewById(R.id.take_photo);
			// align with "top" of the take_photo button, but remember to take the rotation into account!
			view.getLocationOnScreen(gui_location);
			int view_left = gui_location[0];
			this.getLocationOnScreen(gui_location);
			int this_left = gui_location[0];
			int diff_x = view_left - ( this_left + canvas.getWidth()/2 );
    		/*if( MyDebug.LOG ) {
    			Log.d(TAG, "view left: " + view_left);
    			Log.d(TAG, "this left: " + this_left);
    			Log.d(TAG, "canvas is " + canvas.getWidth() + " x " + canvas.getHeight());
    		}*/
			int max_x = canvas.getWidth();
			if( ui_rotation == 90 ) {
				// so we don't interfere with the top bar info (time, etc)
				max_x -= (int)(1.5*text_y);
			}
			if( canvas.getWidth()/2 + diff_x > max_x ) {
				// in case goes off the size of the canvas, for "black bar" cases (when preview aspect ratio != screen aspect ratio)
				diff_x = max_x - canvas.getWidth()/2;
			}
			text_base_y = canvas.getHeight()/2 + diff_x - (int)(0.5*text_y);
		}

		if( camera != null && this.phase != PHASE_PREVIEW_PAUSED ) {
			/*canvas.drawText("PREVIEW", canvas.getWidth() / 2,
					canvas.getHeight() / 2, p);*/
			boolean draw_angle = this.has_level_angle && sharedPreferences.getBoolean("preference_show_angle", true);
			boolean draw_geo_direction = this.has_geo_direction && sharedPreferences.getBoolean("preference_show_geo_direction", true);
			if( draw_angle ) {
				int color = Color.WHITE;
				p.setTextSize(14 * scale + 0.5f); // convert dps to pixels
				int pixels_offset_x = 0;
				if( draw_geo_direction ) {
					pixels_offset_x = - (int) (80 * scale + 0.5f); // convert dps to pixels
					p.setTextAlign(Paint.Align.LEFT);
				}
				else {
					p.setTextAlign(Paint.Align.CENTER);
				}
				if( Math.abs(this.level_angle) <= 1.0 ) {
					color = Color.GREEN;
				}
				String string = "Angle: " + decimalFormat.format(this.level_angle) + (char)0x00B0;
				drawTextWithBackground(canvas, p, string, color, Color.BLACK, canvas.getWidth() / 2 + pixels_offset_x, text_base_y);
			}
			if( draw_geo_direction ) {
				int color = Color.WHITE;
				p.setTextSize(14 * scale + 0.5f); // convert dps to pixels
				if( draw_angle ) {
					p.setTextAlign(Paint.Align.LEFT);
				}
				else {
					p.setTextAlign(Paint.Align.CENTER);
				}
				float geo_angle = (float)Math.toDegrees(this.geo_direction[0]);
				if( geo_angle < 0.0f ) {
					geo_angle += 360.0f;
				}
				String string = " Direction: " + Math.round(geo_angle) + (char)0x00B0;
				drawTextWithBackground(canvas, p, string, color, Color.BLACK, canvas.getWidth() / 2, text_base_y);
			}
			//if( this.is_taking_photo_on_timer ) {
			if( this.isOnTimer() ) {
				long remaining_time = (take_photo_time - System.currentTimeMillis() + 999)/1000;
				if( MyDebug.LOG )
					Log.d(TAG, "remaining_time: " + remaining_time);
				if( remaining_time >= 0 ) {
					p.setTextSize(42 * scale + 0.5f); // convert dps to pixels
					p.setTextAlign(Paint.Align.CENTER);
					drawTextWithBackground(canvas, p, "" + remaining_time, Color.RED, Color.rgb(75, 75, 75), canvas.getWidth() / 2, canvas.getHeight() / 2);
				}
			}
			else if( this.video_recorder != null && video_start_time_set ) {
            	long video_time = (System.currentTimeMillis() - video_start_time);
            	//int ms = (int)(video_time % 1000);
            	video_time /= 1000;
            	int secs = (int)(video_time % 60);
            	video_time /= 60;
            	int mins = (int)(video_time % 60);
            	video_time /= 60;
            	long hours = video_time;
            	//String time_s = hours + ":" + String.format("%02d", mins) + ":" + String.format("%02d", secs) + ":" + String.format("%03d", ms);
            	String time_s = hours + ":" + String.format("%02d", mins) + ":" + String.format("%02d", secs);
            	/*if( MyDebug.LOG )
					Log.d(TAG, "video_time: " + video_time + " " + time_s);*/
    			p.setTextSize(24 * scale + 0.5f); // convert dps to pixels
    			p.setTextAlign(Paint.Align.CENTER);
    			int pixels_offset_y = (int) (164 * scale + 0.5f); // convert dps to pixels
				drawTextWithBackground(canvas, p, "" + time_s, Color.RED, Color.BLACK, canvas.getWidth() / 2, canvas.getHeight() - pixels_offset_y);
			}
		}
		else if( camera == null ) {
			/*if( MyDebug.LOG ) {
				Log.d(TAG, "no camera!");
				Log.d(TAG, "width " + canvas.getWidth() + " height " + canvas.getHeight());
			}*/
			p.setColor(Color.WHITE);
			p.setTextSize(14 * scale + 0.5f); // convert dps to pixels
			p.setTextAlign(Paint.Align.CENTER);
			int pixels_offset = (int) (20 * scale + 0.5f); // convert dps to pixels
			canvas.drawText("FAILED TO OPEN CAMERA.", canvas.getWidth() / 2, canvas.getHeight() / 2, p);
			canvas.drawText("CAMERA MAY BE IN USE", canvas.getWidth() / 2, canvas.getHeight() / 2 + pixels_offset, p);
			canvas.drawText("BY ANOTHER APPLICATION?", canvas.getWidth() / 2, canvas.getHeight() / 2 + 2*pixels_offset, p);
			//canvas.drawRect(0.0f, 0.0f, 100.0f, 100.0f, p);
			//canvas.drawRGB(255, 0, 0);
			//canvas.drawRect(0.0f, 0.0f, canvas.getWidth(), canvas.getHeight(), p);
		}
		if( this.has_zoom && camera != null && sharedPreferences.getBoolean("preference_show_zoom", true) ) {
			float zoom_ratio = this.zoom_ratios.get(zoom_factor)/100.0f;
			// only show when actually zoomed in
			if( zoom_ratio > 1.0f + 1.0e-5f ) {
				// Convert the dps to pixels, based on density scale
				int pixels_offset_y = 2*text_y;
				p.setTextSize(14 * scale + 0.5f); // convert dps to pixels
				p.setTextAlign(Paint.Align.CENTER);
				drawTextWithBackground(canvas, p, "Zoom: " + zoom_ratio +"x", Color.WHITE, Color.BLACK, canvas.getWidth() / 2, text_base_y - pixels_offset_y);
			}
		}
		if( camera != null && sharedPreferences.getBoolean("preference_free_memory", true) ) {
			int pixels_offset_y = 1*text_y;
			p.setTextSize(14 * scale + 0.5f); // convert dps to pixels
			p.setTextAlign(Paint.Align.CENTER);
			long time_now = System.currentTimeMillis();
			if( free_memory_gb < 0.0f || time_now > last_free_memory_time + 1000 ) {
				long free_mb = main_activity.freeMemory();
				if( free_mb >= 0 ) {
					free_memory_gb = free_mb/1024.0f;
					last_free_memory_time = time_now;
				}
			}
			if( free_memory_gb >= 0.0f ) {
				drawTextWithBackground(canvas, p, "Free memory: " + decimalFormat.format(free_memory_gb) + "GB", Color.WHITE, Color.BLACK, canvas.getWidth() / 2, text_base_y - pixels_offset_y);
			}
		}
		
		{
			if( !this.has_battery_frac || System.currentTimeMillis() > this.last_battery_time + 60000 ) {
				// only check periodically - unclear if checking is costly in any way
				Intent batteryStatus = main_activity.registerReceiver(null, battery_ifilter);
				int battery_level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
				int battery_scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
				has_battery_frac = true;
				battery_frac = battery_level/(float)battery_scale;
				last_battery_time = System.currentTimeMillis();
				if( MyDebug.LOG )
					Log.d(TAG, "Battery status is " + battery_level + " / " + battery_scale + " : " + battery_frac);
			}
			//battery_frac = 0.2999f; // test
			int battery_x = (int) (5 * scale + 0.5f); // convert dps to pixels
			int battery_y = battery_x;
			int battery_width = (int) (5 * scale + 0.5f); // convert dps to pixels
			int battery_height = 4*battery_width;
			if( ui_rotation == 90 || ui_rotation == 270 ) {
				int diff = canvas.getWidth() - canvas.getHeight();
				battery_x += diff/2;
				battery_y -= diff/2;
			}
			if( ui_rotation == 90 ) {
				battery_y = canvas.getHeight() - battery_y - battery_height;
			}
			if( ui_rotation == ( ui_placement_right ? 180 : 0 ) ) {
				battery_x = canvas.getWidth() - battery_x - battery_width;
			}
			p.setColor(Color.WHITE);
			p.setStyle(Paint.Style.STROKE);
			canvas.drawRect(battery_x, battery_y, battery_x+battery_width, battery_y+battery_height, p);
			p.setColor(battery_frac >= 0.3f ? Color.GREEN : Color.RED);
			p.setStyle(Paint.Style.FILL);
			canvas.drawRect(battery_x+1, battery_y+1+(1.0f-battery_frac)*(battery_height-2), battery_x+battery_width-1, battery_y+battery_height-1, p);
		}
		
		boolean store_location = sharedPreferences.getBoolean("preference_location", false);
		if( store_location && has_set_location ) {
			int location_x = (int) (20 * scale + 0.5f); // convert dps to pixels
			int location_y = (int) (5 * scale + 0.5f); // convert dps to pixels
			int location_size = (int) (20 * scale + 0.5f); // convert dps to pixels
			if( ui_rotation == 90 || ui_rotation == 270 ) {
				int diff = canvas.getWidth() - canvas.getHeight();
				location_x += diff/2;
				location_y -= diff/2;
			}
			if( ui_rotation == 90 ) {
				location_y = canvas.getHeight() - location_y - location_size;
			}
			if( ui_rotation == ( ui_placement_right ? 180 : 0 ) ) {
				location_x = canvas.getWidth() - location_x - location_size;
			}
			location_dest.set(location_x, location_y, location_x + location_size, location_y + location_size);
			canvas.drawBitmap(location_bitmap, null, location_dest, p);
		}
		
		{
			p.setTextSize(14 * scale + 0.5f); // convert dps to pixels
			p.setTextAlign(Paint.Align.LEFT);
			int location_x = (int) (50 * scale + 0.5f); // convert dps to pixels
			int location_y = (int) (15 * scale + 0.5f); // convert dps to pixels
			if( ui_rotation == 90 || ui_rotation == 270 ) {
				int diff = canvas.getWidth() - canvas.getHeight();
				location_x += diff/2;
				location_y -= diff/2;
			}
			if( ui_rotation == 90 ) {
				location_y = canvas.getHeight() - location_y;
			}
			if( ui_rotation == ( ui_placement_right ? 180 : 0 ) ) {
				location_x = canvas.getWidth() - location_x;
				p.setTextAlign(Paint.Align.RIGHT);
			}
	        Calendar c = Calendar.getInstance();
	        String current_time = DateFormat.getTimeInstance().format(c.getTime());
	        drawTextWithBackground(canvas, p, current_time, Color.WHITE, Color.BLACK, location_x, location_y);
	    }

		canvas.restore();
		
		if( this.focus_success != FOCUS_DONE ) {
			int size = (int) (50 * scale + 0.5f); // convert dps to pixels
			if( this.focus_success == FOCUS_SUCCESS )
				p.setColor(Color.GREEN);
			else if( this.focus_success == FOCUS_FAILED )
				p.setColor(Color.RED);
			else
				p.setColor(Color.WHITE);
			p.setStyle(Paint.Style.STROKE);
			int pos_x = 0;
			int pos_y = 0;
			if( has_focus_area ) {
				pos_x = focus_screen_x;
				pos_y = focus_screen_y;
			}
			else {
				pos_x = canvas.getWidth() / 2;
				pos_y = canvas.getHeight() / 2;
			}
			canvas.drawRect(pos_x - size, pos_y - size, pos_x + size, pos_y + size, p);
			if( focus_complete_time != -1 && System.currentTimeMillis() > focus_complete_time + 1000 ) {
				focus_success = FOCUS_DONE;
			}
			p.setStyle(Paint.Style.FILL); // reset
		}
		if( this.using_face_detection && this.faces_detected != null ) {
			p.setColor(Color.YELLOW);
			p.setStyle(Paint.Style.STROKE);
			for(Face face : faces_detected) {
				// Android doc recommends filtering out faces with score less than 50
				if( face.score >= 50 ) {
					calculateCameraToPreviewMatrix();
					face_rect.set(face.rect);
					this.camera_to_preview_matrix.mapRect(face_rect);
					/*int eye_radius = (int) (5 * scale + 0.5f); // convert dps to pixels
					int mouth_radius = (int) (10 * scale + 0.5f); // convert dps to pixels
					float [] top_left = {face.rect.left, face.rect.top};
					float [] bottom_right = {face.rect.right, face.rect.bottom};
					canvas.drawRect(top_left[0], top_left[1], bottom_right[0], bottom_right[1], p);*/
					canvas.drawRect(face_rect, p);
					/*if( face.leftEye != null ) {
						float [] left_point = {face.leftEye.x, face.leftEye.y};
						cameraToPreview(left_point);
						canvas.drawCircle(left_point[0], left_point[1], eye_radius, p);
					}
					if( face.rightEye != null ) {
						float [] right_point = {face.rightEye.x, face.rightEye.y};
						cameraToPreview(right_point);
						canvas.drawCircle(right_point[0], right_point[1], eye_radius, p);
					}
					if( face.mouth != null ) {
						float [] mouth_point = {face.mouth.x, face.mouth.y};
						cameraToPreview(mouth_point);
						canvas.drawCircle(mouth_point[0], mouth_point[1], mouth_radius, p);
					}*/
				}
			}
			p.setStyle(Paint.Style.FILL); // reset
		}
	}

	private void drawTextWithBackground(Canvas canvas, Paint paint, String text, int foreground, int background, int location_x, int location_y) {
		final float scale = getResources().getDisplayMetrics().density;
		p.setStyle(Paint.Style.FILL);
		paint.setColor(background);
		paint.setAlpha(127);
		paint.getTextBounds(text, 0, text.length(), text_bounds);
		final int padding = (int) (2 * scale + 0.5f); // convert dps to pixels
		if( paint.getTextAlign() == Paint.Align.RIGHT || paint.getTextAlign() == Paint.Align.CENTER ) {
			float width = paint.measureText(text); // n.b., need to use measureText rather than getTextBounds here
			/*if( MyDebug.LOG )
				Log.d(TAG, "width: " + width);*/
			if( paint.getTextAlign() == Paint.Align.CENTER )
				width /= 2.0f;
			text_bounds.left -= width;
			text_bounds.right -= width;
		}
		/*if( MyDebug.LOG )
			Log.d(TAG, "text_bounds left-right: " + text_bounds.left + " , " + text_bounds.right);*/
		text_bounds.left += location_x - padding;
		text_bounds.top += location_y - padding;
		text_bounds.right += location_x + padding;
		text_bounds.bottom += location_y + padding;
		canvas.drawRect(text_bounds, paint);
		paint.setColor(foreground);
		canvas.drawText(text, location_x, location_y, paint);
	}

	public void scaleZoom(float scale_factor) {
		if( MyDebug.LOG )
			Log.d(TAG, "scaleZoom() " + scale_factor);
		if( this.camera != null && this.has_zoom ) {
			float zoom_ratio = this.zoom_ratios.get(zoom_factor)/100.0f;
			zoom_ratio *= scale_factor;

			int new_zoom_factor = zoom_factor;
			if( zoom_ratio <= 1.0f ) {
				new_zoom_factor = 0;
			}
			else if( zoom_ratio >= zoom_ratios.get(max_zoom_factor)/100.0f ) {
				new_zoom_factor = max_zoom_factor;
			}
			else {
				// find the closest zoom level
				if( scale_factor > 1.0f ) {
					// zooming in
    				for(int i=zoom_factor;i<zoom_ratios.size();i++) {
    					if( zoom_ratios.get(i)/100.0f >= zoom_ratio ) {
    						if( MyDebug.LOG )
    							Log.d(TAG, "zoom int, found new zoom by comparing " + zoom_ratios.get(i)/100.0f + " >= " + zoom_ratio);
    						new_zoom_factor = i;
    						break;
    					}
    				}
				}
				else {
					// zooming out
    				for(int i=zoom_factor;i>=0;i--) {
    					if( zoom_ratios.get(i)/100.0f <= zoom_ratio ) {
    						if( MyDebug.LOG )
    							Log.d(TAG, "zoom out, found new zoom by comparing " + zoom_ratios.get(i)/100.0f + " <= " + zoom_ratio);
    						new_zoom_factor = i;
    						break;
    					}
    				}
				}
			}
			if( MyDebug.LOG ) {
				Log.d(TAG, "ScaleListener.onScale zoom_ratio is now " + zoom_ratio);
				Log.d(TAG, "    old zoom_factor " + zoom_factor + " ratio " + zoom_ratios.get(zoom_factor)/100.0f);
				Log.d(TAG, "    chosen new zoom_factor " + new_zoom_factor + " ratio " + zoom_ratios.get(new_zoom_factor)/100.0f);
			}
			zoomTo(new_zoom_factor, true);
		}
	}
	
	public void zoomIn() {
		if( MyDebug.LOG )
			Log.d(TAG, "zoomIn()");
    	if( zoom_factor < max_zoom_factor ) {
			zoomTo(zoom_factor+1, true);
        }
	}
	
	public void zoomOut() {
		if( MyDebug.LOG )
			Log.d(TAG, "zoomOut()");
		if( zoom_factor > 0 ) {
			zoomTo(zoom_factor-1, true);
        }
	}
	
	public void zoomTo(int new_zoom_factor, boolean update_seek_bar) {
		if( MyDebug.LOG )
			Log.d(TAG, "ZoomTo(): " + new_zoom_factor);
		if( new_zoom_factor < 0 )
			new_zoom_factor = 0;
		if( new_zoom_factor > max_zoom_factor )
			new_zoom_factor = max_zoom_factor;
		// problem where we crashed due to calling this function with null camera should be fixed now, but check again just to be safe
    	if(new_zoom_factor != zoom_factor && camera != null) {
			Camera.Parameters parameters = camera.getParameters();
			if( parameters.isZoomSupported() ) {
				if( MyDebug.LOG )
					Log.d(TAG, "zoom was: " + parameters.getZoom());
				parameters.setZoom((int)new_zoom_factor);
				try {
					camera.setParameters(parameters);
					zoom_factor = new_zoom_factor;
					if( update_seek_bar ) {
						Activity activity = (Activity)this.getContext();
					    SeekBar zoomSeekBar = (SeekBar) activity.findViewById(R.id.zoom_seekbar);
						zoomSeekBar.setProgress(max_zoom_factor-zoom_factor);
					}
				}
	        	catch(RuntimeException e) {
	        		// crash reported in v1.3 on device "PANTONE 5 SoftBank 107SH (SBM107SH)"
		    		if( MyDebug.LOG )
		    			Log.e(TAG, "runtime exception in ZoomTo()");
					e.printStackTrace();
	        	}
	    		clearFocusAreas();
			}
        }
	}
	
	public void changeExposure(int change, boolean update_seek_bar) {
		if( MyDebug.LOG )
			Log.d(TAG, "changeExposure(): " + change);
		if( change != 0 && camera != null && ( min_exposure != 0 || max_exposure != 0 ) ) {
			Camera.Parameters parameters = camera.getParameters();
			int current_exposure = parameters.getExposureCompensation();
			int new_exposure = current_exposure + change;
			setExposure(new_exposure, update_seek_bar);
		}
	}

	public void setExposure(int new_exposure, boolean update_seek_bar) {
		if( MyDebug.LOG )
			Log.d(TAG, "setExposure(): " + new_exposure);
		if( camera != null && ( min_exposure != 0 || max_exposure != 0 ) ) {
			Camera.Parameters parameters = camera.getParameters();
			int current_exposure = parameters.getExposureCompensation();
			if( new_exposure < min_exposure )
				new_exposure = min_exposure;
			if( new_exposure > max_exposure )
				new_exposure = max_exposure;
			if( new_exposure != current_exposure ) {
				if( MyDebug.LOG )
					Log.d(TAG, "change exposure from " + current_exposure + " to " + new_exposure);
				parameters.setExposureCompensation(new_exposure);
				try {
					camera.setParameters(parameters);
					// now save
					SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getContext());
					SharedPreferences.Editor editor = sharedPreferences.edit();
					editor.putString("preference_exposure", "" + new_exposure);
					editor.apply();
		    		showToast(change_exposure_toast, "Exposure compensation " + new_exposure);
		    		if( update_seek_bar ) {
		    			MainActivity main_activity = (MainActivity)this.getContext();
		    			main_activity.setSeekBarExposure();
		    		}
				}
	        	catch(RuntimeException e) {
	        		// just to be safe
		    		if( MyDebug.LOG )
		    			Log.e(TAG, "runtime exception in changeExposure()");
					e.printStackTrace();
	        	}
			}
		}
	}

	public void switchCamera() {
		if( MyDebug.LOG )
			Log.d(TAG, "switchCamera()");
		//if( is_taking_photo && !is_taking_photo_on_timer ) {
		if( this.phase == PHASE_TAKING_PHOTO ) {
			// just to be safe - risk of cancelling the autofocus before taking a photo, or otherwise messing things up
			if( MyDebug.LOG )
				Log.d(TAG, "currently taking a photo");
			return;
		}
		int n_cameras = Camera.getNumberOfCameras();
		if( MyDebug.LOG )
			Log.d(TAG, "found " + n_cameras + " cameras");
		if( n_cameras > 1 ) {
			closeCamera();
			cameraId = (cameraId+1) % n_cameras;
		    Camera.CameraInfo info = new Camera.CameraInfo();
		    Camera.getCameraInfo(cameraId, info);
		    if( info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT ) {
				showToast(switch_camera_toast, "Front Camera");
		    }
		    else {
				showToast(switch_camera_toast, "Back Camera");
		    }
			this.openCamera();
			
			// we update the focus, in case we weren't able to do it when switching video with a camera that didn't support focus modes
			updateFocusForVideo();
		}
	}

	public void switchVideo(boolean save, boolean update_preview_size) {
		if( MyDebug.LOG )
			Log.d(TAG, "switchVideo()");
		boolean old_is_video = is_video;
		if( this.is_video ) {
			if( video_recorder != null ) {
				stopVideo();
			}
			this.is_video = false;
			showToast(switch_video_toast, "Photo");
		}
		else {
			//if( is_taking_photo_on_timer ) {
			if( this.isOnTimer() ) {
				takePictureTimerTask.cancel();
				if( beepTimerTask != null ) {
					beepTimerTask.cancel();
				}
				/*is_taking_photo_on_timer = false;
				is_taking_photo = false;*/
				this.phase = PHASE_NORMAL;
				if( MyDebug.LOG )
					Log.d(TAG, "cancelled camera timer");
				this.is_video = true;
			}
			//else if( this.is_taking_photo ) {
			else if( this.phase == PHASE_TAKING_PHOTO ) {
				// wait until photo taken
				if( MyDebug.LOG )
					Log.d(TAG, "wait until photo taken");
			}
			else {
				this.is_video = true;
			}
			
			if( this.is_video ) {
				showToast(switch_video_toast, "Video");
				//if( this.is_preview_paused ) {
			}
		}
		
		if( is_video != old_is_video ) {
			updateFocusForVideo();

			Activity activity = (Activity)this.getContext();
			ImageButton view = (ImageButton)activity.findViewById(R.id.take_photo);
			view.setImageResource(is_video ? R.drawable.take_video : R.drawable.take_photo);

			if( save ) {
				// now save
				SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getContext());
				SharedPreferences.Editor editor = sharedPreferences.edit();
				editor.putBoolean(getIsVideoPreferenceKey(), is_video);
				editor.apply();
	    	}
			
			if( update_preview_size ) {
				if( this.is_preview_started ) {
					camera.stopPreview();
					this.is_preview_started = false;
				}
				setPreviewSize();				
				// always start the camera preview, even if it was previously paused
		        this.startCameraPreview();
			}
		}
	}
	
	private void updateFocusForVideo() {
		if( MyDebug.LOG )
			Log.d(TAG, "updateFocusForVideo()");
		if( this.supported_focus_values != null && camera != null ) {
			Camera.Parameters parameters = camera.getParameters();
			String current_focus_mode = parameters.getFocusMode();
			boolean focus_is_video = current_focus_mode.equals(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
			if( MyDebug.LOG ) {
				Log.d(TAG, "current_focus_mode: " + current_focus_mode);
				Log.d(TAG, "focus_is_video: " + focus_is_video + " , is_video: " + is_video);
			}
			if( focus_is_video != is_video ) {
				if( MyDebug.LOG )
					Log.d(TAG, "need to change focus mode");
				updateFocus(is_video ? "focus_mode_continuous_video" : "focus_mode_auto", true, true);
				if( MyDebug.LOG ) {
					parameters = camera.getParameters();
					current_focus_mode = parameters.getFocusMode();
					Log.d(TAG, "new focus mode: " + current_focus_mode);
				}
			}
		}
	}

	public void cycleFlash() {
		if( MyDebug.LOG )
			Log.d(TAG, "cycleFlash()");
		//if( is_taking_photo && !is_taking_photo_on_timer ) {
		if( this.phase == PHASE_TAKING_PHOTO ) {
			// just to be safe - risk of cancelling the autofocus before taking a photo, or otherwise messing things up
			if( MyDebug.LOG )
				Log.d(TAG, "currently taking a photo");
			return;
		}
		if( this.supported_flash_values != null && this.supported_flash_values.size() > 1 ) {
			int new_flash_index = (current_flash_index+1) % this.supported_flash_values.size();
			updateFlash(new_flash_index);

			// now save
			String flash_value = supported_flash_values.get(current_flash_index);
			if( MyDebug.LOG ) {
				Log.d(TAG, "save new flash_value: " + flash_value);
			}
			SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getContext());
			SharedPreferences.Editor editor = sharedPreferences.edit();
			editor.putString(getFlashPreferenceKey(cameraId), flash_value);
			editor.apply();
		}
	}

	private boolean updateFlash(String flash_value) {
		if( MyDebug.LOG )
			Log.d(TAG, "updateFlash(): " + flash_value);
		if( supported_flash_values != null ) {
	    	int new_flash_index = supported_flash_values.indexOf(flash_value);
			if( MyDebug.LOG )
				Log.d(TAG, "new_flash_index: " + new_flash_index);
	    	if( new_flash_index != -1 ) {
	    		updateFlash(new_flash_index);
	    		return true;
	    	}
		}
    	return false;
	}
	
	private void updateFlash(int new_flash_index) {
		if( MyDebug.LOG )
			Log.d(TAG, "updateFlash(): " + new_flash_index);
		// updates the Flash button, and Flash camera mode
		if( supported_flash_values != null && new_flash_index != current_flash_index ) {
			boolean initial = current_flash_index==-1;
			current_flash_index = new_flash_index;
			if( MyDebug.LOG )
				Log.d(TAG, "    current_flash_index is now " + current_flash_index + " (initial " + initial + ")");

			Activity activity = (Activity)this.getContext();
		    ImageButton flashButton = (ImageButton) activity.findViewById(R.id.flash);
	    	String [] flash_entries = getResources().getStringArray(R.array.flash_entries);
	    	String [] flash_icons = getResources().getStringArray(R.array.flash_icons);
			String flash_value = supported_flash_values.get(current_flash_index);
			if( MyDebug.LOG )
				Log.d(TAG, "    flash_value: " + flash_value);
	    	String [] flash_values = getResources().getStringArray(R.array.flash_values);
	    	for(int i=0;i<flash_values.length;i++) {
				/*if( MyDebug.LOG )
					Log.d(TAG, "    compare to: " + flash_values[i]);*/
	    		if( flash_value.equals(flash_values[i]) ) {
					if( MyDebug.LOG )
						Log.d(TAG, "    found entry: " + i);
	    			//flashButton.setText(flash_entries[i]);
	    			int resource = getResources().getIdentifier(flash_icons[i], null, activity.getApplicationContext().getPackageName());
	    			flashButton.setImageResource(resource);
	    			if( !initial ) {
	    				showToast(flash_toast, flash_entries[i]);
	    			}
	    			break;
	    		}
	    	}
	    	this.setFlash(flash_value);
		}
	}

	private void setFlash(String flash_value) {
		if( MyDebug.LOG )
			Log.d(TAG, "setFlash() " + flash_value);
		Camera.Parameters parameters = camera.getParameters();
    	if( flash_value.equals("flash_off") ) {
    		parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
    	}
    	else if( flash_value.equals("flash_auto") ) {
    		parameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
    	}
    	else if( flash_value.equals("flash_on") ) {
    		parameters.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
    	}
    	else if( flash_value.equals("flash_torch") ) {
    		parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
    	}
    	else if( flash_value.equals("flash_red_eye") ) {
    		parameters.setFlashMode(Camera.Parameters.FLASH_MODE_RED_EYE);
    	}
		camera.setParameters(parameters);
	}

	private List<String> getSupportedFlashModes(List<String> supported_flash_modes) {
		if( MyDebug.LOG )
			Log.d(TAG, "getSupportedFlashModes()");
		List<String> output_modes = new Vector<String>();
		if( supported_flash_modes != null ) {
			/*for(String flash_mode : supported_flash_modes) {
				if( flash_mode.equals(Camera.Parameters.FLASH_MODE_OFF) ) {
					output_modes.add("flash_off");
				}
				else if( flash_mode.equals(Camera.Parameters.FLASH_MODE_AUTO) ) {
					output_modes.add("flash_auto");
				}
				else if( flash_mode.equals(Camera.Parameters.FLASH_MODE_ON) ) {
					output_modes.add("flash_on");
				}
				else if( flash_mode.equals(Camera.Parameters.FLASH_MODE_TORCH) ) {
					output_modes.add("flash_torch");
				}
				else if( flash_mode.equals(Camera.Parameters.FLASH_MODE_RED_EYE) ) {
					output_modes.add("flash_red_eye");
				}
			}*/
			// also resort as well as converting
			// first one will be the default choice
			if( supported_flash_modes.contains(Camera.Parameters.FLASH_MODE_AUTO) ) {
				output_modes.add("flash_auto");
				if( MyDebug.LOG )
					Log.d(TAG, " supports flash_auto");
			}
			if( supported_flash_modes.contains(Camera.Parameters.FLASH_MODE_OFF) ) {
				output_modes.add("flash_off");
				if( MyDebug.LOG )
					Log.d(TAG, " supports flash_off");
			}
			if( supported_flash_modes.contains(Camera.Parameters.FLASH_MODE_ON) ) {
				output_modes.add("flash_on");
				if( MyDebug.LOG )
					Log.d(TAG, " supports flash_on");
			}
			if( supported_flash_modes.contains(Camera.Parameters.FLASH_MODE_TORCH) ) {
				output_modes.add("flash_torch");
				if( MyDebug.LOG )
					Log.d(TAG, " supports flash_torch");
			}
			if( supported_flash_modes.contains(Camera.Parameters.FLASH_MODE_RED_EYE) ) {
				output_modes.add("flash_red_eye");
				if( MyDebug.LOG )
					Log.d(TAG, " supports flash_red_eye");
			}
		}
		return output_modes;
	}

	public void cycleFocusMode() {
		if( MyDebug.LOG )
			Log.d(TAG, "cycleFocusMode()");
		//if( is_taking_photo && !is_taking_photo_on_timer ) {
		if( this.phase == PHASE_TAKING_PHOTO ) {
			// just to be safe - otherwise problem that changing the focus mode will cancel the autofocus before taking a photo, so we never take a photo, but is_taking_photo remains true!
			if( MyDebug.LOG )
				Log.d(TAG, "currently taking a photo");
			return;
		}
		if( this.supported_focus_values != null && this.supported_focus_values.size() > 1 ) {
			int new_focus_index = (current_focus_index+1) % this.supported_focus_values.size();
			updateFocus(new_focus_index, false, true);
		}
	}
	
	private boolean updateFocus(String focus_value, boolean quiet, boolean save) {
		if( MyDebug.LOG )
			Log.d(TAG, "updateFocus(): " + focus_value);
		if( this.supported_focus_values != null ) {
	    	int new_focus_index = supported_focus_values.indexOf(focus_value);
			if( MyDebug.LOG )
				Log.d(TAG, "new_focus_index: " + new_focus_index);
	    	if( new_focus_index != -1 ) {
	    		updateFocus(new_focus_index, quiet, save);
	    		return true;
	    	}
		}
    	return false;
	}

	private void updateFocus(int new_focus_index, boolean quiet, boolean save) {
		if( MyDebug.LOG )
			Log.d(TAG, "updateFocus(): " + new_focus_index + " current_focus_index: " + current_focus_index);
		// updates the Focus button, and Focus camera mode
		if( this.supported_focus_values != null && new_focus_index != current_focus_index ) {
			boolean initial = current_focus_index==-1;
			current_focus_index = new_focus_index;
			if( MyDebug.LOG )
				Log.d(TAG, "    current_focus_index is now " + current_focus_index + " (initial " + initial + ")");

			Activity activity = (Activity)this.getContext();
		    ImageButton focusModeButton = (ImageButton) activity.findViewById(R.id.focus_mode);
	    	String [] focus_entries = getResources().getStringArray(R.array.focus_mode_entries);
	    	String [] focus_icons = getResources().getStringArray(R.array.focus_mode_icons);
			String focus_value = supported_focus_values.get(current_focus_index);
			if( MyDebug.LOG )
				Log.d(TAG, "    focus_value: " + focus_value);
	    	String [] focus_values = getResources().getStringArray(R.array.focus_mode_values);
	    	for(int i=0;i<focus_values.length;i++) {
				if( MyDebug.LOG )
					Log.d(TAG, "    compare to: " + focus_values[i]);
	    		if( focus_value.equals(focus_values[i]) ) {
					if( MyDebug.LOG )
						Log.d(TAG, "    found entry: " + i);
	    			int resource = getResources().getIdentifier(focus_icons[i], null, activity.getApplicationContext().getPackageName());
	    			focusModeButton.setImageResource(resource);
	    			if( !initial && !quiet ) {
	    				showToast(focus_toast, focus_entries[i]);
	    			}
	    			break;
	    		}
	    	}
	    	this.setFocus(focus_value);

	    	if( save ) {
				// now save
				SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getContext());
				SharedPreferences.Editor editor = sharedPreferences.edit();
				editor.putString(getFocusPreferenceKey(cameraId), focus_value);
				editor.apply();
	    	}
		}
	}

	private void setFocus(String focus_value) {
		if( MyDebug.LOG )
			Log.d(TAG, "setFocus() " + focus_value);
		if( camera == null ) {
			if( MyDebug.LOG )
				Log.d(TAG, "null camera");
			return;
		}
		Camera.Parameters parameters = camera.getParameters();
    	if( focus_value.equals("focus_mode_auto") ) {
    		parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
    	}
    	else if( focus_value.equals("focus_mode_infinity") ) {
    		parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
    	}
    	else if( focus_value.equals("focus_mode_macro") ) {
    		parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
    	}
    	else if( focus_value.equals("focus_mode_fixed") ) {
    		parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
    	}
    	else if( focus_value.equals("focus_mode_edof") ) {
    		parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_EDOF);
    	}
    	else if( focus_value.equals("focus_mode_continuous_video") ) {
    		parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
    	}
    	else {
    		if( MyDebug.LOG )
    			Log.d(TAG, "setFocus() received unknown focus value " + focus_value);
    	}
		camera.setParameters(parameters);
		clearFocusAreas();
		tryAutoFocus();
	}

	private List<String> getSupportedFocusModes(List<String> supported_focus_modes) {
		if( MyDebug.LOG )
			Log.d(TAG, "getSupportedFocusModes()");
		List<String> output_modes = new Vector<String>();
		if( supported_focus_modes != null ) {
			// also resort as well as converting
			// first one will be the default choice
			if( supported_focus_modes.contains(Camera.Parameters.FOCUS_MODE_AUTO) ) {
				output_modes.add("focus_mode_auto");
				if( MyDebug.LOG )
					Log.d(TAG, " supports focus_mode_auto");
			}
			if( supported_focus_modes.contains(Camera.Parameters.FOCUS_MODE_INFINITY) ) {
				output_modes.add("focus_mode_infinity");
				if( MyDebug.LOG )
					Log.d(TAG, " supports focus_mode_infinity");
			}
			if( supported_focus_modes.contains(Camera.Parameters.FOCUS_MODE_MACRO) ) {
				output_modes.add("focus_mode_macro");
				if( MyDebug.LOG )
					Log.d(TAG, " supports focus_mode_macro");
			}
			if( supported_focus_modes.contains(Camera.Parameters.FOCUS_MODE_FIXED) ) {
				output_modes.add("focus_mode_fixed");
				if( MyDebug.LOG )
					Log.d(TAG, " supports focus_mode_fixed");
			}
			if( supported_focus_modes.contains(Camera.Parameters.FOCUS_MODE_EDOF) ) {
				output_modes.add("focus_mode_edof");
				if( MyDebug.LOG )
					Log.d(TAG, " supports focus_mode_edof");
			}
			if( supported_focus_modes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO) ) {
				output_modes.add("focus_mode_continuous_video");
				if( MyDebug.LOG )
					Log.d(TAG, " supports focus_mode_continuous_video");
			}
		}
		return output_modes;
	}
	
	public void takePicturePressed() {
		if( MyDebug.LOG )
			Log.d(TAG, "takePicturePressed");
		if( camera == null ) {
			if( MyDebug.LOG )
				Log.d(TAG, "camera not available");
			/*is_taking_photo_on_timer = false;
			is_taking_photo = false;*/
			this.phase = PHASE_NORMAL;
			return;
		}
		if( !this.has_surface ) {
			if( MyDebug.LOG )
				Log.d(TAG, "preview surface not yet available");
			/*is_taking_photo_on_timer = false;
			is_taking_photo = false;*/
			this.phase = PHASE_NORMAL;
			return;
		}
		//if( is_taking_photo_on_timer ) {
		if( this.isOnTimer() ) {
			takePictureTimerTask.cancel();
			if( beepTimerTask != null ) {
				beepTimerTask.cancel();
			}
			/*is_taking_photo_on_timer = false;
			is_taking_photo = false;*/
			this.phase = PHASE_NORMAL;
			if( MyDebug.LOG )
				Log.d(TAG, "cancelled camera timer");
		    showToast(take_photo_toast, "Cancelled timer");
			return;
		}
    	//if( is_taking_photo ) {
		if( this.phase == PHASE_TAKING_PHOTO ) {
    		if( is_video ) {
    			if( !video_start_time_set || System.currentTimeMillis() - video_start_time < 500 ) {
    				// if user presses to stop too quickly, we ignore
    				// firstly to reduce risk of corrupt video files when stopping too quickly (see RuntimeException we have to catch in stopVideo),
    				// secondly, to reduce a backlog of events which slows things down, if user presses start/stop repeatedly too quickly
    	    		if( MyDebug.LOG )
    	    			Log.d(TAG, "ignore pressing stop video too quickly after start");
    			}
    			else {
    				stopVideo();
    			}
    		}
    		else {
	    		if( MyDebug.LOG )
	    			Log.d(TAG, "already taking a photo");
    		}
    		return;
    	}

    	// make sure that preview running (also needed to hide trash/share icons)
        this.startCameraPreview();

        //is_taking_photo = true;
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getContext());
		String timer_value = sharedPreferences.getString("preference_timer", "0");
		long timer_delay = 0;
		try {
			timer_delay = Integer.parseInt(timer_value) * 1000;
		}
        catch(NumberFormatException e) {
    		if( MyDebug.LOG )
    			Log.e(TAG, "failed to parse timer_value: " + timer_value);
    		e.printStackTrace();
    		timer_delay = 0;
        }

		String burst_mode_value = sharedPreferences.getString("preference_burst_mode", "1");
		try {
			n_burst = Integer.parseInt(burst_mode_value);
    		if( MyDebug.LOG )
    			Log.d(TAG, "n_burst: " + n_burst);
		}
        catch(NumberFormatException e) {
    		if( MyDebug.LOG )
    			Log.e(TAG, "failed to parse burst_mode_value: " + burst_mode_value);
    		e.printStackTrace();
    		n_burst = 1;
        }
		image_name=null;
		image_name=new String[n_burst];		
		remaining_burst_photos = n_burst-1;

		if( timer_delay == 0 ) {
			takePicture();
		}
		else {
			takePictureOnTimer(timer_delay, false);
		}
	}
	
	private void takePictureOnTimer(long timer_delay, boolean repeated) {
		if( MyDebug.LOG ) {
			Log.d(TAG, "takePictureOnTimer");
			Log.d(TAG, "timer_delay: " + timer_delay);
		}
        this.phase = PHASE_TIMER;
		class TakePictureTimerTask extends TimerTask {
			public void run() {
				if( beepTimerTask != null ) {
					beepTimerTask.cancel();
				}
				takePicture();
			}
		}
		take_photo_time = System.currentTimeMillis() + timer_delay;
		if( MyDebug.LOG )
			Log.d(TAG, "take photo at: " + take_photo_time);
		if( !repeated ) {
			showToast(take_photo_toast, "Started timer");
		}
    	takePictureTimer.schedule(takePictureTimerTask = new TakePictureTimerTask(), timer_delay);

		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getContext());
		if( sharedPreferences.getBoolean("preference_timer_beep", true) ) {
    		class BeepTimerTask extends TimerTask {
    			public void run() {
    			    try {
    			        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
    					Activity activity = (Activity)getContext();
    			        Ringtone r = RingtoneManager.getRingtone(activity.getApplicationContext(), notification);
    			        r.play();
    			    }
    			    catch(Exception e) {
    			    }		
    			}
    		}
        	beepTimer.schedule(beepTimerTask = new BeepTimerTask(), 0, 1000);
		}
	}

	private void takePicture() {
		if( MyDebug.LOG )
			Log.d(TAG, "takePicture");
		this.thumbnail_anim = false;
        this.phase = PHASE_TAKING_PHOTO;
		if( camera == null ) {
			if( MyDebug.LOG )
				Log.d(TAG, "camera not available");
			/*is_taking_photo_on_timer = false;
			is_taking_photo = false;*/
			this.phase = PHASE_NORMAL;
			showGUI(true);
			return;
		}
		if( !this.has_surface ) {
			if( MyDebug.LOG )
				Log.d(TAG, "preview surface not yet available");
			/*is_taking_photo_on_timer = false;
			is_taking_photo = false;*/
			this.phase = PHASE_NORMAL;
			showGUI(true);
			return;
		}
		focus_success = FOCUS_DONE; // clear focus rectangle

        if( is_video ) {
    		if( MyDebug.LOG )
    			Log.d(TAG, "start video recording");
			MainActivity main_activity = (MainActivity)Preview.this.getContext();
			File videoFile = main_activity.getOutputMediaFile(MainActivity.MEDIA_TYPE_VIDEO);
			if( videoFile == null ) {
	            Log.e(TAG, "Couldn't create media video file; check storage permissions?");
	    	    showToast(null, "Failed to save video file");
			}
			else {
				video_name = videoFile.getAbsolutePath();
	    		if( MyDebug.LOG )
	    			Log.d(TAG, "save to: " + video_name);
	        	this.camera.unlock();
	        	video_recorder = new MediaRecorder();
	        	video_recorder.setCamera(camera);
				SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getContext());
				boolean record_audio = sharedPreferences.getBoolean("preference_record_audio", true);
				if( record_audio ) {
	        		String pref_audio_src = sharedPreferences.getString("preference_record_audio_src", "audio_src_camcorder");
		    		if( MyDebug.LOG )
		    			Log.d(TAG, "pref_audio_src: " + pref_audio_src);
	        		int audio_source = MediaRecorder.AudioSource.CAMCORDER;
	        		if( pref_audio_src.equals("audio_src_mic") ) {
		        		audio_source = MediaRecorder.AudioSource.MIC;
	        		}
		    		if( MyDebug.LOG )
		    			Log.d(TAG, "audio_source: " + audio_source);
					video_recorder.setAudioSource(audio_source);
				}
				video_recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

				/*video_recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
				if( record_audio ) {
					video_recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
				}
	        	video_recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);*/
	        	CamcorderProfile profile = getCamcorderProfile();
	    		if( MyDebug.LOG ) {
	    			Log.d(TAG, "current_video_quality: " + current_video_quality);
	    			if( current_video_quality != -1 )
	    				Log.d(TAG, "current_video_quality value: " + video_quality.get(current_video_quality).intValue());
	    			Log.d(TAG, "resolution " + profile.videoFrameWidth + " x " + profile.videoFrameHeight);
	    		}
				if( record_audio ) {
					video_recorder.setProfile(profile);
				}
				else {
					// from http://stackoverflow.com/questions/5524672/is-it-possible-to-use-camcorderprofile-without-audio-source
					video_recorder.setOutputFormat(profile.fileFormat);
					video_recorder.setVideoFrameRate(profile.videoFrameRate);
					video_recorder.setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight);
					video_recorder.setVideoEncodingBitRate(profile.videoBitRate);
					video_recorder.setVideoEncoder(profile.videoCodec);
				}

	        	video_recorder.setOrientationHint(this.current_rotation);
	        	video_recorder.setOutputFile(video_name);
	        	try {
	        		showGUI(false);
	        		/*if( true ) // test
	        			throw new IOException();*/
		        	video_recorder.setPreviewDisplay(mHolder.getSurface());
					video_recorder.prepare();
	            	video_recorder.start();
	            	video_start_time = System.currentTimeMillis();
	            	video_start_time_set = true;
    				showToast(stopstart_video_toast, "Started recording video");
    				// don't send intent for ACTION_MEDIA_SCANNER_SCAN_FILE yet - wait until finished, so we get completed file
				}
	        	catch(IOException e) {
		    		if( MyDebug.LOG )
		    			Log.e(TAG, "failed to save video");
					e.printStackTrace();
		    	    showToast(null, "Failed to save video");
		    		video_recorder.reset();
		    		video_recorder.release(); 
		    		video_recorder = null;
					/*is_taking_photo = false;
					is_taking_photo_on_timer = false;*/
					this.phase = PHASE_NORMAL;
					showGUI(true);
					this.reconnectCamera();
				}
	        	catch(RuntimeException e) {
	        		// needed for emulator at least - although MediaRecorder not meant to work with emulator, it's good to fail gracefully
		    		if( MyDebug.LOG )
		    			Log.e(TAG, "runtime exception starting video recorder");
					e.printStackTrace();
		    	    showToast(null, "Failed to record video");
		    		video_recorder.reset();
		    		video_recorder.release(); 
		    		video_recorder = null;
					/*is_taking_photo = false;
					is_taking_photo_on_timer = false;*/
					this.phase = PHASE_NORMAL;
					showGUI(true);
					this.reconnectCamera();
				}
			}
        	return;
		}

		showGUI(false);
        Camera.Parameters parameters = camera.getParameters();
		String focus_mode = parameters.getFocusMode();
		if( MyDebug.LOG )
			Log.d(TAG, "focus_mode is " + focus_mode);

		if( focus_mode.equals(Camera.Parameters.FOCUS_MODE_AUTO) || focus_mode.equals(Camera.Parameters.FOCUS_MODE_MACRO) ) {
	        Camera.AutoFocusCallback autoFocusCallback = new Camera.AutoFocusCallback() {
				@Override
				public void onAutoFocus(boolean success, Camera camera) {
					if( MyDebug.LOG )
						Log.d(TAG, "autofocus complete: " + success);
					takePictureWhenFocused();
				}
	        };
			if( MyDebug.LOG )
				Log.d(TAG, "start autofocus to take picture");
    		try {
    	    	camera.autoFocus(autoFocusCallback);
    			count_cameraAutoFocus++;
    		}
    		catch(RuntimeException e) {
    			// just in case? We got a RuntimeException report here from 1 user on Google Play:
    			// 21 Dec 2013, Xperia Go, Android 4.1
    			autoFocusCallback.onAutoFocus(false, camera);

    			if( MyDebug.LOG )
					Log.e(TAG, "runtime exception from autoFocus when trying to take photo");
    			e.printStackTrace();
    		}
		}
		else {
			takePictureWhenFocused();
		}
	}

	private void takePictureWhenFocused() {
		// should be called when auto-focused
		if( MyDebug.LOG )
			Log.d(TAG, "takePictureWhenFocused");
		if( camera == null ) {
			if( MyDebug.LOG )
				Log.d(TAG, "camera not available");
			/*is_taking_photo_on_timer = false;
			is_taking_photo = false;*/
			this.phase = PHASE_NORMAL;
			showGUI(true);
			return;
		}
		if( !this.has_surface ) {
			if( MyDebug.LOG )
				Log.d(TAG, "preview surface not yet available");
			/*is_taking_photo_on_timer = false;
			is_taking_photo = false;*/
			this.phase = PHASE_NORMAL;
			showGUI(true);
			return;
		}
		if( MyDebug.LOG )
			Log.d(TAG, "remaining_burst_photos: " + remaining_burst_photos);

    	Camera.ShutterCallback shutterCallback = new Camera.ShutterCallback() {
    		// don't do anything here, but we need to implement the callback to get the shutter sound (at least on Galaxy Nexus and Nexus 7)
            public void onShutter() {
    			if( MyDebug.LOG )
    				Log.d(TAG, "shutterCallback.onShutter()");
            }
        };

        Camera.PictureCallback jpegPictureCallback = new Camera.PictureCallback() {
    	    public void onPictureTaken(byte[] data, Camera cam) {
    	    	// n.b., this is automatically run in a different thread
	            System.gc();
    			if( MyDebug.LOG )
    				Log.d(TAG, "onPictureTaken");

        		MainActivity main_activity = (MainActivity)Preview.this.getContext();
    			boolean image_capture_intent = false;
       	        Uri image_capture_intent_uri = null;
    	        String action = main_activity.getIntent().getAction();
    	        if( MediaStore.ACTION_IMAGE_CAPTURE.equals(action) ) {
        			if( MyDebug.LOG )
        				Log.d(TAG, "from image capture intent");
        			image_capture_intent = true;
        	        Bundle myExtras = main_activity.getIntent().getExtras();
        	        if (myExtras != null) {
        	        	image_capture_intent_uri = (Uri) myExtras.getParcelable(MediaStore.EXTRA_OUTPUT);
            			if( MyDebug.LOG )
            				Log.d(TAG, "save to: " + image_capture_intent_uri);
        	        }
    	        }

    	        boolean success = false;
    	        Bitmap bitmap = null;
				SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(Preview.this.getContext());
				boolean auto_stabilise = sharedPreferences.getBoolean("preference_auto_stabilise", false);
    			if( auto_stabilise && has_level_angle && main_activity.supportsAutoStabilise() )
    			{
    				//level_angle = -129;
    				if( test_have_angle )
    					level_angle = test_angle;
    				while( level_angle < -90 )
    					level_angle += 180;
    				while( level_angle > 90 )
    					level_angle -= 180;
        			if( MyDebug.LOG )
        				Log.d(TAG, "auto stabilising... angle: " + level_angle);
    				BitmapFactory.Options options = new BitmapFactory.Options();
    				//options.inMutable = true;
    				options.inPurgeable = true;
        			bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);
        			int width = bitmap.getWidth();
        			int height = bitmap.getHeight();
        			if( MyDebug.LOG ) {
        				Log.d(TAG, "level_angle: " + level_angle);
        				Log.d(TAG, "decoded bitmap size " + width + ", " + height);
        				Log.d(TAG, "bitmap size: " + width*height*4);
        			}
        			/*for(int y=0;y<height;y++) {
        				for(int x=0;x<width;x++) {
        					int col = bitmap.getPixel(x, y);
        					col = col & 0xffff0000; // mask out red component
        					bitmap.setPixel(x, y, col);
        				}
        			}*/
        			if( test_low_memory ) {
        		    	level_angle = 45.0;
        			}
        		    Matrix matrix = new Matrix();
        		    double level_angle_rad_abs = Math.abs( Math.toRadians(level_angle) );
        		    int w1 = width, h1 = height;
        		    double w0 = (w1 * Math.cos(level_angle_rad_abs) + h1 * Math.sin(level_angle_rad_abs));
        		    double h0 = (w1 * Math.sin(level_angle_rad_abs) + h1 * Math.cos(level_angle_rad_abs));
        		    // apply a scale so that the overall image size isn't increased
        		    float orig_size = w1*h1;
        		    float rotated_size = (float)(w0*h0);
        		    float scale = (float)Math.sqrt(orig_size/rotated_size);
        			if( test_low_memory ) {
            			if( MyDebug.LOG )
            				Log.d(TAG, "TESTING LOW MEMORY");
        		    	scale *= 2.0f; // test 20MP
        		    	//scale *= 1.613f; // test 13MP
        			}
        			if( MyDebug.LOG ) {
        				Log.d(TAG, "w0 = " + w0 + " , h0 = " + h0);
        				Log.d(TAG, "w1 = " + w1 + " , h1 = " + h1);
        				Log.d(TAG, "scale = sqrt " + orig_size + " / " + rotated_size + " = " + scale);
        			}
        		    matrix.postScale(scale, scale);
        		    w0 *= scale;
        		    h0 *= scale;
        		    w1 *= scale;
        		    h1 *= scale;
        			if( MyDebug.LOG ) {
        				Log.d(TAG, "after scaling: w0 = " + w0 + " , h0 = " + h0);
        				Log.d(TAG, "after scaling: w1 = " + w1 + " , h1 = " + h1);
        			}
        		    Camera.CameraInfo info = new Camera.CameraInfo();
        		    Camera.getCameraInfo(cameraId, info);
        		    if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            		    matrix.postRotate((float)-level_angle);
        		    }
        		    else {
            		    matrix.postRotate((float)level_angle);
        		    }
        		    Bitmap new_bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
        		    // careful, as new_bitmap is sometimes not a copy!
        		    if( new_bitmap != bitmap ) {
        		    	bitmap.recycle();
        		    	bitmap = new_bitmap;
        		    }
    	            System.gc();
        			if( MyDebug.LOG ) {
        				Log.d(TAG, "rotated and scaled bitmap size " + bitmap.getWidth() + ", " + bitmap.getHeight());
        				Log.d(TAG, "rotated and scaled bitmap size: " + bitmap.getWidth()*bitmap.getHeight()*4);
        			}
        			double tan_theta = Math.tan(level_angle_rad_abs);
        			double sin_theta = Math.sin(level_angle_rad_abs);
        			double denom = (double)( h0/w0 + tan_theta );
        			double alt_denom = (double)( w0/h0 + tan_theta );
        			if( denom == 0.0 || denom < 1.0e-14 ) {
        	    		if( MyDebug.LOG )
        	    			Log.d(TAG, "zero denominator?!");
        			}
        			else if( alt_denom == 0.0 || alt_denom < 1.0e-14 ) {
        	    		if( MyDebug.LOG )
        	    			Log.d(TAG, "zero alt denominator?!");
        			}
        			else {
            			int w2 = (int)(( h0 + 2.0*h1*sin_theta*tan_theta - w0*tan_theta ) / denom);
            			int h2 = (int)(w2*h0/(double)w0);
            			int alt_h2 = (int)(( w0 + 2.0*w1*sin_theta*tan_theta - h0*tan_theta ) / alt_denom);
            			int alt_w2 = (int)(alt_h2*w0/(double)h0);
            			if( MyDebug.LOG ) {
            				//Log.d(TAG, "h0 " + h0 + " 2.0*h1*sin_theta*tan_theta " + 2.0*h1*sin_theta*tan_theta + " w0*tan_theta " + w0*tan_theta + " / h0/w0 " + h0/w0 + " tan_theta " + tan_theta);
            				Log.d(TAG, "w2 = " + w2 + " , h2 = " + h2);
            				Log.d(TAG, "alt_w2 = " + alt_w2 + " , alt_h2 = " + alt_h2);
            			}
            			if( alt_w2 < w2 ) {
                			if( MyDebug.LOG ) {
                				Log.d(TAG, "chose alt!");
                			}
            				w2 = alt_w2;
            				h2 = alt_h2;
            			}
            			if( w2 <= 0 )
            				w2 = 1;
            			else if( w2 >= bitmap.getWidth() )
            				w2 = bitmap.getWidth()-1;
            			if( h2 <= 0 )
            				h2 = 1;
            			else if( h2 >= bitmap.getHeight() )
            				h2 = bitmap.getHeight()-1;
            			int x0 = (bitmap.getWidth()-w2)/2;
            			int y0 = (bitmap.getHeight()-h2)/2;
            			if( MyDebug.LOG ) {
            				Log.d(TAG, "x0 = " + x0 + " , y0 = " + y0);
            			}
            			new_bitmap = Bitmap.createBitmap(bitmap, x0, y0, w2, h2);
            		    if( new_bitmap != bitmap ) {
            		    	bitmap.recycle();
            		    	bitmap = new_bitmap;
            		    }
        	            System.gc();
        			}
    			}

    			String exif_orientation_s = null;
    			String picFileName = null;
    			File picFile = null;
    	        try {
	    			OutputStream outputStream = null;
	    			if( image_capture_intent ) {
	        			if( image_capture_intent_uri != null )
	        			{
	        			    // Save the bitmap to the specified URI (use a try/catch block)
	        			    outputStream = main_activity.getContentResolver().openOutputStream(image_capture_intent_uri);
	        			}
	        			else
	        			{
	        			    // If the intent doesn't contain an URI, send the bitmap as a parcel
	        			    // (it is a good idea to reduce its size to ~50k pixels before)
		        			if( MyDebug.LOG )
		        				Log.d(TAG, "sent to intent via parcel");
	        				if( bitmap == null ) {
			        			if( MyDebug.LOG )
			        				Log.d(TAG, "create bitmap");
			    				BitmapFactory.Options options = new BitmapFactory.Options();
			    				//options.inMutable = true;
			    				options.inPurgeable = true;
			        			bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);
			        			int width = bitmap.getWidth();
			        			int height = bitmap.getHeight();
			        			if( MyDebug.LOG ) {
			        				Log.d(TAG, "decoded bitmap size " + width + ", " + height);
			        				Log.d(TAG, "bitmap size: " + width*height*4);
			        			}
			        			final int small_size_c = 128;
			        			if( width > small_size_c ) {
			        				float scale = ((float)small_size_c)/(float)width;
				        			if( MyDebug.LOG )
				        				Log.d(TAG, "scale to " + scale);
				        		    Matrix matrix = new Matrix();
				        		    matrix.postScale(scale, scale);
				        		    Bitmap new_bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
				        		    // careful, as new_bitmap is sometimes not a copy!
				        		    if( new_bitmap != bitmap ) {
				        		    	bitmap.recycle();
				        		    	bitmap = new_bitmap;
				        		    }
				        			if( MyDebug.LOG ) {
				        				Log.d(TAG, "scaled bitmap size " + bitmap.getWidth() + ", " + bitmap.getHeight());
				        				Log.d(TAG, "scaled bitmap size: " + bitmap.getWidth()*bitmap.getHeight()*4);
				        			}
				        		}
	        				}
	        				main_activity.setResult(Activity.RESULT_OK, new Intent("inline-data").putExtra("data", bitmap));
	        				main_activity.finish();
	        			}
	    			}
	    			else {
	        			picFile = main_activity.getOutputMediaFile(MainActivity.MEDIA_TYPE_IMAGE);
	        	        if( picFile == null ) {
	        	            Log.e(TAG, "Couldn't create media image file; check storage permissions?");
	        	    	    showToast(null, "Failed to save image file");
	        	        }
	        	        else {
		    	            picFileName = picFile.getAbsolutePath();
	        	    		if( MyDebug.LOG )
	        	    			Log.d(TAG, "save to: " + picFileName);
		    	            outputStream = new FileOutputStream(picFile);
	        	        }
	    			}
	    			
	    			if( outputStream != null ) {
        	            if( bitmap != null ) {
        	    			int image_quality = getImageQuality();
            	            bitmap.compress(Bitmap.CompressFormat.JPEG, image_quality, outputStream);
        	            }
        	            else {
        	            	outputStream.write(data);
        	            }
        	            outputStream.close();
        	    		if( MyDebug.LOG )
        	    			Log.d(TAG, "onPictureTaken saved photo");

        				success = true;
        	            if( picFile != null ) {
        	            	if( bitmap != null ) {
        	            		// need to update EXIF data!
                	    		if( MyDebug.LOG )
                	    			Log.d(TAG, "write temp file to record EXIF data");
        	            		File tempFile = File.createTempFile("opencamera_exif", "");
    		    	            OutputStream tempOutputStream = new FileOutputStream(tempFile);
            	            	tempOutputStream.write(data);
            	            	tempOutputStream.close();
                	    		if( MyDebug.LOG )
                	    			Log.d(TAG, "read back EXIF data");
            	            	ExifInterface exif = new ExifInterface(tempFile.getAbsolutePath());
            	            	String exif_aperture = exif.getAttribute(ExifInterface.TAG_APERTURE);
            	            	String exif_datetime = exif.getAttribute(ExifInterface.TAG_DATETIME);
            	            	String exif_exposure_time = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME);
            	            	String exif_flash = exif.getAttribute(ExifInterface.TAG_FLASH);
            	            	String exif_focal_length = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH);
            	            	String exif_gps_altitude = exif.getAttribute(ExifInterface.TAG_GPS_ALTITUDE);
            	            	String exif_gps_altitude_ref = exif.getAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF);
            	            	String exif_gps_datestamp = exif.getAttribute(ExifInterface.TAG_GPS_DATESTAMP);
            	            	String exif_gps_latitude = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
            	            	String exif_gps_latitude_ref = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF);
            	            	String exif_gps_longitude = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
            	            	String exif_gps_longitude_ref = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF);
            	            	String exif_gps_processing_method = exif.getAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD);
            	            	String exif_gps_timestamp = exif.getAttribute(ExifInterface.TAG_GPS_TIMESTAMP);
            	            	// leave width/height, as this will have changed!
            	            	String exif_iso = exif.getAttribute(ExifInterface.TAG_ISO);
            	            	String exif_make = exif.getAttribute(ExifInterface.TAG_MAKE);
            	            	String exif_model = exif.getAttribute(ExifInterface.TAG_MODEL);
            	            	String exif_orientation = exif.getAttribute(ExifInterface.TAG_ORIENTATION);
            	            	exif_orientation_s = exif_orientation; // store for later use (for the thumbnail, to save rereading it)
            	            	String exif_white_balance = exif.getAttribute(ExifInterface.TAG_WHITE_BALANCE);

            					if( !tempFile.delete() ) {
            						if( MyDebug.LOG )
            							Log.e(TAG, "failed to delete temp " + tempFile.getAbsolutePath());
            					}
            	            	if( MyDebug.LOG )
                	    			Log.d(TAG, "now write new EXIF data");
            	            	ExifInterface exif_new = new ExifInterface(picFile.getAbsolutePath());
            	            	if( exif_aperture != null )
            	            		exif_new.setAttribute(ExifInterface.TAG_APERTURE, exif_aperture);
            	            	if( exif_datetime != null )
            	            		exif_new.setAttribute(ExifInterface.TAG_DATETIME, exif_datetime);
            	            	if( exif_exposure_time != null )
            	            		exif_new.setAttribute(ExifInterface.TAG_EXPOSURE_TIME, exif_exposure_time);
            	            	if( exif_flash != null )
	            	            	exif_new.setAttribute(ExifInterface.TAG_FLASH, exif_flash);
	            	            if( exif_focal_length != null )
	            	            	exif_new.setAttribute(ExifInterface.TAG_FOCAL_LENGTH, exif_focal_length);
	            	            if( exif_gps_altitude != null )
	            	            	exif_new.setAttribute(ExifInterface.TAG_GPS_ALTITUDE, exif_gps_altitude);
	            	            if( exif_gps_altitude_ref != null )
	            	            	exif_new.setAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF, exif_gps_altitude_ref);
	            	            if( exif_gps_datestamp != null )
	            	            	exif_new.setAttribute(ExifInterface.TAG_GPS_DATESTAMP, exif_gps_datestamp);
	            	            if( exif_gps_latitude != null )
	            	            	exif_new.setAttribute(ExifInterface.TAG_GPS_LATITUDE, exif_gps_latitude);
	            	            if( exif_gps_latitude_ref != null )
	            	            	exif_new.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, exif_gps_latitude_ref);
	            	            if( exif_gps_longitude != null )
	            	            	exif_new.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, exif_gps_longitude);
	            	            if( exif_gps_longitude_ref != null )
	            	            	exif_new.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, exif_gps_longitude_ref);
	            	            if( exif_gps_processing_method != null )
	            	            	exif_new.setAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD, exif_gps_processing_method);
	            	            if( exif_gps_timestamp != null )
	            	            	exif_new.setAttribute(ExifInterface.TAG_GPS_TIMESTAMP, exif_gps_timestamp);
            	            	// leave width/height, as this will have changed!
	            	            if( exif_iso != null )
	            	            	exif_new.setAttribute(ExifInterface.TAG_ISO, exif_iso);
	            	            if( exif_make != null )
	            	            	exif_new.setAttribute(ExifInterface.TAG_MAKE, exif_make);
	            	            if( exif_model != null )
	            	            	exif_new.setAttribute(ExifInterface.TAG_MODEL, exif_model);
	            	            if( exif_orientation != null )
	            	            	exif_new.setAttribute(ExifInterface.TAG_ORIENTATION, exif_orientation);
	            	            if( exif_white_balance != null )
	            	            	exif_new.setAttribute(ExifInterface.TAG_WHITE_BALANCE, exif_white_balance);
	            	            setGPSDirectionExif(exif_new);
            	            	exif_new.saveAttributes();
                	    		if( MyDebug.LOG )
                	    			Log.d(TAG, "now saved EXIF data");
        	            	}
        	            	else if( Preview.this.has_geo_direction && sharedPreferences.getBoolean("preference_location", false) ) {
            	            	if( MyDebug.LOG )
                	    			Log.d(TAG, "add GPS direction exif info");
            	            	long time_s = System.currentTimeMillis();
            	            	ExifInterface exif = new ExifInterface(picFile.getAbsolutePath());
	            	            setGPSDirectionExif(exif);
            	            	exif.saveAttributes();
                	    		if( MyDebug.LOG ) {
                	    			Log.d(TAG, "done adding GPS direction exif info, time taken: " + (System.currentTimeMillis() - time_s));
                	    		}
        	            	}

            	            main_activity.broadcastFile(picFile);
        	            	test_last_saved_image = picFileName;
        	            }
        	            if( image_capture_intent ) {
        	            	main_activity.setResult(Activity.RESULT_OK);
        	            	main_activity.finish();
        	            }
        	        }
    			}
    	        catch(FileNotFoundException e) {
    	    		if( MyDebug.LOG )
    	    			Log.e(TAG, "File not found: " + e.getMessage());
    	            e.getStackTrace();
    	    	    showToast(null, "Failed to save photo");
    	        }
    	        catch(IOException e) {
    	    		if( MyDebug.LOG )
    	    			Log.e(TAG, "I/O error writing file: " + e.getMessage());
    	            e.getStackTrace();
    	    	    showToast(null, "Failed to save photo");
    	        }

    			is_preview_started = false; // preview automatically stopped due to taking photo
    	        phase = PHASE_NORMAL; // need to set this even if remaining burst photos, so we can restart the preview
	            if( remaining_burst_photos > 0 ) {
	    	    	// we need to restart the preview; and we do this in the callback, as we need to restart after saving the image
	    	    	// (otherwise this can fail, at least on Nexus 7)

	            	startCameraPreview();
	        		if( MyDebug.LOG )
	        			Log.d(TAG, "burst mode photos remaining: onPictureTaken started preview");
	            }
	            else {
	    	        phase = PHASE_NORMAL;
					boolean pause_preview = sharedPreferences.getBoolean("preference_pause_preview", false);
	        		if( MyDebug.LOG )
	        			Log.d(TAG, "pause_preview? " + pause_preview);
					if( pause_preview && success ) {
		    			
		    		 //   MainActivity activity = (MainActivity)Preview.this.getContext();
		    		  //  Intent intent1 = new Intent(activity,ImageSwitcherActivity.class);		
		    		//	intent1.putExtra("image_name", image_name);
		    		//	activity.startActivity(intent1);
						
						setPreviewPaused(true);
		    			preview_image_name = picFileName;
		    			detect_retina();
					}
					else {
		    	    	// we need to restart the preview; and we do this in the callback, as we need to restart after saving the image
		    	    	// (otherwise this can fail, at least on Nexus 7)
			            startCameraPreview();
						showGUI(true);
		        		if( MyDebug.LOG )
		        			Log.d(TAG, "onPictureTaken started preview");
					}
	            }

	            if( bitmap != null ) {
        		    bitmap.recycle();
        		    bitmap = null;
	            }

	            if( success && picFile != null ) {
	            	// update thumbnail - this should be done after restarting preview, so that the preview is started asap
	            	long time_s = System.currentTimeMillis();
	                Camera.Parameters parameters = cam.getParameters();
	        		Camera.Size size = parameters.getPictureSize();
	        		int ratio = (int) Math.ceil((double) size.width / Preview.this.getWidth());
    				BitmapFactory.Options options = new BitmapFactory.Options();
    				options.inMutable = false;
    				options.inPurgeable = true;
    				options.inSampleSize = Integer.highestOneBit(ratio) * 4; // * 4 to increase performance, without noticeable loss in visual quality 
        			if( !sharedPreferences.getBoolean("preference_thumbnail_animation", true) ) {
        				// can use lower resolution if we don't have the thumbnail animation
        				options.inSampleSize *= 4;
        			}
    	    		if( MyDebug.LOG ) {
    	    			Log.d(TAG, "    picture width   : " + size.width);
    	    			Log.d(TAG, "    preview width   : " + Preview.this.getWidth());
    	    			Log.d(TAG, "    ratio           : " + ratio);
    	    			Log.d(TAG, "    inSampleSize    : " + options.inSampleSize);
    	    			Log.d(TAG, "    current_rotation: " + current_rotation);
    	    		}
    	    		Bitmap old_thumbnail = thumbnail;
        			thumbnail = BitmapFactory.decodeByteArray(data, 0, data.length, options);
        			int thumbnail_rotation = 0;
    				// now get the rotation from the Exif data
					try {
						if( exif_orientation_s == null ) {
							// haven't already read the exif orientation
		    	    		if( MyDebug.LOG )
		    	    			Log.d(TAG, "    read exif orientation");
		                	ExifInterface exif = new ExifInterface(picFile.getAbsolutePath());
			            	exif_orientation_s = exif.getAttribute(ExifInterface.TAG_ORIENTATION);
						}
	    	    		if( MyDebug.LOG )
	    	    			Log.d(TAG, "    exif orientation string: " + exif_orientation_s);
						int exif_orientation = 0;
						// from http://jpegclub.org/exif_orientation.html
						if( exif_orientation_s.equals("0") || exif_orientation_s.equals("1") ) {
							// leave at 0
						}
						else if( exif_orientation_s.equals("3") ) {
							exif_orientation = 180;
						}
						else if( exif_orientation_s.equals("6") ) {
							exif_orientation = 90;
						}
						else if( exif_orientation_s.equals("8") ) {
							exif_orientation = 270;
						}
						else {
							// just leave at 0
		    	    		if( MyDebug.LOG )
		    	    			Log.e(TAG, "    unsupported exif orientation: " + exif_orientation_s);
						}
	    	    		if( MyDebug.LOG )
	    	    			Log.d(TAG, "    exif orientation: " + exif_orientation);
						thumbnail_rotation = (thumbnail_rotation + exif_orientation) % 360;
					}
					catch(IOException exception) {
						if( MyDebug.LOG )
							Log.e(TAG, "exif orientation ioexception");
						exception.printStackTrace();
					}
    	    		if( MyDebug.LOG )
    	    			Log.d(TAG, "    thumbnail orientation: " + thumbnail_rotation);

        			if( thumbnail_rotation != 0 ) {
        				Matrix m = new Matrix();
        				m.setRotate(thumbnail_rotation, thumbnail.getWidth() * 0.5f, thumbnail.getHeight() * 0.5f);
        				Bitmap rotated_thumbnail = Bitmap.createBitmap(thumbnail, 0, 0,thumbnail.getWidth(), thumbnail.getHeight(), m, true);
        				if( rotated_thumbnail != thumbnail ) {
        					thumbnail.recycle();
        					thumbnail = rotated_thumbnail;
        				}
        			}

        			if( sharedPreferences.getBoolean("preference_thumbnail_animation", true) ) {
            			thumbnail_anim = true;
            			thumbnail_anim_start_ms = System.currentTimeMillis();
        			}
	    	    	main_activity.updateGalleryIconToBitmap(thumbnail);
    	    		if( old_thumbnail != null ) {
    	    			// only recycle after we've set the new thumbnail
    	    			old_thumbnail.recycle();
    	    		}
    	    		if( MyDebug.LOG )
    	    			Log.d(TAG, "    time to create thumbnail: " + (System.currentTimeMillis() - time_s));
	            }

	            System.gc();

	            if( remaining_burst_photos > 0 ) {
	            	image_name[remaining_burst_photos]=picFileName;
	            	remaining_burst_photos--;

	        		String timer_value = sharedPreferences.getString("preference_burst_interval", "0");
	        		long timer_delay = 0;
	        		try {
	        			timer_delay = Integer.parseInt(timer_value) * 1000;
	        		}
	                catch(NumberFormatException e) {
	            		if( MyDebug.LOG )
	            			Log.e(TAG, "failed to parse timer_value: " + timer_value);
	            		e.printStackTrace();
	            		timer_delay = 0;
	                }

	        		if( timer_delay == 0 ) {
	        			// we go straight to taking a photo rather than refocusing, for speed
	        			// need to manually set the phase and rehide the GUI
	        	        phase = PHASE_TAKING_PHOTO;
						showGUI(false);
		            	takePictureWhenFocused();
	        		}
	        		else {
	        			takePictureOnTimer(timer_delay, true);
	        		}
	            }
    	    }
    	};
    	
    	{
    		if( MyDebug.LOG )
    			Log.d(TAG, "current_rotation: " + current_rotation);
			Camera.Parameters parameters = camera.getParameters();
			parameters.setRotation(current_rotation);
			camera.setParameters(parameters);

			SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getContext());
			boolean enable_sound = sharedPreferences.getBoolean("preference_shutter_sound", true);
    		if( MyDebug.LOG )
    			Log.d(TAG, "enable_sound? " + enable_sound);
            if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 ) {
            	camera.enableShutterSound(enable_sound);
            }
    		if( MyDebug.LOG )
    			Log.d(TAG, "about to call takePicture");
    		String toast_text = "";
    		if( n_burst > 1 ) {

    			int photo = (n_burst-remaining_burst_photos);
    			toast_text = "Checking photo... (" +  photo + " / " + n_burst + ")";
    		}
    		else {
    			toast_text = "Checking photo...";
    		}
    		if( MyDebug.LOG )
    			Log.d(TAG, toast_text);
    		try {
    			camera.takePicture(shutterCallback, null, jpegPictureCallback);
        		count_cameraTakePicture++;
    			showToast(take_photo_toast, toast_text);
    		}
    		catch(RuntimeException e) {
    			// just in case? We got a RuntimeException report here from 1 user on Google Play; I also encountered it myself once of Galaxy Nexus when starting up
    			if( MyDebug.LOG )
					Log.e(TAG, "runtime exception from takePicture");
    			e.printStackTrace();
	    	    showToast(null, "Failed to take picture");
    		}
    	}
		if( MyDebug.LOG )
			Log.d(TAG, "takePicture exit");
    }

	private void setGPSDirectionExif(ExifInterface exif) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getContext());
    	if( this.has_geo_direction && sharedPreferences.getBoolean("preference_location", false) ) {
			float geo_angle = (float)Math.toDegrees(Preview.this.geo_direction[0]);
			if( geo_angle < 0.0f ) {
				geo_angle += 360.0f;
			}
			if( MyDebug.LOG )
				Log.d(TAG, "save geo_angle: " + geo_angle);
			// see http://www.sno.phy.queensu.ca/~phil/exiftool/TagNames/GPS.html
			String GPSImgDirection_string = Math.round(geo_angle*100) + "/100";
			if( MyDebug.LOG )
				Log.d(TAG, "GPSImgDirection_string: " + GPSImgDirection_string);
		   	exif.setAttribute(TAG_GPS_IMG_DIRECTION, GPSImgDirection_string);
		   	exif.setAttribute(TAG_GPS_IMG_DIRECTION_REF, "M");
    	}
	}

	public void clickedShare() {
		if( MyDebug.LOG )
			Log.d(TAG, "clickedShare");
		//if( is_preview_paused ) {
		if( this.phase == PHASE_PREVIEW_PAUSED ) {
			if( preview_image_name != null ) {
				if( MyDebug.LOG )
					Log.d(TAG, "Share: " + preview_image_name);
				Intent intent = new Intent(Intent.ACTION_SEND);
				intent.setType("image/jpeg");
				intent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + preview_image_name));
				Activity activity = (Activity)this.getContext();
				activity.startActivity(Intent.createChooser(intent, "Photo"));
			}
			startCameraPreview();
			tryAutoFocus();
		}
	}
	
	public String clickedHistogram() {
		if( MyDebug.LOG )
			Log.d(TAG, "clickedHistogram");
		//if( is_preview_paused ) {
		//if( this.phase == PHASE_PREVIEW_PAUSED ) {
			if( preview_image_name != null ) {
				if( MyDebug.LOG )
					Log.d(TAG, "Share: " + preview_image_name);
				return preview_image_name;
				//Intent intent = new Intent(Intent.ACTION_SEND);
			//	intent.setType("image/jpeg");
			//	intent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + preview_image_name));
		//		Activity activity = (Activity)this.getContext();
		//		activity.startActivity(Intent.createChooser(intent, "Photo"));
			}
		//	startCameraPreview();
		//	tryAutoFocus();
	//	}
			return preview_image_name;

	//	return null;
	}
	
	public void clickedTrash() {
		if( MyDebug.LOG )
			Log.d(TAG, "clickedTrash");
		//if( is_preview_paused ) {
		if( this.phase == PHASE_PREVIEW_PAUSED ) {
			if( preview_image_name != null ) {
				if( MyDebug.LOG )
					Log.d(TAG, "Delete: " + preview_image_name);
				File file = new File(preview_image_name);
				if( !file.delete() ) {
					if( MyDebug.LOG )
						Log.e(TAG, "failed to delete " + preview_image_name);
				}
				else {
					if( MyDebug.LOG )
						Log.d(TAG, "successsfully deleted " + preview_image_name);
    	    	    showToast(null, "Photo deleted");
					MainActivity main_activity = (MainActivity)this.getContext();
    	            main_activity.broadcastFile(file);
				}
			}
			startCameraPreview();
			tryAutoFocus();
		}
    }

	
	
	//try to detect retina 
	
		 /*
		  * Scaling down the image
		  */
		 public Bitmap getScaleImage(ImageView view, int boundBoxInDp) {
		  Drawable drawing = view.getDrawable();
		  Bitmap bitmap = ((BitmapDrawable) drawing).getBitmap();

		  // Get current dimensions
		  int width = bitmap.getWidth();
		  int height = bitmap.getHeight();

		  // Determine how much to scale: the dimension requiring
		                // less scaling is.
		  // closer to the its side. This way the image always 
		                // stays inside your.
		  // bounding box AND either x/y axis touches it.
		  float xScale = ((float) boundBoxInDp) / width;
		  float yScale = ((float) boundBoxInDp) / height;
		  float scale = (xScale <= yScale) ? xScale : yScale;

		  // Create a matrix for the scaling and add the scaling data
		  Matrix matrix = new Matrix();
		  matrix.postScale(scale, scale);

		  // Create a new bitmap and convert it to a format understood

		                // by the
		  // ImageView
		  Bitmap scaledBitmap = Bitmap.
		                     createBitmap(bitmap, 0, 0, width, height,
		                     matrix, true);
		  
		  // Apply the scaled bitmap
		  view.setImageBitmap(scaledBitmap);
		  return scaledBitmap;

		 }

		 
			private int N=0;
			private double errorv=0;
			File file=null;
			private int SIZE = 256;
		    private int NUMBER_OF_COLOURS = 3;
		    Bitmap   greyBmp=null;
		    public final int RED = 0;
		    public final int GREEN = 1;
		    public final int BLUE = 2;
		    private int[][] colourBins;
		    private int maxY;
	   /// to Detect retina images
	   public void detect_retina()
	   {
	   	

	   	
	 	     Bitmap d =null;

	//String    preview_image_name  = getIntent().getExtras().getString("names");
	if(preview_image_name!= null){
	   file = new File(preview_image_name);

	  
	  String im=file.getAbsolutePath();
	  
	   d = BitmapFactory.decodeFile(im);
	  

	  }


	//Bitmap greyBmp=null;


	// greyBmp = toGrayscale(d);
	 //  image.setImageBitmap(greyBmp);
	  int h = d.getHeight(); int w = d.getWidth();
	  
	  
//	     int []histogram= new int [256];
	  // initialize all intensity values to 0
	//   for(int i = 0; i < 255; i++)
	//  {
//	      histogram[i] = 0;
	//  }

	  // resizing of image
	  // Bitmap resized = Bitmap.createScaledBitmap(yourBitmap, newWidth, newHeight, true);

	  Bitmap resized = Bitmap.createScaledBitmap(d, 1500, 1152, true);
	  
	  colourBins = new int[NUMBER_OF_COLOURS][];
	 int [][] colourBins_new = new int[NUMBER_OF_COLOURS][];

	  for (int i = 0; i < NUMBER_OF_COLOURS; i++) {
	      colourBins[i] = new int[SIZE];
	      colourBins_new[i]= new int[SIZE];
	  }
	  
	  for (int i = 0; i < NUMBER_OF_COLOURS; i++) {
	      for (int j = 0; j < SIZE; j++) {
	          colourBins[i][j] = 0;
	          colourBins_new[i][j]=0;
	      }
	  }
	  
	  for (int x = 0; x < d.getWidth(); x++) {
	      for (int y = 0; y < d.getHeight(); y++) {

	          int pixel = d.getPixel(x, y);

	          colourBins[RED][Color.red(pixel)]++;
	          colourBins[GREEN][Color.green(pixel)]++;
	          colourBins[BLUE][Color.blue(pixel)]++;
	      }
	  }
	  
	  for (int x = 0; x < resized.getWidth(); x++) {
	      for (int y = 0; y < resized.getHeight(); y++) {

	          int pixel = resized.getPixel(x, y);

	          colourBins_new[RED][Color.red(pixel)]++;
	          colourBins_new[GREEN][Color.green(pixel)]++;
	          colourBins_new[BLUE][Color.blue(pixel)]++;
	      }
	  }

	  maxY = 0;

	  for (int i = 0; i < NUMBER_OF_COLOURS; i++) {
	      for (int j = 0; j < SIZE; j++) {
	          if (maxY < colourBins[i][j]) {
	              maxY = colourBins[i][j];
	          }
	      }
	  }
	  
	 // isColored = true;
	//  view = (LinearLayout) findViewById(R.id.lyt);
	 // view.addView(new MyHistogram(getApplicationContext(),d));
	  

	  
	  
	   double[]  Rmean= {225984.933333333,27065.5500000000,23549.7500000000,3476.26666666667,6238.21666666667,2895.71666666667,2216.25000000000,1194.51666666667,1401.40000000000,1400.86666666667,1265.36666666667,1242.51666666667,1190.68333333333,1220.23333333333,1257.65000000000,1187.88333333333,1268.98333333333,1280.91666666667,1193.23333333333,1295.63333333333,1544.61666666667,1755.98333333333,1674.16666666667,1857.10000000000,1830.05000000000,1937.43333333333,2168.90000000000,2418.31666666667,2412.80000000000,2638.85000000000,2795.83333333333,2930.20000000000,2889.80000000000,3009.03333333333,2889.90000000000,3003.43333333333,3162.20000000000,3239.33333333333,3443.80000000000,3859.01666666667,4058.18333333333,4374.70000000000,4908.23333333333,5189.08333333333,5430.75000000000,5519.78333333333,5655.33333333333,6013.28333333333,6169.43333333333,6193.40000000000,6589.58333333333,6435.71666666667,6774.18333333333,6731.20000000000,6956.06666666667,7645.78333333333,7388.78333333333,7290.60000000000,7701.63333333333,7893.93333333333,7952,8383.35000000000,8504.81666666667,8902.98333333333,8844.53333333333,9164.70000000000,9174.08333333333,9404,9619.31666666667,9497.65000000000,9646.43333333333,10081.7666666667,10062.5833333333,10371.2833333333,10004.1333333333,10125.5166666667,10396.3500000000,10461.5166666667,10586.8333333333,10214.2500000000,10628.4666666667,11118.8833333333,11172,11344.5000000000,11519.3833333333,12029.2166666667,12286.3500000000,12352.4666666667,12568.8500000000,12496.6666666667,12389.4666666667,12541.5500000000,12407.1166666667,12592.6833333333,12181.9833333333,12593.5333333333, 12773.3666666667,12543.1000000000,12516.7166666667,12034.0666666667,12227.4666666667,11979.4500000000,11886.9000000000,12024.9000000000,11816.6333333333,12227.6500000000,12186.6833333333,11784.1166666667,11803.3333333333,11521.0666666667,11503.0833333333,11657.1000000000,11379.1500000000,11764.8333333333,11681.3000000000,11843.1833333333,11670.0500000000,11395.5333333333,11439.5333333333,11237.6166666667,11673.9666666667,11485.0500000000,11154.3333333333,11091.8000000000,10749.9166666667,(float) 10997.1000000000,11160.9000000000,11145.9333333333,11086,10661.6833333333,10523.7166666667,10312.8666666667,9846.21666666667,9893.98333333333,9642.33333333333,9492.25000000000,9278.68333333333,8873.31666666667,8928.75000000000,8686.85000000000,8505.33333333333,8471.81666666667,7897.61666666667,7836.76666666667,7679.30000000000,7550.31666666667,7682.08333333333,7282.15000000000,7118.76666666667,7010.18333333333,6981.45000000000,6982.73333333333,6663.90000000000,6626.63333333333,6587.38333333333,6472.58333333333,6502.86666666667,6165.91666666667,6088.18333333333,5999.35000000000,5863.66666666667,5922.56666666667,5659.90000000000,5638.80000000000,5587.86666666667,5491.23333333333,5565.71666666667,5363.05000000000,5227.56666666667,5154.76666666667,4904.73333333333,4848.58333333333,4589.66666666667,4558.60000000000,4489.11666666667,4409.58333333333,4378.83333333333,4299.15000000000,4257.96666666667,4254.95000000000,4275.35000000000,4292.68333333333,4239.35000000000,4212.35000000000,4245.35000000000,4253.51666666667,4274.15000000000,4201.70000000000,4117.81666666667,4026.78333333333,3866.16666666667,3781.90000000000,3628.78333333333,3531.41666666667,3466.73333333333,3360.73333333333,3227.40000000000,3090.61666666667,2945.36666666667,2794.91666666667,2649.26666666667,2475.31666666667,2302.30000000000,2160.65000000000,2073.21666666667,1936.23333333333,1890.21666666667,1790.91666666667,1684.41666666667,1596.46666666667,1500.93333333333,1413.28333333333,1318.56666666667,1237.95000000000,1179.71666666667,1128.81666666667,1086.90000000000,1033.68333333333,1000.55000000000,990.433333333333,  966.600000000000 , 963.733333333333,949.516666666667,917.050000000000,874.250000000000,821.050000000000,780.133333333333,739.133333333333,711,693.516666666667,678.550000000000,655.250000000000,633.250000000000,610.033333333333,588.200000000000,550.583333333333,519.166666666667,479.250000000000,442.600000000000,418.566666666667,383,371.033333333333,351.583333333333,324.800000000000,303.066666666667,275.066666666667,245.066666666667,208.883333333333,175.183333333333,147.966666666667,124.966666666667,103.150000000000,95.9500000000000,122.700000000000,224.266666666667,286.616666666667};
	   double[]  Rstd=   {55897.9890256718,14192.1264462114,21722.9733868766,2116.81063152033,9334.17500660636,4000.84820283029,3626.39365904186,1370.58822571306,2696.01452996135,3993.14472933032,3892.46303430699,3764.44061386426,3570.70986861425,3605.03117533555,3408.74222951636,2900.94059025553,3012.35641470412,2995.70527216607,2565.22105544081,2792.38098978101,3454.67329805985,3900.60656079159,3455.97971670262,3773.56573584082,3496.70494979662,3733.95972110976,4059.53731896478,4494.66364757167,4295.48036342181,4681.50357139488,4971.19896586247,5181.41215879629,5026.96534734325,5173.88790954781,4834.85364334251,4886.55816055608,4991.41804109984,4933.65060515895,5195.77859877205,5773.70037175193,5908.51558544499,6161.81971058572,6820.14863739480,7124.27312329551,7368.28530664832,7338.85038124920,7448.67966383730,7777.30974159231,7785.20870770970,7611.40588407972,7785.30412911200,7429.94483427673,7490.91969301334,7260.65459952196,7217.48345002692,7682.18500011479,7073.67358612959,6658.79594450142,6829.57286791730,6813.14648648682,6685.97987190043,6882.77868804773,6756.76058300046,6836.30532708577,6617.80681440372,6660.90935996692,6477.69548970501,6461.02301025024,6540.03543890172,6333.37927798530,6340.57261320118,6372.64589533615,6159.55546297072,6185.25503109755,5767.72585524338,5748.83611947071,5576.99704696572,5349.84004035117,5229.93831563153,4907.35388542335,4955.14237756302,5082.92756325298,5018.49756367483,5015.81048590439,4989.41565849918,5052.41483156579,5048.99512010869,5066.79320731069,5074.14720504585,4834.38977155099,4561.56346029077,4463.57978789238,4309.99430213815,4388.28209115844,4200.89704904401,4295.26443007806,4226.62869607247,4130.03803774497,4121.35283487692,4012.11050537552,4111.81930729199,4113.94373215725,4076.03721935403,4207.45065411107,4187.10946356636,4526.47226287869,4821.42631154761,4633.08366795742,4988.56925131257,4981.69955297971,5045.65701250621,5375.03464616347,5216.26997983639,5624.94137722120,5873.77628741309,5975.17828538115,6124.61184774947,5906.30492898261,6084.88106499554,5973.43179361227,6358.40096348362,6332.12158310470,6096.24084688891,6215.33625581765,6111.70292863234,6279.51807958735,6573.39565707152,6695.81786467017,6711.92009709819,6425.99436212719,6402.94537386775,6339.65769130620,6052.67096970537,6035.28066973106,5903.01091993772,5740.22030411991,5673.34060109110,5374.74920342499,5410.94024419705,5248.15238457258,5144.92445190590,5221.28595593230,4961.91584476853,5037.18661174886,5080.68427027672,5120.72876001217,5379.99899909154,5120.78452286962,5135.39547555935,5161.93090101332,5269.34017489985,5390.34570089352,5181.42529864358,5225.05685952897,5220.94083699924,5149.07472777973,5201.54563882394,4969.57735232429,4985.41876415159,4980.34648861002,4899.79694725136,4967.44695319118,4791.80011361217,4803.31690411490,4789.38435400579,4704.91289339806,4784.05309367563,4669.61172278011,4608.68580615852,4631.27875362575,4541.42013503902,4651.03702776228,4549.61003606089,4670.69614526100,4778.17820432222,4872.57298226657,5022.52295695175,5177.24199473367,5146.23588940291,5287.43255572645,5380.47661496984,5408.09377266656,5374.63786663414,5262.94975557064,5383.75383760238,5456.35338115637,5560.08129383625,5511.87433529971,5455.51954521218,5425.91143352555,5269.33402400125,5281.21074555665,5101.42607543296,5044.74316522728,5132.08703001185,5141.26691128824,5070.40292652252,5003.41453609997,4923.10255272351,4803.44385740512,4686.00067950930,4464.75603938703,4220.26213058472,4054.91302682126,3963.68240912355,3715.28262203320,3718.22654141418,3533.81237112184,3312.15994502265,3189.75340668840,3023.05846289093,2885.43581346316,2782.69017725633,2694.80946419317,2685.03191971474,2637.84628695757,2583.43595079584,2493.17987338929,2443.44113714204,2438.60019145929,2476.67071636262,2564.76804294741,2553.80359392176,2517.61382470366,2404.77942626409,2249.96827416427,2117.07031713253,1990.86487863234,1936.67330531647,1913.51718483704,1883.69776467566,1826.36155502163,1786.36685192628,1727.97952913434,1677.35284585311,1569.29896139199,1459.72745501955,1339.29840759118,1238.47977067568,1193.48550035098,1111.57689182561,1109.29496568945,1059.93716301865,962.047248566605,889.787879247912,779.611243117671,704.802890263862,598.572145423454,495.495386137566,394.454825093506,302.693051812920,234.955158000812,224.877625854974,348.339270622230,726.239580086087,957.677648106927};
	 
	   double[]   Gmean={243345.283333333,39999.7666666667,4738.65000000000,2159.75000000000,1826.43333333333,2079.98333333333,2879.61666666667,3553.56666666667,4663.05000000000,5200.98333333333,5912.83333333333,7575.90000000000,8645.50000000000,9778.70000000000,10591.9833333333,12481.9500000000,12571.4166666667,13504.2000000000,15551.3833333333,16601.7333333333,17271.8666666667,18054.4166666667,18938.2333333333,20117.1166666667,23162,26072.2166666667,25290.6333333333,26731.8666666667,28383.7500000000,29121.1666666667,29962.5500000000,32393.9833333333,31204.6500000000,31053.2666666667,29973.1666666667,30137.8833333333,31381.0166666667,31614.5500000000,30561.3500000000,31545.3333333333,29775.9833333333,27743.4666666667,28750.2833333333,29806.6500000000,30250.2666666667,28870.4833333333,28079.4666666667,27016.6500000000,26398.1500000000,25928.3833333333,26838.5000000000,26150.9000000000,24137.0666666667,23394.9000000000,23173.2666666667,22567.3000000000,22787.2500000000,21223.2500000000,19377.7666666667,18761.2166666667,17905.2833333333,16138.0500000000,15297.2166666667,14279.5500000000,13135.8833333333,12431.6666666667,11175.2000000000,10099.8666666667,9667.70000000000,8875.15000000000,7782.68333333333,7138.91666666667,6710.50000000000,6267.13333333333,6036.40000000000,5476.18333333333,4981.36666666667,4803.01666666667,4622.88333333333,4349.76666666667,4009.15000000000,3710.13333333333,3627.85000000000,3231.10000000000,2915,2776.73333333333,2542.11666666667,2384.98333333333,2176.86666666667,2002.60000000000,1866.95000000000,1766.30000000000,1676,1568.96666666667,1478.70000000000,1382.18333333333,1289.40000000000,1201.53333333333,1123.61666666667,1052.06666666667,950.966666666667,901.566666666667,833.616666666667,769.350000000000,726.800000000000,691.250000000000,626.450000000000,588.083333333333,539.700000000000,498.616666666667,467.533333333333,439.516666666667,397.616666666667,363.083333333333,348.050000000000,330.583333333333,311.950000000000,298.883333333333,273.333333333333,257.033333333333,247.816666666667,247.800000000000,232.500000000000,234.483333333333,225.133333333333,216.500000000000,213.583333333333,206.300000000000,208.033333333333,200.583333333333,190.933333333333,182.516666666667,172.533333333333,167.200000000000,168.216666666667,165.250000000000,157.916666666667,153.933333333333,148.750000000000,146.966666666667,142.500000000000,139.333333333333,134.666666666667,135.566666666667,131.683333333333,128.633333333333,123.783333333333,120.900000000000,114.716666666667,108.266666666667,104.900000000000,104.966666666667,101.233333333333,98.9166666666667,100.683333333333,92.3500000000000,91.1833333333333,87.3666666666667,82.8500000000000,82.6333333333333,78.6833333333333,74.5000000000000,70.0333333333333,68.8000000000000,67.7333333333333,63.8333333333333,60.1833333333333,58.8333333333333,57.3666666666667,57.7166666666667,55.4500000000000,52.4500000000000,52.1166666666667,51.2166666666667,50.2500000000000,49.6000000000000,47.6500000000000,45.8166666666667,46.3833333333333,45.4833333333333,42.4000000000000,42.2000000000000,42.3333333333333,41.5000000000000,41.1166666666667,39.1500000000000,37.6500000000000,36.2833333333333,35.6500000000000,33.7166666666667,34.6000000000000,33.5666666666667,34.7333333333333,33.2666666666667,35.3666666666667,34.3000000000000,32.9500000000000,31.9666666666667,32.4333333333333,31.4166666666667,33.2833333333333,31.3833333333333,32.8833333333333,31.1833333333333,33.7333333333333,32.2833333333333,30.3000000000000,29.7500000000000,29.5166666666667,27.5500000000000,27.4833333333333,26.2166666666667,26.7666666666667,26.8166666666667,26.6500000000000,28.1833333333333,25.9666666666667,26.2333333333333,24.4500000000000,23.3666666666667,21.4166666666667,19.2166666666667,19.0666666666667,19.6000000000000,18.8833333333333,16.7666666666667,14.1000000000000,12.3833333333333,11.1166666666667,10.4000000000000,9.91666666666667,7.28333333333333,4.86666666666667,3.76666666666667,3.01666666666667,1.78333333333333,1.35000000000000,0.416666666666667,0.250000000000000,0.0666666666666667,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};      
	   double [] Gstd={31044.8047091109,27131.6418904836,2437.12202336684,2350.93092001402,2973.86664562968,3967.71429095301,6465.73471146470,8345.44214459010,10920.0397985011,11733.2710807326,12284.7135479344,15385.1123996269,17029.5340914801,18203.9645535745,18318.6127328830,19350.3788597125,17687.7428126513,17645.5621902554,19491.1789079944,20220.3582935838,20208.3449137086,19582.8098902156,18496.2058329761,17607.8000311030,18336.1694888934,19459.8073579850,17879.0850721120,17723.9943421213,17410.3824826073,16716.9323671648,16091.0327584884,16337.2222744194,15002.6481163341,14143.3803499833,13092.5551405119,13013.7664464117,13751.0924416463,14138.8836365883,13972.8356631231,14470.8077898366,13668.5617440995,12405.0299618664,12637.1154843904,13100.3599301375,13579.1257386471,12752.9225756317,12818.7500758594,12729.9972916303,12936.9383560395,13169.7210452571,14414.2325174737,14501.9612276222,14193.8164190556,14873.2111446232,15326.7861124847,15318.9023285790,15994.3032259053,15172.4418485722,14107.2843851782,14029.3962554075,13810.0952022292,13048.2842659713,13200.1428691442,12871.0516900379,12410.8648424959,12410.4304445579,11580.1547672688,10902.9732913591,10850.0465910114,10174.8116491541,9145.95740369623,8566.46051800259,8240.33876525120,7893.96523734185,7787.72806494675,7228.09432064857,6774.11467219816,6720.27644329411,6678.69728770023,6456.17101816698,6194.66147917103,5752.31796304956,5724.34003545609,5142.48748323749,4724.97247413738,4648.22547830623,4355.49443024795,4276.63159662799,3976.37719226198,3753.10840752623,3602.16260102696,3538.30130793898,3474.30825696379,3352.45358651205,3138.42629408273,3007.39195808395,2838.56579446233,2620.60389900500,2545.73435238987,2456.11849536557,2179.72231139709,2120.44326487365,1947.01269738677,1717.39859271841,1664.41497636837,1673.49748066635,1486.54172009844,1397.30316877914,1277.86973554308,1195.64327897788,1088.39339639247,1014.56991792660,833.467173037471,709.279397786439,710.668017681907,665.124310839753,593.543750618524,577.368140105635,502.485944374353,441.520445681514,424.763243792158,432.052366568715,393.802432613364,401.007311809016,373.577912218021,341.100040000196,337.392207100919,317.588439571016,313.719510189351,295.959244312093,273.765820170790,261.585873489070,242.880836141866,230.841555463512,228.534245601024,228.264834667460,224.898266391693,212.907385504261,207.178349843964,209.587404740811,213.507551567319,207.510723975827,203.955185063102,205.924897175733,204.319581153980,202.403520993916,195.109364522295,192.903919177134,197.587491169524,188.163728440327,185.672182413074,193.596729789815,184.823449120053,178.439401636794,177.864211362533,162.096939504144,155.752138126812,153.255958124195,147.279490442779,150.244317038149,144.871830164257,139.178278550710,131.501107382934,132.486366423627,131.072869714298,127.486412259246,125.762015184541,117.307385099447,116.553031018280,120.469715114991,115.349200845057,111.687582576108,116.448445225689,116.030547068896,118.089828635317,117.534300799413,115.051768009161,116.139672146634,115.511110576718,117.762543564016,110.153390740029,111.442285739514,113.415789213574,110.961361422855,113.013737228090,103.838339968711,104.471360928425,101.543359973965,107.124800496225,101.449510138515,103.528952799672,106.603870993198,106.307017270282,104.880863269836,110.754724682733,111.588119773461,107.031505737044,108.872023560345,101.965990785341,108.669502319939,116.788246980458,106.731421463801,115.636498206726,108.342127848158,115.768598339886,121.163021106342,113.794685765525,109.095141963334,124.334089529819,116.766959716163,124.630231616444,116.433889246094,126.726499398182,129.674232201988,132.371181384080,137.876448212707,131.916764546291,137.780514066747,128.390086871143,125.275150276646,107.439741355789,95.7997720648834,97.2191071871157,100.035451343130,97.5072516497813,86.6348985670181,75.8780690050307,62.9199361218837,56.0729221538351,51.2330004878013,48.4926315096888,34.3842157651634,22.3686119877807,16.5993329115162,13.3066717899065,7.93851727321766,6.95219755526680,1.92478628940644,1.38545715394729,0.516397779494322,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};



	  double[]   Bmean={516238.333333333,110140.850000000,93610.5666666667,87493.0333333333,87372.8166666667,89956.0166666667,87959.6333333333,85642.9500000000,79428.4166666667,73223.4833333333,65057.7333333333,56480.5666666667,47929.7833333333,39972.8666666667,33045.5333333333,27351.3666666667,22324.4666666667,18511.9166666667,15248.4833333333,12661.2333333333,10636.4500000000,8892.93333333333,7649.75000000000,6490.50000000000,5559.76666666667,4732.23333333333,4048.61666666667,3475.76666666667,3023.63333333333,2616.55000000000,2268.73333333333,1965.50000000000,1712.25000000000,1465.21666666667,1279.46666666667,1117.83333333333,960.816666666667,832.466666666667,727.750000000000,627.816666666667,545.650000000000,477.866666666667,423.350000000000,384.666666666667,345.716666666667,316.383333333333,293.133333333333,277.133333333333,262.133333333333,242.783333333333,228.383333333333,208.916666666667,194.350000000000,177.683333333333,167.550000000000,153.466666666667,142.800000000000,133.450000000000,124.450000000000,120.400000000000,114.700000000000,110.966666666667,107.750000000000,100.166666666667,95.6000000000000,94.9166666666667,91.2833333333333,89.9166666666667,88.6166666666667,81.8333333333333,79.5833333333333,75.8500000000000,74.9333333333333,73.4000000000000,67.8500000000000,65.9500000000000,61.7166666666667,57.6666666666667,56.3833333333333,52.3666666666667,48.9000000000000,47.7333333333333,43.7166666666667,42.9333333333333,42.2000000000000,39.9000000000000,38.7666666666667,36.2833333333333,34.5666666666667,33.1666666666667,31.0166666666667,29.6666666666667,29.3666666666667,29.6666666666667,29.8666666666667,28.8166666666667,29.5000000000000,27.2000000000000,27.9166666666667,25.9500000000000,25.6666666666667,24.9833333333333,25.0166666666667,25.5000000000000,23.7500000000000,23.5000000000000,22.7166666666667,22.1000000000000,21.5333333333333,21.3833333333333,19.0333333333333,19.3333333333333,19.1666666666667,18.9333333333333,17.8333333333333,18.1666666666667,17.2666666666667,16.9333333333333,15.8333333333333,15.2333333333333,14.0833333333333,12.1166666666667,11.3666666666667,11.2666666666667,10.3000000000000,10.5833333333333,10.5500000000000,10.3500000000000,10.3333333333333,10.0333333333333,9.83333333333333,9.11666666666667,10.7166666666667,10.0333333333333,9.86666666666667,8.65000000000000,8.35000000000000,7.81666666666667,6.68333333333333,6.65000000000000,6.78333333333333,6.96666666666667,5.55000000000000,5.41666666666667,5.13333333333333,5.16666666666667,4.88333333333333,4.71666666666667,5.15000000000000,4.48333333333333,4.93333333333333,4.21666666666667,4.13333333333333,3.56666666666667,4.75000000000000,3.40000000000000,2.90000000000000,2.45000000000000,2.63333333333333,1.50000000000000,1.06666666666667,0.933333333333333,0.600000000000000,0.766666666666667,0.733333333333333,0.516666666666667,0.500000000000000,0.300000000000000,0.133333333333333,0.0833333333333333,0.0666666666666667,0.0166666666666667,0,0.0166666666666667,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
	  double[]   Bstd={351699.037388005,61948.9294584250,49189.5552846046,46408.4755829264,43982.1184452491,42722.3196398147,40575.4851027182,39304.3659792568,37518.3126257175,36749.7895128259,35604.5965109471,34198.7917907144,32551.3103587383,30684.2302806201,28882.3143548157,26964.4695334288,24363.8541393658,21853.2205579784,19034.3753892095,16313.0267249117,14017.4545543725,11984.1904627024,10553.2420107018,9233.36244738317,8178.20466678056,7165.88854146508,6310.67772312461,5485.18512076298,4790.55011160007,4164.85805575367,3575.34807060637,3081.49939015497,2636.77978117377,2239.32485354794,1902.20376625681,1628.38223937567,1358.90849134631,1145.42237737826,956.145167068513,790.238120981965,667.020514638907,580.775824386734,511.981224000269,468.345796725811,428.251632494802,394.899819592418,365.435819061443,351.831357264816,341.547746701448,322.427765746522,309.117721619465,292.090146966313,277.116742516671,263.646366364445,249.736635854925,230.132862632169,219.274597599773,209.561562088768,204.250048233507,205.536932225025,200.310377808108,201.334443630717,203.126964136527,200.513591970649,197.104394605705,205.625238481479,207.193604022959,204.723152596475,209.888920716526,201.651332470196,205.654086045828,200.818564288046,196.161025748233,190.105965098683,187.260882555843,183.908177622993,171.371248470273,166.298044330120,164.587526729100,158.738028153058,147.507850983244,143.416410032697,130.596167782780,130.994613250183,127.563450479771,127.626932495869,125.415427195107,115.564508607606,115.578921113020,109.388464465830,106.443984101557,109.602775337041,103.756881195804,108.767933127536,109.703369950659,109.182399349362,109.444436796772,98.6341983530345,100.674385624666,87.1259413780818,92.4472669966032,90.9754095414961,88.4898714239814,92.4778489281238,87.5898811971771,87.3183344173704,84.8927721797420,81.5391556143739,80.3963627364008,84.7286979711538,72.7132670771954,75.1431778349920,72.8814144001868,70.2281672851142,64.7606196164563,63.2064592994667,58.8421809579404,56.9355468681486,53.5005676087953,53.1618464775628,50.8614907659798,44.0600867332085,41.9575922572137,44.5241761281105,40.2093251685287,43.6497196164978,43.3864873910714,44.5705781954639,45.5712021267471,44.3911606205891,44.8674759205399,40.5040661974952,48.1294482132378,45.1614680019416,43.4372591242356,39.8309244478820,38.8014611816425,36.5955872428592,31.9111540282145,32.8317875112891,32.9776974798643,33.1902408908926,27.1333336803676,24.8604864254651,24.2014614117263,25.9322236992646,23.6600870140861,23.4622608376904,25.2618993609355,23.4097558010900,26.0916680425560,21.1732895906705,23.6897078894402,21.2358560439862,27.4693203287237,21.2716091523938,17.5448094598674,15.9218696202233,17.0074427044348,10.3865950114657,7.63326055278259,6.72427090538741,4.27130750433754,5.43799148697655,5.30067685689695,3.74840832510966,3.61962635022653,2.32379000772445,1.03279555898864,0.645497224367903,0.516397779494322,0.129099444873581,0,0.129099444873581,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
	  
	  
	  
	  //To determine number of pixels which in range of retina histogram 
	   N=0;
	  for (int i = 0 ; i<256;i++)
	  {

	 if (colourBins_new[RED][i] > Rmean[i]-Rstd[i] && colourBins_new[RED][i] < Rmean[i]+Rstd[i])
	      N=N+1;
	 
	  if( colourBins_new[GREEN][i] > Gmean[i]-Gstd[i] && colourBins_new[GREEN][i] < Gmean[i]+Gstd[i])
	      N=N+1;
	  
	  if( (colourBins_new[BLUE][i] >( Bmean[i]-Bstd[i] ))&& (colourBins_new[BLUE][i] < ( Bmean[i]+Bstd[i] )))
	      N=N+1;
	  

	  }
	  
	  

	  errorv=0;
	  errorv=1-((double)N/(3*256));

	  if (errorv>.4)
	     // disp('Not Retina image...');
	  {
			final MainActivity main_activity = (MainActivity)this.getContext();
	      AlertDialog.Builder alertDialog = new AlertDialog.Builder(getContext());
	      alertDialog.setTitle("MEID");
	     // alertDialog.setMessage("Not Retina image... \n"+" Number of corrected pixels :"+N+"\n Error : "+errorv);
	      alertDialog.setMessage("Not Retina image... \n");
	      alertDialog.setPositiveButton("OK", null);
	      alertDialog.setNegativeButton("Delete", new DialogInterface.OnClickListener() {

	          public void onClick(DialogInterface dialog, int which) {
	              // TODO Auto-generated method stub
	              if (file != null) {
	              	file.delete(); 	
	               main_activity.broadcastFile(file);
	               startCameraPreview();
	               tryAutoFocus();
	              }
	          }
	      });

	      
	      alertDialog.show();

	  	
	  }
	  
	  else
	    //  disp('Retina image...'); 
	  {
		   final MainActivity main_activity = (MainActivity)this.getContext();
	      AlertDialog.Builder alertDialog = new AlertDialog.Builder(getContext());
	      alertDialog.setTitle("MEID");
	    //  alertDialog.setMessage("Retina image..."+" Number of corrected pixels :"+N+"\n Error : "+errorv);
	      alertDialog.setMessage("Retina image...");
	      alertDialog.setPositiveButton("OK", null);
	      alertDialog.setNegativeButton("Delete", new DialogInterface.OnClickListener() {

	          public void onClick(DialogInterface dialog, int which) {
	              // TODO Auto-generated method stub
	              if (file != null) {
	              	file.delete();
					//	MainActivity main_activity = (MainActivity)histogram.this.getBaseContext();
	  	         //   main_activity.broadcastFile(file);
				//	MainActivity main_activity = (MainActivity)this.getContext();
		            main_activity.broadcastFile(file);
	               startCameraPreview();
	               tryAutoFocus();
	              }
	          }
	      });

	      alertDialog.show();
			
	       

	  }
	   }

	   
	   protected Dialog onCreateDialog(int id) {
	          ProgressDialog dataLoadProgress = new ProgressDialog(getContext());
	          dataLoadProgress.setMessage("Loading...");
	          dataLoadProgress.setIndeterminate(true);
	          dataLoadProgress.setCancelable(false);
	          dataLoadProgress.setProgressStyle(android.R.attr.progressBarStyleLarge);
	          return dataLoadProgress;

	      }
		
		
    private void tryAutoFocus() {
		if( MyDebug.LOG )
			Log.d(TAG, "tryAutoFocus");
		if( camera == null ) {
			if( MyDebug.LOG )
				Log.d(TAG, "no camera");
		}
		else if( !this.has_surface ) {
			if( MyDebug.LOG )
				Log.d(TAG, "preview surface not yet available");
		}
		else if( !this.is_preview_started ) {
			if( MyDebug.LOG )
				Log.d(TAG, "preview not yet started");
		}
		//else if( is_taking_photo ) {
		else if( this.isTakingPhotoOrOnTimer() ) {
			if( MyDebug.LOG )
				Log.d(TAG, "currently taking a photo");
		}
		else {
			// it's only worth doing autofocus when autofocus has an effect (i.e., auto or macro mode)
            Camera.Parameters parameters = camera.getParameters();
			String focus_mode = parameters.getFocusMode();
	        if( focus_mode.equals(Camera.Parameters.FOCUS_MODE_AUTO) || focus_mode.equals(Camera.Parameters.FOCUS_MODE_MACRO) ) {
				if( MyDebug.LOG )
					Log.d(TAG, "try to start autofocus");
		        Camera.AutoFocusCallback autoFocusCallback = new Camera.AutoFocusCallback() {
					@Override
					public void onAutoFocus(boolean success, Camera camera) {
						if( MyDebug.LOG )
							Log.d(TAG, "autofocus complete: " + success);
						focus_success = success ? FOCUS_SUCCESS : FOCUS_FAILED;
						focus_complete_time = System.currentTimeMillis();
					}
		        };
	
				this.focus_success = FOCUS_WAITING;
	    		this.focus_complete_time = -1;
	    		try {
	    			camera.autoFocus(autoFocusCallback);
	    			count_cameraAutoFocus++;
	    		}
	    		catch(RuntimeException e) {
	    			// just in case? We got a RuntimeException report here from 1 user on Google Play
	    			autoFocusCallback.onAutoFocus(false, camera);

	    			if( MyDebug.LOG )
						Log.e(TAG, "runtime exception from autoFocus");
	    			e.printStackTrace();
	    		}
	        }
	        else if( has_focus_area ) {
	        	// do this so we get the focus box, for focus modes that support focus area, but don't support autofocus
				focus_success = FOCUS_SUCCESS;
				focus_complete_time = System.currentTimeMillis();
	        }
		}
    }
    
    private void startCameraPreview() {
		long debug_time = 0;
		if( MyDebug.LOG ) {
			Log.d(TAG, "startCameraPreview");
			debug_time = System.currentTimeMillis();
		}
		//if( camera != null && !is_taking_photo && !is_preview_started ) {
		if( camera != null && !this.isTakingPhotoOrOnTimer() && !is_preview_started ) {
			if( MyDebug.LOG )
				Log.d(TAG, "starting the camera preview");
	    	count_cameraStartPreview++;
			camera.startPreview();
			this.is_preview_started = true;
			if( MyDebug.LOG ) {
				Log.d(TAG, "time after starting camera preview: " + (System.currentTimeMillis() - debug_time));
			}
			if( this.using_face_detection ) {
				if( MyDebug.LOG )
					Log.d(TAG, "start face detection");
				try {
					camera.startFaceDetection();
				}
				catch(RuntimeException e) {
					// I didn't think this could happen, as we only call startFaceDetection() after we've called takePicture() or stopPreview(), which the Android docs say stops the face detection
					// however I had a crash reported on Google Play for Open Camera v1.4
					// 2 Jan 2014, "maxx_ax5", Android 4.0.3-4.0.4
					// startCameraPreview() was called after taking photo in burst mode, but I tested with burst mode and face detection, and can't reproduce the crash on Galaxy Nexus
					if( MyDebug.LOG )
						Log.d(TAG, "face detection already started");
				}
				faces_detected = null;
			}
		}
		this.setPreviewPaused(false);
    }

    private void setPreviewPaused(boolean paused) {
		if( MyDebug.LOG )
			Log.d(TAG, "setPreviewPaused: " + paused);
		Activity activity = (Activity)this.getContext();
	    View shareButton = (View) activity.findViewById(R.id.share);
	    View trashButton = (View) activity.findViewById(R.id.trash);
	    View histButton = (View) activity.findViewById(R.id.histogram);
	    View pic = (View) activity.findViewById(R.id.take_photo);
	    //burst mode
	    View gallerybutt = (View) activity.findViewById(R.id.gallery1);
	    View picswitch = (View) activity.findViewById(R.id.switcher1);
	    View insert = (View) activity.findViewById(R.id.insert);
	    
		/*is_preview_paused = paused;
		if( is_preview_paused ) {*/
	    if( paused ) {
	    	this.phase = PHASE_PREVIEW_PAUSED;
		    shareButton.setVisibility(View.VISIBLE);
		    trashButton.setVisibility(View.VISIBLE);
		    histButton.setVisibility(View.VISIBLE);
		    pic.setVisibility(View.GONE);
		    insert.setVisibility(View.VISIBLE);
		    gallerybutt.setVisibility(View.VISIBLE);
		    picswitch.setVisibility(View.VISIBLE);
		    // shouldn't call showGUI(false), as should already have been disabled when we started to take a photo
		}
		else {
	    	this.phase = PHASE_NORMAL;
	    	pic.setVisibility(View.VISIBLE);
			shareButton.setVisibility(View.GONE);
		    trashButton.setVisibility(View.GONE);
		    histButton.setVisibility(View.GONE);
		    insert.setVisibility(View.GONE);
		    
		    gallerybutt.setVisibility(View.GONE);
		    picswitch.setVisibility(View.GONE);
		    preview_image_name = null;
			showGUI(true);
		}
    }
    
    private void showGUI(final boolean show) {
		if( MyDebug.LOG )
			Log.d(TAG, "showGUI: " + show);
		final Activity activity = (Activity)this.getContext();
		activity.runOnUiThread(new Runnable() {
			public void run() {
		    	final int visibility = show ? View.VISIBLE : View.GONE;
			  //  View switchCameraButton = (View) activity.findViewById(R.id.switch_camera);
			  //  View switchVideoButton = (View) activity.findViewById(R.id.switch_video);
			    View flashButton = (View) activity.findViewById(R.id.flash);
			    View focusButton = (View) activity.findViewById(R.id.focus_mode);
			    View exposureButton = (View) activity.findViewById(R.id.exposure);
			//    switchCameraButton.setVisibility(visibility);
			    if( !is_video )
			    //	switchVideoButton.setVisibility(visibility); // still allow switch video when recording video
			    if( supported_flash_values != null )
			    	flashButton.setVisibility(visibility);
			    if( supported_focus_values != null )
			    	focusButton.setVisibility(visibility);
			    if( exposures != null && !is_video ) // still allow exposure when recording video
			    	exposureButton.setVisibility(visibility);
			}
		});
    }

    public void onAccelerometerSensorChanged(SensorEvent event) {
		/*if( MyDebug.LOG )
    	Log.d(TAG, "onAccelerometerSensorChanged: " + event.values[0] + ", " + event.values[1] + ", " + event.values[2]);*/

    	this.has_gravity = true;
    	for(int i=0;i<3;i++) {
    		//this.gravity[i] = event.values[i];
    		this.gravity[i] = sensor_alpha * this.gravity[i] + (1.0f-sensor_alpha) * event.values[i];
    	}
    	calculateGeoDirection();
    	
		double x = gravity[0];
		double y = gravity[1];
		this.has_level_angle = true;
		this.level_angle = Math.atan2(-x, y) * 180.0 / Math.PI;
		if( this.level_angle < -0.0 ) {
			this.level_angle += 360.0;
		}
		this.level_angle -= (float)this.current_orientation;
		if( this.level_angle < -180.0 ) {
			this.level_angle += 360.0;
		}
		else if( this.level_angle > 180.0 ) {
			this.level_angle -= 360.0;
		}

		this.invalidate();
	}

    public void onMagneticSensorChanged(SensorEvent event) {
    	this.has_geomagnetic = true;
    	for(int i=0;i<3;i++) {
    		//this.geomagnetic[i] = event.values[i];
    		this.geomagnetic[i] = sensor_alpha * this.geomagnetic[i] + (1.0f-sensor_alpha) * event.values[i];
    	}
    	calculateGeoDirection();
    }
    
    private void calculateGeoDirection() {
    	if( !this.has_gravity || !this.has_geomagnetic ) {
    		return;
    	}
    	if( !SensorManager.getRotationMatrix(this.deviceRotation, this.deviceInclination, this.gravity, this.geomagnetic) ) {
    		return;
    	}
        SensorManager.remapCoordinateSystem(this.deviceRotation, SensorManager.AXIS_X, SensorManager.AXIS_Z, this.cameraRotation);
    	this.has_geo_direction = true;
    	SensorManager.getOrientation(cameraRotation, geo_direction);
    	//SensorManager.getOrientation(deviceRotation, geo_direction);
		/*if( MyDebug.LOG ) {
			Log.d(TAG, "geo_direction: " + (geo_direction[0]*180/Math.PI) + ", " + (geo_direction[1]*180/Math.PI) + ", " + (geo_direction[2]*180/Math.PI));
		}*/
    }
    
    public boolean supportsFaceDetection() {
		if( MyDebug.LOG )
			Log.d(TAG, "supportsFaceDetection");
    	return supports_face_detection;
    }

    List<String> getSupportedColorEffects() {
		if( MyDebug.LOG )
			Log.d(TAG, "getSupportedColorEffects");
		return this.color_effects;
    }

    List<String> getSupportedSceneModes() {
		if( MyDebug.LOG )
			Log.d(TAG, "getSupportedSceneModes");
		return this.scene_modes;
    }

    List<String> getSupportedWhiteBalances() {
		if( MyDebug.LOG )
			Log.d(TAG, "getSupportedWhiteBalances");
		return this.white_balances;
    }
    
    public boolean supportsExposures() {
    	return this.exposures != null;
    }
    
    int getMinimumExposure() {
    	return this.min_exposure;
    }
    
    int getMaximumExposure() {
    	return this.max_exposure;
    }
    
    int getCurrentExposure() {
    	if( camera == null )
    		return 0;
		Camera.Parameters parameters = camera.getParameters();
		int current_exposure = parameters.getExposureCompensation();
		return current_exposure;
    }
    
    List<String> getSupportedExposures() {
		if( MyDebug.LOG )
			Log.d(TAG, "getSupportedExposures");
    	return this.exposures;
    }

    /*List<Camera.Size> getSupportedPictureSizes() {
		if( MyDebug.LOG )
			Log.d(TAG, "getSupportedPictureSizes");
    	if( this.camera == null )
    		return new Vector<Camera.Size>();
		Camera.Parameters parameters = camera.getParameters();
    	List<Camera.Size> sizes = parameters.getSupportedPictureSizes();
    	for(Camera.Size size : sizes) {
			Log.d(TAG, "    size: " + size.width + " x " + size.height);
    	}
    	return sizes;
    }*/
    
    public List<Camera.Size> getSupportedPictureSizes() {
		if( MyDebug.LOG )
			Log.d(TAG, "getSupportedPictureSizes");
		return this.sizes;
    }
    
    int getCurrentPictureSizeIndex() {
		if( MyDebug.LOG )
			Log.d(TAG, "getCurrentPictureSizeIndex");
    	return this.current_size_index;
    }
    
    List<Integer> getSupportedVideoQuality() {
		if( MyDebug.LOG )
			Log.d(TAG, "getSupportedVideoQuality");
		return this.video_quality;
    }
    
    public int getCameraId() {
    	return this.cameraId;
    }
    
    private int getImageQuality(){
		if( MyDebug.LOG )
			Log.d(TAG, "getImageQuality");
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getContext());
		String image_quality_s = sharedPreferences.getString("preference_quality", "90");
		int image_quality = 0;
		try {
			image_quality = Integer.parseInt(image_quality_s);
		}
		catch(NumberFormatException exception) {
			if( MyDebug.LOG )
				Log.e(TAG, "image_quality_s invalid format: " + image_quality_s);
			image_quality = 90;
		}
		return image_quality;
    }
    
    public void onResume() {
		if( MyDebug.LOG )
			Log.d(TAG, "onResume");
		this.app_is_paused = false;
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getContext());
		String ui_placement = sharedPreferences.getString("preference_ui_placement", "ui_right");
		this.ui_placement_right = ui_placement.equals("ui_right");
		this.openCamera();
    }

    public void onPause() {
		if( MyDebug.LOG )
			Log.d(TAG, "onPause");
		this.app_is_paused = true;
		this.closeCamera();
    }
    
    public void showToast(final ToastBoxer clear_toast, final String message) {
		class RotatedTextView extends View {
			private String text = "";
			private Paint paint = new Paint();
			private Rect bounds = new Rect();
			private Rect rect = new Rect();

			public RotatedTextView(String text, Context context) {
				super(context);

				this.text = text;
			}

			@Override 
			protected void onDraw(Canvas canvas) { 
				final float scale = getResources().getDisplayMetrics().density;
				paint.setTextSize(14 * scale + 0.5f); // convert dps to pixels
				paint.setShadowLayer(1, 0, 1, Color.BLACK);
				paint.getTextBounds(text, 0, text.length(), bounds);
				/*if( MyDebug.LOG ) {
					Log.d(TAG, "bounds: " + bounds);
				}*/
				final int padding = (int) (14 * scale + 0.5f); // convert dps to pixels
				final int offset_y = (int) (32 * scale + 0.5f); // convert dps to pixels
				canvas.save();
				canvas.rotate(ui_rotation, canvas.getWidth()/2, canvas.getHeight()/2);

				rect.left = canvas.getWidth()/2 - bounds.width()/2 + bounds.left - padding;
				rect.top = canvas.getHeight()/2 + bounds.top - padding + offset_y;
				rect.right = canvas.getWidth()/2 - bounds.width()/2 + bounds.right + padding;
				rect.bottom = canvas.getHeight()/2 + bounds.bottom + padding + offset_y;

				paint.setStyle(Paint.Style.FILL);
				paint.setColor(Color.rgb(75, 75, 75));
				canvas.drawRect(rect.left, rect.top, rect.right, rect.bottom, paint);

				paint.setStyle(Paint.Style.STROKE);
				paint.setColor(Color.rgb(150, 150, 150));
				canvas.drawLine(rect.left, rect.top, rect.right, rect.top, paint);
				canvas.drawLine(rect.left, rect.top, rect.left, rect.bottom, paint);

				paint.setStyle(Paint.Style.FILL); // needed for Android 4.4!
				paint.setColor(Color.WHITE);
				canvas.drawText(text, canvas.getWidth()/2 - bounds.width()/2, canvas.getHeight()/2 + offset_y, paint);
				canvas.restore();
			} 
		}

		if( MyDebug.LOG )
			Log.d(TAG, "showToast");
		final Activity activity = (Activity)this.getContext();
		// We get a crash on emulator at least if Toast constructor isn't run on main thread (e.g., the toast for taking a photo when on timer).
		// Also see http://stackoverflow.com/questions/13267239/toast-from-a-non-ui-thread
		activity.runOnUiThread(new Runnable() {
			public void run() {
				if( clear_toast != null && clear_toast.toast != null )
					clear_toast.toast.cancel();
				/*clear_toast = Toast.makeText(activity.getApplicationContext(), message, Toast.LENGTH_SHORT);
				clear_toast.show();*/

				Toast toast = new Toast(activity);
				if( clear_toast != null )
					clear_toast.toast = toast;
				View text = new RotatedTextView(message, activity);
				toast.setView(text);
				toast.setDuration(Toast.LENGTH_SHORT);
				toast.show();
			}
		});
	}
	
	public void setUIRotation(int ui_rotation) {
		if( MyDebug.LOG )
			Log.d(TAG, "setUIRotation");
		this.ui_rotation = ui_rotation;
	}

    void locationChanged(Location location) {
		if( MyDebug.LOG )
			Log.d(TAG, "locationChanged");
		this.has_received_location = true;
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getContext());
		boolean store_location = sharedPreferences.getBoolean("preference_location", false);
		if( store_location ) {
			this.location = location;
		}
		updateParametersFromLocation();
    }
    
    private void updateParametersFromLocation() {
    	if( camera != null ) {
    		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getContext());
    		boolean store_location = sharedPreferences.getBoolean("preference_location", false);
    		// Android camera source claims we need to check lat/long != 0.0d
    		if( store_location && location != null && ( location.getLatitude() != 0.0d || location.getLongitude() != 0.0d ) ) {
	    		if( MyDebug.LOG ) {
	    			Log.d(TAG, "updating parameters from location...");
	    			Log.d(TAG, "lat " + location.getLatitude() + " long " + location.getLongitude());
	    		}
	            Camera.Parameters parameters = camera.getParameters();
	            parameters.removeGpsData();
	            parameters.setGpsLatitude(location.getLatitude());
	            parameters.setGpsLongitude(location.getLongitude());
	            parameters.setGpsProcessingMethod(location.getProvider()); // from http://boundarydevices.com/how-to-write-an-android-camera-app/
	            if( location.hasAltitude() ) {
		            parameters.setGpsAltitude(location.getAltitude());
	            }
	            else {
	            	// Android camera source claims we need to fake one if not present
	            	// and indeed, this is needed to fix crash on Nexus 7
		            parameters.setGpsAltitude(0);
	            }
	            if( location.getTime() != 0 ) { // from Android camera source
	            	parameters.setGpsTimestamp(location.getTime() / 1000);
	            }
	            camera.setParameters(parameters);
	            this.has_set_location = true;
    		}
    		else {
	    		if( MyDebug.LOG )
	    			Log.d(TAG, "removing location data from parameters...");
	            Camera.Parameters parameters = camera.getParameters();
	            parameters.removeGpsData();
	            camera.setParameters(parameters);
	            this.has_set_location = false;
    		}
    	}
    }
	
	public boolean isVideo() {
		return is_video;
	}
	
    // must be static, to safely call from other Activities
    public static String getFlashPreferenceKey(int cameraId) {
    	return "flash_value_" + cameraId;
    }

    // must be static, to safely call from other Activities
    public static String getFocusPreferenceKey(int cameraId) {
    	return "focus_value_" + cameraId;
    }

    // must be static, to safely call from other Activities
    public static String getResolutionPreferenceKey(int cameraId) {
    	return "camera_resolution_" + cameraId;
    }
    
    // must be static, to safely call from other Activities
    public static String getVideoQualityPreferenceKey(int cameraId) {
    	return "video_quality_" + cameraId;
    }
    
    // must be static, to safely call from other Activities
    public static String getIsVideoPreferenceKey() {
    	return "is_video";
    }
    
    // for testing:
    public Camera getCamera() {
		/*if( MyDebug.LOG )
			Log.d(TAG, "getCamera: " + camera);*/
    	return this.camera;
    }
    
    public boolean supportsFocus() {
    	return this.supported_focus_values != null;
    }

    public boolean supportsFlash() {
    	return this.supported_flash_values != null;
    }
    
    public String getCurrentFlashValue() {
    	if( this.current_flash_index == -1 )
    		return null;
    	return this.supported_flash_values.get(current_flash_index);
    }
    
    public boolean hasFocusArea() {
    	return this.has_focus_area;
    }
    
    public boolean isTakingPhotoOrOnTimer() {
    	//return this.is_taking_photo;
    	return this.phase == PHASE_TAKING_PHOTO || this.phase == PHASE_TIMER;
    }
    
    public boolean isTakingPhoto() {
    	return this.phase == PHASE_TAKING_PHOTO;
    }

    public boolean isOnTimer() {
    	//return this.is_taking_photo_on_timer;
    	return this.phase == PHASE_TIMER;
    }

    public boolean isPreviewStarted() {
    	return this.is_preview_started;
    }

}
