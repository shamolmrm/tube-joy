package com.bahalabs.sortopia.ads

import android.app.Activity
import android.content.Context
import android.view.View
import com.bahalabs.sortopia.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

/**
 * AdMob behind Google's consent SDK (UMP): no ad is requested and the Mobile Ads SDK doesn't start
 * until consent allows it. Required for players in the EEA, UK and Switzerland. Full-screen ads
 * are preloaded and reloaded after every show.
 */
class Ads(private val context: Context, private val scope: CoroutineScope) {

    init {
        // Test traffic must never reach real ads: debug builds always carry Google's test ids.
        check(!BuildConfig.DEBUG || BuildConfig.ADMOB_BANNER.startsWith(TEST_PUBLISHER)) {
            "Debug builds must use Google's test ad units"
        }
    }

    private val consent: ConsentInformation = UserMessagingPlatform.getConsentInformation(context)
    private val started = AtomicBoolean(false)

    private val _ready = MutableStateFlow(false)
    /** True once ads may be requested (consent resolved and SDK started). */
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private val _privacyOptionsRequired = MutableStateFlow(false)
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    private var rewarded: RewardedAd? = null
    private var interstitial: InterstitialAd? = null
    private var lastFullScreenAt = 0L

    /** Call once per launch from the activity. Shows the consent form only where the law asks for it. */
    fun gatherConsent(activity: Activity) {
        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .build()
        consent.requestConsentInfoUpdate(
            activity,
            params,
            { UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { onConsentResolved() } },
            { onConsentResolved() }, // offline etc.: fall back to what was decided before
        )
        // A returning player who already answered can start right away.
        onConsentResolved()
    }

    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { onConsentResolved() }
    }

    private fun onConsentResolved() {
        _privacyOptionsRequired.value =
            consent.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        if (!consent.canRequestAds()) return
        if (!started.compareAndSet(false, true)) return
        // Initializing on the main thread can block it for seconds: Google recommends a background thread.
        scope.launch(Dispatchers.IO) {
            MobileAds.setRequestConfiguration(
                RequestConfiguration.Builder()
                    // A casual puzzle for a general audience: family-friendly ads only.
                    .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_PG)
                    .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE)
                    .build(),
            )
            MobileAds.initialize(context) {
                scope.launch(Dispatchers.Main) {
                    _ready.value = true
                    loadRewarded()
                    loadInterstitial()
                }
            }
        }
    }

    private fun loadRewarded() {
        RewardedAd.load(context, BuildConfig.ADMOB_REWARDED, AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
                rewarded = ad
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                rewarded = null
                retryLater { loadRewarded() }
            }
        })
    }

    private fun loadInterstitial() {
        InterstitialAd.load(context, BuildConfig.ADMOB_INTERSTITIAL, AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                interstitial = ad
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                interstitial = null
                retryLater { loadInterstitial() }
            }
        })
    }

    private fun retryLater(block: () -> Unit) {
        scope.launch(Dispatchers.Main) {
            kotlinx.coroutines.delay(RETRY_MS)
            block()
        }
    }

    /**
     * Shows a rewarded video. Returns true only when the SDK reports the reward was earned:
     * closing the video early grants nothing.
     */
    suspend fun showRewarded(activity: Activity): Boolean {
        val ad = rewarded ?: run {
            if (_ready.value) loadRewarded()
            return false
        }
        rewarded = null
        return suspendCancellableCoroutine { cont ->
            var earned = false
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    lastFullScreenAt = System.currentTimeMillis()
                    loadRewarded()
                    if (cont.isActive) cont.resume(earned)
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    loadRewarded()
                    if (cont.isActive) cont.resume(false)
                }
            }
            ad.show(activity) { earned = true }
        }
    }

    /**
     * Shows an interstitial between levels, never more often than every [MIN_GAP_MS] (a rewarded
     * video the player chose counts too), so the game never feels like an ad wall.
     */
    suspend fun showInterstitial(activity: Activity): Boolean {
        if (System.currentTimeMillis() - lastFullScreenAt < MIN_GAP_MS) return false
        val ad = interstitial ?: return false
        interstitial = null
        return suspendCancellableCoroutine { cont ->
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    lastFullScreenAt = System.currentTimeMillis()
                    loadInterstitial()
                    if (cont.isActive) cont.resume(true)
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    loadInterstitial()
                    if (cont.isActive) cont.resume(false)
                }
            }
            ad.show(activity)
        }
    }

    /** An adaptive banner sized to the screen width, or null before ads may load. */
    fun createBanner(context: Context, widthDp: Int): Banner? {
        if (!_ready.value) return null
        val size = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp)
        val view = AdView(context).apply {
            adUnitId = BuildConfig.ADMOB_BANNER
            setAdSize(size)
            loadAd(AdRequest.Builder().build())
        }
        return Banner(view, size.height)
    }

    class Banner(val view: View, val heightDp: Int) {
        fun destroy() = (view as AdView).destroy()
    }

    private companion object {
        const val TEST_PUBLISHER = "ca-app-pub-3940256099942544"
        const val MIN_GAP_MS = 60_000L
        const val RETRY_MS = 30_000L
    }
}
