@file:OptIn(ExperimentalMaterial3Api::class)

package com.quran.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ============================================================
// 1. نماذج الأذكار وأقسامها
// ============================================================
data class DhikrItem(
    val id: String,
    val text: String,
    val virtue: String,
    val targetCount: Int
)

data class AdhkarCategory(
    val tab: Int,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val items: List<DhikrItem>
)

/** أرقام الأقسام (ترتيبها مطابق لترتيب AdhkarCategories) */
object AdhkarTabs {
    const val MORNING = 0
    const val EVENING = 1
    const val SLEEP = 2
    const val WAKE = 3
    const val AFTER_PRAYER = 4
    const val HOME = 5
    const val MOSQUE = 6
    const val FOOD = 7
    const val DISTRESS = 8
    const val TRAVEL = 9
    const val DUAS = 10
}

private fun d(id: String, text: String, virtue: String, n: Int = 1) = DhikrItem(id, text, virtue, n)

private const val AYAT_KURSI =
    "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ۚ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ ۚ لَّهُ مَا فِي السَّمَاوَاتِ وَمَا فِي الْأَرْضِ ۗ مَن ذَا الَّذِي يَشْفَعُ عِندَهُ إِلَّا بِإِذْنِهِ ۚ يَعْلَمُ مَا بَيْنَ أَيْدِيهِمْ وَمَا خَلْفَهُمْ ۖ وَلَا يُحِيطُونَ بِشَيْءٍ مِّنْ عِلْمِهِ إِلَّا بِمَا شَاءَ ۚ وَسِعَ كُرْسِيُّهُ السَّمَاوَاتِ وَالْأَرْضَ ۖ وَلَا يَئُودُهُ حِفْظُهُمَا ۚ وَهُوَ الْعَلِيُّ الْعَظِيمُ"

private const val IKHLAS =
    "قُلْ هُوَ اللَّهُ أَحَدٌ ۝ اللَّهُ الصَّمَدُ ۝ لَمْ يَلِدْ وَلَمْ يُولَدْ ۝ وَلَمْ يَكُن لَّهُ كُفُوًا أَحَدٌ"

private const val FALAQ =
    "قُلْ أَعُوذُ بِرَبِّ الْفَلَقِ ۝ مِن شَرِّ مَا خَلَقَ ۝ وَمِن شَرِّ غَاسِقٍ إِذَا وَقَبَ ۝ وَمِن شَرِّ النَّفَّاثَاتِ فِي الْعُقَدِ ۝ وَمِن شَرِّ حَاسِدٍ إِذَا حَسَدَ"

private const val NAS =
    "قُلْ أَعُوذُ بِرَبِّ النَّاسِ ۝ مَلِكِ النَّاسِ ۝ إِلَٰهِ النَّاسِ ۝ مِن شَرِّ الْوَسْوَاسِ الْخَنَّاسِ ۝ الَّذِي يُوَسْوِسُ فِي صُدُورِ النَّاسِ ۝ مِنَ الْجِنَّةِ وَالنَّاسِ"

private const val TAHLIL_FULL =
    "لَا إِلَٰهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَىٰ كُلِّ شَيْءٍ قَدِيرٌ"

