package com.quran.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import java.util.concurrent.TimeUnit

val UthmanicFontFamily = FontFamily(
    Font(R.font.uthmanic, FontWeight.Normal),
    Font(R.font.uthmanic, FontWeight.Bold)
)

data class DailyVerseItem(
    val id: Int,
    val text: String,
    val surah: String,
    val ayahNumber: Int,
    val page: Int,
    val reflection: String
)

object DailyVerseRepository {

    val verses: List<DailyVerseItem> = listOf(
        DailyVerseItem(1, "﴿بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ ۝ الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ﴾", "الفاتحة", 2, 1, "افتتح يومك بحمد الله وشكره على نعمه الظاهرة والباطنة."),
        DailyVerseItem(2, "﴿وَمَن يَتَّقِ اللَّهَ يَجْعَل لَّهُ مَخْرَجًا ۝ وَيَرْزُقْهُ مِنْ حَيْثُ لَا يَحْتَسِبُ﴾", "الطلاق", 2, 558, "التقوى مفتاح الفرج والرزق من أبواب لم تكن في الحسبان."),
        DailyVerseItem(3, "﴿فَاذْكُرُونِي أَذْكُرْكُمْ وَاشْكُرُوا لِي وَلَا تَكْفُرُونِ﴾", "البقرة", 152, 23, "أعظم شرف للعبد أن يذكره الله في الملأ الأعلى."),
        DailyVerseItem(4, "﴿فَإِنَّ مَعَ الْعُسْرِ يُسْرًا ۝ إِنَّ مَعَ الْعُسْرِ يُسْرًا﴾", "الشرح", 5, 596, "لن يغلب عسرٌ يسرين؛ الفرج قريب مع كل ضيق."),
        DailyVerseItem(5, "﴿وَإِذَا سَأَلَكَ عِبَادِي عَنِّي فَإِنِّي قَرِيبٌ ۖ أُجِيبُ دَعْوَةَ الدَّاعِ إِذَا دَعَانِ﴾", "البقرة", 186, 29, "ارفع حاجتك إلى الله بيقين، فهو أقرب إليك من حبل الوريد."),
        DailyVerseItem(6, "﴿لَا يُكَلِّفُ اللَّهُ نَفْسًا إِلَّا وُسْعَهَا﴾", "البقرة", 286, 49, "ما ابتلاك الله بأمر إلا وقد أودع فيك القدرة على تجاوزه."),
        DailyVerseItem(7, "﴿وَمَن يَتَوَكَّلْ عَلَى اللَّهِ فَهُوَ حَسْبُهُ﴾", "الطلاق", 3, 558, "من فوض أمره لله كفاه ما أهمه وأغناه عن الخلق."),
        DailyVerseItem(8, "﴿أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ﴾", "الرعد", 28, 252, "السكينة الحقيقية لا تُشترى، بل تُنال بمداومة ذكر الله."),
        DailyVerseItem(9, "﴿وَقُل رَّبِّ زِدْنِي عِلْمًا﴾", "طه", 114, 320, "العلم النافع نور يرفع صاحبه في الدنيا والآخرة."),
        DailyVerseItem(10, "﴿إِنَّ اللَّهَ مَعَ الصَّابِرِينَ﴾", "البقرة", 153, 23, "معية الله الخاصة تحفّ أهل الصبر والثبات عند الشدائد."),
        DailyVerseItem(11, "﴿رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ﴾", "البقرة", 201, 31, "دعاء جامع يجمع خيري الدنيا والآخرة."),
        DailyVerseItem(12, "﴿وَلَسَوْفَ يُعْطِيكَ رَبُّكَ فَتَرْضَىٰ﴾", "الضحى", 5, 596, "عطاء الله واسع يجبر الخواطر ويحقق الأمنيات."),
        DailyVerseItem(13, "﴿قُلْ يَا عِبَادِيَ الَّذِينَ أَسْرَفُوا عَلَىٰ أَنفُسِهِمْ لَا تَقْنَطُوا مِن رَّحْمَةِ اللَّهِ﴾", "الزمر", 53, 464, "باب التوبة مفتوح دائماً، ورحمة الله أوسع من كل ذنب."),
        DailyVerseItem(14, "﴿إِنَّ رَبِّي قَرِيبٌ مُّجِيبٌ﴾", "هود", 61, 228, "لا تستبطئ الإجابة، فربك يسمع همسك ويعلم سرك."),
        DailyVerseItem(15, "﴿وَعَسَىٰ أَن تَكْرَهُوا شَيْئًا وَهُوَ خَيْرٌ لَّكُمْ﴾", "البقرة", 216, 34, "تدبير الله لك خير من تدبيرك لنفسك، فارضَ بقضائه."),
        DailyVerseItem(16, "﴿وَاصْبِرْ لِحُكْمِ رَبِّكَ فَإِنَّكَ بِأَعْيُنِنَا﴾", "الطور", 48, 525, "أنت في حفظ الله ورعايته ما دمت صابراً محتسباً."),
        DailyVerseItem(17, "﴿ادْعُونِي أَسْتَجِبْ لَكُمْ﴾", "غافر", 60, 474, "وعدٌ إلهي لا يُخلف لمن أقبل على ربه بقلب صادق."),
        DailyVerseItem(18, "﴿وَهُوَ مَعَكُمْ أَيْنَ مَا كُنتُمْ﴾", "الحديد", 4, 538, "استشعار معية الله يورث الطمأنينة ويمنع من المعصية."),
        DailyVerseItem(19, "﴿إِن تَنصُرُوا اللَّهَ يَنصُرْكُمْ وَيُثَبِّتْ أَقْدَامَكُمْ﴾", "محمد", 7, 507, "انصر دين الله في نفسك وأخلاقك يثبتك الله في المواقف كلها."),
        DailyVerseItem(20, "﴿لَئِن شَكَرْتُمْ لَأَزِيدَنَّكُمْ﴾", "إبراهيم", 7, 256, "الشكر قيد النعم الموجودة وصيد النعم المفقودة."),
        DailyVerseItem(21, "﴿رَبِّ اشْرَحْ لِي صَدْرِي ۝ وَيَسِّرْ لِي أَمْرِي﴾", "طه", 25, 313, "دعاء مبارك لتيسير الأمور الصعبة وانشراح الصدر."),
        DailyVerseItem(22, "﴿وَمَا تَوْفِيقِي إِلَّا بِاللَّهِ ۚ عَلَيْهِ تَوَكَّلْتُ وَإِلَيْهِ أُنِيبُ﴾", "هود", 88, 231, "كل نجاح وتيسير في حياتك هو محض توفيق من الله."),
        DailyVerseItem(23, "﴿إِنَّ اللَّهَ لَا يُضِيعُ أَجْرَ الْمُحْسِنِينَ﴾", "التوبة", 120, 206, "كل معروف تقدمه ولو صغر محفوظ عند الله."),
        DailyVerseItem(24, "﴿فَسَبِّحْ بِحَمْدِ رَبِّكَ وَكُن مِّنَ السَّاجِدِينَ﴾", "الحجر", 98, 267, "التسبيح والسجود دواء القلوب عند ضيق الصدر."),
        DailyVerseItem(25, "﴿وَأَحْسِنُوا ۛ إِنَّ اللَّهَ يُحِبُّ الْمُحْسِنِينَ﴾", "البقرة", 195, 30, "الإحسان في القول والعمل يجلب محبة الله والناس."),
        DailyVerseItem(26, "﴿حَسْبُنَا اللَّهُ وَنِعْمَ الْوَكِيلُ﴾", "آل عمران", 173, 73, "كلمة قيلت عند الشدائد فانقلبت بها المحن إلى منح."),
        DailyVerseItem(27, "﴿رَبَّنَا لَا تُزِغْ قُلُوبَنَا بَعْدَ إِذْ هَدَيْتَنَا وَهَبْ لَنَا مِن لَّدُنكَ رَحْمَةً﴾", "آل عمران", 8, 50, "الثبات على الهداية أعظم نعمة يسألها المؤمن ربه."),
        DailyVerseItem(28, "﴿وَقُولُوا لِلنَّاسِ حُسْنًا﴾", "البقرة", 83, 12, "الكلمة الطيبة صدقة تفتح القلوب وتُذهب الشحناء."),
        DailyVerseItem(29, "﴿إِنَّ الصَّلَاةَ تَنْهَىٰ عَنِ الْفَحْشَاءِ وَالْمُنكَرِ﴾", "العنكبوت", 45, 401, "الصلاة الخاشعة حصن يحمي قلبك وسلوكك طوال اليوم."),
        DailyVerseItem(30, "﴿يَا أَيُّهَا الَّذِينَ آمَنُوا اسْتَعِينُوا بِالصَّبْرِ وَالصَّلَاةِ﴾", "البقرة", 153, 23, "أقوى سلاحين لمواجهة أعباء الحياة: الصبر والصلاة."),
        DailyVerseItem(31, "﴿وَمَا أُوتِيتُم مِّنَ الْعِلْمِ إِلَّا قَلِيلًا﴾", "الإسراء", 85, 290, "التواضع أمام عظمة علم الله سمة العقلاء."),
        DailyVerseItem(32, "﴿وَلَا تَيْأَسُوا مِن رَّوْحِ اللَّهِ﴾", "يوسف", 87, 246, "مهما اشتدت الظلمة، ففجر الفرج قادم بإذن الله."),
        DailyVerseItem(33, "﴿إِنَّ رَحْمَتَ اللَّهِ قَرِيبٌ مِّنَ الْمُحْسِنِينَ﴾", "الأعراف", 56, 157, "أحسن في عبادتك ومعاملاتك لتنال قرب رحمة الله."),
        DailyVerseItem(34, "﴿وَمَن يُؤْمِن بِاللَّهِ يَهْدِ قَلْبَهُ﴾", "التغابن", 11, 557, "الإيمان يملأ القلب بالرضا والسكينة عند المصائب."),
        DailyVerseItem(35, "﴿فَاصْبِرْ صَبْرًا جَمِيلًا﴾", "المعارج", 5, 568, "الصبر الجميل هو الصبر بلا شكوى للخلق ولا جزع."),
        DailyVerseItem(36, "﴿وَخَلَقْنَاكُمْ أَزْوَاجًا﴾", "النبأ", 8, 582, "المودة والرحمة بين الزوجين آية من آيات الله."),
        DailyVerseItem(37, "﴿رَبِّ اجْعَلْنِي مُقِيمَ الصَّلَاةِ وَمِن ذُرِّيَّتِي﴾", "إبراهيم", 40, 260, "اجعل الدعاء بصلاح الذرية ورداً ثابتاً في سجودك."),
        DailyVerseItem(38, "﴿وَبِالْوَالِدَيْنِ إِحْسَانًا﴾", "الإسراء", 23, 284, "برّ الوالدين باب مفتوح للبركة والتوفيق في الدنيا والآخرة."),
        DailyVerseItem(39, "﴿إِنَّ الْحَسَنَاتِ يُذْهِبْنَ السَّيِّئَاتِ﴾", "هود", 114, 234, "أتبع السيئة بالحسنة تمحُها وتجدد نقاء قلبك."),
        DailyVerseItem(40, "﴿وَتَعَاوَنُوا عَلَى الْبِرِّ وَالتَّقْوَىٰ﴾", "المائدة", 2, 106, "كن عوناً لغيرك في الخير يكن الله في عونك."),
        DailyVerseItem(41, "﴿ادْفَعْ بِالَّتِي هِيَ أَحْسَنُ﴾", "فصلت", 34, 480, "مقابلة الإساءة بالإحسان تطفئ العداوات وتكسب القلوب."),
        DailyVerseItem(42, "﴿إِنَّ أَكْرَمَكُمْ عِندَ اللَّهِ أَتْقَاكُمْ﴾", "الحجرات", 13, 517, "ميزان التفاضل عند الله هو التقوى وصلاح القلب."),
        DailyVerseItem(43, "﴿وَمَا كَانَ اللَّهُ مُعَذِّبَهُمْ وَهُمْ يَسْتَغْفِرُونَ﴾", "الأنفال", 33, 180, "الاستغفار أمانٌ من البلاء ومفتاح للخيرات."),
        DailyVerseItem(44, "﴿رَبَّنَا اغْفِرْ لَنَا ذُنُوبَنَا وَإِسْرَافَنَا فِي أَمْرِنَا﴾", "آل عمران", 147, 68, "الاعتراف بالتقصير وطلب المغفرة طريق النجاة."),
        DailyVerseItem(45, "﴿وَقُل رَّبِّ أَدْخِلْنِي مُدْخَلَ صِدْقٍ وَأَخْرِجْنِي مُخْرَجَ صِدْقٍ﴾", "الإسراء", 80, 290, "دعاء عظيم لكل بداية جديدة أو سفر أو عمل."),
        DailyVerseItem(46, "﴿إِنَّ اللَّهَ وَمَلَائِكَتَهُ يُصَلُّونَ عَلَى النَّبِيِّ﴾", "الأحزاب", 56, 426, "أكثر من الصلاة على النبي ﷺ لتكفى همك ويُغفر ذنبك."),
        DailyVerseItem(47, "﴿وَلَا تَمْشِ فِي الْأَرْضِ مَرَحًا﴾", "الإسراء", 37, 285, "التواضع رفعة، والكبر يبغضه الله والناس."),
        DailyVerseItem(48, "﴿وَأَقِيمُوا الصَّلَاةَ وَآتُوا الزَّكَاةَ﴾", "البقرة", 43, 7, "الصدقة والزكاة تطهر المال وتبارك في الرزق."),
        DailyVerseItem(49, "﴿فَإِذَا عَزَمْتَ فَتَوَكَّلْ عَلَى اللَّهِ﴾", "آل عمران", 159, 71, "خذ بالأسباب ثم اعتمد بقلبك على الله وحده."),
        DailyVerseItem(50, "﴿رَبِّ هَبْ لِي حُكْمًا وَأَلْحِقْنِي بِالصَّالِحِينَ﴾", "الشعراء", 83, 370, "اسأل الله الحكمة في القول والعمل وصحبة الصالحين."),
        DailyVerseItem(51, "﴿وَمَا الْحَيَاةُ الدُّنْيَا إِلَّا مَتَاعُ الْغُرُورِ﴾", "آل عمران", 185, 74, "اعمل لآخرتك ولا تجعل الدنيا أكبر همك."),
        DailyVerseItem(52, "﴿سَيَجْعَلُ اللَّهُ بَعْدَ عُسْرٍ يُسْرًا﴾", "الطلاق", 7, 559, "كل ضيق سيعقبه اتساع وفرج من الله لا محالة."),
        DailyVerseItem(53, "﴿وَاللَّهُ خَيْرٌ حَافِظًا ۖ وَهُوَ أَرْحَمُ الرَّاحِمِينَ﴾", "يوسف", 64, 243, "استودع الله نفسك وأهلك ومالك، فودائعه لا تضيع."),
        DailyVerseItem(54, "﴿إِنَّ مَعِيَ رَبِّي سَيَهْدِينِ﴾", "الشعراء", 62, 369, "قمة اليقين بالله حين تنغلق كل السبل الأرضية."),
        DailyVerseItem(55, "﴿وَلَا تَحْزَنْ إِنَّ اللَّهَ مَعَنَا﴾", "التوبة", 40, 193, "لا مكان للحزن في قلب يوقن أن الله معه."),
        DailyVerseItem(56, "﴿قُل لَّن يُصِيبَنَا إِلَّا مَا كَتَبَ اللَّهُ لَنَا﴾", "التوبة", 51, 195, "الرضا بالقدر يريح النفس من القلق والخوف."),
        DailyVerseItem(57, "﴿وَأَن لَّيْسَ لِلْإِنسَانِ إِلَّا مَا سَعَىٰ﴾", "النجم", 39, 527, "اجتهد في طاعة الله وعمارة الأرض، فثمرة سعيك محفوظة."),
        DailyVerseItem(58, "﴿إِنَّ اللَّهَ يُحِبُّ التَّوَّابِينَ وَيُحِبُّ الْمُتَطَهِّرِينَ﴾", "البقرة", 222, 35, "جدد توبتك كل يوم لتنال محبة الله."),
        DailyVerseItem(59, "﴿رَبَّنَا هَبْ لَنَا مِنْ أَزْوَاجِنَا وَذُرِّيَّاتِنَا قُرَّةَ أَعْيُنٍ﴾", "الفرقان", 74, 366, "صلاح الأسرة نعمة لا تقدر بثمن."),
        DailyVerseItem(60, "﴿وَاذْكُر رَّبَّكَ إِذَا نَسِيتَ﴾", "الكهف", 24, 296, "الذكر يوقظ القلب من الغفلة ويعيد له صفاءه."),
        DailyVerseItem(61, "﴿فَفِرُّوا إِلَى اللَّهِ﴾", "الذاريات", 50, 522, "إذا ضاقت بك الدنيا فاهرب إلى الله بالصلاة والدعاء."),
        DailyVerseItem(62, "﴿وَمَا بِكُم مِّن نِّعْمَةٍ فَمِنَ اللَّهِ﴾", "النحل", 53, 272, "انسب كل نعمة إلى المنعم سبحانه يدمها عليك."),
        DailyVerseItem(63, "﴿إِنَّ الْأَبْرَارَ لَفِي نَعِيمٍ﴾", "الانفطار", 13, 587, "أهل الصدق والبر ينعمون بطمأنينة القلب في الدنيا والجنة في الآخرة."),
        DailyVerseItem(64, "﴿وَقُلِ الْحَمْدُ لِلَّهِ سَيُرِيكُمْ آيَاتِهِ فَتَعْرِفُونَهَا﴾", "النمل", 93, 385, "تأمل في خلق الله وتدبيره يزدد إيمانك ويقينك."),
        DailyVerseItem(65, "﴿رَبِّ إِنِّي لِمَا أَنزَلْتَ إِلَيَّ مِنْ خَيْرٍ فَقِيرٌ﴾", "القصص", 24, 388, "إظهار الافتقار إلى الله سبب لجلب الأرزاق والخيرات."),
        DailyVerseItem(66, "﴿وَلَا تَقْفُ مَا لَيْسَ لَكَ بِهِ عِلْمٌ﴾", "الإسراء", 36, 285, "التثبت قبل الكلام يحفظ اللسان ويصون الأعراض."),
        DailyVerseItem(67, "﴿إِنَّ السَّمْعَ وَالْبَصَرَ وَالْفُؤَادَ كُلُّ أُولَٰئِكَ كَانَ عَنْهُ مَسْئُولًا﴾", "الإسراء", 36, 285, "صُن جوارحك عما لا يرضي الله."),
        DailyVerseItem(68, "﴿وَسَارِعُوا إِلَىٰ مَغْفِرَةٍ مِّن رَّبِّكُمْ وَجَنَّةٍ عَرْضُهَا السَّمَاوَاتُ وَالْأَرْضُ﴾", "آل عمران", 133, 67, "المسارعة إلى الخيرات علامة حياة القلب."),
        DailyVerseItem(69, "﴿فَاسْتَبِقُوا الْخَيْرَاتِ﴾", "البقرة", 148, 23, "لا تؤجل عمل الخير، فالأعمار بيد الله."),
        DailyVerseItem(70, "﴿وَالْكَاظِمِينَ الْغَيْظَ وَالْعَافِينَ عَنِ النَّاسِ ۗ وَاللَّهُ يُحِبُّ الْمُحْسِنِينَ﴾", "آل عمران", 134, 67, "العفو عند المقدرة من شيم الكرام وسبب لمحبة الله."),
        DailyVerseItem(71, "﴿إِنَّ اللَّهَ يَأْمُرُ بِالْعَدْلِ وَالْإِحْسَانِ﴾", "النحل", 90, 277, "العدل أساس الملك، والإحسان روح التعامل."),
        DailyVerseItem(72, "﴿وَمَن يَعْمَلْ مِثْقَالَ ذَرَّةٍ خَيْرًا يَرَهُ﴾", "الزلزلة", 7, 599, "لا تحتقرنّ من المعروف شيئاً، فالله يضاعف الحسنات."),
        DailyVerseItem(73, "﴿أَلَمْ يَأْنِ لِلَّذِينَ آمَنُوا أَن تَخْشَعَ قُلُوبُهُمْ لِذِكْرِ اللَّهِ﴾", "الحديد", 16, 539, "آن الأوان أن يلين القلب لآيات الله ومواعظه."),
        DailyVerseItem(74, "﴿رَبَّنَا تَقَبَّلْ مِنَّا ۖ إِنَّكَ أَنتَ السَّمِيعُ الْعَلِيمُ﴾", "البقرة", 127, 19, "الاهتمام بقبول العمل أعظم من العمل نفسه."),
        DailyVerseItem(75, "﴿قُلْ هُوَ اللَّهُ أَحَدٌ ۝ اللَّهُ الصَّمَدُ﴾", "الإخلاص", 1, 604, "توحيد الله الخالص هو أساس النجاة وسعادة الدارين.")
    )

