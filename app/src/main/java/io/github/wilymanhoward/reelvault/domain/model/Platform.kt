package io.github.wilymanhoward.reelvault.domain.model

/** A short-form video platform ReelVault knows how to preview and open. */
enum class Platform(val displayName: String) {
    TIKTOK("TikTok"),
    INSTAGRAM("Instagram"),
    YOUTUBE("YouTube"),
    FACEBOOK("Facebook"),
    X("X"),
    PINTEREST("Pinterest"),
    XIAOHONGSHU("RED"),
    OTHER("Web"),
}