private fun sabahMasaa(morning: Boolean): List<DhikrItem> {
    val k = if (morning) "m" else "e"
    val time = if (morning) "الصباح" else "المساء"
    val list = mutableListOf<DhikrItem>()
    list += d("${k}_kursi", AYAT_KURSI, "آية الكرسي • مرة واحدة • حفظ وتحصين")
    list += d("${k}_ikhlas", IKHLAS, "سورة الإخلاص • ثلاث مرات • تكفيك من كل شيء", 3)
    list += d("${k}_falaq", FALAQ, "سورة الفلق • ثلاث مرات", 3)
    list += d("${k}_nas", NAS, "سورة الناس • ثلاث مرات", 3)
    list += if (morning) {
        d(
            "m_mulk",
            "أَصْبَحْنَا وَأَصْبَحَ الْمُلْكُ لِلَّهِ، وَالْحَمْدُ لِلَّهِ، لَا إِلَٰهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَىٰ كُلِّ شَيْءٍ قَدِيرٌ، رَبِّ أَسْأَلُكَ خَيْرَ مَا فِي هَٰذَا الْيَوْمِ وَخَيْرَ مَا بَعْدَهُ، وَأَعُوذُ بِكَ مِنْ شَرِّ مَا فِي هَٰذَا الْيَوْمِ وَشَرِّ مَا بَعْدَهُ، رَبِّ أَعُوذُ بِكَ مِنَ الْكَسَلِ وَسُوءِ الْكِبَرِ، رَبِّ أَعُوذُ بِكَ مِنْ عَذَابٍ فِي النَّارِ وَعَذَابٍ فِي الْقَبْرِ",
            "من أذكار الصباح الثابتة • مرة واحدة"
        )
    } else {
        d(
            "e_mulk",
            "أَمْسَيْنَا وَأَمْسَى الْمُلْكُ لِلَّهِ، وَالْحَمْدُ لِلَّهِ، لَا إِلَٰهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَىٰ كُلِّ شَيْءٍ قَدِيرٌ، رَبِّ أَسْأَلُكَ خَيْرَ مَا فِي هَٰذِهِ اللَّيْلَةِ وَخَيْرَ مَا بَعْدَهَا، وَأَعُوذُ بِكَ مِنْ شَرِّ مَا فِي هَٰذِهِ اللَّيْلَةِ وَشَرِّ مَا بَعْدَهَا، رَبِّ أَعُوذُ بِكَ مِنَ الْكَسَلِ وَسُوءِ الْكِبَرِ، رَبِّ أَعُوذُ بِكَ مِنْ عَذَابٍ فِي النَّارِ وَعَذَابٍ فِي الْقَبْرِ",
            "من أذكار المساء الثابتة • مرة واحدة"
        )
    }
    list += if (morning) {
        d("m_bika", "اللَّهُمَّ بِكَ أَصْبَحْنَا، وَبِكَ أَمْسَيْنَا، وَبِكَ نَحْيَا، وَبِكَ نَمُوتُ، وَإِلَيْكَ النُّشُورُ", "سنة نبوية عند الصباح • مرة واحدة")
    } else {
        d("e_bika", "اللَّهُمَّ بِكَ أَمْسَيْنَا، وَبِكَ أَصْبَحْنَا، وَبِكَ نَحْيَا، وَبِكَ نَمُوتُ، وَإِلَيْكَ الْمَصِيرُ", "سنة نبوية عند المساء • مرة واحدة")
    }
    list += d(
        "${k}_istighfar",
        "اللَّهُمَّ أَنْتَ رَبِّي لَا إِلَٰهَ إِلَّا أَنْتَ، خَلَقْتَنِي وَأَنَا عَبْدُكَ، وَأَنَا عَلَىٰ عَهْدِكَ وَوَعْدِكَ مَا اسْتَطَعْتُ، أَعُوذُ بِكَ مِنْ شَرِّ مَا صَنَعْتُ، أَبُوءُ لَكَ بِنِعْمَتِكَ عَلَيَّ، وَأَبُوءُ بِذَنْبِي فَاغْفِرْ لِي فَإِنَّهُ لَا يَغْفِرُ الذُّنُوبَ إِلَّا أَنْتَ",
        "سيد الاستغفار • مرة واحدة"
    )
    list += if (morning) {
        d("m_nima", "اللَّهُمَّ مَا أَصْبَحَ بِي مِنْ نِعْمَةٍ أَوْ بِأَحَدٍ مِنْ خَلْقِكَ فَمِنْكَ وَحْدَكَ لَا شَرِيكَ لَكَ، فَلَكَ الْحَمْدُ وَلَكَ الشُّكْرُ", "شكر النعمة • مرة واحدة")
    } else {
        d("e_nima", "اللَّهُمَّ مَا أَمْسَى بِي مِنْ نِعْمَةٍ أَوْ بِأَحَدٍ مِنْ خَلْقِكَ فَمِنْكَ وَحْدَكَ لَا شَرِيكَ لَكَ، فَلَكَ الْحَمْدُ وَلَكَ الشُّكْرُ", "شكر النعمة • مرة واحدة")
    }
    list += d(
        "${k}_ashhadu",
        if (morning) "اللَّهُمَّ إِنِّي أَصْبَحْتُ أُشْهِدُكَ، وَأُشْهِدُ حَمَلَةَ عَرْشِكَ، وَمَلَائِكَتَكَ، وَجَمِيعَ خَلْقِكَ، أَنَّكَ أَنْتَ اللَّهُ لَا إِلَٰهَ إِلَّا أَنْتَ وَحْدَكَ لَا شَرِيكَ لَكَ، وَأَنَّ مُحَمَّدًا عَبْدُكَ وَرَسُولُكَ"
        else "اللَّهُمَّ إِنِّي أَمْسَيْتُ أُشْهِدُكَ، وَأُشْهِدُ حَمَلَةَ عَرْشِكَ، وَمَلَائِكَتَكَ، وَجَمِيعَ خَلْقِكَ، أَنَّكَ أَنْتَ اللَّهُ لَا إِلَٰهَ إِلَّا أَنْتَ وَحْدَكَ لَا شَرِيكَ لَكَ، وَأَنَّ مُحَمَّدًا عَبْدُكَ وَرَسُولُكَ",
        "أربع مرات", 4
    )
    list += d(
        "${k}_fitra",
        if (morning) "أَصْبَحْنَا عَلَىٰ فِطْرَةِ الْإِسْلَامِ، وَعَلَىٰ كَلِمَةِ الْإِخْلَاصِ، وَعَلَىٰ دِينِ نَبِيِّنَا مُحَمَّدٍ ﷺ، وَعَلَىٰ مِلَّةِ أَبِينَا إِبْرَاهِيمَ حَنِيفًا مُسْلِمًا وَمَا كَانَ مِنَ الْمُشْرِكِينَ"
        else "أَمْسَيْنَا عَلَىٰ فِطْرَةِ الْإِسْلَامِ، وَعَلَىٰ كَلِمَةِ الْإِخْلَاصِ، وَعَلَىٰ دِينِ نَبِيِّنَا مُحَمَّدٍ ﷺ، وَعَلَىٰ مِلَّةِ أَبِينَا إِبْرَاهِيمَ حَنِيفًا مُسْلِمًا وَمَا كَانَ مِنَ الْمُشْرِكِينَ",
        "مرة واحدة"
    )
    list += d("${k}_rida", "رَضِيتُ بِاللَّهِ رَبًّا، وَبِالْإِسْلَامِ دِينًا، وَبِمُحَمَّدٍ ﷺ نَبِيًّا", "ثلاث مرات", 3)
    list += d("${k}_afini", "اللَّهُمَّ عَافِنِي فِي بَدَنِي، اللَّهُمَّ عَافِنِي فِي سَمْعِي، اللَّهُمَّ عَافِنِي فِي بَصَرِي، لَا إِلَٰهَ إِلَّا أَنْتَ", "ثلاث مرات", 3)
    list += d(
        "${k}_afw",
        "اللَّهُمَّ إِنِّي أَسْأَلُكَ الْعَفْوَ وَالْعَافِيَةَ فِي دِينِي وَدُنْيَايَ وَأَهْلِي وَمَالِي، اللَّهُمَّ اسْتُرْ عَوْرَاتِي وَآمِنْ رَوْعَاتِي، اللَّهُمَّ احْفَظْنِي مِنْ بَيْنِ يَدَيَّ وَمِنْ خَلْفِي وَعَنْ يَمِينِي وَعَنْ شِمَالِي وَمِنْ فَوْقِي، وَأَعُوذُ بِعَظَمَتِكَ أَنْ أُغْتَالَ مِنْ تَحْتِي",
        "دعاء العافية والحفظ • مرة واحدة"
    )
    list += d("${k}_ghayb", "اللَّهُمَّ عَالِمَ الْغَيْبِ وَالشَّهَادَةِ فَاطِرَ السَّمَاوَاتِ وَالْأَرْضِ، رَبَّ كُلِّ شَيْءٍ وَمَلِيكَهُ، أَشْهَدُ أَنْ لَا إِلَٰهَ إِلَّا أَنْتَ، أَعُوذُ بِكَ مِنْ شَرِّ نَفْسِي وَمِنْ شَرِّ الشَّيْطَانِ وَشِرْكِهِ، وَأَنْ أَقْتَرِفَ عَلَىٰ نَفْسِي سُوءًا أَوْ أَجُرَّهُ إِلَىٰ مُسْلِمٍ", "مرة واحدة")
    list += d("${k}_yahayy", "يَا حَيُّ يَا قَيُّومُ بِرَحْمَتِكَ أَسْتَغِيثُ، أَصْلِحْ لِي شَأْنِي كُلَّهُ، وَلَا تَكِلْنِي إِلَىٰ نَفْسِي طَرْفَةَ عَيْنٍ", "مرة واحدة")
    list += d("${k}_bismillah", "بِسْمِ اللَّهِ الَّذِي لَا يَضُرُّ مَعَ اسْمِهِ شَيْءٌ فِي الْأَرْضِ وَلَا فِي السَّمَاءِ وَهُوَ السَّمِيعُ الْعَلِيمُ", "ثلاث مرات • لم يضره شيء بإذن الله", 3)
    list += d("${k}_hasbi", "حَسْبِيَ اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ عَلَيْهِ تَوَكَّلْتُ وَهُوَ رَبُّ الْعَرْشِ الْعَظِيمِ", "سبع مرات", 7)
    if (!morning) {
        list += d("e_kalimat", "أَعُوذُ بِكَلِمَاتِ اللَّهِ التَّامَّاتِ مِنْ شَرِّ مَا خَلَقَ", "ثلاث مرات مساءً للحفظ والتحصين", 3)
    } else {
        list += d("m_adad", "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ: عَدَدَ خَلْقِهِ، وَرِضَا نَفْسِهِ، وَزِنَةَ عَرْشِهِ، وَمِدَادَ كَلِمَاتِهِ", "ثلاث مرات صباحاً", 3)
    }
    list += d("${k}_tasbih100", "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ", "مائة مرة • تُحَطّ بها الخطايا", 100)
    list += d("${k}_tahlil", TAHLIL_FULL, "عشر مرات", 10)
    list += d("${k}_salat", "اللَّهُمَّ صَلِّ وَسَلِّمْ عَلَىٰ نَبِيِّنَا مُحَمَّدٍ", "عشر مرات • الصلاة على النبي ﷺ", 10)
    list += d("${k}_istaghfir", "أَسْتَغْفِرُ اللَّهَ وَأَتُوبُ إِلَيْهِ", "الإكثار منه في $time • مائة مرة", 100)
    return list
}

