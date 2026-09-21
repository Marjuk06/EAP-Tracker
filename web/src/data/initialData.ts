import { Subject, RoutineItem, UserProfile } from '../types';

export const initialSubjects: Subject[] = [
  {
    id: 'physics',
    name: 'Physics',
    iconName: 'Zap',
    papers: [
      {
        id: 'phy-1',
        name: '1st Paper',
        chapters: [
          'ভৌত জগৎ ও পরিমাপ',
          'ভেক্টর',
          'গতিবিদ্যা',
          'নিউটনীয় বলবিদ্যা',
          'কাজ, শক্তি ও ক্ষমতা',
          'মহাকর্ষ ও অভিকর্ষ',
          'পদার্থের গাঠনিক ধর্ম',
          'পর্যায়বৃত্ত গতি',
          'তরঙ্গ',
          'আদর্শ গ্যাস ও গ্যাসের গতি তত্ত্ব'
        ].map((name, idx) => ({
          id: `phy-1-${idx}`,
          name,
          sections: [
            { name: 'Book', label: 'বই রিডিং', isCompleted: false },
            { name: 'Slide', label: 'ক্লাস / স্লাইড', isCompleted: false },
            { name: 'QB', label: 'প্রশ্নব্যাংক', isCompleted: false },
            { name: 'Concept', label: 'কনসেপ্ট / প্র্যাকটিস', isCompleted: false }
          ]
        }))
      },
      {
        id: 'phy-2',
        name: '2nd Paper',
        chapters: [
          'তাপগতিবিদ্যা',
          'স্থির তড়িৎ',
          'চল তড়িৎ',
          'তড়িৎ প্রবাহের চৌম্বক ক্রিয়া ও চৌম্বকত্ব',
          'তড়িৎচৌম্বক আবেশ ও পরিবর্তী প্রবাহ',
          'জ্যামিতিক আলোকবিজ্ঞান',
          'ভৌত আলোকবিজ্ঞান',
          'আধুনিক পদার্থবিজ্ঞানের সূচনা',
          'পরমাণুর মডেল ও নিউক্লিয়ার পদার্থবিজ্ঞান',
          'সেমিকন্ডাক্টর ও ইলেকট্রনিক্স',
          'জ্যোতির্বিজ্ঞান'
        ].map((name, idx) => ({
          id: `phy-2-${idx}`,
          name,
          sections: [
            { name: 'Book', label: 'বই রিডিং', isCompleted: false },
            { name: 'Slide', label: 'ক্লাস / স্লাইড', isCompleted: false },
            { name: 'QB', label: 'প্রশ্নব্যাংক', isCompleted: false },
            { name: 'Concept', label: 'কনসেপ্ট / প্র্যাকটিস', isCompleted: false }
          ]
        }))
      }
    ]
  },
  {
    id: 'chemistry',
    name: 'Chemistry',
    iconName: 'FlaskConical',
    papers: [
      {
        id: 'chem-1',
        name: '1st Paper',
        chapters: [
          'ল্যাবরেটরির নিরাপদ ব্যবহার',
          'গুণগত রসায়ন',
          'মৌলের পর্যায়বৃত্ত ধর্ম ও রাসায়নিক বন্ধন',
          'রাসায়নিক পরিবর্তন',
          'কর্মমুখী রসায়ন'
        ].map((name, idx) => ({
          id: `chem-1-${idx}`,
          name,
          sections: [
            { name: 'Book', label: 'বই রিডিং', isCompleted: false },
            { name: 'Slide', label: 'ক্লাস / স্লাইড', isCompleted: false },
            { name: 'QB', label: 'প্রশ্নব্যাংক', isCompleted: false },
            { name: 'Concept', label: 'কনসেপ্ট / প্র্যাকটিস', isCompleted: false }
          ]
        }))
      },
      {
        id: 'chem-2',
        name: '2nd Paper',
        chapters: [
          'পরিবেশ রসায়ন',
          'জৈব রসায়ন',
          'পরিমাণগত রসায়ন',
          'তড়িৎ রসায়ন',
          'অর্থনৈতিক রসায়ন'
        ].map((name, idx) => ({
          id: `chem-2-${idx}`,
          name,
          sections: [
            { name: 'Book', label: 'বই রিডিং', isCompleted: false },
            { name: 'Slide', label: 'ক্লাস / স্লাইড', isCompleted: false },
            { name: 'QB', label: 'প্রশ্নব্যাংক', isCompleted: false },
            { name: 'Concept', label: 'কনসেপ্ট / প্র্যাকটিস', isCompleted: false }
          ]
        }))
      }
    ]
  },
  {
    id: 'higher-math',
    name: 'Higher Math',
    iconName: 'Calculator',
    papers: [
      {
        id: 'math-1',
        name: '1st Paper',
        chapters: [
          'ম্যাট্রিক্স ও নির্ণায়ক',
          'ভেক্টর',
          'সরলরেখা',
          'বৃত্ত',
          'বিন্যাস ও সমাবেশ',
          'ত্রিকোণমিতিক অনুপাত',
          'সংযুক্ত কোণের ত্রিকোণমিতিক অনুপাত',
          'ফাংশন ও ফাংশনের লেখচিত্র',
          'অন্তরীকরণ',
          'যোগজীকরণ'
        ].map((name, idx) => ({
          id: `math-1-${idx}`,
          name,
          sections: [
            { name: 'Book', label: 'বই রিডিং', isCompleted: false },
            { name: 'Slide', label: 'ক্লাস / স্লাইড', isCompleted: false },
            { name: 'QB', label: 'প্রশ্নব্যাংক', isCompleted: false },
            { name: 'Concept', label: 'কনসেপ্ট / প্র্যাকটিস', isCompleted: false }
          ]
        }))
      },
      {
        id: 'math-2',
        name: '2nd Paper',
        chapters: [
          'বাস্তব সংখ্যা ও অসমতা',
          'যোগাশ্রয়ী প্রোগ্রাম',
          'জটিল সংখ্যা',
          'বহুপদী ও বহুপদী সমীকরণ',
          'দ্বিপদী বিস্তৃতি',
          'কণিক',
          'বিপরীত ত্রিকোণমিতিক ফাংশন ও সমীকরণ',
          'স্থিতিবিদ্যা',
          'সমতলে বস্তুকণার গতি',
          'বিস্তার পরিমাপ ও সম্ভাবনা'
        ].map((name, idx) => ({
          id: `math-2-${idx}`,
          name,
          sections: [
            { name: 'Book', label: 'বই রিডিং', isCompleted: false },
            { name: 'Slide', label: 'ক্লাস / স্লাইড', isCompleted: false },
            { name: 'QB', label: 'প্রশ্নব্যাংক', isCompleted: false },
            { name: 'Concept', label: 'কনসেপ্ট / প্র্যাকটিস', isCompleted: false }
          ]
        }))
      }
    ]
  },
  {
    id: 'biology',
    name: 'Biology',
    iconName: 'Dna',
    papers: [
      {
        id: 'bio-1',
        name: '1st Paper',
        chapters: [
          'কোষ ও এর গঠন',
          'কোষ বিভাজন',
          'অণুজীব',
          'নগ্নবীজী ও আবৃতবীজী',
          'টিস্যু ও টিস্যুতন্ত্র',
          'উদ্ভিদ শারীরতত্ত্ব',
          'জীবপ্রযুক্তি'
        ].map((name, idx) => ({
          id: `bio-1-${idx}`,
          name,
          sections: [
            { name: 'Book', label: 'বই রিডিং', isCompleted: false },
            { name: 'Slide', label: 'ক্লাস / স্লাইড', isCompleted: false },
            { name: 'QB', label: 'প্রশ্নব্যাংক', isCompleted: false },
            { name: 'Concept', label: 'কনসেপ্ট / প্র্যাকটিস', isCompleted: false }
          ]
        }))
      },
      {
        id: 'bio-2',
        name: '2nd Paper',
        chapters: [
          'প্রাণীর বিভিন্নতা ও শ্রেণিবিন্যাস',
          'প্রাণীর পরিচিতি (হাইড্রা, ঘাসফড়িং, রুই)',
          'পরিপাক ও শোষণ',
          'রক্ত ও সংবহন',
          'শ্বসন ও শ্বাসক্রিয়া',
          'চলন ও অঙ্গচালনা',
          'জিনতত্ত্ব ও বিবর্তন'
        ].map((name, idx) => ({
          id: `bio-2-${idx}`,
          name,
          sections: [
            { name: 'Book', label: 'বই রিডিং', isCompleted: false },
            { name: 'Slide', label: 'ক্লাস / স্লাইড', isCompleted: false },
            { name: 'QB', label: 'প্রশ্নব্যাংক', isCompleted: false },
            { name: 'Concept', label: 'কনসেপ্ট / প্র্যাকটিস', isCompleted: false }
          ]
        }))
      }
    ]
  }
];

