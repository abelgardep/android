/*
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * Copyright (C) 2022 ownCloud GmbH.
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.presentation.ui.files.details

import android.accounts.Account
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.owncloud.android.domain.capabilities.model.OCCapability
import com.owncloud.android.domain.capabilities.usecases.GetCapabilitiesAsLiveDataUseCase
import com.owncloud.android.domain.capabilities.usecases.RefreshCapabilitiesFromServerAsyncUseCase
import com.owncloud.android.domain.files.GetUrlToOpenInWebUseCase
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.files.usecases.GetFileByIdAsStreamUseCase
import com.owncloud.android.domain.files.usecases.GetFileByIdUseCase
import com.owncloud.android.domain.utils.Event
import com.owncloud.android.extensions.ViewModelExt.runUseCaseWithResult
import com.owncloud.android.extensions.getRunningWorkInfosByTags
import com.owncloud.android.extensions.isDownload
import com.owncloud.android.extensions.isUpload
import com.owncloud.android.presentation.UIResult
import com.owncloud.android.providers.ContextProvider
import com.owncloud.android.providers.CoroutinesDispatcherProvider
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.preview.PreviewAudioFragment
import com.owncloud.android.ui.preview.PreviewTextFragment
import com.owncloud.android.ui.preview.PreviewVideoFragment
import com.owncloud.android.usecases.transfers.downloads.CancelDownloadForFileUseCase
import com.owncloud.android.usecases.transfers.uploads.CancelUploadForFileUseCase
import com.owncloud.android.workers.DownloadFileWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class FileDetailsViewModel(
    private val openInWebUseCase: GetUrlToOpenInWebUseCase,
    refreshCapabilitiesFromServerAsyncUseCase: RefreshCapabilitiesFromServerAsyncUseCase,
    getCapabilitiesAsLiveDataUseCase: GetCapabilitiesAsLiveDataUseCase,
    val contextProvider: ContextProvider,
    private val cancelDownloadForFileUseCase: CancelDownloadForFileUseCase,
    private val getFileByIdUseCase: GetFileByIdUseCase,
    getFileByIdAsStreamUseCase: GetFileByIdAsStreamUseCase,
    private val cancelUploadForFileUseCase: CancelUploadForFileUseCase,
    private val coroutinesDispatcherProvider: CoroutinesDispatcherProvider,
    private val workManager: WorkManager,
    account: Account,
    ocFile: OCFile,
) : ViewModel() {

    private val _openInWebUriLiveData: MediatorLiveData<Event<UIResult<String?>>> = MediatorLiveData()
    val openInWebUriLiveData: LiveData<Event<UIResult<String?>>> = _openInWebUriLiveData

    var capabilities: LiveData<OCCapability?> =
        getCapabilitiesAsLiveDataUseCase.execute(GetCapabilitiesAsLiveDataUseCase.Params(account.name))

    private val account: StateFlow<Account> = MutableStateFlow(account)
    val currentFile: StateFlow<OCFile> =
        getFileByIdAsStreamUseCase.execute(GetFileByIdAsStreamUseCase.Params(ocFile.id!!))
            .stateIn(
                viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ocFile
            )

    private val _ongoingTransferUUID = MutableLiveData<UUID>()
    private val _ongoingTransfer = Transformations.switchMap(_ongoingTransferUUID) { transferUUID ->
        workManager.getWorkInfoByIdLiveData(transferUUID)
    }.map { Event(it) }
    val ongoingTransfer: LiveData<Event<WorkInfo?>> = _ongoingTransfer

    init {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            refreshCapabilitiesFromServerAsyncUseCase.execute(RefreshCapabilitiesFromServerAsyncUseCase.Params(account.name))
        }
    }

    fun getCurrentFile() = currentFile.value
    fun getAccount() = account.value

    fun startListeningToWorkInfo(uuid: UUID?) {
        uuid ?: return

        _ongoingTransferUUID.postValue(uuid)
    }

    fun checkOnGoingTransfersWhenOpening() {
        val listOfWorkers =
            workManager.getRunningWorkInfosByTags(listOf(getCurrentFile().id!!.toString(), getAccount().name, DownloadFileWorker::class.java.name))
        listOfWorkers.firstOrNull()?.let { workInfo ->
            _ongoingTransferUUID.postValue(workInfo.id)
        }
    }

    fun cancelCurrentTransfer() {
        val currentTransfer = ongoingTransfer.value?.peekContent() ?: return
        if (currentTransfer.isUpload()) {
            cancelUploadForFileUseCase.execute(CancelUploadForFileUseCase.Params(currentFile.value))
        } else if (currentTransfer.isDownload()) {
            cancelDownloadForFileUseCase.execute(CancelDownloadForFileUseCase.Params(currentFile.value))
        }
    }

    fun isOpenInWebAvailable(): Boolean = capabilities.value?.isOpenInWebAllowed() ?: false

    fun openInWeb(fileId: String) {
        runUseCaseWithResult(
            coroutineDispatcher = coroutinesDispatcherProvider.io,
            liveData = _openInWebUriLiveData,
            useCase = openInWebUseCase,
            useCaseParams = GetUrlToOpenInWebUseCase.Params(openWebEndpoint = capabilities.value?.filesOcisProviders?.openWebUrl!!, fileId = fileId),
            showLoading = false,
            requiresConnection = true,
        )
    }

    // TODO: I don't like this at all. Move navigation to a common place.
    fun navigateToPreviewOrOpenFile(fileDisplayActivity: FileDisplayActivity, file: OCFile) {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            val useCaseResult = getFileByIdUseCase.execute(GetFileByIdUseCase.Params(fileId = file.id!!))
            val fileWaitingToPreview = useCaseResult.getDataOrNull()
            viewModelScope.launch(coroutinesDispatcherProvider.main) {
                when {
                    PreviewAudioFragment.canBePreviewed(fileWaitingToPreview) -> {
                        fileDisplayActivity.startAudioPreview(fileWaitingToPreview!!, 0)
                    }
                    PreviewVideoFragment.canBePreviewed(fileWaitingToPreview) -> {
                        fileDisplayActivity.startVideoPreview(fileWaitingToPreview!!, 0)
                    }
                    PreviewTextFragment.canBePreviewed(fileWaitingToPreview) -> {
                        fileDisplayActivity.startTextPreview(fileWaitingToPreview)
                    }
                    else -> fileDisplayActivity.fileOperationsHelper.openFile(fileWaitingToPreview)
                }
            }
        }
    }
}