    fun getTodayIndex(): Int {
        val now = Calendar.getInstance()
        val epochDay = TimeUnit.MILLISECONDS.toDays(
            now.timeInMillis + now.timeZone.getOffset(now.timeInMillis)
        )
        return ((epochDay % verses.size).toInt() + verses.size) % verses.size
    }

    fun getTodayVerse(): DailyVerseItem = verses[getTodayIndex()]
}

@Composable
fun DailyVerseCard(
    nav: Nav,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val fx = LocalFx.current
    val todayIndex = remember { DailyVerseRepository.getTodayIndex() }
    var currentIndex by remember { mutableIntStateOf(todayIndex) }
    val verse = DailyVerseRepository.verses[currentIndex]

    Surface(
        onClick = { fx.click(); nav.openPage(verse.page) },
        shape = RoundedCornerShape(26.dp),
        border = BorderStroke(1.6.dp, GoldWarm),
        shadowElevation = 5.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Box(Modifier.background(Brush.linearGradient(listOf(RoyalForest, TealStart, TealEnd)))) {
            IslamicPattern(Modifier.matchParentSize())

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = GoldShine.copy(alpha = 0.16f),
                        border = BorderStroke(1.2.dp, GoldShine.copy(alpha = 0.7f))
                    ) {
                        Text(
                            text = if (currentIndex == todayIndex) "✦ آيَةُ الْيَوْمِ ✦" else "✦ آيَةٌ مُخْتَارَةٌ ✦",
                            fontFamily = UthmanicFontFamily,
                            color = GoldShine,
                            fontWeight = FontWeight.Bold,
                            fontSize = 23.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 22.dp, vertical = 4.dp)
                        )
                    }

                    if (currentIndex != todayIndex) {
                        IconButton(
                            onClick = { fx.click(); currentIndex = todayIndex },
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .size(34.dp)
                                .background(Color.White.copy(alpha = 0.12f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "العودة لآية اليوم",
                                tint = GoldShine,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "اليوم ${verse.id} من 75",
                    color = GoldSoftBg.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = verse.text,
                    fontFamily = UthmanicFontFamily,
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    lineHeight = 46.sp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "سُورَةُ ${verse.surah} • آيَةٌ ${verse.ayahNumber}",
                    fontFamily = UthmanicFontFamily,
                    color = GoldShine,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(RoyalForest.copy(alpha = 0.45f))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "💡 ${verse.reflection}",
                        color = GoldSoftBg,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(14.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VersePillButton("اقرأ في المصحف", Icons.AutoMirrored.Filled.MenuBook) {
                        nav.openPage(verse.page)
                    }
                    VersePillButton("نسخ", Icons.Default.ContentCopy) {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val copyText = "${verse.text}\n[سورة ${verse.surah}: ${verse.ayahNumber}]\n${verse.reflection}"
                        clipboard.setPrimaryClip(ClipData.newPlainText("Ayah", copyText))
                        Toast.makeText(context, "تم نسخ الآية", Toast.LENGTH_SHORT).show()
                    }
                    VersePillButton("مشاركة", Icons.Default.Share) {
                        shareText(context, "${verse.text}\n[سورة ${verse.surah}: ${verse.ayahNumber}]\n\n${verse.reflection}")
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = {
                            fx.click()
                            currentIndex = if (currentIndex - 1 < 0) DailyVerseRepository.verses.lastIndex else currentIndex - 1
                        },
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, GoldShine.copy(alpha = 0.5f)),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, "الآية السابقة", tint = GoldShine, modifier = Modifier.size(18.dp))
                        }
                    }

                    Text(
                        text = "تصفح 75 آية يومية متجددة",
                        color = GoldSoftBg.copy(alpha = 0.75f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Surface(
                        onClick = {
                            fx.click()
                            currentIndex = (currentIndex + 1) % DailyVerseRepository.verses.size
                        },
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, GoldShine.copy(alpha = 0.5f)),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "الآية التالية", tint = GoldShine, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VersePillButton(text: String, icon: ImageVector, onClick: () -> Unit) {
    val fx = LocalFx.current
    Surface(
        onClick = { fx.click(); onClick() },
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, GoldShine.copy(alpha = 0.75f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = GoldShine, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(5.dp))
            Text(text, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
        }
    }
}

