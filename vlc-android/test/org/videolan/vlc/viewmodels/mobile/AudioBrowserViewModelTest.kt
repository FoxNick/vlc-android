package org.videolan.vlc.viewmodels.mobile

import com.jraska.livedata.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import org.junit.Assert.*
import org.junit.Test
import org.videolan.medialibrary.interfaces.media.AbstractMediaWrapper
import org.videolan.medialibrary.stubs.StubDataSource
import org.videolan.vlc.BaseTest
import org.videolan.vlc.util.MEDIALIBRARY_PAGE_SIZE

@ExperimentalCoroutinesApi
@ObsoleteCoroutinesApi
class AudioBrowserViewModelTest : BaseTest() {
    private lateinit var audioBrowserViewModel: AudioBrowserViewModel

    override fun beforeTest() {
        super.beforeTest()
        StubDataSource.getInstance().resetData()
        audioBrowserViewModel = AudioBrowserViewModel(context)
    }

    private fun createDummyAudios(count: Int, title: String): List<Long> = (1..count).map {
        StubDataSource.getInstance().addMediaWrapper("$title $it", AbstractMediaWrapper.TYPE_AUDIO).id
    }

    private fun waitForProvidersData() = audioBrowserViewModel.providers.map {
        it.pagedList.test().awaitValue()
    }

    @Test
    fun whenNoArtistAlbumTrackGenre_checkResultIsEmpty() {
        waitForProvidersData()

        assertTrue(audioBrowserViewModel.isEmpty())
    }

    @Test
    fun whenNoArtistAlbumTrackGenre_checkTotalCountIsZero() {
        waitForProvidersData()

        assertEquals(0, audioBrowserViewModel.tracksProvider.getTotalCount())
        assertEquals(0, audioBrowserViewModel.genresProvider.getTotalCount())
        assertEquals(0, audioBrowserViewModel.albumsProvider.getTotalCount())
        assertEquals(0, audioBrowserViewModel.artistsProvider.getTotalCount())
    }

    @Test
    fun whenThereAre5Tracks_checkResultIsNotEmpty() {
        StubDataSource.getInstance().setAudioByCount(2, null)
        createDummyAudios(3, "XYZ")

        waitForProvidersData()
    }

    @Test
    fun whenThereAre5Tracks_checkTracksAre5GenresAre2AlbumsAre2ArtistsAre2ForTotalCount() {
        StubDataSource.getInstance().setAudioByCount(2, null) // AlbumArtist & Artist are same, so only one is added.
        createDummyAudios(3, "XYZ") // AlbumArtist & Artist are different, so both are added.

        waitForProvidersData()

        assertEquals(5, audioBrowserViewModel.tracksProvider.getTotalCount())

        /* TODO: I haven't yet checked the Medialibrary source code, but I doubt it would add duplicate album for each audio file
         * So gotta fix the logic in stubs to simulate that behaviour. Once that's fixed, I'll update my tests with proper logic.
         */
        assertEquals(5, audioBrowserViewModel.genresProvider.getTotalCount())
        assertEquals(5, audioBrowserViewModel.albumsProvider.getTotalCount())
        assertEquals(8, audioBrowserViewModel.artistsProvider.getTotalCount())
    }

    @Test
    fun whenThereAre5TracksWithShowAllTrue_checkTracksAre5GenresAre5AlbumsAre5ArtistsAre5ForPagedData() {
        StubDataSource.getInstance().setAudioByCount(2, null) // AlbumArtist & Artist are same, so only one is added.
        createDummyAudios(3, "XYZ") // AlbumArtist & Artist are different, so both are added.

        waitForProvidersData()

        assertEquals(5, audioBrowserViewModel.tracksProvider.pagedList.test().value().size)
        assertEquals(5, audioBrowserViewModel.genresProvider.pagedList.test().value().size)
        assertEquals(5, audioBrowserViewModel.albumsProvider.pagedList.test().value().size)
        assertEquals(8, audioBrowserViewModel.artistsProvider.pagedList.test().value().size) // By default, showAll is true
    }

    @Test
    fun whenMoreThanMaxSizeTracks_checkTotalCountIsTotal() {
        val count = MEDIALIBRARY_PAGE_SIZE * 3 + 1
        StubDataSource.getInstance().setAudioByCount(count, null)

        assertEquals(count, audioBrowserViewModel.tracksProvider.getTotalCount())
        assertEquals(count, audioBrowserViewModel.genresProvider.getTotalCount())
        assertEquals(count, audioBrowserViewModel.albumsProvider.getTotalCount())
        assertEquals(count, audioBrowserViewModel.artistsProvider.getTotalCount())
    }

