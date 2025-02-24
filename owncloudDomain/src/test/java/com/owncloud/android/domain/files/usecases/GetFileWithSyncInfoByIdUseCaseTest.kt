package com.owncloud.android.domain.files.usecases

import com.owncloud.android.domain.files.FileRepository
import com.owncloud.android.testutil.OC_FILE
import com.owncloud.android.testutil.OC_FILE_WITH_SYNC_INFO_AND_SPACE
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test

@ExperimentalCoroutinesApi
class GetFileWithSyncInfoByIdUseCaseTest {

    private val repository: FileRepository = spyk()
    private val useCase = GetFileWithSyncInfoByIdUseCase(repository)
    private val useCaseParams = GetFileWithSyncInfoByIdUseCase.Params(OC_FILE.id!!)

    @Test
    fun `get file with sync by id returns OCFileWithSyncInfo when no error`() = runTest {
        every { repository.getFileWithSyncInfoByIdAsFlow(useCaseParams.fileId) } returns flowOf(OC_FILE_WITH_SYNC_INFO_AND_SPACE)

        val useCaseResult = useCase.execute(useCaseParams)

        useCaseResult.collect { result ->
            Assert.assertEquals(OC_FILE_WITH_SYNC_INFO_AND_SPACE, result)
        }

        verify(exactly = 1) { repository.getFileWithSyncInfoByIdAsFlow(useCaseParams.fileId) }
    }

    @Test
    fun `get file with sync by id returns true when repository is null`() = runTest {
        val useCaseResult = useCase.execute(useCaseParams)

        every { repository.getFileWithSyncInfoByIdAsFlow(useCaseParams.fileId) } returns flowOf(null)
        Assert.assertEquals(null, useCaseResult)

        verify(exactly = 1) { repository.getFileWithSyncInfoByIdAsFlow(useCaseParams.fileId) }
    }

    @Test(expected = Exception::class)
    fun `get file with sync by id returns an exception`() = runTest {
        every { repository.getFileWithSyncInfoByIdAsFlow(useCaseParams.fileId) } throws Exception()

        useCase.execute(useCaseParams)

        verify(exactly = 1) {
            repository.getFileWithSyncInfoByIdAsFlow(useCaseParams.fileId)
        }
    }

}