package com.example.ui.dashboard

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.DocumentEntity
import com.example.data.repository.DocumentRepository
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DashboardUiState(
    val documents: List<DocumentEntity> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: String = "Semua",
    val isLoading: Boolean = false,
    val loadingMessage: String = "",
    val statusMessage: String? = null,
    val isAppLocked: Boolean = false,
    val pendingScanResult: GmsDocumentScanningResult? = null,
    val pendingGalleryUris: List<Uri>? = null
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class DashboardViewModel(
    private val repository: DocumentRepository,
    initialLocked: Boolean = false
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow("Semua")
    private val _isLoading = MutableStateFlow(false)
    private val _loadingMessage = MutableStateFlow("")
    private val _statusMessage = MutableStateFlow<String?>(null)
    private val _isAppLocked = MutableStateFlow(initialLocked)
    private val _pendingScanResult = MutableStateFlow<GmsDocumentScanningResult?>(null)
    private val _pendingGalleryUris = MutableStateFlow<List<Uri>?>(null)

    val uiState: StateFlow<DashboardUiState> = combine(
        _searchQuery.debounce(250L).distinctUntilChanged(),
        _selectedCategory
    ) { query, category ->
        Pair(query, category)
    }.flatMapLatest { (query, category) ->
        repository.searchDocuments(query, category)
    }.combine(_isLoading) { docs, loading ->
        Pair(docs, loading)
    }.combine(_loadingMessage) { (docs, loading), loadMsg ->
        Triple(docs, loading, loadMsg)
    }.combine(_statusMessage) { (docs, loading, loadMsg), status ->
        DashboardUiState(
            documents = docs,
            isLoading = loading,
            loadingMessage = loadMsg,
            statusMessage = status
        )
    }.combine(_searchQuery) { state, query ->
        state.copy(searchQuery = query)
    }.combine(_selectedCategory) { state, category ->
        state.copy(selectedCategory = category)
    }.combine(_isAppLocked) { state, locked ->
        state.copy(isAppLocked = locked)
    }.combine(_pendingScanResult) { state, pendingScan ->
        state.copy(pendingScanResult = pendingScan)
    }.combine(_pendingGalleryUris) { state, pendingUris ->
        state.copy(pendingGalleryUris = pendingUris)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = DashboardUiState(isAppLocked = initialLocked)
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelected(category: String) {
        _selectedCategory.value = category
    }

    fun onScanResultReceived(result: GmsDocumentScanningResult) {
        _pendingScanResult.value = result
    }

    fun onGalleryImagesReceived(uris: List<Uri>) {
        if (uris.isNotEmpty()) {
            _pendingGalleryUris.value = uris
        }
    }

    fun dismissPendingDialogs() {
        _pendingScanResult.value = null
        _pendingGalleryUris.value = null
    }

    fun confirmSaveScan(title: String, category: String) {
        val scan = _pendingScanResult.value ?: return
        _pendingScanResult.value = null

        viewModelScope.launch {
            _isLoading.value = true
            _loadingMessage.value = "Menyimpan PDF & ekstraksi OCR lokal..."
            try {
                val saved = repository.processAndSaveScanResult(
                    result = scan,
                    customTitle = title,
                    category = category
                )
                if (saved != null) {
                    _statusMessage.value = "Dokumen '${saved.title}' berhasil disimpan!"
                } else {
                    _statusMessage.value = "Gagal memproses hasil pemindaian."
                }
            } catch (e: Exception) {
                _statusMessage.value = "Error: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun confirmSaveGallery(title: String, category: String) {
        val uris = _pendingGalleryUris.value ?: return
        _pendingGalleryUris.value = null

        viewModelScope.launch {
            _isLoading.value = true
            _loadingMessage.value = "Mengonversi ${uris.size} gambar ke PDF & menjalankan OCR..."
            try {
                val saved = repository.processAndSaveGalleryImages(
                    uris = uris,
                    customTitle = title,
                    category = category
                )
                if (saved != null) {
                    _statusMessage.value = "Dokumen galeri '${saved.title}' berhasil dibuat!"
                } else {
                    _statusMessage.value = "Gagal membuat PDF dari gambar galeri."
                }
            } catch (e: Exception) {
                _statusMessage.value = "Error: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateDocument(doc: DocumentEntity, newTitle: String, newCategory: String) {
        viewModelScope.launch {
            repository.updateDocument(doc.copy(title = newTitle, category = newCategory))
            _statusMessage.value = "Dokumen berhasil diperbarui."
        }
    }

    fun deleteDocument(doc: DocumentEntity) {
        viewModelScope.launch {
            repository.deleteDocument(doc)
            _statusMessage.value = "Dokumen '${doc.title}' telah dihapus."
        }
    }

    fun unlockApp() {
        _isAppLocked.value = false
    }

    fun lockApp() {
        _isAppLocked.value = true
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun getShareablePdfUri(pdfPath: String): Uri {
        return repository.getShareablePdfUri(pdfPath)
    }
}

class DashboardViewModelFactory(
    private val repository: DocumentRepository,
    private val initialLocked: Boolean
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            return DashboardViewModel(repository, initialLocked) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