val AdhkarCategories: List<AdhkarCategory> by lazy {
    listOf(
        AdhkarCategory(
            AdhkarTabs.MORNING, "أذكار الصباح", "من الفجر حتى الضحى", Icons.Default.WbSunny,
            sabahMasaa(true)
        ),
        AdhkarCategory(
            AdhkarTabs.EVENING, "أذكار المساء", "من العصر حتى العشاء", Icons.Default.NightsStay,
            sabahMasaa(false)
        ),
        AdhkarCategory(
            AdhkarTabs.SLEEP, "أذكار النوم", "قبل أن تنام", Icons.Default.Bedtime,
            listOf(
                d("s_kursi", AYAT_KURSI, "آية الكرسي • مرة واحدة • حافظ من الله حتى الصباح"),
                d("s_ikhlas", IKHLAS, "تُقرأ مع المعوذتين: يجمع كفيه وينفث فيهما ويمسح بهما جسده • ثلاث مرات", 3),
                d("s_falaq", FALAQ, "ثلاث مرات", 3),
                d("s_nas", NAS, "ثلاث مرات", 3),
                d(
                    "s_baqara",
                    "آمَنَ الرَّسُولُ بِمَا أُنزِلَ إِلَيْهِ مِن رَّبِّهِ وَالْمُؤْمِنُونَ ۚ كُلٌّ آمَنَ بِاللَّهِ وَمَلَائِكَتِهِ وَكُتُبِهِ وَرُسُلِهِ لَا نُفَرِّقُ بَيْنَ أَحَدٍ مِّن رُّسُلِهِ ۚ وَقَالُوا سَمِعْنَا وَأَطَعْنَا ۖ غُفْرَانَكَ رَبَّنَا وَإِلَيْكَ الْمَصِيرُ ۝ لَا يُكَلِّفُ اللَّهُ نَفْسًا إِلَّا وُسْعَهَا ۚ لَهَا مَا كَسَبَتْ وَعَلَيْهَا مَا اكْتَسَبَتْ ۗ رَبَّنَا لَا تُؤَاخِذْنَا إِن نَّسِينَا أَوْ أَخْطَأْنَا ۚ رَبَّنَا وَلَا تَحْمِلْ عَلَيْنَا إِصْرًا كَمَا حَمَلْتَهُ عَلَى الَّذِينَ مِن قَبْلِنَا ۚ رَبَّنَا وَلَا تُحَمِّلْنَا مَا لَا طَاقَةَ لَنَا بِهِ ۖ وَاعْفُ عَنَّا وَاغْفِرْ لَنَا وَارْحَمْنَا ۚ أَنتَ مَوْلَانَا فَانصُرْنَا عَلَى الْقَوْمِ الْكَافِرِينَ",
                    "خواتيم سورة البقرة • مرة واحدة • من قرأهما في ليلة كفتاه"
                ),
                d("s_bismika", "بِاسْمِكَ رَبِّي وَضَعْتُ جَنْبِي، وَبِكَ أَرْفَعُهُ، فَإِنْ أَمْسَكْتَ نَفْسِي فَارْحَمْهَا، وَإِنْ أَرْسَلْتَهَا فَاحْفَظْهَا بِمَا تَحْفَظُ بِهِ عِبَادَكَ الصَّالِحِينَ", "مرة واحدة"),
                d("s_khalaqta", "اللَّهُمَّ إِنَّكَ خَلَقْتَ نَفْسِي وَأَنْتَ تَوَفَّاهَا، لَكَ مَمَاتُهَا وَمَحْيَاهَا، إِنْ أَحْيَيْتَهَا فَاحْفَظْهَا، وَإِنْ أَمَتَّهَا فَاغْفِرْ لَهَا، اللَّهُمَّ إِنِّي أَسْأَلُكَ الْعَافِيَةَ", "مرة واحدة"),
                d("s_aslamtu", "اللَّهُمَّ أَسْلَمْتُ نَفْسِي إِلَيْكَ، وَفَوَّضْتُ أَمْرِي إِلَيْكَ، وَوَجَّهْتُ وَجْهِي إِلَيْكَ، وَأَلْجَأْتُ ظَهْرِي إِلَيْكَ، رَغْبَةً وَرَهْبَةً إِلَيْكَ، لَا مَلْجَأَ وَلَا مَنْجَا مِنْكَ إِلَّا إِلَيْكَ، آمَنْتُ بِكِتَابِكَ الَّذِي أَنْزَلْتَ، وَبِنَبِيِّكَ الَّذِي أَرْسَلْتَ", "ليكن آخر ما تقول • مرة واحدة"),
                d("s_qini", "اللَّهُمَّ قِنِي عَذَابَكَ يَوْمَ تَبْعَثُ عِبَادَكَ", "ثلاث مرات", 3),
                d("s_tasbih", "سُبْحَانَ اللَّهِ", "تسبيح فاطمة رضي الله عنها • ثلاث وثلاثون مرة", 33),
                d("s_hamd", "الْحَمْدُ لِلَّهِ", "ثلاث وثلاثون مرة", 33),
                d("s_takbir", "اللَّهُ أَكْبَرُ", "أربع وثلاثون مرة", 34),
                d("s_bismika2", "بِاسْمِكَ اللَّهُمَّ أَمُوتُ وَأَحْيَا", "مرة واحدة")
            )
        ),
        AdhkarCategory(
            AdhkarTabs.WAKE, "الاستيقاظ", "عند الاستيقاظ من النوم", Icons.Default.AlarmOn,
            listOf(
                d("w_ahyana", "الْحَمْدُ لِلَّهِ الَّذِي أَحْيَانَا بَعْدَ مَا أَمَاتَنَا وَإِلَيْهِ النُّشُورُ", "عند الاستيقاظ • مرة واحدة"),
                d("w_afani", "الْحَمْدُ لِلَّهِ الَّذِي عَافَانِي فِي جَسَدِي، وَرَدَّ عَلَيَّ رُوحِي، وَأَذِنَ لِي بِذِكْرِهِ", "مرة واحدة"),
                d(
                    "w_tahlil",
                    "لَا إِلَٰهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَىٰ كُلِّ شَيْءٍ قَدِيرٌ، الْحَمْدُ لِلَّهِ، وَسُبْحَانَ اللَّهِ، وَلَا إِلَٰهَ إِلَّا اللَّهُ، وَاللَّهُ أَكْبَرُ، وَلَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ الْعَلِيِّ الْعَظِيمِ، رَبِّ اغْفِرْ لِي",
                    "مرة واحدة"
                )
            )
        ),
        AdhkarCategory(
            AdhkarTabs.AFTER_PRAYER, "بعد الصلاة", "أذكار دبر الصلوات المكتوبة", Icons.Default.Mosque,
            listOf(
                d("p_istighfar", "أَسْتَغْفِرُ اللَّهَ", "ثلاث مرات بعد السلام", 3),
                d("p_salam", "اللَّهُمَّ أَنْتَ السَّلَامُ وَمِنْكَ السَّلَامُ، تَبَارَكْتَ يَا ذَا الْجَلَالِ وَالْإِكْرَامِ", "مرة واحدة"),
                d("p_la_mani", "لَا إِلَٰهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَىٰ كُلِّ شَيْءٍ قَدِيرٌ، اللَّهُمَّ لَا مَانِعَ لِمَا أَعْطَيْتَ، وَلَا مُعْطِيَ لِمَا مَنَعْتَ، وَلَا يَنْفَعُ ذَا الْجَدِّ مِنْكَ الْجَدُّ", "مرة واحدة"),
                d("p_tasbih", "سُبْحَانَ اللَّهِ", "ثلاث وثلاثون مرة", 33),
                d("p_hamd", "الْحَمْدُ لِلَّهِ", "ثلاث وثلاثون مرة", 33),
                d("p_takbir", "اللَّهُ أَكْبَرُ", "ثلاث وثلاثون مرة", 33),
                d("p_tamam", TAHLIL_FULL, "تمام المائة • مرة واحدة"),
                d("p_kursi", AYAT_KURSI, "آية الكرسي • مرة بعد كل صلاة"),
                d("p_muawwidhat", "$IKHLAS\n\n$FALAQ\n\n$NAS", "الإخلاص والمعوذتان • مرة بعد كل صلاة، وثلاثاً بعد الفجر والمغرب"),
                d("p_ianni", "اللَّهُمَّ أَعِنِّي عَلَىٰ ذِكْرِكَ وَشُكْرِكَ وَحُسْنِ عِبَادَتِكَ", "مرة واحدة")
            )
        ),
        AdhkarCategory(
            AdhkarTabs.HOME, "المنزل واللباس", "الدخول والخروج وقضاء الحاجة", Icons.Default.Home,
            listOf(
                d("h_kharaja", "بِسْمِ اللَّهِ، تَوَكَّلْتُ عَلَى اللَّهِ، وَلَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ", "عند الخروج من المنزل"),
                d("h_adilla", "اللَّهُمَّ إِنِّي أَعُوذُ بِكَ أَنْ أَضِلَّ أَوْ أُضَلَّ، أَوْ أَزِلَّ أَوْ أُزَلَّ، أَوْ أَظْلِمَ أَوْ أُظْلَمَ، أَوْ أَجْهَلَ أَوْ يُجْهَلَ عَلَيَّ", "عند الخروج من المنزل"),
                d("h_walaj", "بِسْمِ اللَّهِ وَلَجْنَا، وَبِسْمِ اللَّهِ خَرَجْنَا، وَعَلَى اللَّهِ رَبِّنَا تَوَكَّلْنَا", "عند دخول المنزل، ثم سلّم على أهلك"),
                d("h_khala", "بِسْمِ اللَّهِ، اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنَ الْخُبُثِ وَالْخَبَائِثِ", "عند دخول الخلاء"),
                d("h_ghufran", "غُفْرَانَكَ", "عند الخروج من الخلاء"),
                d("h_thawb", "الْحَمْدُ لِلَّهِ الَّذِي كَسَانِي هَٰذَا وَرَزَقَنِيهِ مِنْ غَيْرِ حَوْلٍ مِنِّي وَلَا قُوَّةٍ", "عند لبس الثوب")
            )
        ),
        AdhkarCategory(
            AdhkarTabs.MOSQUE, "الوضوء والمسجد", "الوضوء والأذان والمسجد", Icons.Default.WaterDrop,
            listOf(
                d("o_wudu", "أَشْهَدُ أَنْ لَا إِلَٰهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، وَأَشْهَدُ أَنَّ مُحَمَّدًا عَبْدُهُ وَرَسُولُهُ", "بعد الوضوء"),
                d("o_tawwabin", "اللَّهُمَّ اجْعَلْنِي مِنَ التَّوَّابِينَ وَاجْعَلْنِي مِنَ الْمُتَطَهِّرِينَ", "بعد الوضوء"),
                d("o_adhan", "اللَّهُمَّ رَبَّ هَٰذِهِ الدَّعْوَةِ التَّامَّةِ، وَالصَّلَاةِ الْقَائِمَةِ، آتِ مُحَمَّدًا الْوَسِيلَةَ وَالْفَضِيلَةَ، وَابْعَثْهُ مَقَامًا مَحْمُودًا الَّذِي وَعَدْتَهُ", "بعد سماع الأذان"),
                d("o_dukhul", "اللَّهُمَّ افْتَحْ لِي أَبْوَابَ رَحْمَتِكَ", "عند دخول المسجد، بتقديم الرِّجل اليمنى"),
                d("o_khuruj", "اللَّهُمَّ إِنِّي أَسْأَلُكَ مِنْ فَضْلِكَ", "عند الخروج من المسجد، بتقديم الرِّجل اليسرى")
            )
        ),
        AdhkarCategory(
            AdhkarTabs.FOOD, "الطعام والشراب", "قبل الأكل وبعده", Icons.Default.Restaurant,
            listOf(
                d("f_bismillah", "بِسْمِ اللَّهِ", "قبل الطعام"),
                d("f_awwalihi", "بِسْمِ اللَّهِ فِي أَوَّلِهِ وَآخِرِهِ", "إذا نسيت التسمية في أول الطعام"),
                d("f_hamd", "الْحَمْدُ لِلَّهِ الَّذِي أَطْعَمَنِي هَٰذَا وَرَزَقَنِيهِ مِنْ غَيْرِ حَوْلٍ مِنِّي وَلَا قُوَّةٍ", "بعد الطعام"),
                d("f_mudif", "اللَّهُمَّ بَارِكْ لَهُمْ فِيمَا رَزَقْتَهُمْ، وَاغْفِرْ لَهُمْ وَارْحَمْهُمْ", "دعاء للمضيف"),
                d("f_iftar", "ذَهَبَ الظَّمَأُ وَابْتَلَّتِ الْعُرُوقُ وَثَبَتَ الْأَجْرُ إِنْ شَاءَ اللَّهُ", "عند الإفطار من الصيام")
            )
        ),
        AdhkarCategory(
            AdhkarTabs.DISTRESS, "الهم والكرب", "عند الضيق والحزن والقلق", Icons.Default.Favorite,
            listOf(
                d("k_karb", "لَا إِلَٰهَ إِلَّا اللَّهُ الْعَظِيمُ الْحَلِيمُ، لَا إِلَٰهَ إِلَّا اللَّهُ رَبُّ الْعَرْشِ الْعَظِيمِ، لَا إِلَٰهَ إِلَّا اللَّهُ رَبُّ السَّمَاوَاتِ وَرَبُّ الْأَرْضِ وَرَبُّ الْعَرْشِ الْكَرِيمِ", "دعاء الكرب"),
                d("k_abduka", "اللَّهُمَّ إِنِّي عَبْدُكَ، ابْنُ عَبْدِكَ، ابْنُ أَمَتِكَ، نَاصِيَتِي بِيَدِكَ، مَاضٍ فِيَّ حُكْمُكَ، عَدْلٌ فِيَّ قَضَاؤُكَ، أَسْأَلُكَ بِكُلِّ اسْمٍ هُوَ لَكَ سَمَّيْتَ بِهِ نَفْسَكَ، أَوْ أَنْزَلْتَهُ فِي كِتَابِكَ، أَوْ عَلَّمْتَهُ أَحَدًا مِنْ خَلْقِكَ، أَوِ اسْتَأْثَرْتَ بِهِ فِي عِلْمِ الْغَيْبِ عِنْدَكَ، أَنْ تَجْعَلَ الْقُرْآنَ رَبِيعَ قَلْبِي، وَنُورَ صَدْرِي، وَجَلَاءَ حُزْنِي، وَذَهَابَ هَمِّي", "دعاء الهم والحزن"),
                d("k_hamm", "اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنَ الْهَمِّ وَالْحَزَنِ، وَالْعَجْزِ وَالْكَسَلِ، وَالْبُخْلِ وَالْجُبْنِ، وَضَلَعِ الدَّيْنِ وَغَلَبَةِ الرِّجَالِ", "مرة واحدة"),
                d("k_yunus", "لَا إِلَٰهَ إِلَّا أَنْتَ سُبْحَانَكَ إِنِّي كُنتُ مِنَ الظَّالِمِينَ", "دعوة ذي النون عليه السلام", 3),
                d("k_hasbuna", "حَسْبُنَا اللَّهُ وَنِعْمَ الْوَكِيلُ", "عند الشدة والخوف", 7),
                d("k_hawqala", "لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ", "كنز من كنوز الجنة", 10)
            )
        ),
        AdhkarCategory(
            AdhkarTabs.TRAVEL, "السفر والتنقل", "الركوب والسفر والوداع", Icons.Default.Flight,
            listOf(
                d(
                    "t_safar",
                    "اللَّهُ أَكْبَرُ، اللَّهُ أَكْبَرُ، اللَّهُ أَكْبَرُ، سُبْحَانَ الَّذِي سَخَّرَ لَنَا هَٰذَا وَمَا كُنَّا لَهُ مُقْرِنِينَ، وَإِنَّا إِلَىٰ رَبِّنَا لَمُنقَلِبُونَ، اللَّهُمَّ إِنَّا نَسْأَلُكَ فِي سَفَرِنَا هَٰذَا الْبِرَّ وَالتَّقْوَىٰ، وَمِنَ الْعَمَلِ مَا تَرْضَىٰ، اللَّهُمَّ هَوِّنْ عَلَيْنَا سَفَرَنَا هَٰذَا وَاطْوِ عَنَّا بُعْدَهُ، اللَّهُمَّ أَنْتَ الصَّاحِبُ فِي السَّفَرِ، وَالْخَلِيفَةُ فِي الْأَهْلِ، اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنْ وَعْثَاءِ السَّفَرِ، وَكَآبَةِ الْمَنْظَرِ، وَسُوءِ الْمُنْقَلَبِ فِي الْمَالِ وَالْأَهْلِ",
                    "عند ركوب السيارة أو الطائرة أو بدء السفر"
                ),
                d("t_wada", "أَسْتَوْدِعُ اللَّهَ دِينَكَ وَأَمَانَتَكَ وَخَوَاتِيمَ عَمَلِكَ", "دعاء لمن يودّع مسافراً"),
                d("t_matar", "اللَّهُمَّ صَيِّبًا نَافِعًا", "عند نزول المطر"),
                d("t_ghadab", "أَعُوذُ بِاللَّهِ مِنَ الشَّيْطَانِ الرَّجِيمِ", "عند الغضب")
            )
        ),
        AdhkarCategory(
            AdhkarTabs.DUAS, "أدعية جامعة", "من القرآن الكريم والسنة", Icons.Default.AutoAwesome,
            listOf(
                d("d_hasana", "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ", "من جوامع الدعاء • سورة البقرة"),
                d("d_tazigh", "رَبَّنَا لَا تُزِغْ قُلُوبَنَا بَعْدَ إِذْ هَدَيْتَنَا وَهَبْ لَنَا مِن لَّدُنكَ رَحْمَةً ۚ إِنَّكَ أَنتَ الْوَهَّابُ", "سورة آل عمران"),
                d("d_sadri", "رَبِّ اشْرَحْ لِي صَدْرِي ۝ وَيَسِّرْ لِي أَمْرِي", "سورة طه"),
                d("d_zalamna", "رَبَّنَا ظَلَمْنَا أَنفُسَنَا وَإِن لَّمْ تَغْفِرْ لَنَا وَتَرْحَمْنَا لَنَكُونَنَّ مِنَ الْخَاسِرِينَ", "سورة الأعراف"),
                d("d_walidayya", "رَبِّ ارْحَمْهُمَا كَمَا رَبَّيَانِي صَغِيرًا", "دعاء للوالدين • سورة الإسراء"),
                d("d_ibrahim", "رَبِّ اجْعَلْنِي مُقِيمَ الصَّلَاةِ وَمِن ذُرِّيَّتِي ۚ رَبَّنَا وَتَقَبَّلْ دُعَاءِ ۝ رَبَّنَا اغْفِرْ لِي وَلِوَالِدَيَّ وَلِلْمُؤْمِنِينَ يَوْمَ يَقُومُ الْحِسَابُ", "سورة إبراهيم"),
                d("d_muqallib", "اللَّهُمَّ يَا مُقَلِّبَ الْقُلُوبِ ثَبِّتْ قَلْبِي عَلَىٰ دِينِكَ", "دعاء الثبات"),
                d("d_huda", "اللَّهُمَّ إِنِّي أَسْأَلُكَ الْهُدَىٰ وَالتُّقَىٰ وَالْعَفَافَ وَالْغِنَىٰ", "من جوامع الدعاء"),
                d("d_kfini", "اللَّهُمَّ اكْفِنِي بِحَلَالِكَ عَنْ حَرَامِكَ، وَأَغْنِنِي بِفَضْلِكَ عَمَّنْ سِوَاكَ", "دعاء قضاء الدَّين والرزق")
            )
        )
    )
}

