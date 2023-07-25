package com.maxrave.simpmusic.ui.fragment.other

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.adapter.artist.SeeArtistOfNowPlayingAdapter
import com.maxrave.simpmusic.adapter.search.SearchItemAdapter
import com.maxrave.simpmusic.common.Config
import com.maxrave.simpmusic.data.db.entities.SongEntity
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.data.model.searchResult.songs.Artist
import com.maxrave.simpmusic.data.queue.Queue
import com.maxrave.simpmusic.databinding.BottomSheetNowPlayingBinding
import com.maxrave.simpmusic.databinding.BottomSheetSeeArtistOfNowPlayingBinding
import com.maxrave.simpmusic.databinding.FragmentDownloadedBinding
import com.maxrave.simpmusic.extension.connectArtists
import com.maxrave.simpmusic.extension.toTrack
import com.maxrave.simpmusic.viewModel.DownloadedViewModel
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.insetter.applyInsetter

@AndroidEntryPoint
class DownloadedFragment : Fragment() {
    private var _binding: FragmentDownloadedBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel by viewModels<DownloadedViewModel>()

    private lateinit var downloadedAdapter: SearchItemAdapter
    private lateinit var listDownloaded: ArrayList<Any>
    
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentDownloadedBinding.inflate(inflater, container, false)
        binding.topAppBarLayout.applyInsetter {
            type(statusBars = true) {
                margin()
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listDownloaded = ArrayList<Any>()
        downloadedAdapter = SearchItemAdapter(arrayListOf(), requireContext())
        binding.rvDownloaded.apply {
            adapter = downloadedAdapter
            layoutManager = LinearLayoutManager(context)
        }

        viewModel.getListDownloadedSong()
        viewModel.listDownloadedSong.observe(viewLifecycleOwner){ downloaded ->
            listDownloaded.clear()
            val tempDownloaded = mutableListOf<SongEntity>()
            for (i in downloaded.size - 1 downTo 0) {
                tempDownloaded.add(downloaded[i])
            }
            listDownloaded.addAll(tempDownloaded)
            downloadedAdapter.updateList(listDownloaded)
        }

        downloadedAdapter.setOnClickListener(object : SearchItemAdapter.onItemClickListener {
            override fun onItemClick(position: Int, type: String) {
                val song = downloadedAdapter.getCurrentList()[position] as SongEntity
                val args = Bundle()
                args.putString("type", Config.ALBUM_CLICK)
                args.putString("videoId", song.videoId)
                args.putString("from", "Downloaded")
                args.putInt("index", position)
                args.putInt("downloaded", 1)
                Queue.clear()
                Queue.setNowPlaying(song.toTrack())
                Queue.addAll(downloadedAdapter.getCurrentList().map { (it as SongEntity).toTrack()} as ArrayList<Track>)
                Queue.removeTrackWithIndex(position)
                findNavController().navigate(R.id.action_global_nowPlayingFragment, args)
            }

            override fun onOptionsClick(position: Int, type: String) {
                val song = listDownloaded[position] as SongEntity
                viewModel.getSongEntity(song.videoId)
                val dialog = BottomSheetDialog(requireContext())
                val bottomSheetView = BottomSheetNowPlayingBinding.inflate(layoutInflater)
                with(bottomSheetView) {
                    viewModel.songEntity.observe(viewLifecycleOwner) { songEntity ->
                        if (songEntity.liked) {
                            tvFavorite.text = "Liked"
                            cbFavorite.isChecked = true
                        }
                        else {
                            tvFavorite.text = "Like"
                            cbFavorite.isChecked = false
                        }
                    }
                    tvSongTitle.text = song.title
                    tvSongTitle.isSelected = true
                    tvSongArtist.text = song.artistName?.connectArtists()
                    tvSongArtist.isSelected = true
                    ivThumbnail.load(song.thumbnails)

                    btLike.setOnClickListener {
                        if (cbFavorite.isChecked){
                            cbFavorite.isChecked = false
                            tvFavorite.text = "Like"
                            viewModel.updateLikeStatus(song.videoId, 0)
                            viewModel.listDownloadedSong.observe(viewLifecycleOwner){ downloaded ->
                                listDownloaded.clear()
                                val tempDownloaded = mutableListOf<SongEntity>()
                                for (i in downloaded.size - 1 downTo 0) {
                                    tempDownloaded.add(downloaded[i])
                                }
                                listDownloaded.addAll(tempDownloaded)
                                downloadedAdapter.updateList(listDownloaded)
                            }
                        }
                        else {
                            cbFavorite.isChecked = true
                            tvFavorite.text = "Liked"
                            viewModel.updateLikeStatus(song.videoId, 1)
                            viewModel.listDownloadedSong.observe(viewLifecycleOwner){ downloaded ->
                                listDownloaded.clear()
                                val tempDownloaded = mutableListOf<SongEntity>()
                                for (i in downloaded.size - 1 downTo 0) {
                                    tempDownloaded.add(downloaded[i])
                                }
                                listDownloaded.addAll(tempDownloaded)
                                downloadedAdapter.updateList(listDownloaded)
                            }
                        }
                    }

                    btSeeArtists.setOnClickListener {
                        val subDialog = BottomSheetDialog(requireContext())
                        val subBottomSheetView = BottomSheetSeeArtistOfNowPlayingBinding.inflate(layoutInflater)
                        Log.d("FavoriteFragment", "onOptionsClick: ${song.artistId}")
                        if (!song.artistName.isNullOrEmpty()) {
                            val tempArtist = mutableListOf<Artist>()
                            for (i in 0 until song.artistName.size) {
                                tempArtist.add(Artist(name = song.artistName[i], id = song.artistId?.get(i)))
                            }
                            Log.d("FavoriteFragment", "onOptionsClick: $tempArtist")
                            val artistAdapter = SeeArtistOfNowPlayingAdapter(tempArtist)
                            subBottomSheetView.rvArtists.apply {
                                adapter = artistAdapter
                                layoutManager = LinearLayoutManager(requireContext())
                            }
                            artistAdapter.setOnClickListener(object : SeeArtistOfNowPlayingAdapter.OnItemClickListener {
                                override fun onItemClick(position: Int) {
                                    val artist = tempArtist[position]
                                    if (artist.id != null) {
                                        findNavController().navigate(R.id.action_global_artistFragment, Bundle().apply {
                                            putString("channelId", artist.id)
                                        })
                                        subDialog.dismiss()
                                        dialog.dismiss()
                                    }
                                }

                            })
                        }

                        subDialog.setCancelable(true)
                        subDialog.setContentView(subBottomSheetView.root)
                        subDialog.show()
                    }
                    btShare.setOnClickListener {
                        val shareIntent = Intent(Intent.ACTION_SEND)
                        shareIntent.type = "text/plain"
                        val url = "https://youtube.com/watch?v=${song.videoId}"
                        shareIntent.putExtra(Intent.EXTRA_TEXT, url)
                        val chooserIntent = Intent.createChooser(shareIntent, "Chia sẻ URL")
                        startActivity(chooserIntent)
                    }
                }
                dialog.setCancelable(true)
                dialog.setContentView(bottomSheetView.root)
                dialog.show()
            }

        })

        binding.topAppBar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }
}