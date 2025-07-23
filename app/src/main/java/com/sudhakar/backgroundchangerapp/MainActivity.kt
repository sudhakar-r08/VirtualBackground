package com.sudhakar.backgroundchangerapp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import org.opencv.android.CameraActivity
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils.bitmapToMat
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfFloat
import org.opencv.core.MatOfInt
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc.CHAIN_APPROX_SIMPLE
import org.opencv.imgproc.Imgproc.COLOR_BGR2GRAY
import org.opencv.imgproc.Imgproc.COLOR_BGR2HSV
import org.opencv.imgproc.Imgproc.COLOR_BayerBG2RGB
import org.opencv.imgproc.Imgproc.COLOR_RGBA2RGB
import org.opencv.imgproc.Imgproc.Canny
import org.opencv.imgproc.Imgproc.INTER_AREA
import org.opencv.imgproc.Imgproc.RETR_LIST
import org.opencv.imgproc.Imgproc.THRESH_BINARY
import org.opencv.imgproc.Imgproc.blur
import org.opencv.imgproc.Imgproc.calcHist
import org.opencv.imgproc.Imgproc.contourArea
import org.opencv.imgproc.Imgproc.cvtColor
import org.opencv.imgproc.Imgproc.dilate
import org.opencv.imgproc.Imgproc.erode
import org.opencv.imgproc.Imgproc.findContours
import org.opencv.imgproc.Imgproc.resize
import org.opencv.imgproc.Imgproc.threshold
import org.opencv.video.BackgroundSubtractorKNN
import org.opencv.video.BackgroundSubtractorMOG2


class MainActivity : CameraActivity(), CvCameraViewListener2 {


    private var bgs: Boolean = false
    private lateinit var mRgba: Mat
    private lateinit var sub: BackgroundSubtractorMOG2
    private lateinit var sub1: BackgroundSubtractorKNN
    private lateinit var mGray: Mat
    private lateinit var mRgb: Mat
    private lateinit var mFGMask: Mat

    private val TAG = "MainActivity"
    private lateinit var mOpenCvCameraView: OpenCVCamera


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //! [ocv_loader_init]
        if (OpenCVLoader.initLocal()) {
            Log.i(TAG, "OpenCV loaded successfully");
        } else {
            Log.e(TAG, "OpenCV initialization failed!");
            (Toast.makeText(this, "OpenCV initialization failed!", Toast.LENGTH_LONG)).show();
            return;
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val decorView = window.decorView
        // Hide the status bar.
        val uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN
        decorView.systemUiVisibility = uiOptions
        mOpenCvCameraView = findViewById<View>(R.id.fd_activity_surface_view) as OpenCVCamera
        mOpenCvCameraView.visibility = CameraBridgeViewBase.VISIBLE
        mOpenCvCameraView.setCvCameraViewListener(this)

    }

    override fun onPause() {
        super.onPause()
        mOpenCvCameraView.disableView()
    }

    override fun onResume() {
        super.onResume()
        mOpenCvCameraView.enableView()

    }

    override fun getCameraViewList(): List<CameraBridgeViewBase?>? {
        return listOf(mOpenCvCameraView)
    }

    override fun onDestroy() {
        super.onDestroy()
        mOpenCvCameraView.disableView()
    }

    override fun onCameraViewStarted(width: Int, height: Int) {

    }

    override fun onCameraViewStopped() {

    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame?): Mat {
        //  return if (bgs) bgSubtractionMOG2(inputFrame) else backgroundRemoval(inputFrame)
        val src = inputFrame!!.rgba()
        Core.flip(src, src, 1)

        // return doCanny(src)
        return contours(src)
    }

    private fun backgroundRemoval(inputFrame: CameraBridgeViewBase.CvCameraViewFrame?): Mat {
        val src = inputFrame!!.rgba()
        Core.flip(src, src, 1)
        val dst = getBg(src)
        val rmBg = doBackgroundRemoval(src)
        rmBg.copyTo(dst, rmBg)
        return dst
    }

    private fun bgSubtractionMOG2(inputFrame: CameraBridgeViewBase.CvCameraViewFrame?): Mat {
        mRgba = inputFrame!!.rgba()
        Core.flip(mRgba, mRgba, 1)
        cvtColor(
            mRgba,
            mRgb,
            COLOR_RGBA2RGB
        ) //the apply function will throw the above error if you don't feed it an RGB image
        sub.apply(mRgb, mFGMask, 0.5) //apply() exports a gray image by definition
        //sub1.apply(mRgba, mFGMask, 0.9); //apply() exports a gray image by definition

        val dst = getBg(mRgba)
        mRgba.copyTo(dst, mFGMask)
        return dst
    }

    private fun getBg(src: Mat): Mat {
        val bg = Mat()
        val matrix = Matrix()
        matrix.postRotate(270F)
        val mBitmap = BitmapFactory.decodeStream(assets.open("drawable/bg.jpg"))
        val rotatedBitmap = Bitmap.createBitmap(
            mBitmap,
            0,
            0,
            mBitmap.width,
            mBitmap.height,
            matrix,
            true
        )
        val resizedBitmap = Bitmap.createScaledBitmap(
            rotatedBitmap, src.size().width.toInt(), src.size().height.toInt(), false
        )
        val bmp32: Bitmap = resizedBitmap.copy(Bitmap.Config.ARGB_8888, true)
        bitmapToMat(bmp32, bg)
        return bg
    }


    fun resizeImages(src: Mat, dst: Mat): Mat {
        val width = src.size().width
        val height = src.size().height
        val size = Size(width, height)
        val interpolation = INTER_AREA
        resize(src, dst, size, 0.0, 0.0, interpolation)
        return dst
    }