/** (عدد الأذكار المكتملة اليوم، إجمالي أذكار القسم) */
fun adhkarCategoryProgress(st: AppState, tab: Int): Pair<Int, Int> {
    val items = AdhkarCategories.getOrNull(tab)?.items ?: return 0 to 0
    val done = items.count { st.adhkarRemaining(it.id, it.targetCount) == 0 }
    return done to items.size
}

// ============================================================
// 2. شاشة الأذكار التفاعلية المتطورة (AdhkarScreen)
// ============================================================
@Composable
fun AdhkarScreen(nav: Nav) {
    val st = LocalApp.current
    val p = LocalPalette.current
    val fx = LocalFx.current
    val ctx = LocalContext.current

    val cats = AdhkarCategories
    val selected = nav.adhkarTab.coerceIn(0, cats.lastIndex)
    val cat = cats[selected]

    val listState = rememberLazyListState()
    val chipsState = rememberLazyListState()
    LaunchedEffect(selected) {
        chipsState.animateScrollToItem(selected)
        listState.scrollToItem(0)
    }

    val (done, total) = adhkarCategoryProgress(st, selected)
    val progressAnim by animateFloatAsState(
        targetValue = if (total > 0) done.toFloat() / total else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        label = "adhkarProgress"
    )
    val allDone = total > 0 && done == total

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(p.bg)
    ) {
        ScreenHeader(
            title = "الأذكار",
            subtitle = "${cat.title} • ${cat.subtitle}",
            onBack = { nav.back() },
            trailing = {
                IconButton(onClick = { fx.click(); st.adhkarFontScale -= 0.1f }) {
                    Icon(Icons.Default.TextDecrease, "تصغير الخط", tint = GoldSoftBg)
                }
                IconButton(onClick = { fx.click(); st.adhkarFontScale += 0.1f }) {
                    Icon(Icons.Default.TextIncrease, "تكبير الخط", tint = GoldSoftBg)
                }
                IconButton(onClick = { fx.click(); nav.push(Screen.Tasbih) }) {
                    Icon(Icons.Default.Spa, "المسبحة", tint = GoldShine)
                }
            }
        )

        // اختيار القسم (مرر يميناً ويساراً لرؤية كل الأقسام)
        LazyRow(
            state = chipsState,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(cats, key = { _, c -> c.tab }) { index, c ->
                val isSel = index == selected
                val (cd, ct) = adhkarCategoryProgress(st, index)
                val complete = ct > 0 && cd == ct
                val bg by animateColorAsState(if (isSel) EmeraldVibrant else p.card, label = "chipBg")
                Surface(
                    onClick = { fx.click(); nav.adhkarTab = index },
                    shape = RoundedCornerShape(16.dp),
                    color = bg,
                    border = BorderStroke(1.3.dp, if (isSel) GoldShine else p.border),
                    shadowElevation = if (isSel) 4.dp else 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (complete) Icons.Default.CheckCircle else c.icon,
                            null,
                            tint = if (isSel) GoldShine else if (complete) EmeraldVibrant else GoldWarm,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            c.title,
                            color = if (isSel) Color.White else p.ink,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // شريط التقدم + إعادة ضبط القسم
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = p.card,
                border = BorderStroke(1.dp, GoldWarm.copy(alpha = 0.45f)),
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("إنجاز ${cat.title}", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = p.ink)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("$done / $total", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = EmeraldVibrant)
                            IconButton(
                                onClick = {
                                    fx.click()
                                    st.adhkarReset(cat.items)
                                    Toast.makeText(ctx, "تمت إعادة ضبط عدادات القسم ✓", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Refresh, "إعادة ضبط القسم", tint = GoldWarm, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progressAnim },
                        color = EmeraldVibrant,
                        trackColor = p.border.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(CircleShape)
                    )
                    if (allDone) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "أتممت ${cat.title} • تقبّل الله منك ✓",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.5.sp,
                            color = EmeraldVibrant
                        )
                    } else {
                        Spacer(Modifier.height(6.dp))
                        Text("اضغط على البطاقة لتنقص العدّاد بعد كل مرة", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = p.muted)
                    }
                }
            }

            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                itemsIndexed(cat.items, key = { _, item -> item.id }) { index, item ->
                    val left = st.adhkarRemaining(item.id, item.targetCount)
                    val isDone = left == 0

                    val cardBg by animateColorAsState(
                        targetValue = if (isDone) EmeraldVibrant.copy(alpha = 0.13f) else p.card,
                        label = "dhikrCardBg"
                    )
                    val borderClr by animateColorAsState(
                        targetValue = if (isDone) EmeraldVibrant else GoldWarm.copy(alpha = 0.55f),
                        label = "dhikrCardBorder"
                    )

                    Surface(
                        onClick = {
                            if (left > 0) {
                                fx.click()
                                fx.vibrate(28L)
                                st.totalTasbih += 1
                                val nextVal = st.adhkarTick(item.id, item.targetCount)
                                if (nextVal == 0 && st.stageVib) fx.vibrate(110L)
                            }
                        },
                        shape = RoundedCornerShape(22.dp),
                        color = cardBg,
                        border = BorderStroke(1.3.dp, borderClr),
                        shadowElevation = 3.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                NumberBadge(index + 1, 32)

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isDone) EmeraldVibrant else GoldWarm,
                                    border = BorderStroke(1.dp, GoldShine.copy(alpha = 0.6f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.TouchApp,
                                            contentDescription = null,
                                            tint = if (isDone) GoldShine else RoyalForest,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = if (isDone) "تم ✓" else if (item.targetCount > 1) "المتبقي: $left من ${item.targetCount}" else "اضغط عند الانتهاء",
                                            color = if (isDone) Color.White else RoyalForest,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }

                            // نص الذكر بالخط العثماني العريض
                            Text(
                                text = item.text,
                                fontFamily = UthmanicFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = (22 * st.adhkarFontScale).sp,
                                lineHeight = (40 * st.adhkarFontScale).sp,
                                color = p.ink,
                                textAlign = TextAlign.Justify,
                                modifier = Modifier.fillMaxWidth()
                            )

                            HorizontalDivider(color = p.border.copy(alpha = 0.45f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.virtue,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = p.muted,
                                    modifier = Modifier.weight(1f)
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    IconButton(
                                        onClick = {
                                            fx.click()
                                            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            cm.setPrimaryClip(ClipData.newPlainText("ذكر", item.text))
                                            Toast.makeText(ctx, "تم نسخ الذكر ✓", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(EmeraldVibrant.copy(alpha = 0.1f))
                                    ) {
                                        Icon(Icons.Default.ContentCopy, "نسخ", tint = EmeraldVibrant, modifier = Modifier.size(16.dp))
                                    }

                                    IconButton(
                                        onClick = { fx.click(); shareText(ctx, item.text) },
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(GoldWarm.copy(alpha = 0.14f))
                                    ) {
                                        Icon(Icons.Default.Share, "مشاركة", tint = GoldWarm, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
