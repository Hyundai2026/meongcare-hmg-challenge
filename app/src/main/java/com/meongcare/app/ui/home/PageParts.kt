package com.meongcare.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.meongcare.app.R
import com.meongcare.app.ui.theme.DesignColors
import com.meongcare.app.ui.theme.DesignScale
import com.meongcare.app.ui.theme.DesignSpace
import com.meongcare.app.ui.theme.DesignType

/* 처음 화면 바로가기로 여는 쪽(반려동물 카드·릴렉스, 019)이 같이 쓰는 조각. */

/** 쪽 머리: [←] + 제목 (프로토타입 머리 40/600) */
@Composable
fun PageHeader(title: String, onBack: () -> Unit, scale: DesignScale, colors: DesignColors, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.size(scale.touch(96.dp))) {
            Icon(
                HomeIcons.Back,
                contentDescription = stringResource(R.string.page_back),
                tint = colors.homeText,
                modifier = Modifier.size(scale.gap(40.dp)),
            )
        }
        Spacer(Modifier.width(scale.gap(DesignSpace.l)))
        Text(title, style = scale.style(DesignType.title), color = colors.homeText)
    }
}

/** 프로토타입 스위치 모양 (트랙 104×60, 손잡이 48). 누르는 곳은 부르는 쪽이 정한다 */
@Composable
fun DesignSwitchTrack(on: Boolean, track: Color, scale: DesignScale, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(scale.gap(104.dp), scale.gap(60.dp))
            .background(track, RoundedCornerShape(scale.gap(30.dp))),
    ) {
        Box(
            Modifier
                .padding(scale.gap(6.dp))
                .offset(x = if (on) scale.gap(44.dp) else 0.dp)
                .size(scale.gap(48.dp))
                .background(Color.White, CircleShape),
        )
    }
}

/** 구역 사이 가로선 (2px) */
@Composable
fun DesignDivider(colors: DesignColors, scale: DesignScale, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(scale.gap(2.dp).coerceAtLeast(1.dp)).background(colors.homeLine))
}