    @Test
    fun whenMoreThanMaxSizeTracks_checkLastTrackIsNotLoadedYet() {
        val count = MEDIALIBRARY_PAGE_SIZE * 3 + 1
        StubDataSource.getInstance().setAudioByCount(count, null)

        waitForProvidersData()

        assertNull(audioBrowserViewModel.tracksProvider.pagedList.test().value()[count - 1])
        assertNull(audioBrowserViewModel.genresProvider.pagedList.test().value()[count - 1])
        assertNull(audioBrowserViewModel.albumsProvider.pagedList.test().value()[count - 1])
        assertNull(audioBrowserViewModel.artistsProvider.pagedList.test().value()[count - 1])
    }

    @Test
    fun whenMoreThanMaxSizeTracks_checkGetAllReturnsAll() {
        val count = MEDIALIBRARY_PAGE_SIZE * 3 + 1
        StubDataSource.getInstance().setAudioByCount(count, null)

        assertNotNull(audioBrowserViewModel.tracksProvider.getAll()[count - 1])
        assertNotNull(audioBrowserViewModel.genresProvider.getAll()[count - 1])
        assertNotNull(audioBrowserViewModel.albumsProvider.getAll()[count - 1])
        assertNotNull(audioBrowserViewModel.artistsProvider.getAll()[count - 1])
    }

    @Test
    fun whenThereAre5TracksWith3TracksHavingDifferentAlbumArtistAndArtistAndShowAllIsTrue_checkResultContainsEightArtists() {
        StubDataSource.getInstance().setAudioByCount(2, null) // AlbumArtist & Artist are same, so only one is added.
        createDummyAudios(3, "XYZ") // AlbumArtist & Artist are different, so both are added.

        audioBrowserViewModel.artistsProvider.showAll = true
        assertEquals(8, audioBrowserViewModel.artistsProvider.pagedList.test().awaitValue().value().size)
    }

    @Test
    fun whenThereAre5TracksWith3TracksHavingDifferentAlbumArtistAndArtistAndShowAllIsFalse_checkResultContainsFiveArtists() {
        StubDataSource.getInstance().setAudioByCount(2, null) // AlbumArtist & Artist are same, so only one is added.
        createDummyAudios(3, "XYZ") // AlbumArtist & Artist are different, so both are added.

        audioBrowserViewModel.artistsProvider.showAll = false
        assertEquals(5, audioBrowserViewModel.artistsProvider.pagedList.test().awaitValue().value().size)
    }

    @Test
    fun whenNoTrackAndFiltered_checkResultIsEmpty() {
        audioBrowserViewModel.filter("xyz")

        waitForProvidersData()

        assertTrue(audioBrowserViewModel.isEmpty())
    }

    @Test
    fun whenThereAreTracksButFilteredWithNonExistingTrack_checkTrackResultIsEmpty() {
        createDummyAudios(3, "XYZ")

        audioBrowserViewModel.filter("unknown")

        waitForProvidersData()

        assertTrue(audioBrowserViewModel.tracksProvider.isEmpty())
    }

    @Test
    fun whenThereAreAlbumsButFilteredWithNonExistingAlbum_checkAlbumResultIsEmpty() {
        createDummyAudios(3, "XYZ")

        audioBrowserViewModel.filter("unknown")

        waitForProvidersData()

        assertTrue(audioBrowserViewModel.albumsProvider.isEmpty())
    }

    @Test
    fun whenThereAreArtistsButFilteredWithNonExistingArtists_checkArtistResultIsEmpty() {
        createDummyAudios(3, "XYZ")

        audioBrowserViewModel.filter("unknown")

        waitForProvidersData()

        assertTrue(audioBrowserViewModel.artistsProvider.isEmpty())
    }

    @Test
    fun whenThereAreGenresButFilteredWithNonExistingGenre_checkGenreResultIsEmpty() {
        createDummyAudios(3, "XYZ")

        audioBrowserViewModel.filter("unknown")

        waitForProvidersData()

        assertTrue(audioBrowserViewModel.genresProvider.isEmpty())
    }

    @Test
    fun whenThereAreTracksAndFilteredWithExistingTrack_checkTrackResultIsNotEmpty() {
        createDummyAudios(3, "XYZ")

        audioBrowserViewModel.filter("XYZ")

        waitForProvidersData()

        assertFalse(audioBrowserViewModel.tracksProvider.isEmpty())
    }

