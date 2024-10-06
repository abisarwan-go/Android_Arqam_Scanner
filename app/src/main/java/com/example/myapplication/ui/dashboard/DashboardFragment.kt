package com.example.myapplication.ui.dashboard

import ApiErrorResponse
import BoardingStatus
import StudentBoardingStatus
import StudentExitPermission
import StudentMealTypeInfoToday
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.fragment.app.Fragment
import com.example.myapplication.R
import com.example.myapplication.api.ApiService
import com.example.myapplication.api.RetrofitClient
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.myapplication.databinding.FragmentDashboardBinding
import java.io.IOException
import java.net.SocketTimeoutException


class DashboardFragment : Fragment() {

    private lateinit var previewView: PreviewView
    private lateinit var messageView: TextView
    private lateinit var apiService: ApiService
    private var selectedButton = "autorisation_de_sortie"

    companion object {
        const val CAMERA_REQUEST_CODE = 1001
    }

    private var lastScannedBarcode: String? = null
    @androidx.camera.core.ExperimentalGetImage
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            // Barcode scanning
            val barcodeScanner = BarcodeScanning.getClient()

            val imageAnalyzer = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(Executors.newSingleThreadExecutor(), { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                            barcodeScanner.process(image)
                                .addOnSuccessListener { barcodes ->
                                    for (barcode in barcodes) {
                                        val barcodeValue = barcode.rawValue ?: ""

                                        // Check if the barcode is the same as the last one
                                        if (barcodeValue != lastScannedBarcode) {
                                            // Update the last scanned barcode
                                            lastScannedBarcode = barcodeValue

                                            // Call the API with the barcode value
                                            callApi(barcodeValue)
                                        }
                                    }
                                }
                                .addOnFailureListener {
                                    messageView.text = "Barcode scanning failed"
                                }
                                .addOnCompleteListener {
                                    imageProxy.close()
                                }
                        }
                    })
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )
            } catch (exc: Exception) {
                messageView.text = "Camera initialization failed"
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }
    @androidx.camera.core.ExperimentalGetImage
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize UI components
        previewView = view.findViewById(R.id.camera_view)
        messageView = view.findViewById(R.id.message_view)

        messageView.text = "Aucune carte scannée pour le moment"

        val buttonAutorisationDeSortie: Button = view.findViewById(R.id.button_autorisation_de_sortie)
        val buttonRepasChaudFroid: Button = view.findViewById(R.id.button_repas_chaud_froid)
        val buttonDemiExterne: Button = view.findViewById(R.id.button_demi_externe)

        // Set default button as autorisation_de_sortie
        selectedButton = "autorisation_de_sortie"

        // Set button click listeners
        buttonAutorisationDeSortie.setOnClickListener {
            selectedButton = "autorisation_de_sortie"
        }

        buttonRepasChaudFroid.setOnClickListener {
            selectedButton = "repas_chaud_froid"
        }

        buttonDemiExterne.setOnClickListener {
            selectedButton = "demi_externe"
        }

        // Initialize Retrofit client
        apiService = RetrofitClient.getClient().create(ApiService::class.java)

        // Check and request camera permission before starting the camera
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            startCamera()  // Permission already granted, start camera
        } else {
            // Request camera permission
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_REQUEST_CODE
            )
        }
    }

    // Handle Exit Permission response
    private fun handleExitPermissionResponse(responseBody: StudentExitPermission?, qrCode: String): String {
        return if (responseBody != null) {
        """
        ${responseBody.firstName} ${responseBody.lastName.uppercase()}
        ${if (responseBody.canGoOutInCaseOfAbsence) "✔️ Peut sortir" else "❌ Ne peut pas sortir"}
        """.trimIndent()
        } else {
            "Appel API réussi, mais aucune donnée reçue pour l'autorisation de sortie."
        }
    }

    // Handle Meal Type Info response
    private fun handleMealTypeResponse(responseBody: StudentMealTypeInfoToday?, qrCode: String): String {
        return if (responseBody != null) {
            """
        ${responseBody.firstName} ${responseBody.lastName.uppercase()}
        ${if (responseBody.mealTypeToday == MealType.HOT) "🔥 Repas chaud" 
            else if (responseBody.mealTypeToday == MealType.COLD) "❄️ Repas froid" 
                else "\uD83D\uDE22 Pas de plat pour aujourd'hui"}
        ${if (responseBody.alreadyTakenToday) "⚠️ ATTENTION: Repas déjà pris aujourd'hui" else ""}
        """.trimIndent()
        } else {
            "Appel API réussi, mais aucune donnée reçue pour les informations sur le type de repas."
        }
    }

    // Handle Boarding Status response
    private fun handleBoardingStatusResponse(responseBody: StudentBoardingStatus?, qrCode: String): String {
        return if (responseBody != null) {
            """
            ${responseBody.firstName} ${responseBody.lastName.uppercase()}
            ${if (responseBody.boardingStatus==BoardingStatus.EXTERNAL) "\uD83C\uDFE0 Externe" else "\uD83C\uDF7D\uFE0F Demi-pensionnaire"}
            """.trimIndent()
        } else {
            "Appel API réussi, mais aucune donnée reçue pour le statut d'internat."
        }
    }

    @androidx.camera.core.ExperimentalGetImage
    private fun callApi(qrCode: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = when (selectedButton) {
                    "autorisation_de_sortie" -> apiService.updateExitPermission(qrCode).execute()
                    "repas_chaud_froid" -> apiService.updateMealType(qrCode).execute()
                    "demi_externe" -> apiService.updateBoardingStatus(qrCode).execute()
                    else -> null
                }

                response?.let {
                    withContext(Dispatchers.Main) {
                        if (it.isSuccessful) {
                            val message = when (selectedButton) {
                                "autorisation_de_sortie" -> {
                                    val exitPermissionResponse = it.body() as? StudentExitPermission
                                    handleExitPermissionResponse(exitPermissionResponse, qrCode)
                                }
                                "repas_chaud_froid" -> {
                                    val mealTypeResponse = it.body() as? StudentMealTypeInfoToday
                                    handleMealTypeResponse(mealTypeResponse, qrCode)
                                }
                                "demi_externe" -> {
                                    val boardingStatusResponse = it.body() as? StudentBoardingStatus
                                    handleBoardingStatusResponse(boardingStatusResponse, qrCode)
                                }
                                else -> "Unknown response type."
                            }
                            messageView.text = message
                        } else {
                            // Use Retrofit to convert the errorBody into ApiErrorResponse
                            val errorConverter = RetrofitClient.getClient().responseBodyConverter<ApiErrorResponse>(
                                ApiErrorResponse::class.java,
                                arrayOfNulls<Annotation>(0)
                            )

                            val errorBody = it.errorBody()
                            val errorResponse: ApiErrorResponse? = errorBody?.let { body ->
                                errorConverter.convert(body)
                            }

                            // Display only the "message" from the error response
                            messageView.text = errorResponse?.message ?: "Unknown error occurred"
                        }
                    }
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    // Afficher un message personnalisé pour une erreur de connexion
                    messageView.text = "Veuillez vous connecter au Wi-Fi Arqam et vérifier que le signal est suffisant."
                }
            } catch (e: SocketTimeoutException) {
                withContext(Dispatchers.Main) {
                    // Gérer une erreur de délai dépassé (timeout)
                    messageView.text = "Veuillez vous connecter au Wi-Fi Arqam et vérifier que le signal est suffisant."
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    messageView.text = "Erreur : ${e.message ?: "Erreur inconnue"}"
                }
            }
        }
    }

    @androidx.camera.core.ExperimentalGetImage
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, start camera
                startCamera()
            } else {
                // Permission denied, show a message
                messageView.text = "Camera permission denied"
            }
        }
    }

    private lateinit var binding: FragmentDashboardBinding
    @androidx.camera.core.ExperimentalGetImage
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDashboardBinding.inflate(inflater, container, false)

        // Initialize the button group listener
        binding.buttonLayout.addOnButtonCheckedListener { group, checkedId, isChecked ->
            when (checkedId) {
                R.id.button_autorisation_de_sortie -> {
                    messageView.text = "En cours de chargement...";
                    lastScannedBarcode = null
                    if (isChecked) {
                        startCamera()
                        binding.buttonAutorisationDeSortie.setBackgroundColor(
                            ContextCompat.getColor(requireContext(), R.color.blue)
                        )
                    } else {
                        binding.buttonAutorisationDeSortie.setBackgroundColor(
                            ContextCompat.getColor(requireContext(), R.color.white)
                        )
                    }
                }
                R.id.button_repas_chaud_froid -> {
                    messageView.text = "En cours de chargement...";
                    lastScannedBarcode = null
                    if (isChecked) {
                        binding.buttonRepasChaudFroid.setBackgroundColor(
                            ContextCompat.getColor(requireContext(), R.color.blue)
                        )
                    } else {
                        binding.buttonRepasChaudFroid.setBackgroundColor(
                            ContextCompat.getColor(requireContext(), R.color.white)
                        )
                    }
                }
                R.id.button_demi_externe -> {
                    messageView.text = "En cours de chargement...";
                    lastScannedBarcode = null
                    if (isChecked) {
                        binding.buttonDemiExterne.setBackgroundColor(
                            ContextCompat.getColor(requireContext(), R.color.blue)
                        )
                    } else {
                        binding.buttonDemiExterne.setBackgroundColor(
                            ContextCompat.getColor(requireContext(), R.color.white)
                        )
                    }
                }
            }
        }

        return binding.root
    }
}
