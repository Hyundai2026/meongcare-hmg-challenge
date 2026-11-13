package com.meongcare.app.ui.home

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * 홈 화면 아이콘 (계획 019 D6). 확정 디자인 프로토타입(`docs/product/design/prototype/MeongCare car.html`)의
 * 선 아이콘 SVG 경로를 그대로 옮겼다 (팀 디자인 자료). 24 격자, 선 굵기 1.8, 둥근 끝.
 * 색은 `Icon(tint = ...)`로 칠한다.
 */
object HomeIcons {
    /** 병원 찾기: 위치 핀 */
    val Hospital: ImageVector by lazy {
        lineIcon(
            "Hospital",
            "M12 21s-6.5-5.8-6.5-11a6.5 6.5 0 0 1 13 0c0 5.2-6.5 11-6.5 11z",
            circle(12f, 10f, 2.4f),
        )
    }

    /** 릴렉스: 잎 */
    val Relax: ImageVector by lazy {
        lineIcon(
            "Relax",
            "M5.5 18.5C5.5 10.5 11 5.5 19 5c.2 8.3-4.8 13.5-13.5 13.5z",
            "M5.5 18.5c2.6-3.6 5.6-6.2 9-8",
        )
    }

    /** 반려동물 카드: 발바닥 */
    val PetCard: ImageVector by lazy {
        lineIcon(
            "PetCard",
            circle(6.5f, 10.5f, 1.7f),
            circle(9.7f, 6.6f, 1.7f),
            circle(14.3f, 6.6f, 1.7f),
            circle(17.5f, 10.5f, 1.7f),
            "M12 12c-2.5 0-5 3-5 5.2 0 1.5 1.1 2.3 2.4 2.3.9 0 1.7-.4 2.6-.4s1.7.4 2.6.4c1.3 0 2.4-.8 2.4-2.3 0-2.2-2.5-5.2-5-5.2z",
        )
    }

    /** 버튼 끝 화살표 (선 굵기 2) */
    val Chevron: ImageVector by lazy { lineIcon("Chevron", "M9.5 6l6 6-6 6", strokeWidth = 2f) }

    /** 진료 기록: 문서 (프로토타입 진료 기록 칸, 채운 모양) */
    val Record: ImageVector by lazy {
        ImageVector.Builder("Record", 24.dp, 24.dp, 24f, 24f).apply {
            addPath(
                PathParser().parsePathString(
                    "M14 2H6c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z",
                ).toNodes(),
                fill = SolidColor(Color.Black),
            )
        }.build()
    }

    /** 음성 명령: 마이크 (프로토타입 음성 힌트 아이콘) */
    val Mic: ImageVector by lazy {
        lineIcon("Mic", "M12 3a3 3 0 0 1 3 3v5a3 3 0 0 1-6 0V6a3 3 0 0 1 3-3z", "M5.5 11a6.5 6.5 0 0 0 13 0", "M12 17.5V21")
    }

    /** 음성 안내: 스피커 (채운 모양) */
    val Speaker: ImageVector by lazy {
        ImageVector.Builder("Speaker", 24.dp, 24.dp, 24f, 24f).apply {
            addPath(
                PathParser().parsePathString(
                    "M3 9v6h4l5 5V4L7 9H3zm13.5 3c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02zM14 3.23v2.06c2.89.86 5 3.54 5 6.71s-2.11 5.85-5 6.71v2.06c4.01-.91 7-4.49 7-8.77s-2.99-7.86-7-8.77z",
                ).toNodes(),
                fill = SolidColor(Color.Black),
            )
        }.build()
    }

    /** 화면 머리 [←] (프로토타입 머리 화살표) */
    val Back: ImageVector by lazy { lineIcon("Back", "M19 12H5", "M11 6l-6 6 6 6", strokeWidth = 2f) }

    private fun circle(cx: Float, cy: Float, r: Float) =
        "M${cx - r} $cy a$r $r 0 1 0 ${2 * r} 0 a$r $r 0 1 0 ${-2 * r} 0z"

    private fun lineIcon(name: String, vararg paths: String, strokeWidth: Float = 1.8f): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            paths.forEach {
                addPath(
                    PathParser().parsePathString(it).toNodes(),
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = strokeWidth,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()
}