    private fun doBackgroundRemoval(frame: Mat): Mat {
        // init
        val hsvImg = Mat()
        val hsvPlanes: ArrayList<Mat> = ArrayList()
        val thresholdImg = Mat()

        // threshold the image with the histogram average value
        hsvImg.create(frame.size(), CvType.CV_8U)
        cvtColor(frame, hsvImg, COLOR_BGR2HSV)
        Core.split(hsvImg, hsvPlanes)
        val threshValue: Double = getHistAverage(hsvImg, hsvPlanes[0])
        //val threshValue: Double = 100.0
        Log.d("threshValue", "" + threshValue)

        threshold(
            hsvPlanes[0],
            thresholdImg,
            threshValue,
            threshValue * 3,
            THRESH_BINARY
        )
        blur(thresholdImg, thresholdImg, Size(9.0, 9.0))

        // dilate to fill gaps, erode to smooth edges
        dilate(thresholdImg, thresholdImg, Mat(), Point((-1).toDouble(), 1.0), 2)
        erode(thresholdImg, thresholdImg, Mat(), Point((-1).toDouble(), 1.0), 1)
        threshold(thresholdImg, thresholdImg, threshValue, 255.0, THRESH_BINARY)

        // create the new image
        val foreground = Mat(frame.size(), CvType.CV_8UC3, Scalar(255.0, 255.0, 255.0))
        frame.copyTo(foreground, thresholdImg)
        return foreground
    }

    private fun getHistAverage(hsvImg: Mat, hueValues: Mat): Double {
        // init
        var average = 0.0
        val hist_hue = Mat()
        val histSize = MatOfInt(180)
        val hue: MutableList<Mat> = ArrayList()
        hue.add(hueValues)

        // compute the histogram
        calcHist(hue, MatOfInt(0), Mat(), hist_hue, histSize, MatOfFloat(0F, 179F))

        // get the average for each bin
        for (h in 0..179) {
            average += hist_hue[h, 0][0] * h
        }
        return average / hsvImg.size().height / hsvImg.size().width.also { average = it }
    }

    private fun doCanny(frame: Mat): Mat {
        // init
        val grayImage = Mat()
        val detectedEdges = Mat()

        // convert to grayscale
        cvtColor(frame, grayImage, COLOR_BGR2GRAY)

        // reduce noise with a 3x3 kernel
        blur(grayImage, detectedEdges, Size(2.0, 2.0))


        // canny detector, with ratio of lower:upper threshold of 3:1
        /*  Imgproc.Canny(
              detectedEdges,
              detectedEdges,
              100.0,
              200.0,
              3,
              true
          )*/
        sub.apply(frame, mFGMask, 0.5)
        val hsvImg = Mat()
        val hsvPlanes: ArrayList<Mat> = ArrayList()
        val thresholdImg = Mat()

        // threshold the image with the histogram average value
        hsvImg.create(frame.size(), CvType.CV_8U)
        cvtColor(frame, hsvImg, COLOR_BGR2HSV)
        Core.split(hsvImg, hsvPlanes)
        val threshValue: Double = getHistAverage(hsvImg, hsvPlanes[0])
        //  Imgproc.dilate(mFGMask, mFGMask, Mat(), Point((-1).toDouble(), 1.0), 2)
        //  Imgproc.erode(detectedEdges, detectedEdges, Mat(), Point((-1).toDouble(), 1.0), 6)
        threshold(mFGMask, mFGMask, threshValue, threshValue * 3, THRESH_BINARY)
        val foreground = Mat(frame.size(), CvType.CV_8UC3, Scalar(255.0, 255.0, 255.0))
        frame.copyTo(foreground, mFGMask)

        // using Canny's output as a mask, display the result
        val dest = Mat()
        Core.add(dest, Scalar.all(255.0), dest)
        frame.copyTo(dest, foreground)
        val bg = getBg(frame)
        frame.copyTo(bg, dest)

        return bg
    }

    fun toggle(view: View) {
        var label = "Method One"
        bgs = !bgs
        (view as Button).text = if (bgs) "Method One" else "Method Two"

    }

    fun contours(mat: Mat): Mat {

        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        val balele = Mat()
        //convert the image to black and white
        cvtColor(mat, mat, COLOR_BGR2GRAY)

        //convert the image to black and white does (8 bit)
        Canny(mat, mat, 90.0, 270.0)
        findContours(
            mat,
            contours,
            hierarchy,
            RETR_LIST,
            CHAIN_APPROX_SIMPLE
        )

        var maxArea = 1.0
        var maxAreaIdx = -1
        var temp_contour = contours[0] //the largest is at the index 0 for starting point

        val approxCurve = MatOfPoint2f()
        var largest_contour: Mat = contours[0]
        var largest_contours: MutableList<MatOfPoint?> = ArrayList()
        for (idx in contours.indices) {
            temp_contour = contours[idx]
            val contourarea = contourArea(temp_contour)
            //compare this contour to the previous largest contour found
            if (contourarea > maxArea) {
                //check if this contour is a square
                maxArea = contourarea
                maxAreaIdx = idx
                largest_contours.add(temp_contour)
                largest_contour = temp_contour

            }
        }
        val temp_largest = largest_contours[largest_contours.size - 1]
        largest_contours = ArrayList()
        largest_contours.add(temp_largest)
        cvtColor(mat, mat, COLOR_BayerBG2RGB)
        //Imgproc.drawContours(mat, largest_contours, -1, Scalar(255.0, 255.0, 255.0), 100);
        return mat
    }


}