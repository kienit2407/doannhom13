package com.example.doan13.ui.adapters

import com.example.doan13.ui.fragments.SearchPlaylistsFragment
import com.example.doan13.ui.fragments.SearchTracksFragment
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class SearchPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 2 // Chỉ có 2 tab

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> SearchTracksFragment()    // Tab đầu tiên: Bài hát
            1 -> SearchPlaylistsFragment() // Tab thứ hai: Playlist
            else -> SearchTracksFragment() // Mặc định
        }
    }
}