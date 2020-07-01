package sg.gov.dsta.DroneControl;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;

public class DrawingView extends View {

    protected static final String TAG = "DrawingView";
    private final int minPixelHeight = 50;


    private Paint mPaint;
    private Path mPath;

    private Rect mRect = new Rect();
    public static Rect croppedRect = null;
    float downx, downy;

    private Context mContext;

    public static Double objectHeight;





    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setFilterBitmap(true);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(Color.BLUE);
        mPaint.setDither(true);
        mPaint.setStrokeWidth(50);
        mPaint.setAlpha(50);
        mPath = new Path();


    }

    @Override
    protected void onDraw(Canvas canvas) {

        canvas.drawRect(mRect, mPaint);

        super.onDraw(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        float upx, upy;


        switch (event.getAction()){

            case MotionEvent.ACTION_DOWN:

                mPaint.setAlpha(50);




                mPath.moveTo(event.getX(), event.getY());

                downx = event.getX();
                downy = event.getY();

                break;

            case MotionEvent.ACTION_MOVE:
                mPath.lineTo(event.getX(), event.getY());

                upx = event.getX();
                upy = event.getY();
                mRect.set(Math.round(downx), Math.round(downy), Math.round(upx), Math.round(upy));


                invalidate();

                break;

            case MotionEvent.ACTION_UP:


                //get pixel height of object
                int height = mRect.bottom - mRect.top;
                int absHeight = Math.abs(height);

                if (absHeight > minPixelHeight) {
                    showToast("Height of pixels is: "+ absHeight);

                    croppedRect = new Rect(mRect);

                    takePhoto();

                    mPaint.setAlpha(0);

                    objectHeight = (double) absHeight;

                } else  {
                    showToast("Selection Area is too small");
                }



                clearCanvas();

                break;
        }

        return true;
    }




    public void clearCanvas() {

        mRect.set(0,0,0,0);

    }

    private void takePhoto(){


        BaseProduct product = DJIDemoApplication.getProductInstance();
        if (product != null && product.isConnected()) {

            final Camera camera = product.getCamera();
            if (camera != null) {

                SettingsDefinitions.ShootPhotoMode photoMode = SettingsDefinitions.ShootPhotoMode.SINGLE; // Set the camera capture mode as Single mode

                camera.setShootPhotoMode(photoMode, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (null == djiError) {

                            camera.startShootPhoto(new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onResult(DJIError djiError) {
                                    if (djiError == null) {
                                        showToast("take photo: success");
                                    } else {
                                        showToast(djiError.getDescription());
                                    }
                                }
                            });


                        }
                    }
                });

            }


        }

    }

    private void showToast(String string){
//        Toast.makeText(mContext, string, Toast.LENGTH_SHORT).show();
    }









}