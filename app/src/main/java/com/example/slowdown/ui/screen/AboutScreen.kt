package com.sharonZ.slowdown.ui.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sharonZ.slowdown.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val versionName = getVersionName(context)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Logo and Info
            item {
                Spacer(modifier = Modifier.height(32.dp))

                // App Icon
                Image(
                    painter = painterResource(id = R.mipmap.ic_launcher_round),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(24.dp))
                )

                Spacer(modifier = Modifier.height(16.dp))

                // App Name
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                // Version
                Text(
                    text = stringResource(R.string.about_version, versionName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Tagline
                Text(
                    text = stringResource(R.string.about_tagline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))
            }

            // Developer Section
            item {
                AboutSectionTitle(title = stringResource(R.string.about_developer_section))
            }

            item {
                AboutItem(
                    icon = Icons.Outlined.Person,
                    title = stringResource(R.string.about_developer),
                    subtitle = stringResource(R.string.about_developer_name),
                    onClick = null
                )
            }

            item {
                AboutItem(
                    icon = Icons.Outlined.Email,
                    title = stringResource(R.string.about_contact),
                    subtitle = stringResource(R.string.about_email),
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:${context.getString(R.string.about_email)}")
                            putExtra(Intent.EXTRA_SUBJECT, "[SlowDown] Feedback")
                        }
                        context.startActivity(Intent.createChooser(intent, context.getString(R.string.about_send_email)))
                    }
                )
            }

            item {
                AboutItem(
                    icon = Icons.Outlined.Code,
                    title = stringResource(R.string.about_github),
                    subtitle = stringResource(R.string.about_github_url),
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.about_github_url)))
                        context.startActivity(intent)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Links Section
            item {
                AboutSectionTitle(title = stringResource(R.string.about_links_section))
            }

            item {
                AboutItem(
                    icon = Icons.Outlined.Star,
                    title = stringResource(R.string.about_rate),
                    subtitle = stringResource(R.string.about_rate_subtitle),
                    onClick = {
                        // Try to open app store (Google Play or other)
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Fallback to browser
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}"))
                            context.startActivity(intent)
                        }
                    }
                )
            }

            item {
                AboutItem(
                    icon = Icons.Outlined.Share,
                    title = stringResource(R.string.about_share),
                    subtitle = stringResource(R.string.about_share_subtitle),
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.app_name))
                            putExtra(Intent.EXTRA_TEXT, context.getString(R.string.about_share_text))
                        }
                        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.about_share)))
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Legal Section
            item {
                AboutSectionTitle(title = stringResource(R.string.about_legal_section))
            }

            item {
                AboutItem(
                    icon = Icons.Outlined.PrivacyTip,
                    title = stringResource(R.string.about_privacy),
                    subtitle = stringResource(R.string.about_privacy_subtitle),
                    onClick = {
                        // TODO: Add privacy policy URL
                    }
                )
            }

            item {
                AboutItem(
                    icon = Icons.Outlined.Description,
                    title = stringResource(R.string.about_terms),
                    subtitle = stringResource(R.string.about_terms_subtitle),
                    onClick = {
                        // TODO: Add terms URL
                    }
                )
            }

            item {
                AboutItem(
                    icon = Icons.Outlined.Source,
                    title = stringResource(R.string.about_licenses),
                    subtitle = stringResource(R.string.about_licenses_subtitle),
                    onClick = {
                        // TODO: Add licenses screen
                    }
                )

                Spacer(modifier = Modifier.height(32.dp))
            }

            // Footer
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.about_copyright),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = stringResource(R.string.about_made_with_love),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun AboutSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun AboutItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (onClick != null) {
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun getVersionName(context: Context): String {
    return try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "Unknown"
    } catch (e: Exception) {
        "Unknown"
    }
}