import masterEapData from './master_eap_data.json';

export const initialRoutines: RoutineItem[] = (masterEapData.offlineRoutine as any) || [];

export const motivationalQuotes = [
  {
    quote: "কঠিন পরিশ্রম কখনো বৃথা যায় না, প্রতিটি নির্ঘুম রাত তোমার স্বপ্নের সিঁড়ি তৈরি করছে।",
    author: "Admission Mentor"
  },
  {
    quote: "আজকের ত্যাগই আগামীকালের বুয়েট/মেডিকেলের স্বপ্নের পরিচয়পত্র।",
    author: "EAP Guide"
  },
  {
    quote: "Success isn't always about greatness. It's about consistency. Consistent hard work leads to success.",
    author: "Dwayne Johnson"
  },
  {
    quote: "প্রতিদিনের ছোট ছোট অগ্রগতি দিনশেষে এক বিশাল সাফল্যের রূপ নেয়। লেগে থাকো!",
    author: "Academic Inspiration"
  },
  {
    quote: "The secret of getting ahead is getting started.",
    author: "Mark Twain"
  },
  {
    quote: "প্রশ্নব্যাংক সমাধানের গতি আর নির্ভুলতাই অ্যাডমিশনের আসল অস্ত্র।",
    author: "BUETian Advice"
  }
];

export const admissionExamPresets = [
  { name: 'BUET Preliminary Admission Test', days: 120 },
  { name: 'BUET Final Written Test', days: 145 },
  { name: 'CKRUET Combined Admission Test', days: 135 },
  { name: 'DU A-Unit Admission Exam', days: 110 },
  { name: 'Medical College Admission Test (MBBS)', days: 95 },
  { name: 'IUT Admission Test', days: 130 },
  { name: 'MIST Admission Test', days: 100 }
];

export const defaultProfile: UserProfile = {
  name: 'Marjuk Amin',
  targetInstitution: 'BUET CSE',
  collegeName: 'Notre Dame College',
  hscBatch: "HSC '25",
  rollNo: 'EAP-2025-042',
  avatarEmoji: 'Zap',
  quoteText: 'বুয়েট ক্যাম্পাসের লাল দালানই আমার চূড়ান্ত গন্তব্য।',
  targetExamName: 'BUET Preliminary Admission Test',
  targetExamEpoch: Date.now() + 120 * 24 * 60 * 60 * 1000,
  dailyGoalHours: 8,
  preferredTrack: 'engineering',
  dailyRoutineAlertTime: '08:00',
  eveningReminderTime: '21:00',
  hapticEnabled: true,
  soundEnabled: true
};
