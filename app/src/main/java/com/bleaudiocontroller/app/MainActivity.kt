package com.bleaudiocontroller.app

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import android.widget.SeekBar
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.layout_main.album_text_view
import kotlinx.android.synthetic.main.layout_main.artist_text_view
import kotlinx.android.synthetic.main.layout_main.btnConnect
import kotlinx.android.synthetic.main.layout_main.play_pause_button
import kotlinx.android.synthetic.main.layout_main.progress_bar
import kotlinx.android.synthetic.main.layout_main.song_title_text_view
import kotlinx.android.synthetic.main.layout_main.time_left_text_view
import kotlinx.android.synthetic.main.layout_main.total_time
import kotlinx.android.synthetic.main.layout_main.tvStatus
import java.util.UUID
import java.util.concurrent.TimeUnit


class MainActivity : ComponentActivity() {
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private val slaveDeviceAddress = "DA:4C:10:DE:17:00"
    private val serviceUuid = UUID.fromString(uuid) // Example UUID
    private var bluetoothGatt: BluetoothGatt? = null
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var bluetoothGattServer: BluetoothGattServer
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var handler: Handler
    private val songs = listOf(
        R.raw.raw1, // Replace with your audio file resource IDs
        R.raw.raw2,
        R.raw.raw2
    )
    private var currentSongIndex = 0
    private var serviceUUID: UUID? = null
    private var characteristicUuid: UUID? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_main)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestMultiplePermissions.launch(arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT))
        }
        else{
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            requestBluetooth.launch(enableBtIntent)
        }
        checkSelfPermission()
        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

        bluetoothGattServer = bluetoothManager.openGattServer(this, gattServerCallback)
        init()
    }
    
    private fun init() {
        btnConnect.setOnClickListener {
            onConnectClicked()
        }
        
        if (!hasLocationPermission()) {
            requestLocationPermission()
        }
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        mediaPlayer = MediaPlayer.create(this, songs[currentSongIndex]) // Replace "your_song" with your audio file name

        checkSelfPermission()

        val service = BluetoothGattService(getUUID(), BluetoothGattService.SERVICE_TYPE_PRIMARY)
        val characteristic = BluetoothGattCharacteristic(
            getCharactersticUUID(),
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        service.addCharacteristic(characteristic)
        bluetoothGattServer.addService(service)
        val mediaMetadataRetriever = MediaMetadataRetriever()
        mediaMetadataRetriever.setDataSource(this, Uri.parse(uriParsePath + songs[currentSongIndex])) // Replace "your_song" with your audio file name

        val songTitle = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
        val artistName = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
        val albumName = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)

        resetTime()

        song_title_text_view.text = songTitle
        artist_text_view.text = artistName
        album_text_view.text = albumName

        Log.e("songTitle", "" + songTitle)
        Log.e("artistName", "" + artistName)
        Log.e("albumName", "" + albumName)
        progress_bar.max = mediaPlayer.duration
         handler = Handler(Looper.getMainLooper())
         runOnUiThread(object : Runnable {
            override fun run() {
                progress_bar?.progress = mediaPlayer.currentPosition
                handler.postDelayed(this, 1000) //Set the update time
            }
        })

        handler.postDelayed(updateProgress, 1000)
        
        progress_bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer.seekTo(progress)
                    updateTimeLeft()
                    resetTime()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // No implementation needed
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // No implementation needed
            }
        })
    }

    private fun onConnectClicked() {
        if (bluetoothAdapter.isEnabled) {
            checkSelfPermission()
            val device = bluetoothAdapter.getRemoteDevice(slaveDeviceAddress) // Replace with actual MAC address
            connectToDevice(device)
        } else {
            Log.e(TAG, "Bluetooth is not enabled")
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            PERMISSION_REQUEST_FINE_LOCATION
        )
    }
    
    private fun connectToDevice(device: BluetoothDevice) {
        checkSelfPermission()
        tvStatus.text = "Status: Connecting..."
        bluetoothGatt = device.connectGatt(this, false, gattCallback)
       tvStatus.text= bluetoothManager.getConnectionState(device, BluetoothProfile.GATT).toString();
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                runOnUiThread {
                    tvStatus.text = "Status: Connected"
                }
                checkSelfPermission()
                gatt?.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                runOnUiThread {
                    tvStatus.text = "Status: Disconnected"
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            val service = gatt?.getService(serviceUuid)
            val characteristic = service?.getCharacteristic(serviceUuid)
            checkSelfPermission()
            gatt?.setCharacteristicNotification(characteristic, true)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            val dataReceived = characteristic?.value
            runOnUiThread {
                val receivedText = dataReceived?.toString(Charsets.UTF_8)
                tvStatus.text = receivedText
            }
        }
    }

    private val gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onCharacteristicReadRequest(
            device: BluetoothDevice,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic
        ) {
            // Handle read requests if necessary
            checkSelfPermission()
            bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null)
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray
        ) {
            // Handle write requests
            val command = value.toString(Charsets.UTF_8)
            if (command == "play") {
                mediaPlayer.start()
            } else if (command == "pause") {
                mediaPlayer.pause()
            }

            if (responseNeeded) {
                checkSelfPermission()
                bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
            }
        }
    }

    private val updateProgress: Runnable = object : Runnable {
        override fun run() {
            progress_bar.progress = mediaPlayer.currentPosition
            updateTimeLeft()
            handler.postDelayed(this, 1000)
        }
    }

    private fun updateTimeLeft() {

        val timeLeftInMillis = mediaPlayer.duration - mediaPlayer.currentPosition
        val timeLeft = String.format(
            "%02d:%02d",
            TimeUnit.MILLISECONDS.toMinutes(timeLeftInMillis.toLong()),
            TimeUnit.MILLISECONDS.toSeconds(timeLeftInMillis.toLong()) % 60
        )
        time_left_text_view.text = timeLeft
    }

    private fun resetTime()
    {
        val duration = mediaPlayer.duration
        val time = String.format(
            "%02d:%02d",
            TimeUnit.MILLISECONDS.toMinutes(duration.toLong()),
            TimeUnit.MILLISECONDS.toSeconds(duration.toLong()) -
                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration.toLong()))
        )
        total_time.text = time;
    }

    fun togglePlayPause() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
            play_pause_button.text = "Play"
        } else {
            mediaPlayer.start()
            play_pause_button.text = "Pause"
        }
    }

    fun playPrevious() {
        if (currentSongIndex > 0) {
            currentSongIndex--
            mediaPlayer.reset()
            mediaPlayer.setDataSource(this, Uri.parse(uriParsePath + songs[currentSongIndex]))
            mediaPlayer.prepare()
            mediaPlayer.start()
            updateSongInfo()
            resetTime()
        }
    }

    fun playNext() {
        if (currentSongIndex < songs.size - 1) {
            currentSongIndex++
            mediaPlayer.reset()
            mediaPlayer.setDataSource(this, Uri.parse(uriParsePath + songs[currentSongIndex]))
            mediaPlayer.prepare()
            mediaPlayer.start()
            updateSongInfo()
            resetTime()
        }
    }

    private fun updateSongInfo() {
        val mediaMetadataRetriever = MediaMetadataRetriever()
        mediaMetadataRetriever.setDataSource(this, Uri.parse(uriParsePath + songs[currentSongIndex]))
        val songTitle = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
        val artistName = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
        val albumName = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)

        song_title_text_view.text = songTitle
        artist_text_view.text = artistName
        album_text_view.text = albumName
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        handler.removeCallbacks(updateProgress)
        mediaPlayer.release() //  Release the MediaPlayer when the activity is destroyed
        super.onDestroy()
        checkSelfPermission()
        bluetoothGatt?.close()

    }

    private fun getCharactersticUUID(): UUID {
        if (characteristicUuid != null) {
            return characteristicUuid as UUID
        }
        return try {
            val uuid = UUID.fromString(uuid) // Example UUID
            val parcelUuid = ParcelUuid(uuid)
            characteristicUuid = parcelUuid.uuid
            parcelUuid.uuid
        } catch (ex: IllegalArgumentException) {
            Log.e("UUID Error", "Invalid UUID format")
            UUID.randomUUID()
        }
    }

    private fun getUUID(): UUID {
        if (serviceUUID != null) {
            return serviceUUID as UUID
        }
        return try {
            val uuid = UUID.fromString(uuid) // Example UUID
            val parcelUuid = ParcelUuid(uuid)
            serviceUUID = parcelUuid.uuid
            parcelUuid.uuid
        } catch (ex: IllegalArgumentException) {
            Log.e("UUID Error", "Invalid UUID format")
            UUID.randomUUID()
        }
    }

    private var requestBluetooth = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            init()
        }else{
            //deny
        }
    }

    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                Log.d("test006", "${it.key} = ${it.value}")
            }
        }

    private fun checkSelfPermission() {
        if (ActivityCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
    }

    companion object {
        private const val PERMISSION_REQUEST_FINE_LOCATION = 123
        private const val uuid = "31990d6c-893d-4876-875e-adb9cfa59e48"
        private const val uriParsePath = "android.resource://com.bleaudiocontroller.app/"
    }
}
