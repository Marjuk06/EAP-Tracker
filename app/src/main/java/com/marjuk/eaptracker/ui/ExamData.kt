package com.marjuk.eaptracker.ui

val offlineExams = listOf(
    // ================= WEEK 1 (15-Aug to 21-Aug) =================
    ExamItem(
        date = "15-Aug-26",
        day = "Saturday",
        exams = emptyList(),
        syllabus = listOf("কোর্স শুরুর পূর্বপ্রস্তুতি")
    ),
    ExamItem(
        date = "16-Aug-26",
        day = "Sunday",
        exams = emptyList(),
        syllabus = listOf("স্ব-অধ্যয়ন ও প্রস্তুতি")
    ),
    ExamItem(
        date = "17-Aug-26",
        day = "Monday",
        exams = emptyList(),
        syllabus = listOf("অরিয়েন্টেশন ও দিকনির্দেশনা")
    ),
    ExamItem(
        date = "18-Aug-26",
        day = "Tuesday",
        exams = emptyList(),
        syllabus = listOf("স্ব-অধ্যয়ন ও প্রস্তুতি")
    ),
    ExamItem(
        date = "19-Aug-26",
        day = "Wednesday",
        exams = listOf("P-01 Introductory Exam MCQ (25)"),
        syllabus = listOf("Introductory Exam: Physics Basics & Overview")
    ),
    ExamItem(
        date = "20-Aug-26",
        day = "Thursday",
        exams = emptyList(),
        syllabus = listOf("স্ব-অধ্যয়ন ও প্র্যাকটিস")
    ),
    ExamItem(
        date = "21-Aug-26",
        day = "Friday",
        exams = emptyList(),
        syllabus = listOf("সাপ্তাহিক রিভিশন")
    ),

    // ================= WEEK 2 (22-Aug to 28-Aug) =================
    ExamItem(
        date = "22-Aug-26",
        day = "Saturday",
        exams = listOf("P-01 MCQ (20) + Written (10)"),
        syllabus = listOf("P-01: ভেক্টর (ভেক্টর লব্ধি, উপাংশ বিভাজন, ডট ও ক্রস গুণন, ভেক্টর ক্যালকুলাস, নদী-নৌকা)")
    ),
    ExamItem(
        date = "23-Aug-26",
        day = "Sunday",
        exams = emptyList(),
        syllabus = listOf("স্ব-অধ্যয়ন ও প্র্যাকটিস")
    ),
    ExamItem(
        date = "24-Aug-26",
        day = "Monday",
        exams = listOf("C-01 MCQ (20) + Written (10)"),
        syllabus = listOf("C-01: পরিমাণগত রসায়ন (মোল, ঘনমাত্রা, জারণ-বিজারণ, টাইট্রেশন) ও ল্যাবরেটরি")
    ),
    ExamItem(
        date = "25-Aug-26",
        day = "Tuesday",
        exams = emptyList(),
        syllabus = listOf("স্ব-অধ্যয়ন ও প্র্যাকটিস")
    ),
    ExamItem(
        date = "26-Aug-26",
        day = "Wednesday",
        exams = listOf("M-01 MCQ (20) + Written (10)"),
        syllabus = listOf("M-01: সরলরেখা (স্থানাঙ্ক ব্যবস্থা, বিভিন্ন ক্ষেত্রে সরলরেখার সমীকরণ ও প্রতিবিম্ব)")
    ),
    ExamItem(
        date = "27-Aug-26",
        day = "Thursday",
        exams = listOf(
            "Engg. Offline Weekly Exam-01: (P₁ + C₁ + M₁)",
            "Varsity 'KA' Offline Weekly Exam-01: (P₁ + C₁ + M₁ + Bio₁/Ba/E)"
        ),
        syllabus = listOf(
            "Engg Weekly-01: P-01 + C-01 + M-01",
            "Varsity 'KA' Weekly-01: P₁ + C₁ + M₁ + Bio₁/Bangla/English"
        )
    ),
    ExamItem(
        date = "28-Aug-26",
        day = "Friday",
        exams = listOf("Medical Offline Weekly Exam-01: (Bio₁ + C₁ + P₁ + GK + E)"),
        syllabus = listOf("Medical Weekly-01: Biology-01 + Chemistry-01 + Physics-01 + GK + English")
    ),

    // ================= WEEK 3 (29-Aug to 04-Sep) =================
    ExamItem(
        date = "29-Aug-26",
        day = "Saturday",
        exams = listOf("M-02 MCQ (20) + Written (10)"),
        syllabus = listOf("M-02: বৃত্ত (শর্ত, বিভিন্ন ক্ষেত্রে সমীকরণ, স্পর্শক-ছেদক, সাধারণ স্পর্শক)")
    ),
    ExamItem(
        date = "30-Aug-26",
        day = "Sunday",
        exams = emptyList(),
        syllabus = listOf("স্ব-অধ্যয়ন ও প্র্যাকটিস")
    ),
    ExamItem(
        date = "31-Aug-26",
        day = "Monday",
        exams = listOf("P-02 MCQ (20) + Written (10)"),
        syllabus = listOf("P-02: গতিবিদ্যা ও ভৌত জগৎ ও পরিমাপ (প্রাস সংক্রান্ত সমস্যাবলি সহ)")
    ),
    ExamItem(
        date = "01-Sep-26",
        day = "Tuesday",
        exams = emptyList(),
        syllabus = listOf("স্ব-অধ্যয়ন ও প্র্যাকটিস")
    ),
    ExamItem(
        date = "02-Sep-26",
        day = "Wednesday",
        exams = listOf("C-02 MCQ (20) + Written (10)"),
        syllabus = listOf("C-02: রাসায়নিক পরিবর্তন (গ্রীন কেমিস্ট্রি, সাম্যাবস্থা, Kp, Kc, গতিবিদ্যা, আরহেনিয়াস)")
    ),
    ExamItem(
        date = "03-Sep-26",
        day = "Thursday",
        exams = listOf(
            "Engg. Offline Weekly Exam-02: (P₂ + C₂ + M₂)",
            "Varsity 'KA' Offline Weekly Exam-02: (P₂ + C₂ + M₂ + Bio₂/Ba/E)"
        ),
        syllabus = listOf(
            "Engg Weekly-02: P-02 + C-02 + M-02",
            "Varsity 'KA' Weekly-02: P₂ + C₂ + M₂ + Bio₂/Bangla/English"
        )
    ),
    ExamItem(
        date = "04-Sep-26",
        day = "Friday",
        exams = listOf("Medical Offline Weekly Exam-02: (Bio₂ + C₂ + P₂ + GK + E)"),
        syllabus = listOf("Medical Weekly-02: Biology-02 + Chemistry-02 + Physics-02 + GK + English")
    ),

    // ================= WEEK 4 (05-Sep to 11-Sep) =================
    ExamItem(
        date = "05-Sep-26",
        day = "Saturday",
        exams = listOf("C-03 MCQ (20) + Written (10)"),
        syllabus = listOf("C-03: রাসায়নিক পরিবর্তন (অম্ল-ক্ষার সাম্যাবস্থা, তাপ-রসায়ন) ও পরিবেশ রসায়ন")
    ),
    ExamItem(
        date = "06-Sep-26",
        day = "Sunday",
        exams = emptyList(),
        syllabus = listOf("স্ব-অধ্যয়ন ও প্র্যাকটিস")
    ),
    ExamItem(
        date = "07-Sep-26",
        day = "Monday",
        exams = listOf("M-03 MCQ (20) + Written (10)"),
        syllabus = listOf("M-03: কণিক (কণিক সনাক্তকরণ, পরাবৃত্ত, উপবৃত্ত, অধিবৃত্ত)")
    ),
    ExamItem(
        date = "08-Sep-26",
        day = "Tuesday",
        exams = emptyList(),
        syllabus = listOf("স্ব-অধ্যয়ন ও প্র্যাকটিস")
    ),
    ExamItem(
        date = "09-Sep-26",
        day = "Wednesday",
        exams = listOf("P-03 MCQ (20) + Written (10)"),
        syllabus = listOf("P-03: নিউটনীয় বলবিদ্যা (নিউটনের সূত্র, ঘর্ষণ, ভরবেগ, সংঘর্ষ, জড়তার ভ্রামক, টর্ক)")
    ),
    ExamItem(
        date = "10-Sep-26",
        day = "Thursday",
        exams = listOf(
            "Engg. Offline Weekly Exam-03: (P₃ + C₃ + M₃)",
            "Varsity 'KA' Offline Weekly Exam-03: (P₃ + C₃ + M₃ + Bio₃/Ba/E)"
        ),
        syllabus = listOf(
            "Engg Weekly-03: P-03 + C-03 + M-03",
            "Varsity 'KA' Weekly-03: P₃ + C₃ + M₃ + Bio₃/Bangla/English"
        )
    ),
    ExamItem(
        date = "11-Sep-26",
        day = "Friday",
        exams = listOf("Medical Offline Weekly Exam-03: (Bio₃ + C₃ + P₃ + GK + E)"),
        syllabus = listOf("Medical Weekly-03: Biology-03 + Chemistry-03 + Physics-03 + GK + English")
    ),

    // ================= WEEK 5 (12-Sep to 18-Sep) =================
    ExamItem(
        date = "12-Sep-26",
        day = "Saturday",
        exams = listOf("P-04 MCQ (20) + Written (10)"),
        syllabus = listOf("P-04: নিউটনীয় বলবিদ্যা (বৃত্তাকার গতি ও ব্যাংকিং) এবং কাজ, শক্তি ও ক্ষমতা")
    ),
    ExamItem(
        date = "13-Sep-26",
        day = "Sunday",
        exams = emptyList(),
        syllabus = listOf("স্ব-অধ্যয়ন ও প্র্যাকটিস")
    ),
    ExamItem(
        date = "14-Sep-26",
        day = "Monday",
        exams = listOf("C-04 MCQ (20) + Written (10)"),
        syllabus = listOf("C-04: গুণগত রসায়ন (পরমাণু মডেল, কোয়ান্টাম সংখ্যা, বর্ণালী, দ্রাব্যতা, ক্রোমাটোগ্রাফি)")
    ),
    ExamItem(
        date = "15-Sep-26",
        day = "Tuesday",
        exams = emptyList(),
        syllabus = listOf("স্ব-অধ্যয়ন ও প্র্যাকটিস")
    ),
    ExamItem(
        date = "16-Sep-26",
        day = "Wednesday",
        exams = listOf("M-04 MCQ (20) + Written (10)"),
        syllabus = listOf("M-04: বাস্তব সংখ্যা ও অসমতা এবং জটিল সংখ্যা (মডুলাস, আর্গুমেন্ট, সঞ্চারপথ)")
    ),
    ExamItem(
        date = "17-Sep-26",
        day = "Thursday",
        exams = listOf(
            "Engg. Offline Weekly Exam-04: (P₄ + C₄ + M₄)",
            "Varsity 'KA' Offline Weekly Exam-04: (P₄ + C₄ + M₄ + Bio₄/Ba/E)"
        ),
        syllabus = listOf(
            "Engg Weekly-04: P-04 + C-04 + M-04",
            "Varsity 'KA' Weekly-04: P₄ + C₄ + M₄ + Bio₄/Bangla/English"
        )
    ),
    ExamItem(
        date = "18-Sep-26",
        day = "Friday",
        exams = listOf("Medical Offline Weekly Exam-04: (Bio₄ + C₄ + P₄ + GK + E)"),
        syllabus = listOf("Medical Weekly-04: Biology-04 + Chemistry-04 + Physics-04 + GK + English")
    ),

    // ================= WEEK 6 (19-Sep to 25-Sep) =================
    ExamItem(
        date = "19-Sep-26",
        day = "Saturday",
        exams = listOf("M-05 MCQ (20) + Written (10)"),
        syllabus = listOf("M-05: ম্যাট্রিক্স ও নির্ণায়ক এবং বহুপদী ও বহুপদী সমীকরণ")
    ),
    ExamItem(
        date = "20-Sep-26",
        day = "Sunday",
        exams = listOf("Engg. Monthly Revision Test-01: PCM Lecture (01-04) Written (600)"),
        syllabus = listOf("১ম মাসের সম্পূর্ণ ইঞ্জিনিয়ারিং সিলেবাস: P(01-04) + C(01-04) + M(01-04)")
    ),
    ExamItem(
        date = "21-Sep-26",
        day = "Monday",
        exams = emptyList(),
        syllabus = listOf("রিভিশন ও স্ব-অধ্যয়ন")
    ),
    ExamItem(
        date = "22-Sep-26",
        day = "Tuesday",
        exams = emptyList(),
        syllabus = listOf("লেকচার পর্যালোচনা")
    ),
    ExamItem(
        date = "23-Sep-26",
        day = "Wednesday",
        exams = emptyList(),
        syllabus = listOf("প্রশ্নব্যাংক অনুশীলন")
    ),
    ExamItem(
        date = "24-Sep-26",
        day = "Thursday",
        exams = emptyList(),
        syllabus = listOf("কনসেপ্ট ক্লিয়ারিং")
    ),
    ExamItem(
        date = "25-Sep-26",
        day = "Friday",
        exams = emptyList(),
        syllabus = listOf("সাপ্তাহিক মূল্যায়ন")
    )
)

