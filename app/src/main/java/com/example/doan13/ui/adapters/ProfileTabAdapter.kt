package com.example.doan13.ui.adapters

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.doan13.ui.fragments.LikedPlaylistsTabFragment
import com.example.doan13.ui.fragments.LikedTracksTabFragment
import com.example.doan13.ui.fragments.PlaylistsTabFragment
import com.example.doan13.ui.fragments.TracksTabFragment
import com.example.doan13.viewmodels.FavoriteViewModel

class ProfileTabAdapter(fragment: Fragment, private val favoriteViewModel: FavoriteViewModel) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> TracksTabFragment(favoriteViewModel)
            1 -> PlaylistsTabFragment(favoriteViewModel)
            2 -> LikedTracksTabFragment(favoriteViewModel)
            3 -> LikedPlaylistsTabFragment(favoriteViewModel)
            else -> LikedPlaylistsTabFragment(favoriteViewModel)
        }
    }
}