package com.tinashe.hymnal.ui.hymns

import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.DividerItemDecoration.VERTICAL
import com.tinashe.hymnal.R
import com.tinashe.hymnal.data.model.constants.Status
import com.tinashe.hymnal.databinding.FragmentHymnsBinding
import com.tinashe.hymnal.extensions.arch.observeNonNull
import com.tinashe.hymnal.ui.AppBarBehaviour
import com.tinashe.hymnal.ui.hymns.adapter.HymnListAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HymnsFragment : Fragment() {

    private val viewModel: HymnsViewModel by viewModels()

    private var binding: FragmentHymnsBinding? = null

    private val listAdapter: HymnListAdapter = HymnListAdapter()

    private var appBarBehaviour: AppBarBehaviour? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is AppBarBehaviour) {
            appBarBehaviour = context
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return FragmentHymnsBinding.inflate(inflater, container, false).also {
            setHasOptionsMenu(true)
            binding = it
            binding?.hymnsListView?.apply {
                addItemDecoration(DividerItemDecoration(context, VERTICAL))
                adapter = listAdapter
            }
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.statusLiveData.observeNonNull(viewLifecycleOwner) {
            binding?.apply {
                hymnsListView.isVisible = it != Status.LOADING
                progressBar.isVisible = it == Status.LOADING
            }
        }
        viewModel.hymnalTitleLiveData.observeNonNull(viewLifecycleOwner) {
            appBarBehaviour?.setAppBarTitle(it)
        }
        viewModel.hymnListLiveData.observeNonNull(viewLifecycleOwner) { hymns ->
            listAdapter.submitList(ArrayList(hymns))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.hymns_list, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView : SearchView = searchItem.actionView as SearchView
        searchItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                appBarBehaviour?.setAppBarExpanded(false)
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                appBarBehaviour?.setAppBarExpanded(true)
                return true
            }
        })
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(query: String?): Boolean {
                viewModel.performSearch(query)
                return true
            }
        })
    }
}