val onlineExams = listOf(
    // ================= WEEK 1 (15-Aug to 21-Aug) =================
    ExamItem(
        date = "15-Aug-26",
        day = "Saturday",
        exams = emptyList(),
        syllabus = listOf("অনলাইন পরীক্ষার প্রস্তুতি সূচনা")
    ),
    ExamItem(
        date = "16-Aug-26",
        day = "Sunday",
        exams = emptyList(),
        syllabus = listOf("স্ব-অধ্যয়ন ও গাইডলাইন")
    ),
    ExamItem(
        date = "17-Aug-26",
        day = "Monday",
        exams = emptyList(),
        syllabus = listOf("অনলাইন অরিয়েন্টেশন")
    ),
    ExamItem(
        date = "18-Aug-26",
        day = "Tuesday",
        exams = emptyList(),
        syllabus = listOf("লাইভ ক্লাস ও টেস্ট প্রস্তুতি")
    ),
    ExamItem(
        date = "19-Aug-26",
        day = "Wednesday",
        exams = listOf("P-01 Part-01 MCQ (15) + Wri. (10)"),
        syllabus = listOf("P-01 Part-01: ভেক্টর (ভেক্টর লব্ধি, সামান্তরিক সূত্র, উপাংশ বিভাজন, আপেক্ষিক বেগ)")
    ),
    ExamItem(
        date = "20-Aug-26",
        day = "Thursday",
        exams = listOf("P-01 Part-02 MCQ (15) + Wri. (10)"),
        syllabus = listOf("P-01 Part-02: ভেক্টর (ডট ও ক্রস গুণন, ভেক্টর ক্যালকুলাস, নদী-নৌকা)")
    ),
    ExamItem(
        date = "21-Aug-26",
        day = "Friday",
        exams = listOf("C-01 Part-01 MCQ (15) + Wri. (10)"),
        syllabus = listOf("C-01 Part-01: পরিমাণগত রসায়ন (মোল, রাসায়নিক বিক্রিয়া, সহানুপাত সূত্র, ঘনমাত্রা)")
    ),

    // ================= WEEK 2 (22-Aug to 28-Aug) =================
    ExamItem(
        date = "22-Aug-26",
        day = "Saturday",
        exams = listOf("C-01 Part-02 MCQ (15) + Wri. (10)"),
        syllabus = listOf("C-01 Part-02: পরিমাণগত রসায়ন (জারণ-বিজারণ, টাইট্রেশন) ও ল্যাবরেটরির নিরাপদ ব্যবহার")
    ),
    ExamItem(
        date = "23-Aug-26",
        day = "Sunday",
        exams = listOf("Bio-01 Part-01+02 MCQ (30) + Wri. (5)"),
        syllabus = listOf("Biology-01 (Part-01 + Part-02)")
    ),
    ExamItem(
        date = "24-Aug-26",
        day = "Monday",
        exams = listOf("M-01 Part-01 MCQ (15) + Wri. (10)"),
        syllabus = listOf("M-01 Part-01: সরলরেখা (স্থানাঙ্ক ব্যবস্থা থেকে বিভিন্ন ক্ষেত্রে সমীকরণ)")
    ),
    ExamItem(
        date = "25-Aug-26",
        day = "Tuesday",
        exams = listOf("M-01 Part-02 MCQ (15) + Wri. (10)"),
        syllabus = listOf("M-01 Part-02: সরলরেখা (সমীকরণ সম্পর্কিত সমস্যাবলি থেকে প্রতিবিম্ব)")
    ),
    ExamItem(
        date = "26-Aug-26",
        day = "Wednesday",
        exams = listOf("P-02 Part-01 MCQ (15) + Wri. (10)"),
        syllabus = listOf("P-02 Part-01: গতিবিদ্যা (প্রাস বাদে)")
    ),
    ExamItem(
        date = "27-Aug-26",
        day = "Thursday",
        exams = listOf(
            "P-02 Part-02 MCQ (15) + Wri. (10)",
            "Engg. Weekly Live Exam-01: (P₁ + C₁ + M₁) [27-28 Aug]"
        ),
        syllabus = listOf(
            "P-02 Part-02: ভৌত জগৎ ও পরিমাপ; গতিবিদ্যা (প্রাস)",
            "Engg Weekly Live-01: P-01 + C-01 + M-01"
        )
    ),
    ExamItem(
        date = "28-Aug-26",
        day = "Friday",
        exams = listOf(
            "Varsity 'KA' Weekly Live Exam-01: (P₁ + C₁ + M₁ + Bio₁/Ba/E) [28-29 Aug]",
            "Medical Weekly Live Exam-01: (Bio₁ + C₁ + P₁ + GK + E) [28-29 Aug]",
            "Varsity 'Kha' Weekly Live Exam-01: (E + GK + B) [28-29 Aug]"
        ),
        syllabus = listOf("Varsity 'KA', 'Kha' & Medical Weekly Live Exam-01")
    ),

    // ================= WEEK 3 (29-Aug to 04-Sep) =================
    ExamItem(
        date = "29-Aug-26",
        day = "Saturday",
        exams = listOf("C-02 Part-01 MCQ (15) + Wri. (10)"),
        syllabus = listOf("C-02 Part-01: রাসায়নিক পরিবর্তন (গ্রীন কেমিস্ট্রি, রাসায়নিক সাম্যাবস্থা, Kp, Kc)")
    ),
    ExamItem(
        date = "30-Aug-26",
        day = "Sunday",
        exams = listOf("C-02 Part-02 MCQ (15) + Wri. (10)"),
        syllabus = listOf("C-02 Part-02: রাসায়নিক পরিবর্তন (গতিবিদ্যা, আরহেনিয়াস সমীকরণ, প্রভাবক)")
    ),
    ExamItem(
        date = "31-Aug-26",
        day = "Monday",
        exams = listOf("Bio-02 Part-01+02 MCQ (30) + Wri. (5)"),
        syllabus = listOf("Biology-02 (Part-01 + Part-02)")
    ),
    ExamItem(
        date = "01-Sep-26",
        day = "Tuesday",
        exams = listOf("M-02 Part-01 MCQ (15) + Wri. (10)"),
        syllabus = listOf("M-02 Part-01: বৃত্ত (শর্ত, বিভিন্ন ক্ষেত্রে সমীকরণ, পোলার সমীকরণ)")
    ),
    ExamItem(
        date = "02-Sep-26",
        day = "Wednesday",
        exams = listOf("M-02 Part-02 MCQ (15) + Wri. (10)"),
        syllabus = listOf("M-02 Part-02: বৃত্ত (স্পর্শক-ছেদক, দুটি বৃত্তের পারস্পরিক অবস্থান)")
    ),
    ExamItem(
        date = "03-Sep-26",
        day = "Thursday",
        exams = listOf(
            "P-03 Part-01 MCQ (15) + Wri. (10)",
            "Engg. Weekly Live Exam-02: (P₂ + C₂ + M₂) [03-04 Sep]"
        ),
        syllabus = listOf(
            "P-03 Part-01: নিউটনীয় বলবিদ্যা (নিউটনের সূত্র, ঘর্ষণ)",
            "Engg Weekly Live-02: P-02 + C-02 + M-02"
        )
    ),
    ExamItem(
        date = "04-Sep-26",
        day = "Friday",
        exams = listOf(
            "P-03 Part-02 MCQ (15) + Wri. (10)",
            "Varsity 'KA' Weekly Live Exam-02 [04-05 Sep]",
            "Medical Weekly Live Exam-02 [04-05 Sep]",
            "Varsity 'Kha' Weekly Live Exam-02 [04-05 Sep]"
        ),
        syllabus = listOf("P-03 Part-02 | Varsity 'KA', 'Kha' & Medical Weekly Live Exam-02")
    ),

    // ================= WEEK 4 (05-Sep to 11-Sep) =================
    ExamItem(
        date = "05-Sep-26",
        day = "Saturday",
        exams = listOf("C-03 Part-01 MCQ (15) + Wri. (10)"),
        syllabus = listOf("C-03 Part-01: রাসায়নিক পরিবর্তন (অম্ল-ক্ষার সাম্যাবস্থা), পরিবেশ রসায়ন")
    ),
    ExamItem(
        date = "06-Sep-26",
        day = "Sunday",
        exams = listOf(
            "C-03 Part-02 MCQ (15) + Wri. (10)",
            "Bio-03 Part-01+02 MCQ (30) + Wri. (5)"
        ),
        syllabus = listOf(
            "C-03 Part-02: রাসায়নিক পরিবর্তন (তাপ-রসায়ন)",
            "Biology-03 (Part-01 + Part-02)"
        )
    ),
    ExamItem(
        date = "07-Sep-26",
        day = "Monday",
        exams = listOf("M-03 Part-01 MCQ (15) + Wri. (10)"),
        syllabus = listOf("M-03 Part-01: কণিক (কণিক সনাক্তকরণ ও পরাবৃত্ত)")
    ),
    ExamItem(
        date = "08-Sep-26",
        day = "Tuesday",
        exams = listOf("M-03 Part-02 MCQ (15) + Wri. (10)"),
        syllabus = listOf("M-03 Part-02: কণিক (উপবৃত্ত, অধিবৃত্ত, স্পর্শক/ছেদক)")
    ),
    ExamItem(
        date = "09-Sep-26",
        day = "Wednesday",
        exams = listOf("P-04 Part-01 MCQ (15) + Wri. (10)"),
        syllabus = listOf("P-04 Part-01: নিউটনীয় বলবিদ্যা (বৃত্তাকার গতি ও ব্যাংকিং)")
    ),
    ExamItem(
        date = "10-Sep-26",
        day = "Thursday",
        exams = listOf(
            "P-04 Part-02 MCQ (15) + Wri. (10)",
            "Engg. Weekly Live Exam-03: (P₃ + C₃ + M₃) [10-11 Sep]"
        ),
        syllabus = listOf(
            "P-04 Part-02: কাজ, শক্তি ও ক্ষমতা",
            "Engg Weekly Live-03: P-03 + C-03 + M-03"
        )
    ),
    ExamItem(
        date = "11-Sep-26",
        day = "Friday",
        exams = listOf(
            "C-04 Part-01 MCQ (15) + Wri. (10)",
            "Varsity 'KA' Weekly Live Exam-03 [11-12 Sep]",
            "Medical Weekly Live Exam-03 [11-12 Sep]",
            "Varsity 'Kha' Weekly Live Exam-03 [11-12 Sep]"
        ),
        syllabus = listOf("C-04 Part-01 | Varsity 'KA', 'Kha' & Medical Weekly Live Exam-03")
    ),

    // ================= WEEK 5 (12-Sep to 18-Sep) =================
    ExamItem(
        date = "12-Sep-26",
        day = "Saturday",
        exams = listOf("C-04 Part-02 MCQ (15) + Wri. (10)"),
        syllabus = listOf("C-04 Part-02: দ্রাব্যতা, আয়ন সনাক্তকরণ, ক্রোমাটোগ্রাফি")
    ),
    ExamItem(
        date = "13-Sep-26",
        day = "Sunday",
        exams = listOf("Bio-04 Part-01+02 MCQ (30) + Wri. (5)"),
        syllabus = listOf("Biology-04 (Part-01 + Part-02)")
    ),
    ExamItem(
        date = "14-Sep-26",
        day = "Monday",
        exams = listOf("M-04 Part-01 MCQ (15) + Wri. (10)"),
        syllabus = listOf("M-04 Part-01: বাস্তব সংখ্যা ও অসমতা, জটিল সংখ্যা (মডুলাস, আর্গুমেন্ট)")
    ),
    ExamItem(
        date = "15-Sep-26",
        day = "Tuesday",
        exams = listOf("M-04 Part-02 MCQ (15) + Wri. (10)"),
        syllabus = listOf("M-04 Part-02: জটিল সংখ্যা (পোলার রূপ, অনুবন্ধী, মূল, সঞ্চারপথ)")
    ),
    ExamItem(
        date = "16-Sep-26",
        day = "Wednesday",
        exams = listOf("P-05 Part-01 MCQ (15) + Wri. (10)"),
        syllabus = listOf("P-05 Part-01: মহাকর্ষ ও অভিকর্ষ")
    ),
    ExamItem(
        date = "17-Sep-26",
        day = "Thursday",
        exams = listOf(
            "P-05 Part-02 MCQ (15) + Wri. (10)",
            "Engg. Weekly Live Exam-04: (P₄ + C₄ + M₄) [17-18 Sep]"
        ),
        syllabus = listOf(
            "P-05 Part-02: পদার্থের গাঠনিক ধর্ম",
            "Engg Weekly Live-04: P-04 + C-04 + M-04"
        )
    ),
    ExamItem(
        date = "18-Sep-26",
        day = "Friday",
        exams = listOf(
            "Varsity 'KA' Weekly Live Exam-04 [18-19 Sep]",
            "Medical Weekly Live Exam-04 [18-19 Sep]",
            "Varsity 'Kha' Weekly Live Exam-04 [18-19 Sep]"
        ),
        syllabus = listOf("Varsity 'KA', 'Kha' & Medical Weekly Live Exam-04")
    ),

    // ================= WEEK 6 (19-Sep to 25-Sep) =================
    ExamItem(
        date = "19-Sep-26",
        day = "Saturday",
        exams = listOf(
            "C-05 Part-01 MCQ (15) + Wri. (10)",
            "C-05 Part-02 MCQ (15) + Wri. (10)"
        ),
        syllabus = listOf("C-05: মৌলের পর্যায়বৃত্ত ধর্ম, ব্লক মৌল, সংকরায়ন, বন্ধন, পোলারিটি")
    ),
    ExamItem(
        date = "20-Sep-26",
        day = "Sunday",
        exams = listOf(
            "Bio-05 Part-01+02 MCQ (30) + Wri. (5)",
            "Engg. Monthly Live Exam-01 Written (600) [20-21 Sep]"
        ),
        syllabus = listOf("Bio-05 | Engineering Monthly Revision Test-01: PCM Lecture (01-04)")
    ),
    ExamItem(
        date = "21-Sep-26",
        day = "Monday",
        exams = emptyList(),
        syllabus = listOf("রিভিশন ও স্ব-অধ্যয়ন")
    ),
    ExamItem(
        date = "22-Sep-26",
        day = "Tuesday",
        exams = emptyList(),
        syllabus = listOf("অনলাইন লেকচার রিভিশন")
    ),
    ExamItem(
        date = "23-Sep-26",
        day = "Wednesday",
        exams = emptyList(),
        syllabus = listOf("প্রশ্নব্যাংক অনুশীলন")
    ),
    ExamItem(
        date = "24-Sep-26",
        day = "Thursday",
        exams = emptyList(),
        syllabus = listOf("কনসেপ্ট ক্লিয়ারিং")
    ),
    ExamItem(
        date = "25-Sep-26",
        day = "Friday",
        exams = emptyList(),
        syllabus = listOf("সাপ্তাহিক সেলফ টেস্ট")
    )
)
