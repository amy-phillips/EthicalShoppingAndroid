package uk.co.islovely.ethicalshopping

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import uk.co.islovely.ethicalshopping.databinding.FragmentFirstBinding

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private var mainActivity: MainActivity? = null
    private var subscribed: Boolean = false
    private val LOGTAG = "MainMenuFragment"

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root

    }

    private fun scoresProgressCallback(progress: Int, subscribed: Boolean, foodSections: List<FoodSection>) {
        activity?.runOnUiThread(java.lang.Runnable {
            if(isAdded) {
                binding.progressGetfoods.visibility = View.VISIBLE
                binding.progressGetfoods.progress = progress
            }
        })
        // if we just started then we have no data, so quit here
        if(progress==0)
            return
        if(subscribed){
            activity?.runOnUiThread(java.lang.Runnable {
                if(isAdded) {
                    binding.buttonEthicalconsumer.setBackgroundColor(Color.GREEN);
                    binding.buttonEthicalconsumer.setText(R.string.ethical_consumer);
                }
            })
        } else {
            activity?.runOnUiThread(java.lang.Runnable {
                if(isAdded) {
                    binding.buttonEthicalconsumer.setBackgroundColor(Color.RED);
                    binding.buttonEthicalconsumer.setText(R.string.ethical_consumer_requires_login);
                }
            })
        }
        if(progress == 100) {
            if(isAdded) {
                binding.progressGetfoods.visibility = View.INVISIBLE
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonTesco.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }
        binding.buttonSainsburys.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }
        binding.buttonEthicalconsumer.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_ethicalConsumerFragment)
        }

        // kick off getting score tables
        ScoresRepository.startGettingScores(::scoresProgressCallback)
    }

    override fun onDestroyView() {
        //ScoresRepository.stopGettingScoreUpdates()
        super.onDestroyView()
        _binding = null
    }


}