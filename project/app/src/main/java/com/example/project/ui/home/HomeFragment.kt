package com.example.project.ui.home

import NetworkManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.project.PacketViewModel
import com.example.project.Profile
import com.example.project.R
import com.example.project.databinding.FragmentHomeBinding
import com.example.project.ui.addprofile.AddProfileFragment.Companion.PROFILE_PREFS_KEY


class HomeFragment : Fragment() {

    private lateinit var packetViewModel: PacketViewModel
    private val networkManager = NetworkManager()

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var profileNameTextView: TextView
    private lateinit var profileStepTextView: TextView
    private lateinit var profileTimeTextView: TextView
    private lateinit var profileAlarmTextView: TextView


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val view = binding.root


        // Initialize the views
        profileNameTextView = view.findViewById(R.id.profile_name)
        profileStepTextView = view.findViewById(R.id.profile_step)
        profileTimeTextView = view.findViewById(R.id.profile_time)
        profileAlarmTextView = view.findViewById(R.id.profile_alarm)


        return view
    }

    private var isFirstLaunch = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        packetViewModel = ViewModelProvider(requireActivity()).get(PacketViewModel::class.java)

        val sharedPreferences = requireContext().getSharedPreferences(PROFILE_PREFS_KEY, Context.MODE_PRIVATE)
        val selectedProfileName = sharedPreferences.getString("selected_profile", null)
        if (selectedProfileName != null) {
            val profileJson = sharedPreferences.getString(selectedProfileName, null)
            if (profileJson != null) {
                val selectedProfile = Profile.fromJson(profileJson)
                showProfileDetails(selectedProfile)

                if (isFirstLaunch && selectedProfile != null) {
                    sendPacketBasedOnSelectedProfile(selectedProfile)
                    isFirstLaunch = false
                }

            } else {
                Toast.makeText(requireContext(), "프로필이 없습니다. 프로필을 추가해주세요.", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_homeFragment_to_addProfileFragment)
            }
        } else {
            Toast.makeText(requireContext(), "프로필이 없습니다. 프로필을 추가해주세요.", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_homeFragment_to_addProfileFragment)
        }

        binding.addProfileButton.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_addProfileFragment)
        }

        binding.changeButton.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_profileListFragment)
        }

        binding.cameraOuputButton.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_streamingFragment)
        }

        binding.trunOffButton.setOnClickListener {
            packetViewModel.updateParameter2("1")   // 마지막에 1 로 고쳐야함
            var resultPacket: Pair<Boolean, ByteArray> = packetViewModel.makePacketToSend()
            val isSuccess: Boolean = resultPacket.first
            val dataToSend: ByteArray = resultPacket.second
            Log.d("Packet", "Data: ${packetViewModel.parsingPacket(dataToSend)}")

            if (isSuccess) {
                networkManager.sendPacketToServer(dataToSend, packetViewModel, requireContext())
            } else {
                Toast.makeText(requireContext(), "패킷 생성 실패", Toast.LENGTH_SHORT).show()
            }

        }
    }

    fun sendPacketBasedOnSelectedProfile(profile: Profile) {
        if (profile.optimalStep != "미설정") {
            packetViewModel.updateParameter0(profile.optimalStep)
        }
        packetViewModel.updateParameter1("0")
        packetViewModel.updateParameter2("0")
        if (profile.alarmTime == "미설정") {
            packetViewModel.updateParameter3("0")
        }else {
            packetViewModel.updateParameter3(profile.alarmTime)
        }
        if (profile.alarmMode == "진동") {
            packetViewModel.updateParameter4("1")
        } else if (profile.alarmMode == "소리") {
            packetViewModel.updateParameter4("2")
        } else if (profile.alarmMode == "미설정") {
            packetViewModel.updateParameter4("0")
        }
        var resultPacket: Pair<Boolean, ByteArray> = packetViewModel.makePacketToSend()
        val isSuccess: Boolean = resultPacket.first
        val dataToSend: ByteArray = resultPacket.second
        Log.d("Packet", "Data: ${packetViewModel.parsingPacket(dataToSend)}")

        if (isSuccess) {
            networkManager.sendPacketToServer(dataToSend, packetViewModel, requireContext())
        } else {
            Toast.makeText(requireContext(), "패킷 생성 실패", Toast.LENGTH_SHORT).show()
        }
    }


    fun showProfileDetails(profile: Profile?) {
        profile?.let {
            profileNameTextView.text = it.name
            profileStepTextView.text = "최적 단계: ${it.optimalStep}"
            profileTimeTextView.text = "알람 시간(분): ${it.alarmTime}"
            profileAlarmTextView.text = "알람 방식: ${it.alarmMode}"
        }
    }

//    override fun onResume() {
//        super.onResume()
//        connectToRaspberryPiHotspot(requireActivity())
//    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

