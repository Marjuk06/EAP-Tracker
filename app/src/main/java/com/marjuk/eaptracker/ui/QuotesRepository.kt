package com.marjuk.eaptracker.ui

import androidx.compose.runtime.mutableStateOf
import kotlin.random.Random

data class QuoteItem(
    val quote: String,
    val category: String,
    val emoji: String
)

object QuotesRepository {

    val allQuotes = listOf(
        // 🔥 Discipline & Consistency
        QuoteItem("You don’t need motivation. You need one more focused session.", "Discipline", "🔥"),
        QuoteItem("Small progress every day becomes a big result.", "Discipline", "🔥"),
        QuoteItem("Study today. Thank yourself tomorrow.", "Discipline", "🔥"),
        QuoteItem("Consistency beats intensity.", "Discipline", "🔥"),
        QuoteItem("One page. One problem. One step closer.", "Discipline", "🔥"),
        QuoteItem("Don’t break the chain.", "Discipline", "🔥"),
        QuoteItem("Your future is built in sessions like this.", "Discipline", "🔥"),
        QuoteItem("A little every day. A lot by the end.", "Discipline", "🔥"),

        // 🎯 Focus / EAP Tracker style
        QuoteItem("Don’t count the hours. Make the hours count.", "Focus", "🎯"),
        QuoteItem("Focus now. Freedom later.", "Focus", "🎯"),
        QuoteItem("Track the effort. Trust the process.", "Focus", "🎯"),
        QuoteItem("Every focused minute is an investment.", "Focus", "🎯"),
        QuoteItem("Your only competition is yesterday’s you.", "Focus", "🎯"),
        QuoteItem("Less scrolling. More progress.", "Focus", "🎯"),
        QuoteItem("Stay focused. Your goal hasn’t moved.", "Focus", "🎯"),
        QuoteItem("One focused session can change your whole day.", "Focus", "🎯"),

        // ⚡ When feeling lazy / procrastinating
        QuoteItem("You said you wanted it. Now prove it.", "Anti-Lazy", "⚡"),
        QuoteItem("Start before you feel ready.", "Anti-Lazy", "⚡"),
        QuoteItem("You don’t have to feel motivated to begin.", "Anti-Lazy", "⚡"),
        QuoteItem("Just start. Momentum will follow.", "Anti-Lazy", "⚡"),
        QuoteItem("Five minutes is better than zero.", "Anti-Lazy", "⚡"),
        QuoteItem("The hardest part is opening the book.", "Anti-Lazy", "⚡"),
        QuoteItem("Do it tired. Do it slowly. Just don’t quit.", "Anti-Lazy", "⚡"),

        // 🧠 Exam-focused
        QuoteItem("The exam rewards what you practiced when nobody was watching.", "Exam Ready", "🧠"),
        QuoteItem("Every question you solve today is one less surprise tomorrow.", "Exam Ready", "🧠"),
        QuoteItem("Prepare now so panic doesn’t prepare you later.", "Exam Ready", "🧠"),
        QuoteItem("Your marks are being built before exam day.", "Exam Ready", "🧠"),
        QuoteItem("Don’t wish for a better result. Prepare for one.", "Exam Ready", "🧠"),
        QuoteItem("The syllabus won’t finish itself. 😄", "Exam Ready", "🧠"),
        QuoteItem("Future you is counting on today’s effort.", "Exam Ready", "🧠"),

        // 🏆 Strong motivational
        QuoteItem("Your goal is bigger than your excuses.", "Motivation", "🏆"),
        QuoteItem("You are closer than you think. Keep going.", "Motivation", "🏆"),
        QuoteItem("Dreams don’t respond to procrastination.", "Motivation", "🏆"),
        QuoteItem("The version of you you want to become is built through discipline.", "Motivation", "🏆"),
        QuoteItem("Make today’s effort tomorrow’s advantage.", "Motivation", "🏆"),
        QuoteItem("Nobody can study for you. Nobody can earn your result for you.", "Motivation", "🏆"),
        QuoteItem("You don’t need a perfect day. You need a productive one.", "Motivation", "🏆"),
        QuoteItem("Keep going. Your future self is watching.", "Motivation", "🏆")
    )

    val top10Quotes = listOf(
        "Don’t count the hours. Make the hours count.",
        "One focused session can change your whole day.",
        "Small progress every day becomes a big result.",
        "Start before you feel ready.",
        "Your future is built in sessions like this.",
        "Five minutes is better than zero.",
        "Track the effort. Trust the process.",
        "Focus now. Freedom later.",
        "You don’t need a perfect day. You need a productive one.",
        "Keep going. Your future self is watching."
    )

    val currentQuote = mutableStateOf(allQuotes.first())
    private var lastIndex = -1

    fun getRandomQuote(category: String? = null): QuoteItem {
        val filtered = if (category != null && category != "All") {
            allQuotes.filter { it.category.equals(category, ignoreCase = true) }
        } else {
            allQuotes
        }
        if (filtered.isEmpty()) return allQuotes.first()

        lastIndex = (lastIndex + 1) % filtered.size
        val quote = filtered[lastIndex]
        currentQuote.value = quote
        return quote
    }

    fun nextQuote() {
        val nextIdx = (allQuotes.indexOf(currentQuote.value) + 1) % allQuotes.size
        currentQuote.value = allQuotes[nextIdx]
    }
}
