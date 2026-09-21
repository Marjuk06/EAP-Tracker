export interface MotivationQuoteItem {
  id: string;
  quote: string;
  category: 'top10' | 'discipline' | 'focus' | 'lazy' | 'exam' | 'motivational';
  author: string;
}

export const adminQuotesLibrary: MotivationQuoteItem[] = [
  // ⭐ Top 10 Curated Selection
  { id: 'top-1', quote: "Don’t count the hours. Make the hours count.", category: 'top10', author: "EAP Mentor" },
  { id: 'top-2', quote: "One focused session can change your whole day.", category: 'top10', author: "EAP Mentor" },
  { id: 'top-3', quote: "Small progress every day becomes a big result.", category: 'top10', author: "EAP Mentor" },
  { id: 'top-4', quote: "Start before you feel ready.", category: 'top10', author: "EAP Mentor" },
  { id: 'top-5', quote: "Your future is built in sessions like this.", category: 'top10', author: "EAP Mentor" },
  { id: 'top-6', quote: "Five minutes is better than zero.", category: 'top10', author: "EAP Mentor" },
  { id: 'top-7', quote: "Track the effort. Trust the process.", category: 'top10', author: "EAP Mentor" },
  { id: 'top-8', quote: "Focus now. Freedom later.", category: 'top10', author: "EAP Mentor" },
  { id: 'top-9', quote: "You don’t need a perfect day. You need a productive one.", category: 'top10', author: "EAP Mentor" },
  { id: 'top-10', quote: "Keep going. Your future self is watching.", category: 'top10', author: "EAP Mentor" },

  // 🔥 Discipline & Consistency
  { id: 'disc-1', quote: "You don’t need motivation. You need one more focused session.", category: 'discipline', author: "Discipline Guide" },
  { id: 'disc-2', quote: "Small progress every day becomes a big result.", category: 'discipline', author: "Consistency" },
  { id: 'disc-3', quote: "Study today. Thank yourself tomorrow.", category: 'discipline', author: "Daily Habit" },
  { id: 'disc-4', quote: "Consistency beats intensity.", category: 'discipline', author: "Discipline" },
  { id: 'disc-5', quote: "One page. One problem. One step closer.", category: 'discipline', author: "Daily Push" },
  { id: 'disc-6', quote: "Don’t break the chain.", category: 'discipline', author: "Habit Tracker" },
  { id: 'disc-7', quote: "Your future is built in sessions like this.", category: 'discipline', author: "Dedication" },
  { id: 'disc-8', quote: "A little every day. A lot by the end.", category: 'discipline', author: "Daily Habit" },

  // 🎯 Focus / EPA Tracker style
  { id: 'foc-1', quote: "Don’t count the hours. Make the hours count.", category: 'focus', author: "EPA Focus" },
  { id: 'foc-2', quote: "Focus now. Freedom later.", category: 'focus', author: "Focus Master" },
  { id: 'foc-3', quote: "Track the effort. Trust the process.", category: 'focus', author: "EPA Tracker" },
  { id: 'foc-4', quote: "Every focused minute is an investment.", category: 'focus', author: "Deep Work" },
  { id: 'foc-5', quote: "Your only competition is yesterday’s you.", category: 'focus', author: "Self Growth" },
  { id: 'foc-6', quote: "Less scrolling. More progress.", category: 'focus', author: "Focus Guard" },
  { id: 'foc-7', quote: "Stay focused. Your goal hasn’t moved.", category: 'focus', author: "EPA Goal" },
  { id: 'foc-8', quote: "One focused session can change your whole day.", category: 'focus', author: "Deep Work" },

  // ⚡ When the user feels lazy
  { id: 'lazy-1', quote: "You said you wanted it. Now prove it.", category: 'lazy', author: "Reality Check" },
  { id: 'lazy-2', quote: "Start before you feel ready.", category: 'lazy', author: "Action First" },
  { id: 'lazy-3', quote: "You don’t have to feel motivated to begin.", category: 'lazy', author: "Discipline" },
  { id: 'lazy-4', quote: "Just start. Momentum will follow.", category: 'lazy', author: "Momentum" },
  { id: 'lazy-5', quote: "Five minutes is better than zero.", category: 'lazy', author: "Micro Start" },
  { id: 'lazy-6', quote: "The hardest part is opening the book.", category: 'lazy', author: "First Step" },
  { id: 'lazy-7', quote: "Do it tired. Do it slowly. Just don’t quit.", category: 'lazy', author: "Resilience" },

  // 🧠 Exam-focused
  { id: 'exam-1', quote: "The exam rewards what you practiced when nobody was watching.", category: 'exam', author: "Exam Mentor" },
  { id: 'exam-2', quote: "Every question you solve today is one less surprise tomorrow.", category: 'exam', author: "Question Bank" },
  { id: 'exam-3', quote: "Prepare now so panic doesn’t prepare you later.", category: 'exam', author: "Exam Ready" },
  { id: 'exam-4', quote: "Your marks are being built before exam day.", category: 'exam', author: "BUET/Medical Guide" },
  { id: 'exam-5', quote: "Don’t wish for a better result. Prepare for one.", category: 'exam', author: "Admission Master" },
  { id: 'exam-6', quote: "The syllabus won’t finish itself.", category: 'exam', author: "Syllabus Mentor" },
  { id: 'exam-7', quote: "Future you is counting on today’s effort.", category: 'exam', author: "Future Self" },

  // 🏆 Strong motivational ones
  { id: 'mot-1', quote: "Your goal is bigger than your excuses.", category: 'motivational', author: "Power Motivation" },
  { id: 'mot-2', quote: "You are closer than you think. Keep going.", category: 'motivational', author: "Encouragement" },
  { id: 'mot-3', quote: "Dreams don’t respond to procrastination.", category: 'motivational', author: "Dream Builder" },
  { id: 'mot-4', quote: "The version of you you want to become is built through discipline.", category: 'motivational', author: "Identity" },
  { id: 'mot-5', quote: "Make today’s effort tomorrow’s advantage.", category: 'motivational', author: "Advantage" },
  { id: 'mot-6', quote: "Nobody can study for you. Nobody can earn your result for you.", category: 'motivational', author: "Accountability" },
  { id: 'mot-7', quote: "You don’t need a perfect day. You need a productive one.", category: 'motivational', author: "Productivity" },
  { id: 'mot-8', quote: "Keep going. Your future self is watching.", category: 'motivational', author: "Future Self" }
];
