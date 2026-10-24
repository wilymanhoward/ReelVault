package io.github.wilymanhoward.reelvault.domain.tagging

import io.github.wilymanhoward.reelvault.domain.model.TagRule

/**
 * On-device, free auto-tagging: hashtags from the caption plus keyword → category rules.
 * ML Kit image labels (when enabled) are fed in as extra text, so "Pasta" on a thumbnail
 * can trigger the Food rule just like the word in a caption.
 */
object AutoTagger {
    private val HASHTAG = Regex("""#([\p{L}\p{N}_]+)""")
    private val WORD = Regex("""[\p{L}\p{N}]+""")
    // Lets "recipe" match "recipes" and "workout" match "workouts" without a real stemmer.
    private const val MAX_SUFFIX = 2

    /** Hashtags in [text], lowercased, without the '#'. */
    fun hashtags(text: String): Set<String> =
        HASHTAG.findAll(text).map { it.groupValues[1].lowercase() }.filter { it.any(Char::isLetter) }.toSet()

    /** Categories of the enabled [rules] whose keywords appear in [text]. */
    fun categories(text: String, rules: List<TagRule>): Set<String> {
        val lower = text.lowercase()
        val words = WORD.findAll(lower).map { it.value }.toList()
        return rules.filter { it.enabled }.filter { rule ->
            rule.keywords.any { kw ->
                val k = kw.trim().lowercase()
                when {
                    k.isEmpty() -> false
                    ' ' in k -> lower.contains(k)
                    // Short keywords (ai, dj, abs) must match exactly, or "ai" would tag "air" and "aid".
                    k.length <= 3 -> k in words
                    else -> words.any { w -> w.startsWith(k) && w.length - k.length <= MAX_SUFFIX }
                }
            }
        }.map { it.category.trim() }.toSet()
    }

    /**
     * All auto tags for an item. A hashtag that only repeats a category (#food next to Food) is dropped.
     */
    fun autoTags(title: String?, caption: String?, imageLabels: List<String>, rules: List<TagRule>): Set<String> {
        val text = listOfNotNull(title, caption).joinToString(" ")
        val tags = hashtags(text)
        val cats = categories("$text ${tags.joinToString(" ")} ${imageLabels.joinToString(" ")}", rules)
        val catLower = cats.map { it.lowercase() }.toSet()
        return cats + tags.filterNot { it in catLower }
    }

    /** Rules shipped with the app. Users can edit, disable or delete them. */
    val DEFAULT_RULES: List<TagRule> = listOf(
        "Food" to "food, recipe, cook, cooking, baking, bake, dinner, lunch, breakfast, dessert, pasta, meal, foodie, restaurant, snack, salad, cake",
        "Fitness" to "workout, gym, fitness, exercise, training, abs, cardio, yoga, pilates, muscle, squat, running, hiit",
        "Travel" to "travel, trip, vacation, holiday, beach, hotel, flight, explore, wanderlust, itinerary, hiking, mountain, city guide",
        "Fashion" to "fashion, outfit, ootd, style, streetwear, dress, sneakers, clothing, haul, wardrobe, jewelry",
        "Tech" to "tech, technology, gadget, iphone, android, smartphone, laptop, coding, programming, software, review, unboxing, ai",
        "Music" to "music, song, cover, guitar, piano, singing, concert, dj, beat, album, rap, lyrics",
        "Comedy" to "funny, comedy, joke, prank, meme, lol, humor, skit, standup",
        "DIY" to "diy, craft, crafts, handmade, woodworking, homemade, tutorial, makeover, organize, decor, hack",
        "Beauty" to "beauty, makeup, skincare, hair, hairstyle, nails, cosmetics, grwm, lipstick, glow",
        "Education" to "learn, learning, education, study, lesson, explained, science, history, facts, howto, language",
        "Finance" to "finance, money, invest, investing, stocks, budget, saving, crypto, personal finance, side hustle",
    ).map { (category, keywords) -> TagRule(category = category, keywords = keywords.split(",").map { it.trim() }) }
}
