package com.gft.example.mvi.xml.ui.choose

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.gft.example.mvi.xml.R
import com.gft.example.mvi.xml.databinding.FragmentChooseBinding
import com.gft.mvi.fragment.handleNavigationEffect
import com.gft.mvi.fragment.handleViewEffect
import com.gft.mvi.fragment.observeViewState
import org.koin.androidx.viewmodel.ext.android.viewModel

class ChooseFragment : Fragment(R.layout.fragment_choose) {
    private val viewModel by viewModel<ChooseViewModel>()
    private var _binding: FragmentChooseBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentChooseBinding.bind(view)

        observeViewState()
        initLayout()
        observeViewEffect()
        observeNavigation()
    }

    private fun observeViewState() {
        observeViewState(viewModel) {
            binding.numberTv.text = it.randomNumber.toString()
        }
    }

    private fun observeNavigation() {
        handleNavigationEffect(viewModel) { effect ->
            when (effect) {
                is ChoiceNavigationEffect.NavigateToDetails -> {
                    val action = ChooseFragmentDirections.toDetailsFragment(effect.id)
                    findNavController().navigate(action)
                }
            }

        }
    }

    private fun observeViewEffect() {
        handleViewEffect(viewModel) { effect ->
            when (effect) {
                is ChoiceViewEffect.ShowToast -> Toast.makeText(binding.root.context, effect.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun initLayout() {
        binding.apply {
            showToastBt.setOnClickListener {
                viewModel.onEvent(ChoiceViewEvent.OnShowToastClicked)
            }
            showFirstDetailsBt.setOnClickListener {
                viewModel.onEvent(ChoiceViewEvent.OnShowDetailsClicked("1"))
            }
            showSecondDetailsBt.setOnClickListener {
                viewModel.onEvent(ChoiceViewEvent.OnShowDetailsClicked("2"))
            }
            drawNumberBt.setOnClickListener {
                viewModel.onEvent(ChoiceViewEvent.OnDrawNumberClicked)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