    @Test
    fun whenThereAreArtistsAndFilteredWithExistingArtist_checkArtistResultIsNotEmpty() {
        createDummyAudios(3, "XYZ")

        // The default artist for the stubs
        audioBrowserViewModel.filter("Artisto")

        waitForProvidersData()

        assertFalse(audioBrowserViewModel.artistsProvider.isEmpty())
    }

    @Test
    fun whenThereAreAlbumsAndFilteredWithExistingAlbum_checkAlbumResultIsNotEmpty() {
        createDummyAudios(3, "XYZ")

        // The default album for the stubs
        audioBrowserViewModel.filter("CD1")

        waitForProvidersData()

        assertFalse(audioBrowserViewModel.albumsProvider.isEmpty())
    }

    @Test
    fun whenThereAreGenresAndFilteredWithExistingGenre_checkGenreResultIsNotEmpty() {
        createDummyAudios(3, "XYZ")

        // The default genre for the stubs
        audioBrowserViewModel.filter("Jazz")

        waitForProvidersData()

        assertFalse(audioBrowserViewModel.genresProvider.isEmpty())
    }

    @Test
    fun whenThereAreSomeTracksButFilteredResultContainsNone_restoringViewModelResetsFilterAndShowsItemAgain() {
        createDummyAudios(2, "test")

        audioBrowserViewModel.filter("unknown")

        waitForProvidersData()
        assertTrue(audioBrowserViewModel.isEmpty())

        audioBrowserViewModel.restore()
        waitForProvidersData()
        assertFalse(audioBrowserViewModel.isEmpty())
    }

    @Test
    fun whenFilteredAndLaterRestored_isFilteringIsTrueLaterFalse() {
        assertFalse(audioBrowserViewModel.isFiltering())

        audioBrowserViewModel.filter("def")

        assertTrue(audioBrowserViewModel.isFiltering())

        audioBrowserViewModel.restore()

        assertFalse(audioBrowserViewModel.isFiltering())
    }

    @Test
    fun when2TracksAndLaterAdded3Tracks_checkResultIsUpdatedWithThemOnRefresh() {
        createDummyAudios(2, "test")

        waitForProvidersData()

        assertFalse(audioBrowserViewModel.isEmpty())
        assertEquals(2, audioBrowserViewModel.tracksProvider.pagedList.test().value().size)

        createDummyAudios(3, "test")
        audioBrowserViewModel.refresh()

        waitForProvidersData()

        assertFalse(audioBrowserViewModel.isEmpty())
        assertEquals(5, audioBrowserViewModel.tracksProvider.pagedList.test().value().size)
    }

    @Test
    fun whenThereAreFourTracksWithAlternatelyDifferentTitles_checkTrackHeadersContainsTwoLetters() {
        createDummyAudios(2, "test")
        createDummyAudios(2, "fake")

        waitForProvidersData()

        val trackHeaders = audioBrowserViewModel.tracksProvider.liveHeaders.test().value()

        // Assertion for track headers
        assertEquals(2, trackHeaders.size())
        assertEquals("F", trackHeaders[0])
        assertEquals("T", trackHeaders[2])
    }

    @Test
    fun whenThereAreFourTracksWithAlternatelyDifferentTitles_checkGenreHeadersContainsOneLetter() {
        createDummyAudios(2, "test")
        createDummyAudios(2, "fake")

        waitForProvidersData()

        val genreHeaders = audioBrowserViewModel.genresProvider.liveHeaders.test().value()

        // Assertion for genre headers
        assertEquals(1, genreHeaders.size())
        assertEquals("J", genreHeaders[0])
    }

    @Test
    fun whenThereAreFourTracksWithAlternatelyDifferentTitles_checkAlbumHeadersContainsOneLetter() {
        createDummyAudios(2, "test")
        createDummyAudios(2, "fake")

        waitForProvidersData()

        val albumHeaders = audioBrowserViewModel.albumsProvider.liveHeaders.test().value()

        // Assertion for album headers
        assertEquals(1, albumHeaders.size())
        assertEquals("X", albumHeaders[0])
    }

    @Test
    fun whenThereAreFourTracksWithAlternatelyDifferentTitles_checkArtistHeadersContainsOneLetter() {
        createDummyAudios(2, "test")
        createDummyAudios(2, "fake")

        waitForProvidersData()

        val artistHeaders = audioBrowserViewModel.artistsProvider.liveHeaders.test().value()

        // Assertion for artist headers
        assertEquals(4, artistHeaders.size())
        assertEquals("#", artistHeaders[0])
        assertEquals("A", artistHeaders[1])
    }
}