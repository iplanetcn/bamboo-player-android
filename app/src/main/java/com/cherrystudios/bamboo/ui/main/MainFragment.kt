package com.cherrystudios.bamboo.ui.main

import android.Manifest
import android.R.attr.fragment
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.provider.Settings
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.cherrystudios.bamboo.R
import com.cherrystudios.bamboo.adapter.MusicItemAdapter
import com.cherrystudios.bamboo.base.BaseFragment
import com.cherrystudios.bamboo.constant.ACTION_MUSIC_PROGRESS
import com.cherrystudios.bamboo.constant.EXTRA_DURATION
import com.cherrystudios.bamboo.constant.EXTRA_POSITION
import com.cherrystudios.bamboo.databinding.FragmentMainBinding
import com.cherrystudios.bamboo.extension.registerReceiverCompat
import com.cherrystudios.bamboo.service.MusicPlayService
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.jvm.java

class MainFragment : BaseFragment() {
    companion object {
        fun newInstance(): Fragment {
            return MainFragment().apply { arguments = Bundle() }
        }
    }

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: MainViewModel
    private lateinit var adapter: MusicItemAdapter

    private var musicService: MusicPlayService? = null
    private var isBound = false
    private val progressReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val pos = intent?.getLongExtra(EXTRA_POSITION, 0L) ?: 0L
            val dur = intent?.getLongExtra(EXTRA_DURATION, 0L) ?: 0L
            Timber.d("progress: $pos / $dur")
            binding.playControl.progressHorizontal.progress = ((pos * 100) / dur).toInt()
        }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicPlayService.MusicBinder
            musicService = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
            musicService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = MusicItemAdapter()
        adapter.setOnItemClickListener { position ->
            val audioFile = adapter.data[position]
            val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, audioFile.id)
            musicService?.play(uri)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.adapter = adapter

        binding.playControl.btnPlayPause.setOnClickListener {
            musicService?.playPause()
        }

        binding.playControl.btnPlayNext.setOnClickListener {
            // TODO
        }

        binding.playControl.btnPlayPrevious.setOnClickListener {
            // TODO
        }

        checkPermissions()

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.audioFiles.collect { audioFiles ->
                        updateAudioFiles(audioFiles)
                    }
                }
                launch {
                    viewModel.uiState.collect { state ->
                        updateUI(state)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onStart() {
        super.onStart()
        Intent(context, MusicPlayService::class.java).also { intent ->
            activity?.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        if (isBound) {
            activity?.unbindService(connection)
            isBound = false
        }
    }

    override fun onResume() {
        super.onResume()
        activity?.run { registerReceiverCompat(progressReceiver, IntentFilter(ACTION_MUSIC_PROGRESS)) }
    }

    override fun onPause() {
        super.onPause()
        activity?.run {
            unregisterReceiver(progressReceiver)
        }
    }

    fun updateAudioFiles(audioFiles: List<AudioFile>) {
        if (audioFiles.isNotEmpty()) {
            adapter.data += audioFiles.filterNot { it in adapter.data }
        }
    }

    fun updateUI(uiState: UiState) {
        binding.progressCircular.visibility = if (uiState.isLoading) View.VISIBLE else View.GONE
    }


    /**
     * 检查权限
     */
    private fun checkPermissions() {
        val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val permissionsNotGranted = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsNotGranted.isEmpty()) {
            viewModel.queryMediaAudio(requireActivity().application)
        } else {
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                if (permissions.entries.all { it.value }) {
                    // All permissions granted.
                    viewModel.queryMediaAudio(requireActivity().application)
                } else {
                    // Permissions denied.
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(getString(R.string.permission_dialog_title))
                        .setMessage(getString(R.string.permission_dialog_message))
                        .setPositiveButton(getString(R.string.permission_dialog_positive_button)) { _, _ ->
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            val uri = Uri.fromParts("package", requireContext().packageName, null)
                            intent.data = uri
                            startActivity(intent)
                        }
                        .setNegativeButton(getString(R.string.permission_dialog_negative_button)) { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                }
            }.launch(permissionsNotGranted.toTypedArray())
        }
    }
}