package com.example.b2.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.b2.R
import com.example.b2.adapters.FileAdapter
import com.example.b2.databinding.ActivityMainBinding
import com.example.b2.models.FileData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : AppCompatActivity() {

    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private lateinit var adapter: FileAdapter
    private val items = mutableListOf<FileData>()

    private var job: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.cl_main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        adapter = FileAdapter(items)

        with(binding) {
            rvFiles.adapter = adapter
            rvFiles.layoutManager = LinearLayoutManager(this@MainActivity)

            btPause.setOnClickListener {
                btPause.isEnabled = false
                btScan.isEnabled = true
                btScan.setBackgroundColor(getColor(R.color.green))
                btPause.setBackgroundColor(getColor(R.color.grey))
                stopScan()
            }

            btScan.setOnClickListener {
                btPause.isEnabled = true
                btScan.isEnabled = false
                btScan.setBackgroundColor(getColor(R.color.grey))
                btPause.setBackgroundColor(getColor(R.color.red))
                startScan()
            }
        }

        startScan()
    }

    @SuppressLint("SetTextI18n")
    private fun startScan() {
        job = CoroutineScope(Dispatchers.IO).launch {
            getStorageInfo()
            val rootDir = File(Environment.getExternalStorageDirectory().absolutePath)
            scanFiles(rootDir)
        }
    }

    private fun stopScan() {
        job?.cancel()
    }

    private suspend fun scanFiles(directory: File) {
        directory.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                scanFiles(file)
            } else {
                val fileData = FileData(
                    name = file.name,
                    path = file.path,
                    type = file.extension,
                    size = file.length().toString()
                )
                withContext(Dispatchers.Main) {
                    items.add(0, fileData)
                    adapter.notifyItemInserted(0)
                    binding.tvCount.text = "File scanned: ${items.size}"
                }
            }
        }
    }

    private suspend fun getStorageInfo() {
        val freeSpace = Environment.getExternalStorageDirectory().freeSpace
        val totalSpace = Environment.getExternalStorageDirectory().totalSpace
        withContext(Dispatchers.Main) {
            binding.tvFree.text = "Free: ${formatSize(freeSpace)}"
            binding.tvMemory.text = "Memory: ${formatSize(totalSpace)}"
        }
    }

    private fun formatSize(size: Long): String {
        val mb = size / 1024 / 1024
        val gb = size / 1024 / 1024 / 1024
        return "${gb}GB"
    }
}