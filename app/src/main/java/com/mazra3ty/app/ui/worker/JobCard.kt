package com.mazra3ty.app.ui.worker

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mazra3ty.app.ui.theme.*

@Composable
fun JobCard(
    job: Job,
    onApplyClick: (Job) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .border(
                width = 1.dp,
                color = CardBorder,
                shape = RoundedCornerShape(12.dp)
            ),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            // Row 1: Title + Price
            Row(
                modifier       = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.Top
            ) {
                // Job Title
                Text(
                    text       = job.title,
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color      = TextDark,
                    modifier   = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Price
                Text(
                    text       = job.price,
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color      = GreenPrimary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Duration
            Text(
                text     = "⏱ ${job.duration}",
                fontSize = 12.sp,
                color    = TextGray,
                modifier = Modifier.align(Alignment.End)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Row 2: Location + Button
            Row(
                modifier          = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                // Location
                Text(
                    text     = "📍 ${job.location}",
                    fontSize = 12.sp,
                    color    = TextGray,
                    modifier = Modifier.weight(1f)
                )

                // Apply Button
                Button(
                    onClick = { onApplyClick(job) },
                    shape   = RoundedCornerShape(20.dp),
                    colors  = ButtonDefaults.buttonColors(
                        containerColor = GreenButton
                    ),
                    contentPadding = PaddingValues(
                        horizontal = 16.dp,
                        vertical   = 8.dp
                    )
                ) {
                    Text(
                        text     = "Apply Now",
                        fontSize = 12.sp,
                        color    = White
                    )
                }
            }
        }
    }
}