package com.diegodinho.individualdoublecamera

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.media.ImageReader.OnImageAvailableListener
import android.os.*
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.Surface
import android.view.TextureView
import android.view.TextureView.SurfaceTextureListener
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

import java.io.*
import java.util.*



class MainActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var textureView: TextureView
    private lateinit var textureView2: TextureView

    companion object {


        //Check state orientation of output image
        private val ORIENTATIONS = SparseIntArray()
        private const val REQUEST_CAMERA_PERMISSION = 200


        init {
            ORIENTATIONS.append(Surface.ROTATION_0, 90)
            ORIENTATIONS.append(Surface.ROTATION_90, 0)
            ORIENTATIONS.append(Surface.ROTATION_180, 270)
            ORIENTATIONS.append(Surface.ROTATION_270, 180)
        }
    }

    //Entrada de React Native
    private var counter: Int? = null

    //Buton video
    private lateinit var button: Button
    var accessVideo: Boolean = true

    //Button inicio
    private lateinit var buttonInicio: Button

    private var serie: Int = 0
    //serie 2
    //Button
    private lateinit var buttonSerie2: Button
    //variable
    private var Serie2: Boolean = false
    private var SwitchSerie2: Boolean = false

    private var anguloVideo: Int = 30
    //serie 3
    //Button
    private lateinit var buttonSerie3: Button
    //variable
    private var Serie3: Boolean = true

    //Icono
    private lateinit var clockView: View
    private lateinit var clock2View: View

    private lateinit var serie1View: View
    private lateinit var serie2View: View
    private lateinit var serie3View: View

    //Timer
    private val timer = object: CountDownTimer(40000, 40000){
        override fun onTick(p0: Long) { }

        override fun onFinish() {
            clockView.visibility = View.INVISIBLE
            clock2View.visibility = View.INVISIBLE
            accessVideo = true
        }

    }

    //Timer serie 1
    private val timer1 = object: CountDownTimer(1000, 1000){
        override fun onTick(p0: Long) { }

        override fun onFinish() {
            serie1View.visibility = View.INVISIBLE
        }

    }

    //Timer serie 2
    private val timer2 = object: CountDownTimer(1000, 1000){
        override fun onTick(p0: Long) { }

        override fun onFinish() {
            serie2View.visibility = View.INVISIBLE
            SwitchSerie2 = true
        }

    }

    //Timer serie 3
    private val timer3 = object: CountDownTimer(1000, 1000){
        override fun onTick(p0: Long) { }

        override fun onFinish() {
            serie3View.visibility = View.INVISIBLE
        }

    }

    private lateinit var cameraId: String
    private var cameraDevice: CameraDevice? = null
    private var cameraCaptureSessions: CameraCaptureSession? = null
    private var captureRequestBuilder: CaptureRequest.Builder? = null
    private var imageDimension: Size? = null
    private var imageReader: ImageReader? = null


    //Position and Dynamics
    private var sensorManager: SensorManager? = null
    var accelerometer: Sensor? = null
    var giroscope: Sensor? = null
    var magneticField: Sensor? = null
    var derivada: Double = -10.0
    var diferencia: Double = 0.0
    var control: Boolean? = null
    var control2: Boolean = false
    var rad2degrees: Double = 1.0

    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    //Save to FILE
    private var file: File? = null
    private val mFlashSupported = false
    private var mBackgroundHandler: Handler? = null
    private var mBackgroundThread: HandlerThread? = null
    var texture: SurfaceTexture? = null
    var texture2: SurfaceTexture? = null


    var SCB: CameraDevice.StateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            setPreview()
        }

        override fun onDisconnected(camera: CameraDevice) {
            cameraDevice = camera
            returnHome()
        }

        override fun onError(camera: CameraDevice, i: Int) {
            cameraDevice = camera
            returnHome()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        if (serie == 2) {
            Serie2 = true
            Serie3 = false
        } else if (serie == 3) {
            Serie2 = false
            Serie3 = true
        } else {
            Serie2 = false
            Serie3 = false
        }

        serie1View = findViewById(R.id.serie_1)
        serie2View = findViewById(R.id.serie_2)
        serie3View = findViewById(R.id.serie_3)

        clockView = findViewById(R.id.clock)
        clock2View = findViewById(R.id.clock_2)

        //Boton para cambiar el modo camera o modo con video
        button = findViewById(R.id.button_video)
        button.setOnClickListener{
            accessVideo = !accessVideo
            serie1View.visibility = View.VISIBLE
            timer1.start()
        }




        //Boton para iniciar la actividad
        buttonInicio = findViewById(R.id.button_time)
        buttonInicio.setOnClickListener{
            clockView.visibility = View.VISIBLE
            clock2View.visibility = View.VISIBLE
            accessVideo = false
            timer.start()
        }

        //Boton para iniciar segunda serie
        buttonSerie2 = findViewById(R.id.button_serie_2)
        buttonSerie2.setOnClickListener {
            Serie2 = true
            Serie3 = false
            serie2View.visibility = View.VISIBLE
            counter = 0
            timer2.start()
        }

        //Boton para iniciar tercera serie
        buttonSerie3 = findViewById(R.id.button_serie_3)
        buttonSerie3.setOnClickListener {
            Serie2 = false
            Serie3 = true
            serie3View.visibility = View.VISIBLE
            timer3.start()
        }

        //Accelerometer
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        giroscope = sensorManager!!.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        magneticField = sensorManager!!.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        sensorManager!!.registerListener(
            this@MainActivity,
            accelerometer,
            SensorManager.SENSOR_DELAY_NORMAL
        )
        sensorManager!!.registerListener(
            this@MainActivity,
            giroscope,
            SensorManager.SENSOR_DELAY_NORMAL
        )
        sensorManager!!.registerListener(
            this@MainActivity,
            magneticField,
            SensorManager.SENSOR_DELAY_NORMAL
        )

        //Camera
        textureView = findViewById(R.id.texture_view1)
        textureView2 = findViewById(R.id.texture_view2)

        assert(textureView != null)
        textureView.setSurfaceTextureListener(textureListener)
        textureView2.setSurfaceTextureListener(textureListener2)
    }

    private fun Double(number: Int): String {
        return if (number < 10) "0$number" else "" + number
    }

    private fun saveImage() {
        if (cameraDevice == null) return
        var manager: CameraManager? = null
        manager = getSystemService(CAMERA_SERVICE) as CameraManager
        try {
            val characteristics = manager.getCameraCharacteristics(
                cameraDevice!!.id
            )
            var jpegSizes: Array<Size?>? = null
            if (characteristics != null) jpegSizes =
                characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    ?.getOutputSizes(ImageFormat.JPEG)

            //Capture image with custom size
            val width = 1152 //1600
            val height = 864 //1200
            imageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1)
            val outputSurface: MutableList<Surface> = ArrayList(2)
            outputSurface.add(imageReader!!.surface)
            outputSurface.add(Surface(textureView!!.surfaceTexture))
            val captureBuilder =
                cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureBuilder.addTarget(imageReader!!.surface)
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)

            //Check orientation base on device
            val rotation = windowManager.defaultDisplay.rotation
            captureBuilder.set(
                CaptureRequest.JPEG_ORIENTATION,
                ORIENTATIONS[rotation]
            )
            val calendar = Calendar.getInstance(TimeZone.getDefault())
            file = File(
                Environment.getExternalStorageDirectory()
                    .toString() + "/DCIM/Camera/IMG_" + calendar[Calendar.YEAR] + Double(
                    calendar[Calendar.MONTH] + 1
                ) + Double(calendar[Calendar.DATE]) + "_" + Double(
                    calendar[Calendar.HOUR_OF_DAY]
                ) + Double(calendar[Calendar.MINUTE]) + Double(calendar[Calendar.SECOND]) + ".jpg"
            )
            var readerListener: OnImageAvailableListener? = null
            readerListener = object : OnImageAvailableListener {
                override fun onImageAvailable(imageReadr: ImageReader) {
                    var image: Image? = null
                    try {
                        image = imageReader!!.acquireLatestImage()
                        val buffer = image.planes[0].buffer
                        val bytes = ByteArray(buffer.capacity())
                        buffer[bytes]
                        save(bytes)
                    } catch (e: FileNotFoundException) {
                        e.printStackTrace()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    } finally {
                        run { image?.close() }
                    }
                }

                @Throws(IOException::class)
                private fun save(bytes: ByteArray) {
                    var outputStream: OutputStream? = null
                    try {
                        outputStream = FileOutputStream(file)
                        outputStream.write(bytes)
                    } finally {
                        outputStream?.close()
                    }
                }
            }
            imageReader!!.setOnImageAvailableListener(readerListener, mBackgroundHandler)
            val captureListener: CameraCaptureSession.CaptureCallback =
                object : CameraCaptureSession.CaptureCallback() {
                    override fun onCaptureCompleted(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        result: TotalCaptureResult
                    ) {
                        super.onCaptureCompleted(session, request, result)
                        Toast.makeText(this@MainActivity, "Saved $file", Toast.LENGTH_SHORT).show()
                        returnHome()
                    }
                }
            cameraDevice!!.createCaptureSession(
                outputSurface,
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                        try {
                            cameraCaptureSession.capture(
                                captureBuilder.build(),
                                captureListener,
                                mBackgroundHandler
                            )

                        } catch (e: CameraAccessException) {
                            e.printStackTrace()
                        }
                    }

                    override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {}
                },
                mBackgroundHandler
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun returnHome() {
        texture!!.release()
        texture2!!.release()
        //        if(imageReader != null) {
//            imageReader.close();
//            imageReader = null;
//        }
        if (cameraDevice != null) {
            cameraDevice!!.close()
            cameraDevice = null
        }
        if (cameraCaptureSessions != null) {
            cameraCaptureSessions!!.close()
            cameraCaptureSessions = null
        }
    }

    private fun setPreview() {
        try {
            texture = textureView!!.surfaceTexture
            assert(texture != null)
            texture!!.setDefaultBufferSize(1600, 1200) //1600, 1200 de referencia
            val surface = Surface(texture)

            texture2 = textureView2!!.surfaceTexture
            texture2!!.setDefaultBufferSize(1600,1200)
            val surface2 = Surface(texture2)


            captureRequestBuilder =
                cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureRequestBuilder!!.addTarget(surface)
            captureRequestBuilder!!.addTarget(surface2)
            cameraDevice!!.createCaptureSession(
                Arrays.asList(surface, surface2),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                        if (cameraDevice == null) return
                        cameraCaptureSessions = cameraCaptureSession
                        updatePreview()
                    }

                    override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                        Toast.makeText(this@MainActivity, "Changed", Toast.LENGTH_SHORT).show()
                    }
                },
                null
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun updatePreview() {
        if (cameraDevice == null) Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show()
        captureRequestBuilder!!.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
        try {
            cameraCaptureSessions!!.setRepeatingRequest(
                captureRequestBuilder!!.build(),
                null,
                mBackgroundHandler
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun startCamera() {
        val manager = getSystemService(CAMERA_SERVICE) as CameraManager
        try {
            cameraId = manager.cameraIdList[0]
            val characteristics = manager.getCameraCharacteristics(cameraId)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
            imageDimension = map.getOutputSizes(SurfaceTexture::class.java)[0]
            //Check realtime permission if run higher API 23
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ), REQUEST_CAMERA_PERMISSION
                )
                return
            }
            manager.openCamera(cameraId, SCB, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    var textureListener: SurfaceTextureListener = object : SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, i: Int, i1: Int) {
            startCamera()
        }

        override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, i: Int, i1: Int) {}
        override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
            return false
        }

        override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {}
    }

    var textureListener2: SurfaceTextureListener = object : SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, i: Int, i1: Int) {
            Log.i("diegol","buena perro hicimos el listener")
        }

        override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, i: Int, i1: Int) {}
        override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
            return false
        }

        override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {}
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "You can't use camera without permission", Toast.LENGTH_SHORT)
                    .show()
                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        derivada = -10.0 //accelerometer
        startBackgroundThread()
        if (textureView!!.isAvailable) {
            startCamera()
        } else {
            textureView!!.surfaceTextureListener = textureListener
            textureView2!!.surfaceTextureListener = textureListener2
        }
    }

    override fun onPause() {
        stopBackgroundThread()
        // Don't receive any more updates from either sensor.
        sensorManager!!.unregisterListener(this)
        super.onPause()
    }

    private fun stopBackgroundThread() {
        mBackgroundThread!!.quitSafely()
        try {
            mBackgroundThread!!.join()
            mBackgroundThread = null
            mBackgroundHandler = null
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    private fun startBackgroundThread() {
        mBackgroundThread = HandlerThread("Camera Background")
        mBackgroundThread!!.start()
        mBackgroundHandler = Handler(mBackgroundThread!!.looper)
    }

    //Accelerometer



    override fun onSensorChanged(sensorEvent: SensorEvent) {


        when (sensorEvent!!.sensor.type) {
            //Sensor.TYPE_GYROSCOPE -> {
            //    navigate(sensorEvent!!.values[1]).toString()
            //}

            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(sensorEvent.values, 0, accelerometerReading, 0, accelerometerReading.size)
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(sensorEvent.values, 0, magnetometerReading, 0, magnetometerReading.size)
                updateOrientationAngles()

            }
        }

    }



    fun add(arr: IntArray, index: Int, element: Int): IntArray {
        if (index < 0 || index >= arr.size) {
            return arr
        }

        val result = arr.toMutableList()
        result.add(index, element)
        return result.toIntArray()
    }



    fun updateOrientationAngles() {
        // Update rotation matrix, which is needed to update orientation angles.
        SensorManager.getRotationMatrix(
            rotationMatrix,
            null,
            accelerometerReading,
            magnetometerReading
        )

        // "rotationMatrix" now has up-to-date information.

        SensorManager.getOrientation(rotationMatrix, orientationAngles)

        // "orientationAngles" now has up-to-date information.
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {}

}