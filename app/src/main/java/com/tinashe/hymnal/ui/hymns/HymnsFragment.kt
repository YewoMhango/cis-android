package com.tinashe.hymnal.ui.hymns

import android.app.ActivityOptions
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.DividerItemDecoration.VERTICAL
import com.tinashe.hymnal.R
import com.tinashe.hymnal.data.model.Hymnal
import com.tinashe.hymnal.data.model.constants.Status
import com.tinashe.hymnal.databinding.FragmentHymnsBinding
import com.tinashe.hymnal.extensions.arch.observeNonNull
import com.tinashe.hymnal.extensions.context.getColorPrimary
import com.tinashe.hymnal.ui.AppBarBehaviour
import com.tinashe.hymnal.ui.hymns.adapter.HymnListAdapter
import com.tinashe.hymnal.ui.hymns.hymnals.HymnalListFragment.Companion.SELECTED_HYMNAL_KEY
import com.tinashe.hymnal.ui.hymns.sing.SingHymnsActivity
import dagger.hilt.android.AndroidEntryPoint
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt

@AndroidEntryPoint
class HymnsFragment : Fragment() {

    private val viewModel: HymnsViewModel by viewModels()

    private var binding: FragmentHymnsBinding? = null

    private val listAdapter: HymnListAdapter = HymnListAdapter { pair ->
        openSelectedHymn(pair.first.hymnId, pair.second)
    }

    private var appBarBehaviour: AppBarBehaviour? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        appBarBehaviour = context as? AppBarBehaviour
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return FragmentHymnsBinding.inflate(inflater, container, false).also {
            binding = it
            binding?.hymnsListView?.apply {
                addItemDecoration(DividerItemDecoration(context, VERTICAL))
                adapter = listAdapter
            }
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.showHymnalsPromptLiveData.observe(
            viewLifecycleOwner,
            {
                showHymnalsTargetPrompt()
            }
        )
        viewModel.statusLiveData.observeNonNull(viewLifecycleOwner) {
            binding?.apply {
                hymnsListView.isVisible = it != Status.LOADING
                progressBar.isVisible = it == Status.LOADING
            }
        }
        viewModel.messageLiveData.observeNonNull(viewLifecycleOwner) {
            binding?.snackbar?.show(messageText = it)
        }
        viewModel.hymnalTitleLiveData.observeNonNull(viewLifecycleOwner) {
            appBarBehaviour?.setAppBarTitle(it)
        }
        viewModel.selectedHymnIdLiveData.observeNonNull(viewLifecycleOwner) {
            openSelectedHymn(it)
        }
        viewModel.hymnListLiveData.observeNonNull(viewLifecycleOwner) { hymns ->
            listAdapter.submitList(ArrayList(hymns))
        }

        findNavController()
            .currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<Hymnal>(SELECTED_HYMNAL_KEY)
            ?.observeNonNull(viewLifecycleOwner) {
                viewModel.hymnalSelected(it)
            }
    }

    private fun openSelectedHymn(hymnId: Int, animView: View? = null) {
        val intent = SingHymnsActivity.singIntent(requireContext(), hymnId)
        animView?.let {
            val options = ActivityOptions.makeSceneTransitionAnimation(
                requireActivity(),
                it,
                getString(R.string.transition_shared_element)
            )
            requireActivity().startActivity(intent, options.toBundle())
        } ?: startActivity(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.hymns_list, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView: SearchView = searchItem.actionView as SearchView
        searchView.queryHint = getString(R.string.hint_search_hymns)
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.actions_number -> {
                val fragment = PickHymnFragment.newInstance { number ->
                    viewModel.hymnNumberSelected(requireContext(), number)
                }
                fragment.show(childFragmentManager, fragment.tag)
                true
            }
            R.id.actions_hymnals -> {
                with(findNavController()) {
                    if (currentDestination?.id == R.id.navigation_hymns) {
                        navigate(R.id.action_navigate_to_hymnalListFragment)
                    }
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showHymnalsTargetPrompt() {
        MaterialTapTargetPrompt.Builder(requireActivity())
            .setTarget(R.id.actions_hymnals)
            .setIcon(R.drawable.ic_bookshelf_prompt)
            .setBackgroundColour(requireContext().getColorPrimary())
            .setPrimaryText(R.string.switch_between_hymnals)
            .setSecondaryText(R.string.switch_between_hymnals_message)
            .show()

        viewModel.hymnalsPromptShown()
    }
}
